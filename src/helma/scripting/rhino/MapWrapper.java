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

package helma.scripting.rhino;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.Undefined;
import java.util.HashMap;
import java.util.Map;

/**
 *  A class that wraps a Java Map as a native JavaScript object. This is
 *  used by the RhinoCore Wrapper for instances of helma.util.SystemMap
 *  and helma.util.WrappedMap.
 */
public class MapWrapper extends ScriptableObject implements Wrapper {
    Map map;
    RhinoCore core;

    /**
     * Creates a new MapWrapper object.
     */
    public MapWrapper() {
        map = null;
    }

    /**
     * Creates a new MapWrapper object.
     *
     * @param map the Map
     * @param core the RhinoCore instance
     */
    public MapWrapper(Map map, RhinoCore core) {
        this.map = map;
        this.core = core;
        setParentScope(core.global);
        setPrototype(ScriptableObject.getObjectPrototype(core.global));
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     * @param value ...
     */
    public void put(String name, Scriptable start, Object value) {
        if (map == null) {
            map = new HashMap();
        }

        if (value == null || value == Undefined.instance) {
            map.remove(name);
        } else if (value instanceof Wrapper) {
            map.put(name, ((Wrapper) value).unwrap());
        } else {
            map.put(name, value);
        }
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     *
     * @return ...
     */
    public Object get(String name, Scriptable start) {
        if (map == null) {
            return null;
        }

        Object obj = map.get(name);

        if (obj != null && !(obj instanceof Scriptable)) {
            // do NOT wrap primitives - otherwise they'll be wrapped as Objects,
            // which makes them unusable for many purposes (e.g. ==)
            if (obj instanceof String ||
                    obj instanceof Number ||
                    obj instanceof Boolean) {
                return obj;
            }

            return Context.toObject(obj, core.global);
        }

        return obj;
    }

    /**
     *
     *
     * @param name ...
     * @param start ...
     *
     * @return ...
     */
    public boolean has(String name, Scriptable start) {
        return (map != null) && map.containsKey(name);
    }

    /**
     *
     *
     * @param name ...
     */
    public void delete(String name) {
        if (map != null) {
            map.remove(name);
        }
    }

    /**
     *
     *
     * @param idx ...
     * @param start ...
     * @param value ...
     */
    public void put(int idx, Scriptable start, Object value) {
        if (map == null) {
            map = new HashMap();
        }

        if (value == null || value == Undefined.instance) {
            map.remove(Integer.toString(idx));
        } else if (value instanceof Wrapper) {
            map.put(Integer.toString(idx), ((Wrapper) value).unwrap());
        } else {
            map.put(Integer.toString(idx), value);
        }
    }

    /**
     *
     *
     * @param idx ...
     * @param start ...
     *
     * @return ...
     */
    public Object get(int idx, Scriptable start) {
        if (map == null) {
            return null;
        }

        Object obj = map.get(Integer.toString(idx));

        if (obj != null && !(obj instanceof Scriptable)) {
            // do NOT wrap primitives - otherwise they'll be wrapped as Objects,
            // which makes them unusable for many purposes (e.g. ==)
            if (obj instanceof String ||
                    obj instanceof Number ||
                    obj instanceof Boolean) {
                return obj;
            }

            return Context.toObject(obj, core.global);
        }

        return obj;
    }

    /**
     *
     *
     * @param idx ...
     * @param start ...
     *
     * @return ...
     */
    public boolean has(int idx, Scriptable start) {
        return (map != null) && map.containsKey(Integer.toString(idx));
    }

    /**
     *
     *
     * @param idx ...
     */
    public void delete(int idx) {
        if (map != null) {
            map.remove(Integer.toString(idx));
        }
    }


    /**
     * Return an array containing the property key values of this map.
     */
    public Object[] getIds() {
        if (map == null) {
            return new Object[0];
        }

        return map.keySet().toArray();
    }

    public Object getDefaultValue(Class hint) {
        if (hint == null || hint == String.class) {
            return map == null ? "{}" : map.toString();
        }
        return super.getDefaultValue(hint);
    }

    /**
     * Return the wrapped Map object.
     */
    public Object unwrap() {
        if (map == null) {
            map = new HashMap();
        }
        return map;
    }

    /**
     * Return the class name for wrapped maps.
     */
    public String getClassName() {
        return "[MapWrapper]";
    }

    /**
     * Return a string representation for this wrapped map. This calls
     * Map.toString(), so usually the contents of the map will be listed.
     */
    public String toString() {
        if (map == null) {
            return "[MapWrapper{}]";
        } else {
            return "[MapWrapper"+map.toString()+"]";
        }
    }
}
