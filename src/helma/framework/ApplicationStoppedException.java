// ApplicationStoppedException.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework;

import FESI.Exceptions.EcmaScriptException;

/** 
 * This is thrown when a request is made to a stopped
 * application
 */
 
public class ApplicationStoppedException extends RuntimeException {


    public ApplicationStoppedException () {
	super ("The application has been stopped");
    }


}
