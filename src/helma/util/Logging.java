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
            if (logname.startsWith("org.mortbay."))
                return getConsoleLog().getSedatedLog();
            else
                return getConsoleLog();
        } else {
            if (logname.startsWith("org.mortbay."))
                return getFileLog(logname).getSedatedLog();
            else
                return getFileLog(logname);
        }
    }

    /**
     * Get a logger to System.out.
     * @return a logger that writes to System.out
     */
    public static Logger getConsoleLog() {
        ensureRunning();
        return consoleLog;
    }


    /**
     * Get a file logger, creating it if it doesn't exist yet.
     * @param logname the base name for the file logger
     * @return a file logger
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
        // FIXME: make log dir changeable at runtime
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

    /**
     * Rotate log files on all registered logs
     */
    static void rotateLogs() {
        int nloggers = loggers.size();
        ArrayList files = new ArrayList(nloggers);

        for (int i = nloggers - 1; i >= 0; i--) {
            FileLogger log = (FileLogger) loggers.get(i);

            try {
                File file = log.rotateLogFile();
                if (file != null) {
                    files.add(file);
                }
            } catch (IOException io) {
                System.err.println("Error rotating log " + log.getName() + ": " +
                                    io.toString());
            }
        }

        if (!files.isEmpty()) {
            new FileLogger.GZipper(files).start();
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
     * Returns the timestamp for the last Midnight
     *
     * @return last midnight timestamp in milliseconds
     */
    static long lastMidnight() {
        return nextMidnight() - 86400000;
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
                    rotateLogs();
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
                    wait(333);
                } catch (InterruptedException ix) {
                    break;
                }
            }
        }

    }
}

