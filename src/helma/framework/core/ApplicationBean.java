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
import helma.framework.repository.*;
import helma.framework.FutureResult;
import helma.main.Server;

import java.io.File;
import java.io.Serializable;
import java.io.IOException;
import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Application bean that provides a handle to the scripting environment to
 * application specific functionality.
 */
public class ApplicationBean implements Serializable {
    transient Application app;
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
    public synchronized void addRepository(Object obj) {
        Resource current = app.getCurrentCodeResource();
        Repository parent = current == null ?
                null : current.getRepository().getRootRepository();
        Repository rep;
        if (obj instanceof String) {
            String path = (String) obj;
            File file = findResource(null, path);
            if (!file.exists()) {
                file = findResource(app.hopHome, path);
            }
            if (!file.exists()) {
                throw new RuntimeException("Repository path does not exist: " + file);
            }
            if (file.isDirectory()) {
                rep = new FileRepository(file, parent);
            } else if (file.isFile()) {
                if (file.getName().endsWith(".zip")) {
                    rep = new ZipRepository(file, parent);
                } else {
                    rep = new SingleFileRepository(file, parent);
                }
            } else {
                throw new RuntimeException("Unsupported file type in addRepository: " + file);
            }
        } else if (obj instanceof Repository) {
            rep = (Repository) obj;
        } else {
            throw new RuntimeException("Invalid argument to addRepository: " + obj);
        }
        app.addRepository(rep);
        try {
            app.typemgr.checkRepository(rep, true);
        } catch (IOException iox) {
            getLogger().error("Error checking repository " + rep, iox);
        }
    }

    /**
     * Helper method to resolve a repository path.
     * @param parent the parent file
     * @param path the repository path
     * @return our best guess of what the file may be
     */
    private File findResource(File parent, String path) {
        File file = new File(parent, path).getAbsoluteFile();
        if (!file.exists()) {
            file = new File(parent, path + ".zip").getAbsoluteFile();
        }
        if (!file.exists()) {
            file = new File(parent, path + ".js").getAbsoluteFile();
        }
        return file;
    }

    /**
     * Get the app's classloader
     * @return the app's classloader
     */
    public ClassLoader getClassLoader() {
        return app.getClassLoader();
    }

    /**
     * Return the number of currently active sessions
     * @return the current number of active sessions
     */
    public int countSessions() {
        return app.countSessions();
    }

    /**
     * Get a session object for the specified session id
     * @param sessionID the session id
     * @return the session belonging to the session id, or null
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
     * Create a new session with the given session id
     * @param sessionID the session id
     * @return the newly created session
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
     * Get an array of all active sessions
     * @return an array of session beans
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
     * Register a user with the given name and password using the
     * database mapping of the User prototype
     * @param username the user name
     * @param password the user password
     * @return the newly registered user, or null if we failed
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
     * Get a user object with the given name
     * @param username the user name
     * @return the user object, or null
     */
    public INode getUser(String username) {
        if ((username == null) || "".equals(username.trim())) {
            return null;
        }

        return app.getUserNode(username);
    }

    /**
     * Get an array of currently active registered users
     * @return an array of user nodes
     */
    public INode[] getActiveUsers() {
        List activeUsers = app.getActiveUsers();

        return (INode[]) activeUsers.toArray(new INode[0]);
    }

    /**
     * Get an array of all registered users
     * @return an array containing all registered users
     */
    public INode[] getRegisteredUsers() {
        List registeredUsers = app.getRegisteredUsers();

        return (INode[]) registeredUsers.toArray(new INode[0]);
    }

    /**
     * Get an array of all currently active sessions for a given user node
     * @param usernode the user node
     * @return an array of sessions for the given user
     */
    public SessionBean[] getSessionsForUser(INode usernode) {
        if (usernode == null) {
            return new SessionBean[0];
        } else {
            return getSessionsForUser(usernode.getName());
        }
    }

    /**
     * Get an array of all currently active sessions for a given user name
     * @param username the user node
     * @return an array of sessions for the given user
     */
    public SessionBean[] getSessionsForUser(String username) {
        if ((username == null) || "".equals(username.trim())) {
            return new SessionBean[0];
        }

        List userSessions = app.getSessionsForUsername(username);

        return (SessionBean[]) userSessions.toArray(new SessionBean[0]);
    }

    /**
     * Add a cron job that will run once a minute
     * @param functionName the function name
     */
    public void addCronJob(String functionName) {
        CronJob job = new CronJob(functionName);

        job.setFunction(functionName);
        app.customCronJobs.put(functionName, job);
    }

    /**
     * Add a cron job that will run at the specified time intervals
     *
     * @param functionName the function name
     * @param year comma separated list of years, or *
     * @param month comma separated list of months, or *
     * @param day comma separated list of days, or *
     * @param weekday comma separated list of weekdays, or *
     * @param hour comma separated list of hours, or *
     * @param minute comma separated list of minutes, or *
     */
    public void addCronJob(String functionName, String year, String month, String day,
                           String weekday, String hour, String minute) {
        CronJob job = CronJob.newJob(functionName, year, month, day, weekday, hour, minute);

        app.customCronJobs.put(functionName, job);
    }

    /**
     * Unregister a previously registered cron job
     * @param functionName the function name
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
     * Get a wrapper around the app's apps.properties
     *
     * @return a readonly wrapper around the application's apps.properties
     */
    public Map getAppsProperties() {
        Server server = Server.getServer();
        if (server == null)
            return new SystemMap();
        return new WrappedMap(server.getAppsProperties(app.getName()), true);
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
     * Get the number of currently available threads/request evaluators
     * @return the currently available threads
     */
    public int getFreeThreads() {
        return app.countFreeEvaluators();
    }

    /**
     * Get the number of currently active request threads
     * @return the number of currently active threads
     */
    public int getActiveThreads() {
        return app.countActiveEvaluators();
    }

    /**
     * Get the maximal thread number for this application
     * @return the maximal number of threads/request evaluators
     */
    public int getMaxThreads() {
        return app.countEvaluators();
    }

    /**
     * Set the maximal thread number for this application
     * @param n the maximal number of threads/request evaluators
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
     * Return a map of skin resources
     *
     * @return a map containing the skin resources
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
     * Return a map of skin resources including the app-specific skinpath
     *
     * @param skinpath an array of directory paths or HopObjects to search for skins
     * @return a map containing the skin resources
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
     * Return the absolute application directory (appdir property
     * in apps.properties file)
     * @return the app directory as absolute path
     */
    public String getAppDir() {
        return app.getAppDir().getAbsolutePath();
    }

    /**
     * Return the absolute server directory
     * @return the server directory as absolute path
     */
    public String getServerDir() {
        File f = app.getServerDir();

        if (f == null) {
            return app.getAppDir().getAbsolutePath();
        }

        return f.getAbsolutePath();
    }

    /**
     * Return the app's default charset/encoding.
     * @return the app's charset
     */
    public String getCharset() {
        return app.getCharset();
    }

    /**
     * Set the path for global macro resolution
     * @param path an array of global namespaces, or null
     */
    public void setGlobalMacroPath(String[] path) {
        app.globalMacroPath = path;
    }

    /**
     * Get the path for global macro resolution
     * @return an array of global namespaces, or null
     */
    public String[] getGlobalMacroPath() {
        return app.globalMacroPath;
    }

    /**
     * Trigger a synchronous Helma invocation with a default timeout of 30 seconds.
     *
     * @param thisObject the object to invoke the function on,
     * or null for global invokation
     * @param function the function or function name to invoke
     * @param args an array of arguments
     * @return the value returned by the function
     * @throws Exception exception thrown by the function
     */
    public Object invoke(Object thisObject, Object function, Object[] args)
            throws Exception {
        // default timeout of 30 seconds
        return invoke(thisObject, function, args, 30000L);
    }

    /**
     * Trigger a synchronous Helma invocation.
     *
     * @param thisObject the object to invoke the function on,
     * or null for global invokation
     * @param function the function or function name to invoke
     * @param args an array of arguments
     * @param timeout the timeout in milliseconds. After waiting
     * this long, we will try to interrupt the invocation
     * @return the value returned by the function
     * @throws Exception exception thrown by the function
     */
    public Object invoke(Object thisObject, Object function,
                         Object[] args, long timeout)
            throws Exception {
        RequestEvaluator reval = app.getEvaluator();
        try {
            return reval.invokeInternal(thisObject, function, args, timeout);
        } finally {
            app.releaseEvaluator(reval);
        }
    }

    /**
     * Trigger an asynchronous Helma invocation. This method returns
     * immedately with an object that allows to track the result of the
     * function invocation with the following properties:
     *
     * <ul>
     * <li>running - true while the function is running, false afterwards</li>
     * <li>result - the value returned by the function, if any</li>
     * <li>exception - the exception thrown by the function, if any</li>
     * <li>waitForResult() - wait indefinitely until invocation terminates
     * and return the result</li>
     * <li>waitForResult(t) - wait for the specified number of milliseconds
     * for invocation to terminate and return the result</li>
     * </ul>
     *
     * @param thisObject the object to invoke the function on,
     * or null for global invokation
     * @param function the function or function name to invoke
     * @param args an array of arguments
     * this long, we will try to interrupt the invocation
     * @return an object with the properties described above
     */
    public FutureResult invokeAsync(Object thisObject,
                              final Object function,
                              final Object[] args) {
        // default timeout of 15 minutes
        return new AsyncInvoker(thisObject, function, args, 60000L * 15);
    }

    /**
     * Trigger an asynchronous Helma invocation. This method returns
     * immedately with an object that allows to track the result of the
     * function invocation with the following methods and properties:
     *
     * <ul>
     * <li>running - true while the function is running, false afterwards</li>
     * <li>result - the value returned by the function, if any</li>
     * <li>exception - the exception thrown by the function, if any</li>
     * <li>waitForResult() - wait indefinitely until invocation terminates
     * and return the result</li>
     * <li>waitForResult(t) - wait for the specified number of milliseconds
     * for invocation to terminate and return the result</li>
     * </ul>
     *
     * @param thisObject the object to invoke the function on,
     * or null for global invokation
     * @param function the function or function name to invoke
     * @param args an array of arguments
     * @param timeout the timeout in milliseconds. After waiting
     * this long, we will try to interrupt the invocation
     * @return an object with the properties described above
     */
    public FutureResult invokeAsync(Object thisObject, Object function,
                              Object[] args, long timeout) {
        return new AsyncInvoker(thisObject, function, args, timeout);
    }

    /**
     * Return a string presentation of this AppBean
     * @return string description of this app bean object
     */
    public String toString() {
        return "[Application " + app.getName() + "]";
    }

    class AsyncInvoker extends Thread implements FutureResult {

        private Object thisObject;
        private Object function;
        private Object[] args;
        private long timeout;

        private Object result;
        private Exception exception;
        private boolean running = true;

        private AsyncInvoker(Object thisObj, Object func, Object[] args, long timeout) {
            thisObject = thisObj;
            function = func;
            this.args = args;
            this.timeout = timeout;
            start();
        }

        public void run() {
            RequestEvaluator reval = null;
            try {
                reval = app.getEvaluator();
                setResult(reval.invokeInternal(thisObject, function, args, timeout));
            } catch (Exception x) {
                setException(x);
            } finally {
                running = false;
                app.releaseEvaluator(reval);
            }
        }

        public synchronized boolean getRunning() {
            return running;
        }

        private synchronized void setResult(Object obj) {
            result = obj;
            running = false;
            notifyAll();
        }

        public synchronized Object getResult() {
            return result;
        }

        public synchronized Object waitForResult() throws InterruptedException {
            if (!running)
                return result;
            wait();
            return result;
        }

        public synchronized Object waitForResult(long timeout)
                throws InterruptedException {
            if (!running)
                return result;
            wait(timeout);
            return result;
        }

        private synchronized void setException(Exception x) {
            exception = x;
            running = false;
            notifyAll();
        }

        public synchronized Exception getException() {
            return exception;
        }

        public String toString() {
            return new StringBuffer("AsyncInvokeThread{running: ").append(running)
                    .append(", result: ").append(result).append(", exception: ")
                    .append(exception).append("}").toString();
        }

    }
}
