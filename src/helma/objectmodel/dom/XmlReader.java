package helma.objectmodel.dom;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.Reader;

import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

import helma.objectmodel.INode;
import helma.objectmodel.db.DbKey;
import helma.objectmodel.db.ExternalizableVector;
import helma.objectmodel.db.Node;
import helma.objectmodel.db.NodeHandle;
import helma.objectmodel.db.NodeManager;
import helma.objectmodel.db.Property;

public class XmlReader implements XmlConstants	{

	private HashMap convertedNodes;
	private NodeManager nmgr = null;

	public XmlReader ()	{
	}
	
	public XmlReader (NodeManager nmgr)	{
		this.nmgr = nmgr;
	}

	/**
	  * main entry to read an xml-file.
	  */
	public INode read (File file, INode helmaNode) throws RuntimeException	{
		try	{
			return read (new FileInputStream(file), helmaNode);
		}	catch (FileNotFoundException notfound)	{
			System.err.println ("couldn't find xml-file: " + file.getAbsolutePath ());
			return helmaNode;
		}
	}

	/**
	  * read an InputStream with xml-content.
	  */
	public INode read (InputStream in, INode helmaNode) throws RuntimeException	{
		return read (new InputSource (in), helmaNode);
	}

	/**
	  * read an character reader with xml-content.
	  */
	public INode read (Reader in, INode helmaNode) throws RuntimeException	{
		return read (new InputSource (in), helmaNode);
	}

	/**
	  * read an InputSource with xml-content.
	  */
	public INode read (InputSource in, INode helmaNode) throws RuntimeException	{
		if (helmaNode==null && nmgr==null)
			throw new RuntimeException ("can't create a new Node without a NodeManager");
		Document document = XmlUtil.parse (in);
		Element element = XmlUtil.getFirstElement(document);
		if (element==null)
			throw new RuntimeException ("corrupted xml-file");

		if (helmaNode==null)	{
			return convert (element);
		}	else	{
			convertedNodes = new HashMap ();
			INode convertedNode = convert (element, helmaNode);
			convertedNodes = null;
			return convertedNode;
		}
	}

	/**
	  * convert children of an Element to a given helmaNode
	  */
	public INode convert (Element element, INode helmaNode)	{
		String idref = element.getAttribute("idref");
		String key = idref + "-" + element.getAttribute("prototyperef");
		if( idref!=null && !idref.equals("") )	{
			if( convertedNodes.containsKey(key) )	{
				return (INode)convertedNodes.get(key);
			}
		}
		key = element.getAttribute("id") + "-" + element.getAttribute("prototype");
		convertedNodes.put( key, helmaNode );
		String prototype = element.getAttribute("prototype");
		if( !prototype.equals("") && !prototype.equals("hopobject") )	{
			helmaNode.setPrototype( prototype );
		}
		children(helmaNode, element);
		return helmaNode;
	}


	// used by convert(Element,INode)
	private INode children( INode helmaNode, Element element )	{
		NodeList list = element.getChildNodes();
		int len = list.getLength();
		Element childElement;
		for ( int i=0; i<len; i++ )	{
			try	{
				childElement = (Element)list.item(i);
			}	catch( ClassCastException e )	{
				continue;		// ignore CDATA, comments etc
			}
			INode workNode = null;

			if ( childElement.getTagName().equals("hop:child") )	{

				convert (childElement, helmaNode.createNode(null));

			}	else if ( !"".equals(childElement.getAttribute("id")) || !"".equals(childElement.getAttribute("idref")) )	{

				String childTagName = childElement.getTagName();
				INode newNode = convert (childElement, helmaNode.createNode (childTagName));
				helmaNode.setNode (childTagName, newNode);

			}	else	{

				String type = childElement.getAttribute("type");
				String key  = childElement.getTagName();
				String content = XmlUtil.getTextContent(childElement);
				if ( type.equals("boolean") )	{
					if ( content.equals("true") )	{
						helmaNode.setBoolean(key,true);
					}	else	{
						helmaNode.setBoolean(key,false);
					}
				}	else if ( type.equals("date") )	{
					SimpleDateFormat format = new SimpleDateFormat ( DATEFORMAT );
					try	{
						Date date = format.parse(content);
						helmaNode.setDate(key, date);
					}	catch ( ParseException e )	{
						helmaNode.setString(key,content);
					}
				}	else if ( type.equals("float") )	{
					helmaNode.setFloat(key, (new Double(content)).doubleValue() );
				}	else if ( type.equals("integer") )	{
					helmaNode.setInteger(key, (new Long(content)).longValue() );
				}	else {
					helmaNode.setString(key,content);
				}
			}
		}
		return helmaNode;
	}


	/**
	  * This is a basic de-serialization method for XML-2-Node conversion.
	  * It reads a Node from a database-like file and should return a Node
	  * that matches exactly the one dumped to that file before.
	  * It only supports persistent-capable Nodes (from objectmodel.db-package).
	  */
	public helma.objectmodel.db.Node convert (Element element)	{
		// FIXME: this method should use Element.getAttributeNS():
		// FIXME: do we need the name value or is it retrieved through mappings anyway?
		String name = element.getAttribute("name");
// 		String name = null;
		String id = element.getAttribute("id");
		String prototype = element.getAttribute("prototype");
		if ( "".equals(prototype) )
			prototype = "hopobject";
		helma.objectmodel.db.Node helmaNode = null;
		try		{
			long created = Long.parseLong (element.getAttribute	("created"));
			long lastmodified = Long.parseLong (element.getAttribute	("lastModified"));
			helmaNode = new helma.objectmodel.db.Node (name,id,prototype,nmgr.safe,created,lastmodified);
		}	catch ( NumberFormatException e )	{
			helmaNode = new helma.objectmodel.db.Node (name,id,prototype,nmgr.safe);
		}

		// now loop through all child elements and retrieve properties/subnodes for this node.
		NodeList list = element.getChildNodes();
		int len = list.getLength();
		Hashtable propMap  = new Hashtable();
		List      subnodes = new ExternalizableVector();
		for ( int i=0; i<len; i++ )	{

			Element childElement;
			try	{
				childElement = (Element)list.item(i);
			}	catch( ClassCastException e )	{
				continue;		// ignore CDATA, comments etc
			}

			if ( childElement.getTagName().equals("hop:child") )	{
				// add a new NodeHandle, presume all IDs in this objectcache are unique,
				// a prerequisite for a simple internal database.
				subnodes.add (new NodeHandle (new DbKey(null,childElement.getAttribute("idref") ) ) );
				continue;
			}

			if ( childElement.getTagName().equals("hop:parent") )	{
				// add a NodeHandle to parent object
				helmaNode.setParentHandle (new NodeHandle (new DbKey(null,childElement.getAttribute("idref") ) ) );
				continue;
			}	

			// if we come until here, childelement is a property value
			Property prop = new Property (childElement.getTagName(), helmaNode);
			if ( !"".equals(childElement.getAttribute("id")) || !"".equals(childElement.getAttribute("idref")) )	{
				// we've got an object!
				String idref = childElement.getAttribute("idref");
				prop.setNodeHandle (new NodeHandle(new DbKey(null,idref)));
				
			}	else	{
				String type = childElement.getAttribute("type");
				String content = XmlUtil.getTextContent(childElement);
				if ( type.equals("boolean") )	{
					if ( content.equals("true") )	{
						prop.setBooleanValue(true);
					}	else	{
						prop.setBooleanValue(false);
					}
				}	else if ( type.equals("date") )	{
					SimpleDateFormat format = new SimpleDateFormat ( DATEFORMAT );
					try	{
						Date date = format.parse(content);
						prop.setDateValue (date);
					}	catch ( ParseException e )	{
						prop.setStringValue (content);
					}
				}	else if ( type.equals("float") )	{
					prop.setFloatValue ((new Double(content)).doubleValue());
				}	else if ( type.equals("integer") )	{
	    			prop.setIntegerValue ((new Long(content)).longValue());
				}	else {
	    			prop.setStringValue (content);
				}
			}
   			propMap.put (childElement.getTagName().toLowerCase(), prop);
		}
		if ( propMap.size()>0 )
			helmaNode.setPropMap (propMap);
		else
			helmaNode.setPropMap (null);
		if ( subnodes.size()>0 )
			helmaNode.setSubnodes (subnodes);
		else
			helmaNode.setSubnodes (null);
		return helmaNode;
	}

}

