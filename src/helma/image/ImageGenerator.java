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

import helma.main.Server;

import java.awt.*;
import java.awt.image.*;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Factory class for generating Image objects from various sources.
 *  
 */
public abstract class ImageGenerator {
    protected static ImageGenerator generator = null;

    /**
     * @return
     */
    public static ImageGenerator getInstance() {
        if (generator == null) {
            // first see wether an image wrapper class was specified in
            // server.properties:
            String className = Server.getServer().getProperty("imageGenerator");
            Class generatorClass = null;
            if (className == null) {
                // if no class is defined, try the default ones:
                try {
                    Class.forName("com.sun.jimi.core.Jimi");
                    // if we're still here, JimiWrapper can be used
                    className = "helma.image.jimi.JimiGenerator";
                } catch (ClassNotFoundException e1) {
                    try {
                        Class.forName("javax.imageio.ImageIO");
                        // if we're still here, ImageIOWrapper can be used
                        className = "helma.image.imageio.ImageIOGenerator";
                    } catch (ClassNotFoundException e2) {
                    }
                }
            }
            // now let's get the generator class and create an instance:
            try {
                generatorClass = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(
                    "The imageGenerator class cannot be found: " + className);
            }
            try {
                generator = (ImageGenerator)generatorClass.newInstance();
            } catch (Exception e) {
                throw new RuntimeException(
                    "The ImageGenerator instance could not be created: "
                        + className);
            }
        }
        return generator;
    }

    /**
     * @param w ...
     * @param h ...
     * 
     * @return ...
     */
    public ImageWrapper createImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        return new ImageWrapper(img, w, h, this);
    }

    /**
     * @param src ...
     * 
     * @return ...
     * @throws IOException
     */
    public ImageWrapper createImage(byte[] src) throws IOException {
        Image img = read(src);
        return img != null ? new ImageWrapper(img, this) : null;
    }
    
    /**
     * @param filenamne ...
     * 
     * @return ...
     * @throws IOException
     */
    public ImageWrapper createImage(String filenamne)
        throws IOException {
        Image img = read(filenamne);
        return img != null ? new ImageWrapper(img, this) : null;
    }

    /**
     * @param url ...
     * 
     * @return ...
     * @throws MalformedURLException
     * @throws IOException
     */
    public ImageWrapper createImage(URL url)
        throws MalformedURLException, IOException {
        Image img = read(url);
        return img != null ? new ImageWrapper(img, this) : null;
    }

    /**
     * @param iw ...
     * @param filter ...
     * 
     * @return ...
     */
    public ImageWrapper createImage(ImageWrapper iw, ImageFilter filter) {
        // use the ImagFilterOp wrapper for ImageFilters that works directly
        // on BufferedImages. The filtering is much faster like that.
        // Attention: needs testing with all the filters!
        
        return createImage(iw, new ImageFilterOp(filter));
//        Image img = ImageWaiter.waitForImage(
//            Toolkit.getDefaultToolkit().createImage(
//                new FilteredImageSource(iw.getSource(), filter)));
//        return img != null ? new ImageWrapper(img, this) : null;
    }

    /**
     * @param iw ...
     * @param imageOp ...
     * 
     * @return ...
     */
    public ImageWrapper createImage(ImageWrapper iw, BufferedImageOp imageOp) {
        Image img = imageOp.filter(iw.getBufferedImage(), null);
        return img != null ? new ImageWrapper(img, this) : null;
    }

    /**
     * @param file the filename the filename of the image to create
     * 
     * @return the newly created image
     * @throws IOException
     */
    public Image read(String filename) throws IOException {
        return ImageWaiter.waitForImage(
            Toolkit.getDefaultToolkit().createImage(filename)
        );
    }

    /**
     * @param url the URL the filename of the image to create
     * 
     * @return the newly created image
     * @throws IOException
     */
    public Image read(URL url) throws IOException {
        return ImageWaiter.waitForImage(
            Toolkit.getDefaultToolkit().createImage(url)
        );
    }

    /**
     * @param src the data of the image to create
     * 
     * @return the newly created image
     * @throws IOException
     */
    public Image read(byte[] src) throws IOException {
        return ImageWaiter.waitForImage(
            Toolkit.getDefaultToolkit().createImage(src)
        );
    }

    /**
     * Saves the image. Image format is deduced from filename.
     * 
     * @param image
     * @param filename
     * @param quality
     * @param alpha
     * @throws IOException
     */
    public abstract void write(ImageWrapper wrapper, String filename,
        float quality, boolean alpha) throws IOException;
}