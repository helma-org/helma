package helma.doc;

import helma.framework.IPathElement;
import java.io.*;
import java.util.*;
import FESI.Parser.*;


public class DocPrototype extends DocElement implements IPathElement	{

	DocFunction[] functions;
	DocApplication app;

	public DocPrototype( String name, String location, DocApplication app ) throws DocException	{
		super( name, location, PROTOTYPE );
		this.app = app;
		if ( name.equals("hopobject")==false && name.equals("global")==false )
			readPropertiesFile( new File(location,"type.properties").getAbsolutePath());
		checkCommentFile();
		readFunctions();
	}

	public String getFullName()				{		return ( "Prototype " + name );	}

	/**	return number of functions	*/
	public int countFunctions()			{		return functions.length;	}

	/** return array of functions of a special type	*/
	public int countFunctions(int type)	{
		int ct = 0;
		for ( int i=0; i<functions.length; i++ )	{
			if ( functions[i].getType()==type )	{
				ct++;
			}
		}
		return ct;
	}

	/**	return a single function	*/
	public DocFunction getFunction(String name)	{
		for ( int i=0; i<functions.length; i++ )	{
			if ( functions[i].getName().equals(name) )
				return functions[i];
		}
		return null;
	}

	/** return array of functions	*/
	public DocFunction[] listFunctions()	{
		return functions;
	}

	/** return array of functions of a special type	*/
	public DocFunction[] listFunctions(int type)	{
		Vector funcVec = new Vector();
		for ( int i=0; i<functions.length; i++ )	{
			if ( functions[i].getType()==type )	{
				funcVec.addElement(functions[i]);
			}
		}
		return (DocFunction[])funcVec.toArray(new DocFunction[funcVec.size()]);;
	}


	/** return the application	*/
	public DocApplication getApplication()		{		return app;		}


	/** create the function list & read all valid files	*/
	private void readFunctions()	{
		DocRun.debug("parsing Prototype " + name + " using " + location );
		String files[] = (new File(location)).list();
		Vector funcVec = new Vector();;
		for ( int i=0; i<files.length; i++ )	{
		DocRun.debug("reading " + files[i] );
			try	{
				if ( files[i].endsWith(DocRun.actionExtension) )	{
					readAction(new File(location,files[i]).getAbsolutePath(),funcVec);
				}	else if ( files[i].endsWith(DocRun.scriptExtension) )	{
					readFunctionFile(new File(location,files[i]).getAbsolutePath(),funcVec);
				}	else if ( files[i].endsWith(DocRun.templateExtension) )	{
					readTemplate(new File(location,files[i]).getAbsolutePath(),funcVec);
				}	else if ( files[i].endsWith(DocRun.skinExtension) )	{
					readSkin(new File(location,files[i]).getAbsolutePath(),funcVec);
				}
			}	catch ( DocException e )	{
				DocRun.log ( "couldn't read function " + name + "." + files[i] + ": " + e.getMessage() );
			}
		}
		functions = (DocFunction[])funcVec.toArray(new DocFunction[funcVec.size()]);
		DocRun.debug( name + " has " + functions.length + " functions" );
		Arrays.sort(functions,new DocComparator(this));
	}

	private void readAction(String f, Vector funcVec) throws DocException	{
		EcmaScriptTokenManager mgr = createTokenManager(new File(f));
		Token tok = mgr.getNextToken();
		DocFunction func = new DocFunction( fileToFuncName(f), f, DocElement.ACTION, this, getCommentFromToken(tok) );
		func.readSource(f);
		funcVec.addElement( func );
	}

	private void readFunctionFile(String f, Vector funcVec) throws DocException	{
		EcmaScriptTokenManager mgr = createTokenManager(new File(f));
		Token tok = mgr.getNextToken();
		while( tok.kind!=0 )	{
			if( tok.kind == EcmaScriptConstants.FUNCTION )	{
				int beginLine   = tok.beginLine;
				int beginColumn = tok.beginColumn;
				String funcName = mgr.getNextToken().toString();
				DocFunction func;
				if ( funcName.endsWith("_macro") )
					func = new DocFunction( funcName, f, DocElement.MACRO, this, getCommentFromToken(tok) );
				else
					func = new DocFunction( funcName, f, DocElement.FUNCTION, this, getCommentFromToken(tok) );
				funcVec.addElement(func);

				tok = mgr.getNextToken();
				int endLine=0, endColumn=0;
				while( tok.kind!=0 && tok.kind!=EcmaScriptConstants.FUNCTION )	{
					endLine = tok.endLine;
					endColumn = tok.endColumn;
					tok = mgr.getNextToken();
				}
				func.readSource(f,beginLine,beginColumn,endLine,endColumn);
				//DocRun.log("going from " + beginLine +"." + beginColumn + " to " + endLine + "." + endColumn );
				//DocRun.log("============================here starts a new one");
			}
			if ( tok.kind!=0 && tok.kind!=EcmaScriptConstants.FUNCTION )
				tok = mgr.getNextToken();
		}
	}

	private void readTemplate(String f, Vector funcVec) throws DocException	{
		String content = readAFile(f);
		// get the first scripting zone and tokenize it
		DocFunction func;
		try	{
			StringReader str = new StringReader( content.substring(content.indexOf("<%")+2,content.indexOf("%>")) );
			EcmaScriptTokenManager mgr = new EcmaScriptTokenManager(new ASCII_CharStream(str,1,1),0);
			Token tok = mgr.getNextToken();
			func = new DocFunction( fileToFuncName(f), f, DocElement.TEMPLATE, this, getCommentFromToken(tok) );
		}	catch ( IndexOutOfBoundsException e )	{
			func = new DocFunction( fileToFuncName(f), f, DocElement.TEMPLATE, this, "" );
		}
		func.setSource(content);
		funcVec.addElement(func);
	}

	private void readSkin(String f, Vector funcVec) throws DocException	{
		// can't be commented yet
		// simply add them
		DocFunction func = new DocFunction(fileToFuncName(f),f,DocElement.SKIN, this,"");
		func.readSource(f);
		funcVec.addElement(func);
	}


	/** create token manager for a file	  */
	private EcmaScriptTokenManager createTokenManager(File f)	{
	    try	{
	    	ASCII_CharStream is = new ASCII_CharStream(new FileReader(f), 1, 1);
			EcmaScriptTokenManager mgr = new EcmaScriptTokenManager(is,0);
			return mgr;
		}	catch ( FileNotFoundException shouldnotappear )	{	}
		return null;
	}

	/** connect all available specialTokens	  */
	private String getCommentFromToken(Token tok)	{
		StringBuffer buf = new StringBuffer();
		while( tok.specialToken!=null )	{
			buf.append(tok.specialToken.toString() );
			tok = tok.specialToken;
		}
		return ( buf.toString().trim() );
	}

	private String fileToFuncName(String f)	{
		return fileToFuncName(new File(f));
	}

	private String fileToFuncName(File f)	{
		String tmp = f.getName();
		return tmp.substring(0,tmp.indexOf("."));
	}

	public String toString()	{
		return ( "[DocPrototype " + name + "]" );
	}


	////////////////////////////////////
	// from helma.framework.IPathElement
	////////////////////////////////////
	
	public String getElementName()	{
		return name;
	}

	public IPathElement getChildElement(String name)	{
		for ( int i=0; i<functions.length; i++ )	{
			if ( name.equals( functions[i].getTypeName().toLowerCase()+"_"+functions[i].getName() ) )
				return functions[i];
		}
		return null;
	}

//		return getFunction(name);
//	}

	public IPathElement getParentElement()	{
		return app;
	}

	public java.lang.String getPrototype()	{
		return "docprototype";
	}

}

