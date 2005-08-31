/**
  * macro rendering a skin
  * @param name name of skin
  */
function skin_macro(par) {
    if (par && par.name) {
        this.renderSkin(par.name);
    }
}


/**
  * macro-wrapper for href-function
  * @param action name of action to call on this prototype, default main
  */
function href_macro(par) {
    return this.href((par && par.action) ? par.action : "main");
}


/**
 * Macro returning the URL of an application.
 * using absoluteURI if set in app.properties, otherwise we go for the href calculated by 
 * the application (which is using baseURI if set)
 */
function url_macro() {
    var str = this.getProperty("absoluteuri");
    if (str != null && str != "") {
        return str;
    } else {
        return this.getRootHref();
    }
}


/** 
  * Macro returning the title of an application
  */
function title_macro() {
    var title = this.name;
    return(title);
}


/**
  * Macro rendering a description of an application from a 
  * file called description.txt or doc.html located in the
  * app's root. Limits description to 200 chars.
  * @param Object Macro parameter object
  */
function description_macro(param) {
    var str = "";
    var appHome = this.getAppDir();
    var f = new File(this.getAppDir().toString(), "description.txt");
    if (!f.exists())
        f = new File(this.getAppDir().toString(), "doc.html");
    if (f.exists()) {
        str = f.readAll();
        if (str.length > 200)
            str = str.substring(0, 200) + "...";
    }
    return(str);
}


/**
  * Macro returning the server's uptime nicely formatted
  */
function uptime_macro() {
    return formatAge((java.lang.System.currentTimeMillis() - this.starttime) / 1000);
}


/**
  * Macro returning the number of active sessions.
  * parameter used by global.formatCount
  * @see global.formatCount
  */
function countSessions_macro(par) {
    if (this.isActive() == true)
        return this.sessions.size() + formatCount(this.sessions.size(), par);
    else
        return 0;
}


/**
  * Macro returning the number of logged-in users or the list
  * of logged-in users if http-parameter showusers is set to true.
  * Makes use of this.countUsers_macro and this.listUsers_macro
  * @see application.countUsers_macro
  * @see application.listUsers_macro
  */
function users_macro(par) {
    if (req.data.showusers == "true") {
        this.listUsers_macro(par);
    } else {
        this.countUsers_macro(par);
    }
}


/**
  * Macro returning the number of logged-in users if application
  * is active
  * parameter used by global.formatCount
  * @see global.formatCount
  */
function countUsers_macro(par) {
    if (this.isActive() == true)
        return this.activeUsers.size() + formatCount(this.activeUsers.size(), par);
    else
        return 0;
}


/**
  * Macro rendering the list of logged-in users if application is active
  * @param separator html-code written between elements
  */
function listUsers_macro(par) {
    var separator = (par && par.separator) ? par.separator : ", ";
    if (this.activeUsers.size() == 0)
        return "";
    var users = this.activeUsers.iterator();
    while (users.hasNext()) {
        res.write(users.next().__name__);
        if (users.hasNext()) {
            res.write(separator);
        }
    }
}


/**
  * Macro returning the number of active evaluators (=threads)
  */
function countActiveEvaluators_macro() {
    return this.countActiveEvaluators();
}


/**
  * Macro returning the number of free evaluators (=threads)
  */
function countFreeEvaluators_macro() {
    return this.countFreeEvaluators();
}


/**
  * Macro returning the current number of objects in the cache
  */
function cacheusage_macro(param) {
    return this.getCacheUsage();
}


/**
  * Macro returning the number of objects allowed in the cache
  */
function cachesize_macro(param) {
    return this.getProperty("cachesize", "1000");
}


/**
  * Macro formatting the number of requests in the last 5 minutes
  */
function requestCount_macro(par) {
    if (app.data.stat == null || app.data.stat[this.name] == null)
        return "not available";
    if (this.isActive()) {
        var obj = app.data.stat[this.name];
        return obj.requestCount + formatCount(obj.requestCount, par);
    } else {
        return 0 + formatCount(0, par);
    }
}


/**
  * Macro formatting the number of errors in the last 5 minutes
  */
function errorCount_macro(par) {
    if (app.data.stat == null || app.data.stat[this.name] == null)
        return "not available";
    if (this.isActive()) {
        var obj = app.data.stat[this.name];
        return obj.errorCount + formatCount(obj.errorCount, par);
    } else {
        return 0 + formatCount(0, par);
    }
}


/**
* Macro formatting app.properties
*/
function properties_macro(par) {
    formatProperties(this.getProperties(), par);
}


function repositories_macro(param) {
    var repos = this.getRepositories().iterator();
    while (repos.hasNext())
        res.writeln(repos.next().getName());
}

