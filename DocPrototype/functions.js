
///**
//  * utility function for head_macro, rendering link to app and to prototype
//  */
//function getPath()	{
//	var appObj = this.getParentElement ();
//	var str = appObj.getPath();
//	str += '/<a href="' + this.href("main") + '">' + this.name + '</a>';
//	return( str );
//}



function translateType (filter) {
	if (filter=="actions")
		return Packages.helma.doc.DocElement.ACTION;
	else if (filter=="templates")
		return Packages.helma.doc.DocElement.TEMPLATE;
	else if (filter=="functions")
		return Packages.helma.doc.DocElement.FUNCTION;
	else if (filter=="macros")
		return Packages.helma.doc.DocElement.MACRO;
	else if (filter=="skins")
		return Packages.helma.doc.DocElement.SKIN;
	else
		return -1;
}


function getApplication () {
	return this.getParentElement ();
}


