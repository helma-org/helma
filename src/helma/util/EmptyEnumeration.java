// EmptyEnumeration.java
// Copyright (c) Hannes Wallnöfer 2001

package helma.util;

import java.util.Enumeration;

/**
 * Utility class for empty enum
 */
 
public class EmptyEnumeration implements Enumeration {


    public boolean hasMoreElements () {
	return false;
    }

    public Object nextElement () {
	return null;
    }

}
