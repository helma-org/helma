/*
 * Helma License Notice
 *
 * The contents of this file are subject to the Helma License
 * Version 2.0 (the "License"). You may not use this file except in
 * compliance with the License. A copy of the License is available at
 * http://adele.helma.org/download/helma/license.txt
 *
 * Copyright 1998-2006 Helma Software. All Rights Reserved.
 *
 * $RCSfile: Html.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */


/**
 * @fileoverview Fields and methods of the helma.Html
 * and helma.Html.Tablewriter classes.
 * <br /><br />
 * To use this optional module, its repository needs to be added to the 
 * application, for example by calling app.addRepository('modules/helma/Html.js')
 */

// take care of any dependencies
app.addRepository('modules/core/String.js');
app.addRepository('modules/core/Object.js');
app.addRepository('modules/core/Array.js');

/**
 * Define the global namespace if not existing
 */
if (!global.helma) {
    global.helma = {};
}

/**
 * Creates a new instance of helma.Html
 * @class This class provides various methods for rendering
 * X/Html tags.
 * @returns A newly created instance of helma.Html
 * @constructor
 */
helma.Html = function() {
    return this;
};

/**
 * Static helper method that renders an arbitrary markup part.
 * @param {String} name The element's name
 * @param {String} start Prefix of each rendered element
 * @param {String} end Suffix of each rendered element
 * @param {Object} attr Optional element attributes
 */
helma.Html.renderMarkupPart = function(name, start, end, attr) {
    res.write(start);
    res.write(name);
    if (attr) {
        for (var i in attr) {
            if (i == "prefix" || i == "suffix" || 
                i == "default" || attr[i] == null) {
                continue;
            }
            res.write(" ");
            res.write(i);
            res.write("=\"");
            res.write(attr[i]);
            res.write("\"");
        }
    }
    res.write(end);
    return;
};

/**
 * Static helper method used in helma.Html.checkBox
 * and helma.Html.dropDown to check if a current value
 * matches against one or more selected values passed
 * as argument
 * @param {String} value The current value to check
 * @param {String|Array} selectedValue Either a single
 * value to check against the current value, or an array
 * containing values.
 * @returns True in case the value is among the selected
 * values, false otherwise
 * @type Boolean
 */
helma.Html.isSelected = function(value, selectedValue) {
    if (selectedValue == null || value == null)
        return false;
    if (selectedValue instanceof Array)
        return selectedValue.contains(value);
    return value == selectedValue;
};


/** @ignore */
helma.Html.prototype.toString = function() {
    return "[helma.Html]";
};

/**
 * Renders the opening tag of an arbitrary x/html tag
 * @param {String} name The tag name
 * @param {Object} attr An optional object containing element attributes
 */
helma.Html.prototype.openTag = function(name, attr) {
    helma.Html.renderMarkupPart(name, "<", ">", attr);
    return;
};

/**
 * Returns the opening tag of an arbitrary x/html tag
 * @param {String} name The tag name
 * @param {Object} attr An optional object containing element attributes
 * @returns The rendered x/html opening tag
 * @type String
 * @see #openTag
 */
helma.Html.prototype.openTagAsString = function(name, attr) {
    res.push();
    helma.Html.renderMarkupPart(name, "<", ">", attr);
    return res.pop();
};

/**
 * Renders the closing tag of an arbitrary x/html tag
 * @param {String} name The tag name
 */
helma.Html.prototype.closeTag = function(name) {
    helma.Html.renderMarkupPart(name, "</", ">", null);
    return;
};

/**
 * Returns the closing tag of an arbitray x/html element
 * @param {String} name The tag name
 * @returns The rendered closing tag
 * @type String
 * @see #closeTag
 */
helma.Html.prototype.closeTagAsString = function(name) {
    res.push();
    helma.Html.renderMarkupPart(name, "</", ">", null);
    return res.pop();
};

/**
 * Renders an empty arbitrary x/html tag ("contentless tag")
 * @param {String} name The tag name
 * @param {Object} attr An optional object containing tag attributes
 */
helma.Html.prototype.tag = function(name, attr) {
    helma.Html.renderMarkupPart(name, "<", " />", attr);
    return;
};

/**
 * Returns an empty arbitrary x/html tag ("contentless tag")
 * @param {String} name The tag name
 * @param {Object} attr An optional object containing tag attributes
 * @returns The rendered element
 * @type String
 * @see #tag
 */
helma.Html.prototype.tagAsString = function(name, attr) {
    res.push();
    helma.Html.renderMarkupPart(name, "<", " />", attr);
    return res.pop();
};

/**
 * Renders an arbitrary x/html element
 * @param {String} name The element name
 * @param {String} str The content of the element
 * @param {Object} attr An optional object containing element attributes
 */
helma.Html.prototype.element = function(name, str, attr) {
    helma.Html.renderMarkupPart(name, "<", ">", attr);
    res.write(str);
    helma.Html.renderMarkupPart(name, "</", ">");
    return;
};

/**
 * Return an arbitrary x/html element
 * @param {String} name The element name
 * @param {String} str The content of the element
 * @param {Object} attr An optional object containing element attributes
 * @returns The rendered element
 * @type String
 * @see #element
 */
helma.Html.prototype.elementAsString = function(name, str, attr) {
    res.push();
    this.element(name, str, attr);
    return res.pop();
};

/**
 * Renders an x/html link tag
 * @param {Object} attr An object containing the link attributes
 * @param {String} text The text to appear as link
 */
helma.Html.prototype.link = function(attr, text) {
    if (!attr) {
        res.write("[Html.link: insufficient arguments]");
        return;
    }
    this.openTag("a", attr);
    res.write(text);
    this.closeTag("a");
    return;
};

/**
 * Returns a rendered x/html link tag
 * @param {Object} attr An object containing the link attributes
 * @param {String} text The text to appear as link
 * @returns The rendered link tag
 * @type String
 * @see #link
 */
helma.Html.prototype.linkAsString = function(attr, text) {
    res.push();
    this.link(attr, text);
    return res.pop();
};

/**
 * Renders an x/html input tag of type "hidden"
 * @param {Object} param An object containing the tag attributes
 */
helma.Html.prototype.hidden = function(param) {
    if (!param) {
        res.write("[Html.hidden: insufficient arguments]");
        return;
    }
    var attr = Object.prototype.reduce.call(param);
    attr.type = "hidden";
    attr.value = (attr.value != null) ? encodeForm(attr.value) : "";
    this.tag("input", attr);
    return;
};

/**
 * Returns a rendered x/html input tag of type "hidden"
 * @param {Object} attr An object containing the tag attributes
 * @returns The rendered input element
 * @type String
 * @see #hidden
 */
helma.Html.prototype.hiddenAsString = function(attr) {
    res.push();
    this.hidden(attr);
    return res.pop();
};

/**
 * Renders an x/html text input tag
 * @param {Object} param An object containing the tag attributes
 */
helma.Html.prototype.input = function(param) {
    if (!param) {
        res.write("[Html.input: insufficient arguments]");
        return;
    }
    var attr = Object.prototype.reduce.call(param);
    attr.type = "text";
    if (!attr.size)
        attr.size = 20;
    attr.value = (attr.value != null) ? encodeForm(attr.value) : "";
    this.tag("input", attr);
    return;
};

/**
 * Returns a rendered x/html text input tag
 * @param {Object} attr An object containing the tag attributes
 * @returns The rendered text input tag
 * @type String
 * @see #input
 */
helma.Html.prototype.inputAsString = function(attr) {
    res.push();
    this.input(attr);
    return res.pop();
};

/**
 * Renders an x/html textarea tag
 * @param {Object} param An object containing the tag attributes
 */
helma.Html.prototype.textArea = function(param) {
    if (!param) {
        res.write("[Html.textArea: insufficient arguments]");
        return;
    }
    var attr = Object.prototype.reduce.call(param);
    var value = (attr.value != null) ? encodeForm(attr.value) : "";
    delete attr.value;
    this.openTag("textarea", attr);
    res.write(value);
    this.closeTag("textarea");
    return;
};

/**
 * Returns a rendered x/html textarea tag
 * @param {Object} attr An object containing the tag attributes
 * @returns The rendered textarea tag
 * @type String
 * @see #textArea
 */
helma.Html.prototype.textAreaAsString = function(attr) {
    res.push();
    this.textArea(attr);
    return res.pop();
};

/**
 * Renders an x/html checkbox input tag
 * @param {Object} param An object containing the tag attributes
 */
helma.Html.prototype.checkBox = function(param) {
    if (!param) {
        res.write("[Html.checkBox: insufficient arguments]");
        return;
    }
    var attr = Object.prototype.reduce.call(param);
    attr.type = "checkbox";
    if (attr.selectedValue != null) {
        if (helma.Html.isSelected(param.value, param.selectedValue))
            attr.checked = "checked";
        else
            delete attr.checked;
        delete attr.selectedValue;
    }
    this.tag("input", attr);
    return;
};

/**
 * Returns a rendered x/html checkbox input tag
 * @param {Object} attr An object containing the tag attributes
 * @returns The rendered checkbox tag
 * @type String
 * @see #checkBox
 */
helma.Html.prototype.checkBoxAsString = function(attr) {
    res.push();
    this.checkBox(attr);
    return res.pop();
};

/**
 * Renders an x/html radiobutton input tag
 * @param {Object} param An object containing the tag attributes
 */
helma.Html.prototype.radioButton = function(param) {
    if (!param) {
        res.write("[Html.radioButton: insufficient arguments]");
        return;
    }
    var attr = Object.prototype.reduce.call(param);
    attr.type = "radio";
    if (attr.selectedValue != null) {
        if (attr.value == attr.selectedValue)
            attr.checked = "checked";
        else
            delete attr.checked;
        delete attr.selectedValue;
    }
    this.tag("input", attr);
    return;
};

/**
 * Returns a rendered x/html radio input tag
 * @param {Object} attr An object containing the tag attributes
 * @returns The rendered element
 * @type String
 * @see #radioButton
 */
helma.Html.prototype.radioButtonAsString = function(attr) {
    res.push();
    this.radioButton(attr);
    return res.pop();
};

/**
 * Renders an x/html submit input tag
 * @param {Object} param An object containing the tag attributes
 */
helma.Html.prototype.submit = function(param) {
    if (!param) {
        res.write("[Html.submit: insufficient arguments]");
        return;
    }
    var attr = Object.prototype.reduce.call(param);
    attr.type = "submit";
    if (!attr.name)
        attr.name = attr.type;
    attr.value = (attr.value != null) ? encodeForm(attr.value) : attr.type;
    this.tag("input", attr);
    return;  
};

/**
 * Returns a rendered x/html submit input tag
 * @param {Object} attr An object containing the tag attributes
 * @returns The rendered submit input tag
 * @type String
 * @see #submit
 */
helma.Html.prototype.submitAsString = function(attr) {
    res.push();
    this.submit(attr);
    return res.pop();
};

/**
 * Renders an x/html button input tag
 * @param {Object} param An object containing the tag attributes
 */
helma.Html.prototype.button = function(param) {
    if (!param) {
        res.write("[Html.button: insufficient arguments]");
        return;
    }
    var attr = Object.prototype.reduce.call(param);
    attr.type = "button";
    if (!attr.name)
        attr.name = attr.type;
    attr.value = (attr.value != null) ? encodeForm(attr.value) : attr.type;
    this.tag("input", attr);
    return;  
};

/**
 * Returns a rendered x/html button input tag
 * @param {Object} param An object containing the tag attributes
 * @returns The rendered button input tag
 * @type String
 * @see #button
 */
helma.Html.prototype.buttonAsString = function(attr) {
    res.push();
    this.button(attr);
    return res.pop();
};

/**
 * Renders a x/html drop down select box
 * @param {Object} param An object containing the tag attributes
 * @param {Array} options Either an array of strings, an array with 
 * several <code>{value: v, display: d}</code> objects, or a collection 
 * of <code>["value", "display"]</code> arrays in an array
 * @param {String} selectedValue The value to pre-select
 * @param {String} firstOption An optional first option to display in the
 * select box (this option will always have no value)
 */
helma.Html.prototype.dropDown = function(param, options, selectedValue, firstOption) {
    if (!param) {
        res.write("[Html.dropDown: insufficient arguments]");
        return;
    }
    var attr = Object.prototype.reduce.call(param);
    if (!attr.size)
        attr.size = 1;
    this.openTag("select", attr);
    res.write("\n ");
    if (firstOption) {
        this.openTag("option", {value: ""});
        res.write(firstOption);
        this.closeTag("option");
        res.write("\n ");
    }
    for (var i in options) {
        var attr = new Object();
        var display = "";
        if ((options[i] instanceof Array) && options[i].length > 0) {
            // option is an array
            attr.value = options[i][0];
            display = options[i][1];
        } else if (options[i].value != null && options[i].display != null) {
            // option is an object
            attr.value = options[i].value;
            if (options[i]["class"] != null) {
               attr["class"] = options[i]["class"];
            }
            display = options[i].display;
        } else {
            // assume option is a string
            attr.value = i;
            display = options[i];
        }
        if (helma.Html.isSelected(attr.value, selectedValue))
            attr.selected = "selected";
        this.openTag("option", attr);
        res.write(display);
        this.closeTag("option");
        res.write("\n ");
    }
    this.closeTag("select");
    res.write("\n ");
    return;
};

/**
 * Returns a rendered x/html drop down select box
 * @param {Object} param An object containing the tag attributes
 * @param {Array} options Either an array of strings, an array with 
 * several <code>{value: v, display: d}</code> objects, or a collection 
 * of <code>["value", "display"]</code> arrays in an array
 * @param {String} selectedValue The value to pre-select
 * @param {String} firstOption An optional first option to display in the
 * select box (this option will always have no value)
 * @returns The rendered drop down select box
 * @type String
 * @see #dropDown
 */
helma.Html.prototype.dropDownAsString = function(attr, options, selectedValue, firstOption) {
    res.push();
    this.dropDown(attr, options, selectedValue, firstOption);
    return res.pop();
};

/**
 *  Renders an image map based on an array containing the map parameters.
 *  @param {String} name The name of the image map
 *  @param {Array} param An array containing objects, where each of them
 *  contains the attributes for a single image map entry
 */
helma.Html.prototype.map = function(name, param) {
    if (!name || !param) {
        res.write("[Html.map: insufficient arguments]");
        return;
    }
    this.openTag("map", {name: name});
    var attr = Object.prototype.reduce.call(param);
    for (var i in areas) {
        if (!areas[i].alt)
            areas[i].alt = "";
        if (!areas[i].shape)
            areas[i].shape = "rect";
        this.openTag("area", areas[i]);
    }
    this.closeTag("map");
    return;
};

/**
 *  Returns a rendered image map based on an array containing the map parameters.
 *  @param {String} name The name of the image map
 *  @param {Array} areas An array containing objects, where each of them
 *  contains the attributes for a single image map entry
 * @returns The rendered image map
 * @type String
 * @see #map
 */
helma.Html.prototype.mapAsString = function(name, areas) {
    res.push();
    this.map(name, areas);
    return res.pop();
};

/**
 * Renders a complete x/html table.
 * @param {Array} headers An array containing table headers
 * @param {Array} data A two-dimensional array containing the table data
 * @param {Object} param An object containing the following properties:
 * <ul>
 * <li><code>table</code>: Attributes to render within the opening <code>&lt;table&gt;</code> tag</li>
 * <li><code>tr</code>: Attributes to render within each <code>&lt;tr&gt;</code> tag</li>
 * <li><code>td</code>: Attributes to render within each <code>&lt;td&gt;</code> tag</li>
 * <li><code>th</code>: Attributes to render within each <code>&lt;th&gt;</code> tag</li>
 * <li><code>trHead</code>: Attributes to render within each <code>&lt;tr&gt;</code> tag
 in the header area of the table</li>
 * <li><code>trEven</code>: Attributes to render within each even <code>&lt;tr&gt;</code> tag</li>
 * <li><code>trOdd</code>: Attributes to render within each odd <code>&lt;tr&gt;</code> tag</li>
 * <li><code>tdEven</code>: Attributes to render within each even <code>&lt;td&gt;</code> tag</li>
 * <li><code>tdOdd</code>: Attributes to render within each odd <code>&lt;td&gt;</code> tag</li>
 * <li><code>thEven</code>: Attributes to render within each even <code>&lt;th&gt;</code> tag</li>
 * <li><code>thOdd</code>: Attributes to render within each odd <code>&lt;th&gt;</code> tag</li>
 * </ul>
 */
helma.Html.prototype.table = function(headers, data, param) {
    if (!param) {
        res.write("[Html.table: insufficient arguments]");
        return;
    }
    var attr = Object.prototype.reduce.call(param);
    if (!attr.trHead) attr.trHead = attr.tr;
    if (!attr.trEven) attr.trEven = attr.tr;
    if (!attr.trOdd)  attr.trOdd = attr.tr;
    if (!attr.tdEven) attr.tdEven = attr.td;
    if (!attr.tdOdd)  attr.tdOdd = attr.td;
    if (!attr.thEven) attr.thEven = attr.th;
    if (!attr.thOdd)  attr.thOdd = attr.th;
    this.openTag("table", attr.table);
    if (headers) {
        this.openTag("tr", attr.trHead);
        for (var i in headers) {
            var evenOdd = i % 2 == 0 ? "Even" : "Odd";
            this.openTag("th", attr["th"+evenOdd]);
            res.write(headers[i]);
            this.closeTag("th");
        }
        this.closeTag("tr");
    }
    for (var i in data) {
        var evenOdd = i % 2 == 0 ? "Even" : "Odd";
        this.openTag("tr", attr["tr"+evenOdd]);
        for (var j in data[i]) {
            var evenOddCell = j % 2 == 0 ? "Even" : "Odd";
            this.openTag("td", attr["td"+evenOddCell]);
            res.write(data[i][j]);
            this.closeTag("td");
        }
        this.closeTag("tr");
    }
    this.closeTag("table");
    return;
};

/**
 * Returns a rendered x/html table
 * @param {Array} headers An array containing table headers
 * @param {Array} data A two-dimensional array containing the table data
 * @param {Object} attr For a description see {@link #table}
 * @returns The rendered table
 * @type String
 * @see #table
 */
helma.Html.prototype.tableAsString = function(headers, data, attr) {
    res.push();
    this.table(headers, data, attr);
    return res.pop();
};

/*********************************************************************/
/*                                                                                         */
/* the following functions should be deliberately altered or removed */
/* (most of these can easily be replaced by the methods they call)    */
/*                                                                                         */
/*********************************************************************/

/**
 * Renders an x/html opening link tag
 * @param {Object} attr An object containing the tag attributes 
 */
helma.Html.prototype.openLink = function(attr) {
    this.openTag("a", attr);
    return;
};

/**
 * Returns an x/html opening link tag
 * @param {Object} attr An object containing the tag attributes 
 * @returns The rendered open link tag
 * @type String
 * @see #openTag
 */
helma.Html.prototype.openLinkAsString = function(attr) {
    return this.openTagAsString("a", attr);
};

/**
 * Renders an x/html closing link tag
 */
helma.Html.prototype.closeLink = function() {
    this.closeTag("a");
    return;
};

/**
 * Returns a rendered x/html closing link tag
 * @returns Rhe rendered closing link tag
 * @type String
 * @see #closeLink
 */
helma.Html.prototype.closeLinkAsString = function() {
    return this.closeTagAsString("a");
};

/**
 * Renders a color definition string. If the string passed as
 * argument contains only hex characters it will be prefixed with a
 * hash sign if necessary, otherwise this method assumes that the
 * value is a named color (eg. "yellow").
 * @param {String} c The color definintion
 * @deprecated
 */
helma.Html.prototype.color = function(c) {
    if (c) {
        var nonhex = /[^0-9,a-f]/gi;
        if (!c.match(nonhex)) {
            c = c.pad("0", 6);
            res.write("#");
        }
    }
    res.write(c);
    return;
};

/**
 * Returns a color definition.
 * @param {String} c The color definintion
 * @returns The rendered color definition
 * @type String
 * @see #color
 * @deprecated
 */
helma.Html.prototype.colorAsString = function(c) {
    res.push();
    this.color(c);
    return res.pop();
};

/**
 * Renders an x/html opening form tag
 * @param {Object} attr An object containing the tag attributes
 */
helma.Html.prototype.form = function(attr) {
    this.openTag("form", attr);
    return;
};

/**
 * Returns an x/html opening form tag
 * @param {Object} attr An object containing the tag attributes
 * @returns The rendered opening form tag
 * @type String
 * @see #form
 */
helma.Html.prototype.formAsString = function(attr) {
    res.push();
    this.form(attr);
    return res.pop();
};

/**
 * Renders an x/html password input tag
 * @param {Object} attr An object containing the tag attributes
 */
helma.Html.prototype.password = function(attr) {
    if (!attr) {
        res.write("[Html.password: insufficient arguments]");
        return;
    }
    attr.type = "password";
    if (!attr.size)
        attr.size = 20;
    this.tag("input", attr);
    return;
};

/**
 * Returns a rendered x/html password input tag
 * @param {Object} attr An object containing the tag attributes
 * @returns The rendered password input tag
 * @type String
 * @see #password
 */
helma.Html.prototype.passwordAsString = function(attr) {
    res.push();
    this.password(attr);
    return res.pop();
};

/**
 * Renders an x/html file input tag
 * @param {Object} attr An object containing the tag attributes
 */
helma.Html.prototype.file = function(attr) {
    if (!attr) {
        res.write("[Html.file: insufficient arguments]");
        return;
    }
    attr.type = "file";
    this.tag("input", attr);
    return;
};

/**
 * Returns a rendered x/html file input tag
 * @param {Object} attr An object containing the tag attributes
 * @returns The rendered file input tag
 * @type String
 * @see #file
 */
helma.Html.prototype.fileAsString = function(attr) {
    res.push();
    this.file(attr);
    return res.pop();
};

/**
 * Parses the string passed as argument and converts any
 * URL in it into a link tag
 * @param {String} str The string wherein URLs should be
 * converted into link tags
 * @returns The string containing URLs converted into link tags
 * @type String
 */
helma.Html.prototype.activateUrls = function(str) {
    var re = /(^|\/>|\s+)([fhtpsr]+:\/\/[^\s]+?)([\.,;:\)\]\"]?)(?=[\s<]|$)/gim;
    var func = function(str, p1, p2, p3) {
        res.push();
        res.write(p1);
        res.write('<a href="');
        res.write(p2);
        res.write('">');
        res.write(p2.clip(50, "...", true));
        res.write('</a>');
        res.write(p3);
        return res.pop();
    };
    return str.replace(re, func);
};

/**
 * Creates a new TableWriter instance
 * @class This class provides various methods for
 * programmatically creating an x/html table.
 * @param {Number} numberOfColumns The number of columns in the table
 * @param {Object} attr An object containing attributes to use when
 * rendering the single table elements. For a description see {@link #table}.
 * @returns An instance of TableWriter
 * @constructor
 */
helma.Html.TableWriter = function(numberOfColumns, attr) {
    if (isNaN(numberOfColumns))
        throw "Illegal argument in TableWriter(): first argument must be a number";
    if (numberOfColumns < 1)
        throw "Illegal argument in TableWriter(): first argument must be > 1";
    /** @private */
    this.ncols = numberOfColumns;
    /** @private */
    this.written = 0;
    // if no attributes object given, create an empty one
    if (!attr)
        attr = {};
    if (!attr.trEven) attr.trEven = attr.tr;
    if (!attr.trOdd)  attr.trOdd = attr.tr;
    if (!attr.trHead) attr.trHead = attr.trEven;
    if (!attr.tdEven) attr.tdEven = attr.td;
    if (!attr.tdOdd)  attr.tdOdd = attr.td;
    if (!attr.thEven) attr.thEven = attr.th;
    if (!attr.thOdd)  attr.thOdd = attr.th;
    /** @private */
    this.attr = attr;

    /**
     * If set to true the first row of the table data is rendered
     * using <code>&lt;th&gt;</code> tags (defaults to false).
     * @type Boolean
     */
    this.writeHeader = false;

    /**
     * If set to true the TableWriter returns the rendered table
     * as string, otherwise the table is written directly to response,
     * which is the default.
     * @type Boolean
     */
    this.writeString = false;

    this.dontEnum("ncols", "written", "attr", "writeHeader", "writeString");

    return this;
};

/** @ignore */
helma.Html.TableWriter.prototype.toString = function() {
    return "[helma.Html.TableWriter]";
}

/**
 * Writes a single table cell to response.
 * @param {String} text The content of the table cess
 * @param {Object} attr An optional object containig attributes
 * to render for this table cell
 */
helma.Html.TableWriter.prototype.write = function(text, attr) {
    // set up some variables
    var isHeaderRow = (this.writeHeader && this.written < this.ncols);
    var isNewRow = (this.written % this.ncols == 0);
    var isEvenRow = ((this.written / this.ncols) % 2 == 0);
    var isEvenCol = ((this.written % this.ncols) % 2 == 0);
    // write out table and table row tags
    if (this.written == 0) {
        if (this.writeString)
            res.push();
        helma.Html.prototype.openTag.call(this, "table", this.attr.table);
        helma.Html.prototype.openTag.call(this, "tr", this.attr.trHead);
    } else if (isNewRow) {
        helma.Html.prototype.closeTag.call(this, "tr");
        if (isEvenRow)
             helma.Html.prototype.openTag.call(this, "tr", this.attr.trEven);
        else
             helma.Html.prototype.openTag.call(this, "tr", this.attr.trOdd);
    }
    // get the attribute object for the table cell
    if (!attr) {
        // no explicit attribute given
        if (isEvenCol) {
            attr = isHeaderRow ? this.attr.thEven : this.attr.tdEven;
        } else {
            attr = isHeaderRow ? this.attr.thOdd : this.attr.tdOdd;
        }
    }
    // write out table cell tag
    helma.Html.prototype.openTag.call(this, isHeaderRow ? "th" : "td", attr);
    // write out table cell contents
    if (text) {
        res.write(text);
    }
    // close table cell
    helma.Html.prototype.closeTag.call(this, isHeaderRow ? "th" : "td");
    if (attr && !isNaN(attr.colspan)) {
        this.written += attr.colspan;
    } else {
        this.written += 1;
    }
    return;
};

/**
 * Closes all open table tags. If {@link #writeString} is set to
 * true, this method returns the rendered table.
 * @returns The rendered table, if {@link #writeString} is set to
 * true, otherwise void.
 * @type String
 */
helma.Html.TableWriter.prototype.close = function() {
    if (this.written > 0) {
        while (this.written++ % this.ncols != 0)
            res.write("<td></td>");
        res.write("</tr></table>");
        this.written = 0;
    }
    if (this.writeString)
        return res.pop();
    return;
};



helma.lib = "Html";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
for (var i in helma[helma.lib].TableWriter.prototype)
    helma[helma.lib].TableWriter.prototype.dontEnum(i);
delete helma.lib;
