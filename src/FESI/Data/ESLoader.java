// ESLoader.java
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
import FESI.jslib.JSFunction;

import java.util.Date;
import java.util.Enumeration;

import java.lang.reflect.*;

import java.io.*;
import java.awt.event.*;
import java.util.EventListener;
import java.util.zip.*;

/**
 * Implements the common functionality of package(object) and beans loaders
 */
public abstract class ESLoader extends ESObject {
    
    // Debug support
    static protected boolean debugJavaAccess = false;
    public static void setDebugJavaAccess(boolean b) {
        debugJavaAccess = b;
    }
    static public boolean isDebugJavaAccess() {
        return debugJavaAccess;
    }

    static protected boolean debugLoader = false;
    public static void setDebugLoader(boolean b) {
        debugLoader = b;
    }
    static public boolean isDebugLoader() {
        return debugLoader;
    }

    // Incremental package name
    protected String packageName = null;
    protected ESLoader previousPackage = null;
    protected ClassLoader classLoader = null;

    // the non compatible flag
    static private CompatibilityDescriptor nonCompatible =
        new CompatibilityDescriptor(-1, null, null);
        
      
    /**
     * To contruct the Bean or Package object
     */
    public ESLoader(Evaluator evaluator) {
        super(null, evaluator);
    }

    /**
     * To contruct the Bean or Package object with a specific class loader
     */
    public ESLoader(Evaluator evaluator, ClassLoader loader) {
        super(null, evaluator);
        this.classLoader = loader;
    }

    /**
     * To construct a bean or package sub-object (with a specific 
     * partial or complete package name
     * @param packageName The extension of the package name
     * @param previousPackage Represents the higher level package names
     * @param classLoader the class loader to use for this loader
     * @param evaluator the evaluator    
     */
    public ESLoader(String packageName, 
                     ESLoader previousPackage,
                     ClassLoader classLoader,
                     Evaluator evaluator) {
        super(null, evaluator);
        this.packageName = packageName;
        this.previousPackage = previousPackage;
        this.classLoader = classLoader;
    }
	
    /**
     * Build the prefix name of the package, concatenating
     * all upper level prefix separated by dots
     * @return prefix of the current package name
     */
    protected String buildPrefix() {
        if (previousPackage == null) {
            return null;
        } else {
            String prefix = previousPackage.buildPrefix();
            if (prefix == null) {
                return packageName;
            } else {
                return prefix + "." + packageName;
            }
        }
    }
    
    // overrides
    public ESObject getPrototype() {
        throw new ProgrammingError("Cannot get prototype of Package");
    }
    
    // overrides
    public int getTypeOf() {
       return EStypeObject;
    }

    // overrides
    public ESValue getPropertyInScope(String propertyName, ScopeChain previousScope, int hash) 
                    throws EcmaScriptException {
       throw new EcmaScriptException("A loader object ("+this+") should not be part of a with statement");
    }

    
    // overrides
    public boolean hasProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        return true; // So it can be dereferenced by scopechain
                     // and wont be created
    }
    
    // overrides
    public boolean isHiddenProperty(String propertyName, int hash) {
        return false;
    }
        
    // overrides
    public void putProperty(String propertyName, ESValue propertyValue, int hash) 
                                throws EcmaScriptException {
        return; // None can be put by the user
    }
    
    // overrides
    public void putHiddenProperty(String propertyName, ESValue propertyValue) 
                                throws EcmaScriptException {
        throw new ProgrammingError("Cannot put hidden property in " + this);
    }
    
    // overrides
    public boolean deleteProperty(String propertyName, int hash) 
                                throws EcmaScriptException {
        // all possible package name do potentialy exists and
        // cannot be deleted, as they are recreated at the first
        // reference.
        return false;
    }
    
    // overrides
    public ESValue getDefaultValue(int hint) 
                                throws EcmaScriptException {
        if (hint == EStypeString) {
            return new ESString(this.toString());
        } else {
            throw new EcmaScriptException ("No default value for " + this + 
                                     " and hint " + hint);
        }
    }
    
    // overrides
    public ESValue getDefaultValue() 
                                throws EcmaScriptException {
        return this.getDefaultValue(EStypeString);
    }
        
    // overrides
    public ESObject doConstruct(ESObject thisObject, 
                                ESValue[] arguments) 
                                        throws EcmaScriptException {
        throw new EcmaScriptException("No constructor for loader object: " + this);
    }    
        
    // overrides
    public double doubleValue() {
        double d = Double.NaN;
        return d;
    }

    // overrides
    public boolean booleanValue() {
        return true;
    }

    // overrides
    public String toString() {
        return this.toDetailString();
    }
         
    //---------------------------------------------------------------
    // Tools for the java wrapper objects
    //---------------------------------------------------------------

    /**
     * Returns true if it is a primitive type
     * @param the Class to test
     * @return true if primitive type
     */
    static boolean isBasicClass(Class cls) {
        return cls == String.class ||
               cls == Character.class ||
               cls == Byte.class ||
               cls == Short.class ||
               cls == Integer.class ||
               cls == Long.class ||
               cls == Float.class ||
               cls == Double.class ||
               cls == Boolean.class ||
               cls == Date.class;
    }
      
    //  With the Hop, all instances of Number (including java.math.BigXXX) are 
    //  treated as native numbers, so this is not called by normalizeValue.
    static boolean isPrimitiveNumberClass(Class cls) {
        return cls == Byte.class ||
               cls == Short.class ||
               cls == Integer.class ||
               cls == Long.class ||
               cls == Float.class ||
               cls == Double.class;
    }
      
    /**
     * Transform a java object to an EcmaScript value (primitive if possible)
     * @param obj the object to transform
     * @param evaluator the evaluator
     * @return the EcmaScript object
     * @exception EcmaScriptException the normalization failed
     */
    public static ESValue normalizeValue(Object obj, Evaluator evaluator) 
                                throws EcmaScriptException  {
        if (obj == null) {
            return ESNull.theNull;
        } else if (obj instanceof String) {
            return new ESString((String) obj);
        } else if (obj instanceof Number) {
            return new ESNumber(((Number) obj).doubleValue());
        } else if (obj instanceof Boolean) {
            return ESBoolean.makeBoolean(((Boolean) obj).booleanValue());
        } else if (obj instanceof Character) {
            return new ESNumber(((Character) obj).charValue());
        } else if (obj instanceof JSFunction) {
            return JSWrapper.wrapJSFunction(evaluator, (JSFunction) obj);  
        } else if (obj instanceof JSWrapper) {
            return ((JSWrapper)obj).getESObject();
        } else if (obj instanceof Date) {
           return new DatePrototype(evaluator, (Date) obj);
        } else if (obj instanceof ESWrapper) {
           return (ESWrapper) obj; // A wrapper received externally
        } else if (obj instanceof ESArrayWrapper) {
           return (ESArrayWrapper) obj; // An array wrapper received externally  
        } else if (obj.getClass().isArray()) {
            return new ESArrayWrapper(obj, evaluator);
        } // else if (obj instanceof helma.framework.IPathElement) {   // Hannes Wallnoefer, 13. Aug 2001
        //     return evaluator.engine.getElementWrapper ((helma.framework.IPathElement) obj);
        // }
        // return new ESWrapper(obj, evaluator);
        return evaluator.engine.getObjectWrapper (obj);
    }
    
    /**
     * Transform a java object to an EcmaScript object (not a primitive)
     * @param obj the object to transform
     * @param evaluator the evaluator
     * @return the EcmaScript object
     * @exception EcmaScriptException the normalization failed
     */
    public static ESObject normalizeObject(Object obj, Evaluator evaluator) 
                            throws EcmaScriptException {
        ESValue value = normalizeValue(obj, evaluator);
        return (ESObject) value.toESObject(evaluator);
    }
    
     /**
      * Convert the primitive class types to their  corresponding
      * class types. Must be called for primitive classes only.
      *
      *  @param target The primitive class to convert
      *  @return The converted class
      */
     private static Class convertPrimitive(Class target) {
            if (target==java.lang.Boolean.TYPE) {
                target=Boolean.class;
            } else if (target==java.lang.Character.TYPE) {
                target=Character.class;
            } else if (target==java.lang.Byte.TYPE) {
                target=Byte.class;
            } else if (target==java.lang.Short.TYPE) {
                target=Short.class;
            } else if (target==java.lang.Integer.TYPE) {
                target=Integer.class;
            } else if (target==java.lang.Long.TYPE) {
                target=Long.class;
            } else if (target==java.lang.Float.TYPE) {
                target=Float.class;
            } else if (target==java.lang.Double.TYPE) {
                target=Double.class;
            }  else {
                throw new ProgrammingError("Not a recognized primitive type: " + target);
            }
        return target;
    }
    
    /**
     * Get a number correlated to the wideness of the class in some lousy sense
     */
    static private int getNumberSize(Class cls) {
        
        if (cls == Byte.class) {
            return 1;
        } else if (cls == Character.class) {
            return 2;
        } else if (cls == Short.class) { // short and char widen in the same way
            return 2;
        } else if (cls == Integer.class) {
            return 3;
        } else if (cls == Long.class) {
            return 4;
        } else if (cls == Float.class) {
            return 5;
        } else if (cls == Double.class) {
            return 6;
        } else {
            throw new ProgrammingError("Unexpected number class");
        }
    }
    
    /**
     * Check that each object in the paremeter array can be converted 
     * to the type specified by the target array and convert them if needed.
     * <P>Even if the parameters are compatible with an EcmaScript value,

     * some may have to be converted to an intermediate type. For example
     * an EcmaScript string of 1 character long is compatible with a Java
     * Character, but some conversion is needed. Arrays need a similar
     * special processing.
     * <P> The parameters have been converted to java Objects by the routine
     * toJavaObject. Wrapped objects (java objects given to an EcmaScript 
     * routine and given back as parameters) have been unwrapped, so they 
     * are their original object again (including arrays), we do therefore 
     * not have ESWrapped objects as parameters. 
     * ESObjects have been wrapped in a JSObject,  including Array objects. 
     * The handling of array is more delicate as they
     * could not be converted to a cannonical form - we must know the element
     * type to understand if they are convertible.
     * <P> The target classes which are primitive are converted to their
     * object counterpart, as only object can be given as parameter and the
     * invoke mechanism will convert them back to primitive if needed.
     * <P>All the conversion needed are described in the returned
     * compatibilityDescriptor and will only be applied for the selected 
     * routine.
     * <P>The distance is a metric on how "far" a parameter list is
     * from the immediate value (used currntly only for simple value
     * widening). It allows to select the routine having the nearest
     * parameter list. This is not totally full proof and multiple routine
     * can have the same distance (including 0, because of the conversion
     * of primitive type to the corresponding objects).
     * <P>The tracing must allow to find the logic of the conversion.
     * @param target The class objects of the target routine
     * @param parms The EcmaScript objects converted to Java objects (IN/OUT)
     * @return a compatibility descriptor.
     */
    static CompatibilityDescriptor areParametersCompatible(Class target[], Object params[]) {
        int n = target.length;
        if (n!=params.length) return nonCompatible; // Ensure we have the same number
        boolean [] convertToChar = null; // flag to indicate if conversion to char is needed
        Object [] convertedArrays = null; // Converted array if any needed
        int distance = 0; // For perfect match, something added for widening
        for (int i=0; i<n; i++) {
            boolean accepted = false;
            Class targetClass = target[i];
            Class sourceClass = null;
            String debugInfo = " not accepted";
            if (params[i] == null) {
                // A null parameter is of type Object, so we check
                // that whatever is the target class be an object
                if (targetClass.isPrimitive()) { // or: Object.class.isAssignableFrom(targetClass)
                    accepted = false;
                    debugInfo = " rejected (null cannot be assigned to primitive)";

                } else {
                    accepted = true;
                    debugInfo = " accepted (null to Object)";
                }
            } else {
                // We consider all primitive types as they object
                // equivallent, as the parameter can only be done as
                // object anyhow. Invoke will convert back if needed.
                if (targetClass.isPrimitive()) {
                    // To accept long by Long, etc... - must be done after test of assigning null to object
                     targetClass = convertPrimitive(targetClass); 
                }  
                // The simplest case is direct object compatibility              
                sourceClass = params[i].getClass();
                accepted = targetClass.isAssignableFrom(sourceClass);
                debugInfo = " accepted (subclassing)";
                
                if (!accepted) {
                    // If we do not have direct object compatibility, we check various
                    // allowed conversions.
                    
                    // Handle number and number widening
                    if ((isPrimitiveNumberClass(sourceClass) ||
                         sourceClass == Character.class) 
                                 && isPrimitiveNumberClass(targetClass)) {
                        // Can be widened ?
                        int targetSize = getNumberSize(targetClass);
                        int sourceSize = getNumberSize(sourceClass);
                        if (targetSize>sourceSize) {
                            accepted = true; // if == already accepted because same class
                                             // or must be rejected (because char != short)
                            distance += Math.abs(targetSize-sourceSize);
                            debugInfo = " accepted (number widening: " + distance + ")";
                        } else {
                            debugInfo = " rejected (not widening numbers)";
                        }
                    // Handle String of length 1 as a Char, which can be converted to a number    
                    } else if ((targetClass == Character.class ||
                                targetClass == Integer.class ||          
                                targetClass == Long.class ||          
                                targetClass == Float.class ||          
                                targetClass == Double.class)          
                                              && params[i] instanceof String) {
                        if (((String) params[i]).length()==1) {
                            accepted = true; // will require conversion of parameter
                            if (convertToChar == null) {
                                convertToChar = new boolean[n];
                            }
                            convertToChar[i] = true;
                            debugInfo = " accepted (String(1) as Character)";
                        } else {
                            debugInfo = " rejected (String not of length 1)";
                        }
                    
                    // Handle array conversion if not from a native java array    
                    } else if (targetClass.isArray()) {
                        if (params[i] instanceof JSWrapper) {
                            ESObject esArray = ((JSWrapper) params[i]).getESObject();
                            if (esArray instanceof ArrayPrototype) {
                                Object array = null;
                                try {
                                    // We convert to the orginal class (possibly a primitive type)
                                    array = ((ArrayPrototype) esArray).toJavaArray(targetClass.getComponentType());
                                    accepted = true;
                                    debugInfo = " accepted (array converted)";
                                    if (convertedArrays == null) {
                                        convertedArrays = new Object[n];
                                    }
                                    convertedArrays[i] = array; // save it for replacement at end
                                } catch (EcmaScriptException e) {
                                    // ignore
                                    debugInfo = " rejected ("+e.getMessage()+")";
                                }
                            } else {
                                debugInfo = " rejected (EcmaScript object is not an array)";
                            }
                        } else {
                            debugInfo = " rejected (only same native array or EcmaScript Array can be assigned to array)";
                        }
                        
                    } else {
                       debugInfo = " rejected (incompatible types)";
                    }
                } // if ! acccepted
            } // if ! null
            
            if (debugJavaAccess) System.out.println (" Assign " + sourceClass + 
                                    " to " + target[i] +
                                    debugInfo);
            if (!accepted) return nonCompatible;
            
        } // for
        
        // Compatible, return appropriate descriptor for future
        // processing of conversion of the "nearest" method
        return new CompatibilityDescriptor(distance,convertToChar,convertedArrays);
    }

    // overrides
    static public String typeName(Class t) {
        String brackets = "";
        while (t.isArray()) {
            brackets += "[]";
            t = t.getComponentType();
        }
        return t.getName() + brackets;
    }
    

}
