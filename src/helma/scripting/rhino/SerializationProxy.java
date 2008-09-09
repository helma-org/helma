/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 2008 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.scripting.rhino;

import helma.objectmodel.INode;
import helma.objectmodel.db.NodeHandle;
import org.mozilla.javascript.Context;

import java.io.Serializable;

/**
 * Serialization proxy/placeholder interface. This is used for
 * for various Helma and Rhino related classes.. 
 */
public interface SerializationProxy extends Serializable {
    public Object getObject(RhinoEngine engine);
}

/**
 * Serialization proxy for app, req, res, path objects.
 */
class ScriptBeanProxy implements SerializationProxy {
    String name;

    ScriptBeanProxy(String name) {
        this.name = name;
    }

    /**
     * Lookup the actual object in the current scope
     *
     * @return the object represented by this proxy
     */
    public Object getObject(RhinoEngine engine) {
        return engine.global.get(name, engine.global);
    }

}

/**
 * Serialization proxy for global scope
 */
class GlobalProxy implements SerializationProxy {
    boolean shared;

    GlobalProxy(GlobalObject scope) {
        shared = !scope.isThreadScope;
    }

    /**
     * Lookup the actual object in the current scope
     *
     * @return the object represented by this proxy
     */
    public Object getObject(RhinoEngine engine) {
        return shared ? engine.core.global : engine.global;
    }
}

/**
 * Serialization proxy for various flavors of HopObjects/Nodes
 */
class HopObjectProxy implements SerializationProxy {
    Object ref;
    boolean wrapped = false;

    HopObjectProxy(HopObject obj) {
        INode n = obj.getNode();
        if (n == null) {
            ref = obj.getClassName();
        } else {
            if (n instanceof helma.objectmodel.db.Node) {
                ref = new NodeHandle((helma.objectmodel.db.Node) n);
            } else {
                ref = n;
            }
        }
        wrapped = true;
    }

    HopObjectProxy(helma.objectmodel.db.Node node) {
        ref = new NodeHandle(node.getKey());
    }

    /**
     * Lookup the actual object in the current scope
     *
     * @return the object represented by this proxy
     */
    public Object getObject(RhinoEngine engine) {
        if (ref instanceof String)
            return engine.core.getPrototype((String) ref);
        else if (ref instanceof NodeHandle) {
            Object n = ((NodeHandle) ref).getNode(engine.app.getWrappedNodeManager());
            return wrapped ? Context.toObject(n, engine.global) : n;
        }
        return Context.toObject(ref, engine.global);
    }

}
