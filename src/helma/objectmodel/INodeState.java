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

import java.io.*;
import java.util.*;

/**
 * Interface that defines states of nodes
 */
public interface INodeState {
    public final static int TRANSIENT = -3;
    public final static int VIRTUAL = -2;
    public final static int INVALID = -1;
    public final static int CLEAN = 0;
    public final static int NEW = 1;
    public final static int MODIFIED = 2;
    public final static int DELETED = 3;
}
