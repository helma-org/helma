/*
 *
 * @(#) EncapsulatedEventAdaptor.java 1.4@(#)
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
 * FESI.Interpreter.EventAdaptor
 * </p>
 *
 * @version 1.0
 * @author Laurence P. G. Cable.
 */

package FESI.Interpreter;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

import FESI.Data.ESWrapper;
import FESI.Exceptions.*;

import java.util.EventObject;
import java.util.EventListener;


import java.beans.BeanInfo;
import java.beans.EventSetDescriptor;
import java.beans.Introspector;
import java.beans.IntrospectionException;


/**
 * <p>
 * FESI.Interpreter.EventAdaptor is an abstract base
 * class designed to support the dynamic generation of java.util.EventListener
 * sub-interface adaptor classes.
 * </p>
 * <p>
 * These dynamically generated adaptor classes can be used to take arbitrary
 * events and call EcmaScript handlers. 
 * </p>
 * <p>
 * Using this dynamic adaptor class, objects that emits events on arbitrary`
 * sub-interfaces of java.util.EventListener can be encapsulated and delivered
 * onto a single EventListener interface, this allowing late binding of event
 * behaviors and filtering applications.
 * </p>
 *
 * <P>see sunw.demo.encapsulatedEvents.EncapsulatedEvent</P>
 * <P>see sunw.demo.encapsulatedEvents.EncapsulatedEventListener</P>
 * <P>see sunw.demo.encapsulatedEvents.EncapsulatedEventException</P>
 * <P>see sunw.demo.encapsulatedEvents.EncapsulatedEventAdaptorGenerator</P>
 */

public abstract class EventAdaptor {

    /**
     * The Event Source
     */

    protected Object source;
    
    /**
     * The target Wrapper object
     */
    protected ESWrapper wrapper;

    /**
     * The Event Source's add<T>Listener method
     */

    protected Method addAdaptorMethod;

    /**
     * The Event Source's remove<T>Listener method
     */

    protected Method removeAdaptorMethod; 


    /**
     * Generate an Adaptor instance for the Listener Interface and Event Source
     *
     * @param  lc       The java.lang.Class object for the listener to adapt.
     * @param  s        The source object that the adaptor will listen to.
     * @param  w        The wrapper object that the adaptor will listen to.
     *
     * @return The newly instantiated dynamic adaptor
     * 
     * @exception ClassNotFoundException If class cannot be loaded
     * @exception IntrospectionException If error in introspection
     * @exception InstantiationException If class found but cannot be instantiated
     * @exception IllegalAccessException If operationis not allowed
     */

    public static EventAdaptor getEventAdaptor(Class lc, Object s, ESWrapper w) 
         throws ClassNotFoundException, InstantiationException, 
                 IllegalAccessException, IntrospectionException {
        Class                    eeac  = null;
        EventAdaptor eea   = null;
        BeanInfo                 sbi   = Introspector.getBeanInfo(s.getClass());
        EventSetDescriptor[]     sesd  = sbi.getEventSetDescriptors();

        /*
         * first validate that the event source emits event on the interface
         * specified.
         */
 
        if (validateEventSource(sesd, lc) == -1) { 
            throw new IllegalArgumentException("Object: "           +
                                               s                    +
                                               " does not source: " +
                                               lc.getName()
                      );
        }

        // generate the adaptor class

        eeac = EventAdaptor.getEventAdaptorClass(lc);

        // instantiate an instance of it ...

        eea  = (EventAdaptor)eeac.newInstance();

        eea.setWrapper(w);
        
        // and register the adaptor with the event source

        try {
            eea.setSource(s);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register with source");
        }

        return eea;
    }


    /**
     *
     * @param   lc       The java.lang.Class object for the listener to adapt.
     *
     * @exception ClassNotFoundException If class cannot be loaded
     *
     * @return The Class object for the newly generated dynamic adaptor class.
     */

    public static Class getEventAdaptorClass(Class lc) throws ClassNotFoundException {
        if (!java.util.EventListener.class.isAssignableFrom(lc)) {
            throw new IllegalArgumentException("Class is not a subinterface of java.util.EventListenerEventListener");
        }

        return EventAdaptorGenerator.getAdaptorClassForListenerClass(lc);
    }

    /**
     * <p> default constructor.
     */

    protected EventAdaptor() { super(); }

    /**
     * @return the java.lang.Class object for the java.util.EventListener subinterface this instance adapts.
     */

    abstract public Class getListenerClass();

    /**
     * @return the name of the java.util.EventListener subinterface this instance adapts.
     */

    public String getListenerClassName() {
        return getListenerClass().getName();
    }

    /**
     * @return the event source object that this instance is adapting.
     */

    public synchronized Object getSource() { return source; }


    public synchronized ESWrapper getWarepper() { return wrapper; }
    public synchronized void setWrapper(ESWrapper w) { wrapper = w; }
    
    /**
     * @param  sesd     the EventSetDescriptor[] from the prospective event source.
     * @param  lc       the java.lang.Class for the EventListener we are adapting.
     * @return the index of the matching EventSetDescriptor or -1.
     */

    private static int validateEventSource(EventSetDescriptor[] sesd, Class lc) {
        for (int i = 0; i < sesd.length; i++)
            if (lc.equals(sesd[i].getListenerType())) return i;

        return -1;
    }

    /**
     * <p>
     * setSource sets the adaptor instance to listen to the specified
     * source. This operation will fail if the source does not emit events
     * on the EventListener subinterface this adaptor implements.
     * </p>
     *
     * @param s         the prospective event source.
     *
     * @exception IntrospectionException If any error with introspection
     * @exception Exception If any other error
     */

    public synchronized void setSource(Object s)
    throws IntrospectionException, Exception {

        if (source != null && s != null && source.equals(s)) return;

        if (source != null) removeAdaptorFromSource(); // unregister current

        if (s == null) {
            source              = null;
            addAdaptorMethod    = null;
            removeAdaptorMethod = null;
            return;
        }

        BeanInfo             sbi   = Introspector.getBeanInfo(s.getClass());
        EventSetDescriptor[] sesd  = sbi.getEventSetDescriptors();

        int                  i;

        if ((i = validateEventSource(sesd, getListenerClass())) == -1) {
            throw new IllegalArgumentException("Object: "           +
                                               s                    +
                                               " does not source: " +
                                               getListenerClassName()
                      );
        }


        // update state

        Object               olds  = source;
        Method               oldam = addAdaptorMethod;
        Method               oldrm = removeAdaptorMethod;

        source              = s;
        addAdaptorMethod    = sesd[i].getAddListenerMethod();
        removeAdaptorMethod = sesd[i].getRemoveListenerMethod();

        try {
            addAdaptorToSource(); // register with new source
        } catch (Exception e) {

            // something went wrong ... restore previous state.

            source              = olds;
            addAdaptorMethod    = oldam;
            removeAdaptorMethod = oldrm;

            if (source != null) addAdaptorToSource();

            throw e; // reraise problem
        }
    }

    /**
     *
     * <p>
     * This method is called from simple EventListener method stubs of
     * dynamic subclasses in order to create and EncapsulatedEvent and
     * deliver it to the registered listeners.
     * </p>
     *
     * @param e  The EventObject being encapsulated
     * @param lm The jav.lang.reflect.Method describing the listener Method.
     *
     * @exception Exception If any error occured in dispatch event
     */

    protected void fire(EventObject e, Method lm) throws Exception {
        Object [] a = new Object [] {e};
        // System.out.println("FIRE: " + lm.getName() + " for " + getListenerClass().getName());
        wrapper.dispatchEvent(a, getListenerClass(), lm);
    }

    /**
     * <p>
     * This method is called from cracked EventListener method stubs of
     * dynamic subclasses in order to create and EncapsulatedEvent and
     * deliver it to the registered listeners.
     * </p>
     *
     * @param a  The cracked Event arguments being encapsulated
     * @param lm The jav.lang.reflect.Method describing the listener Method.
     *
     * @exception Exception If any error occured in dispatch event
     */

    protected void fire(Object[] a, Method lm) throws Exception {
        //System.out.println("FIRE: " + lm.getName() + " for " + getListenerClass().getName());
        wrapper.dispatchEvent(a, getListenerClass(), lm);
    }

    
    /**
     * <p>
     * EncapsulatedEventListener's may raise exceptions to the originating
     * event source through an adaptor by throwing the actual exception within
     * an instance of EncapsulatedEventException.
     * </p>
     * <p>
     * This method is called to verify that it is allowable to re-raise the
     * exception from the adaptor to the source, by checking that the listener
     * method on the adaptor that the source delivered the original event to
     * is permitted to throw such an exception, otherwise a RuntimeException
     * is raised.
     * </p>
     *
     * @param ex        the exception thrown by the listener
     * @param eel       the listener instance that threw it
     * @param lm        the adaptors listener method that must re-raise it
     *
     */

/*
    protected final void handleEventException(EncapsulatedEventException ex, EncapsulatedEventListener eel, Method lm) throws Exception {
        Class   ec  = ex.getExceptionClass();
        Class[] ext = lm.getExceptionTypes();

        // let's see if the Exception encapsulated is one the source is expecting
        // if it is then throw it.

        for (int i = 0; i < ext.length; i++) {
            if (ext[i].isAssignableFrom(ec)) throw ex.getException();
        }

        // if we get here then the Exception the listener is trying
        // to throw isnt one the source is expecting ... so throw it as a RTE

        throw new RuntimeException("Event Source ["                + 
                                   source                          +
                                    "] is not prepared to catch [" +
                                   ex.getException()               +
                                   "] from listener ["             +
                                   eel
        );
    }
*/
    /**
     * Adds this Adaptor to the Event Source
     */

    protected void addAdaptorToSource() {
        try {
            Object[] args = new Object[1];

            args[0] = this;

            addAdaptorMethod.invoke(source, args);
        } catch (InvocationTargetException ite) {
            throw new ProgrammingError("cannot add adaptor ["          +
                               this                            +
                               "] to source ["                 +
                               source                          +
                               "] InvocationTargetException: " +
                               ite.getMessage()
            );
        } catch (IllegalAccessException iae) {
            throw new ProgrammingError("cannot add adaptor ["       +
                               this                         +
                               "] to source ["              +
                               source                       +
                               "] IllegalAccessException: " +
                               iae.getMessage()
            );
        }
    }

    /**
     * Removes this Adaptor instance from the Event Source
     */

    protected void removeAdaptorFromSource() {
        try {
            Object[] args = new Object[1];

            args[0] = this;

            removeAdaptorMethod.invoke(source, args);
        } catch (InvocationTargetException ite) {
            System.err.println("cannot remove adaptor ["       +
                               this                            +
                               "] from source ["               +
                               source                          +
                               "] InvocationTargetException: " +
                               ite.getMessage()
            );
        } catch (IllegalAccessException iae) {
            System.err.println("cannot remove adaptor ["    +
                               this                         +
                               "] from source ["            +
                               source                       +
                               "] IllegalAccessException: " +
                               iae.getMessage()
            );
        }
    }
}