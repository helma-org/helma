// MathObject.java
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

package FESI.Data;

import FESI.Exceptions.*;
import FESI.Interpreter.*;

/**
 * Implemements the EcmaScript Math singleton.
 */
public class MathObject extends ObjectPrototype {
        
    private FunctionPrototype fp;
    
    private MathObject(ESObject prototype, Evaluator evaluator, FunctionPrototype functionPrototype) 
                                throws EcmaScriptException {
        super(prototype, evaluator);
      
        // Initialization used to be in makeMathObject, but this caused
        // some problemsto the users of JBuilder. So it is moved in
        // the constructor
      
        putHiddenProperty("E", new ESNumber(Math.E));
        putHiddenProperty("LN10", new ESNumber(Math.log(10.0d)));
        putHiddenProperty("LN2", new ESNumber(Math.log(2.0d)));
        putHiddenProperty("LOG2E", new ESNumber(1.0d/Math.log(2.0d)));
        putHiddenProperty("LOG10E", new ESNumber(1.0d/Math.log(10.0d)));
        putHiddenProperty("PI", new ESNumber(Math.PI));
        putHiddenProperty("SQRT1_2", new ESNumber(1.0d/Math.sqrt(2.0d)));
        putHiddenProperty("SQRT2", new ESNumber(Math.sqrt(2.0d)));
        
        putHiddenProperty("abs", 
           new BuiltinMathFunctionOne("abs", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.abs(arg);
                }
            }
        );
        putHiddenProperty("acos", 
           new BuiltinMathFunctionOne("acos", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.acos(arg);
                }
            }
        );
        putHiddenProperty("asin", 
           new BuiltinMathFunctionOne("asin", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.asin(arg);
                }
            }
        );
        putHiddenProperty("atan", 
           new BuiltinMathFunctionOne("atan", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.atan(arg);
                }
            }
        );
        putHiddenProperty("atan2", 
           new BuiltinMathFunctionTwo("atan2", evaluator, functionPrototype) {
                public double applyMathFunction(double arg1, double arg2) {
                   return Math.atan2(arg1,arg2);
                }
            }
        );
        putHiddenProperty("ceil", 
           new BuiltinMathFunctionOne("ceil", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.ceil(arg);
                }
            }
        );
        putHiddenProperty("cos", 
           new BuiltinMathFunctionOne("cos", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.cos(arg);
                }
            }
        );
        putHiddenProperty("exp", 
           new BuiltinMathFunctionOne("exp", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.exp(arg);
                }
            }
        );
        putHiddenProperty("floor", 
           new BuiltinMathFunctionOne("floor", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.floor(arg);
                }
            }
        );
        putHiddenProperty("log", 
           new BuiltinMathFunctionOne("log", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.log(arg);
                }
            }
        );
        putHiddenProperty("max", 
           new BuiltinMathFunctionTwo("max", evaluator, functionPrototype) {
                public double applyMathFunction(double arg1, double arg2) {
                   return Math.max(arg1,arg2);
                }
            }
        );
        putHiddenProperty("min", 
           new BuiltinMathFunctionTwo("min", evaluator, functionPrototype) {
                public double applyMathFunction(double arg1, double arg2) {
                   return Math.min(arg1,arg2);
                }
            }
        );
        putHiddenProperty("pow", 
           new BuiltinMathFunctionTwo("pow", evaluator, functionPrototype) {
                public double applyMathFunction(double arg1, double arg2) {
                   double d = Double.NaN;
                   try {
                        d = Math.pow(arg1,arg2);
                    } catch (ArithmeticException e) {
                        // return NaN
                    }
                   return d;
                }
            }
        );
        putHiddenProperty("random", 
           new BuiltinMathFunctionZero("random", evaluator, functionPrototype) {
                public double applyMathFunction() {
                   return Math.random();
                }
            }
        );
        putHiddenProperty("round", 
           new BuiltinMathFunctionOne("round", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.round(arg);
                }
            }
        );
        putHiddenProperty("sin", 
           new BuiltinMathFunctionOne("sin", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.sin(arg);
                }
            }
        );
        putHiddenProperty("sqrt", 
           new BuiltinMathFunctionOne("sqrt", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.sqrt(arg);
                }
            }
        );
        putHiddenProperty("tan", 
           new BuiltinMathFunctionOne("tan", evaluator, functionPrototype) {
                public double applyMathFunction(double arg) {
                   return Math.tan(arg);
                }
            }
        );
    }
    
    // overrides
    public String getESClassName() {
        return "Math";
    }
    
    // class of nilary functions    
    abstract class BuiltinMathFunctionZero extends BuiltinFunctionObject {
        BuiltinMathFunctionZero(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 0);
        }
        abstract double applyMathFunction();
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
               throws EcmaScriptException { 
           return new ESNumber(applyMathFunction());
        }
    }
    
    // class of unary functions
    abstract class BuiltinMathFunctionOne extends BuiltinFunctionObject {
        BuiltinMathFunctionOne(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        abstract double applyMathFunction(double arg);
        public ESValue callFunction(ESObject thisObject, 
                            ESValue[] arguments)
               throws EcmaScriptException { 
           double arg = (arguments.length>0) ? 
                               arguments[0].doubleValue() : 
                               Double.NaN;
           if (Double.isNaN(arg)) {
               return new ESNumber(Double.NaN);
           }
           return new ESNumber(applyMathFunction(arg));
        }
    }
    
    // class of dyadic functions
    abstract class BuiltinMathFunctionTwo extends BuiltinFunctionObject {
        BuiltinMathFunctionTwo(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 2);
        }
        abstract double applyMathFunction(double arg1, double arg2);
        public ESValue callFunction(ESObject thisObject, 
                                ESValue[] arguments)
               throws EcmaScriptException { 
           if (arguments.length<2) {
               throw new EcmaScriptException("Missing parameter in function " + this);
           }
           double arg1 = (arguments.length>0) ? 
                               arguments[0].doubleValue() : 
                               Double.NaN;
           double arg2 = (arguments.length>01) ? 
                               arguments[1].doubleValue() : 
                               Double.NaN;
           if (Double.isNaN(arg1) || Double.isNaN(arg2)) {
               return new ESNumber(Double.NaN);
           }
           return new ESNumber(applyMathFunction(arg1, arg2));
        }
    }

    /**
     * Utility function to create the Math single object
     *
     * @param evaluator the Evaluator
     * @param objectPrototype The Object prototype attached to the evaluator
     * @param functionPrototype The Function prototype attached to the evaluator
     *
     * @return the Math singleton
     */
    static public ESObject makeMathObject (Evaluator evaluator, 
                                    ObjectPrototype prototype, 
                                    FunctionPrototype functionPrototype) {
        try {
          MathObject mo = new MathObject(prototype, evaluator, functionPrototype);
          return mo;
        } catch (EcmaScriptException e) {
            e.printStackTrace();
            throw new ProgrammingError(e.getMessage());
        }
    }
}