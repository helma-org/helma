// WebBroadcaster.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.util;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A utility hack to do "html web broadcasts".
 */
 
public class WebBroadcaster implements Runnable {

  private Vector connections;
  private ServerSocket serverSocket;
  private Thread listener;
  private boolean paranoid;
  static String lastResult = "";
  private Vector accept, deny;
  static String reloadJS = "<html>\r\n<head><SCRIPT language=JavaScript>\r\n<!--\r\nfunction reload (url) {\r\n  window.location.href=url;\r\n}\r\n//-->\r\n</SCRIPT>\r\n";
  static String scrollJS = "<SCRIPT language=JavaScript>\r\n<!--\r\nfunction scroller () {\r\n  window.scroll(1, 500000);\r\n  window.setTimeout(\"scroller()\", 100);\r\n}\r\nscroller();\r\n//-->\r\n</SCRIPT>\r\n</head>\r\n<body>\r\n";
  long time;
  int last;

  /**
   * 
   */
  public static void main (String args[]) {
    System.out.println ("Usage: java helma.util.WebBroadcaster [port]");
    int p = 8080; 
    if (args.length > 0) try {
      p = Integer.parseInt (args[0]);
    } catch (NumberFormatException nfx) {
      System.out.println ("Error parsing port number: "+args[0]);
    }
    
    try {
      WebBroadcaster server = new WebBroadcaster (p);
      // webserver.setParanoid (false);
      // webserver.acceptClient ("192.168.*.*");
      System.out.println ("started web broadcast server on port "+p);
    } catch (IOException x) {
      System.out.println ("Error creating web broadcast server: "+x);
    }

  }


  /**
   * Creates a Web server at the specified port number.
   */
  public WebBroadcaster (int port) throws IOException {
    super();
    connections = new Vector ();
    accept = new Vector ();
    deny = new Vector ();
    // make a new server socket with extra large queue size
    this.serverSocket = new ServerSocket (port, 2000);
    listener = new Thread (this);
    listener.start ();
  }


  public void broadcast (String message) {
    long start = System.currentTimeMillis ();
    int l = connections.size ();
    synchronized (this) {
      if (l != last) {
        System.out.println ("broadcasting to "+l+" clients in "+time+" millis.");
        last = l;
      }
    }
    for (int i=l-1; i>=0; i--) {
      try {
        Connection c = (Connection) connections.elementAt (i);
        c.send (message);
      } catch (Exception ignore) {}
    }
    time = System.currentTimeMillis () - start;
  }


  /**
   * Switch client filtering on/off. 
   * @see acceptClient(java.lang.String)
   * @see denyClient(java.lang.String)
   */
  public void setParanoid (boolean p) {
    paranoid = p;
  }
  

  /**
   * Add an IP address to the list of accepted clients. The parameter can contain '*' as wildcard
   * character, e.g. "192.168.*.*". You must call setParanoid(true) in order for this to have any
   * effect. 
   *
   * @see denyClient(java.lang.String)
   * @see setParanoid(boolean)
   */
  public void acceptClient (String address) throws IllegalArgumentException {
    try {
      AddressMatcher m = new AddressMatcher (address);
      accept.addElement (m);
    } catch (Exception x) {
      throw new IllegalArgumentException ("\""+address+"\" does not represent a valid IP address");
    }
  }

  /**
   * Add an IP address to the list of denied clients. The parameter can contain '*' as wildcard
   * character, e.g. "192.168.*.*". You must call setParanoid(true) in order for this to have any
   * effect. 
   *
   * @see acceptClient(java.lang.String)
   * @see setParanoid(boolean)
   */
  public void denyClient (String address) throws IllegalArgumentException {
    try {
      AddressMatcher m = new AddressMatcher (address);
      deny.addElement (m);
    } catch (Exception x) {
      throw new IllegalArgumentException ("\""+address+"\" does not represent a valid IP address");
    }
  }
  
  private boolean checkSocket (Socket s) {
    int l = deny.size ();
    byte address[] = s.getInetAddress ().getAddress ();
    for (int i=0; i<l; i++) {
      AddressMatcher match = (AddressMatcher) deny.elementAt (i);
      if (match.matches (address))
        return false;
    }
    l = accept.size ();
    for (int i=0; i<l; i++) {
      AddressMatcher match = (AddressMatcher) accept.elementAt (i);
      if (match.matches (address))
        return true;
    }
    return false;
  }

  /**
   * Listens for client requests until stopped.
   */  
  public void run() {
    Thread current = Thread.currentThread ();
    try {
      while (listener == current) {
        Socket socket = serverSocket.accept();
        try {
          // if (!paranoid || checkSocket (socket))
          new Connection (socket);
          // else 
          // socket.close ();
        } catch (Exception x) {
          System.out.println ("Error in listener: "+x);
        }
      }
    }
    catch (Exception exception) {
      System.err.println("Error accepting Web connections (" + exception + ").");
    }
    finally {
      try {
        serverSocket.close();
        serverSocket = null;
      }
      catch (IOException ignore) {}
    }
  }

class Connection implements Runnable {

  private Socket socket;
  private InputStream input;
  private BufferedWriter output;
  private Thread responder;
  
  public Connection (Socket socket) throws IOException {
    super();
    this.socket = socket;
    socket.setSoTimeout (30000);
    input  = new BufferedInputStream (socket.getInputStream());
    output = new BufferedWriter (new OutputStreamWriter(socket.getOutputStream()));
    responder = new Thread (this);
    responder.start();
  }

  /* public void run () {
    Thread current = Thread.currentThread ();
    if (current == cleaner) {
      cleanup ();
    } else if (current == responder) {
      respond ();
    }
  } */

  public void run () {

    boolean newConnection = false;

    try {

      DataInputStream reader = new DataInputStream (input);
      boolean keepalive = false;
      int cycle = 0;
      // implement keep-alive connections
      do {
        // if (cycle > 0) System.out.println ("Reusing connection: "+cycle);
        String line = reader.readLine();
        if (line == null) throw new IOException ("connection reset");
        int contentLength = 0;
        StringTokenizer tokens = new StringTokenizer(line);
        String method = tokens.nextToken();
        String uri = tokens.nextToken ();
        String httpversion = tokens.nextToken ();
        keepalive = "HTTP/1.1".equals (httpversion);
        
        do {
          // System.out.println (line);
          line = reader.readLine();
          if (line != null) {
            line = line.toLowerCase ();
            if (line.startsWith ("content-length:"))
              contentLength = Integer.parseInt (line.substring (15).trim ());
            if (line.startsWith ("connection:"))
              keepalive = line.indexOf ("keep-alive") > -1;
          }
        } while (line != null && ! line.equals(""));
        // System.out.println ("");

        if ("GET".equals (method)) {
           
           output.write (httpversion+" 200 OK\r\n");
           output.write ("Server: helma.WebBroadcast\r\n");
           output.write ("Content-Type: text/html\r\n");
           newConnection = uri.startsWith ("/NEW");
           if (!newConnection) {
             output.write ("Content-Length: 5\r\n");
             if (keepalive)
                 output.write ("Connection: keep-alive\r\n");
             output.write ("\r\n");
             output.write ("done.");
             output.flush ();
             cycle += 1;
             if (uri.startsWith ("/MSG")) 
                 broadcast (uri+"<br>\r\n");
             continue;
           } 
           
           output.write ("Connection: close\r\n");
           output.write ("\r\n");
           output.write (reloadJS);
           output.write (scrollJS);
           output.flush ();

           connections.addElement (this);
        
        } else {
           output.write ("HTTP/1.0 400 Bad Request\r\n");
           output.write ("Server: helma.WebBroadcast\r\n\r\n");
           output.write ("Bad Request.");
           // output.write (lastResult);
           output.flush ();
           keepalive = false;
        }
      } while (keepalive && !newConnection);
    } catch (Exception x) {
      System.out.print (".");
    } finally {
        if (newConnection)  // leave connection open
          return;
        try {
          output.close();
        } catch (IOException ignore) {}
        try {
          input.close();
        } catch (IOException ignore) {}
        try {
          socket.close();
        } catch (IOException ignore) {}
    }
  }
  
  public void cleanup () {
      
  }
  
  public synchronized void send (String message) {
    try {
         output.write (message);
         output.flush ();
    } catch (Exception exception) {
         try {
           connections.removeElement (this);
         } catch (Exception ignore) {}
         try {
           output.close();
         } catch (IOException ignore) {}
         try {
           input.close();
         } catch (IOException ignore) {}
         try {
           socket.close();
         } catch (IOException ignore) {}
    }
  }


  public String toString () {
      return socket.getInetAddress ().getHostName ();
  }

}


class AddressMatcher {

    int pattern[];
    
    public AddressMatcher (String address) throws Exception {
      pattern = new int[4];
      StringTokenizer st = new StringTokenizer (address, ".");
      if (st.countTokens () != 4)
        throw new Exception ("\""+address+"\" does not represent a valid IP address");
      for (int i=0; i<4; i++) {
        String next = st.nextToken ();
        if ("*".equals (next))
          pattern[i] = 256; 
        else
          pattern[i] = (byte) Integer.parseInt (next);
      }
    }
    
    public boolean matches (byte address[]) {
      for (int i=0; i<4; i++) {
        if (pattern[i] > 255) // wildcard
          continue;
        if (pattern[i] != address[i])
          return false;
      }
      return true;
    }
}

}