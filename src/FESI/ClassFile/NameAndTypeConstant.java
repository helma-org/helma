/*
 *
 * @(#) NameAndTypeConstant.java 1.2@(#)
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
 * FESI.ClassFile.NameAndTypeConstant
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
 * <p> this class provides minimal support for NAME_AND_TYPE CPE's </p>
 */

class NameAndTypeConstant extends ConstantPoolEntry {

    private UTF8Constant name;
    private UTF8Constant desc;

    /**
     * <p> construct a CONSTANT_NAMEANDTYPE CPE </p>
     *
     * @param n		the name 
     * @param d		the type
     * @param cf	the class file
     */

    NameAndTypeConstant(String n, String d, ClassFile cf) {
    	super(CONSTANT_NAMEANDTYPE, cf);

    	name = new UTF8Constant(n, cf);
    	desc = new UTF8Constant(d, cf);

    	addToConstantPool();
    }

    /**
     * <p> write the CPE to the stream </p>
     *
     * @param dos the output stream
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {

	if (debug()) {
	    System.err.println(getConstantPoolIndex() +
			       " NAME: "	      +
			       name.getConstantPoolIndex() +
			       " TYPE: "		   +
			       desc.getConstantPoolIndex()
	    );
	}

    	dos.writeByte(getTag());
    	dos.writeShort(name.getConstantPoolIndex());
    	dos.writeShort(desc.getConstantPoolIndex());
    }

    /**
     * @return the name string
     */

    String getName() { return name.getString(); }

    /**
     * @return the type descriptor string
     */

    String getDescriptor() { return desc.getString(); }

    /**
     * @return object equality
     */

    public boolean equals(Object o) {
    	if (o instanceof String) {
    	    return name.getString().equals(o) || 
    	           desc.getString().equals(o);
    	} else if (o instanceof NameAndTypeConstant) {
    	    NameAndTypeConstant nandt = (NameAndTypeConstant)o;

    	    return name.equals(nandt.getName()) &&
    	           desc.equals(nandt.getDescriptor());
    		       
    	}

    	return false;
    }
}
