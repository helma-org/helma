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

package helma.util;

import helma.util.mime.*;
import java.io.*;
import java.util.*;

/**
 * Utility class for MIME file uploads via HTTP POST.
 */
public class FileUpload {
    public Hashtable parts;
    int maxKbytes;

    /**
     * Creates a new FileUpload object.
     */
    public FileUpload() {
        maxKbytes = 4096;
    }

    /**
     * Creates a new FileUpload object.
     *
     * @param max ...
     */
    public FileUpload(int max) {
        maxKbytes = max;
    }

    /**
     *
     *
     * @return ...
     */
    public Hashtable getParts() {
        return parts;
    }

    /**
     *
     *
     * @param is ...
     * @param contentType ...
     * @param contentLength ...
     *
     * @throws Exception ...
     * @throws MimeParserException ...
     * @throws IOException ...
     */
    public void load(InputStream is, String contentType, int contentLength, String encoding)
              throws Exception {
        parts = new Hashtable();

        String boundary = MimePart.getSubHeader(contentType, "boundary");

        if (boundary == null) {
            throw new MimeParserException("Error parsing MIME input stream.");
        }

        if ((maxKbytes > -1) && (contentLength > (maxKbytes * 1024))) {
            throw new IOException("Size of upload exceeds limit of " + maxKbytes +
                                  " kB.");
        }

        byte[] b = new byte[contentLength];
        MultipartInputStream in = new MultipartInputStream(new BufferedInputStream(is),
                                                           boundary.getBytes());

        while (in.nextInputStream()) {
            MimeParser parser = new MimeParser(in, new MimeHeadersFactory());
            MimeHeaders headers = (MimeHeaders) parser.parse();

            InputStream bodystream = parser.getInputStream();
            int read;
            int count = 0;

            while ((read = bodystream.read(b, count, 4096)) > -1) {
                count += read;

                if (count == b.length) {
                    byte[] newb = new byte[count + 4096];

                    System.arraycopy(b, 0, newb, 0, count);
                    b = newb;
                }
            }

            byte[] newb = new byte[count];

            System.arraycopy(b, 0, newb, 0, count);

            String type = headers.getValue("Content-Type");
            String disposition = headers.getValue("Content-Disposition");
            String name = MimePart.getSubHeader(disposition, "name");
            String filename = MimePart.getSubHeader(disposition, "filename");

            if (filename != null) {
                int sep = filename.lastIndexOf("\\");

                if (sep > -1) {
                    filename = filename.substring(sep + 1);
                }

                sep = filename.lastIndexOf("/");

                if (sep > -1) {
                    filename = filename.substring(sep + 1);
                }
            }

            Object existingValue = parts.get(name);
            Object newValue = null;

            if (filename != null) {
                newValue = new MimePart(filename, newb, type);
            } else {
                newValue = new String(newb, encoding);
            }

            if (existingValue == null) {
                // no previous value, just add new object
                parts.put(name, newValue);
            } else if (existingValue instanceof ArrayList) {
                // already multiple values, add to list
                ((ArrayList) existingValue).add(newValue);
            } else {
                // already one value, convert to list
                ArrayList list = new ArrayList();
                list.add(existingValue);
                list.add(newValue);
                parts.put(name, list);
            }
        }
    }
}
