// ProgrammingError.java
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

package FESI.Exceptions;

/**
 * Exception used to signal internal programming error (assert error)
 */
public class ProgrammingError extends Error {
    
   private static String eol = System.getProperty("line.separator", "\n");

   /**
    * Create an anonymous programming error exception
    */
   public ProgrammingError() {
   }

   /**
    * Create a programming error exception with explanatory test
    * @param message Reason of the error
    */
   public ProgrammingError(String message) {
      super(message);
   }
   
   /**
    * Display the internal error asking to contact the author
    */
   public String getMessage() {
       String m = super.getMessage();
       m += eol + "*** FESI Internal error - contact the author ***";
       return m;
   }
}