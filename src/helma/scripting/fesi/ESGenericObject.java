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

package helma.scripting.fesi;

import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Interpreter.*;
import helma.framework.IPathElement;
import helma.framework.core.*;
import java.util.*;

/**
 *  A wrapper for a Java object that may or may not implement the IPathElement interface.
 */
public class ESGenericObject extends ObjectPrototype {
    ESWrapper wrapper;
    Object wrappedObject;

    /**
     * Creates a new ESGenericObject object.
     *
     * @param prototype ...
     * @param evaluator ...
     * @param obj ...
     */
    public ESGenericObject(ESObject prototype, Evaluator evaluator, Object obj) {
        super(prototype, evaluator);
        wrappedObject = obj;
        wrapper = new ESWrapper(obj, evaluator);
    }

    /**
     *
     *
     * @return ...
     */
    public String getESClassName() {
        return "GenericObject";
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return wrappedObject.toString();
    }

    /**
     *
     *
     * @return ...
     */
    public String toDetailString() {
        return wrapper.toDetailString();
    }

    /**
     *
     *
     * @param propertyName ...
     * @param propertyValue ...
     * @param hash ...
     *
     * @throws EcmaScriptException ...
     */
    public void putProperty(String propertyName, ESValue propertyValue, int hash)
                     throws EcmaScriptException {
        wrapper.putProperty(propertyName, propertyValue, hash);
    }

    /**
     *
     *
     * @param propertyName ...
     * @param hash ...
     *
     * @return ...
     *
     * @throws EcmaScriptException ...
     */
    public boolean hasProperty(String propertyName, int hash)
                        throws EcmaScriptException {
        return super.hasProperty(propertyName, hash) ||
               wrapper.hasProperty(propertyName, hash);
    }

    /**
     *
     *
     * @param propertyName ...
     * @param hash ...
     *
     * @return ...
     *
     * @throws EcmaScriptException ...
     */
    public boolean deleteProperty(String propertyName, int hash)
                           throws EcmaScriptException {
        return wrapper.deleteProperty(propertyName, hash);
    }

    /**
     *
     *
     * @param i ...
     *
     * @return ...
     *
     * @throws EcmaScriptException ...
     */
    public ESValue getProperty(int i) throws EcmaScriptException {
        return wrapper.getProperty(i);
    }

    /**
     *
     *
     * @param index ...
     * @param propertyValue ...
     *
     * @throws EcmaScriptException ...
     */
    public void putProperty(int index, ESValue propertyValue)
                     throws EcmaScriptException {
        wrapper.putProperty(index, propertyValue);
    }

    /**
     *
     *
     * @param propertyName ...
     * @param hash ...
     *
     * @return ...
     *
     * @throws EcmaScriptException ...
     */
    public ESValue getProperty(String propertyName, int hash)
                        throws EcmaScriptException {
        ESValue val = super.getProperty(propertyName, hash);

        if ((val == null) || (val == ESUndefined.theUndefined)) {
            val = wrapper.getProperty(propertyName, hash);
        }

        return val;
    }

    /**
     *
     *
     * @param evaluator ...
     * @param thisObject ...
     * @param functionName ...
     * @param arguments ...
     *
     * @return ...
     *
     * @throws EcmaScriptException ...
     * @throws NoSuchMethodException ...
     */
    public ESValue doIndirectCall(Evaluator evaluator, ESObject thisObject,
                                  String functionName, ESValue[] arguments)
                           throws EcmaScriptException, NoSuchMethodException {
        if (super.hasProperty(functionName, functionName.hashCode())) {
            return super.doIndirectCall(evaluator, thisObject, functionName, arguments);
        }

        return wrapper.doIndirectCall(evaluator, thisObject, functionName, arguments);
    }

    /**
     *
     *
     * @return ...
     */
    public Enumeration getAllProperties() {
        return wrapper.getProperties();
    }

    /**
     *
     *
     * @return ...
     */
    public Enumeration getProperties() {
        return wrapper.getProperties();
    }

    /**
     *
     *
     * @return ...
     */
    public Object toJavaObject() {
        return wrappedObject;
    }

    /**
     * An ESNode equals another object if it is an ESNode that wraps the same INode
     * or the wrapped INode itself. FIXME: doesen't check dbmapping/type!
     */
    public boolean equals(Object what) {
        if (what == null) {
            return false;
        }

        if (what == this) {
            return true;
        }

        if (what instanceof ESGenericObject) {
            ESGenericObject other = (ESGenericObject) what;

            return (wrappedObject.equals(other.wrappedObject));
        }

        return false;
    }
}
