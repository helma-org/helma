@echo off


REM --------------------------------------------
REM Defualt == jar
REM "core"           target builds core classes
REM "clean"          target removes bin directory
REM "jar"            target builds core + jar
REM "javadoc"        target builds the javadoc
REM "package"        target builds core + jar + javadoc + distribution
REM --------------------------------------------
set TARGET=%1%


REM --------------------------------------------
REM No need to edit anything past here
REM --------------------------------------------
set BUILDFILE=build.xml
if "%TARGET%" == "" goto setdist
goto final

:setdist
set TARGET=jar
goto final

:final

if "%JAVA_HOME%" == "" goto javahomeerror

if exist %JAVA_HOME%\lib\tools.jar set CLASSPATH=%CLASSPATH%;%JAVA_HOME%\lib\tools.jar
set CP=%CLASSPATH%;ant.jar;xml.jar

echo Classpath: %CP%
echo JAVA_HOME: %JAVA_HOME%

%JAVA_HOME%\bin\java.exe -classpath "%CP%" org.apache.tools.ant.Main -buildfile %BUILDFILE% %TARGET%

goto end


REM -----------ERROR-------------
:javahomeerror
echo "ERROR: JAVA_HOME not found in your environment."
echo "Please, set the JAVA_HOME variable in your environment to match the"
echo "location of the Java Virtual Machine you want to use."

:end
