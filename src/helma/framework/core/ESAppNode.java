// ESAppNode.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import helma.objectmodel.*;
import FESI.Exceptions.*;
import FESI.Data.*;
import FESI.Interpreter.Evaluator;

/**
 * ESApp represents the app node of an application, providing an app-wide transient shared
 * space as well as access to some app related runtime information.
 */

public class ESAppNode extends ESNode {

    private Application app;
    private DatePrototype createtime;

    public ESAppNode (INode node, RequestEvaluator eval) throws EcmaScriptException {
	super (eval.esNodePrototype, eval.evaluator, node, eval);
	app = eval.app;
	createtime = new DatePrototype (eval.evaluator, node.created());
	FunctionPrototype fp = (FunctionPrototype) eval.evaluator.getFunctionPrototype();
	putHiddenProperty("countEvaluators", new AppCountEvaluators ("countEvaluators", evaluator, fp));
	putHiddenProperty("countFreeEvaluators", new AppCountFreeEvaluators ("countFreeEvaluators", evaluator, fp));
	putHiddenProperty("countBusyEvaluators", new AppCountBusyEvaluators ("countBusyEvaluators", evaluator, fp));
	putHiddenProperty("setNumberOfEvaluators", new AppSetNumberOfEvaluators ("setNumberOfEvaluators", evaluator, fp));
    }

    /**
     * Overrides getProperty to return some app-specific properties
     */
    public ESValue getProperty (String propname, int hash) throws EcmaScriptException {
	if ("requestCount".equals (propname)) {
	    return new ESNumber (app.requestCount);
	}
	if ("xmlrpcCount".equals (propname)) {
	    return new ESNumber (app.xmlrpcCount);
	}
	if ("errorCount".equals (propname)) {
	    return new ESNumber (app.errorCount);
	}
	if ("upSince".equals (propname)) {
	    return createtime;
	}
	return super.getProperty (propname, hash);
    }


    class AppCountEvaluators extends BuiltinFunctionObject {
        AppCountEvaluators (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 0);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return new ESNumber (app.allThreads.size());
        }
    }

    class AppCountFreeEvaluators extends BuiltinFunctionObject {
        AppCountFreeEvaluators (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 0);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return new ESNumber (app.freeThreads.size());
        }
    }

    class AppCountBusyEvaluators extends BuiltinFunctionObject {
        AppCountBusyEvaluators (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 0);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return new ESNumber (app.allThreads.size() - app.freeThreads.size());
        }
    }

    class AppSetNumberOfEvaluators extends BuiltinFunctionObject {
        AppSetNumberOfEvaluators (String name, Evaluator evaluator, FunctionPrototype fp) {
            super (fp, evaluator, name, 1);
        }
        public ESValue callFunction (ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
            RequestEvaluator ev = new RequestEvaluator (app);
            if (arguments.length != 1)
                return ESBoolean.makeBoolean (false);
            return ESBoolean.makeBoolean (app.setNumberOfEvaluators (arguments[0].toInt32()));
        }
    }



    public String toString () {
	return ("AppNode "+node.getNameOrID ());
    }

}













































