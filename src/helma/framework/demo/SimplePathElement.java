// SimplePathElement.java
// Copyright Hannes Wallnöfer 2001

package helma.framework.demo;

import helma.framework.IPathElement;

/**
 * This is an example implementation for the helma.framework.IPathElement interface.
 * It creates any child element which is requested on the fly without ever asking.
 */

public class SimplePathElement implements IPathElement {

    String name;
    String prototype;
    IPathElement parent;

    /**
     *  Constructor for the root element.
     */
    public SimplePathElement () {
	name = "root";
	prototype = "root";
	parent = null;
    }

    /**
     * Constructor for non-root elements.
     */
    public SimplePathElement (String n, IPathElement p) {
	name = n;
	prototype = "hopobject";
	parent = p;
    }

    /**
     * Returns a child element for this object, creating it on the fly.
     */
    public IPathElement getChildElement (String n) {
	return new SimplePathElement (n, this);
    }

    /**
     * Returns this object's parent element
     */
    public IPathElement getParentElement () {
	return parent;
    }

    /**
     * Returns the element name to be used for this object.
     */
    public String getElementName () {
	return name;
    }

    /**
     * Returns the name of the scripting prototype to be used for this object.
     * This will be "root" for the root element and "hopobject for everything else.
     */
    public String getPrototype () {
	return prototype;
    }

    /**
     * Returns a string representation of this element.
     */
    public String toString () {
	return "SimplePathElement "+name;
    }

}

