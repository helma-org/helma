/*
 *
 * @(#) FieldConstant.java 1.2@(#)
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
 * FESI.ClassFile.FieldConstant
 * </p> 
 *
 * @version 1.0
 * @author Laurence P. G. Cable
 */


package FESI.ClassFile;

import FESI.ClassFile.ClassFile;
import FESI.ClassFile.RefConstant;

/**
 * <p> implements a CONSTANT_FIELDREF CPE </p>
 */

final class FieldConstant extends RefConstant {

    /**
     * <p> construct a CONSTANT_FIELDREF CPE </p>
     *
     * @param cName	the class name
     * @param nName	the name of the field
     * @param tName 	the type descriptor for the field
     * @param cf	the class file
     */
    
    FieldConstant(String cName, String nName, String tName, ClassFile cf) {
    	super(CONSTANT_FIELDREF, cName, nName, tName, cf);
    }
}
