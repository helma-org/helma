==============
ABOUT ANTCLICK
==============

Antclick is an integrated Helma/Antville package. It comes with 
integrated web server and SQL database. It should run out of the 
box although you may have to modify the start script. 

This version of Antclick contains Mckoi as the embedded database 
and Jetty as the embedded web server.

Although Antcklick comes preconfigured with the Mckoi database, it 
can easily be set up to run with other databases such as MySQL and 
Oracle. It is also possible to configure it to use a fully featured 
web server instead of the built-in web server. 


======================
INSTALLING AND RUNNING 
======================

Simply uncompress the content of the archive file into any place on 
your hard disk. Start Helma on Windows by opening the file hop.bat. 
On Unix systems open a terminal window, change to the Antclick 
directory and type ./hop.sh.

If you manage to get it running you should be able to connect your
browser to http://127.0.0.1:8080/ (port 8080, that is). Now you can 
set up and configure your antville site.


==============
ABOUT ANTVILLE
==============

Antville is an open source project aimed to the development of an 
"easy to maintain and use" weblog-hosting system. It is not limited 
to just one weblog, it can easily host up to several hundred or 
thousand weblogs (the number of weblogs is more limited by the site 
owner's choice and server power than software limitations).

Antville is entirely written in JavaScript and based on the Helma 
Object Publisher, a powerful and fast scriptable open source web 
application server (which itself is written in Java). Antville works 
with a relational database in the backend.

============================
ABOUT HELMA OBJECT PUBLISHER
============================

Helma Object Publisher is a web application server.

With Helma Object Publisher (sometimes simply refered to as Helma or
Hop) you can define Objects and map them to a relational database
table. These so-called HopObjects can be created, modified and deleted
using a comfortable object/container model. Hence, no manual fiddling
around with database code is necessary.

HopObjects are extended JavaScript objects which can be scripted using
server-side JavaScript. Beyond the common JavaScript features, Helma
provides special "skin" and template functionalities which facilitate
the rendering of objects via a web interface.

Thanks to Helma's relational database mapping technology, HopObjects
create a hierarchical structure, the Url space of a Helma site. The
parts between slashes in a Helma Url represent HopObjects (similar to
the document tree in static sites). The Helma Url space can be thought
of as an analogy to the Document Object Model (Dom) in client-side
JavaScript.


===================
SYSTEM REQUIREMENTS
===================

You need Java 2 runtime version 1.3 or higher to run Helma. Helma has 
been used successfully on Windows, Linux and Mac OS X platforms.


============================
INSTALLING AND RUNNING HELMA
============================

Simply unzip the contents of the archive file into any place on your
hard disk. Start Helma by opening the file hop.bat or hop.sh,
respectively.

If you manage to get it running you should be able to connect your
browser to http://127.0.0.1:8080/ (port 8080, that is).

This version is set up to use its own embedded Web server and a very
basic embedded object database. For this reason it is able to run
virtually without installation on any platform with a Java 1.1 virtual
machine.

On the other hand, the embedded Web server and object db are meant for
development work and not ready for prime time deployment. For that
you'd probably use an external relational database, the Berkeley DB
package and a full featured Web server like Apache.


=====================================
DOCUMENTATION AND FURTHER INFORMATION
=====================================

Currently, a documentation-in-progress is available online only.
Please refer to http://helma.org/.

For further information http://helma.org generally is a good place.
There is also a mailing-list about Helma-related stuff available at
http://helma.org/lists/listinfo/hop.

For questions, comments or suggestions feel free to contact
tobi@helma.at.



--

This document was last modified on Friday 25 October 2002 by
hannes@helma.at
