/*
 * Copyright 2001 Hannes Wallnoefer
 */
 
package helma.xmlrpc;

import java.net.URL;

/**
 * A callback interface for an asynchronous XML-RPC call.
 */ 

public interface AsyncCallback {

    /**
     * Call went ok, handle result.
     */
    public void handleResult (Object result, URL url, String method);

    /**
     * Something went wrong, handle error.
     */
    public void handleError (Exception exception, URL url, String method);

} 
