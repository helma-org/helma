// ESString.java
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

import FESI.Interpreter.Evaluator;
import FESI.Exceptions.*;

/** 
 * Implements the string primitive value
 */
public final class ESString extends ESPrimitive {

    // The value
    private String string;
	
    /**
     * Create a new value from the string parameters 
     *
     * @param value The immutable value
     */
	public ESString(String value) {
		this.string = value;
    }

     // overrides
   public String toDetailString() {
        return "ES:\"" + string + "\"";
    }

    // overrides
    public int getTypeOf() {
        return EStypeString;
    }

    // overrides
    public String getTypeofString() {
        return "string";
    }
   
    // overrides
    public String toString() {
        return string;
    }
    
    /**
      * Returns the length of the string
      *
      * @return the length of the string
      */
     public int getStringLength() {
        return string.length();
     }
    
     // overrides
     public double doubleValue() {
        double value = Double.NaN;
        try {
           // Will accept leading / trailing spaces, unlike new Integer !
           value = (Double.valueOf(string)).doubleValue();
        } catch (NumberFormatException e) {
        }
        return value;
     }
        
     // overrides
     public boolean booleanValue() {
        return string.length()>0;
     }
 
     // overrides
     public ESValue toESString() {
        return this;
     }
 
     // overrides
     public ESValue toESObject(Evaluator evaluator) throws EcmaScriptException {
         StringPrototype theObject = null;
         ESObject sp = evaluator.getStringPrototype();
         theObject= new StringPrototype(sp, evaluator);
         theObject.value = this;
         return theObject;
      }
    
     // overrides
     public Object toJavaObject() {
        return string;
     }
     
    // overrides
    public boolean isStringValue() {
        return true; 
    }
 
}