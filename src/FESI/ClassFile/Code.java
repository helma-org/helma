/*
 *
 * @(#) Code.java 1.2@(#)
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
 * FESI.ClassFile.Code
 * </p> 
 *
 * @version 1.0
 * @author Laurence P. G. Cable
 */


package FESI.ClassFile;


import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Vector;

import FESI.ClassFile.Attribute;
import FESI.ClassFile.ClassConstant;

/**
 * <p>
 * The Code attribute is defined to describe the implementation for each
 * Method Implementation in a class. In particular it contains the byte
 * codes and exception information.
 * </p>
 */

final class Code extends Attribute {

    final static byte	OP_NOP			= (byte) 0x00;
    final static byte	OP_ACONST_NULL		= (byte) 0x01;

    // int consts

    final static byte	OP_ICONST_m1		= (byte) 0x02;
    final static byte	OP_ICONST_0		= (byte) 0x03;
    final static byte	OP_ICONST_1		= (byte) 0x04;
    final static byte	OP_ICONST_2		= (byte) 0x05;
    final static byte	OP_ICONST_3		= (byte) 0x06;
    final static byte	OP_ICONST_4		= (byte) 0x07;
    final static byte	OP_ICONST_5		= (byte) 0x08;

    // long consts

    final static byte	OP_LCONST_0		= (byte) 0x09;
    final static byte	OP_LCONST_1		= (byte) 0x0A;

    // float consts

    final static byte	OP_FCONST_0		= (byte) 0x0B;
    final static byte	OP_FCONST_1		= (byte) 0x0C;
    final static byte	OP_FCONST_2		= (byte) 0x0D;

    // double consts

    final static byte	OP_DCONST_0		= (byte) 0x0E;
    final static byte	OP_DCONST_1		= (byte) 0x0F;

    final static byte	OP_BIPUSH		= (byte) 0x10;
    final static byte	OP_SIPUSH		= (byte) 0x11;

    final static byte	OP_LDC			= (byte) 0x12;
    final static byte	OP_LDC_WIDE		= (byte) 0x13;
    final static byte	OP_LDC2_WIDE		= (byte) 0x14;


    // typed loads local

    final static byte	OP_ILOAD		= (byte) 0x15;
    final static byte	OP_LLOAD		= (byte) 0x16;
    final static byte	OP_FLOAD		= (byte) 0x17;
    final static byte	OP_DLOAD		= (byte) 0x18;
    final static byte	OP_ALOAD		= (byte) 0x19;

    // int loads

    final static byte	OP_ILOAD_0		= (byte) 0x1A;
    final static byte	OP_ILOAD_1		= (byte) 0x1B;
    final static byte	OP_ILOAD_2		= (byte) 0x1C;
    final static byte	OP_ILOAD_3		= (byte) 0x1D;

    // long loads

    final static byte	OP_LLOAD_0		= (byte) 0x1E;
    final static byte	OP_LLOAD_1		= (byte) 0x1F;
    final static byte	OP_LLOAD_2		= (byte) 0x20;
    final static byte	OP_LLOAD_3		= (byte) 0x21;

    // float loads

    final static byte	OP_FLOAD_0		= (byte) 0x22;
    final static byte	OP_FLOAD_1		= (byte) 0x23;
    final static byte	OP_FLOAD_2		= (byte) 0x24;
    final static byte	OP_FLOAD_3		= (byte) 0x25;

    // double loads

    final static byte	OP_DLOAD_0		= (byte) 0x26;
    final static byte	OP_DLOAD_1		= (byte) 0x27;
    final static byte	OP_DLOAD_2		= (byte) 0x28;
    final static byte	OP_DLOAD_3		= (byte) 0x29;

    // ref loads

    final static byte	OP_ALOAD_0		= (byte) 0x2A;
    final static byte	OP_ALOAD_1		= (byte) 0x2B;
    final static byte	OP_ALOAD_2		= (byte) 0x2C;
    final static byte	OP_ALOAD_3		= (byte) 0x2D;

    final static byte	OP_IALOAD		= (byte) 0x2E;
    final static byte	OP_LALOAD		= (byte) 0x2F;

    // array loads 

    final static byte	OP_FALOAD		= (byte) 0x30;
    final static byte	OP_DALOAD		= (byte) 0x31;
    final static byte	OP_AALOAD		= (byte) 0x32;
    final static byte	OP_BALOAD		= (byte) 0x33;
    final static byte	OP_CALOAD		= (byte) 0x34;
    final static byte	OP_SALOAD		= (byte) 0x35;

    final static byte	OP_ISTORE		= (byte) 0x36;
    final static byte	OP_LSTORE		= (byte) 0x37;
    final static byte	OP_FSTORE		= (byte) 0x38;
    final static byte	OP_DSTORE		= (byte) 0x39;
    final static byte	OP_ASTORE		= (byte) 0x3A;

    // int stores

    final static byte	OP_ISTORE_0		= (byte) 0x3B;
    final static byte	OP_ISTORE_1		= (byte) 0x3C;
    final static byte	OP_ISTORE_2		= (byte) 0x3D;
    final static byte	OP_ISTORE_3		= (byte) 0x3E;

    // long stores 

    final static byte	OP_LSTORE_0		= (byte) 0x3F;
    final static byte	OP_LSTORE_1		= (byte) 0x40;
    final static byte	OP_LSTORE_2		= (byte) 0x41;
    final static byte	OP_LSTORE_3		= (byte) 0x42;

    // float stores

    final static byte	OP_FSTORE_0		= (byte) 0x43;
    final static byte	OP_FSTORE_1		= (byte) 0x44;
    final static byte	OP_FSTORE_2		= (byte) 0x45;
    final static byte	OP_FSTORE_3		= (byte) 0x46;

    // double stores

    final static byte	OP_DSTORE_0		= (byte) 0x47;
    final static byte	OP_DSTORE_1		= (byte) 0x48;
    final static byte	OP_DSTORE_2		= (byte) 0x49;
    final static byte	OP_DSTORE_3		= (byte) 0x4A;

    // ref stores

    final static byte	OP_ASTORE_0		= (byte) 0x4B;
    final static byte	OP_ASTORE_1		= (byte) 0x4C;
    final static byte	OP_ASTORE_2		= (byte) 0x4D;
    final static byte	OP_ASTORE_3		= (byte) 0x4E;

    final static byte	OP_IASTORE		= (byte) 0x4F;

    // array stores

    final static byte	OP_LASTORE		= (byte) 0x50;
    final static byte	OP_FASTORE		= (byte) 0x51;
    final static byte	OP_DASTORE		= (byte) 0x52;
    final static byte	OP_AASTORE		= (byte) 0x53;
    final static byte	OP_BASTORE		= (byte) 0x54;
    final static byte	OP_CASTORE		= (byte) 0x55;
    final static byte	OP_SASTORE		= (byte) 0x56;

    final static byte	OP_POP			= (byte) 0x57;
    final static byte	OP_POP2			= (byte) 0x58;

    // dup's

    final static byte	OP_DUP			= (byte) 0x59;
    final static byte	OP_DUP_X1		= (byte) 0x5A;
    final static byte	OP_DUP_X2		= (byte) 0x5B;
    final static byte	OP_DUP2			= (byte) 0x5C;
    final static byte	OP_DUP2_X1		= (byte) 0x5D;
    final static byte	OP_DUP2_X2		= (byte) 0x5E;
    final static byte	OP_SWAP			= (byte) 0x5F;

    // arith

    final static byte	OP_IADD			= (byte) 0x60;
    final static byte	OP_LADD			= (byte) 0x61;
    final static byte	OP_FADD			= (byte) 0x62;
    final static byte	OP_DADD			= (byte) 0x63;

    final static byte	OP_ISUB			= (byte) 0x64;
    final static byte	OP_LSUB			= (byte) 0x65;
    final static byte	OP_FSUB			= (byte) 0x66;
    final static byte	OP_DSUB			= (byte) 0x67;

    final static byte	OP_IMUL			= (byte) 0x68;
    final static byte	OP_LMUL			= (byte) 0x69;
    final static byte	OP_FMUL			= (byte) 0x6A;
    final static byte	OP_DMUL			= (byte) 0x6B;

    final static byte	OP_IDIV			= (byte) 0x6C;
    final static byte	OP_FDIV			= (byte) 0x6E;
    final static byte	OP_LDIV			= (byte) 0x6D;
    final static byte	OP_DDIV			= (byte) 0x6F;

    // arith misc

    final static byte	OP_IREM			= (byte) 0x70;
    final static byte	OP_LREM			= (byte) 0x71;
    final static byte	OP_FREM			= (byte) 0x72;
    final static byte	OP_DREM			= (byte) 0x73;

    final static byte	OP_INEG			= (byte) 0x74;
    final static byte	OP_LNEG			= (byte) 0x75;
    final static byte	OP_FNEG			= (byte) 0x76;
    final static byte	OP_DNEG			= (byte) 0x77;

    final static byte	OP_ISHL			= (byte) 0x78;
    final static byte	OP_LSHL			= (byte) 0x79;

    final static byte	OP_ISHR			= (byte) 0x7A;
    final static byte	OP_LSHR			= (byte) 0x7B;

    final static byte	OP_IUSHR		= (byte) 0x7C;
    final static byte	OP_LUSHR		= (byte) 0x7D;

    final static byte	OP_IAND			= (byte) 0x7E;
    final static byte	OP_LAND			= (byte) 0x7F;

    final static byte	OP_IOR			= (byte) 0x80;
    final static byte	OP_LOR			= (byte) 0x81;

    final static byte	OP_IXOR			= (byte) 0x82;
    final static byte	OP_LXOR			= (byte) 0x83;
    
    // local int += const

    final static byte	OP_IINC			= (byte) 0x84;

    // int conversions

    final static byte	OP_I2L			= (byte) 0x85;
    final static byte	OP_I2F			= (byte) 0x86;
    final static byte	OP_I2D			= (byte) 0x87;

    // long conversions

    final static byte	OP_L2I			= (byte) 0x88;
    final static byte	OP_L2F			= (byte) 0x89;
    final static byte	OP_L2D			= (byte) 0x8A;

    // float conversions

    final static byte	OP_F2I			= (byte) 0x8B;
    final static byte	OP_F2L			= (byte) 0x8C;
    final static byte	OP_F2D			= (byte) 0x8D;

    // double conversions

    final static byte	OP_D2I			= (byte) 0x8E;
    final static byte	OP_D2L			= (byte) 0x8F;
    final static byte	OP_D2F			= (byte) 0x90;

    // int conversions

    final static byte	OP_I2B			= (byte) 0x91;
    final static byte	OP_I2C			= (byte) 0x92;
    final static byte	OP_I2S			= (byte) 0x93;

    // long comparision's

    final static byte	OP_LCMP			= (byte) 0x94;
    
    // float comparision's

    final static byte	OP_FCMPL		= (byte) 0x95;
    final static byte	OP_FCMPG		= (byte) 0x96;

    // double comparision's

    final static byte	OP_DCMPL		= (byte) 0x97;
    final static byte	OP_DCMPG		= (byte) 0x98;

    // int to zero comparisions

    final static byte	OP_IFEQ			= (byte) 0x99;
    final static byte	OP_IFNE			= (byte) 0x9A;
    final static byte	OP_IFLT			= (byte) 0x9B;
    final static byte	OP_IFGE			= (byte) 0x9C;
    final static byte	OP_IFGT			= (byte) 0x9D;
    final static byte	OP_IFLE			= (byte) 0x9E;

    // int to int comparision's

    final static byte	OP_IFICMPEQ		= (byte) 0x9F;
    final static byte	OP_IFICMPNE		= (byte) 0xA0;
    final static byte	OP_IFICMPLT		= (byte) 0xA1;
    final static byte	OP_IFICMPGE		= (byte) 0xA2;
    final static byte	OP_IFICMPGT		= (byte) 0xA3;
    final static byte	OP_IFICMPLE		= (byte) 0xA4;

    // ref comparisions

    final static byte	OP_IFACMPEQ		= (byte) 0xA5;
    final static byte	OP_IFACMPNE		= (byte) 0xA6;

    // goto

    final static byte	OP_GOTO			= (byte) 0xA7;

    final static byte	OP_JSR			= (byte) 0xA8;

    final static byte	OP_RET			= (byte) 0xA9;

    final static byte	OP_TABLESWITCH		= (byte) 0xAA;

    final static byte	OP_LOOKUP_SWITCH	= (byte) 0xAB;


    // return's

    final static byte	OP_IRETURN		= (byte) 0xAC;
    final static byte	OP_LRETURN		= (byte) 0xAD;
    final static byte	OP_FRETURN		= (byte) 0xAE;
    final static byte	OP_DRETURN		= (byte) 0xAF;
    final static byte	OP_ARETURN		= (byte) 0xB0;
    final static byte	OP_RETURN		= (byte) 0xB1;


    // getfield's

    final static byte	OP_GETSTATIC		= (byte) 0xB2;
    final static byte	OP_GETFIELD		= (byte) 0xB4;

    //  invoke virtual

    final static byte	OP_INVOKE_VIRTUAL	= (byte) 0xB6;

    // invoke static

    final static byte	OP_INVOKE_STATIC	= (byte) 0xB8;

    // method invocation

    final static byte	OP_INVOKE_SPECIAL	= (byte) 0xB7;

    // invoke interface

    final static byte	OP_INVOKE_INTERFACE	= (byte) 0xB9;

    // new

    final static byte	OP_NEW			= (byte) 0xBB;

    // array misc

    final static byte	OP_NEWARRAY		= (byte) 0xBD;

    final static byte	ARRAY_T_BOOLEAN		= (byte) 0x4;
    final static byte	ARRAY_T_CHAR		= (byte) 0x5;
    final static byte	ARRAY_T_FLOAT		= (byte) 0x6;
    final static byte	ARRAY_T_DOUBLE		= (byte) 0x7;
    final static byte	ARRAY_T_BYTE		= (byte) 0x8;
    final static byte	ARRAY_T_SHORT		= (byte) 0x9;
    final static byte	ARRAY_T_INT		= (byte) 0xA;
    final static byte	ARRAY_T_LONG		= (byte) 0xB;
    
    // putfield's

    final static byte	OP_PUTSTATIC		= (byte) 0xB3;
    final static byte	OP_PUTFIELD		= (byte) 0xB5;

    // array's

    final static byte	OP_ANEWARRAY		= (byte) 0xBD;
    final static byte	OP_ARRAYLENGTH		= (byte) 0xBE;

    // exceptions

    final static byte	OP_ATHROW		= (byte) 0xBF;

    // cast

    final static byte	OP_CHECKCAST		= (byte) 0xC0;

    // instanceof

    final static byte	OP_INSTANCEOF		= (byte) 0xC1;

    // monitor

    final static byte	OP_MONITOR_ENTER	= (byte) 0xC2;
    final static byte	OP_MONITOR_EXIT		= (byte) 0xC3;

    // wide

    final static byte	OP_WIDE			= (byte) 0xC4;

    // arrays

    final static byte	OP_MULTI_NEW_ARRAY 	= (byte) 0xC5;

    // compare to null

    final static byte	OP_IFNULL		= (byte) 0xc6;
    final static byte	OP_IFNONNULL		= (byte) 0xc7;

    // goto wide

    final static byte	OP_GOTO_WIDE		= (byte) 0xc8;

    final static byte	OP_JSR_WIDE		= (byte) 0xc9;


    /*
     * inst vars
     */

    private Vector		attributes;

    private int			length = 12;	// starting value

    private short		currentPC;

    private short		maxLocals;
    
    private short		maxStack;

    private Vector		byteCodes = new Vector(1);

    private Vector		exceptions;

    /**
     * <p> construct a Code Attribute </p>
     *
     * @param locals	number of words used to describe local vars
     * @param maxstack	max number of stack words used.
     *
     */

    Code(ClassFile cf, short locals, short stack) {
    	super(CODE, cf);

    	maxLocals = (locals >= 0 ? locals : 0);
    	maxStack  = (stack  >  2 ? stack  : 2);
    }
    
    /**
     * <p> write the code attribute to the stream </p>
     *
     * @param dos the output stream
     *
     * @throws IOException
     */
    
    void write(DataOutputStream dos) throws IOException {
    	int i;

    	dos.writeShort(getNameConstantPoolIndex());
    	dos.writeInt(getLength());
    	dos.writeShort(maxStack);
    	dos.writeShort(maxLocals);

    	// write the code ...

    	dos.writeInt(byteCodes.size());

    	for (i = 0; i < byteCodes.size(); i++) {
    		dos.writeByte(((Byte)byteCodes.elementAt(i)).byteValue());
    	}

    	// write exceptions (if any)

    	if (exceptions != null) {
    		dos.writeShort(exceptions.size());

    		for (i = 0; i < exceptions.size(); i++) {
    			((ExceptionTableEntry)exceptions.elementAt(i)).write(dos);
    		}
    	} else dos.writeShort(0);

    	// write attributes (if any)

    	if (attributes != null) {
    		dos.writeShort(attributes.size());

    		for (i = 0; i < attributes.size(); i ++) {
    			((Attribute)attributes.elementAt(i)).write(dos);
    		}
    	} else dos.writeShort(0);
    }

    /**
     * <p> returns the length of the Code attribute in bytes </p>
     *
     * @return the length of the attribute.
     */

    int getLength() { return length; }

    /**
     * @return object equality.
     */

    public boolean equals(Object o) {
    	return ((Object)this).equals(o);
    }

    /**
     * @return the current PC offset from the start of the method.
     */

    short getCurrentPC() { return currentPC; }

    /**
     * <p>
     * adds per Code attribute, can be used for SourceFile, LocalVariable,
     * and LineNumberTable attributes etc.
     * </p>
     *
     * @param attr Attribte to be added.
     */

    void addAttribute(Attribute attr) {
    	if (attributes == null) attributes = new Vector(1);

    	attributes.addElement(attr);
    	length += attr.getLength() + 6; // sizeof(Attribute)
    }

    /**
     * <p>
     * Adds an entry to the Exception handler table for this Code attribute.
     * An entry describes the start and stop pc offset within the Code fragment
     * for which an exception handler is provided, the start pc of the handler
     * code within the fragment itself, and the class of the exception type
     * for this handler.
     * </p>
     *
     * @param start	start pc offset for this exception handler range
     * @param stop	stop pc offset for this exception handler range
     * @param handler	handler pc start offset for this exception
     * @param ct	CONSTANT_CLASS describing the exception class handled
     */

    void addExceptionTableEntry(short start,   short	     stop,
    		                short handler, ClassConstant ct) {
    	exceptions.addElement(
    		new ExceptionTableEntry(start, stop, handler, ct)
    	);

    	length += 8; // sizeof(ExceptionTableEntry)
    }

    /**
     * <p> add an opcode to the implementation </p>
     */

    void addOp(byte opCode) {
    	byteCodes.addElement(new Byte(opCode));
    	currentPC++;
    	length++;
    }

    /**
     * <p> add an opcode and a 1 byte operand </p>
     */

    void addOp1(byte opCode, byte op1) {
    	byteCodes.addElement(new Byte(opCode));
    	byteCodes.addElement(new Byte(op1));
    	currentPC += 2;
    	length    += 2;
    }

    /**
     * <p> add an opcode and 2, 1 byte operands </p>
     */

    void addOp2(byte opCode, byte op1, byte op2) {
    	byteCodes.addElement(new Byte(opCode));
    	byteCodes.addElement(new Byte(op1));
    	byteCodes.addElement(new Byte(op2));
    	currentPC += 3;
    	length    += 3;
    }

    /**
     * <p> add an opcode and 4, 1 byte operands </p>
     */

    void addOp4(byte opCode, byte op1, byte op2, byte op3, byte op4) {
    	byteCodes.addElement(new Byte(opCode));
    	byteCodes.addElement(new Byte(op1));
    	byteCodes.addElement(new Byte(op2));
    	byteCodes.addElement(new Byte(op3));
    	byteCodes.addElement(new Byte(op4));
    	currentPC += 5;
    	length    += 5;

    }

    /**
     * <p> add an opcode and a 2 byte operand </p>
     */

    void addOpShort(byte opCode,short op) {
    	addOp2(opCode,
    	       (byte)((op >>> 8) & 0xff),
    	       (byte)( op        & 0xff)
    	);
    }

    /**
     * <p> add an opcode and a 4 byte operand </p>
     */

    void addOpInt(byte opCode,int op) {
    	addOp4(opCode,	
    	       (byte)((op >>> 24) & 0xff),
    	       (byte)((op >>> 16) & 0xff),
    	       (byte)((op >>>  8) & 0xff),
    	       (byte)( op         & 0xff)
    	);
    }

    /**
     * <p> increment the local word count </p>
     *
     * @param n the number of local words to increment by.
     */

    void incrLocals(short n) { maxLocals += n; }

    /**
     * <p> increment the max operand stack word count </p>
     *
     * @param n the number of words to increment the max stack count by
     */

    void incrMaxStack(short n) { maxStack += n; }
}

/*
 * private implementation class to represent exception table entries.
 */

final class ExceptionTableEntry {
    private short		startPC;
    private short		stopPC;
    private short		handlerPC;
    private ClassConstant	exceptionType;

    /*
     * construct and Exception Table Entry
     */

    ExceptionTableEntry(short start, short	   stop,
    		    short handler,   ClassConstant eType) {
    	super();

    	startPC       = start;
    	stopPC        = stop;
    	handlerPC     = handler;
    	exceptionType = eType;
    }

    /*
     * wrote the exception table entry to the stream
     */

    void write(DataOutputStream dos) throws IOException {
    	dos.writeShort(startPC);
    	dos.writeShort(stopPC);
    	dos.writeShort(handlerPC);
    	if (exceptionType != null)
    		dos.writeShort(exceptionType.getConstantPoolIndex());
    	else
    		dos.writeShort(0);
    }
}