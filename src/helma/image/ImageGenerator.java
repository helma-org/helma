// ImageGenerator.java
// Copyright (c) Hannes Wallnöfer 1999-2000

package helma.image;

import java.awt.*;
import java.awt.image.*;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * This creates an invisible frame in order to be able to create images
 * from Java. (Java needs a window context in order to user the Image class).
 */
 
public class ImageGenerator {

    public ImageGenerator () {
	// nothing to do
    }

    public ImageWrapper createPaintableImage (int w, int h) {
	Image img = new BufferedImage (w, h, BufferedImage.TYPE_INT_RGB);
	Graphics g = img.getGraphics ();
	ImageWrapper rimg = null;
	rimg = new SunImageWrapper (img, g, w, h, this);
	return rimg;
    }

    public ImageWrapper createPaintableImage (byte src[]) {
	ImageWrapper rimg = null;
	Image img1 = Toolkit.getDefaultToolkit ().createImage (src);
	ImageLoader loader = new ImageLoader (img1);
	loader.load ();
	int w = loader.getWidth ();
	int h = loader.getHeight ();
	Image img = new BufferedImage (w, h, BufferedImage.TYPE_INT_RGB);
	Graphics g = img.getGraphics ();
	g.drawImage (img1, 0, 0, null);
	rimg = new SunImageWrapper (img, g, w, h, this);
	return rimg;
    }

    public ImageWrapper createImage (byte src[]) {
	ImageWrapper rimg = null;
	Image img = Toolkit.getDefaultToolkit ().createImage (src);
	ImageLoader loader = new ImageLoader (img);
	loader.load ();
	int w = loader.getWidth ();
	int h = loader.getHeight ();
	rimg = new SunImageWrapper (img, null, w, h, this);
	return rimg;
    }


    public ImageWrapper createPaintableImage (String urlstring) throws MalformedURLException {
	ImageWrapper rimg = null;
	URL url = new URL (urlstring);
	Image img1 = Toolkit.getDefaultToolkit ().createImage (url);
	ImageLoader loader = new ImageLoader (img1);
	loader.load ();
	int w = loader.getWidth ();
	int h = loader.getHeight ();
	Image img = new BufferedImage (w, h, BufferedImage.TYPE_INT_RGB);
	Graphics g = img.getGraphics ();
	g.drawImage (img1, 0, 0, null);
	rimg = new SunImageWrapper (img, g, w, h, this);
	return rimg;
    }

    public ImageWrapper createPaintableImage (ImageWrapper iw, ImageFilter filter) {
	ImageWrapper rimg = null;
	FilteredImageSource fis = new FilteredImageSource (iw.getSource(), filter);
	Image img1 = Toolkit.getDefaultToolkit().createImage (fis);
	ImageLoader loader = new ImageLoader (img1);
	loader.load ();
	int w = loader.getWidth ();
	int h = loader.getHeight ();
	Image img = new BufferedImage (w, h, BufferedImage.TYPE_INT_RGB);
	Graphics g = img.getGraphics ();
	g.drawImage (img1, 0, 0, null);
	rimg = new SunImageWrapper (img, g, w, h, this);
	return rimg;
    }


     public Image createImage (String filename) {
	Image img = null;
	img = Toolkit.getDefaultToolkit ().createImage (filename);
	ImageLoader loader = new ImageLoader (img);
	loader.load ();
	return img;
    }

    public Image createImage (ImageProducer producer) {
	Image img = null;
	img = Toolkit.getDefaultToolkit ().createImage (producer);
	ImageLoader loader = new ImageLoader (img);
	loader.load ();
	return img;
    }

    class ImageLoader implements ImageObserver {

	Image img;
	int w, h;
	boolean waiting;

	ImageLoader (Image img) {
	    this.img = img;
	    waiting = true;
	}

	int getWidth () {
	    return w;
	}
	
	int getHeight () {
	    return h;
	}

	synchronized void load () {
	    w = img.getWidth(this);
	    h = img.getHeight (this);
	    if (w == -1 || h == -1) try {
	        wait (30000);
	    } catch (InterruptedException x) {
	        waiting = false;
	        return;
	    } finally {
	        waiting = false;
	    }
	    // if width and height haven't been set, throw tantrum
	    if (w == -1 || h == -1) {
	        throw new RuntimeException ("Error loading image");
	    }
	}


	public synchronized boolean imageUpdate(Image img,
                           int infoflags,
                           int x,
                           int y,
                           int width,
                           int height) {
	    if (w == -1 && (infoflags & WIDTH) > 0)
	        w = width;
	    if (h == -1 && (infoflags & HEIGHT) > 0)
	        h = height;
	    if (h > -1 && w > -1) {
	        // we know all we want to know. notify waiting thread that
	        // the image is loaded and ready to be used.
	        notifyAll ();
	        return false;
	    }
	    // check if there was an error
	    if (!waiting || (infoflags & ERROR) > 0 || (infoflags & ABORT) > 0) {
	        notifyAll ();
	        return false;
	    }
	    return true;
	}


    }

}
