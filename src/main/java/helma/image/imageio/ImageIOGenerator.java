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

import helma.image.ImageGenerator;
import helma.image.ImageWrapper;

import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;


/**
 * A wrapper for an image that uses the ImageIO Framework.
 */
public class ImageIOGenerator extends ImageGenerator {

    protected void write(ImageWrapper wrapper, ImageWriter writer, float quality, boolean alpha) throws IOException {
        BufferedImage bi = wrapper.getBufferedImage();
        // Set some parameters
        ImageWriteParam param = writer.getDefaultWriteParam();
        if (param.canWriteCompressed() &&
            quality >= 0.0 && quality <= 1.0) {
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            String[] types = param.getCompressionTypes();
            // If compression types are defined, but none is set, set the first one,
            // since setCompressionQuality, which requires MODE_EXPLICIT to be set,
            // will complain otherwise. 
            if (types != null && param.getCompressionType() == null) {
                param.setCompressionType(types[0]);
            }
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
                0, 0, raster.getWidth(), raster.getHeight(),
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
    }

    /**
     * Saves the image. Image format is deduced from filename.
     *
     * @param wrapper the image to write
     * @param filename the file to write to
     * @param quality image quality
     * @param alpha to enable alpha
     * @throws IOException
     * @see helma.image.ImageGenerator#write(helma.image.ImageWrapper, java.lang.String, float, boolean)
     */
    public void write(ImageWrapper wrapper, String filename, float quality, boolean alpha) throws IOException {
        // determine suffix:
        int pos = filename.lastIndexOf('.');
        if (pos != -1) {
            String extension = filename.substring(pos + 1, filename.length()).toLowerCase();

            // Find a writer for that file suffix
            ImageWriter writer = null;
            Iterator iter = ImageIO.getImageWritersBySuffix(extension);
            if (iter.hasNext())
                writer = (ImageWriter)iter.next();
            if (writer != null) {
                ImageOutputStream ios = null;
                try {
                    // Prepare output file
                    File file = new File(filename);
                    if (file.exists())
                        file.delete();
                    ios = ImageIO.createImageOutputStream(file);
                    writer.setOutput(ios);
                    this.write(wrapper, writer, quality, alpha);
                 } finally {
                    if (ios != null)
                        ios.close();
                    writer.dispose();
                }
            }
        }
    }

    /**
     * Saves the image. Image format is deduced from type.
     *
     * @param wrapper the image to write
     * @param out the outputstream to write to
     * @param mimeType the mime type
     * @param quality image quality
     * @param alpha to enable alpha
     * @throws IOException
     * @see helma.image.ImageGenerator#write(helma.image.ImageWrapper, java.io.OutputStream, java.lang.String, float, boolean)
     */
    public void write(ImageWrapper wrapper, OutputStream out, String mimeType, float quality, boolean alpha) throws IOException {
            // Find a writer for that type
        ImageWriter writer = null;
        Iterator iter = ImageIO.getImageWritersByMIMEType(mimeType);
        if (iter.hasNext())
            writer = (ImageWriter)iter.next();
        if (writer != null) {
            ImageOutputStream ios = null;
            try {
                ios = ImageIO.createImageOutputStream(out);
                writer.setOutput(ios);
                this.write(wrapper, writer, quality, alpha);
            } finally {
                if (ios != null)
                    ios.close();
                writer.dispose();
            }
        }
   }
}