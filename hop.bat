@echo off

rem Batch file for starting Hop with a JDK-like virtual machine.

rem Set Helma TCP ports
set WEB_PORT=8080
set XMLRPC_PORT=8081

rem Set Classpath
set JARS=lib\helma.jar;lib\crimson.jar;lib\village.jar;lib\servlet.jar;lib\jetty.jar
set JARS=%JARS%;lib\regexp.jar;lib\netcomponents.jar;lib\jimi.jar;lib\apache-dom.jar
set JARS=%JARS%;lib\mail.jar;lib\activation.jar;lib\mysql.jar;lib\jdom.jar;lib\minml.jar

echo Starting Web server on port %WEB_PORT%
echo Starting XML-RPC server on port %XMLRPC_PORT%

java -classpath %JARS% helma.main.Server -w %WEB_PORT% -x %XMLRPC_PORT%
