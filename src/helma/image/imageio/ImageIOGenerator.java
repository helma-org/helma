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
 * ImageIOGenerator defines it's own functions for reading from various
 * resources. These return BufferedImages, therefore all the images
 * are from the beginning in that format when working with ImageIO
 */

package helma.image.imageio;

import java.awt.Image;
import java.awt.image.*;
import java.io.*;
import java.net.URL;
import java.util.Iterator;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;

import helma.image.*;


/**
 * A wrapper for an image that uses the ImageIO Framework.
 */
public class ImageIOGenerator extends ImageGenerator {
    /**
     * @param file the filename the filename of the image to create
     *
     * @return the newly created image
     * @throws IOException
     */
    public Image read(String filename)
    throws IOException {
        return ImageIO.read(new File(filename));
    }
    
    /**
     * @param url the URL the filename of the image to create
     *
     * @return the newly created image
     * @throws IOException
     */
    public Image read(URL url)
    throws IOException {
        return ImageIO.read(url);
    }
    
    /**
     * @param src the data of the image to create
     *
     * @return the newly created image
     * @throws IOException
     */
    public Image read(byte[] src)
    throws IOException {
        return ImageIO.read(new ByteArrayInputStream(src));
    }

    /**
     * Saves the image. Image format is deduced from filename.
     *
     * @param filename ...
     * @param quality ...
     * @param alpha ...
     * @throws IOException
     * @see helma.image.ImageGenerator#write(helma.image.ImageWrapper, java.lang.String, float, boolean)
     */
    public void write(ImageWrapper wrapper, String filename, float quality, boolean alpha) throws IOException {
        int pos = filename.lastIndexOf('.');
        if (pos != -1) {
            String extension = filename.substring(pos + 1,
                filename.length()).toLowerCase();

            // Find a writer for that file extensions
            ImageWriter writer = null;
            Iterator iter = ImageIO.getImageWritersByFormatName(extension);
            if (iter.hasNext())
                writer = (ImageWriter) iter.next();
            if (writer != null) {
                ImageOutputStream ios = null;
                try {
                    BufferedImage bi = wrapper.getBufferedImage();
                    // Prepare output file
                    File file = new File(filename);
                    if (file.exists())
                        file.delete();
                    ios = ImageIO.createImageOutputStream(file);
                    writer.setOutput(ios);
                    // Set some parameters
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    if (param.canWriteCompressed() &&
                        quality >= 0.0 && quality <= 1.0) {
                        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        param.setCompressionQuality(quality);
                    }
                    if (param.canWriteProgressive())
                        param.setProgressiveMode(ImageWriteParam.MODE_DISABLED);
                    // if bi has type ARGB and alpha is false, we have to tell the writer to not use the alpha channel:
                    // this is especially needed for jpeg files where imageio seems to produce wrong jpeg files right now...
                    if (bi.getType() == BufferedImage.TYPE_INT_ARGB
                        && !alpha) {
                        // create a new BufferedImage that uses a WritableRaster of bi, with all the bands except the alpha band:
                        WritableRaster raster = bi.getRaster();
                        WritableRaster newRaster = raster.createWritableChild(
                            0, 0, wrapper.getWidth(), wrapper.getHeight(),
                            0, 0, new int[] {0, 1, 2 }
                        );
                        // create a ColorModel that represents the one of the ARGB except the alpha channel:
                        DirectColorModel cm = (DirectColorModel) bi.getColorModel();
                        DirectColorModel newCM = new DirectColorModel(
                            cm.getPixelSize(), cm.getRedMask(),
                            cm.getGreenMask(), cm.getBlueMask());
                        // now create the new buffer that is used ot write the image:
                        BufferedImage rgbBuffer = new BufferedImage(newCM,
                            newRaster, false, null);
                        writer.write(null, new IIOImage(rgbBuffer, null,
                            null), param);
                    } else {
                        writer.write(null, new IIOImage(bi, null, null),
                            param);
                    }
                } finally {
                    if (ios != null)
                        ios.close();
                    writer.dispose();
                }
            }
        }
}
}