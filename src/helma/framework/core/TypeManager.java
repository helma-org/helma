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
    Prototype nodeProto;
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
	File f = new File (appDir, "user");
	if (!f.exists())	
	    f.mkdir ();
	f = new File (appDir, "root");
	if (!f.exists())	
	    f.mkdir ();
	f = new File (appDir, "global");
	if (!f.exists())	
	    f.mkdir ();
	f = new File (appDir, "hopobject");
	if (!f.exists())	
	    f.mkdir ();
	prototypes = new HashMap ();
	registeredEvaluators = Collections.synchronizedList (new ArrayList (30));
	nodeProto = null;
    }


    public void check () {
	// long now = System.currentTimeMillis ();
	// System.out.print ("checking "+Thread.currentThread ());
	String[] list = appDir.list ();
	if (list == null)
	    throw new RuntimeException ("Can't read app directory "+appDir+" - check permissions");
	for (int i=0; i<list.length; i++) {
	    File protoDir = new File (appDir, list[i]);
	    // cut out ".." and other directories that contain "."
	    if (isValidTypeName (list[i]) && protoDir.isDirectory ()) {
	        Prototype proto = getPrototype (list[i]);
	        if (proto != null) {
	            // check if existing prototype needs update
	            // app.logEvent (protoDir.lastModified ());
	            updatePrototype (list[i], protoDir, proto);
	        } else {
	            // create new prototype
	            proto = new Prototype (protoDir, app);
	            registerPrototype (list[i], protoDir, proto);
	            prototypes.put (list[i], proto);
	            if ("hopobject".equalsIgnoreCase (list[i]))
	                nodeProto = proto;
	            // give logger thread a chance to tell what's going on
	            Thread.yield();
	        }
	    }
	}

	/* for (Iterator it=prototypes.values ().iterator (); it.hasNext (); ) {
	    Prototype proto = (Prototype) it.next ();
	    if (!proto.getCodeDir ().exists ()) {
	        app.logEvent ("TypeManager: Can't remove prototype from running application. Restart for changes to take effect.");
	    }
	} */

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

    public Prototype getPrototype (String typename) {
	return (Prototype) prototypes.get (typename);
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

        updatables.put ("type.properties", proto.dbmap);

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
	    fn.endsWith (app.actionExtension) || fn.endsWith (app.scriptExtension) || "type.properties".equalsIgnoreCase (fn)) {
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

































































































