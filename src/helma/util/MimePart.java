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

import java.io.*;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * This represents a MIME part of a HTTP file upload
 */
public class MimePart implements Serializable {
    public final String name;
    public int contentLength;
    public String contentType;
    private byte[] content;
    public Date lastModified;
    public String eTag;

    /**
     * Creates a new MimePart object.
     *
     * @param name ...
     * @param content ...
     * @param contentType ...
     */
    public MimePart(String name, byte[] content, String contentType) {
        this.name = normalizeFilename(name);
        this.content = (content == null) ? new byte[0] : content;
        this.contentType = contentType;
        contentLength = (content == null) ? 0 : content.length;
    }

    /**
     *
     *
     * @return ...
     */
    public String getContentType() {
        return contentType;
    }

    /**
     *
     *
     * @return ...
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return name;
    }

    /**
     *
     *
     * @return ...
     */
    public byte[] getContent() {
        return content;
    }

    /**
     *
     *
     * @return ...
     */
    public String getText() {
        if ((contentType == null) || contentType.startsWith("text/")
                                  || contentType.startsWith("application/text")) {
            String charset = getSubHeader(contentType, "charset");
            if (charset != null) {
                try {
                    return new String(content, charset);
                } catch (UnsupportedEncodingException uee) {
                    return new String(content);
                }
            } else {
                return new String(content);
            }
        } else {
            return null;
        }
    }

    /**
     *
     *
     * @param dir ...
     *
     * @return ...
     */
    public String writeToFile(String dir) {
        return writeToFile(dir, null);
    }

    /**
     *
     *
     * @param dir ...
     * @param fname ...
     *
     * @return ...
     */
    public String writeToFile(String dir, String fname) {
        try {
            File base = new File(dir);

            // make directories if they don't exist
            if (!base.exists()) {
                base.mkdirs();
            }

            String filename = name;

            if (fname != null) {
                if (fname.indexOf(".") < 0) {
                    // check if we can use extension from name
                    int ndot = (name == null) ? (-1) : name.lastIndexOf(".");

                    if (ndot > -1) {
                        filename = fname + name.substring(ndot);
                    } else {
                        filename = fname;
                    }
                } else {
                    filename = fname;
                }
            }

            File file = new File(base, filename);
            FileOutputStream fout = new FileOutputStream(file);

            fout.write(getContent());
            fout.close();

            return filename;
        } catch (Exception x) {
            return null;
        }
    }

    /**
     *  Get a sub-header from a header, e.g. the charset from
     *  <code>Content-Type: text/plain; charset="UTF-8"</code>
     */
    public static String getSubHeader(String header, String subHeaderName) {
        if (header == null) {
            return null;
        }

        StringTokenizer headerTokenizer = new StringTokenizer(header, ";");

        while (headerTokenizer.hasMoreTokens()) {
            String token = headerTokenizer.nextToken().trim();
            int i = token.indexOf("=");

            if (i > 0) {
                String hname = token.substring(0, i).trim();

                if (hname.equalsIgnoreCase(subHeaderName)) {
                    String value = token.substring(i + 1);
                    return value.replace('"', ' ').trim();
                }
            }
        }

        return null;
    }

    /**
     * Normalize a upload file name. Internet Explorer on Windows sends
     * the whole path, so we cut off everything before the actual name.
     */
    public  static String normalizeFilename(String filename) {
        if (filename == null)
            return null;
        int idx = filename.lastIndexOf('/');
        if (idx > -1)
            filename = filename.substring(idx + 1);
        idx = filename.lastIndexOf('\\');
        if (idx > -1)
            filename = filename.substring(idx + 1);
        return filename;
    }

}
