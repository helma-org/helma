// ESNull.java
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

/**
 * Implements the NULL EcmaScript primitive value
 * <P> There is only one value of this type which is referenced
 * by ESNull.theNull. In general it is not identical for a routine
 * to return ESNull, ESUndefined or the java null. They all have
 * different purposes.
 */
public final class ESNull extends ESPrimitive {

    /**
     * the READ-ONLY null value
     */
    public static ESNull theNull = new ESNull(); 
	
    private ESNull() {
    }
    
    // overrides
	public String toDetailString() {
		return "ES:<null>";
    }

   // overrides
    public int getTypeOf() {
        return EStypeNull;
    }
    
   // overrides
    public String toString() {
        return "null";
    }
    
   // overrides
    public String getTypeofString() {
        return "object";
    }

   // overrides
    public double doubleValue() {
        return 0;
    }

   // overrides
    public boolean booleanValue() {
        return false;
    }

   // overrides
    public Object toJavaObject() {
        return null;
    }
    
}