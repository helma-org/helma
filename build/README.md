_This is the README file for the Helma build files as part of the Helma Object Publisher._

## Prerequisites

The Helma build script is using Apache Ant. 
For more information about Ant, see <http://ant.apache.org/>.

## Building

The build system is started by invoking the shell script appropriate to your 
platform, ie. build.sh for *nix (Linux, NetBSD etc.) and build.bat for Windows 
systems. You probably need to modify the script and set the `JAVA_HOME` to fit your system.

The generic syntax is

    ant target

The parameter `target` specifies one of the build targets listed below.

## Build Targets

**compile**  
Compiles the source files into the `./classes` directory (which will be created if necessary).

**jar**  
Creates a helma.jar file (snapshot) in the lib directory. The file is named `helma-yyyymmdd.jar`.

**javadocs**  
Creates the JavaDoc API documentation.

**package**  
Creates the full Helma distribution packages and places them in the dist directory.

**app [name]**  
Gets an application from the source code repository, zips / targzs it and places the files in the dist directory.

**module [name]**  
Gets a module from the source code repository, zips it and places the file in the dist directory.
