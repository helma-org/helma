/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.objectmodel.dom;

import helma.objectmodel.*;
import helma.objectmodel.db.NodeManager;
import helma.objectmodel.db.Node;
import helma.framework.core.Application;

import java.io.*;
import java.util.ArrayList;

/**
 * A simple XML-database
 */
public final class XmlDatabase implements IDatabase {

    protected File dbHomeDir;
    protected Application app;
    protected NodeManager nmgr;
    protected XmlIDGenerator idgen;

    // character encoding to use when writing files.
    // use standard encoding by default.
    protected String encoding = null;

    /**
     * Initializes the database from an application.
     * @param app
     * @throws DatabaseException
     */
    public void init(File dbHome, Application app) throws DatabaseException {
        this.app = app;
        nmgr = app.getNodeManager();
        dbHomeDir = dbHome;

        if (!dbHomeDir.exists() && !dbHomeDir.mkdirs()) {
            throw new DatabaseException("Can't create database directory "+dbHomeDir);
        }

        if (!dbHomeDir.canWrite()) {
            throw new DatabaseException("No write permission for database directory "+dbHomeDir);
        }

        File stylesheet = new File(dbHomeDir, "helma.xsl");
        // if style sheet doesn't exist, copy it
        if (!stylesheet.exists()) {
            copyStylesheet(stylesheet);
        }

        this.encoding = app.getCharset();

        // get the initial id generator value
        long idBaseValue;
        try {
            idBaseValue = Long.parseLong(app.getProperty("idBaseValue", "1"));
            // 0 and 1 are reserved for root nodes
            idBaseValue = Math.max(1L, idBaseValue);
        } catch (NumberFormatException ignore) {
            idBaseValue = 1L;
        }

        ITransaction txn = null;

        try {
            txn = beginTransaction();

            try {
                idgen = getIDGenerator(txn);

                if (idgen.getValue() < idBaseValue) {
                    idgen.setValue(idBaseValue);
                }
            } catch (ObjectNotFoundException notfound) {
                // will start with idBaseValue+1
                idgen = new XmlIDGenerator(idBaseValue);
            }

            // check if we need to set the id generator to a base value
            Node node = null;

            try {
                node = (Node) getNode(txn, "0");
            } catch (ObjectNotFoundException notfound) {
                node = new Node("root", "0", "Root", nmgr.safe);
                node.setDbMapping(app.getDbMapping("root"));
                insertNode(txn, node.getID(), node);
                // register node with nodemanager cache
                nmgr.registerNode(node);
            }

            try {
                node = (Node) getNode(txn, "1");
            } catch (ObjectNotFoundException notfound) {
                node = new Node("users", "1", null, nmgr.safe);
                node.setDbMapping(app.getDbMapping("__userroot__"));
                insertNode(txn, node.getID(), node);
                // register node with nodemanager cache
                nmgr.registerNode(node);
            }

            commitTransaction(txn);
        } catch (Exception x) {
            x.printStackTrace();

            try {
                abortTransaction(txn);
            } catch (Exception ignore) {
            }

            throw (new DatabaseException("Error initializing db"));
        }
    }
    
    /**
     * Try to copy style sheet for XML files to database directory
     */
    private void copyStylesheet(File destination) {
        InputStream in = null;
        FileOutputStream out = null;
        byte[] buffer = new byte[1024];
        int read;
        
        try {
            in = getClass().getResourceAsStream("helma.xsl");
            out = new FileOutputStream(destination);
            while ((read = in.read(buffer, 0, buffer.length)) > 0) {
                out.write(buffer, 0, read);
            }
        } catch (IOException iox) {
            System.err.println("Error copying db style sheet: "+iox);
        } finally {
            try {
                if (out != null) 
                    out.close();
                if (in != null) 
                    in.close();
            } catch (IOException ignore) {
            }
        }
    }

    /**
     * Shut down the database
     */
    public void shutdown() {
        // nothing to do
    }

    /**
     * Start a new transaction.
     *
     * @return the new tranaction object
     * @throws DatabaseException
     */
    public ITransaction beginTransaction() throws DatabaseException {
        return new Transaction();
    }

    /**
     * committ the given transaction, makint its changes persistent
     *
     * @param txn
     * @throws DatabaseException
     */
    public void commitTransaction(ITransaction txn) throws DatabaseException {
        if (idgen.dirty) {
            try {
                saveIDGenerator(txn);
                idgen.dirty = false;                
            } catch (IOException x) {
                throw new DatabaseException(x.toString());
            }
        }
        txn.commit();
    }

    /**
     * Abort the given transaction
     *
     * @param txn
     * @throws DatabaseException
     */
    public void abortTransaction(ITransaction txn) throws DatabaseException {
        txn.abort();
    }

    /**
     * Get the id for the next new object to be stored.
     *
     * @return the id for the next new object to be stored
     * @throws ObjectNotFoundException
     */
    public String nextID() throws ObjectNotFoundException {
        if (idgen == null) {
            getIDGenerator(null);
        }

        return idgen.newID();
    }

    /**
     * Get the id-generator for this database.
     *
     * @param txn
     * @return the id-generator for this database
     * @throws ObjectNotFoundException
     */
    public XmlIDGenerator getIDGenerator(ITransaction txn)
                               throws ObjectNotFoundException {
        File file = new File(dbHomeDir, "idgen.xml");

        this.idgen = XmlIDGenerator.getIDGenerator(file);

        return idgen;
    }

    /**
     * Write the id-generator to file.
     *
     * @param txn
     * @throws IOException
     */
    public void saveIDGenerator(ITransaction txn)
                         throws IOException {
        File tmp = File.createTempFile("idgen.xml.", ".tmp", dbHomeDir);

        XmlIDGenerator.saveIDGenerator(idgen, tmp);

        File file = new File(dbHomeDir, "idgen.xml");
        if (file.exists() && !file.canWrite()) {
            throw new IOException("No write permission for "+file);
        }
        Resource res = new Resource(file, tmp);
        txn.addResource(res, ITransaction.ADDED);
    }

    /**
     * Retrieves a Node from the database.
     *
     * @param txn the current transaction
     * @param kstr the key
     * @return the object associated with the given key
     * @throws IOException if an I/O error occurred loading the object.
     * @throws ObjectNotFoundException if no object is stored by this key.
     */
    public INode getNode(ITransaction txn, String kstr)
                  throws IOException, ObjectNotFoundException {
        File f = new File(dbHomeDir, kstr + ".xml");

        if (!f.exists()) {
            throw new ObjectNotFoundException("Object not found for key " + kstr);
        }

        try {
            XmlDatabaseReader reader = new XmlDatabaseReader(nmgr);
            Node node = reader.read(f);

            return node;
        } catch (Exception x) {
            app.logError("Error reading " +f, x);
            throw new IOException(x.toString());
        }
    }
    /**
     * Save a node with the given key. Writes the node to a temporary file
     * which is copied to its final name when the transaction is committed.
     *
     * @param txn
     * @param kstr
     * @param node
     * @throws java.io.IOException
     */
    public void insertNode(ITransaction txn, String kstr, INode node)
                throws IOException {
        File f = new File(dbHomeDir, kstr + ".xml");

        if (f.exists()) {
            throw new IOException("Object already exists for key " + kstr);
        }

        // apart from the above check insertNode() is equivalent to updateNode()
        updateNode(txn, kstr, node);
    }

    /**
     * Update a node with the given key. Writes the node to a temporary file
     * which is copied to its final name when the transaction is committed.
     *
     * @param txn
     * @param kstr
     * @param node
     * @throws java.io.IOException
     */
    public void updateNode(ITransaction txn, String kstr, INode node)
                throws IOException {
        XmlWriter writer = null;
        File tmp = File.createTempFile(kstr + ".xml.", ".tmp", dbHomeDir);

        if (encoding != null) {
            writer = new XmlWriter(tmp, encoding);
        } else {
            writer = new XmlWriter(tmp);
        }

        writer.setMaxLevels(1);
        writer.write(node);
        writer.close();

        File file = new File(dbHomeDir, kstr+".xml");
        if (file.exists() && !file.canWrite()) {
            throw new IOException("No write permission for "+file);
        }
        Resource res = new Resource(file, tmp);
        txn.addResource(res, ITransaction.ADDED);
    }

    /**
     * Marks an element from the database as deleted
     *
     * @param txn
     * @param kstr
     * @throws IOException
     */
    public void deleteNode(ITransaction txn, String kstr)
                    throws IOException {
        Resource res = new Resource(new File(dbHomeDir, kstr+".xml"), null);
        txn.addResource(res, ITransaction.DELETED);
    }

    /**
     * set the file encoding to use
     *
     * @param encoding the database's file encoding
     */
    public void setEncoding(String encoding) {
        this.encoding = encoding;
    }

    /**
     * get the file encoding used by this database
     *
     * @return the file encoding used by this database
     */
    public String getEncoding() {
        return encoding;
    }

    class Transaction implements ITransaction {

        ArrayList writeFiles = new ArrayList();
        ArrayList deleteFiles = new ArrayList();

        /**
         * Complete the transaction by making its changes persistent.
         */
        public void commit() throws DatabaseException {
            // move through updated/created files and persist them
            int l = writeFiles.size();
            for (int i=0; i<l; i++) {
                Resource res = (Resource) writeFiles.get(i);
                try {
                    // because of a Java/Windows quirk, we have to delete
                    // the existing file before trying to overwrite it
                    if (res.file.exists()) {
                        res.file.delete();
                    }
                    // move temporary file to permanent name
                    if (res.tmpfile.renameTo(res.file)) {
                        // success - delete tmp file
                        res.tmpfile.delete();
                    } else {
                        // error - leave tmp file and print a message
                        app.logError("*** Error committing "+res.file);
                        app.logError("*** Committed version is in "+res.tmpfile);
                    }
                } catch (SecurityException ignore) {
                    // shouldn't happen
                }
            }

            // move through deleted files and delete them
            l = deleteFiles.size();
            for (int i=0; i<l; i++) {
                Resource res = (Resource) deleteFiles.get(i);
                // delete files enlisted as deleted
                try {
                    res.file.delete();
                } catch (SecurityException ignore) {
                    // shouldn't happen
                }
            }
            // clear registered resources
            writeFiles.clear();
            deleteFiles.clear();
        }

        /**
         * Rollback the transaction, forgetting the changed items
         */
        public void abort() throws DatabaseException {
            int l = writeFiles.size();
            for (int i=0; i<l; i++) {
                Resource res = (Resource) writeFiles.get(i);
                // delete tmp files created by this transaction
                try {
                    res.tmpfile.delete();
                } catch (SecurityException ignore) {
                    // shouldn't happen
                }
            }

            // clear registered resources
            writeFiles.clear();
            deleteFiles.clear();
        }

        /**
         * Adds a resource to the list of resources encompassed by this transaction
         *
         * @param res the resource to add
         * @param status the status of the resource (ADDED|UPDATED|DELETED)
         */
        public void addResource(Object res, int status)
               throws DatabaseException {
            if (status == DELETED) {
                deleteFiles.add(res);
            } else {
                writeFiles.add(res);
            }
        }

    }

    /**
     * A holder class for two files, the temporary file and the permanent one
     */
    class Resource {
        File tmpfile;
        File file;

        public Resource(File file, File tmpfile) {
            this.file = file;
            this.tmpfile = tmpfile;
        }
    }


    class IDGenerator {

    }

}

