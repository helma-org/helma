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

import helma.scripting.*;

// import java.util.Vector;
// import java.util.Iterator;
import java.io.*;

// import helma.framework.*;
// import helma.framework.core.*;
// import helma.util.Updatable;

/**
 *  An class that updates fesi interpreters with actionfiles and templates.
 */
public class RhinoActionAdapter {
    String sourceName;
    String function;
    String functionAsString;

    /**
     * Creates a new RhinoActionAdapter object.
     *
     * @param action ...
     */
    public RhinoActionAdapter(ActionFile action) {
        String content = action.getContent();
        String functionName = action.getFunctionName();

        sourceName = action.toString();
        function = composeFunction(functionName,
                                   "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10",
                                   content);

        // check if this is a template and we need to generate an "_as_string" variant
        if (action instanceof Template) {
            functionAsString = composeFunction(functionName + "_as_string",
                                               "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10",
                                               "res.pushStringBuffer(); " + content +
                                               "\r\nreturn res.popStringBuffer();\r\n");
        } else {
            functionAsString = null;
        }
    }

    protected String composeFunction(String funcname, String params, String body) {
        if ((body == null) || "".equals(body.trim())) {
            body = ";\r\n";
        } else {
            body = body + "\r\n";
        }

        if (params == null) {
            params = "";
        }

        StringBuffer f = new StringBuffer("function ");

        f.append(funcname);
        f.append(" (");
        f.append(params);
        f.append(") {\n");
        f.append(body);
        f.append("\n}");

        return f.toString();
    }
}
