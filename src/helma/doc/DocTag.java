package helma.doc;

import java.util.*;

public final class DocTag	{

	// for public use we have less types than
	// internally. eg, we're combining "return" and "returns"
	// or "arg" and "param".
	// those that aren't combined, need to match the index of
	// the tags-array!
	public static final int PARAMETER	= 0;
	public static final int RETURN		= 2;
	public static final int AUTHOR		= 4;
	public static final int VERSION		= 5;
	public static final int SEE			= 6;

	public static final String[][] tags = {
		{"@arg","Argument"},
		{"@param","Parameter"},
		{"@return","Returns"},
		{"@returns","Returns"},
		{"@author","Author"},
		{"@version","Version"},
		{"@see","See also"}
	};

	private String	name;
	// kind is for internal use, type is external
	private int		kind; 
	private String	text;

	public static boolean isTagStart (String rawLine) {
		rawLine = rawLine.trim ();
		for (int i=0; i<tags.length; i++) {
			if (rawLine.startsWith (tags[i][0])) {
				return true;
			}
		}
		return false;
	}

	public static DocTag parse (String rawTag) throws DocException {
		rawTag = rawTag.trim ();
		int kind = -1;
		for (int i=0; i<tags.length; i++) {
			if (rawTag.startsWith (tags[i][0])) {
				kind = i;
				break;
			}
		}
		if (kind == -1)
			throw new DocException ("unsupported tag type: " + rawTag);
		String content = rawTag.substring (tags[kind][0].length ()).trim ();
		if (kind == 0 || kind==1) {
			StringTokenizer tok = new StringTokenizer (content);
			String name = "";
			if (tok.hasMoreTokens ())
				name = tok.nextToken ();
			return new DocTag (kind, name, content.substring (name.length ()).trim ());
		} else {
			return new DocTag (kind, "", content);
		}
	}

	private DocTag (int kind, String name, String text) {
		this.kind = kind;
		this.name = (name!=null) ? name : "";
		this.text = (text!=null) ? text : "";
	}

	public String getName ()	{
		return name;
	}

	public int getType ()	{
		if (kind==0 || kind==1)
			return PARAMETER;
		else if (kind==2 || kind==3)
			return RETURN;
		else
			return kind;
	}


	public String getTag () {
		return tags[kind][0];
	}

	public String getText ()	{
		return text;
	}

	public String toString()	{
		return tags [kind][1] + ": " + name + " " + text;
	}

}

