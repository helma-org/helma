package helma.objectmodel.dom;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import helma.objectmodel.*;
import helma.util.SystemProperties;

public class XmlConverter implements XmlConstants {

    private boolean DEBUG = false;

    private boolean sparse = false;

    private Properties props;

    private char   defaultSeparator = '_';

    private int offset = 0;

    public XmlConverter() {
	props = new SystemProperties();
    }

    public XmlConverter(String propFile) {
	props = new SystemProperties(propFile);
	extractProperties(props);
    }

    public XmlConverter(File propFile) {
	this ( propFile.getAbsolutePath() );
    }

    public XmlConverter(Properties props) {
	this.props = props;
	extractProperties(props);
    }

    public INode convert( String desc ) {
	return convert(desc, new TransientNode() );
    }

    public INode convert( String desc, INode helmaNode ) throws RuntimeException {
	try {
	    return convert( new URL(desc), helmaNode );
	} catch ( MalformedURLException notanurl ) {
	    try {
	        return convert( new File(desc), helmaNode );
	    } catch ( FileNotFoundException notfound ) {
	        throw new RuntimeException( "couldn't read xml: " + desc );
	    }
	} catch ( IOException ioerror ) {
	    throw new RuntimeException( "couldn't read xml: " + desc );
	}
    }

    public INode convert( File file, INode helmaNode ) throws RuntimeException, FileNotFoundException {
	return convert( new FileInputStream(file), helmaNode );
    }

    public INode convert( URL url, INode helmaNode ) throws RuntimeException, IOException, MalformedURLException {
	return convert( url.openConnection().getInputStream(), helmaNode );
    }

    public INode convert( InputStream in, INode helmaNode ) throws RuntimeException {
	Document document = XmlUtil.parse (in);
	if ( document!=null && document.getDocumentElement()!=null ) {
	    return convert( document.getDocumentElement(), helmaNode, new HashMap() );
	} else {
	    return helmaNode;
	}
    }

    public INode convertFromString( String xml, INode helmaNode ) throws RuntimeException {
	Document document = XmlUtil.parse (new InputSource (new StringReader (xml)));
	if ( document!=null && document.getDocumentElement()!=null ) {
	    return convert( document.getDocumentElement(), helmaNode, new HashMap() );
	} else {
	    return helmaNode;
	}
    }

    public INode convert( Element element, INode helmaNode, Map nodeCache ) {
	offset++;
	// previousNode is used to cache previous nodes with the same prototype
	// so we can reset it in the nodeCache after we've run
	Object previousNode = null;
	if (DEBUG)
	    debug("reading " + element.getNodeName() );
	String prototype = props.getProperty(element.getNodeName()+"._prototype");
	if ( prototype == null && !sparse )
	    prototype = "HopObject";
	// if we have a prototype (either explicit or implicit "hopobject"),
	// set it on the Helma node and store it in the node cache.
	if ( prototype != null ) {
	    helmaNode.setName( element.getNodeName() );
	    helmaNode.setPrototype( prototype );
	    previousNode = nodeCache.put (prototype, helmaNode);
	}
	// check attributes of the current element
	attributes(element, helmaNode, nodeCache);
	// check child nodes of the current element
	if ( element.hasChildNodes() )
	    children(element, helmaNode, nodeCache);

	// if it exists, restore the previous node we've replaced in the node cache.
	if (previousNode != null)
	    nodeCache.put (prototype, previousNode);

	offset--;
	return helmaNode;
    }

    /**
     * parse xml children and create hopobject-children
     */
    private INode children( Element element, helma.objectmodel.INode helmaNode, Map nodeCache ) {
	NodeList list = element.getChildNodes();
	int len = list.getLength();
	boolean nodeIsInitialized = !nodeCache.isEmpty();
	StringBuffer textcontent = new StringBuffer();
	String domKey, helmaKey;
	for ( int i=0; i<len; i++ ) {

	    // loop through the list of children
	    org.w3c.dom.Node childNode = list.item(i);

	    // if the current node hasn't been initialized yet, try if it can
	    // be initialized and converted from one of the child elements.
	    if (!nodeIsInitialized) {
	        if (childNode.getNodeType() == Node.ELEMENT_NODE) {
	            convert ((Element) childNode, helmaNode, nodeCache);
	            if (helmaNode.getPrototype() != null)
	                return helmaNode;
	        }
	        continue;
	    }

	    // if it's text content of this element -> append to StringBuffer
	    if ( childNode.getNodeType() == Node.TEXT_NODE ||
	         childNode.getNodeType() == Node.CDATA_SECTION_NODE ) {
	        textcontent.append( childNode.getNodeValue().trim() );
	        continue;
	    }

	    // it's some kind of element (property or child)
	    if ( childNode.getNodeType() == Node.ELEMENT_NODE ) {

	        Element childElement = (Element)childNode;

	        // get the basic key we have to look for in the properties-table
	        domKey = element.getNodeName()+"."+childElement.getNodeName();

	        // is there a childtext-2-property mapping?
	        if ( props!=null && props.containsKey(domKey+"._text") ) {
	            helmaKey = props.getProperty(domKey+"._text");
	            if( helmaKey.equals("") )
	                // if property is set but without value, read elementname for this mapping
	                helmaKey = childElement.getNodeName().replace(':',defaultSeparator);
	            if (DEBUG)
	                debug("childtext-2-property mapping, helmaKey " + helmaKey + " for domKey " + domKey );
	            // check if helmaKey contains an explicit prototype name in which to
	            // set the property.
	            int dot = helmaKey.indexOf(".");
	            if (dot > -1) {
	                String prototype = helmaKey.substring (0, dot);
	                INode node = (INode) nodeCache.get (prototype);
	                helmaKey = helmaKey.substring (dot+1);
	                if (node != null && node.getString(helmaKey)==null) {
	                    node.setString (helmaKey, XmlUtil.getTextContent (childNode));
	                }
	            } else if ( helmaNode.getString(helmaKey)==null ) {
	                helmaNode.setString( helmaKey, XmlUtil.getTextContent(childNode) );
	                if (DEBUG)
	                    debug("childtext-2-property mapping, setting " + helmaKey + " as string" );
	            }
	            continue;
	        }

	        // is there a simple child-2-property mapping?
	        // (lets the user define to use only one element and make this a property
	        // and simply ignore other elements of the same name)
	        if ( props!=null && props.containsKey(domKey+"._property") ) {
	            helmaKey = props.getProperty(domKey+"._property");
	            // if property is set but without value, read elementname for this mapping:
	            if ( helmaKey.equals("") )
	                helmaKey = childElement.getNodeName().replace(':',defaultSeparator);
	            if (DEBUG)
	                debug("child-2-property mapping, helmaKey " + helmaKey + " for domKey " + domKey);
	            // get the node on which to opererate, depending on the helmaKey
	            // value from the properties file.
	            INode node = helmaNode;
	            int dot = helmaKey.indexOf(".");
	            if (dot > -1) {
	                String prototype = helmaKey.substring (0, dot);
	                if (!prototype.equalsIgnoreCase (node.getPrototype()))
	                    node = (INode) nodeCache.get (prototype);
	                helmaKey = helmaKey.substring (dot+1);
	            }
	            if (node == null)
	                continue;

	            if ( node.getNode(helmaKey)==null ) {
	                convert( childElement, node.createNode(helmaKey), nodeCache );
	                if (DEBUG)
	                    debug( "read " + childElement.toString() + node.getNode(helmaKey).toString() );
	            }
	            continue;
	        }


	        // map it to one of the children-lists
	        helma.objectmodel.INode newHelmaNode = null;
	        String childrenMapping = props.getProperty(element.getNodeName()+"._children");
	        // do we need a mapping directly among _children of helmaNode?
	        // can either be through property elname._children=_all or elname._children=childname
	        if( childrenMapping!=null && ( childrenMapping.equals("_all") || childrenMapping.equals(childElement.getNodeName()) ) ) {
	            newHelmaNode = convert(childElement, helmaNode.createNode(null), nodeCache );
	        }
	        // in which virtual subnode collection should objects of this type be stored?
	        helmaKey = props.getProperty(domKey);
	        if ( helmaKey==null && !sparse ) {
	            helmaKey = childElement.getNodeName().replace(':',defaultSeparator);
	        }
	        if (helmaKey == null) {
	            // we don't map this child element itself since we do
	            // sparse parsing, but there may be something of interest
	            // in the child's attributes and child elements.
	            attributes (childElement, helmaNode, nodeCache);
	            children (childElement, helmaNode, nodeCache);
	            continue;
	        }

	        // get the node on which to opererate, depending on the helmaKey
	        // value from the properties file.
	        INode node = helmaNode;
	        int dot = helmaKey.indexOf(".");
	        if (dot > -1) {
	            String prototype = helmaKey.substring (0, dot);
	            if (!prototype.equalsIgnoreCase (node.getPrototype()))
	                node = (INode) nodeCache.get (prototype);
	            helmaKey = helmaKey.substring (dot+1);
	        }
	        if (node == null)
	            continue;

	        // try to get the virtual node
	        INode worknode = null;
	        if ("_children".equals (helmaKey)) {
	            worknode = node;
	        } else {
	            worknode = node.getNode( helmaKey );
	            if ( worknode==null ) {
	                // if virtual node doesn't exist, create it
	                worknode = helmaNode.createNode( helmaKey );
	            }
	        }
	        if (DEBUG)
	            debug( "mounting child "+ childElement.getNodeName() + " at worknode " + worknode.toString() );
	        // now mount it, possibly re-using the helmaNode that's been created before
	        if ( newHelmaNode!=null ) {
	            worknode.addNode(newHelmaNode);
	        } else {
	            convert( childElement, worknode.createNode( null ), nodeCache );
	        }
	    }
	    // forget about other types (comments etc)
	    continue;
	}

	// if there's some text content for this element, map it:
	if ( textcontent.length()>0 && !sparse ) {
	    helmaKey = props.getProperty(element.getNodeName()+"._text");
	    if ( helmaKey==null )
	        helmaKey = "text";
	    if (DEBUG)
	        debug ("setting text "+textcontent+" to property "+helmaKey+" of object "+helmaNode);
	    helmaNode.setString(helmaKey, textcontent.toString().trim() );
	}

	return helmaNode;
    }

    /**
     * set element's attributes as properties of helmaNode
     */
    private INode attributes( Element element, INode helmaNode, Map nodeCache ) {
	NamedNodeMap nnm = element.getAttributes();
	int len = nnm.getLength();
	for ( int i=0; i<len; i++ ) {
	    org.w3c.dom.Node attr = nnm.item(i);
	    String helmaKey = props.getProperty(element.getNodeName()+"._attribute."+attr.getNodeName());
	    // unless we only map explicit attributes, use attribute name as property name
	    // in case no property name was defined.
	    if ( helmaKey==null && !sparse)
	        helmaKey = attr.getNodeName().replace(':',defaultSeparator);
	    if (helmaKey != null) {
	        // check if the mapping contains the prototype to which 
	        // the property should be applied
	        int dot = helmaKey.indexOf (".");
	        if (dot > -1) {
	            String prototype = helmaKey.substring (0, dot);
	            INode node = (INode) nodeCache.get (prototype);
	            if (node != null) {
	                node.setString (helmaKey.substring(dot+1), attr.getNodeValue());
	            }
	        } else if (helmaNode.getPrototype() != null) {
	            helmaNode.setString( helmaKey, attr.getNodeValue() );
	        }
	    }
	}
	return helmaNode;
    }

    /**
     * utility function
     */
    private void extractProperties( Properties props ) {
	if ( props.containsKey("separator") ) {
	    defaultSeparator = props.getProperty("separator").charAt(0);
	}
	sparse = "sparse".equalsIgnoreCase (props.getProperty("_mode"));
    }


    /** for testing */
    void debug(Object msg) {
	for ( int i=0; i<offset; i++ ) {
	    System.out.print("   ");
	}
	System.out.println(msg.toString());
    }


    public static void main ( String args[] ) {
    }


}

