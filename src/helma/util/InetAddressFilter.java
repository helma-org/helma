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

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * A class for paranoid servers to filter IP addresses.
 */
public class InetAddressFilter {
    private Vector patterns;

    /**
     * Creates a new InetAddressFilter object.
     */
    public InetAddressFilter() {
        patterns = new Vector();
    }

    /**
     * Addes an address template to the address filter.
     *
     * @param address The string representation of the IP address, either version 4 or 6.
     *
     * @throws IOException if the parameter does not represent a valid IP address
     */
    public void addAddress(String address) throws IOException {
        boolean v6 = false;
        String separator = ".";
        int length = 4;
        int loop = 4;

        // check if this is a v4 or v6 IP address
        if (address.indexOf(":") > -1) {
            v6 = true;
            separator = ":.";
            length = 16;
            loop = 8;
        }

        int[] pattern = new int[length];

        StringTokenizer st = new StringTokenizer(address, separator);

        if (st.countTokens() != loop) {
            throw new IOException("\"" + address +
                                  "\" does not represent a valid IP address");
        }

        for (int i = 0; i < loop; i++) {
            String next = st.nextToken();

            if (v6) {
                if ("*".equals(next)) {
                    pattern[i*2] = pattern[i*2+1] = 256;
                } else if (next.length() == 0) {
                    pattern[i*2] = pattern[i*2+1] = 0;
                } else if (next.length() <= 2) {
                    pattern[i*2] = 0;
                    pattern[i*2+1] = (byte) Integer.parseInt(next, 16);
                } else {
                    pattern[i*2] = (byte) Integer.parseInt(next.substring(0,2), 16);
                    pattern[i*2+1] = (byte) Integer.parseInt(next.substring(2), 16);
                }
            } else {
                if ("*".equals(next)) {
                    pattern[i] = 256;
                } else {
                    pattern[i] = (byte) Integer.parseInt(next);
                }
            }
        }

        patterns.addElement(pattern);
    }

    /**
     * Check if the given address matches any of our patterns
     *
     * @param address the ip address to match
     *
     * @return true if we find a match
     */
    public boolean matches(InetAddress address) {
        if (address == null) {
            return false;
        }

        byte[] add = address.getAddress();

        if (add == null) {
            return false;
        }

        int l = patterns.size();

        for (int k = 0; k < l; k++) {
            int[] pattern = (int[]) patterns.elementAt(k);

            // is the address different version than pattern?
            if (pattern.length != add.length)
                continue;

            for (int i = 0; i < add.length; i++) {
                if ((pattern[i] < 255) && (pattern[i] != add[i])) {
                    // not wildcard and doesn't match
                    break;
                }

                if (i == add.length-1) {
                    return true;
                }
            }
        }

        return false;
    }
}
