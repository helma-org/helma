

/**
  * scheduler function, runs global.appStat every minute
  */
function scheduler()	{
	appStat();
	return 60000;
}


/**
  * initializes requestStat storage on startup
  */
function onStart()	{
	app.requestStat = new HopObject();
	app.addressFilter = new Packages.helma.util.InetAddressFilter();
	var str = root.getProperty("allowadmin");
	if ( str!=null && str!="" )	{
		var arr = str.split(",");
		for ( var i in arr )	{
			var str = new java.lang.String(arr[i]);
			app.addressFilter.addAddress(str.trim());
		}
	}
}


/** 
  * updates the request stats in app.requestStat every 5 minutes
  */
function appStat()	{
	if ( app.requestStat==null )	{
		app.requestStat = new HopObject();
	}
	if( (new Date()-300000) < app.requestStat.lastRun )	{
		return;
	}
	var arr = root.getApplications();
	for ( var i=0; i<arr.length; i++ )	{
		var tmp = app.requestStat.get(arr[i].getName());
		if ( tmp==null )	{
			tmp = new HopObject();
			tmp.lastTotal = 0;
			tmp.last5Min  = 0;
		}
		var ct = arr[i].getRequestCount();
		tmp.last5Min = ct - tmp.lastTotal;
		tmp.lastTotal = ct;
		app.requestStat.set(arr[i].getName(), tmp);
	}
	app.requestStat.lastRun = new Date();
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
  * @arg application-object
  */
function checkAuth(appObj)	{
	var ok = false;

	// check against root
	var rootUsername = root.getProperty("adminusername");
	var rootPassword = root.getProperty("adminpassword");

	if ( rootUsername==null || rootUsername=="" || rootPassword==null || rootPassword=="" )
		return forceStealth();

	var uname = req.getUsername();
	var pwd   = req.getPassword();

	if ( uname==null || uname=="" || pwd==null || pwd=="" )
		return forceAuth();

	var md5username = calcMD5(uname);
	var md5password = calcMD5(pwd);

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
  * check access to the base-app by ip-addresses
  */
function checkAddress()	{
	if ( !app.addressFilter.matches(java.net.InetAddress.getByName(req.data.http_remotehost)) )
		return forceStealth();
	else
		return true;
}


/**
  * response is reset to 401 / authorization required
  */
function forceAuth(appObj)	{
	res.status = 401;
	res.realm  = (appObj==null) ? "helma" : appObj.name;
	res.reset();
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



