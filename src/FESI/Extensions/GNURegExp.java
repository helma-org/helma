// GNURegExp.java
// FESI Copyright (c) Jean-Marc Lugrin, 1999
// this file (c) mike dillon, 1999
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

import gnu.regexp.*;

/**
  * An EcmaScript RegExp  object based on GNU pattern matcher.
  * May not coexist with the ORO regexp matcher.
  */
class ESGNURegExp extends ESObject
{
	private String regExpString;
	private boolean ignoreCase = false;
	private boolean global = false;
	private RE pattern = null; //  null means no valid pattern

	private int groups;

	static private final String IGNORECASEstring = "ignoreCase";
	static private final int IGNORECASEhash = IGNORECASEstring.hashCode();
	static private final String GLOBALstring = "global";
	static private final int GLOBALhash = GLOBALstring.hashCode();

	// Normal constructor
	ESGNURegExp(ESObject prototype, Evaluator evaluator, String regExpString)
	{
		super(prototype, evaluator);
		this.regExpString = regExpString;
	}

	// Prototype constructor
	ESGNURegExp(ESObject prototype, Evaluator evaluator)
	{
		super(prototype, evaluator);
		this.regExpString = "";
	}

	public RE getPattern() throws EcmaScriptException
	{
		if (pattern == null)
		{
			compile();
		}
		return pattern;
	}

	public boolean isGlobal()
	{
		return global;
	}

	public void compile() throws EcmaScriptException
	{
		try
		{
			pattern = new RE(regExpString,
				(ignoreCase ? RE.REG_ICASE : 0) | RE.REG_MULTILINE,
				RESyntax.RE_SYNTAX_PERL5);
                }
                catch(REException e)
                {
			throw new EcmaScriptException(this.toString(), e);
		}
	}

	public String getESClassName()
	{
		return "RegExp";
	}

	public String toString()
	{
		return "/" + ((regExpString == null) ? "<null>" : regExpString)
			+ "/";
	}

	public String toDetailString()
	{
		return "ES:[Object: builtin " + this.getClass().getName() + ":"
			+ this.toString() + "]";
	}

	public ESValue getPropertyInScope(String propertyName,
		ScopeChain previousScope, int hash) throws EcmaScriptException
	{
		if (IGNORECASEstring.equals(propertyName))
		{
			return ESBoolean.makeBoolean(ignoreCase);
		}
		else if (GLOBALstring.equals(propertyName))
		{
			return ESBoolean.makeBoolean(global);
		}
		else
		{
			return super.getPropertyInScope(propertyName,
				previousScope, hash);
		}
	}

	public ESValue getProperty(String propertyName, int hash)
		throws EcmaScriptException
	{
		if (IGNORECASEstring.equals(propertyName))
		{
			return ESBoolean.makeBoolean(ignoreCase);
		}
		else if (GLOBALstring.equals(propertyName))
		{
			return ESBoolean.makeBoolean(global);
		}
		else
		{
			return super.getProperty(propertyName, hash);
		}
	}

	public void putProperty(String propertyName, ESValue propertyValue,
		int hash) throws EcmaScriptException
	{
		if (hash == IGNORECASEhash &&
			IGNORECASEstring.equals(propertyName))
		{
			boolean oldIgnoreCase = ignoreCase;
			ignoreCase = (((ESPrimitive) propertyValue).booleanValue());
			if (oldIgnoreCase != ignoreCase)
				pattern = null; // force recompilation
		}
		else if (hash == GLOBALhash && GLOBALstring.equals(propertyName))
		{
			global = (((ESPrimitive) propertyValue).booleanValue());
		}
		else
		{
			super.putProperty(propertyName, propertyValue, hash);
		}
	}

	public String[] getSpecialPropertyNames()
	{
		String [] ns = { GLOBALstring, IGNORECASEstring };
		return ns;
	}
}

public class GNURegExp extends Extension
{
	static private final String INDEXstring = "index";
	static private final int INDEXhash = INDEXstring.hashCode();
	static private final String INPUTstring = "input";
	static private final int INPUThash = INPUTstring.hashCode();

	private Evaluator evaluator = null;
	private ESObject esRegExpPrototype;

	class ESRegExpPrototypeTest extends BuiltinFunctionObject
	{
		ESRegExpPrototypeTest(String name, Evaluator evaluator,
			FunctionPrototype fp)
		{
			super(fp, evaluator, name, 1);
		}

		public ESValue callFunction(ESObject thisObject,
			ESValue[] arguments) throws EcmaScriptException
		{
			if (arguments.length < 1)
			{
				throw new EcmaScriptException(
					"test requires 1 string argument");
			}
			RE pattern = ((ESGNURegExp) thisObject).getPattern();
			boolean contains = pattern.getMatch(arguments[0].toString())
				!= null;
			return ESBoolean.makeBoolean(contains);
		}
	}

	class ESRegExpPrototypeExec extends BuiltinFunctionObject
	{
		ESRegExpPrototypeExec(String name, Evaluator evaluator,
			FunctionPrototype fp)
		{
			super(fp, evaluator, name, 1);
		}

		public ESValue callFunction(ESObject thisObject,
			ESValue[] arguments) throws EcmaScriptException
		{
			if (arguments.length < 1)
			{
				throw new EcmaScriptException(
					"exec requires 1 string argument");
			}
			RE pattern = ((ESGNURegExp) thisObject).getPattern();
			String str = arguments[0].toString();
			REMatch match = pattern.getMatch(str);
			if (match != null)
			{
				int groups = pattern.getNumSubs() + 1;
				ESObject ap = this.evaluator.getArrayPrototype();
				ArrayPrototype resultArray = new ArrayPrototype(ap,
					this.evaluator);
				resultArray.setSize(groups);
				resultArray.putProperty(INDEXstring, new ESNumber(
					match.getStartIndex()), INDEXhash);
				resultArray.putProperty(INPUTstring, new ESString(str), INPUThash);
				for (int i = 0; i < groups; i++)
				{
					String sub = match.toString(i);
					resultArray.setElementAt(new ESString(
						(sub == null) ? "" : sub), i);
				}
				return resultArray;
			}
			else
			{
				return ESNull.theNull;
			}
		}
	}

	class GlobalObjectRegExp extends BuiltinFunctionObject
	{
		GlobalObjectRegExp(String name, Evaluator evaluator,
			FunctionPrototype fp)
		{
			super(fp, evaluator, name, 1);
		}

		public ESValue callFunction(ESObject thisObject,
			ESValue[] arguments) throws EcmaScriptException
		{
			return doConstruct(thisObject, arguments);
		}

		public ESObject doConstruct(ESObject thisObject,
			ESValue[] arguments) throws EcmaScriptException
		{

			ESGNURegExp regExp = null;
			if (arguments.length == 0)
			{
				throw new EcmaScriptException(
					"GNURegExp requires 1 argument");
			}
			else if (arguments.length == 1)
			{
				regExp = new ESGNURegExp(esRegExpPrototype,
					this.evaluator, arguments[0].toString());
			}
			return regExp;
		}
	}

	public void initializeExtension(Evaluator evaluator)
		throws EcmaScriptException
	{
		this.evaluator = evaluator;
		GlobalObject go = evaluator.getGlobalObject();
		ObjectPrototype op = (ObjectPrototype) evaluator.getObjectPrototype();
		FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
		esRegExpPrototype = new ESGNURegExp(op, evaluator);

		ESObject globalObjectRegExp = new GlobalObjectRegExp("RegExp", evaluator, fp);

		globalObjectRegExp.putHiddenProperty("prototype",esRegExpPrototype);
		globalObjectRegExp.putHiddenProperty("length",new ESNumber(1));

		esRegExpPrototype.putHiddenProperty("constructor",globalObjectRegExp);
		esRegExpPrototype.putHiddenProperty("test",
			new ESRegExpPrototypeTest("test", evaluator, fp));
		esRegExpPrototype.putHiddenProperty("exec",
			new ESRegExpPrototypeExec("exec", evaluator, fp));

		go.putHiddenProperty("RegExp", globalObjectRegExp);

		class StringPrototypeSearch extends BuiltinFunctionObject
		{
			StringPrototypeSearch(String name, Evaluator evaluator,
				FunctionPrototype fp)
			{
				super(fp, evaluator, name, 1);
			}

			public ESValue callFunction(ESObject thisObject,
				ESValue[] arguments) throws EcmaScriptException
			{
				if (arguments.length < 1)
				{
					throw new EcmaScriptException(
						"search requires 1 pattern argument");
				}
				String str = thisObject.toString();
				ESGNURegExp pattern;
				if (arguments[0] instanceof ESGNURegExp)
				{
					pattern = (ESGNURegExp) arguments[0];
				}
				else
				{
					throw new EcmaScriptException("The search argument must be a GNURegExp");
				}
				RE re = pattern.getPattern();
				REMatch match = re.getMatch(str);
				int matchStart = (match == null) ? -1 : match
					.getStartIndex();
				return new ESNumber(matchStart);
			}
		}

		class StringPrototypeReplace extends BuiltinFunctionObject
		{
			StringPrototypeReplace(String name, Evaluator evaluator,
				FunctionPrototype fp)
			{
				super(fp, evaluator, name, 1);
			}

			public ESValue callFunction(ESObject thisObject,
				ESValue[] arguments) throws EcmaScriptException
			{
				if (arguments.length < 2)
				{
					throw new EcmaScriptException(
						"replace requires 2 arguments: pattern and replacement string");
				}
				String str = thisObject.toString();
				ESGNURegExp pattern;
				if (arguments[0] instanceof ESGNURegExp)
				{
					pattern = (ESGNURegExp) arguments[0];
				}
				else
				{
					throw new EcmaScriptException(
						"The replace argument must be a GNURegExp");
				}
				String replacement = arguments[1].toString();
				RE re = pattern.getPattern();

				String result = null;
				if (pattern.isGlobal())
				{
					result = re.substituteAll(str,
						replacement);
				}
				else
				{
					result = re.substitute(str,
						replacement);
				}
				return new ESString(result);
			}
		}

		class StringPrototypeMatch extends BuiltinFunctionObject
		{
			StringPrototypeMatch(String name, Evaluator evaluator,
				FunctionPrototype fp)
			{
				super(fp, evaluator, name, 1);
			}

			public ESValue callFunction(ESObject thisObject,
				ESValue[] arguments) throws EcmaScriptException
			{
				if (arguments.length < 1)
				{
					throw new EcmaScriptException(
						"match requires 1 pattern argument");
				}
				String str = thisObject.toString();
				ESGNURegExp pattern;
				if (arguments[0] instanceof ESGNURegExp)
				{
					pattern = (ESGNURegExp) arguments[0];
				}
				else
				{
					throw new EcmaScriptException(
						"The match argument must be a GNURegExp");
				}

				RE re = pattern.getPattern();
				REMatch match = re.getMatch(str);

				if (match != null)
				{
					// Group count is one more than the
					// subexpression count
					int groups = re.getNumSubs() + 1;
					ESObject ap = this.evaluator
						.getArrayPrototype();
					ArrayPrototype resultArray = new
						ArrayPrototype(ap, this.evaluator);
					resultArray.setSize(groups);
					resultArray.putProperty(INDEXstring,
						new ESNumber(match.getStartIndex()),
						INDEXhash);
					resultArray.putProperty(INPUTstring,
						new ESString(str), INPUThash);
					for (int i = 0; i < groups; i++)
					{
						String sub = match.toString(i);
						if (sub != null)
						{
							resultArray.setElementAt(
								new ESString(sub), i);
						}
						else
						{
							resultArray.setElementAt(
								new ESString(""), i);
						}
					}
					return resultArray;
				}
				else
				{
					return ESNull.theNull;
				}
			}
		}

		class StringPrototypeSplit extends BuiltinFunctionObject
		{
			StringPrototypeSplit(String name, Evaluator evaluator,
				FunctionPrototype fp)
			{
				super(fp, evaluator, name, 1);
			}

			public ESValue callFunction(ESObject thisObject,
				ESValue[] arguments) throws EcmaScriptException
			{
				String str = thisObject.toString();
				ESObject ap = this.evaluator.getArrayPrototype();
				ArrayPrototype theArray = new ArrayPrototype(ap,
					this.evaluator);
				if (arguments.length <= 0)
				{
					theArray.setSize(1);
					theArray.setElementAt(thisObject, 0);
				}
				else
				{
					if (arguments[0] instanceof ESGNURegExp)
					{
						ESGNURegExp pattern = (ESGNURegExp)
							arguments[0];
						int n = -1;
						if (arguments.length > 1)
						{
							n = arguments[1].toUInt32();
							if (n <= 0) n = -1;
						}
						RE re = pattern.getPattern();
						Vector result = new Vector();
						int pos = 0;
						int len = str.length();

						while (pos < len)
						{
							REMatch match = re.getMatch(str, pos);

							if (match != null &&
								(n == -1 || n - 1 > result.size()))
							{
								int start = match.getStartIndex();
								int end = match.getEndIndex();
								int matchLen = end - start;
								int chunkLen = start - pos;
								if (matchLen == 0) chunkLen++;
								result.addElement(str.substring(pos,
									pos + chunkLen));
								pos = (int)Math.max(end, pos + 1);
							}
							else
							{
								result.addElement(str.substring(pos));
								break;
							}
						}

						int l = result.size();
						theArray.setSize(l);
						for (int i = 0; i < l; i++)
						{
							theArray.setElementAt(new ESString(
								(String)result.elementAt(i)), i);
						}

					}
					else
					{ // ! instanceof ESGNURegExp
						String sep = arguments[0].toString();
						int strLen = str.length();
						int sepLen = sep.length();

						if (sepLen == 0)
						{
							theArray.setSize(strLen);
							for (int i = 0; i < strLen; i++)
							{
								theArray.setElementAt(new ESString(
									str.substring(i, i + 1)), i);
							}
						}
						else
						{
							int i = 0;
							int start = 0;
							while (start < strLen)
							{
								int pos = str.indexOf(sep, start);
								if (pos < 0)
									pos = strLen;
								theArray.setSize(i + 1);
								theArray.setElementAt(new
									ESString(str.substring(start, pos)), i);
								start = pos + sepLen;
								i++;
							}
						}
					} //  instanceof ESGNURegExp
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