// OptionalRegExp.java
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

package FESI.Extensions;

import FESI.Parser.*;
import FESI.AST.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;
import FESI.Data.*;


/**
 * Create the regular expression object from either the
 * OROINC library or the GNU regexp libray depending which
 * one (if any) is available).
 */


public class OptionalRegExp extends Extension {
   
    private Evaluator evaluator = null;
    
    /**
     * A dummy object used if no regular expression tool can be found
     */
    class GlobalObjectRegExp extends BuiltinFunctionObject {
        GlobalObjectRegExp(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
         public ESValue callFunction(ESObject thisObject, 
                                            ESValue[] arguments)
                   throws EcmaScriptException {
               return doConstruct(thisObject, arguments);
         }
            
         public ESObject doConstruct(ESObject thisObject, 
                                            ESValue[] arguments)
                   throws EcmaScriptException {
                       
               throw new EcmaScriptException("RegExp package not loaded, probably not on CLASSPATH");
         }
    }
    

    public OptionalRegExp () {
        super();
    }
      
    /**
     * Load the library at extension initialization time
     */     
    public void initializeExtension(Evaluator evaluator) throws EcmaScriptException {
        
        Object regExp = null; // none fond
        	
        this.evaluator = evaluator;

	 // First attempt using ORO (as it is of higher quality)
        regExp = evaluator.addExtension("FESI.Extensions.ORORegExp");
        
        if (regExp == null) {
		// Then attempt using GNU (as it is LGPL)
        	regExp = evaluator.addExtension("FESI.Extensions.GNURegExp");
        }
        
        // If neither is present, make a dummy object which will generate an error
        // if the user attempt to use regluar expression
        if (regExp == null) {

            GlobalObject go = evaluator.getGlobalObject();
            FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();       
            
            ESObject globalObjectRegExp = 
               new GlobalObjectRegExp("RegExp", evaluator, fp);         
            globalObjectRegExp.putHiddenProperty("length",new ESNumber(1));
            go.putHiddenProperty("RegExp", globalObjectRegExp);
        }
    }

}
 
 