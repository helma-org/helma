/*
 *
 * @(#) ClassFile.java 1.2@(#)
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
 * FESI.ClassFile.ClassFile
 * </p> 
 *
 * @version 1.0
 * @author Laurence P. G. Cable
 */

package FESI.ClassFile;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.Vector;

import FESI.ClassFile.Attribute;
import FESI.ClassFile.ClassConstant;
import FESI.ClassFile.ConstantPoolEntry;
import FESI.ClassFile.FieldDesc;
import FESI.ClassFile.MethodDesc;

/**
 * <p>
 * The ClassFile class is designed to provide lightweight, minimal support
 * for the runtime construction of Java VM ClassFile's, or Class
 * implementations.
 * </p>
 * <p>
 * The ClassFile provides API's to construct an in-core description of a
 * Java class implementation, and subsequently write that description to 
 * a stream which may then be either loaded into the VM via a ClassLoader
 * or written to some persistent store.
 * </p>
 * <p>
 * It should be noted that the ClassFile provide little or no validation of
 * the Class it describes during the construction of that description, and
 * therefore users of this class and package should be familiar with the 
 * contents of the Java VM Specification published by Addison-Wesley.
 * </p>
 */

final class ClassFile {

    /**
     * <p> the magic number for Java VM class files. </p>
     */

    final private static int MAGIC = 0xcafebabe;

    /**
     * <p> the major and minor version numbers for Java VM class files. </p>
     */

    final private static short MAJOR = 45;
    final private static short MINOR = 3;
    
    /**
     * <p> the access flags constants for Java VM class files. </p>
     */

    final static         short ACC_PUBLIC    = 0x0001;
    final static         short ACC_FINAL     = 0x0010;
    final static         short ACC_SUPER     = 0x0020;
    final static         short ACC_INTERFACE = 0x0200;
    final static         short ACC_ABSTRACT  = 0x0400;

    /*
     * inst vars represent the format of the classfile itself.
     */

    private Vector		constantPool = new Vector(1);

    private short		accessFlags  = (short)(ACC_PUBLIC | ACC_SUPER);

    private ClassConstant	thisClass;
    private ClassConstant   	superClass;

    private Vector	  	interfaces;
    private Vector		fields;
    private Vector		methods;
    private Vector		attributes;

    /**
     * @return are we debuging (used to print audit trail).
     */

    static boolean debug() { return false; }

    /**
     * <p> Construct a new ClassFile object. </p>
     *
     * @param tClass	name of "this" class
     *
     * @param sClass	name of superclass
     *
     */

    ClassFile(String tClass, String sClass) {
    	thisClass  = addClassConstant(tClass);
    	superClass = addClassConstant(sClass);
    }

    /**
     * <p> Write the constant pool to the stream </p>
     *
     * @param dos the stream to write to.
     */

    private void writeConstantPool(DataOutputStream dos) throws IOException {

	if (debug()) System.err.println("write constant pool: " + constantPool.size());

    	dos.writeShort(constantPool.size() + 1); // for index zero

    	for (int i = 0; i < constantPool.size(); i++) {
    	    ((ConstantPoolEntry)constantPool.elementAt(i)).write(dos);
    	}
    }

    /**
     * <p> Write the list of interfaces to the stream </p>
     *
     * @param dos the stream to write to.
     */

    private void writeInterfaces(DataOutputStream dos) throws IOException {
    	if (interfaces != null) {
	    if (debug()) System.err.println("write interfaces: " + interfaces.size());
    	    dos.writeShort(interfaces.size());

    	    for (int i = 0; i < interfaces.size(); i++) {
    	        dos.writeShort(
		    ((ConstantPoolEntry)interfaces.elementAt(i)).getConstantPoolIndex()
		);
    	    }
    	} else dos.writeShort(0);
    }

    /**
     * <p> Write the list of Fields defs to the stream </p>
     *
     * @param dos the stream to write to.
     */

    private void writeFields(DataOutputStream dos) throws IOException {
    	if (fields != null) {
	    if (debug()) System.err.println("write fields: " + fields.size());

	    dos.writeShort(fields.size());

    	    for (int i = 0; i < fields.size(); i++) {
    	        ((FieldDesc)fields.elementAt(i)).write(dos);
    	    }
    	} else dos.writeShort(0);
    }

    /**
     * <p> Write the list of Method defs to the stream. </p>
     *
     * @param dos the stream to write to.
     */

    private void writeMethods(DataOutputStream dos) throws IOException {
    	if (methods != null) {
	    if (debug()) System.err.println("write methods: " + methods.size());

    	    dos.writeShort(methods.size());

    	    for (int i = 0; i < methods.size(); i++) {
    	        ((MethodDesc)methods.elementAt(i)).write(dos);
    	    }
    	} else dos.writeShort(0);

    }

    /**
     * <p> Write the list of Attributes to the stream </p>
     *
     * @param dos the stream to write to.
     */

    private void writeAttributes(DataOutputStream dos) throws IOException {
    	if (attributes != null) {
	    if (debug()) System.err.println("write attributes: " + attributes.size());

    	    dos.writeShort(attributes.size());

    	    for (int i = 0; i < attributes.size(); i++) {
    	        ((Attribute)attributes.elementAt(i)).write(dos);
    	    }
    	} else dos.writeShort(0);
    }

    /**
     * <p> Write the ClassFile to the Stream </p>
     *
     * @param os the stream to write to.
     */

    public synchronized void write(OutputStream os) throws IOException {
    	DataOutputStream dos = new DataOutputStream(os);

    	try {
    	    dos.writeInt(MAGIC);

    	    dos.writeShort(MINOR);
    	    dos.writeShort(MAJOR);

    	    writeConstantPool(dos);

	    if (debug()) System.err.println("access: " + accessFlags);

    	    dos.writeShort(accessFlags);		

    	    dos.writeShort(thisClass.getConstantPoolIndex());
    	    dos.writeShort(superClass.getConstantPoolIndex());

    	    writeInterfaces(dos);

    	    writeFields(dos);

    	    writeMethods(dos);

    	    writeAttributes(dos);

    	    dos.close(); // all done!
    	} catch (IOException ioe) {
	    System.err.println("Bad IO");
    	} catch (Exception e) {
    	    System.err.println("Oops");
    	}
    }

    /**
     * <p> Add an entry to the Constant Pool. </p>
     *
     * @param cpe the new constant pool entry
     *
     * @return the index of the new entry in the pool
     */

    public synchronized short addConstantPoolEntry(ConstantPoolEntry cpe) {
	if (!constantPool.contains(cpe)) constantPool.addElement(cpe);

	return (short)(constantPool.indexOf(cpe) + 1);
    }

    /**
     * <p> Find a matching Constant Pool Entry. </p>
     *
     * @param tag	The tag value of the constant pool entries to match on.
     * @param value	The value to match on.
     *
     * @return the matching entry or null.
     */

    synchronized ConstantPoolEntry match(byte tag, Object value) {
        for (int i = 0; i < constantPool.size(); i++) {
            ConstantPoolEntry cpe = (ConstantPoolEntry)constantPool.elementAt(i);

            if (cpe.getTag() == tag && cpe.equals(value))
		return cpe;
	}

        return null;
    }

    /**
     * @return the current value of the accessFlags.
     */

    public synchronized short getAccessFlags() { return accessFlags; }

    /**
     * <p> modify the value of the Class File's access flags </p>
     *
     * @param newf the new flag values. [NOT VALIDATED]
     */

    public synchronized void setAccessFlags(short newf) {

	// TODO - verify new flag combination.

	accessFlags = newf;
    }

    /**
     * @param newMethod the method desc to add to the class file.
     */

    public synchronized void addMethodDesc(MethodDesc newMethod) {
    	if (methods == null) methods = new Vector(1);

    	methods.addElement(newMethod);
    }

    /**
     * @param newField the field desc to add to the class file.
     */

    public synchronized void addFieldDesc(FieldDesc newField) {
	if (fields == null) fields = new Vector(1);

	fields.addElement(newField);
    }

    /**
     * @param sConstant add a CONSTANT_STRING to the ClassFile.
     * 
     * @param sConstant the string value to add.
     * 
     * @return The new StringConstant
     */

    public StringConstant addStringConstant(String sConstant) {
	UTF8Constant c = (UTF8Constant)match(ConstantPoolEntry.CONSTANT_UTF8, sConstant);

	if (c == null) {
	    c = new UTF8Constant(sConstant, this);
	}

	StringConstant s = new StringConstant(c, this);

	return s;
    }

    /**
     * <p> Add a new CONSTANT_INTEGER to the Constant Pool </p>
     *
     * @param iConstant the integer value to add.
     * 
     * @return the new IntegerConstant.
     */

    public IntegerConstant addIntegerConstant(int iConstant) {
	IntegerConstant c = (IntegerConstant)match(ConstantPoolEntry.CONSTANT_INTEGER, new Integer(iConstant));

	if (c == null) {
	    c = new IntegerConstant(iConstant, this);
	}

	return c;
    }

    /**
     * <p> Add a new UTF8_CONSTANT to the constant pool </p>
     *
     * @param sConstant the string to add.
     *
     * @return the new UUTF8Constant
     */

    public UTF8Constant addUTF8Constant(String sConstant) {
	UTF8Constant c = (UTF8Constant)match(ConstantPoolEntry.CONSTANT_UTF8, sConstant);

	if (c == null) {
		c = new UTF8Constant(sConstant, this);
	}

	return c;
    }

    /**
     * <p> add a new CONSTANT_CLASS to the Constant Pool </p>
     *
     * @param classConstant the name of the class to add
     *
     * @return the newly ClassConstant
     */

    public ClassConstant addClassConstant(String classConstant) {
	ClassConstant c = (ClassConstant)match(ConstantPoolEntry.CONSTANT_CLASS, classConstant);

	if (c == null) {
		c = new ClassConstant(classConstant, this);
	}

	return c;
    }

    /**
     * <p> add a CONSTANT_METHOD to the constant pool </p>
     *
     * @param cName the name of the defining class
     * @param mName the method name
     * @param tName the fully qualified type descriptor for the method
     *
     * @return the new created CONSTANT_METHOD
     */

    public MethodConstant addMethodConstant(String cName, String mName, String tName) {
	return new MethodConstant(cName, mName, tName, this);
    }

    /**
     *
     *
     * @param cName the name of the defining class
     * @param fName the name of the field
     * @param tName the fully qualified type descriptor of the field
     *
     * @return the new created CONSTANT_FIELD
     */

    public FieldConstant addFieldConstant(String cName, String fName, String tName) {
	return new FieldConstant(cName, fName, tName, this);
    }

    /**
     * <p> add the name of an interface this class implements to the constant pool </p>
     * 
     * @param iName the name of the interface
     */

    public void addInterface(String iName) {
	if (interfaces == null) interfaces = new Vector(1);

	interfaces.addElement((Object)addClassConstant(iName));
    }

    /**
     * <p>
     * convenience routine to take a type name and map it to the internal form.
     * java.lang.Object -> java/lang/Object
     * </p>
     *
     * @param str the string to map
     *
     * @return the mapped string value.
     */

    public static String fullyQualifiedForm(String str) {
	return str.replace('.', '/');
    }


    /**
     * <p>
     * convenience routine to construct type descriptors from fully
     * qualified names, e.g: java.lang.Object => Ljava/lang/Object;
     * </p>
     *
     * @param str name of a java "type"
     *
     * @return the class descriptor.
     */

    public static String fieldType(String str) {
	return "L" + ClassFile.fullyQualifiedForm(str) + ";";
    }

}
