package helma.objectmodel.db;

import java.io.*;
import java.util.Date;
import org.w3c.dom.Element;
import org.w3c.dom.Document;

import helma.objectmodel.*;
import helma.objectmodel.dom.*;

/**
 * A simple XML-database
 */

public class XmlDatabase implements IDatabase {

    private String dbHome;
    private File dbBaseDir;
    private NodeManager nmgr;
	private IDGenerator idgen;

    public XmlDatabase (String dbHome, String dbFilename, NodeManager nmgr) throws DatabaseException {
		this.dbHome = dbHome;
		this.nmgr = nmgr;
	    dbBaseDir = new File (dbHome);
    	if (!dbBaseDir.exists() && !dbBaseDir.mkdirs() )
    		throw new RuntimeException("Couldn't create DB-directory");
	}

    public void shutdown ()		{	    }
    public ITransaction beginTransaction () throws DatabaseException	{	    return null;    }
    public void commitTransaction (ITransaction txn) throws DatabaseException	{    }
    public void abortTransaction (ITransaction txn) throws DatabaseException	{    }

	public String nextID()	{
		if (idgen==null)	{
			try	{
				getIDGenerator(null);
			}	catch ( Exception e )	{
				System.err.println("couldn't load IDGenerator");
				return "";
			}
		}
		return idgen.newID();
	}
	
    public IDGenerator getIDGenerator (ITransaction txn) throws ObjectNotFoundException {
		File file = new File (dbBaseDir, "idgen.xml");
		this.idgen = IDGenParser.getIDGenerator(file);
		return idgen;
    }

	public void saveIDGenerator (ITransaction txn, IDGenerator idgen) throws Exception	{
		File file = new File (dbBaseDir, "idgen.xml");
		IDGenParser.saveIDGenerator(idgen,file);
		this.idgen = idgen;
	}

    public INode getNode (ITransaction txn, String kstr) throws Exception	{
		File f = new File (dbBaseDir, kstr+".xml");
		if ( ! f.exists() )
	    	throw new ObjectNotFoundException ("Object not found for key "+kstr+".");
        try     {
			XmlReader reader = new XmlReader (nmgr);
            Node node = (Node)reader.read (f,null);
		    System.out.println("getNode("+kstr+")="+node.toString() );
		    node.dump();
			return node;
        }	catch ( RuntimeException x )   { 
			// fixme: do logging, not just printing...
			System.err.println("runtimeexception: " + x.toString());
            x.printStackTrace();
            throw new ObjectNotFoundException(x.toString());
        }
    }

    public void saveNode (ITransaction txn, String kstr, INode node) throws Exception {
		System.out.println("save("+kstr+","+node.toString()+"):" );
		XmlWriter writer = new XmlWriter (new File (dbBaseDir,kstr+".xml") ); 
		writer.setMaxLevels(1);
		// FIXME: synchronize here?
		boolean result = writer.write((Node)node); 
		writer.close(); 
		System.out.println("\n");
    }

    public void deleteNode (ITransaction txn, String kstr) throws Exception {
		File f = new File (dbBaseDir, kstr+".xml");
		f.delete();
		System.out.println("delete("+kstr+")" );
    }


}
