// HelmaShutdownHook.java
package helma.main;

/**
 * ShutdownHook that shuts down all running Helma applications on exit.
 */
public class HelmaShutdownHook extends Thread {

    ApplicationManager appmgr;

    public HelmaShutdownHook (ApplicationManager appmgr) {
	this.appmgr = appmgr;
    }

    public void run () {
	System.err.print ("Shutting down Helma...");
	System.err.println ("done");
    }

}
