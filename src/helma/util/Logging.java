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

import org.apache.commons.logging.*;
import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 *  Implementation of Jakarta Commons LogFactory that supports both
 *  simple console logging and logging to files that are rotated and
 *  gzipped each night.
 *
 * @author Stefan Pollach
 * @author Daniel Ruthardt
 * @author Hannes Wallnoefer
 */
public class Logging extends LogFactory {

    // we use one static thread for all Loggers
    static Runner runner;

    // the list of active loggers
    static ArrayList loggers = new ArrayList();

    // hash map of loggers
    static HashMap loggerMap = new HashMap();

    // log directory
    String logdir;

    // static console logger
    static Logger consoleLog = new Logger(System.out);

    /**
     *  Constructs a log factory, getting the base logging directory from the
     *  helma.logdir system property.
     */
    public Logging() {
        logdir = System.getProperty("helma.logdir", "log");
    }

    /**
     * Get a logger for a file name. The log file is created in the
     * directory specified by the "log.dir" System property. If the
     * logname is "console" a log that writes to System.out is returned.
     */
    public Log getInstance(String logname) {
        if (logname == null) {
            throw new LogConfigurationException("No logname specified!");
        }

        if ("console".equals(logdir)) {
            return getConsoleLog();
        } else {
            return getFileLog(logname);
        }
    }

    /**
     * Get a logger to System.out.
     */
    public static Log getConsoleLog() {
        ensureRunning();
        return consoleLog;
    }


    /**
     *  Get a file logger, creating it if it doesn't exist yet.
     */
    public synchronized Logger getFileLog(String logname) {
        Logger log = (Logger) loggerMap.get(logname);

        if (log == null) {
            log = new FileLogger(logdir, logname);
            loggerMap.put(logname, log);
            loggers.add(log);
        }

        ensureRunning();
        return log;
    }

    public synchronized Log getInstance (Class clazz) {
        return getInstance(clazz.getPackage().getName());
    }

    public void setAttribute(String name, Object value) {
        if ("logdir".equals(name)) {
            // FIXME: make log dir changable at runtime
        }
    }

    public Object getAttribute(String name) {
        if ("logdir".equals(name)) {
            return logdir;
        }
        return null;
    }

    public String[] getAttributeNames() {
        return new String[] {};
    }

    public void removeAttribute(String parm1) {
        // nothing to do
    }

    /**
     * Flush all logs and shut down.
     */
    public void release() {
        shutdown();
    }

    /**
     * Make sure logger thread is active.
     */
    public synchronized static void ensureRunning() {
        if ((runner == null) || !runner.isAlive()) {
            runner = new Runner();
            runner.setDaemon(true);
            runner.start();
        }
    }

    /**
     * Shut down logging, stopping the logger thread and closing all logs.
     */
    public synchronized static void shutdown() {
        if (runner != null && runner.isAlive()) {
            runner.interrupt();
        }
        runner = null;
        Thread.yield();
        closeAll();
    }

    /**
     * Close all open logs.
     */
    static void closeAll() {

        consoleLog.write();

        int nloggers = loggers.size();

        for (int i = nloggers - 1; i >= 0; i--) {
            FileLogger log = (FileLogger) loggers.get(i);

            log.write();
            log.closeFile();
        }

        loggers.clear();
        loggerMap.clear();
        consoleLog = null;
    }

    static void gzip(File file) {
        final int BUFFER_SIZE = 8192;

        try {
            File zipped = new File(file.getAbsolutePath() + ".gz");
            GZIPOutputStream zip = new GZIPOutputStream(new FileOutputStream(zipped));
            BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
            byte[] b = new byte[BUFFER_SIZE];
            int len = 0;

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


    /**
     * Returns the timestamp for the next Midnight
     *
     * @return next midnight timestamp in milliseconds
     */
    static long nextMidnight() {
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
     *  The static runner class that loops through all loggers.
     */
    static class Runner extends Thread {

        public synchronized void run() {
            long nextMidnight = nextMidnight();

            while ((runner == this) && !isInterrupted()) {

                long now = System.currentTimeMillis();

                if (nextMidnight < now) {
                    new LogRotator().start();
                    nextMidnight = nextMidnight();
                }

                // write the stdout console log
                consoleLog.write();

                int nloggers = loggers.size();

                for (int i = nloggers-1; i >= 0; i--) {
                    try {
                        FileLogger log = (FileLogger) loggers.get(i);

                        // write out the log entries
                        log.write();

                        // if log hasn't been used in the last 30 minutes, close it
                        if (now - log.lastMessage > 1800000) {
                            log.closeFile();
                        }
                    } catch (Exception x) {
                        System.err.println("Error in Logger main loop: " + x);
                    }
                }

                try {
                    wait(250);
                } catch (InterruptedException ix) {
                    break;
                }
            }
        }

    }

    /**
     * Log rotator thread calls rotateLogFiles on all
     */
    static class LogRotator extends Thread {

        public void run() {

            FileLogger[] logs = (FileLogger[]) loggers.toArray(new FileLogger[0]);

            ArrayList archives = new ArrayList();

            for (int i = 0; i < logs.length; i++) {
                try {
                    File archive = logs[i].rotateLogFile();
                    if (archive != null) {
                        archives.add(archive);
                    }
                } catch (IOException io) {
                    System.err.println("Error rotating log " + logs[i].getName() + ": " +
                                        io.toString());
                }
            }

            // reduce thread priority for zipping
            setPriority(MIN_PRIORITY);
            Iterator it = archives.iterator();
            while (it.hasNext()) {
                gzip((File) it.next());
            }

        }

    }

    /**
     * Utility thread class to gzip a file in a separate thread
     */
    static class FileGZipper extends Thread {

        File file;

        FileGZipper(File file) {
            this.file = file;
        }

        public void run() {
            // reduce thread priority for zipping
            setPriority(MIN_PRIORITY);
            gzip(file);
        }
    }

}

