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

import helma.framework.core.Prototype;
import helma.framework.repository.Resource;
import helma.framework.repository.Resource;

public class ActionResource implements ScriptingResource {

    private String name;
    private Resource resource;
    private Prototype prototype;
    private String content;
    private long lastmod;

    public ActionResource(Resource resource, Prototype prototype) {
        this.resource = resource;
        name = resource.getName();
        this.prototype = prototype;
        lastmod = resource.lastModified();
    }

    public ActionResource(String content, String name, Prototype prototype) {
        this.name = name;
        this.prototype = prototype;
        this.content = content;
    }

    public String getContent() {
        if (content != null) {
            return content;
        } else {
            return new String(resource.getContent());
        }
    }

    public String getName() {
        return name;
    }

    public String getResourceName() {
        if (resource != null) {
            return name;
        } else {
            return "UnknownInternalSource." + name;
        }
    }

    public String getFunctionName() {
        return resource.getShortName() + "_action";
    }

    public Prototype getPrototype() {
        return prototype;
    }

    public boolean needsUpdate() {
        if (resource != null) {
            if (lastmod != resource.lastModified()) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void update() {
        if (resource != null) {
            if (!resource.exists()) {
                remove();
            } else {
                lastmod = resource.lastModified();
            }
        }
    }

    public void remove() {
        prototype.removeActionResource(this);
    }

    public String toString() {
        return "ActionSource[" + name + "]";
    }

    public Resource getResource() {
        return resource;
    }

}
