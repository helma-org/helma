// Updatable.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.util;


/**
 * An interface of classes that can update themselves and know when to do so.
 */


public interface Updatable {

    public boolean needsUpdate ();

    public void update ();

}
































