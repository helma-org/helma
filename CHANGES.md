# Change Log

## May 17, 2020

* Added support for colored log output
* Added xgettext and po2js tasks (only running with Antville right now)

## April 13, 2020

* Added support for gzip compressed response in helma.Http
* Fixed helma.Http.getURL() not following redirects if protocol changes (e.g. http → https)
* Fixed references to obsolete Base64 encoder in modules
* Updated JavaMail library to implementation package

## March 21, 2020

* Completely rewrote build system with Gradle
* Separated launcher from main source as Gradle subproject
* Launcher now includes all JARs found in `lib`
* Upgraded Rhino to version 1.7.12
* Upgraded Jetty to version 9.x
* Fixed compatibility issues with Java 11
* Removed support for Apache JServ Protocol (AJP)
* Added support for CommonJS require() method
* Allow variable arguments in res.write() and res.writeln()
* Replaced Helma’s MD5 and Base64 methods with equivalent methods from Apache Commons
* Refactored String methods from Java to JavaScript: encode(), encodeForm(), encodeXml(), stripTags()
* Replaced custom String methods with Rhino’s built-in ones: endsWith(), repeat(), startsWith(), trim()
* Refactored custom String.pad() method with built-in methods
* Redefined custom Array.contains() method with built-in Array.includes()
* Refactored custom Array methods with built-in methods: intersection(), union()
