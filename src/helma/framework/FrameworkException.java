// FrameworkException.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework;

/**
 * The basic exception class used to tell when certain things go 
 * wrong in evaluation of requests.
 */
 
public class FrameworkException extends RuntimeException {

    public FrameworkException (String msg) {
	super (msg);
    }

}
