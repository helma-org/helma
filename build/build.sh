#!/bin/sh

# --------------------------------------------
# Default == jar
# "snapshot"       target compiles and builds jar in src/ and lib/
# "checkout"       target gets sources from helma.org in src/
# "compile"        target compiles java sources in work/
# "jar"            target compiles and builds jar in work/
# "javadoc"        target builds the javadoc
# "package"        target builds core + jar + javadoc + distribution
# --------------------------------------------
TARGET=${1}

# export JAVA_HOME=/usr/lib/java

export CVSHOME=:pserver:anonymous@coletta.helma.at:/opt/cvs

# comment this out to log in to CVS server
# cvs -d :pserver:anonymous@coletta.helma.at:/opt/cvs login

#--------------------------------------------
# No need to edit anything past here
#--------------------------------------------
if test -z "${JAVA_HOME}" ; then
    echo "ERROR: JAVA_HOME not found in your environment."
    echo "Please, set the JAVA_HOME variable in your environment to match the"
    echo "location of the Java Virtual Machine you want to use."
    exit
fi

if test -z "${TARGET}" ; then
TARGET=jar
fi

if test -f ${JAVA_HOME}/lib/tools.jar ; then
    CLASSPATH=${CLASSPATH}:${JAVA_HOME}/lib/tools.jar
fi

echo "Now building ${TARGET}..."

CP=${CLASSPATH}:ant.jar:jaxp.jar:crimson.jar:../lib/netcomponents.jar

echo "Classpath: ${CP}"
echo "JAVA_HOME: ${JAVA_HOME}"

BUILDFILE=build.xml

${JAVA_HOME}/bin/java -classpath ${CP} org.apache.tools.ant.Main -buildfile ${BUILDFILE} ${TARGET}

