

/**
  * scheduler function, runs global.appStat every minute
  */
function scheduler() {
	appStat();
	return 60000;
}


/**
  * initializes app.data.requestStat storage on startup,
  * creates app.data.addressFilter
  */
function onStart() {
	app.data.requestStat = new HopObject();
	app.data.addressFilter = createAddressFilter();
}

/**
  * initializes addressFilter from app.properties,
  * hostnames are converted, wildcards are only allowed in ip-addresses
  * (so, no network-names, sorry)
  */
function createAddressFilter()	{
	var filter = new Packages.helma.util.InetAddressFilter();
	var str = root.getProperty("allowadmin");
	if ( str!=null && str!="" )	{
		var arr = str.split(",");
		for ( var i in arr )	{
			var str = new java.lang.String(arr[i]);
			var result = tryEval("filter.addAddress(str.trim());");
			if ( result.error!=null )	{
				var str = java.net.InetAddress.getByName(str.trim()).getHostAddress();
				var result = tryEval("filter.addAddress(str);");
			}
			if ( result.error==null )	{
				app.log( "allowed address for app manage: " + str );
			}
		}
	}	else	{
		app.log("no addresses allowed for app manage, all access will be denied");
	}
	return filter;
}


/** 
  * updates the request stats in app.data.requestStat every 5 minutes
  */
function appStat()	{
	if ( app.data.requestStat==null )	{
		app.data.requestStat = new HopObject();
	}
	if( (new Date()-300000) < app.data.requestStat.lastRun )	{
		return;
	}
	var arr = root.getApplications();
	for ( var i=0; i<arr.length; i++ )	{
		var tmp = app.data.requestStat.get(arr[i].getName());
		if ( tmp==null )	{
			tmp = new HopObject();
			tmp.lastTotal = 0;
			tmp.last5Min  = 0;
		}
		var ct = arr[i].getRequestCount();
		tmp.last5Min = ct - tmp.lastTotal;
		tmp.lastTotal = ct;
		app.data.requestStat.set(arr[i].getName(), tmp);
	}
	app.data.requestStat.lastRun = new Date();
}


/**
  * utility function to sort object-arrays by name
  */
function sortByName(a,b)	{
	if ( a.name>b.name )
		return 1;
	else if ( a.name==b.name )
		return 0;
	else
		return -1;
}


/**
  * utility function to sort property-arrays by key
  */
function sortProps(a,b)	{
	if ( a>b )
		return 1;
	else if ( a==b )
		return 0;
	else
		return -1;
}

/**
  * check access to an application or the whole server, authenticate against md5-encrypted
  * properties of base-app or the particular application. if username or password aren't set
  * go into stealth-mode and return a 404. if username|password are wrong, prepare response-
  * object for http-auth and return false.
  * @arg appObj application object to check against (if adminUsername etc are set in app.properties)
  */
function checkAuth(appObj)	{
	var ok = false;

	// check against root
	var rootUsername = root.getProperty("adminusername");
	var rootPassword = root.getProperty("adminpassword");

	if ( rootUsername==null || rootUsername=="" || rootPassword==null || rootPassword=="" )	{
		return createAuth();
	}

	var uname = req.username;
	var pwd   = req.password;

	if ( uname==null || uname=="" || pwd==null || pwd=="" )
		return forceAuth();

	var md5username = Packages.helma.util.MD5Encoder.encode(uname);
	var md5password = Packages.helma.util.MD5Encoder.encode(pwd);

	if ( md5username==rootUsername && md5password==rootPassword )
		return true;

	if ( appObj!=null && appObj.isActive() )	{
		// check against application
		var appUsername = appObj.getProperty("adminusername");
		var appPassword = appObj.getProperty("adminpassword");
		if ( appUsername==null || appUsername=="" || appPassword==null || appPassword=="" )
			return forceStealth();
		if ( md5username==appUsername && md5password==appPassword )
			return true;
	}
	return forceAuth();
}


/**
  * check access to the manage-app by ip-addresses
  */
function checkAddress()	{
	if ( !app.data.addressFilter.matches(java.net.InetAddress.getByName(req.data.http_remotehost)) )	{
		app.log("denied request from " + req.data.http_remotehost );
		// forceStealth seems a bit like overkill here.
		// display a message that the ip address must be added to server.properties
		res.write ("Access to address "+req.data.http_remotehost+" denied.");
		return false;
	}	else	{
		return true;
	}
}


/**
  * response is reset to 401 / authorization required
  * @arg realm realm for http-auth
  */
function forceAuth(realm)	{
	res.reset();
	res.status = 401;
	res.realm = (realm!=null) ? realm : "helma";
	res.write ("Authorization Required. The server could not verify that you are authorized to access the requested page.");
	return false;
}

/**
  * response is reset to 404 / notfound
  */
function forceStealth()	{
	res.reset();
	res.status = 404;
	return false;
}


/** 
  * response is either a html form to enter auth data or input from
  * html form is saved to server.properties
  * access is only allowed if remote host is in the list of friendly
  * ip-adresses in server.properties
  */
function createAuth()	{
	if ( checkAddress()!=true )	{
		// double check
		return false;
	}
	var obj = new Object();
	obj.msg = "";
	
	if ( req.data.username!=null && req.data.password!=null && req.data.password2!=null )	{
		// we have input from webform
		if ( req.data.username=="" )
			obj.msg += "username can't be left empty!<br>";
		if ( req.data.password=="" )
			obj.msg += "password can't be left empty!<br>";
		else if ( req.data.password!=req.data.password2 )
			obj.msg += "password and re-typed password don't match!<br>";
		if ( obj.msg!="" )	{
			obj.username = req.data.username;
			res.reset();
			renderSkin("pwdform",obj);
			return false;
		}
		var f = new File(root.getHopHome().toString, "server.properties");
		var str = f.readAll();
		var sep = java.lang.System.getProperty("line.separator");
		str += sep + "adminUsername=" + Packages.helma.util.MD5Encoder.encode(req.data.username) + sep;
		str += "adminPassword=" + Packages.helma.util.MD5Encoder.encode(req.data.password) + sep;
		f.remove();
		f.open();
		f.write(str);
		f.close();
		app.log( req.data.http_remotehost + " saved new adminUsername/adminPassword to server.properties");
		res.redirect ( root.href("main") );
	
	}	else	{
		// no input from webform, so print it
		res.reset();
		res.data.title = "username & password on " + root.hostname_macro();
		res.data.head = renderSkinAsString("head");
		res.data.body = renderSkinAsString("pwdform",obj);
		renderSkin("basic");
		return false;
	}
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
