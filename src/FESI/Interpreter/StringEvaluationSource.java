// StringEvaluationSource.java
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

import java.util.StringTokenizer;

/**
 * Describe a string used as a source. The string is trimmed
 * to its first significant characters during display.
 */
public class StringEvaluationSource extends EvaluationSource {
    
    private String theString;
    
    
    /**
     * Create a string source description
     * @param theString Describe the source
     * @param previousSource Describe the calling source
     */
    public StringEvaluationSource(String theString, EvaluationSource previousSource) {
        super(previousSource);
        this.theString = theString;
    }
    
    protected String getEvaluationSourceText() {
        String displayString = new String("");
        // All this to print just first line of string...
        boolean firstLineFound = false;
        boolean moreLinesFound = false;
        StringTokenizer t = new StringTokenizer(theString, "\n\r");
        while (t.hasMoreTokens()) {
            String theLine = t.nextToken();
            if (theLine.equals("\n") || theLine.equals("\r")) continue;
            if (theLine.trim().length()>0) { // Skip any leading empty lines
               if (!firstLineFound) {
                   displayString = theLine;
                   firstLineFound = true;
                   continue;
               }
               moreLinesFound = true;
               break;
            }
        }
        if (moreLinesFound) {
            return "in string starting with: '" + displayString + "'...";
        } else {
            return "in string: '" + displayString + "'";
        }
    }
}