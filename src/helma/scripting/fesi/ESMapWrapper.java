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
import FESI.Interpreter.Evaluator;
import helma.framework.core.*;
import helma.objectmodel.INode;
import java.util.*;

/**
 * An EcmaScript object that makes stuff in a hashtable accessible as its properties
 */
public class ESMapWrapper extends ESWrapper {
    private Map data;
    private FesiEngine engine;
    private boolean readonly = false;

    /**
     * Creates a new ESMapWrapper object.
     *
     * @param engine ...
     */
    public ESMapWrapper(FesiEngine engine) {
        super(new Object(), engine.getEvaluator());
        this.engine = engine;
    }

    /**
     * Creates a new ESMapWrapper object.
     *
     * @param engine ...
     * @param data ...
     */
    public ESMapWrapper(FesiEngine engine, Map data) {
        super(new Object(), engine.getEvaluator());
        this.engine = engine;
        this.data = data;
    }

    /**
     *
     *
     * @param readonly ...
     */
    public void setReadonly(boolean readonly) {
        this.readonly = readonly;
    }

    /**
     *
     *
     * @param data ...
     */
    public void setData(Map data) {
        this.data = data;
    }

    /**
     * Overridden to make the object read-only
     */
    public void putProperty(String propertyName, ESValue propertyValue, int hash)
                     throws EcmaScriptException {
        if (data == null) {
            data = new HashMap();
        }

        if (propertyValue == ESNull.theNull) {
            deleteProperty(propertyName, hash);
        } else if (readonly == false) {
            data.put(propertyName, propertyValue.toJavaObject());
        } else {
            throw new EcmaScriptException("object is readonly");
        }
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
        if (readonly == false) {
            data.remove(propertyName);

            return true;
        } else {
            return false;
        }
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
        if (data == null) {
            return ESNull.theNull;
        }

        Object val = data.get(propertyName);

        if (val == null) {
            return ESNull.theNull;
        }

        if (val instanceof String) {
            return new ESString((String) val);
        } else if (val instanceof INode) {
            return engine.getNodeWrapper((INode) val);
        } else if (val instanceof Map) {
            return new ESMapWrapper(engine, (Map) val);
        } else if (val instanceof ESValue) {
            return (ESValue) val;
        }

        return ESLoader.normalizeValue(val, evaluator);
    }

    /**
     *
     *
     * @return ...
     */
    public Enumeration getAllProperties() {
        return getProperties();
    }

    /**
     *
     *
     * @return ...
     */
    public Enumeration getProperties() {
        Object[] keys = (data == null) ? null : data.keySet().toArray();

        return new Enum(keys);
    }

    class Enum implements Enumeration {
        Object[] elements;
        int pos;

        Enum(Object[] elements) {
            this.elements = elements;
            pos = 0;
        }

        public boolean hasMoreElements() {
            return (elements != null) && (pos < elements.length);
        }

        public Object nextElement() {
            if ((elements == null) || (pos >= elements.length)) {
                throw new NoSuchElementException();
            }

            return elements[pos++];
        }
    }
}
