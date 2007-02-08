This is the README file for the Helma build files as part of the Helma Object Publisher.

PREREQUISITES
=============

The Helma build script is uses Apache Ant. 
For more information about Ant, see <http://ant.apache.org/>.

For checking out the source files from Helma's CVS you also need a CVS client. 
More information about CVS at <http://www.cvshome.org/>.


STARTING BUILD
==============

The build system is started by invoking the shell script appropriate to your 
platform, ie. build.sh for *nix (Linux, NetBSD etc.) and build.bat for Windows 
systems. You need to modify the script and set the JAVA_HOME to fit your system.

The generic syntax is

    ant target

The parameter "target" specifies one of the build targets listed below.


BUILD TARGETS
=============

compile
    Compiles the source files contained in the work/checkout/hop/ directory into the work/classes/ directory (which will be created if necessary).

jar
    Creates a helma.jar file (snapshot) in the lib-directory. The file is named helma-yyyymmdd.jar.

javadocs
    Creates the javadoc API documentation.

package
    Creates the full helma distribution packages and places them in the dist directory.

app [name]
    Gets an application from the cvs, zips/targzs it and places the files in the dist directory.

module [name]
    Gets a module from the cvs, zips it and places the file in the dist directory.


--



