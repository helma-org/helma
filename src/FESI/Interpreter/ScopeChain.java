// ScopeChain.java
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

import FESI.Exceptions.*;
import FESI.Data.*;

/**
 * The scope chaine has established by WITH statements. Used to 
 * lookup values by name.
 */
public class ScopeChain {
    private ScopeChain previousElement;
    private ESObject thisElement;
    
    /**
     * CReate a new scope chain linked to a previous one (which
     * is null only for the topmost chain)
     * @param thisElement Object to look at at this level
     * @param previousElement previous object in scope chain
     */
    ScopeChain(ESObject thisElement, ScopeChain previousElement) {
        this.previousElement = previousElement;
        this.thisElement = thisElement;
    }
    
    /** 
     * Return the previous element in scope chain
     * @return The previous element
     */
    ScopeChain previousScope() {
        return previousElement;
    }
    

    /**
     * Return a reference to an object in the scope chain, so that the value
     * can be accessed or modified.
     *
     * @param   identifier  The name of the object
     * @return  an ESReference object    
     * @exception  EcmaScriptException Not thrown 
     */
    public ESReference getReference(String identifier) throws EcmaScriptException {
      return getReference(identifier, identifier.hashCode());
   } 

    /**
     * Return a reference to an object in the scope chain, so that the value
     * can be accessed or modified.
     *
     * @param   identifier  The name of the object
     * @param   hash The hashCode of this identifier
     * @return  an ESReference object    
     * @exception   EcmaScriptException  Not thrown
     */
    public ESReference getReference(String identifier, int hash) throws EcmaScriptException {
          ScopeChain theChain = this;
          do {
              if (theChain.thisElement.hasProperty(identifier, hash)) {
                  return new ESReference(theChain.thisElement, identifier, hash);
              }
              theChain = theChain.previousElement;
          } while (theChain != null);
      return new ESReference(null, identifier, hash);
   } 
    

    /**
     * Return a value for an object in the scope chain.
     * return ESUndefined for undefined properties of existing objects,
     * but generate an error for unexistent object.
     * <P>A variant of getProperty is used, which will call recursively
     * this routine and the getProperty in a previous scope until all
     * scopes have been examined or a value is returned. This avoid the
     * call of hasProperty followed by a second call to getProperty to
     * get the property value.
     *
     * @param   identifier  The name of the object
     * @return  The value of the object, possibly ESUndefined
     * @exception   EcmaScriptException  if no global object has that value
     */
   public ESValue getValue(String identifier) throws EcmaScriptException {
       return getValue(identifier, identifier.hashCode());
   }


    /**
     * Return a value for an object in the scope chain.
     * return ESUndefined for undefined properties of existing objects,
     * but generate an error for unexistent object.
     * <P>A variant of getProperty is used, which will call recursively
     * this routine and the getProperty in a previous scope until all
     * scopes have been examined or a value is returned. This avoid the
     * call of hasProperty followed by a second call to getProperty to
     * get the property value.
     *
     * @param   identifier  The name of the object
     * @param hash The hash code of the element (for optimization)
     * @return  The value of the object, possibly ESUndefined
     * @exception   EcmaScriptException  if no global object has that value
     */
   public ESValue getValue(String identifier, int hash) throws EcmaScriptException {
       return thisElement.getPropertyInScope(identifier, previousElement, hash);
   }
    
    
    /**
     * Call a function defined by name in the scope chain.
     * <P>A variant of doCall is used, which will call recursively
     * this routine and the doCall in a previous scope until all
     * scopes have been examined or a value is returned. This avoid the
     * call of hasProperty followed by a second call to getProperty to
     * get the property value.
     *
     * @param   evaluator  The evaluator doing the call
     * @param   thisObject  The 'this' object of the call
     * @param   functionName  The name of the function to call
     * @param hash The hash code of the function name (for optimization)
     * @param arguments The parameters of the call
     * @return  The result of the call, possibly NULL
     * @exception   EcmaScriptException  for any error
     */
   public ESValue doIndirectCall(Evaluator evaluator,
                                ESObject thisObject, 
                                String functionName,
                                int hash, 
                                ESValue[] arguments) throws EcmaScriptException {
       return thisElement.doIndirectCallInScope(evaluator, previousElement, thisObject, functionName, hash, arguments);
   }

}