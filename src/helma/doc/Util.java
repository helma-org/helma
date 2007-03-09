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
     * Extract a part of a file defined by two points from a String array
     * @param lines an array of lines
     * @param start of string to extract defined by column x and row y
     * @param end of string to extract
     * @return string
     */
    public static String extractString (String[] lines, Point start, Point end) {
        StringBuffer buf = new StringBuffer();
        int to = Math.min(end.y + 1, lines.length);
        for (int i = start.y; i < to; i++) {
            int from = (i == start.y) ? start.x : 0;
            if (from < 0 || from > lines[i].length()) {
                System.err.println("Start index " + from + " out of range [0.." + 
                        lines[i].length() + "]");
                from = 0;
            }
            if (i == end.y && end.x < lines[i].length())
                buf.append(lines[i].substring(from, end.x));
            else
                buf.append(lines[i].substring(from));
            buf.append("\n");
        }
        return buf.toString().trim();
    }

    /**
     * Extract a part of a file defined by two points from a String array
     * @param lines an array of lines
     * @param start of string to extract defined by column x and row y
     * @param end of string to extract
     * @return string
     */
    public static String extractString (String[] lines, int start, int end) {
        StringBuffer buf = new StringBuffer();
        int to = Math.min(end + 1, lines.length);
        for (int i = start; i < to; i++) {
            buf.append(lines[i]);
            buf.append("\n");
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
