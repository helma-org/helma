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
	BufferedImage img = new BufferedImage (w, h, BufferedImage.TYPE_INT_RGB);
	Graphics g = img.getGraphics ();
	ImageWrapper rimg = new SunImageWrapper (img, g, w, h, this);
	return rimg;
    }

    public ImageWrapper createPaintableImage (byte src[]) {
	ImageWrapper rimg = null;
	Image img1 = Toolkit.getDefaultToolkit ().createImage (src);
	ImageLoader loader = new ImageLoader (img1);
	loader.getDimensions ();
	int w = loader.getWidth ();
	int h = loader.getHeight ();
	Image img = new BufferedImage (w, h, BufferedImage.TYPE_INT_RGB);
	Graphics g = img.getGraphics ();
	if (!g.drawImage (img1, 0, 0, loader))
	    loader.getBits ();
	rimg = new SunImageWrapper (img, g, w, h, this);
	return rimg;
    }

    public ImageWrapper createImage (byte src[]) {
	ImageWrapper rimg = null;
	Image img = Toolkit.getDefaultToolkit ().createImage (src);
	ImageLoader loader = new ImageLoader (img);
	loader.getDimensions ();
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
	loader.getDimensions ();
	int w = loader.getWidth ();
	int h = loader.getHeight ();
	Image img = new BufferedImage (w, h, BufferedImage.TYPE_INT_RGB);
	Graphics g = img.getGraphics ();
	if(!g.drawImage (img1, 0, 0, loader))
	    loader.getBits ();
	rimg = new SunImageWrapper (img, g, w, h, this);
	return rimg;
    }

    public ImageWrapper createPaintableImage (ImageWrapper iw, ImageFilter filter) {
	ImageWrapper rimg = null;
	FilteredImageSource fis = new FilteredImageSource (iw.getSource(), filter);
	Image img1 = Toolkit.getDefaultToolkit().createImage (fis);
	ImageLoader loader = new ImageLoader (img1);
	loader.getDimensions ();
	int w = loader.getWidth ();
	int h = loader.getHeight ();
	Image img = new BufferedImage (w, h, BufferedImage.TYPE_INT_RGB);
	Graphics g = img.getGraphics ();
	if (!g.drawImage (img1, 0, 0, loader))
	    loader.getBits ();
	rimg = new SunImageWrapper (img, g, w, h, this);
	return rimg;
    }


     public Image createImage (String filename) {
	Image img = null;
	img = Toolkit.getDefaultToolkit ().createImage (filename);
	ImageLoader loader = new ImageLoader (img);
	loader.getDimensions ();
	return img;
    }

    public Image createImage (ImageProducer producer) {
	Image img = null;
	img = Toolkit.getDefaultToolkit ().createImage (producer);
	ImageLoader loader = new ImageLoader (img);
	loader.getDimensions ();
	return img;
    }

    class ImageLoader implements ImageObserver {

	Image img;
	int w, h;
	boolean waiting;
	boolean firstFrameLoaded;

	ImageLoader (Image img) {
	    this.img = img;
	    waiting = true;
	    firstFrameLoaded = false;
	}

	synchronized void getDimensions () {
	    w = img.getWidth(this);
	    h = img.getHeight (this);
	    if (w == -1 || h == -1) try {
	        wait (45000);
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

	synchronized void getBits () {
	    if (!firstFrameLoaded) try {
	        wait (45000);
	    } catch (InterruptedException x) {
	        waiting = false;
	        return;
	    } finally {
	        waiting = false;
	    }
	}

	int getWidth () {
	    return w;
	}

	int getHeight () {
	    return h;
	}

	public synchronized boolean imageUpdate(Image img,
                           int infoflags,
                           int x,
                           int y,
                           int width,
                           int height) {
	    if ((infoflags & WIDTH) > 0 || (infoflags & HEIGHT) > 0) {
	        if ((infoflags & WIDTH) > 0)
	            w = width;
	        if ((infoflags & HEIGHT) > 0)
	            h = height;
	        if (w > -1 && h > -1) {
	            notifyAll ();
	            return false;
	        }
	    }
	    if ((infoflags & ALLBITS) > 0 || (infoflags & FRAMEBITS) > 0) {
	        firstFrameLoaded = true;
	        notifyAll ();
	        return false;
	    }
	    // check if there was an error
	    if (!waiting || (infoflags & ERROR) > 0 || (infoflags & ABORT) > 0) {
	        // we either timed out or there was an error.
	        notifyAll ();
	        return false;
	    }
	    return true;
	}


    }

}
