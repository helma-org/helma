
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



function headline_macro (param) {
	res.write (this.getName ());
}

/**
  * macro formatting list of methods of this prototype
  * @param filter actions | functions | macros | templates | skins
  * @param skin skin to apply to the docfunction object
  * @param separator
  * @param desc Description that is passed on to the called skin
  */
function methods_macro (param) {
	var skinname = (param.skin) ? param.skin : "list";
	var separator = (param.separator) ? param.separator : "";
	var arr = this.listChildren ();
	var type = this.translateType (param.filter);	
	var sb = new java.lang.StringBuffer ();
	for (var i=0; i<arr.length; i++) {
		if (arr[i].getType () == type) {
			sb.append (arr[i].renderSkinAsString (skinname, param));
			sb.append (separator);
		}
	}
	var str = sb.toString ();
	if (str.length>0)
		return str.substring (0, str.length - separator.length);
	else
		return str;
}


function inheritance_macro (param) {
	var action = param.action ? param.action : "main";
	var target = param.target ? ('target="' + param.target + '" ') : '';
	var obj = this.getParentPrototype ();
	if (obj!=null) {
		obj = this.inheritanceUtil (obj, param);
	}
	if (param.deep=="true") {
		while (obj!=null) {
			obj = this.inheritanceUtil (obj, param);
		}
	}
}

function inheritanceUtil (obj, param) {
	if (obj.getName ()=="hopobject" && param.hopobject!="true")
		return null;
	var tmp = new Object ();
	for (var i in param)
		tmp[i] = param[i];
	tmp.href = obj.href ((param.action) ? param.action : "main");
	delete tmp.hopobject;
	delete tmp.action;
	delete tmp.deep;
	delete tmp.separator;
	res.write (renderLinkTag (tmp));
	res.write (obj.getName () + "</a>");
	if (obj.getParentPrototype ())
		res.write (param.separator);
	return obj.getParentPrototype ();
}


/**
  * loops through the parent prototypes and renders a skin on each
  * @param skin
  */
function parentPrototype_macro (param) {
	var skinname = (param.skin) ? param.skin : "asParentList";
	var obj = this.getParentPrototype ();
	while (obj!=null) {
		obj.renderSkin (skinname);
		obj = obj.getParentPrototype ();
	}
}


function typeProperties_macro (param) {
	var props = this.getTypeProperties ();
	if (props!=null) {
		res.encode(props.getContent ());
	}
}


