function main_action () {
	this.getParentElement ().readFiles ();
	res.data.body = this.renderSkinAsString ("main");
	renderSkin ("api");
}



