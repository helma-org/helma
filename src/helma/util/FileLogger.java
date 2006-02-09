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

import org.apache.commons.logging.Log;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * An extended Logger that writes to a file and rotates files each midnight.
 *
 * @author Stefan Pollach
 * @author Daniel Ruthardt
 * @author Hannes Wallnoefer
 */
public class FileLogger extends Logger implements Log {

    // fields used for logging to files
    private String name;
    private File logdir;
    private File logfile;

    // number format for log file rotation
    DecimalFormat nformat = new DecimalFormat("000");
    DateFormat aformat = new SimpleDateFormat("yyyy-MM-dd");

    // timestamp of last log message
    long lastMessage;

    /**
     * Create a file logger. The actual file names do have numbers appended and are
     * rotated every x bytes.
     */
    protected FileLogger(String directory, String name) {
        this.name = name;
        logdir = new File(directory);
        // make logdir have an absolute path in case it doesn't already
        if (!logdir.isAbsolute())
            logdir = logdir.getAbsoluteFile();
        logfile = new File(logdir, name + ".log");

        if (!logdir.exists()) {
            logdir.mkdirs();
        }

        openFile();

        // create a synchronized list for log entries since different threads may
        // attempt to modify the list at the same time
        entries = Collections.synchronizedList(new LinkedList());

        lastMessage = System.currentTimeMillis();
    }

    /**
     * Open the file and get a writer to it. This will either rotate the log files
     * or it will return a writer that appends to an existing file.
     */
    private synchronized void openFile() {
        try {
            if (logfile.exists() && (logfile.lastModified() < lastMidnight())) {
                // rotate if a log file exists and is NOT from today
                File archive = rotateLogFile();
                // gzip rotated log file in a separate thread
                if (archive != null) {
                    new GZipper(archive).start();
                }
            }
            // create a new log file, appending to an existing file
            writer = new PrintWriter(new FileWriter(logfile.getAbsolutePath(), true),
                                     false);
        } catch (IOException iox) {
            System.err.println("Error creating log " + name + ": " + iox);
        }
    }

    /**
     * Actually closes the file writer of a log.
     */
    synchronized void closeFile() {
        if (writer != null) {
            try {
                writer.close();
            } catch (Exception ignore) {
                // ignore
            } finally {
                writer = null;
            }
        }
    }

    /**
     * This is called by the runner thread to perform actual output.
     */
    synchronized void write() {
        if (entries.isEmpty()) {
            return;
        }

        try {
            int l = entries.size();

            if (writer == null || !logfile.exists()) {
                // open/create the log file if necessary
                openFile();
            }

            for (int i = 0; i < l; i++) {
                String entry = (String) entries.remove(0);
                writer.println(entry);
            }

            writer.flush();
        } catch (Exception x) {
            int size = entries.size();

            if (size > 1000) {
                // more than 1000 entries queued plus exception - something
                // is definitely wrong with this logger. Write a message to std err and
                // discard queued log entries.
                System.err.println("Error writing log file " + this + ": " + x);
                System.err.println("Discarding " + size + " log entries.");
                entries.clear();
            }
        }
    }


    /**
     *  Rotate log files, closing the file writer and renaming the old
     *  log file. Returns the renamed log file for zipping, or null if
     *  the log file couldn't be rotated.
     *
     *  @return the old renamed log file, or null
     */
    protected synchronized File rotateLogFile() throws IOException {
        // if the logger is not file based do nothing.
        if (logfile == null) {
            return null;
        }

        closeFile();

        // only backup/rotate if the log file is not empty,
        if (logfile.exists() && (logfile.length() > 0)) {
            String today = aformat.format(new Date());
            int ct = 0;

            // first append just the date
            String archname = name + "-" + today + ".log";
            File archive = new File(logdir, archname);
            File zipped = new File(logdir, archname + ".gz");

            // increase counter until we find an unused log archive name, checking
            // both unzipped and zipped file names
            while (archive.exists() || zipped.exists()) {
                // for the next try we append a counter
                String archidx = (ct > 999) ? Integer.toString(ct) : nformat.format(++ct);

                archname = name + "-" + today + "-" + archidx + ".log";
                archive = new File(logdir, archname);
                zipped = new File(logdir, archname + ".gz");
            }

            if (logfile.renameTo(archive)) {
                return archive;
            } else {
                System.err.println("Error rotating log file " + canonicalName +
                        ". Will append to old file.");
            }
        }

        // no log file rotated
        return null;
    }

    /**
     * Return a string representation of this Logger
     */
    public String toString() {
        return "FileLogger[" + name + "]";
    }

    /**
     *  Return an object  which identifies  this logger.
     */
    public String getName() {
        return name;
    }


    /**
     * Append a message to the log.
     */
    public void log(String msg) {
        lastMessage = System.currentTimeMillis();

        // it's enough to render the date every second
        if ((lastMessage - 1000) > dateLastRendered) {
            renderDate();
        }

        entries.add(dateCache + msg);
    }

    /**
     *
     *
     * @return the timestamp for last midnight in millis
     */
    private static long lastMidnight() {
        return Logging.nextMidnight() - 86400000;
    }

    /**
     * a Thread class that zips up a file, filename will stay the same.
     */
    static class GZipper extends Thread {
        List files;
        final static int BUFFER_SIZE = 8192;

        public GZipper(List files) {
            this.files = files;
            setPriority(MIN_PRIORITY);
        }

        public GZipper(File file) {
            files = new ArrayList(1);
            files.add(file);
            setPriority(MIN_PRIORITY);
        }

        public void run() {
            Iterator it = files.iterator();
            File file = null;

            while (it.hasNext()) {
                try {
                    file = (File) it.next();
                    File zipped = new File(file.getAbsolutePath() + ".gz");
                    GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(zipped));
                    BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
                    byte[] b = new byte[BUFFER_SIZE];
                    int len;

                    while ((len = in.read(b, 0, BUFFER_SIZE)) != -1) {
                        zip.write(b, 0, len);
                    }

                    zip.close();
                    in.close();
                    file.delete();
                } catch (Exception e) {
                    System.err.println("Error gzipping " + file);
                    System.err.println(e.toString());
                }
            }
        }
    }

}
