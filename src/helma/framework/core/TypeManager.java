// TypeManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import helma.objectmodel.*;
import helma.objectmodel.db.DbMapping;
import helma.scripting.*;
import helma.util.*;
import java.util.*;
import java.io.*;


/**
 * The type manager periodically checks the prototype definitions for its 
 * applications and updates the evaluators if anything has changed.
 */

public class TypeManager {

    Application app;
    File appDir;
    HashMap prototypes;
    HashMap zipfiles;
    long lastCheck = 0;
    long idleSeconds = 120; // if idle for longer than 5 minutes, slow down
    boolean rewire;

    // this contains only those evaluatores which have already been initialized
    // and thus need to get updates
    List registeredEvaluators;


    static HashSet standardTypes;
    static {
	standardTypes = new HashSet ();
	standardTypes.add ("user");
	standardTypes.add ("global");
	standardTypes.add ("root");
	standardTypes.add ("hopobject");
    }

    public TypeManager (Application app) {
	this.app = app;
	appDir = app.appDir;
	// make sure the directories for the standard prototypes exist, and lament otherwise
	if (appDir.list().length == 0) {
	    for (Iterator it=standardTypes.iterator (); it.hasNext (); ) {
	        File f = new File (appDir, (String) it.next ());
	        if (!f.exists() && !f.mkdir ())	
	            app.logEvent ("Warning: directory "+f.getAbsolutePath ()+" could not be created.");
	        else if (!f.isDirectory ())
	            app.logEvent ("Warning: "+f.getAbsolutePath ()+" is not a directory.");
	    }
	}
	prototypes = new HashMap ();
	zipfiles = new HashMap ();
	registeredEvaluators = Collections.synchronizedList (new ArrayList ());
    }


    /**
     * Run through application's prototype directories and create prototypes, but don't
     * compile or evaluate any scripts.
     */
    public void createPrototypes () {
	check (false);
    }


    /**
     * Run through application's prototype directories and check if anything has been updated.
     * If so, update prototypes and scripts.
     */
    public synchronized void checkPrototypes () {
	if (System.currentTimeMillis () - lastCheck  < 500l)
	    return;
	try {
	    check (true);
	} catch (Exception ignore) {}
	lastCheck = System.currentTimeMillis ();
    }

    /**
     * Run through application's prototype directories and check if anything has been updated.
     */
    public void check (boolean update) {
	// long now = System.currentTimeMillis ();
	// System.out.print ("checking "+Thread.currentThread ());
	File[] list = appDir.listFiles ();
	if (list == null)
	    throw new RuntimeException ("Can't read app directory "+appDir+" - check permissions");
	for (int i=0; i<list.length; i++) {
	    String filename = list[i].getName ();
	    Prototype proto = getPrototype (filename);
	    if (proto != null) {
	        // check if existing prototype needs update
	        // app.logEvent (protoDir.lastModified ());
	        updatePrototype (filename, list[i], proto);
	    } else if (list[i].isDirectory () && isValidTypeName (filename)) {
	        // leave out ".." and other directories that contain "."
	        // create new prototype
	        proto = new Prototype (filename, app);
	        registerPrototype (filename, list[i], proto, update);
	        prototypes.put (filename, proto);
	        // give logger thread a chance to tell what's going on
	        // Thread.yield();
	    } else if (filename.toLowerCase().endsWith (".zip") && !list[i].isDirectory ()) {
	        ZippedAppFile zipped = (ZippedAppFile) zipfiles.get (filename);
	        if (zipped == null) {
	            zipped = new ZippedAppFile (list[i], app);
	            zipfiles.put (filename, zipped);
	        }
	    }
	}

	// loop through zip files to check for updates
	for (Iterator it=zipfiles.values ().iterator (); it.hasNext (); ) {
	    ZippedAppFile zipped = (ZippedAppFile) it.next ();
	    if (zipped.needsUpdate ()) {
	        zipped.update ();
	    }
	}
	
	// check if standard prototypes have been created
	// as a performance hack, we only do this when update is false, i.e. the first time we're called.
	if (!update) {
	    for (Iterator it=standardTypes.iterator (); it.hasNext (); ) {
	        String pname = (String) it.next();
	        if (prototypes.get (pname) == null) {
	            Prototype proto = new Prototype (pname, app);
	            registerPrototype (pname, new File (appDir, pname), proto, false);
	            prototypes.put (pname, proto);
	        }
	    }
	}
	
	if (rewire) {
	    // there have been changes in the  DbMappings
	    app.rewireDbMappings ();
	    rewire = false;
	}
	// app.logEvent (" ...done @ "+ (System.currentTimeMillis () - now)+ "--- "+idleSeconds);
    }


    private boolean isValidTypeName (String str) {
    	if (str == null)
    	    return false;
    	char[] c = str.toCharArray ();
	for (int i=0; i<c.length; i++)
	    if (!Character.isJavaIdentifierPart (c[i]))
	        return false;
	return true;
    }

    /* public void start () {
    	stop ();
	typechecker = new Thread (this, "Typechecker-"+app.getName());
	typechecker.setPriority (Thread.MIN_PRIORITY);
	typechecker.start ();
    }

    public void stop () {
	if (typechecker != null && typechecker.isAlive ())
	    typechecker.interrupt ();
	typechecker = null;
    }

    public void run () {

	while (Thread.currentThread () == typechecker) {
	    idleSeconds++;
	    try {
	        // for each idle minute, add 300 ms to sleeptime until 5 secs are reached.
	        // (10 secs are reached after 30 minutes of idle state)
	        // the above is all false.
	        long sleeptime = 1000 + Math.min (idleSeconds*30, 4000);
	        typechecker.sleep (sleeptime);
	    } catch (InterruptedException x) {
	        // app.logEvent ("Typechecker interrupted");
	        break;
	    }
	    try {
	        checkPrototypes ();
	    } catch (Exception ignore) {}
	}
    } */


    /**
    *   Get a prototype defined for this application
    */
    public Prototype getPrototype (String typename) {
	return (Prototype) prototypes.get (typename);
    }

    /**
     * Get a prototype, creating it if id doesn't already exist
     */
    public Prototype createPrototype (String typename) {
	Prototype p = getPrototype (typename);
	if (p == null) {
	    p = new Prototype (typename, app);
	    p.templates = new HashMap ();
	    p.functions = new HashMap ();
	    p.actions = new HashMap ();
	    p.skins = new HashMap ();
	    p.updatables = new HashMap ();
	    prototypes.put (typename, p);
	}
	return p;
    }


    /**
     *  Create a prototype from a directory containing scripts and other stuff
     */
    public void registerPrototype (String name, File dir, Prototype proto, boolean update) {
        // app.logEvent ("registering prototype "+name);

        // show the type checker thread that there has been type activity
        idleSeconds = 0;

        HashMap ntemp = new HashMap ();
        HashMap nfunc = new HashMap ();
        HashMap nact = new HashMap ();
        HashMap nskins = new HashMap ();
        HashMap updatables = new HashMap ();

        if (update) {
            String list[] = dir.list();
            for (int i=0; i<list.length; i++) {
                File tmpfile = new File (dir, list[i]);
                int dot = list[i].indexOf (".");

                if (dot < 0)
                    continue;

                String tmpname = list[i].substring(0, dot);

                if (list[i].endsWith (app.templateExtension)) {
                    try {
                        Template t = new Template (tmpfile, tmpname, proto);
                        updatables.put (list[i], t);
                        ntemp.put (tmpname, t);
                    } catch (Throwable x) {
                        app.logEvent ("Error creating prototype: "+x);
                    }

                } else if (list[i].endsWith (app.scriptExtension) && tmpfile.length () > 0) {
                    try {
                        FunctionFile ff = new FunctionFile (tmpfile, tmpname, proto);
                        updatables.put (list[i], ff);
                        nfunc.put (tmpname, ff);
                    } catch (Throwable x) {
                        app.logEvent ("Error creating prototype: "+x);
                    }

                } else if (list[i].endsWith (app.actionExtension) && tmpfile.length () > 0) {
                    try {
                        Action af = new Action (tmpfile, tmpname, proto);
                        updatables.put (list[i], af);
                        nact.put (tmpname, af);
                    } catch (Throwable x) {
                        app.logEvent ("Error creating prototype: "+x);
                    }
                }  else if (list[i].endsWith (app.skinExtension)) {
                    try {
                        SkinFile sf = new SkinFile (tmpfile, tmpname, proto);
                        updatables.put (list[i], sf);
                        nskins.put (tmpname, sf);
                    } catch (Throwable x) {
                        app.logEvent ("Error creating prototype: "+x);
                    }
                }
            }
        }

        // Create and register type properties file
        File propfile = new File (dir, "type.properties");
        SystemProperties props = new SystemProperties (propfile.getAbsolutePath ());
        DbMapping dbmap = new DbMapping (app, name, props);
        updatables.put ("type.properties", dbmap);


        proto.templates = ntemp;
        proto.functions = nfunc;
        proto.actions = nact;
        proto.skins = nskins;
        proto.updatables = updatables;

        // init prototype on evaluators that are already initialized.
        Iterator evals = getRegisteredRequestEvaluators ();
        while (evals.hasNext ()) {
            RequestEvaluator reval = (RequestEvaluator) evals.next ();
            proto.initRequestEvaluator (reval);
        }

    }


    /**
    * Update a prototype based on the directory which defines it.
    */
    public void updatePrototype (String name, File dir, Prototype proto) {

        boolean needsUpdate = false;
        HashSet updatables = null;

        // our plan is to do as little as possible, so first check if anything has changed at all...
        for (Iterator i = proto.updatables.values().iterator(); i.hasNext(); ) {
            Updatable upd = (Updatable) i.next();
            if (upd.needsUpdate ()) {
                if (updatables == null)
                    updatables = new HashSet ();
                needsUpdate = true;
                updatables.add (upd);
            }
        }

        // check if file have been created since last update
        if (proto.lastUpdate < dir.lastModified ()) {
            String[] list = dir.list();
            for (int i=0; i<list.length; i++) {
	    String fn = list[i];
	    if (!proto.updatables.containsKey (fn)) {
	        if (fn.endsWith (app.templateExtension) || fn.endsWith (app.scriptExtension) ||
	    	fn.endsWith (app.actionExtension) || fn.endsWith (app.skinExtension) ||
	    	"type.properties".equalsIgnoreCase (fn)) {
	            needsUpdate = true;
	            // updatables.add ("[new:"+proto.getName()+"/"+fn+"]");
                      }
	    }
             }
        }

        if (!needsUpdate)
	return;
	
        proto.lastUpdate = System.currentTimeMillis ();
	
        // let the thread know we had to do something.
        idleSeconds = 0;
        // app.logEvent ("TypeManager: Updating prototypes for "+app.getName()+": "+updatables);

        // first go through new files and create new items
        String[] list = dir.list ();
        for (int i=0; i<list.length; i++) {
            String fn = list[i];
            int dot = fn.indexOf (".");

            if (dot < 0)
                continue;

            if (proto.updatables.containsKey (fn) || !(fn.endsWith (app.templateExtension) || fn.endsWith (app.scriptExtension) ||
            fn.endsWith (app.actionExtension) || fn.endsWith (app.skinExtension) || "type.properties".equalsIgnoreCase (fn))) {
                continue;
            }

            String tmpname = list[i].substring(0, dot);
            File tmpfile = new File (dir, list[i]);

            if (list[i].endsWith (app.templateExtension)) {
                try {
                    Template t = new Template (tmpfile, tmpname, proto);
                    proto.updatables.put (list[i], t);
                    proto.templates.put (tmpname, t);
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }

            } else if (list[i].endsWith (app.scriptExtension)) {
                try {
                    FunctionFile ff = new FunctionFile (tmpfile, tmpname, proto);
                    proto.updatables.put (list[i], ff);
                    proto.functions.put (tmpname, ff);
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }

            }  else if (list[i].endsWith (app.actionExtension)) {
                try {
                    Action af = new Action (tmpfile, tmpname, proto);
                    proto.updatables.put (list[i], af);
                    proto.actions.put (tmpname, af);
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }

            }  else if (list[i].endsWith (app.skinExtension)) {
                SkinFile sf = new SkinFile (tmpfile, tmpname, proto);
                proto.updatables.put (list[i], sf);
                proto.skins.put (tmpname, sf);
            }
        }

        // next go through existing updatables
        if (updatables == null)
            return;
        for (Iterator i = updatables.iterator(); i.hasNext(); ) {
            Updatable upd = (Updatable) i.next();

            if (upd.needsUpdate ()) {
                if (upd instanceof DbMapping)
                    rewire = true;
                try {
                    upd.update ();
                } catch (Exception x) {
                     if (upd instanceof DbMapping)
	            app.logEvent ("Error updating db mapping for type "+name+": "+x);
                     else
	            app.logEvent ("Error updating "+upd+" of prototye type "+name+": "+x);
                }
            }
        }
    }



    public void initRequestEvaluator (RequestEvaluator reval) {
        if (!registeredEvaluators.contains (reval))
            registeredEvaluators.add (reval);
        for (Iterator it = prototypes.values().iterator(); it.hasNext(); ) {
            Prototype p = (Prototype) it.next ();
            p.initRequestEvaluator (reval);
        }
        reval.initialized = true;
    }

    public void unregisterRequestEvaluator (RequestEvaluator reval) {
        registeredEvaluators.remove (reval);
    }

    public Iterator getRegisteredRequestEvaluators () {
        return registeredEvaluators.iterator ();
    }

    public int countRegisteredRequestEvaluators () {
        return registeredEvaluators.size ();
    }

}

