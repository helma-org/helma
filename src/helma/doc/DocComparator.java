package helma.doc;

import java.util.Comparator;

public class DocComparator implements Comparator {

	public static final int BY_TYPE = 0;
	public static final int BY_NAME = 1;

	int mode;
	DocElement docEl;

	public DocComparator(int mode, DocElement docEl) {
		this.mode = mode;
		this.docEl = docEl;
	}

	public DocComparator(DocElement docEl) {
		this.mode = 0;
		this.docEl = docEl;
	}

	public int compare(Object obj1, Object obj2) {
		DocElement e1 = (DocElement)obj1;
		DocElement e2 = (DocElement)obj2;
		if (mode==BY_TYPE && e1.getType()>e2.getType())
			return 1;
		else if (mode==BY_TYPE && e1.getType()<e2.getType())
			return -1;
		else {
			return e1.name.compareTo(e2.name);
		}
	}

	public boolean equals(Object obj) {
		DocElement el = (DocElement) obj;
		if (el.name.equals(docEl.name) && el.getType()==docEl.getType()) {
			return true;
		} else {
			return false;
		}
	}

}
