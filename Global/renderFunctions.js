

function renderLink (docEl, param) {
	var text = "";
	if (docEl.getType () == docEl.APPLICATION || docEl.getType () == docEl.PROTOTYPE) {
		text = docEl.getName ();
	} else if (docEl.getType () == docEl.SKIN) {
		text = docEl.getName () + ".skin";
	} else if (docEl.getType () == docEl.MACRO) {
		if (param.handler!="false" && docEl.getParentElement () && docEl.getParentElement().getName()!="global") {
			text = docEl.getParentElement ().getName () + ".";
		}
		var str = docEl.getName ();
		if (str.indexOf("_macro")) {
			text += str.substring (0, str.length-6);
		}			
	} else if (docEl.getType () == docEl.FUNCTION) {
		var text = docEl.getName () + "(";
		var arr = docEl.listParameters ();
		for (var i=0; i<arr.length ;i++) {
			text += arr[i];
			if (i<arr.length-1)
				text += ",&nbsp;";
		}
		text += ")";
	} else {
		text = docEl.getName ();
	}
	param.href = docEl.href ("main");
	if (!param.target) {
		param.target = "main";
	}
	return renderLinkTag (param) + text + '</a>';
}




function renderLinkTag (param) {
	var sb = new java.lang.StringBuffer ();
	sb.append ('<a');
	for (var i in param) {
		sb.append (' ');
		sb.append (i);
		sb.append ('="');
		sb.append (param[i]);
		sb.append ('"');
	}
	sb.append ('>');
	return sb.toString ();
}


/**
  * renders the name of the location relative to the application
  * root.
  */
function renderLocation (docEl, param) {
	var f = docEl.getLocation ();
        // with repositories, always display full file path
        return f.getAbsolutePath();
}



/**
  * renders tag list.
  * @param param.skin skin to render on found DocTags
  * @param param.separator String printed between tags
  * @param param.type type string (param|return|author|version|see) to filter tags.
  */
function renderTags (docEl, param) {
	var skinname = (param.skin) ? param.skin : "main";
	var type = param.type;
	if (type=="params")
		type = "param";
	else if (type=="returns")
		type = "return";
    else if (type=="arg")
        type = "param";
	var str = "";
	var arr = docEl.listTags ();
	for (var i=0; i<arr.length; i++) {
		if (arr[i].getType () == type) {
			if (type=="see" || type=="overrides") {
				param.link = renderReference (arr[i], docEl);
			}
			str += arr[i].renderSkinAsString (skinname, param);
			str += (param.separator) ? param.separator : "";
		}
	}
	return str;
}


/**
  * renders a reference to functions in other prototypes, masks
  * urls in a see tag
  * (see- and overrides-tags)
  * @param docTagObj
  * @param docEl needed to be able to walk up to application object
  */
function renderReference (docTagObj, docEl) {		
	// prepare the text:
	var text = docTagObj.getText ();
	text = new java.lang.String (text);
	text = text.trim ();
	if (text.indexOf("http://")==0)	{
		// an url is a simple job
		return '<a href="' + text + '" target="_new">' + text + '</a>';
	}	else	{
		// make sure we only use the first item in the text so that unlinked comments
		// can follow, store & split the that
		var tok = new java.util.StringTokenizer (text);
		var tmp = tok.nextToken ();
		text = " " + text.substring (tmp.length + 1);
		var parts = tmp.split(".");
		// try to find the application object
		var obj = docEl;
		while (obj!=null) {
			if (obj.getType () == Packages.helma.doc.DocElement.APPLICATION) {
				var appObj = obj;
				break;
			}
			obj = obj.getParentElement ();
		}
		var protoObj = appObj.getChildElement ("prototype_" + parts[0]);
		if (protoObj==null) {
			// prototype wasn't found, return the unlinked tag
			return tmp + text;
		}
		if (parts.length==1) {
			// no function specified, return the linked prototype
			return '<a href="' + protoObj.href ("main") + '">'  + format (tmp) + '</a>' + text;
		}
		// try to find  a function object:
		var arr = protoObj.listChildren ();
		for (var i=0; i<arr.length; i++) {
			if (arr[i].getName () == parts [1]) {
				return '<a href="' + arr[i].href("main") + '">' + format(tmp) + '</a>' + text;
			}
		}
		// function not found:
		return tmp + text;
	}
}




/**
  * function rendering a comment.
  * @param param.length comment is shortened to the given length.
  * @returns string
  */
function renderComment (docEl, param) {
	var str = docEl.getComment ();
	if (param.length) {
		if (param.length < str.length) {
			return str.substring (0, param.length) + " ...";
		}
	}
	return str;
}

