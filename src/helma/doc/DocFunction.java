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

package helma.doc;

import helma.framework.IPathElement;
import java.awt.Point;
import java.lang.reflect.Constructor;
import java.io.*;
import java.util.Vector;
import org.mozilla.javascript.TokenStream;

/**
 * 
 */
public class DocFunction extends DocFileElement {

    protected DocFunction(String name, File location, DocElement parent, int type) {
        super(name, location, type);
        this.parent = parent;
    }

    /**
     * creates a new independent DocFunction object of type ACTION
     */
    public static DocFunction newAction(File location) {
        return newAction(location, null);
    }

    /**
     * creates a new DocFunction object of type ACTION connected to another DocElement
     */
    public static DocFunction newAction(File location, DocElement parent) {
        String name = Util.nameFromFile(location, ".hac");
        DocFunction func = new DocFunction(name, location, parent, ACTION);
        String rawComment = "";
        try {
            TokenStream ts = getTokenStream (location);
            Point p = getPoint (ts);
            ts.getToken();
            rawComment = Util.getStringFromFile(location, p, getPoint(ts));
        } catch (IOException io) {
            io.printStackTrace();
            throw new DocException (io.toString());
        }
        func.parseComment(rawComment);
        func.content = Util.readFile(location);
        return func;
    }

    /**
     * reads a function file and creates independent DocFunction objects of type FUNCTION
     */
    public static DocFunction[] newFunctions(File location) {
        return newFunctions(location, null);
    }


    /**
     * reads a function file and creates DocFunction objects of type FUNCTION
     * connected to another DocElement.
     */
    public static DocFunction[] newFunctions(File location, DocElement parent) {
        Vector vec = new Vector();
        try {
            // the function file:
            TokenStream ts = getTokenStream (location);
            DocFunction curFunction = null;
            Point curFunctionStart = null;
            while (!ts.eof()) {

                // store the position of the last token 
                Point endOfLastToken = getPoint (ts);
                // new token
                int tok = ts.getToken();

                // if we're currently parsing a functionbody and come to the start
                // of the next function or eof -> read function body
                if (curFunction != null && (tok== ts.FUNCTION || ts.eof())) {
                    curFunction.content = "function " + Util.getStringFromFile(location, curFunctionStart, endOfLastToken);
                }

                if (tok == ts.FUNCTION) {
                    // store the function start for parsing the function body later
                    curFunctionStart = getPoint (ts); 
                    // get and chop the comment
                    String rawComment = Util.getStringFromFile(location, endOfLastToken, getPoint (ts)).trim ();
                    if (rawComment.endsWith("function")) {
                        rawComment = rawComment.substring (0, rawComment.length()-8).trim();
                    }
                    // position stream at function name token
                    tok = ts.getToken();
                    // get the name and create the function object
                    String name = ts.getString();
                    curFunction = newFunction (name, location, parent);
                    curFunction.parseComment (rawComment);
                    vec.add (curFunction);

                    // subloop on the tokenstream: find the parameters of a function
                    // only if it's a function (and not a macro or an action)
                    if (curFunction.type == FUNCTION) {
                        while (!ts.eof() && tok != ts.RP) {
                            // store the position of the last token 
                            endOfLastToken = getPoint (ts);
                            // new token
                            tok = ts.getToken();
                            if (tok==ts.NAME) {
                                curFunction.addParameter (ts.getString());
                            }
                        }
                    }
                } // end if

            }

        } catch (IOException io) {
            io.printStackTrace();
            throw new DocException (io.toString());
        }
        return (DocFunction[]) vec.toArray(new DocFunction[0]);
    }


    private static DocFunction newFunction (String funcName, File location, DocElement parent) {
        if (funcName.endsWith("_action")) {
            return new DocFunction(funcName, location, parent, ACTION);
        } else if (funcName.endsWith("_macro")) {
            return new DocFunction(funcName, location, parent, MACRO);
        } else {
            return new DocFunction(funcName, location, parent, FUNCTION);
        }
    }


    /**
     * creates a rhino token stream for a given file
     */
    protected static TokenStream getTokenStream (File f) {
        FileReader reader = null;
        try {
            reader = new FileReader(f);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            throw new DocException (fnfe.toString());
        }
        String name = f.getName();
        int line = 0;

        // FIXME for some reason this doesn't compile with winxp/j2sdk141_03
        // return new TokenStream (reader, null, name, line);

        // so we have to use reflection:
        try {
            Class c = Class.forName ("org.mozilla.javascript.TokenStream");
            Constructor[] constr = c.getDeclaredConstructors();
            Object[] params = new Object[4];
            params[0] = reader;
            params[1] = null;
            params[2] = name;
            params[3] = new Integer(line);
            return (TokenStream) constr[0].newInstance(params);
        } catch (Exception anything) {
            anything.printStackTrace();
            throw new DocException (anything.toString());
        }
    }


    /**
     * returns a pointer to the current position in the TokenStream
     */
    protected static Point getPoint (TokenStream ts) {
        return new Point (ts.getOffset(), ts.getLineno());
    }



    /**
     * from helma.framework.IPathElement. All macros, templates, actions etc
     * have the same prototype.
     */
    public java.lang.String getPrototype() {
        return "docfunction";
    }
}
