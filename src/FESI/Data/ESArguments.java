// ESArguments.java
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
 * Implements the "arguments" object for function call
 */
public final class ESArguments extends ESObject {
        
    private ESObject callee;   // Called object
    private int length;        // Number of arguments
    String [] argumentNames;    // Argument names from 0 to n
    // (not readily available) int [] hashCodes;  // Argument hash codes from 0 to n
    
    /**
     * Create a new arguments object - only called by makeNewESArgument 
     * @param prototype The Object prototype
     * @param evaluator The evaluator
     * @param argumentNames The array of argument names
     * @param length The number of arguments (max of names and values
     * @param callee The called object
     */
    private ESArguments(ESObject prototype,
                        Evaluator evaluator,
                        String [] argumentNames, 
                        int length, 
                        ESObject callee) {
        super(prototype, evaluator, (length<3) ? 5 : 11 ); // limit space requirements
        this.argumentNames = argumentNames;  
        this.length = length;
        this.callee = callee;
    }
  
    // overrides
    public ESValue getPropertyInScope(String propertyName, 
                                      ScopeChain previousScope, 
                                      int hash) 
                throws EcmaScriptException {
         if (propertyName.equals("callee")) {
             return callee;
         } else if (propertyName.equals("arguments")) {
             return this;
         } else if (propertyName.equals("length")) {
             return new ESNumber((double) length);
        }
        // directly test on get
        if (super.hasProperty(propertyName, hash)) {
            return (ESValue) super.getProperty(propertyName, hash);
        } 
        int index = -1; // indicates not a valid index value
        try {
            char c = propertyName.charAt(0);
            if ('0' <= c && c <= '9') {
               index = Integer.parseInt(propertyName); 
            }
        } catch (NumberFormatException e) {
        } catch (StringIndexOutOfBoundsException e) { // for charAt
        }
        if (index>=0 && index<argumentNames.length) {
            propertyName = argumentNames[index];
            hash = propertyName.hashCode();
            return super.getProperty(propertyName, hash); // will be defined
        }
        if (previousScope == null) {
          throw new EcmaScriptException("global variable '" + propertyName + "' does not have a value");
        } else {
            return previousScope.getValue(propertyName, hash);
        }
    }
  
    // overrides
    public ESValue doIndirectCallInScope(Evaluator evaluator,
                                        ScopeChain previousScope,
                                        ESObject thisObject, 
                                        String functionName,
                                        int hash, 
                                        ESValue[] arguments) 
                                        throws EcmaScriptException {
        if (functionName.equals("callee")) {
            return callee.callFunction(thisObject,arguments);
        } else {
            return super.doIndirectCallInScope(evaluator, previousScope, thisObject, functionName, hash, arguments);
        }
    }

    // overrides
    public ESValue getProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
                                
         if (propertyName.equals("callee")) {
             return callee;
         } else if (propertyName.equals("arguments")) {
             return this;
         } else if (propertyName.equals("length")) {
             return new ESNumber((double) length);
         } else {
             // Assume that it is more likely a name than a number
             if (super.hasProperty(propertyName, hash)) {
                 return super.getProperty(propertyName, hash);
             }
            int index = -1; // indicates not a valid index value
            try {
                char c = propertyName.charAt(0);
                if ('0' <= c && c <= '9') {
                   index = Integer.parseInt(propertyName); 
                }
            } catch (NumberFormatException e) {
            }
            if (index>=0 && index<argumentNames.length) {
                propertyName = argumentNames[index];
                hash = propertyName.hashCode();
            }
            return super.getProperty(propertyName, hash);
         }
    }

    // overrides
    public ESValue getProperty(int index) 
                            throws EcmaScriptException {
        if (index>=0 && index<argumentNames.length) {
            String propertyName = argumentNames[index];
            return super.getProperty(propertyName, propertyName.hashCode());
        } else {
            String iString = Integer.toString(index);
            return getProperty(iString, iString.hashCode());
        }
    }

    // overrides
    public boolean hasProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
         if (propertyName.equals("callee")) {
             return true;
         } else if (propertyName.equals("arguments")) {
             return true;
         } else if (propertyName.equals("length")) {
             return true;
         } else if (super.hasProperty(propertyName, hash)) {
             return true;
         } else {
            int index = -1; // indicates not a valid index value
            try {
                char c = propertyName.charAt(0);
                if ('0' <= c && c <= '9') {
                   index = Integer.parseInt(propertyName); 
                }            } catch (NumberFormatException e) {
            }
            if (index>=0 && index<argumentNames.length) {
                return true;
            }
            return false;
         }
    }
    
    /**
     * Make a new ESArgument from names and values - the number
     * of names and values do not have to be identical.
     * @param evaluator theEvaluator
     * @param callee the called function
     * @param argumentNames the names of the arguments of the function
     * @param agumentValues the values of the argument.
     * @return the new ESArguments
     */
    public static ESArguments makeNewESArguments(Evaluator evaluator, 
                                ESObject callee,
                                String [] argumentNames,
                                ESValue[] argumentValues) {
        ObjectPrototype op = 
                (ObjectPrototype) evaluator.getObjectPrototype();
        // Get maximum number of arguments (formal or actual), as
        // more than the number of formal arguments can be reached
        // using the (old fashioned) arguments variable).
        int maxArgs = Math.max(argumentValues.length,  argumentNames.length);
        ESArguments args = new ESArguments(op, evaluator, argumentNames, 
                                            maxArgs,
                                            callee);
      try {
        for (int i=0; i<maxArgs; i++) {
            ESValue val = (i<argumentValues.length) ? argumentValues[i] :
                                    ESUndefined.theUndefined;
            if (i<argumentNames.length) {
                args.putProperty(argumentNames[i], val, argumentNames[i].hashCode());
            } else {
                String iString = Integer.toString(i);
                args.putProperty(iString, val, iString.hashCode()); // arguments after name use index as name
            }
        }
       } catch (EcmaScriptException e) {
          e.printStackTrace();
          throw new ProgrammingError(e.getMessage());
       }
        return args;
    }   
    
    // overrides    
    public boolean deleteProperty(String propertyName, int hash) 
                                throws EcmaScriptException {
        return !hasProperty(propertyName, hash); // none can be deleted...
    }
    
    // overrides
    public ESValue getDefaultValue(Evaluator evaluator, int hint) 
                                throws EcmaScriptException {
        return callee.getDefaultValue(hint);
    }
    
    // overrides
    public int getTypeOf() {
        return callee.getTypeOf();
    }

    // overrides
    public Object toJavaObject() {
        return callee.toJavaObject();
    }

    // overrides
    public String getTypeofString() {
        return callee.getTypeofString();
    }
    
    // overrides
    public String toString() {
        return callee.toString();
    }
    
    // overrides
    public String toDetailString() {
        return callee.toDetailString();
    }
    
       
    // overrides
    public String[] getSpecialPropertyNames() {
        String [] ns = {"arguments","callee","length"};
        return ns;
    }

}