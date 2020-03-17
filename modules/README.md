# Helma Modules

The Helma modules directory is organized in several groups:

* **modules/core** which contains extensions to core JavaScript types such as
Object, Array, or Date.
* **modules/helma** which provide new functionality to JavaScript, usually by
wrapping a Java library.
* **modules/jala**, a Git submodule providing the very useful ORF.at libraries for Helma

To use a HelmaLib module in your Helma application, you need to add it to the
app's repositories. The simplest way to do so is by using the `app.addRepository()`
function:

    app.addRepository('modules/helma/Search.js');
