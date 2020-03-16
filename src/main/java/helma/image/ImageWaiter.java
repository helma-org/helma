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

import java.awt.Image;

import java.awt.image.ImageObserver;

/**
 * The ImageWaiter will only be used like this:
 * image = ImageWaiter.waitForImage(image);
 */
public class ImageWaiter implements ImageObserver {
    Image image;
    int width;
    int height;
    boolean waiting;
    boolean firstFrameLoaded;

    private ImageWaiter(Image image) {
        this.image = image;
        waiting = true;
        firstFrameLoaded = false;
    }
        
    public static Image waitForImage(Image image) {
        ImageWaiter waiter = new ImageWaiter(image);
        try {
            waiter.waitForImage();
        } finally {
            waiter.done();
        }
        return waiter.width == -1 || waiter.height == -1 ? null : image;
    }

    private synchronized void waitForImage() {
        width = image.getWidth(this);
        height = image.getHeight(this);

        if (width == -1 || height == -1) {
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
        if (width == -1 || height == -1) {
            throw new RuntimeException("Error loading image");
        }
    }

    private synchronized void done() {
        waiting = false;
        notifyAll();
    }

    public synchronized boolean imageUpdate(Image img, int infoflags, int x,
        int y, int w, int h) {
        // check if there was an error
        if (!waiting || (infoflags & ERROR) > 0 || (infoflags & ABORT) > 0) {
            // we either timed out or there was an error.
            notifyAll();

            return false;
        }

        if ((infoflags & WIDTH) > 0 || (infoflags & HEIGHT) > 0) {
            if ((infoflags & WIDTH) > 0) {
                width = w;
            }

            if ((infoflags & HEIGHT) > 0) {
                height = h;
            }

            if (width > -1 && h > -1 && firstFrameLoaded) {
                notifyAll();

                return false;
            }
        }

        if ((infoflags & ALLBITS) > 0 || (infoflags & FRAMEBITS) > 0) {
            firstFrameLoaded = true;
            notifyAll();

            return false;
        }

        return true;
    }
}