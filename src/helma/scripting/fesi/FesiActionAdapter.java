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

package helma.scripting.fesi;

import FESI.AST.ASTFormalParameterList;
import FESI.AST.ASTStatementList;
import FESI.AST.EcmaScriptTreeConstants;
import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Interpreter.*;
import FESI.Parser.*;
import helma.framework.*;
import helma.framework.core.*;
import helma.scripting.*;
import helma.util.Updatable;
import java.io.*;
import java.util.Iterator;
import java.util.Vector;

/**
 *  An class that updates fesi interpreters with actionfiles and templates.
 */
public class FesiActionAdapter {
    Prototype prototype;
    Application app;
    String functionName;
    String sourceName;

    // this is the parsed function which can be easily applied to FesiEngine objects
    TypeUpdater pfunc;

    // this is the parsed function which can be easily applied to FesiEngine objects
    TypeUpdater pfuncAsString;

    /**
     * Creates a new FesiActionAdapter object.
     *
     * @param action ...
     */
    public FesiActionAdapter(ActionFile action) {
        prototype = action.getPrototype();
        app = action.getApplication();

        Reader reader = null;

        functionName = action.getFunctionName().replace('.', '_');
        sourceName = action.getSourceName();

        try {
            reader = action.getReader();
            pfunc = parseFunction(functionName,
                                  "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10",
                                  reader, sourceName);
        } catch (Throwable x) {
            String message = x.getMessage();

            pfunc = new ErrorFeedback(functionName, message);
        } finally {
            try {
                reader.close();
            } catch (Exception ignore) {
            }
        }

        // check if this is a template and we need to generate an "_as_string" variant
        if (action instanceof Template) {
            String content = "res.pushStringBuffer(); " + action.getContent() +
                             "\r\nreturn res.popStringBuffer();\r\n";

            try {
                pfuncAsString = parseFunction(functionName + "_as_string",
                                              "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10",
                                              new StringReader(content), sourceName);
            } catch (Throwable x) {
                String message = x.getMessage();

                pfunc = new ErrorFeedback(functionName + "_as_string", message);
            }
        } else {
            pfuncAsString = null;
        }
    }

    /**
     *
     *
     * @param engine ...
     *
     * @throws EcmaScriptException ...
     */
    public synchronized void updateEvaluator(FesiEngine engine)
                                      throws EcmaScriptException {
        if (pfunc != null) {
            pfunc.updateEvaluator(engine);
        }

        if (pfuncAsString != null) {
            pfuncAsString.updateEvaluator(engine);
        }
    }

    protected TypeUpdater parseFunction(String funcName, String params, Reader body,
                                        String sourceName)
                                 throws EcmaScriptException {
        // ESObject fp = app.eval.evaluator.getFunctionPrototype();
        // ConstructedFunctionObject function = null;
        ASTFormalParameterList fpl = null;
        ASTStatementList sl = null;
        FunctionEvaluationSource fes = null;

        /* if (body == null || "".equals (body.trim()))
           body = ";\r\n";
           else
               body = body + "\r\n"; */
        if (params == null) {
            params = "";
        } else {
            params = params.trim();
        }

        EcmaScript parser;
        StringReader is;

        // Special case for empty parameters
        if (params.length() == 0) {
            fpl = new ASTFormalParameterList(EcmaScriptTreeConstants.JJTFORMALPARAMETERLIST);
        } else {
            is = new java.io.StringReader(params);
            parser = new EcmaScript(is);

            try {
                fpl = (ASTFormalParameterList) parser.FormalParameterList();
                is.close();
            } catch (ParseException x) {
                throw new EcmaScriptParseException(x,
                                                   new FileEvaluationSource(sourceName,
                                                                            null));
            }
        }

        // this is very very very strange: without the toString, lots of obscure exceptions
        // deep inside the parser...
        // is = new java.io.StringReader(body.toString ());
        try {
            parser = new EcmaScript(body);
            sl = (ASTStatementList) parser.StatementList();
            body.close();
        } catch (ParseException x) {
            app.logEvent("Error parsing file " + app.getName() + ":" + sourceName + ": " +
                         x);
            throw new EcmaScriptParseException(x,
                                               new FileEvaluationSource(sourceName, null));
        } catch (Exception x) {
            app.logEvent("Error parsing file " + app.getName() + ":" + sourceName + ": " +
                         x);
            throw new RuntimeException(x.getMessage());
        }

        fes = new FunctionEvaluationSource(new FileEvaluationSource(sourceName, null),
                                           funcName);

        return new ParsedFunction(fpl, sl, fes, null, funcName);
    }

    interface TypeUpdater {
        public void updateEvaluator(FesiEngine engine)
                             throws EcmaScriptException;
    }

    class ParsedFunction implements TypeUpdater {
        ASTFormalParameterList fpl = null;
        ASTStatementList sl = null;
        FunctionEvaluationSource fes = null;
        String fullFunctionText = null;
        String functionName;

        public ParsedFunction(ASTFormalParameterList fpl, ASTStatementList sl,
                              FunctionEvaluationSource fes, String fullFunctionText,
                              String functionName) {
            this.fpl = fpl;
            this.sl = sl;
            this.fes = fes;
            this.fullFunctionText = fullFunctionText;
            this.functionName = functionName;
        }

        public void updateEvaluator(FesiEngine engine)
                             throws EcmaScriptException {
            ObjectPrototype op = engine.getPrototype(prototype.getName());

            EcmaScriptVariableVisitor vdvisitor = engine.evaluator.getVarDeclarationVisitor();
            Vector vnames = vdvisitor.processVariableDeclarations(sl, fes);

            FunctionPrototype fp = ConstructedFunctionObject.makeNewConstructedFunction(engine.evaluator,
                                                                                        functionName,
                                                                                        fes,
                                                                                        fullFunctionText,
                                                                                        fpl.getArguments(),
                                                                                        vnames,
                                                                                        sl);

            op.putHiddenProperty(functionName, fp);
        }
    }

    class ErrorFeedback implements TypeUpdater {
        String functionName;
        String errorMessage;

        public ErrorFeedback(String fname, String msg) {
            functionName = fname;
            errorMessage = msg;
        }

        public void updateEvaluator(FesiEngine engine)
                             throws EcmaScriptException {
            ObjectPrototype op = engine.getPrototype(prototype.getName());

            FunctionPrototype fp = (FunctionPrototype) engine.evaluator.getFunctionPrototype();
            FunctionPrototype func = new ThrowException(functionName, engine.evaluator,
                                                        fp, errorMessage);

            op.putHiddenProperty(functionName, func);
        }
    }

    class ThrowException extends BuiltinFunctionObject {
        String message;

        ThrowException(String name, Evaluator evaluator, FunctionPrototype fp,
                       String message) {
            super(fp, evaluator, name, 1);
            this.message = (message == null) ? "No error message available" : message;
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            throw new EcmaScriptException(message);
        }

        public ESObject doConstruct(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            throw new EcmaScriptException(message);
        }
    }
}
