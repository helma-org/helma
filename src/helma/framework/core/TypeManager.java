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
    Hashtable prototypes;
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
	prototypes = new Hashtable ();
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
	        long sleeptime = 1500 + Math.min (idleSeconds*30, 3500);
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
        Hashtable ntemp = new Hashtable ();
        Hashtable nfunc = new Hashtable ();
        Hashtable nact = new Hashtable ();
        Hashtable nskins = new Hashtable ();

        for (int i=0; i<list.length; i++) {
            File tmpfile = new File (dir, list[i]);
            int dot = list[i].indexOf (".");

            if (dot < 0)
                continue;

            String tmpname = list[i].substring(0, dot);

            if (list[i].endsWith (app.templateExtension)) {
                try {
                    Template t = new Template (tmpfile, tmpname, proto);
                    ntemp.put (tmpname, t);
                } catch (Throwable x) {
                    app.logEvent ("Error creating prototype: "+x);
                }

            } else if (list[i].endsWith (app.scriptExtension) && tmpfile.length () > 0) {
                try {
                    FunctionFile ff = new FunctionFile (tmpfile, tmpname, proto);
                    nfunc.put (tmpname, ff);
                } catch (Throwable x) {
                    app.logEvent ("Error creating prototype: "+x);
                }

            } else if (list[i].endsWith (app.actionExtension) && tmpfile.length () > 0) {
                try {
                    Action af = new Action (tmpfile, tmpname, proto);
                    nact.put (tmpname, af);
                } catch (Throwable x) {
                    app.logEvent ("Error creating prototype: "+x);
                }
           }  else if (list[i].endsWith (app.skinExtension)) {
                try {
                    SkinFile sf = new SkinFile (tmpfile, tmpname, proto);
                    nskins.put (tmpname, sf);
                } catch (Throwable x) {
                    app.logEvent ("Error creating prototype: "+x);
                }
           }
        }
        proto.templates = ntemp;
        proto.functions = nfunc;
        proto.actions = nact;
        proto.skins = nskins;

        // init prototype on evaluators that are already initialized.
        Iterator evals = getRegisteredRequestEvaluators ();
        while (evals.hasNext ()) {
            RequestEvaluator reval = (RequestEvaluator) evals.next ();
            proto.initRequestEvaluator (reval);
        }

    }


    public void updatePrototype (String name, File dir, Prototype proto) {
        // app.logEvent ("updating prototype "+name);

        String list[] = dir.list();
        Hashtable ntemp = new Hashtable ();
        Hashtable nfunc = new Hashtable ();
        Hashtable nact = new Hashtable ();
        Hashtable nskins = new Hashtable ();

        for (int i=0; i<list.length; i++) {
            File tmpfile = new File (dir, list[i]);
            int dot = list[i].indexOf (".");

            if (dot < 0)
                continue;

            String tmpname = list[i].substring(0, dot);
            if (list[i].endsWith (app.templateExtension)) {
                Template t = proto.getTemplate (tmpname);
	   try {
                    if (t == null) {
                        t = new Template (tmpfile, tmpname, proto);
                        idleSeconds = 0;
	       } else if (t.lastmod != tmpfile.lastModified ()) {
                        t.update (tmpfile);
                        idleSeconds = 0;
                    }
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }
                ntemp.put (tmpname, t);

            } else if (list[i].endsWith (app.scriptExtension) && tmpfile.length () > 0) {
                FunctionFile ff = proto.getFunctionFile (tmpname);
                try {
                    if (ff == null) {
                        ff = new FunctionFile (tmpfile, tmpname, proto);
	           idleSeconds = 0;
                    } else if (ff.lastmod != tmpfile.lastModified ()) {
                        ff.update (tmpfile);
	           idleSeconds = 0;
                    }
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }
                nfunc.put (tmpname, ff);

           }  else if (list[i].endsWith (app.actionExtension) && tmpfile.length () > 0) {
                Action af = proto.getAction (tmpname);
                try {
                    if (af == null) {
                        af = new Action (tmpfile, tmpname, proto);
	           idleSeconds = 0;
                    } else if (af.lastmod != tmpfile.lastModified ()) {
                        af.update (tmpfile);
	           idleSeconds = 0;
                    }
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }
                nact.put (tmpname, af);

           }  else if (list[i].endsWith (app.skinExtension)) {
                SkinFile sf = proto.getSkinFile (tmpname);
                try {
                    if (sf == null) {
                        sf = new SkinFile (tmpfile, tmpname, proto);
	           idleSeconds = 0;
                    } else if (sf.lastmod != tmpfile.lastModified ()) {
                        sf.update (tmpfile);
	           idleSeconds = 0;
                    }
                } catch (Throwable x) {
                    app.logEvent ("Error updating prototype: "+x);
                }
                nskins.put (tmpname, sf);

           } else if ("type.properties".equalsIgnoreCase (list[i])) {
	    try {
	        if (proto.dbmap.read ()) {
	            idleSeconds = 0;
	            rewire = true;
	        }
	    } catch (Exception ignore) {
	        app.logEvent ("Error updating db mapping for type "+name+": "+ignore);
	    }
	}
        }
        proto.templates = ntemp;
        proto.functions = nfunc;
        proto.actions = nact;
        proto.skins = nskins;
    }



    public void initRequestEvaluator (RequestEvaluator reval) {
        if (!registeredEvaluators.contains (reval))
            registeredEvaluators.add (reval);
        for (Enumeration en = prototypes.elements(); en.hasMoreElements(); ) {
            Prototype p = (Prototype) en.nextElement ();
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

































































































