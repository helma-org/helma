// MimePartDataSource.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.util;

import javax.activation.*;
import java.io.*;

/**
 * Makes MimeParts usable as Datasources in the Java Activation Framework (JAF)
 */

public class MimePartDataSource implements DataSource {

    private MimePart part;
    private String name;

    public MimePartDataSource (MimePart part) {
        this.part = part;
        this.name = part.getName ();
    }

    public MimePartDataSource (MimePart part, String name) {
        this.part = part;
        this.name = name;
    }


    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(part.getContent ());
    }

    public OutputStream getOutputStream () throws IOException {
        throw new IOException ("Can't write to MimePart object.");
    }

    public String getContentType() {
        return part.contentType;
    }

    public String getName () {
        return name;
    }

}
