
/**
  * macro rendering a skin
  * @param name name of skin
  */
function skin_macro(par)	{
	if ( par && par.name )	{
		this.renderSkin(par.name);
	}
}

/**
  * macro-wrapper for href-function
  * @param action name of action to call on this prototype, default main
  */
function href_macro(param)			{	return this.href ((param && param.action) ? param.action : "main");	}

function comment_macro (param)	{	return renderComment (this, param);		}
function content_macro (param)	{	return this.getContent ();					}
function tags_macro (param) 		{	return renderTags (this, param);			}
function location_macro (param)	{	return renderLocation (this, param);	}
function link_macro (param) 		{	return renderLink (this, param);			}

//// END OF COPIED FUNCTIONS



function headline_macro (param)	{
	var p = this.getParentElement ();
	var handler = (p!=null) ? p.getName () : "";
	if (this.getType () == this.ACTION) {
		res.write ("/" + this.getName ());
	} else if (this.getType () == this.FUNCTION) {
		if (handler!="" && handler!="global")
			res.write (handler + ".");
		res.write (this.getName () + "&nbsp;(");
		var arr = this.listParameters ();
		for (var i=0; i<arr.length; i++) {
			res.write (arr[i]);
			if (i<arr.length-1) {
				res.write (",&nbsp");
			}
		}
		res.write (")");
	} else if (this.getType () == this.MACRO) {
		res.write ("&lt;%&nbsp;");
		if (handler!="" && handler!="global")
			res.write (handler + ".");
		var name = this.getName ();
		if (name.indexOf("_macro")>-1)
			name = name.substring (0, name.length-6);
		res.write (name);
		res.write ("&nbsp;%&gt;");
	}	else if (this.getType () == this.SKIN) {
		if (handler!="" && handler!="global")
			res.write (handler + "/");
		res.write (this.getName ());
		res.write (".skin");
	}
}


function skinparameters_macro (param) {
	if (this.getType () == this.SKIN) {
		this.parameters_macro (param);
	}
}


function parameters_macro (param) {
	var separator = (param.separator) ? param.separator : ", ";
	var arr = this.listParameters ();
	for (var i=0; i<arr.length ;i++) {
		res.write (arr[i]);
		if (i<arr.length-1)
			res.write (separator);
	}
}


function type_macro (param) {
	return this.getTypeName ();
}


/**
  * macro returning nicely formatted sourcecode of this method.
  * code is encoded, &gt% %&lt;-tags are colorcoded, line numbers are added
  */
function source_macro(param) {
    var sourcecode = this.getContent();
    if (param.as=="highlighted") {

        sourcecode = encode(sourcecode);

        // highlight macro tags
        r = new RegExp("&lt;%","gim");
        sourcecode = sourcecode.replace(r, '<font color="#aa3300">&lt;%');

        r = new RegExp("%&gt;","gim");
        sourcecode = sourcecode.replace(r, '%&gt;</font>');

        // highlight js-comments
        r = new RegExp("^([ \\t]*//.*)", "gm");
        sourcecode = sourcecode.replace(r, '<font color="#33aa00">$1</font>');

        // highlight quotation marks, but not for skins
        if (this.getTypeName() != "Skin") {
            r = new RegExp("(&quot;.*?&quot;)", "gm");
            sourcecode = sourcecode.replace(r, '<font color="#9999aa">$1</font>');
            r = new RegExp("(\'[\']*\')", "gm");
            sourcecode = sourcecode.replace(r, '<font color="#9999aa">$1</font>');
        }

        // remove all CR and LF, just <br> remains
        var r = new RegExp("[\\r\\n]","gm");
        sourcecode = sourcecode.replace(r, "");

	    var arr = sourcecode.split("<br />");
	    for (var i=0; i<arr.length; i++) {
		    res.write('<font color="#aaaaaa">' + (i+1) + ':</font> ');
		    if (i<99) {
		        res.write(' ');
		    }
		    if (i<9) {
		        res.write(' ');
		    }
    		res.write(arr[i] + "\n");
    	}

	} else {
	    res.write(sourcecode);
	}
}



