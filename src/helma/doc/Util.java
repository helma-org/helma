/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.doc;

import java.io.*;
import java.util.*;

/**
 * 
 */
public final class Util {
    /**
     *
     *
     * @param line ...
     *
     * @return ...
     */
    public static String chopDelimiters(String line) {
        if (line == null) {
            return null;
        } else if (line.startsWith("/**")) {
            return line.substring(3).trim();
        } else if (line.startsWith("/*")) {
            return line.substring(2).trim();
        } else if (line.endsWith("*/")) {
            return line.substring(0, line.length() - 2);
        } else if (line.startsWith("*")) {
            return line.substring(1).trim();
        } else if (line.startsWith("//")) {
            return line.substring(2).trim();
        } else {
            return line;
        }
    }
}
