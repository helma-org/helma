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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import helma.scripting.ScriptingResource;
import helma.framework.repository.Resource;

/**
 * Represents a skin read from some resource
 */
public final class SkinResource implements ScriptingResource {

    // Cached name of the skin
    private String name;

    // Resource the skin was read from
    private Resource resource;

    // Time the skin was last modified
    private long lastmod;

    // The prototype the skin belongs to
    private Prototype prototype;

    // The appliaction the skin belongs to
    private Application app;

    // The actual skin
    private Skin skin;

    // Cached content of the skin represented
    private String content;

    /**
     * Constructs a SkinResource using the given resource to fetch the skin
     * from and belonging to the given prototype
     * @param resource resource to fetch the skin from
     * @param prototype prototype the skin belongs to
     */
    public SkinResource(Resource resource, Prototype prototype) {
        this.prototype = prototype;
        this.resource = resource;
        name = resource.getName();
        app = prototype.app;
    }

    /**
     * Constructs a SkinResource using the given name and content and belonging
     * to the given prototype
     * @param content skin content
     * @param name skin name
     * @param prototype ptototype the skin belongs to
     */
    public SkinResource(String content, String name, Prototype prototype) {
        this.prototype = prototype;
        app = prototype.app;
        this.name = name;
        skin = new Skin(content, app);
    }

    /**
     * Constructs a SkinResource using the given resource to fetch the skin from
     * and belonging to the given application
     * @param source resource to fetch the skin from
     * @param app application the ksin belongs to
     */
    public SkinResource(Resource source, Application app) {
        this.app = app;
        this.resource = resource;
        this.name = source.getName();
    }

    /**
     * Returns the full name of the skin represented
     * @return skin name
     */
    public String getResourceName() {
        if (resource != null) {
            return name;
        } else {
            return "UnknownInternalSource." + name;
        }
    }

    /**
     * Returns the name of the skin represented
     * @return skin name
     */
    public String getName() {
        return name;
    }

    /**
     * Reads the content of the skin and reconstructs the skin object
     * The application-wide character-encoding is used
     */
    private void read() {
        String encoding = app.getProperty("skinCharset");
        try {
            Reader reader;
            if (encoding == null) {
                reader = new InputStreamReader(resource.getInputStream());
            } else {
                reader = new InputStreamReader(resource.getInputStream(), encoding);
            }

            char[] characterBuffer = new char[(int) resource.getLength()];
            int length = reader.read(characterBuffer);

            reader.close();
            skin = new Skin(characterBuffer, length, app);
        } catch (IOException x) {
            app.logEvent("Error reading Skin " + resource + ": " + x);
        }

        lastmod = resource.lastModified();
    }

    /**
     * Returns the actual skin that is represented
     * @return skin represented
     */
    public Skin getSkin() {
        if (skin == null) {
            read();
        }
        return skin;
    }

    /**
     * Returns the string representation
     * @return string
     */
    public String toString() {
        return "[SkinSource " + name + "]";
    }

    /**
     * Checks wether the skin needs to be updated
     * @return true if the skin needs to be updated
     */
    public boolean needsUpdate() {
        // if skin object is null we only need to call update if the file doesn't
        // exist anymore, while if the skin is initialized, we'll catch both
        // cases (file deleted and file changed) by just calling lastModified().
        return (skin != null) ? (lastmod != resource.lastModified()) : (!resource.exists());
    }

    /**
     * Updates the skin by rereading the content of the resource
     */
    public void update() {
        if (!resource.exists()) {
            remove();
        } else {
            if (skin != null) {
                read();
            }
        }
    }

    /**
     * Removes the skin from the prototype it belongs to
     */
    public void remove() {
        if (prototype != null) {
            prototype.removeSkinResource(this);
        }
    }

    /**
     * Returns the resource the represented skin was fetched from
     * @return skin resource
     */
    public Resource getResource() {
        return resource;
    }

}
