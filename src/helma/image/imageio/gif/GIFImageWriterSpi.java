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
 * The imageio integration is inspired by the package org.freehep.graphicsio.gif
 */

package helma.image.imageio.gif;

import java.io.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.spi.*;

public class GIFImageWriterSpi extends ImageWriterSpi {
	
    public GIFImageWriterSpi() {
        super(
            "Helma Object Publisher, http://helma.org/",
            "1.0",
            new String[] {"gif", "GIF"},
            new String[] {"gif", "GIF"},
            new String[] {"image/gif", "image/x-gif"},
            "helma.image.imageio.gif.GIFImageWriter",
            STANDARD_OUTPUT_TYPE,
            null,
            false, null, null, null, null,
            false, null, null, null, null
        );
    }

    public String getDescription(Locale locale) {
        return "Graphics Interchange Format";
    }

    public ImageWriter createWriterInstance(Object extension)
        throws IOException {
        return new GIFImageWriter(this);
    }

    public boolean canEncodeImage(ImageTypeSpecifier type) {
        // FIXME handle # colors
        return true;
    }
}