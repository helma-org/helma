// ValueDescription.java
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
 * A ValueDescription is used to describe the property or field of
 * an object (for debugging tools).
 */
public class ValueDescription {

    private static String eol = System.getProperty("line.separator", "\n");

    /**
     * The name of this value (for example the property name
     * if it is a property), null if not known or irrelevant.
     */
    public String name = null;
  
  
    /**
     * The string describing the type of this value.
     */
    public String type;
  
    /**
     * The string describing the value.
     */
    public String value;
  
  
    /**
     * Build a value descriptor for an unknown name
     */
    public ValueDescription (String type, String value) {
        this.type = type;
        this.value = value;
    }
  
    /**
     * Build a value descriptor for a specified name
     */
    public ValueDescription (String name, String type, String value) {
        this.name = name;
        this.type = type;
        this.value = value;
    }
  
    public String toString() {
        String propertyValue = value;
        // Remove leading eol
        while (propertyValue.indexOf("\n")==0) {
            propertyValue = propertyValue.substring(1);
        }
        while (propertyValue.indexOf(eol)==0) {
           propertyValue = propertyValue.substring(eol.length());
        }
        // limit size
        if (propertyValue.length()>250) {
            propertyValue = propertyValue.substring(0,250) + "...";
        }
        // keep only first line
        int ieol = propertyValue.indexOf(eol);
        if (ieol==-1) ieol = propertyValue.indexOf("\n");
        if (ieol!=-1) {
            propertyValue = propertyValue.substring(0,ieol) + "...";
        }

       if (name == null) {
           return type + ": " + propertyValue;
       } else {
           return name + ": [" + type + "]: " + propertyValue;
       }
    }
}