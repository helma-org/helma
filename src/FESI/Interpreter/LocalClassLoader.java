// LocalClassLoaderjava
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

import java.io.*;
import java.util.Hashtable;
import java.util.Properties;
import java.util.zip.*;
import java.net.*;
import java.awt.*;


// import sun.awt.image.*;   // byte array image source

import FESI.Data.ESLoader;
import FESI.Exceptions.*;


/**
 * This code is largely inspired of Java Examples in a Nutshell.
 * The loaders are shared by all instance of the evaluator, they
 * are therefore synchronized when needed.
 **/

public  class LocalClassLoader extends ClassLoader {
    
    public final static String urlPrefix = "FESI";
    public final static String resourceName = "fesiresource"; // must match class name
    private static Hashtable loadersByCookie = new Hashtable(); // loaders by cookie
    private static Hashtable loadersByFilename = new Hashtable(); // loaders by file name
    private static final String protocolPathProp = "java.protocol.handler.pkgs";
    private static int cookieCount = 1;
    private static char fileSep = System.getProperty("file.separator", "/").charAt(0);

    /** This is the directory from which the classes will be loaded */
    private boolean asJar;
    private String myCookie;
    private ZipFile zipFile = null;
    private File directoryFile = null;
    
    /** The constructor.  Just initialize the directory */
    private LocalClassLoader(File dir) { 
        this.directoryFile = dir;
        this.asJar = false;
        myCookie = "fcl"+cookieCount++;
        loadersByCookie.put(myCookie, this);
        if (ESLoader.isDebugLoader()) System.out.println(" ** New class loader: " + this);
    }
    
    private LocalClassLoader(ZipFile zipFile) { 
        this.asJar = true;
        this.zipFile = zipFile;
        myCookie = "fcl"+cookieCount++;
        loadersByCookie.put(myCookie, this);
        if (ESLoader.isDebugLoader()) System.out.println(" ** New class loader: " + this);
    }
    
    /** The factory.  Make a loader if none exist for the same source */
    synchronized public static LocalClassLoader makeLocalClassLoader(String filename) throws EcmaScriptException{
        LocalClassLoader loader = null;
        boolean asJar = false;
        File file = new File(filename);
        String fullname = null;
        if (file.isFile()) {
            try {
                fullname = file.getCanonicalPath();
            } catch (IOException e) {
                fullname = file.getAbsolutePath(); // Hope this work
            }
            loader = (LocalClassLoader) loadersByFilename.get(fullname);
            if (loader == null) {
                ZipFile zipFile = null;
                try {
                     zipFile = new ZipFile(fullname); 
                } catch (IOException e) {
                     throw new EcmaScriptException("IO Error opening zip file '" + fullname + "' : " + e);
                }
                loader = new LocalClassLoader(zipFile);
                loadersByFilename.put(fullname, loader);
            } else {
                 if (ESLoader.isDebugLoader()) System.out.println(" ** loader in cache: " + loader);
            }
        } else if (file.isDirectory()) {
            try {
                fullname = file.getCanonicalPath();
            } catch (IOException e) {
                fullname = file.getAbsolutePath(); // Hope this work
            }
            loader = (LocalClassLoader) loadersByFilename.get(fullname);
            if (loader == null) {
                loader = new LocalClassLoader(file);
                loadersByFilename.put(fullname, loader);
            } else {
                 if (ESLoader.isDebugLoader()) System.out.println(" ** loader in cache: " + loader);
            }
        } else {
            throw new EcmaScriptException("No file or directory '" + filename + "' found");
        }
        return loader;
    }

    /**
     * A convenience method that calls the 2-argument form of this method 
     *
     * @param   name  The name of the class
     * @return   the loaded class  
     * @exception   ClassNotFoundException If class cannot be loaded 
     */
    public Class loadClass(String name) throws ClassNotFoundException { 
      return loadClass(name, true); 
    }
    
    /**
     * This is one abstract method of ClassLoader that all subclasses must
     * define.  Its job is to load an array of bytes from somewhere and to
     * pass them to defineClass().  If the resolve argument is true, it must
     * also call resolveClass(), which will do things like verify the presence
     * of the superclass.  Because of this second step, this method may be
     * called to load superclasses that are system classes, and it must take 
     * this into account.
     * @param  classname  The name of the class to load
     * @param resolve True if class must be resolved
     * @return   the loaded class  
     * @exception   ClassNotFoundException If class cannot be loaded 
     **/
    public Class loadClass(String classname, boolean resolve) 
      throws ClassNotFoundException {
      // if (ESLoader.isDebugLoader()) System.out.println(" ** loadClass: " + classname);
      try {
        // Our ClassLoader superclass has a built-in cache of classes it has
        // already loaded.  So, first check the cache.
        Class c = findLoadedClass(classname);
    
        // After this method loads a class, it will be called again to
        // load the superclasses.  Since these may be system classes, we've
        // got to be able to load those too.  So try to load the class as
        // a system class (i.e. from the CLASSPATH) and ignore any errors
        if (c == null) {
          try { c = findSystemClass(classname); }
          catch (Exception e) {}
        }
    
        // If the class wasn't found by either of the above attempts, then
        // try to load it from a file in (or beneath) the directory
        // specified when this ClassLoader object was created.  Form the
        // filename by replacing all dots in the class name with
        // (platform-independent) file separators and by adding the
        // ".class" extension.
        // Alternatively try to load it from the jar file which was
        // specified.
        if (c == null) {
            
            //String asEntryName = (asJar ? (classname.replace('.','/') + ".class") : (classname + ".class"));
            String asEntryName = classname.replace('.',(asJar ? '/' : fileSep)) + ".class";
            byte classbytes[] = getResourceBuffer(asEntryName);
            if (classbytes == null) {
                if (ESLoader.isDebugLoader()) System.out.println(" ** class '" + classname + "' not loaded");
                throw new ClassNotFoundException("Class '" + classname + "' not foud by " + this);
            } else {
                if (ESLoader.isDebugLoader()) System.out.println(" ** class '" + classname + "' loaded");
                // Now call an inherited method to convert those bytes into a Class
                c = defineClass(classname, classbytes, 0, classbytes.length);
            }
        }
    
        // If the resolve argument is true, call the inherited resolveClass
        // method.
        if (resolve) resolveClass(c);
    
        // And we're done.  Return the Class object we've loaded.
        return c;
      }
      // If anything goes wrong, throw a ClassNotFoundException error
      catch (Exception e) { 
        if (ESLoader.isDebugLoader()) System.out.println(" ** Error loading '" + classname + "' by loader: " + this + ", " + e);
	    throw new ClassNotFoundException(e.toString()); }
    }
    
    public URL getResource(String name) {
        if (ESLoader.isDebugLoader()) System.out.println(" ** getResource: '" + name + "' asked to: " + this);
        URL url = getSystemResource(name);
        if (url != null) {
            if (ESLoader.isDebugLoader()) System.out.println(" ** URL found in system as: " + url);
            return url;
        }
        try { 
           url = new URL(resourceName, null, "/" + urlPrefix + myCookie + "/+/" + name);
           if (ESLoader.isDebugLoader())System.out.println(" ** URL found as " + url);
        } catch (MalformedURLException e) {
           if (ESLoader.isDebugLoader()) System.out.println(" ** Bad URL " + url + " " + e);
        }
        return url;
    }

    public InputStream getResourceAsStream(String name) {
        if (ESLoader.isDebugLoader()) System.out.println(" ** getResourceAsStream: '" + name + "' asked to: " + this);
        InputStream back = getSystemResourceAsStream(name);
        if (back != null) {
            if (ESLoader.isDebugLoader()) System.out.println(" ** getResourceAsStream("+name+") is a system resource");
            return back;
        }
        return getLocalResourceAsStream(name);
    }
      
    private byte [] getResourceBuffer(String name) {
        byte buf[] = null;

        if (ESLoader.isDebugLoader()) System.out.println(" ** getResourceBuffer, resource '" + name + "'");
        
        if (asJar) {
            ZipEntry  zipEntry = zipFile.getEntry(name);
            if (zipEntry == null) {
                if (ESLoader.isDebugLoader()) System.out.println(" ** Resource '" + name + "'not found in jar by: " + this);
                return null;
            }
            try {    
                InputStream inputStream = zipFile.getInputStream(zipEntry);
                int limit = (int)zipEntry.getSize();
                buf = new byte[limit];
                
                int total = 0;
                while  (total < limit)
                {
                    int ct = inputStream.read(buf,total,limit-total);
                    total = total + ct;
                    if (ct == 0) {
                       if (ESLoader.isDebugLoader()) {
                           System.out.println(" ** Error read entry '" + name + "' in jar, loader: " + this);
                           System.out.println("Only " +
                            total + " bytes out of " + limit + " read from entry '" +
                            name + "' in jar '" + zipFile.getName() +"'");
                        }
                        throw new IOException ("Only " +
                            total + " bytes out of " + limit + " read from entry '" +
                            name + "' in jar '" + zipFile.getName() +"'");
                    } 
                }
                inputStream.close();
            } catch (IOException e) {
                if (ESLoader.isDebugLoader()) System.out.println(" ** Error reading jar: " + e);
                return null;
            }
            
        } else {
            
            try {    
                // Create a File object.  Interpret the filename relative to the
                // directory specified for this ClassLoader.
                File f = new File(directoryFile, name);
                
                // Read from file
                // Get the length of the class file, allocate an array of bytes for
                // it, and read it in all at once.
                int length = (int) f.length();
                buf = new byte[length];
                DataInputStream in = new DataInputStream(new FileInputStream(f));
                in.readFully(buf);
                in.close();
            } catch (IOException e) {
                if (ESLoader.isDebugLoader()) System.out.println(" ** Error reading file: " + e);
                return null;
            }
        }
        
        return buf;
    }  
    
    private Object getLocalResource(String name) {
        if (ESLoader.isDebugLoader()) System.out.println(" ** getLocalResource, resource '" + name + "' asked to: " + this);
        
        byte buf[] = getResourceBuffer(name);
        if (buf==null) return null;
        
        if (name.endsWith(".gif") || name.endsWith(".jpeg")) {
            Image image = Toolkit.getDefaultToolkit().createImage(buf);
            if (ESLoader.isDebugLoader()) System.out.println(" ** Returning image resource: " + image);
            return image;
            // return new ByteArrayImageSource(buf); // SUN specific method
        } else {
            ByteArrayInputStream s = new ByteArrayInputStream(buf);
            if (ESLoader.isDebugLoader()) System.out.println(" ** Returning stream resource: " + s);
            return s;
        }
    }

    private InputStream getLocalResourceAsStream(String name) {
        if (ESLoader.isDebugLoader()) System.out.println(" ** getLocalResourceAsStream,  resource '" + name + "' asked to: " + this);
        if (asJar) {
            int limit;
            
            try
            {
                ZipEntry  zipEntry = zipFile.getEntry(name);
        
                if (zipEntry != null) {
                    InputStream inputStream = zipFile.getInputStream(zipEntry);
                    if (ESLoader.isDebugLoader()) System.out.println(" ** Resource found, returned as stream: " + inputStream);
                    return inputStream;
                }
            } catch (Exception e) {
                if (ESLoader.isDebugLoader()) System.out.println(" ** Exception when loading resource: " + name + 
                    ": " + e);
            }
        } else {
   
          // Create a File object.  Interpret the filename relative to the
          // directory specified for this ClassLoader.
          File f = new File(directoryFile, name);
    
          // Get the stream of this file.
          try {
             InputStream inputStream = new FileInputStream(f);
             if (ESLoader.isDebugLoader()) System.out.println(" ** Resource found, returned as stream: " + inputStream);
             return inputStream;
          } catch (IOException e) {
                if (ESLoader.isDebugLoader()) System.out.println(" ** Exception when loading resource: " + name + 
                    ": " + e);
          }
        }
        if (ESLoader.isDebugLoader()) System.out.println(" ** Resource not found: " + name);
        return null;
    }

    public static InputStream getLocalResourceAsStream(String cookie,
                               String name) {
        if (ESLoader.isDebugLoader()) System.out.println(" ** static getLocalResourceAsStream, cookie: " + cookie + ", resource: " + name);
        LocalClassLoader cl = (LocalClassLoader) loadersByCookie.get(cookie);
        if (cl == null) {
            if (ESLoader.isDebugLoader()) System.err.println(" @@ LocalClassLoader cookie: " + cookie + " NOT FOUND !");
            return null;
        }
        if (ESLoader.isDebugLoader()) System.out.println(" ** Classloader found: " + cl);
        return cl.getLocalResourceAsStream(name);
    }

    public static Object getLocalResource(String cookie,
                               String name) {
        if (ESLoader.isDebugLoader()) System.out.println(" ** static getLocalResource, cookie: " + cookie + ", resource: " + name);
        LocalClassLoader cl = (LocalClassLoader) loadersByCookie.get(cookie);
        if (cl == null) {
            if (ESLoader.isDebugLoader()) System.out.println(" @@ LocalClassLoader cookie: " + cookie + " NOT FOUND !");
            return null;
        }
        if (ESLoader.isDebugLoader()) System.out.println(" ** Classloader found: " + cl);
        return cl.getLocalResource(name);
    }

    public String toString() {
          return "LocalClassLoader["+myCookie+"]:" + 
                      (asJar ? ("JAR='" + zipFile.getName()): ("DIR='" + directoryFile)) +"'";
    }
    
    static {
        // Add this protocol type to the http properties
        Properties newP = new Properties(System.getProperties());
        newP.put(protocolPathProp,
             newP.getProperty(protocolPathProp)+"|FESI.Interpreter");
        System.setProperties(newP);
    }
}