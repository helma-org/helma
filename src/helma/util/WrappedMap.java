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
import java.util.Set;
import java.util.Collection;
import java.util.HashMap;

/**
 *  A Map that wraps another map and can be set to read-only.
 */
public class WrappedMap implements Map {

    // the wrapped map
    private Map wrapped = null;

    // is this map readonly?
    boolean readonly = false;

    // isolate changes from the wrapped map?
    boolean copyOnWrite = false;

    public final static int READ_ONLY = 1;
    public final static int COPY_ON_WRITE = 2;

    /**
     *  Constructor
     */
    public WrappedMap(Map map) {
        if (map == null) {
            throw new IllegalArgumentException(
                "Argument must not be null in WrappedMap constructor");
        }
        wrapped = map;
    }

    /**
     *  Constructor
     */
    public WrappedMap(Map map, int mode) {
        this(map);
        if (mode == READ_ONLY) {
            readonly = true;
        } else if (mode == COPY_ON_WRITE) {
            copyOnWrite = true;
        }
    }

    /**
     *  Set the readonly flag on or off
     */
    public void setReadonly(boolean ro) {
        readonly = ro;
    }

    /**
     *  Is this map readonly?
     */
    public boolean isReadonly() {
        return readonly;
    }

    /**
     *  Set the copyOnWrite flag on or off
     */
    public void setCopyOnWrite(boolean cow) {
        copyOnWrite = cow;
    }

    /**
     *  Is this map copyOnWrite?
     */
    public boolean isCopyOnWrite() {
        return copyOnWrite;
    }

    // Methods from interface java.util.Map -
    // these are just proxies to the wrapped map, except for
    // readonly checks on modifiers.

    public int size() {
        return wrapped.size();
    }

    public boolean isEmpty() {
        return wrapped.isEmpty();
    }

    public boolean containsKey(Object key) {
        return wrapped.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return wrapped.containsValue(value);
    }

    public Object get(Object key) {
        return wrapped.get(key);
    }

    // Modification Operations - check for readonly

    public Object put(Object key, Object value) {
        if (readonly) {
            throw new RuntimeException("Attempt to modify readonly map");
        }
        if (copyOnWrite) {
            wrapped = new HashMap (wrapped);
            copyOnWrite = false;
        }
        return wrapped.put(key, value);
    }

    public Object remove(Object key) {
        if (readonly) {
            throw new RuntimeException("Attempt to modify readonly map");
        }
        if (copyOnWrite) {
            wrapped = new HashMap (wrapped);
            copyOnWrite = false;
        }
        return wrapped.remove(key);
    }

    public void putAll(Map t) {
        if (readonly) {
            throw new RuntimeException("Attempt to modify readonly map");
        }
        if (copyOnWrite) {
            wrapped = new HashMap (wrapped);
            copyOnWrite = false;
        }
        wrapped.putAll(t);
    }

    public void clear() {
        if (readonly) {
            throw new RuntimeException("Attempt to modify readonly map");
        }
        if (copyOnWrite) {
            wrapped = new HashMap (wrapped);
            copyOnWrite = false;
        }
        wrapped.clear();
    }


    // Views

    public Set keySet() {
        return wrapped.keySet();
    }

    public Collection values() {
        return wrapped.values();
    }

    public Set entrySet() {
        return wrapped.entrySet();
    }


    // Comparison and hashing

    public boolean equals(Object o) {
        return wrapped.equals(o);
    }

    public int hashCode() {
        return wrapped.hashCode();
    }

}
