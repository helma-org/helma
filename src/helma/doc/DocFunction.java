package helma.doc;

import java.io.*;
import java.util.*;

import FESI.Parser.*;
import helma.framework.IPathElement;

public class DocFunction extends DocFileElement {

	/**
	  * creates a new independent DocFunction object of type ACTION
	  */
	public static DocFunction newAction (File location) {
		return newAction (location, null);
	}


	/**
	  * creates a new DocFunction object of type ACTION connected to another DocElement
	  */
	public static DocFunction newAction (File location, DocElement parent) {
		String name = nameFromFile (location, ".hac");
		DocFunction func = new DocFunction (name, location, parent, ACTION);
		func.parseActionFile ();
		return func;
	}		

	/**
	  * creates a new independent DocFunction object of type TEMPLATE
	  */
	public static DocFunction newTemplate (File location) {
		return newTemplate (location, null);
	}


	/**
	  * creates a new DocFunction object of type TEMPLATE connected to another DocElement
	  */
	public static DocFunction newTemplate (File location, DocElement parent) {
		String name = nameFromFile (location, ".hsp");
		DocFunction func = new DocFunction (name, location, parent, TEMPLATE);
		func.parseTemplateFile ();
		return func;
	}


	/**
	  * reads a function file and creates independent DocFunction objects of type FUNCTION
	  */
	public static DocFunction[] newFunctions (File location) {
		return newFunctions (location, null);
	}


	/**
	  * reads a function file and creates DocFunction objects of type FUNCTION
	  * connected to another DocElement.
	  */
	public static DocFunction[] newFunctions (File location, DocElement parent) {
		Vector vec = new Vector ();
		EcmaScriptTokenManager mgr = createTokenManager(location);
		Token tok = mgr.getNextToken();
		while (tok.kind!=0) {
			if (tok.kind == EcmaScriptConstants.FUNCTION) {
				// store the start position of the function:
				int beginLine   = tok.beginLine;
				int beginColumn = tok.beginColumn;
				// the name is stored in the next token:
				String funcName = mgr.getNextToken().toString();
				// create the function object
				DocFunction func;
				if (funcName.endsWith("_action"))
					func = new DocFunction (funcName, location, parent, ACTION);
				else if (funcName.endsWith("_macro"))
					func = new DocFunction (funcName, location, parent, MACRO);
				else
					func = new DocFunction (funcName, location, parent, FUNCTION);
				// parse the comment from the special token(s) before this token:
				func.parseCommentFromToken (tok);
				// find the parameters of this function, but only if it's
				// neither macro nor action:
				if (func.type==FUNCTION) {
					while (tok.kind!=0 && tok.kind!=EcmaScriptConstants.RPAREN) {
						if (tok.kind==EcmaScriptConstants.IDENTIFIER) {
							func.addParameter (tok.image);
						}
						tok = mgr.getNextToken ();
					}
				} else {
					tok = mgr.getNextToken ();
				}
				// now find the end of the function:
				int endLine=0, endColumn=0;
				while (tok.kind!=0 && tok.kind!=EcmaScriptConstants.FUNCTION)	{
					endLine = tok.endLine;
					endColumn = tok.endColumn;
					tok = mgr.getNextToken ();
				}
				// now we know the exact position of the function in the file,
				// re-read it and extract the source code:
				func.parseSource (location, beginLine, beginColumn, endLine, endColumn);
				vec.add (func);
			}
			if (tok.kind != EcmaScriptConstants.FUNCTION) {
				tok = mgr.getNextToken();
			}
		}
		return (DocFunction[]) vec.toArray (new DocFunction[0]);
	}

	protected DocFunction (String name, File location, DocElement parent, int type) {
		super (name, location, type);
		this.parent = parent;
	}


	/**
	  * reads the content of a .hac file and parses the comment before the first 
	  * javascript element
	  */
	private void parseActionFile () {
		EcmaScriptTokenManager mgr = createTokenManager(location);
		Token tok = mgr.getNextToken();
		parseCommentFromToken (tok);
		content = readFile (location);
	}


	/**
	  * reads the content of a .hsp file and parses the comment before the first 
	  * javascript element (only if file starts with &gt;%-tag!).
	  */
	private void parseTemplateFile () {
		content = readFile (location);
		StringReader str = new StringReader (content.substring (content.indexOf("<%") + 2, content.indexOf ("%>")));
		ASCII_CharStream ascii = new ASCII_CharStream (str,1,1);
		EcmaScriptTokenManager mgr = new EcmaScriptTokenManager (ascii);
		Token tok = mgr.getNextToken();
		parseCommentFromToken (tok);
	}


	/**
	  * from helma.framework.IPathElement. All macros, templates, actions etc
	  * have the same prototype.
	  */
	public java.lang.String getPrototype ()	{
		return "docfunction";
	}


}

