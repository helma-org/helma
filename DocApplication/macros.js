
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



function linkToManage_macro (param) {
   if (res.data.rendering != true) {
      return ('<a href="' + root.href ("main") + '" target="_top">back to manage console</a>');
   }
}

function headline_macro (param) {
	res.write (this.getName ());
}



function hrefRoot_macro (param) {
	var obj = this.getChildElement ("prototype_root");
	if (obj == null) {
	   var obj = this.getChildElement ("prototype_Root");
   }	   
	if (obj!=null)	{
		var action = (param.action) ? param.action : "main";
		return obj.href (action);
	}
}


/**
  * list all prototypes of this application
  * @param skin name of skin to render on prototype
  * @param separator
  */
function prototypes_macro(param)	{
	var skin = (param.skin) ? param.skin : "asPrototypeList";
	var separator = (param.separator) ? param.separator : "";
	var arr = this.listChildren ();
	for ( var i=0; i<arr.length; i++ )	{
		arr[i].renderSkin(skin);
		if (i < arr.length-1)
			res.write (separator);
	}
}


/**
  * list all methods of all prototypes, sort them alphabetically
  * @param skin name of skin to render on each method
  * @param skinSeparator name of skin to render as separator between each letters
  */
function functions_macro(param)	{
	var skinname = (param.skin) ? param.skin : "asListItem";
	var skinIndexSeparator = (param.indexSeparator) ? param.indexSeparator : "indexSeparator";
	var separator = (param.separator) ? param.separator : "";
	var arr = this.listFunctions ();
	var lastLetter = "";
	for (var i=0; i<arr.length; i++) {
		if (arr[i].getName ().substring (0,1)!=lastLetter) {
			lastLetter = arr[i].getName ().substring (0,1);
			var tmp = new Object ();
			tmp.letter = lastLetter.toUpperCase ();
			this.renderSkin (skinIndexSeparator, tmp);
		}
		arr[i].renderSkin (skinname);
		if (i < arr.length-1)
			res.write (separator);
	}
}


