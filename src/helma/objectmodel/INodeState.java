// INodeState.java
// Copyright (c) Hannes Wallnöfer 2001
 
package helma.objectmodel;

import java.util.*;
import java.io.*;

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




