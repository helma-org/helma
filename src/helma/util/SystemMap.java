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

package helma.util;


import java.util.*;

/**
 * Map class used internally by Helma.
 */
public class SystemMap extends HashMap {


    /**
     *  Construct an empty SystemMap.
     */
    public SystemMap() {
        super();
    }

    /**
     *  Construct an empty SystemMap with the given initial capacity.
     */
    public SystemMap(int initialCapacity) {
        super(initialCapacity);
    }

    /**
     *  Construct a SystemMap with the contents of Map map.
     */
    public SystemMap(Map map) {
        super(map);
    }

}
