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
    private ZipRepository repository;
    private String name;
    private String shortName;
    private String baseName;

    protected ZipResource(File zipfile, ZipEntry zipentry, ZipRepository repository) {
        this.zipentry = zipentry;
        this.zipfile = zipfile;
        this.repository = repository;

        String entryname = zipentry.getName();
        int lastSlash = entryname.lastIndexOf('/');

        shortName = entryname.substring(lastSlash + 1);
        name = new StringBuffer(repository.getName()).append('/')
                .append(shortName).toString();

        // base name is short name with extension cut off
        int lastDot = shortName.lastIndexOf(".");
        baseName = (lastDot == -1) ? shortName : shortName.substring(0, lastDot);
    }

    public long lastModified() {
        return zipfile.lastModified();
    }

    public InputStream getInputStream() throws IOException {
        ZipFile zipfile = null;
        try {
            zipfile = repository.getZipFile();
            int size = (int) zipentry.getSize();
            byte[] buf = new byte[size];
            InputStream in = zipfile.getInputStream(zipentry);
            int read = 0;
            while (read < size) {
                int r = in.read(buf, read, size-read);
                if (r == -1)
                    break;
                read += r;
            }
            in.close();
            return new ByteArrayInputStream(buf);
        } finally {
            zipfile.close();
        }
    }

    public boolean exists() {
        ZipFile zipfile = null;
        try {
            zipfile = repository.getZipFile();
            return (zipfile.getEntry(zipentry.getName()) != null);
        } catch (Exception ex) {
            return false;
        } finally {
            try {
                zipfile.close();
            } catch (Exception ex) {}
        }
    }

    public String getContent() throws IOException {
        ZipFile zipfile = null;
        try {
            zipfile = repository.getZipFile();
            InputStreamReader in = new InputStreamReader(zipfile.getInputStream(zipentry));
            int size = (int) zipentry.getSize();
            char[] buf = new char[size];
            int read = 0;
            while (read < size) {
                int r = in.read(buf, read, size-read);
                if (r == -1)
                    break;
                read += r;
            }
            in.close();
            return new String(buf);
        } finally {
            zipfile.close();
        }
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public String getBaseName() {
        return baseName;
    }

    public URL getUrl() {
        // TODO: we might want to return a Jar URL
        // http://java.sun.com/j2se/1.5.0/docs/api/java/net/JarURLConnection.html
        throw new UnsupportedOperationException("getUrl() not implemented for ZipResource");
    }

    public long getLength() {
        return zipentry.getSize();
    }

    public Repository getRepository() {
        return repository;
    }

    public int hashCode() {
        return 17 + name.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof ZipResource && name.equals(((ZipResource) obj).name);
    }

    public String toString() {
        return getName();
    }
}
