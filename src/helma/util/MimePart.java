// MimePart.java
// Copyright (c) Hannes Wallnöfer 2001

package helma.util;

import java.io.*;
import java.util.Date;

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


    public MimePart (String name, byte[] content, String contentType) {
	this.name = name;
	this.content = content == null ? new byte[0] : content;
	this.contentType = contentType;
	contentLength = content == null ? 0 : content.length;
    }

    public String getContentType () {
	return contentType;
    }

    public int getContentLength () {
	return contentLength;
    }

    public String getName () {
	return name;
    }

    public byte[] getContent () {
	return content;
    }

    public String getText () {
	if (contentType == null || contentType.startsWith ("text/")) {
	    // FIXME: check for encoding
	    return new String (content);
	} else {
	    return null;
	}
    }


    public String writeToFile (String dir) {
	return writeToFile (dir, null);
    }

    public String writeToFile (String dir, String fname) {
	try {
	    File base = new File (dir);
	    // make directories if they don't exist
	    if (!base.exists ())
	        base.mkdirs ();

	    String filename = name;
	    if (fname != null) {
	        if (fname.indexOf (".") < 0) {
	            // check if we can use extension from name
	            int ndot = name == null ? -1 : name.lastIndexOf (".");
                          if (ndot > -1)
	                filename = fname + name.substring (ndot);
	            else
	                filename = fname;
	        } else {
	            filename = fname;
	        }
	    }
	    File file = new File (base, filename);
	    FileOutputStream fout = new FileOutputStream (file);
	    fout.write (getContent ());
	    fout.close ();
	    return filename;
	} catch (Exception x) {
	    return null;
	}
    }

}
