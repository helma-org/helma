// HelmaSecurityManager.java

package helma.main;

import java.security.Permission;
import java.io.FileDescriptor;
import java.net.InetAddress;
import java.util.HashSet;
import helma.framework.core.AppClassLoader;

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
    private final static HashSet forbidden = new HashSet ();
    static {
	forbidden.add ("exitVM");
	forbidden.add ("setSecurityManager");
    }

    public void checkPermission(Permission p) {
	if (p instanceof RuntimePermission) {
	    if (forbidden.contains (p.getName())) {
	        Class[] classes = getClassContext();
	        for (int i=0; i<classes.length; i++) {
	            if (classes[i].getClassLoader() instanceof AppClassLoader)
	                throw new SecurityException (p.getName()+" not allowed for application code");
	        }
	    }
	}
    }
    public void checkPermission(Permission p, Object context) {
    }
    public void checkCreateClassLoader() {}
    public void checkAccess(Thread thread) {}
    public void checkAccess(ThreadGroup group) {}
    public void checkExit(int status) {
	Class[] classes = getClassContext();
	for (int i=0; i<classes.length; i++) {
	    if (classes[i].getClassLoader() instanceof AppClassLoader)
	        throw new SecurityException ("operation not allowed for application code");
	}
    }
    public void checkExec(String cmd) {}
    public void checkLink(String lib) {}
    public void checkRead(FileDescriptor fdesc) {}
    public void checkRead(String file) {}
    public void checkRead(String file, Object context) {}
    public void checkWrite(FileDescriptor fdesc) {}
    public void checkWrite(String file) {}
    public void checkDelete(String file) {}
    public void checkConnect(String host, int port) {}
    public void checkConnect(String host, int port, Object context) {}
    public void checkListen(int port) {}
    public void checkAccept(String host, int port) {}
    public void checkMulticast(InetAddress addr) {}
    public void checkMulticast(InetAddress addr, byte ttl) {}
    public void checkPropertiesAccess() {}
    public void checkPropertyAccess(String key) {}
    public boolean checkTopLevelWindow(Object window) { return true; }
    public void checkPrintJobAccess() {}
    public void checkSystemClipboardAccess() {}
    public void checkAwtEventQueueAccess() {}
    public void checkPackageAccess(String pkg) {}
    public void checkPackageDefinition(String pkg) {}
    public void checkSetFactory() {}
    public void checkMemberAccess(Class clazz, int which) {}
    public void checkSecurityAccess(String target) {}

    /**
     *  Utility method that returns the name of the application trying
     *  to execute the code in question. Returns null if the current code 
     *  does not belong to any application.
     */
    protected String getApplication () {
	Class[] classes = getClassContext();
	for (int i=0; i<classes.length; i++) {
	    if (classes[i].getClassLoader() instanceof AppClassLoader)
	        return ((AppClassLoader) classes[i].getClassLoader()).getAppName ();
	}
	// no application class loader found in stack - return null
	return null;
    }

}
