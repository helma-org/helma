// ESBeans.java
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

import java.util.Date;
import java.util.Enumeration;

import java.lang.reflect.*;

import java.io.*;
import java.awt.event.*;
import java.util.EventListener;
import java.util.zip.*;
import java.beans.Beans;

/**
 * Implements the beans loader
 */
public class ESBeans extends ESLoader {
    
    /**
     * Create the top level bean loader (object Bean)
     * @param evaluator the evaluator
     */
    public ESBeans(Evaluator evaluator) {
        super(evaluator);
    }
    
    /**
     * Create a new bean loader or package prefix
     * @param packageName The extension of the package name
     * @param previousPackage Represents the higher level package names
     * @param classLoader the class loader to use for this loader
     * @param evaluator the evaluator
     */
    public ESBeans(String packageName, 
                     ESBeans previousPackage,
                     ClassLoader classLoader, 
                     Evaluator evaluator) {
        super(packageName,previousPackage,classLoader,evaluator);
    }
	
    // overrides
    public ESObject getPrototype() {
        throw new ProgrammingError("Cannot get prototype of Beans");
    }
    
    // overrides
    public String getESClassName() {
        return "Beans";
    }

    // overrides
    // Getting a property dynamically creates a new Beans prefix object
    public ESValue getProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        ESValue value = (ESValue) properties.get(propertyName, hash);
        if (value == null) {
           String packageName = buildPrefix();
           value = new ESBeans(propertyName, this, classLoader, evaluator);
           properties.put(propertyName, hash, false, false, value); // Cache it for faster retrievial
       } 
       return value;
    }
    
    
    // overrides
    // Establish a bean classloader
    // The parameter is the directory or jar to load from
    public ESValue callFunction(ESObject thisObject, 
                               ESValue[] arguments) 
                                        throws EcmaScriptException {
         if (previousPackage == null && classLoader == null) {
             // This is the Beans object
             if (arguments.length<1) {
                 throw new EcmaScriptException("Missing class directory or jar file name");
             }
             String directoryOrJar = arguments[0].toString();
             ClassLoader classLoader =     
                     LocalClassLoader.makeLocalClassLoader(directoryOrJar);
              return new ESBeans(null, null, classLoader, evaluator);
         } else {
             throw new EcmaScriptException("Java class not found: '" + buildPrefix() +"'");
         }    

    }    
    
   
    // overrides
    // instantiates a bean
    public ESObject doConstruct(ESObject thisObject, 
                                ESValue[] arguments) 
                                        throws EcmaScriptException {
                                            
       String beanName = buildPrefix();
       ESObject value = null; 
       
       if (beanName == null) {
           throw new EcmaScriptException("cannot create beans without a package name");
       }
       
       try {
           Object bean = Beans.instantiate(classLoader, beanName);
           if (debugJavaAccess) {
                System.out.println(" ** Bean '" + beanName + "' created");
           }
           value = new ESWrapper(bean, evaluator, true);
       } catch (ClassNotFoundException e) {
           throw new EcmaScriptException("Bean '" + beanName + "' not found: " + e);
       } catch (IOException e) {
           throw new EcmaScriptException("IOexception loading bean '" + beanName + "': " + e);
       }
       return value;
    }    


    // overrides
    public String getTypeofString() {
        return "JavaBeans";
    }
  
    // overrides
    public String toDetailString() {
        return "ES:<" + getESClassName() + ":'" + buildPrefix() + "'" + 
            ((classLoader==null) ? "" : (",@" + classLoader)) + ">";
    }
    
    
    
    
}
