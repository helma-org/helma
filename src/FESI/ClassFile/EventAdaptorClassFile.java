/*
 *
 * @(#) EncapsulatedEventAdaptorClassFile.java 1.3@(#)
 *
 * Copyright (c) 1997 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * 
 */

/**
 * <p>
 * FESI.ClassFile.EncapsulatedEventAdaptorClassFile
 * </p> 
 *
 * @version 1.0
 * @author Laurence P. G. Cable
 */

package FESI.ClassFile;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import java.io.IOException;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.Vector;

import FESI.Interpreter.EventAdaptor;
import FESI.Interpreter.EventAdaptorGenerator;

import FESI.ClassFile.Attribute;
import FESI.ClassFile.ClassFile;
import FESI.ClassFile.ClassConstant;
import FESI.ClassFile.Code;
import FESI.ClassFile.ConstantPoolEntry;
import FESI.ClassFile.Exceptions;
import FESI.ClassFile.IntegerConstant;
import FESI.ClassFile.FieldDesc;
import FESI.ClassFile.FieldConstant;
import FESI.ClassFile.MethodDesc;
import FESI.ClassFile.MethodConstant;
import FESI.ClassFile.StringConstant;

/**
 * <p>
 * This class is used by the EventAdaptorGenerator to author the
 * implementation of the EventAdaptor classes that it is responsible
 * for generating and loading.
 * </p>
 *
 * <p>
 * This class wraps all the ClassFile generic support classes and provides 
 * the adaptor specific implemenetation.
 * </p>
 *
 * @see FESI.Interpreter.EventAdaptorGenerator
 */

public final class EventAdaptorClassFile {

    private static String superClassName =
        "FESI.Interpreter.EventAdaptor";

    /*
     * 
     */

    private String              listenerName;
    private Class               listenerClass;

    private String              adaptorName;
    private ClassFile           classFile;

    private Method[]            listenerMethods;

    /*
     *
     */
     
    private StringConstant      listenerNameConst;

    private FieldConstant       methodsField;
    private FieldConstant       clazzField;

    private MethodConstant      fireMethod; 
    private MethodConstant      crackedFireMethod; 

    private MethodConstant      forNameMethod; 

    private MethodConstant      getMethodsMethod; 

    /**
     * <p>
     * This statis function creates the class file implementation and writes
     * it to the stream for loading ...
     * </p>
     *
     * @param className the name of the adaptor class to synthesize.
     * @param os        the stream to write the class file into
     * @exception IOException If any IO error occured during class loading
     * @exception ClassNotFoundException If the class could not be loaded
     *
     * @returns
     */

    public EventAdaptorClassFile(String className, OutputStream os)  
                    throws IOException, ClassNotFoundException {

        adaptorName  = className;
        listenerName = EventAdaptorGenerator.getBaseNameFromAdaptorName(className);

        listenerClass = Class.forName(listenerName);

        listenerMethods = listenerClass.getMethods();

        classFile = new ClassFile(className, superClassName);

        // generate the misc class descriptions ...

        generateClassSundries();

        // generate the class initializer

        generateInitializer();

        // now the constructor ...

        generateConstructor();

        // now the methods ...

        generateListenerMethods();

        // write the resulting adaptor class to the stream provided.

        write(os);
    }

    /**
     * Are we running debug for this Adaptor generation?
     */

    private boolean debug() { return false; }

    /**
     * Generate misc constant pool entries etc etc ...
     */

    private void generateClassSundries() {

        // the adaptor implements the Listener interface.

        classFile.addInterface(ClassFile.fullyQualifiedForm(listenerName));

        listenerNameConst = classFile.addStringConstant(listenerName);

        /*
         * private static java.lang.reflect.Method[] methods;
         */

        classFile.addFieldDesc(
             new FieldDesc(
                "methods",
                "[Ljava/lang/reflect/Method;",
                (short)(FieldDesc.ACC_STATIC | FieldDesc.ACC_PRIVATE),
                classFile,
                (Attribute[])null
            )
        );

        methodsField = classFile.addFieldConstant(adaptorName,
                                                  "methods",
                                                  "[Ljava/lang/reflect/Method;"
                       );

        /*
         * java.lang.reflect.Method[] java.lang.reflect.Method.getMethods();
         */

        getMethodsMethod = classFile.addMethodConstant(
                                "java/lang/Class",
                                "getMethods",
                                "()[Ljava/lang/reflect/Method;"
                           );

        /*
         * java.lang.Class java.lang.Class.forName();
         */

        forNameMethod = classFile.addMethodConstant(
                                "java/lang/Class",
                                "forName",
                                "(Ljava/lang/String;)Ljava/lang/Class;"
                         );
        /*
         * private static java.lang.Class clazz;
         */

        classFile.addFieldDesc(
             new FieldDesc(
                "clazz",
                "Ljava/lang/Class;",
                (short)(FieldDesc.ACC_STATIC | FieldDesc.ACC_PRIVATE),
                classFile,
                (Attribute[])null
            )
        );

        clazzField = classFile.addFieldConstant(adaptorName,
                                                "clazz",
                                                "Ljava/lang/Class;"
                     );

        /*
         * these are superclass methods called from listener stubs ...
         */

        fireMethod = classFile.addMethodConstant(
                           adaptorName,
                           "fire",
                           "(Ljava/util/EventObject;Ljava/lang/reflect/Method;)V"
               );

        crackedFireMethod = classFile.addMethodConstant(
                                adaptorName,
                                "fire",
                                "([Ljava/lang/Object;Ljava/lang/reflect/Method;)V"
               );

        /*
         * stub out base class abstract method:
         *
         * public static Class getListenerClass() { return clazz; }
         *
         */

        Code c = new Code(classFile, (short)1, (short)2);

        c.addOpShort(Code.OP_GETSTATIC, clazzField.getConstantPoolIndex());
        c.addOp     (Code.OP_ARETURN);

        Code[] ary = { c };

        classFile.addMethodDesc(
            new MethodDesc(
                "getListenerClass",
                "()Ljava/lang/Class;",
                (short)MethodDesc.ACC_PUBLIC,
                classFile,
                ary
            )
        );
    }

    /**
     * <p> Generate class Initializer method </p>
     */

    private void generateInitializer() {
        Code           c   = new Code(classFile, (short)0, (short)3);
        Code[]         ary = { c };
        short          i   = listenerNameConst.getConstantPoolIndex();


        // clazz = Class.forName( <The_Listener_Interface> );

        if (i <= 255) 
            c.addOp1(Code.OP_LDC, (byte)i);
        else
            c.addOpShort(Code.OP_LDC_WIDE, i);

        c.addOpShort(Code.OP_INVOKE_STATIC,
                     forNameMethod.getConstantPoolIndex()
        );

        c.addOp(Code.OP_DUP);

        c.addOpShort(Code.OP_PUTSTATIC,
                     clazzField.getConstantPoolIndex()
        );


        // methods = clazz.getMethods();

        c.addOpShort(Code.OP_INVOKE_VIRTUAL,
                     getMethodsMethod.getConstantPoolIndex()
        );

        c.addOpShort(Code.OP_PUTSTATIC,
                     methodsField.getConstantPoolIndex()
        );

        c.addOp     (Code.OP_RETURN);

        classFile.addMethodDesc(
            new MethodDesc(
                "<clinit>",
                "()V",
                (short)(MethodDesc.ACC_PRIVATE | MethodDesc.ACC_STATIC),
                classFile,
                ary
            )
        );
    }

    /**
     * Author the no-args public constructor for this Adaptor
     * 
     * public void <init>() { super(); }
     */

    private void generateConstructor() {
        Code            c   = new Code(classFile, (short)1, (short)2);
        Code[]          ary = { c };
        MethodConstant  mc;

        // get a MethodConstant for the superclass constructor

        mc = classFile.addMethodConstant(
                ClassFile.fullyQualifiedForm(superClassName),
                "<init>",
                "()V"
             );

        // push this onto the stack

        c.addOp     (Code.OP_ALOAD_0);

        // call the superclass constructor

        c.addOpShort(Code.OP_INVOKE_SPECIAL, mc.getConstantPoolIndex());

        c.addOp     (Code.OP_RETURN);


        // now add a method to the class file describing the constructor ...

        classFile.addMethodDesc(
            new MethodDesc(
                "<init>",
                "()V",
                (short)MethodDesc.ACC_PUBLIC,
                classFile,
                ary
            )
        );
    }

    /**
     * Author the Listener Method Stubs for the EventListener interface 
     * this class is adapting to the EventListener interface.
     */

    private void generateListenerMethods() {
        for (int i = 0; i < listenerMethods.length; i++) {

            /* we can only generate code for EventListener methods of
             * the form:
             *
             *          void <method_name> ( <EventObject Subtype> )
             * or:
             *          void <method_name> ( {<arbitrary arg list>} )
             *
             * if we dont match these patterns we drop the method on the
             * floor.
             */

            if (!Void.TYPE.equals(listenerMethods[i].getReturnType())) {
                System.err.println(
                        "Detected unexpected method signature: " +
                        listenerMethods[i]                       +
                        " in interface: "                        +
                        listenerName
                );
            } else {
                Class[] lmParams     = listenerMethods[i].getParameterTypes();
                Class[] lmExceptions = listenerMethods[i].getExceptionTypes();


                if (lmParams != null     &&
                    lmParams.length == 1 &&
                    java.util.EventObject.class.isAssignableFrom(lmParams[0])) {
                        generateSimpleListenerMethodStub(listenerMethods[i],
                                                         lmParams[0],
                                                         lmExceptions,
                                                         i
                        );
                } else {
                    generateCrackedListenerMethodStub(listenerMethods[i],
                                                      lmParams,
                                                      lmExceptions,
                                                      i
                    );
                }
            }
        }
    }

    /**
     * Generate a simple EventListener interface method stub
     */

    private void generateSimpleListenerMethodStub(Method listenerMethod, Class  listenerParam, Class[] listenerExceptions, int listenerMethodTableIndex) {
        Code          c     = new Code(classFile, (short)2, (short)4);
        Attribute[]   ary;

        /*
         * public void <EventListenerMethod>(<EventObject> e)
         * {
         *      EncapsulatedEvent t = new EncapsulatedEvent(e);
         *      Method            m = findListenerMethod();
         *
         *      fire(t, m);
         * }
         */

        c.addOp     (Code.OP_ALOAD_0);                  // this
        c.addOp     (Code.OP_ALOAD_1);                  // event object

        c.addOpShort(Code.OP_GETSTATIC,
                     methodsField.getConstantPoolIndex()
        );
        
        if (listenerMethodTableIndex <= 255) {
            c.addOp1(Code.OP_BIPUSH, (byte)listenerMethodTableIndex);
        } else {
            short i = classFile.addIntegerConstant(listenerMethodTableIndex).getConstantPoolIndex();

            if (i <= 255)
                c.addOp1(Code.OP_LDC, (byte)i);
            else
                c.addOpShort(Code.OP_LDC_WIDE, i);
        }

        c.addOp     (Code.OP_AALOAD);
        
        c.addOpShort(Code.OP_INVOKE_VIRTUAL,
                     fireMethod.getConstantPoolIndex()
        );                                              // call fire();

        c.addOp   (Code.OP_RETURN);                     // get out of here

        if (listenerExceptions != null && listenerExceptions.length > 0) {
                ary = new Attribute[2];

                ary[1] = new Exceptions(listenerExceptions, classFile);
        } else {
                ary = new Attribute[1];
        }

        ary[0] = c;

        // define the listener method

        classFile.addMethodDesc(
            new MethodDesc(
                listenerMethod.getName(),
                "(" + ClassFile.fieldType(listenerParam.getName()) + ")V",
                (short)(listenerMethod.getModifiers() & ~MethodDesc.ACC_ABSTRACT),
                classFile,
                ary
            )
        );
    }

    /**
     * Generate a cracked EventListener interface method stub
     */

    private void generateCrackedListenerMethodStub(Method listenerMethod, Class[] listenerParams, Class[] listenerExceptions, int listenerMethodTableIndex) {
        Code          c     = new Code(classFile,
                                       (short)(listenerParams.length * 2 + 1),
                                       (short)9
                                  );
        Attribute[]   ary;
        String        methodPDesc = "";
        boolean       wasDoubleWord;    // was the last param processed double?

        c.addOp     (Code.OP_ALOAD_0);                  // this

        /*
         * For cracked Event listener methods we construct an array of Objects
         * to contain the cracked actual parameters ... primitive types are
         * wrapped in a container object suitable for their type.
         */

        if (listenerParams.length <= 255) {
            c.addOp1(Code.OP_BIPUSH, (byte)listenerParams.length);
        } else {
            short i = classFile.addIntegerConstant(listenerParams.length).getConstantPoolIndex();

            if (i <= 255)
                c.addOp1(Code.OP_LDC, (byte)i);
            else
                c.addOpShort(Code.OP_LDC_WIDE, (short)i);
        }

        c.addOpShort(Code.OP_ANEWARRAY,
                classFile.addClassConstant("java.lang.Object").getConstantPoolIndex()
        );

        /*
         * we've now constructed and array of java/lang/Object ... now populate
         * it with the actual params.
         */

        int lvarIdx = 1; // because locals[0] == this

        /*
         * for each formal parameter, generate code to load the actual
         * param from this methods local vars, then if it is a primitive
         * type, then construct a container object and initialize it
         * to the primitives value.
         *
         * as a side effect of this loop we also construct the methods
         * descriptor to optimise processing of the type info
         */

        for (int i = 0; i < listenerParams.length; i++, lvarIdx += (wasDoubleWord ? 2 : 1)) {

                c.addOp(Code.OP_DUP); // the array reference

                if (lvarIdx <= 255) { // the array index
                    c.addOp1(Code.OP_BIPUSH, (byte)i);
                } else {
                    short ic = classFile.addIntegerConstant(i).getConstantPoolIndex();
                    if (ic < 255)
                        c.addOp1(Code.OP_LDC, (byte)ic);
                    else
                        c.addOpShort(Code.OP_LDC_WIDE, (short)ic);
                }

                /*
                 * get the param value onto TOS
                 * as a side effect gather method descriptor string.
                 */ 

                 String s = processParam(c, listenerParams[i], lvarIdx);

                 c.addOp(Code.OP_AASTORE); // arrayref, index, value

                 wasDoubleWord = s.equals("J") || s.equals("D");

                 methodPDesc += s;
        }

        // that's the array constructed ... now lets call my superclass fire
        
        // but first we need to tell that method which listener is firing ...

        c.addOpShort(Code.OP_GETSTATIC,
                     methodsField.getConstantPoolIndex()
        );
        
        if (listenerMethodTableIndex <= 255) {
            c.addOp1(Code.OP_BIPUSH, (byte)listenerMethodTableIndex);
        } else {
            short i = classFile.addIntegerConstant(listenerMethodTableIndex).getConstantPoolIndex();

            if (i <= 255)
                c.addOp1(Code.OP_LDC, (byte)i);
            else
                c.addOpShort(Code.OP_LDC_WIDE, i);
        }

        c.addOp     (Code.OP_AALOAD);   // this, array, method
        
        // now we can call the fire method

        c.addOpShort(Code.OP_INVOKE_VIRTUAL,
                     crackedFireMethod.getConstantPoolIndex()
        );                                              // call fire();

        c.addOp   (Code.OP_RETURN);                     // get out of here

        if (listenerExceptions != null && listenerExceptions.length > 0) {
                ary = new Attribute[2];

                ary[1] = new Exceptions(listenerExceptions, classFile);
        } else {
                ary = new Attribute[1];
        }

        ary[0] = c;

        // define the listener method

        classFile.addMethodDesc(
            new MethodDesc(
                listenerMethod.getName(),
                "(" + methodPDesc + ")V",
                (short)(listenerMethod.getModifiers() & ~MethodDesc.ACC_ABSTRACT),
                classFile,
                ary
            )
        );
    }

    /*
     * This method is used to generate code for cracked event listener 
     * stubs. Its job is to generate code to load the appropriate parameter
     * data type onto the stack, create a wrapper object if needed and leave
     * the appropriate value on TOS for storing into the objects array.
     */

    private String processParam(Code c, Class pClass, int pIdx) {
        ClassConstant  cc = null;
        MethodConstant mc = null;
        byte           ldOpCode        = Code.OP_ALOAD; // load ref by default
        byte           convOpCode      = 0;
        boolean        singleWordParam = true;
        Class          pType           = pClass;
        boolean        isPrimitive;
        boolean        isArrayRef;

        String         pDesc = "";

        // is this an array reference?

        while (pType.isArray()) { // side - effect: construct array param desc 
            pType = pType.getComponentType();
            pDesc += "[";
        }

        isPrimitive = pType.isPrimitive();
        isArrayRef  = pClass.isArray();

        if (isPrimitive) { // builtin datatype
            if (pType.equals(java.lang.Long.TYPE)) {
                pDesc += "J";

                if (!isArrayRef) {
                    cc = classFile.addClassConstant("java/lang/Long");

                    mc = classFile.addMethodConstant(
                            "java/lang/Long",
                            "<init>",
                            "(J)V"
                         );

                    ldOpCode         = Code.OP_LLOAD;
                    singleWordParam  = false;
                }
            } else if (pType.equals(java.lang.Float.TYPE)) {
                pDesc += "F";

                if (!isArrayRef) {
                        cc = classFile.addClassConstant("java/lang/Float");

                        mc = classFile.addMethodConstant(
                                "java/lang/Float",
                                "<init>",
                                "(F)V"
                             );

                        ldOpCode  = Code.OP_FLOAD;
                }
            } else if (pType.equals(java.lang.Double.TYPE)) {
                pDesc += "D";

                if (!isArrayRef) {
                    cc = classFile.addClassConstant("java/lang/Double");

                    mc = classFile.addMethodConstant(
                            "java/lang/Double",
                            "<init>",
                            "(D)V"
                         );

                    ldOpCode         = Code.OP_DLOAD;
                    singleWordParam  = false;
                }
            } else { // integer, array or objref computational types ...

                ldOpCode = Code.OP_ILOAD;

                if (pType.equals(java.lang.Boolean.TYPE))   {
                    pDesc += "Z";

                    if (!isArrayRef) {
                        cc = classFile.addClassConstant("java/lang/Boolean");

                        mc = classFile.addMethodConstant(
                               "java/lang/Boolean",
                               "<init>",
                               "(Z)V"
                             );

                        convOpCode  = Code.OP_I2B;
                    }
                } else if (pType.equals(java.lang.Character.TYPE)) {
                    pDesc += "C";

                    if (!isArrayRef) {
                        cc = classFile.addClassConstant("java/lang/Character");

                        mc = classFile.addMethodConstant(
                                "java/lang/Character",
                                "<init>",
                                "(C)V"
                             );

                        convOpCode  = Code.OP_I2C;
                    }
                } else if (pType.equals(java.lang.Byte.TYPE))      {
                    pDesc += "B";
        
                    if (!isArrayRef) {
                        cc = classFile.addClassConstant("java/lang/Byte");

                        mc = classFile.addMethodConstant(
                                "java/lang/Character",
                                "<init>",
                                "(C)V"
                             );

                        convOpCode  = Code.OP_I2B;
                    }
                } else if (pType.equals(java.lang.Short.TYPE))     {
                    pDesc += "S";

                    if (!isArrayRef) {
                        cc = classFile.addClassConstant("java/lang/Short");

                        mc = classFile.addMethodConstant(
                                "java/lang/Short",
                                "<init>",
                                "(S)V"
                             );

                        convOpCode  = Code.OP_I2S;
                    }
                } else if (pType.equals(java.lang.Integer.TYPE))   {
                    pDesc += "I";

                    if (!isArrayRef) {
                        cc = classFile.addClassConstant("java/lang/Integer");

                        mc = classFile.addMethodConstant(
                                "java/lang/Integer",
                                "<init>",
                                "(I)V"
                             );
                    }
                }
            }
        } else { // handle descriptors for non-primitives ...
            pDesc += ClassFile.fieldType(pType.getName());
        }

        // now load the param value onto TOS ...

        if (pIdx < 255)
            c.addOp1(ldOpCode, (byte)pIdx);
        else {
            c.addOp(Code.OP_WIDE);
            c.addOpShort(ldOpCode, (short)pIdx);
        }

        if (isPrimitive && !isArrayRef) { // additional processing for primitives
            if (convOpCode != 0) {   // narrow Int?
                c.addOp(convOpCode); // then widen the reference
            }

            // we now have the param's value of TOS,
            // construct a container object for it

            c.addOpShort(Code.OP_NEW, (short)cc.getConstantPoolIndex());

            if (singleWordParam) {
                c.addOp(Code.OP_DUP_X1);        // this, <param_word>, this
                c.addOp(Code.OP_SWAP);          // this, <param_word> 
            } else {
                c.addOp(Code.OP_DUP_X2);        // this, <param_2word>, this
                c.addOp(Code.OP_DUP_X2);        // this, this, <param_2word>, this
                c.addOp(Code.OP_POP);           // this, this, <param_2word>
            }

            c.addOpShort(Code.OP_INVOKE_SPECIAL, mc.getConstantPoolIndex());
        }

        // param value on TOS

        return pDesc; // type descriptor - side effect
    }

    /**
     * <p> write the class file to the stream </p>
     *
     * @param os the output stream
     *
     * @throws   IOException
     */

    private void write(OutputStream os) throws IOException {
        classFile.write(os);
    }
}