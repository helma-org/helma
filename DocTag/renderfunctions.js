/**
  * function renders list of tags, language is hardcoded here
  * @arg number of current argument (for formatting arg1, arg2 etc)
  * @arg method method-object from which we try to find other prototypes/methods
  * when we're formatting a see tag
  */
function render(argCt,docFunc)	{
	var str = "";
	if ( this.getKind() == this.ARG )	{
		str = "<b>arg" + argCt + ":</b> " + format(this.text);
	}	else if ( this.getKind() == this.PARAM )	{
		str = "<b>Parameter " + this.name;
		if ( this.text!=null && this.text!="" )	{
			str += ":</b> " + format(this.text);
		}
	}	else if ( this.getKind() == this.RETURNS )	{
		str = "<b>Returns:</b> " + format(this.text);
	}	else if ( this.getKind() == this.AUTHOR ) 	{
		str = "<b>by " + format(this.text) + "</b>";
	}	else if ( this.getKind() == this.VERSION ) 	{
		str = "<b>Version " + format(this.text) + "</b>";
	}	else if ( this.getKind() == this.RELEASE ) 	{
		str = "<b>since" + format(this.text) + "</b>";
	}	else if ( this.getKind() == this.SEE ) 	{
		if ( this.text.indexOf("http://")==0 )	{
			str = '<a href="' + this.text + '">' + this.text + '</a>';
		}	else	{
			var tmp = new java.lang.String(this.text);
			tmp = tmp.trim();
			var arr = tmp.split(".");
			var obj = docFunc.getApplication().getDocPrototype(arr[0]);
			if( arr.length>1 && obj.getFunction(arr[1])!=null )	{
				str = '<b>See also: <a href="' + obj.href("main") + '#' + obj.getFunction(arr[1]).name + '">' + format(tmp) + '</a></b>';
			}	else if ( obj!=null )	{
				str = '<b>See also: <a href="' + obj.href("main") + '">' + format(tmp) + '</a></b>';
			}
		}
		if ( str=="" )	{
			str = "<b>See also:</b> " + format(this.text);
		}
	}
	return str + "<br />";
}



