/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.util;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Utility class for asynchronous logging.
 */
public final class Logger {
    // we use one static thread for all Loggers
    static Runner runner;

    // the list of active loggers
    static ArrayList loggers;

    // hash map of loggers
    static HashMap loggerMap;

    // fields for date rendering and caching
    static DateFormat dformat = new SimpleDateFormat("[yyyy/MM/dd HH:mm] ");
    static long dateLastRendered;
    static String dateCache;

    // buffer for log items
    private List entries;

    // fields used for logging to files
    private String filename;
    private File logdir;
    private File logfile;
    private PrintWriter writer;

    // the canonical name for this logger
    String canonicalName;

    // used when logging to a PrintStream such as System.out
    private PrintStream out = null;

    // flag to tell runner thread if this log should be closed/discarded
    boolean closed = false;

    // number format for log file rotation
    DecimalFormat nformat = new DecimalFormat("000");
    DateFormat aformat = new SimpleDateFormat("yyyy-MM-dd");

    /**
     * Create a logger for a PrintStream, such as System.out.
     */
    public Logger(PrintStream out) {
        this.out = out;
        canonicalName = out.toString();

        // create a synchronized list for log entries since different threads may
        // attempt to modify the list at the same time
        entries = Collections.synchronizedList(new LinkedList());

        // register this instance with static logger list
        start(this);
    }

    /**
     * Create a file logger. The actual file names do have numbers appended and are
     * rotated every x bytes.
     */
    private Logger(String dirname, String filename) {
        this.filename = filename;
        logdir = new File(dirname);
        logfile = new File(logdir, filename + ".log");

        try {
            canonicalName = logfile.getCanonicalPath();
        } catch (IOException iox) {
            canonicalName = logfile.getAbsolutePath();
        }

        if (!logdir.exists()) {
            logdir.mkdirs();
        }

        try {
            if (logfile.exists() && (logfile.lastModified() < lastMidnight())) {
                // rotate if a log file exists and is NOT from today
                rotateLogFile();
            } else {
                // create a new log file, append to an existing file
                writer = new PrintWriter(new FileWriter(logfile.getAbsolutePath(), true),
                                         false);
            }
        } catch (IOException iox) {
            System.err.println("Error creating log " + canonicalName + ": " + iox);
        }

        // create a synchronized list for log entries since different threads may
        // attempt to modify the list at the same time
        entries = Collections.synchronizedList(new LinkedList());

        // register this instance with static logger list
        start(this);
    }

    /**
     * Get a logger with a symbolic file name within a directory.
     */
    public static synchronized Logger getLogger(String dirname, String filename) {
        if ((filename == null) || (dirname == null)) {
            throw new RuntimeException("Logger can't use null as file or directory name");
        }

        File file = new File(dirname, filename + ".log");
        Logger log = null;

        if (loggerMap != null) {
            try {
                log = (Logger) loggerMap.get(file.getCanonicalPath());
            } catch (IOException iox) {
                log = (Logger) loggerMap.get(file.getAbsolutePath());
            }
        }

        if ((log == null) || log.isClosed()) {
            log = new Logger(dirname, filename);
        }

        return log;
    }

    /**
     * Append a message to the log.
     */
    public void log(String msg) {
        // if we are closed, drop message without further notice
        if (closed) {
            return;
        }

        // it's enough to render the date every 5 seconds
        if ((System.currentTimeMillis() - 5000) > dateLastRendered) {
            renderDate();
        }

        entries.add(dateCache + msg);
    }

    private static synchronized void renderDate() {
        dateLastRendered = System.currentTimeMillis();
        dateCache = dformat.format(new Date());
    }

    /**
     *  Return an object  which identifies  this logger.
     */
    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     *  Get the list of unwritten entries
     */
    public List getEntries() {
        return entries;
    }

    /**
     * This is called by the runner thread to perform actual output.
     */
    public void write() {
        if (entries.isEmpty()) {
            return;
        }

        try {
            int l = entries.size();

            // check if writing to printstream or file
            if (out != null) {
                for (int i = 0; i < l; i++) {
                    String entry = (String) entries.get(0);

                    entries.remove(0);
                    out.println(entry);
                }
            } else {
                if ((writer == null) || !logfile.exists() || !logfile.canWrite()) {
                    // rotate the log file if we can't write to it
                    rotateLogFile();
                }

                for (int i = 0; i < l; i++) {
                    String entry = (String) entries.get(0);

                    entries.remove(0);
                    writer.println(entry);
                }

                writer.flush();
            }
        } catch (Exception x) {
            int e = entries.size();

            if (e > 1000) {
                // more than 1000 entries queued plus exception - something
                // is definitely wrong with this logger. Write a message to std err and
                // discard queued log entries.
                System.err.println("Error writing log file " + this + ": " + x);
                System.err.println("Discarding " + e + " log entries.");
                entries.clear();
            }
        }
    }

    /**
     *  Rotate log files, closing, renaming and gzipping the old file and
     *  start a new one.
     */
    private void rotateLogFile() throws IOException {
        // if the logger is not file based do nothing.
        if (logfile == null) {
            return;
        }

        if (writer != null) {
            try {
                writer.close();
            } catch (Exception ignore) {
            }
        }

        // only backup/rotate if the log file is not empty,
        if (logfile.exists() && (logfile.length() > 0)) {
            String today = aformat.format(new Date());
            int ct = 0;
            File archive = null;

            // first append just the date
            String archname = filename + "-" + today + ".log.gz";

            while ((archive == null) || archive.exists()) {
                archive = new File(logdir, archname);

                // for the next try we append a counter
                String archidx = (ct > 999) ? Integer.toString(ct) : nformat.format(++ct);

                archname = filename + "-" + today + "-" + archidx + ".log.gz";
            }

            if (logfile.renameTo(archive)) {
                (new GZipper(archive)).start();
            } else {
                System.err.println("Error rotating log file " + canonicalName +
                                   ". Old file will possibly be overwritten!");
            }
        }

        writer = new PrintWriter(new FileWriter(logfile), false);
    }

    /**
     * Tell whether this log is closed.
     */
    public boolean isClosed() {
        return closed;
    }

    /**
     * Tells a log to close down. Only the flag is set, the actual closing is
     * done by the runner thread next time it comes around.
     */
    public void close() {
        this.closed = true;
    }

    /**
     * Actually closes the file writer of a log.
     */
    void closeFiles() {
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception ignore) {
            }
        }
    }

    /**
     * Return a string representation of this Logger
     */
    public String toString() {
        return "Logger[" + canonicalName + "]";
    }

    /**
     *  Add a log to the list of logs and
     *  create and start the runner thread if necessary.
     */
    static synchronized void start(Logger log) {
        if (loggers == null) {
            loggers = new ArrayList();
        }

        if (loggerMap == null) {
            loggerMap = new HashMap();
        }

        loggers.add(log);
        loggerMap.put(log.canonicalName, log);

        if ((runner == null) || !runner.isAlive()) {
            runner = new Runner();
            runner.start();
        }
    }

    /**
     *  Return a list of all active Loggers
     */
    public static List getLoggers() {
        if (loggers == null) {
            return null;
        }

        return (List) loggers.clone();
    }

    /**
     *  Notify the runner thread that it should wake up and run.
     */
    public static void wakeup() {
        if (runner != null) {
            runner.wakeup();
        }
    }

    private static void rotateAllLogs() {
        int nloggers = loggers.size();

        for (int i = nloggers - 1; i >= 0; i--) {
            Logger log = (Logger) loggers.get(i);

            try {
                log.rotateLogFile();
            } catch (IOException io) {
                System.err.println("Error rotating log " + log.getCanonicalName() + ": " +
                                   io.toString());
            }
        }
    }

    /**
     *
     *
     * @return ...
     */
    public static long nextMidnight() {
        Calendar cal = Calendar.getInstance();

        cal.set(Calendar.DATE, 1 + cal.get(Calendar.DATE));
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 1);

        // for testing, rotate the logs every minute:
        // cal.set (Calendar.MINUTE, 1 + cal.get(Calendar.MINUTE));
        return cal.getTime().getTime();
    }

    /**
     *
     *
     * @return ...
     */
    public static long lastMidnight() {
        return nextMidnight() - 86400000;
    }

    /**
     *  test main method
     */
    public static void main(String[] args) {
        Logger log = new Logger(".", "testlog");
        long start = System.currentTimeMillis();

        for (int i = 0; i < 1000; i++) {
            log.log("test log entry " + i);

            try {
                Thread.sleep(100);
            } catch (InterruptedException ie) {
            }
        }

        log.log("done: " + (System.currentTimeMillis() - start));
        System.err.println(System.currentTimeMillis() - start);
        log.close();
    }

    /**
     *  The static runner class that loops through all loggers.
     */
    static class Runner extends Thread {
        public synchronized void run() {
            long nextMidnight = nextMidnight();

            while ((runner == this) && !isInterrupted()) {
                if (nextMidnight < System.currentTimeMillis()) {
                    rotateAllLogs();
                    nextMidnight = nextMidnight();
                }

                int nloggers = loggers.size();

                for (int i = nloggers - 1; i >= 0; i--) {
                    try {
                        Logger log = (Logger) loggers.get(i);

                        log.write();

                        if (log.closed && log.entries.isEmpty()) {
                            loggers.remove(log);
                            log.closeFiles();
                        }
                    } catch (Exception x) {
                        System.err.println("Error in Logger main loop: " + x);
                    }
                }

                // if there are no active logs, exit logger thread
                if (loggers.size() == 0) {
                    return;
                }

                try {
                    wait(250);
                } catch (InterruptedException ix) {
                }
            }
        }

        public synchronized void wakeup() {
            notifyAll();
        }
    }

    /**
     * a Thread class that zips up a file, filename will stay the same.
     */
    class GZipper extends Thread {
        File file;
        File temp;

        public GZipper(File file) {
            this.file = file;
            this.temp = new File(file.getAbsolutePath() + ".tmp");
        }

        public void run() {
            try {
                GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(temp));
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                byte[] b = new byte[1024];
                int len = 0;

                while ((len = in.read(b, 0, 1024)) != -1) {
                    zip.write(b, 0, len);
                }

                zip.close();
                in.close();
                file.delete();
                temp.renameTo(file);
            } catch (Exception e) {
                System.err.println(e.toString());
            }
        }
    }
}
