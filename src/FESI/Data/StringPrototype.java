// StringPrototype.java
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

class StringPrototype extends ESObject {

   private static final String LENGTHstring = ("length").intern();
   private static final int LENGTHhash = LENGTHstring.hashCode();

    ESString value = new ESString("");
        
    StringPrototype(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator);
    }
    public String getESClassName() {
        return "String";
    }
    public String toString() {
         return value.toString();
    }
    public ESValue toESString() {
       return value;
    }

    public boolean booleanValue() throws EcmaScriptException {
       return value.booleanValue();
    }
    
    public double doubleValue() throws EcmaScriptException {
       return value.doubleValue();
    }
    
    public ESValue getPropertyInScope(String propertyName, ScopeChain previousScope, int hash) 
             throws EcmaScriptException {
        if (hash==LENGTHhash && propertyName.equals(LENGTHstring)) {
            return new ESNumber(value.getStringLength());
        }
        return super.getPropertyInScope(propertyName, previousScope, hash);
    }

    public ESValue getProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        if (hash==LENGTHhash && propertyName.equals(LENGTHstring)) {
             return new ESNumber(value.getStringLength());
         } else {
             return super.getProperty(propertyName, hash);
         }
     }

    
    public String[] getSpecialPropertyNames() {
        String [] ns = {LENGTHstring};
        return ns;
    }
    
    public Object toJavaObject() {
        return value.toString();
    }

    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" + 
            ((value == null) ? "null" : value.toString()) + "]";
    }
    
    /**
     * Information routine to check if a value is a string
     * if true, must implement toString without a evaluator.
     * @return true 
     */
    public boolean isStringValue() {
        return true; 
    }

}