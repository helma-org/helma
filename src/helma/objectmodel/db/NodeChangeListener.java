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

import java.util.List;

public interface NodeChangeListener {

    /**
     * Called when a transaction is committed that has created, modified, 
     * deleted or changed the child collection one or more nodes.
     */
    public void nodesChanged(List inserted, List updated, List deleted, List parents);

}
