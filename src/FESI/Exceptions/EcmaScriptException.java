// EcmaScriptException.java
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

import java.util.Vector;
import FESI.Interpreter.*;
import java.io.*;

/**
 * Superclass of all common exceptions used by the FESI system
 */
public class EcmaScriptException extends Exception {

  /**
   * The end of line string for this machine.
   */
  static protected String eol = System.getProperty("line.separator", "\n");
  
  
  /**
   * The list of EcmaScript evaluation sources at this error
   * @serial EcmaScritp sources callback
   */
  protected Vector evaluationSources = new Vector();
  
  /**
   * @serial The original exception which trigerred this exception
   */
  private Throwable originatingException = null;  // If the exception package another one


    /**
     * Create a generic exception with cause "reason"
     *
     * @param   reason the reason of the exception  
     */
  public EcmaScriptException(String reason) {
        super(reason);
  }
  
    /**
     * Create a generic exception with cause "reason", originially caused
     * by the originatingException
     *
     * @param   reason the reason of the exception  
     * @param   originatingException the original exception creating this exception  
     */
  public EcmaScriptException(String reason, Throwable originatingException) {
        super(reason);
        this.originatingException = originatingException;
  }
  
/**
 * Get the originating exception (if any) or null
 *
 * @return   originating exception or null.
 */
  public Throwable getOriginatingException() {
      return originatingException;
  }
  
  /**
   * Append an evaluation source to the list of evaluation sources
   *
   * @param es The evaluation source to add 
   */  
  public void appendEvaluationSource(EvaluationSource es) {
      evaluationSources.addElement(es);
  }
  
  /**
   * Get the line number of the error if possible
   */
  public int getLineNumber() {
          
      if (evaluationSources.size()>0) {
          EvaluationSource es = (EvaluationSource) evaluationSources.elementAt(0);
          return es.getLineNumber();
      } else {
          return -1;
      }
  }

  /**
   * Display the message, the originating exception and the
   * EmcaScript callback chain.
   * <P>If cause by another exception displays its callback chain
   */
  public String getMessage() {
      String msg = super.getMessage();
      if (originatingException!=null) {
          msg += eol + "Caused by exception: " + eol + "  " + originatingException.getMessage();
      }
      if (originatingException != null) {
          StringWriter sw = new StringWriter();
          PrintWriter pw = new PrintWriter(sw);
          originatingException.printStackTrace(pw);
          msg += eol;
          pw.close();
          /*
          // THIS WAS NOT NEEDED AND IS AN INCOMPATIBILITY
          // BETWEEN JDK 1.1/.2
      	   try {
            sw.close();
            if (false) throw new IOException(); // to make JDK 1.1 Happy
      	   } catch (IOException ignore) { 
      	   	 ; // To make JDK 1.2 happy
          }
          */
          msg += sw.toString();
      }
      for (int i = 0; i<evaluationSources.size(); i++) {
          EvaluationSource es = (EvaluationSource) evaluationSources.elementAt(i);
          msg += eol + (i==0 ? "detected " : "called ") + es;
      }
      return msg;
  }

  /**
   * If true would the the parser error detected when a statement is incomplete.
   */
  public boolean isIncomplete() {
      return false;
  }
  
  
   /**
     * Prints this <code>Throwable</code> and its backtrace to the 
     * standard error stream. 
     */
    public void printStackTrace() {
        // System.err.println(this);
        printStackTrace0(new PrintWriter(System.err));
    }

    /**
     * Prints this <code>Throwable</code> and its backtrace to the 
     * specified print stream. 
     */
    public void printStackTrace(java.io.PrintStream s) { 
        s.println(this);
        PrintWriter w = new PrintWriter(s);
        printStackTrace0(w);
    }

    /**
     * Prints this <code>Throwable</code> and its backtrace to the specified
     * print writer.
     */
    public void printStackTrace(java.io.PrintWriter w) { 
        w.println(this);
        printStackTrace0(w);
    }

    /**
     * Print the stack trace to a stream, with the backtrace of
     * the originating exception if any.
     */
    private void printStackTrace0(PrintWriter w) {
        super.printStackTrace(w);

        if (originatingException != null) {
            w.println("due to:");
            originatingException.printStackTrace(w);
        }
        w.flush();
    }

}
