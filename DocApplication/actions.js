function main_action () {
	this.renderSkin ("frameset");
}


function prototypes_action () {
	res.data.body = this.renderSkinAsString ("prototypes");
	renderSkin ("api");
}


function summary_action () {
	res.data.body = this.renderSkinAsString ("summary");
	renderSkin ("api");
}


function functionindex_action () {
	res.data.body = this.renderSkinAsString ("functionindex");
	renderSkin ("api");
}


function render_action () {
	res.writeln("<html><head><title>render</title></head><body>rendering API ... ");
	var prefix = this.href ("");
	this.storePage (this, "main");
	this.storePage (this, "prototypes");
	this.storePage (this, "summary");
	this.storePage (this, "functionindex");
	var ct = 4;
	var arr = this.listChildren ();
	for (var i=0; i<arr.length; i++) {
		this.storePage (arr[i], "list", "../");
		this.storePage (arr[i], "main", "../");
		ct += 2;
		var subarr = arr[i].listChildren ();
		for (var j=0; j<subarr.length; j++) {
			this.storePage (subarr[j], "main", "../../");
			ct += 1;
		}
	}
	res.writeln (" ... wrote " + ct + " files");
}
