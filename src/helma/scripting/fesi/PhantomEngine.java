// PhantomEngine.java
// Copyright (c) Hannes Wallnöfer 2002

package helma.scripting.fesi;

import helma.scripting.ScriptingException;

public final class PhantomEngine extends FesiEngine {

    /**
     *
     */
    public Object invoke (Object thisObject, String functionName, Object[] args, boolean xmlrpc) throws ScriptingException {
	return super.invoke (thisObject, functionName, args, xmlrpc);
    }

}