
/**
  * construct an application object so that we can use
  * skins for non-active applications too
  * @arg name
  */
function constructor(name)	{
	this.name = name;
}


/**
  * of no use, just to avoid error message
  */
function onRequest () {
}


/**
  * return true/false to determine if application is running
  */
function isActive()	{
	if ( root.getApplication(this.name)==null )
		return false;
	else
		return true;
}


