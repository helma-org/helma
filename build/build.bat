@echo off


REM --------------------------------------------
REM Defualt == jar
REM "checkout"       target gets sources from helma.org
REM "compile"        target compiles java sources
REM "jar"            target compiles and builds jar
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

set CP=%CLASSPATH%;ant.jar;jaxp.jar;crimson.jar
if exist %JAVA_HOME%\lib\tools.jar set CP=%CP%;%JAVA_HOME%\lib\tools.jar

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
