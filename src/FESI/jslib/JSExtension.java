// JSExtension.java
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

package FESI.jslib;

/**
 * Interface used to describe EcmaScript extensions with the
 * jslib package. An extension must implements this interface
 * to be loadable. A new instance of the extension is created
 * by FESI at load time.
 * <P>As there can be multiple extension (possibly in multiple
 * threads) in a single project, an extension should not have
 * shared static properties (unless protected and to share
 * information between various instances).
 */

abstract public interface JSExtension {

    /**
     * Called by the FESI interpreter the first time the extension
     * is loaded in the evaluator.
     *
     * @param   globalObject  The global object of this evaluator
     * @exception   JSException  To be thrown in case of error
     */
    abstract public void initializeExtension(JSGlobalObject globalObject)
           throws JSException;
}
 
 