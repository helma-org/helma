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
	var appObj = this.getApplication();
	appObj.renderSkin("head",obj);
}


/**
  * utility function for head_macro, rendering link to app and to prototype
  */
function getPath()	{
	var appObj = this.getApplication();
	var str = appObj.getPath();
	str += '/<a href="' + this.href("main") + '">' + this.name + '</a>';
	return( str );
}


/**
  * macro returning the comment for this prototype
  */
function comment_macro(par)	{
	return this.getComment();
}


/**
  * macro formatting list of actions of this prototype
  */
function actions_macro(par)	{
	this.printMethods( Packages.helma.doc.DocElement.ACTION, "listElementAction","Actions" );
}


/**
  * macro formatting list of templates of this prototype
  */
function templates_macro(par)	{
	this.printMethods( Packages.helma.doc.DocElement.TEMPLATE, "listElementTemplate","Templates" );
}


/**
  * macro formatting list of functions of this prototype
  */
function functions_macro(par)	{
	this.printMethods( Packages.helma.doc.DocElement.FUNCTION, "listElementFunction","Functions" );
}


/**
  * macro formatting list of skins of this prototype
  */
function skins_macro(par)	{
	this.printMethods( Packages.helma.doc.DocElement.SKIN, "listElementSkin","Skins" );
}


/**
  * macro formatting list of macros of this prototype
  */
function macros_macro(par)	{
	this.printMethods( Packages.helma.doc.DocElement.MACRO, "listElementMacro","Macros" );
}


/**
  * macro-utility: renders a list of methods of this prototype
  * usage of docprototype.listHeader/listFooter skin is hardcoded
  * @arg type integer - which type of methods are listed
  * @arg skin skin to be called on method
  * @arg desc string describing the type of method (ie "Skins", "Actions")
  */
function printMethods(type,skin,desc)	{
	var arr = this.listFunctions(type);
	if ( arr.length > 0 )	{
		var obj = new Object();
		obj.desc  = desc;
		this.renderSkin("listHeader",obj);
		for ( var i in arr )	{
			arr[i].renderSkin(skin,obj);
		}
		this.renderSkin("listFooter",obj);
	}
}



