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



function render_action () {
	res.write ("<html><head><title>render</title></head><body>rendering ... " + new Date () );
	var prefix = this.href ("");
	this.storePage (this, "main");
	this.storePage (this, "prototypes");
	this.storePage (this, "summary");
	var arr = this.listChildren ();
	for (var i=0; i<arr.length; i++) {
		this.storePage (arr[i], "list", "../");
		this.storePage (arr[i], "main", "../");
		res.writeln (arr[i]);
		var subarr = arr[i].listChildren ();
		for (var j=0; j<subarr.length; j++) {
			this.storePage (subarr[j], "main", "../../");
			res.writeln (subarr[j]);
		}
	}
}


function getDocPrototype (obj) {
	var tmp = obj;
	while (tmp!=null && tmp.getType () != this.PROTOTYPE) {
		tmp = tmp.getParentElement ();
	}
	return tmp;
}

function getDir (dir, obj) {
	dir.mkdir ();
	if (obj.getType () == this.APPLICATION) {
		return dir;
	} else if (obj.getType () == this.PROTOTYPE) {
		var protoObj = this.getDocPrototype (obj);
		var dir = new File (dir, protoObj.getElementName ());
		dir.mkdir ();
		return dir;
	} else {
		var protoObj = this.getDocPrototype (obj);
		var dir = this.getDir (dir, protoObj);
		res.writeln("dir="+dir);
		dir = new File (dir, obj.getElementName ());
		res.writeln("dir="+dir);
		dir.mkdir ();
		return dir;
	}
}


function storePage (obj, action, backPath) {
	var str = this.getPage (obj, action, backPath);
	var appObj = this.getParentElement ();
	var dir = new File (appObj.getAppDir ().getAbsolutePath (), ".docs");
	dir = this.getDir (dir, obj);
	var f = new File (dir, action + ".html");
	f.remove ();
	f.open ();
	f.write (str);
	f.close ();
}


function getPage (obj, action, backPath) {
	backPath = (backPath==null) ? "" : backPath;
	res.pushStringBuffer ();
	eval ("obj." + action + "_action ();");
	var str = res.popStringBuffer ();
	var reg = new RegExp ("href=\"" + this.href ("") + "([^\"]+)\"");
	reg.global = true;
	str = str.replace (reg, "href=\"" + backPath + "$1.html\"");
	var reg = new RegExp ("src=\"" + this.href ("") + "([^\"]+)\"");
	reg.global = true;
	str = str.replace (reg, "src=\"" + backPath + "$1.html\"");
	return str;
}


