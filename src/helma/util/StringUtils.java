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

package helma.util;


import java.util.StringTokenizer;

/**
 * Utility class for String manipulation.
 */
public class StringUtils {


    /**
     *  Split a string into an array of strings. Use comma and space
     *  as delimiters.
     */
    public static String[] split(String str) {
        return split(str, ", \t\n\r\f");
    }

    /**
     *  Split a string into an array of strings.
     */
    public static String[] split(String str, String delim) {
        if (str == null) {
            return new String[0];
        }
        StringTokenizer st = new StringTokenizer(str, delim);
        String[] s = new String[st.countTokens()];
        for (int i=0; i<s.length; i++) {
            s[i] = st.nextToken();
        }
        return s;
    }

}
