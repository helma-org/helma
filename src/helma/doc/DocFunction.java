package helma.doc;

import helma.framework.IPathElement;
import java.io.*;

public class DocFunction extends DocElement implements IPathElement	{
	
	public static final String[] typeSuffix = {"_action","_as_string","","_macro",""};

	private DocPrototype prototype;
	private String source;

	public DocFunction( String name, String location, int type, DocPrototype prototype, String rawComment ) throws DocException	{
		super(name,location, type);
		this.prototype = prototype;
		parseComment(rawComment);
	}

	public String getFullName()	{
		switch(type)	{
			case ACTION:     return ( "Action " + name );
			case TEMPLATE:   return ( "Template " + name );
			case FUNCTION:   return ( "Function " + name );
			case MACRO:      return ( "Macro " + name );
			case SKIN:       return ( "Skin " + name );
		}
		return ( "Method " + name );
	}

	public DocPrototype getDocPrototype()	{		return prototype;	}

	public void readSource(String sourceFile, int beginLine, int beginColumn, int endLine, int endColumn )	{
		StringBuffer buf = new StringBuffer ();
		int ct=0;
		try	{
			BufferedReader in = new BufferedReader(new FileReader(sourceFile));
			String line="";
 			while ( line!=null )	{
	 			line = in.readLine();
 				if ( line==null )	break;
 				ct++;
 				if ( ct==beginLine )
 					buf.append(line.substring(beginColumn-1, line.length())+"\n");
 				else if ( ct>beginLine && ct<endLine )
 					buf.append(line+"\n");
 				else if ( ct==endLine )
 					buf.append(line.substring(0,endColumn));
 			}
 		}	catch ( FileNotFoundException e )	{
	 	}	catch ( StringIndexOutOfBoundsException e )	{
 		}	catch ( IOException e )	{
			DocRun.log(e.getMessage());
	 	}
 		source = buf.toString();
	}

	public void readSource(String sourceFile)	{
		StringBuffer buf = new StringBuffer ();
		try	{
			BufferedReader in = new BufferedReader(new FileReader(sourceFile));
			String line="";
 			while ( line!=null )	{
	 			line = in.readLine();
 				if ( line==null )	break;
 				buf.append(line+"\n");
 			}
 		}	catch ( FileNotFoundException e )	{
 		}	catch ( IOException e )	{
			DocRun.log(e.getMessage());
	 	}
 		source = buf.toString();
	}

	public void setSource(String source)	{	this.source = source;	}
	public String getSource()				{	return (source!=null)?source:"";	}

	public String toString()	{
		return ( "[DocFunction " + getTypeName() + " " + name + "]" );
	}

	////////////////////////////////////
	// from helma.framework.IPathElement
	////////////////////////////////////
	
	public String getElementName()	{
		return getTypeName().toLowerCase()+"_"+name;
	}

	public IPathElement getChildElement(String name)	{
		return null;
	}

	public IPathElement getParentElement()	{
		return prototype;
	}

	public java.lang.String getPrototype()	{
		return "docfunction";
	}


}

