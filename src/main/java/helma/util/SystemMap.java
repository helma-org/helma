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


import java.util.*;

/**
 * Map class used internally by Helma. We use this class to be able to
 *  wrap maps as native objects within a scripting engine rather
 *  than exposing them through Java reflection. 
 */
public class SystemMap extends HashMap {


    private static final long serialVersionUID = 2926260006469380544L;

    /**
     *  Construct an empty SystemMap.
     */
    public SystemMap() {
        super();
    }

    /**
     *  Construct an empty SystemMap with the given initial capacity.
     */
    public SystemMap(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     *  Construct a SystemMap with the contents of Map map.
     */
    public SystemMap(Map map) {
        super(map);
    }

    /**
     * @return A String representation of this map. The returned string is similar to
     * the one returned by java.util.HashMap.toString(), but additionally displays
     * Object arrays in a human friendly way.
     */
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("{");

        Iterator i = entrySet().iterator();
            boolean hasNext = i.hasNext();
            while (hasNext) {
            Map.Entry e = (Map.Entry) (i.next());
                Object key = e.getKey();
                Object value = e.getValue();
                append(buf, key);
                buf.append("=");
                append(buf, value);

                hasNext = i.hasNext();
                if (hasNext)
                    buf.append(", ");
            }

        buf.append("}");
        return buf.toString();
    }

    /**
     * Display an object in a human friendly way, paying attention to avoid
     * infinite recursion.
     */
    protected void append(StringBuffer buf, Object obj) {
        if (obj == this) {
            buf.append("(this Map)");
        } else if (obj instanceof Object[]) {
            Object[] array = (Object[]) obj;
            if (array.length == 1) {
                append(buf, array[0]);
            } else {
                buf.append("[");
                for (int i = 0; i < array.length; i++) {
                    append(buf, array[i]);
                    if (i < array.length - 1)
                        buf.append(",");
                }
                buf.append("]");
            }
        } else {
            buf.append(obj);
        }
    }


}
