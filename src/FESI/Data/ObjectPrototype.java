// ObjectPrototype.java
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

import FESI.Interpreter.Evaluator;
import FESI.Exceptions.*;

/**
 * Implements the prototype and is the class of all Object objects.
 * <P>All functionality of objects is implemented in the superclass
 * ESObject.
 */
public class ObjectPrototype extends ESObject {
     
    /**
     * Create a new Object with a specific prototype. This should be used
     * by routine implementing object with another prototype than Object.
     * To create an EcmaScript Object use ObjectObject.createObject()
     *
     * @param  prototype the prototype of the new object
     * @param  evaluator The evaluator 
     */
    public ObjectPrototype(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator);
    }
}