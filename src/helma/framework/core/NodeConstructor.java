// NodeConstructor.java
// Copyright (c) Hannes Wallnöfer 2000

package helma.framework.core;

import helma.objectmodel.db.Node;
import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Interpreter.*;

/**
 * A constructor for user defined data types. This first constructs a node, sets its prototype
 * and invokes the scripted constructor function on it.
 */

public class NodeConstructor extends BuiltinFunctionObject {
    	
        RequestEvaluator reval;
        String typename;

        public NodeConstructor (String name, FunctionPrototype fp, RequestEvaluator reval) {
            super(fp, reval.evaluator, name, 1);
            typename = name;
            this.reval = reval;
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return doConstruct(thisObject, arguments);
        }

        public ESObject doConstruct(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode node = null;
           if ("Node".equals (typename) || "hopobject".equalsIgnoreCase (typename)) {
               if (arguments.length == 0) {
                   Node n = new Node ((String) null, (String) null, reval.app.nmgr.safe);
                   node = new ESNode (reval.esNodePrototype, this.evaluator, n, reval);
                   reval.objectcache.put (node.getNode (), node);
               } else {
                   Node n = new Node (arguments[0].toString(), (String) null, reval.app.nmgr.safe);
                   node = new ESNode (reval.esNodePrototype, this.evaluator, n, reval);
                   reval.objectcache.put (node.getNode (), node);
               }
           } else {
               // Typed nodes are instantiated as helma.objectmodel.db.Node from the beginning
               // even if we don't know yet if they are going to be stored in a database. The reason
               // is that we want to be able to use the specail features like subnode relations even for
               // transient nodes.
               ObjectPrototype op = reval.getPrototype (typename);
               Node n = new Node (typename, typename, reval.app.nmgr.safe);
               node = new ESNode (op, reval.evaluator, n, reval);
               node.setPrototype (typename);
               node.getNode ().setDbMapping (reval.app.getDbMapping (typename));
               try {
                   // first try calling "constructor", if that doesn't work, try calling a function
                   // with the name of the type.
                   // HACK: There is an incompatibility problem here, because the property
                   // constructor is defined as the constructor of the object by EcmaScript.
                   if (op.getProperty("constructor", "constructor".hashCode()) instanceof ConstructedFunctionObject)
                       node.doIndirectCall (reval.evaluator, node, "constructor", arguments);
                   else
                       node.doIndirectCall (reval.evaluator, node, typename, arguments);
               } catch (Exception ignore) {}
           }
           return node;
        }

        public ESValue getPropertyInScope(String propertyName, ScopeChain previousScope, int hash) throws EcmaScriptException {
            return super.getPropertyInScope (propertyName, previousScope, hash);
        }

        public ESValue getProperty(String propertyName, int hash) throws EcmaScriptException {
	if ("prototype".equals (propertyName))
	    return reval.getPrototype (typename);
             return super.getProperty(propertyName, hash);
        }

        public String[] getSpecialPropertyNames() {
            String ns[] = {};
            return ns;
        }

    }  // class NodeConstructor



