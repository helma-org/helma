// Interpret.java
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

import FESI.Parser.*;
import FESI.AST.*;
import FESI.Extensions.Extension;
import FESI.Extensions.BasicIOInterface;
// no GUI needed for Hop
// import FESI.gui.*;
import FESI.Data.*;
import FESI.Exceptions.*;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.Properties;

/**
 * A general purpose interpreter for basic command line and GUI interfaces.
 * Load common extensions and provide various useful if limited commands
 * to evaluate and test scripts.
 * <P>This class can be subclassed, however the interface for subclassing can
 * evolve over time. It may be preferable to write interpreters from scratchs
 * using this one as an example.
 */
public class Interpret { // removed implements InterpreterCommands
  
	// Most of the data is protected to allow subclassing the
	// interpreter. They should NOT be used from other classses
	// of this package (at least in FESI), as FESI can be
	// build for other interpreter environments.
  protected static final InputStream originalIn = System.in;
  protected static final PrintStream originalOut = System.out;
  protected static final PrintStream originalErr = System.err;
   
  protected InputStream inputStream = System.in;
  protected PrintStream printStream = System.out;
  protected PrintStream errorStream = System.err;
   
  private boolean versionPrinted = false;
  protected boolean interactive = false;
  protected boolean windowOnly = false;  // Use MessageBox for alert, and GUI if interactive
  protected boolean isAWT = true;        // If false, use Swing (if windowOnly)
  protected boolean anyError = false;    // Any error in a test file
  protected boolean anyMainTest = false; // any file processed by -T
  private static String eol = System.getProperty("line.separator", "\n");
  protected ESObject document = null;
  protected Evaluator evaluator = new Evaluator();
 
  protected DataInputStream lineReader = null;
	protected GuiFactory guiFactory = null;
  protected Console console = null;

  // For interactive evaluation - protected for subclass use ONLY
  protected ESValue lastResult = null; // Means last command did not evaluate anything
  protected long timeOfEval= 0;  // Valid only if lastResult is not null
  protected long startTime = 0;
  protected  ESValue theValue = ESUndefined.theUndefined;
  protected String[] esArgs = new String[0]; // Arguments after -A

  /**
   * Create and initialize the interpreter
   */
  public Interpret() {
      super();
      setupCommands();
  }

	/**
	 * Overridable error exit (can be changed by subclass)
	 * IMPLEMENTATION SHOULD NOT RETURN
	 */
	protected void errorExit() {
		System.exit(1);
	}
	
  /**
   * Exit with a status of 0 if no error was detected and 1 otherwise
   */
  public void exit() {
      System.exit(anyError ? 1 : 0);
  }
  
  /**
   * Forget any last result of a previous evaluation.
   */
  void clearLastResult() {
      lastResult = null;
  }
  
	
	/**
	 * Do any standard extension initialization. Can be overriden for
	 * specific implementation.
	 */
	protected void loadCommonExtensions() {
    try {
        evaluator.addMandatoryExtension("FESI.Extensions.JavaAccess");
    } catch (EcmaScriptException e) {
        errorStream.println("Cannot initialize JavaAccess - exiting: " + eol + e);
        e.printStackTrace();
        errorExit();
    }
    
    try {
        evaluator.addMandatoryExtension("FESI.Extensions.Database");
    } catch (EcmaScriptException e) {
        errorStream.println("Cannot initialize Database - exiting: " + eol + e);
        e.printStackTrace();
        errorExit();
    }

    // Note that we use OptinalRegExp, so it is not a problem of the ORO
	  // stuff is notpresent.
    try {
        evaluator.addMandatoryExtension("FESI.Extensions.OptionalRegExp");
    } catch (EcmaScriptException e) {
        errorStream.println("Cannot initialize OptionalRegExp - exiting: " + eol + e);
        e.printStackTrace();
        errorExit();
    }
    
	}
	
  /** 
   * Reset the system (including at initialization)
   */
  protected void reset() throws EcmaScriptException {
    
    if (windowOnly) {
        if (console != null) { 
            // A reset from an existing console
            if (isAWT) {  
                // reset stream while out of console.
                System.setIn(originalIn);
                System.setOut(originalOut);
                System.setErr(originalErr);
                inputStream = originalIn;
                printStream = originalOut;
                errorStream = originalErr;
                // The AWT window tends to be badly behaved, we can
                // reset it as well
                console.dispose();
                console = null;
            } else {
                // assume Swing
                console.clear();
            }
        }
    }
      
    // Reset the main evaluator, forgetting all globals
    evaluator.reset();
    
    if (windowOnly && isAWT) {  
        BasicIOInterface basicIOw = null;       
        try {
            basicIOw = (BasicIOInterface) evaluator.addMandatoryExtension("FESI.Extensions.BasicIOw");
        } catch (EcmaScriptException e) {
            errorStream.println("Cannot initialize BasicIOw - exiting: " + eol + e);
            e.printStackTrace();
            errorExit();
        }
        document = basicIOw.getDocument();
    } else if (windowOnly && !isAWT) {
        BasicIOInterface basicIOs = null;        
        try {
            basicIOs = (BasicIOInterface) evaluator.addMandatoryExtension("FESI.Extensions.BasicIOs");
        } catch (EcmaScriptException e) {
            errorStream.println("Cannot initialize BasicIOs - exiting: " + eol + e);
            e.printStackTrace();
            errorExit();
        }
        document = basicIOs.getDocument();
    } else {
        BasicIOInterface basicIO = null;
        try {
            basicIO = (BasicIOInterface) evaluator.addMandatoryExtension("FESI.Extensions.BasicIO");
        } catch (EcmaScriptException e) {
            errorStream.println("Cannot initialize BasicIO - exiting: " + eol + e);
            e.printStackTrace();
            errorExit();
        }
        document = basicIO.getDocument();
    }
    
   loadCommonExtensions();

    if (windowOnly && interactive) {
        if (console == null) {
        	try {
            if (isAWT) {
            	guiFactory = (GuiFactory) Class.forName("FESI.awtgui.AwtGuiFactory").newInstance();
            } else {
              guiFactory = (GuiFactory) Class.forName("FESI.swinggui.SwingGuiFactory").newInstance();
            }
           } catch (ClassNotFoundException ex) {
              errorStream.println("Cannot load GUI - exiting: " + eol + ex);
              ex.printStackTrace();
              errorExit();
           } catch (IllegalAccessException ex) {
              errorStream.println("Cannot load GUI - exiting: " + eol + ex);
              ex.printStackTrace();
              errorExit();
           } catch (InstantiationException ex) {
              errorStream.println("Cannot load GUI - exiting: " + eol + ex);
              ex.printStackTrace();
              errorExit();
        	 }
        }
    	   console = guiFactory.makeConsole(this, getTitle() ,25,80);
    	
        inputStream = console.getConsoleIn();
        printStream = console.getConsoleOut();
        errorStream = console.getConsoleOut();
        // lineReader = console.getConsoleIn();
        lineReader = new DataInputStream(inputStream);
        System.setIn(inputStream);
        System.setOut(printStream);
        System.setErr(errorStream);
    } else if (interactive) {
        // BufferedReader lineReader = new BufferedReader(new InputStreamReader(System.in));
        lineReader = new DataInputStream(inputStream);
    }

    // Wait until IO has been redirected
    try {
        evaluator.addMandatoryExtension("FESI.Extensions.FileIO");
    } catch (EcmaScriptException e) {
        errorStream.println("Cannot initialize FileIO - exiting: " + eol + e);
        e.printStackTrace();
        errorExit();
    }

    if (interactive) {
          printVersion();
    }
  }

  /**
   * Print standard version text
   */
  protected void printVersion() {
      printStream.println(Evaluator.getWelcomeText());
      versionPrinted = true;
  }
    
  /**
   * Display standard about text (for GUI)
   */
  public void displayAboutText() {
      printAbout();
  }

	/**
	 * Define the title of the window (to help implementing subclasses)
	 */
	protected String getTitle() {
		return "FESI - EcmaScript interpreter";
	}
	
  /**
   * Print standard ABOUT text
   */
  protected void printAbout() {
      printVersion();
      printStream.println();
      printStream.println("Provided 'as is', use at your own risk, no support available.");
      printStream.println("Can be freely used and redistributed as long as this notice is included.");
      printStream.println("Feedback may be sent to 'jmlugrin@worldcom.ch', but I may not");
      printStream.println("be able (or willing) to answer all mails.");
  }

  /**
   * Print a message waiting for a confirmation from the user
   */
  protected void finalMessage(String str) {
      if (windowOnly && guiFactory != null) {
          MessageBox mb = guiFactory.displayMessageBox("FESI Error", str);
          mb.waitOK();
                    
      } else {
          
         errorStream.println(str);
      }
  }
  
  /**
   * Print a short usage guide
   */
  protected void usage() {
      errorStream.println("usage: fesi [-waivD] [-e ext] [-T file] [-h file] [-f file] [-m module] [-o outfile] file -A ...");
      errorStream.println("      -w  Use message box for alert and AWT GUI if interactive");
      errorStream.println("      -s  As -w, but use Swing instead of AWT");
      errorStream.println("      -i  Start interactive read-eval-print loop");
      errorStream.println("      -v  display version even if not interactive");
      errorStream.println("      -e ext   Load the extension class ext");
      errorStream.println("      -T file   Process an estest file, exit 1 if any failure");

      errorStream.println("      -h file   Expand the script in an html file");
      errorStream.println("      -D  turnon all debug flags");
      errorStream.println("      -f file   Load and execute the specified file");
      errorStream.println("      -m module Load and execute the specified module from FESI.path");
      errorStream.println("      -o file   Redirect standard output to that file");
      errorStream.println("      file  Load file in editor or interpret it");
      errorStream.println("      --  Standard input to load (loaded last if not present)");
      errorStream.println("      -A...  Remaining arguments are for EcmaScript args[]");
      errorStream.println("  By default silently interprets stdin");
      errorStream.println();
  }
  
  /**
   * Process arguments and interpret what is required
   * MADE PUBLIC TO EASE USAGE ON THIS CLASS BY USER APPLICATIONS
   * This is not called by any other routine in FESI.
   * @param args the argument String array
   */
  public void doWork(String args[]) {
    boolean someFileLoaded = false;
 
    // first pass to handle -i, -w, -s options
  	  // Selecting the interactive mode and GUI
   OUTONE:
    for (int i=0; i<args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("-")) {
         for (int j=1; j<arg.length();j++) {
            char c = arg.charAt(j);   
            if (c=='i') {
              interactive = true;
              continue;
            } else if (c=='w') {
              windowOnly = true;
              continue;
            } else if (c=='s') {
              windowOnly = true;
              isAWT = false;
              continue;
            } else if (c=='A') {
              break OUTONE;  // After -A reserved for called program
            }
        } // for
      } // if
    }  // for
 
    try {
        reset();
    } catch (EcmaScriptException e) {
        errorStream.println("[[Error during initialization: " +e.getMessage() + "]]");
        e.printStackTrace();
        return;
    }
       
    // second pass to handle -o, -v and -D options and check other options validity
    // This pass validates the positional rules of the options
    OUTTWO: // for each argument
      for (int i=0; i<args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("-")) {
          if (arg.equals("--")) continue OUTTWO;
        INTWO:  // for each letter in - argument
          for (int j=1; j<arg.length();j++) {
              char c = arg.charAt(j);   
              if (c=='o') {
                  if (j<arg.length()-1) {
                      if (! windowOnly) usage();
                      finalMessage("-o must be last option in '-' string");
                      if (interactive) errorExit();
                      return;
                  }
                  if (i<args.length) {
                      String fileName = args[++i];
                      File outFile = new File(fileName);
                      try {
                        // USE DEPRECATED ROUTINE AS DEFAULT IO STREAMS USE DEPRECATED STREAMS
                         printStream = new PrintStream(new FileOutputStream(outFile));
                         System.setOut(printStream);
                         continue OUTTWO;
                      } catch (IOException e){
                         finalMessage("[[IO Error creating output file' " + fileName + 
                             "']]" +
                             eol + "[[IO Error: " + e.getMessage() + "]]");
                         if (interactive) errorExit();
                         return;
                      }
                  } else {
                      if (! windowOnly) usage();
                      finalMessage("-o requires a file parameter");
                      if (interactive) errorExit();
                      return;
                  }
                
              } else if (c=='i') {   // already handle
                  continue INTWO;
                  
              } else if (c=='D') {
                  ESLoader.setDebugJavaAccess(true);
                  ESLoader.setDebugLoader(true);
                  ESWrapper.setDebugEvent(true);
                  evaluator.setDebugParse(true);
                  continue INTWO;
                  
              } else if (c=='v') {
                  if (!versionPrinted) printVersion();
                  continue INTWO;
                  
              } else if (c=='w') {  // already handled
                  continue INTWO;
                  
              } else if (c=='s') {  // already handled
                  continue INTWO;
                  
              } else if (c=='e') { 
                  if (j<arg.length()-1) {
                      if (! windowOnly) usage();
                      finalMessage("-e must be last option in '-' string");
                      if (interactive) errorExit();
                      return;
                  }
                  if (i+1>=args.length) {
                      if (! windowOnly) usage();
                      finalMessage("-e requires a file parameter");
                      if (interactive) errorExit();
                      return;
                  }
                  i++;  // Skip file name
                  continue OUTTWO; 
                  
              } else if (c=='T') { 
                  if (j<arg.length()-1) {
                      if (! windowOnly) usage();
                      finalMessage("-T must be last option in '-' string");
                      if (interactive) errorExit();
                      return;
                  }
                  if (i+1>=args.length) {
                      if (! windowOnly) usage();
                      finalMessage("-T requires a file parameter");
                      if (interactive) errorExit();
                      return;
                  }
                  i++;  // Skip file name
                  continue OUTTWO; 
                  
              } else if (c=='f') { 
                  if (j<arg.length()-1) {
                      if (! windowOnly) usage();
                      finalMessage("-f must be last option in '-' string");
                      if (interactive) errorExit();
                      return;
                  }
                  if (i+1>=args.length) {
                      if (! windowOnly) usage();
                      finalMessage("-f requires a file parameter");
                      if (interactive) errorExit();
                      return;
                  }
                  i++;  // Skip file name
                  continue OUTTWO; 
                  
              } else if (c=='m') { 
                  if (j<arg.length()-1) {
                      if (! windowOnly) usage();
                      finalMessage("-m must be last option in '-' string");
                      if (interactive) errorExit();
                      return;
                  }
                  if (i+1>=args.length) {
                      if (! windowOnly) usage();
                      finalMessage("-m requires a file parameter");
                      if (interactive) errorExit();
                      return;
                  }
                  i++;  // Skip file name
                  continue OUTTWO; 

              } else if (c=='h') { 
                  if (j<arg.length()-1) {
                      if (! windowOnly) usage();
                      finalMessage("-h must be last option in '-' string");
                      if (interactive) errorExit();
                      return;
                  }
                  if (i+1>=args.length) {
                      if (! windowOnly) usage();
                      finalMessage("-h requires a file parameter");
                      if (interactive) errorExit();
                      return;
                  }
                  i++;  // Skip file name
                  continue OUTTWO; 
                  
              } else if (c=='A') { 
                  if (j<arg.length()-1) {
                      if (! windowOnly) usage();
                      finalMessage("-A must be last option in '-' string");
                      if (interactive) errorExit();
                      return;
                  }
                  i++; // skip to first args
                  int l = args.length - i;
                  esArgs = new String[l];
                  for (int k = 0; i<args.length; i++, k++) {
                      esArgs[k]=args[i];
                  }
                  break OUTTWO;

              } else {  
                  if (! windowOnly) usage();
                  finalMessage("Unrecognize option '"+ c + "' on command line");
                  if (interactive) errorExit();
                  return;
              }
          } // for
       } // if
    } // for


   // Set the args array from values after -A
   try {
       ESObject ap = evaluator.getArrayPrototype();
       ArrayPrototype argsArray = new ArrayPrototype(ap, evaluator);
       for (int i=0; i<esArgs.length; i++) {
          argsArray.putProperty(i, new ESString(esArgs[i]));   
       }
       ESObject go = evaluator.getGlobalObject();
       String ARGSstring = ("args").intern();
       go.putProperty(ARGSstring, argsArray, ARGSstring.hashCode());
    } catch (EcmaScriptException e) {
    }

    // Third pass to load files
   OUTTHREE:
    for (int i=0; i<args.length; i++) {
      String arg = args[i];
      if (arg.startsWith("-") && !args.equals("--")) {
        INTHREE:  // for each letter in - argument
          for (int j=1; j<arg.length();j++) {
              char c = arg.charAt(j);   
              if (c=='i') {  // already handled
                  continue INTHREE;
                  
              } else if (c=='v') {  // already handled
                  continue INTHREE;
                  
              } else if (c=='w') {  // already handled
                  continue INTHREE;
                  
              } else if (c=='s') {  // already handled
                  continue INTHREE;
                  
              } else if (c=='D') {  // already handled
                  continue INTHREE;
                        
              } else if (c=='o') {  // already handled
                  i++;  // skip file name
                  continue INTHREE;
                        
              } else if (c=='T') {  
                  String fileName = args[++i];
                  someFileLoaded = true; // So it exit without reading stdin not not -i
                  doTest(fileName);
                  anyMainTest = true;
                  continue OUTTHREE;
              	
              } else if (c=='e') {  
                  String extensionName = args[++i];
                  try {
                      evaluator.addMandatoryExtension(extensionName);
                  } catch (EcmaScriptException e) {
                      errorStream.println("Cannot load extension extensionName - exiting: " + eol + e);
                      errorExit();
                  }
                  continue OUTTHREE;
                        
              } else if (c=='f') {  
                  String fileName = args[++i];
                  someFileLoaded = true; // So it exit without reading stdin not not -i
                  doLoadFile(fileName);
                  continue OUTTHREE;
                        
              } else if (c=='m') {  
                  String moduleName = args[++i];
                  someFileLoaded = true; // So it exit without reading stdin not not -i
                  doLoad(moduleName);
                  continue OUTTHREE;
                        
              } else if (c=='h') {  
                  String fileName = args[++i];
                  someFileLoaded = true; // So it exit without reading stdin not not -i
                  doExpand(fileName);
                  continue OUTTHREE;
                  
              } else if (c=='A') {
                  break OUTTHREE;
              }
            

            } // for
            continue OUTTHREE;
       } // starts with -
      
      // Falls here only if args is a file name or is --
      String fileName = args[i];
      
      // Check if -- (execute code from stdin)
      if (fileName.equals("--")) {
          if (interactive) {
              errorStream.println("Argument '--' not allowed in interactive mode");
              errorExit();
          } else {
              someFileLoaded = true; // So it exits without reading stdin not not -i
              try {
                evaluator.evaluate(new BufferedReader(new InputStreamReader(System.in)),
                             null, new FileEvaluationSource("<System.in>", null),false);
              } catch (EcmaScriptException e) {
                  finalMessage(e.getMessage() + eol +
                             "[[Error loading file '" + fileName + "']]");
                 if (interactive) errorExit();
                 return;
              } 
           } 
          
      // Else a file name to edit
      } else {
          
          if (interactive && console.supportsEditing()) {
              console.createEditor(fileName);
          } else {
              doLoad(fileName); // If not interactive just load the file
          }
          
      } 
             
    } // for OUTTHREE
    
    if (interactive) {
        
        printStream.println("Interactive read eval print loop - type @help for a list of commands");
        
        try {
            printStream.print("> "); printStream.flush();
            // USE DEPRECATED ROUTINE AS DEFAULT IO STREAMS USE DEPRECATED STREAMS
            String line = lineReader.readLine();

            while(line!= null) {
                
                if (line.startsWith("@")) {
                    StringTokenizer st = new StringTokenizer(line);
                    String command = null;
                    String parameter = null;
                    command=st.nextToken().toLowerCase().trim();
                    if (st.hasMoreTokens()) {
                        parameter = st.nextToken().trim();
                    }  
                    // printStream.println("command '" + command +"'");
                    
                    if (Command.executeCommand(this, printStream, command, parameter)) break;         
                   
                } else if (!line.equals("")){  // Real evaluation
                
                    while (true) {
                        lastResult = null;
                        try {
                            startTime = System.currentTimeMillis();
                            theValue = evaluator.evaluate(line);
                            timeOfEval = System.currentTimeMillis() - startTime;
                            lastResult = theValue;
                            if (theValue != null) printStream.println("@@ Result: " + theValue.toString());
                        } catch (EcmaScriptException e) {
                            if (e.isIncomplete()) {
                               printStream.print("More> "); printStream.flush();
                              // USE DEPRECATED ROUTINE AS DEFAULT IO STREAMS USE DEPRECATED STREAMS
                               String moreLine = lineReader.readLine();
                               if (moreLine != null) {
                                   if (moreLine.startsWith("@end")) {
                                        printStream.println("[[Error: " + e.getMessage() + "]]");
                                        break; // Terminate with current error
                                   } else if (moreLine.startsWith("@")) {
                                       printStream.println("[[Only command @end allowed when reading script end]]");
                                       printStream.println("[[Command and script ignored]]");
                                       break;
                                   }
                                   line = line + eol + moreLine;
                                   // printStream.println();
                                   continue; 
                               }
                            }
                          printStream.println("[[Error: " + e.getMessage() + "]]");
                          // e.printStackTrace();
                        } catch (Exception e) {
                          printStream.println("[[**Uncatched error: " + e + "]]");
                          e.printStackTrace();
                        } 
                        break;
                    } 
                }
                printStream.print("> "); printStream.flush();
                // USE DEPRECATED ROUTINE AS DEFAULT IO STREAMS USE DEPRECATED STREAMS
                line = lineReader.readLine();
             }  
          } catch (IOException e) {
             errorStream.println("[[IO error reading line: " + e + "]]");
             errorExit();
          }
          
      } else {   // if !interactive
          if (!someFileLoaded) {  // No file on command line, load standard input in batch
              try {
                evaluator.evaluate(new BufferedReader(new InputStreamReader(System.in)), 
                            null, new FileEvaluationSource("<System.in>", null),false);
                someFileLoaded = true;
              } catch (EcmaScriptException e) {
                 finalMessage(e.getMessage() +
                     eol + "[[Error interpreting standard input]]");
                 if (interactive) errorExit();
                 return;
             }
          } 
      }
      // Normal exit - force exit in case other thread where started (awt)
      if (interactive || anyMainTest) exit();
  }

  /**
   * Display the current user directoy
   */
  protected void doPwd() {
      String ud = System.getProperty("user.dir", "");
      printStream.println("[[User directory: " + ud + "]]");
  }

  /**
   * Display the current load path
   */
  protected void doPath() {
       String pathSource = "FESI.path";
       String path = System.getProperty(pathSource, null);
       pathSource = "java.class.path";
       if (path == null) path = System.getProperty("java.class.path",null);
       if (path == null) {
           pathSource = "DEFAULT";
           path = ".";
       }
       printStream.println("[[Load path (" + pathSource + "): " + path + "]]");
  }
   
  /**
   * Do the command load, loading from a module (via a path)
   * @param moduleName module to load
   */
  protected void doLoad(String moduleName) {
      if (moduleName==null || moduleName.equals("")) {
        printStream.println("[[Module name missing for @load.]]");
        return;
      }
      try {
          try {
            document.putHiddenProperty("URL", 
                           new ESString("module://" + moduleName));
          } catch (EcmaScriptException ignore) {
          }
        printStream.println("@@ Loading module '" + moduleName + "' . . .");
        startTime = System.currentTimeMillis();
        
        theValue = evaluator.evaluateLoadModule(moduleName);        // EVALUATION
        
        timeOfEval = System.currentTimeMillis() - startTime;
        lastResult = theValue;
        if (theValue != null) printStream.println("@@ Resulting in: " + theValue);
      } catch (EcmaScriptException e) {
         printStream.println("[[Error loading the module '" + moduleName + "']]");
         printStream.println("[[Error: " + e.getMessage() + "]]");
      } catch (Exception e) {
          printStream.println("[[**Uncatched error: " + e + "]]");
          e.printStackTrace();
      } finally {
          try {
                 document.putHiddenProperty("URL", 
                        new ESString("module://<stdin>"));
          } catch (EcmaScriptException ignore) {
          }
     }
  }

  /**
   * Load a file from a specified directory and file name.
   * Called by the GUI to load a file found by the file chooser.
   * @param directoryName The name of the directory
   * @param fileName the name of the file
   */
  public void loadFile(String directoryName, String fileName) {
      File directory = new File(directoryName);
      File file = new File(directory, fileName);
       if (file.exists()) {
          try {
              try {
                document.putHiddenProperty("URL", 
                               new ESString("file://" + file.getCanonicalPath()));
              } catch (EcmaScriptException ignore) {
              } catch (IOException ignore) {
              }
              printStream.println("@@ Loading file '" + file.getPath() + "' . . .");
        
              theValue = evaluator.evaluateLoadFile(file);        // EVALUATION
              if (interactive && theValue != null) {
                   printStream.println("@@ Resulting in: " + theValue);
              }
                  
            } catch (EcmaScriptException e) {
                errorStream.println(e.getMessage() + 
                eol + "[[Error loading file' " + file.getPath() + "']]");
                return;
            } finally {
               try {
                   document.putHiddenProperty("URL", 
                                new ESString("file://<stdin>"));
               } catch (EcmaScriptException ignore) {
               }
          }
      } else {        
          errorStream.println("[[File " + file.getPath() + " not found]]");
      }
     
  }
  

  /**
   * Execute a string - return the line number of any error or 0
   * Called by the GUI to load a buffer content.
   * @param text The text to execute
   * @param source The identification of the source
   * @return the line number of any error if possible or 0
   */
  public int executeString(String text, String source) {
      try {
          try {
            document.putHiddenProperty("URL", 
                           new ESString("source://" + source));
          } catch (EcmaScriptException ignore) {
          }
          printStream.println("@@ Executing '" + source + "' . . .");
    
          theValue = evaluator.evaluate(text, source);        // EVALUATION
          if (interactive && theValue != null) {
               printStream.println("@@ Resulting in: " + theValue);
          }
              
        } catch (EcmaScriptException e) {
            errorStream.println(e.getMessage() + 
            eol + "[[Error executing '" + source + "']]");
            return e.getLineNumber();
        } finally {
           try {
               document.putHiddenProperty("URL", 
                            new ESString("file://<stdin>"));
           } catch (EcmaScriptException ignore) {
           }
      }
      return 0;
  }

  /**
   * Load a file given as a parameter
   * @param fileName the name of the file
   */
  protected void doLoadFile(String fileName) {
      // Maybe we have an absolute file name given by the interactive shell
      File file = new File(fileName);
      if (!file.exists()) {
          file = new File(fileName + ".es");
      }
      if (!file.exists()) {
          file = new File(fileName + ".esw");
      }
      if (!file.exists()) {
          file = new File(fileName + ".js");
      }
      if (file.exists()) {
          try {
              try {
                document.putHiddenProperty("URL", 
                               new ESString("file://" + file.getCanonicalPath()));
              } catch (EcmaScriptException ignore) {
              } catch (IOException ignore) {
              }
              if (interactive)
                  printStream.println("@@ Loading file '" + file.getPath() + "' . . .");
            
                  theValue = evaluator.evaluateLoadFile(file);        // EVALUATION
                  if (interactive && theValue != null) {
                       printStream.println("@@ Resulting in: " + theValue);
                  }
                  
            } catch (EcmaScriptException e) {
                finalMessage(e.getMessage() + 
                eol + "[[Error loading file' " + file.getPath() + "']]");
                if (!interactive) errorExit();
                return;
            } finally {
               try {
                         document.putHiddenProperty("URL", 
                                new ESString("file://<stdin>"));
               } catch (EcmaScriptException ignore) {
               }
          }
      } else {
         
          finalMessage("File " + file.getPath() + " not found");
          if (interactive) errorExit();
      }
  }
  
  /**
   * Expand an html source file containing <script> commands
   * @param file to expand
   */
  protected void doExpand(String fileName) {
      if (fileName == null || fileName.equals("")) {
        errorStream.println("[[File name missing for @expand.]]");
        if (! interactive) errorExit();
        return;
      }
      File file = new File(fileName);
      if (!file.exists()) {
          file = new File(fileName + ".html");
      }
      if (!file.exists()) {
          file = new File(fileName + ".htm");
      }
      BufferedReader lr = null;
     try {
         document.putHiddenProperty("URL", 
                            new ESString("file://" + file.getAbsolutePath()));
        lr = new BufferedReader(new FileReader(file));
        if (interactive) printStream.println("@@ Expanding html file '" + 
                                                file.getPath() + "' . . .");
        boolean inScript = false;
        StringBuffer script = null;
        String src = lr.readLine();
        String srclc = src.toLowerCase();
        int lineNumber = 0;
        while (src!= null) {
            lineNumber ++;
            if (inScript && srclc.indexOf("</script>")!=-1) {
                inScript = false;
                evaluator.evaluate(script.toString());
            } else if (inScript) {
                script.append(src+eol);
            } else if (srclc.indexOf("<script>")!=-1) {
                inScript = true;
                script = new StringBuffer();
            } else {
                 printStream.println(src);
            }
            src = lr.readLine();
            srclc = (src == null ) ? null : src.toLowerCase();
        }
      
        if (inScript) {
            errorStream.println("[[Error - end of file reached with openened <script>]]");
        }
      } catch (FileNotFoundException e) {
        errorStream.println("[[File '" + fileName + "' not found.]]");
      } catch (EcmaScriptException e) {
         errorStream.println("[[Error expanding the file '" + file.getPath() + "']]");
         errorStream.println("[[Error: " + e.getMessage() + "]]");
      } catch (Exception e) {
          errorStream.println("[[**Uncatched error: " + e + "]]");
          e.printStackTrace(errorStream);
      } finally {
          try { 
            document.putHiddenProperty("URL", 
                            new ESString("file://<stdin>"));
          } catch (EcmaScriptException e) {}
              if (lr!= null) {try {lr.close();} catch (IOException e) {}}
      }
   }

   /**
    * Process a test file using the test conventions
    * @param fileName Source test file
    */
   protected void doTest(String fileName) {
      if (fileName== null || fileName.equals("")) {
        errorStream.println("[[File name missing for @test.]]");
        if (! interactive) errorExit();
        return;
      }
      
    File file = new File(fileName);
    if (!file.exists()) {
      file = new File(fileName + ".estest");
    }
    BufferedReader lr = null;
    int nTests = 0;
    int nSuccess = 0;
    int nErrors = 0;
    try {
		lr = new BufferedReader(new FileReader(file));
		document.putHiddenProperty("URL", 
					   new ESString("file://" + file.getAbsolutePath()));
		printStream.println("@@ Processing test file '" + file.getPath() + "' . . .");
		String currentTest = null;
		StringBuffer scriptBuffer = new StringBuffer();
		String src = lr.readLine();
		String srclc = src.toLowerCase();
		int lineNumber = 0;
		while (src!= null) {
			lineNumber ++;
			if  (srclc.startsWith("@test")) {
				String scriptString = new String(scriptBuffer);
				if (currentTest != null) {
					nTests ++;
					boolean success = testString(currentTest, scriptString, file);
                        if (success) {nSuccess ++;} else { nErrors++; anyError=true;}
				} else {
					// The header part is not protected against errors
					evaluator.evaluate(scriptString);
				}
				scriptBuffer = new StringBuffer();
				currentTest = src;
			} else {
				scriptBuffer.append(src);
				scriptBuffer.append("\n");
			}
			src = lr.readLine();
			srclc = (src == null ) ? null : src.toLowerCase();
		}

		if (currentTest != null) {
			nTests ++;
    		String scriptString = new String(scriptBuffer);
			boolean success = testString(currentTest, scriptString, file);
			if (success) {nSuccess ++;} else { nErrors++; anyError=true;}
		} 
    
    } catch (FileNotFoundException e) {
        errorStream.println("[[File '" + fileName + "' not found.]]");
    } catch (EcmaScriptException e) {
         errorStream.println("[[Error expanding the file '" + file.getPath() + "']]");
         errorStream.println("[[Error: " + e.getMessage() + "]]");
    } catch (Exception e) {
       errorStream.println("[[**Uncatched error: " + e + "]]");
      e.printStackTrace(errorStream);
    } finally {
      if (lr!= null) {try {lr.close();} catch (IOException e) {}}

      try { 
        document.putHiddenProperty("URL", 
                        new ESString("file://<stdin>"));
      } catch (EcmaScriptException e) {}
    }
    printStream.println("@@ " + nTests + " tests, " + 
              nSuccess + " successes, " + 
              nErrors + " errors.");
  }
  
  /**
   * Execute a fragment to test in a controlled environment
   * @param currentTest Name of the test to execute
   * @param scriptString The fragment to test
   * @param file The source of tests 
   */
  protected boolean testString(String currentTest, String scriptString, File file) {
    printStream.println("@@ Testing " + currentTest);
    try {
        ESValue theValue = evaluator.evaluate(scriptString);
        if (theValue == null) {
               throw new EcmaScriptException("No value returned from @test");
        }
        boolean success = false;
        try {
            success = theValue.booleanValue();
        } catch (EcmaScriptException e) {
               throw new EcmaScriptException("@test did not return a boolean value");
        } 
        if (!success) {
               throw new EcmaScriptException("@test did not return 'true'");
        }
        return true;
    } catch (Exception e) {
        printStream.println("[[Test " + currentTest + 
                           " in file " + file.getPath() + " failed]]");
        printStream.println("[[Error: " + e.getMessage() + "]]");
        return false;                                   
    }
  }
  
  /**
   * Print details of last execution
   */
  protected void printDetail() {
      if (lastResult == null) {
          printStream.println("** No last result available");
      } else {
          printStream.println("** Result: " + lastResult.toDetailString());
          printStream.println("** Evaluated in " + timeOfEval + 
                                      " ms (note: +/-20 ms precision!)");
      }
  }

  protected void toggleDebugParse() {
        evaluator.setDebugParse( ! evaluator.isDebugParse());
        printStream.println("@@ debugParse is now: " + evaluator.isDebugParse());
  }               
                        
  protected void toggleDebugJavaAccess() {
        ESLoader.setDebugJavaAccess( ! ESLoader.isDebugJavaAccess());
        printStream.println("@@ debugJavaAccess is now: " + ESLoader.isDebugJavaAccess());
  }
                        
  protected void toggleDebugLoader() {
        ESLoader.setDebugLoader( ! ESLoader.isDebugLoader());
        printStream.println("@@ debugLoader is now: " + ESLoader.isDebugLoader());
  }
     
  protected void toggleDebugEvent() {                   
         ESWrapper.setDebugEvent( ! ESWrapper.isDebugEvent());
         printStream.println("@@ debugJavaAccess is now: " + ESWrapper.isDebugEvent());
  }
   
  /**
   * List the loaded extensions
   */
  protected void listExtensions() {
         int i = 0;
         for (Enumeration e = evaluator.getExtensions(); 
                                 e.hasMoreElements() ;) {
              printStream.println(" " + e.nextElement());
              i++;
         }
         printStream.println(" " + i + " extensions loaded");
  }
  /**
   * Display the @help text information (called by the GUI)
   */
  public void displayHelpText() {
      printHelp();
  }

  /**
   * Print the help message on the print stream
   */
  protected void printHelp() {
       Command.printHelp(printStream);
  }

  /**
   * Reset the evaluator, forgetting all global variables
   */
  protected void resetEvaluator() {
        try {
           printStream.println("@@ Reseting global object to default values");
           reset();
        } catch (EcmaScriptException e) {
            errorStream.println("[[Error during initialization: " +e.getMessage() + "]]");
            e.printStackTrace();
           return;
        }
  }
    
  /**
   * List the free memory
   */
  protected void listMemory() {
        Runtime rt = Runtime.getRuntime();
        long fm = rt.freeMemory();
        long tm = rt.totalMemory();
        printStream.println("@@ Total memory: " + tm + ", free memory: " + fm);
  }
    
  /**
   * List the visible properties of an object
   * @param parameter The expression returning an object on which to list the properties
   */
  protected void listProperties(String parameter) {
        ESObject listObject = evaluator.getGlobalObject();
        try {
            if (parameter != null) {
                ESValue listValue = evaluator.evaluate(parameter);
                if (! (listValue instanceof ESObject)) {
                   printStream.println("Cannot evaluate '" + parameter +"' to an object");
                   return;
               }
               listObject = (ESObject) listValue;
            }
            boolean directEnumeration = listObject.isDirectEnumerator();
            for (Enumeration e = listObject.getProperties() ; e.hasMoreElements() ;) {
               String property = e.nextElement().toString();
               printStream.print(property);
               if (! directEnumeration) {
                   String propertyValue = 
                           listObject.getProperty(property, property.hashCode()).toString();
                   // Remove leading eol
                   while (propertyValue.indexOf("\n")==0) {
                       propertyValue = propertyValue.substring(1);
                   }
                   while (propertyValue.indexOf(eol)==0) {
                       propertyValue = propertyValue.substring(eol.length());
                   }
                   // limit size
                   if (propertyValue.length()>250) {
                       propertyValue = propertyValue.substring(0,250) + "...";
                   }
                   // keep only first line
                   int ieol = propertyValue.indexOf(eol);
                   if (ieol==-1) ieol = propertyValue.indexOf("\n");
                   if (ieol!=-1) {
                       propertyValue = propertyValue.substring(0,ieol) + "...";
                   }
                   printStream.println(": " + propertyValue);
               }
           }
       } catch (Exception e) {
           printStream.println("Cannot evaluate '" + parameter +"' properties");
           printStream.println(e);
       }
   }  

   /**
    * List all propoerties of an object (visible or not)
    * @param parameter The expression returning an object on which to list the properties
    */
   protected void listAllProperties(String parameter) {
        ESObject listObject = evaluator.getGlobalObject();
        try {
            if (parameter != null) {
                ESValue listValue = evaluator.evaluate(parameter);
                if (! (listValue instanceof ESObject)) {
                   printStream.println("Cannot evaluate '" + parameter +"' to an object");
                   return;
               }
               listObject = (ESObject) listValue;
            }
            for (Enumeration e = listObject.getAllProperties() ; e.hasMoreElements() ;) {
               String property = e.nextElement().toString();
               ESValue propertyValue = listObject.getProperty(property, property.hashCode());
               printStream.println(propertyValue.getDescription(property));
            }
       } catch (Exception e) {
           printStream.println("Cannot evaluate '" + parameter +"' properties");
           printStream.println(e);
       }
   }
    

   /**
    * Clear the console
    */
   protected void clearConsole () {
       if (console != null) {    // defensive programming
           console.clear();
       }
   }
   
  /**
   * Describe an object
   * @param paramter Expression resulting in object to describe
   */
  protected void describe(String parameter) {
        ESValue toBeDescribed = evaluator.getGlobalObject();
        try {
            if (parameter != null) {
                toBeDescribed = evaluator.evaluate(parameter);
            } else {
                parameter = "global";
            }
            printStream.println(toBeDescribed.getDescription(parameter));
            
            if (toBeDescribed.isComposite()) {
                for (Enumeration e = toBeDescribed.getAllDescriptions() ; e.hasMoreElements() ;) {
                    ValueDescription description = (ValueDescription) e.nextElement();
                    printStream.println("   " + description.toString());
                }
            }
       } catch (Exception e) {
           printStream.println("Cannot evaluate '" + parameter +"'");
           printStream.println(e);
       }
   }


   /**
    * Setup the command array
    */
   protected void setupCommands() {
       // Add commands in alphabetic order preferably
       new Command("about", "display general information") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.printAbout();
                return false;
          }
       };
       new Command("clear", "Clear console output") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.clearConsole();
                return false;
          }
       };
       new Command("debugEvent", "Toggle debug flag for event processing") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.toggleDebugEvent();
                return false;
          }
       };
       new Command("debugLoader", "Toggle debug flag for dynamic loading") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.toggleDebugLoader();
                return false;
          }
       };
       new Command("debugJavaAccess", "Toggle debug flag for java interfacing") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.toggleDebugJavaAccess();
                return false;
          }
       };
       new Command("debugParse", "Toggle debug flag for parsing") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.toggleDebugParse();
                return false;
          }
       };
       new Command("describe", "Display details on the value given as parameter") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.describe(parameter);
                return false;
          }
       };
       new Command("detail", "Display details on last result of an evaluation") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.printDetail();
                return false;
          }
       };
       new Command("exit", "Exit the interpreter") {
           boolean doCommand(Interpret interpreter, String parameter) {
                return true;
          }
       };
       new Command("expand", "Expand between <script></script> in an .html file") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.clearLastResult();
                interpreter.doExpand(parameter);
                return false;
          }
       };
       new Command("extensions", "Display the list of loaded extensions") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.listExtensions();
                return false;
          }
       };
       new Command("help", "Display the list of commands") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.printHelp();
                return false;
          }
       };
       new Command("list", "List the enumerated properties of the object") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.listProperties(parameter);
                return false;
          }
       };
       new Command("listAll", "List all properties of the object") {
           boolean doCommand(Interpret interpreter, String parameter) {

                interpreter.listAllProperties(parameter);
                return false;
          }
       };
       new Command("load", "Load a .js, .es or .esw file") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.clearLastResult();
                interpreter.doLoad(parameter);
                return false;
          }
       };
       new Command("memory", "Give information on available memory") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.listMemory();
                return false;
          }
       };
       new Command("path", "Display the current load path") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.doPath();
                return false;
          }
       };
       new Command("pwd", "Display the current user directory") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.doPwd();
                return false;
          }
       };
       new Command("reset", "Restore the interpreter to the initial state") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.clearLastResult();
                interpreter.resetEvaluator();
                return false;
          }
       };
       new Command("test", "Execute a test file (.estest)") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.clearLastResult();
                interpreter.doTest(parameter);
                return false;
          }
       };
       new Command("version", "Display the version of the interpreter") {
           boolean doCommand(Interpret interpreter, String parameter) {
                interpreter.printVersion();
                return false;
          }
       };
   }

  /**
   * Main interpreter
   * @param args String parameter array
   */
  public static void main(String args[]) {
      Interpret i = new Interpret();
      i.doWork(args);
  }

}

/**
 * Common code for all commands
 */
abstract class Command {
    
    /**
     * Keep track of all commands
     */
    static private Vector allCommands = new Vector();
    
    protected String name;
    protected String lowerCaseName;   // For case insensitive search
    protected String help;
    
    /**
     * Print the help text of a command
     */
    static void printHelp(PrintStream printStream) {
        for (Enumeration e = allCommands.elements() ; e.hasMoreElements() ;) {
            Command cmd = (Command) e.nextElement();
            printStream.println("  @"+cmd.name+ " - " + cmd.help);
        }
    }
    
    /**
     * Execute a command
     * @param interpreter The interpreter context
     * @param printStream The output stream to use
     * @param command the command name (as typed)
     * @param parameter the command parameter string
     */
    static boolean executeCommand(Interpret interpreter, 
                               PrintStream printStream,
                               String command, 
                               String parameter) {
        Vector foundCmds = new Vector();
        String lcCommand = command.toLowerCase();
        for (Enumeration e = allCommands.elements() ; e.hasMoreElements() ;) {
            Command cmd = (Command) e.nextElement();
            if (cmd.lowerCaseName.equals(command)) {
                // Exact same name, execute
                return cmd.doCommand(interpreter, parameter);
            } else if (cmd.lowerCaseName.startsWith(command)) {
                // Prefix, add to list of candidates
                foundCmds.addElement(cmd);
            }
        }
        if (foundCmds.size()==0) {
            printStream.println("@@ Command '" + command + "' not recognized");
            interpreter.printHelp();
        } else if (foundCmds.size()==1) {
            Command cmd = (Command) foundCmds.elementAt(0);
            return cmd.doCommand(interpreter, parameter);
        } else {
            printStream.println("@@ Command More than one command starting with '" + 
                                        command + "'");
            for (Enumeration e = foundCmds.elements() ; e.hasMoreElements() ;) {
                Command cmd = (Command) e.nextElement();
                printStream.println("  @"+cmd.name+ " - " + cmd.help);
            }
        }
        return false;
    }
    
    /**
     * Define a new command by name with a specified help string
     */
    Command(String name, String help) {
        this.name = name;
        this.lowerCaseName = "@" + name.toLowerCase();
        this.help = help;
        allCommands.addElement(this);
    }
    
    /**
     * Execute a command with the specified parameter
     * @param interpreter the interpreter context
     * @param parameter The string parameter
     * @return true if the interpreter must exit
     */
    abstract boolean doCommand(Interpret interpreter, String parameter);
}
