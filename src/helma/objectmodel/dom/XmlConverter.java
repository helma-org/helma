package helma.objectmodel.dom;

import java.io.*;
import java.net.*;
import java.util.*;

import javax.xml.parsers.*;
import org.w3c.dom.*;

import helma.objectmodel.*;
import helma.util.SystemProperties;

public class XmlConverter implements XmlConstants	{

    private boolean DEBUG=false;

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
	    return convert( document.getDocumentElement(), helmaNode );
	} else {
	    return helmaNode;
	}
    }

    public INode convert( Element element, INode helmaNode ) {
	offset++;
	if (DEBUG)
	    debug("reading " + element.getNodeName() );
	helmaNode.setName( element.getNodeName() );
	String prototype = (String)props.get(element.getNodeName().toLowerCase()+"._prototype");
	if ( prototype == null )
	    prototype = "HopObject";
	helmaNode.setPrototype( prototype );
	attributes(element, helmaNode);
	if ( element.hasChildNodes() ) {
	    children(element, helmaNode);
	}
	offset--;
	return helmaNode;
    }

    /**
     * parse xml children and create hopobject-children
     */
    private INode children( Element element, helma.objectmodel.INode helmaNode ) {
	NodeList list = element.getChildNodes();
	int len = list.getLength();
	StringBuffer textcontent = new StringBuffer();
	String domKey, helmaKey;
	for ( int i=0; i<len; i++ ) {

	    // loop through the list of children
	    org.w3c.dom.Node childNode = list.item(i);

	    // if it's text content of this element -> append to StringBuffer
	    if ( childNode.getNodeType()==org.w3c.dom.Node.TEXT_NODE ) {
	        textcontent.append( childNode.getNodeValue().trim() );
	        continue;
	    }

	    // FIXME: handle CDATA!

	    // it's some kind of element (property or child)
	    if ( childNode.getNodeType()==org.w3c.dom.Node.ELEMENT_NODE ) {

	        Element childElement = (Element)childNode;

	        // get the basic key we have to look for in the properties-table
	        domKey = (element.getNodeName()+"."+childElement.getNodeName()).toLowerCase();

	        // is there a childtext-2-property mapping?
	        if ( props!=null && props.containsKey(domKey+"._text") ) {
	            helmaKey = (String)props.get(domKey+"._text");
	            if( helmaKey.equals("") )
	                // if property is set but without value, read elementname for this mapping
	                helmaKey = childElement.getNodeName().replace(':',defaultSeparator);
	            if (DEBUG)
	                debug("childtext-2-property mapping, helmaKey " + helmaKey + " for domKey " + domKey );
	            if ( helmaNode.getString(helmaKey,false)==null ) {
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
	            helmaKey = (String)props.get(domKey+"._property");
	            // if property is set but without value, read elementname for this mapping:
	            if ( helmaKey.equals("") )
	                helmaKey = childElement.getNodeName().replace(':',defaultSeparator);
	            if (DEBUG)
	                debug("child-2-property mapping, helmaKey " + helmaKey + " for domKey " + domKey);
	            if ( helmaNode.getNode(helmaKey,false)==null ) {
	                convert( childElement, helmaNode.createNode(helmaKey) );
	                if (DEBUG)
	                    debug( "read " + childElement.toString() + helmaNode.getNode(helmaKey,false).toString() );
	            }
	            continue;
	        }


	        // map it to one of the children-lists
	        helma.objectmodel.INode newHelmaNode = null;
	        String childrenMapping = (String)props.get(element.getNodeName().toLowerCase()+"._children");
	        // do we need a mapping directly among _children of helmaNode?
	        // can either be through property elname._children=_all or elname._children=childname
	        if( childrenMapping!=null && ( childrenMapping.equals("_all") || childrenMapping.equals(childElement.getNodeName()) ) ) {
	            newHelmaNode = convert(childElement, helmaNode.createNode(null) );
	        }
	        // which name to choose for a virtual subnode:
	        helmaKey = (String)props.get(domKey);
	        if ( helmaKey==null ) {
	            helmaKey = childElement.getNodeName().replace(':',defaultSeparator);
	        }
	        // try to get the virtual node
	        helma.objectmodel.INode worknode = helmaNode.getNode( helmaKey, false );
	        if ( worknode==null ) {
	            // if virtual node doesn't exist, create it
	            worknode = helmaNode.createNode( helmaKey );
	        }
	        if (DEBUG)
	            debug( "mounting child "+ childElement.getNodeName() + " at worknode " + worknode.toString() );
	        // now mount it, possibly re-using the helmaNode that's been created before
	        if ( newHelmaNode!=null ) {
	            worknode.addNode(newHelmaNode);
	        } else {
	            convert( childElement, worknode.createNode( null ) );
	        }
	    }
	    // forget about other types (comments etc)
	    continue;
	}

	// if there's some text content for this element, map it:
	if ( textcontent.length()>0 ) {
	    helmaKey = (String)props.get(element.getNodeName().toLowerCase()+"._text");
	    if ( helmaKey==null )
	        helmaKey = "text";
	    helmaNode.setString(helmaKey, textcontent.toString().trim() );
	}

	return helmaNode;
    }

    /**
     * set element's attributes as properties of helmaNode
     */
    private INode attributes( Element element, INode helmaNode ) {
	NamedNodeMap nnm = element.getAttributes();
	int len = nnm.getLength();
	for ( int i=0; i<len; i++ ) {
	    org.w3c.dom.Node attr = nnm.item(i);
	    if ( attr.getNodeName().equals("_prototype") ) {
	        helmaNode.setPrototype( attr.getNodeValue() );
	    } else if ( attr.getNodeName().equals("_name") ) {
	        helmaNode.setName( attr.getNodeValue() );
	    } else {
	        String helmaKey = (String)props.get(element.getNodeName().toLowerCase()+"._attribute."+attr.getNodeName().toLowerCase());
	        if ( helmaKey==null )
	            helmaKey = attr.getNodeName().replace(':',defaultSeparator);
	        helmaNode.setString( helmaKey, attr.getNodeValue() );
	    }
	}
	return helmaNode;
    }

    /**
     * utility function
     */
    private void extractProperties( Properties props ) {
	if ( props.containsKey("separator") ) {
	    defaultSeparator = ((String)props.get("separator")).charAt(0);
	}
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

