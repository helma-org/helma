/**
  * main action, show server-stats
  * perform start, stop, restart and flush-action
  *
  */
function main_action() {

    if (checkAddress() == false)    return;

    if (req.data.app != null && req.data.app != "" && req.data.action != null && req.data.action != "") {

        var appObj = root.getApp(req.data.app);
        // check access for application. md5-encoded uname/pwd can also be put in
        // app.properties to limit access to a single app
        if (checkAuth(appObj) == false)    return;

        if (req.data.action == "start") {
            this.startApplication(req.data.app);
            res.redirect(appObj.href("main"));

        } else if (req.data.action == "stop") {
            if (checkAuth() == false)    return;
            this.stopApplication(req.data.app);
            res.redirect(root.href("main"));

        } else if (req.data.action == "restart") {
            this.stopApplication(req.data.app);
            this.startApplication(req.data.app);
            res.redirect(appObj.href("main"));

        } else if (req.data.action == "flush") {
            appObj.clearCache();
            res.redirect(appObj.href("main"));

        }

    }

    // output only to root
    if (checkAuth() == false)    return;

    res.data.title = "Helma Object Publisher - Serverinfo";
    res.data.body = this.renderSkinAsString("main");
    renderSkin("global");
}

/**
* return the helma object publisher logo, built into hop core
* to be independent of static html-paths
*/
function image_action() {
    if (checkAddress() == false)    return;

    res.contentType = "image/gif";
    res.writeBinary(Packages.helma.util.Logo.hop);
}

function makekey_action() {

    if (checkAddress() == false)
        return;

    var obj = new Object();
    obj.msg = "";
    if (req.data.username != null && req.data.password != null) {

        // we have input from webform
        if (req.data.username == "")
            obj.msg += "username can't be left empty!<br>";
        if (req.data.password == "")
            obj.msg += "password can't be left empty!<br>";
        if (obj.msg != "") {
            obj.username = req.data.username;
            res.reset();
            res.data.body = renderSkinAsString("pwdform", obj);
        } else {
            // render the md5-string:
            obj.propsString = "adminAccess=" + Packages.org.apache.commons.codec.digest.DigestUtils.md5Hex(req.data.username + "-" + req.data.password) + "<br>\n";
            res.data.body = renderSkinAsString("pwdfeedback", obj);
        }

    } else {

        // no input from webform, so print it
        res.data.body = renderSkinAsString("pwdform", obj);

    }

    res.data.title = "username & password on " + root.hostname_macro();
    res.data.head = renderSkinAsString("head");
    renderSkin("basic");
}

/**
* prints server-stats for mrtg-tool.
* doesn't check username or password, so that we don't have
* to write them cleartext in a mrtg-configfile but checks the
* remote address.
*/
function mrtg_action() {

    if (checkAddress() == false)
        return;


    if (req.data.action == "memory") {

        res.write(this.jvmTotalMemory_macro() + "\n");
        res.write(this.jvmFreeMemory_macro() + "\n");
        res.write("0\n0\n");

    } else if (req.data.action == "netstat" && isLinux()) {

        var str = (new File("/proc/net/tcp")).readAll();
        var arr = str.split("\n");
        res.write(arr.length - 2 + "\n");
        res.write("0\n0\n0\n");

    } else if (req.data.action == "loadavg" && isLinux()) {

        // load average of last 5 minutes:
        var str = (new File("/proc/loadavg")).readAll();
        var arr = str.split(" ");
        res.write(arr[1] * 100 + "\n");
        res.write("0\n0\n0\n");

    } else {
        res.write("0\n0\n0\n0\n");
    }
}
