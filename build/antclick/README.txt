This is the README file for version 1.2 of Helma Object Publisher.

============================
ABOUT HELMA OBJECT PUBLISHER
============================

Helma Object Publisher is a web application server.

With Helma Object Publisher (sometimes simply refered to as Helma or Hop) you
can define Objects and map them to a relational database table. These so-called
HopObjects can be created, modified and deleted using a comfortable
object/container model. Hence, no manual fiddling around with database code is
necessary.

HopObjects are extended JavaScript objects which can be scripted using
server-side JavaScript. Beyond the common JavaScript features, Helma provides
special "skin" and template functionalities which facilitate the rendering of
objects via a web interface.

Thanks to Helma's relational database mapping technology, HopObjects create a
hierarchical structure, the Url space of a Helma site. The parts between slashes
in a Helma Url represent HopObjects (similar to the document tree in static
sites). The Helma Url space can be thought of as an analogy to the Document
Object Model (Dom) in client-side JavaScript.


===================
SYSTEM REQUIREMENTS
===================

WINDOWS:
1) On Windows Hop won't run with Microsoft's version of Java (jview).
You can get a compatible Java runtime from Sun or IBM:
http://java.sun.com/j2se/1.3/jre/download-windows.html
http://www.ibm.com/java/jdk/download/
2) In the Windows start script I've hardcoded c:\java\lib\classes.zip 
in the CLASSPATH argument. Not good.

MACINTOSH:
1) If you are using the Mac version you should own a G3 CPU and/or 
have MRJ 2.2 (http://www.apple.com/java) installed. Other platforms 
have less frustration potential. The Mac OS version currently is not
up-to-date (version 0.1).

LINUX
The recomended virtual machine for running Hop on Linux is Blackdown's 
port of JDK 1.2.2 RC4 (http://www.blackdown.org/java-linux/mirrors.html). 
JDK 1.1.7v3 will work, but much slower and show a lot of CPU activity 
even when the Hop is idle. IBM's version of JDK 1.1.8 also works well, 
but this JVM has some problems of its own with thread handling.


============================
INSTALLING AND RUNNING HELMA
============================

Simply unzip the contents of the archive file into any place on your hard disk.
Start Helma by opening the file hop.bat or hop.sh, respectively.

If you manage to get it running you should be able to connect your browser to
http://127.0.0.1:8080/ (port 8080, that is).

This version is set up to use its own embedded Web server and a very basic
embedded object database. For this reason it is able to run virtually without
installation on any platform with a Java 1.1 virtual machine.

On the other hand, the embedded Web server and object db are meant for
development work and not ready for prime time deployment. For that you'd
probably use an external relational database, the Berkeley DB package and a full
featured Web server like Apache.


=====================================
DOCUMENTATION AND FURTHER INFORMATION
=====================================

Currently, a documentation-in-progress is available online only. Please refer to
http://helma.org/docs/.

For further information http://helma.org generally is a good place. There is
also a mailing-list about Helma-related stuff available at
http://helma.org/lists/listinfo/hop.

For questions, comments or suggestions feel free to contact tobi@helma.at.



--
This document was last modified on Friday 22 June 2001 by tobi@helma.at
