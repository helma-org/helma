// ActivatedImageWrapper.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.image;

import java.awt.*;
import java.awt.image.*;
import com.sun.jimi.core.*;
import com.sun.jimi.core.util.*;
import Acme.JPM.Encoders.GifEncoder;
import java.io.IOException;
import java.io.FileOutputStream;

/** 
 * A wrapper for an image that uses the Sun version of JIMI available at
 * http://java.sun.com/products/jimi.
 */
 
public class SunImageWrapper extends ImageWrapper {

    public SunImageWrapper (Image img, Graphics g, int width, int height,
	    ImageGenerator imggen) throws ClassNotFoundException {
	super (img, g, width, height, imggen);
	Class.forName ("com.sun.jimi.core.Jimi");
    }


    public void reduceColors (int colors) {
	try {
	    ColorReducer redux = new ColorReducer (colors, true);	
	    img = redux.getColorReducedImage (img);
	} catch (Exception x) {
	    throw new RuntimeException (x.getMessage ());
	}
    }

    public void saveAs (String filename) {
    	try {
	    if (filename.toLowerCase().endsWith (".gif")) {
	        // sun's jimi package doesn't encode gifs, use Acme encoder
	        FileOutputStream fout = new FileOutputStream (filename);
	        GifEncoder enc = new GifEncoder (img, fout);
	        enc.encode ();
	        fout.close ();
	    } else {
	        Jimi.putImage (img, filename);
	    }
	} catch (JimiException x) {
	    throw new RuntimeException (x.getMessage ());
	}  catch (IOException iox) {
	    throw new RuntimeException (iox.getMessage ());
	}
    }


}
