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
import java.util.Enumeration;
import java.util.List;
import java.util.ArrayList;

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

    /**
     *  Split a string into an array of lines.
     *  @param str the string to split
     *  @return an array of lines
     */
    public static String[] splitLines(String str) {
        return str.split("\\r\\n|\\r|\\n");
    }

    /**
     * Get the character array for a string. Useful for use from
     * Rhino, where the Java String methods are not readily available
     * without constructing a new String instance.
     * @param str a string
     * @return the char array
     */
    public static char[] toCharArray(String str) {
        return str == null ? new char[0] : str.toCharArray();
    }

    /**
     * Collect items of a string enumeration into a String array.
     * @param enum an enumeration of strings
     * @return the enumeration values as string array
     */
    public static String[] collect(Enumeration enum) {
        List list = new ArrayList();
        while (enum.hasMoreElements()) {
            list.add(enum.nextElement());
        }
        return (String[]) list.toArray(new String[list.size()]);
    }

}
