package helma.framework.core;

import java.io.Serializable;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import helma.objectmodel.INode;


public class ApplicationBean implements Serializable {

	Application app;

	public ApplicationBean(Application app)	{
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
		INode[] theArray = new INode[app.activeUsers.size()];
		int i=0;
		for (Enumeration e=app.activeUsers.elements(); e.hasMoreElements(); ) {
			User user = (User) e.nextElement ();
			INode usernode = user.getUserNodeHandle().getNode(app.getWrappedNodeManager());
			if (usernode==null)
				theArray[i++] = null;
			else
				theArray[i++] = usernode;
		}
		return theArray;
	}

	public SessionBean[] getSessionsForUser (INode usernode)	{
		return getSessionsForUser(usernode.getName());
	}

	public SessionBean[] getSessionsForUser (String username) {
		if (username==null || "".equals (username.trim ()) )
			return new SessionBean[0];
		Hashtable userSessions = app.getSessionsForUsername(username);
		if (userSessions==null)
			return new SessionBean[0];
		SessionBean[] theArray = new SessionBean[userSessions.size()];
		int i=0;
		for (Enumeration e=userSessions.elements(); e.hasMoreElements(); ) {
        	SessionBean sb = new SessionBean ((Session) e.nextElement ());
            theArray[i++] = sb;
		}
		return theArray;
	}		

	// property related methods:

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

	public Object getskinfiles () {
		//FIXME:
//            ESObject skinz = new ObjectPrototype (null, evaluator);
//            for (Iterator it = app.getPrototypes().iterator(); it.hasNext(); ) {
//                Prototype p = (Prototype) it.next ();
//                ESObject  proto = new ObjectPrototype (null, evaluator);
//                for (Iterator it2 = p.skins.values().iterator(); it2.hasNext(); ) {
//                    SkinFile sf = (SkinFile) it2.next ();
//                    String name = sf.getName ();
//                    Skin skin = sf.getSkin ();
//                    proto.putProperty (name, new ESString (skin.getSource ()), name.hashCode ());
//                }
//                skinz.putProperty (p.getName (), proto, p.getName ().hashCode ());
//            }
            return null;
	}


	public String toString() {
		return "[Application " + app.getName() + "]";
	}


}



