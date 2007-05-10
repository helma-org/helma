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

package helma.scripting.rhino;

import helma.framework.repository.Resource;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 *  Support for .hac (action) and .hsp (template) files
 */
public class HacHspConverter {

    public static String convertHac(Resource action, String encoding)
            throws IOException {
        String functionName = action.getBaseName().replace('.', '_') + "_action";
        return composeFunction(functionName, null, action.getContent(encoding));
    }

    public static String convertHsp(Resource template, String encoding)
            throws IOException {
        String functionName = template.getBaseName().replace('.', '_');
        String body = processHspBody(template.getContent(encoding));
        return composeFunction(functionName,
                               "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10",
                               body);
    }

    public static String convertHspAsString(Resource template, String encoding)
            throws IOException {
        String functionName = template.getBaseName().replace('.', '_') + "_as_string";
        String body = processHspBody(template.getContent(encoding));
        return composeFunction(functionName,
                               "arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10",
                               "res.pushStringBuffer(); " + body +
                               "\r\nreturn res.popStringBuffer();\r\n");
    }

    static String composeFunction(String funcname, String args, String body) {
        if ((body == null) || "".equals(body.trim())) {
            body = ";\r\n";
        } else {
            body = body + "\r\n";
        }

        StringBuffer f = new StringBuffer("function ");

        f.append(funcname);
        f.append(" (");
        if (args != null)
            f.append(args);
        f.append(") {\n");
        f.append(body);
        f.append("\n}");

        return f.toString();
    }

    static String processHspBody(String content) {
        ArrayList partBuffer = new ArrayList();
        char[] cnt = content.toCharArray();
        int l = cnt.length;

        if (l == 0) {
            return "";
        }

        // if last character is whitespace, swallow it.
        // this is necessary for some inner templates to look ok.
        if (Character.isWhitespace(cnt[l - 1])) {
            l -= 1;
        }

        int lastIdx = 0;

        for (int i = 0; i < (l - 1); i++) {
            if ((cnt[i] == '<') && (cnt[i + 1] == '%')) {
                int j = i + 2;

                while ((j < (l - 1)) && ((cnt[j] != '%') || (cnt[j + 1] != '>'))) {
                    j++;
                }

                if (j > (i + 2)) {
                    if ((i - lastIdx) > 0) {
                        partBuffer.add(new HspBodyPart(new String(cnt, lastIdx,
                                                              i - lastIdx), true));
                    }

                    String script = new String(cnt, i + 2, (j - i) - 2);

                    partBuffer.add(new HspBodyPart(script, false));
                    lastIdx = j + 2;
                }

                i = j + 1;
            }
        }

        if (lastIdx < l) {
            partBuffer.add(new HspBodyPart(new String(cnt, lastIdx, l - lastIdx),
                                       true));
        }

        StringBuffer templateBody = new StringBuffer();
        int nparts = partBuffer.size();

        for (int k = 0; k < nparts; k++) {
            HspBodyPart nextPart = (HspBodyPart) partBuffer.get(k);

            if (nextPart.isStatic || nextPart.content.trim().startsWith("=")) {
                // check for <%= ... %> statements
                if (!nextPart.isStatic) {
                    nextPart.content = nextPart.content.trim().substring(1).trim();

                    // cut trailing ";"
                    while (nextPart.content.endsWith(";"))
                        nextPart.content = nextPart.content.substring(0,
                                                                      nextPart.content.length() -
                                                                      1);
                }

                StringTokenizer st = new StringTokenizer(nextPart.content, "\r\n", true);
                String nextLine = st.hasMoreTokens() ? st.nextToken() : null;

                // count newLines we "swallow", see explanation below
                int newLineCount = 0;

                templateBody.append("res.write (");

                if (nextPart.isStatic) {
                    templateBody.append("\"");
                }

                while (nextLine != null) {
                    if ("\n".equals(nextLine)) {
                        // append a CRLF
                        newLineCount++;
                        templateBody.append("\\r\\n");
                    } else if (!"\r".equals(nextLine)) {
                        try {
                            StringReader lineReader = new StringReader(nextLine);
                            int c = lineReader.read();

                            while (c > -1) {
                                if (nextPart.isStatic &&
                                        (((char) c == '"') || ((char) c == '\\'))) {
                                    templateBody.append('\\');
                                }

                                templateBody.append((char) c);
                                c = lineReader.read();
                            }
                        } catch (IOException srx) {
                        }
                    }

                    nextLine = st.hasMoreTokens() ? st.nextToken() : null;
                }

                if (nextPart.isStatic) {
                    templateBody.append("\"");
                }

                templateBody.append("); ");

                // append the number of lines we have "swallowed" into
                // one write statement, so error messages will *approximately*
                // give correct line numbers.
                for (int i = 0; i < newLineCount; i++) {
                    templateBody.append("\r\n");
                }
            } else {
                templateBody.append(nextPart.content);

                if (!nextPart.content.trim().endsWith(";")) {
                    templateBody.append(";");
                }
            }
        }

        // templateBody.append ("\r\nreturn null;\r\n");
        return templateBody.toString();
    }

    static class HspBodyPart {
        String content;
        boolean isPart;
        boolean isStatic;

        public HspBodyPart(String content, boolean isStatic) {
            isPart = false;
            this.content = content;
            this.isStatic = isStatic;
        }

        public String getName() {
            return isStatic ? null : content;
        }

        public String toString() {
            return "Template.Part [" + content + "," + isStatic + "]";
        }
    }
}
