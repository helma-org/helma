// ObjectNotFoundException.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel;


/**
 * Thrown when an object could not found in the database where
 * it was expected.
 */
 
public class ObjectNotFoundException extends Exception {

    public ObjectNotFoundException (String msg) {
	super (msg);
    }

}































