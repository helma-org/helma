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
import helma.objectmodel.db.DbSource;
import helma.util.CronJob;
import helma.util.SystemMap;
import helma.util.WrappedMap;
import helma.framework.repository.Repository;
import helma.framework.repository.FileRepository;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
     * Clear the application cache.
     */
    public void clearCache() {
        app.clearCache();
    }

    /**
     * Get the app's event logger. This is a Log with the
     * category helma.[appname].event.
     *
     * @return the app logger.
     */
    public Log getLogger() {
        return  app.getEventLog();
    }

    /**
     * Get the app logger. This is a commons-logging Log with the
     * category <code>logname</code>.
     *
     * @return a logger for the given log name.
     */
    public Log getLogger(String logname) {
        return  LogFactory.getLog(logname);
    }

    /**
     * Log a INFO message to the app log.
     *
     * @param msg the log message
     */
    public void log(Object msg) {
        getLogger().info(msg);
    }

    /**
     * Log a INFO message to the log defined by logname.
     *
     * @param logname the name (category) of the log
     * @param msg the log message
     */
    public void log(String logname, Object msg) {
        getLogger(logname).info(msg);
    }

    /**
     * Log a DEBUG message to the app log if debug is set to true in
     * app.properties.
     *
     * @param msg the log message
     */
    public void debug(Object msg) {
        if (app.debug()) {
            getLogger().debug(msg);
        }
    }

    /**
     * Log a DEBUG message to the log defined by logname
     * if debug is set to true in app.properties.
     *
     * @param logname the name (category) of the log
     * @param msg the log message
     */
    public void debug(String logname, Object msg) {
        if (app.debug()) {
            getLogger(logname).debug(msg);
        }
    }

    /**
     * Returns the app's repository list.
     *
     * @return the an array containing this app's repositories
     */
    public Object[] getRepositories() {
        return app.getRepositories().toArray();
    }

    /**
     * Add a repository to the app's repository list. The .zip extension
     * is automatically added, if the original library path does not
     * point to an existing file or directory.
     *
     * @param obj the repository, relative or absolute path to the library.
     */
    public void addRepository(Object obj) {
        Repository rep;
        if (obj instanceof String) {
            String path = (String) obj;
            File file = new File(path).getAbsoluteFile();
            if (!file.exists()) {
                file = new File(path + ".zip").getAbsoluteFile();
            }
            if (!file.exists()) {
                throw new RuntimeException("Repository path does not exist: " + file);
            }
            rep = new FileRepository(file);
        } else if (obj instanceof Repository) {
            rep = (Repository) obj;
        } else {
            throw new RuntimeException("Invalid argument to addRepository: " + obj);
        }
        app.addRepository(rep);
    }

    /**
     * Get the app's classloader
     * @return the app's classloader
     */
    public ClassLoader getClassLoader() {
        return app.getClassLoader();
    }

    /**
     *
     *
     * @return ...
     */
    public int countSessions() {
        return app.countSessions();
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

        Session session = app.createSession(sessionID.trim());

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
        Map sessions = app.getSessions();
        SessionBean[] array = new SessionBean[sessions.size()];
        int i = 0;

        Iterator it = sessions.values().iterator();
        while (it.hasNext()) {
            array[i++] = new SessionBean((Session) it.next());
        }

        return array;
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

    /**
     * Returns an read-only map of the custom cron jobs registered with the app
     *
     * @return a map of cron jobs
     */
    public Map getCronJobs() {
        return new WrappedMap(app.customCronJobs, true);
    }

    /**
     * Returns the number of elements in the NodeManager's cache
     */
    public int getCacheusage() {
        return app.getCacheUsage();
    }

    /**
     * Returns the app's data node used to share data between the app's evaluators
     *
     * @return the app.data node
     */
    public INode getData() {
        return app.getCacheNode();
    }

    /**
     * Returns the app's modules map used to register application modules
     *
     * @return the module map
     */
    public Map getModules() {
        return app.modules;
    }

    /**
     * Returns the absolute path of the app dir. When using repositories this
     * equals the first file based repository.
     *
     * @return the app dir
     */
    public String getDir() {
        return app.getAppDir().getAbsolutePath();
    }

    /**
     * @return the app name
     */
    public String getName() {
        return app.getName();
    }

    /**
     * @return the application start time
     */
    public Date getUpSince() {
        return new Date(app.starttime);
    }

    /**
     * @return the number of requests processed by this app
     */
    public long getRequestCount() {
        return app.getRequestCount();
    }

    /**
     * @return the number of XML-RPC requests processed
     */
    public long getXmlrpcCount() {
        return app.getXmlrpcCount();
    }

    /**
     * @return the number of errors encountered
     */
    public long getErrorCount() {
        return app.getErrorCount();
    }

    /**
     * @return the wrapped helma.framework.core.Application object
     */
    public Application get__app__() {
        return app;
    }

    /**
     * Get a wrapper around the app's properties
     *
     * @return a readonly wrapper around the application's app properties
     */
    public Map getProperties() {
        if (properties == null) {
            properties = new WrappedMap(app.getProperties(), true);
        }
        return properties;
    }

    /**
     * Get a wrapper around the app's db properties
     *
     * @return a readonly wrapper around the application's db properties
     */
    public Map getDbProperties() {
        return new WrappedMap(app.getDbProperties(), true);
    }

    /**
     * Return a DbSource object for a given name.
     */
    public DbSource getDbSource(String name) {
        return app.getDbSource(name);
    }

    /**
     * Get an array of this app's prototypes
     *
     * @return an array containing the app's prototypes
     */
    public Prototype[] getPrototypes() {
        return (Prototype[]) app.getPrototypes().toArray(new Prototype[0]);
    }

    /**
     * Get a prototype by name.
     *
     * @param name the prototype name
     * @return the prototype
     */
    public Prototype getPrototype(String name) {
        return app.getPrototypeByName(name);
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
     *  Return a skin for a given object. The skin is found by determining the prototype
     *  to use for the object, then looking up the skin for the prototype.
     */
    public Skin getSkin(String protoname, String skinname, Object[] skinpath) {
        try {
            return app.getSkin(protoname, skinname, skinpath);
        } catch (Exception x) {
            return null;
        }
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

            Object skinmap = p.getScriptableSkinMap();
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

            Object skinmap = p.getScriptableSkinMap(skinpath);
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
