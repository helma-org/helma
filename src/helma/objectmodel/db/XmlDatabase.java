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

public final class XmlDatabase implements IDatabase {

	private String dbHome;
	private File dbBaseDir;
	private NodeManager nmgr;
	private IDGenerator idgen;
	// character encoding to use when writing files.
	// use standard encoding by default.
	private String encoding = null;
	

	public XmlDatabase (String dbHome, String dbFilename, NodeManager nmgr) throws DatabaseException {
		this.dbHome = dbHome;
		this.nmgr = nmgr;
		dbBaseDir = new File (dbHome);
		if (!dbBaseDir.exists() && !dbBaseDir.mkdirs() )
			throw new RuntimeException("Couldn't create DB-directory");
		this.encoding = nmgr.app.getCharset ();
	}

	public void shutdown ()	 {   }
	public ITransaction beginTransaction () throws DatabaseException {    return null;    }
	public void commitTransaction (ITransaction txn) throws DatabaseException {    }
	public void abortTransaction (ITransaction txn) throws DatabaseException {    }

	public String nextID() throws ObjectNotFoundException {
		if (idgen==null) {
			getIDGenerator(null);
		}
		return idgen.newID();
	}

	public IDGenerator getIDGenerator (ITransaction txn) throws ObjectNotFoundException {
		File file = new File (dbBaseDir, "idgen.xml");
		this.idgen = IDGenParser.getIDGenerator(file);
		return idgen;
	}

	public void saveIDGenerator (ITransaction txn, IDGenerator idgen) throws Exception {
		File file = new File (dbBaseDir, "idgen.xml");
		IDGenParser.saveIDGenerator(idgen,file);
		this.idgen = idgen;
	}

	public INode getNode (ITransaction txn, String kstr) throws Exception {
		File f = new File (dbBaseDir, kstr+".xml");
		if ( ! f.exists() )
			throw new ObjectNotFoundException ("Object not found for key "+kstr+".");
		try {
			XmlDatabaseReader reader = new XmlDatabaseReader (nmgr);
			Node node = reader.read (f);
			return node;
		} catch ( RuntimeException x )   {
			nmgr.app.logEvent("error reading node from XmlDatbase: " + x.toString() );
			throw new ObjectNotFoundException(x.toString());
		}
	}

	public void saveNode (ITransaction txn, String kstr, INode node) throws Exception {
		XmlWriter writer = null;
		File file = new File (dbBaseDir,kstr+".xml");
		if (encoding != null)
		    writer = new XmlWriter (file, encoding);
		else
		    writer = new XmlWriter (file);
		writer.setMaxLevels(1);
		boolean result = writer.write((Node)node);
		writer.close();
	}

	public void deleteNode (ITransaction txn, String kstr) throws Exception {
		File f = new File (dbBaseDir, kstr+".xml");
		f.delete();
	}

	public void setEncoding (String enc) {
		this.encoding = encoding;
	}

	public String getEncoding () {
		return encoding;
	}
}
