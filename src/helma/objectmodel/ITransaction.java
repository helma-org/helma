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


/**
 * This interface is kept for databases that are able
 * to run transactions.
 */
public interface ITransaction {

    public final int ADDED = 0;
    public final int UPDATED = 1;
    public final int DELETED = 2;

    /**
     * Complete the transaction by making its changes persistent.
     */
    public void commit() throws DatabaseException;

    /**
     * Rollback the transaction, forgetting the changed items
     */
    public void abort() throws DatabaseException;

    /**
     * Adds a resource to the list of resources encompassed by this transaction
     *
     * @param res the resource to add
     * @param status the status of the resource (ADDED|UPDATED|DELETED)
     */
    public void addResource(Object res, int status) throws DatabaseException;
}
