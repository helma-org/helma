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

package helma.objectmodel.db;

import helma.objectmodel.*;
import helma.objectmodel.dom.IDGenParser;
import helma.objectmodel.dom.XmlDatabaseReader;
import helma.objectmodel.dom.XmlWriter;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * A simple XML-database
 */
public final class XmlDatabase implements IDatabase {

    private File dbHomeDir;
    private NodeManager nmgr;
    private IDGenerator idgen;

    // character encoding to use when writing files.
    // use standard encoding by default.
    private String encoding = null;

    /**
     * Creates a new XmlDatabase object.
     *
     * @param dbHome ...
     * @param nmgr ...
     *
     * @throws DatabaseException ...
     * @throws RuntimeException ...
     */
    public XmlDatabase(String dbHome, NodeManager nmgr)
                throws DatabaseException {
        this.nmgr = nmgr;
        dbHomeDir = new File(dbHome);

        if (!dbHomeDir.exists() && !dbHomeDir.mkdirs()) {
            throw new DatabaseException("Can't create database directory "+dbHomeDir);
        }

        if (!dbHomeDir.canWrite()) {
            throw new DatabaseException("No write permission for database directory "+dbHomeDir);
        }

        this.encoding = nmgr.app.getCharset();
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
     * Get the next free id to use for new objects
     *
     * @return
     * @throws ObjectNotFoundException
     */
    public String nextID() throws ObjectNotFoundException {
        if (idgen == null) {
            getIDGenerator(null);
        }

        return idgen.newID();
    }

    /**
     * Get the id-generator
     *
     * @param txn
     * @return
     * @throws ObjectNotFoundException
     */
    public IDGenerator getIDGenerator(ITransaction txn)
                               throws ObjectNotFoundException {
        File file = new File(dbHomeDir, "idgen.xml");

        this.idgen = IDGenParser.getIDGenerator(file);

        return idgen;
    }

    /**
     * Write the id-generator to file
     *
     * @param txn
     * @param idgen
     * @throws IOException
     */
    public void saveIDGenerator(ITransaction txn, IDGenerator idgen)
                         throws IOException {
        File tmp = File.createTempFile("idgen.xml.", ".tmp", dbHomeDir);

        IDGenParser.saveIDGenerator(idgen, tmp);
        this.idgen = idgen;

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
     * @param txn
     * @param kstr
     * @return
     * @throws IOException
     * @throws ObjectNotFoundException
     * @throws ParserConfigurationException
     * @throws SAXException
     */
    public INode getNode(ITransaction txn, String kstr)
                  throws IOException, ObjectNotFoundException,
                         ParserConfigurationException, SAXException {
        File f = new File(dbHomeDir, kstr + ".xml");

        if (!f.exists()) {
            throw new ObjectNotFoundException("Object not found for key " + kstr + ".");
        }

        try {
            XmlDatabaseReader reader = new XmlDatabaseReader(nmgr);
            Node node = reader.read(f);

            return node;
        } catch (RuntimeException x) {
            nmgr.app.logError("Error reading " +f+": " + x.toString());
            throw new ObjectNotFoundException(x.toString());
        }
    }

    /**
     * Write the node to a temporary file.
     *
     * @param txn the transaction we're in
     * @param kstr the node's key
     * @param node the node to save
     * @throws IOException
     */
    public void saveNode(ITransaction txn, String kstr, INode node)
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
                    // move temporary file to permanent name
                    if (res.tmpfile.renameTo(res.file)) {
                        // success - delete tmp file
                        res.tmpfile.delete();
                    } else {
                        // error - leave tmp file and print a message
                        nmgr.app.logError("*** Error committing "+res.file);
                        nmgr.app.logError("*** Committed version is in "+res.tmpfile);
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

}

