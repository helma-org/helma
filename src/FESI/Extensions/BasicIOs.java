// BasicIOs.java
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
import FESI.Data.*;
import FESI.Interpreter.*;
import FESI.Exceptions.*;
import FESI.gui.*;
import FESI.swinggui.*;

import java.io.*;



/**
  * Swing based basic IO for FESI - See BasicIO
  */
public class BasicIOs extends Extension implements BasicIOInterface {
    
    class GlobalObjectWrite extends BuiltinFunctionObject {
        GlobalObjectWrite(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
           for (int i = 0; i<arguments.length; i++) { 
               System.out.print(arguments[i].toString());
           }
           return ESUndefined.theUndefined;
        }
    }
    
    class GlobalObjectWriteln extends BuiltinFunctionObject {
        GlobalObjectWriteln(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
           for (int i = 0; i<arguments.length; i++) { 
               System.out.print(arguments[i].toString());
           }
           System.out.println();
           return ESUndefined.theUndefined;
        }
    }
    
    class GlobalObjectAlert extends BuiltinFunctionObject {
        GlobalObjectAlert(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
           StringBuffer sb = new StringBuffer();
           for (int i = 0; i<arguments.length; i++) { 
               sb.append(arguments[i].toString());
           }
           MessageBox mb = new SwingMessageBox("EcmaScript Alert",sb.toString());
           mb.waitOK();
           return ESUndefined.theUndefined;
        }
    }
    
    class GlobalObjectPrompt extends BuiltinFunctionObject {
        GlobalObjectPrompt(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
           String prompt = "";
           String defaultResponse = "";
           if (arguments.length>0) prompt = arguments[0].toString();
           if (arguments.length>1) defaultResponse = arguments[1].toString();
           PromptBox pb = new SwingPromptBox("EcmaScript promt",prompt, defaultResponse);
           return new ESString(pb.waitResponse());
        }
    }
    class GlobalObjectConfirm extends BuiltinFunctionObject {
        GlobalObjectConfirm(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
           StringBuffer sb = new StringBuffer();
           for (int i = 0; i<arguments.length; i++) { 
               sb.append(arguments[i].toString());
           }
           ConfirmationBox mb = new SwingConfirmationBox("EcmaScript Confirm",sb.toString());
           boolean response = mb.waitYesOrNo();
           return ESBoolean.makeBoolean(response);
        }
    }

    class GlobalObjectExit extends BuiltinFunctionObject {
        GlobalObjectExit(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
           int status = 0;
           if (arguments.length>0) {
               status=arguments[0].toInt32();
           }
               
           System.exit(status);
           return null; // Never reached
        }
    }

    class GlobalObjectNoop extends BuiltinFunctionObject {
        GlobalObjectNoop(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 0); // 0 = Just some default value
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
               
           return ESUndefined.theUndefined; 
        }
    }

    class GlobalObjectLoad extends BuiltinFunctionObject {
        GlobalObjectLoad(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1); // 0 = Just some default value
        }
        public ESValue callFunction(ESObject thisObject, 
                                        ESValue[] arguments)
               throws EcmaScriptException {
           String fileName = null;
           if (arguments.length>0) fileName = arguments[0].toString();
           if (fileName == null) throw new EcmaScriptException("Missing file name for load");
           File file = new File(fileName);
           ESValue value; 
           try {
               //value = this.evaluator.evaluateLoadFile(file);
               value = this.evaluator.evaluateLoadModule(fileName);
           } catch (EcmaScriptParseException e) {
               e.setNeverIncomplete();
               throw e;
           }
           return value;
           
        }
    }
 
    private Evaluator evaluator = null;
    private ESObject document = null;
    private ESObject window = null;
    
    public BasicIOs () {
        super();
    }
    
    // implements BasicIOInterface
    public ESObject getDocument() { return document; }
        
    public void initializeExtension(Evaluator evaluator) throws EcmaScriptException {
        
        this.evaluator = evaluator;
        
        GlobalObject go = evaluator.getGlobalObject();
        document = ObjectObject.createObject(evaluator);
        window = ObjectObject.createObject(evaluator);
        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
        
        go.putHiddenProperty("document", document);
        document.putHiddenProperty("write", 
                   new GlobalObjectWrite("write", evaluator, fp));
        document.putHiddenProperty("writeln", 
                   new GlobalObjectWriteln("writeln", evaluator, fp));
        document.putHiddenProperty("open", 
                   new GlobalObjectNoop("open", evaluator, fp));
        document.putHiddenProperty("close", 
                   new GlobalObjectNoop("close", evaluator, fp));
        document.putHiddenProperty("URL", new ESString("file://<unknown>"));
    
        go.putHiddenProperty("window", window);
        window.putHiddenProperty("alert", 
                   new GlobalObjectAlert("alert", evaluator, fp));
        window.putHiddenProperty("prompt", 
                   new GlobalObjectPrompt("prompt", evaluator, fp));
        window.putHiddenProperty("confirm", 
                   new GlobalObjectConfirm("confirm", evaluator, fp));

        go.putHiddenProperty("write", 
                   new GlobalObjectWrite("write", evaluator, fp));
        go.putHiddenProperty("writeln", 
                   new GlobalObjectWriteln("writeln", evaluator, fp));
        go.putHiddenProperty("alert", 
                   new GlobalObjectAlert("alert", evaluator, fp));
        go.putHiddenProperty("prompt", 
                   new GlobalObjectPrompt("prompt", evaluator, fp));
        go.putHiddenProperty("confirm", 
                   new GlobalObjectConfirm("confirm", evaluator, fp));
        go.putHiddenProperty("exit", 
                   new GlobalObjectExit("exit", evaluator, fp));
        go.putHiddenProperty("load", 
                   new GlobalObjectLoad("load", evaluator, fp));
     }
 }
 
 