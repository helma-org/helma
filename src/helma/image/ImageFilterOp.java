/*
 * Helma License Notice
 * 
 * The contents of this file are subject to the Helma License Version 2.0 (the
 * "License"). You may not use this file except in compliance with the License.
 * A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 * 
 * Copyright 1998-2003 Helma Software. All Rights Reserved.
 * 
 * $RCSfile$
 * $Author$
 * $Revision$
 * $Date$
 */

/**
 * This class does pretty much the opposite of java.awt.image.BufferedImageFilter:
 * It wraps an ImageFilter in a BufferedImageOp
 * Optimizations have been added, like the ignoring of color models 
 * and the assumption of INT_RGB type for destination buffers in 
 * order to speed things up by almost a factor of 2.
 */

package helma.image;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.Hashtable;

public class ImageFilterOp implements BufferedImageOp {
    ImageFilter filter;
    
    /**
     * Construct a ImageFilterOp
     */
    public ImageFilterOp(ImageFilter filter) {
        this.filter = filter;
    }

    /**
     * Do the filter operation
     * 
     * @param src The source BufferedImage. Can be any type.
     * @param dst The destination image. If not null, must be of type
     * TYPE_INT_RGB, TYPE_INT_ARGB or TYPE_INT_ARGB_PRE
     * @return the filtered image
     */
    public BufferedImage filter(BufferedImage src, BufferedImage dst) {
        int width = src.getWidth();
        int height = src.getHeight();
        
        BufferedImageConsumer consumer = new BufferedImageConsumer(dst);

        ImageFilter fltr = filter.getFilterInstance(consumer);
        fltr.setDimensions(width, height);
        
        ColorModel cm = src.getColorModel();
        /*
        if (cm.getPixelSize() == 8) {
            // byte. indexed or gray:
            WritableRaster raster = src.getRaster();
            byte pixels[] = new byte[width];
            // calculate scanline by scanline in order to safe memory.
            // It also seems to run faster like that
            for (int y = 0; y < height; y++) {
                raster.getDataElements(0, y, width, 1, pixels);
                fltr.setPixels(0, y, width, 1, cm, pixels, 0, width);
            }
        } else {
            // integer, use the simple rgb mode:
        }
        */
        // allways work in integer mode. this is more effective, and most
        // filters convert to integer internally anyhow
        cm = new SimpleColorModel();
        int pixels[] = new int[width];
        // calculate scanline by scanline in order to safe memory.
        // It also seems to run faster like that
        for (int y = 0; y < height; y++) {
            src.getRGB(0, y, width, 1, pixels, 0, width);
            fltr.setPixels(0, y, width, 1, cm, pixels, 0, width);
        }

        return consumer.getImage();
    }

    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel destCM) {
        return null;
    }

    public RenderingHints getRenderingHints() {
        return null;
    }

    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return null;
    }

    public Rectangle2D getBounds2D(BufferedImage src) {
        return null;
    }
    
    /**
     * This is a dummy ColorModel that does nothing else than returning an
     * unchanged rgb value in getRGB, getRed, getGreen, getBlue, getAlpha.
     * This speeds up things for BufferedImages by at least a factor 1.5!
     */
    class SimpleColorModel extends ColorModel {
        public SimpleColorModel() {
            this(32);
        }
        
        public SimpleColorModel(int bits) {
            super(bits);
        }
        
        public int getRGB(int rgb) {
            // this is the part that speeds up most.
            // java.awt.image.ColorModel would return the same value, but with
            // 4 function calls and a lot of shifts and ors per color!
            return rgb;
        }

        public int getAlpha(int pixel) {
            return pixel >>> 24;
        }

        public int getRed(int pixel) {
            return (pixel >> 16) & 0xff;
        }

        public int getGreen(int pixel) {
            return (pixel >>  8) & 0xff;
        }

        public int getBlue(int pixel) {
            return pixel & 0xff;
        }
    }
    
    /**
     * This is a dummy ImageConsumser that does nothing else than writing
     * The resulting rows from the ImageFilter into the image
     * If the image was not specified in the constructor, setDimensions
     * creates it with the given dimensions.
     */
    class BufferedImageConsumer implements ImageConsumer {
        BufferedImage image;
        BufferedImage compatible;
        int width;
        int height;
        boolean first = true;
        
        /*
         * Constructor with no compatible image. if image is null, setDimensions
         * will create a default INT_ARGB image of the size defined by the filter.
         */
        public BufferedImageConsumer(BufferedImage image) {
            this(image, null);
        }
        
        /*
         * Constructor with a compatible image. if image is null, setDimensions
         * will create a compatible image of the size defined by the filter.
         */
        public BufferedImageConsumer(BufferedImage image, BufferedImage compatible) {
            this.image = image;
            this.compatible = compatible;
        }
        
        public BufferedImage getImage() {
            return image;
        }

        public void setDimensions(int w, int h) {
            if (image == null) {
                if (compatible != null) {
                    // create a compatible image with the new dimensions:
                    image = new BufferedImage(
                        compatible.getColorModel(),
                        compatible.getRaster().createCompatibleWritableRaster(w, h),
                        compatible.isAlphaPremultiplied(),
                        null
                    );
                } else {
                    // assume standard format:
                    image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                }
            }
            width = image.getWidth();
            height = image.getHeight();
        }

        public void setPixels(int x, int y, int w, int h, ColorModel model, int[] pixels, int off, int scansize) {
            // Cropping may be necessary: It's possible that the size of the 
            // specified destination image is not the same as the size the 
            // ImageFilter would produce!
            if (x < width && y < height) {
                if (x + w > width)
                    w = width - x;
                if (y + h > height)
                    h = height - y;
                image.setRGB(x, y, w, h, pixels, off, scansize);
            }
        }

        public void setPixels(int x, int y, int w, int h, ColorModel model, byte[] pixels, int off, int scansize) {
            if (x < width && y < height) {
                if (x + w > width)
                    w = width - x;
                if (y + h > height)
                    h = height - y;
                image.getRaster().setDataElements(x, y, w, h, pixels);
            }
        }

        public void setProperties(Hashtable props) {
        }

        public void setColorModel(ColorModel model) {
        }

        public void setHints(int hintflags) {
        }

        public void imageComplete(int status) {
        }
    }
}