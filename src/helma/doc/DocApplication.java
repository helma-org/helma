package helma.doc;

import helma.framework.IPathElement;
import helma.main.Server;
import java.io.*;
import java.util.*;

public class DocApplication extends DocElement implements IPathElement {
	
	private DocPrototype[] prototypes;

	/** read all prototypes
	  * @param name application to be documented
	  * @param appDir directory of this application	  */
	public DocApplication(String name, String appDir) throws DocException	{
		super( name, appDir, APPLICATION );
		checkCommentFile();
		readPrototypes();
	}
	
	public String getFullName()	{
		return ( "Application " + name );
	}

	/**	return number of prototypes	*/
	public int countPrototypes()	{
		return prototypes.length;
	}

	/**	return a single prototype	*/
	public DocPrototype getDocPrototype(String name)	{
		for ( int i=0; i<prototypes.length; i++ )	{
			if ( prototypes[i].getName().equalsIgnoreCase(name) )
				return prototypes[i];
		}
		return null;
	}

	/** return array of prototypes	*/
	public DocPrototype[] listPrototypes()	{
		return prototypes;
	}

	public DocFunction[] listFunctions()	{
		return listFunctions(-1);
	}

	public DocFunction[] listFunctions(int type)	{
		Vector funcVec = new Vector();
		for ( int i=0; i<prototypes.length; i++ )	{
			DocFunction[] tmp = prototypes[i].listFunctions();
			for ( int j=0; j<tmp.length; j++ )	{
				if ( type==-1 || tmp[j].getType()==type )
					funcVec.addElement(tmp[j]);
			}
		}
		DocFunction[] funcArr = (DocFunction[])funcVec.toArray(new DocFunction[funcVec.size()]);
		Arrays.sort(funcArr,new DocComparator(DocComparator.BY_NAME,this));
		return funcArr;
	}

	/** read prototypes, create them and make them parse their functions */
	private void readPrototypes()	{
		File d = new File ( location );
		String pt[] = d.list();
		Vector ptVec = new Vector();
		for ( int i=0; i<pt.length; i++ )	{
			File f = new File ( d.getAbsolutePath(), pt[i] );
			if ( f.isDirectory() && DocRun.prototypeAllowed(pt[i]) )	{
				try	{
					DocPrototype p = new DocPrototype(pt[i],f.getAbsolutePath(),this);
					ptVec.addElement(p);
					//break;
				}	catch ( DocException e )	{
					DocRun.log( "couldn't read prototype " + pt[i] + ": " + e.getMessage() );
				}
			}
		}
		prototypes = (DocPrototype[])ptVec.toArray(new DocPrototype[ptVec.size()]);
		Arrays.sort(prototypes,new DocComparator(this));
	}

	public String toString()	{
		return ( "[DocApplication " + name + "]" );
	}


	////////////////////////////////////
	// from helma.framework.IPathElement
	////////////////////////////////////
	
	public String getElementName()	{
		return "api";
	}

	public IPathElement getChildElement(String name)	{
		for( int i=0; i<prototypes.length; i++ )	{
			if ( prototypes[i].getElementName().equals(name) )	{
				return prototypes[i];
			}
		}
		return null;
	}

	public IPathElement getParentElement()	{
		// FIXME: Server.getServer() throws a NullPointerException from here ?
		Server s = helma.main.Server.getServer();
		return s.getChildElement(this.name);
	}

	public java.lang.String getPrototype()	{
		return "docapplication";
	}


}




