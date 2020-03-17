# About Jala

Jala is an open-source collection of JavaScript modules for Helma Object Publisher. Copyright 2004 ORF Online und Teletext GmbH, Vienna (Austria). You can find more information about each module in the API Documentation located in the `docs` directory.

## Licensing

Jala itself is licensed under the Apache 2.0 License, but parts of Jala require third party libraries coming with different licenses. You can find all necessary information in the `licenses` directory.

## Installation

Move the Jala folder into the `modules` directory of your Helma installation. To include a certain Jala module simply add the following line to your Helma application's source code (replace `[name]` with the desired module name):

    app.addRepository("./modules/jala/code/[name].js");

If you want to include the whole Jala package at once, you can use the `all` module for convenience:

    app.addRepository("./modules/jala/code/all.js");

Alternatively, you can import the Jala module from within Helma's 
`apps.properties` file (replace `[appName]` with the name of your Helma application,  `[n]` with a number between 0 and 9 and `[moduleName]` with the desired module 
name):

    [appName].respository.[n] = ./modules/jala/code/[moduleName].js

More information about the `addRepository()` method and generally including repositories in a Helma application is available at 
http://helma.org/stories/77712/.

## Contact, Bugs and Feedback

The Jala Project is hosted at https://dev.orf.at/jala/ providing all necessary information about Subversion access, Ticketing, Releases etc.

Although we encourage you to post your questions and comments as ticket, we also provide a mailing list for convenience (details at 
https://dev.orf.at/trac/jala/wiki/MailingList).

For immediate contact you can reach the developers via jaladev AT gmail.com.
