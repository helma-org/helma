// JSException.java
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

import FESI.Exceptions.*;
import java.io.*;

/**
 * Thrown when the EcmaScript interpreter detect an error. Package
 * the message of the EcmaScriptException (or other exception) which
 * was generated.
 */

public class JSException extends Exception {

	/** @serial Original exception which caused the current exception */
    private Throwable originatingException = null;
    
    /**
     * Constructs a <code>JSException</code> with the 
     * specified detail message. 
     *
     * @param   s   the detail message.
     */
    public JSException(String s) {
        super(s);
    }
    
    /**
     * Constructs a <code>JSException</code> with the 
     * specified detail message, but refereing to the
     * original exception
     *
     * @param   s   the detail message.
     */
    public JSException(String s, Throwable originatingException) {
        super(s);
        this.originatingException = originatingException;
    }
    
    /**
     * Get the originating exception (if any) or null. Look down
     * until a true originating exception is found, if possible.
     *
     * @return   originating exception or null.
     */
    public Throwable getOriginatingException() {
        Throwable previousLevel = null;
        Throwable currentLevel = originatingException;
        while (currentLevel != null) {
            previousLevel = currentLevel;
            if (currentLevel instanceof JSException) {
                JSException oe = (JSException) originatingException;
                currentLevel = oe.getOriginatingException();
            } else if (currentLevel instanceof EcmaScriptException) {
                EcmaScriptException oe = (EcmaScriptException) originatingException;
                currentLevel = oe.getOriginatingException();
            //} else if (currentLevel instanceof java.lang.reflect.InvocationTargetException) {
            //    java.lang.reflect.InvocationTargetException oe = (java.lang.reflect.InvocationTargetException) originatingException;
            //    currentLevel = oe.getTargetException();
            } else {
                currentLevel = null;
            }
        } // while
        
        return previousLevel;
    }
    
    /**
     * Prints this <code>Throwable</code> and its backtrace to the 
     * standard error stream. 
     */
    public void printStackTrace() { 
        System.err.println(this);
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

    private void printStackTrace0(PrintWriter w) {
        super.printStackTrace(w);

        if (originatingException != null) {
            w.println("due to:");
            originatingException.printStackTrace(w);
        }
        w.flush();
    }

}