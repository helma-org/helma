// ClassInfo.java
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

package FESI.Interpreter;

import java.util.Hashtable;
import java.util.Vector;

import java.lang.reflect.*;
import java.beans.*;

import FESI.Data.*;
import FESI.Exceptions.*;

/**
 * This class contains the static routines to access the cache of class information
 * as well as the ClassInfo (class specific cached information). The information
 * is separated between the bean based information and the low level information.
 * Even if most classes will have the same information in both cases, they are 
 * likely to be used in only one way, so there space overhead is ususally small.
 * <P>The cache is filled in a lazy manner, only requested information is cached,
 * even if this requires more sweeps of the low level information. The rational is
 * that for small set of information this is not a major drawback, and for large
 * set of information (for example the list of methods of an AWT component), only
 * a small subset of it is really used. So the small foorprint is given more importance
 * than a faster access for seldom cases.
 * <P>The cache is shared by all instance of the evaluator and class loaders (it is
 * enough to identify the classes by their Class object). So the access must
 * be synchronized and no evaluator specific information must be added.
 * <P>The existence of this cache defeats class garbage collections for classes
 * accessed via EcmaScript. This should not be too severe a restriction, and can
 * be lifted in JDK 1.2.
 */
public class ClassInfo {
    
   /** Cache of information on all classes, using the class object as a key */
   static private Hashtable allClassInfo = new Hashtable();

   /** Cache of public methods */
   private Hashtable publicMethods = null;
   /** Cache of bean methods */
   private Hashtable beanMethods = null;
   /** Cache of bean properties */
   private Hashtable beanProperties = null;
   /** Cache of BeanInofo */
   private BeanInfo beanInfo = null;
   
    /**
     * Ensure that ClassInfo objects can only be created via
     * the factory.
     */
   private ClassInfo() {
   }

    /**
     * Ensure that the specified class has a ClassInfo object in the
     * cached. Create an empty one if needed. Must be called synchronized.
     *
     * @param   cls  The class for which we look for a ClassInfo
     * @return  the ClassInfo of cls, added to the cache if needed
     */
   private static ClassInfo ensureClassInfo(Class cls) {
        boolean debug = ESLoader.isDebugJavaAccess();
        ClassInfo classInfo = (ClassInfo) allClassInfo.get(cls);
        if (classInfo == null) {
           if (debug) System.out.println("** Class info for class '" +
                     cls + "' not found in cache, created");
            classInfo = new ClassInfo();
            allClassInfo.put(cls, classInfo);
        }
        return classInfo;
   }
   

    /**
     * Get the property descriptor for the specified field of the specified class,
     * considered as a bean.
     *
     * @param   fieldName  The name of the property
     * @param   cls  The class for which we look for the property.
     * @return  The PropertyDescriptor or null if not found or in case of error
     */
   synchronized public static PropertyDescriptor lookupBeanField(String fieldName, Class cls) {
        ClassInfo classInfo = ClassInfo.ensureClassInfo(cls);
        return classInfo.cachedBeanFieldLookup(fieldName, cls);
   }
   

    /**
     * Get the property descriptor for the specified field of the specified class,
     * considered as a bean, in the current ClassInfo cache. Add it to the cache
     * if needed, after veryfying that the accessors are OK.
     *
     * @param   fieldName  The name of the property
     * @param   cls  The class for which we look for the property.
     * @return  The PropertyDescriptor or null if not found or in case of error
     */
    private PropertyDescriptor cachedBeanFieldLookup(String propertyName, Class cls) {
        boolean debug = ESLoader.isDebugJavaAccess();

        // Check that there is a bean properties cache, chech if the property was cached
        if (beanProperties != null) {
           if (debug) System.out.println("** Bean properties for class '" +
                     cls + "' found in cache");
            PropertyDescriptor descriptor = 
                      (PropertyDescriptor) beanProperties.get(propertyName);
            if (descriptor!= null) {
               if (debug) System.out.println("** property descriptor '" + propertyName + "' found in cache");
                return descriptor;
            }
        } 
        // Not in cache
        if (debug) System.out.println("** No property named '" +
                      propertyName + "' found in cache, lookup started");

        // Do we have a cached BeanInfo ? create it if no
        if (beanInfo == null) {
            try {
               beanInfo = Introspector.getBeanInfo(cls);
           } catch (IntrospectionException e) {
               if (debug) System.out.println(" ** Error getting beaninfo: " + e);
               return null;
           }
        }
        
        // Get the property descriptor by name
        PropertyDescriptor [] allProperties = beanInfo.getPropertyDescriptors(); 
        PropertyDescriptor descriptor = null; // none found
        for (int i=0; i<allProperties.length; i++) {
            PropertyDescriptor property = allProperties[i];
            if (debug) System.out.println("** Property examined: " + property.getName());
            if (!property.getName().equals(propertyName)) continue;
            descriptor = property;
            break;
        }
        // If we found it, test it and add it to the cache
        if (descriptor != null) {
           // Check that it is valid here (to speed up safe usage later) 
            Method readMethod = descriptor.getReadMethod();
            Method writeMethod = descriptor.getWriteMethod();
            Class propCls = descriptor.getPropertyType();
            if (descriptor instanceof IndexedPropertyDescriptor) {
                // Check indexed access
                IndexedPropertyDescriptor iDescriptor = (IndexedPropertyDescriptor) descriptor;
                Method indexedReadMethod = iDescriptor.getIndexedReadMethod();
                Method indexedWriteMethod = iDescriptor.getIndexedWriteMethod();
                Class propIndexedCls = iDescriptor.getIndexedPropertyType();
                if (propIndexedCls == null) {
                    throw new ProgrammingError("getIndexedPropertyType returned null for " + propertyName);
                }
                if (propIndexedCls == Void.TYPE) {
                    throw new ProgrammingError("Void indexed property type for '" + propertyName + "' is not allowed");
                }
                if (indexedReadMethod != null && indexedReadMethod.getParameterTypes().length != 1) {
                    throw new ProgrammingError("Indexed getter of property ' " + propertyName + "' should have 1 parameter!");
                }
                if (indexedWriteMethod != null) {
                    Class [] paramCls = indexedWriteMethod.getParameterTypes();
                    if (paramCls == null || paramCls.length != 2) {
                        throw new ProgrammingError("Indexed setter of property ' " + propertyName + "' should have 2 parameter!");
                    }
                    if (paramCls[0]!=propIndexedCls) { // SHOULD CHECK INSTANCE
                        throw new ProgrammingError("Inconstant parameter type for indexed setter of indexed property' " + propertyName + "', type: " + propCls);
                    }
                }
                // Check non indexed access
                if (propCls != null) {
                    if (!propCls.isArray()) {
                        throw new ProgrammingError("Non array type (" + propCls + ") for array access of indexed property '" + propertyName + "'");
                    }
                    if (propCls.getComponentType()!=propIndexedCls) {
                        throw new ProgrammingError("Type missmatch between array and non array access of indexed property '" + propertyName + "'");
                    }
                    if (readMethod != null && readMethod.getParameterTypes().length != 0) {
                        throw new ProgrammingError("Non indexed getter of indxed property ' " + propertyName + "' is not supposed to have a parameter!");
                    }
                    if (writeMethod != null) {
                        Class [] paramCls = writeMethod.getParameterTypes();
                        if (paramCls == null || paramCls.length != 1) {
                            throw new ProgrammingError("Non indexed setter of indexed property ' " + propertyName + "' should have 1 parameter!");
                        }
                        if (paramCls[0]!=propCls) { // SHOULD CHECK INSTANCE
                            throw new ProgrammingError("Inconstant parameter type for non indexed setter of indexed property' " + propertyName + "', type: " + propCls);
                        }
                    }
                } // non indexed access
            } else {
                if (propCls == null) {
                    throw new ProgrammingError("getPropertyType returned null for " + propertyName);
                }
                if (propCls == Void.TYPE) {
                    throw new ProgrammingError("Void property type for '" + propertyName + "' is not allowed");
                }
                if (readMethod != null && readMethod.getParameterTypes().length != 0) {
                    throw new ProgrammingError("Non indexed getter of property ' " + propertyName + "' is not supposed to have a parameter!");
                }
                if (writeMethod != null) {
                    Class [] paramCls = writeMethod.getParameterTypes();
                    if (paramCls == null || paramCls.length != 1) {
                        throw new ProgrammingError("Non indexed setter of property ' " + propertyName + "' should have 1 parameter!");
                    }
                    if (paramCls[0]!=propCls) { // SHOULD CHECK INSTANCE
                        throw new ProgrammingError("Inconstant parameter type for setter of property' " + propertyName + "', type: " + propCls);
                    }
                }
            }
            
           // Add to cache 
           if (debug) System.out.println("** property '" + propertyName + "' + found, add to cache");
           if (beanProperties==null) {
                beanProperties = new Hashtable();
            }
            beanProperties.put(propertyName, descriptor);
        } else {
            if (debug) System.out.println("** No method named '" + propertyName + "' found");
        }
        return descriptor;
   }
   

    /**
     * Get the list of public method in this class or superclass, by name (the
     * methods may be static or instance!).
     *
     * @param   functionName  The nam of the method being looked up
     * @param   cls  The class of the method being looked up
     * @return  The method array or null if none found or in case of error  
     */
   synchronized public static Method[] lookupPublicMethod(String functionName, Class cls) throws EcmaScriptException {
        ClassInfo classInfo = ClassInfo.ensureClassInfo(cls);
        return classInfo.cachedPublicMethodLookup(functionName, cls);
   }


    /**
     * Look for a method in a list of interface or all superinterfaces
     *
     * @param functionName The name of the function to search
     * @param interfaces The list of interfaces to search
     * @param paramTypes The type of parameters of the function
     *
     * @return The method if found, null otherwise
     */
    private Method getInInterfaces(String functionName, Class [] interfaces, Class[] paramTypes) {
       boolean debug = ESLoader.isDebugJavaAccess();      
 
       if (debug && interfaces.length>0) {
            System.out.println("** Looking in " + interfaces.length + " interfaces");
       }
    SEARCHININTERFACE:
       for (int ix=0; ix<interfaces.length; ix++) {
           Class theInterface=interfaces[ix];
           if (Modifier.isPublic(theInterface.getModifiers())) {
                if (debug) {
                    System.out.println("** Looking in public interface: " + theInterface);
                }
                try {
                   Method method = theInterface.getDeclaredMethod(functionName,paramTypes);
                   if (Modifier.isPublic(method.getModifiers())) {
                       if (debug) {
                           System.out.println("** Public method found: " + functionName);
                       }
                       return method;
                   }
                } catch (NoSuchMethodException e) {
                    if (debug) {
                         System.out.println("** The method has no public declaration in the interface: "+ functionName);
                    }
                    // throw new ProgrammingError("The method has no public declaration in a public class: "+ functionName);
                } catch (SecurityException e) {
                    throw new ProgrammingError("Access error inspecting method "+ functionName + ": " + e);
                }
            } else {
                if (debug) {
                           System.out.println("** Interface " + theInterface + " is not public - not searching for method");
                }
            }
            
            // Not found, try super interfaces
            Class [] superInterfaces = theInterface.getInterfaces();
            Method method = getInInterfaces(functionName, superInterfaces, paramTypes);
            if (method!=null) {
               if (debug) System.out.println("** Method found in super interfaces");
               return method;
            }
        }
        
        if (debug) {
           System.out.println("** No method found in interface and super interfaces");
        }
        return null;
  }

    /**
     * Get a public method list, where method matched in name and class.
     * The method list is cached in this ClassInfo object (which must be
     * for the specified class).
     * <P>Only the public variant of a method is returned.
     * <P>If a method was not found, the cache is not modified, and a new 
     * slow lookup will be done on next search. The case is rare enought
     * that it is better to keep the code simpler.
     *
     * @param   functionName  The name of the function
     * @param   cls  The class in which the function is defined
     * @return   The list of methods or null in case of error or if none found.
     */
   private Method [] cachedPublicMethodLookup(String functionName, Class cls) throws EcmaScriptException {
        boolean debug = ESLoader.isDebugJavaAccess();
        if (publicMethods != null) {
           if (debug) System.out.println("** Method descriptor for class '" +
                     cls + "' found in cache");
            Method [] methods = (Method []) publicMethods.get(functionName);
            if (methods!= null) {
                if (debug) System.out.println("** " + methods.length +
                        " method(s) named '" + functionName + "' found in cache");
                return methods;
            }
        } 
        // Not in cache, find if any matching the same name can be found
         if (debug) System.out.println("** No method named '" +
                      functionName + "' found in class cache, lookup started");
        Method [] allMethods = cls.getMethods();
        Vector methodVector = new Vector(allMethods.length);
        boolean wasFound = false;
        for (int i=0; i<allMethods.length; i++) {
            Method method = allMethods[i];
            if (debug) System.out.println("** Method examined: " + method.toString());
            if (!method.getName().equals(functionName)) continue;
            // Method has same name, some closer examination is needed:
            // If the class itself is not public, there is an access error if
            // the method is invoked on it - the identical method in the
            // superclass must be searched, to be used in the invocation.
            // I am not too sure of what happens if the method is defined in an
            // interface...
            if (!Modifier.isStatic(cls.getModifiers()) && !Modifier.isPublic(cls.getModifiers())) {
               if (debug) System.out.println("** Class " + cls + 
                           " is not public, examining superclasses and interfaces to find proper method descriptor");

            Class[] paramTypes = method.getParameterTypes();
            SEARCHPUBLIC:
               for (Class theClass=cls;theClass!=null;theClass=theClass.getSuperclass()) {
           
                   // Look in interfaces first 
                   Class [] interfaces = cls.getInterfaces();
                   Method m = getInInterfaces(functionName, interfaces, paramTypes);
                   if (m != null) {
                       method = m;
                       wasFound = true;
                       break SEARCHPUBLIC;
                   }
                   
                   // Look in the class
                   if (Modifier.isPublic(theClass.getModifiers())) {
                       if (debug) {
                           System.out.println("** Looking in public class: " + theClass);
                        }
                        try {
                           m = theClass.getDeclaredMethod(functionName,paramTypes);
                           if (Modifier.isPublic(method.getModifiers())) {
                               if (debug) {
                                   System.out.println("** Public method found: " + functionName);
                               }
                               method = m;
                               wasFound = true;
                               break SEARCHPUBLIC;
                           }
                        } catch (NoSuchMethodException e) {
                            if (debug) {
                                 System.out.println("** The method has no public declaration in the public class: "+ functionName);
                            }
                            // throw new ProgrammingError("The method has no public declaration in a public class: "+ functionName);
                        } catch (SecurityException e) {
                            throw new ProgrammingError("Access error inspecting method "+ functionName + ": " + e);
                        }
                    } else {
                        if (debug) {
                           System.out.println("** Class " + theClass + " is not public - not searching for method");
                        }
                    }
                    
               } // for SEARCHPUBLIC
               
               if (!wasFound) {
                    throw new EcmaScriptException("The method '" + functionName + "' has no public declaration in a public class or public interface ");
               }
               
            } // if class not public
            if (Modifier.isPublic (method.getModifiers ()))
                method.setAccessible (true);
            //  save it
            methodVector.addElement(method);
        }
        // If we have some, cache them
        Method [] methods = null;
        int nmbMethods = methodVector.size();
        if (nmbMethods>0) {
           if (debug) System.out.println("** " + nmbMethods + " methods named: '" + functionName + "' + found, add to class cache");
            methods = new Method[nmbMethods];
            methodVector.copyInto(methods);
            if (publicMethods==null) {
                publicMethods = new Hashtable();
            }
            publicMethods.put(functionName, methods);
        } else {
            if (debug) System.out.println("** No method named '" + functionName + "' found");
        }
        return methods;
    }
    
    
    /**
     * Get the list of public bean method in this bean, by name (the
     * methods may be static or instance!).
     *
     * @param   functionName  The nam of the method being looked up
     * @param   cls  The class of the method being looked up
     * @return  The method array or null if none found or in case of error  
     */
   synchronized public static Method[] lookupBeanMethod(String functionName, Class cls) {
        ClassInfo classInfo = ClassInfo.ensureClassInfo(cls);
        return classInfo.cachedBeanMethodLookup(functionName, cls);
   }


    /**
     * Get a bean method list, where method matched in name and class.
     * The method lists a cached in this ClassInfo object (which must be
     * for the specified class).
     * <P>Only the public variant of a method is returned.
     * <P>If a method was not found, the cache is not modified, and a new 
     * slow lookup will be done on next search. The case is rare enought
     * that it is better to keep the code simpler.
     *
     * @param   functionName  The name of the function
     * @param   cls  The class in which the function is defined
     * @return   The list of methods or null in case of error or if none found.
     */
   private Method [] cachedBeanMethodLookup(String functionName, Class cls) {
        boolean debug = ESLoader.isDebugJavaAccess();
        if (beanMethods != null) {
           if (debug) System.out.println("** Method descriptor for bean '" +
                     cls + "' found in cache");
            Method [] methods = (Method []) beanMethods.get(functionName);
            if (methods!= null) {
                if (debug) System.out.println("** " + methods.length +
                        " method(s) named '" + functionName + "' found in cache");
                return methods;
            }
        } 
        // Not in cache, find if any matching the same name can be found
         if (debug) System.out.println("** No method named '" +
                      functionName + "' found in bean cache, lookup started");
         
        // Do we have a cached BeanInfo ? create it if no
        if (beanInfo == null) {
            try {
               beanInfo = Introspector.getBeanInfo(cls);
           } catch (IntrospectionException e) {
               if (debug) System.out.println(" ** Error getting beaninfo: " + e);
               return null;
           }
        }

        MethodDescriptor [] allDescriptors = beanInfo.getMethodDescriptors();
        Vector methodVector = new Vector(allDescriptors.length);
        for (int i=0; i<allDescriptors.length; i++) {
            Method method = allDescriptors[i].getMethod();
            if (debug) System.out.println("** Method examined: " + method.toString());
            if (!allDescriptors[i].getName().equals(functionName)) continue;
            // Method has same name, some tuning neede:
            // If the class itself is not public, there is an access error if
            // ther method is invoked on it - the identical method in the
            // superclass must be looked at.
            // I am not too sure of what happens if the method is defined in an
            // interface...
            if (!Modifier.isStatic(cls.getModifiers()) && !Modifier.isPublic(cls.getModifiers())) {
               if (debug) System.out.println("** Bean class " + cls +
                           " is not public, examining superclasses to find proper method descriptor");
            SEARCHPUBLIC:
               for (Class theClass=cls;theClass!=null;theClass=theClass.getSuperclass()) {
                    if (Modifier.isPublic(theClass.getModifiers())) {
                       if (debug) {
                               System.out.println("** Looking in public superlass: " + theClass);
                        }
                        try {
                           Class[] paramTypes = method.getParameterTypes();
                           Method m = theClass.getDeclaredMethod(functionName,paramTypes);
                           if (Modifier.isPublic(method.getModifiers())) {
                               if (debug) {
                                   System.out.println("** Public method found: " + functionName);
                               }
                               method = m;
                               break SEARCHPUBLIC;
                           }
                        } catch (NoSuchMethodException e) {
                            throw new ProgrammingError("Error inspecting method "+ functionName + ": " + e);
                        } catch (SecurityException e) {
                            throw new ProgrammingError("Acess error inspecting method "+ functionName + ": " + e);
                        }
                    } else {
                        if (debug) {
                           System.out.println("** Superlass " + theClass + " is not public");
                        }
                    }
               } // for
            } // if class not public
            if (Modifier.isPublic (method.getModifiers ()))
                method.setAccessible (true);
            //  save it
            methodVector.addElement(method);
        }
        // If we have some, cache them
        Method [] methods = null;
        int nmbMethods = methodVector.size();
        if (nmbMethods>0) {
           if (debug) System.out.println("** " + nmbMethods + " methods named: '"
                                       + functionName + "' + found, add to bean cache");
            methods = new Method[nmbMethods];
            methodVector.copyInto(methods);
            if (beanMethods==null) {
                beanMethods = new Hashtable();
            }
            beanMethods.put(functionName, methods);
        } else {
            if (debug) System.out.println("** No bean method named: '" +
                                            functionName + "' + found");
        }
        return methods;
    }

}
