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

import helma.objectmodel.INode;
import helma.framework.repository.FileResource;

import java.io.*;
import java.util.*;

/**
 * Manages skins for a Helma application
 */
public final class SkinManager implements FilenameFilter {
    Application app;
    // the skin property name to use in database skin objects
    final String skinProperty;
    // the file name extension for skin files
    final String skinExtension;

    /**
     * Creates a new SkinManager object.
     *
     * @param app ...
     */
    public SkinManager(Application app) {
        this.app = app;
        skinProperty = app.getProperty("skinProperty", "skin");
        skinExtension = ".skin";
    }

    protected Skin getSkin(Prototype proto, String skinname, Object[] skinpath) throws IOException {
        if (proto == null) {
            return null;
        }

        Skin skin = null;

        // First check if the skin has been already used within the execution of this request
        // check for skinsets set via res.skinpath property
        do {
            if (skinpath != null) {
                for (int i = 0; i < skinpath.length; i++) {
                    skin = getSkinInPath(skinpath[i], proto.getName(), skinname);

                    if (skin != null) {
                        return skin;
                    }
                }
            }

            // skin for this prototype wasn't found in the skinsets.
            // the next step is to look if it is defined as skin file in the application directory
            skin = proto.getSkin(skinname);

            if (skin != null) {
                return skin;
            }

            // still not found. See if there is a parent prototype which might define the skin.
            proto = proto.getParentPrototype();
        } while (proto != null);

        // looked every where, nothing to be found
        return null;
    }

    protected Skin getSkinInPath(Object skinset, String prototype, String skinname) throws IOException {
        if ((prototype == null) || (skinset == null)) {
            return null;
        }

        // check if the skinset object is a HopObject (db based skin)
        // or a String (file based skin)
        if (skinset instanceof INode) {
            INode n = (INode) ((INode) skinset).getChildElement(prototype);

            if (n != null) {
                n = (INode) n.getChildElement(skinname);

                if (n != null) {
                    String skin = n.getString(skinProperty);

                    if (skin != null) {
                        return new Skin(skin, app);
                    }
                }
            }
        } else {
            // Skinset is interpreted as directory name from which to
            // retrieve the skin
            StringBuffer b = new StringBuffer(skinset.toString());
            b.append(File.separatorChar).append(prototype).append(File.separatorChar)
                         .append(skinname).append(skinExtension);

            // TODO: check for lower case prototype name for backwards compat

            File f = new File(b.toString());

            if (f.exists() && f.canRead()) {
                return Skin.getSkin(new FileResource(f), app);
            }
        }

        // Inheritance is taken care of in the above getSkin method.
        // the sequence is prototype.skin-from-db, prototype.skin-from-file, parent.from-db, parent.from-file etc.
        return null;
    }

    protected Map getSkinFiles(String skinDir, Prototype proto) {
        File dir = new File(skinDir.toString(), proto.getName());
        // if directory does not exist use lower case property name
        if (!dir.isDirectory()) {
            dir = new File(skinDir.toString(), proto.getLowerCaseName());
            if (!dir.isDirectory()) {
                return null;
            }
        }

        String[] skinNames = dir.list(this);

        if ((skinNames == null) || (skinNames.length == 0)) {
            return null;
        }

        HashMap map = new HashMap();

        for (int i = 0; i < skinNames.length; i++) {
            String name = skinNames[i].substring(0, skinNames[i].length() - 5);
            File file = new File(dir, skinNames[i]);

            map.put(name, (new FileResource(file)));
        }

        return map;
    }

    /**
     * Implements java.io.FilenameFilter.accept()
     */
    public boolean accept(File d, String n) {
        return n.endsWith(skinExtension);
    }
}
