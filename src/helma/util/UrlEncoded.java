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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.net.URLCodec;

import java.io.UnsupportedEncodingException;
import java.util.BitSet;

/**
 * A subclass of Jakarta Commons Codec URLCodec that offers
 * lazy encode/decode methods that only returns a new String
 * if actual work had to be done for encoding/decoding.
 * This is because URLCodec is a bit inefficient (e.g. it
 * preallocates buffers and stuff for each call to encode/decode),
 * and we call it often with short strings that don't need encoding.
 */
public final class UrlEncoded extends URLCodec {

    /**
     * URL-encode a string using the given encoding, or return it
     * unchanged if no encoding was necessary.
     *
     * @param str      The string to be URL-encoded
     * @param encoding the encoding to use
     * @return the URL-encoded string, or str if no encoding necessary
     */
    public static String smartEncode(String str, String encoding)
            throws UnsupportedEncodingException {
        int l = str.length();
        boolean needsSpaceEncoding = false;
        BitSet urlsafe = WWW_FORM_URL;

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            if (c == ' ') {
                needsSpaceEncoding = true;
            } else if (!urlsafe.get(c)) {
                return new URLCodec().encode(str, encoding);
            }
        }

        if (needsSpaceEncoding) {
            return str.replace(' ', '+');
        }

        return str;
    }

    /**
     * URL-decode a string using the given encoding,
     * or return it unchanged if no encoding was necessary.
     *
     * @param str      The string to be URL-decoded
     * @param encoding the encoding to use
     * @return the URL-decoded string, or str if no decoding necessary
     */
    public static String smartDecode(String str, String encoding)
            throws DecoderException, UnsupportedEncodingException {
        if ((str.indexOf('+') == -1) && (str.indexOf('%') == -1)) {
            return str;
        } else {
            return new URLCodec().decode(str, encoding);
        }
    }

}
