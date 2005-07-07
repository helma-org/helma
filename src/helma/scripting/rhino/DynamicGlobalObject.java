/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.scripting.rhino;

import helma.framework.core.Application;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Context;


/**
 * This class implements a global scope object that is a dynamic proxy
 * to a shared global scope and a per-thread dynamic scope.
 */
public class DynamicGlobalObject extends GlobalObject {

    public DynamicGlobalObject(RhinoCore core, Application app) {
        super(core, app);
    }

    public Object get(String s, Scriptable scriptable) {
        Context cx = Context.getCurrentContext();
        Scriptable scope = (Scriptable) cx.getThreadLocal("threadscope");
        if (scope != null) {
            Object obj = scope.get(s, scope);
            if (obj != null && obj != NOT_FOUND) {
                return obj;
            }
        }
        return super.get(s, scriptable);
    }

}
