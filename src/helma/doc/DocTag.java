package helma.doc;

import java.util.*;

public class DocTag	{

	public static final int ARG			= 0;
	public static final int PARAM		= 1;
	public static final int RETURNS     = 2;
	public static final int AUTHOR   	= 3;
	public static final int VERSION		= 4;
	public static final int RELEASE		= 5;
	public static final int SEE			= 6;

	/** number of different tag-types	**/	
	public static final int TYPE_COUNT = 7;

	/**	constants are used as array indices!	**/
	public static final String[] kindNames  = {"arg","param","return","author","version","release","see"};
	public static final String[] kindDesc   = {"Arguments","Parameter","Returns","Author","Version","Release","See also"};

	private String	name;
	private int		kind;
	private String	text;
	
	public DocTag( String rawTag ) throws DocException	{
		//DocRun.log(rawTag);
		try	{
			StringTokenizer tok = new StringTokenizer(rawTag);
			String kindstr = tok.nextToken().toLowerCase();
			for ( int i=0; i<kindNames.length; i++ )	{
				if ( kindstr.equals(kindNames[i]) )	{
					kind = i;
					break;
				}
			}
			StringBuffer buf = new StringBuffer();
			if ( kind==PARAM )	{
				name = tok.nextToken();
			}
			while ( tok.hasMoreTokens() )	{
				buf.append( tok.nextToken() + " " );
			}
			text = buf.toString();
		}	catch ( Exception e )	{	}
	}

	public String getName()	{		return (name!=null && !name.equals("null"))?name:"";	}
	public int    getKind()	{		return kind;	}
	public String getText()	{		return (text!=null && !text.equals("null"))?text:"";	}

	public String toString()	{
		return ( (name!=null)?name+" ":"" ) + text;
	}


}

