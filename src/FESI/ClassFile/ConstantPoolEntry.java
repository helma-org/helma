/*
 *
 * @(#) ConstantPoolEntry.java 1.2@(#)
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
 * FESI.ClassFile.ConstantPoolEntry
 * </p> 
 *
 * @version 1.0
 * @author Laurence P. G. Cable
 */

package FESI.ClassFile;

import java.io.DataOutputStream;
import java.io.IOException;

import FESI.ClassFile.ClassFile;

/**
 * <p>
 * The ConstantPoolEntry is an abstract base class representing common 
 * behaviors of specific subtypes, as defined below and in the VM spec.
 * </p>
 *
 * <p>
 * In particular this class handles, equality, sharing, output and indexing
 * of all subtypes.
 * </p>
 */

abstract class ConstantPoolEntry {
    
    /*
     * subtype tag values.
     */
    
    final static byte CONSTANT_UTF8	       	  = 1;
    final static byte CONSTANT_UNICODE	          = 2;
    final static byte CONSTANT_INTEGER	          = 3;
    final static byte CONSTANT_FLOAT	      	  = 4;
    final static byte CONSTANT_LONG	          = 5;
    final static byte CONSTANT_DOUBLE	          = 6;
    final static byte CONSTANT_CLASS	      	  = 7;
    final static byte CONSTANT_STRING	          = 8;
    final static byte CONSTANT_FIELDREF	          = 9;
    final static byte CONSTANT_METHODREF     	  = 10;
    final static byte CONSTANT_INTERFACEMETHODREF = 11;
    final static byte CONSTANT_NAMEANDTYPE        = 12;

    /*
     * 
     */
    
    private byte   	tag;

    private ClassFile	classFile;

    private short	index = -1;

    /**
     * <p> construct the CPE, set the type tag and class file </p>
     */

    ConstantPoolEntry(byte t, ClassFile cf) {
    	tag	     = t;
    	classFile    = cf;
    }

    /**
     *
     */

    byte	getTag() { return tag; }

    /**
     * @return the CPE's constant pool index.
     */

    short getConstantPoolIndex() {
    	if (index == -1) index = classFile.addConstantPoolEntry(this);

    	return (short)index;
    }

    /**
     * @return the Class File this CPE is contained within.
     */

    ClassFile getClassFile() { return classFile; };

    /**
     * <p> * write the CPE to the stream </p> 
     *
     * @throws IOException
     */

    abstract void write(DataOutputStream dos) throws IOException;

    /**
     * <p> test the CPE for equality </p>
     *
     * @return object's equality.
     */

    public abstract boolean equals(Object o);

    /**
     * <p> add the CPE into the Class File's constant pool </p>
     */

    protected void addToConstantPool() {
    	if (index == -1) index = classFile.addConstantPoolEntry(this);
    }

    /**
     * @return are we in debug mode?
     */

    protected static boolean debug() { return ClassFile.debug(); }
}
