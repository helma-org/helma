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

package helma.scripting.fesi.extensions;

import FESI.AST.*;
import FESI.Data.*;
import FESI.Exceptions.*;
import FESI.Extensions.*;
import FESI.Interpreter.*;
import FESI.Parser.*;
import com.oroinc.net.ftp.*;
import helma.objectmodel.*;
import java.io.*;

/**
 * A FTP-client object that allows to do some FTP from HOP applications.
 * FTP support is far from complete but can easily be extended if more
 * functionality is needed.
 * This uses the NetComponent classes from savarese.org (ex oroinc.com).
 */
class ESFtpClient extends ESObject {
    private FTPClient ftpclient;
    private String server;
    private Exception lastError = null;
    private File localDir = null;

    /**
     * Create a new FTP Client
     *
     * @param prototype The prototype object for the FTP object
     * @param evaluator The current evaluator
     */
    ESFtpClient(ESObject prototype, Evaluator evaluator, ESValue srvstr) {
        super(prototype, evaluator);
        this.server = srvstr.toString();
    }

    ESFtpClient(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator);
    }

    /**
     *
     *
     * @return ...
     */
    public String getESClassName() {
        return "FtpClient";
    }

    /**
     *
     *
     * @return ...
     */
    public String toString() {
        return "[FtpClient]";
    }

    /**
     *
     *
     * @return ...
     */
    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" +
               this.toString() + "]";
    }

    ESValue getLastError() throws EcmaScriptException {
        if (lastError == null) {
            return ESNull.theNull;
        } else {
            return ESLoader.normalizeValue(lastError, evaluator);
        }
    }

    /**
     * Login to the FTP server
     *
     * @param   arguments  The argument list
     * @return  true if successful, false otherwise
     */
    ESValue login(ESValue[] arguments) throws EcmaScriptException {
        if (server == null) {
            return ESBoolean.makeBoolean(false);
        }

        try {
            ftpclient = new FTPClient();
            ftpclient.connect(server);

            boolean b = ftpclient.login(arguments[0].toString(), arguments[1].toString());

            return ESBoolean.makeBoolean(b);
        } catch (Exception x) {
            return ESBoolean.makeBoolean(false);
        } catch (NoClassDefFoundError x) {
            return ESBoolean.makeBoolean(false);
        }
    }

    ESValue cd(ESValue[] arguments) throws EcmaScriptException {
        if (ftpclient == null) {
            return ESBoolean.makeBoolean(false);
        }

        try {
            ftpclient.changeWorkingDirectory(arguments[0].toString());

            return ESBoolean.makeBoolean(true);
        } catch (Exception wrong) {
        }

        return ESBoolean.makeBoolean(false);
    }

    ESValue mkdir(ESValue[] arguments) throws EcmaScriptException {
        if (ftpclient == null) {
            return ESBoolean.makeBoolean(false);
        }

        try {
            return ESBoolean.makeBoolean(ftpclient.makeDirectory(arguments[0].toString()));
        } catch (Exception wrong) {
        }

        return ESBoolean.makeBoolean(false);
    }

    ESValue lcd(ESValue[] arguments) throws EcmaScriptException {
        try {
            localDir = new File(arguments[0].toString());

            if (!localDir.exists()) {
                localDir.mkdirs();
            }

            return ESBoolean.makeBoolean(true);
        } catch (Exception wrong) {
        }

        return ESBoolean.makeBoolean(false);
    }

    ESValue putFile(ESValue[] arguments) throws EcmaScriptException {
        if (ftpclient == null) {
            return ESBoolean.makeBoolean(false);
        }

        try {
            String fn = arguments[0].toString();
            File f = (localDir == null) ? new File(fn) : new File(localDir, fn);
            InputStream fin = new BufferedInputStream(new FileInputStream(f));

            ftpclient.storeFile(arguments[1].toString(), fin);
            fin.close();

            return ESBoolean.makeBoolean(true);
        } catch (Exception wrong) {
        }

        return ESBoolean.makeBoolean(false);
    }

    ESValue putString(ESValue[] arguments) throws EcmaScriptException {
        if (ftpclient == null) {
            return ESBoolean.makeBoolean(false);
        }

        try {
            byte[] bytes = null;

            // check if this already is a byte array
            if (arguments[0] instanceof ESArrayWrapper) {
                Object o = ((ESArrayWrapper) arguments[0]).toJavaObject();

                if (o instanceof byte[]) {
                    bytes = (byte[]) o;
                }
            }

            if (bytes == null) {
                bytes = arguments[0].toString().getBytes();
            }

            ByteArrayInputStream bin = new ByteArrayInputStream(bytes);

            ftpclient.storeFile(arguments[1].toString(), bin);

            return ESBoolean.makeBoolean(true);
        } catch (Exception wrong) {
        }

        return ESBoolean.makeBoolean(false);
    }

    ESValue getFile(ESValue[] arguments) throws EcmaScriptException {
        if (ftpclient == null) {
            return ESBoolean.makeBoolean(false);
        }

        try {
            String fn = arguments[0].toString();
            File f = (localDir == null) ? new File(fn) : new File(localDir, fn);
            OutputStream out = new BufferedOutputStream(new FileOutputStream(f));

            ftpclient.retrieveFile(arguments[0].toString(), out);
            out.close();

            return ESBoolean.makeBoolean(true);
        } catch (Exception wrong) {
        }

        return ESBoolean.makeBoolean(false);
    }

    ESValue getString(ESValue[] arguments) throws EcmaScriptException {
        if (ftpclient == null) {
            return ESNull.theNull;
        }

        try {
            ByteArrayOutputStream bout = new ByteArrayOutputStream();

            ftpclient.retrieveFile(arguments[0].toString(), bout);

            return new ESString(bout.toString());
        } catch (Exception wrong) {
        }

        return ESNull.theNull;
    }

    /**
     * Disconnect from FTP server
     *
     * @param   arguments  The argument list
     * @return  true if successful, false otherwise
     */
    ESValue logout(ESValue[] arguments) throws EcmaScriptException {
        if (ftpclient != null) {
            try {
                ftpclient.logout();
            } catch (IOException ignore) {
            }

            try {
                ftpclient.disconnect();
            } catch (IOException ignore) {
            }
        }

        return ESBoolean.makeBoolean(true);
    }

    ESValue binary(ESValue[] arguments) throws EcmaScriptException {
        if (ftpclient != null) {
            try {
                ftpclient.setFileType(FTP.BINARY_FILE_TYPE);

                return ESBoolean.makeBoolean(true);
            } catch (IOException ignore) {
            }
        }

        return ESBoolean.makeBoolean(false);
    }

    ESValue ascii(ESValue[] arguments) throws EcmaScriptException {
        if (ftpclient != null) {
            try {
                ftpclient.setFileType(FTP.ASCII_FILE_TYPE);

                return ESBoolean.makeBoolean(true);
            } catch (IOException ignore) {
            }
        }

        return ESBoolean.makeBoolean(false);
    }
}


/**
 * 
 */
public class FtpExtension extends Extension {
    private transient Evaluator evaluator = null;
    private ESObject esFtpPrototype = null;

    /**
     * Creates a new FtpExtension object.
     */
    public FtpExtension() {
        super();
    }

    /**
     *
     *
     * @param evaluator ...
     *
     * @throws EcmaScriptException ...
     */
    public void initializeExtension(Evaluator evaluator)
                             throws EcmaScriptException {
        this.evaluator = evaluator;

        GlobalObject go = evaluator.getGlobalObject();
        ObjectPrototype op = (ObjectPrototype) evaluator.getObjectPrototype();
        FunctionPrototype fp = (FunctionPrototype) evaluator.getFunctionPrototype();

        esFtpPrototype = new ESFtpClient(op, evaluator);

        ESObject globalFtpObject = new GlobalObjectFtpClient("FtpClient", evaluator, fp);

        globalFtpObject.putHiddenProperty("prototype", esFtpPrototype);
        globalFtpObject.putHiddenProperty("length", new ESNumber(1));

        esFtpPrototype.putHiddenProperty("login",
                                         new FtpClientLogin("login", evaluator, fp));
        esFtpPrototype.putHiddenProperty("cd", new FtpClientCD("cd", evaluator, fp));
        esFtpPrototype.putHiddenProperty("mkdir",
                                         new FtpClientMKDIR("mkdir", evaluator, fp));
        esFtpPrototype.putHiddenProperty("lcd", new FtpClientLCD("lcd", evaluator, fp));
        esFtpPrototype.putHiddenProperty("putFile",
                                         new FtpClientPutFile("putFile", evaluator, fp));
        esFtpPrototype.putHiddenProperty("putString",
                                         new FtpClientPutString("putString", evaluator, fp));
        esFtpPrototype.putHiddenProperty("getFile",
                                         new FtpClientGetFile("getFile", evaluator, fp));
        esFtpPrototype.putHiddenProperty("getString",
                                         new FtpClientGetString("getString", evaluator, fp));
        esFtpPrototype.putHiddenProperty("logout",
                                         new FtpClientLogout("logout", evaluator, fp));
        esFtpPrototype.putHiddenProperty("binary",
                                         new FtpClientBinary("binary", evaluator, fp));
        esFtpPrototype.putHiddenProperty("ascii",
                                         new FtpClientAscii("ascii", evaluator, fp));

        go.putHiddenProperty("FtpClient", globalFtpObject);
    }

    class GlobalObjectFtpClient extends BuiltinFunctionObject {
        GlobalObjectFtpClient(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            return doConstruct(thisObject, arguments);
        }

        public ESObject doConstruct(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = null;

            if (arguments.length != 1) {
                throw new EcmaScriptException("FtpClient requires 1 argument");
            }

            ftp = new ESFtpClient(esFtpPrototype, this.evaluator, arguments[0]);

            return ftp;
        }
    }

    class FtpClientLogin extends BuiltinFunctionObject {
        FtpClientLogin(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.login(arguments);
        }
    }

    class FtpClientCD extends BuiltinFunctionObject {
        FtpClientCD(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.cd(arguments);
        }
    }

    class FtpClientMKDIR extends BuiltinFunctionObject {
        FtpClientMKDIR(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.mkdir(arguments);
        }
    }

    class FtpClientLCD extends BuiltinFunctionObject {
        FtpClientLCD(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.lcd(arguments);
        }
    }

    class FtpClientPutFile extends BuiltinFunctionObject {
        FtpClientPutFile(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.putFile(arguments);
        }
    }

    class FtpClientPutString extends BuiltinFunctionObject {
        FtpClientPutString(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.putString(arguments);
        }
    }

    class FtpClientGetFile extends BuiltinFunctionObject {
        FtpClientGetFile(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.getFile(arguments);
        }
    }

    class FtpClientGetString extends BuiltinFunctionObject {
        FtpClientGetString(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.getString(arguments);
        }
    }

    class FtpClientLogout extends BuiltinFunctionObject {
        FtpClientLogout(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.logout(arguments);
        }
    }

    class FtpClientBinary extends BuiltinFunctionObject {
        FtpClientBinary(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.binary(arguments);
        }
    }

    class FtpClientAscii extends BuiltinFunctionObject {
        FtpClientAscii(String name, Evaluator evaluator, FunctionPrototype fp) {
            super(fp, evaluator, name, 1);
        }

        public ESValue callFunction(ESObject thisObject, ESValue[] arguments)
                             throws EcmaScriptException {
            ESFtpClient ftp = (ESFtpClient) thisObject;

            return ftp.ascii(arguments);
        }
    }
}
