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

package helma.framework.core;

import java.util.*;
import java.io.UnsupportedEncodingException;

import helma.util.UrlEncoded;

/**
 *  Represents a URI request path that has been resolved to an object path.
 *  Offers methods to access objects in the path by index and prototype names,
 *  and to render the path as URI again.
 */
public class RequestPath {

    Application app;

    List objects;
    List ids;

    Map primaryProtos;
    Map secondaryProtos;

    /**
     * Creates a new RequestPath object.
     *
     * @param app the application we're running in
     */
    public RequestPath(Application app) {
        this.app = app;
        objects = new ArrayList();
        ids = new ArrayList();
        primaryProtos = new HashMap();
        secondaryProtos = new HashMap();
    }

    /**
     * Adds an item to the end of the path.
     *
     * @param id the item id representing the path in the URL
     * @param obj the object to which the id resolves
     */
    public void add(String id, Object obj) {
        ids.add(id);
        objects.add(obj);

        Prototype proto = app.getPrototype(obj);

        if (proto != null) {
            primaryProtos.put(proto.getName(), obj);
            primaryProtos.put(proto.getLowerCaseName(), obj);
            proto.registerParents(secondaryProtos, obj);
        }
    }

    /**
     * Returns the number of objects in the request path.
     */
    public int size() {
        return objects.size();
    }

    /**
     * Gets an object in the path by index.
     *
     * @param idx the index of the object in the request path
     */
    public Object get(int idx) {
        if (idx < 0 || idx >= objects.size()) {
            return null;
        }

        return objects.get(idx);
    }

    /**
     * Gets an object in the path by prototype name.
     *
     * @param typeName the prototype name of the object in the request path
     */
    public Object getByPrototypeName(String typeName) {
        // search primary prototypes first
        Object obj = primaryProtos.get(typeName);

        if (obj != null) {
            return obj;
        }

        // if that fails, consult secondary prototype map
        return secondaryProtos.get(typeName);
    }

    /**
     * Returns the string representation of this path usable for links.
     */
    public String href(String action) throws UnsupportedEncodingException {
        StringBuffer buffer = new StringBuffer(app.getBaseURI());

        int start = 1;
        String hrefRootPrototype = app.getHrefRootPrototype();

        if (hrefRootPrototype != null) {
            Object rootObject = getByPrototypeName(hrefRootPrototype);

            if (rootObject != null) {
                start = objects.indexOf(rootObject) + 1;
            }
        }

        for (int i=start; i<ids.size(); i++) {
            buffer.append(UrlEncoded.encode(ids.get(i).toString(), app.charset));
            buffer.append("/");
        }

        if (action != null) {
            buffer.append(UrlEncoded.encode(action, app.charset));
        }

        return buffer.toString();
    }
    
    /**
     * Checks if the given object is contained in the request path
     *
     * @param obj the element to check
     * @return the index of the element, or -1 if it isn't contained
     */
    public int contains(Object obj) {
        return objects.indexOf(obj);
    }

   /**
    * Return a string representation of the Request Path
    */
    public String toString() {
        // If there's just one element we're on the root object.
        if (ids.size() <= 1) 
            return "/";
        
        StringBuffer buffer = new StringBuffer();
        for (int i=1; i<ids.size(); i++) {
            buffer.append('/');
            buffer.append(ids.get(i));
        }
        return buffer.toString();
    } 
}