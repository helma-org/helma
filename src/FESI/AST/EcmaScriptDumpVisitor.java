package FESI.AST;

import FESI.Parser.*;

public class EcmaScriptDumpVisitor implements EcmaScriptVisitor
{
  private int indent = 0;

  private String indentString() {
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < indent; ++i) {
      sb.append(" ");
    }
    return sb.toString();
  }

  public Object visit(SimpleNode node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTProgram node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTStatementList node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTFunctionDeclaration node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTFormalParameterList node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTVariableDeclaration node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTIfStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTWhileStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTForStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTForInStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTForVarStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTForVarInStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTContinueStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTBreakStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTReturnStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTWithStatement node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTThisReference node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTCompositeReference node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTFunctionCallParameters node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTPropertyIdentifierReference node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTPropertyValueReference node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTAllocationExpression node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTOperator node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTPostfixExpression node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTUnaryExpression node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTBinaryExpressionSequence node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTOrExpressionSequence node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTAndExpressionSequence node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTConditionalExpression node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTAssignmentExpression node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTExpressionList node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTEmptyExpression node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTLiteral node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }
  public Object visit(ASTIdentifier node, Object data) {
    System.out.println(indentString() + node);
    ++indent;
    data = node.childrenAccept(this, data);
    --indent;
    return data;
  }

}