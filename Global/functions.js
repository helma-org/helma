

/**
  * scheduler function, runs global.appStat every minute
  */
function scheduler() {
	appStat();
	return 60000;
}


/**
  * initializes app.data.stat storage on startup,
  * creates app.data.addressFilter
  */
function onStart() {
	app.data.addressFilter = createAddressFilter();
	app.data.addressString = root.getProperty ("allowadmin");
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
  * updates the stats in app.data.stat every 5 minutes
  */
function appStat ()	{
	if (app.data.stat==null)
		app.data.stat = new HopObject ();
	if ((new Date()-300000) < app.data.stat.lastRun)
		return;
	var arr = root.getApplications ();
	for (var i=0; i<arr.length; i++) {
		var tmp = app.data.stat.get (arr[i].getName());
		if (tmp==null) {
			tmp = new HopObject();
			tmp.lastTotalRequestCount = 0;
			tmp.lastTotalErrorCount   = 0;
      }
		tmp.requestCount = arr[i].getRequestCount () - tmp.lastTotalRequestCount;
		tmp.lastTotalRequestCount = arr[i].getRequestCount ();
		tmp.errorCount   = arr[i].getErrorCount () - tmp.lastTotalErrorCount;
		tmp.lastTotalErrorCount = arr[i].getErrorCount ();
		app.data.stat.set (arr[i].getName(), tmp);
	}
	app.data.stat.lastRun = new Date();
}


/**
  * utility function to sort object-arrays by name
  */
function sortByName(a,b)	{
	if (a.name > b.name)
		return 1;
	else if (a.name == b.name)
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
	var adminAccess = root.getProperty("adminAccess");

	if (adminAccess==null || adminAccess=="")	{
	   res.redirect (root.href ("makekey"));
	}

	var uname = req.username;
	var pwd   = req.password;

	if ( uname==null || uname=="" || pwd==null || pwd=="" )
		return forceAuth();

	var md5key = Packages.helma.util.MD5Encoder.encode(uname + "-" + pwd);

	if (md5key==adminAccess)
		return true;

	if (appObj!=null && appObj.isActive())	{
		// check against application
		var appUsername = appObj.getProperty("adminusername");
		var appPassword = appObj.getProperty("adminpassword");
		if ( md5username==appUsername && md5password==appPassword )
			return true;
	}
	return forceAuth();
}


/**
  * check access to the manage-app by ip-addresses
  */
function checkAddress()	{
	// if allowadmin value in server.properties has changed,
	// re-construct the addressFilter
	if (app.data.addressString != root.getProperty ("allowadmin")){
		app.data.addressFilter = createAddressFilter();
		app.data.addressString = root.getProperty ("allowadmin");
	}
	if ( !app.data.addressFilter.matches(java.net.InetAddress.getByName(req.data.http_remotehost)) )	{
		app.log("denied request from " + req.data.http_remotehost );
		// forceStealth seems a bit like overkill here.
		// display a message that the ip address must be added to server.properties
		res.write ("Access from address "+req.data.http_remotehost+" denied.");
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
  * macro-utility: formatting property lists
  */
function formatProperties(props,par)	{
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
		res.write ( par.itemprefix + arr[i] + par.separator + props.getProperty(arr[i]) + par.itemsuffix );
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


/**
  * tries to make out if this server is running linux from java's system properties
  */
function isLinux () {
   var str = java.lang.System.getProperty("os.name");
   return (str!=null && str.toLowerCase().indexOf("linux")!=-1);
}




