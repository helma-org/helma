package helma.scripting.fesi;

import java.util.Map;

import helma.objectmodel.INode;

import FESI.Interpreter.Evaluator;
import FESI.Exceptions.EcmaScriptException;
import FESI.Data.ESNull;
import FESI.Data.ESValue;
import FESI.Data.ESWrapper;

/**
 * Wrap a Java Bean for use in EcmaScript.
 */

public class ESBeanWrapper extends ESWrapper {

	FesiEvaluator eval;

	public ESBeanWrapper (Object object, FesiEvaluator eval) {
		super (object, eval.getEvaluator(),true);
		this.eval = eval;
	}

	/**
 	* Wrap getProperty, return ESNode if INode would be returned.
 	*/
    public ESValue getProperty(String propertyName, int hash)
                                 throws EcmaScriptException {
		try {
			ESValue val = super.getProperty (propertyName, hash);
			if (val instanceof ESWrapper && ((ESWrapper)val).getJavaObject() instanceof INode)	{
				return eval.getNodeWrapper( (INode) ((ESWrapper)val).getJavaObject() );
			} else if (val instanceof ESWrapper && ((ESWrapper)val).getJavaObject() instanceof Map)	{
				return new ESMapWrapper(eval, (Map) ((ESWrapper)val).getJavaObject() );
			} else {
				return val;
			}
		} catch (Exception rte) {
			return ESNull.theNull;
		}
    }

    public void putProperty(String propertyName, ESValue propertyValue, int hash)
                              throws EcmaScriptException {
    	try {
    		super.putProperty (propertyName, propertyValue, hash);
    	} catch (Exception rte) {
    		// create a nice error message
    		throw new EcmaScriptException("can't set property " + propertyName +
    		" to this value on " + getJavaObject().toString() );
    	}
	}

}



