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

import helma.objectmodel.INode;
import helma.objectmodel.db.IDGenerator;

/**
 * Interface that is implemented by Database wrappers
 */
public interface IDatabase {
    // db-related
    public void shutdown();

    // id-related
    public String nextID() throws ObjectNotFoundException;

    /**
     *
     *
     * @param transaction ...
     *
     * @return ...
     *
     * @throws Exception ...
     */
    public IDGenerator getIDGenerator(ITransaction transaction)
                               throws Exception;

    /**
     *
     *
     * @param transaction ...
     * @param idgen ...
     *
     * @throws Exception ...
     */
    public void saveIDGenerator(ITransaction transaction, IDGenerator idgen)
                         throws Exception;

    // node-related
    public INode getNode(ITransaction transaction, String key)
                  throws Exception;

    /**
     *
     *
     * @param transaction ...
     * @param key ...
     * @param node ...
     *
     * @throws Exception ...
     */
    public void saveNode(ITransaction transaction, String key, INode node)
                  throws Exception;

    /**
     *
     *
     * @param transaction ...
     * @param key ...
     *
     * @throws Exception ...
     */
    public void deleteNode(ITransaction transaction, String key)
                    throws Exception;

    // transaction-related
    public ITransaction beginTransaction();

    /**
     *
     *
     * @param transaction ...
     *
     * @throws DatabaseException ...
     */
    public void commitTransaction(ITransaction transaction)
                           throws DatabaseException;

    /**
     *
     *
     * @param transaction ...
     *
     * @throws DatabaseException ...
     */
    public void abortTransaction(ITransaction transaction)
                          throws DatabaseException;
}
