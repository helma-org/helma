package helma.framework.core;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.List;

import helma.objectmodel.INode;

public class ApplicationBean implements Serializable {

    Application app;

    public ApplicationBean(Application app) {
	this.app = app;
    }

    public void clearCache () {
	app.clearCache ();
    }

    public void log (String msg) {
	app.logEvent (msg);
    }

    public void log (String logname, String msg) {
	app.getLogger (logname).log (msg);
    }

    public void debug (String msg) {
	if (app.debug()) {
	    app.logEvent (msg);
	}
    }

    public void debug (String logname, String msg) {
	if (app.debug()) {
	    app.getLogger (logname).log (msg);
	}
    }

    public int countSessions () {
	return app.sessions.size();
    }

    public SessionBean getSession (String sessionID) {
	if (sessionID==null)
	    return null;
	Session session = app.getSession (sessionID.trim ());
	if (session == null)
	    return null;
	return new SessionBean (session);
    }

    public SessionBean createSession (String sessionID) {
	if (sessionID==null)
	    return null;
	Session session = session = app.checkSession (sessionID.trim ());
	if (session == null)
	    return null;
	return new SessionBean (session);
    }

    public SessionBean[] getSessions () {
	SessionBean[] theArray = new SessionBean[app.sessions.size()];
	int i=0;
	for (Enumeration e=app.sessions.elements(); e.hasMoreElements(); ) {
	    SessionBean sb = new SessionBean ((Session) e.nextElement ());
	    theArray[i++] = sb;
	}
	return theArray;
    }

    public INode registerUser (String username, String password) {
	if (username==null || password==null || "".equals (username.trim ()) || "".equals (password.trim ()) )
	    return null;
	else
	    return app.registerUser (username, password);
    }

    public INode getUser (String username) {
	if (username==null || "".equals (username.trim()) )
	    return null;
	return app.getUserNode (username);
    }

    public INode[] getActiveUsers () {
	List activeUsers = app.getActiveUsers ();
	return (INode[]) activeUsers.toArray (new INode[0]);
    }

    public SessionBean[] getSessionsForUser (INode usernode)	{
	if (usernode==null)
	    return new SessionBean[0];
	else
	    return getSessionsForUser(usernode.getName());
    }

    public SessionBean[] getSessionsForUser (String username) {
	if (username==null || "".equals (username.trim ()) )
	    return new SessionBean[0];
	List userSessions = app.getSessionsForUsername (username);
	return (SessionBean[]) userSessions.toArray (new SessionBean[0]);
    }

    // getter methods for readonly properties of this application

    public INode getdata() {
	return app.getCacheNode ();
    }

    public Date getupSince () {
	return new Date (app.starttime);
    }

    public long getrequestCount ()	{
	return app.getRequestCount ();
    }

    public long getxmlrpcCount ()	{
	return app.getXmlrpcCount ();
    }

    public long geterrorCount () {
	return app.getErrorCount ();
    }

    public Application get__app__ () {
	return app;
    }

    public Map getproperties () {
	return app.getProperties ();
    }

    public int getfreeThreads () {
	return app.countFreeEvaluators ();
    }

    public int getactiveThreads () {
	return app.countActiveEvaluators ();
    }

    public int getmaxThreads () {
	return app.countEvaluators ();
    }

    public void setmaxThreads (int n) {
	// add one to the number to compensate for the internal scheduler.
	app.setNumberOfEvaluators (n+1);
    }

    public Map getskinfiles () {
	Map skinz = new Hashtable ();
	for (Iterator it = app.getPrototypes().iterator(); it.hasNext(); ) {
	    Prototype p = (Prototype) it.next ();
	    Map proto = new Hashtable ();
	    for (Iterator it2 = p.skins.values().iterator(); it2.hasNext(); ) {
	        SkinFile sf = (SkinFile) it2.next ();
	        String name = sf.getName ();
	        Skin skin = sf.getSkin ();
	        proto.put (name, skin.getSource ());
	    }
	    skinz.put (p.getName (), proto);
	}
	return skinz;
    }


    public String toString() {
	return "[Application " + app.getName() + "]";
    }

}



