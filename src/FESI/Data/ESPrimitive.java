// ESPrimitive.java
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
 * This is the superclass of all primitive values (non object)
 * of FESI. The conversion to a primitive value from a primitive
 * value is always the same value, independently of the hint (9.1).
 * <P>Use the isPrimitive function, so that further primitive could
 * be added without subclassing this class.
 */
public abstract class ESPrimitive extends ESValue {


    /**
     * Create a new primitive value. Does not add any specific information.
     *
     */
	public ESPrimitive() {
		super();
    }


    /**
     * Indicate that this value is a primitive value, useful for various
     * tests in conversions. This avoid testing the type, in case additional
     * primitives must be created.
     *
     * @return  true   
     */
    public final boolean isPrimitive() {
        return true;
    }
	
    /**
     * Return false to indicate that this value is not composite.
     *
     * @return     false
     */
    public boolean isComposite() {return false; }


    /**
     * Convert to a primitive - a NOOP for a primitive.
     *
     * @return   this  
     * @exception   EcmaScriptException not thrown 
     */
    public final ESValue toESPrimitive() throws EcmaScriptException {
        return this;
    }

    /**
     * Convert to a primitive - a NOOP for a primitive.
     *
     * @param   hint ignored 
     * @return     this
     * @exception   EcmaScriptException  not thrown
     */
    public final ESValue toESPrimitive(int hint) throws EcmaScriptException {
        return this;
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
                                    "PRIMITIVE",
                                    this.toString());
    }

       
}