// JSRWrapper.java
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

import FESI.jslib.*;
import FESI.Exceptions.*;
import FESI.Extensions.Extension;
import FESI.Interpreter.Evaluator;
import FESI.Interpreter.UserEvaluationSource;

import java.io.Reader;

/**
 * Package an EcmaScript object as a JSObject for use by the
 * Netscape like interface.
 */
public class JSWrapper implements JSObject {
    
    protected Evaluator evaluator;
    protected ESObject object;
    
    /**
     * Create a JSWraper for an EcmaScript object
     * @param object the EcmaScript object
     * @param evaluator theEvaluator
     */
    public JSWrapper(ESObject object, Evaluator evaluator) {
       super();
       this.object = object;
       this.evaluator = evaluator;
    }

    // overrides
    public ESObject getESObject() {
        return object;
    }
    
    /**
     * Return the global object attached to this object
     * @return the global object
     */
    public JSGlobalObject getGlobalObject() {
        return new JSGlobalWrapper(evaluator.getGlobalObject(), evaluator);
    }
    
    /**
     * Implements the call the specified EcmaScript method of this object
     *
     * @param   methodName  The name of the method to call
     * @param   args  An array of parameters.
     * @return  The result of the evaluation
     * @exception   JSException  For any error during interpretation
     */
    public Object call(String methodName,Object args[]) throws JSException {
        Object obj = null;
        synchronized (evaluator) {
            try {
                ESValue function = object.getProperty(methodName, methodName.hashCode());
                ESValue[] esargs = null;
                if (args == null) {
                    esargs = new ESValue[0];
                } else {
                    esargs = new ESValue[args.length];
                    for (int i=0; i<args.length; i++) {
                        esargs[i] = ESLoader.normalizeValue(args[i], evaluator);
                    }
                }
                ESValue value = function.callFunction(object, esargs); // should never return null
                obj = value.toJavaObject();
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return obj;
    }
    
    /**
     * Implements the evaluation of a string with this object as the 'this' object.
     * The string is considered a main program (top level return is not allowed)
     *
     * @param   s  The string to evaluate
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    public Object eval(String s) throws JSException {
        Object obj = null;
        synchronized (evaluator) {
            try {
                ESValue value = evaluator.evaluate(s, object, false); // Can return null !
                if (value != null) obj = value.toJavaObject();
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return obj;
    }
    

    /**
     * Evaluate a Reader stream with this object as the 'this' object.
     * Consider the stream being a main program, not allowing the
     * return statement.
     *
     * @param   r  The Reader stream to evaluate
     * @param   d A description of the Reader for error messages
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    public Object eval(Reader r, String d) throws JSException {
        Object obj = null;
        synchronized (evaluator) {
            try {
                UserEvaluationSource ses;
                if (d==null) {
                    ses = new UserEvaluationSource("<Anonymous stream>", null);
                } else {
                    ses = new UserEvaluationSource(d, null);
                }
                ESValue value = 
                    evaluator.evaluate(r, 
                                       object, 
                                       ses,
                                       false); 
                if (value != null) obj = value.toJavaObject();
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return obj;
    }
    
    /**
     * Evaluate a Reader stream with this object as the 'this' object.
     * Consider the stream being a function program, allowing the
     * return statement.
     *
     * @param   r  The Reader stream to evaluate
     * @param   d A description of the Reader for error messages
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    public Object evalAsFunction(Reader r, String d) throws JSException {
        Object obj = null;
        synchronized (evaluator) {
            try {
                UserEvaluationSource ses;
                if (d==null) {
                    ses = new UserEvaluationSource("<Anonymous stream>", null);
                } else {
                    ses = new UserEvaluationSource(d, null);
                }
                ESValue value = 
                    evaluator.evaluate(r, 
                                       object, 
                                       ses,
                                       true); 
                if (value != null) obj = value.toJavaObject();
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return obj;
    }

    /**
     * Implements the evaluation of a string with this object as the 'this' object.
     * The string is considered a function (top level return are allowed)
     * Passing the specified parameters (names and values must have the same length)
     *
     * @param   s  The string to evaluate
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    public Object evalAsFunction(String s) throws JSException {
        
        Object obj = null;
        synchronized (evaluator) {
            try {
                ESValue value = evaluator.evaluate(s, object, true); // Can return null !
                if (value != null) obj = value.toJavaObject();
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return obj;
        /*
          // This work but is less efficient
        evalAsFunction(s, null, null);
        */
    }
    
    
    /**
     * Evaluate a Reader stream with this object as the 'this' object.
     * Consider the stream being a function program, allowing the
     * return statement.
     * Passing the specified parameters (names and values must have the same length)
     *
     * @param   r  The Reader stream to evaluate
     * @param   d A description of the Reader for error messages
     * @param  names the names of the parameters
     * @param  values the values of the parameters
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    public Object evalAsFunction(Reader r, String d, String [] names, Object values[]) throws JSException {
        Object obj = null;
        throw new ProgrammingError("NOT IMPLEMENTED");
        /*
        synchronized (evaluator) {
            try {
                UserEvaluationSource ses;
                if (d==null) {
                    ses = new UserEvaluationSource("<Anonymous stream>", null);
                } else {
                    ses = new UserEvaluationSource(d, null);
                }
                ESValue value = 
                    evaluator.evaluate(r, 
                                       object, 
                                       ses,
                                       true); 
                if (value != null) obj = value.toJavaObject();
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return obj;
        */
    }

    /**
     * Implements the evaluation of a string with this object as the 'this' object.
     * The string is considered a function (top level return are allowed)
     *
     * @param  body  The string to evaluate
     * @param  names the names of the parameters
     * @param  values the values of the parameters
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    public Object evalAsFunction(String body, String [] names, Object values[]) throws JSException {
        Object obj = null;
        synchronized (evaluator) {
            try {
                // Create function
                int argLength = (names==null ? 0 : names.length);
                int checkLength = (values==null ? 0 : names.length);
                if (argLength!=checkLength) {
                    throw new JSException("argument names and values arrays must have the same length, now: " +
                                            argLength + ", " + checkLength);
                }
                ESValue esArgs[] = new ESValue[argLength+1]; // space for body
                for (int i=0; i<argLength; i++) {
                    esArgs[i] = new ESString(names[i]);
                }
                esArgs[argLength] = new ESString(body); // body is the last value
                ESObject fo = evaluator.getFunctionObject();
                ESObject theFunction = fo.doConstruct(null, esArgs);
                // Now call function
                esArgs = new ESValue[argLength]; // just what is needed
                for (int i=0; i<argLength; i++) {
                   esArgs[i] = ESLoader.normalizeValue(values[i], this.evaluator);
                }
                ESValue value = theFunction.callFunction(object, esArgs);
                if (value != null) obj = value.toJavaObject();
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return obj;
    }
    

    /**
     * Implements the get named property of this object.
     *
     * @param   name  The name of the property to get
     * @return  The value of the property
     * @exception   JSException  For any error during interpretation
     */
    public Object getMember(String name) throws JSException {
        Object obj = null;
        synchronized (evaluator) {
            try {
                ESValue value = object.getProperty(name, name.hashCode());
                obj = value.toJavaObject();
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return obj;
    }
    
    /**
     * Implement the get indexed property of this object (useful for arrays).
     *
     * @param   index  The index value of the property (converted
     *                 to string if not an array)
     * @return  The value of the property
     * @exception   JSException  For any error during interpretation
     */
    public Object getSlot(int index) throws JSException {
        Object obj = null;
        synchronized (evaluator) {
            try {
                ESValue value = object.getProperty(index);
                obj = value.toJavaObject();
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return obj;
    }
    
    // This Netscape function is not implemented
    // public static JSObject getWindow(Applet applet) throws JSException;
    
    /**
     * Implement the deletion of a named property of this object
     *
     * @param   name  The name of the property to delete
     * @exception   JSException  For any error during interpretation
     */
    public void removeMember(String name) throws JSException {
        synchronized (evaluator) {
            try {
                object.deleteProperty(name, name.hashCode());
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        // return;
    }
    
    /**
     * Implements the set value of a named property of this object
     *
     * @param   name  The name of the property to set
     * @param   value  The value to set the property to.
     * @exception   JSException  For any error during interpretation
     */
    public void setMember(String name, Object value) throws JSException {
        synchronized (evaluator) {
            try {
                ESValue esvalue = ESLoader.normalizeValue(value, evaluator);
                object.putProperty(name, esvalue, name.hashCode());
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return;
    }
    
    /**
     * Implement the set property by index value. Useful for arrays.
     *
     * @param   index  The index of the property in the array.
     * @param   value  The value to set the property to.
     * @exception   JSException  For any error during interpretation
     */
    public void setSlot(int index, Object value) throws JSException {
        synchronized (evaluator) {
            try {
                ESValue esvalue = ESLoader.normalizeValue(value, evaluator);
                object.putProperty(index, esvalue);
            } catch (EcmaScriptException e) {
                throw new JSException (e.getMessage(), e);
            }
        }
        return;
    }
    
        
    
    /**
     * Implement the creation of a new evaluator, with no extension loaded.
     *
     * @return     The global object of the created evaluator.
     * @exception   JSException  For any error during initialization
     */
    static public JSGlobalObject makeEvaluator() throws JSException {
        Evaluator evaluator = new Evaluator();
        GlobalObject go = evaluator.getGlobalObject();
        return new JSGlobalWrapper(go,evaluator);
    }
    
    /**
     * Implement the creation of a new evaluator, with specfied extensions loaded.
     *
     * @param   extensions  The class name of the extensions to load.
     * @return     The global object of the created evaluator.
     * @exception   JSException  For any error during initialization
     */
    static public JSGlobalObject makeEvaluator(String [] extensions) throws JSException {
        Evaluator evaluator = new Evaluator();
        GlobalObject go = evaluator.getGlobalObject();
        try {
            if (extensions != null) {
                for (int i =0; i<extensions.length; i++) {
                    Object e = evaluator.addMandatoryExtension(extensions[i]);
                    if (e==null) { // Should never happens
                        throw new JSException ("Could not load extension '" + extensions[i] + "'");
                    }
                }
            }
            return new JSGlobalWrapper(go,evaluator);
        } catch (EcmaScriptException e) {
            throw new JSException (e.getMessage(), e);
        }
    }
      
      

    /**
     * Create a built-in function object from a JSFunction object, so that
     * it can be called as a standard function by native objects.
     * Parameters are transformed in JSobjects if possible Java primitives are
     * are left unhacnged and FESI primitives are transformed to Java primitives.
     * @param evaluator the Evaluator
     * @param jsf The function to wrap
     */
    static public ESObject wrapJSFunction(Evaluator evaluator, JSFunction jsf) {
        synchronized (evaluator) {
            final JSFunction theFunction = jsf;
            class WrapedJSFunction extends BuiltinFunctionObject {
                WrapedJSFunction(String name, Evaluator evaluator, FunctionPrototype fp) {
                    super(fp, evaluator, name, 1);
                }
                public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments) 
                       throws EcmaScriptException { 
                   ESValue value = ESUndefined.theUndefined;
                   Object jsArguments[] = new Object[arguments.length];
                   for (int i =0; i<arguments.length; i++) {
                   	 if (arguments[i] instanceof ESWrapper) {
                   	 		jsArguments[i] = ((ESWrapper)arguments[i]).getJavaObject();
                   	 } else if (arguments[i] instanceof ESObject) {
                   	 		jsArguments[i] = new JSWrapper((ESObject) arguments[i], this.evaluator);
                   	 } else {
                   	 		jsArguments[i] = arguments[i].toJavaObject();
                   	 }
                   }
                   try {
                      Object result = theFunction.doCall(
                      			new JSWrapper(thisObject, this.evaluator), jsArguments);
                      value = ESLoader.normalizeValue(result, this.evaluator);
                   } catch (JSException e) {
                       throw new EcmaScriptException(e.getMessage());
                   }  
                   return value;                  
                }
                public ESObject doConstruct(ESObject thisObject, 
                                        ESValue[] arguments) 
                       throws EcmaScriptException { 
                   ESObject value = null;
                   Object jsArguments[] = new Object[arguments.length];
                   for (int i =0; i<arguments.length; i++) {
                   	 if (arguments[i] instanceof ESWrapper) {
                   	 		jsArguments[i] = ((ESWrapper)arguments[i]).getJavaObject();
                   	 } else if (arguments[i] instanceof ESObject) {
                   	 		jsArguments[i] = new JSWrapper((ESObject) arguments[i], this.evaluator);
                   	 } else {
                   	 		jsArguments[i] = arguments[i].toJavaObject(); 
                   	 }
                   }
                   try {
                      Object result = theFunction.doNew(
                      			new JSWrapper(thisObject, this.evaluator), jsArguments);
                      value = ESLoader.normalizeObject(result, this.evaluator);
                   } catch (JSException e) {
                       throw new EcmaScriptException(e.getMessage());
                   }  
                   return value;                  
                }
            }
           return new WrapedJSFunction(jsf.toString(),
                                    evaluator, 
                                    (FunctionPrototype) evaluator.getFunctionPrototype());
       }
    }

    /**
     * Display the string value of the contained object
     * @return The string value
     */
    public String toString() {
        return object.toString();
    }
}