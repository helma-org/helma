package helma.doc;

public class DocException extends RuntimeException {

    String str;

    public DocException (String str) {
		super (str);
    	this.str = str;
    }
    
    public String getMessage() {
    	return str;
    }
    
}
