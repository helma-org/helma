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

import helma.framework.core.Application;


/**
 * An interface for objects that generate IDs (Strings) that are
 * unique for a specific type.
 */
public interface IDGenerator {

    /**
     * Init the ID generator for the given application.
     *
     * @param app
     */
    public void init(Application app);

    /**
     * Shut down the ID generator.
     */
    public void shutdown();

    /**
     * Generate a new ID for a specific type.
     *
     * @param dbmap
     * @return
     */
    public String generateID(DbMapping dbmap);

}
