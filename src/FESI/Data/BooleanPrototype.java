// BooleanPrototype.java
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

/**
 * Implements the prototype and is the class of all Boolean objects
 */
class BooleanPrototype extends ESObject {

    // The value
    protected ESBoolean value = ESBoolean.makeBoolean(false);
     
    /** Create a new Boolean initialized to false
     * @param prototype the BooleanPrototype
     * @param evaluator the evaluator
     */   
    BooleanPrototype(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator);
    }
    
    // overrides
    public String getESClassName() {
        return "Boolean";
    }

    // overrides
    public String toString(Evaluator evaluator) throws EcmaScriptException {
         return value.toString();
    }

    // overrides
    public double doubleValue() throws EcmaScriptException {
       return value.doubleValue();
    }

    // overrides
    public ESValue toESBoolean() throws EcmaScriptException {
       return value;
    }

    // overrides
    public boolean booleanValue() {
        return value.booleanValue();
    }
    
    // overrides
    public Object toJavaObject() {
        return new Boolean(value.booleanValue());
    }

    // overrides
    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" + 
            ((value == null) ? "null" : value.toString()) + "]";
    }
    
    // overrides
    public boolean isBooleanValue() {
        return true; 
    }

}