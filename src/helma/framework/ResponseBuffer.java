// ResponseBuffer.java

package helma.framework;

import java.util.*;
import java.io.Writer;
import java.io.IOException;

public class ResponseBuffer implements ResponsePart {


    int length;
    int size;
    // List list;
    ResponsePart[] parts;
    StringPart stringPart;


    public ResponseBuffer () {
	// list = new ArrayList ();
	length = 0;
	size = 0;
	parts = new ResponsePart[16];
    }

    public ResponseBuffer (int initialCapacity) {
	// list = new ArrayList (initialCapacity);
	length = 0;
	size = 0;
	parts = new ResponsePart[16];
    }

    public void add (String str) {
	if (stringPart == null) {
	    // System.err.println ("Adding STRING_Part "+size+" LENGTH = "+str.length());
	    stringPart = new StringPart (str);
	    if (size == parts.length) {
	        ResponsePart[] newParts = new ResponsePart[parts.length*2];
	        System.arraycopy (parts, 0, newParts, 0, size);
	        parts = newParts;
	    }
	    parts[size] = stringPart;
	    size += 1;
	} else {
	    stringPart.append (str);
	}
	length += str.length();
    }

    public void add (ResponsePart part) {
	// System.err.println ("Adding ResponsePart "+size+" LENGTH = "+part.length());
	if (size == parts.length) {
	    ResponsePart[] newParts = new ResponsePart[parts.length*2];
	    System.arraycopy (parts, 0, newParts, 0, size);
	    parts = newParts;
	}
	parts[size] = part;
	length += part.length();
	size += 1;
	stringPart = null;
    }

    /* public void add (int i, ResponsePart part) {
	list.add (i, part);
	length += part.length();
    } */

    public void clear () {
	for (int i=0; i<size; i++)
	     parts[i] = null;
	length = 0;
	size = 0;
    }

    public void writeTo (Writer writer) throws IOException {
	for (int i=0; i<size; i++) {
	    /* Object o = list.get(i);
	    if (o instanceof ResponsePart)
	        ((ResponsePart) o).writeTo (writer);
	    else if (o != null)
	        writer.write (o.toString()); */
	    parts[i].writeTo (writer);
	}
    }

    public void appendTo (StringBuffer buffer) {
	for (int i=0; i<size; i++) {
	    /* Object o = list.get(i);
	    if (o instanceof ResponsePart)
	        ((ResponsePart) o).appendTo (buffer);
	    else if (o != null)
	        buffer.append (o.toString()); */
	    parts[i].appendTo (buffer);
	}
    }

    public String toString () {
	StringBuffer buffer = new StringBuffer(length);
	appendTo (buffer);
	return buffer.toString ();
    }

    public int length () {
	return length;
    }

    public void setSize (int newSize) {
	if (newSize > size)
	    return;
	for (int i=size; i>newSize; i--) {
	    /* Object o = list.remove (i-1);
	    if (o instanceof ResponsePart)
	        length -= ((ResponsePart) o).length();
	    else if (o instanceof String)
	        length -= ((String) o).length();
	    */
	    length -= parts[i].length();
	    parts[i] = null;
	}
	size = newSize;
    }

    public int size () {
	return size;
    }

}

class StringPart implements ResponsePart {

   char[] chars;
   int count;

   public StringPart (String str) {
	count = str.length();
	chars = new char[count+32];
	str.getChars (0, count, chars, 0);
   }

   public int length() {
	return count;
   }

   public void writeTo (Writer writer) throws IOException {
	writer.write (chars, 0, count);
   }

   public void appendTo (StringBuffer buffer) {
	buffer.append (chars, 0, count);
   }

   public void append (String str) {
	int l = str.length();
	if (count+l > chars.length) {
	    int newLength = count*2;
	    if (newLength < count+l)
	        newLength = count+l;
	    char[] newChars = new char[newLength];
	    System.arraycopy (chars, 0, newChars, 0, count);
	    chars = newChars;
	}
	str.getChars (0, l, chars, count);
	count += l;
   }

   public String toString () {
	return String.valueOf (chars, 0, count);
   }

}

