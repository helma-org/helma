/*
 *
 * @(#) EventAdaptorGenerator.java 1.3@(#)
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
 * FESI.Interpreter.EventAdaptorGenerator
 * </p>
 *
 * @version 1.0
 * @author  Laurence P. G. Cable
 */

package FESI.Interpreter;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.lang.reflect.Method;
import java.lang.ClassLoader;
import java.lang.ClassFormatError;

import java.util.Hashtable;


import FESI.ClassFile.EventAdaptorClassFile;

/**
 * <p>
 * The EventAdaptorGenerator is the class responsible for
 * dynamically generating classes that adapt arbitrary sub-interfaces of
 * java.util.EventListener to FESI.ClassFile.EventListener.
 * <p>
 * It is largely derived of EventAdaptorGenerator.java(1.3) of Sun, 
 * delivered as an example of for the bean development tools.
 * </p>
 * <p>
 * The dynamically generated adaptor class is implemented as a subclass of
 * FESI.ClassFile and implements the specified
 * sub-interface of java.util.EventListener. Each listener method of the
 * sub-interface calls into FESI.ClassFile.EncapsulatedEvent to
 * deliver the event to the proper EcmaScript event processing routine.
 * </p>
 */

public final class EventAdaptorGenerator extends ClassLoader {

    private static String packagePrefix = "FESI.ClassFile";
    private static String adaptorInfix  = ".DYN_EE_ADAPTOR.";

    private static String adaptorClassNamePrefix = packagePrefix + adaptorInfix;


    /**
     * @return debug status
     */

    private boolean debug() { return false; }

    /*
     * one instance of the Generator ...
     */

    private static EventAdaptorGenerator generator =
                                new EventAdaptorGenerator(); 

    /**
     * constructs a generator
     */

    private EventAdaptorGenerator() { super(); }

    /**
     * invokes the ClassLoader to load the named class
     *
     * @param className the name of the class to load
     *
     * @return the newly loaded Class
     *
     * @exception ClassNotFoundException if the class cannnot be loaded
     *
     * @see java.lang.ClassLoader
     */

    private static Class loadClassNamed(String className)
    throws ClassNotFoundException {
        return generator.loadClass(className);
    }

    /**
     * invoke the Class Loader to generate and load a dynamically generated
     * adaptor subclass of EventAdaptor to adapt to the specified
     * sub-interface of java.util.EventListener.
     *
     * @param lc The java.lang.Class object for the sub-interface to adapt
     *
     * @return the newly loaded dynamic adaptor class.
     *
     * @throws ClassNotFoundException
     */

    static Class getAdaptorClassForListenerClass(Class lc)
        throws ClassNotFoundException {

        // we assume that lc is a subinterface of java.util.EventListener

        return loadClassNamed(mapListenerNameToAdaptorName(lc.getName()));
    }

    /**
     * invoke the Class Loader to generate and load a dynamically generated
     * adaptor subclass of EncapsulatedEventAdaptor.
     *
     * @return the newly loaded class
     *
     * @throw ClassNotFoundException
     */

    static Class getAdaptorClassForListenerClass(String lcn)
    throws ClassNotFoundException {

        // we assume that lc is a subinterface of java.util.EventListener

        return loadClassNamed(mapListenerNameToAdaptorName(lcn));
    }

    /**
     * loadClass will lookup classes with its Class Loader or via the
     * system, unless the class requested is a dynamic adaptor class,
     * then we invoke the code generator to create the adaptor class
     * on the fly.
     *
     * @return the newly loaded dynamic adaptor class.
     *
     * @exception ClassNotFoundException If the class cannot be found
     */

    protected Class loadClass(String className, boolean resolve)
    throws ClassNotFoundException {
        Class c = findLoadedClass(className); // check the cache

        if (debug()) System.err.println("loadClass(" + className + ")");

        if (c == null) { // not in cache
            if (isAdaptorClassName(className)) { // is this an adaptor?

                // generate an adaptor to this sub-interface

                c = generateAdaptorClass(className);
            } else try { // regular class
                ClassLoader mycl = this.getClass().getClassLoader();

                // look for the requeseted class elsewhere ...

                if (mycl != null) {
                    c = mycl.loadClass(className); // try the CL that loaded me
                }

                if (c == null) {
                    c = findSystemClass(className); // try system for class
                }
            } catch (NoClassDefFoundError ncdfe) {
                throw new ClassNotFoundException(ncdfe.getMessage());
            } 
        }

        if (c != null) { // class found?
                if (resolve) resolveClass(c);
        } else
            throw new ClassNotFoundException(className);

        if (debug()) System.err.println("loaded: " + c.getName());

        return c;
    }

    /**
     * map the EventListener subclass name to the synthetic name for a
     * dynamically generated adaptor class.
     *
     * @return the synthesised adaptor class name.
     */

    private static String mapListenerNameToAdaptorName(String listenerName) {
        return adaptorClassNamePrefix + listenerName;
    }

    /**
     * Checks to determine if class name is that of a dynamically generated
     * EncapsulatedEventAdaptor Class as created by this Generator.
     * 
     * This is defined to be so, iff the class prefix matches the one
     * we generate.
     *
     * @return is this the "name" of an adaptor class?
     */

    private static boolean isAdaptorClassName(String className) {
        return className.startsWith(adaptorClassNamePrefix);
    }

    /**
     * @return the name of the sub-interface we are adapting from the name of the adaptor class.
     */

    public static String getBaseNameFromAdaptorName(String className) {
        return ((className != null && isAdaptorClassName(className))
                        ? className.substring(adaptorClassNamePrefix.length())
                        : null
        );
    }

    /**
     * generates the dynamic Adaptor class to bridge the EventListener
     * interface to the EncapsulatedListener interface.
     *
     * @return the newly generated adaptor class.
     *
     */

    private Class generateAdaptorClass(String className) {
        ByteArrayOutputStream   baos  = new ByteArrayOutputStream(512);
        byte[]                  cimpl = null;
        Class                   clazz = null;

        if (debug()) System.err.println("generateAdaptorClass(" + className + ")");

        try { // generate the classfile into the stream
            new EventAdaptorClassFile(className, (OutputStream)baos);
        } catch (IOException            ioe)  {
            return null;
        } catch (ClassNotFoundException cnfe) {
            return null;
        }

        cimpl = baos.toByteArray(); // get the classfile as an array of bytes
        
        // now "try to" intern it into the runtime ...

        try {
            clazz = defineClass(className, cimpl, 0, cimpl.length);
        } catch (ClassFormatError ex) {
            System.err.println("Failed to define adaptor for " + className);
            System.err.println("Caught: " + ex);
            ex.printStackTrace();
        }

        return clazz;
    }
}