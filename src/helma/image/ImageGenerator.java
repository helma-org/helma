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

import java.awt.*;
import java.awt.image.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Factory class for generating Image objects from various sources.
 *
 */
public class ImageGenerator {
    /**
     * Creates a new ImageGenerator object.
     */
    public ImageGenerator() {
        // nothing to do
    }

    /**
     *
     *
     * @param w ...
     * @param h ...
     *
     * @return ...
     */
    public ImageWrapper createPaintableImage(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics g = img.getGraphics();
        ImageWrapper rimg = new SunImageWrapper(img, g, w, h, this);

        return rimg;
    }

    /**
     *
     *
     * @param src ...
     *
     * @return ...
     */
    public ImageWrapper createPaintableImage(byte[] src) {
        ImageWrapper rimg = null;
        Image img1 = Toolkit.getDefaultToolkit().createImage(src);
        ImageLoader loader = new ImageLoader(img1);

        try {
            loader.getDimensions();

            int w = loader.getWidth();
            int h = loader.getHeight();
            Image img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();

            g.drawImage(img1, 0, 0, null);
            rimg = new SunImageWrapper(img, g, w, h, this);
        } finally {
            loader.done();
        }

        return rimg;
    }

    /**
     *
     *
     * @param src ...
     *
     * @return ...
     */
    public ImageWrapper createImage(byte[] src) {
        ImageWrapper rimg = null;
        Image img = Toolkit.getDefaultToolkit().createImage(src);
        ImageLoader loader = new ImageLoader(img);

        try {
            loader.getDimensions();

            int w = loader.getWidth();
            int h = loader.getHeight();

            rimg = new SunImageWrapper(img, null, w, h, this);
        } finally {
            loader.done();
        }

        return rimg;
    }

    /**
     *
     *
     * @param urlstring ...
     *
     * @return ...
     *
     * @throws MalformedURLException ...
     */
    public ImageWrapper createPaintableImage(String urlstring)
                                      throws MalformedURLException {
        ImageWrapper rimg = null;
        URL url = new URL(urlstring);
        Image img1 = Toolkit.getDefaultToolkit().createImage(url);
        ImageLoader loader = new ImageLoader(img1);

        try {
            loader.getDimensions();

            int w = loader.getWidth();
            int h = loader.getHeight();
            Image img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();

            g.drawImage(img1, 0, 0, null);
            rimg = new SunImageWrapper(img, g, w, h, this);
        } finally {
            loader.done();
        }

        return rimg;
    }

    /**
     *
     *
     * @param iw ...
     * @param filter ...
     *
     * @return ...
     */
    public ImageWrapper createPaintableImage(ImageWrapper iw, ImageFilter filter) {
        ImageWrapper rimg = null;
        FilteredImageSource fis = new FilteredImageSource(iw.getSource(), filter);
        Image img1 = Toolkit.getDefaultToolkit().createImage(fis);
        ImageLoader loader = new ImageLoader(img1);

        try {
            loader.getDimensions();

            int w = loader.getWidth();
            int h = loader.getHeight();
            Image img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics g = img.getGraphics();

            g.drawImage(img1, 0, 0, null);
            rimg = new SunImageWrapper(img, g, w, h, this);
        } finally {
            loader.done();
        }

        return rimg;
    }

    /**
     *
     *
     * @param filename ...
     *
     * @return ...
     */
    public Image createImage(String filename) {
        Image img = null;

        img = Toolkit.getDefaultToolkit().createImage(filename);

        ImageLoader loader = new ImageLoader(img);

        loader.getDimensions();
        loader.done();

        return img;
    }

    /**
     *
     *
     * @param producer ...
     *
     * @return ...
     */
    public Image createImage(ImageProducer producer) {
        Image img = null;

        img = Toolkit.getDefaultToolkit().createImage(producer);

        ImageLoader loader = new ImageLoader(img);

        loader.getDimensions();
        loader.done();

        return img;
    }

    class ImageLoader implements ImageObserver {
        Image img;
        int w;
        int h;
        boolean waiting;
        boolean firstFrameLoaded;

        ImageLoader(Image img) {
            this.img = img;
            waiting = true;
            firstFrameLoaded = false;
        }

        synchronized void getDimensions() {
            w = img.getWidth(this);
            h = img.getHeight(this);

            if ((w == -1) || (h == -1)) {
                try {
                    wait(45000);
                } catch (InterruptedException x) {
                    waiting = false;

                    return;
                } finally {
                    waiting = false;
                }
            }

            // if width and height haven't been set, throw tantrum
            if ((w == -1) || (h == -1)) {
                throw new RuntimeException("Error loading image");
            }
        }

        synchronized void done() {
            waiting = false;
            notifyAll();
        }

        int getWidth() {
            return w;
        }

        int getHeight() {
            return h;
        }

        public synchronized boolean imageUpdate(Image img, int infoflags, int x, int y,
                                                int width, int height) {
            // check if there was an error
            if (!waiting || ((infoflags & ERROR) > 0) || ((infoflags & ABORT) > 0)) {
                // we either timed out or there was an error.
                notifyAll();

                return false;
            }

            if (((infoflags & WIDTH) > 0) || ((infoflags & HEIGHT) > 0)) {
                if ((infoflags & WIDTH) > 0) {
                    w = width;
                }

                if ((infoflags & HEIGHT) > 0) {
                    h = height;
                }

                if ((w > -1) && (h > -1) && firstFrameLoaded) {
                    notifyAll();

                    return false;
                }
            }

            if (((infoflags & ALLBITS) > 0) || ((infoflags & FRAMEBITS) > 0)) {
                firstFrameLoaded = true;
                notifyAll();

                return false;
            }

            return true;
        }
    }
}
