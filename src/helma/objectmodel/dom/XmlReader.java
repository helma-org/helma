package helma.objectmodel.dom;

import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import helma.objectmodel.*;

public class XmlReader implements XmlConstants	{

	private int offset = 0;
	private HashMap convertedNodes;

	public XmlReader()	{
	}

	public INode read( String desc )	{
		return read(desc, new TransientNode() );
	}

	public INode read( String desc, INode helmaNode ) throws RuntimeException	{
		try	{
			return read( new File(desc), helmaNode );
		}	catch ( FileNotFoundException notfound )	{
			throw new RuntimeException( "couldn't find xml-file: " + desc );
		}	catch ( IOException ioerror )	{
			throw new RuntimeException( "couldn't read xml: " + desc );
		}
	}

	public INode read( File file, INode helmaNode ) throws RuntimeException, FileNotFoundException	{
		return read( new FileInputStream(file), helmaNode );
	}

	public INode read( InputStream in, INode helmaNode ) throws RuntimeException	{
		Document document = XmlUtil.parse(in);
		if ( document!=null && document.getDocumentElement()!=null )	{
			Node tmp = document.getDocumentElement().getFirstChild();
			Element workelement = null;
			while( tmp!=null )	{
				tmp = tmp.getNextSibling();
				if ( tmp.getNodeType()==Node.ELEMENT_NODE )	{
					workelement = (Element) tmp;
					break;
				}
			}
			return startConversion( helmaNode, workelement );
		}	else	{
			return helmaNode;
		}
	}

	public INode startConversion( INode helmaNode, Element element )	{
		convertedNodes = new HashMap();
		INode convertedNode = convert(helmaNode, element );
		convertedNodes = null;
		return convertedNode;
	}

	public INode convert( INode helmaNode, Element element )	{
		offset++;
		String idref = element.getAttributeNS(NAMESPACE, "idref");
		String key = idref + "-" + element.getAttributeNS(NAMESPACE, "prototyperef");
		if( idref!=null && !idref.equals("") )	{
			if( convertedNodes.containsKey(key) )	{
				offset--;
				return (INode)convertedNodes.get(key);
			}
		}
		key = element.getAttributeNS(NAMESPACE, "id") + "-" + element.getAttributeNS(NAMESPACE, "prototype");
		convertedNodes.put( key, helmaNode );

		// FIXME: import id on persistent nodes
		String prototype = element.getAttributeNS(NAMESPACE, "prototype");
		if( !prototype.equals("") && !prototype.equals("hopobject") )	{
			helmaNode.setPrototype( prototype );
		}
		children(helmaNode, element);
		offset--;
		return helmaNode;
	}


	private INode children( INode helmaNode, Element element )	{
		NodeList list = element.getChildNodes();
		int len = list.getLength();
		Element childElement;
		for ( int i=0; i<len; i++ )	{
			try	{
				childElement = (Element)list.item(i);
			}	catch( ClassCastException e )	{
				continue;
			}
			INode workNode = null;
			if ( childElement.getTagName().equals("hop:child") )	{
				convert( helmaNode.createNode(null), childElement );
			}	else if ( !"".equals(childElement.getAttributeNS(NAMESPACE,"id")) || !"".equals(childElement.getAttributeNS(NAMESPACE,"idref")) )	{
				// we've got an object!
				helmaNode.setNode( childElement.getTagName(), convert( helmaNode.createNode(childElement.getTagName()), childElement ) );
			}	else	{
				String type = childElement.getAttribute("hop:type");
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

	/** for testing */
	public static void main ( String args[] ) throws Exception	{
		try	{
			XmlReader x = new XmlReader ();
			INode node = x.read("test.xml");
		}	catch ( Exception e )	{
				System.out.println("exception " + e.toString() );
			throw new RuntimeException(e.toString());
		}
	}


	/** for testing */
	void debug(Object msg)	{
		for ( int i=0; i<offset; i++ )	{
			System.out.print("   ");
		}
		System.out.println(msg.toString());
	}


}

