// JSFunction.java
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
 * Interface that an object must implement to be considered an EcmaScript
 * function.
 * <P>This interface may be implemented by an object which will then be recognized
 * as a function or a constructor if used in EcmaScript.
 * <P>Non function objects need not implement any specific interface,
 * FESI using introspection to discover their properties.
 * <P>The parameters passed to the doCall or doNew arguments are transformed
 * as follow:
 * <BR>If they are FESI objects they are wrapped as JSObjects.
 * <BR>If they are FESI primitive, the corresponding most primitive
 * java object is used (for example Integer).
 * <BR>Otherwise (native objects) they are passed as native objects.
 * <P>
 * @see FESI.jslib.JSFunctionAdapter
 */
public interface JSFunction {
    
    /**
     * Call the specified EcmaScript method of this object
     *
     * @param   thisObject  The object for which the function is called.
     * @param   args  An array of parameters.
     * @return  The result of the evaluation
     * @exception   JSException  For any error during interpretation
     */
    abstract public Object doCall(JSObject thisObject, Object args[]) throws JSException;

    /**
     * Create a new object, using the specified EcmaScript method of this object
     *
     * @param   thisObject  The object for which the function is called.
     * @param   args  An array of parameters.
     * @return  The result of the evaluation
     * @exception   JSException  For any error during interpretation
     */
    abstract public Object doNew(JSObject thisObject, Object args[]) throws JSException;

}
 
 