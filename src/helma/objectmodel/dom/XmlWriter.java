package helma.objectmodel.dom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import helma.objectmodel.*;
import helma.objectmodel.INode;
import helma.objectmodel.IProperty;
import helma.objectmodel.TransientNode;
import helma.objectmodel.db.Node;
import helma.objectmodel.db.DbMapping;
import helma.util.HtmlEncoder;

public class XmlWriter	extends OutputStreamWriter implements XmlConstants		{

	private final static String LINESEPARATOR = System.getProperty("line.separator");
	
	private Vector convertedNodes;
	private int maxLevels = 3;
	
	private String indent = "  ";
	private StringBuffer prefix = new StringBuffer();

	private static int fileid;
 	private SimpleDateFormat format = new SimpleDateFormat ( DATEFORMAT );

	private boolean dbmode = true;
	
	/**
	  * create ids that can be used for temporary files.
	  */
	public static int generateID()	{
		return fileid++;
	}

	/**
	  * empty constructor, will use System.out as outputstream.
	  */
	public XmlWriter ()	{
		super(System.out);
	}

	public XmlWriter (OutputStream out)	{
		super(out);
	}

	public XmlWriter (String desc) throws FileNotFoundException	{
		super (new FileOutputStream (desc));
	}

	public XmlWriter (File file) throws FileNotFoundException	{
		super (new FileOutputStream (file));
	}

	/**
	  * by default writing only descends 50 levels into the node tree to prevent
	  * infite loops. number can be changed here.
	  */
	public void setMaxLevels (int levels)	{
		maxLevels = levels;
	}

	public void setDatabaseMode (boolean dbmode)	{
		this.dbmode = dbmode;
	}

	/**
	  * set the number of space chars
	  */
	public void setIndent (int ct)	{
		StringBuffer tmp = new StringBuffer ();
		for ( int i=0; i<ct; i++ )	{
			tmp.append(" ");
		}
		indent = tmp.toString();
	}

	/**
	  * starting point for printing a node tree.
	  * creates document header too and initializes
	  * the cache of already converted nodes.
	  */
	public boolean write( INode node ) throws IOException	{
		convertedNodes = new Vector();
		writeln ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
		writeln ("<!-- printed by helma object publisher     -->");
		writeln ("<!-- created " + (new Date()).toString() + " -->" );
		write   ("<xmlroot xmlns:hop=\"");
		write   (NAMESPACE);
		writeln ("\">");
		write   (node,null,0);
		writeln ("</xmlroot>");
		convertedNodes = null;
		return true;
	}

	/**
	  * write a hopobject and print all its properties and children.
      * references are made here if a node already has been fully printed
      * or if this is the last level that's going to be dumped
	  */
	public void write (INode node, String name, int level) throws IOException	{
		if (node==null)
			return;
		prefix.append(indent);
		if ( ++level>maxLevels )	{
			writeReferenceTag (node, name);
			prefix = prefix.delete( prefix.length()-indent.length(), Integer.MAX_VALUE );
			return;
		}
		if ( convertedNodes.contains(node) )	{
			writeReferenceTag (node, name);
		}	else	{
			convertedNodes.addElement (node);
			writeTagOpen  (node,name);
			if ( node.getParent()!=null )	{
				writeReferenceTag  (node.getParent(),"hop:parent");
			}
			writeProperties (node,level);
			writeChildren (node,level);
			writeTagClose (node,name);
		}
		prefix = prefix.delete( prefix.length()-indent.length(), Integer.MAX_VALUE );
	}



	/**
	  * loop through properties and print them with their property-name
	  * as elementname
	  */
	private void writeProperties (INode node, int level) throws IOException	{
		Enumeration e = null;
		if ( dbmode==true && node instanceof helma.objectmodel.db.Node )	{
			// a newly constructed db.Node doesn't have a propMap,
			// but returns an enumeration of all it's db-mapped properties
			Hashtable props = ((Node)node).getPropMap();
			if (props==null)
				return;
			e = props.keys();
		}	else	{
			e = node.properties();
		}
		while ( e.hasMoreElements() )	{
			String key = (String)e.nextElement();
			IProperty prop = node.get(key,false);
			if ( prop!=null )	{
				int type = prop.getType();
				if( type==IProperty.NODE )	{
					write (node.getNode(key,false), key, level);
				}	else	{
					writeProperty (node.get(key,false));
				}
			}
		}
	}

	public void writeNullProperty (String key) throws IOException	{
		write (prefix.toString());
		write (indent);
		write ("<");
		write (key);
		write (" type=\"null\"/>");
		write (LINESEPARATOR);
	}	

	/**
	  * write a single property, set attribute type according to type, 
	  * apply xml-encoding.
	  */
	public void writeProperty (IProperty property) throws IOException	{
		write (prefix.toString());
		write (indent);
		write ("<");
		write (property.getName());
		switch (property.getType())	{
			case IProperty.BOOLEAN:
				write (" type=\"boolean\"");
				break;
			case IProperty.FLOAT:
				write (" type=\"float\"");
				break;
			case IProperty.INTEGER: 
				write (" type=\"integer\"");
				break;
		}
		if ( property.getType()==IProperty.DATE )	{
			write (" type=\"date\"");
			write (">");
			write ( format.format (property.getDateValue()) );
		}	else	{
			write (">");
			write ( HtmlEncoder.encodeXml (property.getStringValue()) );
		}
		write ("</");
		write (property.getName());
		write (">");
		write (LINESEPARATOR);
	}

	/**
	  * loop through the children-array and print them as <hop:child>
	  */
	private void writeChildren (INode node, int level) throws IOException	{
		if ( dbmode==true && node instanceof helma.objectmodel.db.Node )	{
			Node dbNode = (Node)node;
			DbMapping smap = dbNode.getDbMapping() == null ? null : dbNode.getDbMapping().getSubnodeMapping ();
			if (smap != null && smap.isRelational ())
				return;
		}
		Enumeration e = node.getSubnodes();
		while (e.hasMoreElements())	{
			INode nextNode = (INode)e.nextElement();
			write (nextNode, "hop:child", level);
		}
	}

	/**
	  * write an opening tag for a node. Include id and prototype, use a
	  * name if parameter is non-empty.
	  */
	public void writeTagOpen (INode node, String name) throws IOException	{
		write (prefix.toString());
		write ("<");
		write ( (name==null)?"hopobject" : name);
		write (" id=\"");
		write (getNodeIdentifier(node));
		write ("\" name=\"");
		write (node.getName());
		write ("\" prototype=\"");
		write (getNodePrototype(node));
		write ("\" created=\"");
		write (Long.toString(node.created()));
		write ("\" lastModified=\"");
		write (Long.toString(node.lastModified()));
		//FIXME: do we need anonymous-property?
		write ("\">");
		write (LINESEPARATOR);
	}

	/**
	  * write a closing tag for a node
	  * e.g. </root>
	  */
	public void writeTagClose (INode node, String name) throws IOException	{
		write (prefix.toString());
		write ("</");
		write ( (name==null)?"hopobject" : name);
		write (">");
		write (LINESEPARATOR);
	}

	/**
	  * write a tag holding a reference to an element that has
	  * been written out before.
	  * e.g. <parent idref="35" prototyperef="hopobject"/>
	  */
	public void writeReferenceTag (INode node, String name) throws IOException	{
		write (prefix.toString());
		write ("<");
		write ( (name==null)?"hopobject" : name);
		write ( " idref=\"");
		write (getNodeIdentifier(node));
		write ("\" prototyperef=\"");
		write (getNodePrototype(node));
		write ("\"");
		write ("/>");
		write (LINESEPARATOR);
	}

	/**
	  * retrieve prototype-string of a node, defaults to "hopobject"
	  */
	private String getNodePrototype( INode node )	{
		if ( node.getPrototype()==null || "".equals(node.getPrototype()) )	{
			return "hopobject";
		}	else	{
			return node.getPrototype();
		}
	}

	/**
	  * TransientNode produces a different ID each time we call the getID()-method
	  * this is a workaround and uses hashCode if INode stands for a TransientNode.
	  */	  
	private String getNodeIdentifier( INode node )	{
		try	{
			TransientNode tmp = (TransientNode)node;
			return Integer.toString( tmp.hashCode() );
		}	catch ( ClassCastException e )	{
			return node.getID();
		}
	}

	public void writeln(String str) throws IOException {
		write (str);
		write (LINESEPARATOR);
	}		

}

