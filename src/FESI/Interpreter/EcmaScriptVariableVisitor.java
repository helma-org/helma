// EcmaScriptVariableVisitor.java
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
import FESI.Exceptions.*;

import java.util.Vector;

/**
 * The variable visitor use the visitor pattern to iterate the
 * parsed code. It examine all variable declaration at the current
 * level (it does not recurse in functions) and return the
 * list of variables as a vector.
 * <P>The variable declarations will be ignored by the evaluation
 * visitor (the tree is not modified).
 */
public class EcmaScriptVariableVisitor 
            implements EcmaScriptVisitor,
                       EcmaScriptConstants
{
    
  // The visitor work on behalf on an evaluator 
  private Evaluator evaluator = null;
  private boolean debug = false;
  private Vector variableList = null; 
  
 /**
   * Create a new visitor
   * @param evaluator On behalf of this evaluator
   */
  public EcmaScriptVariableVisitor(Evaluator evaluator) {
      super();
      this.evaluator = evaluator;
  }
  
  /**
   * Process all variable declarations at the global level
   * @param node The parse tree
   * @evaluationSource A description of the source for error messages
   * @return A vector of variables
   */
  public Vector processVariableDeclarations(ASTProgram node,
                                   EvaluationSource evaluationSource) {
      if (debug) System.out.println("processVariableDeclarations for program: " + node);
      variableList = new Vector();
      node.jjtAccept(this, evaluationSource);
      return variableList;
  }
  
  /**
   * Process all variable declarations at the statement list level
   * @param node The parse tree
   * @evaluationSource A description of the source for error messages
   * @return A vector of variables
   */
  public Vector processVariableDeclarations(ASTStatementList node,
                                   EvaluationSource evaluationSource) {
      if (debug) System.out.println("processVariableDeclarations for function body: " + node);
      variableList = new Vector();
      node.jjtAccept(this, evaluationSource);
      return variableList;
  }
  
  /*--------------------------------------------------------------------
   * The following routines implement the walking process
   * Irrelevant parts of the tree are skipped
   *------------------------------------------------------------------*/

  private void badAST() {
      throw new ProgrammingError("Bad AST walk in EcmaScriptVariableVisitor");
  }
  
  
  // The dispatching is by node type - if the specific visitor
  // is not implemented, then this routine is called
  public Object visit(SimpleNode node, Object data) {
    badAST();
    return data;
  }
  
  public Object visit(ASTProgram node, Object data) {
    data = node.childrenAccept(this, data);
    return data;
  }
  
  public Object visit(ASTStatementList node, Object data) {
    data = node.childrenAccept(this, data);
    return data;
  }
  
  public Object visit(ASTFunctionDeclaration node, Object data) {
    ; // ignore function declarations in this mode
    return data;
  }
  
  public Object visit(ASTFormalParameterList node, Object data) {
    badAST();
    return data;
  }
  
  public Object visit(ASTStatement node, Object data) {
    data = node.childrenAccept(this, data);
    return data;
  }
  
  public Object visit(ASTVariableDeclaration node, Object data) {
    int nChildren = node.jjtGetNumChildren();
    if (nChildren<1 || nChildren>2) {
        throw new ProgrammingError("Bad AST in variable declaration");
    }
    ASTIdentifier idNode = (ASTIdentifier) (node.jjtGetChild(0));
    if (debug) System.out.println("VAR DECL: " + idNode.getName());
    variableList.addElement(idNode.getName());
    //try {
    //    evaluator.createVariable(idNode.getName());
    //} catch (EcmaScriptException e) {
    //    e.printStackTrace();
    //    throw new ProgrammingError(e.getMessage());
    //}
    return data;
  }

  public Object visit(ASTIfStatement node, Object data) {
    data = node.childrenAccept(this, data);
    return data;
  }

  public Object visit(ASTContinueStatement node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }

  public Object visit(ASTWhileStatement node, Object data) {
    data = node.childrenAccept(this, data);
    return data;
  }

  public Object visit(ASTForStatement node, Object data) {
    data = node.childrenAccept(this, data);
    return data;
  }

  public Object visit(ASTForInStatement node, Object data) {
    data = node.childrenAccept(this, data);
    return data;
  }

  public Object visit(ASTForVarStatement node, Object data) {
    data = node.childrenAccept(this, data);
    return data;
  }
  public Object visit(ASTForVarInStatement node, Object data) {
    data = node.childrenAccept(this, data);
    return data;
  }
  public Object visit(ASTBreakStatement node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  public Object visit(ASTReturnStatement node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  public Object visit(ASTWithStatement node, Object data) {
    node.setEvaluationSource(data);
    data = node.childrenAccept(this, data);
    return data;
  }
  
  public Object visit(ASTThisReference node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTCompositeReference node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTFunctionCallParameters node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTPropertyValueReference node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  public Object visit(ASTPropertyIdentifierReference node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  public Object visit(ASTAllocationExpression node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  public Object visit(ASTOperator node, Object data) {
    badAST();
    return data;
  }
  public Object visit(ASTPostfixExpression node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTUnaryExpression node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTBinaryExpressionSequence node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTAndExpressionSequence node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTOrExpressionSequence node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTConditionalExpression node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  // Can we really have a cascade ?
  public Object visit(ASTAssignmentExpression node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTExpressionList node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTEmptyExpression node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTLiteral node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }
  
  public Object visit(ASTIdentifier node, Object data) {
    ; // no internal variable declarations possible
    return data;
  }

}