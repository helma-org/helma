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

/*
 * A few explanations:
 * 
 * - ImageWrapper.image is either an AWT Image or a BufferedImage.
 *   It depends on the ImageGenerator in what form the Image initially is.
 *   (the ImageIO implementation only uses BufferedImages for example.
 * 
 *   As soon as some action that needs the graphics object is performed and the  
 *   image is still in AWT format, it is converted to a BufferedImage
 * 
 *   Any internal function that performs graphical actions needs to call
 *   createGraphics (but only if this.graphics == null)
 * 
 * - ImageWrapper objects are created and safed by the ImageGenerator class
 *   all different implementations of Imaging functionallity are implemented
 *   as a ImageGenerator extending class.
 * 
 */

package helma.image;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.*;

/**
 * Abstract base class for Image Wrappers.
 */
public class ImageWrapper {
    protected Image image;
    protected int width;
    protected int height;
    protected Graphics2D graphics;
    protected ImageGenerator generator;

    /**
     * Creates a new ImageWrapper object.
     * 
     * @param img ...
     * @param g ...
     * @param width ...
     * @param height ...
     * @param imggen ...
     */
    public ImageWrapper(Image image, int width, int height,
        ImageGenerator generator) {
        this.image = image;
        this.width = width;
        this.height = height;
        this.generator = generator;
        // graphics are turned off by default. every graphical function
        // has to assure that graphcis != null, and if not, call createGraphics
        // the function that turns this image into a paintable one! 
        this.graphics = null;
    }
    
    public ImageWrapper(Image image, ImageGenerator generator) {
        this(image, image.getWidth(null), image.getHeight(null), generator);
    }

    /**
     * Converts the internal image object to a BufferedImage (if it's not
     * already) and returns it. this is necessary as not all images are of type
     * BufferedImage. e.g. images loaded from a resource with the Toolkit are
     * not. By using getBufferedImage, images are only converted to a
     * getBufferedImage when this is actually needed, which is better than
     * storing images as BufferedImage in general.
     * 
     * @return the Image object as a BufferedImage
     */
    public BufferedImage getBufferedImage() {
        if (!(image instanceof BufferedImage)) {
            BufferedImage buffered = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);
            buffered.createGraphics().drawImage(image, 0, 0, null);
            image = buffered;
        }
        return (BufferedImage)image;
    }
    
    /**
     * convert's the internal image to a BufferedImage and creates a graphics object:
     */
    protected void createGraphics() {
        BufferedImage img = getBufferedImage();
        graphics = img.createGraphics();
    }

    /**
     * Sets the palette index of the transparent color for Images with an
     * IndexColorModel. This can be used together with
     * {@link helma.image.ImageWrapper#getPixel}.
     */
    public void setTransparentPixel(int trans) throws IOException {
        BufferedImage bi = this.getBufferedImage();
        ColorModel cm = bi.getColorModel();
        if (!(cm instanceof IndexColorModel))
            throw new IOException("Image is not indexed!");
        IndexColorModel icm = (IndexColorModel) cm;
        int mapSize = icm.getMapSize();
        byte reds[] = new byte[mapSize];
        byte greens[] = new byte[mapSize];
        byte blues[] = new byte[mapSize];
        icm.getReds(reds);
        icm.getGreens(greens);
        icm.getBlues(blues);
        // create the new IndexColorModel with the changed transparentPixel:
        icm = new IndexColorModel(icm.getPixelSize(), mapSize, reds, greens,
            blues, trans);
        // create a new BufferedImage with the new IndexColorModel and the old
        // raster:
        image = new BufferedImage(icm, bi.getRaster(), false, null);
    }

    /**
     * Returns the pixel at x, y. If the image is indexed, it returns the
     * palette index, otherwise the rgb code of the color is returned.
     * 
     * @param x the x coordinate of the pixel
     * @param y the y coordinate of the pixel
     * @return the pixel at x, y
     */
    public int getPixel(int x, int y) {
        BufferedImage bi = this.getBufferedImage();
        if (bi.getColorModel() instanceof IndexColorModel)
            return bi.getRaster().getSample(x, y, 0);
        else
            return bi.getRGB(x, y);
    }

    /**
     * Creates and returns a copy of this image.
     * 
     * @return a clone of this image.
     */
    public Object clone() {
        ImageWrapper wrapper = generator.createImage(this.width,
            this.height);
        wrapper.getGraphics().drawImage(image, 0, 0, null);
        return wrapper;
    }

    /**
     * Returns the Graphics object to directly paint to this Image.
     * 
     * @return the Graphics object used by this image
     */
    public Graphics getGraphics() {
        if (graphics == null) createGraphics();
        return graphics;
    }

    /**
     * Returns the Image object represented by this ImageWrapper.
     * 
     * @return the image object
     */
    public Image getImage() {
        return image;
    }

    /**
     * Returns the ImageProducer of the wrapped image
     * 
     * @return the images's ImageProducer
     */
    public ImageProducer getSource() {
        return image.getSource();
    }

    /**
     * Dispose the Graphics context and null out the image.
     */
    public void dispose() {
        image = null;
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
    }

    /**
     * Set the font used to write on this image.
     */
    public void setFont(String name, int style, int size) {
        if (graphics == null) createGraphics();
        graphics.setFont(new Font(name, style, size));
    }

    /**
     * Sets the color used to write/paint to this image.
     * 
     * @param red ...
     * @param green ...
     * @param blue ...
     */
    public void setColor(int red, int green, int blue) {
        if (graphics == null) createGraphics();
        graphics.setColor(new Color(red, green, blue));
    }

    /**
     * Sets the color used to write/paint to this image.
     * 
     * @param color ...
     */
    public void setColor(int color) {
        if (graphics == null) createGraphics();
        graphics.setColor(new Color(color));
    }

    /**
     * Sets the color used to write/paint to this image.
     * 
     * @param color ...
     */
    public void setColor(Color color) {
        if (graphics == null) createGraphics();
        graphics.setColor(color);
    }

    /**
     * Draws a string to this image at the given coordinates.
     * 
     * @param str ...
     * @param x ...
     * @param y ...
     */
    public void drawString(String str, int x, int y) {
        if (graphics == null) createGraphics();
        graphics.drawString(str, x, y);
    }

    /**
     * Draws a line to this image from x1/y1 to x2/y2.
     * 
     * @param x1 ...
     * @param y1 ...
     * @param x2 ...
     * @param y2 ...
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
        if (graphics == null) createGraphics();
        graphics.drawLine(x1, y1, x2, y2);
    }

    /**
     * Draws a rectangle to this image.
     * 
     * @param x ...
     * @param y ...
     * @param w ...
     * @param h ...
     */
    public void drawRect(int x, int y, int w, int h) {
        if (graphics == null) createGraphics();
        graphics.drawRect(x, y, w, h);
    }

    /**
     * Draws another image to this image.
     * 
     * @param filename ...
     * @param x ...
     * @param y ...
     */
    public void drawImage(String filename, int x, int y) {
        if (graphics == null) createGraphics();
        try {
            Image img = generator.read(filename);
            graphics.drawImage(img, x, y, null);
        } catch (Exception ignore) {
        }
    }

    /**
     * Draws a filled rectangle to this image.
     * 
     * @param x ...
     * @param y ...
     * @param w ...
     * @param h ...
     */
    public void fillRect(int x, int y, int w, int h) {
        if (graphics == null) createGraphics();
        graphics.fillRect(x, y, w, h);
    }

    /**
     * Returns the width of this image.
     * 
     * @return the width of this image
     */
    public int getWidth() {
        return width;
    }

    /**
     * Returns the height of this image.
     * 
     * @return the height of this image
     */
    public int getHeight() {
        return height;
    }

    /**
     * Crops the image.
     * 
     * @param x ...
     * @param y ...
     * @param w ...
     * @param h ...
     */
    public void crop(int x, int y, int w, int h) {
        if (image instanceof BufferedImage) {
            image = ((BufferedImage)image).getSubimage(x, y, w, h);
        } else {
            BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = buffered.createGraphics();
            g2d.drawImage(image, -x, -y, null);
            g2d.dispose();
            image = buffered;
        }
    }
    
    /**
     * resizes the image using the Graphics2D approach
     */
    protected BufferedImage resize(int w, int h, boolean smooth) {
        BufferedImage buffered = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = buffered.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, 
            smooth ? RenderingHints.VALUE_INTERPOLATION_BICUBIC :
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, 
            smooth ? RenderingHints.VALUE_RENDER_QUALITY :
                RenderingHints.VALUE_RENDER_SPEED
        );

        AffineTransform at = AffineTransform.getScaleInstance(
            (double) w / width,
            (double) h / height
        );
        g2d.drawImage(image, at, null);
        g2d.dispose();
        return buffered;
    }

    /**
     * Resize the image
     * 
     * @param w ...
     * @param h ...
     */

    public void resize(int w, int h) {
        double factor = Math.max(
            (double) w / width,
            (double) h / height
        );
        // if the image is scaled, used the Graphcis2D method, otherwise use AWT:
        if (factor > 1f)
            image = resize(w, h, true);
        else {
            // Area averaging has the best results for shrinking of images:
            /*
            // as getScaledInstance is asynchronous, the ImageWaiter is needed here too:
            Image scaled = ImageWaiter.waitForImage(image.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING));
            if (scaled == null)
                throw new RuntimeException("Image cannot be resized.");
            */
            image = new ImageFilterOp(new AreaAveragingScaleFilter(w, h)).filter(getBufferedImage(), null);
        }
        width = w;
        height = h;
    }

    /**
     * Resize the image, using a fast and cheap algorithm
     * 
     * @param w ...
     * @param h ...
     */
    public void resizeFast(int w, int h) {
        image = resize(w, h, false);
        width = w;
        height = h;
   }

    /**
     * Reduces the colors used in the image. Necessary before saving as GIF.
     * 
     * @param colors colors the number of colors to use, usually <= 256.
     */
    public void reduceColors(int colors) {
        reduceColors(colors, false);
    }

    /**
     * Reduces the colors used in the image. Necessary before saving as GIF.
     * 
     * @param colors colors the number of colors to use, usually <= 256.
     * @param dither ...
     */
    public void reduceColors(int colors, boolean dither) {
        reduceColors(colors, dither, true);
    }

    /**
     * Reduce the colors used in this image. Useful and necessary before saving
     * the image as GIF file.
     * 
     * @param colors the number of colors to use, usually <= 256.
     * @param dither ...
     * @param alphaToBitmask ...
     */

    public void reduceColors(int colors, boolean dither, boolean alphaToBitmask) {
        image = Quantize.process(getBufferedImage(), colors, dither,
            alphaToBitmask);
    }

    /**
     * Save the image. Image format is deduced from filename.
     * 
     * @param filename ...
     * @throws IOException
     */
    public void saveAs(String filename)
    throws IOException {
        saveAs(filename, -1f, false); // -1 means default quality
    }

    /**
     * Saves the image. Image format is deduced from filename.
     * 
     * @param filename ...
     * @param quality ...
     * @throws IOException
     */
    public void saveAs(String filename, float quality)
    throws IOException {
        saveAs(filename, quality, false);
    }

    /**
     * Saves the image. Image format is deduced from filename.
     * 
     * @param filename ...
     * @param quality ...
     * @param alpha ...
     * @throws IOException
     */
    public void saveAs(String filename, float quality, boolean alpha)
        throws IOException {
        generator.write(this, filename, quality, alpha);
    }
}