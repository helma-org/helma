// ESLoader.java
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
 * Descripe the compatibility of a method, allowing to
 * postpone the choice of the method to call until all
 * potentially compatible ones have been examined.
 */
class CompatibilityDescriptor {
  int distance;
  boolean [] convertToChar = null; // flag to indicate if conversion to char is needed
  Object [] convertedArrays = null; // Converted array if any needed
  
/*   
 * Create a new descrptor (note that there is a predefined one for
 * incompatible parameters).
 * @param distance -1 if incompatible, or evaluation of conversion distance
 * @param convertToChar Optional flag for each parameter
 * @param convertedArrays Optional list of converted arrays
 */
  CompatibilityDescriptor(int distance, 
            boolean[] convertedToChar, 
            Object[]  convertedArrays) {
     this.distance = distance;
     if (distance>=0) {
       this.convertToChar = convertedToChar;
       this.convertedArrays = convertedArrays;
     }
  }
  
  /** Return true if the method is compatible with the parameters */
  public boolean isCompatible() {
    return distance>=0;
  }

  /** Return the distance between perfect compatibility and this one */
  public int getDistance() {
    return distance;
  }
  
  /** Convert the parameters for this method call */
  public void convert(Object params[]) {
    if (params != null && distance>=0) {
      // Modify the parameters 
      int n = params.length;
      
      if (convertToChar!=null) {
          for (int i=0; i<n; i++) {
              if (convertToChar[i]) {
                  String s = (String) params[i];
                  Character c = new Character(s.charAt(0));
                  params[i] = c;
              }
          }
      }
      if (convertedArrays!=null) {
          for (int i=0; i<n; i++) {
              if (convertedArrays[i] != null) {
                  params[i] = convertedArrays[i];
              }
          }
      }
    }
      
  } // convert
            
} // class CompatibilityDescriptor