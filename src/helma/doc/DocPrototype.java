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

package helma.doc;

import helma.framework.repository.Resource;
import helma.framework.core.Prototype;
import helma.util.ResourceProperties;

import java.io.*;
import java.util.Iterator;

/**
 * 
 */
public class DocPrototype extends DocElement {

    DocPrototype parentPrototype = null;
    Prototype proto;

    /**
     * creates a prototype based on a prototype of the application
     *
     * @param proto
     * @param parent
     */    protected DocPrototype(Prototype proto, DocElement parent) {
        super(proto.getName(), PROTOTYPE);
        this.parent = parent;
        this.proto = proto;
    }

    /**
     * checks the type.properites for _extend values and connected a possible
     * parent prototype with this prototype. this can't be successfull at construction
     * time but only -after- all prototypes are parsed and attached to a parent
     * DocApplication object.
     */
    public void checkInheritance() {
        // hopobject is the top prototype:
        if (name.equals("hopobject")) {
            return;
        }

        // check for "_extends" in the the type.properties
        String ext = proto.getTypeProperties().getProperty("_extends");

        if ((ext != null) && (parent != null)) {
            // try to get the prototype if available
            parentPrototype = (DocPrototype) parent.getChildElement("prototype_" + ext);
        }

        if ((parentPrototype == null) && (parent != null) && !name.equals("global")) {
            // if no _extend was set, get the hopobject prototype
            parentPrototype = (DocPrototype) parent.getChildElement("prototype_hopobject");
        }
    }

    /**
     * Return this prototype's parent prototype
     *
     * @return this prototype's parent prototype
     */
    public DocPrototype getParentPrototype() {
        return parentPrototype;
    }

    /**
     * runs through the prototype directory and parses all helma files
     */
    public void readFiles() throws IOException {
        children.clear();

        Iterator it = proto.getCodeResources();

        while (it.hasNext()) {
            Resource res = (Resource) it.next();

            String name = res.getShortName();
            if (getDocApplication().isExcluded(name)) {
                continue;
            }

            try {
                if (name.endsWith(".hac")) {
                    addChild(DocFunction.newAction(res, this));
                } else if (name.endsWith(".js")) {
                    DocElement[] elements = DocFunction.newFunctions(res, this);

                    for (int j = 0; j < elements.length; j++) {
                        addChild(elements[j]);
                    }
                }
            } catch (Throwable err) {
                proto.getApplication().logError("Couldn't parse file " + res, err);
            }
        }

        it = proto.getSkinResources();

        while (it.hasNext()) {
            Resource res = (Resource) it.next();

            String name = res.getShortName();
            if (getDocApplication().isExcluded(name)) {
                continue;
            }

            addChild(DocSkin.newInstance(res, this));
        }

        ResourceProperties props = proto.getTypeProperties();
        it = props.getResources();

        int index = 0;
        while (it.hasNext()) {
            Resource res = (Resource) it.next();
            if (res.exists()) {
                addChild(new DocProperties(res, props, index++, this));
            }
        }
    }
}
