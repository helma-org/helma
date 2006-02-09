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

import java.util.ArrayList;

/**
 * A subclass of ArrayList that adds an addSorted(Object) method to
 */
public class SubnodeList extends ArrayList {

   /**
    * Inserts the specified element at the specified position in this
    * list without performing custom ordering
    *
    * @param obj element to be inserted.
    */
    public boolean addSorted(Object obj)  {
        return add(obj);
    }

}
