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

package helma.main;

import helma.framework.core.AppClassLoader;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;
import java.util.HashSet;

/**
 *  Liberal security manager for Helma system that makes sure application code
 *  is not allowed to exit the VM and set a security manager.
 *
 *  This class can be subclassed to implement actual security policies. It contains
 *  a utility method <code>getApplication</code> that can be used to determine
 *  the name of the application trying to execute the action in question, if any.
 */
public class HelmaSecurityManager extends SecurityManager {
    // The set of actions forbidden to application code.
    // We are pretty permissive, forbidding only System.exit() 
    // and setting the security manager.
    private final static HashSet forbidden = new HashSet();

    static {
        forbidden.add("exitVM");
        forbidden.add("setSecurityManager");
    }

    /**
     *
     *
     * @param p ...
     */
    public void checkPermission(Permission p) {
        if (p instanceof RuntimePermission) {
            if (forbidden.contains(p.getName())) {
                Class[] classes = getClassContext();

                for (int i = 0; i < classes.length; i++) {
                    if (classes[i].getClassLoader() instanceof AppClassLoader) {
                        throw new SecurityException(p.getName() +
                                                    " not allowed for application code");
                    }
                }
            }
        }
    }

    /**
     *
     *
     * @param p ...
     * @param context ...
     */
    public void checkPermission(Permission p, Object context) {
    }

    /**
     *
     */
    public void checkCreateClassLoader() {
    }

    /**
     *
     *
     * @param thread ...
     */
    public void checkAccess(Thread thread) {
    }

    /**
     *
     *
     * @param group ...
     */
    public void checkAccess(ThreadGroup group) {
    }

    /**
     *
     *
     * @param status ...
     */
    public void checkExit(int status) {
        Class[] classes = getClassContext();

        for (int i = 0; i < classes.length; i++) {
            if (classes[i].getClassLoader() instanceof AppClassLoader) {
                throw new SecurityException("operation not allowed for application code");
            }
        }
    }

    /**
     *
     *
     * @param cmd ...
     */
    public void checkExec(String cmd) {
    }

    /**
     *
     *
     * @param lib ...
     */
    public void checkLink(String lib) {
    }

    /**
     *
     *
     * @param fdesc ...
     */
    public void checkRead(FileDescriptor fdesc) {
    }

    /**
     *
     *
     * @param file ...
     */
    public void checkRead(String file) {
    }

    /**
     *
     *
     * @param file ...
     * @param context ...
     */
    public void checkRead(String file, Object context) {
    }

    /**
     *
     *
     * @param fdesc ...
     */
    public void checkWrite(FileDescriptor fdesc) {
    }

    /**
     *
     *
     * @param file ...
     */
    public void checkWrite(String file) {
    }

    /**
     *
     *
     * @param file ...
     */
    public void checkDelete(String file) {
    }

    /**
     *
     *
     * @param host ...
     * @param port ...
     */
    public void checkConnect(String host, int port) {
    }

    /**
     *
     *
     * @param host ...
     * @param port ...
     * @param context ...
     */
    public void checkConnect(String host, int port, Object context) {
    }

    /**
     *
     *
     * @param port ...
     */
    public void checkListen(int port) {
    }

    /**
     *
     *
     * @param host ...
     * @param port ...
     */
    public void checkAccept(String host, int port) {
    }

    /**
     *
     *
     * @param addr ...
     */
    public void checkMulticast(InetAddress addr) {
    }

    /**
     *
     */
    public void checkPropertiesAccess() {
    }

    /**
     *
     *
     * @param key ...
     */
    public void checkPropertyAccess(String key) {
    }

    /**
     *
     *
     * @param window ...
     *
     * @return ...
     */
    public boolean checkTopLevelWindow(Object window) {
        return true;
    }

    /**
     *
     */
    public void checkPrintJobAccess() {
    }

    /**
     *
     */
    public void checkSystemClipboardAccess() {
    }

    /**
     *
     */
    public void checkAwtEventQueueAccess() {
    }

    /**
     *
     *
     * @param pkg ...
     */
    public void checkPackageAccess(String pkg) {
    }

    /**
     *
     *
     * @param pkg ...
     */
    public void checkPackageDefinition(String pkg) {
    }

    /**
     *
     */
    public void checkSetFactory() {
    }

    /**
     *
     *
     * @param clazz ...
     * @param which ...
     */
    public void checkMemberAccess(Class clazz, int which) {
    }

    /**
     *
     *
     * @param target ...
     */
    public void checkSecurityAccess(String target) {
    }

    /**
     *  Utility method that returns the name of the application trying
     *  to execute the code in question. Returns null if the current code
     *  does not belong to any application.
     */
    protected String getApplication() {
        Class[] classes = getClassContext();

        for (int i = 0; i < classes.length; i++) {
            if (classes[i].getClassLoader() instanceof AppClassLoader) {
                return ((AppClassLoader) classes[i].getClassLoader()).getAppName();
            }
        }

        // no application class loader found in stack - return null
        return null;
    }
}
