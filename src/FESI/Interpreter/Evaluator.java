// Evaluator.java
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
import FESI.Parser.*;
import FESI.AST.*;
import FESI.Extensions.Extension;
import FESI.jslib.*;
import FESI.Data.*;

import java.util.Vector;
import java.util.Hashtable;
import java.util.Enumeration;
import java.util.EventObject;
import java.util.StringTokenizer;

import java.io.*;
import java.util.zip.*;

/**
 * Defines the evaluation interface and contains the evaluation context.
 * <P><B>Important:</B> This object is also used as the synchronization
 * object - all entries into the evaluation process must be synchronized
 * on the evaluator, as the inside of the evaluator is not synchronized
 * for speed reasons.
 */
public class Evaluator {

  // used to stop thread, 06.12.99 Hannes Wallnoefer
  public volatile Thread thread;
  // used to retrieve wrappers with correct Prototype for path elements in ESLoader
  public helma.framework.core.RequestEvaluator reval;

  private static String eol = System.getProperty("line.separator", "\n");

  /**
   * Return the version identifier of the interpreter
   */
  public static String getVersion() {
      return "1.1.4 (30-Jan-2000)";
  }
    
  /**
   * Return the welcome text (including copyright and version)
   * of the interpreter (as two lines)
   */
  public static String getWelcomeText() {
      return 
      "FESI (pronounced like 'fuzzy'): an EcmaScript Interpreter" + eol +
      "Copyright (c) Jean-Marc Lugrin, 1998 - Version: " + Evaluator.getVersion();

  }
    
  private boolean debugParse = false;

  // All privileged objects of interest of the evaluator
  
  private GlobalObject globalObject = null;

  private ESObject objectPrototype = null;
  private ESObject functionPrototype = null;
  private ESObject functionObject = null;
  private ESObject stringPrototype = null;
  private ESObject numberPrototype = null;
  private ESObject booleanPrototype = null;
  private ESObject arrayPrototype = null;
  private ESObject datePrototype = null;
  private ESObject packageObject = null;
  
  // Current environment
  private ScopeChain theScopeChain = null;
  private ESObject currentVariableObject = null;
  private ESObject currentThisObject = null;

  // Visitors used for interpretation
  private EcmaScriptFunctionVisitor functionDeclarationVisitor = null;
  private EcmaScriptVariableVisitor varDeclarationVisitor = null;
  //private EcmaScriptEvaluateVisitor evaluationVisitor = null;
  
  // List of loaded extensions
  private Hashtable extensions = null;
     
  /**
   * Reset the evaluator, forgetting all global definitions and loaded extensions
   */
  protected void reset() {
       functionDeclarationVisitor =  new EcmaScriptFunctionVisitor(this);
       varDeclarationVisitor =  new EcmaScriptVariableVisitor(this);
       // evaluationVisitor = new EcmaScriptEvaluateVisitor(this);
       globalObject = GlobalObject.makeGlobalObject(this);
       packageObject = new ESPackages(this);
       
       extensions = new Hashtable(); // forget extensions
       
  }
  
  /**
   * Create a new empty evaluator
   */
  public Evaluator () {
      reset();
  }
  
  /**
   * Get the variable visitor of this evaluator
   * @return the Variable visitor
   */
  public EcmaScriptVariableVisitor getVarDeclarationVisitor() {
      return varDeclarationVisitor;
  }

   //------------------------------------------------------------
   // Access to special objects of the environment
   //------------------------------------------------------------
  
  /**
   * Get the this object of this evaluator
   * @return the this object
   */
  public ESObject getThisObject() {
      return currentThisObject;
  }
  
  /**
   * Get the global object of this evaluator
   * @return the global object
   */
  public GlobalObject getGlobalObject() {
      return globalObject;
  }
  

  /** 
   * Set the debug mode for the parser
   * @param dp true to set debug mode on
   */
  public void setDebugParse(boolean dp) {
      debugParse = dp;
  }
  
  /**
   * Return the debug state for the parser
   * @return true if debug on
   */
  public boolean isDebugParse() {
      return debugParse;
  }
  
   /**
    * Set the object prototype object
    * <P>Used only by initilization code
    * @param o the object
    */
   public void setObjectPrototype(ESObject o) {
        objectPrototype = o;
    }
    /**
     * Get the Object prototype object
     * @return the ESObject
     */
    public ESObject getObjectPrototype() {
        return objectPrototype;
    }
    
   /**
    * Set the Function prototype object
    * <P>Used only by initilization code
    * @param o the object
    */
    public void setFunctionPrototype(ESObject o) {
        functionPrototype = o;
    }
    /**
     * Get the Function prototype object
     * @return the ESObject
     */
    public ESObject getFunctionPrototype() {
        return functionPrototype;
    }
    
   /**
    * Set the Function object
    * <P>Used only by initilization code
    * @param o the object
    */
    public void setFunctionObject(ESObject o) {
        functionObject = o;
    }
    /**
     * Get the Function object
     * @return the ESObject
     */
    public ESObject getFunctionObject() {
        return functionObject;
    }
    
    
   /**
    * Set the String object prototype
    * <P>Used only by initilization code
    * @param o the object
    */
    public void setStringPrototype(ESObject o) {
        stringPrototype = o;
    }
    /**
     * Get the String prototype object
     * @return the ESObject
     */
    public ESObject getStringPrototype() {
        return stringPrototype;
    }
    

   /**
    * Set the Number object prototpe
    * <P>Used only by initilization code
    * @param o the object
    */
    public void setNumberPrototype(ESObject o) {
        numberPrototype = o;
    }
    /**
     * Get the Number prototype object
     * @return the ESObject
     */
    public ESObject getNumberPrototype() {
        return numberPrototype;
    }

   /**
    * Set the Boolean object prototype
    * <P>Used only by initilization code
    * @param o the object
    */
    public void setBooleanPrototype(ESObject o) {
        booleanPrototype = o;
    }
    /**
     * Get the Boolean prototype object
     * @return the ESObject
     */
    public ESObject getBooleanPrototype() {
        return booleanPrototype;
    }

   /**
    * Set the Array prototype object
    * <P>Used only by initilization code
    * @param o the object
    */
    public void setArrayPrototype(ESObject o) {
        arrayPrototype = o;
    }
    /**
     * Get the Array prototype object
     * @return the ESObject
     */
    public ESObject getArrayPrototype() {
        return arrayPrototype;
    }
    
   /**
    * Set the Date object prototype
    * <P>Used only by initilization code
    * @param o the object
    */
    public void setDatePrototype(ESObject o) {
        datePrototype = o;
    }
    /**
     * Get the Date prototype object
     * @return the ESObject
     */
   public ESObject getDatePrototype() {
        return datePrototype;
    }

    /**
     * Get the Package object
     * @return the ESObject
     */
    public ESObject getPackageObject() {
        return packageObject;
    }


   //------------------------------------------------------------
   // Extension support
   //------------------------------------------------------------
   
   /**
    * Get a loaded extension by name
    * @param name Extension to look up
    * @return the extension or null if not loaded
    */
   public Extension getExtension(String name) {
       return (Extension) extensions.get(name);
   }
   
   /**
    * Get the list of all extensions
    * @return The extensions enumnerator
    */
   public Enumeration getExtensions() {
       return extensions.keys();
   }
   
   /**
    * Add an extension by name, load it if not already loaded
    * @param name the name of the extension to load
    * @return the loaded object or null in case of error
    * @exception Error ini initalizing the extension
    */
   public Object addExtension(String name) throws EcmaScriptException {
       Object extension = getExtension(name);
       if (extension == null) {
           try {
               extension = Class.forName(name).newInstance();
               if (extension instanceof Extension) {
                  ((Extension) extension).initializeExtension(this);
               } else if (extension instanceof JSExtension) {
                  GlobalObject go = this.getGlobalObject();
                  JSGlobalWrapper jgo = new JSGlobalWrapper(go,this);
                  try {
                      ((JSExtension) extension).initializeExtension(jgo);
                  } catch (JSException e) {
                      return null;
                  }
               } else {
                   return null;
               }
               extensions.put(name, extension);
          } catch (ClassNotFoundException e) { // return null
              extension = null;
          } catch (NoClassDefFoundError e) { // return null
              extension = null;
          } catch (IllegalAccessException e) { // return null
              extension = null;
          } catch (InstantiationException e) { // return null
              extension = null;
          }
       }
       return extension;
   }
     
   
   /**
    * Add an extension by name, load it if not already loaded.
    * Generate an error if not found
    * @param name the name of the extension to load
    * @return the loaded object 
    * @exception EcmaScriptException if the extension cannot be loaded or error during initilization
    */
   public Object addMandatoryExtension(String name) throws EcmaScriptException {
       Object extension = getExtension(name);
       if (extension == null) {
           try {
               extension = Class.forName(name).newInstance();
               if (extension instanceof Extension) {
                  ((Extension) extension).initializeExtension(this);
               } else if (extension instanceof JSExtension) {
                  GlobalObject go = this.getGlobalObject();
                  JSGlobalWrapper jgo = new JSGlobalWrapper(go,this);
                  try {
                      ((JSExtension) extension).initializeExtension(jgo);
                  } catch (JSException e) {
                      throw new EcmaScriptException("Error initializing extension " + name, e);
                  }
               } else {
                   throw new EcmaScriptException("Extenstion object " + name + " of wrong type " + extension.getClass());
               }
               extensions.put(name, extension);
          } catch (ClassNotFoundException e) { 
                throw new EcmaScriptException("Error loading extension " + name, e);
          } catch (NoClassDefFoundError e) { 
                throw new EcmaScriptException("Error loading extension " + name, e);
          } catch (IllegalAccessException e) { 
                throw new EcmaScriptException("Error loading extension " + name, e);
          } catch (InstantiationException e) {
                throw new EcmaScriptException("Error loading extension " + name, e);
          }
       }
       return extension;
   }

  /**
    * Add an initialized extension.
    * Generate an error if not found
	* @param name the name of the extension to load
	* @param extension The extension object
 	* @return the loaded object 
 	* @exception EcmaScriptException if the extension cannot be loaded or error during initilization
 	*/
    public Object addMandatoryExtension(String name,FESI.jslib.JSExtension extension) throws EcmaScriptException {
 		   
 		GlobalObject go = this.getGlobalObject();
 		JSGlobalWrapper jgo = new JSGlobalWrapper(go,this);
 		try {
 				((JSExtension) extension).initializeExtension(jgo);
 		} catch (JSException e) {
 				throw new EcmaScriptException("Error initializing extension " + name, e);
 		}
 			   
 		extensions.put(name, extension);
 		
 	   	return extension;
   }            

  /**
   * Get a reference to an indentifier (when its hash code is not known)
   * @param identifier The name of the variable 
   * @return A reference object
   */
  public ESReference getReference(String identifier) throws EcmaScriptException {
     return theScopeChain.getReference(identifier);
  }
  /**
   * Get a reference to an indentifier (when its hash code is known)
   * @param identifier The name of the variable 
   * @param hash Its hash code (must be exact!)
   * @return A reference object
   */
  public ESReference getReference(String identifier,int hash) throws EcmaScriptException {
     return theScopeChain.getReference(identifier, hash);
  }
  /**
   * Get the value of a variable in the scope chain (when its hash code is not known)
   * @param identifier The name of the variable 
   * @return A value
   */
  public ESValue getValue(String identifier) throws EcmaScriptException {
     return theScopeChain.getValue(identifier);
  }
  /**
   * Get the value of a variable in the scope chain (when its hash code is known)
   * @param identifier The name of the variable 
   * @param hash Its hash code (must be exact!)
   * @return A value
   */
  public ESValue getValue(String identifier,int hash) throws EcmaScriptException {
     return theScopeChain.getValue(identifier, hash);
  }
  
  /**
   * Call a routine referenced by name (in the scope chain)
   * @param thisObject the this of the called routine
   * @param functionName The name of the function
   * @param Its hash code
   * @param arguments The argument array
   * @exception EmcaScriptException In case of any error during evaluation
   * @return the resulting value 
   */
  public ESValue doIndirectCall(ESObject thisObject, String functionName,int hash, ESValue[] arguments) throws EcmaScriptException {
     return theScopeChain.doIndirectCall(this, thisObject, functionName, hash, arguments);
  }

  /**
   * Create variable only if does not already exist (do not overwrite parameters
   * and functions of the same name)
   * @param name the new variable name
   * @param hashCode Its hash code
   * @exception EmcaScriptException In case of any error during setting
   */
  public void createVariable(String name, int hashCode) throws EcmaScriptException {
     if (!currentVariableObject.hasProperty(name, hashCode)) {
        ESReference newVar = new ESReference(currentVariableObject, name, hashCode);
        newVar.putValue(currentVariableObject, ESUndefined.theUndefined);
    }
  }
  
  /**
   * Put a value in a variable (given as a reference)
   * @param leftValue The reference to the variable to modify
   * @param rightValue The value to set
   * @exception EmcaScriptException In case of any error during evaluation
   */
  public void putValue(ESReference leftValue, ESValue rightValue) throws EcmaScriptException {
      leftValue.putValue(globalObject, rightValue);
  }

  /**
   * Sub evaluator - evaluate an eval string in a program (not a top level evaluation !)
   * @param theSource The string to evaluate
   * @return The result of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
   public ESValue evaluateEvalString(String theSource) throws EcmaScriptException {
     ESValue theValue = ESUndefined.theUndefined;
    java.io.StringReader is = 
            new java.io.StringReader(theSource);
    EcmaScript parser = new EcmaScript(is);
    ASTProgram programNode = null;
    StringEvaluationSource es = new StringEvaluationSource(theSource, null);
    try {
        // ASTProgram n = parser.Program();
        programNode = (ASTProgram)parser.Program();
        if (debugParse) {
            System.out.println();
            System.out.println("Dump parse tree of eval (debugParse true)");
            programNode.dump("");
        }

    } catch (ParseException e) {
        if (debugParse) {
            System.out.println("[[PARSING ERROR DETECTED: (debugParse true)]]");
            System.out.println(e.getMessage());
            System.out.println("[[BY ROUTINE:]]");
            e.printStackTrace();
            System.out.println();
        }
        throw new EcmaScriptParseException(e, es);
     } catch (TokenMgrError e) {
        if (debugParse) {
            System.out.println("[[LEXICAL ERROR DETECTED: (debugParse true)]]");
            System.out.println(e.getMessage());
            System.out.println("[[BY ROUTINE:]]");
            e.printStackTrace();
            System.out.println();
        }
        throw new EcmaScriptLexicalException(e, es);
    }
        
    ESObject savedVariableObject = currentVariableObject;
    currentVariableObject = globalObject;
    
    try {
        
        functionDeclarationVisitor.processFunctionDeclarations(programNode, es);
        varDeclarationVisitor.processVariableDeclarations(programNode, es);
        EcmaScriptEvaluateVisitor evaluationVisitor = new EcmaScriptEvaluateVisitor(this);
        theValue = evaluationVisitor.evaluateProgram(programNode, es);
       
        if (theValue==null) theValue = ESUndefined.theUndefined; // null is not a valid result
        
        if (evaluationVisitor.getCompletionCode()!= EcmaScriptEvaluateVisitor.C_NORMAL) {
            throw new EcmaScriptException("Unexpected " + 
                    evaluationVisitor.getCompletionCodeString() + 
                    " in eval parameter top level" );
        }
    } finally {
        currentVariableObject = savedVariableObject;
    }
            
    
     return theValue;
}
  

  /**
   * Sub evaluator - evaluate a loaded file as program (not a top level evaluation !)
   * @param file The file to load
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  public ESValue evaluateLoadFile(File file) throws EcmaScriptException {
    ESValue theValue = ESUndefined.theUndefined;
    if (!file.isFile()) {
        throw new EcmaScriptException("File '" + file.getPath() + "' does not exist or is not a text file");
    }
    EvaluationSource es = new FileEvaluationSource(file.getPath(), null);
    FileReader fr=null;
    try {
      fr = new FileReader(file);
      theValue = evaluate(fr, null, es, false); // no return on main file
      if (theValue == null) theValue = ESUndefined.theUndefined;
    } catch (IOException e) {
        throw new EcmaScriptException("IO Error loading file " + file + ": " + e);
    } finally {
       if (fr!=null) {
           try {
               fr.close();
           } catch (IOException ignore) {
           }
       }
    }
    return theValue;
}

  /**
   * Sub evaluator - evaluate a module (a file or jar entry loaded via
   * the FESI.path) as program (not a top level evaluation !)
   * @param moduleName The name of the module to load
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  public ESValue evaluateLoadModule(String moduleName) throws EcmaScriptException {
      ESValue theValue = ESUndefined.theUndefined;
       if (moduleName == null) throw new EcmaScriptException("Missing file or module name for load");
       String path = System.getProperty("FESI.path", null);
       if (path == null) path = System.getProperty("java.class.path",null);
       // System.out.println("** Try loading via " + path);
       
       ESValue value =  ESUndefined.theUndefined; 
       if (path == null) {
           File file = new File(moduleName);
           try {
               value = evaluateLoadFile(file);
           } catch (EcmaScriptParseException e) {
               e.setNeverIncomplete();
               throw e;
           }
       }
       String lcModuleName = moduleName.toLowerCase();
       boolean hasSuffix = lcModuleName.endsWith(".es") ||
                           lcModuleName.endsWith(".esw") ||
                           lcModuleName.endsWith(".js");
       String separator = System.getProperty("path.separator",";");
       StringTokenizer st = new StringTokenizer(path, separator);
       while (st.hasMoreTokens()) {
           String tryPath = st.nextToken();
           value = tryLoad(tryPath, moduleName, hasSuffix);
           if (value != null) break; // Found
       }      
           
       if (value == null) {
           // Not found
           throw new EcmaScriptException("Module " + moduleName + " not found in " + path);
       }
           
       return value;
    }
 
  /** 
   * Try to load in a single path (directory or jar) environment  
   * (Utility routine to try load of each path entry)
   * @param tryPath The path to try
   * @param moduleName the name of the module to load
   * @param hasSuffix true if the module name has a specified suffix
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
   private ESValue tryLoad(String tryPath, String moduleName, boolean hasSuffix) throws EcmaScriptException {
        // System.out.println("** tryPath: " + tryPath);
        File dir = new File(tryPath);
        if (dir.isDirectory()) {
            File file;
            if (hasSuffix) {
                file = new File(dir, moduleName);
            } else {
                file = new File(dir, moduleName+".es");
                if (! file.exists()) {
                    file = new File(dir, moduleName+".esw");
                }
                if (! file.exists()) {
                    file = new File(dir, moduleName+".js");
                }
            }
            if (!file.exists()) return null;
            
            // A File is found, load it
            String cp;
            try {
                cp = file.getCanonicalPath();
            } catch (IOException e) {
                throw new EcmaScriptException("IO error accessing module " + moduleName + 
                                            " in directory  " + dir, e);
            }
            // System.out.println("** File found: " + cp);
            return evaluateLoadFile(file);
            
        } else if (dir.isFile()) {
            // System.out.println("** Looking in jar/zip: " + dir);
            ZipFile zipFile;
            try {
                String cp = dir.getCanonicalPath();
                zipFile = new ZipFile(cp); 
            } catch (IOException e) {
                return null;        // Cannot open jar/zip, ignore
            }
            ZipEntry  zipEntry;
            if (hasSuffix) {
                zipEntry = zipFile.getEntry(moduleName);
            } else {
                zipEntry = zipFile.getEntry(moduleName + ".es");
                if (zipEntry==null) {
                    zipEntry = zipFile.getEntry(moduleName + ".esw");
                }
                if (zipEntry==null) {
                    zipEntry = zipFile.getEntry(moduleName + ".js");
                }
            }
            if (zipEntry == null) return null; // Not found in this jar file
            byte buf[] = null;
            try {    
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                int limit = (int)zipEntry.getSize();
                buf = new byte[limit];
                
                int total = 0;
                while  (total < limit)
                {
                    int ct = inputStream.read(buf,total,limit-total);
                    total = total + ct;
                    if (ct == 0) {
                        throw new IOException ("Only " +
                            total + " bytes out of " + limit + " read from entry '" +
                            moduleName + "' in jar '" + zipFile.getName() +"'");
                    } 
                }
                inputStream.close();
            } catch (IOException e) {
                if (ESLoader.isDebugLoader()) System.out.println(" ** Error reading jar: " + e);
                return null;
            }

            EvaluationSource es = new JarEvaluationSource(dir.getPath(), moduleName, null);
            Reader r = new StringReader(new String(buf));
            ESValue theValue = evaluate(r, null, es, false); // no return on main file
            if (theValue == null) theValue = ESUndefined.theUndefined;
            return theValue;

        }
        return null;
    }



  /**
   * subevaluator - Evaluate a function node (inside a program evaluation)
   * @param node The AST node representing the list of statements
   * @param es The evaluation source information for backtracce
   * @param localVariableNames The set of local variable to create
   * @param thisObject The this of this evaluation (so to speak)
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
    public ESValue evaluateFunction(ASTStatementList node,
                          EvaluationSource es,
                          ESObject variableObject,
                          Vector localVariableNames,
                          ESObject thisObject) throws EcmaScriptException {
    ESValue theValue = ESUndefined.theUndefined;
    
    ESObject savedVariableObject = currentVariableObject;
    ESObject savedThisObject = currentThisObject;
    ScopeChain previousScopeChain = theScopeChain;

    currentVariableObject = variableObject;
    currentThisObject = thisObject;
    theScopeChain = new ScopeChain(globalObject, null); 
    theScopeChain = new ScopeChain(variableObject, theScopeChain);
   // EvaluationSource savedEvaluationSource = currentEvaluationSource;
   // currentEvaluationSource = es; 
    try {
        for (Enumeration e = localVariableNames.elements() ; e.hasMoreElements() ;) {

             String variable =(String)(e.nextElement()); 
             createVariable(variable, variable.hashCode());
         }    
         EcmaScriptEvaluateVisitor evaluationVisitor = new EcmaScriptEvaluateVisitor(this);
        theValue = evaluationVisitor.evaluateFunction(node, es);
        int cc = evaluationVisitor.getCompletionCode();
        if ((cc!= EcmaScriptEvaluateVisitor.C_NORMAL) &&
            (cc!= EcmaScriptEvaluateVisitor.C_RETURN)) {
            throw new EcmaScriptException("Unexpected " + 
                    evaluationVisitor.getCompletionCodeString() + 
                    " in function" );
        }
     } finally {
        currentVariableObject = savedVariableObject;
        theScopeChain = previousScopeChain;
        currentThisObject = savedThisObject;
       // currentEvaluationSource = savedEvaluationSource;
   }
        
    return theValue;
  }

  /**
   * Sub evaluator - evaluate a with node (inside a program evaluation)
   * @param node The with statement body
   * @param scopeObject the new scope for this with
   * @param es The evaluation source for back trace
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  public ESValue evaluateWith(ASTStatement node,
                          ESObject scopeObject,
                          EvaluationSource es) throws EcmaScriptException {
    ESValue theValue = ESUndefined.theUndefined;
    
    theScopeChain = new ScopeChain(scopeObject, theScopeChain); 
    try {
        EcmaScriptEvaluateVisitor evaluationVisitor = new EcmaScriptEvaluateVisitor(this);
        theValue = evaluationVisitor.evaluateWith(node, es);
   } finally {
        theScopeChain = theScopeChain.previousScope();
   }
        
    return theValue;
  }

    
    
  /**
   * Top level core evaluator (on parsed program),
   * Must be called from a function synchronized on the evaluator
   * @param program The parsed program information
   * @param thisObject The this of the evaluation (usually the global object)
   * @param acceptReturn If true accept a return statement in the body
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
   
  public ESValue evaluate(ParsedProgram program,
                             ESObject thisObject,
                             boolean acceptReturn) throws EcmaScriptException {
    ASTProgram node = program.getProgramNode();
    ESValue theValue = ESUndefined.theUndefined;

    ESObject savedVariableObject = currentVariableObject;
    ESObject savedThisObject = currentThisObject;
    ScopeChain previousScopeChain = theScopeChain;

    theScopeChain = new ScopeChain(globalObject, null); 
    currentVariableObject = globalObject;
    currentThisObject = (thisObject != null) ? thisObject : globalObject;

    EcmaScriptEvaluateVisitor evaluationVisitor = new EcmaScriptEvaluateVisitor(this);
    try {   

       functionDeclarationVisitor.processFunctionDeclarations(node, program.getEvaluationSource(), thisObject);
       Vector variables = program.getVariableNames();
       for (Enumeration e = variables.elements() ; e.hasMoreElements() ;) {
             String variable = (String)(e.nextElement());
             createVariable(variable,variable.hashCode());
       }
        theValue = evaluationVisitor.evaluateProgram(node, program.getEvaluationSource());
    } finally {
        currentVariableObject = savedVariableObject;
        theScopeChain = previousScopeChain;
        currentThisObject = savedThisObject;
    }
    int completionCode = evaluationVisitor.getCompletionCode();
    if (completionCode != EcmaScriptEvaluateVisitor.C_NORMAL) {

        if (completionCode != EcmaScriptEvaluateVisitor.C_RETURN) {
            throw new EcmaScriptException("Unexpected " + 
                    evaluationVisitor.getCompletionCodeString() + 
                    " in main program" );
        } else if (!acceptReturn) {
            throw new EcmaScriptException(
                "Return is not accepted in main program with the 'eval' interface" );
        }
    }
    return theValue;
  }
  
  
  
  /**
   * subevaluator - Evaluate an event function - must be synchronized
   * @param sourceObject The source of the event (wrapped)
   * @param theFunction The function to call
   * @param args The arguments of the event
   * @exception EmcaScriptException In case of any error during evaluation
   */
  synchronized public void evaluateEvent(
                          ESWrapper sourceObject,
                          ESObject theFunction,
                          Object [] args) throws EcmaScriptException {
        ESValue [] esArgs = new ESValue[args.length];
        for (int i=0; i<args.length; i++) {
           // esArgs[i] = new ESWrapper(args[i], this);
           esArgs[i] = ESLoader.normalizeValue(args[i], this);
        }
        theFunction.callFunction(sourceObject, esArgs);
  }


  /**
   * Top eval (synchronized) evaluate on identified stream
   * @param is Input stream to evaluate
   * @param thisObject The this of this evaluation
   * @param es the identification of the source for back trace
   * @param acceptReturn If true accepts return in main body
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
   
  synchronized public ESValue evaluate(java.io.Reader is,
                          ESObject thisObject, 
                          EvaluationSource es,
                          boolean acceptReturn) throws EcmaScriptException {
    ESValue theValue = ESUndefined.theUndefined;
    EcmaScript parser = new EcmaScript(is);
    ASTProgram programNode = null;
    try {
        programNode = (ASTProgram) parser.Program();
        if (debugParse) {
            System.out.println();
            System.out.println("@@ Dumping parse tree (debugParse true)");
            programNode.dump("");
        }

    } catch (ParseException e) {
        if (debugParse) {
            System.out.println("[[PARSING ERROR DETECTED: (debugParse true)]]");
            System.out.println(e.getMessage());
            System.out.println("[[BY ROUTINE:]]");
            e.printStackTrace();
            System.out.println();
        }
        throw new EcmaScriptParseException(e, es);
     } catch (TokenMgrError e) {
        if (debugParse) {
            System.out.println("[[LEXICAL ERROR DETECTED: (debugParse true)]]");
            System.out.println(e.getMessage());
            System.out.println("[[BY ROUTINE:]]");
            e.printStackTrace();
            System.out.println();
        }
        throw new EcmaScriptLexicalException(e, es);
     }
                    
     Vector variableList = varDeclarationVisitor.processVariableDeclarations(programNode, es);

     ParsedProgram program = new ParsedProgram(programNode, variableList, es);
    
     theValue = evaluate(program, thisObject, acceptReturn);

     return theValue;
  }

  /**
   * Top eval (synchronized) evaluate on anonymous stream
   * @param is Input stream to evaluate
   * @param thisObject The this of this evaluation
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  synchronized public ESValue evaluate(Reader is, ESObject thisObject) throws EcmaScriptException {
    EvaluationSource es = new UserEvaluationSource("<Anonymous stream>", null);
    return evaluate (is, thisObject, es, false); // no return on anonymous streams
  }
    
  /**
   * Top eval (synchronized) evaluate on anonymous stream with null thisObject
   * @param is Input stream to evaluate
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  synchronized public ESValue evaluate(Reader is) throws EcmaScriptException {
    return evaluate (is, null);
  }
  
  /**
   * Top eval (synchronized) evaluate on an identified string (used by the GUI)
   * @param text source to evaluate
   * @param source Identification of the source
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  synchronized public ESValue evaluate(String text, String source) throws EcmaScriptException {
    java.io.StringReader is = null;
    ESValue v = null;
    EvaluationSource es = new UserEvaluationSource(source, null);
    try {
       is = new java.io.StringReader(text);
       v = evaluate(is, globalObject, es, false);
    } finally {
       if (is != null) is.close();
    }
    return v;
  }
  
    

  /**
   * Top eval (synchronized) evaluate a file
   * @param file file to evaluate
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  synchronized public ESValue evaluate(File file) 
              throws EcmaScriptException, IOException {
      return evaluate(file, null);
  }
  
  /**
   * Top eval (synchronized) evaluate a file
   * @param file file to evaluate
   * @param thisObject The this to use for the evaluation
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  synchronized public ESValue evaluate(File file, ESObject thisObject) 
              throws EcmaScriptException, IOException {
    EvaluationSource es = new FileEvaluationSource(file.getPath(), null);

    FileReader fr = null;
    ESValue value = null;
    try {
      fr = new FileReader(file);
      value = evaluate (fr, thisObject, es, false); // No return on a main file
    } finally {
        if (fr!= null) {
            try {
                fr.close();
            } catch (IOException ignore) {
            }
        }
    }
    return value;
  }
  
  /**
   * Top eval (synchronized) evaluate on an anonymous string 
   * @param theSource source to evaluate
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  synchronized public ESValue evaluate(String theSource) throws EcmaScriptException {
       return evaluate(theSource, null, false);
   } 

  //synchronized public ESValue evaluate(String theSource, ESObject thisObject) throws EcmaScriptException {
  //   return evaluate(theSource, thisObject, false); // No return allowed
  //}
  /**
   * Top eval (synchronized) evaluate on an anonymous string with
   * thisObject and acceptBoolean
   * @param theSource source to evaluate
   * @param thisObject this for the evaluation
   * @param returnAccepted If true a return is accepted in the main body
   * @return The last value of the evaluation
   * @exception EmcaScriptException In case of any error during evaluation
   */
  synchronized public ESValue evaluate(String theSource, 
                                       ESObject thisObject, 
                                       boolean returnAccepted) 
                           throws EcmaScriptException {
    java.io.StringReader is = null;
    ESValue v = null;
    EvaluationSource es = new StringEvaluationSource(theSource, null);
    try {
       is = new java.io.StringReader(theSource);
       v = evaluate(is, thisObject, es, returnAccepted);
    } finally {
       if (is != null) is.close();
    }
    return v;
  }

}
