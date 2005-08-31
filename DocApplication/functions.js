/**
* get the prototype of any doc-object (either a prototype, a function or a tag)
*/
function getDocPrototype(obj) {
    var tmp = obj;
    while (tmp != null && tmp.getType() != this.PROTOTYPE) {
        tmp = tmp.getParentElement();
    }
    return tmp;
}


/**
  * get a prototype of this docapplication, ie get on of the children of this object
  */
function getPrototype(name) {
    return this.getChildElement("prototype_" + name);
}


function getDir(dir, obj) {
    dir.mkdir();
    if (obj.getType() == this.APPLICATION) {
        return dir;
    } else {
        var protoObj = this.getDocPrototype(obj);
        var dir = new File (dir, protoObj.getElementName());
        dir.mkdir();
        return dir;
    }
}

function renderApi() {
    var prefix = this.href("");
    this.storePage(this, "main", "", "index.html");
    this.storePage(this, "prototypes");
    this.storePage(this, "summary");
    this.storePage(this, "functionindex");
    var ct = 4;
    var arr = this.listChildren();
    for (var i = 0; i < arr.length; i++) {
        this.storePage(arr[i], "list", "../");
        this.storePage(arr[i], "main", "../");
        ct += 2;
        var subarr = arr[i].listChildren();
        for (var j = 0; j < subarr.length; j++) {
            this.storePage(subarr[j], "main", "../", subarr[j].getElementName() + ".html");
            ct += 1;
        }
    }
    return ct;
}


function storePage(obj, action, backPath, filename) {
    if (filename == null)
        var filename = action + ".html";
    var str = this.getPage(obj, action, backPath);
    var appObj = this.getParentElement();
    var dir = new File (appObj.getAppDir().getAbsolutePath(), ".docs");
    dir = this.getDir(dir, obj);
    var f = new File (dir, filename);
    f.remove();
    f.open();
    f.write(str);
    f.close();
    app.log("wrote file " + f.getAbsolutePath());
}


function getPage(obj, action, backPath) {
    backPath = (backPath == null) ? "" : backPath;
    res.pushStringBuffer();
    eval("obj." + action + "_action ();");
    var str = res.popStringBuffer();
    // get the baseURI out of the url and replace
    // it with the given relative prefix
    // (keep anchors in regex!)
    var reg = new RegExp ("href=\"" + this.href("") + "([^\"#]+)([^\"]*)\"", "gim");
    str = str.replace(reg, "href=\"" + backPath + "$1.html$2\"");
    var reg = new RegExp ("src=\"" + this.href("") + "([^\"#]+)([^\"]*)\"", "gim");
    str = str.replace(reg, "src=\"" + backPath + "$1.html$2\"");
    // shorten links, so that function files can move up one directory
    // in the hierarchy
    var reg = new RegExp ("(prototype_[^/]+/[^/]+)/main.html", "gim");
    str = str.replace(reg, "$1.html");
    return str;
}


