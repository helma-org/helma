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

package helma.objectmodel;

import helma.framework.core.Application;

import java.io.IOException;
import java.io.File;

/**
 * Interface that is implemented by Database wrappers
 */
public interface IDatabase {

    /**
     * Initialize the database with the given db directory and application.
     *
     * @param dbHome
     * @param app
     */
    public void init(File dbHome, Application app);

    /**
     * Let the database know we're shutting down.
     */
    public void shutdown();

    /**
     * Get the next ID from the db's ID generator
     * @return a unique id
     * @throws ObjectNotFoundException
     */
    public String nextID() throws ObjectNotFoundException;


    /**
     * Get the node from the database specified by the given key.
     *
     * @param transaction
     * @param key
     * @return
     * @throws IOException
     * @throws ObjectNotFoundException if no object exists for the key.
     */
    public INode getNode(ITransaction transaction, String key)
                  throws IOException, ObjectNotFoundException;

    /**
     * Save a node with the given key
     *
     * @param transaction
     * @param key
     * @param node
     * @throws IOException
     */
    public void saveNode(ITransaction transaction, String key, INode node)
                  throws IOException;

    /**
     * Delete the node specified by the given key.
     *
     * @param transaction ...
     * @param key ...
     * @throws IOException ...
     */
    public void deleteNode(ITransaction transaction, String key)
                    throws IOException;

    /**
     * Begin a new transaction.
     *
     * @return the transaction
     */
    public ITransaction beginTransaction();

    /**
     * Commit a transaction, making all changes persistent
     *
     * @param transaction
     * @throws DatabaseException
     */
    public void commitTransaction(ITransaction transaction)
                           throws DatabaseException;

    /**
     * Abort a transaction, rolling back all changes.
     *
     * @param transaction
     * @throws DatabaseException
     */
    public void abortTransaction(ITransaction transaction)
                          throws DatabaseException;
}
