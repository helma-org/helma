#!/bin/sh
# Stupid shell script for starting Hop with a JDK-like virtual machine.
# Presumes that you have your classpath set.

export HOP_PORT=8080 

export JARS=lib/helma.jar:lib/berkeley.jar:lib/village.jar:lib/jsdk.jar:lib/openxml.jar
export JARS=$JARS:lib/sax.jar:lib/regexp.jar:lib/netcomponents.jar:lib/jimi.jar
export JARS=$JARS:lib/mail.jar:lib/activation.jar:lib/mysql.jar:lib/jdom.jar:lib/minml.jar

java -classpath $CLASSPATH:$JARS helma.main.Server -w $HOP_PORT
