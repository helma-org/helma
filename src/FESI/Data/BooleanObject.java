// BooleanObject.java
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

package FESI.Data;

import FESI.Exceptions.*;
import FESI.Interpreter.*;

/**
 * Implemements the EcmaScript Boolean singleton.
 */
public class BooleanObject extends BuiltinFunctionObject {
        
            
    private BooleanObject(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator, "Boolean", 1);
    }

    // overrides    
    public String toString() {
        return "<Boolean>";
    }
      
    // overrides    
    public ESValue callFunction(ESObject thisObject,
                                    ESValue[] arguments) 
                                            throws EcmaScriptException {
         if (arguments.length==0) {
             return ESBoolean.makeBoolean(false);
         } else {
             return ESBoolean.makeBoolean(arguments[0].booleanValue());
         } 
    }
    
       
    // overrides    
    public ESObject doConstruct(ESObject thisObject, 
                                ESValue[] arguments) 
                                        throws EcmaScriptException {
         BooleanPrototype theObject = null;
         ESObject bp = evaluator.getBooleanPrototype();
         theObject= new BooleanPrototype(bp, evaluator);
         if (arguments.length>0) {
             theObject.value = ESBoolean.makeBoolean(arguments[0].booleanValue());
         } else {
             theObject.value = ESBoolean.makeBoolean(false);
         }
         return theObject;
    }
        
    /**
     * Utility function to create the single Boolean object
     *
     * @param evaluator the Evaluator
     * @param objectPrototype The Object prototype attached to the evaluator
     * @param functionPrototype The Function prototype attached to the evaluator
     *
     * @return the Boolean singleton
     */
    public static BooleanObject makeBooleanObject(Evaluator evaluator,
                                   ObjectPrototype objectPrototype,
                                   FunctionPrototype functionPrototype) {
            
         BooleanPrototype booleanPrototype = new BooleanPrototype(objectPrototype, evaluator);
         BooleanObject booleanObject = new BooleanObject(functionPrototype, evaluator);
        try {
            // For booleanPrototype
            class BooleanPrototypeToString extends BuiltinFunctionObject {
                BooleanPrototypeToString(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
                       throws EcmaScriptException { 
                   ESValue v = ((BooleanPrototype) thisObject).value;
                   String s = v.toString();
                   return new ESString(s);
                }
            }
            class BooleanPrototypeValueOf extends BuiltinFunctionObject {
                BooleanPrototypeValueOf(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException { 
                   return ((BooleanPrototype) thisObject).value;
                }
            }

            booleanObject.putHiddenProperty("prototype",booleanPrototype);
            booleanObject.putHiddenProperty("length",new ESNumber(1));

            booleanPrototype.putHiddenProperty("constructor",booleanObject);
            booleanPrototype.putHiddenProperty("toString", 
               new BooleanPrototypeToString("toString", evaluator, functionPrototype));
            booleanPrototype.putHiddenProperty("valueOf",
               new BooleanPrototypeValueOf("valueOf", evaluator, functionPrototype));
        } catch (EcmaScriptException e) {
            e.printStackTrace();
            throw new ProgrammingError(e.getMessage());
        }
       
         evaluator.setBooleanPrototype(booleanPrototype);

       return booleanObject;   
    }

}