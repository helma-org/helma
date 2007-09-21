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

import org.apache.commons.fileupload.FileItem;

import java.io.*;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * This represents a MIME part of a HTTP file upload
 */
public class MimePart implements Serializable {
    private final String name;
    private int contentLength;
    private String contentType;
    private byte[] content;
    private Date lastModified;
    private String eTag;
    private FileItem fileItem;
    private File file;

    /**
     * Creates a new MimePart object.
     * @param name the file name
     * @param content the mime part content
     * @param contentType the content type
     */
    public MimePart(String name, byte[] content, String contentType) {
        this.name = normalizeFilename(name);
        this.content = (content == null) ? new byte[0] : content;
        this.contentType = contentType;
        contentLength = (content == null) ? 0 : content.length;
    }

    /**
     * Creates a new MimePart object from a file upload.
     * @param fileItem a commons fileupload file item
     */
    public MimePart(FileItem fileItem) {
        name = normalizeFilename(fileItem.getName());
        contentType = fileItem.getContentType();
        contentLength = (int) fileItem.getSize();
        if (fileItem.isInMemory()) {
            content = fileItem.get();
        } else {
            this.fileItem = fileItem;
        }
    }

    /**
     * @return the content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Get the number of bytes in the mime part's content
     * @return the content length
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * Get the mime part's name
     * @return the file name
     */
    public String getName() {
        return name;
    }

    /**
     * Return the content of the mime part as byte array.
     * @return the mime part content as byte array
     */
    public byte[] getContent() {
        if (content == null && (fileItem != null || file != null)) {
            loadContent();
        }
        return content;
    }

    private synchronized void loadContent() {
        content = new byte[contentLength];
        try {
            InputStream in = getInputStream();
            int read = 0;
            while (read < contentLength) {
                int r = in.read(content, read, contentLength - read);
                if (r == -1)
                    break;
                read += r;
            }
            in.close();
        } catch (Exception x) {
            System.err.println("Error in MimePart.loadContent(): " + x);
            content = new byte[0];
        }
    }

    /**
     * Return an InputStream to read the content of the mime part
     * @return an InputStream for the mime part content
     * @throws IOException an I/O related error occurred
     */
    public InputStream getInputStream() throws IOException {
        if (file != null && file.canRead()) {
            return new FileInputStream(file);
        } else if (fileItem != null) {
            return fileItem.getInputStream();
        } else if (content != null) {
            return new ByteArrayInputStream(content);
        } else {
            return null;
        }
    }

    /**
     * Return the content of the mime part as string, if its content type is
     * null, text/* or application/text. Otherwise, return null.
     *
     * @return the content of the mime part as string
     */
    public String getText() {
        if ((contentType == null) || contentType.startsWith("text/")
                                  || contentType.startsWith("application/text")) {
            String charset = getSubHeader(contentType, "charset");
            byte[] content = getContent();
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
     * Get the last modified date
     * @return the last modified date
     */
    public Date getLastModified() {
        return lastModified;
    }

    /**
     * Set the last modified date
     * @param lastModified the last modified date
     */
    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * Get the ETag of the mime part
     * @return the ETag
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Set the ETag for the mime part
     * @param eTag the ETag
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    /**
     * Write the mimepart to a directory, using its name as file name.
     *
     * @param dir the directory to write the file to
     * @return the absolute path name of the file written, or null if an error occurred
     */
    public String writeToFile(String dir) {
        return writeToFile(dir, null);
    }

    /**
     * Write the mimepart to a file.
     *
     * @param dir the directory to write the file to
     * @return the absolute path name of the file written, or null if an error occurred
     */
    public String writeToFile(String dir, String fname) {
        try {
            File base = new File(dir).getAbsoluteFile();

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

            // set instance variable to the new file
            file = new File(base, filename);

            if (fileItem != null) {
                fileItem.write(file);
                // null out fileItem, since calling write() may have moved the temp file
                fileItem = null;
            } else {
                FileOutputStream fout = new FileOutputStream(file);
                fout.write(getContent());
                fout.close();
            }
            // return absolute file path
            return file.getPath();
        } catch (Exception x) {
            System.err.println("Error in MimePart.writeToFile(): " + x);            
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
