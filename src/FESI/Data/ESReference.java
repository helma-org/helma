// ESreference.java
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
 * Currently a reference is not a FESI value, as it can never be returned
 * as a valid value by an EcmaScript function (8.7). It would be easy
 * to make ESReference a subclass of ESValue or to create a common
 * superclass (ESData) for both ESReference and ESValue if so desired in
 * the future (in fact this was the case in the first version of the system).
 * <P>References are less used than in the standard, as they are built only
 * if required for assignement. 
 */
public class ESReference { 

    private ESObject base;        // null means a property of the global object
	private String propertyName;  // Should never be null for a valid reference
	private int hash; // hashCode of propertyName

    /**
     * Create a new reference given a base and a property name
     *
     * @param   base - may be null 
     * @param   propertyName  - may not be null
     * @param   hash  - hashCode of propertyName
     */
	public ESReference(ESValue base, String propertyName, int hash) {
     // Make sure the property name is not null
     if (propertyName == null) {
          throw new NullPointerException();
      }
		 this.base = (ESObject) base;
      this.propertyName = propertyName;
      this.hash = hash;
	    // System.out.println("NEWREF: " + this);
    }
    

    /**
     * Return the base object to which the property applies
     * Only used for the DELETE operation. See 8.7.1
     *
     * @return     The base object, possibly null
     * @exception   EcmaScriptException Not thrown 
     */
	public ESValue getBase() throws EcmaScriptException {
		return base;
	}
	

    /**
     * Get the name of the property to apply to the base object
     * Only used for the DELETE operation. See 8.7.2
     *
     * @return     The name of the property, never null
     * @exception   EcmaScriptException  not thrown
     */
	public String getPropertyName() throws EcmaScriptException {
		return propertyName;
	}	
	
    /**
     * Return the referenced value unless it is global and not defined, in which case 
     * an exception is raised (see 8.7.3). By the definition of getProperty, and undefined
     * value is returned if the base object is defined but the property does
     * not exist (see 8.6.2.1).
     *
     * @return     The referenced value
     * @exception   EcmaScriptException  if the value is not defined
     */
	public ESValue getValue() throws EcmaScriptException {
		if (base == null) {
			throw new EcmaScriptException("global variable '" + propertyName + "' does not have a value");
		}
		return base.getProperty(propertyName, hash);
	}
	

    /**
     * Update the referenced value, creating it if needed. If the base is
     * is null use the global object, otherwise use the base object.
     * See 8.7.4.
     *
     * @param   g  The global object to use if thre base is null
     * @param   v  The value to put
     * @exception   EcmaScriptException  May be thrown by putProperty
     */
	public void putValue(ESObject g, ESValue v) throws EcmaScriptException {
        // System.out.println("PUT " + v + " to " + this);
        if (base==null) {
            g.putProperty(propertyName, v, hash);
        } else {
  		       base.putProperty(propertyName, v, hash);
        }
	}
    
    /**
     * Return a string identifying the reference type and its content
     *
     * @return   a string  
     */
    public String toDetailString() {
        return "ES:*<" + ((base==null) ? "null" : base.toString()) + ":" + propertyName +">";
    }
    

    /**
     * Return the name of the reference, using a dot notation (could use
     * the bracket notation, or check which one is most appropriate).
     * Note that the base object is returned between braces, we do
     * not know its name at this stage. This is used for debugging
     * purpose, there is no way to get the string of a reference in
     * EcmaScript.
     *
     * @return    The string naming the reference. 
     */
    public String toString() {
        return  ((base==null) ? "" : ("{" + base.toString() + "}.")) + propertyName;
    }
}