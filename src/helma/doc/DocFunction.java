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
import java.util.List;
import java.util.ArrayList;

import org.mozilla.javascript.*;

/**
 *
 */
public class DocFunction extends DocResourceElement {

    private int startLine;

    protected DocFunction(String name, Resource res, DocElement parent, int type, int lineno) {
        super(name, res, type);
        this.parent = parent;
        this.startLine = lineno;
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
        DocFunction func = new DocFunction(name, res, parent, ACTION, 1);
        String rawComment = "";
        Token[] tokens = parseTokens(res);
        rawComment = Util.extractString(lines, getPoint(tokens[0]), getPoint(tokens[1]));
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
        String[] lines = StringUtils.splitLines(res.getContent());
        Token[] tokens = parseTokens(res);
        List list = new ArrayList();
        scanFunctions(lines, tokens, list, res, parent, 0, tokens.length);
        return (DocFunction[]) list.toArray(new DocFunction[0]);
    }

    private static void scanFunctions(String[] lines, Token[] tokens, List list,
                                      Resource res, DocElement parent, int start, int end) {
        // Token token     = null;
        Token lastToken = new Token(Token.EMPTY, "", 0, 0);
        // Point marker;

        String lastNameString = null;
        String functionName   = null;
        String context        = null;
        String comment = "";

        for (int i = start; i  < end - 1; i++) {

            // store the position of the last token
            Point marker = getPoint(lastToken);
            // now get a new token
            Token token = tokens[i];
            // flag for dropping private functions
            boolean dropFunction = false;

            if (token.type == Token.EOL) {

                String c = Util.extractString(lines, marker, getPoint(token));
                if (c.startsWith("/**"))
                    comment = c;

            } else if (token.type == Token.LC) {

                // when we come across a left brace outside of a function,
                // we store the current string of the stream, it might be
                // a function object declaration
                // e.g. HttpClient = { func1:function()...}
                context = token.string;

            } else if (token.type == Token.RC && context != null) {

                // when we come across a right brace outside of a function,
                // we reset the current context cache
                context = null;

            } else if (token.type == Token.THIS) {

                if (parent instanceof DocFunction)
                    lastNameString = parent.getName() + ".prototype";
                // this may be the start of a name chain declaring a function
                // e.g. Number.prototype.functionName = function() { }
                // marker = getPoint(token);
                // marker.x -= (5);

            } else if (token.type == Token.NAME) {

                // store all names, the last one before a function
                // declaration may be used as its name

                if (lastToken.type != Token.DOT) {

                    lastNameString = token.string;

                    // this may be the start of a name chain declaring a function
                    // e.g. Number.prototype.functionName = function() { }
                    marker = getPoint(token);
                    marker.x -= (token.string.length() + 1);

                } else {

                    // token in front of the name was a dot, so we connect the
                    // names that way
                    lastNameString += "." + token.string;

                }

            } else if (token.type == Token.FUNCTION) {

                // store the end of the function word
                Point p = getPoint(token);

                // look at the next token:
                Token peekToken = tokens[i + 1];

                // depending of the style of the declaration we already have all we need
                // or need to fetch the name from the next token:
                if (peekToken.type == Token.NAME) {

                    // if the token after FUNCTION is NAME, it's the usual function
                    // declaration like this: function abc() {}

                    // set the pointer for the start of the actual function body
                    // to the letter f of the function word
                    marker = p;
                    marker.x -= 9;

                    // set stream to next token, so that name of the
                    // function is the stream's current string
                    token = tokens[++i];
                    functionName = token.string;
                } else {

                    // it's a different kind of function declaration.
                    // the function name is the last found NAME-token
                    // if context is set, prepend it to the function name
                    functionName = (context != null) ? context + "." + lastNameString : lastNameString;

                }

                DocFunction theFunction = null;
                if (!dropFunction) {
                    // create the function object
                    DocElement par = parent instanceof DocFunction ? parent.parent : parent;
                    theFunction = newFunction (functionName, res, par, token.lineno + 1);
                    theFunction.parseComment (comment);
                    list.add (theFunction);
                }
                // reset comment
                comment = "";

                // subloop on the tokenstream: find the parameters of a function
                while (i < end && token.type != Token.RP) {
                    token = tokens[++i];
                    if (token.type == Token.NAME && theFunction.type == FUNCTION) {
                        // add names of parameter only for functions, not for macros or actions
                        theFunction.addParameter (token.string);
                    }
                }

                // subloop on the tokenstream: find the closing right bracket of the function
                token = tokens[++i];
                int j = i + 1;
                int level = (token.type == Token.LC) ? 1 : 0;
                while (i < end && level > 0) {
                    // regular expression syntax is troublesome for the TokenStream
                    // we don't need them here, so we just ignore such an error
                    try {
                        token = tokens[++i];
                    } catch(Exception anything) {
                        continue;
                    }
                    if (token.type == Token.LC) {
                        level++;
                    } else if (token.type == Token.RC) {
                        level--;
                    }
                }

                if (dropFunction)
                    continue;

                // parse function body for nested functions
                scanFunctions(lines, tokens, list, res, theFunction, j, i);
                // set the function body, starting at the beginning of the first line
                marker.x = 0;
                theFunction.content = Util.extractString(lines, marker, getPoint(token));

            } // end if

            lastToken = token;

        } // end while

    }

    private static DocFunction newFunction (String funcName, Resource res, DocElement parent, int lineno) {
        if (funcName.endsWith("_action")) {
            return new DocFunction(funcName, res, parent, ACTION, lineno);
        } else if (funcName.endsWith("_macro")) {
            return new DocFunction(funcName, res, parent, MACRO, lineno);
        } else {
            return new DocFunction(funcName, res, parent, FUNCTION, lineno);
        }
    }

    /**
     * Creates a rhino token stream for a given file.
     * @param res the JS Resource
     * @return a TokenStream wrapper
     * @throws java.io.IOException if an I/O exception was raised
     */
    protected static Token[] parseTokens(Resource res) throws IOException {
        Reader reader = new InputStreamReader(res.getInputStream());
        CompilerEnvirons compilerEnv = new CompilerEnvirons();
        compilerEnv.initFromContext(Context.getCurrentContext());
        compilerEnv.setGenerateDebugInfo(true);
        compilerEnv.setGeneratingSource(true);
        compilerEnv.setOptimizationLevel(-1);
        ErrorReporter errorReporter = Context.getCurrentContext().getErrorReporter();
        Parser parser = new Parser(compilerEnv, errorReporter);
        return parser.parseTokens(reader, res.getName(),  0);
    }

    /**
     * Returns a pointer to the current position in the TokenStream
     * @param token the TokenStream
     * @return the current position
     */
    protected static Point getPoint (Token token) {
        return new Point (token.offset, token.lineno);
    }

    /**
     * from helma.framework.IPathElement. All macros, templates, actions etc
     * have the same prototype.
     */
    public java.lang.String getPrototype() {
        return "docfunction";
    }

    /**
     * Get the first line of this function within the containing resource.
     * @return the first line of the function
     */
    public int getStartLine() {
        return startLine;
    }
}
