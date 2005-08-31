function list_action() {
    if (checkAddress() == false)
        return;
    if (checkAuth() == false)
        return;
    res.data.body = this.renderSkinAsString("list");
    renderSkin("api");
}

function main_action() {
    if (checkAddress() == false)
        return;
    if (checkAuth() == false)
        return;
    res.data.body = this.renderSkinAsString("main");
    renderSkin("api");
}

