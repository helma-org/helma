// ORORegExp.java
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

import java.util.Vector;

import com.oroinc.text.regex.*;


/**
  * An EcmaScript RegExp  object based on OROInc pattern matcher.
  * May not coexist with the GNU regexp matcher.
  */
class ESORORegExp extends ESObject {
    
    private String regExpString;
    private boolean ignoreCase = false;
    private boolean global = false;
    private Pattern pattern = null; //  null means no valid pattern

    private int groups;
    private PatternCompiler compiler;
    private PatternMatcher matcher;
     
    static private final String IGNORECASEstring = ("ignoreCase").intern();
    static private final int IGNORECASEhash = IGNORECASEstring.hashCode();
    static private final String GLOBALstring = ("global").intern();
    static private final int GLOBALhash = GLOBALstring.hashCode();

    // Normal constructor       
    ESORORegExp(ESObject prototype, Evaluator evaluator, 
                    PatternCompiler compiler, PatternMatcher matcher,
                    String regExpString) {
        super(prototype, evaluator);
        this.compiler = compiler;
        this.matcher = matcher;
        this.regExpString = regExpString;
    }

    // Prototype constructor
    ESORORegExp(ESObject prototype, Evaluator evaluator, 
                    PatternCompiler compiler, PatternMatcher matcher) {
        super(prototype, evaluator);
        this.compiler = compiler;
        this.matcher = matcher;
        this.regExpString = "";
    }
    
    public Pattern getPattern() throws EcmaScriptException {
        
        if (pattern == null) {
            compile();
        }
        return pattern;
    }
    
    public boolean isGlobal() {
        return global;
    }
    
    public void compile() throws EcmaScriptException  {
       // Recompile the pattern 
       try {
          pattern = compiler.compile(regExpString, 
                      ignoreCase ? Perl5Compiler.CASE_INSENSITIVE_MASK : Perl5Compiler.DEFAULT_MASK);
       } catch(MalformedPatternException e) {
           throw new EcmaScriptException("MalformedPatternException: /" +
               regExpString + "/", e);
       }    
    }
    

    public String getESClassName() {
        return "RegExp";
    }
    
    public String toString() {
         if (regExpString==null) return "/<null>/";
         return "/"+regExpString+"/";
    }
    
    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" + 
            this.toString() + "]";
    }    

    public ESValue getPropertyInScope(String propertyName, ScopeChain previousScope, int hash) 
                throws EcmaScriptException {
        if (propertyName.equals(IGNORECASEstring)) {
            return ESBoolean.makeBoolean(ignoreCase);
        } else if (propertyName.equals(GLOBALstring)) {
            return ESBoolean.makeBoolean(global);
        }
        return super.getPropertyInScope(propertyName, previousScope, hash);
     }
        
     public ESValue getProperty(String propertyName, int hash) 
                                throws EcmaScriptException {
        if (propertyName.equals(IGNORECASEstring)) {
            return ESBoolean.makeBoolean(ignoreCase);
        } else if (propertyName.equals(GLOBALstring)) {
            return ESBoolean.makeBoolean(global);
        } else {
            return super.getProperty(propertyName, hash);
         }
     }
     
     public void putProperty(String propertyName, ESValue propertyValue, int hash) 
                                throws EcmaScriptException {
        if (hash==IGNORECASEhash && propertyName.equals(IGNORECASEstring)) {
            boolean oldIgnoreCase = ignoreCase;
            ignoreCase = (((ESPrimitive) propertyValue).booleanValue());
            if (oldIgnoreCase!=ignoreCase) pattern = null;  // force recompilation
        } else if (hash==GLOBALhash && propertyName.equals(GLOBALstring)) {
            global = (((ESPrimitive) propertyValue).booleanValue());
        } else {
            super.putProperty(propertyName, propertyValue, hash);
       }
    }   
    
    public String[] getSpecialPropertyNames() {
        String [] ns = {GLOBALstring, IGNORECASEstring};
        return ns;
    }
}


public class ORORegExp extends Extension {

    static private final String INDEXstring = ("index").intern();
    static private final int INDEXhash = INDEXstring.hashCode();
    static private final String INPUTstring = ("input").intern();
    static private final int INPUThash = INPUTstring.hashCode();
   
    private Evaluator evaluator = null;
    private ESObject esRegExpPrototype;
    private PatternCompiler compiler;
    private PatternMatcher matcher;

    class ESRegExpPrototypeTest extends BuiltinFunctionObject {
        ESRegExpPrototypeTest(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
            if (arguments.length<1) {
                  throw new EcmaScriptException("test requires 1 string argument");
            }
            ESORORegExp pattern = (ESORORegExp) thisObject;
            String str = arguments[0].toString();
            PatternMatcherInput input   = new PatternMatcherInput(str);
            return ESBoolean.makeBoolean(matcher.contains(input, pattern.getPattern()));
        }
    }
    
    class ESRegExpPrototypeExec extends BuiltinFunctionObject {
        ESRegExpPrototypeExec(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
            if (arguments.length<1) {
                  throw new EcmaScriptException("exec requires 1 string argument");
            }
            ESORORegExp pattern = (ESORORegExp) thisObject;
            String str = arguments[0].toString();
            PatternMatcherInput input = new PatternMatcherInput(str);
            if (matcher.contains(input, pattern.getPattern())) {
                MatchResult result = matcher.getMatch(); 
                int groups = result.groups();
                ESObject ap = this.evaluator.getArrayPrototype();
                ArrayPrototype resultArray = new ArrayPrototype(ap, this.evaluator);
                resultArray.setSize(groups);
                resultArray.putProperty(INDEXstring,
                                    new ESNumber(result.beginOffset(0)), INDEXhash);
                resultArray.putProperty(INPUTstring,
                                    new ESString(str), INPUThash);
                for (int i = 0; i<groups; i++) {
                    int beg = result.beginOffset(i);
                    int end = result.endOffset(i);
                    if (beg<0 || end < 0) {
                    	resultArray.setElementAt(new ESString(""),i);
                    } else {
                    	resultArray.setElementAt(new ESString(str.substring(beg,end)),i);
                    }
                }
                return resultArray;
            } else {
                return ESNull.theNull;
            }
        }
    }
    
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
                       
               ESORORegExp regExp = null;
               if (arguments.length==0) {
                   throw new EcmaScriptException("RegExp requires 1 or 2 arguments");
               } else if (arguments.length==1) {
                   regExp = new ESORORegExp(esRegExpPrototype, this.evaluator, compiler, matcher, arguments[0].toString());
               //} else if (arguments.length>1) {
               //    file = new ESORORegExp(esFilePrototype, this.evaluator, arguments[0].toString(),arguments[1].toString());
               }
               return regExp;
         }
    }
    

    public ORORegExp () {
        super();
    }
           
    public void initializeExtension(Evaluator evaluator) throws EcmaScriptException {
        
        // Create Perl5Compiler and Perl5Matcher instances.
        compiler = new Perl5Compiler();
        matcher  = new Perl5Matcher();

        this.evaluator = evaluator;
        GlobalObject go = evaluator.getGlobalObject();
        ObjectPrototype op = (ObjectPrototype) evaluator.getObjectPrototype();
        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();       
        esRegExpPrototype = new ESORORegExp(op, evaluator, compiler, matcher);
        
        ESObject globalObjectRegExp = 
           new GlobalObjectRegExp("RegExp", evaluator, fp); 
        
        globalObjectRegExp.putHiddenProperty("prototype",esRegExpPrototype);
        globalObjectRegExp.putHiddenProperty("length",new ESNumber(1));

        esRegExpPrototype.putHiddenProperty("constructor",globalObjectRegExp);
        esRegExpPrototype.putHiddenProperty("test", 
                   new ESRegExpPrototypeTest("test", evaluator, fp));
        esRegExpPrototype.putHiddenProperty("exec", 
                   new ESRegExpPrototypeExec("exec", evaluator, fp));
                   
        go.putHiddenProperty("RegExp", globalObjectRegExp);
        
        class StringPrototypeSearch extends BuiltinFunctionObject {
            StringPrototypeSearch(String name, Evaluator evaluator, FunctionPrototype fp) {
                super(fp, evaluator, name, 1);
            }
            public ESValue callFunction(ESObject thisObject, 
                                            ESValue[] arguments)
                   throws EcmaScriptException {
                if (arguments.length<1) {
                      throw new EcmaScriptException("search requires 1 pattern argument");
                }
                String str = thisObject.toString();
                PatternMatcherInput input = new PatternMatcherInput(str);
                ESORORegExp pattern;
                if (arguments[0] instanceof ESORORegExp) {
                    pattern = (ESORORegExp) arguments[0];
                } else {
                    throw new EcmaScriptException("The search argument must be a RegExp");
                }
                if(matcher.contains(input, pattern.getPattern())){
                      MatchResult result = matcher.getMatch();  
                      return new ESNumber(result.beginOffset(0)); 
                } else {
                    return new ESNumber(-1);
                }
            }
        }

        class StringPrototypeReplace extends BuiltinFunctionObject {
            StringPrototypeReplace(String name, Evaluator evaluator, FunctionPrototype fp) {
                super(fp, evaluator, name, 1);
            }
            public ESValue callFunction(ESObject thisObject, 
                                            ESValue[] arguments)
                   throws EcmaScriptException {
                if (arguments.length<2) {
                      throw new EcmaScriptException("replace requires 2 arguments: pattern and replacement string");
                }
                String str = thisObject.toString();
                ESORORegExp pattern;
                if (arguments[0] instanceof ESORORegExp) {
                    pattern = (ESORORegExp) arguments[0];
                } else {
                    throw new EcmaScriptException("The replace argument must be a RegExp");
                }
                String replacement = arguments[1].toString();
                // USE DEPRECATED ROUTINE BECAUSE I AM LAZY
                String result = Util.substitute(matcher, pattern.getPattern(), replacement, str, 
                        pattern.isGlobal() ? Util.SUBSTITUTE_ALL : 1, Util.INTERPOLATE_ALL);
                return new ESString(result);
            }
        }

        class StringPrototypeMatch extends BuiltinFunctionObject {
            StringPrototypeMatch(String name, Evaluator evaluator, FunctionPrototype fp) {
                super(fp, evaluator, name, 1);
            }
            public ESValue callFunction(ESObject thisObject, 
                                            ESValue[] arguments)
                   throws EcmaScriptException {
                if (arguments.length<1) {
                      throw new EcmaScriptException("match requires 1 pattern argument");
                }
                String str = thisObject.toString();
                ESORORegExp pattern;
                if (arguments[0] instanceof ESORORegExp) {
                    pattern = (ESORORegExp) arguments[0];
                } else {
                    throw new EcmaScriptException("The match argument must be a RegExp");
                }

                PatternMatcherInput input = new PatternMatcherInput(str);
                if (matcher.contains(input, pattern.getPattern())) {
                    MatchResult result = matcher.getMatch(); 
                    int groups = result.groups();
                    ESObject ap = this.evaluator.getArrayPrototype();
                    ArrayPrototype resultArray = new ArrayPrototype(ap, this.evaluator);
                    resultArray.setSize(groups);
                    resultArray.putProperty(INDEXstring,
                                        new ESNumber(result.beginOffset(0)), INDEXhash);
                    resultArray.putProperty(INPUTstring,
                                        new ESString(str), INPUThash);
                    for (int i = 0; i<groups; i++) {
                        int beg = result.beginOffset(i);
                        int end = result.endOffset(i);
                        resultArray.setElementAt(new ESString(str.substring(beg,end)),i);
                    }
                    return resultArray;
                } else {
                    return ESNull.theNull;
                }
            }
        }

        class StringPrototypeSplit extends BuiltinFunctionObject {
            StringPrototypeSplit(String name, Evaluator evaluator, FunctionPrototype fp) {
                super(fp, evaluator, name, 1);
            }
            public ESValue callFunction(ESObject thisObject, 
                                            ESValue[] arguments)
                   throws EcmaScriptException {
                String str = thisObject.toString();
                ESObject ap = this.evaluator.getArrayPrototype();
                ArrayPrototype theArray = new ArrayPrototype(ap, this.evaluator);
                if (arguments.length<=0) {
                    theArray.setSize(1);
                    theArray.setElementAt(thisObject, 0);
                } else {
                    if (arguments[0] instanceof ESORORegExp) {
                        ESORORegExp pattern = (ESORORegExp) arguments[0];
                        int n = Util.SPLIT_ALL;
                        if (arguments.length>1) {
                            n = arguments[1].toUInt32();
                        }
                        Vector result = Util.split(matcher, pattern.getPattern(), str, n);
                        int l = result.size();
                        theArray.setSize(l);
                        for (int i=0; i<l; i++) {
                            theArray.setElementAt( 
                                       new ESString((String)result.elementAt(i)), i);                                
                        }
                        
                    } else { // ! instanceof ESORORegExp
                        String sep = arguments[0].toString();
                        if (sep.length()==0) {
                            int l = str.length();
                            theArray.setSize(l);
                            for (int i=0; i<l; i++) {
                                theArray.setElementAt( 
                                           new ESString(str.substring(i,i+1)), i);                                
                            }
                        } else { 
                            int i = 0;
                            int start = 0;
                             while (start<str.length()) {
                                int pos = str.indexOf(sep, start);
                                if (pos<0) pos = str.length();
                                // System.out.println("start: " + start + ", pos: " + pos);
                                theArray.setSize(i+1);
                                theArray.setElementAt(
                                        new ESString(str.substring(start, pos)),i);
                                start = pos + sep.length();
                                i++;  
                            }                              
                        }
                    } //  instanceof ESORORegExp
                }
                return theArray;
            }
        }

        ESObject stringPrototype = evaluator.getStringPrototype();
        stringPrototype.putHiddenProperty("search",
               new StringPrototypeSearch("search", evaluator, fp));
        stringPrototype.putHiddenProperty("replace",
               new StringPrototypeReplace("replace", evaluator, fp));
        stringPrototype.putHiddenProperty("match",
               new StringPrototypeMatch("match", evaluator, fp));
        stringPrototype.putHiddenProperty("split",
               new StringPrototypeSplit("split", evaluator, fp));

     }
 }
 
 