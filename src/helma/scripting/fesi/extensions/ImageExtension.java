// ImageExtension.java
// Copyright (c) Hannes Wallnöfer 1998-2000

package helma.scripting.fesi.extensions;

import helma.objectmodel.*;
import helma.util.*;
import helma.image.*;

import FESI.Interpreter.*;
import FESI.Exceptions.*;
import FESI.Extensions.*;
import FESI.Data.*;

import java.io.*;
import java.awt.image.*;
import java.util.*;
import java.rmi.Naming;


/** 
 * Extension to do Image manipulation from HOP.
 */

public class ImageExtension extends Extension {

    protected Evaluator evaluator = null;

    static boolean remote = false;


    public ImageExtension () {
        super();
    }


    class GlobalObjectImage extends BuiltinFunctionObject {

        ImageExtension imagex;
        ImageGenerator imggen;

        GlobalObjectImage (String name, Evaluator evaluator, FunctionPrototype fp, ImageExtension imagex) {
            super(fp, evaluator, name, 1);
            this.imagex = imagex;
        }
        
        public ESValue callFunction(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           return doConstruct(thisObject, arguments);
        }
        
        public ESObject doConstruct(ESObject thisObject, ESValue[] arguments) throws EcmaScriptException {
           Object img = null;
           IRemoteGenerator rgen = null;

           try {
               if (imggen == null && !remote) {
                   try {
                       imggen = new ImageGenerator ();
                   } catch (UnsatisfiedLinkError noawt) {
	          remote = true;
                   }
               }

               if (remote)
                   rgen = (IRemoteGenerator) Naming.lookup ("//localhost:3033/server");

               if (arguments.length == 1) {
                   if (arguments[0] instanceof ESArrayWrapper) {
                       Object obj = ((ESArrayWrapper) arguments[0]).toJavaObject ();
	          if (obj instanceof byte[]) {
                          img = remote ?
                                 (Object) rgen.createImage ((byte[]) obj) :
                                 (Object) imggen.createImage ((byte[]) obj);
                       }
                   } else if (arguments[0] instanceof ESString) {
                       String imgurl = arguments[0].toString ();
                       img = remote ?
                                 (Object) rgen.createPaintableImage (imgurl) :
                                 (Object) imggen.createPaintableImage (imgurl);
                   }
               } else if (arguments.length == 2) {
                   if (arguments[0] instanceof ESWrapper && arguments[1] instanceof ESWrapper) {
                       // create a new image from an existing one and an image filter
                       Object image = arguments[0].toJavaObject ();
                       Object filter = arguments[1].toJavaObject ();
                       img = imggen.createPaintableImage ((ImageWrapper) image, (ImageFilter) filter);
                   } else if (arguments[0].isNumberValue () && arguments[1].isNumberValue ()) {
                       img = remote ?
                                (Object) rgen.createPaintableImage (arguments[0].toInt32(), arguments[1].toInt32()) :
                                (Object) imggen.createPaintableImage (arguments[0].toInt32(), arguments[1].toInt32());
                   }
               }
           } catch (Exception error) {
               System.err.println ("Error creating Image: "+error);
           }

           if (img == null)
               throw new EcmaScriptException ("Error creating image: Bad parameters or setup problem.");

           return new ESWrapper (img, this.evaluator);
        }
    }
 

    /**
     * Called by the evaluator after the extension is loaded.
     */
    public void initializeExtension(Evaluator evaluator) throws EcmaScriptException {
        
        this.evaluator = evaluator;
        GlobalObject go = evaluator.getGlobalObject();
        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();

        ESObject image = new GlobalObjectImage ("Image", evaluator, fp, this); // the Image constructor
        
        go.putHiddenProperty("Image", image); // register the constructor for a Image object.

    }

 
 }
 
 










