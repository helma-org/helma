@echo off
rem Batch file for Starting Helma with a JDK-like virtual machine.

:: Initialize variables
:: (don't touch this section)
set JAVA_HOME=
set HOP_HOME=
set HTTP_PORT=
set XMLRPC_PORT=
set AJP13_PORT=
set RMI_PORT=
set OPTIONS=

:: Set TCP ports for Helma servers
:: (comment/uncomment to de/activate)
set HTTP_PORT=8080
rem set XMLRPC_PORT=8081
rem set AJP13_PORT=8009
rem set RMI_PORT=5050

:: Uncomment to set HOP_HOME
rem set HOP_HOME=c:\program files\helma

:: Uncomment to set JAVA_HOME variable
rem set JAVA_HOME=c:\program files\java

:: Uncomment to pass options to the Java virtual machine
rem set JAVA_OPTIONS=-server -Xmx128m

:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:::::: No user configuration needed below this line :::::::
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:: Setting the script path
set SCRIPT_DIR=%~d0%~p0

:: Using JAVA_HOME variable if defined. Otherwise, 
:: Java executable must be contained in PATH variable
if "%JAVA_HOME%"=="" goto default
   set JAVACMD=%JAVA_HOME%\bin\java
   goto end
:default
   set JAVACMD=java
:end

:: Setting HOP_HOME to script path if undefined
if "%HOP_HOME%"=="" (
   set HOP_HOME=%SCRIPT_DIR%
)

:: Setting the classpath
set LIB=%SCRIPT_DIR%lib
set JARS=%LIB%\helma.jar
set JARS=%JARS%;%LIB%\jetty.jar
set JARS=%JARS%;%LIB%\crimson.jar
set JARS=%JARS%;%LIB%\xmlrpc.jar
set JARS=%JARS%;%LIB%\village.jar
set JARS=%JARS%;%LIB%\servlet.jar
set JARS=%JARS%;%LIB%\regexp.jar
set JARS=%JARS%;%LIB%\netcomponents.jar
set JARS=%JARS%;%LIB%\jimi.jar
set JARS=%JARS%;%LIB%\apache-dom.jar
set JARS=%JARS%;%LIB%\jdom.jar
set JARS=%JARS%;%LIB%\mail.jar
set JARS=%JARS%;%LIB%\activation.jar
set JARS=%JARS%;%LIB%\mysql.jar

:: Setting Helma server options
if not "%HTTP_PORT%"=="" (
   echo Starting HTTP server on port %HTTP_PORT%
   set OPTIONS=%OPTIONS% -w %HTTP_PORT%
)
if not "%XMLRPC_PORT%"=="" (
   echo Starting XML-RPC server on port %XMLRPC_PORT%
   set OPTIONS=%OPTIONS% -x %XMLRPC_PORT%
)
if not "%AJP13_PORT%"=="" (
   echo Starting AJP13 listener on port %AJP13_PORT%
   set OPTIONS=%OPTIONS% -jk %AJP13_PORT%
)
if not "%RMI_PORT%"=="" (
   echo Starting RMI server on port %RMI_PORT%
   set OPTIONS=%OPTIONS% -p %RMI_PORT%
)
if not "%HOP_HOME%"=="" (
   echo Serving applications from %HOP_HOME%
   set OPTIONS=%OPTIONS% -h %HOP_HOME%
)

:: Invoking the Java virtual machine
%JAVACMD% %JAVA_OPTIONS% -classpath %JARS% helma.main.Server %OPTIONS%
