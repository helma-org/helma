package helma.framework.core;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;

import helma.objectmodel.INode;
import helma.scripting.ScriptingEnvironment;
import helma.scripting.ScriptingException;
import helma.scripting.fesi.*;

public class SessionBean implements Serializable {

    // the wrapped session object
    Session session;

    public SessionBean(Session session)	{
	this.session = session;
    }

    public String toString()	{
	return session.toString ();
    }

    public boolean login (String username, String password) {
	boolean success = session.getApp().loginSession (username, password, session);
	return success;
    }

    public void logout ()	{
	session.getApp().logoutSession (session);
    }

    public void touch () {
	session.touch ();
    }

    public Date lastActive() {
	return new Date (session.lastTouched ());
    }

    public Date onSince() {
	return new Date (session.onSince ());
    }

    // property-related methods:

    public INode getdata() {
	return session.getCacheNode ();
    }

    public INode getuser() {
	return session.getUserNode();
    }

    public String get_id () {
	return session.getSessionID ();
    }

    public String getcookie() {
	return session.getSessionID ();
    }

    public Date getlastActive() {
	return new Date (session.lastTouched ());
    }

    public Date getonSince() {
	return new Date (session.onSince ());
    }

}

