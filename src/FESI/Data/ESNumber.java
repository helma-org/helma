// ESNumber.java
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
  * Implements the number primitive value as a double
  */
  
public final class ESNumber extends ESPrimitive {

    // The value
    private double value;
	
    /**
     * Create a new double with a specific value
     *
     * @param value The (immutable) value
     */    
	public ESNumber(double value) {
		this.value = value;
    }

    // overrides
    public int getTypeOf() {
        return EStypeNumber;
    }
    
    // overrides
    public String getTypeofString() {
        return "number";
    }

    // overrides
    public boolean isNumberValue() {
        return true; 
    }
	
    // overrides
	public double doubleValue() {
		return value;
	}

    // overrides
    public boolean booleanValue() {
        return !(Double.isNaN(value) || value==0.0);
    }

    // overrides
    public String toString() {
        long intValue = (long) value;
        if (((double) intValue) == value) {
            return Long.toString(intValue);
        } else {
            return Double.toString(value);
        }
    }

    // overrides
    public ESValue toESObject(Evaluator evaluator) throws EcmaScriptException {
         NumberPrototype theObject = null;
         ESObject np = evaluator.getNumberPrototype();
         theObject = new NumberPrototype(np, evaluator);
         theObject.value = this;
         return theObject;
    }    

    // overrides
    public ESValue toESNumber() {
        return this;
    }

    // overrides
    public Object toJavaObject() {
        long longValue = (long) value;
        Object o = null;
        if (((double) longValue) == value) {
            if (((byte) longValue) == longValue) {
                o = new Byte((byte) longValue);
            } else if (((short) longValue) == longValue) {
                o = new Short((short) longValue);
            } else if (((int) longValue) == longValue) {
                o = new Integer((int) longValue);
            } else {
                o = new Long(longValue);
            }
        } else {
            o= new Double(value);
        }
        return o;
    }

    // overrides
    public String toDetailString() {
        return "ES:#'" + Double.toString(value)+"'";
    }
	
}