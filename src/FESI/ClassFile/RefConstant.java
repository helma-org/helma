/*
 *
 * @(#) RefConstant.java 1.2@(#)
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
 * FESI.ClassFile.RefConstant
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
import FESI.ClassFile.ClassConstant;
import FESI.ClassFile.NameAndTypeConstant;

/**
 * <p>
 * this base class provides minimal support for METHODREF, FIELDREF, and
 * INTERFACEMETHODREF CPE's
 * </p>
 */

class RefConstant extends ConstantPoolEntry {

    private ClassConstant	clazz;
    private NameAndTypeConstant	nandt;

    /**
     * <p> construct a CPE </p>
     *
     * @param t		the CPE tag value
     * @param cName	the class name
     * @param nName	the name of the referenced field or method
     * @param tName	the type descriptor of the field or method
     * @param cf	the class file
     *
     */

    protected RefConstant(byte   t,     String cName,
    		          String nName, String tName, ClassFile cf) {
    	super(t, cf);
    
    	clazz = new ClassConstant(cName, cf);
    	nandt = new NameAndTypeConstant(nName, tName, cf);

    	addToConstantPool();
    }

    /**
     * <p> write the referenced object to the stream </p>
     *
     * @param dos the output stream
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeByte(getTag());
    	dos.writeShort(clazz.getConstantPoolIndex());
    	dos.writeShort(nandt.getConstantPoolIndex());
    }

    /**
     * @return the class constant for the referenced object
     */

    ClassConstant getClassObject() { return clazz; }

    /**
     * @return the name and type CPE for the referenced object
     */

    NameAndTypeConstant getNameAndType() { return nandt; }

    /**
     * @return object equality
     */

    public boolean equals(Object o) {
    	if (o instanceof String) {
    	    return ((String)o).equals(nandt.getName());
    	} else if (o instanceof RefConstant) {
            RefConstant rc = (RefConstant)o;

    	    return clazz.equals(rc.getClassObject()) &&
    	           nandt.equals(rc.getNameAndType());
    	} 

    	return false;
    }

}
