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
	ImageGenerator imggen) {
	super (img, g, width, height, imggen);
    }


    public void reduceColors (int colors) {
	try {
	    int pixels[][] = getPixels();
	    int palette[] = Quantize.quantizeImage(pixels, colors);
	    int w = pixels.length;
	    int h = pixels[0].length;
	    int pix[] = new int[w * h];
	    // convert to RGB
	    for (int x = w; x-- > 0; ) {
	        for (int y = h; y-- > 0; ) {
	            pix[y * w + x] = palette[pixels[x][y]];
	        }
	    }
	    img = imggen.createImage (new MemoryImageSource(w, h, pix, 0, w));
	} catch (Exception x) {
	    // throw new RuntimeException (x.getMessage ());
	}
    }

    /**
    * Snag the pixels from an image.
     */
    int[][] getPixels () throws IOException {
	int pix[] = new int[width * height];
	PixelGrabber grabber = new PixelGrabber(img, 0, 0, width, height, pix, 0, width);
	try {
	    if (grabber.grabPixels() != true) {
	        throw new IOException("Grabber returned false: " + grabber.status());
	    }
	} catch (InterruptedException e) {
	    e.printStackTrace();
	}
	int pixels[][] = new int[width][height];
	for (int x = width; x-- > 0; ) {
	    for (int y = height; y-- > 0; ) {
	        pixels[x][y] = pix[y * width + x];
	    }
	}
	return pixels;
    }


    public void saveAs (String filename) {
	try {
	    if (filename.toLowerCase().endsWith (".gif")) {
	        // sun's jimi package doesn't encode gifs, use Acme encoder
	        FileOutputStream fout = new FileOutputStream (filename);
	        // Acme gif encoder
	        GifEncoder enc = new GifEncoder (img, fout);
	        enc.encode ();
	        fout.close ();
	    } else {
	        Jimi.putImage (img, filename);
	    }
	} catch (Exception x) {
	    throw new RuntimeException (x.getMessage ());
	}
    }


}
