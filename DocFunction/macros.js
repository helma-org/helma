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
  * macro returning name of file this method resides in
  */
function location_macro(par)	{
	var f = new File ( this.getLocation() );
	return f.getName();
}


/**
  * macro returning the type of this method (Action, Template, Skin, Macro, Function)
  */
function typename_macro(par)	{
	return this.getTypeName();
}


/**
  * macro returning a link to the prototype page
  * @param action name of action to call on this prototype, default main
  */
function prototypehref_macro(par)	{
	return this.getDocPrototype().href( (par&&par.action)?par.action:"main" )
}


/**
  * macro returning the name of the prototype this method belongs to
  */
function prototypename_macro(par)	{
	return this.getDocPrototype().getName();
}


/**
  * macro returning the comment text of this method
  * (excluding the tags!)
  * @param size (optional) text is cutoff after a number of chars
  */
function comment_macro(par)	{
	var str = this.getComment();
	if ( par && par.length && str.length > par.size )	{
		return ( str.substring(0,par.size) );
	}	else	{
		return ( str );
	}
}


/**
  * macro rendering the list of tags
  */
function tags_macro()	{
	var arr = this.listTags();
	var argCt = 0;
	for ( var i in arr )	{
		if ( arr[i].getKind()==Packages.helma.doc.DocTag.ARG )
			argCt++;
		res.write( arr[i].render(argCt,this) );
	}
}


/**
  * macro rendering sequence of arg1, arg2 etc
  * according to number of arguments in doctags.
  */
function args_macro()	{
	var ct = this.countTags(Packages.helma.doc.DocTag.ARG);
	for ( var i=0; i<ct; i++)	{
		res.write ( " arg" + (i+1) );
		if ( i<(ct-1) )	res.write (",");
		else	res.write (" ");
	}
}


/**
  * macro returning nicely formatted sourcecode of this method.
  * code is encoded, &gt% %&lt;-tags are colorcoded, line numbers are added
  */
function source_macro(par)	{
	var str = this.getSource();
	var arr = str.split("<%");
	var str2 = "";
	for ( var i=0; i<arr.length; i++ )	{
		str2 += encode(arr[i]);
		if ( i<arr.length-1 )
			str2 += '<font color="#aa3300">&lt;%';
	}
	var arr = str2.split("%&gt;");
	var str3 = "";
	for ( var i=0; i<arr.length; i++ )	{
		str3 += arr[i];
		if ( i<arr.length-1 )
			str3 += '%&gt;</font>';
	}
	var arr = str3.split("<br>");
	var str4 = "";
	for ( var i=0; i<arr.length; i++ )	{
		str4 += '<font color="#aaaaaa">' + (i+1) + ':</font> '
		if ( i<100 )	str4+=' ';
		str4 += arr[i] + "<br>";
	}
	return ( str4 );
}


/**
  * macro returning the fullname of this method
  */
function fullname_macro(par)	{
	return this.getFullName();
}


