// ESPackages.java
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

/**
 * Implements the object loader
 */
public class ESPackages extends ESLoader {

    /**
     * Create the top level package loader (object Package)
     * @param evaluator the evaluator
     */
    public ESPackages(Evaluator evaluator) {
        super(evaluator);
    }

    /**
     * Create the top level package loader (object Package)
     * @param evaluator the evaluator
     */
    public ESPackages(Evaluator evaluator, ClassLoader loader) {
        super(evaluator, loader);
    }
    
    /**
     * Create a new package loader or package prefix
     * @param packageName The extension of the package name
     * @param previousPackage Represents the higher level package names
     * @param classLoader the class loader to use for this loader
     * @param evaluator the evaluator
     */
    public ESPackages(String packageName, 
                     ESPackages previousPackage,
                     ClassLoader classLoader, 
                     Evaluator evaluator) {
        super(packageName,previousPackage,classLoader,evaluator);
    }
	
    // overrides
    public ESObject getPrototype() {
        throw new ProgrammingError("Cannot get prototype of Package");
    }
    
    // overrides
    public String getESClassName() {
        return "Packages";
    }

    // Utility routine to load a class
    private Class loadClass(String className) throws ClassNotFoundException {
        if (classLoader == null) {
            return Class.forName(className);
        } else {
            return classLoader.loadClass(className); // use our own class loader
        }
    }

    // overrides
    // Getting a property dynamically creates a new Package prefix object
    // If the resulting name represents a class then the class object is created
    // and returned (and will be used for example by the "new" operator).
    public ESValue getProperty(String propertyName, int hash) 
                            throws EcmaScriptException {
        ESValue value = (ESValue) properties.get(propertyName, hash);
        if (value == null) {
           String packageName = buildPrefix();
           String fullName = (packageName == null) ?  propertyName : (packageName + "." + propertyName);
           try {
               Class cls = loadClass(fullName);
               if (debugJavaAccess) {
                    System.out.println("** Class '" + fullName + "' loaded");
               }
               value = new ESWrapper(cls, evaluator);
           } catch (ClassNotFoundException e) {
               if (debugJavaAccess) {
                   System.out.println("** Could not load '" + fullName +
                                       "' by " + this);
                   System.out.println("** Exception: " + e);
               }
               value = new ESPackages(propertyName, this, classLoader, evaluator);
           }
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
             // This is the Package object
             if (arguments.length<1) {
                 throw new EcmaScriptException("Missing class directory or file name");
             }
             String directoryOrJar = arguments[0].toString();
             ClassLoader classLoader =     
                     LocalClassLoader.makeLocalClassLoader(directoryOrJar);
              return new ESPackages(null, null, classLoader, evaluator);
         } else {
             throw new EcmaScriptException("Java class not found: '" + buildPrefix() +"'");
         }    
    }    
        
    // overrides
    public String getTypeofString() {
        return "JavaPackage";
    }
  
    // overrides
    public String toDetailString() {
        return "ES:<" + getESClassName() + ":'" + buildPrefix() + "'" + 
            ((classLoader==null) ? "" : (",@" + classLoader)) + ">";
    }
    
    
    
    
}
