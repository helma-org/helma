// NodeConstructor.java
// Copyright (c) Hannes Wallnöfer 2000

package helma.scripting.fesi;

import helma.objectmodel.db.Node;
import helma.framework.core.*;
import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Interpreter.*;

/**
 * A constructor for user defined data types. This first constructs a node, sets its prototype
 * and invokes the scripted constructor function on it.
 */

public class NodeConstructor extends BuiltinFunctionObject {

        FesiEvaluator fesi;
        String typename;

        public NodeConstructor (String name, FunctionPrototype fp, FesiEvaluator fesi) {
            super(fp, fesi.getEvaluator (), name, 1);
            typename = name;
            this.fesi = fesi;
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return doConstruct(thisObject, arguments);
        }

        public ESObject doConstruct(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           ESNode node = null;
           Application app = fesi.getApplication ();
           if ("Node".equals (typename) || "hopobject".equalsIgnoreCase (typename)) {
               String nodeName = null;
               if (arguments.length == 0)
                   nodeName = arguments[0].toString();
               Node n = new Node (nodeName, (String) null, app.getWrappedNodeManager ());
               node = new ESNode (fesi.getPrototype ("hopobject"), this.evaluator, n, fesi);
               fesi.putObjectWrapper (node.getNode (), node);
           } else {
               // Typed nodes are instantiated as helma.objectmodel.db.Node from the beginning
               // even if we don't know yet if they are going to be stored in a database. The reason
               // is that we want to be able to use the specail features like subnode relations even for
               // transient nodes.
               ObjectPrototype op = fesi.getPrototype (typename);
               Node n = new Node (typename, typename, app.getWrappedNodeManager ());
               node = new ESNode (op, fesi.getEvaluator (), n, fesi);
               node.setPrototype (typename);
               node.getNode ().setDbMapping (app.getDbMapping (typename));
               try {
                   // first try calling "constructor", if that doesn't work, try calling a function
                   // with the name of the type.
                   // HACK: There is an incompatibility problem here, because the property
                   // constructor is defined as the constructor of the object by EcmaScript.
                   if (op.getProperty ("constructor", "constructor".hashCode()) instanceof ConstructedFunctionObject)
                       node.doIndirectCall (fesi.getEvaluator(), node, "constructor", arguments);
                   else
                       node.doIndirectCall (fesi.getEvaluator(), node, typename, arguments);
               } catch (Exception ignore) {}
           }
           return node;
        }

        public ESValue getPropertyInScope(String propertyName, ScopeChain previousScope, int hash) throws EcmaScriptException {
            return super.getPropertyInScope (propertyName, previousScope, hash);
        }

        public ESValue getProperty(String propertyName, int hash) throws EcmaScriptException {
            if ("prototype".equals (propertyName))
                return fesi.getPrototype (typename);
            return super.getProperty(propertyName, hash);
        }

        public String[] getSpecialPropertyNames() {
            String ns[] = {};
            return ns;
        }

    }  // class NodeConstructor



