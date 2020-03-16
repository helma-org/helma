#!/bin/sh

##
## Jala Project [http://opensvn.csie.org/traccgi/jala]
##
## Copyright 2004 ORF Online und Teletext GmbH
##
## Licensed under the Apache License, Version 2.0 (the ``License'');
## you may not use this file except in compliance with the License.
## You may obtain a copy of the License at
##
##    http://www.apache.org/licenses/LICENSE-2.0
##
## Unless required by applicable law or agreed to in writing, software
## distributed under the License is distributed on an ``AS IS'' BASIS,
## WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
## See the License for the specific language governing permissions and
## limitations under the License.
##
## $Revision: 3 $
## $LastChangedBy: tobi $
## $LastChangedDate: 2007-01-19 14:33:08 +0100 (Fri, 19 Jan 2007) $
## $HeadURL: https://robert@opensvn.csie.org/jala/trunk/util/HopKit/build.bat $
##

# Uncomment and set this to the directory of your Helma installation
# if you installed jala in a different subdirectory than "modules/jala"
# HELMA_HOME="path/to/helma/installation"

#--------------------------------------------
# No need to change anything below
# buildfile for ant 1.7.0
#--------------------------------------------
if test -z "${ANT_HOME}" ; then
    echo "ERROR: ANT_HOME not found in your environment."
    echo "Please, set the ANT_HOME variable in your environment to match the"
    echo "location of the Apache Ant installation you want to use."
    exit
fi

#---- store path of this script as BUILD_HOME
BUILD_HOME=$(dirname $0)
if [ "${BUILD_HOME:0:1}" != "/" ] ; then
    # convert to absolute path
    BUILD_HOME=${PWD}/${BUILD_HOME}
fi
export BUILD_HOME

#---- store HELMA_HOME if not defined above
if [ -z "$HELMA_HOME" ] ; then
    HELMA_HOME=$BUILD_HOME/../../../..
fi

#---- Slurp the command line arguments.
while [ $# -ne 0  ]
do
    ANT_CMD_LINE_ARGS="${ANT_CMD_LINE_ARGS} $1"
    shift
done

#---- if there is no build.xml in the working directory, use the lib.xml
#---- in this directory
if test ! -f ${PWD}/build.xml ; then
   BUILD_XML="${BUILD_HOME}/lib.xml"
else
   BUILD_XML="${PWD}/build.xml"
fi

${ANT_HOME}/bin/ant -Dant.home="${BUILD_HOME}" -Dbasedir="${PWD}" -lib "${BUILD_HOME}/lib:${HELMA_HOME}/lib" -propertyfile "${PWD}/build.properties" -file $BUILD_XML ${ANT_CMD_LINE_ARGS}

exit
