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

package helma.framework.core;

import helma.framework.*;
import helma.objectmodel.ConcurrencyException;
import helma.objectmodel.INode;
import helma.scripting.*;
import helma.util.HtmlEncoder;
import helma.util.SystemMap;
import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * This represents a Helma skin, i.e. a template created from containing Macro tags
 * that will be dynamically evaluated.. It uses the request path array
 * from the RequestEvaluator object to resolve Macro handlers by type name.
 */
public final class Skin {
    static final int HANDLER = 0;
    static final int MACRO = 1;
    static final int PARAMNAME = 2;
    static final int PARAMVALUE = 3;
    static final int ENCODE_NONE = 0;
    static final int ENCODE_HTML = 1;
    static final int ENCODE_XML = 2;
    static final int ENCODE_FORM = 3;
    static final int ENCODE_URL = 4;
    static final int ENCODE_ALL = 5;
    private Macro[] macros;
    private Application app;
    private char[] source;
    private int sourceLength;
    private HashSet sandbox;

    /**
     * Create a skin without any restrictions on which macros are allowed to be called from it
     */
    public Skin(String content, Application app) {
        this.app = app;
        sandbox = null;
        source = content.toCharArray();
        sourceLength = source.length;
        parse();
    }

    /**
     * Create a skin with a sandbox which contains the names of macros allowed to be called
     */
    public Skin(String content, Application app, HashSet sandbox) {
        this.app = app;
        this.sandbox = sandbox;
        source = content.toCharArray();
        sourceLength = source.length;
        parse();
    }

    /**
     *  Create a skin without any restrictions on the macros from a char array.
     */
    public Skin(char[] content, int length, Application app) {
        this.app = app;
        this.sandbox = null;
        source = content;
        sourceLength = length;
        parse();
    }

    /**
     * Parse a skin object from source text
     */
    private void parse() {
        ArrayList partBuffer = new ArrayList();

        int start = 0;

        for (int i = 0; i < (sourceLength - 1); i++) {
            if ((source[i] == '<') && (source[i + 1] == '%')) {
                // found macro start tag
                int j = i + 2;

                // search macr end tag
                while ((j < (sourceLength - 1)) &&
                           ((source[j] != '%') || (source[j + 1] != '>'))) {
                    j++;
                }

                if (j > (i + 2)) {
                    partBuffer.add(new Macro(i, j + 2));
                    start = j + 2;
                }

                i = j + 1;
            }
        }

        macros = new Macro[partBuffer.size()];
        partBuffer.toArray(macros);
    }

    /**
     * Get the raw source text this skin was parsed from
     */
    public String getSource() {
        return new String(source, 0, sourceLength);
    }

    /**
     * Render this skin
     */
    public void render(RequestEvaluator reval, Object thisObject, Map paramObject)
                throws RedirectException {
        // check for endless skin recursion
        if (++reval.skinDepth > 50) {
            throw new RuntimeException("Recursive skin invocation suspected");
        }

        if (macros == null) {
            reval.res.writeCharArray(source, 0, sourceLength);
            reval.skinDepth--;

            return;
        }

        try {
            int written = 0;
            Map handlerCache = null;

            if (macros.length > 3) {
                handlerCache = new HashMap();
            }

            for (int i = 0; i < macros.length; i++) {
                if (macros[i].start > written) {
                    reval.res.writeCharArray(source, written, macros[i].start - written);
                }

                macros[i].render(reval, thisObject, paramObject, handlerCache);
                written = macros[i].end;
            }

            if (written < sourceLength) {
                reval.res.writeCharArray(source, written, sourceLength - written);
            }
        } finally {
            reval.skinDepth--;
        }
    }

    /**
     * Check if a certain macro is present in this skin. The macro name is in handler.name notation
     */
    public boolean containsMacro(String macroname) {
        for (int i = 0; i < macros.length; i++) {
            if (macros[i] instanceof Macro) {
                Macro m = (Macro) macros[i];

                if (macroname.equals(m.fullName)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     *  Adds a macro to the list of allowed macros. The macro is in handler.name notation.
     */
    public void allowMacro(String macroname) {
        if (sandbox == null) {
            sandbox = new HashSet();
        }

        sandbox.add(macroname);
    }

    class Macro {
        final int start;
        final int end;
        String handler;
        String name;
        String fullName;
        String prefix;
        String suffix;
        String defaultValue;
        int encoding = ENCODE_NONE;
        Map parameters = null;

        public Macro(int start, int end) {
            this.start = start;
            this.end = end;

            int state = HANDLER;
            boolean escape = false;
            char quotechar = '\u0000';
            String lastParamName = null;
            StringBuffer b = new StringBuffer();

            for (int i = start + 2; i < (end - 2); i++) {
                switch (source[i]) {
                    case '.':

                        if (state == HANDLER) {
                            handler = b.toString().trim();
                            b.setLength(0);
                            state = MACRO;
                        } else {
                            b.append(source[i]);
                        }

                        break;

                    case '\\':

                        if (escape) {
                            b.append(source[i]);
                        }

                        escape = !escape;

                        break;

                    case '"':
                    case '\'':

                        if (!escape && (state == PARAMVALUE)) {
                            if (quotechar == source[i]) {
                                String paramValue = b.toString();

                                if (!setSpecialParameter(lastParamName, paramValue)) {
                                    addGenericParameter(lastParamName, paramValue);
                                }

                                lastParamName = null;
                                b.setLength(0);
                                state = PARAMNAME;
                                quotechar = '\u0000';
                            } else if (quotechar == '\u0000') {
                                quotechar = source[i];
                                b.setLength(0);
                            } else {
                                b.append(source[i]);
                            }
                        } else {
                            b.append(source[i]);
                        }

                        escape = false;

                        break;

                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                    case '\f':

                        if ((state == MACRO) || ((state == HANDLER) && (b.length() > 0))) {
                            name = b.toString().trim();
                            b.setLength(0);
                            state = PARAMNAME;
                        } else if ((state == PARAMVALUE) && (quotechar == '\u0000')) {
                            String paramValue = b.toString();

                            if (!setSpecialParameter(lastParamName, paramValue)) {
                                addGenericParameter(lastParamName, paramValue);
                            }

                            lastParamName = null;
                            b.setLength(0);
                            state = PARAMNAME;
                        } else if (state == PARAMVALUE) {
                            b.append(source[i]);
                        } else {
                            b.setLength(0);
                        }

                        break;

                    case '=':

                        if (state == PARAMNAME) {
                            lastParamName = b.toString().trim();
                            b.setLength(0);
                            state = PARAMVALUE;
                        } else {
                            b.append(source[i]);
                        }

                        break;

                    default:
                        b.append(source[i]);
                        escape = false;
                }
            }

            if (b.length() > 0) {
                if ((lastParamName != null) && (b.length() > 0)) {
                    String paramValue = b.toString();

                    if (!setSpecialParameter(lastParamName, paramValue)) {
                        addGenericParameter(lastParamName, paramValue);
                    }
                } else if (state <= MACRO) {
                    name = b.toString().trim();
                }
            }

            if (handler == null) {
                fullName = name;
            } else {
                fullName = handler + "." + name;
            }
        }

        private boolean setSpecialParameter(String name, String value) {
            if ("prefix".equals(name)) {
                prefix = value;

                return true;
            } else if ("suffix".equals(name)) {
                suffix = value;

                return true;
            } else if ("encoding".equals(name)) {
                if ("html".equalsIgnoreCase(value)) {
                    encoding = ENCODE_HTML;
                } else if ("xml".equalsIgnoreCase(value)) {
                    encoding = ENCODE_XML;
                } else if ("form".equalsIgnoreCase(value)) {
                    encoding = ENCODE_FORM;
                } else if ("url".equalsIgnoreCase(value)) {
                    encoding = ENCODE_URL;
                } else if ("all".equalsIgnoreCase(value)) {
                    encoding = ENCODE_ALL;
                }

                return true;
            } else if ("default".equals(name)) {
                defaultValue = value;

                return true;
            }

            return false;
        }

        private void addGenericParameter(String name, String value) {
            if (parameters == null) {
                parameters = new HashMap();
            }

            parameters.put(name, value);
        }

        /**
         *  Render the macro given a handler object
         */
        public void render(RequestEvaluator reval, Object thisObject, Map paramObject,
                           Map handlerCache) throws RedirectException {
            if ((sandbox != null) && !sandbox.contains(fullName)) {
                String h = (handler == null) ? "global" : handler;

                reval.res.write("[Macro " + fullName + " not allowed in sandbox]");

                return;
            } else if ("response".equalsIgnoreCase(handler)) {
                renderFromResponse(reval);

                return;
            } else if ("request".equalsIgnoreCase(handler)) {
                renderFromRequest(reval);

                return;
            } else if ("param".equalsIgnoreCase(handler)) {
                renderFromParam(reval, paramObject);

                return;
            } else if ("session".equalsIgnoreCase(handler)) {
                renderFromSession(reval);

                return;
            }

            try {
                Object handlerObject = null;

                // flag to tell whether we found our invocation target object
                boolean objectFound = true;

                if (handler != null) {
                    // try to get handler from handlerCache first
                    if (handlerCache != null) {
                        handlerObject = handlerCache.get(handler);
                    }

                    if (handlerObject == null) {
                        // if handler object wasn't found in cache retrieve it
                        if ((handlerObject == null) && (thisObject != null)) {
                            // not a global macro - need to find handler object
                            // was called with this object - check it or its parents for matching prototype
                            if (!handler.equals("this") &&
                                    !handler.equals(app.getPrototypeName(thisObject))) {
                                // the handler object is not what we want
                                Object n = thisObject;

                                // walk down parent chain to find handler object
                                while (n != null) {
                                    Prototype proto = app.getPrototype(n);

                                    if ((proto != null) && proto.isInstanceOf(handler)) {
                                        handlerObject = n;

                                        break;
                                    }

                                    n = app.getParentElement(n);
                                }
                            } else {
                                // we already have the right handler object
                                handlerObject = thisObject;
                            }
                        }

                        if (handlerObject == null) {
                            // eiter because thisObject == null or the right object wasn't found 
                            // in the object's parent path. Check if a matching macro handler
                            // is registered with the response object (res.handlers).
                            handlerObject = reval.res.getMacroHandlers().get(handler);
                        }

                        // the macro handler object couldn't be found
                        if (handlerObject == null) {
                            objectFound = false;
                        }
                        // else put the found handler object into the cache so we don't have to look again
                        else if (handlerCache != null) {
                            handlerCache.put(handler, handlerObject);
                        }
                    }
                } else {
                    // this is a global macro with no handler specified
                    handlerObject = null;
                }

                if (objectFound) {
                    // check if a function called name_macro is defined.
                    // if so, the macro evaluates to the function. Otherwise,
                    // a property/field with the name is used, if defined.
                    String funcName = name + "_macro";

                    if (reval.scriptingEngine.hasFunction(handlerObject, funcName)) {
                        StringBuffer buffer = reval.res.getBuffer();

                        // remember length of response buffer before calling macro
                        int bufLength = buffer.length();

                        // remember length of buffer with prefix written out
                        int preLength = 0;

                        if (prefix != null) {
                            buffer.append(prefix);
                            preLength = prefix.length();
                        }

                        // System.err.println ("Getting macro from function");
                        // pass a clone of the parameter map so if the script changes it,
                        // Map param = ;
                        // if (parameters == null)
                        //     parameters = new HashMap ();
                        Object[] arguments = {
                                                 (parameters == null) ? new SystemMap()
                                                                      : new SystemMap(parameters)
                                             };

                        Object value = reval.scriptingEngine.invoke(handlerObject,
                                                                    funcName, arguments,
                                                                    false);

                        // check if macro wrote out to response buffer
                        if (buffer.length() == (bufLength + preLength)) {
                            // function didn't write out anything itself. 
                            // erase previously written prefix
                            if (preLength > 0) {
                                buffer.setLength(bufLength);
                            }

                            // write out macro's return value
                            writeResponse(value, buffer, true);
                        } else {
                            if (encoding != ENCODE_NONE) {
                                // if an encoding is specified, re-encode the macro's output
                                String output = buffer.substring(bufLength + preLength);

                                buffer.setLength(bufLength);
                                writeResponse(output, buffer, false);
                            } else {
                                // no re-encoding needed, just append suffix
                                if (suffix != null) {
                                    buffer.append(suffix);
                                }
                            }

                            writeResponse(value, buffer, false);
                        }
                    } else {
                        // System.err.println ("Getting macro from property");
                        Object value = reval.scriptingEngine.get(handlerObject, name);

                        writeResponse(value, reval.res.getBuffer(), true);
                    }
                } else {
                    String msg = "[HopMacro unhandled: " + fullName + "]";

                    reval.res.write(" " + msg + " ");
                    app.logEvent(msg);
                }
            } catch (RedirectException redir) {
                throw redir;
            } catch (ConcurrencyException concur) {
                throw concur;
            } catch (TimeoutException timeout) {
                throw timeout;
            } catch (Exception x) {
                // x.printStackTrace();
                String msg = x.getMessage();

                if ((msg == null) || (msg.length() < 10)) {
                    msg = x.toString();
                }

                msg = "[HopMacro error in " + fullName + ": " + msg + "]";
                reval.res.write(" " + msg + " ");
                app.logEvent(msg);
            }
        }

        private void renderFromResponse(RequestEvaluator reval) {
            Object value = null;

            if ("message".equals(name)) {
                value = reval.res.message;
            } else if ("error".equals(name)) {
                value = reval.res.error;
            }

            if (value == null) {
                value = reval.res.get(name);
            }

            writeResponse(value, reval.res.getBuffer(), true);
        }

        private void renderFromRequest(RequestEvaluator reval) {
            if (reval.req == null) {
                return;
            }

            Object value = reval.req.get(name);

            writeResponse(value, reval.res.getBuffer(), true);
        }

        private void renderFromSession(RequestEvaluator reval) {
            if (reval.session == null) {
                return;
            }

            Object value = reval.session.getCacheNode().getString(name);

            writeResponse(value, reval.res.getBuffer(), true);
        }

        private void renderFromParam(RequestEvaluator reval, Map paramObject) {
            if (paramObject == null) {
                reval.res.write("[HopMacro error: Skin requires a parameter object]");
            } else {
                Object value = paramObject.get(name);

                writeResponse(value, reval.res.getBuffer(), true);
            }
        }

        /**
         * Utility method for writing text out to the response object.
         */
        void writeResponse(Object value, StringBuffer buffer, boolean useDefault) {
            String text;

            if (value == null) {
                if (useDefault) {
                    text = defaultValue;
                } else {
                    return;
                }
            } else {
                // do not render doubles as doubles unless
                // they actually have a decimal place. This is necessary because
                // all numbers are handled as Double in JavaScript.
                if (value instanceof Double) {
                    Double d = (Double) value;
                    if (d.longValue() == d.doubleValue()) {
                        text = Long.toString(d.longValue());
                    } else {
                        text = d.toString();
                    }
                } else {
                    text = value.toString();
                }
            }

            if ((text != null) && (text.length() > 0)) {
                if (prefix != null) {
                    buffer.append(prefix);
                }

                switch (encoding) {
                    case ENCODE_NONE:
                        buffer.append(text);

                        break;

                    case ENCODE_HTML:
                        HtmlEncoder.encode(text, buffer);

                        break;

                    case ENCODE_XML:
                        HtmlEncoder.encodeXml(text, buffer);

                        break;

                    case ENCODE_FORM:
                        HtmlEncoder.encodeFormValue(text, buffer);

                        break;

                    case ENCODE_URL:
                        buffer.append(URLEncoder.encode(text));

                        break;

                    case ENCODE_ALL:
                        HtmlEncoder.encodeAll(text, buffer);

                        break;
                }

                if (suffix != null) {
                    buffer.append(suffix);
                }
            }
        }

        public String toString() {
            return "[HopMacro: " + fullName + "]";
        }

        /**
         * Return the full name of the macro in handler.name notation
         */
        public String getFullName() {
            return fullName;
        }
    }
}
