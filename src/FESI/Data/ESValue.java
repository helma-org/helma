// ESValue.java
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

import java.util.Enumeration;

import FESI.Exceptions.*;
import FESI.Interpreter.Evaluator;

/**
  * All EcmaScript values are subclasses of this class.
  * The ESValue support many operations which may not be
  * implemented by all type (and then generate an error),
  * to simplify type checking.
  * <P>ESReference are currently not value - see ESReference.
  */
public abstract class ESValue {

    // Codes for the getTypeOf, used to implement "=="   
    public static final int EStypeUndefined = 1;
    public static final int EStypeNull = 2;
    public static final int EStypeBoolean = 3;
    public static final int EStypeNumber = 4;
    public static final int EStypeString = 5;
    public static final int EStypeObject = 6;

    // The following routines access the value as a primitive type. They are
    // the prefered way to access the value of a primitive type or the default
    // value of an object if its type is known.
    // If the object is a reference, it will be dereferenced until a value is
    // found, this may generate an error or return a dummy value.
    //
    // toString() is considered part of these data access routine, however
    // it may never fail. It is implemented as described in 9.8.
    

    /**
     * Returns a detailed description of the value, intended for debugging.
     * (toString returns the official string representation, as defined in 9.8).
     *
     * @return   the detailled information  
     */
    abstract public String toDetailString(); 
    
    /**
     * Return a Java object which is the object to pass to Java routines
     * called by FESI. This may be the corresponding Java object (for
     * example the String), or a wrapper object. When received back from
     * a Java routine, an equivallent (but probably not identical) object
     * must be built.
     *
     * @return   a Java object equivallent to the EcmaScript object. 
     */
    public abstract Object toJavaObject();  
    

    /**
     * Return the double value of this ESValue as defined in 9.3, throw an 
     * exception if not defined.
     *
     * @return     a double
     * @exception   EcmaScriptException  Thrown because by default this is not supported
     */
    public double doubleValue() throws EcmaScriptException {
        throw new EcmaScriptException("Conversion to double unsupported by " + this);
    }
    

    /**
     * Return the boolean value of this ESValue as defined in 9.2, throw an 
     * exception if not defined.
     *
     * @return     a boolean
     * @exception   EcmaScriptException  Thrown because by default this is not supported
     */
    public boolean booleanValue() throws EcmaScriptException {
        throw new EcmaScriptException("Conversion to boolean unsupported by " + this);
    }
    
    /**
     * Return the EcmaScript object of this ESValue as definined in 9.9, throw
     * an exception if not defined.
     *
     * @param   evaluator  The evaluator object
     * @return     an ESObject
     * @exception   EcmaScriptException  Thrown because by default this is not supported
     */
    public ESValue toESObject(Evaluator evaluator) throws EcmaScriptException {
        throw new EcmaScriptException("Conversion to object unsupported by " + this);
    }
    
    // The following routines are derived from the doubleValue of the
    // ESvalue. They may be overriden for efficiency by classes which
    // contain an integer or other equivallent.
    

    /**
     * Return the Integer value, as defined in 9.4.
     *
     * @return     An integer inside a double
     * @exception   EcmaScriptException  Not thrown
     */
     public double toInteger() throws EcmaScriptException {
        double value = this.doubleValue();
        if (Double.isNaN(value)) {
            return 0.0;
        } else if ((value == 0.0) || 
                 Double.isInfinite(value)) {
            return value;
        } else {
            return (double)((long) value);
        }
    }

    /**
     * Return the 32 bit integer, as defined in 9.5
     *
     * @return    The signed 32 bit integer 
     * @exception   EcmaScriptException  Not thrown
     */
    public int toInt32() throws EcmaScriptException {
        double value = this.toInteger();
        return (int) value;
    }

    /**
     * Returned the unsigned 32 bit integer (9.6). Currently
     * implemented as toInt32 !
     *
     * @return     The integer
     * @exception   EcmaScriptException  Not thrown
     */
    public int toUInt32() throws EcmaScriptException {
        double value = this.toInteger();
        return (int) value;
    }

    /**
     * Return the unsigned 16 bit integer (9.7). Currently
     * ignore the sign issue.
     *
     * @return     The unsigned as a short
     * @exception   EcmaScriptException  Not thrown
     */
    public short toUInt16() throws EcmaScriptException {
        double value = this.toInteger();
        return (short) value;
    }
   
   
    // Convertion to EcmaScript primitive type - rebuild the primitive type based
    // on the convertion to the Java primitive type. May be overriden by a subclass
    // for efficiency purpose (especially if it does not require a conversion).
    
    // In fact on toESNumber is used (to implement the operator +), and this
    // is a very minor performance enhancement - so the routine could be
    // easily supressed

    /**
     * Convert the value to an EcmaScript boolean (9.2) if possible
     *
     * @return    The EcmaScript boolean value 
     * @exception   EcmaScriptException  Not thrown
     */
    public ESValue toESBoolean() throws EcmaScriptException {
        return ESBoolean.makeBoolean(this.booleanValue());
    }
    

    /**
     * Convert the value to an EcmaScript string (9.8) if possible
     *
     * @return  The EcmaScript string value (there is always one!)   
     */
    public ESValue toESString() {
        return new ESString(this.toString());
    }

    /**
     * Convert the value to an EcmaScript number (9.3) if possible
     *
     * @return    The EcmaScript number value 
     * @exception   EcmaScriptException  From doubleValue
     */
    public ESValue toESNumber() throws EcmaScriptException {
        double d = this.doubleValue();
        return new ESNumber(d);
    }


    // Provide support to easily distinguish primitive values from other, and
    // to convert values to primitive value. If the desired type is known
    // the direct conversion routines are prefered.
    

    /**
     * Return true if the value is a built-in primitive
     *
     * @return     true if a primitive
     */
    abstract public boolean isPrimitive();

    /**
     * Transform to a primitive as described in 9.1
     *
     * @return     A primitive value
     * @exception   EcmaScriptException  If conversion is impossible
     */
    abstract public ESValue toESPrimitive() throws EcmaScriptException;

    /**
     * Transform to a primitive as described in 9.1, with a specified hint
     *
     * @param   preferedType  the prefered type to return
     * @return     a primitive value
     * @exception   EcmaScriptException  If conversion is impossible
     */
    abstract public ESValue toESPrimitive(int preferedType) throws EcmaScriptException;



    
    // [[Call]] support (to ease check of type)
    public ESValue callFunction(ESObject thisObject, 
                                ESValue[] arguments) 
                            throws EcmaScriptException {
         throw new EcmaScriptException("Function called on non object: " + this);
    }
        
    // [[Construct]] support (to ease check of type)
    public ESObject doConstruct(ESObject thisObject, 
                                ESValue[] arguments) 
                            throws EcmaScriptException {
         throw new EcmaScriptException("'new' called on non object: " + this);
    }    
    
    // abstract public ESValue doNewObject();
    

    
    /**
     * Information routine to check if a value is a number (for array indexing)
     * if true, must implement conversions to double and int without evaluator.
     * @return true if a number.
     */
    public boolean isNumberValue() {
        return false; 
    }
 
    
    /**
     * Information routine to check if a value is a string
     * if true, must implement toString without a evaluator.
     * @return true if a String (ESString or StringPrototype).
     */
    public boolean isStringValue() {
        return false; 
    }

    
    /**
     * Information routine to check if a value is a boolean
     * if true, must implement booleanValue without a evaluator.
     * @return true if a boolean (ESBoolean or BooleanPrototype).
     */
    public boolean isBooleanValue() {
        return false; 
    }

    /**
     * Return a code indicating the type of the object for the implementation
     * of the "==" operator.
     *
     * @return  A type code   
     */
    public abstract int getTypeOf();
    
    /**
     * Return the name of the type of the object for the typeof operator
     *
     * @return  The name of the type as a String   
     */
    public abstract String getTypeofString();

    // Support to list description of objects

    /**
     * Return true if the value is composite (even if not an
     * object). A composite value can be examined by getAllDescriptions.
     * A composite value may have no component!
     *
     * @return     true if composite
     */
    abstract public boolean isComposite();

    /**
     * Return an enumeration of all description of elements of this
     * value (for example properties of an object).
     *
     * @return     Enumerator of all components or NULL.
     */
    public Enumeration getAllDescriptions() {return null;}

    /**
     * Returns a full description of the value, with the specified name.
     *
     * @param name The name of the value to describe
     *
     * @return   the description of this value  
     */
    abstract public ValueDescription getDescription(String name);
                                    


    /**
     * Returns a full description of the unnamed value.
     *
     * @return   the description of this value  
     */
    public ValueDescription getDescription() {
        return getDescription(null);
    }
}