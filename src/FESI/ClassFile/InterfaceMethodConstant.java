/*
 *
 * @(#) InterfaceMethodConstant.java 1.2@(#)
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
 * FESI.ClassFile.InterfaceMethodConstant
 * </p> 
 *
 * @version 1.0
 * @author Laurence P. G. Cable
 */


package FESI.ClassFile;

import FESI.ClassFile.ClassFile;
import FESI.ClassFile.RefConstant;

/**
 * <p> this class provides minimal support for CONSTANT_INTERFACEMETHODREF CPE's </p>
 */

class InterfaceMethodConstant extends RefConstant {

    /**
     * <p> construct a CONSTANT_INTERFACEMETHODREF </p>
     *
     * @param cName	name of interface
     * @param nName	name of method
     * @param tName	method type descriptor
     * @param cf	class file
     *
     */

    InterfaceMethodConstant(String cName, String nName, String tName, ClassFile cf) {
	super(CONSTANT_INTERFACEMETHODREF, cName, nName, tName, cf);
    }
}
