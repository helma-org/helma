/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 *
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

package helma.image;

import Acme.JPM.Encoders.GifEncoder;
import com.sun.jimi.core.*;
import com.sun.jimi.core.util.*;
import java.awt.*;
import java.awt.image.*;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * A wrapper for an image that uses the Sun version of JIMI available at
 * http://java.sun.com/products/jimi.
 */
public class SunImageWrapper extends ImageWrapper {
    /**
     * Creates a new SunImageWrapper object.
     *
     * @param img ...
     * @param g ...
     * @param width ...
     * @param height ...
     * @param imggen ...
     */
    public SunImageWrapper(Image img, Graphics g, int width, int height,
                           ImageGenerator imggen) {
        super(img, g, width, height, imggen);
    }

    /**
     * Reduce the colors used in this image. Useful and necessary before saving
     * the image as GIF file.
     *
     * @param colors the number of colors to use, usually <= 256.
     */
    public void reduceColors(int colors) {
        try {
            // first, try to use JIMI's ColorReducer class. It is able to
            // preserve transparency on GIF files, but does throw exceptions on some GIFs.
            img = new ColorReducer(colors, false).getColorReducedImage(img);
        } catch (Exception excpt) {
            // JIMI sometimes fails to reduce colors, throwing an exception.
            // Use our alternative Quantizer in this case.
            System.err.println("Using alternative color reducer ("+excpt+")");
            try {
                int[][] pixels = getPixels();
                int[] palette = Quantize.quantizeImage(pixels, colors);
                int w = pixels.length;
                int h = pixels[0].length;
                int[] pix = new int[w * h];

                // convert to RGB
                for (int x = w; x-- > 0;) {
                    for (int y = h; y-- > 0;) {
                        pix[(y * w) + x] = palette[pixels[x][y]];
                    }
                }

                img = imggen.createImage(new MemoryImageSource(w, h, pix, 0, w));
            } catch (IOException ioxcpt) {
                System.err.println("Error in reduceColors(): "+ioxcpt);
            }
        }
    }

    /**
     * Snag the pixels from an image.
     */
    int[][] getPixels() throws IOException {
        int[] pix = new int[width * height];
        PixelGrabber grabber = new PixelGrabber(img, 0, 0, width, height, pix, 0, width);

        try {
            if (grabber.grabPixels() != true) {
                throw new IOException("Grabber returned false: " + grabber.status());
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        int[][] pixels = new int[width][height];

        for (int x = width; x-- > 0;) {
            for (int y = height; y-- > 0;) {
                pixels[x][y] = pix[(y * width) + x];
            }
        }

        return pixels;
    }

    /**
     *
     *
     * @param filename ...
     */
    public void saveAs(String filename) {
        try {
            if (filename.toLowerCase().endsWith(".gif")) {
                // sun's jimi package doesn't encode gifs, use Acme encoder
                FileOutputStream fout = new FileOutputStream(filename);

                // Acme gif encoder
                GifEncoder enc = new GifEncoder(img, fout);

                enc.encode();
                fout.close();
            } else {
                Jimi.putImage(img, filename);
            }
        } catch (Exception x) {
            throw new RuntimeException(x.getMessage());
        }
    }
}
