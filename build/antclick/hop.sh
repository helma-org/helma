#!/bin/sh
# Shell script for starting Helma with a JDK-like virtual machine.
# Presumes that you have your classpath set.

# set Helma TCP ports
WEB_PORT=8080
XMLRPC_PORT=8081

# if JAVA_HOME variable is set, use it. Otherwise, java executable
# must be contained in PATH variable.
if [  "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME"/bin/java
else
   JAVACMD=java
fi

# set classpath
JARS=lib/helma.jar:lib/crimson.jar:lib/village.jar:lib/servlet.jar:lib/jetty.jar
JARS=$JARS:lib/mckoidb.jar
JARS=$JARS:lib/regexp.jar:lib/netcomponents.jar:lib/jimi.jar:lib/apache-dom.jar
JARS=$JARS:lib/mail.jar:lib/activation.jar:lib/mysql.jar:lib/jdom.jar:lib/minml.jar

echo Starting Web server on port $WEB_PORT
echo Starting XML-RPC server on port $XMLRPC_PORT

# launch
$JAVACMD -classpath $CLASSPATH:$JARS helma.main.Server -w $WEB_PORT -x $XMLRPC_PORT
