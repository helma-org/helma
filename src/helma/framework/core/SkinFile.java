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

package helma.framework.core;

import helma.util.Updatable;
import java.io.*;

/**
 * This represents a File containing a Hop skin
 */
public final class SkinFile implements Updatable {
    String name;
    Prototype prototype;
    Application app;
    File file;
    Skin skin;
    long lastmod = 0;

    /**
     * Creates a new SkinFile object.
     *
     * @param file ...
     * @param name ...
     * @param proto ...
     */
    public SkinFile(File file, String name, Prototype proto) {
        this.prototype = proto;
        this.file = file;
        this.name = name;
        this.app = proto.app;
        skin = null;
    }

    /**
     * Create a skinfile without a file, passing the skin body directly. This is used for
     * Skins contained in zipped applications. The whole update mechanism is bypassed
     *  by immediately setting the skin member.
     */
    public SkinFile(String body, String name, Prototype proto) {
        this.prototype = proto;
        this.app = proto.app;
        this.name = name;
        this.file = null;
        skin = new Skin(body, app);
    }

    /**
     * Create a skinfile that doesn't belong to a prototype, or at
     * least it doesn't know about its prototype and isn't managed by the prototype.
     */
    public SkinFile(File file, String name, Application app) {
        this.app = app;
        this.file = file;
        this.name = name;
        this.prototype = null;
        skin = null;
    }

    /**
     * Tell the type manager whether we need an update. this is the case when
     * the file has been modified or deleted.
     */
    public boolean needsUpdate() {
        // if skin object is null we only need to call update if the file doesn't 
        // exist anymore, while if the skin is initialized, we'll catch both 
        // cases (file deleted and file changed) by just calling lastModified().
        return (skin != null) ? (lastmod != file.lastModified()) : (!file.exists());
    }

    /**
     *
     */
    public void update() {
        if (!file.exists()) {
            // remove skin from  prototype
            remove();
        } else {
            // we only need to update if the skin has already been initialized
            if (skin != null) {
                read();
            }
        }
    }

    private void read() {
        try {
            FileReader reader = new FileReader(file);
            char[] c = new char[(int) file.length()];
            int length = reader.read(c);

            reader.close();
            skin = new Skin(c, length, app);
        } catch (IOException x) {
            app.logEvent("Error reading Skin " + file + ": " + x);
        }

        lastmod = file.lastModified();
    }

    /**
     *
     */
    public void remove() {
        if (prototype != null) {
            prototype.removeSkinFile(this);
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
    public Skin getSkin() {
        if (skin == null) {
            read();
        }

        return skin;
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
    public String toString() {
        return "[SkinFile "+prototype.getName() + "/" + name+"]";
    }
}
