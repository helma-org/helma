// EcmaScriptLexicalException.java
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

import FESI.Parser.*;
import FESI.Interpreter.*;

/**
 * Exception used to signal lexical error during parsing
 */
public class EcmaScriptLexicalException extends EcmaScriptException 
                    implements EcmaScriptConstants {

    /** @serial Token creating the error */
    private TokenMgrError tokenMgrError;
    
    /** @serial Identification of source creating the error */
    private EvaluationSource evaluationSource;
    
    /**
     * Create a new lexical exception
     *
     * @param e The error from the token manager
     * @param s The evaluation source location of the error
     */
    public EcmaScriptLexicalException(TokenMgrError e, EvaluationSource s) {
        super("Lexical error");
        tokenMgrError = e;
        evaluationSource = s;
    }
  
   /**
    * Get the line number of the error if possible
    */
   public int getLineNumber() {
          
      if (evaluationSource != null) {
          return evaluationSource.getLineNumber();
      } else {
          return -1;
      }
   }

    /**
     * Return the text of the token error and the location
     */
    public String getMessage() {
        String retval = tokenMgrError.getMessage();
        retval += eol + evaluationSource;
        return retval;
    }
    
    /**
     * Return true in case of unclosed comment, as in this case
     * the statement is not complete and the user may be prompted
     * to complete the statement.
     */
    public boolean isIncomplete() {
      String s = tokenMgrError.getMessage();
      return s.indexOf(".  Probably unclosed comment.")!= -1;
       // See TokenMgrError
   }
}