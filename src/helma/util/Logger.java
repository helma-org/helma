// Logger.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.util;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Utility class for asynchronous logging.
 */
 
public class Logger implements Runnable {

    private Thread logger;

    private Vector entries;
    private String filename;
    private String dirname;
    private File dir;
    private File currentFile;
    private PrintWriter currentWriter;
    private int fileindex = 0;
    private DecimalFormat nformat;
    private DateFormat dformat;
    private long dateLastRendered;
    private String dateCache;
    private PrintStream out = null;

    public Logger (PrintStream out) {
	dformat = DateFormat.getInstance ();
	this.out = out;
    }

    public Logger (String dirname, String filename) throws IOException {
	if (filename == null || dirname == null)
	    throw new IOException ("Logger can't use null as file or directory name");
	this.filename = filename;
	this.dirname = dirname;
	nformat = new DecimalFormat ("00000");
	dformat = DateFormat.getInstance ();
	dir = new File (dirname);
	if (!dir.exists())
	    dir.mkdirs ();
	currentFile =  new File (dir, filename+nformat.format(++fileindex)+".log");
	while (currentFile.exists())
	    currentFile =  new File (dir, filename+nformat.format(++fileindex)+".log");
	currentWriter = new PrintWriter (new FileWriter (currentFile), false);
	entries = new Vector ();
	logger = new Thread (this);
	// logger.setPriority (Thread.MIN_PRIORITY+2);
	logger.start ();
    }

    public void log (String msg) {
	// it's enough to render the date every 15 seconds
	if (System.currentTimeMillis () - 15000 > dateLastRendered)
	    renderDate ();
    	// log directly to printstream or to buffer?
    	if (out == null)
	    entries.addElement (dateCache + " " + msg);
	else
	    out.println (dateCache + " " + msg);
    }

    private void renderDate () {
	dateCache = dformat.format (new Date());
	dateLastRendered = System.currentTimeMillis ();
    }

    public void run () {
	while (Thread.currentThread () == logger) {
	    try {
	        if (currentFile.length() > 10000000) {
	            // rotate log files each 10 megs
	            swapFile ();
	        }
	        int l = entries.size();
	        for (int i=0; i<l; i++) {
	            Object entry = entries.elementAt (0);
	            entries.removeElementAt (0);
	            currentWriter.println (entry.toString());
	        }
	        currentWriter.flush ();
	        logger.sleep (1000l);
	    } catch (InterruptedException ir) {
	        Thread.currentThread().interrupt ();
	    }
	}
    }

    private void swapFile () {
    	try {
    	    currentWriter.close();
	    currentFile =  new File (dir, filename+nformat.format(++fileindex)+".log");
	    currentWriter = new PrintWriter (new FileWriter (currentFile), false);
	} catch (IOException iox) {
	    System.err.println ("Error swapping Log files: "+iox);
	}
    }

}
