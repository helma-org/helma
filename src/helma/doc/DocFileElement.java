package helma.doc;

import java.io.*;
import java.util.*;

import FESI.Parser.*;

public abstract class DocFileElement extends DocElement {

	/**
	  * extracts the function name from a file. basically chops the given suffix
	  * and throws an error if the file name doesn't fit.
	  */
	static protected String nameFromFile (File f, String suffix) throws DocException {
		String filename = f.getName ();
		if (!filename.endsWith (suffix))
			throw new DocException ("file doesn't have suffix " + suffix + ": " + f.toString());
		return filename.substring (0, filename.lastIndexOf(suffix));
	}


	/**
	  * creates fesi token manager for a given file.
	  */
	static protected EcmaScriptTokenManager createTokenManager (File f)	{
	    try	{
	    	ASCII_CharStream is = new ASCII_CharStream(new FileReader(f), 1, 1);
			EcmaScriptTokenManager mgr = new EcmaScriptTokenManager(is,0);
			return mgr;
		}	catch (FileNotFoundException fnfe)	{
			fnfe.printStackTrace ();
			throw new DocException (fnfe.toString ());
		}
	}


	protected DocFileElement (String name, File location, int type) {
		super (name, location, type);
	}


	/**
	  * extracts a part of the source code, used to get the code for a
	  * single function from a function file. sets the field "content".
	  */
	protected void parseSource (File sourceFile, int beginLine, int beginColumn, int endLine, int endColumn)	{
		StringBuffer buf = new StringBuffer ();
		int ct=0;
		try	{
			BufferedReader in = new BufferedReader (new FileReader (sourceFile));
			String line="";
 			while (line!=null)	{
	 			line = in.readLine();
 				if (line==null)	break;
 				ct++;
 				if (ct==beginLine)
 					buf.append (line.substring(beginColumn-1, line.length())+"\n");
 				else if ( ct>beginLine && ct<endLine )
 					buf.append (line+"\n");
 				else if (ct==endLine)
 					buf.append (line.substring(0,endColumn));
 			}
 		}	catch (Exception e)	{
			debug (e.getMessage ());
	 	}
 		content = buf.toString ();
	}


	/**
	  * connects all available specialTokens starting at the given token.
	  */
	protected void parseCommentFromToken (Token tok)	{
		StringBuffer buf = new StringBuffer();
		while (tok.specialToken!=null) {
			buf.append (tok.specialToken.toString() );
			tok = tok.specialToken;
		}
		parseComment (buf.toString().trim());
	}


}

