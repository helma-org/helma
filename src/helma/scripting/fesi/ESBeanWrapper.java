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

import FESI.Data.ESNull;
import FESI.Data.ESValue;
import FESI.Data.ESWrapper;
import FESI.Exceptions.EcmaScriptException;
import FESI.Interpreter.Evaluator;
import helma.framework.core.ApplicationBean;
import helma.objectmodel.INode;
import helma.util.SystemProperties;
import java.util.Map;

/**
 * Wrap a Java Bean for use in EcmaScript.
 */
public class ESBeanWrapper extends ESWrapper {
    FesiEngine engine;

    /**
     * Creates a new ESBeanWrapper object.
     *
     * @param object ...
     * @param engine ...
     */
    public ESBeanWrapper(Object object, FesiEngine engine) {
        super(object, engine.getEvaluator(), true);
        this.engine = engine;
    }

    /**
     * Wrap getProperty, return ESNode if INode would be returned,
     * ESMapWrapper if Map would be returned.
     */
    public ESValue getProperty(String propertyName, int hash)
                        throws EcmaScriptException {
        try {
            ESValue val = super.getProperty(propertyName, hash);

            if (val instanceof ESWrapper) {
                Object theObject = ((ESWrapper) val).getJavaObject();

                if (val instanceof ESWrapper && theObject instanceof INode) {
                    return engine.getNodeWrapper((INode) theObject);
                } else if (val instanceof ESWrapper && theObject instanceof Map) {
                    ESMapWrapper wrapper = new ESMapWrapper(engine, (Map) theObject);

                    if (theObject instanceof SystemProperties &&
                            super.getJavaObject() instanceof ApplicationBean) {
                        wrapper.setReadonly(true);
                    }

                    return wrapper;
                }
            }

            return val;
        } catch (Exception rte) {
            return ESNull.theNull;
        }
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
        try {
            super.putProperty(propertyName, propertyValue, hash);
        } catch (Exception rte) {
            // create a nice error message
            throw new EcmaScriptException("can't set property " + propertyName +
                                          " to this value on " +
                                          getJavaObject().toString());
        }
    }
}
