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
     * Saves the image. Image format is deduced from filename.
     *
     * @param filename ...
     * @param quality ...
     * @param alpha ...
     * @throws IOException
     * @see helma.image.ImageGenerator#write(helma.image.ImageWrapper, java.lang.String, float, boolean)
     */
    public void write(ImageWrapper wrapper, String filename, float quality, boolean alpha) throws IOException {
        try {
            String lowerCaseName = filename.toLowerCase();
            if (lowerCaseName.endsWith(".gif")) {
                // sun's jimi package doesn't encode gifs, use helma's encoder instead
                DataOutputStream out = new DataOutputStream(
                    new FileOutputStream(filename));
                GIFEncoder encoder = new GIFEncoder();
                encoder.encode(wrapper.getBufferedImage(), out);
                out.close();
            } else {
                // let's not rely on Jimi's file-extension detecting mechanisms,
                // as these do not seem to specify file type depending options
                // afterwars. Instead, the file ending is checked here:

                JimiImage source = Jimi.createRasterImage(wrapper.getSource());
                JimiEncoder encoder = null;
                if (lowerCaseName.endsWith(".jpg")
                    || lowerCaseName.endsWith(".jpeg")) {
                    // JPEG
                    encoder = new JPGEncoder();
                    // the quality value does mean something here and can be specified:
                    if (quality >= 0.0 && quality <= 1.0) {
                        JPGOptions options = new JPGOptions();
                        options.setQuality(Math.round(quality * 100));
                        source.setOptions(options);
                    }
                } else if (lowerCaseName.endsWith(".png")) {
                    // PNG
                    encoder = new PNGEncoder();
                    // the alpha parameter does mean something here:
                    ((PNGEncoder) encoder).setAlpha(new Boolean(alpha));
                    PNGOptions options = new PNGOptions();
                    // TODO: Use quality for CompressionType control, similar to ImageIOWrapper (?)
                    options.setCompressionType(PNGOptions.COMPRESSION_MAX);
                    source.setOptions(options);
                }
                if (encoder != null) {
                    FileOutputStream out = new FileOutputStream(filename);
                    encoder.encodeImages(new JimiImageEnumeration(source), out);
                    out.close();
                } else { // if nothing worked, fall back to the Jimi mechanisms and see wether something comes out
                    Jimi.putImage(wrapper.getImage(), filename);
                }
            }
        } catch (JimiException e) {
            throw new IOException(e.getMessage());
        }
    }

}