// ActivatedImageWrapper.java
// Copyright (c) Hannes Wallnöfer 1999-2000
  
package helma.image;

import java.awt.*;
import java.awt.image.*;
import com.activated.jimi.*;
import com.activated.jimi.util.*;

/** 
 * A wrapper for an image that uses the Activated version of JIMI.
 */
 
public class ActivatedImageWrapper extends ImageWrapper {

    public ActivatedImageWrapper (Image img, Graphics g, int width, int height,
	    ImageGenerator imggen) throws ClassNotFoundException {
	super (img, g, width, height, imggen);
	Class.forName ("com.activated.jimi.Jimi");
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
	    Jimi.putImage (img, filename);
	} catch (JimiException x) {
	    throw new RuntimeException (x.getMessage ());
	}
    }
    

}





