// INode.java
// Copyright (c) Hannes Wallnöfer 2000
 
package helma.objectmodel;


/**
 * Minimal Interface for Nodes that build a hierarchic tree
 */
 
public interface IPathElement {

    public INode getSubnode (String name);
    public INode getNode (String name, boolean inherit);


}




