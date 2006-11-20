/**
 * construct an application object so that we can use
 * skins for non-active applications too
 * @arg name
 */
function constructor(name) {
    this.name = name;
}

/**
  * return true/false to determine if application is running
  */
function isActive() {
    if (root.getApplication(this.name) == null)
        return false;
    else
        return true;
}

/**
 * Method used by Helma for URL composition.
 */
function href(action) {
    var base = root.href() + this.name + "/";
    return action ? base + action : base;
}

/**
 * Method used by Helma request path resolution.
 */
function getChildElement(name) {
    if (name == "api")
        return this.getDoc();
    return null;
}
