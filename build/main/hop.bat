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

:: Uncomment to add your own jar files to the class path
rem set JARS=C:\path\to\some.jar

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
cd %HOP_HOME%


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
   set OPTIONS=%OPTIONS% -h "%HOP_HOME%
)
if not "%JARS%"=="" (
   set JARS=-classpath %JARS%
)

:: Invoking the Java virtual machine
%JAVACMD% %JAVA_OPTIONS% %JARS% -jar launcher.jar %OPTIONS%
