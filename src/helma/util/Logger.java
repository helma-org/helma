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

/**
 * A simple logger that writes to a PrintStream such as System.out.
 */
public class Logger implements Log {

    // buffer for log items; create a synchronized list for log entries since
    // different threads may attempt to modify the list at the same time
    List entries = Collections.synchronizedList(new LinkedList());

    // Writer for log output
    PrintWriter writer;

    // the canonical name for this logger
    String canonicalName;

    // fields for date rendering and caching
    static DateFormat dformat = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss] ");
    static long dateLastRendered;
    static String dateCache;

    public final static int TRACE = 1;
    public final static int DEBUG = 2;
    public final static int INFO =  3;
    public final static int WARN =  4;
    public final static int ERROR = 5;
    public final static int FATAL = 6;
    
    int logLevel = INFO;    

    // timestamp of last log message, used to close file loggers after longer
    // periods of inactivity
    long lastMessage = System.currentTimeMillis();

    // sedated log instance for jetty
    private Log sedatedLog = new SedatedLog();

    /**
     * zero argument constructor, only here for FileLogger subclass
     */
    Logger() {
        init();
    }

    /**
     * Create a logger for a PrintStream, such as System.out.
     * @param out the output stream
     */
    protected Logger(PrintStream out) {
        init();
        writer = new PrintWriter(out);
        canonicalName = out.toString();
    }

    /**
     * Get loglevel from System properties
     */
     private void init() {
        String level = System.getProperty("helma.loglevel");
        if ("trace".equalsIgnoreCase(level))
            logLevel = TRACE;
        else if ("debug".equalsIgnoreCase(level))
            logLevel = DEBUG;
        else if ("info".equalsIgnoreCase(level))
            logLevel = INFO;
        else if ("warn".equalsIgnoreCase(level))
            logLevel = WARN;
        else if ("error".equalsIgnoreCase(level))
            logLevel = ERROR;
        else if ("fatal".equalsIgnoreCase(level))
            logLevel = FATAL;
    }

    /**
     * Get the current log level.
     * @return the current log level
     */
    public int getLogLevel() {
        return logLevel;
    }

    /**
     * Set the log level for this logger.
     * @param logLevel the new log level
     */
    public void setLogLevel(int logLevel) {
        this.logLevel = logLevel;
    }

    /**
     * Return a string representation of this Logger
     */
    public String toString() {
        return new StringBuffer(getClass().getName()).append("[")
                .append(canonicalName).append(",").append(logLevel)
                .append("]").toString();
    }

    /**
     * Return an object  which identifies  this logger.
     * @return the canonical name of this logger
     */
    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     * Append a message to the log.
     * @param level a string representing the log level
     * @param msg the log message
     * @param exception an exception, or null
     */
    protected void log(String level, Object msg, Throwable exception) {
        lastMessage = System.currentTimeMillis();
        // it's enough to render the date every second
        if ((lastMessage - 1000) > dateLastRendered) {
            renderDate();
        }
        // add a safety net so we don't grow indefinitely even if writer thread
        // has gone. the 2000 entries threshold is somewhat arbitrary. 
        if (entries.size() < 2000) {
            String message = msg == null ? "null" : msg.toString();
            entries.add(new Entry(dateCache, level, message, exception));
        }
    }

    /**
     * This is called by the runner thread to perform actual output.
     */
    protected synchronized void write() {
        if (entries.isEmpty()) {
            return;
        }

        try {
            // make sure we have a valid writer
            ensureOpen();

            int l = entries.size();
            for (int i = 0; i < l; i++) {
                Entry entry = (Entry) entries.remove(0);
                writer.print(entry.date);
                writer.print(entry.level);
                writer.println(entry.message);
                if (entry.exception != null)
                    entry.exception.printStackTrace(writer);
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
     * This is called by the runner thread to to make sure we have an open writer.
     */
    protected void ensureOpen() {
        // nothing to do for console logger
    }

    // Pre-render the date to use for log messages. Called about once a second.
    protected static synchronized void renderDate() {
        Date date = new Date();
        dateCache = dformat.format(date);
        dateLastRendered = date.getTime();
    }

    // methods to implement org.apache.commons.logging.Log interface

    public boolean isTraceEnabled() {
        return logLevel <= TRACE;
    }

    public boolean isDebugEnabled() {
        return logLevel <= DEBUG;
    }

    public boolean isInfoEnabled() {
        return logLevel <= INFO;
    }

    public boolean isWarnEnabled() {
        return logLevel <= WARN;
    }

    public boolean isErrorEnabled() {
        return logLevel <= ERROR;
    }

    public boolean isFatalEnabled() {
        return logLevel <= FATAL;
    }

    public void trace(Object parm1) {
        if (logLevel <= TRACE)
            log("[TRACE] ", parm1, null);
    }

    public void trace(Object parm1, Throwable parm2) {
        if (logLevel <= TRACE)
            log("[TRACE] ", parm1, parm2);
    }

    public void debug(Object parm1) {
        if (logLevel <= DEBUG)
            log("[DEBUG] ", parm1, null);
    }

    public void debug(Object parm1, Throwable parm2) {
        if (logLevel <= DEBUG)
            log("[DEBUG] ", parm1, parm2);
    }

    public void info(Object parm1) {
        if (logLevel <= INFO)
            log("[INFO] ", parm1, null);
    }

    public void info(Object parm1, Throwable parm2) {
        if (logLevel <= INFO)
            log("[INFO] ", parm1, parm2);
    }

    public void warn(Object parm1) {
        if (logLevel <= WARN)
            log("[WARN] ", parm1, null);
    }

    public void warn(Object parm1, Throwable parm2) {
        if (logLevel <= WARN)
            log("[WARN] ", parm1, parm2);
    }

    public void error(Object parm1) {
        if (logLevel <= ERROR)
            log("[ERROR] ", parm1, null);
    }

    public void error(Object parm1, Throwable parm2) {
        if (logLevel <= ERROR)
            log("[ERROR] ", parm1, parm2);
    }

    public void fatal(Object parm1) {
        if (logLevel <= FATAL)
            log("[FATAL] ", parm1, null);
    }

    public void fatal(Object parm1, Throwable parm2) {
        if (logLevel <= FATAL)
            log("[FATAL] ", parm1, parm2);
    }

    // utility method to get the stack trace from a Throwable as string
    public static String getStackTrace(Throwable t) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        t.printStackTrace(writer);
        writer.close();
        return stringWriter.toString();
    }

    class Entry {
        final String date, level, message;
        final Throwable exception;

        Entry(String date, String level, String message, Throwable exception) {
            this.date = date;
            this.level = level;
            this.message = message;
            this.exception = exception;
        }
    }

    /**
     * return a "quiet" version of this log that routes debug() output to trace()
     * @return a possibly less verbose version of this log.
     */
    protected Log getSedatedLog() {
        return sedatedLog;
    }

    /*
     * A inner class that "calms down" logging output by routing debug() output
     * to trace(). This is useful for software like Jetty, which has really
     * verbose output at DEBUG level (dumps whole HTTP request and response headers).
     * You can enable that output by setting the log level to TRACE.
     */
    class SedatedLog implements Log {

        public void debug(Object o) {
            // Route debug() to trace()
            Logger.this.trace(o);
        }

        public void debug(Object o, Throwable t) {
            // Route debug() to trace()
            Logger.this.trace(o, t);
        }

        public void error(Object o) {
            Logger.this.error(o);
        }

        public void error(Object o, Throwable t) {
            Logger.this.error(o, t);
        }

        public void fatal(Object o) {
            Logger.this.fatal(o);
        }

        public void fatal(Object o, Throwable t) {
            Logger.this.fatal(o, t);
        }

        public void info(Object o) {
            Logger.this.info(o);
        }

        public void info(Object o, Throwable t) {
            Logger.this.info(o, t);
        }

        public void trace(Object o) {
            // swallow trace()
        }

        public void trace(Object o, Throwable t) {
            // swallow trace()
        }

        public void warn(Object o) {
            Logger.this.warn(o);
        }

        public void warn(Object o, Throwable t) {
            Logger.this.warn(o, t);
        }

        public boolean isDebugEnabled() {
            // Route debug() to trace()
            return Logger.this.isTraceEnabled();
        }

        public boolean isErrorEnabled() {
            return Logger.this.isErrorEnabled();
        }

        public boolean isFatalEnabled() {
            return Logger.this.isFatalEnabled();
        }

        public boolean isInfoEnabled() {
            return Logger.this.isInfoEnabled();
        }

        public boolean isTraceEnabled() {
            // swallow trace()
            return false;
        }

        public boolean isWarnEnabled() {
            return Logger.this.isWarnEnabled();
        }

    }

}
