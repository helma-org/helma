// JSUtil.java
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

import FESI.Interpreter.Evaluator;

/**
 * Abstract class used for interfacing the FESI EcmaScript interpreter
 * with Java code. Contains the static utility functions, as the
 * evaluator factory and object factory.
 */

abstract public class JSUtil {
    

    /**
     * Create a new evaluator, with no extension loaded.
     *
     * @return     The global object of the created evaluator.
     * @exception   JSException  For any error during initialization
     */
    static public JSGlobalObject makeEvaluator() throws JSException {
        return FESI.Data.JSWrapper.makeEvaluator();
    }

    /**
     * Create a new evaluator, with specfied extensions loaded.
     *
     * @param   extensions  The class name of the extensions to load.
     * @return     The global object of the created evaluator.
     * @exception   JSException  For any error during initialization
     */
    static public JSGlobalObject makeEvaluator(String [] extensions) throws JSException {
        return FESI.Data.JSWrapper.makeEvaluator(extensions);
    }
        
  /**
   * Return the version identifier of the interpreter
   */
  public static String getVersion() {
      return Evaluator.getVersion();
  }
    
  /**
   * Return the welcome text (including copyright and version)
   * of the interpreter (as two lines)
   */
  public static String getWelcomeText() {
      return Evaluator.getWelcomeText();
  }
  
}
 
 