// NumberObject.java
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
 * Implemements the EcmaScript Number singleton.
 */
public class NumberObject extends BuiltinFunctionObject {
        
            
    private NumberObject(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator, "Number", 1);
    }
    
    // overrides
    public String toString() {
        return "<Number>";
    }
     
    // overrides 
    public ESValue callFunction(ESObject thisObject,
                                ESValue[] arguments) 
                                        throws EcmaScriptException {
         if (arguments.length==0) {
             return new ESNumber(0.0);
         } else {
             return new ESNumber(arguments[0].doubleValue());
         }
    } 
    
    // overrides   
    public ESObject doConstruct(ESObject thisObject, 
                                ESValue[] arguments) 
                                        throws EcmaScriptException {
         NumberPrototype theObject = null;
         ESObject np = evaluator.getNumberPrototype();
         theObject= new NumberPrototype(np, evaluator);
         if (arguments.length>0) {
             theObject.value = new ESNumber(arguments[0].doubleValue());
         } else {
             theObject.value = new ESNumber(0.0);
         }
         return theObject;
    }    
   
    /**
     * Utility function to create the single Number object
     *
     * @param evaluator the Evaluator
     * @param objectPrototype The Object prototype attached to the evaluator
     * @param functionPrototype The Function prototype attached to the evaluator
     *
     * @return the Number singleton
     */
    public static NumberObject makeNumberObject(Evaluator evaluator,
                                   ObjectPrototype objectPrototype,
                                   FunctionPrototype functionPrototype) {
                                       
                                    
       NumberPrototype numberPrototype = new NumberPrototype(objectPrototype, evaluator);
       NumberObject numberObject = new NumberObject(functionPrototype, evaluator);

        try {

               // For numberPrototype
            class NumberPrototypeToString extends BuiltinFunctionObject {
                NumberPrototypeToString(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
                       throws EcmaScriptException { 
                   ESValue v = ((NumberPrototype) thisObject).value;
                   String s = v.toString();
                   if (arguments.length>0) {
                       double d = arguments[0].doubleValue();
                       if (!Double.isNaN(d)) {
                           s = Long.toString(((long)v.doubleValue()),(int)d);
                       }
                   }
                   return new ESString(s);
                }
            }
            class NumberPrototypeValueOf extends BuiltinFunctionObject {
                NumberPrototypeValueOf(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException { 
                   return ((NumberPrototype) thisObject).value;
                }
            }

            numberObject.putHiddenProperty("prototype",numberPrototype);
            numberObject.putHiddenProperty("length",new ESNumber(1));
            numberObject.putHiddenProperty("MAX_VALUE",new ESNumber(Double.MAX_VALUE));
            numberObject.putHiddenProperty("MIN_VALUE",new ESNumber(Double.MIN_VALUE));
            numberObject.putHiddenProperty("NaN",new ESNumber(Double.NaN));
            numberObject.putHiddenProperty("NEGATIVE_INFINITY",new ESNumber(Double.NEGATIVE_INFINITY));
            numberObject.putHiddenProperty("POSITIVE_INFINITY",new ESNumber(Double.POSITIVE_INFINITY));
    
            numberPrototype.putHiddenProperty("constructor",numberObject);
            numberPrototype.putHiddenProperty("toString", 
               new NumberPrototypeToString("toString", evaluator, functionPrototype));
            numberPrototype.putHiddenProperty("valueOf",
               new NumberPrototypeValueOf("valueOf", evaluator, functionPrototype));
        } catch (EcmaScriptException e) {
            e.printStackTrace();
            throw new ProgrammingError(e.getMessage());
        }
       
       evaluator.setNumberPrototype(numberPrototype);

       return numberObject;   
   }
}