// ImageGenerator.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.image;

import java.awt.*;
import java.net.URL;

/**
 * This creates an invisible frame in order to be able to create images
 * from Java. (Java needs a window context in order to user the Image class).
 */
 
public class ImageGenerator extends Window {
    
    public ImageGenerator () {
	super (new Frame() {
                public void setVisible (boolean b) {
                    // This frame can never be shown
                }
                public synchronized void dispose() {
                    try {
                        getToolkit().getSystemEventQueue();
                        super.dispose();
                    } catch (Exception e) {
                        // untrusted code not allowed to dispose
                    }
                }
	}
	);
	this.setBounds (0, 0, 0, 0);
	this.setVisible (true);
    }
    
    public ImageWrapper createPaintableImage (int w, int h) {
	Image img = createImage (w, h);
	Graphics g = img.getGraphics ();
	ImageWrapper rimg = null;
	try {
	    try {
	        rimg = new ActivatedImageWrapper (img, g, w, h, this);
	    } catch (NoClassDefFoundError notfound) {
	        rimg = new SunImageWrapper (img, g, w, h, this);
	    } catch (ClassNotFoundException notfound) {
	        rimg = new SunImageWrapper (img, g, w, h, this);
	    }
	} catch (Exception x) {}
	return rimg;
    }

    public ImageWrapper createPaintableImage (byte src[]) {
	ImageWrapper rimg = null;
	MediaTracker tracker = new MediaTracker (this);
	try {
	    Image img1 = Toolkit.getDefaultToolkit ().createImage (src);
	    tracker.addImage (img1, 0);
	    tracker.waitForAll ();
	    int w = img1.getWidth (null);
	    int h = img1.getHeight (null);
	    Image img = createImage (w, h);
	    Graphics g = img.getGraphics ();
	    g.drawImage (img1, 0, 0, null);
	    try {
	        rimg = new ActivatedImageWrapper (img, g, w, h, this);
	    } catch (ClassNotFoundException notfound) {
	        rimg = new SunImageWrapper (img, g, w, h, this);
	    } catch (NoClassDefFoundError notfound) {
	        rimg = new SunImageWrapper (img, g, w, h, this);
	    }
	} catch (Exception x) {}
	return rimg;
    }

    public ImageWrapper createImage (byte src[]) {
	ImageWrapper rimg = null;
	MediaTracker tracker = new MediaTracker (this);
	try {
	    Image img = Toolkit.getDefaultToolkit ().createImage (src);
	    tracker.addImage (img, 0);
	    tracker.waitForAll ();
	    int w = img.getWidth (null);
	    int h = img.getHeight (null);
	    try {
	        rimg = new ActivatedImageWrapper (img, null, w, h, this);
	    } catch (ClassNotFoundException notfound) {
	        rimg = new SunImageWrapper (img, null, w, h, this);
	    }  catch (NoClassDefFoundError notfound) {
	        rimg = new SunImageWrapper (img, null, w, h, this);
	    }
	} catch (Exception x) {}
	return rimg;
    }


    public ImageWrapper createPaintableImage (String urlstring) {
	ImageWrapper rimg = null;
	MediaTracker tracker = new MediaTracker (this);
	try {
	    URL url = new URL (urlstring);
	    Image img1 = Toolkit.getDefaultToolkit ().createImage (url);
	    tracker.addImage (img1, 0);
	    tracker.waitForAll ();
	    int w = img1.getWidth (null);
	    int h = img1.getHeight (null);
	    Image img = createImage (w, h);
	    Graphics g = img.getGraphics ();
	    g.drawImage (img1, 0, 0, null);
	    try {
	        rimg = new ActivatedImageWrapper (img, g, w, h, this);
	    } catch (ClassNotFoundException notfound) {
	        rimg = new SunImageWrapper (img, g, w, h, this);
	    } catch (NoClassDefFoundError notfound) {
	        rimg = new SunImageWrapper (img, g, w, h, this);
	    }
	} catch (Exception x) {
	    x.printStackTrace ();
	}
	return rimg;
    }
    
     public Image createImage (String filename) {
	Image img = null;
	MediaTracker tracker = new MediaTracker (this);
	try {
	    img = Toolkit.getDefaultToolkit ().createImage (filename);
	    tracker.addImage (img, 0);
	    tracker.waitForAll ();
	} catch (Exception x) {
	    x.printStackTrace ();
	}
	return img;
    }
}
