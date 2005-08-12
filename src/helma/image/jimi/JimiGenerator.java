/*
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

package helma.image.jimi;

import helma.image.*;

import java.io.*;

import com.sun.jimi.core.*;
import com.sun.jimi.core.encoder.jpg.JPGEncoder;
import com.sun.jimi.core.encoder.png.PNGEncoder;
import com.sun.jimi.core.options.JPGOptions;
import com.sun.jimi.core.options.PNGOptions;

public class JimiGenerator extends ImageGenerator {
 
    /**
     * Internal function for writing images.
     *
     * @param wrapper ...
     * @param type either a file extension or a mimetype with stripped image/ or image/x-
     * @param quality ...
     * @param alpha ...
     * @throws IOException
     * @see helma.image.ImageGenerator#write(helma.image.ImageWrapper, java.lang.String, float, boolean)
     */
    protected boolean write(ImageWrapper wrapper, String type, OutputStream out, float quality, boolean alpha) throws IOException {
        try {
            if ("gif".equals(type)) {
                // sun's jimi package doesn't encode gifs, use helma's encoder instead
                DataOutputStream dataOut = new DataOutputStream(out);
                GIFEncoder encoder = new GIFEncoder();
                encoder.encode(wrapper.getBufferedImage(), dataOut);
            } else {
                // let's not rely on Jimi's file-extension detecting mechanisms,
                // as these do not seem to specify file type depending options
                // afterwars. Instead, the file ending is checked here:

                JimiImage source = Jimi.createRasterImage(wrapper.getSource());
                JimiEncoder encoder = null;
                if ("jpg".equals(type) || "jpeg".equals(type)) {
                    // JPEG
                    encoder = new JPGEncoder();
                    // the quality value does mean something here and can be specified:
                    if (quality >= 0.0 && quality <= 1.0) {
                        JPGOptions options = new JPGOptions();
                        options.setQuality(Math.round(quality * 100));
                        source.setOptions(options);
                    }
                } else if ("png".equals(type)) {
                    // PNG
                    encoder = new PNGEncoder();
                    // the alpha parameter does mean something here:
                    ((PNGEncoder) encoder).setAlpha(new Boolean(alpha));
                    PNGOptions options = new PNGOptions();
                    // TODO: Use quality for CompressionType control, similar to ImageIOWrapper (?)
                    options.setCompressionType(PNGOptions.COMPRESSION_MAX);
                    source.setOptions(options);
                }
                // if no encoder was found, return false. let jimi handle this in the functions bellow
                if (encoder == null) return false;
                encoder.encodeImages(new JimiImageEnumeration(source), out);
            }
            return true;
        } catch (JimiException e) {
            throw new IOException(e.getMessage());
        }
   }
    
    /**
     * Saves the image. Image format is deduced from filename.
     *
     * @param wrapper ...
     * @param filename ...
     * @param quality ...
     * @param alpha ...
     * @throws IOException
     * @see helma.image.ImageGenerator#write(helma.image.ImageWrapper, java.lang.String, float, boolean)
     */
    public void write(ImageWrapper wrapper, String filename, float quality, boolean alpha) throws IOException {
        // determine the type from the file extension
        int pos = filename.lastIndexOf('.');
        if (pos != -1) {
            String extension = filename.substring(pos + 1, filename.length()).toLowerCase();
            FileOutputStream out = new FileOutputStream(filename);
            boolean written = false;
            try {
                written = this.write(wrapper, extension, out, quality, alpha);
            } finally {
                out.close();
            }
            // if nothing worked, fall back to the Jimi mechanisms and see wether something comes out
            if (!written) {
                try {
                    Jimi.putImage(wrapper.getImage(), filename);
                } catch (JimiException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
    }

    /**
      * Saves the image. Image format is deduced from filename.
     *
     * @param wrapper ...
     * @param out ...
     * @param mimeType ...
     * @param quality ...
     * @param alpha ...
     * @throws IOException
    * @see helma.image.ImageGenerator#write(helma.image.ImageWrapper, java.io.OutputStream, java.lang.String, float, boolean)
     */
    public void write(ImageWrapper wrapper, OutputStream out, String mimeType, float quality, boolean alpha) throws IOException {
        // determine the type from the mime type by taking away image/ and image/x-
        if (mimeType.startsWith("image/")) {
            String type = mimeType.substring(6);
            if (type.startsWith("x-"))
                type = type.substring(2);
            boolean written = false;
            try {
                written = this.write(wrapper, type, out, quality, alpha);
            } finally {
                out.close();
            }
            // if nothing worked, fall back to the Jimi mechanisms and see wether something comes out
            if (!written) {
                try {
                    Jimi.putImage(mimeType, wrapper.getImage(), out);
                } catch (JimiException e) {
                    throw new IOException(e.getMessage());
                }
            }
        }
    }
}