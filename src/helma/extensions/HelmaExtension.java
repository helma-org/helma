package helma.extensions;

import helma.framework.core.Application;
import helma.main.Server;
import helma.scripting.ScriptingEngine;

/**
  * Helma extensions have to subclass this. The extensions to be loaded are
  * defined in <code>server.properties</code> by setting <code>extensions =
  * packagename.classname, packagename.classname</code>.
  */

public abstract class HelmaExtension {

	/**
	  * called by the Server at startup time. should check wheter the needed classes
	  * are present and throw a ConfigurationException if not.
	  */
	public abstract void init (Server server) throws ConfigurationException;

	/**
	  * called when an Application is started.
	  */
	public abstract void applicationStarted (Application app) throws ConfigurationException;

	/**
	  * called when an Application is stopped.
	  */
	public abstract void applicationStopped (Application app);

	/**
	  * called by the ScriptingEngine when it is initizalized. throws a ConfigurationException
	  * when this type of ScriptingEngine is not supported.
	  */
	public abstract void initScripting (Application app, ScriptingEngine engine) throws ConfigurationException;

	public abstract String getName ();

}

