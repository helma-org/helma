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

import java.util.*;

/**
 * 
 */
public final class DocTag {

    // the tag name
    private String type;

    // the name of the parameter
    private String name;
    
    // the actual comment
    private String text;

    private DocTag(String type, String name, String text) {
        this.type = type;
        this.name = (name != null) ? name.trim() : "";
        this.text = (text != null) ? text.trim() : "";
    }


    /**
     *
     *
     * @param rawTag ...
     *
     * @return ...
     *
     * @throws DocException ...
     */
    public static DocTag parse(String rawTag) throws DocException {
        StringTokenizer tok = new StringTokenizer(rawTag.trim());
        String name = "";
        String type = "";
        StringBuffer comment = new StringBuffer ();
        try {
            type = matchTagName(tok.nextToken().substring (1));
        } catch (NoSuchElementException nsee) {
            throw new DocException ("invalid tag: " + rawTag);
        }
        try {
            if (isTagWithName(type)) {
                name = tok.nextToken();
            }
            while (tok.hasMoreTokens()) {
                comment.append (" ").append(tok.nextToken());
            }
        } catch (NoSuchElementException nsee) { // ignore
        }
        return new DocTag (type, name, comment.toString());
    }


    /**
     * @param rawTag a line from a helmadoc comment
     * @return true if the line represents a tag
     */
    public static boolean isTagStart(String rawTag) {
        if (rawTag.trim().startsWith("@"))
            return true;
        else
            return false;
    }


    /**
     * match tags where we want different names to be valid
     * as one and the same tag
     * @param tagName original name
     * @return modified name if tag was matched
     */
    public static String matchTagName(String tagName) {
        if ("returns".equals(tagName)) {
            return "return";
        } else if ("arg".equals(tagName)) {
            return "param";
        } else {
            return tagName;
        }
    }


    public static boolean isTagWithName(String tagName) {
        if ("param".equals (tagName))
            return true;
        else
            return false;
    }


    /**
     *
     *
     * @return ...
     */
    public String getType() {
        return type;
    }

    /**
     *
     *
     * @return ...
     */
    public String getName() {
        return name;
    }

    /**
     *
     *
     * @return ...
     */
    public String getText() {
        return text;
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        StringBuffer buf = new StringBuffer ("[@" + type);
        if (name!=null && !"".equals(name))
            buf.append (" ").append(name);
        if (text!=null && !"".equals(text))
            buf.append (" ").append(text);
        return buf.toString() + "]";
    }
}
