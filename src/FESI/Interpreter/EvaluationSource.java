// EvaluationSource.java
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

package FESI.Interpreter;

/**
 * Abstract declaration of an evaluation source. An evaluation
 * source is used to describe the source of a program for
 * error messages and debugging purpose.
 */
public abstract class EvaluationSource {

   /**
    * The end of line string for this machine.
    */
    protected String eol = System.getProperty("line.separator", "\n");

    /**
     * The previous source in case of multi-level evaluation. 
     */
    protected EvaluationSource previousSource = null;
    
    /**
     * Create a evaluation source linked to the previous source 
     * (which can be null)
     */
    public EvaluationSource(EvaluationSource previousSource) {
        super();
        this.previousSource = previousSource;
    }
   
    /**
     * Return a description of the evaluation source. Must be
     * implemented for each specific evaluation source.
     */
    abstract protected String getEvaluationSourceText();
 
    /**
     * Display the description of the evaluation source
     */
    public String toString() {
        return getEvaluationSourceText();
    }
    
    /**
     * Return an evaluation source number if defined
     * @return -1 if not defined
     */
    public int getLineNumber() {
       if (previousSource != null) {
           return previousSource.getLineNumber();
       } 
       return -1; // undefined
    }   
}