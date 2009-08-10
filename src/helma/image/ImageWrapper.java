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
 * - this.image is either an AWT Image or a BufferedImage.
 *   It depends on the ImageGenerator in what form the Image initially is.
 *   (the ImageIO implementation only uses BufferedImages for example.)
 * 
 *   As soon as some action that needs the graphics object is performed and the  
 *   image is still in AWT format, it is converted to a BufferedImage
 * 
 *   Any internal function that performs graphical actions needs to call
 *   getGraphics, never rely on this.graphics being set correctly!
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
    protected ImageGenerator generator;
    private Graphics2D graphics;

    /**
     * Creates a new ImageWrapper object.
     * 
     * @param image ...
     * @param width ...
     * @param height ...
     */
    public ImageWrapper(Image image, int width, int height,
        ImageGenerator generator) {
        this.image = image;
        this.width = width;
        this.height = height;
        this.generator = generator;
        // graphics are turned off by default. getGraphics activates it if necessary.
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
            Graphics2D g2d = buffered.createGraphics();
            g2d.drawImage(image, 0, 0, null);
            g2d.dispose();
            setImage(buffered);
        }
        return (BufferedImage)image;
    }

    /**
     * Returns the Graphics object to directly paint to this Image. Converts the 
     * internal image to a BufferedImage if necessary.
     * 
     * @return the Graphics object for drawing into this image
     */
    public Graphics2D getGraphics() {
        if (graphics == null) {
            // make sure the image is a BufferedImage and then create a graphics object
            BufferedImage img = getBufferedImage();
            graphics = img.createGraphics();
        }
        return graphics;
    }
    
    /**
     * Sets the internal image and clears the stored graphics object.
     * Any code that is changing the internal image should do it through this function
     * to make sure getGraphcis() returns a valid graphics object the next time it is called.
     */
    protected void setImage(Image img) {
        // flush image and dispose graphics before updating them
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
        if (image != null) {
            image.flush();
        }
        image = img;
        width = image.getWidth(null);
        height = image.getHeight(null);
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
        if (image != null) {
            image.flush();
            image = null;
        }
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
    }

    /**
     * Set the font used to write on this image.
     */
    public void setFont(String name, int style, int size) {
        getGraphics().setFont(new Font(name, style, size));
    }

    /**
     * Sets the color used to write/paint to this image.
     * 
     * @param red ...
     * @param green ...
     * @param blue ...
     */
    public void setColor(int red, int green, int blue) {
        getGraphics().setColor(new Color(red, green, blue));
    }

    /**
     * Sets the color used to write/paint to this image.
     * 
     * @param color ...
     */
    public void setColor(int color) {
        getGraphics().setColor(new Color(color));
    }

    /**
     * Sets the color used to write/paint to this image.
     * 
     * @param color ...
     */
    public void setColor(Color color) {
        getGraphics().setColor(color);
    }

    /**
     * Sets the color used to write/paint to this image.
     * 
     * @param color ...
     */
    public void setColor(String color) {
        getGraphics().setColor(Color.decode(color));
    }
    /**
     * Draws a string to this image at the given coordinates.
     * 
     * @param str ...
     * @param x ...
     * @param y ...
     */
    public void drawString(String str, int x, int y) {
        getGraphics().drawString(str, x, y);
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
        getGraphics().drawLine(x1, y1, x2, y2);
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
        getGraphics().drawRect(x, y, w, h);
    }

    /**
     * Draws another image to this image.
     * 
     * @param filename ...
     * @param x ...
     * @param y ...
     */
    public void drawImage(String filename, int x, int y) 
        throws IOException {
        Image img = generator.read(filename);
        if (img != null)
            getGraphics().drawImage(img, x, y, null);
    }

    /**
     * Draws another image to this image.
     * 
     * @param image ...
     * @param x ...
     * @param y ...
     */
    public void drawImage(ImageWrapper image, int x, int y) 
        throws IOException {
        getGraphics().drawImage(image.getImage(), x, y, null);
    }

    /**
     * Draws another image to this image.
     * 
     * @param image ...
     * @param at ...
     */
    public void drawImage(ImageWrapper image, AffineTransform at) 
        throws IOException {
        getGraphics().drawImage(image.getImage(), at, null);
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
        getGraphics().fillRect(x, y, w, h);
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
        // do not use the CropFilter any longer:
        if (image instanceof BufferedImage && x + w <= width && y + h <= height) {
            // BufferedImages define their own function for cropping:
            setImage(((BufferedImage)image).getSubimage(x, y, w, h));
        } else {
            // The internal image will be a BufferedImage after this.
            // Simply create it with the cropped dimensions and draw the image into it:
            BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = buffered.createGraphics();
            g2d.drawImage(image, -x, -y, null);
            g2d.dispose();
            setImage(buffered);
        }
    }
    
    /**
     * Trims the image.
     * 
     * @param x the x-coordinate of the pixel specifying the background color
     * @param y the y-coordinate of the pixel specifying the background color
     */
    
   public void trim(int x, int y) {
       trim(x, y, true, true, true, true);
   }

   /**
    * Trims the image.
    * 
    * @param x
    * @param y
    * @param trimLeft
    * @param trimTop
    * @param trimRight
    * @param trimBottom
    */
   public void trim(int x, int y, boolean trimLeft, boolean trimTop, boolean trimRight, boolean trimBottom) {
        BufferedImage bi = this.getBufferedImage();
        int color = bi.getRGB(x, y);
        int left = 0, top = 0, right = width - 1, bottom = height - 1;

        // create a BufferedImage of only 1 pixel height for fetching the rows of the image in the correct format (ARGB)
        // This speeds up things by more than factor 2, compared to the standard BufferedImage.getRGB solution,
        // which is supposed to be fast too. This is probably the case because drawing to BufferedImages uses 
        // very optimized code which may even be hardware accelerated.
        if (trimTop || trimBottom) {
            BufferedImage row = new BufferedImage(width, 1, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = row.createGraphics();
            int pixels[] = ((DataBufferInt)row.getRaster().getDataBuffer()).getData();
            // make sure alpha values do not add up for each row:
            g2d.setComposite(AlphaComposite.Src);
            if (trimTop) {
                // top:
                for (top = 0; top < height; top++) {
                    g2d.drawImage(bi, null, 0, -top);
                    // now pixels contains the rgb values of the row y!
                    // scan this row now:
                    for (x = 0; x < width; x++) {
                        if (pixels[x] != color)
                            break;
                    }
                    if (x < width)
                        break;
                }
            }
            if (trimBottom) {
                // bottom:
                for (bottom = height - 1;  bottom > top; bottom--) {
                    g2d.drawImage(bi, null, 0, -bottom);
                    // now pixels contains the rgb values of the row y!
                    // scan this row now:
                    for (x = 0; x < width; x++) {
                        if (pixels[x] != color)
                            break;
                    }
                    if (x < width)
                        break;
                }
            }
            g2d.dispose();
        }
        if (trimLeft || trimRight) {
            BufferedImage column = new BufferedImage(1, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = column.createGraphics();
            int pixels[] = ((DataBufferInt)column.getRaster().getDataBuffer()).getData();
            // make sure alpha values do not add up for each row:
            g2d.setComposite(AlphaComposite.Src);
            if (trimLeft) {
                // left:
                for (left = 0; left < width; left++) {
                    g2d.drawImage(bi, null, -left, 0);
                    // now pixels contains the rgb values of the row y!
                    // scan this row now:
                    for (y = 0; y < height; y++) {
                        if (pixels[y] != color)
                            break;
                    }
                    if (y < height)
                        break;
                }
            }
            if (trimRight) {
                // right:
                for (right = width - 1; right > left; right--) {
                    g2d.drawImage(bi, null, -right, 0);
                    // now pixels contains the rgb values of the row y!
                    // scan this row now:
                    for (y = 0; y < height; y++) {
                        if (pixels[y] != color)
                            break;
                    }
                    if (y < height)
                        break;
                }
            }
            g2d.dispose();
        }
        crop(left, top, right - left + 1, bottom - top + 1);
    }
    
    /**
     * Resizes the image using the Graphics2D approach
     */
    protected void resize(int w, int h, boolean smooth) {
        BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
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
        setImage(buffered);
    }

    /**
     * Resizes the image
     * 
     * @param w ...
     * @param h ...
     */
    public void resize(int w, int h) {
        double factor = Math.max(
            (double) w / width,
            (double) h / height
        );
        // If the image is scaled, used the Graphcis2D method, otherwise use AWT:
        if (factor > 1f) {
            // Scale it with the Graphics2D approach for superior quality.
            resize(w, h, true);
        } else {
            // Area averaging has the best results for shrinking of images:

            // As getScaledInstance is asynchronous, the ImageWaiter is needed here too:
            // Image scaled = ImageWaiter.waitForImage(image.getScaledInstance(w, h, Image.SCALE_AREA_AVERAGING));
            // if (scaled == null)
            //     throw new RuntimeException("Image cannot be resized.");
            
            // This version is up to 4 times faster than getScaledInstance:
            ImageFilterOp filter = new ImageFilterOp(new AreaAveragingScaleFilter(w, h));
            setImage(filter.filter(getBufferedImage(), null));
        }
    }

    /**
     * Resize the image, using a fast and cheap algorithm
     * 
     * @param w ...
     * @param h ...
     */
    public void resizeFast(int w, int h) {
        resize(w, h, false);
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
        setImage(ColorQuantizer.quantizeImage(getBufferedImage(), colors, dither,
            alphaToBitmask));
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
        generator.write(this, checkFilename(filename), quality, alpha);
    }
    
    /**
     * Saves the image. Image format is deduced from mimeType.
     * 
     * @param out ...
     * @param mimeType ...
     * @throws IOException
     */
    public void saveAs(OutputStream out, String mimeType)
        throws IOException {
        generator.write(this, out, mimeType, -1f, false); // -1 means default quality
    }
    
    /**
     * Saves the image. Image format is deduced from mimeType.
     * 
     * @param out ...
     * @param mimeType ...
     * @param quality ...
     * @throws IOException
     */
    public void saveAs(OutputStream out, String mimeType, float quality)
        throws IOException {
        generator.write(this, out, mimeType, quality, false);
    }

    /**
     * Saves the image. Image format is deduced from mimeType.
     * 
     * @param out ...
     * @param mimeType ...
     * @param quality ...
     * @param alpha ...
     * @throws IOException
     */
    public void saveAs(OutputStream out, String mimeType, float quality, boolean alpha)
        throws IOException {
        generator.write(this, out, mimeType, quality, alpha);
    }
    
    /**
     * Sets the palette index of the transparent color for Images with an
     * IndexColorModel. This can be used together with
     * {@link helma.image.ImageWrapper#getPixel}.
     */
    public void setTransparentPixel(int trans)  {
        BufferedImage bi = this.getBufferedImage();
        ColorModel cm = bi.getColorModel();
        if (!(cm instanceof IndexColorModel))
            throw new RuntimeException("Image is not indexed!");
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
        setImage(new BufferedImage(icm, bi.getRaster(), false, null));
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
     * Utility method to be used by write().
     * Converts file name to absolute path and creates parent directories.
     * @param filename the file name
     * @return the absolute path for the file name
     * @throws IOException if missing directories could not be created
     */
    String checkFilename(String filename) throws IOException {
        File file = new File(filename).getAbsoluteFile();
        File parent = file.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            throw new IOException("Error creating directories for " + filename);
        }
        return file.getPath();
    }
}