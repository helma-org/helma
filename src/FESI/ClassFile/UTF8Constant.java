/*
 *
 * @(#) UTF8Constant.java 1.2@(#)
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
 * FESI.ClassFile.UTF8Constant
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
 * <p> this class provides minimal support for CONSTANT_UTF8 CPE's </p>
 */

class UTF8Constant extends ConstantPoolEntry {

    private String string;

    /**
     * <p> construct a CONSTANT_UTF8 CPE </p>
     *
     * @param s 	the string
     * @param cf	the class file
     */

    UTF8Constant(String s, ClassFile cf) {
    	super(CONSTANT_UTF8, cf);
    
    	string = s;

    	addToConstantPool();
    }

    /**
     * <p> write the CPE to the output stream </p>
     *
     * @param dos the output stream
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeByte(getTag());
    	dos.writeUTF(string);
    }

    /**
     * @return the string constant
     */

    String getString() { return string; }

    /**
     * @return object equality
     */

    public boolean equals(Object o) {
    	if (o instanceof String) {
    	    return string.equals((String)o);
    	} else if (o instanceof UTF8Constant) {
    	    return string.equals(((UTF8Constant)o).getString());
    	}

    	return false;
    }
}
