/*
 *
 * @(#) FloatConstant.java 1.2@(#)
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
 * FESI.ClassFile.FloatConstant
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
  * <p> provides minimal support for FLOAT_CONSTANT CPE </p>
  */

class FloatConstant extends ConstantPoolEntry {

    private float	floating;

    /**
     * <p> construct a CONSTANT_FLOAT </p>
     *
     * @param f		the float value
     * @param cf	the class file
     */

    FloatConstant(float f, ClassFile cf) {
    	super(CONSTANT_FLOAT, cf);
    
    	floating = f;

    	addToConstantPool();
    }

    /**
     * <p> write the CONSTANT_FLOAT to the stream </p>
     *
     * @param dos 	the output stream
     *
     * @throws	IOException
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeByte(getTag());
    	dos.writeFloat(floating);
    }

    /**
     * <p> return the value of the constant </p>
     *
     * @return the value of the CONSTANT_FLOAT
     */

    float getValue() { return floating; }

    /**
     * @return object equality
     */

    public boolean equals(Object o) {
    	if (o instanceof Float) {
    	    return floating == ((Float)o).floatValue();
    	} else if (o instanceof FloatConstant) {
    	    FloatConstant fc = (FloatConstant)o;

    	    return floating == fc.getValue();
    	}

    	return false;
    }
}
