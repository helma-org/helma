function translateType(filter) {
    if (filter == "actions")
        return Packages.helma.doc.DocElement.ACTION;
    else if (filter == "functions")
        return Packages.helma.doc.DocElement.FUNCTION;
    else if (filter == "macros")
        return Packages.helma.doc.DocElement.MACRO;
    else if (filter == "skins")
        return Packages.helma.doc.DocElement.SKIN;
    else if (filter == "properties")
        return Packages.helma.doc.DocElement.PROPERTIES;
    else
        return -1;
}

/**
 * Get the application we're part of.
 */
function getApplication() {
    return this.getParentElement();
}

/**
 * Method used by Helma for URL composition.
 */
function href(action) {
    var base = this.getParentElement().href() 
             + this.getElementName() + "/";
    return action ? base + action : base;
}
