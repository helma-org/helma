// Action.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.Vector;
import java.util.Iterator;
import java.io.*;
import helma.framework.*;
import helma.util.Updatable;
import FESI.Data.*;
import FESI.Parser.*;
import FESI.AST.ASTFormalParameterList;
import FESI.AST.ASTStatementList;
import FESI.AST.EcmaScriptTreeConstants;
import FESI.Interpreter.*;
import FESI.Exceptions.*;



/**
 * An Action is a JavaScript function that is exposed as a URI. It is 
 * usually represented by a file with extension .hac (hop action file)
 * that contains the pure JavaScript body of the function. 
 */


public class Action implements Updatable {

    String name;
    String functionName;
    Prototype prototype;
    Application app;
    File file;
    long lastmod;

    // this is the parsed function which can be easily applied to RequestEvaluator objects
    TypeUpdater pfunc;


    public Action (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.app;
	this.name = name;
	this.file = file;
	if (file != null)
	    update ();
    }

    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate () {
	return lastmod != file.lastModified () || !file.exists ();
    }


    public void update () {

	if (!file.exists ()) {
	    // remove functions declared by this from all object prototypes
	    remove ();
	} else {
	    try {
	        FileReader reader = new FileReader (file);
	        char cbuf[] = new char[(int) file.length ()];
	        reader.read (cbuf);
	        reader.close ();
	        String content = new String (cbuf);
	        update (content);
	    } catch (IOException filex) {
	        app.logEvent ("*** Error reading action file "+file+": "+filex);
	    }
	
	    lastmod = file.lastModified ();
	}
    }


    public void update (String content) throws Exception {
	// app.logEvent ("Reading text template " + name);

	functionName = name+"_action";

             try {
	    pfunc = parseFunction (functionName, "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10", content);
             } catch (Throwable x) {
                 String message = x.getMessage ();
                 pfunc =  new ErrorFeedback (functionName, message);
             }

	Iterator evals = app.typemgr.getRegisteredRequestEvaluators ();
	while (evals.hasNext ()) {
	    try {
	        RequestEvaluator reval = (RequestEvaluator) evals.next ();
	        updateRequestEvaluator (reval);
	    } catch (Exception ignore) {}
	}
    }

    void remove () {
	prototype.actions.remove (name);
	prototype.updatables.remove (file.getName());

	Iterator evals = app.typemgr.getRegisteredRequestEvaluators ();
	while (evals.hasNext ()) {
	    try {
	        RequestEvaluator reval = (RequestEvaluator) evals.next ();
	        ObjectPrototype op = reval.getPrototype (prototype.getName());
	        functionName = name+"_action";
	        ESValue esv = (ESValue) op.getProperty (functionName, functionName.hashCode());
	        if (esv instanceof ConstructedFunctionObject || esv instanceof ThrowException) {
	            op.deleteProperty (functionName, functionName.hashCode());
	        }
	    } catch (Exception ignore) {}
	}
    }

    public String getName () {
	return name;
    }

    public String getFunctionName () {
	return functionName;
    }

    public String toString () {
	return prototype.getName()+"/"+file.getName();
    }

    public synchronized void updateRequestEvaluator (RequestEvaluator reval) throws EcmaScriptException {
        if (pfunc != null)
            pfunc.updateRequestEvaluator (reval);
    }

    protected TypeUpdater parseFunction (String funcname, String params, String body) throws EcmaScriptException {

        // ESObject fp = app.eval.evaluator.getFunctionPrototype();
        // ConstructedFunctionObject function = null;
        ASTFormalParameterList fpl = null;
        ASTStatementList sl = null;
        FunctionEvaluationSource fes = null;

        if (body == null || "".equals (body.trim()))
            body = ";\r\n";
        else
            body = body + "\r\n";
        if (params == null) params = "";
        else params = params.trim ();

        String fulltext = "function "+funcname+" (" + params + ") {\n" + body + "\n}";

        EcmaScript parser;
        StringReader is;

        // Special case for empty parameters
        if (params.length()==0) {
            fpl = new ASTFormalParameterList(EcmaScriptTreeConstants.JJTFORMALPARAMETERLIST);
        } else {
            is = new java.io.StringReader(params);
            parser = new EcmaScript(is);
            try {
                fpl = (ASTFormalParameterList) parser.FormalParameterList();
                is.close();
            } catch (ParseException x) {
                throw new EcmaScriptParseException (x, new StringEvaluationSource(fulltext, null));
            }
        }
        // this is very very very strange: without the toString, lots of obscure exceptions
        // deep inside the parser...
        is = new java.io.StringReader(body.toString ());
        try {
            parser = new EcmaScript (is);
            sl = (ASTStatementList) parser.StatementList();
            is.close();
        } catch (ParseException x) {
            app.logEvent ("Error parsing file "+app.getName()+":"+prototype.getName()+"/"+file.getName()+": "+x);
            throw new EcmaScriptParseException (x, new StringEvaluationSource(fulltext, null));
        } catch (Exception x) {
            app.logEvent ("Error parsing file "+app.getName()+":"+prototype.getName()+"/"+file.getName()+": "+x);
            throw new RuntimeException (x.getMessage ());
        }

        fes = new FunctionEvaluationSource (new StringEvaluationSource(fulltext, null), funcname);
        return new ParsedFunction (fpl, sl, fes, fulltext, funcname);
    }

    class ParsedFunction implements TypeUpdater {

        ASTFormalParameterList fpl = null;
        ASTStatementList sl = null;
        FunctionEvaluationSource fes = null;
        String fullFunctionText = null;
        String functionName;

        public ParsedFunction (ASTFormalParameterList fpl, ASTStatementList sl, FunctionEvaluationSource fes,
		String fullFunctionText, String functionName) {
	this.fpl = fpl;
	this.sl = sl;
	this.fes = fes;
	this.fullFunctionText = fullFunctionText;
	this.functionName = functionName;
        }

        public void updateRequestEvaluator (RequestEvaluator reval) throws EcmaScriptException {

	ObjectPrototype op = reval.getPrototype (prototype.getName());

	EcmaScriptVariableVisitor vdvisitor = reval.evaluator.getVarDeclarationVisitor();
	Vector vnames = vdvisitor.processVariableDeclarations(sl, fes);

	FunctionPrototype fp = ConstructedFunctionObject.makeNewConstructedFunction (
		reval.evaluator, functionName, fes,
		fullFunctionText, fpl.getArguments(), vnames, sl);
	op.putHiddenProperty (functionName, fp);
        }

    }

    class ErrorFeedback implements TypeUpdater {

        String functionName, errorMessage;

        public ErrorFeedback (String fname, String msg) {
            functionName = fname;
            errorMessage = msg;
        }

        public void updateRequestEvaluator (RequestEvaluator reval) throws EcmaScriptException {

            ObjectPrototype op = reval.getPrototype (prototype.getName ());

            FunctionPrototype fp = (FunctionPrototype) reval.evaluator.getFunctionPrototype ();
            FunctionPrototype func = new ThrowException (functionName, reval.evaluator, fp, errorMessage);
            op.putHiddenProperty (functionName, func);

        }
    }

    class ThrowException extends BuiltinFunctionObject {
        String message;
        ThrowException (String name, Evaluator evaluator, FunctionPrototype fp, String message) {
            super (fp, evaluator, name, 1);
            this.message = message == null ? "No error message available" : message;
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            throw new EcmaScriptException (message);
        }
        public ESObject doConstruct (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            throw new EcmaScriptException (message);
        }
    }

    interface TypeUpdater {
        public void updateRequestEvaluator (RequestEvaluator reval) throws EcmaScriptException;
    }
}
































