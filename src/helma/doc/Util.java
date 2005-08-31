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

package helma.doc;

import helma.framework.repository.Resource;

import java.awt.Point;
import java.io.*;


/**
 * 
 */
public final class Util {
    /**
     * chops a string from comment-delimiters
     *
     * @param line a line of raw text
     *
     * @return chopped string
     */
    public static String chopDelimiters(String line) {
        line = line.trim();
        if (line == null) {
            return null;
        }
        if (line.endsWith("*/")) {
            line = line.substring(0, line.length() - 2);
        }
        if (line.startsWith("/**")) {
            line = line.substring(3).trim();
        } else if (line.startsWith("/*")) {
            line = line.substring(2).trim();
        } else if (line.startsWith("*")) {
            line = line.substring(1).trim();
        } else if (line.startsWith("//")) {
            line = line.substring(2).trim();
        }
        return line;
    }

    /**
     * chops anything that comes after a closing comment tag
     *
     * @param comment the raw comment
     *
     * @return chopped comment
     */
    public static String chopComment (String comment) {
        int idx = comment.indexOf ("*/");
        if (idx>0) {
            return comment.substring (0, idx+2);
        } else {
            return comment;
        }
    }

    /**
     * reads a part of a file defined by two points

     * @param start of string to extract defined by column x and row y

     * @param end of string to extract

     * @return string
     */
    public static String getStringFromFile (Resource res, Point start, Point end) {
        StringBuffer buf = new StringBuffer();
        int ct = 0;
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(res.getInputStream()));
            String line = "";
            while (line != null) {
                line = in.readLine();
                if (line == null) {
                    break;
                }
                if ((ct > start.y) && (ct < end.y)) {
                    buf.append(line).append("\n");
                } else if (ct == start.y) {
                    if (start.y==end.y) {
                        buf.append (line.substring (start.x, end.x));
                        break;
                    } else {
                        buf.append(line.substring(start.x, line.length())).append("\n");
                    }
                } else if (ct == end.y) {
                    buf.append(line.substring(0, end.x));
                    break;
                }
                ct++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
        return buf.toString().trim();
    }


    /**
     * method to debug file/stream-handling with Point objects. extracts the line p
     * points to and prints it with a pointer to the given column
     *
     * @param sourceFile
     * @param p x-value is used for column, y for row
     * @param debugStr string prefixed to output
     */
    public static void debugLineFromFile (File sourceFile, Point p, String debugStr) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(sourceFile));
            String line = "";
            int ct = 0;
            while (line != null) {
                line = in.readLine ();
                if (line==null) {
                    System.out.println ("eof reached");
                    break;
                }
                if (ct==p.y) {
                    System.out.println (debugStr + ": " + line);
                    for (int i=0; i<(debugStr.length()+1+p.x); i++) {
                        System.out.print (" ");
                    }
                    System.out.println ("^");
                    break;
                }
                ct++;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

}
