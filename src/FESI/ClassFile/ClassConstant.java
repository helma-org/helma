/*
 *
 * @(#) ClassConstant.java 1.2@(#)
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
 * FESI.ClassFile.ClassConstant
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

class ClassConstant extends ConstantPoolEntry {

    private UTF8Constant name;

    /**
     * <p> Construct a CONSTANT_CLASS constant pool entry </p>
     */

    ClassConstant(String className, ClassFile cf) {
    	super(CONSTANT_CLASS, cf);
    
    	name = cf.addUTF8Constant(ClassFile.fullyQualifiedForm(className));

    	addToConstantPool();
    }

    /**
     * <p> write the CONSTANT_CLASS to the stream </p>
     *
     * @param dos the stream.
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {
	
	if (debug()) {
	    System.err.println(getConstantPoolIndex() +
			       " CLASS: "	      +
			       name.getConstantPoolIndex()
	    );
	}

    	dos.writeByte(getTag());
    	dos.writeShort(name.getConstantPoolIndex());
    }

    /**
     * <p> return the class represented by the CONSTANT_CLASS </p>
     *
     * @return the name of the class
     */

    String getClassName() { return name.getString(); }

    /**
     * <p> returns the Class object for the class represented by the constant. </p>
     *
     * @return The java.lang.Class object for the class.
     */

    Class getClassObject() throws ClassNotFoundException {
    	return Class.forName(name.getString());
    }

    /**
     * <p> compare the object, by name or value. </p>
     * 
     * @param the object for comparison
     * 
     * @return object equality.
     */

    public boolean equals(Object o) {
    	if (o == null) return false;

    	if (o instanceof String) {
    	    return ((String)o).equals(name.getString());
    	} else if (o instanceof ClassConstant) {
    	    ClassConstant cc = (ClassConstant)o;

    	    return name.getString().equals(cc.getClassName());
    	}
    
		return false;
	}
}
