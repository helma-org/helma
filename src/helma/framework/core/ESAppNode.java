// ESAppNode.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.framework.core;

import helma.objectmodel.*;
import FESI.Exceptions.*;
import FESI.Data.*;

/**
 * ESApp represents the app node of an application, providing an app-wide transient shared
 * space as well as access to some app related runtime information.
 */

public class ESAppNode extends ESNode {

    private Application app;
    private DatePrototype createtime;

    public ESAppNode (INode node, RequestEvaluator eval) {
	super (eval.esNodePrototype, eval.evaluator, node, eval);
	app = eval.app;
	createtime = new DatePrototype (eval.evaluator, node.created());
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


    public String toString () {
	return ("AppNode "+node.getNameOrID ());
    }

}













































