// ConstructedFunctionObject.java
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
import java.util.Vector;
import FESI.Exceptions.*;
import FESI.AST.ASTStatementList;
import FESI.Interpreter.*;

/**
 * Implements functions constructed from source text
 */
public class ConstructedFunctionObject extends FunctionPrototype {
        
    private static final String PROTOTYPEstring = new String("prototype").intern();
    private static final int PROTOTYPEhash=PROTOTYPEstring.hashCode();
    
    private ASTStatementList theFunctionAST;
    private String [] theArguments;
    private Vector localVariableNames;
    private EvaluationSource evaluationSource = null;
    private String functionSource = null;
    
    private ESValue currentArguments = ESNull.theNull;

    private ConstructedFunctionObject(FunctionPrototype functionPrototype,
                                 Evaluator evaluator,
                                 String functionName,
                                 EvaluationSource evaluationSource,
                                 String functionSource,
                                 String [] arguments,
                                 Vector localVariableNames,
                                 ASTStatementList aFunctionAST) {
        super(functionPrototype, evaluator, functionName, arguments.length);
        this.evaluationSource = evaluationSource;
        this.functionSource = functionSource;
        theFunctionAST = aFunctionAST;
        theArguments = arguments;
        this.localVariableNames = localVariableNames;
        
        //try {
        //   targetObject.putProperty(functionName, this);
       //} catch (EcmaScriptException e) {
       //    throw new ProgrammingError(e.getMessage());
       //}
    }
    
    /**
     * get the string defining the function
     * @return the source string
     */
    public String getFunctionImplementationString() {
        if (functionSource == null) {
           StringBuffer str = new StringBuffer();
           str.append("function ");
           str.append(getFunctionName());
           str.append("(");
           for (int i=0; i<theArguments.length; i++) {
               if (i>0) str.append(",");
               str.append(theArguments[i]);
           }
           str.append(")");
           str.append("function {<internal abstract syntax tree representation>}");
           return str.toString();
        } else {
           return functionSource;
        }
    }
    
    /**
     * Get the list of local variables of the function as a vector
     * @return the Vector of local variable name strings
     */
    public Vector getLocalVariableNames() {
        return localVariableNames;
    }
    
    /**
     * Get the function parameter description as a string
     *
     * @return the function parameter string as (a,b,c)
     */
    public String getFunctionParametersString() {
       StringBuffer str = new StringBuffer();
       str.append("(");
       for (int i=0; i<theArguments.length; i++) {
           if (i>0) str.append(",");
           str.append(theArguments[i]);
       }
       str.append(")");
       return str.toString();
    }
   
    // overrides
    public ESValue callFunction(ESObject thisObject, 
                            ESValue[] arguments)
           throws EcmaScriptException {
        ESValue value = null;
        ESArguments args = ESArguments.makeNewESArguments(evaluator,
                             this,
                             theArguments,
                             arguments); 
        ESValue oldArguments = currentArguments;
        currentArguments = args;
        try {
           value = evaluator.evaluateFunction(theFunctionAST, 
                            evaluationSource,
                            args, 
                            localVariableNames, 
                            thisObject);
        } finally {
            currentArguments = oldArguments;
        }
        return value;
    }
    
    // overrides
    public ESObject doConstruct(ESObject thisObject, 
                            ESValue[] arguments)
           throws EcmaScriptException { 
        ESValue prototype = getProperty(PROTOTYPEstring, PROTOTYPEhash);
        ESObject op = evaluator.getObjectPrototype();
        if (!(prototype instanceof ESObject)) prototype = op;
        ESObject obj = new ObjectPrototype((ESObject) prototype, evaluator);
        ESValue result = callFunction(obj, arguments);
        // The next line was probably a misinterpretation of // 15.3.2.1 (18)
        // which returned an other object if the function returned an object
        // if (result instanceof ESObject) obj = (ESObject) result;
        return obj;
    }
    
    // overrides
    public String toString () {
       return getFunctionImplementationString();
    }
   
    // overrides
    public String toDetailString() {
       StringBuffer str = new StringBuffer();
       str.append("<Function: ");
       str.append(getFunctionName());
       str.append("(");
       for (int i=0; i<theArguments.length; i++) {
           if (i>0) str.append(",");
           str.append(theArguments[i]);
       }
       str.append(")>");
       return str.toString();
    }
   
    /**
     * Utility function to create a function object. Used by the
     * EcmaScript Function function to create new functions
     *
     * @param evaluator the Evaluator
     * @param functionName the name of the new function
     * @param evaluationSource An identification of the source of the function
     * @param sourceString The source of the parsed function
     * @param arguments The array of arguments
     * @param localVariableNames the list of local variable declared by var
     * @param aFunctionAST the parsed function
     *
     * @return A new function object
     */
   public static ConstructedFunctionObject makeNewConstructedFunction(
               Evaluator evaluator,
               String functionName,
               EvaluationSource evaluationSource,
               String sourceString,
               String [] arguments,
               Vector localVariableNames,
               ASTStatementList aFunctionAST) {
        ConstructedFunctionObject theNewFunction = null;
        try {
            FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
             
            theNewFunction = new ConstructedFunctionObject (
                           fp,
                           evaluator,
                           functionName,
                           evaluationSource,
                           sourceString,
                           arguments,
                           localVariableNames,
                           aFunctionAST);
            ObjectPrototype thePrototype = ObjectObject.createObject(evaluator);
            theNewFunction.putHiddenProperty("prototype",thePrototype);
            thePrototype.putHiddenProperty("constructor",theNewFunction);
        } catch (EcmaScriptException e) {
            e.printStackTrace();
            throw new ProgrammingError(e.getMessage());
        }
        return theNewFunction;
    }
    
    // overrides
    public ESValue getPropertyInScope(String propertyName, ScopeChain previousScope, int hash) 
                throws EcmaScriptException {
       if (propertyName.equals("arguments")) {
          return currentArguments;
       } else {
          return super.getPropertyInScope(propertyName, previousScope, hash);
       }
     }

    // overrides
    public ESValue getProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
         if (propertyName.equals("arguments")) {
             return currentArguments;
         } else {
             return super.getProperty(propertyName, hash);
         }
    }

    // overrides
    public boolean hasProperty(String propertyName, int hash) 
                          throws EcmaScriptException {
         if (propertyName.equals("arguments")) {
             return true;
         } else {
             return super.hasProperty(propertyName, hash);
         }
    }

    // overrides
    public void putProperty(String propertyName, ESValue propertyValue, int hash) 
                                throws EcmaScriptException {
         if (!propertyName.equals("arguments")) {
           super.putProperty(propertyName, propertyValue, hash);
         } // Allowed via putHiddenProperty, used internally !
    }
    
   // public ESValue replaceCurrentArguments(ESObject newArguments) {
   //     ESValue oldArguments = currentArguments;
   //     currentArguments = newArguments;
   //     return oldArguments;
   // }

}
