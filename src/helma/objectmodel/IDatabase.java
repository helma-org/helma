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
import java.io.IOException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;

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
     * @throws IOException ...
     */
    public IDGenerator getIDGenerator(ITransaction transaction)
                throws IOException, ObjectNotFoundException;

    /**
     *
     *
     * @param transaction ...
     * @param idgen ...
     *
     * @throws IOException ...
     */
    public void saveIDGenerator(ITransaction transaction, IDGenerator idgen)
                 throws IOException;

    // node-related
    public INode getNode(ITransaction transaction, String key)
                  throws IOException, ObjectNotFoundException,
                         SAXException, ParserConfigurationException;

    /**
     *
     *
     * @param transaction ...
     * @param key ...
     * @param node ...
     *
     * @throws IOException ...
     */
    public void saveNode(ITransaction transaction, String key, INode node)
                  throws IOException;

    /**
     *
     *
     * @param transaction ...
     * @param key ...
     *
     * @throws IOException ...
     */
    public void deleteNode(ITransaction transaction, String key)
                    throws IOException;

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
