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
function href_macro(par)	{
	return this.href( (par&&par.action)?par.action:"main" );
}


/**
  * macro rendering page head
  */
function head_macro(par)	{
	var obj = new Object();
	obj.path = this.getPath();
	this.renderSkin("head",obj);
}


/**
  * utility function for head_macro, rendering link to app
  */
function getPath()	{
	return( '<a href="' + this.href("main")  + '">' + this.name + '</a>' );
}


/**
  * link to the "real" application object (ie not the DocApplication)
  */
function parentlink_macro(par)	{
	var url = getProperty("baseURI");
	url = (url==null || url=="null") ? "" : url;
	url += this.name + "/";
	url += (par&&par.action)?par.action:"main";
	return url;
}


/**
  * list all prototypes of this application
  * @param skin name of skin to render on prototype
  */
function prototypes_macro(par)	{
	var skin = (par && par.skin&&par.skin!="")?par.skin:"appList";
	var arr = this.listPrototypes();
	for ( var i=0; i<arr.length; i++ )	{
		arr[i].renderSkin(skin);
	}
}


/**
  * list all methods of all prototypes, sort and separate them alphabetically
  * @param skin name of skin to render on method
  * @param skinSeparator name of skin to render separator between start-letters
  */
function index_macro(par)	{
	var skin = (par && par.skin && par.skin!="") ? par.skin : "indexList";
	var skinSeparator = (par && par.skinSeparator && par.skinSeparator!="") ? par.skinSeparator : "indexListSeparator";
	var arr = this.listFunctions();
	var lastLetter = '';
	for ( var i=0; i<arr.length; i++ )	{
		if ( arr[i].name.substring(0,1)!=lastLetter )	{
			lastLetter = arr[i].name.substring(0,1);
			var obj = new Object();
			obj.letter = lastLetter.toUpperCase();
			arr[i].renderSkin(skinSeparator,obj);
		}
		arr[i].renderSkin(skin);
	}
}


/** 
  * macro escaping the request-path, used for handing over redirect urls
  */
function requestpath_macro(par)	{
	res.write( escape(req.path) );
}

