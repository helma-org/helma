package helma.objectmodel.dom;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import helma.objectmodel.*;
import helma.util.HtmlEncoder;

public class XmlWriter	extends OutputStreamWriter implements XmlConstants		{

	private final static String LINESEPARATOR = System.getProperty("line.separator");
	
	private Vector convertedNodes;
	private int maxLevels = 3;
	
	private String indent = "  ";
	private StringBuffer prefix = new StringBuffer();

	private static int fileid;
	
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
      * if node has already been fully printed, just make a reference here.
	  */
	public void write (INode node, String name, int level) throws IOException	{
		if ( ++level>maxLevels )
			return;
		prefix.append(indent);
		if ( convertedNodes.contains(node) )	{
			writeReferenceTag (node, name);
		}	else	{
			convertedNodes.addElement (node);
			writeTagOpen  (node,name);
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
		Enumeration e = node.properties();
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
		write (" hop:type=\"null\"/>");
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
				write (" hop:type=\"boolean\"");
				break;
			case IProperty.FLOAT:
				write (" hop:type=\"float\"");
				break;
			case IProperty.INTEGER: 
				write (" hop:type=\"integer\"");
				break;
		}
		if ( property.getType()==IProperty.DATE )	{
			write (" hop:type=\"date\"");
			SimpleDateFormat format = new SimpleDateFormat ( DATEFORMAT );
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
		write (" hop:id=\"");
		write (getNodeIdentifier(node));
		write ("\" hop:prototype=\"");
		write (getNodePrototype(node));
		write ("\"");
		write (">");
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
	  * been dumped before.
	  * e.g. <parent hop:idref="t35" hop:prototyperef="hopobject"/>
	  */
	public void writeReferenceTag (INode node, String name) throws IOException	{
		write (prefix.toString());
		write ("<");
		write ( (name==null)?"hopobject" : name);
		write ( " hop:idref=\"");
		write (getNodeIdentifier(node));
		write ("\" hop:prototyperef=\"");
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

