/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.framework;


/**
 * Interface that objects need to implement to build a Helma URL tree. Apart from methods
 * to retrieve the identifier and its child and parent elements, this interface defines a method
 * that determines which prototype to use to add scripts and skins to an object. <p>
 *
 * Please note that this interface is still work in progress. You should expect it to get some
 * additional methods that allow for looping through child elements, for example, or retrieving the
 * parent element. <p>
 *
 */
public interface IPathElement {
    /**
     *  Return the name to be used to get this element from its parent
     */
    public String getElementName();

    /**
     * Retrieve a child element of this object by name.
     */
    public IPathElement getChildElement(String name);

    /**
     * Return the parent element of this object.
     */
    public IPathElement getParentElement();

    /**
     * Get the name of the prototype to be used for this object. This will
     * determine which scripts, actions and skins can be called on it
     * within the Helma scripting and rendering framework.
     */
    public String getPrototype();
}
