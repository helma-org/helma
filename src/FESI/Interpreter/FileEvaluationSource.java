// FileEvaluationSource.java
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
 * Describe a file used as a source. 
 */
public class FileEvaluationSource extends EvaluationSource {
    
    private String theFileName;
    
    /**
     * Create a file source description
     * @param theFileName Describe the source file name
     * @param previousSource Describe the calling source
     */
    public FileEvaluationSource(String theFileName, EvaluationSource previousSource) {
        super(previousSource);
        this.theFileName = theFileName;
    }
    protected String getEvaluationSourceText() {
        return "in file: '" + theFileName + "'";
    }
}