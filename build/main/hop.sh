#!/bin/sh
# Shell script for starting Helma with a JDK-like virtual machine.
# Presumes that you have your classpath set.

# Set TCP ports for Helma servers
# (comment/uncomment to de/activate)
HTTP_PORT=8080
#XMLRPC_PORT=8081
#AJP13_PORT=8009
#RMI_PORT=5050
#HOP_HOME=/usr/local/helma

# if JAVA_HOME variable is set, use it. Otherwise, Java executable
# must be contained in PATH variable.
if [ "$JAVA_HOME" ]; then
   JAVACMD="$JAVA_HOME/bin/java"
else
   JAVACMD=java
fi

# Set classpath
JARS=lib/helma.jar:lib/xmlrpc.jar:lib/crimson.jar:lib/village.jar
JARS=$JARS:lib/servlet.jar:lib/regexp.jar:lib/netcomponents.jar
JARS=$JARS:lib/jimi.jar:lib/apache-dom.jar:lib/jdom.jar:lib/mail.jar
JARS=$JARS:lib/activation.jar:lib/mysql.jar:lib/jetty.jar

if [ "$HTTP_PORT" ]; then
   SWITCHES="$SWITCHES" -w $HTTP_PORT
   echo Starting HTTP server on port $HTTP_PORT
fi
if [ "$XMLRPC_PORT" ]; then
   SWITCHES="$SWITCHES" -x $XMLRPC_PORT
   echo Starting XML-RPC server on port $XMLRPC_PORT
fi
if [ "$AJP13_PORT" ]; then
   SWITCHES="$SWITCHES" -jk $AJP13_PORT
   echo Starting AJP13 listener on port $AJP13_PORT
fi
if [ "$RMI_PORT" ]; then
   SWITCHES="$SWITCHES" -p $RMI_PORT
   echo Starting RMI server on port $RMI_PORT
fi
if [ "$HOP_HOME" ]; then
   SWITCHES="$SWITCHES" -h $HOP_HOME
fi

# Invoking the Java VM
$JAVACMD -classpath $CLASSPATH:$JARS helma.main.Server $SWITCHES
