package helma.scripting.fesi.extensions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;

import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Extensions.*;
import FESI.Interpreter.*;

import helma.framework.core.Application;
import helma.framework.core.RequestEvaluator;
import helma.objectmodel.INode;
import helma.objectmodel.db.Node;
import helma.objectmodel.dom.*;
import helma.scripting.fesi.ESNode;

public class DomExtension extends Extension  {

    private transient Evaluator evaluator = null;

    public DomExtension() {
        super();
    }

    public void initializeExtension(Evaluator evaluator) throws EcmaScriptException {
        this.evaluator = evaluator;
        GlobalObject go = evaluator.getGlobalObject();
        ObjectPrototype op = (ObjectPrototype) evaluator.getObjectPrototype();
        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();

        ESObject globalXml = new GlobalObjectXml("Xml", evaluator, fp);
        globalXml.putHiddenProperty ("length",new ESNumber(1));
        globalXml.putHiddenProperty ("load", new XmlLoad ("load", evaluator, fp));
        globalXml.putHiddenProperty ("save", new XmlSave ("save", evaluator, fp));
        globalXml.putHiddenProperty ("create", new XmlCreate ("create", evaluator, fp));
        globalXml.putHiddenProperty ("get", new XmlGet ("get", evaluator, fp));
        go.putHiddenProperty ("Xml", globalXml);
	}

	class GlobalObjectXml extends BuiltinFunctionObject {
		GlobalObjectXml(String name, Evaluator evaluator, FunctionPrototype fp) {
			super(fp, evaluator, name, 1);
		}
		public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			return doConstruct(thisObject, arguments);
		}
		public ESObject doConstruct(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException	{
			throw new EcmaScriptException("Xml can't be instanced");
		}
	}

    class XmlSave extends BuiltinFunctionObject {
		XmlSave(String name, Evaluator evaluator, FunctionPrototype fp) {
			super(fp, evaluator, name, 1);
		}
		public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			if ( arguments==null || arguments.length<2 )
				throw new EcmaScriptException("not enough arguments");
			INode node = null;
			try	{
				node = ((ESNode)arguments[1]).getNode();
			}	catch ( Exception e )	{
				// we definitly need a node
				throw new EcmaScriptException("argument is not an hopobject");
			}
			try	{
				File tmpFile = new File(arguments[0].toString()+".tmp."+XmlWriter.generateID());
				XmlWriter writer = new XmlWriter (tmpFile);
				boolean result = writer.write(node);
				writer.close();
				File finalFile = new File(arguments[0].toString());
				tmpFile.renameTo (finalFile);
				this.evaluator.reval.app.logEvent("wrote xml to " + finalFile.getAbsolutePath() );
			}	catch (IOException io)	{
				throw new EcmaScriptException (io.toString());
			}
			return ESBoolean.makeBoolean(true);
        }
    }

    class XmlCreate extends BuiltinFunctionObject {
		XmlCreate(String name, Evaluator evaluator, FunctionPrototype fp) {
			super(fp, evaluator, name, 1);
		}
		public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			if ( arguments==null || arguments.length==0 )
				throw new EcmaScriptException("not enough arguments");
			INode node = null;
			try	{
				node = ((ESNode)arguments[0]).getNode();
			}	catch ( Exception e )	{
				// we definitly need a node
				throw new EcmaScriptException("argument is not an hopobject");
			}
			try	{
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				XmlWriter writer = new XmlWriter (out);
				boolean result = writer.write(node);
				writer.flush();
				return new ESString (out.toString());
			}	catch (IOException io)	{
				throw new EcmaScriptException (io.toString());
			}
        }
    }

    class XmlLoad extends BuiltinFunctionObject {
        XmlLoad(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			if ( arguments==null || arguments.length==0 )
				throw new EcmaScriptException("no arguments for Xml.load()");
			INode node = null;
			try	{
				node = ((ESNode)arguments[1]).getNode();
			}	catch ( Exception e )	{	//classcast, arrayindex etc
				// make sure we have a node, even if 2nd arg doesn't exist or is not a node
				node = new Node ( (String)null, (String)null, this.evaluator.reval.app.getWrappedNodeManager() );
			}
			try	{
				XmlReader reader = new XmlReader ();
				INode result = reader.read (arguments[0].toString(),node);
				return this.evaluator.reval.getNodeWrapper (result);
			}	catch ( NoClassDefFoundError e )	{
				throw new EcmaScriptException ("Can't load dom-capable xml parser.");
			}	catch ( RuntimeException f )	{
				throw new EcmaScriptException (f.toString());
			}
        }
    }

    class XmlGet extends BuiltinFunctionObject {
        XmlGet(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			if ( arguments==null || arguments.length==0 )
				throw new EcmaScriptException("Xml.get() needs a location as an argument");
			try	{
				XmlConverter converter;
				if ( arguments.length>1 )	{
					converter = new XmlConverter (arguments[1].toString());
				}	else	{
					converter = new XmlConverter ();
				}
				INode node = new helma.objectmodel.db.Node ( (String)null, (String)null, this.evaluator.reval.app.getWrappedNodeManager() );
				INode result = converter.convert (arguments[0].toString(),node);
				return this.evaluator.reval.getNodeWrapper(result);
			}	catch ( NoClassDefFoundError e )	{
				throw new EcmaScriptException("Can't load dom-capable xml parser.");
			}	catch ( RuntimeException f )	{
				throw new EcmaScriptException(f.toString());
			}
        }
    }

}


