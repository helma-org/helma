// BuiltinFunctionObject.java
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

import java.util.Hashtable;
import java.util.Vector;
import FESI.Exceptions.*;
import FESI.Interpreter.Evaluator;

/**
 * Implement the common functionality of all built-in functions
 */
public abstract class BuiltinFunctionObject extends FunctionPrototype {
        
  
    protected BuiltinFunctionObject(ESObject functionPrototype,
                                 Evaluator evaluator,
                                 String functionName,
                                 int length) {
        super(functionPrototype, evaluator, functionName, length);
    }
    
    
    // overrides
    public void putProperty(String propertyName, ESValue propertyValue, int hash) 
                                throws EcmaScriptException {
         if (!propertyName.equals("prototype")) {
           super.putProperty(propertyName, propertyValue, hash);
         } // Allowed via putHiddenProperty, used internally !
    }

    
    /**
     * get the string defining the function
     * @return a string indicating that the function is native
     */
    public String getFunctionImplementationString() {
        return "{[native: " + this.getClass().getName() + "]}";
    }

    /**
     * Get the function parameter description as a string
     *
     * @return the function parameter string as (a,b,c)
     */
    public String getFunctionParametersString() {
       return "(<" + getLengthProperty()+ " args>)";
    }

    // overrides
    public String toString() {
        return "<" + this.getFunctionName() + ":" + this.getClass().getName() +">";
    }
  
}