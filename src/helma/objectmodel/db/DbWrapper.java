// DbWrapper.java
// Copyright (c) Hannes Wallnöfer 1999-2000


package helma.objectmodel.db;

import com.sleepycat.db.*;
import helma.objectmodel.ObjectNotFoundException;
import java.io.*;

/**
 * A wrapper around a Berkeley embedded database. Used to gracefully handle the case
 * when the native library can not be loaded.
 */

public class DbWrapper {

    private boolean loaded, useTransactions;

    private Db db;
    DbEnv dbenv;
    final int checkpointPause = 600000;  // min. 10 minutes between checkpoints
    volatile long lastCheckpoint = 0;
    volatile long txncount=0;

    private File dbBaseDir;

    public DbWrapper (String dbHome, String dbFilename, boolean useTx) throws DbException {
	try {
	    dbBaseDir = new File (dbHome);
	    if (!dbBaseDir.exists())
	        dbBaseDir.mkdirs();

	    useTransactions = useTx;

	    int dbInitFlags = Db.DB_CREATE | Db.DB_THREAD | Db.DB_INIT_MPOOL;
	    if (useTransactions) {
	        dbInitFlags = dbInitFlags | Db.DB_INIT_TXN;
	    }

	    dbenv = new DbEnv (0);
	    try {
	        dbenv.open (dbHome, dbInitFlags, 0); // for berkeley 3.0, add second parameter (null)
	    } catch (FileNotFoundException fnf) {
	        // we just created the dirs, so this shouldn't happen
	    }

	    try {
	        dbenv.set_error_stream(System.err);
	        dbenv.set_errpfx("HOP: ");
	    } catch (Exception e) {
	        System.err.println("Error in DbWrapper: "+e.toString());
	    }

	    db = new Db (dbenv, 0);
	    try {
	        db.upgrade  (dbFilename, 0);
	    } catch (Exception ignore) {
	        // nothing to upgrade, db doesn't exist
	    }

	    try {
	        db.open (dbFilename, null, Db.DB_BTREE, Db.DB_CREATE, 0644);
	    } catch (FileNotFoundException fnf) {
	        // we just created the dirs, so this shouldn't happen
	    }
	    loaded = true;

	} catch (NoClassDefFoundError noclass) {
	    Server.getLogger().log ("Warning: Using file based db as fallback. Reason: "+noclass);
	    loaded = false;
	} catch (UnsatisfiedLinkError nolib) {
	    Server.getLogger().log ("Warning: Using file based db as fallback. Reason: "+nolib);
	    loaded = false;
	}
	
    }


    public void shutdown () throws DbException {
	if (loaded) {
	    db.close (0);
	    dbenv.close (0);
	}
    }

    public DbTxn beginTransaction () throws DbException {
	if (loaded && useTransactions)
	    return dbenv.txn_begin (null, 0);
	else
	    return null;
    }

    public void commitTransaction (DbTxn txn) throws DbException  {
	if (txn == null || !loaded || !useTransactions)
	    return;
	txn.commit (0);
             if (++txncount%100 == 0 && System.currentTimeMillis()-checkpointPause > lastCheckpoint) {
	    // checkpoint transaction logs in time interval specified by server.checkpointPause
	    // if there are more then 100 transactions to checkpoint.
	    checkpoint ();
	}
    }

    public void abortTransaction (DbTxn txn) throws DbException {
	if (txn == null || !loaded || !useTransactions)
	    return;
	txn.abort ();
    }

    protected void checkpoint () throws DbException {
	if (!loaded || !useTransactions || txncount == 0)
	    return;
	long now = System.currentTimeMillis();
	if (now - lastCheckpoint < checkpointPause)
	    return;
	dbenv.txn_checkpoint (0, 0, 0); // for berkeley 3.0, remove third 0 parameter
	txncount = 0;
	lastCheckpoint = now;
	Server.getLogger().log ("Spent "+(System.currentTimeMillis()-now)+" in checkpoint");
    }

    public IDGenerator getIDGenerator (DbTxn txn, String kstr) throws Exception {
	if (loaded)
	    return getIDGenFromDB (txn, kstr);
	else
	    return getIDGenFromFile (kstr);
    }

    public Node getNode (DbTxn txn, String kstr) throws Exception {
	if (loaded)
	    return getNodeFromDB (txn, kstr);
	else
	    return getNodeFromFile (kstr);
    }

    public void save (DbTxn txn, String kstr, Object obj) throws Exception {
	if (loaded)
	    saveToDB (txn, kstr, obj);
	else
	    saveToFile (kstr, obj);
    }

    public void delete (DbTxn txn, String kstr) throws Exception {
	if (loaded)
	    deleteFromDB (txn, kstr);
	else
	    deleteFromFile (kstr);
    }


    private IDGenerator getIDGenFromDB (DbTxn txn, String kstr) throws Exception {
	long now = System.currentTimeMillis ();
	byte[] kbuf = kstr.getBytes ();
	Dbt key = new Dbt (kbuf);
	key.set_size (kbuf.length);
	Dbt data = new Dbt ();
	data.set_flags (Db.DB_DBT_MALLOC);
	
	db.get (txn, key, data, 0);

	byte[] b = data.get_data ();
	if (b == null)
	    throw new ObjectNotFoundException ("Object not found for key "+kstr+".");

	IDGenerator idgen = null;
	ByteArrayInputStream bin = new ByteArrayInputStream (b);
	ObjectInputStream oin = new ObjectInputStream (bin);
	idgen = (IDGenerator) oin.readObject ();

	oin.close ();
	return idgen;
    }


    private Node getNodeFromDB (DbTxn txn, String kstr) throws Exception {
	long now = System.currentTimeMillis ();
	byte[] kbuf = kstr.getBytes ();
	Dbt key = new Dbt (kbuf);
	key.set_size (kbuf.length);
	Dbt data = new Dbt ();
	data.set_flags (Db.DB_DBT_MALLOC);
	
	db.get (txn, key, data, 0);

	byte[] b = data.get_data ();
	if (b == null)
	    throw new ObjectNotFoundException ("Object not found for key "+kstr+".");

	Node node = null;
	ByteArrayInputStream bin = new ByteArrayInputStream (b);
	ObjectInputStream oin = new ObjectInputStream (bin);
	node = (Node) oin.readObject ();
	oin.close ();
	return node;
    }


    private void saveToDB (DbTxn txn, String kstr, Object obj) throws Exception {
	long now = System.currentTimeMillis ();
	byte kbuf[] = kstr.getBytes();
	ByteArrayOutputStream bout = new  ByteArrayOutputStream ();
	ObjectOutputStream oout = new ObjectOutputStream (bout);
	oout.writeObject (obj);
	oout.close ();
	byte vbuf[] = bout.toByteArray ();

	Dbt key = new Dbt (kbuf);
	key.set_size (kbuf.length);
	Dbt value = new Dbt (vbuf);
	value.set_size (vbuf.length);
	
	db.put (txn, key, value, 0);
	// IServer.getLogger().log ("saved "+obj+", size = "+vbuf.length);
    }

    private void deleteFromDB (DbTxn txn, String kstr) throws Exception {

	byte kbuf[] = kstr.getBytes();

	Dbt key = new Dbt (kbuf);
	key.set_size (kbuf.length);
	
	db.del (txn, key, 0);
    }

    ////////////////////////////////////////////////////////////////////////////////
    // File based fallback methods
    ///////////////////////////////////////////////////////////////////////////////

    private IDGenerator getIDGenFromFile (String kstr) throws Exception {

	File f = new File (dbBaseDir, kstr);

	if ( ! f.exists() )
	    throw new ObjectNotFoundException ("Object not found for key "+kstr+".");

	IDGenerator idgen = null;
	FileInputStream bin = new FileInputStream (f);
	ObjectInputStream oin = new ObjectInputStream (bin);
	idgen = (IDGenerator) oin.readObject ();

	oin.close ();
	return idgen;
    }


    private Node getNodeFromFile (String kstr) throws Exception {

	File f = new File (dbBaseDir, kstr);

	if ( ! f.exists() )
	    throw new ObjectNotFoundException ("Object not found for key "+kstr+".");

	Node node = null;
	FileInputStream bin = new FileInputStream (f);
	ObjectInputStream oin = new ObjectInputStream (bin);
	node = (Node) oin.readObject ();
	oin.close ();
	return node;
    }


    private void saveToFile (String kstr, Object obj) throws Exception {

	File f = new File (dbBaseDir, kstr);

	FileOutputStream bout = new  FileOutputStream (f);
	ObjectOutputStream oout = new ObjectOutputStream (bout);
	oout.writeObject (obj);
	oout.close ();
    }

    private void deleteFromFile (String kstr) throws Exception {

	File f = new File (dbBaseDir, kstr);
	f.delete();
    }


}