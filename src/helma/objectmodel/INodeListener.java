// INodeListener.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.objectmodel;

/**
 * An interface for objects that wish to be notified when certain nodes are 
 * modified. This is not used in the HOP as much as it used to be.
 */
 
public interface INodeListener {

    public void nodeChanged (NodeEvent event);

}