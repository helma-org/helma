// UrlEncoded.java
// (c) 2002 Hannes Wallnoefer

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


    public static String encode (String str) {
        int l = str.length();
        boolean needsSpaceEncoding = false;
        for (int i=0; i<l; i++) {
            char c = str.charAt(i);
            if (c == ' ') {
                needsSpaceEncoding = true;
            } else if (!(c>='a' && c<='z' ||
                       c>='A' && c<='Z' ||
                       c>='0' && c<='9')) {
                return URLEncoder.encode (str);
            }
        }
        if (needsSpaceEncoding)
            return str.replace (' ', '+');
        return str;
    }

}