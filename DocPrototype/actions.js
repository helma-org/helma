function list_action () {
	res.data.body = this.renderSkinAsString ("list");
	renderSkin ("api");
}

function main_action () {
	res.data.body = this.renderSkinAsString ("main");
	renderSkin ("api");
}

