// StringObject.java
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
 * Implemements the EcmaScript String singleton.
 */
public class StringObject extends BuiltinFunctionObject {
        
            
    private StringObject(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator, "String", 1);
    }
    
    // overrides
    public String toString() {
        return "<String>";
    }
      
    // overrides
    public ESValue callFunction(ESObject thisObject,
                         ESValue[] arguments) 
                                        throws EcmaScriptException {
         if (arguments.length==0) {
             return new ESString("");
         } else {
             return new ESString(arguments[0].toString());
         }
    } 
    
       
    // overrides
    public ESObject doConstruct(ESObject thisObject, 
                              ESValue[] arguments) 
                                        throws EcmaScriptException {
         StringPrototype theObject = null;
         ESObject sp = evaluator.getStringPrototype();
         theObject= new StringPrototype(sp, evaluator);
         if (arguments.length>0) {
             theObject.value = new ESString(arguments[0].toString());
         } else {
             theObject.value = new ESString("");
         }
         return theObject;
    }    
   
    /**
     * Utility function to create the single String object
     *
     * @param evaluator the Evaluator
     * @param objectPrototype The Object prototype attached to the evaluator
     * @param functionPrototype The Function prototype attached to the evaluator
     *
     * @return the String singleton
     */
    public static StringObject makeStringObject(Evaluator evaluator,
                                   ObjectPrototype objectPrototype,
                                   FunctionPrototype functionPrototype) {

       StringPrototype stringPrototype = new StringPrototype(objectPrototype, evaluator);
       StringObject stringObject = new StringObject(functionPrototype, evaluator);

        try {
            // For stringPrototype
            class StringPrototypeToString extends BuiltinFunctionObject {
                StringPrototypeToString(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
                       throws EcmaScriptException { 
                   return ((StringPrototype) thisObject).value;
                }
            }
            
            class StringPrototypeValueOf extends BuiltinFunctionObject {
                StringPrototypeValueOf(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException { 
                   return ((StringPrototype) thisObject).value;
                }
            }
            
            class StringPrototypeCharAt extends BuiltinFunctionObject {
                StringPrototypeCharAt(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException {
                    String str = thisObject.toString();
                    int pos = 0;
                    if (arguments.length>0) {
                        pos = arguments[0].toInt32();
                    }
                    if (pos>=0 && pos <str.length()) {
                        char c[] = {str.charAt(pos)};
                        return new ESString(new String(c));
                    } else {
                        return new ESString("");
                    }
                }
            }
            
            class StringPrototypeCharCodeAt extends BuiltinFunctionObject {
                StringPrototypeCharCodeAt(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException {
                    String str = thisObject.toString();
                    int pos = 0;
                    if (arguments.length>0) {
                        pos = arguments[0].toInt32();
                    }
                    if (pos>=0 && pos <str.length()) {
                        char c = str.charAt(pos);
                        return new ESNumber((double) c);
                    } else {
                        return new ESNumber(Double.NaN);
                    }
                }
            }
            
            class StringPrototypeIndexOf extends BuiltinFunctionObject {
                StringPrototypeIndexOf(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException {
                    String str = thisObject.toString();
                    int pos = 0;
                    if (arguments.length<=0) {
                        return new ESNumber(-1);
                    }
                    String searched = arguments[0].toString();
                    if (arguments.length>1) {
                        pos = arguments[1].toInt32();
                    }
                    int res = str.indexOf(searched, pos);
                     return new ESNumber(res);
                }
            }
            
            class StringPrototypeLastIndexOf extends BuiltinFunctionObject {
                StringPrototypeLastIndexOf(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException {
                    String str = thisObject.toString();
                    int pos = str.length();
                    if (arguments.length<=0) {
                        return new ESNumber(-1);
                    }
                    String searched = arguments[0].toString();
                    if (arguments.length>1) {
                        double p = arguments[1].doubleValue();
                        if (!Double.isNaN(p)) pos = arguments[1].toInt32(); 
                    }
                    int res = str.lastIndexOf(searched, pos);
                    return new ESNumber(res);
                }
            }

            // This code is replaced by the ReegExp variant when RegExp is loaded
            class StringPrototypeSplit extends BuiltinFunctionObject {
                StringPrototypeSplit(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException {
                    String str = thisObject.toString();
                    ESObject ap = this.evaluator.getArrayPrototype();
                    ArrayPrototype theArray = new ArrayPrototype(ap, this.evaluator);
                    if (arguments.length<=0) {
                        theArray.setSize(1);
                        theArray.setElementAt(thisObject, 0);
                    } else {
                        String sep = arguments[0].toString();
                        if (sep.length()==0) {
                            int l = str.length();
                            theArray.setSize(l);
                            for (int i=0; i<l; i++) {
                                theArray.setElementAt( 
                                           new ESString(str.substring(i,i+1)), i);                                
                            }
                        } else {
                            int i = 0;
                            int start = 0;
                             while (start<str.length()) {
                                int pos = str.indexOf(sep, start);
                                if (pos<0) pos = str.length();
                                // System.out.println("start: " + start + ", pos: " + pos);
                                theArray.setSize(i+1);
                                theArray.setElementAt(
                                        new ESString(str.substring(start, pos)),i);
                                start = pos + sep.length();
                                i++;  
                            }                              
                        }
                    }
                    return theArray;
                }
            }

            class StringPrototypeSubstring extends BuiltinFunctionObject {
                StringPrototypeSubstring(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException {
                    String str = thisObject.toString();
                    int start = 0;
                    int end = str.length();
                    if (arguments.length>0) {
                        start = arguments[0].toInt32();
                    }
                    if (start<0) start = 0;
                    else if (start>str.length()) start = str.length();
                    if (arguments.length>1) {
                        end = arguments[1].toInt32();
                        if (end<0) end = 0;
                        else if (end>str.length()) end = str.length();
                    }
                    if (start>end) {
                        int x = start; start = end; end = x;
                    }
                    return new ESString(str.substring(start, end));
                }
            }

            class StringPrototypeToLowerCase extends BuiltinFunctionObject {
                StringPrototypeToLowerCase(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException {
                    String str = thisObject.toString();
                    
                    return new ESString(str.toLowerCase());
                }
            }

            class StringPrototypeToUpperCase extends BuiltinFunctionObject {
                StringPrototypeToUpperCase(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                                ESValue[] arguments)
                       throws EcmaScriptException {
                    String str = thisObject.toString();
                    
                    return new ESString(str.toUpperCase());
                }
            }

            // For stringObject
            class StringObjectFromCharCode extends BuiltinFunctionObject {
                StringObjectFromCharCode(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
                       throws EcmaScriptException { 
                     ESObject sp = this.evaluator.getStringPrototype();
                     StringPrototype theObject= new StringPrototype(sp, this.evaluator);
                     StringBuffer sb = new StringBuffer();
                     for (int i =0; i<arguments.length; i++) {
                         char c = (char)(arguments[i].toUInt16());
                         sb.append(c);
                     }
                     theObject.value = new ESString(sb.toString());
                     return theObject;
                }
            }

            stringObject.putHiddenProperty("prototype",stringPrototype);
            stringObject.putHiddenProperty("length",new ESNumber(1));
            stringObject.putHiddenProperty("fromCharCode", 
               new StringObjectFromCharCode("fromCharCode", evaluator, functionPrototype));

            stringPrototype.putHiddenProperty("constructor",stringObject);
            stringPrototype.putHiddenProperty("toString", 
               new StringPrototypeToString("toString", evaluator, functionPrototype));
            stringPrototype.putHiddenProperty("valueOf",
               new StringPrototypeValueOf("valueOf", evaluator, functionPrototype));
            stringPrototype.putHiddenProperty("charAt",
               new StringPrototypeCharAt("charAt", evaluator, functionPrototype));
            stringPrototype.putHiddenProperty("charCodeAt",
               new StringPrototypeCharCodeAt("charCodeAt", evaluator, functionPrototype));
            stringPrototype.putHiddenProperty("indexOf",
               new StringPrototypeIndexOf("indexOf", evaluator, functionPrototype));
            stringPrototype.putHiddenProperty("lastIndexOf",
               new StringPrototypeLastIndexOf("lastIndexOf", evaluator, functionPrototype));
            stringPrototype.putHiddenProperty("split",
               new StringPrototypeSplit("split", evaluator, functionPrototype));
            stringPrototype.putHiddenProperty("substring",
               new StringPrototypeSubstring("substring", evaluator, functionPrototype));
            stringPrototype.putHiddenProperty("toLowerCase",
               new StringPrototypeToLowerCase("toLowerCase", evaluator, functionPrototype));
            stringPrototype.putHiddenProperty("toUpperCase",
               new StringPrototypeToUpperCase("toUpperCase", evaluator, functionPrototype));
        } catch (EcmaScriptException e) {
            e.printStackTrace();
            throw new ProgrammingError(e.getMessage());
        }
        
        evaluator.setStringPrototype(stringPrototype);

        return stringObject;
    }
}