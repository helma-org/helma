package helma.scripting.fesi.extensions;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
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
        globalXml.putHiddenProperty ("read", new XmlRead ("read", evaluator, fp, false));
        globalXml.putHiddenProperty ("readFromString", new XmlRead ("readFromString", evaluator, fp, true));
        globalXml.putHiddenProperty ("write", new XmlWrite ("write", evaluator, fp));
        globalXml.putHiddenProperty ("writeToString", new XmlWriteToString ("writeToString", evaluator, fp));
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

	class XmlWrite extends BuiltinFunctionObject {
		XmlWrite(String name, Evaluator evaluator, FunctionPrototype fp) {
			super(fp, evaluator, name, 1);
		}
		public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			if ( arguments==null || arguments.length != 2 )
				throw new EcmaScriptException("Wrong number of arguments in Xml.write()");
			INode node = null;
			try	{
				node = ((ESNode)arguments[0]).getNode();
			}	catch ( Exception e )	{
				// we definitly need a node
				throw new EcmaScriptException("First argument in Xml.write() is not an hopobject");
			}
			try	{
				File tmpFile = new File(arguments[1].toString()+".tmp."+XmlWriter.generateID());
				XmlWriter writer = new XmlWriter (tmpFile, "UTF-8");
				writer.setDatabaseMode(false);
				boolean result = writer.write(node);
				writer.close();
				File finalFile = new File(arguments[1].toString());
				tmpFile.renameTo (finalFile);
				this.evaluator.reval.app.logEvent("wrote xml to " + finalFile.getAbsolutePath() );
			}	catch (IOException io)	{
				throw new EcmaScriptException (io.toString());
			}
			return ESBoolean.makeBoolean(true);
        }
    }

	/**
	  * Xml.create() is used to get a string containing the xml-content.
	  * Useful if Xml-content should be made public through the web.
	  */
    class XmlWriteToString extends BuiltinFunctionObject {
		XmlWriteToString(String name, Evaluator evaluator, FunctionPrototype fp) {
			super(fp, evaluator, name, 1);
		}
		public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			if ( arguments==null || arguments.length==0 )
				throw new EcmaScriptException("Not enough arguments in Xml.writeToString()");
			INode node = null;
			try	{
				node = ((ESNode)arguments[0]).getNode();
			}	catch ( Exception e )	{
				// we definitly need a node
				throw new EcmaScriptException("argument is not an hopobject");
			}
			try	{
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				XmlWriter writer = new XmlWriter (out, "UTF-8");
				//  in case we ever want to limit serialization depth...
				// if (arguments.length > 1 && arguments[1] instanceof ESNumber)
				// 	writer.setMaxLevels(arguments[1].toInt32());
				writer.setDatabaseMode(false);
				boolean result = writer.write(node);
				writer.flush();
				return new ESString (out.toString("UTF-8"));
			}	catch (IOException io)	{
				throw new EcmaScriptException (io.toString());
			}
        }
    }

    class XmlRead extends BuiltinFunctionObject {
        boolean fromstring;
        XmlRead(String name, Evaluator evaluator, FunctionPrototype fp, boolean fromstring) {
            super(fp, evaluator, name, 1);
	    this.fromstring = fromstring;
        }
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
			if ( arguments==null || arguments.length==0 )
				throw new EcmaScriptException("Missing arguments in Xml.read()");
			INode node = null;
			try	{
				node = ((ESNode)arguments[1]).getNode();
			}	catch ( Exception e )	{	//classcast, arrayindex etc
				// make sure we have a node, even if 2nd arg doesn't exist or is not a node
				node = new Node ( (String)null, (String)null, this.evaluator.reval.app.getWrappedNodeManager() );
			}
			try	{
				XmlReader reader = new XmlReader ();
				INode result = null;
				if (fromstring)
					result = reader.read (new StringReader (arguments[0].toString()),node);
				else
					result = reader.read (new File(arguments[0].toString()),node);
				return this.evaluator.reval.getNodeWrapper (result);
			}	catch ( NoClassDefFoundError e )	{
				throw new EcmaScriptException ("Can't load XML parser:"+e);
			}	catch ( Exception f )	{
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


