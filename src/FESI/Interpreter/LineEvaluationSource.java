// LineEvaluationSource.java
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
 * Add line number to an evaluation source
 */

public class LineEvaluationSource extends EvaluationSource {
    
    private int theLineNumber;
     
    /**
     * Create a source description being the line number of a previous description
     * @param theLineNumber Describe the line number
     * @param previousSource Describe the calling source
     */
    public LineEvaluationSource(int theLineNumber, EvaluationSource previousSource) {
        super(previousSource);
        this.theLineNumber = theLineNumber;
    }
    
    /**
     * Return the string describing the line number
     */
    protected String getEvaluationSourceText() {
        return "at line " + theLineNumber + " " + previousSource.getEvaluationSourceText();
    }
    
    /**
      * Get the line number of the error if possible
     */
    public int getLineNumber() {
        return theLineNumber;
    }
}