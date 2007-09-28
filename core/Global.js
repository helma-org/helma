/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2005 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Global.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */


app.addRepository("modules/core/String.js");


/**
 * write out a property contained in app.properties
 * @param Object containing the name of the property
 */
function property_macro(param, name) {
    res.write(getProperty(name || param.name) || String.NULL);
    return;
}


/**
 * wrapper to output a string from within a skin
 * just to be able to use different encodings
 * @param Object containing the string as text property
 */
function write_macro(param, text) {
    res.write(param.text || text || String.NULL);
    return;
}


/**
 * renders the current datetime
 * @param Object containing a formatting string as format property
 */
function now_macro(param) {
    var d = new Date();
    if (param.format) {
        res.write(d.format(param.format));
    } else if (param.as == "timestamp") {
        res.write(d.getTime());
    } else {
        res.write(d);
    }
    return;
}


/**
 * renders a global skin
 */
var skin_macro = function(param, name) {
    var skinName = name || param.name;
    if (skinName) {
        renderSkin(skinName, param);
    }
    return;
}

