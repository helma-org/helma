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
import java.io.*;
import java.rmi.*;
import java.rmi.server.*;
import java.util.*;

/**
 * Abstract base class for Image Wrappers.
 */
public abstract class ImageWrapper {
    Image img;
    Graphics g;
    int width;
    int height;
    int fontstyle;
    int fontsize;
    String fontname;
    ImageGenerator imggen;

    /**
     * Creates a new ImageWrapper object.
     *
     * @param img ...
     * @param g ...
     * @param width ...
     * @param height ...
     * @param imggen ...
     */
    public ImageWrapper(Image img, Graphics g, int width, int height,
                        ImageGenerator imggen) {
        this.img = img;
        this.g = g;
        this.width = width;
        this.height = height;
        this.imggen = imggen;

        if (g != null) {
            Font f = g.getFont();

            fontname = f.getName();
            fontstyle = f.getStyle();
            fontsize = f.getSize();
        }
    }

    /**
     *  Return the Graphics object to directly paint to this Image.
     *
     *  @return the Graphics object used by this image
     */
    public Graphics getGraphics() {
        return g;
    }

    /**
     * Returns the Image object represented by this ImageWrapper.
     *
     * @return the image object
     */
    public Image getImage() {
        return img;
    }


    /**
     * Dispose the Graphics context and null out the image.
     */
    public void dispose() {
        if (img != null) {
            g.dispose();
            img = null;
        }
    }

    /**
     *  Set the font used to write on this image.
     */
    public void setFont(String name, int style, int size) {
        this.fontname = name;
        this.fontstyle = style;
        this.fontsize = size;
        g.setFont(new Font(name, style, size));
    }

    /**
     *  Set the color used to write/paint to this image.
     *
     * @param red ...
     * @param green ...
     * @param blue ...
     */
    public void setColor(int red, int green, int blue) {
        g.setColor(new Color(red, green, blue));
    }

    /**
     * Set the color used to write/paint to this image.
     *
     * @param color ...
     */
    public void setColor(int color) {
        g.setColor(new Color(color));
    }

    /**
     *  Draw a string to this image at the given coordinates.
     *
     * @param str ...
     * @param x ...
     * @param y ...
     */
    public void drawString(String str, int x, int y) {
        g.drawString(str, x, y);
    }

    /**
     *  Draw a line to this image from x1/y1 to x2/y2.
     *
     * @param x1 ...
     * @param y1 ...
     * @param x2 ...
     * @param y2 ...
     */
    public void drawLine(int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
    }

    /**
     *  Draw a rectangle to this image.
     *
     * @param x ...
     * @param y ...
     * @param w ...
     * @param h ...
     */
    public void drawRect(int x, int y, int w, int h) {
        g.drawRect(x, y, w, h);
    }

    /**
     *  Draw another image to this image.
     *
     * @param filename ...
     * @param x ...
     * @param y ...
     */
    public void drawImage(String filename, int x, int y) {
        try {
            Image i = imggen.createImage(filename);

            g.drawImage(i, x, y, null);
        } catch (Exception ignore) {
        }
    }

    /**
     *  Draw a filled rectangle to this image.
     *
     * @param x ...
     * @param y ...
     * @param w ...
     * @param h ...
     */
    public void fillRect(int x, int y, int w, int h) {
        g.fillRect(x, y, w, h);
    }

    /**
     *  get the width of this image.
     *
     * @return ...
     */
    public int getWidth() {
        return width;
    }

    /**
     *  get the height of this image.
     *
     * @return ...
     */
    public int getHeight() {
        return height;
    }

    /**
     * crop this image.
     *
     * @param x ...
     * @param y ...
     * @param w ...
     * @param h ...
     */
    public void crop(int x, int y, int w, int h) {
        ImageFilter filter = new CropImageFilter(x, y, w, h);

        img = Toolkit.getDefaultToolkit().createImage(new FilteredImageSource(img.getSource(),
                                                                              filter));
    }

    /**
     *  resize this image
     *
     * @param w ...
     * @param h ...
     */
    public void resize(int w, int h) {
        img = img.getScaledInstance(w, h, Image.SCALE_SMOOTH);
        width = w;
        height = h;
    }

    /**
     *  resize this image, using a fast and cheap algorithm
     *
     * @param w ...
     * @param h ...
     */
    public void resizeFast(int w, int h) {
        img = img.getScaledInstance(w, h, Image.SCALE_FAST);
        width = w;
        height = h;
    }

    /**
     *  reduce the colors used in this image. Necessary before saving as GIF.
     *
     * @param colors ...
     */
    public abstract void reduceColors(int colors);

    /**
     *  Save this image. Image format is deduced from filename.
     *
     * @param filename ...
     */
    public abstract void saveAs(String filename);

    /**
     * Get ImageProducer of the wrapped image
     */
    public ImageProducer getSource() {
        return img.getSource();
    }

    /**
     * Draw a string to this image, breaking lines when necessary.
     *
     * @param str ...
     */
    public void fillString(String str) {
        Filler filler = new Filler(0, 0, width, height);

        filler.layout(str);
    }

    /**
     * Draw a line to a rectangular section of this image, breaking lines when necessary.
     *
     * @param str ...
     * @param x ...
     * @param y ...
     * @param w ...
     * @param h ...
     */
    public void fillString(String str, int x, int y, int w, int h) {
        Filler filler = new Filler(x, y, w, h);

        filler.layout(str);
    }

    class Filler {
        int x;
        int y;
        int w;
        int h;
        int addedSpace = 0;
        int xLeft;
        int yLeft;
        int realHeight;
        transient Vector lines;

        public Filler(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public void layout(String str) {
            int size = fontsize;

            lines = new Vector();

            while (!splitMessage(str, size) && (size > 10)) {
                lines.setSize(0);
                size = Math.max(2, size - 1);
            }

            Font oldfont = g.getFont();

            g.setFont(new Font(fontname, fontstyle, size));

            int l = lines.size();

            for (int i = 0; i < l; i++) {
                ((Line) lines.elementAt(i)).paint(g, xLeft / 2, (yLeft / 2) + y);
            }

            g.setFont(oldfont);
        }

        private boolean splitMessage(String string, int size) {
            Font font = new Font(fontname, fontstyle, size);
            FontMetrics metrics = Toolkit.getDefaultToolkit().getFontMetrics(font);
            int longestLine = 0;
            int heightSoFar = 0;
            int heightIncrement = (int) (0.84f * metrics.getHeight());
            StringTokenizer tk = new StringTokenizer(string);
            StringBuffer buffer = new StringBuffer();
            int spaceWidth = metrics.stringWidth(" ");
            int currentWidth = 0;
            int maxWidth = w - 2;
            int maxHeight = (h + addedSpace) - 2;

            while (tk.hasMoreTokens()) {
                String nextToken = tk.nextToken();
                int nextWidth = metrics.stringWidth(nextToken);

                if ((((currentWidth + nextWidth) >= maxWidth) && (currentWidth != 0))) {
                    Line line = new Line(buffer.toString(), x, heightSoFar, metrics);

                    lines.addElement(line);

                    if (line.textwidth > longestLine) {
                        longestLine = line.textwidth;
                    }

                    buffer = new StringBuffer();

                    currentWidth = 0;
                    heightSoFar += heightIncrement;
                }

                buffer.append(nextToken);
                buffer.append(" ");
                currentWidth += (nextWidth + spaceWidth);

                if (((1.18 * heightSoFar) > maxHeight) && (fontsize > 10)) {
                    return false;
                }
            }

            if (!"".equals(buffer.toString().trim())) {
                Line line = new Line(buffer.toString(), x, heightSoFar, metrics);

                lines.addElement(line);

                if (line.textwidth > longestLine) {
                    longestLine = line.textwidth;
                }

                if ((longestLine > maxWidth) && (fontsize > 10)) {
                    return false;
                }

                heightSoFar += heightIncrement;
            }

            xLeft = w - longestLine;
            yLeft = (addedSpace + h) - heightSoFar;
            realHeight = heightSoFar;

            return ((1.18 * heightSoFar) <= maxHeight);
        }
    }

    class Line implements Serializable {
        String text;
        int xoff;
        int yoff;
        FontMetrics fm;
        public int textwidth;
        public int len;
        int ascent;

        public Line(String text, int xoff, int yoff, FontMetrics fm) {
            this.text = text.trim();
            len = text.length();
            this.xoff = xoff;
            this.yoff = yoff;
            this.fm = fm;
            textwidth = (len == 0) ? 0 : fm.stringWidth(this.text);
            ascent = (int) (0.9f * fm.getAscent());
        }

        public void paint(Graphics g, int xadd, int yadd) {
            g.drawString(text, xoff + xadd, yoff + ascent + yadd);
        }

        public boolean contains(int y) {
            return (y < (yoff + fm.getHeight())) ? true : false;
        }
    }
}
