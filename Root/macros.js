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
  * macro returning the total number of sessions on this server
  * @see global.formatCount
  */
function countSessions_macro(par)	{
	var arr = this.getApplications();
	var sum = 0;
	for ( var i=0; i<arr.length; i++ )	{
		if ( arr[i].getName()!=app.__app__.getName() )	{
			sum += arr[i].sessions.size();
		}
	}
	return sum + formatCount(sum,par);
}


/**
  * macro returning the number of requests during the last 5 minutes
  * @see global.formatCount
  */
function requestCount_macro(par)	{
	if (app.data.stat==null)	{
		return;
	}
	var arr = this.getApplications ();
	var sum = 0;
	for (var i=0; i<arr.length; i++) {
		if (arr[i].getName() != app.__app__.getName() ) {  // don't include manage app
			var obj = app.data.stat.get (arr[i].name);
			if ( obj!=null )	{
				sum += obj.requestCount;
			}
		}
	}
	return sum + formatCount (sum,par);
}


/**
  * macro returning the number of errors during the last 5 minutes
  * @see global.formatCount
  */
function errorCount_macro(par)	{
	if (app.data.stat==null)	{
		return;
	}
	var arr = this.getApplications ();
	var sum = 0;
	for (var i=0; i<arr.length; i++) {
		if (arr[i].getName() != app.__app__.getName() ) {  // don't include manage app
			var obj = app.data.stat.get (arr[i].name);
			if ( obj!=null )	{
				sum += obj.errorCount;
			}
		}
	}
	return sum + formatCount (sum,par);
}



function extensions_macro (par) {
	var vec = this.getExtensions ();
	var str = "";
	for (var i=0; i<vec.size(); i++) {
		str += vec.elementAt (i).getClass ().getName ();
		if (i!=(vec.size()-1)) {
			str += (par && par.separator) ? par.separator : ", ";
		}
	}
	return (str=="") ? null : str;
}

/**
  * Macro returning hostname of this machine
  */
function hostname_macro(par)	{
	return java.net.InetAddress.getLocalHost().getHostName()
}


/**
  * Macro returning address of this machine
  */
function hostaddress_macro(par)	{
	return java.net.InetAddress.getLocalHost().getHostAddress()
}


/**
  * Macro returning the number of running applications,
  * the manage application being excluded.
  */
function appCount_macro(par) {
	if ( par && par.filter=="active" )	{
		var ct = root.getApplications().length-1;
	}	else if ( par && par.filter=="disabled" )	{
		var ct = root.getAllApplications().length - root.getApplications().length;
	}	else	{
		var ct = root.getAllApplications().length-1;
	}
	return ct + formatCount(ct,par);
}


/**
  * Macro that lists all running applications,
  * the manage application being excluded (-1).
  * @param skin skin of application that will be used.
  */
function appList_macro(par) {
	var skin = (par && par.skin) ? par.skin : "navig_active";
	var apps = new Array();
	if ( par && par.filter=="active" )	{
		var arr = root.getApplications();
		for ( var i=0; i<arr.length; i++ )	{
			apps[apps.length] = arr[i];
		}
	}	else if ( par && par.filter=="disabled" )	{
		var arr = root.getAllApplications();
		for ( var i in arr )	{
			if( arr[i].isActive()==false )	{
				apps[apps.length] = arr[i];
			}
		}
	}	else	{
		var apps = root.getAllApplications();
	}
	apps = apps.sort(sortByName);
	var html = "";
	var param = new Object();
	for (var n in apps) {
		var a = apps[n];
		if ( apps[n].name==app.__app__.getName() )
			continue;
		var item = a.renderSkinAsString(skin);
		html += item;
	}
	return(html);
}


/**
  * Macro that returns the server's uptime nicely formatted
  */
function uptime_macro()	{
	return formatAge( (java.lang.System.currentTimeMillis()-this.starttime) / 1000 );
}


/** 
  * Macro that returns the server's version string
  */
function version_macro()	{
	return this.version;
}


/**
  * Macro that returns the home directory of the hop installation
  */
function home_macro()	{
	return this.getHopHome().toString();
}


/**
  * Macro that returns the free memory in the java virtual machine
  * @param format if "hr", value will be printed human readable
  */
function jvmFreeMemory_macro (param)	{
	var m = java.lang.Runtime.getRuntime ().freeMemory ();
	return (param && param.hr) ? formatBytes (m) : m;
}


/**
  * Macro that returns the total memory available to the java virtual machine
  * @param format if "hr", value will be printed human readable
  */
function jvmTotalMemory_macro (param)	{
	var m = java.lang.Runtime.getRuntime().totalMemory();
	return (param && param.hr) ? formatBytes (m) : m;
}


/**
  * Macro that returns the used memory in the java virtual machine
  * @param format if "hr", value will be printed human readable
  */
function jvmUsedMemory_macro(param)	{
	var m = java.lang.Runtime.getRuntime ().totalMemory () - java.lang.Runtime.getRuntime ().freeMemory ();
	return (param && param.hr) ? formatBytes (m) : m;
}


/**
  * Macro that returns the version and type of the java virtual machine
  */
function jvm_macro()	{
	return java.lang.System.getProperty("java.version") + " " + java.lang.System.getProperty("java.vendor");
}


/**
  * Macro that returns the home directory of the java virtual machine
  */
function jvmHome_macro()	{
	return java.lang.System.getProperty("java.home");
}


/**
  * Macro that greps all jar-files from the class path variable and lists them.
  * @param separator string that is printed between the items
  */
function jvmJars_macro(par)	{
	var separator = (par && par.separator ) ? par.separator : ", ";
	var str = java.lang.System.getProperty("java.class.path");
	var arr = str.split(".jar");
	for ( var i in arr )	{
		var str = arr[i];
		var pos = ( str.lastIndexOf('\\') > str.lastIndexOf('/') ) ? str.lastIndexOf('\\') : str.lastIndexOf('/');
		res.write ( arr[i].substring(pos+1) + ".jar" );
		if ( i < arr.length-1 )	res.write ( separator );
	}
}


/**
  * Macro that returns the name and version of the server's os
  */
function os_macro()	{
	return java.lang.System.getProperty("os.name") + " " + java.lang.System.getProperty("os.arch") + " " + java.lang.System.getProperty("os.version");
}


/**
  * Macro that returns anything from server.properties
  */
function property_macro(par)	{
	if ( par && par.key )	{
		return this.getProperty(key);
	}	else	{
		return "";
	}
}


/**
  * Macro formatting server.properties
  */
function properties_macro(par)	{
	formatProperties(this.getProperties(),par);
}


/**
  * Macro that returns the timezone of this server
  */
function timezone_macro(par)	{
	return java.util.TimeZone.getDefault().getDisplayName(false, java.util.TimeZone.LONG) + " (" + java.util.TimeZone.getDefault().getID() + ")";
}


