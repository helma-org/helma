package helma.doc;

import java.io.*;
import java.util.*;

public abstract class DocElement	{

	// identifiers of this element
	String	name;
	int		type;
	String	location;

	// comment-content
	DocTag	tags[];
	String	comment;

	public static final int DOCROOT    	= 0;
	public static final int APPLICATION	= 1;
	public static final int PROTOTYPE	= 2;
	public static final int METHOD		= 3;
	public static final int ACTION		= 4;
	public static final int TEMPLATE	= 5;
	public static final int FUNCTION	= 6;
	public static final int MACRO   	= 7;
	public static final int SKIN    	= 8;
	public static final int PROPERTY	= 9;	// to be implemented

	public static final String[] typeNames  = {"DocRoot","Application","Prototype","Method","Action","Template","Function","Macro","Skin","Property"};

	/** a default file that is read as comment for applications and prototypes if found in their directories	*/
	public static final String DOCFILENAME = "doc.html";

	public DocElement(String name, String location, int type) throws DocException	{
		if ( (new File(location)).exists()==false )
			throw new DocException( name + " not found in " + location );
		this.name = name;
		this.location = location;
		this.type = type;
	}

	public DocElement(String name, File location, int type) throws DocException	{
		if ( location.exists()==false )
			throw new DocException( name + " not found in " + location.toString() );
		this.name = name;
		this.location = location.getAbsolutePath();
		this.type = type;
	}

	/**	@return the name of the element	*/
	public String getName()	{
		return name;
	}

	abstract public String getFullName();
	
	/** @return absolute path to location of element (directory for apps and prototypes, file for methods)	*/
	public String getLocation()	{
		return location;
	}
	
	public boolean isApplication()	{	return (type==APPLICATION)?true:false;	}
	public boolean isPrototype()	{	return (type==PROTOTYPE)?true:false;	}
	public boolean isMethod()		{	if ( type>=METHOD && type<=SKIN )	return true;	else 	return false;	}
	public boolean isAction()		{	return (type==ACTION)?true:false;	}
	public boolean isTemplate()		{	return (type==TEMPLATE)?true:false;	}
	public boolean isFunction()		{	return (type==FUNCTION)?true:false;	}
	public boolean isMacro()		{	return (type==MACRO)?true:false;	}
	public boolean isSkin()			{	return (type==SKIN)?true:false;		}

	public int getType()			{	return type;	}
	public String getTypeName()		{	return typeNames[type];	}

	public String getDocFileName()	{	return (typeNames[type] + "_" + name).toLowerCase() + ".html";		}

	/**	@return the text of the comment */
	public String getComment()		{		return (comment!=null)?comment:"";	}

	public int countTags()	{		return countTags(-1);	}
	public int countTags(int kind)	{
		int ct=0;
		for ( int i=0; i<tags.length; i++ )	{
			if ( kind==-1 || tags[i].getKind()==kind )	{
				ct++;
			}
		}
		return ct;
	}

	public DocTag[] listTags()		{		return tags;	}
	/**	@return an array of tags that should get a special format
	  * expects a -1 if it should retrieve all tags!	*/
	public DocTag[] listTags(int kind)	{
		Vector vec = new Vector();
		for ( int i=0; i<tags.length; i++ )	{
			if ( kind==-1 || tags[i].getKind()==kind )	{
				vec.addElement(tags[i]);
			}
		}
		DocTag[] dt = (DocTag[])vec.toArray(new DocTag[vec.size()]);
		return dt;
	}

	/** parse rawComment, render DocTags      */
	void parseComment(String rawComment)	{
		try	{
			// get rid of java comment tags (delimiter "/**")
			int beg = rawComment.indexOf("/*");
			beg = ( beg<0 )	? beg=0 : beg+3;
			int end = rawComment.indexOf("*/");
			end = ( end<0 )	? rawComment.length()-1 : end;
			end = ( end<0 )	? 0 : end;
			rawComment = rawComment.substring(beg,end).trim();
			// separate comment from tags:
			StringBuffer commentBuf = new StringBuffer();
			StringBuffer tagBuf = new StringBuffer();
			boolean switched = false;
			StringTokenizer tok = new StringTokenizer(rawComment,"\n");
			while(tok.hasMoreTokens() )	{
				String line = tok.nextToken().trim();
				if ( line.startsWith("*") )
					line = line.substring(1).trim();
				if ( line.length()==0 )	continue;
				if ( line.startsWith("@") && switched==false )
					switched=true;
				if ( switched==true )
					tagBuf.append(line + "\n" );
				else
					commentBuf.append(line + "\n" );
			}
			comment = commentBuf.toString();
			// now create taglist:
			tok = new StringTokenizer(tagBuf.toString(),"@");
			tags = new DocTag[tok.countTokens()];
			int i = 0;
			while(tok.hasMoreTokens() )	{
				tags[i++] = new DocTag(tok.nextToken().trim());
			}
		}	catch ( DocException e ) 	{
			DocRun.log("parse error in " + location + ": " + e.getMessage() );
		}
	}

	/**	read properties file (app.props || type.props), format of comments has to be discussed!	*/
	static Properties readPropertiesFile(String filename)	throws DocException	{
		Properties props = new Properties();
		try	{
			props.load(new FileInputStream(filename));
		}	catch ( IOException e )	{
			DocRun.log("couldn't read file: " + e.getMessage() );
		}
		return props;
	}

	void checkCommentFile() throws DocException	{
		File f = new File (location, DOCFILENAME );
		if ( f.exists() )	{
			String rawComment = readAFile(f.getAbsolutePath());
			parseComment(rawComment);
		}	else	{
			comment = "";
			tags = new DocTag[0];
		}
	}

	/** read a complete file into a string	*/
	public static String readAFile(String str)	{
		StringBuffer buf = new StringBuffer();
		try	{
			BufferedReader in = new BufferedReader(new FileReader(new File(str)));
 			String line = in.readLine();
 			while(line!=null)	{
	 			buf.append(line+"\n");
	 			line = in.readLine();
 			}
 			return ( buf.toString() );
 		}	catch ( IOException e )	{
 			return ("");
 		}
 	}



}

