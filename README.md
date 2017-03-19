_This is the README file for version 1.7.0 of the Helma Javascript Web Application Framework._

__TL;DR__  

- Make sure you have Java 1.4 or higher installed
- Clone this repository
- Build Helma with `./gradlew`
- Invoke `start.sh`, resp. `start.bat`, depending on your platform
- Direct your web browser to http://localhost:8080

# Helma

Helma is an open source web application framework for fast and efficient scripting and serving of your websites and Internet applications.

Helma is written in Java and employs Javascript for its server-side scripting environment, removing the need for compilation cycles and reducing development costs while giving you instant access to leverage the whole wealth of Java libraries out there.

Helma pioneered the simple and codeless mapping of application objects to database tables, which has only recently come into vogue with other web frameworks. In addition, an embedded object-oriented database performs automatic data persistence of unmapped objects.

Helma has proven itself to be stable and fast, capable of serving high traffic sites with hundreds of thousands of dynamic pages per day. The Austrian Broadcasting Corporation, popular weblog hosting sites such as antville.org, twoday.net, and blogger.de, among many others, have successfully been deploying Helma for several years now.

## System Requirements

You need a Java virtual machine 1.4 or higher to run Helma.

For Windows, Linux and Solaris you can get a [Java runtime or development kit](http://java.com/en/download/). If you are on Mac OS X, you might already have a Java runtime that will work well with Helma.

For other operating systems, please consult the documentation about the availability of a Java 1.4 (or higher) runtime.

Helma is built with [Apache Ant](http://ant.apache.org/).

## Installation

Clone this repository to your machine and start the build process with `ant jar`.

After compilation start Helma by invoking `start.bat` or `start.sh`, depending on whether you are on Windows or Linux / Unix / OS X. If the java command is not found, try setting the `JAVA_HOME` variable in the start script to the location of your Java
installation. 

You may also want to have a look at the start script for other settings. You can adjust server wide settings in the `server.properties` file. For example, you should set the `smtp` property to the name of the SMTP server that Helma should use to send e-mail. Applications can be started or stopped by editing the `apps.properties` file through the web interface using the management application that is part of Helma.

After startup you should be able to connect your browser to http://localhost:8080 – port 8080 on the local machine, that is.

Helma comes with a version of [Jetty](http://eclipse.org/jetty/), a lightweight yet industrial strenth web server developed by Mortbay Consulting.

While Jetty works well for deploying real web sites, you may want to run Helma behind an existing web server. This is most easily done by running Helma with the [AJPv13](http://tomcat.apache.org/connectors-doc/index.html) listener which allows you to plug Helma into any web server using the Apache `mod_jk` module.

Finally, Helma can be plugged into Servlet containers using Servlet classes that communicate with Helma either directly or via Java RMI. Be warned that these options may be harder to set up and maintain though, since most of the recent development efforts have been geared towards the AJPv13 setup.

## Documentation and Further Information

After installing and running Helma, you will be able to access introductions to the features of Helma and the various included development tools. Further information you will find on the helma.org website:

- [Guide](http://helma.org/docs/guide/)
- [API Reference](http://helma.org/docs/reference/)
- [Tutorial](http://helma.org/docs/tutorial/)
- [DocBook](http://helma.org/docs/docbook/)

## Mailing List and Support

Please join us on the [Helma mailing lists](http://helma.org/development/mailinglists/) where we will be happy to answer any further questions you may have!
