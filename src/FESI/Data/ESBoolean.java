// ESBoolean.java
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

/**
 * Boolean primitive value
 */
public final class ESBoolean extends ESPrimitive {

    // There is only one true and one false value (allowing
    // efficient comparisons)
    private static ESBoolean trueValue = new ESBoolean(); 
    private static ESBoolean falseValue = new ESBoolean(); 
	
    
    private ESBoolean() {
    }
	
    /**
     * Create a boolean primitive (either true or false)
     * by returning the predefined (unique) true or false values
     *
     * @return either trueValue or falseValue 
     */
	static public ESBoolean makeBoolean(boolean value) {
	   return value ? trueValue: falseValue;
    }
	
   // overrides
    public String toDetailString() {
        return "ES:<" + (this==trueValue ? "true" : "false") + ">";
    }
    
   // overrides
   public int getTypeOf() {
        return EStypeBoolean;
    }
    
   // overrides
    public String getTypeofString() {
        return "boolean";
    }
    
    
   // overrides
    public String toString() {
        return this==trueValue ? "true" : "false";
    }    
    
   // overrides
    public double doubleValue() {
        return this==trueValue ? 1 : 0;
    }
    
   // overrides
    public boolean booleanValue() {
        return this==trueValue;
    }
    
   // overrides
    public ESValue toESBoolean() {
        return this;
    }    
    
   // overrides
    public ESValue toESObject(Evaluator evaluator) throws EcmaScriptException {
         BooleanPrototype theObject = null;
         ESObject bp = evaluator.getBooleanPrototype();
         theObject = new BooleanPrototype(bp, evaluator);
         theObject.value = this;
         return theObject;
    }  
    
    // overrides
    public Object toJavaObject() {
        return new Boolean(this==trueValue);
    }
    
   // overrides
    /**
     * returns true as we implement booleanValue without an evaluator.
     * @return true 
     */
    public boolean isBooleanValue() {
        return true; 
    }

}