// HelmaShutdownHook.java
package helma.main;

import helma.util.Logger;
import java.util.List;

/**
 * ShutdownHook that shuts down all running Helma applications on exit.
 */
public class HelmaShutdownHook extends Thread {

    ApplicationManager appmgr;

    public HelmaShutdownHook (ApplicationManager appmgr) {
	this.appmgr = appmgr;
    }

    public void run () {
	Logger logger = Server.getLogger();
	if (logger != null)
	    logger.log ("Shutting down Helma");
	appmgr.stopAll ();
	List loggers = Logger.getLoggers();
	int l = loggers.size();
	for (int i=0; i<l; i++)
	    ((Logger) loggers.get(i)).close();
	Logger.wakeup();
    }

}
