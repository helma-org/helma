@echo off

rem Batch file for Starting Helma with a JDK-like virtual machine.

:: Initialize variables
:: (don't touch this section)
set SWITCHES=
set HTTP_PORT=
set XMLRPC_PORT=
set AJP13_PORT=
set RMI_PORT=
set HOP_HOME=

:: Set server parameters
:: (comment/uncomment to de/activate)
set HTTP_PORT=8080
rem set XMLRPC_PORT=8081
rem set AJP13_PORT=8009
rem set RMI_PORT=5050
rem set HOP_HOME=c:/program files/helma

:: Set classpath
set JARS=lib\helma.jar;lib\xmlrpc.jar;lib\crimson.jar;lib\village.jar
set JARS=%JARS%;lib\servlet.jar;lib\regexp.jar;lib\netcomponents.jar
set JARS=%JARS%;lib\jimi.jar;lib\apache-dom.jar;lib\jdom.jar;lib\mail.jar
set JARS=%JARS%;lib\activation.jar;lib\mysql.jar;lib\jetty.jar

:: Set switches
if not %HTTP_PORT%"==" (
   echo Starting HTTP server on port %HTTP_PORT%
   set SWITCHES=%SWITCHES% -w %HTTP_PORT%
)
if not %XMLRPC_PORT%"==" (
   echo Starting XML-RPC server on port %XMLRPC_PORT%
   set SWITCHES=%SWITCHES% -x %XMLRPC_PORT%
)
if not %AJP13_PORT%"==" (
   echo Starting AJP13 listener on port %AJP13_PORT%
   set SWITCHES=%SWITCHES% -jk %AJP13_PORT%
)
if not %RMI_PORT%"==" (
   echo Starting RMI server on port %RMI_PORT%
   set SWITCHES=%SWITCHES% -p %RMI_PORT%
)
if not %HOP_HOME%"==" (
   set SWITCHES=%SWITCHES% -h %HOP_HOME%
)

:: Invoking the Java VM
java -classpath %JARS% helma.main.Server %SWITCHES%
