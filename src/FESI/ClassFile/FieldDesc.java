/*
 *
 * @(#) FieldDesc.java 1.2@(#)
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
 * FESI.ClassFile.FieldDesc
 * </p> 
 *
 * @version 1.0
 * @author Laurence P. G. Cable
 */


package FESI.ClassFile;


import java.io.DataOutputStream;
import java.io.IOException;

import FESI.ClassFile.ClassFile;
import FESI.ClassFile.UTF8Constant;
import FESI.ClassFile.Attribute;

/**
 * <p>
 * Implements the field_info structure of a class file, used to describe
 * the attributes of all fields implemented by this class. The class provides
 * minimal support to write the formatted structure to the stream.
 * </p>
 */

final class FieldDesc {

    final static short ACC_PUBLIC	    = 0x0001;
    final static short ACC_PRIVATE	    = 0x0002;
    final static short ACC_PROTECTED        = 0x0004;
    final static short ACC_STATIC	    = 0x0008;
    final static short ACC_FINAL	    = 0x0010;
    final static short ACC_VOLATILE	    = 0x0040;
    final static short ACC_TRANSIENT        = 0x0080;

    private UTF8Constant	name;
    private UTF8Constant	descriptor;

    private short		accessFlags;

    private ClassFile		classFile;

    private Attribute[]		attributes;

    /**
     * <p> construct a descriptor for a field. </p>
     *
     * @param field	name
     * @param desc	its type descriptor
     * @param flags	access flags
     * @param cf	the class file
     * @param attrs	any associated attributes
     *
     */

    FieldDesc(String field, String desc, short flags, ClassFile cf, Attribute[] attrs) {
    	super();

    	// we would validate here ...

    	name        = new UTF8Constant(field, cf);
    	descriptor  = new UTF8Constant(desc,  cf);
    	accessFlags = flags;
    	classFile   = cf;
    	attributes  = attrs;
    }

    /**
     * <p> write the field to the stream </p>
     *
     * @param dos	the output stream
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeShort(accessFlags);
    	dos.writeShort(name.getConstantPoolIndex());
    	dos.writeShort(descriptor.getConstantPoolIndex());

	if (attributes != null && attributes.length == 0) {
    	    dos.writeShort(attributes.length);

    	    for (int i = 0; i < attributes.length; i++) {
    		attributes[i].write(dos);
    	    }
	} else dos.writeShort(0);
    }
}
