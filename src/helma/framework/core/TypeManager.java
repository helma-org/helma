// TypeManager.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import helma.objectmodel.*;
import helma.util.*;
import java.util.*;
import java.io.*;


/**
 * The type manager periodically checks the prototype definitions for its 
 * applications and updates the evaluators if anything has changed.
 */

public class TypeManager implements Runnable {

    Application app;
    File appDir;
    HashMap prototypes;
    HashMap zipfiles;
    long idleSeconds = 120; // if idle for longer than 5 minutes, slow down
    boolean rewire;

    // this contains only those evaluatores which have already been initialized
    // and thus need to get updates
    List registeredEvaluators;

    Thread typechecker;

   // The http broadcaster for pushing out parser output
   // static WebBroadcaster broadcaster;
   // static {
   //   try {
   //      broadcaster = new WebBroadcaster (9999);
   //   } catch (IOException ignore) {}
   // }

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
	for (Iterator it=standardTypes.iterator (); it.hasNext (); ) {
	    File f = new File (appDir, (String) it.next ());
	    if (!f.exists() && !f.mkdir ())	
	        app.logEvent ("Warning: directory "+f.getAbsolutePath ()+" could not be created.");
	    else if (!f.isDirectory ())     
	        app.logEvent ("Warning: "+f.getAbsolutePath ()+" is not a directory.");
	}
	prototypes = new HashMap ();
	zipfiles = new HashMap ();
	registeredEvaluators = Collections.synchronizedList (new ArrayList (30));
    }


    public void check () {
	// long now = System.currentTimeMillis ();
	// System.out.print ("checking "+Thread.currentThread ());
	String[] list = appDir.list ();
	if (list == null)
	    throw new RuntimeException ("Can't read app directory "+appDir+" - check permissions");
	for (int i=0; i<list.length; i++) {
	    File fileOrDir = new File (appDir, list[i]);
	    // cut out ".." and other directories that contain "."
	    if (isValidTypeName (list[i]) && fileOrDir.isDirectory ()) {
	        Prototype proto = getPrototype (list[i]);
	        if (proto != null) {
	            // check if existing prototype needs update
	            // app.logEvent (protoDir.lastModified ());
	            updatePrototype (list[i], fileOrDir, proto);
	        } else {
	            // create new prototype
	            proto = new Prototype (list[i], app);
	            registerPrototype (list[i], fileOrDir, proto);
	            prototypes.put (list[i], proto);
	            // give logger thread a chance to tell what's going on
	            Thread.yield();
	        }
	    } else if (list[i].toLowerCase().endsWith (".zip") && !fileOrDir.isDirectory ()) {
	        ZippedAppFile zipped = (ZippedAppFile) zipfiles.get (list[i]);
	        if (zipped == null) {
	            zipped = new ZippedAppFile (fileOrDir, app);
	            zipfiles.put (list[i], zipped);
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

	if (rewire) {
	    // there have been changes @ DbMappings
	    app.rewireDbMappings ();
	    rewire = false;
	}
	// app.logEvent (" ...done @ "+ (System.currentTimeMillis () - now)+ "--- "+idleSeconds);
    }


    private boolean isValidTypeName (String str) {
    	if (str == null)
    	    return false;
    	int l = str.length ();
    	if (l == 0)
    	    return false;
	for (int i=0; i<l; i++)
	    if (!Character.isJavaIdentifierPart (str.charAt (i)))
	        return false;
	return true;
    }

    public void start () {
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
	        check ();
	    } catch (Exception ignore) {}
	}
    }


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
    public void registerPrototype (String name, File dir, Prototype proto) {
        // app.logEvent ("registering prototype "+name);

        // show the type checker thread that there has been type activity
        idleSeconds = 0;

        String list[] = dir.list();
        HashMap ntemp = new HashMap ();
        HashMap nfunc = new HashMap ();
        HashMap nact = new HashMap ();
        HashMap nskins = new HashMap ();
        HashMap updatables = new HashMap (list.length);

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
        // app.logEvent ("updating prototype "+name);

        boolean needsUpdate = false;
        HashSet updatables = new HashSet ();

        // our plan is to do as little as possible, so first check if anything has changed at all...
        for (Iterator i = proto.updatables.values().iterator(); i.hasNext(); ) {
	Updatable upd = (Updatable) i.next();
	if (upd.needsUpdate ()) {
	    needsUpdate = true;
	    updatables.add (upd);
             }
        }

        String list[] = dir.list();
        for (int i=0; i<list.length; i++) {
	String fn = list[i];
	if (!proto.updatables.containsKey (fn)) {
	    if (fn.endsWith (app.templateExtension) || fn.endsWith (app.scriptExtension) ||
	    fn.endsWith (app.actionExtension) || fn.endsWith (app.skinExtension) || "type.properties".equalsIgnoreCase (fn)) {
	        needsUpdate = true;
	        updatables.add ("[new:"+proto.getName()+"/"+fn+"]");
                 }
	}
        }

        if (!needsUpdate)
	return;

        // let the thread know we had to do something.
        idleSeconds = 0;
        app.logEvent ("TypeManager: Updating prototypes for "+app.getName()+": "+updatables);

        // first go through new files and create new items
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
        for (Iterator i = proto.updatables.values().iterator(); i.hasNext(); ) {
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

































































































