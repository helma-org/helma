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

import java.io.*;
import java.util.ArrayList;

/**
 * A subclass of Vector that implements the Externalizable interface in order
 * to be able to control how it is serialized and deserialized.
 */
public class ExternalizableVector extends ArrayList implements Externalizable {
    static final long serialVersionUID = 2316243615310540423L;

    /**
     *
     *
     * @param in ...
     *
     * @throws IOException ...
     */
    public synchronized void readExternal(ObjectInput in)
                                   throws IOException {
        try {
            int size = in.readInt();

            for (int i = 0; i < size; i++)
                add(in.readObject());
        } catch (ClassNotFoundException x) {
            throw new IOException(x.toString());
        }
    }

    /**
     *
     *
     * @param out ...
     *
     * @throws IOException ...
     */
    public synchronized void writeExternal(ObjectOutput out)
                                    throws IOException {
        int size = size();

        out.writeInt(size);

        for (int i = 0; i < size; i++)
            out.writeObject(get(i));
    }
}
