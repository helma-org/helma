// ArrayPrototype.java
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

import java.util.Vector;
import java.util.Enumeration;
import java.lang.reflect.Array;

/**
 * Implements the prototype and is the class of all Array objects
 */
public class ArrayPrototype extends ESObject {
    
    private static final String LENGTHstring = ("length").intern();
    private static final int LENGTHhash = LENGTHstring.hashCode();

    // The array value
    // We could use a non synchronized vector or directly and array
    protected Vector theArray = new Vector();
    
    /**
     * Create a new empty array
     * @param prototype the ArrayPrototype
     * @param evaluator The evaluator
     */
    public ArrayPrototype(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator);
    }
    
    // overrides
    public String getESClassName() {
        return "Array";
    }

     /**
     * Return a Java array object which is the object to pass to Java routines
     * called by FESI. 
     * @ param componentType the type of the component of the array
     * @return   a java array object  
     */
   public Object toJavaArray(Class componentType) throws EcmaScriptException {
       int l = size();
       Object array = Array.newInstance(componentType, l);
       if (l ==0) return array;
       for (int i =0; i<l; i++) {
           ESValue element = (ESValue) theArray.elementAt(i);
           if (componentType==Integer.TYPE) {
               if (element.isNumberValue()) {
                   double d = element.doubleValue();
                   int value = (int) d;
                   if (value != d) {
                        throw new EcmaScriptException("An element (" + element + ") of array is too large for class " + componentType);
                    }
                   Array.setInt(array,i,value);
               } else {
                   throw new EcmaScriptException("An element (" + element + ") of array cannot be converted to class " + componentType);
               }
            } else if (componentType==Short.TYPE) {
               if (element.isNumberValue()) {
                   double d = element.doubleValue();
                   short value = (short) d;
                   if (value != d) {
                        throw new EcmaScriptException("An element (" + element + ") of array is too large for class " + componentType);
                    }
                   Array.setShort(array,i,value);
               } else {
                   throw new EcmaScriptException("An element (" + element + ") of array cannot be converted to class " + componentType);
               }
            } else if (componentType==Byte.TYPE) {
               if (element.isNumberValue()) {
                   double d = element.doubleValue();
                   byte value = (byte) d;
                   if (value != d) {
                        throw new EcmaScriptException("An element (" + element + ") of array is too large for class " + componentType);
                    }
                   Array.setByte(array,i,value);
               } else {
                   throw new EcmaScriptException("An element (" + element + ") of array cannot be converted to class " + componentType);
               }
            } else if (componentType==Long.TYPE) {
               if (element.isNumberValue()) {
                   double d = element.doubleValue();
                   long value = (long) d;
                   if (value != d) {
                        throw new EcmaScriptException("An element (" + element + ") of array is too large for class " + componentType);
                    }
                   Array.setLong(array,i,value);
               } else {
                   throw new EcmaScriptException("An element (" + element + ") of array cannot be converted to class " + componentType);
               }
            } else if (componentType==Float.TYPE) {
               if (element.isNumberValue()) {
                   double d = element.doubleValue();
                   float value = (float) d;
                   if (value != d) {
                        throw new EcmaScriptException("An element (" + element + ") of array is too large for class " + componentType);
                    }
                   Array.setFloat(array,i,value);
               } else {
                   throw new EcmaScriptException("An element (" + element + ") of array cannot be converted to class " + componentType);
               }
            } else if (componentType==Double.TYPE) {
               if (element.isNumberValue()) {
                   double d = element.doubleValue();
                   Array.setDouble(array,i,d);
               } else {
                   throw new EcmaScriptException("An element (" + element + ") of array cannot be converted to class " + componentType);
               }
            } else if (componentType==Boolean.TYPE) {
               if (element.isBooleanValue()) {
                   boolean b = element.booleanValue();
                   Array.setBoolean(array,i,b);
               } else {
                   throw new EcmaScriptException("An element (" + element + ") of array cannot be converted to class " + componentType);
               }
            } else if (componentType==Character.TYPE) {
               if (element.isStringValue()) {
                   String s = element.toString();
                   if (s.length()!=1) {
                        throw new EcmaScriptException("A string (" + element + ") of array is not of size 1 for conversion to Character");
                   }
                   Array.setChar(array,i,s.charAt(0));
               } else {
                   throw new EcmaScriptException("An element (" + element + ") of array cannot be converted to class " + componentType);
               }
            } else {
                Object o = element.toJavaObject();
                if (o==null) {
                   Array.set(array,i,o);
                } else {   
                   Class sourceClass = o.getClass();
                   if (componentType.isAssignableFrom(sourceClass)) {
                       Array.set(array,i,o);
                   } else {
                       throw new EcmaScriptException("An element (" + element + ") of array cannot be converted to class " + componentType);
                   }
               }
           }
       }
       return array;
    }
    
    // overrides
    public String toDetailString() {
        return "ES:[" + getESClassName() + ":" + this.getClass().getName() +"]";
    }    
    
    /**
     * Return the size of the array
     * @return the size as an int
     */
    public int size() {
        return theArray.size();
    }
    
    /**
     * Set the size of the array, truncating if needed
     * @param size the new size 90 or positive)
     */
    public void setSize(int size) {
        theArray.setSize(size);
    }
    
    /**
     * Set the value of a specific element
     *
     * @param theElement the new element value
     * @param index the index of the element
     */
    public void setElementAt(ESValue theElement, int index) {
        theArray.setElementAt(theElement, index);
    }
    
    /**
     * Reverse the orders of the elements in an array
     * @return the reversed array (which is the same as this one)
     */
    public ESValue reverse() throws EcmaScriptException { 
        int size = theArray.size();
        if (size>0) {
            Vector reversed = new Vector(size);
            reversed.setSize(size);
            for (int i = 0, j=size-1; i<size; i++, j--) {
                reversed.setElementAt(theArray.elementAt(j),i);
            }
            theArray = reversed;
        }
        return this;
    }
    
   
    // This routines are taken from Java examples in a nutshell 
    static interface Comparer {
      /**
       * Compare objects and return a value that indicates their relative order:
       * if (a > b) return a number > 0; 
       * if (a == b) return 0;
       * if (a < b) return a number < 0. 
       **/
        int compare(ESValue a, ESValue b) throws EcmaScriptException;
    }
  
    static class DefaultComparer implements Comparer {
       public int compare(ESValue v1, ESValue v2) throws EcmaScriptException {
           ESValue v1p = v1.toESPrimitive(ESValue.EStypeNumber);
           ESValue v2p = v2.toESPrimitive(ESValue.EStypeNumber);
           if (v1p == ESUndefined.theUndefined && v2p == ESUndefined.theUndefined) return 0;
           if (v1p == ESUndefined.theUndefined ) return 1;
           if (v2p == ESUndefined.theUndefined ) return -1;
           //System.out.println("v1p = " + v1 + " v2p = " + v2); 
           String s1 = v1.toString();
           String s2 = v2.toString();
           //System.out.println("s1 = " + s1 + " s2 = " + s2); 
           return s1.compareTo(s2);
       }
    }

  /**
   * This is the main sort() routine.  It performs a quicksort on the elements
   * of array a between the element from and the element to.  
   * The Comparer argument c is used to perform
   * comparisons between elements of the array.  
   **/
  private void sort(int from, int to, 
                          Comparer c) throws EcmaScriptException
  {
    // If there is nothing to sort, return
    if (theArray.size() < 2) return;

    // This is the basic quicksort algorithm, stripped of frills that can make
    // it faster but even more confusing than it already is.  You should
    // understand what the code does, but don't have to understand just 
    // why it is guaranteed to sort the array...
    // Note the use of the compare() method of the Comparer object.
    int i = from, j = to;
    ESValue center = (ESValue) theArray.elementAt((from + to) / 2);
    do {
      ESValue ai = (ESValue) theArray.elementAt(i);
      ESValue aj = (ESValue) theArray.elementAt(j);
      while((i < to) && (c.compare(center, ai) > 0)) { i++; ai = (ESValue) theArray.elementAt(i);}
      while((j > from) && (c.compare(center, aj) < 0)) {j--; aj = (ESValue) theArray.elementAt(j);}
      if (i < j) { 
        Object tmp = ai;  theArray.setElementAt(aj,i);  theArray.setElementAt(tmp, j);       
      }
      if (i <= j) { i++; j--; }
    } while(i <= j);
    if (from < j) sort(from, j, c); // recursively sort the rest
    if (i < to) sort(i, to, c);
  }

   /**
    * Sort the array with a specified compare routine
    * @param compareFn A function returning a comparer 
    * @return the sorted array (in place)
    */
   public ESValue sort(ESValue compareFn) throws EcmaScriptException {
        if ((compareFn != null) && 
                    (!(compareFn instanceof FunctionPrototype))) {
            throw new EcmaScriptException("Compare function not a function: "  + compareFn);
        }
        Comparer c = null;
        if (compareFn != null) 
            c = new FunctionComparer((FunctionPrototype) compareFn);
        else 
            c = new DefaultComparer();
        
        sort(0, theArray.size()-1, c);
        return this;
    }
    
    // overrides
    public void putProperty(String propertyName, ESValue propertyValue, int hash) 
                                throws EcmaScriptException {
        if (hash==LENGTHhash && propertyName.equals(LENGTHstring)) {
            int length = (int) (((ESPrimitive) propertyValue).doubleValue());
            if (length<0) {
                throw new EcmaScriptException("Invalid length value: " + propertyValue);
            }
            theArray.setSize(length);
        } else {
            int index = -1; // indicates not a valid index value
            try {
                index = Integer.parseInt(propertyName); // should be uint
            } catch (NumberFormatException e) {
            }
           if (index<0) { 
               super.putProperty(propertyName, propertyValue, hash);
           } else {
               putProperty(index, propertyValue);
           }
       }
    }

    // overrides
    public void putProperty(int index, ESValue propertyValue) 
                                throws EcmaScriptException {
                                           
       if (index>=theArray.size()) {
           theArray.setSize(index+1);
       }
       theArray.setElementAt(propertyValue, index);
    }

    // overrides
    public ESValue getPropertyInScope(String propertyName, ScopeChain previousScope, int hash) 
                throws EcmaScriptException {
        if (hash==LENGTHhash && propertyName.equals(LENGTHstring)) {
             return new ESNumber(theArray.size());
        }
        if (hasProperty(propertyName, hash)) {
            return getProperty(propertyName, hash);
        }
        if (previousScope == null) {
          throw new EcmaScriptException("global variable '" + propertyName + "' does not have a value");
        } else {
            return previousScope.getValue(propertyName, hash);
        }
    }

    // overrides
    public ESValue getProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        if (hash==LENGTHhash && propertyName.equals(LENGTHstring)) {
             return new ESNumber(theArray.size());
         } else {
            int index = -1; // indicates not a valid index value
            try {
                index = Integer.parseInt(propertyName); // should be uint
            } catch (NumberFormatException e) {
            }
            if (index<0) {
               return super.getProperty(propertyName, hash);
            } else {
               return getProperty(index);
            }
         }
     }
     
    // overrides
    public ESValue getProperty(int index) 
                            throws EcmaScriptException {
         Object theElement = null;
         if (index<theArray.size()) theElement = theArray.elementAt(index);
         if (theElement == null) {
             return ESUndefined.theUndefined;
         } else {
             return (ESValue) theElement;
         }
     }
     
    // overrides
    public boolean hasProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        if (hash==LENGTHhash && propertyName.equals(LENGTHstring)) {
             return true;
         } else {
            int index = -1; // indicates not a valid index value
            try {
                index = Integer.parseInt(propertyName); // should be uint
            } catch (NumberFormatException e) {
            }
            if (index<0) {
               return super.hasProperty(propertyName, hash);
            } else {
               return index<theArray.size();
            }
         }
    }

    // overrides
    // Skip elements which were never set (are null), as Netscape
    /*
    OLD - DID IGNORE THE NORMAL PROPERTIES OF AN ARRAY
    public Enumeration getProperties() {
         return new Enumeration() {
                int nextIndex = 0;
                public boolean hasMoreElements() {
                    while ( (nextIndex<theArray.size()) &&
                            (theArray.elementAt(nextIndex) == null))
                         nextIndex++;       
                    return nextIndex<theArray.size();
                }
                public Object nextElement() {
                    if (hasMoreElements()) {
                        return new ESNumber(nextIndex++);
                     } else {
                         throw new java.util.NoSuchElementException();
                     }
                 }
         };
      }
    */
    /**
     * Returns an enumerator for the key elements of this object,
     * that is all is enumerable properties and the (non hidden)
     * ones of its prototype, etc... As used for the for in 
     * statement.
     *<P> Skip elements which were never set (are null), as Netscape
     * SHOULD USE SUPER INSTEAD !
     * @return the enumerator 
     */
   public Enumeration getProperties() {
         return new Enumeration() {
                Enumeration props = properties.keys();
                Object currentKey = null;
                int currentHash = 0;
                int nextIndex = 0;
                boolean inside = false;
                ESObject prototype = ArrayPrototype.this.getPrototype();
                public boolean hasMoreElements() {
                		  // Check if hasMoreElements was already called
                    if (currentKey != null) return true;
                    
                    // Check if a numeric key is appropriate
                    while ( (nextIndex<theArray.size()) &&
                            (theArray.elementAt(nextIndex) == null)) {
                         nextIndex++;  
                    }     
                    if (nextIndex<theArray.size()) {
                    		// Should it be an ESNumber?
                      	currentKey = new ESNumber(nextIndex++);
                    		return true;
                    }
                   
                    while (props.hasMoreElements()) {
                       currentKey = props.nextElement();
                       currentHash = currentKey.hashCode();
                       if (inside) {
                          if (properties.containsKey((String) currentKey, currentHash)) continue;
                       } else {
                          if (isHiddenProperty((String) currentKey, currentHash)) continue;
                       }
                       return true;  
                    }
                    if (!inside && prototype != null) {
                        inside = true;
                        props = prototype.getProperties();
                        while (props.hasMoreElements()) {
                           currentKey = props.nextElement();
                           currentHash = currentKey.hashCode();
                           if (properties.containsKey((String) currentKey, currentHash)) continue;
                           return true;  
                        }
                    }
                    return false;
                }
                
                public Object nextElement() {
                    if (hasMoreElements()) {
                       Object key = currentKey;
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
     * <P> Hidde elements which are null (as netscape)
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
                int nextIndex = 0;
                public boolean hasMoreElements() {
                    // OK if we already checked for a property and one exists
                    if (currentKey != null) return true;
                    // loop on idex properties
                    if (nextIndex<theArray.size()) {
                        while ( (nextIndex<theArray.size()) &&
                                (theArray.elementAt(nextIndex) == null))
                            nextIndex++;       
                        if (nextIndex<theArray.size()) {
                            currentKey = Integer.toString(nextIndex);
                            currentHash = currentKey.hashCode();
                            nextIndex++;       
                            return true;
                        }
                    }
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
                    if (!inside && getPrototype() != null) {
                        inside = true;
                        props = getPrototype().getProperties();
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

    
    // overrides
    public String[] getSpecialPropertyNames() {
        String [] ns = {LENGTHstring};
        return ns;
    }

    // Support of custom compare function for sort
    class FunctionComparer implements Comparer {
       FunctionPrototype compareFn;
       ESValue arguments[];
       ESObject thisObject;
       public FunctionComparer (FunctionPrototype fn) {
           this.compareFn = fn;
           this.arguments = new ESValue[2];
           this.thisObject = evaluator.getGlobalObject ();
       }
       public int compare(ESValue v1, ESValue v2) throws EcmaScriptException {
           ESValue v1p = v1.toESPrimitive(ESValue.EStypeNumber);
           ESValue v2p = v2.toESPrimitive(ESValue.EStypeNumber);
           if (v1p == ESUndefined.theUndefined && v2p == ESUndefined.theUndefined) return 0;
           if (v1p == ESUndefined.theUndefined ) return 1;
           if (v2p == ESUndefined.theUndefined ) return -1;
           arguments[0] = v1;
           arguments[1] = v2;
           ESValue compValue = compareFn.callFunction (thisObject, arguments);
           return compValue.toInt32 ();
       }
    }


}