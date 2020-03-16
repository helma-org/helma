function getChildElement(name) {
    if (this.get(name)) 
        return this.get(name);
    var page = new Guide();
    page.name = name;
    page.parent = this;
    return page;
}
