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

package helma.framework.core;

import helma.objectmodel.INode;
import helma.util.CronJob;
import helma.util.SystemMap;
import helma.util.WrappedMap;
import java.io.File;
import java.io.Serializable;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ApplicationBean implements Serializable {
    Application app;

    WrappedMap properties = null;

    /**
     * Creates a new ApplicationBean object.
     *
     * @param app ...
     */
    public ApplicationBean(Application app) {
        this.app = app;
    }

    /**
     *
     */
    public void clearCache() {
        app.clearCache();
    }

    /**
     *
     *
     * @param msg ...
     */
    public void log(Object msg) {
        String str = (msg == null) ? "null" : msg.toString();

        app.logEvent(str);
    }

    /**
     *
     *
     * @param logname ...
     * @param msg ...
     */
    public void log(String logname, Object msg) {
        String str = (msg == null) ? "null" : msg.toString();

        app.getLogger(logname).info(str);
    }

    /**
     *
     *
     * @param msg ...
     */
    public void debug(Object msg) {
        if (app.debug()) {
            String str = (msg == null) ? "null" : msg.toString();

            app.logEvent(str);
        }
    }

    /**
     *
     *
     * @param logname ...
     * @param msg ...
     */
    public void debug(String logname, Object msg) {
        if (app.debug()) {
            String str = (msg == null) ? "null" : msg.toString();

            app.getLogger(logname).info(str);
        }
    }

    /**
     *
     *
     * @return ...
     */
    public int countSessions() {
        return app.sessions.size();
    }

    /**
     *
     *
     * @param sessionID ...
     *
     * @return ...
     */
    public SessionBean getSession(String sessionID) {
        if (sessionID == null) {
            return null;
        }

        Session session = app.getSession(sessionID.trim());

        if (session == null) {
            return null;
        }

        return new SessionBean(session);
    }

    /**
     *
     *
     * @param sessionID ...
     *
     * @return ...
     */
    public SessionBean createSession(String sessionID) {
        if (sessionID == null) {
            return null;
        }

        Session session = app.checkSession(sessionID.trim());

        if (session == null) {
            return null;
        }

        return new SessionBean(session);
    }

    /**
     *
     *
     * @return ...
     */
    public SessionBean[] getSessions() {
        SessionBean[] theArray = new SessionBean[app.sessions.size()];
        int i = 0;

        for (Enumeration e = app.sessions.elements(); e.hasMoreElements();) {
            SessionBean sb = new SessionBean((Session) e.nextElement());

            theArray[i++] = sb;
        }

        return theArray;
    }

    /**
     *
     *
     * @param username ...
     * @param password ...
     *
     * @return ...
     */
    public INode registerUser(String username, String password) {
        if ((username == null) || (password == null) || "".equals(username.trim()) ||
                "".equals(password.trim())) {
            return null;
        } else {
            return app.registerUser(username, password);
        }
    }

    /**
     *
     *
     * @param username ...
     *
     * @return ...
     */
    public INode getUser(String username) {
        if ((username == null) || "".equals(username.trim())) {
            return null;
        }

        return app.getUserNode(username);
    }

    /**
     *
     *
     * @return ...
     */
    public INode[] getActiveUsers() {
        List activeUsers = app.getActiveUsers();

        return (INode[]) activeUsers.toArray(new INode[0]);
    }

    /**
     *
     *
     * @return ...
     */
    public INode[] getRegisteredUsers() {
        List registeredUsers = app.getRegisteredUsers();

        return (INode[]) registeredUsers.toArray(new INode[0]);
    }

    /**
     *
     *
     * @param usernode ...
     *
     * @return ...
     */
    public SessionBean[] getSessionsForUser(INode usernode) {
        if (usernode == null) {
            return new SessionBean[0];
        } else {
            return getSessionsForUser(usernode.getName());
        }
    }

    /**
     *
     *
     * @param username ...
     *
     * @return ...
     */
    public SessionBean[] getSessionsForUser(String username) {
        if ((username == null) || "".equals(username.trim())) {
            return new SessionBean[0];
        }

        List userSessions = app.getSessionsForUsername(username);

        return (SessionBean[]) userSessions.toArray(new SessionBean[0]);
    }

    /**
     *
     *
     * @param functionName ...
     */
    public void addCronJob(String functionName) {
        CronJob job = new CronJob(functionName);

        job.setFunction(functionName);
        app.customCronJobs.put(functionName, job);
    }

    /**
     *
     *
     * @param functionName ...
     * @param year ...
     * @param month ...
     * @param day ...
     * @param weekday ...
     * @param hour ...
     * @param minute ...
     */
    public void addCronJob(String functionName, String year, String month, String day,
                           String weekday, String hour, String minute) {
        CronJob job = CronJob.newJob(functionName, year, month, day, weekday, hour, minute);

        app.customCronJobs.put(functionName, job);
    }

    /**
     *
     *
     * @param functionName ...
     */
    public void removeCronJob(String functionName) {
        app.customCronJobs.remove(functionName);
    }

    // getter methods for readonly properties of this application
    public int getCacheusage() {
        return app.getCacheUsage();
    }

    /**
     *
     *
     * @return ...
     */
    public INode getData() {
        return app.getCacheNode();
    }

    /**
     *
     *
     * @return ...
     */
    public Map getModules() {
        return app.modules;
    }

    /**
     *
     *
     * @return ...
     */
    public String getDir() {
        return app.getAppDir().getAbsolutePath();
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return app.getName();
    }

    /**
     *
     *
     * @return ...
     */
    public Date getUpSince() {
        return new Date(app.starttime);
    }

    /**
     *
     *
     * @return ...
     */
    public long getRequestCount() {
        return app.getRequestCount();
    }

    /**
     *
     *
     * @return ...
     */
    public long getXmlrpcCount() {
        return app.getXmlrpcCount();
    }

    /**
     *
     *
     * @return ...
     */
    public long getErrorCount() {
        return app.getErrorCount();
    }

    /**
     *
     *
     * @return ...
     */
    public Application get__app__() {
        return app;
    }

    /**
     *
     *
     * @return ...
     */
    public Map getProperties() {
        if (properties == null) {
            properties = new WrappedMap(app.getProperties());
            properties.setReadonly(true);
        }
        return properties;
    }

    /**
     *
     *
     * @return ...
     */
    public int getFreeThreads() {
        return app.countFreeEvaluators();
    }

    /**
     *
     *
     * @return ...
     */
    public int getActiveThreads() {
        return app.countActiveEvaluators();
    }

    /**
     *
     *
     * @return ...
     */
    public int getMaxThreads() {
        return app.countEvaluators();
    }

    /**
     *
     *
     * @param n ...
     */
    public void setMaxThreads(int n) {
        // add one to the number to compensate for the internal scheduler.
        app.setNumberOfEvaluators(n + 1);
    }

    /**
     *
     *
     * @return ...
     */
    public Map getSkinfiles() {
        Map skinz = new SystemMap();

        for (Iterator it = app.getPrototypes().iterator(); it.hasNext();) {
            Prototype p = (Prototype) it.next();

            Object skinmap = p.getSkinMap();
            skinz.put(p.getName(), skinmap);
            skinz.put(p.getLowerCaseName(), skinmap);
        }

        return skinz;
    }

    /**
     *
     *
     * @param skinpath ...
     *
     * @return ...
     */
    public Map getSkinfilesInPath(Object[] skinpath) {
        Map skinz = new SystemMap();

        for (Iterator it = app.getPrototypes().iterator(); it.hasNext();) {
            Prototype p = (Prototype) it.next();

            Object skinmap = p.getSkinMap(skinpath);
            skinz.put(p.getName(), skinmap);
            skinz.put(p.getLowerCaseName(), skinmap);
        }

        return skinz;
    }

    /**
     *
     *
     * @return ...
     */
    public String getAppDir() {
        return app.getAppDir().getAbsolutePath();
    }

    /**
     *
     *
     * @return ...
     */
    public String getServerDir() {
        File f = app.getServerDir();

        if (f == null) {
            return app.getAppDir().getAbsolutePath();
        }

        return f.getAbsolutePath();
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return "[Application " + app.getName() + "]";
    }
}
