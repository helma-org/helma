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

import helma.framework.core.*;
import helma.util.Updatable;
import java.io.*;

/**
 * This represents a File containing script functions for a given class/prototype.
 */
public class FunctionFile implements Updatable {
    Prototype prototype;
    File file;
    String sourceName;
    String content;
    long lastmod;

    /**
     * Creates a new FunctionFile object.
     *
     * @param file ...
     * @param sourceName ...
     * @param proto ...
     */
    public FunctionFile(File file, String sourceName, Prototype proto) {
        this.prototype = proto;
        this.sourceName = sourceName;
        this.file = file;
        update();
    }

    /**
     *  Create a function file without a file, passing the code directly. This is used for
     *  files contained in zipped applications. The whole update mechanism is bypassed
     *  by immediately parsing the code.
     *
     * @param body ...
     * @param sourceName ...
     * @param proto ...
     */
    public FunctionFile(String body, String sourceName, Prototype proto) {
        this.prototype = proto;
        this.sourceName = sourceName;
        this.file = null;
        this.content = body;
    }

    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate() {
        return (file != null) && (lastmod != file.lastModified());
    }

    /**
     *
     */
    public void update() {
        if (file != null) {
            if (!file.exists()) {
                remove();
            } else {
                lastmod = file.lastModified();
            }
        }
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
    public String getContent() {
        return content;
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
     */
    public void remove() {
        prototype.removeFunctionFile(this);
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return sourceName;
    }
}
