// ExternalizableVector.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.objectmodel.db;

import java.io.*;
import java.util.Vector;

/**
 * A subclass of Vector that implements the Externalizable interface in order 
 * to be able to control how it is serialized and deserialized.
 */

public class ExternalizableVector extends Vector implements Externalizable {

    static final long serialVersionUID = 2316243615310540423L;

    public synchronized void readExternal (ObjectInput in) throws IOException {
	try {
	    int size = in.readInt ();
	    for (int i=0; i<size; i++)
	        addElement (in.readObject ());
	} catch (ClassNotFoundException x) {
	    throw new IOException (x.toString ());
	}
    }

    public synchronized void writeExternal (ObjectOutput out) throws IOException {
	int size = size ();
	out.writeInt (size);
	for (int i=0; i<size; i++)
	    out.writeObject (elementAt (i));
    }

}




