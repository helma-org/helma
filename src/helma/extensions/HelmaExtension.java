package helma.extensions;

import java.util.HashMap;

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
	  * called when an Application is started. This should be <b>synchronized</b> when
	  * any self-initialization is performed.
	  */
	public abstract void applicationStarted (Application app) throws ConfigurationException;

	/**
	  * called when an Application is stopped.
	  * This should be <b>synchronized</b> when any self-destruction is performed.
	  */
	public abstract void applicationStopped (Application app);

	/**
	  * called when an Application's properties are have been updated.
	  * note that this will be called at startup once *before* applicationStarted().
	  */
	public abstract void applicationUpdated (Application app);

	/**
	  * called by the ScriptingEngine when it is initizalized. Throws a ConfigurationException
	  * when this type of ScriptingEngine is not supported. New methods and prototypes can be
	  * added to the scripting environment. New global vars should be returned in a HashMap
	  * with pairs of varname and ESObjects. This method should be <b>synchronized</b>, if it
	  * performs any other self-initialization outside the scripting environment.
	  */
	public abstract HashMap initScripting (Application app, ScriptingEngine engine) throws ConfigurationException;

	public abstract String getName ();

}

