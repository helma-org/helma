@echo off

rem Batch file for starting Hop with a JDK-like virtual machine.

set JARS=lib\helma.jar;lib\crimson.jar;lib\village.jar;lib\jsdk.jar
set JARS=%JARS%;lib\regexp.jar;lib\netcomponents.jar;lib\jimi.jar
set JARS=%JARS%;lib\mail.jar;lib\activation.jar;lib\mysql.jar;lib\jdom.jar;lib\minml.jar

set HOP_PORT=8080

echo Starting Web server on port %HOP_PORT%

java -classpath c:\winnt\java\packages\rmi.zip;%JARS% helma.main.Server -w %HOP_PORT%
