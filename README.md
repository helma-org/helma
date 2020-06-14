# How to Helma

## TL;DR

- Make sure you have Java 1.6 or higher installed
- Download and unpack the [latest release](https://github.com/antville/helma/releases)
- Invoke `./bin/helma`, resp. `./bin/helma.bat`, depending on your platform
- Direct your web browser to <http://localhost:8080>

## Introduction

Helma is an open source web application framework for fast and efficient scripting and serving of your websites and Internet applications.

Helma is written in Java and employs JavaScript for its server-side scripting environment, removing the need for compilation cycles and reducing development costs while giving you instant access to leverage the whole wealth of Java libraries out there.

Helma pioneered the simple and codeless mapping of application objects to database tables, which has only recently come into vogue with other web frameworks. In addition, an embedded object-oriented database performs automatic data persistence of unmapped objects.

Helma has proven itself to be stable and fast, capable of serving high traffic sites with hundreds of thousands of dynamic pages per day. The Austrian Broadcasting Corporation, popular weblog hosting sites such as antville.org, twoday.net, and blogger.de, among many others, have successfully been deploying Helma for several years now.

Although Helma became a Grande Dame of server-side JavaScript already decades ago when she performed in cozy Finnish clubs, she appears somehow retired nowadays. Nevertheless, she is here to stay for those last ones out there still tinkering with this nostalgic and wonderful piece of software.

## System Requirements

You need a Java virtual machine 1.6 or higher to run Helma.

Please consult the documentation of your platform how to obtain and install Java.

You also can directly download a [Java runtime or development kit](https://www.oracle.com/java/technologies/javase-downloads.html#javasejdk) from Oracle.

Helma is built with [Gradle](https://gradle.org), the build task depends on the binaries [rsync](https://rsync.samba.org) and [npx](https://www.npmjs.com/package/npx) being installed on your system.

## Development

Clone this repository to your machine and start the build process with `./gradlew install`. The build script is going to ask you if you want to update the installation, enter `y`.

> ⚠️    
> Please be aware that this step is going to overwrite files in the installation directory – escpecially at a later time when there might be substantial changes. Should this happen by accident you find the previous installation in the `backups` directory.
>
> Alternatively, you could move or copy the desired files manually from the installation directory `build/install/helma`.

After all files are put into place start Helma by invoking `./bin/helma.bat` or `./bin/helma`, depending on whether you are on Windows or Linux / Unix / OS X, respectively. If the `java` command is not found, try setting the `JAVA_HOME` environment variable to the location of your Java installation.

You can adjust server-wide settings in the `server.properties` file. For example, you could set the `smtp` property to the name of the SMTP server that Helma should use to send e-mail. Applications can be started or stopped by editing the `apps.properties` file through the web interface using the management application that is part of Helma.

If all goes well you should be able to connect your browser to <http://localhost:8080> – port 8080 on the local machine, that is.

Helma comes with a version of [Jetty](http://eclipse.org/jetty/), a lightweight yet industrial strength web server.

While Jetty works well for development and in fact deploying real web sites, you might want to run Helma with the web server you are already using. This is most easily done by proxying Helma. Please consult the documentation of your web server how to achieve this.

Finally, Helma can be plugged into Servlet containers using Servlet classes that communicate with Helma either directly or via Java RMI. Be warned that these options may be harder to set up and maintain though, since most of the recent development efforts have been geared towards a proxied setup.

## Documentation and Further Information

After installing and running Helma, you will be able to access introductions to the features of Helma and the various included development tools. Further information you will find on the helma.org website:

> 😿  
> Unfortunately, the Helma website disappeard in the meantime. However, with some archaeological web digging and thanks to the great search engines and archive services out there it is still possible to find useful resources.

- [helma.org at Internet Archive](http://web.archive.org/web/20180122132315/http://helma.org)
- [Documentation](http://web.archive.org/web/20100530234322/http://helma.org/documentation/)
- [API Reference](https://helma.serverjs.org/reference/)
- [Tutorial](http://web.archive.org/web/20100526182848/http://helma.org/Documentation/Object-Relational+Mapping+Tutorial/)
- [DocBook](http://dev.orf.at/download/helma/documentation/documentation.pdf)
