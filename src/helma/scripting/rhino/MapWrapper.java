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
import org.mozilla.javascript.NativeJavaObject;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 */
public class MapWrapper extends ScriptableObject {
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
     * @param map ...
     * @param core ...
     */
    public MapWrapper(Map map, RhinoCore core) {
        this.map = map;
        this.core = core;
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

        if (value instanceof NativeJavaObject) {
            map.put(name, ((NativeJavaObject) value).unwrap());
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

        if (obj != null) {
            Context cx = Context.getCurrentContext();

            return cx.toObject(obj, core.global);
        }

        return null;
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
     * @return ...
     */
    public Object[] getIds() {
        if (map == null) {
            return new Object[0];
        }

        return map.keySet().toArray();
    }

    /**
     *
     *
     * @return ...
     */
    public String getClassName() {
        return "[MapWrapper]";
    }
}
