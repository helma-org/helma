// ImageWrapper.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.image;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;
import java.io.*;

/** 
 * Abstract base class for Image Wrappers.
 */
 
public abstract class ImageWrapper {

    Image img;
    Graphics g;
    int width, height;
    int fontstyle, fontsize;
    String fontname;
    ImageGenerator imggen;

    public ImageWrapper (Image img, Graphics g, int width, int height, ImageGenerator imggen) {
	this.img = img;
	this.g = g;
	this.width = width;
	this.height = height;
	this.imggen = imggen;
	if (g != null) {
	    Font f = g.getFont ();
	    fontname = f.getName ();
	    fontstyle = f.getStyle ();
	    fontsize = f.getSize ();
	}
    }

    /**
     *  image manipulation methods
     */ 

    public void setFont (String name, int style, int size) {
	this.fontname = name;
	this.fontstyle = style;
	this.fontsize = size;
	g.setFont (new Font (name, style, size));
    }
    
    public void setColor (int red, int green, int blue) {
	g.setColor (new Color (red, green, blue));
    }
    
    public void setColor (int color) {
	g.setColor (new Color (color));
    }

    public void drawString (String str, int x, int y) {
	g.drawString (str, x, y);
    }

    public void drawLine (int x1, int y1, int x2, int y2) {
	g.drawLine (x1, y1, x2, y2);
    }

    public void drawRect (int x, int y, int w, int h) {
	g.drawRect (x, y, w, h);
    }
    
    public void drawImage (String filename, int x, int y) {
	try {
	    Image i = imggen.createImage (filename);
	    g.drawImage (i, x, y, null);
	} catch (Exception ignore) {}
    }

    public void fillRect (int x, int y, int w, int h) {
	g.fillRect (x, y, w, h);
    }

    public int getWidth () {
	return width;
    }

    public int getHeight () {
	return height;
    }

    public void crop (int x, int y, int w, int h) {
	ImageFilter filter = new CropImageFilter (x, y, w, h);
	img = Toolkit.getDefaultToolkit ().createImage(new FilteredImageSource(img.getSource(), filter));

    }

    public void resize (int w, int h) {
	img = img.getScaledInstance (w, h, Image.SCALE_SMOOTH);
	width = w;
	height = h;
    }
    
    public void resizeFast (int w, int h) {
	img = img.getScaledInstance (w, h, Image.SCALE_FAST);
	width = w;
	height = h;
    }

    public abstract void reduceColors (int colors);

    public abstract void saveAs (String filename);

    /**
     * Get ImageProducer of the wrapped image
     */
    public ImageProducer getSource () {
	return img.getSource ();
    }

    public void fillString (String str) {
	Filler filler = new Filler (0, 0, width, height);
	filler.layout (str);
    }

    public void fillString (String str, int x, int y, int w, int h) {
	Filler filler = new Filler (x, y, w, h);
	filler.layout (str);
    }
    
    class Filler {

    int x, y, w, h;
    int addedSpace = 0;
    int xLeft, yLeft;
    int realHeight;
    transient Vector lines;
    
    public Filler (int x, int y, int w, int h) {
	this.x = x;
	this.y = y;
	this.w = w;
	this.h = h;
    }
    
    public void layout (String str) {
	int size = fontsize;
	lines = new Vector ();
	while (!splitMessage (str, size) && size > 10) {
	    lines.setSize (0);
	    size = Math.max (2, size-1);
	}
	Font oldfont = g.getFont ();
	g.setFont (new Font (fontname, fontstyle, size));
	int l = lines.size();
	for (int i = 0; i < l; i++) {
	    ((Line) lines.elementAt (i)).paint (g, xLeft/2, yLeft/2 + y);
	}
	g.setFont (oldfont);
    }
    
    private boolean splitMessage (String string, int size) {

	Font font = new Font (fontname, fontstyle, size);
	FontMetrics metrics = Toolkit.getDefaultToolkit ().getFontMetrics (font);
	int longestLine = 0;
	int heightSoFar = 0;
	int heightIncrement = (int) (0.84f * metrics.getHeight ());
	StringTokenizer tk = new StringTokenizer (string);
	StringBuffer buffer = new StringBuffer();
	int spaceWidth = metrics.stringWidth(" ");
	int currentLine = 0;
	int currentWidth = 0;
	int maxWidth = w - 2, maxHeight = h + addedSpace - 2;
	while (tk.hasMoreTokens()) {
	    String nextToken = tk.nextToken();
	    int nextWidth = metrics.stringWidth(nextToken); 
	    if ((currentWidth + nextWidth >= maxWidth && currentWidth != 0)) {
	        Line line = new Line (buffer.toString(), x, heightSoFar, metrics);
	        lines.addElement (line); 
	        if (line.textwidth > longestLine)
	            longestLine = line.textwidth;
	        buffer = new StringBuffer();

	        currentWidth = 0;
	        heightSoFar += heightIncrement;
	    }
	    buffer.append (nextToken); 
	    buffer.append (" ");
	    currentWidth += (nextWidth + spaceWidth);
	    if (1.18*heightSoFar > maxHeight && fontsize > 10)
	        return false;
	}
	if (! "".equals (buffer.toString().trim())) {
	    Line line = new Line (buffer.toString(), x, heightSoFar, metrics);
	    lines.addElement (line); 

	    if (line.textwidth > longestLine)
	        longestLine = line.textwidth;
	    if (longestLine > maxWidth && fontsize > 10)
	        return false;
	    heightSoFar += heightIncrement;
	}
	xLeft = w - longestLine;
	yLeft = addedSpace + h - heightSoFar;
	realHeight = heightSoFar;
	return (1.18*heightSoFar <= maxHeight);
    }

    }


    class Line implements Serializable {

    String text;
    int xoff, yoff;
    FontMetrics fm;
    public int textwidth, len;
    int ascent;


    public Line (String text, int xoff, int yoff, FontMetrics fm) {
	this.text = text.trim();
	len = text.length();
	this.xoff = xoff;
	this.yoff = yoff;
	this.fm = fm;
	textwidth = (len == 0) ? 0 : fm.stringWidth(this.text);
	ascent = (int) (0.9f * fm.getAscent());
    }

    public void paint (Graphics g, int xadd, int yadd) {
	g.drawString (text, xoff+xadd, yoff+ascent+yadd);
    }


    public boolean contains (int y) {
	return (y < yoff+fm.getHeight()) ? true : false;
    }


    }

}





