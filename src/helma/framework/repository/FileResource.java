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

import java.net.*;
import java.io.*;

public class FileResource implements Resource {

    File file;
    Repository repository;
    String name;
    String shortName;

    public FileResource(File file) {
        this(file, null);
    }

    protected FileResource(File file, FileRepository repository) {
        this.file = file;

        this.repository = repository;
        name = file.getAbsolutePath();
        shortName = file.getName();
        // cut off extension from short name
        if (shortName.lastIndexOf(".") > -1) {
            shortName = shortName.substring(0, shortName.lastIndexOf("."));
        }
    }

    public String getName() {
        return name;
    }

    public String getShortName() {
        return shortName;
    }

    public InputStream getInputStream() {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    public URL getUrl() {
        try {
            return new URL("file:" + file.getAbsolutePath());
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    public long lastModified() {
        return file.lastModified();
    }

    public String getContent() {
        try {
            InputStream in = getInputStream();
            byte[] byteBuffer = new byte[in.available()];

            in.read(byteBuffer);
            in.close();

            return new String(byteBuffer);
        } catch (Exception ignore) {
            return "";
        }
    }

    public long getLength() {
        return file.length();
    }

    public boolean exists() {
        return file.exists();
    }

    public Repository getRepository() {
        return repository;
    }

    public int hashCode() {
        return 17 + name.hashCode();
    }

    public boolean equals(Object obj) {
        return obj instanceof FileResource && name.equals(((FileResource)obj).name);
    }

    public String toString() {
        return getName();
    }
}
