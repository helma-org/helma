// FileIO.java
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

package helma.scripting.rhino.extensions;


import java.io.BufferedReader;
import java.io.File;
import java.io.Reader;
import java.io.Writer;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.EOFException;
import java.io.IOException;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.FunctionObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Undefined;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
  * An EcmaScript FileIO 'File' object
  */
public class FileObject extends ScriptableObject {
    File file = null;
    Object readerWriter = null;
    boolean atEOF = false;
    String lastLine = null;
    Throwable lastError = null;

    protected FileObject() {
    }

    protected FileObject(String fileName) {
        file = new File(fileName);
    }

    protected FileObject(String pathName, String fileName) {
        file = new File(pathName, fileName);
    }

    public static FileObject fileObjCtor(Context cx, Object[] args,
                Function ctorObj, boolean inNewExpr) {
        if (args.length == 0 || args[0] == Undefined.instance) {
            throw new IllegalArgumentException("File constructor called without argument");
        }
        if (args.length < 2 || args[1] == Undefined.instance) {
            return new FileObject(args[0].toString());
        }
        return new FileObject(args[0].toString(), args[1].toString());
    }

    public static void init(Scriptable scope) {
        Method[] methods = FileObject.class.getDeclaredMethods();
        ScriptableObject proto = new FileObject();
        proto.setPrototype(getObjectPrototype(scope));
        Member ctorMember = null;
        for (int i=0; i<methods.length; i++) {
            if ("fileObjCtor".equals(methods[i].getName())) {
                ctorMember = methods[i];
                break;
            }
        }
        FunctionObject ctor = new FunctionObject("File", ctorMember, scope);
        ctor.addAsConstructor(scope, proto);
        String[] fileFuncs = {
                                "getName",
                                "getParent",
                                "isAbsolute",
                                "write",
                                "remove",
                                "list",
                                "flush",
                                "writeln",
                                "close",
                                "getPath",
                                "open",
                                "error",
                                "canRead",
                                "canWrite",
                                "exists",
                                "getAbsolutePath",
                                "getLength",
                                "isDirectory",
                                "isFile",
                                "lastModified",
                                "mkdir",
                                "renameTo",
                                "eof",
                                "isOpened",
                                "readln",
                                "clearError",
                                "readAll"
                               };
        try {
            proto.defineFunctionProperties(fileFuncs, FileObject.class, 0);
        } catch (Exception ignore) {
            System.err.println ("Error defining function properties: "+ignore);
        }
    }

    public String getClassName() {
        return "File";
    }

    public String toString() {
         if (file==null) return "<null>";
         return file.toString();
    }

    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" +
            ((file == null) ? "null" : file.toString()) + "]";
    }

    protected void setError(Throwable e) {
        lastError = e;
    }

    public boolean exists() {
        if (file == null) return false;
        return file.exists();
    }

    public boolean open() {
        if (readerWriter != null) {
            setError(new IllegalStateException("File already open"));
            return false;
        }
        if (file == null) {
            setError(new IllegalArgumentException("Uninitialized File object"));
            return false;
        }

        // We assume that the BufferedReader and PrintWriter creation
        // cannot fail except if the FileReader/FileWriter fails.
        // Otherwise we have an open file until the reader/writer
        // get garbage collected.
        try{
           if (file.exists()) {
               readerWriter = new BufferedReader(new FileReader(file));
           } else {
               readerWriter = new PrintWriter(new FileWriter(file));
           }
           return true;
       } catch (IOException e) {
           setError(e);
           return false;
       }
    }

    public boolean isOpened() {
       return (readerWriter != null);
    }

    public boolean close() {
       if (readerWriter == null)
                       return false;
       try {
          if (readerWriter instanceof Reader) {
              ((Reader) readerWriter).close();
          } else {
              ((Writer) readerWriter).close();
          }
          readerWriter = null;
          return true;
       } catch (IOException e) {
           setError(e);
           readerWriter = null;
           return false;
       }
    }
   
    public boolean write(Object what) {
        if (readerWriter == null) {
            setError(new IllegalStateException("File not opened"));
            return false;
        }
        if (! (readerWriter instanceof PrintWriter)) {
            setError(new IllegalStateException("File not opened for writing"));
            return false;
        }
        PrintWriter writer = (PrintWriter) readerWriter;
        if (what != null) {
            writer.print(what.toString());
        }
        // writer.println();
        return true;
    }

    public boolean writeln(Object what) {
        if (readerWriter == null) {
            setError(new IllegalStateException("File not opened"));
            return false;
        }
        if (! (readerWriter instanceof PrintWriter)) {
            setError(new IllegalStateException("File not opened for writing"));
            return false;
        }
        PrintWriter writer = (PrintWriter) readerWriter;
        if (what != null) {
            writer.print(what.toString());
        }
        writer.println();
        return true;
    }

    public String readln() {
        if (readerWriter == null) {
            setError(new IllegalStateException("File not opened"));
            return null;
        }
        if (! (readerWriter instanceof BufferedReader)) {
            setError(new IllegalStateException("File not opened for reading"));
            return null;
        }
        if (atEOF) {
            setError(new EOFException());
            return null;
        }
        if (lastLine!=null) {
            String line = lastLine;
            lastLine = null;
            return line;
        }
        BufferedReader reader = (BufferedReader) readerWriter;
        // Here lastLine is null, return a new line
        try {
          String line = reader.readLine();
          if (line == null) {
              atEOF = true;
              setError(new EOFException());
          }
          return line;
        } catch (IOException e) {
          setError(e);
          return null;
        }
    }

    public boolean eof() {
        if (readerWriter == null) {
            setError(new IllegalStateException("File not opened"));
            return true;
        }
        if (! (readerWriter instanceof BufferedReader)) {
            setError(new IllegalStateException("File not opened for read"));
            return true;
        }
        if (atEOF) return true;
        if (lastLine!=null) return false;
        BufferedReader reader = (BufferedReader) readerWriter;
        try {
          lastLine = reader.readLine();
          if (lastLine == null) atEOF = true;
          return atEOF;
        } catch (IOException e) {
          setError(e);
          return true;
        }
    }

    public boolean isFile() {
        if (file == null) {
            setError(new IllegalArgumentException("Uninitialized File object"));
            return false;
        }
        return file.isFile();
    }

    public boolean isDirectory() {
        if (file == null) {
            setError(new IllegalArgumentException("Uninitialized File object"));
            return false;
        }
        return file.isDirectory();
    }

    public boolean flush() {
        if (readerWriter == null) {
            setError(new IllegalStateException("File not opened"));
            return false;
        }
        if (readerWriter instanceof Writer) {
              try {
                  ((Writer) readerWriter).flush();
             } catch (IOException e) {
                 setError(e);
                 return false;
             }
        } else {
              setError(new IllegalStateException("File not opened for write"));
              return false; // not supported by reader
        }
        return true;
    }
   
   
    public long getLength() {
       if (file == null) {
           setError(new IllegalArgumentException("Uninitialized File object"));
           return -1;
       }
       return file.length();
    }
  
    public long lastModified() {
       if (file == null) {
           setError(new IllegalArgumentException("Uninitialized File object"));
           return 0L;
       }
       return file.lastModified();
    }
  
    public String error() {
      if (lastError == null) {
          return "";
      } else {
          String exceptionName = lastError.getClass().getName();
          int l = exceptionName.lastIndexOf(".");
          if (l>0) exceptionName = exceptionName.substring(l+1);
          return exceptionName +": " + lastError.getMessage();
      }
    }
   
    public void clearError() {
        lastError = null;
    }
   
    public boolean remove() {
       if (file == null) {
           setError(new IllegalArgumentException("Uninitialized File object"));
           return false;
       }
       if (readerWriter != null) {
           setError(new IllegalStateException("An openened file cannot be removed"));
           return false;
       }
       return file.delete();
    }
   
    public boolean renameTo(FileObject toFile) {
       if (file == null) {
           setError(new IllegalArgumentException("Uninitialized source File object"));
           return false;
       }
       if (toFile.file == null) {
           setError(new IllegalArgumentException("Uninitialized target File object"));
           return false;
       }
       if (readerWriter != null) {
           setError(new IllegalStateException("An openened file cannot be renamed"));
           return false;
       }
       if (toFile.readerWriter!=null) {
           setError(new IllegalStateException("You cannot rename to an openened file"));
           return false;
       }
       return file.renameTo(toFile.file);
    }
   
    public boolean canRead() {
        if (file == null) {
           setError(new IllegalArgumentException("Uninitialized File object"));
           return false;
        }
        return file.canRead();
    }
    
    public boolean canWrite() {
        if (file == null) {
           setError(new IllegalArgumentException("Uninitialized File object"));
           return false;
        }
        return file.canWrite();
    }
    
    public String getParent() {
        if (file == null) {
            setError(new IllegalArgumentException("Uninitialized File object"));
            return "";
        }
        String parent = file.getParent();
        return (parent==null ? "" : parent);
    }
    
    public String getName() {
        if (file == null) {
           setError(new IllegalArgumentException("Uninitialized File object"));
           return "";
        }
        String name = file.getName();
        return (name==null ? "" : name);
    }
    
    public String getPath() {
        if (file == null) {
           setError(new IllegalArgumentException("Uninitialized File object"));
           return "";
        }
        String path = file.getPath();
        return (path==null ? "" : path);
    }
    
    public String getAbsolutePath() {
        if (file == null) {
           setError(new IllegalArgumentException("Uninitialized File object"));
           return "";
        }
        String absolutPath = file.getAbsolutePath();
        return (absolutPath==null ? "" : absolutPath);
    }
    
    public boolean isAbsolute() {
        if (file == null) return false;
        return file.isAbsolute();
    }
    
    public boolean mkdir() {
        if (file == null) return false;
        if(readerWriter != null) return false;
        return file.mkdirs();   // Using multi directory version
    }
    
    public String [] list() {
        if (file == null) return null;
        if(readerWriter != null) return null;
        if (!file.isDirectory()) return null;
        return file.list();   
    }
    
    
    public String readAll() {
        // Open the file for readAll
        if (readerWriter != null) {
            setError(new IllegalStateException("File already open"));
            return null;
        }
        if (file == null) {
            setError(new IllegalArgumentException("Uninitialized File object"));
            return null;
        }
        try{ 
           if (file.exists()) {
               readerWriter = new BufferedReader(new FileReader(file));
           } else {
               setError(new IllegalStateException("File does not exist"));
               return null;
           }
           if(!file.isFile()) {
               setError(new IllegalStateException("File is not a regular file"));
               return null;
           }

           // read content line by line to setup properl eol
           StringBuffer buffer = new StringBuffer((int) (file.length()*1.10));
           BufferedReader reader = (BufferedReader) readerWriter;
           while (true) {
              String line = reader.readLine();
              if (line == null) {
                  break;
              }
              buffer.append(line);
              buffer.append("\n");  // EcmaScript EOL
           }

           
           // Close the file
           ((Reader) readerWriter).close();
           readerWriter = null;
           return buffer.toString();
       } catch (IOException e) {
           readerWriter = null;
           setError(e);
           return null;
       }
    }
  
} //class FileObject

