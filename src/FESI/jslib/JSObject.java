// JSObject.java
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

import java.io.Reader;

/**
 * Interface used for interfacing the FESI EcmaScript interpreter
 * with Java code. Based and largely compatible with the Netscape
 * JavaScript to Java interface.
 * <P>This interface is exported by FESI objects, it is not intended
 * or useful for user objects to extend this interface.
 * <P>Non function objects need not implement any specific interface,
 * FESI using introspection to discover their properties. Function
 * objects must implement JSFunction.
*/

public interface JSObject {
        

    /**
     * Call the specified EcmaScript method of this object
     *
     * @param   methodName  The name of the method to call
     * @param   args  An array of parameters.
     * @return  The result of the evaluation
     * @exception   JSException  For any error during interpretation
     */
    abstract public Object call(String methodName,Object args[]) throws JSException;

    /**
     * Evaluate a string with this object as the 'this' object.
     * Consider the string being a main program, not allowing the
     * return statement.
     *
     * @param   s  The string to evaluate
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    abstract public Object eval(String s) throws JSException;


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
    abstract public Object eval(Reader r, String d) throws JSException;


    /**
     * Evaluate a string with this object as the 'this' object.
     * Consider the string as a function, allowing the return statement.
     *
     * @param   s  The string to evaluate
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    abstract public Object evalAsFunction(String s) throws JSException;

    /**
     * Evaluate a Reader stream with this object as the 'this' object.
     * Consider the stream as a function, allowing the return statement.
     *
     * @param   r  The Reader stream to evaluate
     * @param   d A description of the Reader for error messages
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    //abstract public Object evalAsFunction(Reader r, String d) throws JSException;


    /**
     * Evaluate a string with this object as the 'this' object.
     * Consider the string as a function, allowing the return statement.
     * Passing the specified parameters (names and values must have the same length)
     *
     * @param   s  The string to evaluate
     * @param  names the names of the parameters
     * @param  values the values of the parameters
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    abstract public Object evalAsFunction(String s, String [] names, Object values[]) throws JSException;

    /**
     * Evaluate a Reader stream with this object as the 'this' object.
     * Consider the stream as a function, allowing the return statement.
     * Passing the specified parameters (names and values must have the same length)
     *
     * @param   r  The Reader stream to evaluate
     * @param   d A description of the Reader for error messages
     * @param  names the names of the parameters
     * @param  values the values of the parameters
     * @return  The result of the evaluation (null if no value returned)
     * @exception   JSException  For any error during interpretation
     */
    //abstract public Object evalAsFunction(Reader r, String d, String [] names, Object values[]) throws JSException;

    /**
     * Get the named property of this object.
     *
     * @param   name  The name of the property to get
     * @return  The value of the property
     * @exception   JSException  For any error during interpretation
     */
    abstract public Object getMember(String name) throws JSException;

    /**
     * Get the indexed property of this object (useful for arrays).
     *
     * @param   index  The index value of the property (converted
     *                 to string if not an array)
     * @return  The value of the property
     * @exception   JSException  For any error during interpretation
     */
    abstract public Object getSlot(int index) throws JSException;

    // This Netscape function is not implemented
    // public static JSObject getWindow(Applet applet) throws JSException;

    /**
     * Delete a named property of this object
     *
     * @param   name  The name of the property to delete
     * @exception   JSException  For any error during interpretation
     */
    abstract public void removeMember(String name) throws JSException;

    /**
     * Set the value of a named property of this object
     *
     * @param   name  The name of the property to set
     * @param   value  The value to set the property to.
     * @exception   JSException  For any error during interpretation
     */
    abstract public void setMember(String name, Object value) throws JSException;

    /**
     * Set a property by index value. Useful for arrays.
     *
     * @param   index  The index of the property in the array.
     * @param   value  The value to set the property to.
     * @exception   JSException  For any error during interpretation
     */
    abstract public void setSlot(int index, Object value) throws JSException;
    
    // FESI extension
    
    /**
     * Get the global object of the interpreter
     */
    abstract public JSGlobalObject getGlobalObject();

}
 
 