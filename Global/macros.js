/**
  * macro rendering a skin
  * @param name name of skin
  */
function skin_macro(par)	{
	if ( par && par.name )	{
		renderSkin(par.name);
	}
}


/**
  * Macro returning the actual date and time.
  */
function now_macro() {
	var date = new Date();
	return(date.format("dd.MM.yyyy, HH:mm'h' zzz"));
}


/**
  * macro-utility: formatting property lists
  */
function formatProperties(props,par)	{
	var prefix = (par && par.prefix) ? par.prefix : "";
	var suffix = (par && par.suffix) ? par.suffix : "";
	var separator = (par && par.separator) ? par.separator : "";
	if ( props.size()==0 )
		return "";
	var e = props.keys();
	var arr = new Array();
	while ( e.hasMoreElements() )	{
		arr[arr.length] = e.nextElement();
	}	
	arr.sort(sortProps);
	for ( var i in arr )	{
		// don't print the admin-password
		if ( arr[i].toLowerCase()=="adminusername" || arr[i].toLowerCase()=="adminpassword" )	continue;
		res.write ( prefix + arr[i] + separator + props.getProperty(arr[i]) + suffix );
	}
}


/**
  * macro-utility: formatting an integer value as human readable bytes
  */
function formatBytes(bytes)	{
	if ( bytes > Math.pow(2,30) )	{
		res.write( Math.round( 100*bytes/Math.pow(2,30) ) / 100 + "gb" );
	}	else if ( bytes > Math.pow(2,20) )	{
		res.write( Math.round( 100*bytes/Math.pow(2,20) ) / 100 + "mb" );
	}	else {
		res.write( Math.round( 100*bytes/Math.pow(2,10) ) / 100 + "kb" );
	}
}


/**
  * macro-utility: formatting time in millis as human readable age
  */
function formatAge(age)	{
	var str = "";
	var days = Math.floor(age/86400);
	var age = age - days * 86400;
	var hours = Math.floor(age / 3600);
	var age = age - hours * 3600;
	var minutes = Math.floor(age / 60);
	var seconds = Math.floor(age - minutes * 60);
	if (days > 0)
		str += (days + " days, ");
	if (hours>0)
		str += (hours + "h, ");
	str += (minutes + "min");
	if (days == 0)	str += (", " + seconds + "sec");
	return(str);
}


/**
  * macro-utility: formatting a number-suffix by choosing between singular and plural
  * @arg value number to be formatted
  * @arg param-object object to get fields
  * @param singular string used for value==1
  * @param plural string used for value!=1
  */
function formatCount(ct, par)	{
	if ( !par || !par.singular || !par.plural )	{
		return "";
	}
	if ( ct==1 )
		return par.singular;
	else
		return par.plural;
}



