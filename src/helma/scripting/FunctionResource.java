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

public class FunctionResource implements ScriptingResource {

    private Resource resource;
    private Prototype prototype;
    private String content;
    private String name;
    private long lastmod;

    public FunctionResource(Resource resource, Prototype proto) {
        this.resource = resource;
        this.prototype = prototype;
        name = resource.getName();
        lastmod = resource.lastModified();
    }

    public FunctionResource(String content, String name, Prototype prototype) {
        this.content = content;
        this.prototype = prototype;
        this.name = name;
    }

    public String getContent() {
        if (content != null) {
            return content;
        } else {
            return new String(resource.getContent());
        }
    }

    public String getResourceName() {
        if (resource != null) {
            return name;
        } else {
            return "UnknownInternalSource." + name;
        }
    }

    public String getName() {
        return name;
    }

    public String toString() {
        return "FunctionFile[" + name + "]";
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
        prototype.removeFunctionResource(this);
    }

    public Resource getResource() {
        return resource;
    }

}
