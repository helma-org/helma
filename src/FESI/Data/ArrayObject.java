// ArrayObject.java
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

import java.util.Hashtable;
import FESI.Exceptions.*;
import FESI.Interpreter.*;

/**
 * Implements the Array EcmaScript object. This is a singleton
 */
public class ArrayObject extends BuiltinFunctionObject {
       
    private static final String JOINstring = ("join").intern();
    private static final int JOINhash = JOINstring.hashCode();
    private static final String LENGTHstring = ("length").intern();
    private static final int LENGTHhash = LENGTHstring.hashCode();
    private static final String ZEROstring = ("0").intern();
    private static final int ZEROhash = ZEROstring.hashCode();
   
    /**
     * Create a new Array object - used by makeArrayObject
     *
     * @param prototype Must be an ArrayPrototype
     * @param evaluator the evaluator
     */
    private ArrayObject(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator, "Array", 1);
    }

   
    // overrides
    public ESValue callFunction(ESObject thisObject,
                                ESValue[] arguments) 
                                        throws EcmaScriptException {
        return doConstruct(thisObject, arguments);
    } 
    
       
    // overrides
    public ESObject doConstruct(ESObject thisObject, 
                                ESValue[] arguments) 
                                        throws EcmaScriptException {
        ESObject ap = evaluator.getArrayPrototype();
        ArrayPrototype theArray = new ArrayPrototype(ap, evaluator);
        if (arguments.length > 1) {
            for (int i=0; i<arguments.length; i++) {
              String iString = Integer.toString(i);
              theArray.putProperty(iString, arguments[i], iString.hashCode());   
            }
        } else if (arguments.length == 1) {
            ESValue firstArg = arguments[0];
            // Not clear in standard:
            if (firstArg.isNumberValue()) {
                int length = (int) firstArg.toInt32();
               theArray.putProperty(LENGTHstring, firstArg, LENGTHhash);   
            } else {
                theArray.putProperty(ZEROstring, firstArg, ZEROhash);   
            }
        }
        return theArray;
    }    
   

    /**
     * Utility function to create the single Array object
     *
     * @param evaluator the Evaluator
     * @param objectPrototype The Object prototype attached to the evaluator
     * @param functionPrototype The Function prototype attached to the evaluator
     *
     * @return The Array singleton
     */
     
    public static ArrayObject makeArrayObject(Evaluator evaluator,
                                   ObjectPrototype objectPrototype,
                                   FunctionPrototype functionPrototype) {

       ArrayPrototype arrayPrototype = new ArrayPrototype(objectPrototype, evaluator);
       ArrayObject arrayObject = new ArrayObject(functionPrototype, evaluator);

        try {
            // For arrayPrototype
            class ArrayPrototypeToString extends BuiltinFunctionObject {
                ArrayPrototypeToString(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
                       throws EcmaScriptException {
                      BuiltinFunctionObject join = (BuiltinFunctionObject)
                          thisObject.getProperty(JOINstring, JOINhash);
                    return join.callFunction(thisObject, new ESValue[0]);
                }
            }
            class ArrayPrototypeJoin extends BuiltinFunctionObject {
                ArrayPrototypeJoin(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException { 
                   StringBuffer buffer = new StringBuffer();
                   String separator = ",";
                   if (arguments.length > 0) {
                       separator = arguments[0].toString();
                   }
                   int length = (thisObject.getProperty(ArrayObject.LENGTHstring, ArrayObject.LENGTHhash)).toInt32();
                   for (int i =0; i<length; i++) {
                       if (i>0) buffer.append(separator);
                       String iString = Integer.toString(i);
                       ESValue value = thisObject.getProperty(iString,iString.hashCode());
                       if (value!=ESUndefined.theUndefined && value!=ESNull.theNull) {
                           buffer.append(value.toString());
                       }
                   }
                   return new ESString(buffer.toString());
                }
            }
            class ArrayPrototypeReverse extends BuiltinFunctionObject {
                ArrayPrototypeReverse(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 0);
                }
                public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
                       throws EcmaScriptException {
                     if (!(thisObject instanceof ArrayPrototype)) {
                         throw new EcmaScriptException ("reverse only implemented for arrays");
                     }
                     return ((ArrayPrototype) thisObject).reverse();
                }
            }
            class ArrayPrototypeSort extends BuiltinFunctionObject {
                ArrayPrototypeSort(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
                       throws EcmaScriptException {
                     if (!(thisObject instanceof ArrayPrototype)) {
                         throw new EcmaScriptException ("sort only implemented for arrays");
                     }
                     ESValue compareFn = null;
                     if (arguments.length>0) compareFn = arguments[0];
                     return ((ArrayPrototype) thisObject).sort(compareFn);
                }
            }

            arrayObject.putHiddenProperty("prototype",arrayPrototype);
            arrayObject.putHiddenProperty(LENGTHstring,new ESNumber(1));

            arrayPrototype.putHiddenProperty("constructor",arrayObject);
            arrayPrototype.putHiddenProperty("toString", 
               new ArrayPrototypeToString("toString", evaluator, functionPrototype));
            arrayPrototype.putHiddenProperty("join",
               new ArrayPrototypeJoin("join", evaluator, functionPrototype));
            arrayPrototype.putHiddenProperty("reverse",
               new ArrayPrototypeReverse("reverse", evaluator, functionPrototype));
            arrayPrototype.putHiddenProperty("sort",
               new ArrayPrototypeSort("sort", evaluator, functionPrototype));
        } catch (EcmaScriptException e) {
            e.printStackTrace();
            throw new ProgrammingError(e.getMessage());
        }
        
        evaluator.setArrayPrototype(arrayPrototype);

        return arrayObject;
    }
}