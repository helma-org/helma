/*
 *
 * @(#) Attribute.java 1.2@(#)
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
 * FESI.ClassFile.Attribute
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
 * <p>
 * The Attribute class is an abstract base class for all Attribute types
 * found in the Java VM ClassFile format specification. This is a simple
 * implementationd designed to support the minimal functionaliuty required
 * to emit a valid ClassFile stream.
 * </p>
 */

abstract class Attribute {

    final static String SOURCEFILE         = "SourceFile";
    final static String CONSTANTVALUE      = "ConstantValue";
    final static String LOCALVARIABLETABLE = "LocalVariableTable";
    final static String EXCEPTIONS         = "Exceptions";
    final static String LINENUMBERTABLE    = "LineNumberTable";
    final static String CODE    	   = "Code";
    
    private UTF8Constant name;
    private ClassFile    classFile;

    /**
     * <p> Construct an Attribute, enter it into the ConstantPool. </p>
     */

    protected Attribute(String n, ClassFile cf) {
    	UTF8Constant utf8 = (UTF8Constant)
    		cf.match(ConstantPoolEntry.CONSTANT_UTF8, (Object)n);

    	if (utf8 == null) utf8 = new UTF8Constant(n, cf);
    		
    	name	  = utf8;
    	classFile = cf;
    }

    /**
     * @return the ClassFile this Attribute is contained within
     */

    ClassFile getClassFile() { return classFile; }

    /**
     * @return the "name" of the Attribute.
     */

    String getName() { return name.getString(); }

    /**
     * @return get the index of this Attribute in the ConstantPool 
     */

    short getNameConstantPoolIndex() {
    	return name.getConstantPoolIndex();
    }

    /**
     * @return the length of the attribute as defined by the concrete subclass.
     */

    abstract int getLength();

    /**
     * <p> write the concrete Attribute subclass to the stream <p>
     *
     * @throws IOException
     */

    abstract void write(DataOutputStream dos) throws IOException;

    /**
     * <p> Compare this Attribute with the object and return equality. </p>
     *
     * @return is it equal
     */

    abstract public boolean equals(Object o);

}
