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

import java.net.URLEncoder;

/**
 *  A proxy to java.net.URLEncoder which only encodes when
 *  there is actual work to do. This is necessary because
 *  URLEncoder is quite inefficient (e.g. it preallocates
 *  buffers and stuff), and we call it often with
 *  short string that don't need encoding.
 */
public final class UrlEncoded {
    /**
     *
     *
     * @param str ...
     *
     * @return ...
     */
    public static String encode(String str) {
        int l = str.length();
        boolean needsSpaceEncoding = false;

        for (int i = 0; i < l; i++) {
            char c = str.charAt(i);

            if (c == ' ') {
                needsSpaceEncoding = true;
            } else if (!(((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) ||
                           ((c >= '0') && (c <= '9')))) {
                return URLEncoder.encode(str);
            }
        }

        if (needsSpaceEncoding) {
            return str.replace(' ', '+');
        }

        return str;
    }
}
