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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.BitSet;

/**
 * A proxy to java.net.URLEncoder which only encodes when there is actual work
 * to do. This is necessary because URLEncoder is quite inefficient (e.g. it
 * preallocates buffers and stuff), and we call it often with short string that
 * don't need encoding.
 */
public final class UrlEncoded {

    static BitSet dontNeedEncoding;

    static {
        dontNeedEncoding = new BitSet(256);
        int i;
        for (i = 'a'; i <= 'z'; i++)
            dontNeedEncoding.set(i);
        for (i = 'A'; i <= 'Z'; i++)
            dontNeedEncoding.set(i);
        for (i = '0'; i <= '9'; i++)
            dontNeedEncoding.set(i);
        dontNeedEncoding.set(' '); // encoded separately
        dontNeedEncoding.set('-');
        dontNeedEncoding.set('_');
        dontNeedEncoding.set('.');
        dontNeedEncoding.set('*');
    }

    /**
     * URL-encodes a string using the given encoding, or return it unchanged if
     * no encoding was necessary.
     *
     * @param str The string to be URL-encoded
     * @param encoding the encoding to use
     * @return the URL-encoded string, or str if no encoding necessary
     * @throws UnsupportedEncodingException encoding is not supported
     */
    public static String encode(String str, String encoding)
            throws UnsupportedEncodingException {
        int l = str.length();
        boolean needsSpaceEncoding = false;

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            if (c == ' ') {
                needsSpaceEncoding = true;
            } else if (!dontNeedEncoding.get(c)) {
                return URLEncoder.encode(str, encoding);
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
     * 
     * @param str The string to be URL-decoded
     * @param encoding the encoding to use
     * @return the URL-decoded string, or str if no decoding necessary
     * @throws UnsupportedEncodingException encoding is not supported
     */
    public static String decode(String str, String encoding)
            throws UnsupportedEncodingException {
        if ((str.indexOf('+') == -1) && (str.indexOf('%') == -1)) {
            return str;
        } else {
            return URLDecoder.decode(str, encoding);
        }
    }

}