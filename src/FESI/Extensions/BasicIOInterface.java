// BasicIOInterface.java
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

package FESI.Extensions;

import FESI.Data.ESObject;

/**
 * The interface of all BasicIO extensions, used bu the interpreter
 * to access the document object.
 */
 
public interface BasicIOInterface {
  
   /**
     * Return an object with the document routines (for compatibility with 
     * Netscape). This object will be the "document" object and the caller
     * may add its URL property.
     */
   abstract public ESObject getDocument();
}