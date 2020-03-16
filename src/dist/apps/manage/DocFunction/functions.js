/**
 * Method used by Helma for URL composition.
 */
function href(action) {
    var base = this.getParentElement().href() 
             + this.getElementName() + "/";
    return action ? base + action : base;
}


