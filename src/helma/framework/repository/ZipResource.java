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

package helma.framework.repository;

import java.io.*;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class ZipResource implements Resource {

    private ZipEntry zipentry;
    private File zipfile;
    private Repository repository;
    private String name;
    private String shortName;

    protected ZipResource(File zipfile, ZipEntry zipentry, ZipRepository repository) {
        this.zipentry = zipentry;
        this.zipfile = zipfile;
        this.repository = repository;

        String entryname = zipentry.getName();
        if (entryname.lastIndexOf(".") != -1 && entryname.lastIndexOf(".") > entryname.lastIndexOf("/")) {
            shortName = entryname.substring(entryname.lastIndexOf("/") + 1, entryname.lastIndexOf("."));
        } else {
            shortName = entryname.substring(entryname.lastIndexOf("/") + 1);
        }

        if (entryname.lastIndexOf(".") != -1 && entryname.lastIndexOf(".") > entryname.lastIndexOf("/")) {
            name = repository.getName() + "/" + shortName + entryname.substring(entryname.lastIndexOf("."));
        } else {
            name = repository.getName() + "/" + shortName;
        }
    }

    public long lastModified() {
        return zipfile.lastModified();
    }

    public InputStream getInputStream() {
        return new StringBufferInputStream(getContent());
    }

    public boolean exists() {
        ZipFile zip = null;
        try {
            zip = new ZipFile(zipfile);
            if (zip.getEntry(zipentry.getName()) != null) {
                return true;
            } else {
                return false;
            }
        } catch (IOException ex) {
            return false;
        }
        finally {
            try {
                zip.close();
            } catch (Exception ex) {
                return false;
            }
        }
    }

    public String getContent() {
        ZipFile zip = null;
        try {
            zip = new ZipFile(zipfile);
            InputStreamReader in = new InputStreamReader(zip.getInputStream(zipentry));
            char[] characterBuffer = new char[(int) zipentry.getSize()];
            in.read(characterBuffer);
            in.close();
            return new String(characterBuffer);
        } catch (IOException ignore) {
            return "";
        }
        finally {
            try {
                zip.close();
            } catch (IOException ex) {
                return "";
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public URL getUrl() {
        throw new java.lang.UnsupportedOperationException("ZipPointer: getUrl() is not implemented!");
    }

    public long getLength() {
        return zipentry.getSize();
    }

    public Repository getRepository() {
        return repository;
    }

}
