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

package helma.util;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * A CacheMap subclass that uses WeakHashMaps internally for its
 * rotating tables.
 */
public class WeakCacheMap extends CacheMap {

    public WeakCacheMap(int capacity) {
        super(capacity);
    }

    public WeakCacheMap(int capacity, float loadFactor) {
        super(capacity, loadFactor);
    }

    /**
     * Overridden to return a java.util.WeakHashMap instance.
     *
     * @param capacity the initial capacity
     * @param loadFactor the load factor
     * @return a new Map used for internal caching
     */
    protected Map createTable(int capacity, float loadFactor) {
        return new WeakHashMap(capacity, loadFactor);
    }
}
