// ParsedProgram.java
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

import FESI.Parser.*;
import FESI.AST.*;
import FESI.Data.*;

import java.util.Vector;

/**
 * Represent a parsed program or function
 */
public class ParsedProgram {
    // The parsed tree of the function
    private ASTProgram programNode = null;
    // The list of declared variables
    private Vector variableNames = null;
    // The source of the parsed program    
    private EvaluationSource evaluationSource = null;
    
    /**
     * Create a parsed program representation from the abstract tree and list of variables
     * @param programNode the parsed program
     * @param variableNames The variables declared by var
     * @param evaluationSource the source of the parsed tree
     */
    protected ParsedProgram(ASTProgram programNode,
                            Vector variableNames,
                            EvaluationSource evaluationSource) {
        this.programNode = programNode;
        this.variableNames = variableNames;
        this.evaluationSource = evaluationSource;
    }
    
    /**
     * Get the program node
     * @return the program node
     */
    protected ASTProgram getProgramNode() {
        return programNode;
    }

    /**
     * Get the variable list
     * @return the variable list
     */
    protected Vector getVariableNames() {
        return variableNames;
    }
    
    /**
     * Get the evaluation souce 
     * @return the evaluation source
     */
    protected EvaluationSource getEvaluationSource() {
        return evaluationSource;
    }
}