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

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * A proxy to java.net.URLEncoder which only encodes when there is actual work
 * to do. This is necessary because URLEncoder is quite inefficient (e.g. it
 * preallocates buffers and stuff), and we call it often with short string that
 * don't need encoding.
 */
public final class UrlEncoded {
    // Java 1.4 encode method to use instead of deprecated 1.3 version.
    private static Method encode = null;
    private static Method decode = null;

    // Initialize the encode and decode variables with the 1.4 methods if
    // available.
    // this code was adapted from org.apache.struts.utils.RequestUtils and 
    // org.apache.velocity.tools.view.tools.LinkTool
    
    static {
        try {
            // get version of encode method with two String args
            Class[] args = new Class[] { String.class, String.class };
            encode = URLEncoder.class.getMethod("encode", args);
            decode = URLDecoder.class.getMethod("decode", args);
        } catch (NoSuchMethodException e) {
            System.err.println("UrlEncoded: Can't find JDK 1.4 encode and decode methods. Using JDK 1.3 versions.");
        }
    }

    /**
     * URL-encodes a string using the given encoding, or return it unchanged if
     * no encoding was necessary.
     * This method uses the new URLEncoder.encode() method from java 1.4 if
     * available, otherwise the old deprecated version is used. Reflection is
     * used to find the appropriate method; if the reflection operations throw
     * exceptions other than UnsupportedEncodingException, it returns the url
     * encoded with the old URLEncoder.encode() method.
     * 
     * @param str The string to be URL-encoded
     * @param encoding the encoding to use
     * @return the URL-encoded string, or str if no encoding necessary
     */
    public static String encode(String str, String encoding)
        throws UnsupportedEncodingException {
        int l = str.length();
        boolean needsSpaceEncoding = false;

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            if (c == ' ') {
                needsSpaceEncoding = true;
            } else if (!(((c >= 'a') && (c <= 'z'))
                || ((c >= 'A') && (c <= 'Z')) || ((c >= '0') && (c <= '9')))) {
                if (encode != null) {
                    try {
                        return (String) encode.invoke(null, new Object[] { str,
                                encoding });
                    } catch (IllegalAccessException e) {
                        // don't keep trying if we get one of these
                        encode = null;

                        System.err.println("UrlEncoded: Can't access JDK 1.4 encode method ("
                            + e + "). Using deprecated version from now on.");
                    } catch (InvocationTargetException e) {
                        // this can only be a UnsupportedEncodingException:
                        Throwable ex = e.getTargetException();
                        if (ex instanceof UnsupportedEncodingException)
                            throw (UnsupportedEncodingException) ex;
                    }
                }
                return URLEncoder.encode(str);
            }
        }

        if (needsSpaceEncoding) {
            return str.replace(' ', '+');
        }

        return str;
    }

    /**
     * URL-decode a string using the given encoding, or return it unchanged if
     * no encoding was necessary.
     * This method uses the new URLDecoder.decode() method from java 1.4 if
     * available, otherwise the old deprecated version is used. Reflection is
     * used to find the appropriate method; if the reflection operations throw
     * exceptions other than UnsupportedEncodingException, it returns the url
     * decoded with the old URLDecoder.decode() method.
     * 
     * @param str The string to be URL-decoded
     * @param encoding the encoding to use
     * @return the URL-decoded string, or str if no decoding necessary
     */
    public static String decode(String str, String encoding)
        throws UnsupportedEncodingException {
        if ((str.indexOf('+') == -1) && (str.indexOf('%') == -1)) {
            return str;
        } else {
            if (decode != null) {
                try {
                    return (String) decode.invoke(null, new Object[] { str,
                            encoding });
                } catch (IllegalAccessException e) {
                    // don't keep trying if we get one of these
                    decode = null;

                    System.err.println("UrlEncoded: Can't access JDK 1.4 decode method ("
                        + e + "). Using deprecated version from now on.");
                } catch (InvocationTargetException e) {
                    // this can only be a UnsupportedEncodingException:
                    Throwable ex = e.getTargetException();
                    if (ex instanceof UnsupportedEncodingException)
                        throw (UnsupportedEncodingException) ex;
                }
            }
            return URLDecoder.decode(str);
        }
    }

}