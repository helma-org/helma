// RemoteImage.java
// Copyright (c) Hannes Wallnöfer 1999-2000
  
package helma.image;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.rmi.*;
import java.rmi.server.*;

/**
 * Implementation of an image that is accessible via RMI.
 */
 
public class RemoteImage extends UnicastRemoteObject implements IRemoteImage {

    ImageWrapper wrapped;

    public RemoteImage (ImageWrapper wrapped) throws RemoteException {
	this.wrapped = wrapped;
    }

    public void setFont (String name, int style, int size) {
	wrapped.setFont (name, style, size);
    }
    
    public void setColor (int red, int green, int blue) {
	wrapped.setColor (red, green, blue);
    }
    
    public void setColor (int color) {
	wrapped.setColor (color);
    }

    public void drawString (String str, int x, int y) {
	wrapped.drawString (str, x, y);
    }

    public void drawLine (int x1, int y1, int x2, int y2) {
	wrapped.drawLine (x1, y1, x2, y2);
    }

    public void drawRect (int x, int y, int w, int h) {
	wrapped.drawRect (x, y, w, h);
    }
    
    public void drawImage (String filename, int x, int y) {
	wrapped.drawImage (filename, x, y);
    }

    public void fillRect (int x, int y, int w, int h) {
	wrapped.fillRect (x, y, w, h);
    }

    public int getWidth () {
	return wrapped.getWidth();
    }

    public int getHeight () {
	return wrapped.getHeight();
    }

    public void crop (int x, int y, int w, int h) {
	wrapped.crop (x, y, w, h);
    }

    public void resize (int w, int h) {
	wrapped.resize (w, h);
    }
    
    public void reduceColors (int colors) {
	wrapped.reduceColors (colors);
    }
    
    public void saveAs (String filename) {
	wrapped.saveAs (filename);
    }
    
    public void readFrom (String filename) {
	wrapped.readFrom (filename);
    }
    
    public byte[] getBytes (String type) { 
	return wrapped.getBytes (type);
    }
    
    public void setBytes (byte[] bytes, String type) {
	wrapped.setBytes (bytes, type);
    }

    public void fillString (String str) {
	wrapped.fillString (str);
    }

    public void fillString (String str, int x, int y, int w, int h) {
	wrapped.fillString (str, x, y, w, h);
    }
    

}





