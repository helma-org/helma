/*
 *
 * @(#) StringConstant.java 1.2@(#)
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
 * FESI.ClassFile.StringConstant
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
import FESI.ClassFile.UTF8Constant;

/**
 * <p> this class provides minimal support for CONSTANT_STRING CPE's <p>
 */

final class StringConstant extends ConstantPoolEntry {

    private UTF8Constant string;

    /**
     * <p> construct a CONSTANT_STRING CPE </p>
     *
     * @param str	the constant 
     * @param cf	the class file
     */

    StringConstant(String str, ClassFile cf) {
    	super(CONSTANT_STRING, cf);
    
    	string = new UTF8Constant(str, cf);

    	addToConstantPool();
    }

    /**
     * <p> construct a CONSTANT_STRING CPE </p>
     *
     * @param utf8	the utf8 constant 
     * @param cf	the class file
     */

    StringConstant(UTF8Constant utf8, ClassFile cf) {
    	super(CONSTANT_STRING, cf);

	string = utf8;

    	addToConstantPool();
    }

    /**
     * <p> write the constant to the stream </p>
     *
     * @param dos the output stream
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeByte(getTag());
    	dos.writeShort(string.getConstantPoolIndex());
    }

    /**
     * @return the string constant
     */

    String getString() { return string.getString(); }

    /**
     * @return object equality
     */

    public boolean equals(Object o) {
    	if (o instanceof String) {
    	    return string.equals(o);
    	} else if (o instanceof StringConstant) {
    	    return string.equals(((StringConstant)o).getString());
    	} 

    	return false;
    }
}
