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

package helma.objectmodel;

import helma.framework.core.Application;

import java.util.Properties;

/**
 * Interface Helma object cache classes need to implement.
 *
 */
public interface ObjectCache {

    /**
     * Set the {@link helma.framework.core.Application Application} instance
     * for the cache.
     * @param app the app instance
     */
    void init(Application app);

    /**
     * Called when the application's properties have been updated to let
     * the cache implementation update its settings.
     * @param props
     */
    void updateProperties(Properties props);

    /**
     * Returns true if the collection contains an element for the key.
     *
     * @param key the key that we are looking for
     */
    boolean containsKey(Object key);

    /**
     * Returns the number of keys in object array <code>keys</code> that
     * were not found in the Map.
     * Those keys that are contained in the Map are nulled out in the array.
     * @param keys an array of key objects we are looking for
     * @see ObjectCache#containsKey
     */
    int containsKeys(Object[] keys);

    /**
     * Gets the object associated with the specified key in the
     * hashtable.
     * @param key the specified key
     * @return the element for the key or null if the key
     * 		is not defined in the hash table.
     * @see ObjectCache#put
     */
    Object get(Object key);

    /**
     * Puts the specified element into the hashtable, using the specified
     * key.  The element may be retrieved by doing a get() with the same key.
     * The key and the element cannot be null.
     * @param key the specified key in the hashtable
     * @param value the specified element
     * @exception NullPointerException If the value of the element
     * is equal to null.
     * @see ObjectCache#get
     * @return the old value of the key, or null if it did not have one.
     */
    Object put(Object key, Object value);

    /**
     * Removes the element corresponding to the key. Does nothing if the
     * key is not present.
     * @param key the key that needs to be removed
     * @return the value of key, or null if the key was not found.
     */
    Object remove(Object key);

    /**
     * Removes all items currently stored in the cache.
     *
     * @return true if the operation succeeded
     */
    boolean clear();

    /**
     * Return the number of objects currently stored in the cache.
     * @return the number of cached items
     */
    int size();

    /**
     * Return an array with all objects currently contained in the cache.
     */
    Object[] getCachedObjects();

}
