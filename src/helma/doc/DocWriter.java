package helma.doc;

import java.io.*;
import java.util.Arrays;

public class DocWriter extends DocHtmlWriter	{

	public static String docDir;

	public DocWriter( String filename ) throws FileNotFoundException	{
		super( new File(docDir,filename).getAbsolutePath() );
		DocRun.log("creating " + filename ); 
	}

	public static void start( String parDocDir, DocApplication parApp )	{
		app = parApp;
		docDir = parDocDir;
		if ( !(new File(parDocDir).exists()) )	{
			DocRun.log("creating directory " + parDocDir );
			(new File(parDocDir)).mkdirs();
		}
		printStyleSheet(app);
		printFrameSet(app);
		printAppDoc(app);
		printAppIndex(app);
		printPrototypes(app);
		printFunctionIndex(app);
		if ( DocRun.getOption("-f").equals("true") )
			printFunctions(app);
	}

	/**	print index-1.html .. alphabetical list of all functions */
	public static void printFunctionIndex(DocApplication app)	{
		try	{
			DocWriter dw = new DocWriter("index-1.html");
			dw.printHeader("Application " + app.getName() );
			dw.printNavBar(app.getName(), null, INDEX );
			DocFunction[] df = app.listFunctions();
			Arrays.sort(df,new DocComparator(1,df[0]));
			dw.printFunctionIndex(df);
			dw.close();
		}	catch ( FileNotFoundException e )	{	DocRun.log( e.getMessage() );	}
	}

	/** print stylesheet.css	*/
	public static void printStyleSheet(DocApplication app)	{
		try	{
			DocWriter dw = new DocWriter("stylesheet.css");
			dw.printStyleSheet();
			dw.close();
		}	catch ( FileNotFoundException e )	{	DocRun.log( e.getMessage() );	}
	}

	/** print index.html	*/
	public static void printFrameSet(DocApplication app)	{
		try	{
			DocWriter dw = new DocWriter("index.html");
			dw.printHeader("Application " + app.getName() );
			dw.printFrameSet();
			dw.close();
		}	catch ( FileNotFoundException e )	{	DocRun.log( e.getMessage() );	}
	}

	/** print app.html, list of prototypes */
	public static void printAppDoc(DocApplication app)	{
		try	{
			DocWriter dw = new DocWriter("app.html");
			dw.printHeader("Application " + app.getName() );
			dw.printNavBar(app.getName(), null, APPLICATION );
			dw.printElementTitle(app);
			dw.printComment(app);
			dw.printPrototypeList(app.listPrototypes(),"Prototypes");
			dw.close();
		}	catch ( FileNotFoundException e )	{	DocRun.log( e.getMessage() );	}
	}

	/** print app-frame.html, content of left frame */
	public static void printAppIndex(DocApplication app)	{
		try	{
			DocWriter dw = new DocWriter("app-frame.html");
			dw.printHeader("Application " + app.getName() );
			dw.printAppIndexTitle(app.getName());
			dw.printAppIndexList((DocPrototype[])app.listPrototypes());
			dw.close();
		}	catch ( FileNotFoundException e )	{	DocRun.log( e.getMessage() );	}
	}
	
	/** print all prototype-files, lists of functions	*/
	public static void printPrototypes(DocApplication app)	{
		DocPrototype[] pt = app.listPrototypes();
		for ( int i=0; i<pt.length; i++ )	{
			try	{
				DocWriter dw = new DocWriter( pt[i].getDocFileName());
				dw.printHeader("Application " + app.getName() + " / Prototype " + pt[i].getName());
				dw.printNavBar(app.getName(), pt[i], PROTOTYPE );
				dw.printElementTitle(pt[i]);
				dw.printComment(pt[i]);
				dw.printFunctionList(pt[i].listFunctions(DocElement.ACTION),   "Actions");
				dw.printFunctionList(pt[i].listFunctions(DocElement.TEMPLATE), "Templates");
				dw.printFunctionList(pt[i].listFunctions(DocElement.FUNCTION), "Functions");
				dw.printFunctionList(pt[i].listFunctions(DocElement.MACRO),    "Macros");
				dw.printFunctionList(pt[i].listFunctions(DocElement.SKIN),     "Skins");
				dw.printInheritance(pt[i]);
				dw.close();
			}	catch ( FileNotFoundException e )	{	DocRun.log( e.getMessage() );	}
		}
	}

	/** print all function sources	*/
	public static void printFunctions(DocApplication app)	{
		DocFunction[] df = app.listFunctions();
		for ( int i=0;i<df.length; i++ )	{
			try	{
				File d = new File ( docDir, df[i].getDocPrototype().getName().toLowerCase() );
				if ( !d.exists() )
					d.mkdir();
				DocWriter dw = new DocWriter( df[i].getDocPrototype().getName().toLowerCase() + "/" + df[i].getDocFileName() );
				dw.printHeader( app.getName() + "." + df[i].getDocPrototype().getName() + "." + df[i].getName() );
				dw.printNavBar( app.getName(), df[i].getDocPrototype(), DocHtmlWriter.METHOD);
				dw.printFunction( df[i] );
				dw.close();
			}	catch ( FileNotFoundException e )	{	DocRun.log( e.getMessage() );	}
		}
	}


}

