/*
 *
 * @(#) DoubleConstant.java 1.2@(#)
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
 * FESI.ClassFile.DoubleConstant
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
 * <p> implements a DOUBLE_CONSTANT CPE </p>
 */

class DoubleConstant extends ConstantPoolEntry {

    private double	doubler;

    /**
     * <p> construct a DOUBLE_CONSTANT CPE </p>
     *
     * @param d 	the double constant
     * @param cf	the class file
     */

    DoubleConstant(double d, ClassFile cf) {
    	super(CONSTANT_DOUBLE, cf);
    
    	doubler = d;

    	addToConstantPool();
    }

    /**
     * <p> write the constant CPE to the stream </p>
     *
     * @param dos the stream
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeByte(getTag());
    	dos.writeDouble(doubler);
    }

    /**
     * @return the double constant value.
     */

    double getValue() { return doubler; }

    /**
     * @return the object's equality.
     */

    public boolean equals(Object o) {
    	if (o instanceof Double) {
    	    return doubler == ((Double)o).doubleValue();
    	} else if (o instanceof DoubleConstant) {
    	    DoubleConstant dc = (DoubleConstant)o;

    	    return doubler == dc.getValue();
    	}

    	return false;
    }
}
