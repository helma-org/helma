// ESWrapper.java
// FESI Copyright (c) Jean-Marc Lugrin, 1999
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

package FESI.Data;

import FESI.Exceptions.*;
import FESI.Interpreter.*;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.EventListener;

import java.lang.reflect.*;
import java.beans.*;


/**
 * Wrap a Java Object for use in EcmaScript
 */
public class ESWrapper extends ESObject {

    // For debugging
    static boolean debugEvent = false;
    public static void setDebugEvent(boolean b) {
        debugEvent = b;
    }
    static public boolean isDebugEvent() {
        return debugEvent;
    }
    

    private Object javaObject; // the wrapped object
    private boolean asBean = false; // true if created as a bean
    
    // A marker object never returned as a valid property !
    private static ESObject noPropertyMarker = null;
    
    private Hashtable eventHandlers = null;
    private Hashtable eventAdaptors = null;
    
    /**
     * Wrap a java object in an EcmaScript object, use object
     * and beans reflection to access the java object fields.
     * @param javaObject the Java object to wrap
     * @param evaluator the evaluator
     */       
    public ESWrapper(Object javaObject, Evaluator evaluator) {
        super(null, evaluator);
        this.javaObject = javaObject;
        if (javaObject.getClass().isArray()) {
            throw new ProgrammingError("Object wrapper used on array object");
        }
        // This should be done at startup
        if (noPropertyMarker==null) {
          noPropertyMarker = new ObjectObject(null, evaluator);
        }
    }
	
    /**
     * Wrap a java object in an EcmaScript object, use only
     * beans reflection to access the java object fields if asBean is true.
     * @param javaObject the Java object to wrap
     * @param evaluator the evaluator
     * @param asBean if true always consider the object as a bean
     */       
    public ESWrapper(Object javaObject, Evaluator evaluator, boolean asBean) {
        super(null, evaluator);
        this.javaObject = javaObject;
        this.asBean = asBean;
        if (javaObject.getClass().isArray()) {
            throw new ProgrammingError("Object wrapper used on array object");
        }
        // This should be done at startup
        if (noPropertyMarker==null) {
          noPropertyMarker = new ObjectObject(null, evaluator);
        }
    }
    
    /**
     * Return the wraped object
     * @return the wraped object
     */
    public Object getJavaObject() {
        return javaObject;
    }
    
    /**
     * Return true if object must be considered as a java bean
     * @return true if bean
     */
    public boolean isBean() {
        return asBean;
    }
    
    // overrides
    public ESObject getPrototype() {
        throw new ProgrammingError("Cannot get prototype of Wrapper");
    }
    
    // overrides
    public String getESClassName() {
        return "Java Object";
    }

    // overrides
    public int getTypeOf() {
       return EStypeObject;
   }

    // overrides
    // Get either a bean or an object property - for objects attempt bean access if object access failed
    public ESValue getPropertyInScope(String propertyName, ScopeChain previousScope, int hash) 
                     throws EcmaScriptException {
        ESValue value;
        // if (ESLoader.debugJavaAccess) System.out.println("** Property searched in scope: " + propertyName);
         if (asBean) {
             value = getBeanProperty(propertyName, hash);
             if (value!= noPropertyMarker) return value; // found
         } else {
             value = getObjectProperty(propertyName, hash);
             if (value==noPropertyMarker) value = getBeanProperty(propertyName, hash);
             if (value==noPropertyMarker) value = getCorbaProperty(propertyName, hash);
         }
         if (value==noPropertyMarker) {
              if (previousScope == null) {
              throw new EcmaScriptException("global variable '" + propertyName + "' does not have a value");
            } else {
                value = previousScope.getValue(propertyName, hash);
            }
        }
        return value;
    }

    // overrides
    // Get either a bean or an object property - for objects attempt bean access if object access failed
    public ESValue getProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
         ESValue value;
         
         if (asBean) {
             // If declared as bean only examine using bean convention
             value = getBeanProperty(propertyName, hash);
             if (value == noPropertyMarker) {
                throw new EcmaScriptException("Property '" + propertyName + 
                             "' does not exists in bean " + this);
                //return ESUndefined.theUndefined;
             }
         } else {
             // Otherwise examine as java object, bean, or corba object
            value = getObjectProperty(propertyName, hash);
            if (value == noPropertyMarker) value = getBeanProperty(propertyName, hash);
            if (value == noPropertyMarker && javaObject instanceof Class) {
                /*
                THIS CODE HAS BEEN REPLACED BY A HACK USING THE $ FORM OF THE NAME, SEE BELOW
                Class insideClasses[] = ((Class) javaObject).getDeclaredClasses();
                // System.out.println("***** insideClasses.size: "+insideClasses.length);
                if (insideClasses.length>0) {
                    throw new EcmaScriptException("Handling of subclasses not implemented - sorry");
                    // return ESUndefined.theUndefined; 
                } else {
                    throw new EcmaScriptException("Subclass, field or property '" + propertyName + 
                         "' does not exists in class " + this + " - Subclass search not implemented in JDK 1.1");
                    //return ESUndefined.theUndefined;
                }
                */
                // getDeclaredClasses is not implemented, use name to find internal classes
                String className = ((Class) javaObject).getName() + "$" + propertyName;
                if (ESLoader.debugJavaAccess) System.out.println("** Check if inside class: " + className);
                Class insideClass = null;
                try {
                    insideClass = Class.forName(className);
                } catch (ClassNotFoundException ex) {
                    throw new EcmaScriptException("Subclass, field or property '" + propertyName + 
                         "' does not exists in class " + this);
                    //return ESUndefined.theUndefined;
                }
                return new ESWrapper(insideClass, evaluator);
            }   
            if (value == noPropertyMarker) value = getCorbaProperty(propertyName, hash);
            if (value == noPropertyMarker) {
               throw new EcmaScriptException("Field or property '" + propertyName + 
                         "' does not exists in object " + this);
            }
        }
        
        return value;
    }    

    /**
     * Get a bean property (static property of the class if applied to a Class object)
     *
     * @param propertyName the name of the property
     * @param hash its hash value
     * @return the bean property
     * @exception  NoSuchFieldException There is no such property
     * @exception  EcmaScriptException Error accessing the property value
     * @return an EcmaScript value
     */
     
    private ESValue getBeanProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        if (ESLoader.debugJavaAccess) System.out.println("** Bean property searched: " + propertyName);
        Class cls = null;
        Object theObject = null; // means static
        if (javaObject instanceof Class) {
            cls = (Class) javaObject;
        } else {
            cls = (Class) javaObject.getClass();
            theObject = javaObject;
        }
        
        PropertyDescriptor descriptor =  ClassInfo.lookupBeanField(propertyName, cls);
        if (descriptor == null) {
           return noPropertyMarker;
        }
        if (descriptor instanceof IndexedPropertyDescriptor) {
            Class propCls = descriptor.getPropertyType();
            if (propCls == null) 
                throw new EcmaScriptException("Bean property '" + propertyName + 
                                                "' does not have an array access method");          
        }
        Method readMethod = descriptor.getReadMethod();
        if (readMethod == null) {
            throw new EcmaScriptException("No read method for property " + propertyName);
        }
        if (ESLoader.debugJavaAccess) System.out.println("** Read method found for: " + propertyName);
        Object obj = null;
        try {
           obj = readMethod.invoke(javaObject, null);
        } catch (InvocationTargetException e) {
            throw new EcmaScriptException ("Error int the getter for " + propertyName, e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new EcmaScriptException ("Access error invoking getter for " + propertyName + 
                                            ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ProgrammingError ("Inconsistent type of argument for property " + propertyName + 
                                            ": " + e.getMessage());
        }
        return ESLoader.normalizeValue(obj, evaluator);
   }
    

    /**
     * Get a corba property (use name of property as name of the routine)
     *
     * @param propertyName the name of the property
     * @param hash its hash value
     * @return the bean property
     * @exception  NoSuchFieldException There is no such property
     * @exception  EcmaScriptException Error accessing the property value
     * @return an EcmaScript value
     */
     
    private ESValue getCorbaProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        if (ESLoader.debugJavaAccess) System.out.println("** CORBA property searched: " + propertyName);
        Class cls = (Class) javaObject.getClass();
        Method readMethod = null;
        try {
          readMethod = cls.getMethod(propertyName, null);
          if (readMethod==null) {
             return noPropertyMarker;
          } 
          Class rt = readMethod.getReturnType();  
          if (rt == null ||
             rt == Void.TYPE ||
             readMethod.getParameterTypes().length!=0) {
             // If it is not a no argument value returning method, ignore
             return noPropertyMarker;
          }
        } catch (NoSuchMethodException ignore) {
             return noPropertyMarker;
        }
        
        if (ESLoader.debugJavaAccess) System.out.println("** CORBA read method found for: " + propertyName);
        Object obj = null;
        try {
           obj = readMethod.invoke(javaObject, null);
        } catch (InvocationTargetException e) {
            throw new EcmaScriptException ("Error in the CORBA getter function for " + propertyName, e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new EcmaScriptException ("Access error invoking CORBA getter for " + propertyName + 
                                            ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ProgrammingError ("Inconsistent type of argument for property " + propertyName + 
                                            ": " + e.getMessage());
        }
        return ESLoader.normalizeValue(obj, evaluator);
   }


    /**
     * Get an object property (static property of the class if applied 
     * to a Class object)
     *
     * @param propertyName the name of the property
     * @param hash its hash value
     * @return the bean property
     * @exception  NoSuchFieldException There is no such property
     * @exception  EcmaScriptException Error accessing the property value
     * @return an EcmaScript value
     */
    private ESValue getObjectProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
         if (ESLoader.debugJavaAccess) System.out.println("** Java object field searched: " + propertyName);
         try {
            Class cls = null;
            Object theObject = null; // means static
            if (javaObject instanceof Class) {
                cls = (Class) javaObject;
            } else {
                cls = (Class) javaObject.getClass();
                theObject = javaObject;
            }
            Field fld;
            try {
              if (theObject == null) {
                 fld = cls.getDeclaredField(propertyName); // static
              } else {
                 fld = cls.getField(propertyName);
              }
            } catch (NoSuchFieldException e) {
               return noPropertyMarker;
            }
            int modifiers = fld.getModifiers();
            // ALLOW ACCESS TO STATIC FIELDS. HW, 2001-11-27
            // if ((theObject == null) != Modifier.isStatic(modifiers)) {
            //    throw new EcmaScriptException("Field mode (static) not correct for "+ propertyName);
            // }
            if (!Modifier.isPublic(modifiers)) {
                throw new EcmaScriptException("Field "+ propertyName + " not public");
            }
            Object obj = fld.get(theObject);
            return ESLoader.normalizeValue(obj, evaluator);
         } catch (IllegalAccessException e) {
             throw new EcmaScriptException("Cannot access java field " + 
                              propertyName + " in " + this + ", error: " + e.toString());
         }
    }
    
    // overrides
    public boolean hasProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        // TEST - wont work for functions, just for fields.
        try {
            getProperty(propertyName, hash);
        } catch (Exception e) {
            return false;
        }
        return true; // So it can be dereferenced by scopechain
                     // and wont be created
                     // See deleteProperty too and 8.6.2 on host objects
    }
    
    // overrides
    public boolean isHiddenProperty(String propertyName, int hash) {
        return false;
    }
    
    // overrides
    // Put either a bean or an object property - for objects attempt bean access if object access failed
    public void putProperty(String propertyName, ESValue propertyValue, int hash) 
                                throws EcmaScriptException {
        if (propertyValue == ESUndefined.theUndefined) {
            throw new EcmaScriptException("Cannot set the field or property " +
                propertyName + " of a non EcmaScript object field to undefined");
        }
                          
        if (asBean) {
          if (!putBeanProperty(propertyName, propertyValue, hash)) {
                    throw new EcmaScriptException("Cannot put value in property '" + propertyName + 
                             "' which does not exists in Java Bean '" + this +"'");
          }
       } else {
          if (putObjectProperty(propertyName, propertyValue, hash))  return;
          if (putBeanProperty(propertyName, propertyValue, hash)) return;
          if (putCorbaProperty(propertyName, propertyValue, hash)) return;
             
          throw new EcmaScriptException("Cannot put value in field or property '" + propertyName + 
                           "' which does not exists in Java or Corba object '" + this +"'");
       }
    }

    /**
     * Put a property using the beans access functions. 
     * @param propertyName the name of the property
     * @propertyValue the value of the property
     * @hash the hash code of the property name
     * @exception  NoSuchFieldException There is no such property
     * @exception  EcmaScriptException Error accessing the property value
     */
    private boolean putBeanProperty(String propertyName, ESValue propertyValue, int hash) 
                                throws EcmaScriptException {
        if (ESLoader.debugJavaAccess) System.out.println("** Bean property searched: " + propertyName);
        if (propertyValue == ESUndefined.theUndefined) {
            throw new ProgrammingError("Cannot set bean property " + 
                              propertyName + " to undefined");
        }
        Class cls = null;
        Object theObject = null; // means static
        if (javaObject instanceof Class) {
            cls = (Class) javaObject;
        } else {
            cls = (Class) javaObject.getClass();
            theObject = javaObject;
        }
        PropertyDescriptor descriptor =  ClassInfo.lookupBeanField(propertyName, cls);
        if (descriptor == null) {
              // Possibly event ?
              if (theObject != null && propertyName.startsWith("on")) {
                 putEventHandler(propertyName, propertyValue);
                 return true;    
              } else {
                 return false;
              }
        }
        Class propClass = descriptor.getPropertyType();
        if (descriptor instanceof IndexedPropertyDescriptor) {
            if (propClass == null) throw new EcmaScriptException("Bean property '" + propertyName + "' does not have an array access method");          
        }
        Method writeMethod = descriptor.getWriteMethod();
        if (writeMethod == null) {
            throw new EcmaScriptException("No write method for Java property '" + propertyName +"'");
        }
        if (ESLoader.debugJavaAccess) System.out.println("** Write method found for: " + propertyName);
        Object params[] = new Object [1];
        if (propClass.isArray()) {
            if (! (propertyValue instanceof ArrayPrototype)) {
                throw new EcmaScriptException ("Argument should be Array for property '" + propertyName + "'");
            }
            ArrayPrototype ao = (ArrayPrototype) propertyValue;
            params[0]=ao.toJavaArray(propClass.getComponentType());
        } else {
            params[0]=propertyValue.toJavaObject();
        }
        try {
           writeMethod.invoke(javaObject, params); // System will check consistency
        } catch (InvocationTargetException e) {
            throw new EcmaScriptException ("Error in the setter for " + propertyName, e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new EcmaScriptException ("Access error invoking setter for " + propertyName + 
                                            ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new EcmaScriptException ("Type of argument not suitable for property " + propertyName + 
                                            ": " + e.getMessage());
        }
        if (ESLoader.debugJavaAccess) System.out.println("** Property set: " + propertyName);
        return true;
    }
    
    /**
     * Put a property using the CORBA convention. 
     * @param propertyName the name of the property
     * @propertyValue the value of the property
     * @hash the hash code of the property name
     * @exception  NoSuchFieldException There is no such property
     * @exception  EcmaScriptException Error accessing the property value
     */
    private boolean putCorbaProperty(String propertyName, ESValue propertyValue, int hash) 
                                throws EcmaScriptException {
        if (ESLoader.debugJavaAccess) System.out.println("** Corba property searched: " + propertyName);
        if (propertyValue == ESUndefined.theUndefined) {
            throw new ProgrammingError("Cannot set non EcmaScript property " + 
                              propertyName + " to undefined");
        }
        Object[] params = new Object[1];
        params[0]=propertyValue.toJavaObject();
        Method writeMethod = null;
         
        try {
          writeMethod = lookupMethod(evaluator, propertyName, params, false);
          if (writeMethod==null) {
             // If it is not a no argument value returning method, ignore
            return false;
          }
          Class rt = writeMethod.getReturnType();
          if (rt!=null && rt==Void.class) {
            return false;
          }
        } catch (NoSuchMethodException ignore) {
            return false;
        }
        
        if (ESLoader.debugJavaAccess) System.out.println("** CORBA write method found for: " + propertyName);
        
        try {
           writeMethod.invoke(javaObject, params); // System will check consistency
        } catch (InvocationTargetException e) {
            throw new EcmaScriptException ("Error in the CORBA setter for " + propertyName, e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new EcmaScriptException ("Access error invoking CORBA setter for " + propertyName + 
                                            ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new EcmaScriptException ("Type of argument not suitable for CORBA property " + propertyName + 
                                            ": " + e.getMessage());
        }
        if (ESLoader.debugJavaAccess) System.out.println("** Property set: " + propertyName);
        return true;
    }


    /**
     * Put a property using the beans access functions. If not found and starts by "on"
     * assume that it is an event handler.
     * @param propertyName the name of the property
     * @propertyValue the value of the property
     * @hash the hash code of the property name
     * @exception  NoSuchFieldException There is no such property
     * @exception  EcmaScriptException Error accessing the property value
     */
    private boolean putObjectProperty(String propertyName, ESValue propertyValue, int hash) 
                                throws  EcmaScriptException {
        if (ESLoader.debugJavaAccess) System.out.println("** Object field searched: " + propertyName);
        if (propertyValue == ESUndefined.theUndefined) {
            throw new ProgrammingError("Cannot set java object field " + 
                              propertyName + " to undefined");
        }
        Object theObject = null; // means static
        Class cls = null;
        if (javaObject instanceof Class) {
           cls = (Class) javaObject;
        } else {
           cls = (Class) javaObject.getClass();
           theObject = javaObject;
        }
        
        Field fld;
        try {
            if (theObject == null) {
               fld = cls.getDeclaredField(propertyName); // static
            } else {
               fld = cls.getField(propertyName); // include fields in superclass
            }
        } catch (NoSuchFieldException e) {
              // Possibly event ?
              if (theObject != null && propertyName.startsWith("on")) {
                 putEventHandler(propertyName, propertyValue);
                 return true;    
              } else {
                 return false;
              }
        }
        
        int modifiers = fld.getModifiers();
        if ((theObject == null) != Modifier.isStatic(modifiers)) {
              throw new EcmaScriptException("Field mode (static) not correct for "+ propertyName);
        }
        if (!Modifier.isPublic(modifiers)) {
              throw new EcmaScriptException("Field "+ propertyName + " not public");
        }
        
        try {
             fld.set(theObject, propertyValue.toJavaObject()); 
        } catch (IllegalArgumentException e) {
             throw new EcmaScriptException("Field " + propertyName + " of " + this +
                           " cannot be set with " + propertyValue + ", error: " + e.toString());
        } catch (IllegalAccessException e) {
             throw new EcmaScriptException("Access error setting field " + propertyName + " of " + this +
                            ", error: " + e.toString());
        }
        return true;
    }
    
    // overrides
    public void putHiddenProperty(String propertyName, ESValue propertyValue) 
                                throws EcmaScriptException {
        throw new ProgrammingError("Cannot put hidden property in " + this);
    }

    // overrides
    public boolean deleteProperty(String propertyName, int hash) 
                                throws EcmaScriptException {
        // Well, in fact it should use a hasProperty, but as
        // it is not well implemented...
        return false;
    }

    /** 
     * indicates that the getProperties return an enumerator to the
     * value itself rather than to the index.
     *
     * Return true
     */
    public boolean isDirectEnumerator() {
        return true;
    }
    
    /**
     * If this is an enumerator, use it directly as an enumerator
     * for "for in" statements. It is a "bizare" enumerator,
     * as it returns directly the element to enumerate rather than
     * its index in the object itself. <P>
     * Otherwise returns an dummy enumerator.
     *
     * @returns the enumerator or a dummy enumerator
     */
    public Enumeration getProperties() {
         if (javaObject instanceof Enumeration) {
             return (Enumeration) javaObject;
         } else {
            // No visible properties supported - yet
             return new Enumeration() {
                    public boolean hasMoreElements() {
                        return false;
                    }
                    public Object nextElement() {
                             throw new java.util.NoSuchElementException();
                    }
             };
         }
      }
    
    // No property list supported - yet
    public Enumeration getAllProperties() {
        return getProperties();
    }

    /**
     * Build an event adaptor for a class, keep them in a cache
     * @param listenerClass the class to event to listen to
     * @exception EcmaScriptException Unable to build adaptor
     */
    private EventAdaptor getEventAdaptor(Class listenerType) throws EcmaScriptException {
        if (eventAdaptors == null) eventAdaptors = new Hashtable();
        // Check if event adaptor already created
        EventAdaptor adaptor = (EventAdaptor) eventAdaptors.get(listenerType);
        if (adaptor == null) {
           if (debugEvent) System.out.println("** Creating new adaptor for '" + listenerType.getName() +"'"); 
           try {
              adaptor = EventAdaptor.getEventAdaptor(listenerType, javaObject, this);
            } catch (Exception e) {
                throw new EcmaScriptException("Cannot build adaptor for '" +
                        listenerType.getName() + "', error: " + e);
            }
            eventAdaptors.put(listenerType, adaptor);
       }
       if (debugEvent) System.out.println("** Adaptor found: " + adaptor);
       return adaptor;
    }

    /**
     * Create an event handler for a specific property
     * @param propertyName the name of the property (must start with "on")
     * @param handler The value which must be an event handler routine or script
     */
    private void putEventHandler(String propertyName, 
                                ESValue handler) 
                throws EcmaScriptException {
        String eventName = propertyName.substring(2);
        if (debugEvent) System.out.println("** Attempt to set event '" + propertyName +"'"); 
        
        
        // prepare the handler code - if it is not a function,
        // build a function of one argument (the event)
        ESObject eventHandler = null;
        if (handler != ESNull.theNull) {
            if (handler instanceof FunctionPrototype) {
                eventHandler = (ESObject) handler;
            } else {
                ESValue body = handler.toESString();
                ESObject fo = evaluator.getFunctionObject();
                ESValue event = new ESString("event"); // The parameter of the handling routine
                ESValue [] esArgs = new ESValue [] {event,body};
                try {
                    eventHandler = fo.doConstruct(null, esArgs);
                } catch (EcmaScriptException e) {
                    // The error must be rethrown as if it is the error indicating incmplete
                    // lines the interpreter may ask the user for more lines !
                    throw new EcmaScriptException ("Error creating function anonymous(event){"+body+"}\n" + e);
                }
            }
        }
        
        // Find the handler characteristics and key
        Class cls = javaObject.getClass();
        BeanInfo bi;
        try {
             bi = Introspector.getBeanInfo(cls);
        } catch (IntrospectionException e) {
           throw new EcmaScriptException("BeanInfo not found for java class '" + 
                   cls + "', error: " +e.getMessage());                        
        }
        EventSetDescriptor [] eds = bi.getEventSetDescriptors();
        for (int i=0; i<eds.length; i++) {
            EventSetDescriptor thisEvent = eds[i];
            String name = thisEvent.getName();
            // Check if event name matches the onXxx name
            if (name.equalsIgnoreCase(eventName)) { // We could handle case better
                if (debugEvent) System.out.println("** Event '" + propertyName +"' found");
                Method methods[]  = thisEvent.getListenerMethods();
                // If onXxxx is for an event name, then there must be a single method
                if (methods.length!=1) {
                    throw new EcmaScriptException("Only 1 listener supported, there are " +
                            methods.length + " listeners for event '" + eventName + "'");                        
                }
                Class listenerType = thisEvent.getListenerType();
                String key = listenerType.getName()+":"+methods[0].getName();
                this.getEventAdaptor(listenerType);
                if (eventHandlers==null) eventHandlers = new Hashtable();
                if (handler == ESNull.theNull) {
                    if (debugEvent) System.out.println(" ** Handler removed for key: " + key);
                    eventHandlers.remove(key);
                } else {
                    if (debugEvent) System.out.println(" ** Handler added for key: " + key);
                    eventHandlers.put(key, eventHandler);
                }
                return;
            } else {
                // Check if onXxx matches a method of this event
                Method methods[]  = thisEvent.getListenerMethods();
                for (int j=0; j<methods.length; j++) {
                    Method thisMethod = methods[j];
                    String methodName = thisMethod.getName();
                    if (methodName.equalsIgnoreCase(eventName)) {   // We could handle casing better
                        if (debugEvent) System.out.println("** Event method '" + propertyName +"' found");
                        Class listenerType = thisEvent.getListenerType();
                        String key = listenerType.getName()+":"+thisMethod.getName();
                        this.getEventAdaptor(listenerType);
                        if (eventHandlers==null) eventHandlers = new Hashtable();
                        if (handler == ESNull.theNull) {
                            if (debugEvent) System.out.println(" ** Handler removed for key: " + key);
                            eventHandlers.remove(key);
                        } else {
                            if (debugEvent) System.out.println(" ** Handler added for key: " + key);
                            eventHandlers.put(key, eventHandler);
                        }
                        return;
                    }
                } // for each method
            }
        } // for each event
        throw new EcmaScriptException("Event '" + eventName + "' not found for java class " + cls);                        
    }

    
    // overrides
    public ESValue getDefaultValue(int hint) 
                                throws EcmaScriptException {
        if (hint == EStypeString) {
            return new ESString(javaObject.toString());
        } else {
            throw new EcmaScriptException ("No default value for " + this + 
                                     " and hint " + hint);
        }
    }
    
    // overrides
    public ESValue getDefaultValue() 
                                throws EcmaScriptException {
        return this.getDefaultValue(EStypeString);
    }
    
    
    
     /**
      * Find a method 
      * <P>If static return null instead of exception in case of not found at all
      * @param evaluator theEvaluator
      * @param functionName the name to find
      * @param params The list of parameters to filter by type
      * @staticMethod true if looking for a static method
      * @return the method (null if static and not found)
      * @exception NoSuchMethodException instance object and method not found
      * @exception EcmaScriptException various errors
      */
     private Method lookupMethod(Evaluator evaluator,
                                 String functionName,
                                 Object[] params,
                                  boolean staticMethod) 
                 throws EcmaScriptException, NoSuchMethodException {
                     
         int nArgs = params.length;
         if (ESLoader.debugJavaAccess) System.out.println("** " + (asBean ? "Bean" : "Class") + 
                       " method lookup: " +
                     (staticMethod ? "static " : "") + functionName);
        Class cls = null;
        Object theObject = null; // means static
        if (staticMethod) {
           if (javaObject instanceof Class) {
               cls = (Class) javaObject;
           } else {
               throw new ProgrammingError("Cannot lookup for static method if not class");
           }
        } else {
            cls = (Class) javaObject.getClass();
            theObject = javaObject;
        }
        Method [] methods = null;
        if (asBean) {
            methods = ClassInfo.lookupBeanMethod(functionName, cls);
        } else {
            methods = ClassInfo.lookupPublicMethod(functionName, cls);
        }
        if (methods == null || methods.length==0) {
            if (staticMethod) return null; // A second try will be done
            throw new NoSuchMethodException("No method named '" + functionName + "' found in "+ this);
        }
        // To inform user if a method of proper name and attribte found but not matching arguments
        boolean atLeastOneFoundWithAttributes = false;
        Method nearestMethodFound = null;
        CompatibilityDescriptor descriptorOfNearestMethodFound = null;
        int distanceOfNearestMethodFound = -1; // infinite
        boolean multipleAtSameDistance = false;
        for (int i=0; i<methods.length; i++) {
            Method method = methods[i];
            if (ESLoader.debugJavaAccess) System.out.println("** Method to validate: " + method.toString());
            int modifiers = method.getModifiers();
            if (staticMethod && !Modifier.isStatic(modifiers)) continue; // for static only
            atLeastOneFoundWithAttributes = true;
            Class[] paramTypes = method.getParameterTypes();
            if (paramTypes.length!=nArgs) continue;
            CompatibilityDescriptor cd = ESLoader.areParametersCompatible(paramTypes, params);
            int distance = cd.getDistance();
            if (distance<0) continue; // Method not applicable
            if (ESLoader.debugJavaAccess) System.out.println("** Method acceptable(" + distance + 
                  " : " + Modifier.toString(modifiers) + " " + methods[i].toString());
            // Optimization - if perfect match return immediately
            // Note that "perfect" match could be wrong if there is
            // a bug in the complex parameter matching algorithm,
            // so the optimization is not taken when debugging, to
            // allow to catch multiple "perfect match" which should not happen.
            if (distance == 0 && !ESLoader.debugJavaAccess) {
              cd.convert(params);
              return method; // success
            }
            // Imperfect match, keep the best one if multiple found
            if (nearestMethodFound == null) {
               // None so far
               nearestMethodFound = method;
               descriptorOfNearestMethodFound = cd;
               distanceOfNearestMethodFound = distance;
            } else {
               // Keep best - if identical we have a problem
               if (distance<distanceOfNearestMethodFound) {
                 nearestMethodFound = method;
                 descriptorOfNearestMethodFound = cd;
                 distanceOfNearestMethodFound = distance;
                 multipleAtSameDistance = false;
               } else if (distance == distanceOfNearestMethodFound) {
                  if (ESLoader.debugJavaAccess) System.out.println("** Same distance as previous method!");
                  if (distance!=0) {
                    // if 0 (due to debugging) accept any good one
                    multipleAtSameDistance = true;
                  }
               }
            }
        }
        if (nearestMethodFound!=null) {
          if (multipleAtSameDistance) {
            throw new EcmaScriptException("Ambiguous method '" + functionName + "' matching parameters in "+ this);
          }
          descriptorOfNearestMethodFound.convert(params);
          return nearestMethodFound; // success
        }
        if (atLeastOneFoundWithAttributes) {
            throw new EcmaScriptException("No method '" + functionName + "' matching parameters in "+ this);
        } else {
            if (ESLoader.debugJavaAccess) System.out.println("** Method rejected - did not match attribute or parameters");
            if (staticMethod) return null; // A second try will be done
            throw new EcmaScriptException("No method named '" + functionName + "' found in "+ this);
        }
    }    
    
    // overrides
    public ESValue doIndirectCall(Evaluator evaluator,
    																 ESObject target,
                                  String functionName,
                                  ESValue[] arguments) 
                                        throws EcmaScriptException, NoSuchMethodException {
        int nArgs = arguments.length;
        if (ESLoader.debugJavaAccess) System.out.println("** Method searched: " + functionName + 
                                                         " in object of class " + javaObject.getClass());
        Object[] params = new Object[nArgs];
        for (int k = 0; k<nArgs; k++) {
            if (arguments[k] == ESUndefined.theUndefined) {
                throw new EcmaScriptException("Cannot use undefined as parameter for java method " + functionName + ", use 'null'");
            }
            params[k] = arguments[k].toJavaObject();
        }
        Method method = null;
        if (javaObject instanceof Class) {
           // Will be null if no method of that name, error thrown if
           // method found but does not have compatible parameters. This
           // results in more helpful error message if a method is found
           // as static but does not have the expected parameter types.
           method = lookupMethod(evaluator, functionName, params, true);
        }
        if (method==null) {
           method = lookupMethod(evaluator, functionName, params, false);
        }
        Object obj = null;
        Class retCls = method.getReturnType();
        try {
           obj = method.invoke(javaObject, params);
        } catch (InvocationTargetException e) {
            // Modified by Hannes Wallnoefer to pass through EcmaScriptExceptions
            Throwable te = e.getTargetException (); 
            if (te instanceof EcmaScriptException)  
              throw (EcmaScriptException) te; 
            else  
              throw new EcmaScriptException ("Error in java method " + functionName, e.getTargetException());
        } catch (IllegalAccessException e) {
            throw new EcmaScriptException ("Access error invoking java method " + functionName + 
                                            ": " + e.getMessage());
        } catch (IllegalArgumentException e) {
            throw new ProgrammingError ("Inconsistent type of argument for method " + functionName + 
                                            ": " + e.getMessage());
        }
        ESValue eobj;
        if (retCls != Void.TYPE) {
           eobj = ESLoader.normalizeValue(obj, evaluator);
        } else {
           eobj = ESUndefined.theUndefined;
        }
        return eobj;
    } 
    
    // overrides
    public ESValue doIndirectCallInScope(Evaluator evaluator,
                                        ScopeChain previousScope,
                                        ESObject thisObject, 
                                        String functionName,
                                        int hash, 
                                        ESValue[] arguments) 
                                        throws EcmaScriptException {
        if (ESLoader.debugJavaAccess) System.out.println("** Method searched (indirect): " + functionName);
        try {
            return doIndirectCall(evaluator, thisObject, functionName, arguments); 
        } catch (NoSuchMethodException e) {
            if (previousScope == null) {
                throw new EcmaScriptException("no global function named '" + functionName + "'");
            } else {
                return previousScope.doIndirectCall(evaluator, thisObject, functionName, hash, arguments);
            }
        }
    }
 
 
    // overrides
    public ESValue callFunction(ESObject thisObject, 
                               ESValue[] arguments) 
                                        throws EcmaScriptException {
         return constructOrCall(thisObject, arguments, true);
    } 
       
    // overrides
    public ESObject doConstruct(ESObject thisObject, 
                                ESValue[] arguments) 
                                        throws EcmaScriptException {
         return constructOrCall(thisObject, arguments, false);
    }    
    
    /** 
     * Implements both new and function call on class objects (which create a new
     * object). The only difference is that elementary objects are not normalized
     * to EcmaScript objects if 'new' is used, so that the can be operated upon
     * as java object. Mostly useful for String
     *
     * @param thisObject the target object of the call
     * @param arguments the arguments
     * @param isCall true if call, false if new
     * @return the result of the new or call
     * @exception EmcaScriptException any exception during call
     */
    private  ESObject constructOrCall(ESObject thisObject, 
                                ESValue[] arguments,
                                boolean isCall) 
                                        throws EcmaScriptException {
         if (ESLoader.debugJavaAccess) System.out.println("** Constructor searched for: " + javaObject.toString());
         if (javaObject instanceof Class) {
             int nArgs = arguments.length;
             try {
                Class cls = (Class) javaObject;
                Constructor [] constructors = cls.getConstructors();
                Object[] params = new Object[nArgs];
                for (int k = 0; k<nArgs; k++) {
                    if (arguments[k] == ESUndefined.theUndefined) {
                        throw new EcmaScriptException(
                            "Cannot use undefined as parameter for java constructor " + 
                             cls.toString());
                    }
                   params[k] = arguments[k].toJavaObject();
                }
                // To inform user if a constructor of proper name and attribute found but not matching arguments
                boolean atLeastOneFoundWithAttributes = false;
                Constructor nearestConstructorFound = null;
                CompatibilityDescriptor descriptorOfNearestConstructorFound = null;
                int distanceOfNearestConstructorFound = -1; // infinite
                boolean multipleAtSameDistance = false;
                for (int i=0; i<constructors.length; i++) {
                    Constructor constructor = constructors[i];
                    if (ESLoader.debugJavaAccess) System.out.println(
                              "** Contructor examined: " +  constructor.toString());
                    Class[] paramTypes = constructor.getParameterTypes();
                    int modifiers = constructor.getModifiers();
                    if (!Modifier.isPublic(modifiers)) continue;
                    atLeastOneFoundWithAttributes = true;
                    if (paramTypes.length!=nArgs) continue;
                    CompatibilityDescriptor cd = ESLoader.areParametersCompatible(paramTypes, params);
                    int distance = cd.getDistance();
                    if (distance<0) continue; // Constructor not applicable
                    if (ESLoader.debugJavaAccess) System.out.println("** Constructor acceptable(" + distance + 
                          " : " + Modifier.toString(modifiers) + " " + constructors[i].toString());
                    // Optimization - if perfect match return immediately
                    // Note that "perfect" match could be wrong if there is
                    // a bug in the complex parameter matching algorithm,
                    // so the optimization is not taken when debugging, to
                    // allow to catch multiple "perfect match" which should not happen.
                    if (distance == 0 && !ESLoader.debugJavaAccess) {
                       nearestConstructorFound = constructor;
                       descriptorOfNearestConstructorFound = cd;
                       distanceOfNearestConstructorFound = distance;
                       break; // success
                    }
                    // Imperfect match, keep the best one if multiple found
                    if (nearestConstructorFound == null) {
                       // None so far
                       nearestConstructorFound = constructor;
                       descriptorOfNearestConstructorFound = cd;
                       distanceOfNearestConstructorFound = distance;
                    } else {
                       // Keep best - if identical we have a problem
                       if (distance<distanceOfNearestConstructorFound) {
                         nearestConstructorFound = constructor;
                         descriptorOfNearestConstructorFound = cd;
                         distanceOfNearestConstructorFound = distance;
                         multipleAtSameDistance = false;
                       } else if (distance == distanceOfNearestConstructorFound) {
                          if (ESLoader.debugJavaAccess) System.out.println("** Same distance as previous constructor!");
                          if (distance!=0) {
                            // if 0 (due to debugging) accept any good one
                            multipleAtSameDistance = true;
                          }
                       }
                    }
                }
                // We have found
                if (nearestConstructorFound!=null) {
                  if (multipleAtSameDistance) {
                    throw new EcmaScriptException("Ambiguous constructor for "+ javaObject.toString() );
                  }
                  descriptorOfNearestConstructorFound.convert(params);
                  if (ESLoader.debugJavaAccess) System.out.println(
                            "** Contructor called: " + 
                            nearestConstructorFound.toString());
                  Object obj = null;
                  try {
                       obj = nearestConstructorFound.newInstance(params);
                  } catch (InvocationTargetException e) {
                      throw new EcmaScriptException ("Error creating " + javaObject + 
                                                      ": " + e.getTargetException());
                  }
                  if (ESLoader.isBasicClass(cls) && !isCall) {
                       return new ESWrapper(obj, evaluator); // Do not normalize if new basic class
                  } else {
                      return ESLoader.normalizeObject(obj, evaluator);
                  }
                }

                if (atLeastOneFoundWithAttributes) {
                    throw new EcmaScriptException("No constructor matching parameters in: "+ this);
                } else {
                    throw new EcmaScriptException("No public constructor in: "+ this);
                }
             } catch (Exception e) {
                 throw new EcmaScriptException("Cannot build new " + this + 
                                 ", error: " + e.toString());
             }
         } else {
             throw new EcmaScriptException("Not a java class: " + this);
        }
    }    
  
    
    // overrides
    public double doubleValue() {
        double d = Double.NaN; // should check if doubleValue is present
        return d;
    }
    
    // overrides
    public boolean booleanValue() {
        return true;  // Should check if booleanValue is present
    }
    
    // overrides
    public String toString() {
        return (javaObject == null) ? "<?Wrapper to null?>" : javaObject.toString();
    }
    
    // overrides
    public Object toJavaObject() {
        return javaObject;
    }
     
    //public String getTypeofString() {
    //    return "JavaObject";
    //}
  
  
  
    // Routine to handle events
    public void dispatchEvent(Object [] a, Class listener, Method event) {
        if (debugEvent) System.out.println(" ** Dispatch event: " + event.getName() + " for " + listener.getName());
        if (eventHandlers == null) return; // no handler at all
        String key = listener.getName()+":"+event.getName();
        if (debugEvent) System.out.println(" ** Event key: " + key);
        ESObject handlerFunction = (ESObject) eventHandlers.get(key);
        if (handlerFunction == null) return; // no handler for this event
        if (debugEvent) System.out.println(" ** Handler found: " + handlerFunction);
        try {
           evaluator.evaluateEvent(this, handlerFunction, a);
       } catch (EcmaScriptException e) {
           System.err.println("Exception in FESI event handler: " + e);
       }
    }

  
  
    // Routines to describe this object
    
    // overrides
    public String toDetailString() {
        if (asBean)
            return "ES:[BEAN:" + getESClassName() + ":" + javaObject.toString() + "]";
        else 
            return "ES:[OBJ:" + getESClassName() + ":" + javaObject.toString() + "]";
    }

    static final int stepClass = 0;
    static final int stepConstructors = 1;
    static final int stepMethods = 2;
    static final int stepBeanMethods = 3;
    static final int stepFields = 4;
    static final int stepBeanProperties = 5;
    static final int stepEvents = 6;
    static final int stepNoMore = 7;

    private EventSetDescriptor [] getEvents(Class cls) {
        EventSetDescriptor [] eds = null;
        BeanInfo bi = null;
        try {
             bi = Introspector.getBeanInfo(cls);
        } catch (IntrospectionException e) {
            // ignore events
        }
        if (bi!=null) eds = bi.getEventSetDescriptors();
        if (eds==null) {
            eds = new EventSetDescriptor[0];
        }
        return eds;
    }
 
    // overrides
    public Enumeration getAllDescriptions() {
         return new Enumeration() {
                Class clazz = javaObject.getClass();
                int step = stepClass;
                Constructor [] constructors = clazz.getConstructors();
                Method [] methods = (asBean ? new Method[0] : clazz.getMethods());
                Field[] fields = (asBean ? new Field[0] : clazz.getFields()); 
                PropertyDescriptor [] beanProperties = getBeanPropertyDescriptors();
                MethodDescriptor [] beanMethods = getBeanMethodDescriptors();
                EventSetDescriptor [] events = ESWrapper.this.getEvents(clazz);
                int index = 0;
                
                private PropertyDescriptor [] getBeanPropertyDescriptors() {
                    PropertyDescriptor [] bean_properties = new PropertyDescriptor[0];
                    if (ESWrapper.this.asBean) {
                        try {
                            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                            bean_properties = beanInfo.getPropertyDescriptors();
                        } catch (Exception e) {
                            // ignore
                        }
                    } else {
                        // Only return them if different from fields...
                        PropertyDescriptor [] properties = null; 
                        try {
                            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                            properties = beanInfo.getPropertyDescriptors();                        
                        } catch (Exception e) {
                            // ignore
                        }
                        if (properties!=null) {
                            Field[] fields = clazz.getFields();
                            // This is pretty uggly...
                            int remainingProps = 0;
                            for (int iProps = 0; iProps<properties.length; iProps++) {
                                String propName = properties[iProps].getName();
                                for (int iField = 0; iField<fields.length; iField++) {
                                    String fieldName = fields[iField].getName();
                                    if (propName.equals(fieldName)) {
                                        properties[iProps] = null;
                                        break;
                                    }
                                }
                                if (properties[iProps] != null) remainingProps++;
                            }
                            if (remainingProps>0) {
                                bean_properties = new PropertyDescriptor[remainingProps];
                                int insert = 0;
                                for (int iProps = 0; iProps<properties.length; iProps++) {
                                    if (properties[iProps]!=null) bean_properties[insert++] = properties[iProps];
                                }
                            }
                        }
                    }
                    return bean_properties;
                }         

                private MethodDescriptor [] getBeanMethodDescriptors() {
                    MethodDescriptor [] bean_methods = new MethodDescriptor[0];
                    if (ESWrapper.this.asBean) {
                        try {
                            BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                            bean_methods = beanInfo.getMethodDescriptors();
                        } catch (Exception e) {
                            // ignore
                        }
                    }
                    return bean_methods;
                }         

                public boolean hasMoreElements() {
                    if (step==stepClass) return true;
                    if (step==stepConstructors) {
                         if (constructors.length>index) return true;
                         step ++;
                         index = 0;
                    }
                    if (step==stepMethods) {
                         if (methods.length>index) return true;
                         step ++;
                         index = 0;
                    }
                    if (step==stepBeanMethods) {
                         if (beanMethods.length>index) return true;
                         step ++;
                         index = 0;
                    }
                    if (step==stepFields) {
                         if (fields.length>index) return true;
                         step ++;
                         index = 0;
                    }
                    if (step==stepBeanProperties) {
                         if (beanProperties.length>index) return true;
                         step ++;
                         index = 0;
                    }
                    if (step==stepEvents) {
                         if (events.length>index) return true;
                         step ++;
                         index = 0;
                    }
                    return false;
                }
                public Object nextElement() {
                    if (hasMoreElements()) {
                       switch (step) {
                    case stepClass: {
                               step ++;
                               if (asBean) {
                                   return new ValueDescription("BEAN",
                                            describe_class_or_interface(clazz));
                               } else {
                                   return new ValueDescription("CLASS",
                                            describe_class_or_interface(clazz));
                               }
                           } 
                    case stepConstructors: {
                               if (asBean) {
                                    String info = "[[error]]";
                                    index++;
                                    try {
                                        BeanInfo beanInfo = Introspector.getBeanInfo(clazz);
                                        BeanDescriptor beanDescriptor = beanInfo.getBeanDescriptor();
                                        info = beanDescriptor.getName() + " (" + beanDescriptor.getShortDescription() + ")";
                                    } catch (Exception e) {
                                        // ignore
                                    }
                                   return new ValueDescription("BEANINFO", info);
                               } else {
                                   return new ValueDescription("CONSTR",
                                                describe_method_or_constructor(constructors[index++]));   
                               }
                           } 
                    case stepMethods: {
                           return new ValueDescription("FUNC",
                                        describe_method_or_constructor(methods[index++]));
                           } 
                    case stepBeanMethods: {
                           return new ValueDescription("METHOD",
                                        describe_bean_method(beanMethods[index++]));
                           } 
                    case stepFields: {
                           return new ValueDescription("FIELD",
                                        describe_field(fields[index++], javaObject));
                           } 
                    case stepBeanProperties: {
                           return new ValueDescription("PROPS",
                                        describe_bean_property(beanProperties[index++], javaObject));
                           } 
                    case stepEvents: {
                           return new ValueDescription("EVENT",
                                        describe_event(events[index++]));
                           } 
                        } // switch
                        throw new ProgrammingError("Inconsistent step");
                     } else {
                         throw new java.util.NoSuchElementException();
                     }
                 }
         };
      }

   /**
     * Returns a full description of the value, with the specified name.
     *
     * @param name The name of the value to describe
     *
     * @return   the description of this value  
     */
    public ValueDescription getDescription(String name) {
       return new ValueDescription(name,
                                    "JAVAOBJ",
                                    this.toString());
    }
    
  /** Return the name of an interface or primitive type, handling arrays. */
  private static String typename(Class t) {
    String brackets = "";
    while(t.isArray()) {
      brackets += "[]";
      t = t.getComponentType();
    }
    return t.getName() + brackets;
  }

  /** Return a string version of modifiers, handling spaces nicely. */
  private static String modifiers(int m) {
    if (m == 0) return "";
    else return Modifier.toString(m) + " ";
  }
  
  
  /** describe the modifiers, type, and name of a field */
  private static String describe_field(Field f, Object obj) {
      String s = modifiers(f.getModifiers()) +
                       typename(f.getType()) + " " + f.getName();
      try { 
          Object v = f.get(obj);
          s += " = " + obj.toString();
      } catch (IllegalAccessException ignore) {
      }
      return s + ";";
  }

  /** describe the modifiers, type, and name of a field */
  private static String describe_bean_property(PropertyDescriptor d, Object obj) {
      String a =  (d instanceof IndexedPropertyDescriptor) ? "[]" : ""; 
      String s = d.getName() + a + " (" + d.getShortDescription() + ")";
      Method readMethod = d.getReadMethod();
      if (readMethod != null) {
          try {
             Object v = readMethod.invoke(obj, null);
             s += " = " + v.toString();
          } catch (InvocationTargetException ignore) {
          } catch (IllegalAccessException ignore) {
          } catch (IllegalArgumentException ignore) {
          }
      }
      return s + ";";
  }

  /** Describe the modifiers, return type, name, parameter types and exception
   *  type of a method or constructor.  Note the use of the Member interface
   *  to allow this method to work with both Method and Constructor objects */
  private static String describe_method_or_constructor(Member member) {
    Class returntype=null, parameters[], exceptions[];
    StringBuffer buffer = new StringBuffer();
    if (member instanceof Method) {
      Method m = (Method) member;
      returntype = m.getReturnType();
      parameters = m.getParameterTypes();
      exceptions = m.getExceptionTypes();
    } else {
      Constructor c = (Constructor) member;
      parameters = c.getParameterTypes();
      exceptions = c.getExceptionTypes();
    }

    buffer.append("" + modifiers(member.getModifiers()) +
                     ((returntype!=null)? typename(returntype)+" " : "") +
                     member.getName() + "(");
    for(int i = 0; i < parameters.length; i++) {
      if (i > 0) buffer.append(", ");
      buffer.append(typename(parameters[i]));
    }
    buffer.append(")");
    if (exceptions.length > 0) buffer.append(" throws ");
    for(int i = 0; i < exceptions.length; i++) {
      if (i > 0) buffer.append(", ");
      buffer.append(typename(exceptions[i]));
    }
    buffer.append(";");
    return buffer.toString();
  }
  
  /** Describe the modifiers, return type, name, parameter types and exception
   *  type of a bean method */
  private static String describe_bean_method(MethodDescriptor descriptor) {
    Class returntype=null, parameters[], exceptions[];
    StringBuffer buffer = new StringBuffer();
    Method method = descriptor.getMethod();
    returntype = method.getReturnType();
    parameters = method.getParameterTypes();
    exceptions = method.getExceptionTypes();

    buffer.append(descriptor.getName() + ": " + modifiers(method.getModifiers()) +
                     ((returntype!=null)? typename(returntype)+" " : "") +
                     method.getName() + "(");
    for(int i = 0; i < parameters.length; i++) {
      if (i > 0) buffer.append(", ");
      buffer.append(typename(parameters[i]));
    }
    buffer.append(")");
    if (exceptions.length > 0) buffer.append(" throws ");
    for(int i = 0; i < exceptions.length; i++) {
      if (i > 0) buffer.append(", ");
      buffer.append(typename(exceptions[i]));
    }
    buffer.append(";");
    return buffer.toString();
  }

  private static String describe_class_or_interface(Class c) {
    StringBuffer buffer = new StringBuffer();
    // Print modifiers, type (class or interface), name and superclass.
    if (c.isInterface()) {
      // The modifiers will include the "interface" keyword here...
      buffer.append(Modifier.toString(c.getModifiers()) + " "+c.getName());
    }
    else if (c.getSuperclass() != null)
      buffer.append(Modifier.toString(c.getModifiers()) + " class " +
                       c.getName() +
                       " extends " + c.getSuperclass().getName());
    else
      buffer.append(Modifier.toString(c.getModifiers()) + " class " +
                       c.getName());
        
    // Print interfaces or super-interfaces of the class or interface.
    Class[] interfaces = c.getInterfaces();
    if ((interfaces != null) && (interfaces.length > 0)) {
      if (c.isInterface()) buffer.append(" extends ");
      else buffer.append(" implements ");
      for(int i = 0; i < interfaces.length; i++) {
        if (i > 0) buffer.append(", ");
        buffer.append(interfaces[i].getName());
      }
    }
    return buffer.toString();
  }
  
  private static String describe_event(EventSetDescriptor event) {
      Class eventClass = event.getListenerType();
      return event.getName() + " " + eventClass.getName();
  }
}
