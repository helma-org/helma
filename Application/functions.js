/**
  * construct an application object so that we can use
  * skins for non-active applications too
  * @arg name
  */
function constructor(name)	{
	this.name = name;
}


/**
  * overrides the internal href-function, as
  * helma.framework.core.Application.getNodeHref(Object,String)
  * isn't able to compute correct urls for non-node objects.
  * @arg action of application
  */
function href(action)	{
	var url = getProperty("baseURI");
	url = (url==null || url=="null") ? "" : url;
	url += this.name + "/" + ( (action!=null && action!="") ? action : "main" );
	return url;
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


