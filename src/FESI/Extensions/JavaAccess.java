// JavaAccess.java
// FESI Copyright (c) Jean-Marc Lugrin, 1999
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2 of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package FESI.Extensions;

import FESI.Parser.*;
import FESI.AST.*;
import FESI.Data.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;

import java.awt.event.*;
import java.util.EventListener;



public class JavaAccess extends Extension {
    
    class GlobalObjectJavaTypeOf extends BuiltinFunctionObject {
        GlobalObjectJavaTypeOf(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
             
           if (arguments.length>0) { 
               Object obj = arguments[0].toJavaObject();
               String cn = (obj==null) ? "null" : ESLoader.typeName(obj.getClass());
               return new ESString(cn);
           }
           return ESUndefined.theUndefined;
        }
    }
    
    class GlobalObjectLoadExtension extends BuiltinFunctionObject {
        GlobalObjectLoadExtension(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
             
           Object ext = null;
           if (arguments.length>0) { 
               String pathName = arguments[0].toString();
               ext = this.evaluator.addExtension(pathName);
           }
           return ESBoolean.makeBoolean(ext!=null);
        }
    }
 
    private Evaluator evaluator = null;
    
    public JavaAccess () {
        super();
    }
    
        
    public void initializeExtension(Evaluator evaluator) throws EcmaScriptException {
 
        this.evaluator = evaluator;
        
        GlobalObject go = evaluator.getGlobalObject();
        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
            
        go.putHiddenProperty("javaTypeOf", 
                   new GlobalObjectJavaTypeOf("javaTypeOf", evaluator, fp));
        go.putHiddenProperty("loadExtension", 
                   new GlobalObjectLoadExtension("loadExtension", evaluator, fp));

        ESPackages packagesObject = (ESPackages) evaluator.getPackageObject();
        String java = ("java").intern();
        ESPackages javaPackages = (ESPackages) packagesObject.getProperty(java,java.hashCode()); 
        go.putHiddenProperty("Packages", packagesObject);
        go.putHiddenProperty(java, javaPackages);
        ESBeans javaBeans = new ESBeans(evaluator); 
        go.putHiddenProperty("Beans", javaBeans);
            
     }
 }
 

 
 