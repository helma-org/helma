// Logger.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.util;

import java.io.*;
import java.util.*;
import java.text.*;

/**
 * Utility class for asynchronous logging.
 */
 
public final class Logger {

    // we use one static thread for all Loggers
    static Runner runner;
    // the list of active loggers
    static ArrayList loggers;

    private LinkedList entries;
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
    boolean closed = false;

    /**
     * Create a logger for a PrintStream, such as System.out.
     */
    public Logger (PrintStream out) {
	dformat = new SimpleDateFormat ("[yyyy/MM/dd HH:mm] ");
	this.out = out;
	entries = new LinkedList ();
	
	// register this instance with static logger list
	start (this);
    }

    /**
     * Create a file logger. The actual file names do have numbers appended and are
     * rotated every x bytes.
     */
    public Logger (String dirname, String filename) throws IOException {
	if (filename == null || dirname == null)
	    throw new IOException ("Logger can't use null as file or directory name");
	this.filename = filename;
	this.dirname = dirname;
	nformat = new DecimalFormat ("00000");
	dformat = new SimpleDateFormat ("[yyyy/MM/dd HH:mm] ");
	dir = new File (dirname);
	if (!dir.exists())
	    dir.mkdirs ();
	currentFile =  new File (dir, filename+nformat.format(++fileindex)+".log");
	while (currentFile.exists())
	    currentFile =  new File (dir, filename+nformat.format(++fileindex)+".log");
	currentWriter = new PrintWriter (new FileWriter (currentFile), false);
	entries = new LinkedList ();
	
	// register this instance with static logger list
	start (this);
    }

    /**
     * Append a message with to the log.
     */
    public void log (String msg) {
	// it's enough to render the date every 15 seconds
	if (System.currentTimeMillis () - 15000 > dateLastRendered)
	    renderDate ();
	entries.add (dateCache + msg);
    }

    private synchronized void renderDate () {
	dateLastRendered = System.currentTimeMillis ();
	dateCache = dformat.format (new Date());
    }

    /**
     * This is called by the runner thread to perform actual IO.
     */
    public void run () {
	if (entries.isEmpty ())
	    return;
	try {
	    if (currentFile != null && currentFile.length() > 10000000) {
	        // rotate log files each 10 megs
	        swapFile ();
	    }
	
	    int l = entries.size();
	    if (out != null) {
	        for (int i=0; i<l; i++) {
	            String entry = (String) entries.get (0);
	            entries.remove (0);
	            out.println (entry);
	        }
	    } else {
	        for (int i=0; i<l; i++) {
	            String entry = (String) entries.get (0);
	            entries.remove (0);
	            currentWriter.println (entry);
	        }
	        currentWriter.flush ();
	    }
	
	} catch (Exception x) {
	    //
	}
    }

    /**
     *  Rotata log files, closing the old file and starting a new one.
     */
    private void swapFile () {
    	try {
    	    currentWriter.close();
	    currentFile =  new File (dir, filename+nformat.format(++fileindex)+".log");
	    currentWriter = new PrintWriter (new FileWriter (currentFile), false);
	} catch (IOException iox) {
	    System.err.println ("Error swapping Log files: "+iox);
	}
    }

    /**
     * The static start class adds a log to the list of logs and starts the
     *  runner thread if necessary.
     */
    static synchronized void start (Logger log) {
	if (loggers == null) {
	    loggers = new ArrayList ();
	}
	loggers.add (log);
	if (runner == null || !runner.isAlive ()) {
	    runner = new Runner ();
	    runner.setPriority (Thread.NORM_PRIORITY-1);
	    runner.start ();
	}
    }

    /**
     * Tells a log to close down
     */
    public void close () {
	this.closed = true;
    }

    /**
     * Closes the file writer of a log
     */
    void closeFiles () {
	if (currentWriter != null) try {
	    currentWriter.close ();
	} catch (Exception ignore) {}
    }

    /**
     *  The static runner class that loops through all loggers.
     */
    static class Runner extends Thread {

	public void run () {
	    while (!isInterrupted ()) {
	        int l = loggers.size();
	        for (int i=l-1; i>=0; i--) {
	            Logger log = (Logger) loggers.get (i);
	            log.run ();
	            if (log.closed) {
	                loggers.remove (log);
	                log.closeFiles ();
	            }
	        }
	        try {
	            sleep (700);
	        } catch (InterruptedException ix) {}	
	    }
	}
	
    }


    /**
     *  test main method
     */
    public static void main (String[] args) throws IOException {
	Logger log = new Logger (".", "testlog");
	long start = System.currentTimeMillis ();
	for (int i=0; i<50000; i++)
	    log.log ("test log entry aasdfasdfasdfasdf");
	log.log ("done: "+(System.currentTimeMillis () - start));
	System.err.println (System.currentTimeMillis () - start);
	System.exit (0);
    }

}
