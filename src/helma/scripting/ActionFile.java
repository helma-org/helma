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

package helma.scripting;

import helma.framework.*;
import helma.framework.core.*;
import helma.util.Updatable;
import java.io.*;
import java.util.Iterator;
import java.util.Vector;

/**
 *  An ActionFile is a file containing function code that is exposed as a URI
 *  of objects of this class/type. It is
 *  usually represented by a file with extension .hac (hop action file)
 *  that contains the raw body of the function.
 */
public class ActionFile implements Updatable {
    String name;
    String sourceName;
    Prototype prototype;
    Application app;
    File file;
    String content;
    long lastmod;

    /**
     * Creates a new ActionFile object.
     *
     * @param file ...
     * @param name ...
     * @param proto ...
     */
    public ActionFile(File file, String name, Prototype proto) {
        this.prototype = proto;
        this.app = proto.getApplication();
        this.name = name;
        this.sourceName = file.getParentFile().getName() + "/" + file.getName();
        this.file = file;
        this.lastmod = file.lastModified();
        this.content = null;
    }

    /**
     * Creates a new ActionFile object.
     *
     * @param content ...
     * @param name ...
     * @param sourceName ...
     * @param proto ...
     */
    public ActionFile(String content, String name, String sourceName, Prototype proto) {
        this.prototype = proto;
        this.app = proto.getApplication();
        this.name = name;
        this.sourceName = sourceName;
        this.file = null;
        this.content = content;
    }

    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate() {
        return lastmod != file.lastModified();
    }

    /**
     *
     */
    public void update() {
        if (!file.exists()) {
            // remove functions declared by this from all object prototypes
            remove();
        } else {
            lastmod = file.lastModified();
        }
    }

    /**
     *
     */
    public void remove() {
        prototype.removeActionFile(this);
    }

    /**
     *
     *
     * @return ...
     */
    public File getFile() {
        return file;
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return name;
    }

    /**
     *
     *
     * @return ...
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     *
     *
     * @return ...
     *
     * @throws FileNotFoundException ...
     */
    public Reader getReader() throws FileNotFoundException {
        if (content != null) {
            return new StringReader(content);
        } else if (file.length() == 0) {
            return new StringReader(";");
        } else {
            return new FileReader(file);
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String getContent() {
        if (content != null) {
            return content;
        } else {
            try {
                FileReader reader = new FileReader(file);
                char[] cbuf = new char[(int) file.length()];

                reader.read(cbuf);
                reader.close();

                return new String(cbuf);
            } catch (Exception filex) {
                app.logEvent("Error reading " + this + ": " + filex);

                return null;
            }
        }
    }

    /**
     *
     *
     * @return ...
     */
    public String getFunctionName() {
        return name + "_action";
    }

    /**
     *
     *
     * @return ...
     */
    public Prototype getPrototype() {
        return prototype;
    }

    /**
     *
     *
     * @return ...
     */
    public Application getApplication() {
        return app;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return "ActionFile[" + sourceName + "]";
    }
}
