/*
 *
 * @(#) IntegerConstant.java 1.2@(#)
 *
 * Copyright (c) 1997 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * SUN MAKES NO REPRESENTATIONS OR WARRANTIES ABOUT THE SUITABILITY OF THE
 * SOFTWARE, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE, OR NON-INFRINGEMENT. SUN SHALL NOT BE LIABLE FOR ANY DAMAGES
 * SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING
 * THIS SOFTWARE OR ITS DERIVATIVES.
 * 
 */

/**
 * <p>
 * FESI.ClassFile.IntegerConstant
 * </p> 
 *
 * @version 1.0
 * @author Laurence P. G. Cable
 */


package FESI.ClassFile;

import java.io.DataOutputStream;
import java.io.IOException;

import FESI.ClassFile.ClassFile;
import FESI.ClassFile.ConstantPoolEntry;

/**
 * <p> this class provides minimal support for the CONSTANT_INTEGER CPE </p>
 */

class IntegerConstant extends ConstantPoolEntry {

    private int	integer;

    /**
     * <p> construct a CONSTANT_INTEGER CPE </p>
     *
     * @param i		the integer constant
     * @param cf	the class file
     */

    IntegerConstant(int i, ClassFile cf) {
    	super(CONSTANT_INTEGER, cf);
    
    	integer = i;

    	addToConstantPool();
    }

    /**
     * <p> write the CONSTANT_INTEGER to the stream </p>
     *
     * @param dos the output stream
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeByte(getTag());
    	dos.writeInt(integer);
    }

    /**
     * @return the value of the CONSTANT_INTEGER
     */

    int getValue() { return integer; }

    /**
     * @return object equality
     */

    public boolean equals(Object o) {
    	if (o instanceof Integer) {
    	    return integer == ((Integer)o).intValue();
    	} else if (o instanceof IntegerConstant) {
    	    IntegerConstant ic = (IntegerConstant)o;

    	    return integer == ic.getValue();
    	}

    	return false;
    }
}
