// JarEvaluationSource.java
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
 * Describe a jar entry used as a source. 
 */
public class JarEvaluationSource extends EvaluationSource {
    
    private String theJarName;
    private String theEntryName;

    /**
     * Create a jar source description
     * @param theJarName Describe the source jar
     * @param theEntryName Describe the source entry in the jar
     * @param previousSource Describe the calling source
     */
    public JarEvaluationSource(String theJarName, String theEntryName, EvaluationSource previousSource) {
        super(previousSource);
        this.theJarName = theJarName;
        this.theEntryName = theEntryName;
    }
    
    protected String getEvaluationSourceText() {
        return "in entry: '" + theEntryName + "' of jar: '" + theJarName + "'";
    }
}