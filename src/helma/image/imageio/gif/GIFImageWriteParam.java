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

import java.util.*;
import javax.imageio.*;

public class GIFImageWriteParam extends ImageWriteParam {

    private boolean quantizeColors;
    private String quantizeMode;

    public GIFImageWriteParam(Locale locale) {
        super(locale);
        canWriteProgressive = true;
        progressiveMode = MODE_DEFAULT;
    }

    public ImageWriteParam getWriteParam(Properties properties) {
        return this;
    }
}