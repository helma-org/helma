// EcmaScriptEvaluateVisitor.java
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
import FESI.Exceptions.*;

import java.util.Enumeration;

/**
 * Exception used to package any exception encountered during visiting
 * to an exception accepted by the visitor interface as defined by
 * JavaCC. Eventually the exception will be unpackaged and reraised.
 */
class PackagedException extends RuntimeException {
    EcmaScriptException exception;
    SimpleNode node;
    PackagedException (EcmaScriptException exception, SimpleNode node) {
        super();
        this.exception = exception;
        this.node = node;
    }
    public String getMessage() {
        return exception.getMessage();
    }
}

/**
 * The evaluate visitor use the visitor pattern to evaluate the
 * parsed code. Use for both main program and functions (using
 * different entries). A new evaluator is used everytime as it
 * contains the return code (because we can return only one variable).
 * <P>The parse tree must have been preprocessed by the variable and
 * function visitors first.
 */
public class EcmaScriptEvaluateVisitor 
            implements EcmaScriptVisitor,
                       EcmaScriptConstants
{
   
  // Continuation codes 
  public static final int C_NORMAL = 0;
  public static final int C_RETURN = 1;
  public static final int C_BREAK = 2;
  public static final int C_CONTINUE = 3;

  // Result of comparison
  private static final int COMPARE_TRUE = -1;
  private static final int COMPARE_UNDEFINED = 0;
  private static final int COMPARE_FALSE = 1;
  
  private boolean debug = false;
  
  // Indicator for the data to be returned by the accept
  private static final Object FOR_VALUE = new Object();
  private static final Object FOR_REFERENCE = new Object();
  
  // The visitor work on behalf on an evaluator which provide
  // the context information (as global variables)
  private Evaluator evaluator;
  
  // This is the final completion code - mark unused to protect against recursive use
  private int completionCode = -1;
  
  /**
   * Create a new visitor
   * @param evaluator On behalf of this evaluator
   */
  public EcmaScriptEvaluateVisitor(Evaluator evaluator) {
      super();
      this.evaluator = evaluator;
  }
  
  /**
   * Return the completion code of this evaluation
   * @return the last completion code as an int
   */
  public int getCompletionCode() {
      return completionCode;
  }
  
  /**
   * Return the completion code of this evaluation
   * @return the last completion code as a String
   */
  public String getCompletionCodeString() {
      final String[] data =  {"normal", "return", "break", "continue"};
      return data[completionCode];
  }
  
  /**
   * Evaluate a tree which represents a main program or the source
   * of an eval function.
   * @param node The parsed tree (annotated for variables)
   * @param es A description of the source for error messages
   * @return The result of the evaluation
   * @exception EcmaScriptException In case of any error during the evaluation
  */
  public ESValue evaluateProgram(ASTProgram node, EvaluationSource es) throws EcmaScriptException {
      
      EvaluationSource evaluationSource = es;
      if (completionCode != -1) {
          throw new ProgrammingError("Multiple use of evalution visitor");
      }
      completionCode = C_NORMAL; 
      ESValue result = null;
      
      if (debug) System.out.println("evaluateProgram for: " + node);
      
      try {
         result = (ESValue) node.jjtAccept(this, FOR_VALUE); 
      } catch (PackagedException e) {
          e.exception.appendEvaluationSource(new LineEvaluationSource(e.node.getLineNumber(),es));
          throw e.exception;
      }
      if (debug) System.out.println("evaluateProgram result: " + result);
      return result;
  }
  
  
  /**
   * Evaluate a tree which represents a function. The local variables
   * must have been established by the caller.
   * @param node The parsed tree (annotated for variables)
   * @param es A description of the source for error messages
   * @return The result of the evaluation
   * @exception EcmaScriptException In case of any error during the evaluation
   */
  public ESValue evaluateFunction(ASTStatementList node, EvaluationSource es) throws EcmaScriptException {
      if (completionCode != -1) {
          throw new ProgrammingError("Multiple use of evaluation visitor");
      }
      completionCode = C_NORMAL; 
      EvaluationSource evaluationSource = es;
      ESValue result = null;
      if (debug) System.out.println("evaluateFunction for: " + node);

      // Thread.yields inserted by Hannes Wallnoefer, workaround for poor thread schedulers
      // Thread.yield ();
      if (evaluator.thread != Thread.currentThread())
        throw new helma.framework.TimeoutException();

      try {
         result = (ESValue) node.jjtAccept(this, FOR_VALUE); 
      } catch (PackagedException e) {
          e.exception.appendEvaluationSource(new LineEvaluationSource(e.node.getLineNumber(),es));
          throw e.exception;
      }
      if (debug) System.out.println("evaluateFunction result: " + result);
      return result;
  }
  
  
  /**
   * This is a subevaluator. It evaluates a tree which represents
   * a <b>with</b> statement. It is called indirectly via the
   * evaluator when a with statement is encountered in the tree.
   * @param node The parsed tree (annotated for variables)
   * @param es A description of the source for error messages
   * @return The result of the evaluation
   * @exception EcmaScriptException In case of any error during the evaluation
   */
  public ESValue evaluateWith(ASTStatement node, EvaluationSource es) throws EcmaScriptException {
      if (completionCode != -1) {
          throw new ProgrammingError("Multiple use of evalution visitor");
      }
      completionCode = C_NORMAL; 
      ESValue result = null;
      if (debug) System.out.println("evaluateWith for: " + node);
      try {
          result = (ESValue) node.jjtAccept(this, FOR_VALUE);
      } catch (PackagedException e) {
          e.exception.appendEvaluationSource(new LineEvaluationSource(e.node.getLineNumber(),es));
          throw e.exception;
      }
     if (debug) System.out.println("evaluateWith result: " + result);
     return result;
  }
  
  /*--------------------------------------------------------------------
   * The following routines implement the interpretation process
   * For detail see the EcmaScript standard to which they refer
   *------------------------------------------------------------------*/
  
  // EcmaScript standard 11.8.5
  private int compare(ESValue v1, ESValue v2) throws EcmaScriptException {
      ESValue v1p = v1.toESPrimitive(ESValue.EStypeNumber);
      ESValue v2p = v2.toESPrimitive(ESValue.EStypeNumber);
      // System.out.println("v1p = " + v1 + " v2p = " + v2); 
      if ((v1p instanceof ESString) && (v2p instanceof ESString)) {
          // Note: Convert v1/2 instead of v1/2p for correct
          // behaviour of "" + Object;
            String s1 =v1.toString();
            String s2 =v2.toString();
            int c = s1.compareTo(s2);
            //System.out.println("CS: '"+ s1 + "' " +c+ " '" + s2 + "'");
            return (c < 0) ? COMPARE_TRUE : COMPARE_FALSE;
      } else {
         double d1 = v1.doubleValue();
         double d2 = v2.doubleValue();
         if (Double.isNaN(d1) || Double.isNaN(d2)) return COMPARE_UNDEFINED;
         int c = (v1.doubleValue()<v2.doubleValue()) ?
                                  COMPARE_TRUE : COMPARE_FALSE;
         //System.out.println("CN: '"+ d1 + "' " +c+ " '" + d2 + "'");
         return c;
      }
  }
  
  // EcmaScript standard 11.9.3
  private boolean equal(ESValue v1, ESValue v2) throws EcmaScriptException {
      
      // Not possible to optimize same object, as NaN != NaN
      
      if (v1.getTypeOf()==v2.getTypeOf()){
          // Same types
          if (v1 instanceof ESUndefined) return true;
          if (v1 instanceof ESNull) return true;
          if (v1 instanceof ESNumber) {
              double d1 = v1.doubleValue();
              double d2 = v2.doubleValue();
              return (d1==d2);
          }
          if (v1 instanceof ESString) {
            String s1 =v1.toString();
            String s2 =v2.toString();
            return s1.equals(s2);
          }
          if (v1 instanceof ESBoolean) {
            boolean b1 =v1.booleanValue();
            boolean b2 =v2.booleanValue();
            return b1==b2;
          }
          
          // ESNode wrappers must be checked with equals() because
          // it's possible that different wrappers wrap the same node!
          if (v1 instanceof helma.framework.IPathElement) {
            return v1.equals (v2);
          }
          
          return v1 == v2;
      }          

     if (v1 instanceof ESUndefined && v2 instanceof ESNull) return true;
     if (v2 instanceof ESUndefined && v1 instanceof ESNull) return true;
     
     if ((v1 instanceof ESNumber && v2 instanceof ESString) ||
         (v2 instanceof ESNumber && v1 instanceof ESString)) {
              double d1 = v1.doubleValue();
              double d2 = v2.doubleValue();
              return (d1==d2);
      }
     
     if (v1 instanceof ESBoolean || v2 instanceof ESBoolean) {
              double d1 = v1.doubleValue();
              double d2 = v2.doubleValue();
              return (d1==d2);
      }

     if ((v1 instanceof ESNumber && v2 instanceof ESObject) ||
         (v1 instanceof ESString && v2 instanceof ESObject)) {
              return equal(v1,v2.toESPrimitive());
      }
     if ((v2 instanceof ESNumber && v1 instanceof ESObject) ||
         (v2 instanceof ESString && v1 instanceof ESObject)) {
              return equal(v2,v1.toESPrimitive());
      }

     return false;
     
  }
  

  // The dispatching is by node type - if the specific visitor
  // is not implemented, then this routine is called
  public Object visit(SimpleNode node, Object data) {
    throw new ProgrammingError("Visitor not implemented for node type " + node.getClass());
  }
  
  // All routines have about the same form - thet check the number of
  // children and then iterate the children appropriately, taking the
  // the appropritate action for each children. Recursing the evaluation
  // is done via the routine jjtAccept.
  public Object visit(ASTProgram node, Object data) {
     int n = node.jjtGetNumChildren();
     if (n<=0) throw new ProgrammingError("Empty program not implemented");
     Object result =  node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
     for (int i = 1; i < node.jjtGetNumChildren(); i++) {
         Node statement = node.jjtGetChild(i);
         result =  statement.jjtAccept(this, FOR_VALUE);
     }
     return result;
  }
  
  public Object visit(ASTStatementList node, Object data) {
     int n = node.jjtGetNumChildren();
     // Accepts empty statement lists (for example generated
     // by function(){}
     Object result = ESUndefined.theUndefined;
     for (int i = 0; i < node.jjtGetNumChildren(); i++) {
         if (completionCode != C_NORMAL) return result;
         Node statement = node.jjtGetChild(i);
         result =  statement.jjtAccept(this,FOR_VALUE);
     }
     return result;
  }
  
  public Object visit(ASTFunctionDeclaration node, Object data) {
     return null;  // Ignored during interpretation
  }
  
  
  public Object visit(ASTFormalParameterList node, Object data) {
     // Should not occur during interpretation as we skip parent node
     throw new ProgrammingError("Should not visit this");
  }
  
  public Object visit(ASTStatement node, Object data) {
    Object result = null;
    int nChildren = node.jjtGetNumChildren();
    if (nChildren==1) {
        result = node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
    } else if (nChildren != 0) {
        throw new ProgrammingError("Bad AST in statement (>1 child");
    }
    return result;
  }
  
  public Object visit(ASTVariableDeclaration node, Object data) {
    int nChildren = node.jjtGetNumChildren();
    if (nChildren<1 || nChildren>2) {
        throw new ProgrammingError("Bad AST in variable declaration");
    }
    Object result = null;
    if (nChildren == 2) {
        try {
            Object lvo = node.jjtGetChild(0).jjtAccept(this,FOR_REFERENCE);
            ESReference lv;
            if (lvo instanceof ESReference) {
                lv = (ESReference) lvo;
            } else {
                // Should not happen as first node should be an identifier
                throw new ProgrammingError("Value '"+lvo.toString()+"' is not a variable");
            }
            ESValue rv = (ESValue) node.jjtGetChild(1).jjtAccept(this,FOR_VALUE);
            lv.putValue(null, rv); // null because the variable should be undefined!
            result = rv;
        } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
        }
    }
     return result;
  }
  
  public Object visit(ASTIfStatement node, Object data) {
    Object result = null;
    int nChildren = node.jjtGetNumChildren();
    if (nChildren<2 || nChildren>3) {
        throw new ProgrammingError("Bad AST in IF statement");
    }
    try {
        ESValue testValue = (ESValue) node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
        boolean test = testValue.booleanValue();
        if (test) {
            result = node.jjtGetChild(1).jjtAccept(this,FOR_VALUE);
        } else {
            if (nChildren==3) {
               result = node.jjtGetChild(2).jjtAccept(this,FOR_VALUE);
           }
        }
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
  }
  
  public Object visit(ASTWhileStatement node, Object data) {
    Object result = null;
    node.assertTwoChildren();
    try {
        ESValue testValue = (ESValue) node.jjtGetChild(0).jjtAccept(this,FOR_VALUE);
        while (testValue.booleanValue()) {

            // Thread.yield ();
            if (evaluator.thread != Thread.currentThread())
              throw new helma.framework.TimeoutException();

            result = node.jjtGetChild(1).jjtAccept(this, FOR_VALUE);
            if (completionCode == C_RETURN) {
                    return result;
            } else if (completionCode == C_BREAK) {
                    completionCode = C_NORMAL; 
                    return result;
            } else if (completionCode == C_CONTINUE) {
                    testValue = (ESValue) node.jjtGetChild(0).jjtAccept(this,FOR_VALUE);
                    completionCode = C_NORMAL; 
            } else {
                testValue = (ESValue) node.jjtGetChild(0).jjtAccept(this,FOR_VALUE);
            }
        }
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
  }
  
  public Object visit(ASTForStatement node, Object data) {
    Object result = null;
    try {
        node.assertFourChildren();
        // Evaluate first expression if present
        node.jjtGetChild(0).jjtAccept(this,FOR_VALUE); 
         
         Node testNode = node.jjtGetChild(1);
         ESValue testValue;
         if (testNode instanceof ASTEmptyExpression) {
             testValue = ESBoolean.makeBoolean(true);
         } else { 
             testValue = (ESValue) testNode.jjtAccept(this,FOR_VALUE);
         }
         while (testValue.booleanValue()) {
            // Thread.yield ();
            if (evaluator.thread != Thread.currentThread())
              throw new helma.framework.TimeoutException();

            result = node.jjtGetChild(3).jjtAccept(this, FOR_VALUE);

            if (completionCode == C_RETURN) {
                    return result;
            } else if (completionCode == C_BREAK) {
                    completionCode = C_NORMAL; 
                    return result;
            } else if (completionCode == C_CONTINUE) {
                    node.jjtGetChild(2).jjtAccept(this,FOR_VALUE); 
                    if (testNode instanceof ASTEmptyExpression) {
                        testValue = ESBoolean.makeBoolean(true);
                    } else { 
                        testValue = (ESValue) testNode.jjtAccept(this,FOR_VALUE);
                    }
                    completionCode = C_NORMAL; 
            } else {
                    node.jjtGetChild(2).jjtAccept(this,FOR_VALUE); 
                    if (testNode instanceof ASTEmptyExpression) {
                        testValue = ESBoolean.makeBoolean(true);
                    } else { 
                        testValue = (ESValue) testNode.jjtAccept(this,FOR_VALUE);
                    }
            }
    
        }
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }  
     return result;
  }
  
  // Assume that  in 12.6.2, for var, step 7, should be goto 17
  public Object visit(ASTForVarStatement node, Object data) {
    Object result = null; // No value by default
    try {
        node.assertFourChildren();
        node.jjtGetChild(0).jjtAccept(this,FOR_VALUE); 
         
        Node testNode = node.jjtGetChild(1);
        ESValue testValue;
        if (testNode instanceof ASTEmptyExpression) {
             testValue = ESBoolean.makeBoolean(true);
        } else { 
             testValue = (ESValue) testNode.jjtAccept(this,FOR_VALUE);
        }
        while (testValue.booleanValue()) {
            result = node.jjtGetChild(3).jjtAccept(this,FOR_VALUE);

            if (completionCode == C_RETURN) {
                    return result;
            } else if (completionCode == C_BREAK) {
                    completionCode = C_NORMAL; 
                    return result;
            } else if (completionCode == C_CONTINUE) {
                    node.jjtGetChild(2).jjtAccept(this,FOR_VALUE); 
                    if (testNode instanceof ASTEmptyExpression) {
                        testValue = ESBoolean.makeBoolean(true);
                    } else { 
                        testValue = (ESValue) testNode.jjtAccept(this,FOR_VALUE);
                    }
                    completionCode = C_NORMAL; 
            } else {
                    node.jjtGetChild(2).jjtAccept(this,FOR_VALUE); 
                    if (testNode instanceof ASTEmptyExpression) {
                        testValue = ESBoolean.makeBoolean(true);
                    } else { 
                        testValue = (ESValue) testNode.jjtAccept(this,FOR_VALUE);
                    }
            }

            // Thread.yield ();
            if (evaluator.thread != Thread.currentThread())
              throw new helma.framework.TimeoutException();
    
        }
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }  
     return result;
  }
  
  public Object visit(ASTForInStatement node, Object data) {
    Object result = null; // No value by default
    node.assertThreeChildren();
    try {
        ESValue ob = (ESValue) node.jjtGetChild(1).jjtAccept(this,FOR_VALUE);
        ESObject obj = (ESObject) ob.toESObject(evaluator);
        boolean directEnumeration = obj.isDirectEnumerator();
        for (Enumeration e = obj.getProperties() ; e.hasMoreElements() ;) {
            Object en = e.nextElement();
            ESValue s;
            if (directEnumeration) {
                s = ESLoader.normalizeValue(en, evaluator);
            } else {
                s = new ESString((String) (en.toString()));  
            }

            Object lvo = node.jjtGetChild(0).jjtAccept(this,FOR_REFERENCE);
            ESReference lv;
            if (lvo instanceof ESReference) {
                lv = (ESReference) lvo;
            } else {
                throw new EcmaScriptException("Value '"+lvo.toString()+"' is not an assignable object or property");
            }
            evaluator.putValue(lv, s);
            result = node.jjtGetChild(2).jjtAccept(this,FOR_VALUE);
            if (completionCode == C_RETURN) {
                    break;
            } else if (completionCode == C_BREAK) {
                    completionCode = C_NORMAL; 
                    break;
            } else if (completionCode == C_CONTINUE) {
                    completionCode = C_NORMAL;
                    continue; 
            }
        }        
            
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
  }
  
  public Object visit(ASTForVarInStatement node, Object data) {
    Object result = null; // No value by default
    node.assertFourChildren();        

    try {
        Object lvo = node.jjtGetChild(0).jjtAccept(this,FOR_REFERENCE);
        ESReference lv;
        if (lvo instanceof ESReference) {
            lv = (ESReference) lvo;
        } else {
            // Should not happen as it should be an identifier
            throw new ProgrammingError("Value '"+lvo.toString()+"' is not a variable");
        }        
        ESValue init = (ESValue) node.jjtGetChild(1).jjtAccept(this, FOR_VALUE);
        evaluator.putValue(lv, init);
           
        ESValue ob = (ESValue) node.jjtGetChild(2).jjtAccept(this,FOR_VALUE);
        ESObject obj = (ESObject) ob.toESObject(evaluator);
        boolean directEnumeration = obj.isDirectEnumerator();
        for (Enumeration e = obj.getProperties() ; e.hasMoreElements() ;) {
            Object en = e.nextElement();
            ESValue s;
            if (directEnumeration) {
                s = ESLoader.normalizeValue(en, evaluator);
            } else {
                s = new ESString((String) (en.toString())); 
            }
            // Typing already checked above - will generate an error anyhow
            lv = (ESReference) node.jjtGetChild(0).jjtAccept(this,FOR_REFERENCE);
            evaluator.putValue(lv, s);
            result = node.jjtGetChild(3).jjtAccept(this,FOR_VALUE);
            if (completionCode == C_RETURN) {
                    break;
            } else if (completionCode == C_BREAK) {
                    completionCode = C_NORMAL; 
                    break;
            } else if (completionCode == C_CONTINUE) {
                    completionCode = C_NORMAL;
                    continue; 
            }
        }        
            
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
  }
  
  public Object visit(ASTContinueStatement node, Object data) {
     node.assertNoChildren();
     completionCode = C_CONTINUE;
     return null;
  }

  public Object visit(ASTBreakStatement node, Object data) {
    node.assertNoChildren();
    completionCode = C_BREAK;
    return null;
  }
  
  public Object visit(ASTReturnStatement node, Object data) {
     node.assertOneChild();
     Object result = node.jjtGetChild(0).jjtAccept(this,FOR_VALUE);
     completionCode = C_RETURN;
     return result;
  }
  
  public Object visit(ASTWithStatement node, Object data) {
    node.assertTwoChildren();
    ESValue result = null;
    try {
        EvaluationSource es = (EvaluationSource) node.getEvaluationSource();
        ESValue scopeValue = (ESValue) node.jjtGetChild(0).jjtAccept(this,FOR_VALUE);
        ASTStatement statementNode = (ASTStatement) (node.jjtGetChild(1));
        ESObject scopeObject = (ESObject) scopeValue.toESObject(evaluator);
        result = evaluator.evaluateWith(statementNode, scopeObject, es); 
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
  }
  
  public Object visit(ASTThisReference node, Object data) {
     node.assertNoChildren();
     return evaluator.getThisObject();
  }
  
  /*
   * Attempt to minimize the creation of intermediate ESReferences.
   * This is prettry tricky. The trick is to keep the last result
   * as a delayed reference while looking ahead for parameter or 
   * array index. A delayed reference is represented by a non null
   * currentProperty with an object (which is the last result
   * returned). If the last Result is null, this indicates access to
   * the global environment.
   */
  public Object visit(ASTCompositeReference node, Object forWhat) {
     int nChildren = node.jjtGetNumChildren();
     if (nChildren<2) throw new ProgrammingError("Bad ast");
     
     try {
        // The base node is the first node in the serie.
        // If it is an identifier, it is a reference to a property
        // of the global object, so it can be a delayed reference to the
        // specified property of the global object. Otherwise it is 
        // some kind of expression returning a value, and therefore cannot
        // be a delayed reference.
        ESValue lastResult;
        ESValue currentProperty ;
     	{
	    Node baseNode = node.jjtGetChild(0);
	    if (baseNode instanceof ASTIdentifier) {
	        lastResult = null; // Means lookup in global environment (with scope)
	        String id = ((ASTIdentifier)baseNode).getName();
	        currentProperty = new ESString(id);
	    } else {
	        lastResult = (ESValue) baseNode.jjtAccept(this,FOR_VALUE);
	        currentProperty = null; // No reference so far
	    }
     	}
        
        // Here the currentProperty and lastResult are initialized.
        
        //****
        //System.out.println("--->ASTCompositeReference: for: " + 
        //       (forWhat==FOR_VALUE ? "VALUE" : "REF") + " lr="+lastResult+
        //       				", cp="+currentProperty  + "<---");
        
        for (int i = 1; i < nChildren; i++) {
            
            Node compositor = node.jjtGetChild(i);

            if ((compositor instanceof ASTPropertyValueReference) ||
             (compositor instanceof ASTPropertyIdentifierReference)) {
             	 // Object property accessor, will build a new delayed reference.
             	 
                // First dereference any indirect reference left by a previous iteration
                if (currentProperty != null) {
                    ESValue newBase;
                    String propertyName = currentProperty.toString();
                    if (lastResult == null) {
                        newBase = evaluator.getValue(propertyName,propertyName.hashCode());
                        //****
                        //System.out.println("--->NB = " + newBase + "<---");
                        if (newBase instanceof ESUndefined) {
                            throw new EcmaScriptException("The property '"+propertyName+
                                            "' is not defined in global object");
                        }                    
                    } else {
                        ESObject currentBase = (ESObject) lastResult.toESObject(evaluator);
                        //****
                        //System.out.println("--->CB *** " + currentBase.getClass() + " " + propertyName + "<---");
                        newBase = currentBase.getProperty(propertyName, propertyName.hashCode());
                        if (newBase instanceof ESUndefined) {
                            throw new EcmaScriptException("The property '"+propertyName+
                                            "' is not defined in object '"+currentBase.toString()+"'");
                        }
                    }
                    lastResult = newBase;
                    currentProperty = null; // Assure invariant at end of if
                }
                // Here the lastResult contains the result of the expression before the
                // current compositor, as a value. currentProperty is null as this is not a
                // delayed reference.
                
               
                // We get the current property name (in principle any expression, for example
                // in obj['base'+index.toString()].
                currentProperty = (ESValue) compositor.jjtAccept(this,FOR_VALUE);
                //System.out.println("--->LR = " + lastResult + 
                //            " currentProperty = " + currentProperty.toString() + "<---"); // *********

            } else if (compositor instanceof ASTFunctionCallParameters) {
            	 // We have parameters. The function object may be represented
            	 // by a Function value, or a delayed reference to a base object.
                
                // First get the arguments (evaluated)
                ESValue[] arguments = (ESValue []) compositor.jjtAccept(this, FOR_VALUE);
                
                // Find the 'this' for the function call. If it is a delayed
                // reference, the this object is represented by the last result (the
                // base of the reference).
                ESObject thisObject;
                //System.out.println("--->CP: " + currentProperty + "<---"); // *************
                if (currentProperty != null) {
                    // Delayed reference, find base object (global if base is null)
                    //System.out.println("--->LR: " + lastResult + "<---"); // *************
                    if (lastResult == null) {
                        thisObject = evaluator.getGlobalObject();
                        //System.out.println("--->GO: " + thisObject + "<---"); // *************
                    } else {
                        thisObject = (ESObject) lastResult.toESObject(evaluator); // if function is called via an object
                    }
                    // Special case (see standard document)
                    if (thisObject instanceof ESArguments) {
                        thisObject = evaluator.getGlobalObject();
                    }
                } else {
                    // Assume that global may never be an ESArgument (for performance)
                    thisObject = evaluator.getGlobalObject(); // Global object by default
                }
                
                // The thisObject will be the target of the call.
                // If we have a delayed reference, we can do an indirect call,
                // leaving it up to the target object to find the routine to call.
                // This allow native objects to implement their own lookup without
                // requiring the creating of an intermediate Function object.
                //System.out.println("--->THIS: " + thisObject.toDetailString() + "<---"); // *************
                if (currentProperty != null) {
                    // Use lastResult and property name for indirect call
                    String functionName = currentProperty.toString();
                    // System.out.println("--->FN: " + functionName + ", LR: " + lastResult + "<---"); // *************
                    if (lastResult == null) {
                        lastResult=evaluator.doIndirectCall(thisObject, 
                                                            functionName, functionName.hashCode(), 
                                                            arguments);
                    } else {
                        try {
                            lastResult = thisObject.doIndirectCall(evaluator, thisObject, functionName, arguments);
                        } catch (NoSuchMethodException e) {
                             throw new EcmaScriptException(e.getMessage());
                        }
                    }
                    currentProperty = null;
                } else {
                    System.out.println("--->Last result: " + lastResult + " " + lastResult.getClass() + "<---"); // ********
                    // Via global or WITH, use current object
                    ESValue theFunction = (ESObject) lastResult.toESObject(evaluator); // Conversion needed ?
                    lastResult = theFunction.callFunction(thisObject,arguments);
                }
                completionCode = C_NORMAL;

           } else {
                throw new ProgrammingError("Bad AST");
            }

        } // for
        
        // Either build reference or return object depending on type of request
	 // Here, if propertyName is not null, then lastResult.propertyName contains
	 // the value (delayed dereferencing). Otherwise the value is in 
	 // lastResult.
	 Object result; // Will be the returned value
        if (forWhat == FOR_VALUE) {
            // We want a value
            //System.out.println("--->Build value cp: " + currentProperty + " lr: " + lastResult + "<---"); // ********
            if (currentProperty != null) {
                // Must dereference value
                ESObject currentBase = (ESObject) lastResult.toESObject(evaluator);
                String propertyName = currentProperty.toString();
		 //System.out.println("--->getProperty in cb: " + currentBase + " pn: " + propertyName + "<---"); // *******
                result = currentBase.getProperty(propertyName,propertyName.hashCode());
            } else {
                // Last value is already the final value
                result = lastResult;
            }
        } else {
            // We want a reference - therefore it cannot be just a value, it
            // must be a delayed reference.
            if (currentProperty == null) {
                throw new EcmaScriptException("'"+lastResult.toString()+"' is not an assignable value");
            }
            ESObject currentBase = (ESObject) lastResult.toESObject(evaluator);
            String propertyName = currentProperty.toString();
            //System.out.println("--->Build ref cb: " + currentBase + " pn: " + propertyName + "<---"); // ********
            result = new ESReference(currentBase, propertyName, propertyName.hashCode());
         }

	  return result;

    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
  }
  
  public Object visit(ASTFunctionCallParameters node, Object data) {
     
     int nChildren=node.jjtGetNumChildren();
     ESValue[] arguments  = new ESValue[nChildren];
     for (int i=0; i<nChildren; i++) {
        arguments[i] = (ESValue) node.jjtGetChild(i).jjtAccept(this, FOR_VALUE);
     }
     return arguments;
  }
  
  public Object visit(ASTPropertyValueReference node, Object data) {
     node.assertOneChild();
     return node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
  }
  
  public Object visit(ASTPropertyIdentifierReference node, Object data) {
    node.assertOneChild();
    Object result = null;
    Node idNode = node.jjtGetChild(0);
    if (idNode instanceof ASTIdentifier) {
        String id = ((ASTIdentifier)idNode).getName();
        result = new ESString(id);
    } else {
        throw new ProgrammingError("Bad AST");
    }
     return result;
  }
  
  public Object visit(ASTAllocationExpression node, Object data) {
    node.assertTwoChildren();
    ESValue result = null;
    try {
      int nChildren=node.jjtGetNumChildren();
      Node baseNode = node.jjtGetChild(0);
      ESValue constr = (ESValue) baseNode.jjtAccept(this, FOR_VALUE); // Can be any expression (in fact a a.b.c sequence)
      Node compositor = node.jjtGetChild(1);
      if (compositor instanceof ASTFunctionCallParameters) {
            ASTFunctionCallParameters fc = (ASTFunctionCallParameters) compositor;
            ESValue[] arguments  = (ESValue []) fc.jjtAccept(this, FOR_VALUE);
            result = (ESValue) constr.doConstruct(evaluator.getThisObject(),arguments);
            if (! (result instanceof ESObject)) {
                throw new EcmaScriptException("new " + compositor+ " did not return an object");
            }
            completionCode = C_NORMAL;
      } else {
            throw new ProgrammingError("Bad AST");
      }
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }   
     return result;
  }
  
  public Object visit(ASTOperator node, Object data) {
    throw new ProgrammingError("Bad AST walk");
  }
  
  public Object visit(ASTPostfixExpression node, Object data) {
     ESValue result;
     try {
       node.assertTwoChildren();
       Object lvo = node.jjtGetChild(0).jjtAccept(this,FOR_REFERENCE);
       ESReference lv;
       if (lvo instanceof ESReference) {
            lv = (ESReference) lvo;
       } else {
            throw new EcmaScriptException("Value '"+lvo.toString()+"' is not an assignable object or property");
       }
       int operator = ((ASTOperator)(node.jjtGetChild(1))).getOperator();
       result = (ESValue) lv.getValue();
       double dv = result.doubleValue();
       if (operator == INCR) {
           dv++;
       } else if (operator == DECR) {
           dv--;
       } else {
            throw new ProgrammingError("Bad operator");
       }
       ESValue vr = new ESNumber(dv);
       evaluator.putValue(lv, vr);
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
}
  
  public Object visit(ASTUnaryExpression node, Object data) {
     ESValue r = null;
     try {
        node.assertTwoChildren();
        int operator = ((ASTOperator)(node.jjtGetChild(0))).getOperator();
        switch (operator) {
      case DELETE: {
             Object lvo = node.jjtGetChild(1).jjtAccept(this,FOR_REFERENCE);
             ESReference lv;
             if (lvo instanceof ESReference) {
                lv = (ESReference) lvo;
             } else {
                throw new EcmaScriptException("Value '"+lvo.toString()+"' is not a property reference");
             }
             ESValue base = lv.getBase();
             String propertyName = lv.getPropertyName();
             if (base instanceof ESObject) {
                 r = ESBoolean.makeBoolean(
                         ((ESObject) base).deleteProperty(propertyName, propertyName.hashCode()));
             } else {
                 r = ESBoolean.makeBoolean(true);
             }
           } 
           break;
      case VOID: 
             r = null;
             break;          
      case TYPEOF: {
            Node n = node.jjtGetChild(1);
            if (n instanceof ASTIdentifier) {
                // We need to get a reference, as an null based referenced is "undefined"
                ESReference ref = (ESReference) n.jjtAccept(this,FOR_REFERENCE);
                if (ref.getBase()==null) {
                    r = new ESString("undefined");
                } else {
                    ESValue v = ref.getValue();
                    r = new ESString(v.getTypeofString());
                }
            } else {
                // It is a value, directly get its string
                ESValue v = (ESValue) n.jjtAccept(this,FOR_VALUE);
                r = new ESString(v.getTypeofString());
            }
           } 
           break;
      case INCR: {
               Object lvo = node.jjtGetChild(1).jjtAccept(this,FOR_REFERENCE);
               ESReference lv;
               if (lvo instanceof ESReference) {
                    lv = (ESReference) lvo;
               } else {
                    throw new EcmaScriptException("Value '"+lvo.toString()+"' is not an assignable object or property");
               }               
               ESValue v = lv.getValue();
               double dv = v.doubleValue();
               dv++;
               r = new ESNumber(dv);
               evaluator.putValue(lv, r);
           }
           break;
       case DECR: {
               Object lvo = node.jjtGetChild(1).jjtAccept(this,FOR_REFERENCE);
               ESReference lv;
               if (lvo instanceof ESReference) {
                    lv = (ESReference) lvo;
               } else {
                    throw new EcmaScriptException("Value '"+lvo.toString()+"' is not an assignable object or property");
               }               
               ESValue v = lv.getValue();
               double dv = v.doubleValue();
               dv--;
               r = new ESNumber(dv);
               evaluator.putValue(lv, r);
           }
           break;
      case PLUS: {
              ESValue v = (ESValue) node.jjtGetChild(1).jjtAccept(this,FOR_VALUE);
              r = v.toESNumber();
           }
           break;
      case MINUS: {
              ESValue v = (ESValue) node.jjtGetChild(1).jjtAccept(this,FOR_VALUE);
              double dv = v.doubleValue();
              r = new ESNumber(-dv);
           }
           break;
      case TILDE: {
              ESValue v = (ESValue) node.jjtGetChild(1).jjtAccept(this,FOR_VALUE);
              int iv = v.toInt32();
              r = new ESNumber(~iv);
           }
           break;
      case BANG: {
              ESValue v = (ESValue) node.jjtGetChild(1).jjtAccept(this,FOR_VALUE);
              boolean bv = v.booleanValue();
              r = ESBoolean.makeBoolean(!bv);
           }
           break;
       default:
            throw new ProgrammingError("Unimplemented unary");
       }
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }

     return r;
  }
  
  public Object visit(ASTBinaryExpressionSequence node, Object data) {
     ESValue result = null;
     try {
        ESValue v1 = (ESValue) node.jjtGetChild(0).jjtAccept(this,FOR_VALUE);
        for (int i = 0; i < node.jjtGetNumChildren()-1; i+=2) {
            ESValue v2 = (ESValue) node.jjtGetChild(i+2).jjtAccept(this,FOR_VALUE);
            int operator = ((ASTOperator)(node.jjtGetChild(i+1))).getOperator();
            // System.out.println("V1 = " + v1 + " v2 = " + v2); 
            switch (operator) {
          case PLUS: {
                  ESValue v1p = v1.toESPrimitive();
                  ESValue v2p = v2.toESPrimitive();
                  // System.out.println("v1p = " + v1 + " v2p = " + v2); 
                  if ((v1p instanceof ESString) || (v2p instanceof ESString)) {
                      // Note: Convert v1/2 instead of v1/2p for correct
                      // behaviour of "" + Object;
                     result = new ESString(
                        v1.toString() + v2.toString());
                  } else {
                     result = new ESNumber(
                             v1.doubleValue()+v2.doubleValue());
                  }
                 }
               break;
          case MINUS: {
                    result = new ESNumber(
                        v1.doubleValue()-v2.doubleValue());
               }
               break;
          case STAR: {
                    result = new ESNumber(
                      v1.doubleValue()*v2.doubleValue());
               }
               break;
          case SLASH: {
                    result = new ESNumber(
                      v1.doubleValue()/v2.doubleValue());
               }
               break;
          case REM: {
                    result = new ESNumber(
                      v1.doubleValue()%v2.doubleValue());
               }
               break;
          case LSHIFT: {
                    result = new ESNumber(
                      v1.toInt32()<<v2.toUInt32());
               }
               break;
          case RSIGNEDSHIFT: {
                    result = new ESNumber(
                      v1.toInt32()>>v2.toUInt32());
               }
               break;
          case RUNSIGNEDSHIFT: {
                    result = new ESNumber(
                      v1.toUInt32()>>>v2.toUInt32());
               }
               break;
          case LT: {
                    int compareCode = compare(v1,v2);
                    if (compareCode ==  COMPARE_TRUE) {
                        result=ESBoolean.makeBoolean(true);
                    } else {
                        result=ESBoolean.makeBoolean(false);
                    }
                }
               break;
          case GT: {
                    int compareCode = compare(v2,v1);
                    if (compareCode ==  COMPARE_TRUE) {
                        result=ESBoolean.makeBoolean(true);
                    } else {
                        result=ESBoolean.makeBoolean(false);
                    }
                }
               break;
          case LE: {
                    int compareCode = compare(v2,v1);
                    if (compareCode ==  COMPARE_FALSE) {
                        result=ESBoolean.makeBoolean(true);
                    } else {
                        result=ESBoolean.makeBoolean(false);
                    }
                }
               break;
          case GE: {
                    int compareCode = compare(v1,v2);
                    if (compareCode ==  COMPARE_FALSE) {
                        result=ESBoolean.makeBoolean(true);
                    } else {
                        result=ESBoolean.makeBoolean(false);
                    }
                }
               break;
          case EQ: {
                   result=ESBoolean.makeBoolean(equal(v1,v2));
                }
               break;
          case NE: {
                   result=ESBoolean.makeBoolean(!equal(v1,v2));
                }
               break;
          case BIT_AND: {
                 int iv1 = v1.toInt32();
                 int iv2 = v2.toInt32();
                 result = new ESNumber(iv1 & iv2);
                }
               break;
          case BIT_OR: {
                 int iv1 = v1.toInt32();
                 int iv2 = v2.toInt32();
                 result = new ESNumber(iv1 | iv2);
                }
               break;
          case XOR: {
                 int iv1 = v1.toInt32();
                 int iv2 = v2.toInt32();
                 result = new ESNumber(iv1 ^ iv2);
                }
               break;
           default:
                  throw new ProgrammingError("Unimplemented binary");
           } // switch
           v1 = result;
        } // for
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
  }
  
  public Object visit(ASTAndExpressionSequence node, Object data) {
    ESValue result = null;
    int nChildren = node.jjtGetNumChildren();
    try {
        result = (ESValue) node.jjtGetChild(0).jjtAccept(this,FOR_VALUE);
        int i = 1;
        while (result.booleanValue() && (i<nChildren)) {
            result = (ESValue) node.jjtGetChild(i).jjtAccept(this,FOR_VALUE);
            i ++;
        }
        // Normalize to primitive - could be optimized...
        result = ESBoolean.makeBoolean(result.booleanValue());
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
  }
  
  public Object visit(ASTOrExpressionSequence node, Object data) {
    int nChildren = node.jjtGetNumChildren();
    ESValue result = null;
    try {
        result = (ESValue) node.jjtGetChild(0).jjtAccept(this,FOR_VALUE);
        int i = 1;
        while ((!result.booleanValue()) && (i<nChildren)) {
            result = (ESValue) node.jjtGetChild(i).jjtAccept(this,FOR_VALUE);
            i ++;
        }
        // Normalize to primitive - could be optimized...
        result = ESBoolean.makeBoolean(result.booleanValue());
        
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
  }

  public Object visit(ASTEmptyExpression node, Object data) {
     node.assertNoChildren();
     return ESUndefined.theUndefined;
  }

  public Object visit(ASTConditionalExpression node, Object data) {
    node.assertThreeChildren();
    Object result = null;
    try {
        ESValue t = (ESValue) node.jjtGetChild(0).jjtAccept(this,FOR_VALUE);
        boolean test = t.booleanValue();
        if (test) {
            result = node.jjtGetChild(1).jjtAccept(this, FOR_VALUE);
        } else {
            result = node.jjtGetChild(2).jjtAccept(this,FOR_VALUE);
        }
    } catch (EcmaScriptException e) {
        throw new PackagedException(e,node);
    }
     return result;
  }
  
  public Object visit(ASTAssignmentExpression node, Object data) {
    node.assertThreeChildren();
    ESValue result = null;
    try {
    	  // Get left hand side

        Object lvo = node.jjtGetChild(0).jjtAccept(this,FOR_REFERENCE);
        //System.out.println("REF: " + lvo);
        ESReference lv;
        if (lvo instanceof ESReference) {
            lv = (ESReference) lvo;
        } else {
            throw new EcmaScriptException("Value '"+lvo.toString()+"' is not an assignable object or property");
        }
        
        // get Right hand side
        ESValue v2 = (ESValue) node.jjtGetChild(2).jjtAccept(this,FOR_VALUE);
        
        // Case analysis based on assignement operator type
        int operator = ((ASTOperator)(node.jjtGetChild(1))).getOperator();
        if (operator == ASSIGN) {
        	   // Simple assignement may create a new property
            evaluator.putValue(lv, v2);
            result = v2;
        } else {
        	 // All composite assignement requires a current value
          ESValue v1 = lv.getValue();
          switch (operator) {
      case PLUSASSIGN: {
              ESValue v1p = v1.toESPrimitive();
              ESValue v2p = v2.toESPrimitive();
              // System.out.println("v1p = " + v1 + " v2p = " + v2); 
              if ((v1p instanceof ESString) || (v2p instanceof ESString)) {
                  // Note: Convert v1/2 instead of v1/2p for correct
                  // behaviour of "" + Object;
                 result = new ESString(
                    v1.toString() + v2.toString());
              } else {
                 result = new ESNumber(
                         v1.doubleValue()+v2.doubleValue());
              }
             }
           break;
      case MINUSASSIGN: {
                result = new ESNumber(
                    v1.doubleValue()-v2.doubleValue());
            }
          break;
      case STARASSIGN: {
                result = new ESNumber(
                  v1.doubleValue()*v2.doubleValue());
           }
           break;
      case SLASHASSIGN: {
                result = new ESNumber(
                  v1.doubleValue()/v2.doubleValue());
           }
           break;
      case ANDASSIGN: {
             int iv1 = v1.toInt32();
             int iv2 = v2.toInt32();
             result = new ESNumber(iv1 & iv2);
           }
           break;
      case ORASSIGN: {
             int iv1 = v1.toInt32();
             int iv2 = v2.toInt32();
             result = new ESNumber(iv1 | iv2);
           }
           break;
      case XORASSIGN: {
             int iv1 = v1.toInt32();
             int iv2 = v2.toInt32();
             result = new ESNumber(iv1 ^ iv2);
           }
           break;
      case REMASSIGN: {
                result = new ESNumber(
                  v1.doubleValue()%v2.doubleValue());
           }
           break;
      case LSHIFTASSIGN: {
                result = new ESNumber(
                  v1.toInt32()<<v2.toUInt32());
           }
           break;
      case RSIGNEDSHIFTASSIGN: { 
                result = new ESNumber(
                  v1.toInt32()>>v2.toUInt32());
           }
           break;
      case RUNSIGNEDSHIFTASSIGN: {
                result = new ESNumber(
                  v1.toUInt32()>>>v2.toUInt32());
           }
           break;
      default:
              throw new ProgrammingError("Unimplemented assign operator");
         } // switch
       evaluator.putValue(lv, result);
       v2 = result;
        }
    } catch (EcmaScriptException e) {
            throw new PackagedException(e,node);
    }
     return result;
  }
  
  public Object visit(ASTExpressionList node, Object data) {
     int n = node.jjtGetNumChildren();
     Object result = null;
     if (n<=0) throw new ProgrammingError("Empty expression list");
     result = node.jjtGetChild(0).jjtAccept(this, FOR_VALUE);
     for (int i = 1; i < node.jjtGetNumChildren(); i++) {
         Node statement = node.jjtGetChild(i);
         result = statement.jjtAccept(this, FOR_VALUE);
     }
     return result;
  }
  
  public Object visit(ASTLiteral node, Object data) {
     node.assertNoChildren();
     return (ESValue) node.getValue();
  }
  
  public Object visit(ASTIdentifier node, Object forWhat) {
     Object result;
     try {
        if (forWhat == FOR_VALUE) {
            result = evaluator.getValue(node.getName(),node.hashCode());
        } else {
            result = evaluator.getReference(node.getName(),node.hashCode());
        }
     } catch (EcmaScriptException e) {
        throw new PackagedException(e,node);
     }
     return result;
  }

}

