// JSGlobalObject.java
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

package FESI.jslib;

/**
 * Interface used to represent the GlobalObject wrapper of the interpreter.
 * The global object is used for functions which require the evaluator.
 * It is possible to get it from any JSObject.
 * <P>This interface is exported by FESI objects, it is not intended
 * or useful for user objects to extend this interface.
 */

public interface JSGlobalObject extends JSObject {
        
   /**
     * Mark an object as a bean, restricting its access by FESI scripts
     * to the public bean methods and properties.
     *
     * @param object The object to wrap as a bean.
     */
   public Object makeBeanWrapper(Object object);

    /** 
     * Make a new object based the object prototype object.
     * The object is of class Object and has initially no property.
     *
     * @return A new object
     */
   public JSObject makeJSObject();
    
    /** 
     * Make a new object based on a given prototype (which may be null).
     * The object is of class Object and has initially no property.
     *
     * @param prototype An object to use as prototype for this object
     * @return A new object
     */
    public JSObject makeJSObject(JSObject prototype);
    
   /**
     * Package any object as an EcmaScript object, allowing to use
     * it for example with an "eval" function, where it becomes the
     * 'this' object.
     *
     * @param object The object to wrap.
     */
    public JSObject makeObjectWrapper(Object object);

    /** 
     * Make a new array object.
     * The object is of class Array and is empty (length 0).
     *
     * @return A new object
     */
    public JSObject makeJSArrayObject();
    
}
 
 