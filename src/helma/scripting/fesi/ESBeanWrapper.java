package helma.scripting.fesi;

import java.util.Map;

import helma.framework.core.ApplicationBean;
import helma.objectmodel.INode;
import helma.util.SystemProperties;

import FESI.Interpreter.Evaluator;
import FESI.Exceptions.EcmaScriptException;
import FESI.Data.ESNull;
import FESI.Data.ESValue;
import FESI.Data.ESWrapper;

/**
 * Wrap a Java Bean for use in EcmaScript.
 */

public class ESBeanWrapper extends ESWrapper {

    FesiEngine engine;

    public ESBeanWrapper (Object object, FesiEngine engine) {
	super (object, engine.getEvaluator(),true);
	this.engine = engine;
    }

  /**
   * Wrap getProperty, return ESNode if INode would be returned,
   * ESMapWrapper if Map would be returned.
   */
   public ESValue getProperty(String propertyName, int hash) throws EcmaScriptException {
      try {
         ESValue val = super.getProperty (propertyName, hash);
         if (val instanceof ESWrapper) {
            Object theObject = ((ESWrapper)val).getJavaObject ();
            if (val instanceof ESWrapper && theObject instanceof INode)	{
               return engine.getNodeWrapper ((INode) theObject);
            } else if (val instanceof ESWrapper && theObject instanceof Map)	{
               ESMapWrapper wrapper = new ESMapWrapper(engine, (Map) theObject);
               if (theObject instanceof SystemProperties && super.getJavaObject () instanceof ApplicationBean)
                  wrapper.setReadonly(true);
               return wrapper;
            }
         }
         return val;
      } catch (Exception rte) {
         return ESNull.theNull;
      }
   }

    public void putProperty(String propertyName, ESValue propertyValue, int hash) throws EcmaScriptException {
	try {
	    super.putProperty (propertyName, propertyValue, hash);
	} catch (Exception rte) {
	    // create a nice error message
	    throw new EcmaScriptException("can't set property " + propertyName +
	    " to this value on " + getJavaObject().toString() );
	}
    }

}



