// ConcurrencyException.java
// Copyright (c) Hannes Wallnöfer 1998-2000
 
package helma.objectmodel;


/**
 * Thrown when more than one thrad tries to modify a Node. The evaluator 
 * will normally catch this and try again after a period of time. 
 */
 
public class ConcurrencyException extends RuntimeException {

    public ConcurrencyException (String msg) {
	super (msg);
    }

}

































