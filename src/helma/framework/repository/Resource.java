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

import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

/**
 * Resource represents a pointer to some kind of information (code, skin, ...)
 * from which the content can be fetched
 */
public interface Resource {

    /**
     * Returns the date the resource was last modified
     * @return last modified date
     */
    public long lastModified();

    /**
     * Checks wether this resource actually (still) exists
     * @return true if the resource exists
     */
    public boolean exists();

    /**
     * Returns the lengh of the resource's content
     * @return content length
     */
    public long getLength() throws IOException;

    /**
     * Returns an input stream to the content of the resource
     * @return content input stream
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Returns the content of the resource
     * @return content
     */
    public String getContent() throws IOException;

    /**
     * Returns the name of the resource; does not include the name of the
     * repository the resource was fetched from
     * @return name of the resource
     */
    public String getName();

    /**
     * Returns the short name of the resource which is its name exclusive file
     * ending if it exists
     * @return short name of the resource
     */
    public String getShortName();

    /**
     * Returns an url to the resource if the repository of this resource is
     * able to provide urls
     * @return url to the resource
     */
    public URL getUrl() throws UnsupportedOperationException;

    /**
     * Returns the repository the resource does belong to
     * @return upper repository
     */
    public Repository getRepository();

}
