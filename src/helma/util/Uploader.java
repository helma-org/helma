// Uploader.java
// Copyright (c) Hannes Wallnöfer 1996-2000

package helma.util;

import helma.mime.*;
import java.io.*;
import java.util.*;

/**
 * Utility class for file uploads via HTTP POST.
 */
 
public class Uploader {

    public Hashtable parts;
    int maxKbytes;

    public Uploader () {
	maxKbytes = 500;
    }

    public Uploader (int max) {
	maxKbytes = max;
    }

    public Hashtable getParts () {
	return parts;
    }


    public void load (InputStream is, String contentType, int contentLength) throws Exception {

	parts = new Hashtable ();

	String boundary = getSubHeader (contentType, "boundary");
	if (boundary == null) 
	    throw new MimeParserException ("Error parsing MIME input stream.");
	if (maxKbytes > -1 && contentLength > maxKbytes*1024)
	    throw new IOException ("Size of upload exceeds limit of " + maxKbytes + " kB.");

	byte b[] = new byte[contentLength];
	MultipartInputStream in = new MultipartInputStream (new BufferedInputStream (is), boundary.getBytes ());

	while (in.nextInputStream ()) {

	    MimeParser parser = new MimeParser (in, new MimeHeadersFactory ());
	    MimeHeaders headers = (MimeHeaders) parser.parse ();

	    InputStream bodystream = parser.getInputStream ();
	    int read, count = 0;
	
	    while ((read = bodystream.read (b, count, 4096)) > -1) {
	        count += read;
	        if (count == b.length) {
	            byte newb[] = new byte[count+4096];
	            System.arraycopy (b, 0, newb, 0, count);
	            b = newb;
	        }
	    }

	    byte newb[] = new byte[count];
	    System.arraycopy (b, 0, newb, 0, count);

	    String type = headers.getValue("Content-Type");
	    String disposition = headers.getValue ("Content-Disposition");
	    String name = getSubHeader (disposition, "name");
	    String filename = getSubHeader (disposition, "filename");
	    if (filename != null) {
	        int sep = filename.lastIndexOf ("\\");
	        if (sep > -1)
	            filename = filename.substring (sep+1);
	        sep = filename.lastIndexOf ("/");
	        if (sep > -1)
	            filename = filename.substring (sep+1);
	    }
	    if (filename != null) {
	        MimePart part = new MimePart (filename, newb, type);
	        parts.put (name, part);
	    } else {
	        parts.put (name, new String (newb));
	    }

	} 

    }



    private String getSubHeader (String header, String subHeaderName) {
	if (header == null) 
	    return null;
	String retval = null;
	StringTokenizer headerTokenizer = new StringTokenizer(header, ";");
	while (headerTokenizer.hasMoreTokens()) {
	    String token = headerTokenizer.nextToken().trim ();
	    int i = token.indexOf ("=");
	    if (i > 0) {
	        String hname = token.substring (0, i).trim ();
	        if (hname.equalsIgnoreCase (subHeaderName))
	            retval = token.substring (i+1).replace ('"', ' ').trim ();
	    }
	}
	return retval;
    }


}
