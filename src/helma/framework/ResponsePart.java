// ResponsePart.java

package helma.framework;

import java.io.Writer;
import java.io.IOException;

public interface ResponsePart {

    /**
     *  Get the number of characters in this response part.
     */
    public int length();

    /**
     *  Write this response part to a character stream.
     */
    public void writeTo (Writer writer) throws IOException;

    /** 
     *  Append this response part to a string buffer.
     */
    public void appendTo (StringBuffer buffer);
}
