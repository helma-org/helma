// CryptFile.java
// Copyright (c) Hannes Wallnöfer 2001

package helma.util;

import java.util.*;
import java.io.*;

/**
 *  This file authenticates against a passwd file
 */
 
public class CryptFile {

    private Properties users;
    private CryptFile parentFile;
    private File file;
    private long lastRead = 0;

    public CryptFile (File file, CryptFile parentFile) {
	this.file = file;
	this.parentFile = parentFile;
	users = new Properties ();
    }


    public boolean authenticate (String username, String pw) {
	if (file.exists () && file.lastModified () > lastRead)
	    readFile ();
	else if (!file.exists () && users.size () > 0)
	    users.clear ();
	String realpw = users.getProperty (username);
	if (realpw != null) {
	    try {
	        // check if password matches
	        // first we try with unix crypt algorithm
	        String cryptpw = Crypt.crypt (realpw, pw);
	        if (realpw.equals (cryptpw))
	            return true;
	        // then try MD5
	        if (realpw.equals (MD5Encoder.encode (pw)))
	            return true;
	    } catch (Exception x) {
	        return false;
	    }
	} else {
	    if (parentFile != null)
	        return parentFile.authenticate (username, pw);
	}
	return false;
    }

    private synchronized void readFile () {
	BufferedReader reader = null;
	users = new Properties ();
	try {
	    reader = new BufferedReader (new FileReader (file));
	    String line = reader.readLine ();
	    while (line != null) {
	        StringTokenizer st = new StringTokenizer (line, ":");
	        if (st.countTokens () > 1)
	            users.put (st.nextToken (), st.nextToken ());
	        line = reader.readLine ();
	    }
	} catch (Exception ignore) {
	} finally {
	    if (reader != null)
	        try { reader.close (); } catch (Exception x) {}
	    lastRead = System.currentTimeMillis ();
	}
    }

    public static void main (String args[]) {
	CryptFile cf = new CryptFile (new File ("passwd"), null);
	System.err.println (cf.authenticate ("hns", "asdf"));
    }

}
