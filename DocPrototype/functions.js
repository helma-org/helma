
/**
  * overrides the internal href-function, as
  * helma.framework.core.Application.getNodeHref(Object,String)
  * isn't able to compute correct urls for non-node objects.
  * @arg action of prototype
  */
function href(action)	{
	var url = getProperty("baseURI");
	url = (url==null || url=="null") ? "" : url;
	url += this.getParentElement().getName() + "/api/" + this.name + "/" + ( (action!=null && action!="") ? action : "main" );
	return url;
}

