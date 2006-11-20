/**
* renders the api of a given application. used from commandline.
*/
function renderApi(appName) {

    // supress security checks when accessing actions
    res.data.noWeb = true;

    // start the application
    this.startApplication(appName);
    var appObj = this.getApp(appName);
    var docApp = appObj.getChildElement("api");

    // now render the api
    var ct = docApp.renderApi();
    writeln("rendered " + ct + " files");

    // cleanup
    this.stopApplication(appName);
}


/**
  * lists all applications in appdir.
  * for active apps use this.getApplications() = helma.main.Server.getApplications()
  */
function getAllApplications() {
    var appsDir = this.getAppsHome();
    var dir = appsDir.list();
    var arr = new Array();
    if (!dir)
      return arr;
    for (var i = 0; i < dir.length; i++) {
        if (dir[i].toLowerCase() != "cvs" && dir[i].indexOf(".") == -1)
            arr[arr.length] = this.getApp(dir[i]);
    }
    return arr;
}


/**
  * get application by name, constructs an hopobject of the prototype application
  * if the app is not running (and therefore can't be access through
  * helma.main.ApplicationManager).
  * ATTENTION: javascript should not overwrite helma.main.Server.getApplication() which
  * retrieves active applications.
  * @arg name of application
  */
function getApp(name) {
    if (name == null || name == "")
        return null;
    var appObj = this.getApplication(name);
    if (appObj == null)
        appObj = new Application(name);
    return appObj;
}

/**
 * Method used by Helma path resolution.
 */
function getChildElement(name) {
    return this.getApp(name);
}


