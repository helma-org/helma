

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
		dir = new File (dir, obj.getElementName ());
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
	app.log ("wrote file " + f.toString ());
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


