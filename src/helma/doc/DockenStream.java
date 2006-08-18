/*
 *  Copyright 2006 Hannes Wallnoefer <hannes@helma.at>
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package helma.doc;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.io.Reader;

import org.mozilla.javascript.*;

public class DockenStream {

    Object tokenStream;
    static Constructor cnst;
    static String[] names = new String[]
            { "getToken", "getString", "getOffset", "getLineno", "eof" };
    static Method[] methods = new Method[5];
    final static Class[] noclass = new Class[0];
    final static Object[] noargs = new Object[0];

    int peek;
    boolean peeked;

    static {
        try {
            Class clazz = Class.forName("org.mozilla.javascript.TokenStream");
            cnst = clazz.getDeclaredConstructor(new Class[] {Parser.class, Reader.class, String.class, Integer.TYPE});
            for (int i = 0; i < 5; i++) {
                methods[i] = clazz.getDeclaredMethod(names[i], noclass);
            }
            cnst.setAccessible(true);
            Method.setAccessible(methods, true);
        } catch (Exception x) {
            x.printStackTrace();
        }
    }

    public DockenStream(Parser parser, Reader reader, String name, Integer lineno) {
        try {
            tokenStream = cnst.newInstance(new Object[] {parser, reader, name, lineno});
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public int getToken() {
        if (peeked) {
            peeked = false;
            return peek;
        }
        try {
            return ((Integer) methods[0].invoke(tokenStream, noargs)).intValue();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public String getString() {
        try {
            return (String) methods[1].invoke(tokenStream, noargs);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public int getOffset() {
        try {
            return ((Integer) methods[2].invoke(tokenStream, noargs)).intValue();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public int getLineno() {
        try {
            return ((Integer) methods[3].invoke(tokenStream, noargs)).intValue();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public int peekToken() {
        if (!peeked) {
            peek = getToken();
            peeked = true;
        }
        return peek;
    }

    public boolean eof() {
        try {
            return ((Boolean) methods[4].invoke(tokenStream, noargs)).booleanValue();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}

