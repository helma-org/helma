// EcmaScriptParseException.java
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
 * Exception used to signal parsing errors
 */
public class EcmaScriptParseException extends EcmaScriptException 
                    implements EcmaScriptConstants {

    /** @serial Original parse exception */
    private ParseException parseException;
    /** @serial Source description */
    private EvaluationSource evaluationSource;
    /** @serial true if the synatx error may be that the 
        source is not yet complete (for interactive usage) */
    private boolean canBeIncomplete = true;
    
    /**
     * Create a new parsing exception
     *
     * @param e The error from the parser
     * @param s The evaluation source location of the error
     */
    public EcmaScriptParseException(ParseException e, EvaluationSource s) {
        super("Parsing error");
        parseException = e;
        evaluationSource = s;
    }
    
   /**
    * Get the line number of the error if possible
    */
   public int getLineNumber() {
             
       Token next = null;
       Token tok = parseException.currentToken;
       if (tok != null && tok.next != null) next = tok.next; // get offending token
         
        if (next!=null) {
            return next.beginLine;
        } else {
            return -1;
        }
    }


    /**
     * return true if incomplete parse (maybe the user
     * is given a chance to complete it)
     */
    public boolean isIncomplete() {
        if (!canBeIncomplete) return false; // Some inner error
        Token tok = parseException.currentToken;
        if (tok != null && tok.next != null) tok = tok.next; // get offending token
        return (tok.kind == EOF);
    }
    
    /**
     * Indicate that this cannot be a recoverable incomplete error because,
     * for example, the evaluation source is a string passed as 
     * a parameter to eval, not a stream read from an user.
     */
    public void setNeverIncomplete() {
        canBeIncomplete = false;
    }
  
    /* 
     * Print the error message with helpfull comments if possible
     */
    public String getMessage() {
        Token tok = parseException.currentToken;
        Token next = null;
        String retval;
        if (tok != null && tok.kind==UNTERMINATED_STRING_LITERAL) {
            retval = "Unterminated string constant near line " + tok.beginLine +
                        ", column " + tok.beginColumn;
        } else {
            if (tok != null && tok.next != null) next = tok.next; // get offending token
            if (next != null & isForFutureExtension(next.kind)) {
                retval = "Keyword '" + next.image + 
                          "' reserved for future extension near line " + 
                          next.beginLine +
                            ", column " + next.beginColumn;
            } else {
                retval = "Syntax error detected near line " + next.beginLine +
                            ", column " + next.beginColumn;
            }
            if (tok != null) {
                retval += ", after " + parseException.tokenImage[tok.kind];
            }
        }
        retval += eol + evaluationSource;
        return retval;
    }
    
    
    private boolean isForFutureExtension(int k) {
        return 
            k==CASE ||
            k==CATCH||
            k==CLASS ||
            k==CONST ||
            k==DEBUGGER ||
            k==_DEFAULT ||
            k==DO ||
            k==ENUM ||
            k==EXPORT ||
            k==EXTENDS ||
            k==FINALLY ||
            k==IMPORT ||
            k==SUPER ||
            k==SWITCH ||
            k==THROW ||
            k==TRY;
    }
}