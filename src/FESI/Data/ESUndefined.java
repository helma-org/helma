// ESUndefined.java
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


/**
 * Implements the Undefined primitive value.
 * <P>There is a single Undefined value reached by ESUndefined.theUndefined
 * <P>The primitive Undefined, null and the java null are not
 * equivallent and must be used in the appropriate context
 */
public final class ESUndefined extends ESPrimitive {

    /**
     * the READ-ONLY undefined value
     */
    public static ESUndefined theUndefined = new ESUndefined(); 
	
    private ESUndefined() {
    }	
    

    /**
     * Implements a specific error message if an undfined value is called
     * @param thisObject The object on which the call is made
     * @param arguments The arguments of the function
     * @exception EcmaScriptException Thrown to indicate call on undefined value
     * @return never
     */
    public ESValue callFunction(ESObject thisObject, 
                                ESValue[] arguments) 
                            throws EcmaScriptException {
         throw new EcmaScriptException("Function called on undefined value or property");
    }    

    /**
     * Implements a specific error message if an undfined value is called via new
     * @param thisObject The object on which the call is made
     * @param arguments The arguments of the function
     * @exception EcmaScriptException Thrown to indicate call on undefined value
     * @return never
     */
    public ESObject doConstruct(ESObject thisObject, 
                                ESValue[] arguments) 
                            throws EcmaScriptException {
         throw new EcmaScriptException("'new' called on undefined value or property");
    }    

    // overrides
    public String toDetailString() {
        return "ES:<undefined>";
    }

    // overrides
    public int getTypeOf() {
        return EStypeUndefined;
       } 

    // overrides
    public String getTypeofString() {
        return "undefined";
    }

    // overrides
    public String toString() {
        return "undefined";
    }
    
    // overrides
    public double doubleValue() {
        return Double.NaN;
    }

    // overrides
    public boolean booleanValue() {
        return false;
    }

    // overrides
    public Object toJavaObject() {
        return null; // should throw an error
    }

}