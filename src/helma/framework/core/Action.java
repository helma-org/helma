// Action.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.framework.core;

import java.util.*;
import java.io.*;
import helma.framework.*;
import helma.objectmodel.IServer;
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


public class Action {

    String name;
    Prototype prototype;
    Application app;
    File file;
    long lastmod;


    ParsedFunction pfunc;


    public Action (File file, String name, Prototype proto) {
	this.prototype = proto;
	this.app = proto.app;
	this.name = name;
	this.file = file;
	update (file);
    }


    public void update (File f) {

	this.file = f;
	long fmod = file.lastModified ();
	if (lastmod == fmod)
	    return;

	try {
	    FileReader reader = new FileReader (file);
	    char cbuf[] = new char[(int) file.length ()];
	    reader.read (cbuf);
	    reader.close ();
	    String content = new String (cbuf);
	    update (content);
	} catch (Exception filex) {
	    IServer.getLogger().log ("*** Error reading action file "+file+": "+filex);
	}
	lastmod = fmod;
    }

    

    public void update (String content) throws Exception {
	// IServer.getLogger().log ("Reading text template " + name);

	String fname = name+"_hop_action";

             try {
	    pfunc = parseFunction (fname, "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10", content);
             } catch (Exception x) {
                 String message = x.getMessage ();
                 app.typemgr.generateErrorFeedback (fname, message, prototype.getName ());
             }

   }


    public String getName () {
	return name;
    }

    public String getFunctionName () {
	return pfunc.functionName;
    }

    protected ParsedFunction parseFunction (String funcname, String params, String body) throws EcmaScriptException {

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
            IServer.getLogger().log ("Error parsing file "+app.getName()+":"+prototype.getName()+"/"+file.getName()+": "+x);
            throw new EcmaScriptParseException (x, new StringEvaluationSource(fulltext, null));
        } catch (Exception x) {
            IServer.getLogger().log ("Error parsing file "+app.getName()+":"+prototype.getName()+"/"+file.getName()+": "+x);
            throw new RuntimeException (x.getMessage ());
        }

        fes = new FunctionEvaluationSource (new StringEvaluationSource(fulltext, null), funcname);
        return new ParsedFunction (fpl, sl, fes, fulltext, funcname);
    }

    public void updateRequestEvaluator (RequestEvaluator reval) throws EcmaScriptException {
        if (pfunc != null)
            pfunc.updateRequestEvaluator (reval);
    }

    class ParsedFunction {

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
}
































