// ESObject.java
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
import FESI.Interpreter.Evaluator;
import FESI.Interpreter.FesiHashtable;
import FESI.Interpreter.ScopeChain;

import java.util.Enumeration;

public abstract class ESObject extends ESValue {

    /** Contains the properties of this object */
    protected FesiHashtable properties;
    
    /** The evaluator owning this object */
    protected Evaluator evaluator;

    /** 
     * The prototype of this object ([[prototype]] in the standard,  
     * not the "prototype" property of functions!) 
     */
    private ESObject prototype = null;
    
    // Prepare common names and their hash value
    static private final String TOSTRINGstring = ("toString").intern();
    static private final int TOSTRINGhash = TOSTRINGstring.hashCode();
    static private final String VALUEOFstring = ("valueOf").intern();
    static private final int VALUEOFhash = VALUEOFstring.hashCode();
    	

    /**
     * Create an object with a specific prototype (which may be null)
     * in the context of a specific evaluator (which may not be null)
     * Uses the default hashTable size.
     *
     * @param   prototype  The prototype ESObject - may be null
     * @param   evaluator  The evaluator - must not be null
     */
    protected ESObject(ESObject prototype, Evaluator evaluator) {
        this.prototype = prototype;
        this.properties = new FesiHashtable();
        this.evaluator = evaluator; // It will crash somewhere if null...
    }
	
    

    /**
     * Create an object with a specific prototype (which may be null)
     * in the context of a specific evaluator (which may not be null)
     * Uses the specified hashTable size, which should be a prime.
     * size is usefull for very small (arguments) or very large objects.
     *
     * @param   prototype  The prototype ESObject - may be null
     * @param   evaluator  The evaluator - must not be null
     */
    protected ESObject(ESObject prototype, Evaluator evaluator, int initialSize) {
        this.prototype = prototype;
        this.properties = new FesiHashtable(initialSize);
        this.evaluator = evaluator; // It will crash somewhere if null...
    }

    /**
     * Get the evaluator for this object
     *
     * @return the evaluator
     */
    public final Evaluator getEvaluator() {
        return evaluator;
    }

    /**
     * All objects and thir subclasses are non primitive
     *
     * @return false
     */
    public final boolean isPrimitive() {
        return false;
    }
    

    /**
     * Implements the [[prototype]] property (see 8.6.2)
     *
     * @return     The prototype object or null
     */
    public ESObject getPrototype() {
        return prototype;
    }
    

    /**
     *  Allow the prototype to be set, added 2001-04-05 by Hannes Wallnöfer
     *
     * @param prototype     The new prototype object
     */
    public void setPrototype(ESObject prototype) {
        this.prototype = prototype;
    }


    /**
     * Return the name of the class of objects ([[class]]), as used in the default toString
     * method of objects (15.2.4.2)
     *
     * @return the [[Class]] property of this object    
     */
    public String getESClassName() {
        return "Object";
    }
    

    /**
     * Return a code indicating the type of the object for the implementation
     * of the "==" operator.
     *
     * @return  A type code   
     */
    public int getTypeOf() {
       return EStypeObject;
    }


    /**
     * Either return the property value of the specified property
     * in the current object, or lookup the next object in the scope chain
     * if there is one. If there is nones, generate an error message.
     * <P>This routine must be overriden by subclass which change the
     * implementation of getProperty.
     *
     * @param   propertyName  The property to look for
     * @param   previousScope  The previous scope or null
     * @param   hash  The hashCode of propertyName
     * @return     The value of the specified variable
     * @exception   EcmaScriptException  if not found in any scope
     */
    public ESValue getPropertyInScope(String propertyName, 
                               ScopeChain previousScope, 
                               int hash) 
                throws EcmaScriptException {
        ESValue value = (ESValue) properties.get(propertyName, hash);
        if (value == null) {
            if (previousScope == null) {
                // Return null for undefined global variables.
                // throw new EcmaScriptException("global variable '" + propertyName + "' does not have a value");
                value = ESNull.theNull; 
            } else {
                value = previousScope.getValue(propertyName, hash);
            }
        }
        return value;
    }
    
    /**
     * Get the property by name (see 8.6.2.1) propagating to
     * the prototype if required
     *
     * @param   propertyName  The string naming the property
     * @param   hash  The hashCode of propertyName
     * @return     The property or <em>undefined</em>
     * @exception   EcmaScriptException  Error in host objects ?
     */
    public ESValue getProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        ESValue value = (ESValue) properties.get(propertyName, hash);
        if (value == null) {
            if (prototype == null) {
                value = ESUndefined.theUndefined;
            } else {
                value = prototype.getProperty(propertyName, hash);
            }
        }
        return value;
    }
    

    /**
     * Get the property by index value. By default the index is
     * converted to a string, but this can be optimized for arrays.
     * <P>This is not the same as the indexed properties of the first
     * version of JavaScript and does not allow to access named
     * properties other than the property using the integer string
     * representation as a name.
     *
     * @param   index  The property name as an integer.
     * @return     The property or <em>undefined</em>
     * @exception   EcmaScriptException  Error in host objects ?
     */
    public ESValue getProperty(int index) 
                            throws EcmaScriptException {
        String iString = Integer.toString(index);
        return getProperty(iString, iString.hashCode());
    }

    public boolean hasProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        boolean found = properties.containsKey(propertyName, hash);
        if (!found && prototype != null) {
            found = prototype.hasProperty(propertyName, hash);
        }
        return found;
    }
    
    public boolean isHiddenProperty(String propertyName, int hash) {
        return properties.isHidden(propertyName, hash);
    }
    
    /** 
     * Indicates that the getProperties return an enumerator to the
     * index rather  rather than to the value index (see ESWrapper).
     *
     * @return false
     */
    public boolean isDirectEnumerator() {
        return false;
    }
    
    /**
     * Returns an enumerator for the key elements of this object,
     * that is all is enumerable properties and the (non hidden)
     * ones of its prototype, etc... As used for the for in 
     * statement.
     *
     * @return the enumerator 
     */
   public Enumeration getProperties() {
         return new Enumeration() {
                Enumeration props = properties.keys();
                String currentKey = null;
                int currentHash = 0;
                boolean inside = false;
                public boolean hasMoreElements() {
                    if (currentKey != null) return true;
                    while (props.hasMoreElements()) {
                       currentKey = (String) props.nextElement();
                       currentHash = currentKey.hashCode();
                       if (inside) {
                          if (properties.containsKey(currentKey, currentHash)) continue;
                       } else {
                          if (isHiddenProperty(currentKey, currentHash)) continue;
                       }
                       return true;  
                    }
                    if (!inside && prototype != null) {
                        inside = true;
                        props = prototype.getProperties();
                        while (props.hasMoreElements()) {
                           currentKey = (String) props.nextElement();
                           currentHash = currentKey.hashCode();
                           if (properties.containsKey(currentKey, currentHash)) continue;
                           return true;  
                        }
                    }
                    return false;
                }
                public Object nextElement() {
                    if (hasMoreElements()) {
                       String key = currentKey;
                       currentKey = null;
                       return key; 
                     } else {
                         throw new java.util.NoSuchElementException();
                     }
                 }
         };
      }
    

    /**
     * Get all properties (including hidden ones), for the command
     * @listall of the interpreter. Include the visible properties of the
     * prototype (that is the one added by the user) but not the
     * hidden ones of the prototype (otherwise this would list
     * all functions for any object).
     *
     * @return An enumeration of all properties (visible and hidden).  
     */
    public Enumeration getAllProperties() {
         return new Enumeration() {
                String [] specialProperties = getSpecialPropertyNames();
                int specialEnumerator = 0;
                Enumeration props = properties.keys(); // all of object properties
                String currentKey = null;
                int currentHash = 0;
                boolean inside = false;     // true when examing prototypes properties
                public boolean hasMoreElements() {
                    // OK if we already checked for a property and one exists
                    if (currentKey != null) return true;
                    // Loop on special properties first
                    if (specialEnumerator < specialProperties.length) {
                        currentKey = specialProperties[specialEnumerator];
                        currentHash = currentKey.hashCode();
                        specialEnumerator++;
                        return true;
                    }
                    // loop on standard or prototype properties
                    while (props.hasMoreElements()) {
                       currentKey = (String) props.nextElement();
                       currentHash = currentKey.hashCode();
                       if (inside) {
                          if (properties.containsKey(currentKey, currentHash)) continue;
                          // SHOULD CHECK IF NOT IN SPECIAL
                       }
                       return true;  
                    }
                    // If prototype properties have not yet been examined, look for them
                    if (!inside && prototype != null) {
                        inside = true;
                        props = prototype.getProperties();
                        while (props.hasMoreElements()) {
                           currentKey = (String) props.nextElement();
                           currentHash = currentKey.hashCode();
                           if (properties.containsKey(currentKey, currentHash)) continue;
                           return true;  
                        }
                    }
                    return false;
                }
                public Object nextElement() {
                    if (hasMoreElements()) {
                       String key = currentKey;
                       currentKey = null;
                       return key; 
                     } else {
                         throw new java.util.NoSuchElementException();
                     }
                 }
         };
    }


    /**
     * Put the property by name (see 8.6.2.2), ignoring if
     * read only (integrate functionality of canPut) and
     * creating it if needed and possible.
     * <P>The routine implement the functionality of the canPut attribute.
     *
     * @param   propertyName  The string naming the property
     * @param   propertyValue  The value to put
     * @exception   EcmaScriptException  Error in host objects ?
     */
    public void putProperty(String propertyName, 
                            ESValue propertyValue, 
                            int hash) 
                                throws EcmaScriptException {
        properties.put(propertyName, hash, false, false, propertyValue);
    }
    

    /**
     * Put the property by index value. By default the index is
     * converted to a string, but this can be optimized for arrays.
     * <P>This is not the same as the indexed properties of the first
     * version of JavaScript and does not allow to access named
     * properties other than the property using the integer string
     * representation as a name.
     *
     * @param   index  The property name as an integer.
     * @param   propertyValue  The value to put
     * @exception   EcmaScriptException  Error in host objects ?
     */
    public void putProperty(int index, ESValue propertyValue) 
                                throws EcmaScriptException {
        String iString = Integer.toString(index);
        putProperty(iString, propertyValue, iString.hashCode());
    }
    
    

    /**
     * Put the property as hidden. This is mostly used by initialization
     * code, so a hash value is computed localy and the string is interned.
     *
     * @param   propertyName  The name of the property
     * @param   propertyValue Its value 
     * @exception   EcmaScriptException  Not used
     */
    public void putHiddenProperty(String propertyName, 
                                  ESValue propertyValue) 
                                throws EcmaScriptException {
        propertyName = propertyName.intern();    
        int hash = propertyName.hashCode();                      
        properties.put(propertyName, hash, true, false, propertyValue);
    }


    /**
     * Implements the [[delete]] function (8.6.2.5), only
     * called by the DELETE operator. Should return true if
     * the propery does not exist any more (or did not exist
     * at all) after the return.
     * <P>This routine must implement the DontDelete attribue too.
     *
     * @param   propertyName  The name of the property
     * @return     true if the property is not present anymore
     * @exception   EcmaScriptException Not used 
     */
    public boolean deleteProperty(String propertyName, int hash) 
                                throws EcmaScriptException {
        properties.remove(propertyName, hash);
        return true; // either it did not exist or was deleted !
    }
    
    /**
     * Implements [[DefaultValue]] with hint
     *
     * @param hint A type hint (only string or number)
     
     * @exception EcmaScriptException Propagated or bad hint
     * @return the default value of this object
     */
    public ESValue getDefaultValue(int hint)
                                throws EcmaScriptException {
        ESValue theResult = null;
        ESValue theFunction = null;
        
        if (hint == ESValue.EStypeString) {
            theFunction = this.getProperty(TOSTRINGstring,TOSTRINGhash);
            if (theFunction instanceof ESObject) {
                theResult = theFunction.callFunction(this, new ESValue[0]);
                if (theResult.isPrimitive()) {
                        return theResult;
               }
            }
            theFunction = this.getProperty(VALUEOFstring,VALUEOFhash);
            if (theFunction instanceof ESObject) {
                theResult = theFunction.callFunction(this, new ESValue[0]);
                if (theResult.isPrimitive()) {
                        return theResult;
               }
            }
	    // Throw errror on super to avoid evaluating this with as a string,
	    // as this is exactly what we cannot do.
           throw new EcmaScriptException ("No default value for " + super.toString() + " and hint " + hint);
        } else if (hint == ESValue.EStypeNumber) {
            theFunction = this.getProperty(VALUEOFstring,VALUEOFhash);
            if (theFunction instanceof ESObject) {
                theResult = theFunction.callFunction(this, new ESValue[0]);
                if (theResult.isPrimitive()) {
                        return theResult;
               }
            }
            theFunction = this.getProperty(TOSTRINGstring,TOSTRINGhash);
            if (theFunction instanceof ESObject) {
                theResult = theFunction.callFunction(this, new ESValue[0]);
                if (theResult.isPrimitive()) {
                        return theResult;
               }
            }
        }
        throw new EcmaScriptException ("No default value for " + this + " and hint " + hint);
    }

    /**
     * Implements [[DefaultValue]] with no hint
     * <P> The default is different for dates
     *
     * @exception EcmaScriptException Propagated
     * @return the default value of this object
     */
    public ESValue getDefaultValue() 
                                throws EcmaScriptException {
 
        return this.getDefaultValue(EStypeNumber);
    }
    

    /**
     * Call a function object - not implemented for default objecr
     *
     * @param   thisObject  The current object
     * @param   arguments  The arguments to the function
     * @return   The calculated value  
     * @exception   EcmaScriptException  thrown because the function is not implemented
     */
    public ESValue callFunction(ESObject thisObject, 
                               ESValue[] arguments) 
                                        throws EcmaScriptException {
         throw new EcmaScriptException("No function defined on: " + this);
    }    
    

    /**
     * A construct as thisObject.functionName() was detected,
     * The functionName is looked up, then a call is made.
     * This avoid creating a dummy function object when one does not
     * exists, like for the ESWrapper objects (where functions are
     * really java methods).
     * <P>Only method which do not use the standard EcmaScript
     * function evaluation mechanism need to override this method.
     *
     * @param   evaluator  The evaluator
     * @param target The original target (for recursive calls)
     * @param   functionName The name of the function property 
     * @param   arguments The arguments of the function 
     * @return  The result of calling the function  
     * @exception   EcmaScriptException  Function not defined
     * @exception NoSuchMethodException Method not found
     */
    public ESValue doIndirectCall(Evaluator evaluator,
    				    ESObject target,
                                  String functionName,
                                  ESValue[] arguments) 
                                        throws EcmaScriptException, NoSuchMethodException {
        ESValue theFunction = (ESValue) properties.get(functionName, functionName.hashCode());
        if (theFunction == null) {
            if (prototype == null) {
            			throw new EcmaScriptException("The function '"+functionName+
                            "' is not defined for object '"+target.toString()+"'");
            } else {
                return prototype.doIndirectCall(evaluator, target, functionName, arguments);
            }
        } else {
           	return theFunction.callFunction(target,arguments);
        }
    }

    // A routine which may return a function as the value of a builtin
    // property must override this function
    public ESValue doIndirectCallInScope(Evaluator evaluator,
                                        ScopeChain previousScope,
                                        ESObject thisObject, 
                                        String functionName,
                                        int hash, 
                                        ESValue[] arguments) 
                                        throws EcmaScriptException {
        ESValue theFunction = (ESValue) properties.get(functionName, hash);
        if (theFunction == null) {
            if (previousScope == null) {
                throw new EcmaScriptException("no global function named '" + functionName + "'");
            } else {
                return previousScope.doIndirectCall(evaluator, thisObject, functionName, hash, arguments);
            }
        }
        return theFunction.callFunction(thisObject,arguments);
    }

    /**
     * Call the constructor - not implemented on default object
     *
     * @param   thisObject  The current object
     * @param   arguments  Arguments to new
     * @return     The created obbjecr
     * @exception   EcmaScriptException  thrown because this function is not implemented
     */
    public ESObject doConstruct(ESObject thisObject, 
                                ESValue[] arguments) 
                                        throws EcmaScriptException {
         throw new EcmaScriptException("No constructor defined on: " + this);
    }    
    
    
    

    /**
     * Return a double value for this object if possible
     *
     * @return  The double value   
     * @exception   EcmaScriptException  If not a suitable primitive
     */
    public double doubleValue() throws EcmaScriptException {
        ESValue value = ESUndefined.theUndefined;
        double d = Double.NaN;
        try {
           value = toESPrimitive(EStypeNumber);
           d = value.doubleValue();
        } catch (EcmaScriptException e) {
            throw new ProgrammingError(e.getMessage());
        }
        return d;
    }
    

    /**
     * Return the boolean value of this object if possible
     *
     * @return  the boolean value   
     * @exception   EcmaScriptException If not a suitable primitive 
     */
    public boolean booleanValue() throws EcmaScriptException {
        return true;
    }
    
    public String toString() {
        ESValue value = ESUndefined.theUndefined;
        String string = null;
        try {
            value = toESPrimitive(EStypeString);
        } catch (EcmaScriptException e) {
            return this.toDetailString();
        }
        string = value.toString();
        return string;
    }


    /**
     * Convert to an object
     *
     * @param   evaluator The evaluator  
     * @return  This   
     * @exception   EcmaScriptException  not thrown
     */
    public final ESValue toESObject(Evaluator evaluator) throws EcmaScriptException {
        return this;
    }
    

    /**
     * Convert to a primitive
     *
     * @param   preferedType For string or number  
     * @return    The primitive value 
     * @exception   EcmaScriptException If no suitable default value 
     */
    public final ESValue toESPrimitive(int preferedType) throws EcmaScriptException {
        return getDefaultValue(preferedType);
    }
    

    /**
     * Convert to a primitive
     *
     * @return    The primitive value 
     * @exception   EcmaScriptException If no suitable default value 
     */
    public final ESValue toESPrimitive() throws EcmaScriptException {
        return getDefaultValue();
    }
    
    /**
     * Return a Java object which is the object to pass to Java routines
     * called by FESI. By default wrap the ESObject in a wrapper object,
     * used by the jslib. Overriden by subclass if a better type can be found.
     *
     * @return   a wrapper object over this ESObject. 
     */
    public Object toJavaObject() {
        return new JSWrapper(this, evaluator);
    }

     

    /**
     * Return the name of the type of the object for the typeof operator
     *
     * @return  The name of the type as a String   
     */
    public String getTypeofString() {
        return "object";
    }
  
    public String toDetailString() {
        return "ES:[" + getESClassName() + "]";
    }
    
    /**
     * Return true to indicate that this value is composite.
     *
     * @return     true
     */
    public boolean isComposite() {return true; }

    
    /**
     * Return the list of proprties which are not listed by getAll,
     * that is all special properties handled directly by getProperty,
     * which are not in the property hash table (they are considered
     * hidden)
     * Must be overriden by a subclass which overrides getProperty!
     *
     * return The array of special property names
     */    
    public String[] getSpecialPropertyNames() {
        return new String[0];
    }

    /**
     * Get an enumeration of the description of various aspects
     * of the object, including all properties.
     */
    public Enumeration getAllDescriptions() {
         return new Enumeration() {
                String [] specialProperties = getSpecialPropertyNames();
                int specialEnumerator = 0;
                Enumeration props = properties.keys();
                String currentKey = null;
                int currentHash = 0;
                boolean inside = false;
                boolean inSpecial = true;
                public boolean hasMoreElements() {
                    // If we have one already, send it
                    if (currentKey != null) return true;
                   // Loop on special properties first
                    if (specialEnumerator < specialProperties.length) {
                        currentKey = specialProperties[specialEnumerator];
                        currentHash = currentKey.hashCode();
                        specialEnumerator++;
                        return true;
                    }
                    inSpecial = false;
                    // Otherwise check in current enumeration
                    while (props.hasMoreElements()) {
                       currentKey = (String) props.nextElement();
                       currentHash = currentKey.hashCode();
                       //if (inside) {
                       //   if (properties.containsKey(currentKey, currentHash)) continue;
                       //}
                       return true;  
                    }
                    // Got to prototype enumeration if needed
                    if (!inside && prototype != null) {
                        inside = true;
                        props = prototype.getProperties();
                        while (props.hasMoreElements()) {
                           currentKey = (String) props.nextElement();
                           currentHash = currentKey.hashCode();
                           //if (properties.containsKey(currentKey, currentHash)) continue;
                           return true;  
                        }
                    }
                    return false;
                }
                public Object nextElement() {
                    if (hasMoreElements()) {
                       String key = currentKey;
                       int hash = key.hashCode();
                       currentKey = null;
                       ESValue value = null;
                       try {
                           value = ESObject.this.getProperty(key, hash);
                       } catch (EcmaScriptException e) {
                           throw new ProgrammingError("Unexpected exception " + e);
                       }
                       String propertyKind;
                       if (inSpecial) {
                           propertyKind = "HIDDEN";
                       } else if (inside && properties.containsKey(key, hash)) {
                          propertyKind = "INVISIBLE";
                       } else {
                          propertyKind = isHiddenProperty(key, hash) ? "HIDDEN" : "VISIBLE";
                       }
                       propertyKind += (inside ? " PROTOTYPE" : " OBJECT");
                       propertyKind += " PROPERTY"; 
                       return new ValueDescription(key,
                                    propertyKind,
                                    value.toString()); 
                     } else {
                         throw new java.util.NoSuchElementException();
                     }
                 }
         };
      }


   /**
     * Returns a full description of the value, with the specified name.
     *
     * @param name The name of the value to describe
     *
     * @return   the description of this value  
     */
    public ValueDescription getDescription(String name) {
       return new ValueDescription(name,
                                    "OBJECT",
                                    this.toString());
    }

}
