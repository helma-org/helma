package helma.scripting.fesi;

//import helma.framework.core.*;
//import helma.framework.IPathElement;

import helma.objectmodel.INode;
import FESI.Interpreter.Evaluator;
import FESI.Exceptions.EcmaScriptException;
import FESI.Data.*;
//import java.util.*;


public class ESBeanWrapper extends ESWrapper {

	FesiEvaluator eval;

	public ESBeanWrapper (Object object, FesiEvaluator eval)	{
		super (object, eval.getEvaluator());
		this.eval = eval;
	}

//    public void putProperty(String propertyName, ESValue propertyValue, int hash) throws EcmaScriptException {
//	wrapper.putProperty (propertyName, propertyValue, hash);
//    }

//     public ESValue getProperty (int i) throws EcmaScriptException {
// 	return wrapper.getProperty (i);
//     }

    public ESValue getProperty(String propertyName, int hash) throws EcmaScriptException {
		ESValue val = super.getProperty (propertyName, hash);
		if( val instanceof ESWrapper && ((ESWrapper)val).getJavaObject() instanceof INode )	{
			return eval.getNodeWrapper( (INode) ((ESWrapper)val).getJavaObject() );
		}	else	{
			return val;
		}
    }

 
}


