/*
 *
 * @(#) ConstantValue.java 1.2@(#)
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
 * FESI.ClassFile.ConstantValue
 * </p> 
 *
 * @version 1.0
 * @author Laurence P. G. Cable
 */


package FESI.ClassFile;

import java.io.DataOutputStream;
import java.io.IOException;

import FESI.ClassFile.Attribute;
import FESI.ClassFile.ClassFile;
import FESI.ClassFile.ConstantPoolEntry;
import FESI.ClassFile.IntegerConstant;
import FESI.ClassFile.FloatConstant;
import FESI.ClassFile.DoubleConstant;
import FESI.ClassFile.LongConstant;
import FESI.ClassFile.StringConstant;

/**
 * <p>
 * This class provides Constant Pool support for all the simple constant
 * value data types supported in the class file format.
 * </p>
 */

class ConstantValue extends Attribute {

    private ConstantPoolEntry constant;

    /**
     * <p> construct an Attribute describing a Constant  </p>
     *
     * @param cf	the class file
     * @param cpe 	the cpe of the constant
     */

    private ConstantValue(ClassFile cf, ConstantPoolEntry cpe) {
    	super(Attribute.CONSTANTVALUE, cf);
    	constant = cpe;
    }

    /**
     * <p> Integer Constant </p>
     *
     * @param cf	the class file
     * @param ic	the Integer Constant
     */

    ConstantValue(ClassFile cf, IntegerConstant ic) {
    	this(cf, (ConstantPoolEntry)ic);
    }

    /**
     * <p> Long Constant </p>
     *
     * @param cf	the class file
     * @param lc	the Long Constant
     */

    ConstantValue(ClassFile cf, LongConstant lc) {
    	this(cf, (ConstantPoolEntry)lc);
    }

    /**
     * <p> Float Constant </p>
     *
     * @param cf	the class file
     * @param fc	the Float Constant
     */

    ConstantValue(ClassFile cf, FloatConstant fc) {
    	this(cf, (ConstantPoolEntry)fc);
    }

    /**
     * <p> Double Constant </p>
     *
     * @param cf	the class file
     * @param dc	the Double Constant
     */

    ConstantValue(ClassFile cf, DoubleConstant dc) {
    	this(cf, (ConstantPoolEntry)dc);
    }

    /**
     * <p> String Constant </p>
     *
     * @param cf	the class file
     * @param sc	the String Constant
     */

    ConstantValue(ClassFile cf, StringConstant sc) {
    	this(cf, (ConstantPoolEntry)sc);
    }

    /**
     * @return the length of this ConstantValue Attribute (minus header)
     */

    int getLength() { return 2; } 

    /**
     * @return the CPE of the constant represented
     */

    ConstantPoolEntry getConstant() { return constant; }

    /**
     * @return the CPE type tag of the constant represented
     */

    byte getConstantTag() { return constant.getTag(); }

    /**
     *<p> write the Attribute to the stream </p>
     *
     * @param dos the output stream
     *
     * @throws IOException
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeShort(getNameConstantPoolIndex());
    	dos.writeInt(getLength());
    	dos.writeShort(constant.getConstantPoolIndex());
    }

    /**
     * @return the objects equality.
     */

    public boolean equals(Object o) {
    	return constant.equals(o);
    }
}
