/*
 *
 * @(#) MethodDesc.java 1.2@(#)
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
 * FESI.ClassFile.MethodDesc
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
 * <p> this class provides minimal support for method_info structures </p>
 */

final class MethodDesc {

    final static short ACC_PUBLIC       = 0x0001;
    final static short ACC_PRIVATE      = 0x0002;
    final static short ACC_PROTECTED    = 0x0004;
    final static short ACC_STATIC       = 0x0008;
    final static short ACC_FINAL	= 0x0010;
    final static short ACC_SYNCHRONIZED = 0x0020;
    final static short ACC_NATIVE       = 0x0100;
    final static short ACC_ABSTRACT     = 0x0400;

    private UTF8Constant	name;
    private UTF8Constant	descriptor;

    private short		accessFlags;

    private ClassFile	classFile;

    private Attribute[]	attributes;

    /**
     * <p> construct a descriptor for a method </p>
     *
     * @param method	the name of the method
     * @param desc	a type descriptor for its signature
     * @param flags	access flags
     * @param cf	the class file
     * @param attrs 	arbitrary attributes
     *
     */

    MethodDesc(String method, String desc, short flags, ClassFile cf, Attribute[] attrs) {
    	super();

    	// we would validate here ...

    	name        = new UTF8Constant(method, cf);
    	descriptor  = new UTF8Constant(desc,   cf);
    	accessFlags = flags;
    	classFile   = cf;
    	attributes  = attrs;
    }

    /**
     * <p> write the method to the stream </p>
     *
     * @param dos the output stream
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeShort(accessFlags);
    	dos.writeShort(name.getConstantPoolIndex());
    	dos.writeShort(descriptor.getConstantPoolIndex());

	if (attributes != null && attributes.length > 0) {
    	    dos.writeShort(attributes.length);

    	    for (int i = 0; i < attributes.length; i++) {
    	    	attributes[i].write(dos);
    	    }
	} else dos.writeShort(0);
    }
}
