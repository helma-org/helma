package helma.extensions.demo;

import helma.extensions.HelmaExtension;
import helma.extensions.ConfigurationException;
import helma.framework.core.Application;
import helma.main.Server;
import helma.scripting.ScriptingEngine;

// fesi-related stuff:
import FESI.Data.ESObject;
import FESI.Data.ESWrapper;
import FESI.Data.GlobalObject;
import FESI.Exceptions.EcmaScriptException;
import FESI.Interpreter.Evaluator;
import helma.scripting.fesi.FesiEvaluator;


/**
  * a demo extension implementation, to activate this add <code>extensions =
  * helma.extensions.demo.DemoExtensions</code> to your <code>server.properties</code>.
  * a new global object <code>demo</code> that wraps helma.main.Server
  * will be added to the scripting environment.
  */

public class DemoExtension extends HelmaExtension {

	public void init (Server server) throws ConfigurationException {
	try {
		// just a demo with the server class itself (which is always there, obviously)
	    Class check = Class.forName("helma.main.Server");
	} catch (ClassNotFoundException e) {
	    throw new ConfigurationException("helma-library not present in classpath. make sure helma.jar is included. get it from http://www.helma.org/");
	}
	}

	public void applicationStarted (Application app) throws ConfigurationException {
	app.logEvent ("DemoExtension init with app " + app.getName () );
	}

	public void applicationStopped (Application app) {
	app.logEvent ("DemoExtension stopped on app " + app.getName () );
	}

	public void initScripting (Application app, ScriptingEngine engine) throws ConfigurationException {
	if (engine instanceof FesiEvaluator) {
	    try {
	        initFesi (app, engine);
	        app.logEvent("initScripting DemoExtension with " + app.getName () + " and " + engine.toString() );
	    } catch (EcmaScriptException ecma) {
	        throw new ConfigurationException (ecma.getMessage());
	    }
	} else {
	    throw new ConfigurationException ("scripting engine " + engine.toString () + " not supported in DemoExtension");
	}
	}

	public String getName () {
	return "DemoExtension";
	}

	private void initFesi (Application app, ScriptingEngine engine) throws EcmaScriptException {
	Evaluator evaluator = ((FesiEvaluator)engine).getEvaluator ();
    ESObject demo = new ESWrapper(Server.getServer (), evaluator);
    GlobalObject go = evaluator.getGlobalObject();
	go.putHiddenProperty ("demo", demo);
	}

}

