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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 
 */
public class MD5Encoder {
    private static MessageDigest md;

    /** used by commandline script to create admin username & password          */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("\n\nUsage: helma.util.MD5Encoder <username> <password>");
            System.out.println("Output:");
            System.out.println("adminUsername=<encoded username>");
            System.out.println("adminPassword=<encoded password>\n");
            System.exit(0);
        }

        System.out.println("adminUsername=" + encode(args[0]));
        System.out.println("adminPassword=" + encode(args[1]));
    }

    /**
     *
     *
     * @param str ...
     *
     * @return ...
     *
     * @throws NoSuchAlgorithmException ...
     */
    public static String encode(String str) throws NoSuchAlgorithmException {
        return encode(str.getBytes());
    }

    /**
     *
     *
     * @param message ...
     *
     * @return ...
     *
     * @throws NoSuchAlgorithmException ...
     */
    public static String encode(byte[] message) throws NoSuchAlgorithmException {
        md = MessageDigest.getInstance("MD5");

        byte[] b = md.digest(message);
        StringBuffer buf = new StringBuffer(b.length * 2);

        for (int i = 0; i < b.length; i++) {
            int j = (b[i] < 0) ? (256 + b[i]) : b[i];

            if (j < 16) {
                buf.append("0");
            }

            buf.append(Integer.toHexString(j));
        }

        return buf.toString();
    }

    /**
     *  Convert a long to a byte array.
     */
    public static byte[] toBytes(long n) {
        byte[] b = new byte[8];

        b[0] = (byte) (n);
        n >>>= 8;
        b[1] = (byte) (n);
        n >>>= 8;
        b[2] = (byte) (n);
        n >>>= 8;
        b[3] = (byte) (n);
        n >>>= 8;
        b[4] = (byte) (n);
        n >>>= 8;
        b[5] = (byte) (n);
        n >>>= 8;
        b[6] = (byte) (n);
        n >>>= 8;
        b[7] = (byte) (n);

        return b;
    }
}
