// FesiEngineFactory.java
// Copyright (c) Hannes Wallnöfer 2002

package helma.scripting.fesi;

import helma.scripting.*;
import helma.framework.core.*;

/**
 *  Factory class for FESI evalator engines.
 */
public final class FesiEngineFactory  {

    public static ScriptingEngine getEngine (Application app, RequestEvaluator reval) {
	return new FesiEvaluator (app, reval);
    }
	
}
