/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.scripting.fesi.extensions;

import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Extensions.*;
import FESI.Interpreter.*;
import helma.image.*;
import helma.objectmodel.*;
import helma.util.*;
import java.awt.image.*;
import java.io.*;
import java.rmi.Naming;
import java.util.*;

/**
 * Extension to do Image manipulation from HOP.
 */
public class ImageExtension extends Extension {
    static ImageGenerator imggen;
    protected Evaluator evaluator = null;

    /**
     * Creates a new ImageExtension object.
     */
    public ImageExtension() {
        super();
    }

    /**
     * Called by the evaluator after the extension is loaded.
     */
    public void initializeExtension(Evaluator evaluator)
                             throws EcmaScriptException {
        this.evaluator = evaluator;

        GlobalObject go = evaluator.getGlobalObject();
        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();
        ESObject image = new GlobalObjectImage("Image", evaluator, fp, this); // the Image constructor

        go.putHiddenProperty("Image", image); // register the constructor for a Image object.
    }

    class GlobalObjectImage extends BuiltinFunctionObject {
        ImageExtension imagex;

        GlobalObjectImage(String name, Evaluator evaluator, FunctionPrototype fp,
                          ImageExtension imagex) {
            super(fp, evaluator, name, 1);
            this.imagex = imagex;
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            return doConstruct(thisObject, arguments);
        }

        public ESObject doConstruct(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            Object img = null;

            try {
                if (imggen == null) {
                    try {
                        imggen = new ImageGenerator();
                    } catch (UnsatisfiedLinkError noawt) {
                        System.err.println("Error creating Image: " + noawt);
                    }
                }

                if (arguments.length == 1) {
                    if (arguments[0] instanceof ESArrayWrapper) {
                        Object obj = ((ESArrayWrapper) arguments[0]).toJavaObject();

                        if (obj instanceof byte[]) {
                            img = imggen.createImage((byte[]) obj);
                        }
                    } else if (arguments[0] instanceof ESString) {
                        String imgurl = arguments[0].toString();

                        img = imggen.createPaintableImage(imgurl);
                    }
                } else if (arguments.length == 2) {
                    if (arguments[0] instanceof ESWrapper &&
                            arguments[1] instanceof ESWrapper) {
                        // create a new image from an existing one and an image filter
                        Object image = arguments[0].toJavaObject();
                        Object filter = arguments[1].toJavaObject();

                        img = imggen.createPaintableImage((ImageWrapper) image,
                                                          (ImageFilter) filter);
                    } else if (arguments[0].isNumberValue() &&
                                   arguments[1].isNumberValue()) {
                        img = imggen.createPaintableImage(arguments[0].toInt32(),
                                                          arguments[1].toInt32());
                    }
                }
            } catch (Exception error) {
                System.err.println("Error creating Image: " + error);
            }

            if (img == null) {
                throw new EcmaScriptException("Error creating image: Bad parameters or setup problem.");
            }

            return new ESWrapper(img, this.evaluator);
        }
    }
}
