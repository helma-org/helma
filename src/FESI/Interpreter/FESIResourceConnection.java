// FESIResourceConnection.java
// FESI Copyright (c) Jean-Marc Lugrin, 1999
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2 of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package FESI.Interpreter;

import FESI.Data.ESLoader;

import java.net.*;
import java.io.*;

public class FESIResourceConnection extends URLConnection {
    private static boolean debug = false;

    private Object resource;    // the resource we are fetching
    private String cookie;    // identification of the loader instance to use
    private String name;    // name of the resource
    private final String prefix = LocalClassLoader.urlPrefix;
    private final int prefixLength = prefix.length();

    public FESIResourceConnection (URL url)
        throws MalformedURLException, IOException
    {
        super(url);
        if (ESLoader.isDebugLoader()) System.out.println(" ** new FESIResourceConnection('"+url+"')");
        String file = url.getFile();
        if (file.startsWith("/")) {
            file = file.substring(1);
        }
        if (! file.startsWith(prefix)) {
            throw new MalformedURLException("FESIResource file should start with /" + prefix);
        }
        cookie = file.substring(prefixLength, file.indexOf("/+/"));
        name = file.substring(file.indexOf("/+/")+3);
        if (ESLoader.isDebugLoader()) System.out.println(" ** cookie: " + cookie + ", name: " + name);    
    }

    public void connect() throws IOException {
        if (ESLoader.isDebugLoader()) System.out.println(" ** Connect: cookie: " + cookie + ", name: " + name);    
        Object o = LocalClassLoader.getLocalResource(cookie, name);
        if (o == null) {
            resource = null;
            return;
        } else {
            resource = o;
        }
    }

    public Object getContent() throws IOException {
        if (!connected) {
            connect();
        }
        return resource;
    }

    public InputStream getInputStream() throws IOException {
        if (!connected) {
            connect();
        }
        if (resource instanceof InputStream) {
            return (InputStream) resource;
        }
        return LocalClassLoader.getLocalResourceAsStream(cookie, name);
    }
}