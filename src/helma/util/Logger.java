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

    // buffer for log items
    List entries;

    // Writer for log output
    PrintWriter writer;

    // the canonical name for this logger
    String canonicalName;

    // fields for date rendering and caching
    static DateFormat dformat = new SimpleDateFormat("[yyyy/MM/dd HH:mm:ss] ");
    static long dateLastRendered;
    static String dateCache;

    /**
     * zero argument constructor, only here for FileLogger subclass
     */
    Logger() {}

    /**
     * Create a logger for a PrintStream, such as System.out.
     */
    protected Logger(PrintStream out) {
        writer = new PrintWriter(out);
        canonicalName = out.toString();

        // create a synchronized list for log entries since different threads may
        // attempt to modify the list at the same time
        entries = Collections.synchronizedList(new LinkedList());
    }

    /**
     * Return a string representation of this Logger
     */
    public String toString() {
        return "Logger[" + canonicalName + "]";
    }

    /**
     *  Return an object  which identifies  this logger.
     */
    public String getCanonicalName() {
        return canonicalName;
    }

    /**
     * Append a message to the log.
     */
    public void log(String msg) {
        // it's enough to render the date every second
        if ((System.currentTimeMillis() - 1000) > dateLastRendered) {
            renderDate();
        }

        entries.add(dateCache + msg);
    }

    /**
     * This is called by the runner thread to perform actual output.
     */
    void write() {
        if (entries.isEmpty()) {
            return;
        }

        try {
            int l = entries.size();

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

    // Pre-render the date to use for log messages. Called every 2 seconds or so.
    static synchronized void renderDate() {
        dateLastRendered = System.currentTimeMillis();
        dateCache = dformat.format(new Date());
    }

    // methods to implement org.apache.commons.logging.Log interface

    public boolean isDebugEnabled() {
        return true;
    }

    public boolean isErrorEnabled() {
        return true;
    }

    public boolean isFatalEnabled() {
        return true;
    }

    public boolean isInfoEnabled() {
        return true;
    }

    public boolean isTraceEnabled() {
        return true;
    }

    public boolean isWarnEnabled() {
        return true;
    }

    public void trace(Object parm1) {
        info(parm1);
    }

    public void trace(Object parm1, Throwable parm2) {
        info(parm1, parm2);
    }

    public void debug(Object parm1) {
        info(parm1);
    }

    public void debug(Object parm1, Throwable parm2) {
        info(parm1, parm2);
    }

    public void info(Object parm1) {
        log(parm1.toString());
    }

    public void info(Object parm1, Throwable parm2) {
        log(parm1.toString() + "\n" + parm2.getStackTrace().toString());
    }

    public void warn(Object parm1) {
        info(parm1);
    }

    public void warn(Object parm1, Throwable parm2) {
        info(parm1, parm2);
    }

    public void error(Object parm1) {
        info(parm1);
    }

    public void error(Object parm1, Throwable parm2) {
        info(parm1, parm2);
    }

    public void fatal(Object parm1) {
        info(parm1);
    }

    public void fatal(Object parm1, Throwable parm2) {
        info(parm1, parm2);
    }

}
