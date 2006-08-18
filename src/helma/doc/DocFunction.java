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

import java.awt.Point;
import java.io.*;
import java.util.Vector;
import org.mozilla.javascript.*;
import helma.framework.repository.Resource;

/**
 * 
 */
public class DocFunction extends DocResourceElement {

    protected DocFunction(String name, Resource res, DocElement parent, int type) {
        super(name, res, type);
        this.parent = parent;
    }

    /**
     * creates a new independent DocFunction object of type ACTION
     */
    public static DocFunction newAction(Resource res) throws IOException {
        return newAction(res, null);
    }

    /**
     * creates a new DocFunction object of type ACTION connected to another DocElement
     */
    public static DocFunction newAction(Resource res, DocElement parent) throws IOException{
        String name = res.getBaseName();
        DocFunction func = new DocFunction(name, res, parent, ACTION);
        String rawComment = "";
        try {
            DockenStream ts = getDockenStream (res);
            Point p = getPoint (ts);
            ts.getToken();
            rawComment = Util.getStringFromFile(res, p, getPoint(ts));
            rawComment = Util.chopComment (rawComment);
        } catch (IOException io) {
            io.printStackTrace();
            throw new DocException (io.toString());
        }
        func.parseComment(rawComment);
        func.content = res.getContent();
        return func;
    }

    /**
     * reads a function file and creates independent DocFunction objects of type FUNCTION
     */
    public static DocFunction[] newFunctions(Resource res) {
        return newFunctions(res, null);
    }


    /**
     * reads a function file and creates DocFunction objects of type FUNCTION
     * connected to another DocElement.
     */
    public static DocFunction[] newFunctions(Resource res, DocElement parent) {

        Vector vec = new Vector();

        try {

            int token     = Token.EMPTY;
            int lastToken = Token.EMPTY;
            Point endOfLastToken      = null;
            Point endOfLastUsedToken  = null;
            Point startOfFunctionBody = null;
            Point endOfFunctionBody   = null;

            String lastNameString = null;
            String functionName   = null;
            String context        = null;

            DockenStream ts = getDockenStream (res);

            while (!ts.eof()) {

                // store the position of the last token 
                endOfLastToken = getPoint (ts);

                // store last token
                lastToken = token;

                // now get a new token
                // regular expression syntax is troublesome for the DockenStream
                // we can safely ignore syntax errors in regular expressions here
                try {
                    token = ts.getToken();
                } catch(Exception anything) {
                    continue;
                }

                if (token == Token.LC) {

                    // when we come across a left brace outside of a function,
                    // we store the current string of the stream, it might be
                    // a function object declaration
                    // e.g. HttpClient = { func1:function()...}
                    context = ts.getString();

                } else if (token == Token.RC && context != null) {

                    // when we come across a right brace outside of a function,
                    // we reset the current context cache
                    context = null;

                } else if (token == Token.NAME) {

                    // store all names, the last one before a function
                    // declaration may be used as its name

                    if (lastToken != Token.DOT) {

                        lastNameString = ts.getString();

                        // this may be the start of a name chain declaring a function
                        // e.g. Number.prototype.functionName = function() { }
                        startOfFunctionBody = getPoint(ts);
                        startOfFunctionBody.x -= (ts.getString().length() + 1);

                        // set pointer to end of last function to the token before the name
                        endOfLastUsedToken = endOfLastToken;

                    } else {

                        // token in front of the name was a dot, so we connect the
                        // names that way
                        lastNameString += "." + ts.getString();

                    }

                } else if (token == Token.FUNCTION) {

                    // store the end of the function word
                    Point p = getPoint(ts);

                    // look at the next token:
                    int peekToken = ts.peekToken();

                    // depending of the style of the declaration we already have all we need 
                    // or need to fetch the name from the next token:
                    if (peekToken == Token.NAME) {

                        // if the token after FUNCTION is NAME, it's the usual function
                        // declaration like this: function abc() {}

                        // set the pointer for the start of the actual function body
                        // to the letter f of the function word
                        startOfFunctionBody = p;
                        startOfFunctionBody.x -= 9;

                        // lastToken should be the last thing that didn't belong to this function
                        endOfLastUsedToken = endOfLastToken;

                        // set stream to next token, so that name of the
                        // function is the stream's current string
                        token = ts.getToken();
                        functionName = ts.getString();

                    } else {

                        // it's a different kind of function declaration.
                        // the function name is the last found NAME-token
                        // if context is set, prepend it to the function name
                        functionName = (context != null) ? context + "." + lastNameString : lastNameString;

                    }

                    // get the comment from the file (unfortunately, the stream simply skips comments) ...
                    String rawComment = Util.getStringFromFile(res, endOfLastUsedToken, startOfFunctionBody).trim ();
                    // .. and clean it
                    rawComment = Util.chopComment (rawComment);

                    // create the function object
                    DocFunction theFunction = newFunction (functionName, res, parent);
                    theFunction.parseComment (rawComment);
                    vec.add (theFunction);

                    // subloop on the tokenstream: find the parameters of a function
                    while (!ts.eof() && token != Token.RP) {
                        token = ts.getToken();
                        if (token==Token.NAME && theFunction.type == FUNCTION) {
                            // add names of parameter only for functions, not for macros or actions
                            theFunction.addParameter (ts.getString());
                        }
                    }

                    // subloop on the tokenstream: find the closing right bracket of the function
                    token = ts.getToken();
                    int level = (token == Token.LC) ? 1 : 0;
                    while (!ts.eof() && level > 0) {
                        // regular expression syntax is troublesome for the DockenStream
                        // we don't need them here, so we just ignore such an error
                        try {
                            token = ts.getToken();
                        } catch(Exception anything) {
                            continue;
                        }
                        if (token == Token.LC) {
                            level++;
                        } else if (token == Token.RC) {
                            level--;
                        }
                    }
                    endOfFunctionBody = getPoint(ts);

                    theFunction.content = Util.getStringFromFile(res, startOfFunctionBody, endOfFunctionBody);

                } // end if
            } // end while

        } catch (Exception ex) {
            ex.printStackTrace();
            throw new DocException (ex.toString());
        }
        return (DocFunction[]) vec.toArray(new DocFunction[0]);
    }


    private static DocFunction newFunction (String funcName, Resource res, DocElement parent) {
        if (funcName.endsWith("_action")) {
            return new DocFunction(funcName, res, parent, ACTION);
        } else if (funcName.endsWith("_macro")) {
            return new DocFunction(funcName, res, parent, MACRO);
        } else {
            return new DocFunction(funcName, res, parent, FUNCTION);
        }
    }


    /**
     * creates a rhino token stream for a given file
     */
    protected static DockenStream getDockenStream (Resource res) throws IOException {
        Reader reader = null;
        try {
            reader = new InputStreamReader(res.getInputStream());
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            throw new DocException (fnfe.toString());
        }
        // String name = res.getName();
        Integer line = new Integer(0);
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.initFromContext(Context.getCurrentContext());
        ErrorReporter errorReporter = Context.getCurrentContext().getErrorReporter();
        Parser parser = new Parser(compilerEnv, errorReporter);
        return new DockenStream (parser, reader, null, line);
    }


    /**
     * returns a pointer to the current position in the DockenStream
     */
    protected static Point getPoint (DockenStream ts) {
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
