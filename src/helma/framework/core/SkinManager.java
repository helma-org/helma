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

    public Skin getSkin(Prototype prototype, String skinname, Object[] skinpath)
            throws IOException {
        if (prototype == null) {
            return null;
        }

        // if name contains '#' split name into mainskin and subskin
        String subskin = null;
        int hash = skinname.indexOf('#');
        if (hash > -1) {
            subskin = skinname.substring(hash + 1);
            skinname = skinname.substring(0, hash);
        }
        return getSkin(prototype, skinname, subskin, skinpath);
    }

    public Skin getSkin(Prototype prototype, String skinname,
                        String subskin, Object[] skinpath)
            throws IOException {
        Prototype proto = prototype;

        // Loop over prototype chain and check skinpath and prototype skin resources
        while (proto != null) {
            Skin skin;
            if (skinpath != null) {
                for (int i = 0; i < skinpath.length; i++) {
                    skin = getSkinInPath(skinpath[i], proto.getName(), skinname);
                    if (skin != null) {
                        // check if skin skin contains main skin
                        if (subskin == null && skin.hasMainskin()) {
                            return skin;
                        } else if (subskin != null && skin.hasSubskin(subskin)) {
                            return skin.getSubskin(subskin);
                        }
                        String baseskin = skin.getExtends();
                        if (baseskin != null && !baseskin.equals(skinname)) {
                            return getSkin(prototype, baseskin, subskin, skinpath);
                        }
                    }
                }
            }

            // skin for this prototype wasn't found in the skinsets.
            // the next step is to look if it is defined as skin file in the application directory
            skin = proto.getSkin(prototype, skinname, subskin, skinpath);
            if (skin != null) {
                return skin;
            }

            // still not found. See if there is a parent prototype which might define the skin.
            proto = proto.getParentPrototype();
        }

        // looked every where, nothing to be found
        return null;
    }

    private Skin getSkinInPath(Object skinset, String prototype, String skinname) throws IOException {
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

    /**
     * Implements java.io.FilenameFilter.accept()
     */
    public boolean accept(File d, String n) {
        return n.endsWith(skinExtension);
    }
}
