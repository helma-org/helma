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

import helma.framework.repository.Resource;
import helma.util.StringUtils;

import java.awt.Point;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Vector;

import org.mozilla.javascript.*;

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
    public static DocFunction newAction(Resource res, DocElement parent) throws IOException {
        String name = res.getBaseName();
        String[] lines = StringUtils.splitLines(res.getContent());
        DocFunction func = new DocFunction(name, res, parent, ACTION);
        String rawComment = "";
        TokenStreamInjector ts = getTokenStream (res);
        Point p = getPoint (ts);
        ts.getToken();
        rawComment = Util.extractString(lines, p, getPoint(ts));
        rawComment = Util.chopComment (rawComment);
        func.parseComment(rawComment);
        func.content = res.getContent();
        return func;
    }

    /**
     * reads a function file and creates independent DocFunction objects of type FUNCTION
     */
    public static DocFunction[] newFunctions(Resource res) throws IOException {
        return newFunctions(res, null);
    }


    /**
     * reads a function file and creates DocFunction objects of type FUNCTION
     * connected to another DocElement.
     */
    public static DocFunction[] newFunctions(Resource res, DocElement parent)
                throws IOException {

        Vector vec = new Vector();

            String content = res.getContent();
            String[] lines = StringUtils.splitLines(content);
            String comment = "";

            int token     = Token.EMPTY;
            int lastToken = Token.EMPTY;
            Point marker;

            String lastNameString = null;
            String functionName   = null;
            String context        = null;

            TokenStreamInjector ts = getTokenStream (content);

            while (!ts.eof()) {

                // store the position of the last token
                marker = getPoint (ts);
                // store last token
                lastToken = token;

                // now get a new token
                // regular expression syntax is troublesome for the TokenStream
                // we can safely ignore syntax errors in regular expressions here
                try {
                    token = ts.getToken();
                } catch(Exception anything) {
                    continue;
                }

                if (token == Token.EOL) {

                    String c = Util.extractString(lines, marker, getPoint(ts));
                    if (c.startsWith("/**"))
                        comment = c;

                } else if (token == Token.LC) {

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
                        marker = getPoint(ts);
                        marker.x -= (ts.getString().length() + 1);

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
                        marker = p;
                        marker.x -= 9;

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

                    // create the function object
                    DocFunction theFunction = newFunction (functionName, res, parent);
                    theFunction.parseComment (comment);
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
                        // regular expression syntax is troublesome for the TokenStream
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

                    theFunction.content = Util.extractString(lines, marker, getPoint(ts));
                    comment = "";

                } // end if
            } // end while


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
     * Creates a rhino token stream for a given file.
     * @param src the JS source, either as Resource or String object
     * @return a TokenStream wrapper
     * @throws IOException if an I/O exception was raised
     */
    protected static TokenStreamInjector getTokenStream (Object src) throws IOException {
        // TODO the TokenStreamInjector is really just a hack, and we shouldn't
        // interact with the rhino TokenStream class directly. The proper way to
        // go would be to use the public Parser class to parse the input, and walk
        // through the parse tree and extract the comments manually.
        // As a result of our approach, the TokenStream member in our Parser instance
        // will be null, resulting in a NullPointerException when an error is
        // encountered. For the time being, this is something we can live with.
        Reader reader = null;
        String content = null;
        if (src instanceof Resource) {
            reader = new InputStreamReader(((Resource) src).getInputStream());
        } else if (src instanceof String) {
            content = (String) src;
        } else {
            throw new IllegalArgumentException("src must be either a Resource or a String");
        }
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.initFromContext(Context.getCurrentContext());
        ErrorReporter errorReporter = Context.getCurrentContext().getErrorReporter();
        Parser parser = new Parser(compilerEnv, errorReporter);
        return new TokenStreamInjector (parser, reader, content, 0);
    }

    /**
     * Returns a pointer to the current position in the TokenStream
     * @param ts the TokenStream
     * @return the current position
     */
    protected static Point getPoint (TokenStreamInjector ts) {
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
