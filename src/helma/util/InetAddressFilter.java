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
    Vector patterns;

    /**
     * Creates a new InetAddressFilter object.
     */
    public InetAddressFilter() {
        patterns = new Vector();
    }

    /**
     *
     *
     * @param address ...
     *
     * @throws IOException ...
     */
    public void addAddress(String address) throws IOException {
        int[] pattern = new int[4];
        StringTokenizer st = new StringTokenizer(address, ".");

        if (st.countTokens() != 4) {
            throw new IOException("\"" + address +
                                  "\" does not represent a valid IP address");
        }

        for (int i = 0; i < 4; i++) {
            String next = st.nextToken();

            if ("*".equals(next)) {
                pattern[i] = 256;
            } else {
                pattern[i] = (byte) Integer.parseInt(next);
            }
        }

        patterns.addElement(pattern);
    }

    /**
     *
     *
     * @param address ...
     *
     * @return ...
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

            for (int i = 0; i < 4; i++) {
                if ((pattern[i] < 255) && (pattern[i] != add[i])) { // not wildcard and doesn't match

                    break;
                }

                if (i == 3) {
                    return true;
                }
            }
        }

        return false;
    }
}
