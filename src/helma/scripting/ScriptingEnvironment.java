// ScriptingEnvironment.java
// Copyright (c) Hannes Wallnöfer 1998-2001
 
package helma.scripting;


import java.util.*;


/**
 * This is the interface that must be implemented to make a scripting environment
 * usable by the Helma application server.
 */
public interface ScriptingEnvironment {

    /**
     * Initialize the environment using the given properties
     */
    public void init (Properties props) throws ScriptingException;


    /**
     * Invoke a function on some object, using the given arguments and global vars.
     */
    public Object invoke (Object thisObject, Object[] args, HashMap globals) throws ScriptingException;


}
































