// TimeoutException.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework;

/**
 * TimeoutException is thrown by the request evaluator when a request could 
 * not be serviced within the timeout period specified for an application. 
 */
 
public class TimeoutException extends RuntimeException {
    public TimeoutException () {
	super ("Request timed out");
    }
}



