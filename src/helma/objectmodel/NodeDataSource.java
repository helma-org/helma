// NodeDataSource.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.objectmodel;

import javax.activation.*;
import java.io.*;

/**
 * Makes Nodes usable as Datasources in the Java Activation Framework (JAF)
 */

public class NodeDataSource implements DataSource {

    private INode node;
    private String name;

    public NodeDataSource (INode node) {
        this.node = node;
        this.name = node.getName ();
    }

    public NodeDataSource (INode node, String name) {
        this.node = node;
        this.name = name;
    }


    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(node.getContent ());
    }

    public OutputStream getOutputStream () throws IOException {
        throw new IOException ("Can't write to Node object.");
    }

    public String getContentType() {
        return node.getContentType ();
    }

    public String getName()
    {
        return name;
    }

}
