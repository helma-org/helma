This is the README file for version 1.4.1 of Helma Object Publisher.

===========
ABOUT HELMA
===========

Helma is a scriptable platform for creating dynamic, database backed 
web sites.

Helma provides an easy way to map relational database tables to objects.
These objects are wrapped with a layer of scripts and skins that allow
them to be presented and manipulated over the web. The clue here is that
both functions and skins work in an object oriented manner and force
a clear separation between content, functionality and presentation.
Actions are special functions that are callable over the web. Macros are
special functions that expose functionality to the presentation layer. 
Skins are pieces of layout that do not contain any application logic, 
only macro tags as placeholders for parts that are dynamically provided 
by the application.

In short, Helma provides a one stop framework to create web applications 
with less code and in shorter time than most of the other software out 
there.

===================
SYSTEM REQUIREMENTS
===================

You need a Java virtual machine 1.3 or higher to run Helma.

For Windows, Linux and Solaris you can get a Java runtime or development 
kit from http://java.sun.com/j2se/downloads.html. If you are on Mac OS X, 
you already have a Java runtime that will work well with Helma.

For other operating systems, please consult the documentation about the 
availabilty of a Java 1.3 (or higher) runtime.

===============================================
USING IMAGE MANIPULATION FEATURES WITH JAVA 1.3
===============================================

As of version 1.4.1, Helma uses a new Imaging extension contributed by 
Juerg Lehni that is based on the javax.imageio.* package introduced with 
Java 1.4.0. 

Users of Java 1.3 who wish to use Helma's imaging features need to get the 
JIMI Software Development Kit <http://java.sun.com/products/jimi/>
and add the Jimi classes to Helma's classpath. The most convenient way 
to do this is by installing the Helma source package.

============================
INSTALLING AND RUNNING HELMA
============================

Simply unzip or untar the contents of the archive file into any place 
on your hard disk. Start Helma by invoking hop.bat or hop.sh from the 
command line, depending on whether you are on Windows or 
Linux/Unix/MacOSX. If the java command is not found, try setting the
JAVA_HOME variable in the start script to the location of your Java
installation. 

You may also want to have a look at the start script for other settings.
You can adjust server wide settings in the server.properties file. For
example, you should set the smtp property to the name of the SMTP server
that Helma should use to send Email. Applications can be started or
stopped by editing the apps.properties file through the web interface
using the Management application that is part of Helma.

If you manage to get it running you should be able to connect your
browser to http://localhost:8080/ or http://127.0.0.1:8080/ 
(port 8080 on the local machine, that is).

Helma comes with a version of Jetty, a lightweight yet industrial strenth
web server developed by Mortbay Consulting. See http://jetty.mortbay.com/
for more information. While Jetty works well for deploying real web sites,
you may want to run Helma behind an existing web server. This is most
easily done by running Helma with the AJPv13 listener which allows you to
plug Helma into any web server using the Apache mod_jk module. See
http://jakarta.apache.org/tomcat/tomcat-4.1-doc/jk2/index.html for more
information on mod_jk and AJPv13.

Finally, Helma can be plugged into Servlet containers using Servlet
classes that communicate with Helma either directly or via Java RMI.
(Be warned that these options may be harder to set up and maintain though,
since most of the recent development efforts have been geared towards the
mod_jk/AJPv13 setup.)


=====================================
DOCUMENTATION AND FURTHER INFORMATION
=====================================

Currently, documentation-in-progress is available online at
http://helma.org/. We know that it sucks and hope to do some substantial
improvments within the coming weeks and months.

Your input is highly welcome. There is a mailing-list to discuss Helma at
http://helma.org/lists/listinfo/hop. Don't hesitate to voice any questions,
proposals, complaints, praise you may have on the list. We know we have
a lot to do and to learn, and we're open to suggestions.

For questions, comments or suggestions also feel free to contact
hannes@helma.at.


--

Last modified on June 30, 2004 by Hannes Wallnoefer <hannes@helma.at>
