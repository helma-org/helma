function read_action () {
	this.readApplication ();
	res.redirect (this.href("main"));
}

function main_action () {
	if (checkAddress()==false)
	   return;
	if (checkAuth(this.getParentElement ())==false)
	   return;
	this.renderSkin ("frameset");
}


function prototypes_action () {
	if (checkAddress()==false)
	   return;
	if (checkAuth(this.getParentElement ())==false)
	   return;
	res.data.body = this.renderSkinAsString ("prototypes");
	renderSkin ("api");
}


function summary_action () {
	if (checkAddress()==false)
	   return;
	if (checkAuth(this.getParentElement ())==false)
	   return;
	res.data.body = this.renderSkinAsString ("summary");
	renderSkin ("api");
}


function functionindex_action () {
	if (checkAddress()==false)
	   return;
	if (checkAuth(this.getParentElement ())==false)
	   return;
	res.data.body = this.renderSkinAsString ("functionindex");
	renderSkin ("api");
}


function render_action () {
   // set res.data.rendering, this will suppress the link back to the manage
   // console in the apidocs actions
   res.data.rendering = true;
	if (checkAddress()==false)
	   return;
	if (checkAuth(this.getParentElement ())==false)
	   return;
	var prefix = this.href ("");
	this.storePage (this, "main", "", "index.html");
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
			this.storePage (subarr[j], "main", "../", subarr[j].getElementName () + ".html");
			ct += 1;
		}
	}
	res.data.body =  '<body>rendering API ...<br/>wrote ' + ct + ' files<br/><br/>';
	res.data.body += '<a href="' + root.href ("main") + '">back to manage console</a>';
   res.data.title = "rendering helma api";
   res.data.head = renderSkinAsString("head");
   renderSkin ("basic");
}
