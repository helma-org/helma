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

package helma.scripting.rhino.extensions;

import helma.image.*;
import java.awt.image.*;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.NativeJavaArray;
import org.mozilla.javascript.NativeJavaObject;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * Extension to provide Helma with Image processing features.
 */
public class ImageObject {
    static ImageGenerator imggen;
    static Scriptable global;

    /**
     * Called by the evaluator after the extension is loaded.
     */
    public static void init(Scriptable scope) {
        Method[] methods = ImageObject.class.getDeclaredMethods();
        Member ctorMember = null;
        for (int i=0; i<methods.length; i++) {
            if ("imageCtor".equals(methods[i].getName())) {
                ctorMember = methods[i];
                break;
            }
        }
        FunctionObject ctor = new FunctionObject("Image", ctorMember, scope);
        ScriptableObject.defineProperty(scope, "Image", ctor, ScriptableObject.DONTENUM);
        global = scope;
        // ctor.addAsConstructor(scope, proto);
    }

    public static Object imageCtor (Context cx, Object[] args,
                Function ctorObj, boolean inNewExpr) {
        Object img = null;
        try {
            if (imggen == null) {
                try {
                    imggen = new ImageGenerator();
                } catch (UnsatisfiedLinkError noawt) {
                    System.err.println("Error creating Image: " + noawt);
                    throw new RuntimeException("Error creating Image: " + noawt);
                }
            }

            if (args.length == 1) {
                if (args[0] instanceof NativeJavaArray) {
                    Object obj = ((NativeJavaArray) args[0]).unwrap();
                    if (obj instanceof byte[]) {
                        img = imggen.createImage((byte[]) obj);
                    }
                } else if (args[0] instanceof byte[]) {
                    img = imggen.createImage((byte[]) args[0]);
                } else if (args[0] instanceof String) {
                    String imgurl = args[0].toString();
                    img = imggen.createPaintableImage(imgurl);
                }
            } else if (args.length == 2) {
                if (args[0] instanceof Number &&
                                   args[1] instanceof Number) {
                    img = imggen.createPaintableImage(((Number) args[0]).intValue(),
                                                          ((Number) args[1]).intValue());
                } else if (args[0] instanceof NativeJavaObject &&
                        args[1] instanceof NativeJavaObject) {
                    // create a new image from an existing one and an image filter
                    Object wrapper = ((NativeJavaObject) args[0]).unwrap();
                    Object filter = ((NativeJavaObject) args[1]).unwrap();
                    img = imggen.createPaintableImage((ImageWrapper) wrapper,
                                                          (ImageFilter) filter);
                } else {
                    throw new RuntimeException("Error creating Image from args "+args[0]+","+args[1]);
                }
            }
        } catch (Exception error) {
            System.err.println("Error creating Image: " + error);
        }

        if (img == null) {
            throw new RuntimeException("Error creating image: Bad parameters or setup problem.");
        }

        return Context.toObject(img, global);
    }
}
