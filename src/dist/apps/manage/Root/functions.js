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
    var dir = appsDir.listFiles();
    var arr = new Array();
    var seen = {};
    // first check apps directory for apps directories
    if (dir) {
        for (var i = 0; i < dir.length; i++) {
            if (dir[i].isDirectory() && dir[i].name.toLowerCase() != "cvs") {
                arr[arr.length] = this.getApp(dir[i].name);
                seen[dir[i].name] = true;
            }
        }
    }
    // then check entries in apps.properties for apps not currently running
    var props = wrapJavaMap(root.getAppsProperties(null));
    for (var i in props) {
        if (i.indexOf(".") < 0 && !seen[i] && !root.getApplication(i)) {
            arr[arr.length] = this.getApp(i);
        }
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


