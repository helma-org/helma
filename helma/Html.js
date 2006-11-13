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
 * $Author: czv $
 * $Revision: 1.2 $
 * $Date: 2006/04/24 07:02:17 $
 */


// take care of any dependencies
app.addRepository('modules/core/String.js');


if (!global.helma) {
    global.helma = {};
}

helma.Html = function() {
    var self = this;

    /**
     * helper function to render an arbitrary markup part
     * @param String the element's name
     * @param String prefix of each rendered element
     * @param String suffix of each rendered element
     * @param Object optional element's attributes
     */
    var renderMarkupPart = function(name, start, end, attr) {
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
     * helper function used in Html.checkBox
     * and Html.dropDown to check if a current value
     * matches against one or more selected values 
     * @param String the current value
     * @param StringOrArray the selectedValue
     */
    var isSelected = function(value, selectedValue) {
        if (selectedValue == null || value == null)
            return false;
        if (selectedValue instanceof Array)
            return Array.contains(selectedValue, value);
        return value == selectedValue;
    };

    /**
     * return a textual representation of this object
     * @return String
     */
    this.toString = function() {
        return "[Antville Html Library]";
    };

    /**
     * render the opening tag of an arbitrary x/html element
     * @param String the element's name
     * @param Object the element's attributes
     */
    this.openTag = function(name, attr) {
        renderMarkupPart(name, "<", ">", attr);
        return;
    };

    /**
     * return the opening tag of an arbitrary x/html element
     * @see this.openTag
     * @return String the rendered element
     */
    this.openTagAsString = function(name, attr) {
        res.push();
        renderMarkupPart(name, "<", ">", attr);
        return res.pop();
    };

    /**
     * render the closing tag of an arbitrary x/html element
     * directly to response
     * @param String the element's name
     */
    this.closeTag = function(name) {
        renderMarkupPart(name, "</", ">", null);
        return;
    };

    /**
     * return the closing tag of an arbitray x/html element
     * @see this.closeTag
     * @return String the rendered element
     */
    this.closeTagAsString = function(name) {
        res.push();
        renderMarkupPart(name, "</", ">", null);
        return res.pop();
    };

    /**
     * render an empty arbitrary x/html element ("contentless tag")
     * @param String the element's name
     * @param Object the element's attributes
     */
    this.tag = function(name, attr) {
        renderMarkupPart(name, "<", " />", attr);
        return;
    };

    /**
     * return an empty arbitrary x/html element ("contentless tag")
     * @see this.tag
     * @return String the rendered element
     */
    this.tagAsString = function(name, attr) {
        res.push();
        renderMarkupPart(name, "<", " />", attr);
        return res.pop();
    };

    /**
     * render an arbitrary x/html element
     * @param String the element's name
     * @param String the element's content
     * @param Object the element's attributes
     */
    this.element = function(name, str, attr) {
        renderMarkupPart(name, "<", ">", attr);
        res.write(str);
        renderMarkupPart(name, "</", ">");
        return;
    };

    /**
     * return an arbitrary x/html element
     * @see this.element
     * @param String the rendered element
     */
    this.elementAsString = function(name, str, attr) {
        res.push();
        self.element(name, str, attr);
        return res.pop();
    };

    /**
     * render an x/html link element
     * @param Object the element's attributes
     * @param String the element's content
     */
    this.link = function(attr, text) {
        if (!attr) {
            res.write("[Html.link: insufficient arguments]");
            return;
        }
        self.openTag("a", attr);
        res.write(text);
        self.closeTag("a");
        return;
    };

    /**
     * return an x/html link element
     * @see this.link
     * @return String the rendered element
     */
    this.linkAsString = function(attr, text) {
        res.push();
        self.link(attr, text);
        return res.pop();
    };

    /**
     * render an x/html hidden input element
     * @param Object the element's attributes
     */
    this.hidden = function(param) {
        if (!param) {
            res.write("[Html.hidden: insufficient arguments]");
            return;
        }
        var attr = Object.clone(param);
        attr.type = "hidden";
        attr.value = (attr.value != null) ? encodeForm(attr.value) : "";
        self.tag("input", attr);
        return;
    };

    /**
     * return an x/html hidden input element
     * @see this.hidden
     * @return String the rendered element
     */
    this.hiddenAsString = function(attr) {
        res.push();
        self.hidden(attr);
        return res.pop();
    };

    /**
     * render an x/html text input element
     * @param Object the element's attributes
     */
    this.input = function(param) {
        if (!param) {
            res.write("[Html.input: insufficient arguments]");
            return;
        }
        var attr = Object.clone(param);
        attr.type = "text";
        if (!attr.size)
            attr.size = 20;
        attr.value = (attr.value != null) ? encodeForm(attr.value) : "";
        self.tag("input", attr);
        return;
    };

    /**
     * return an x/html text input element
     * @see this.input
     * @return String the rendered element
     */
    this.inputAsString = function(attr) {
        res.push();
        self.input(attr);
        return res.pop();
    };

    /**
     * render an x/html textarea element
     * @param Object the element's attributes
     */
    this.textArea = function(param) {
        if (!param) {
            res.write("[Html.textArea: insufficient arguments]");
            return;
        }
        var attr = Object.clone(param);
        var value = (attr.value != null) ? encodeForm(attr.value) : "";
        delete attr.value;
        self.openTag("textarea", attr);
        res.write(value);
        self.closeTag("textarea");
        return;
    };

    /**
     * return an x/html textarea element
     * @see this.textArea
     * @return String the rendered element
     */
    this.textAreaAsString = function(attr) {
        res.push();
        self.textArea(attr);
        return res.pop();
    };

    /**
     * render an x/html checkbox input element
     * @param Object the element's attributes
     */
    this.checkBox = function(param) {
        if (!param) {
            res.write("[Html.checkBox: insufficient arguments]");
            return;
        }
        var attr = Object.clone(param);
        attr.type = "checkbox";
        if (attr.selectedValue != null) {
            if (isSelected(param.value, param.selectedValue))
                attr.checked = "checked";
            else
                delete attr.checked;
            delete attr.selectedValue;
        }
        self.tag("input", attr);
        return;
    };

    /**
     * return an x/html checkbox input element
     * @see this.checkBox
     * @return String the rendered element
     */
    this.checkBoxAsString = function(attr) {
        res.push();
        self.checkBox(attr);
        return res.pop();
    };

    /**
     * render an x/html radiobutton input element
     * @param Object the element's attributes
     */
    this.radioButton = function(param) {
        if (!param) {
            res.write("[Html.radioButton: insufficient arguments]");
            return;
        }
        var attr = Object.clone(param);
        attr.type = "radio";
        if (attr.selectedValue != null) {
            if (attr.value == attr.selectedValue)
                attr.checked = "checked";
            else
                delete attr.checked;
            delete attr.selectedValue;
        }
        self.tag("input", attr);
        return;
    };

    /**
     * return an x/html radiobutton input element
     * @see this.radioButton
     * @return String the rendered element
     */
    this.radioButtonAsString = function(attr) {
        res.push();
        self.radioButton(attr);
        return res.pop();
    };

    /**
     * render an x/html submit input element
     * @param Object the element's attributes
     */
    this.submit = function(param) {
        if (!param) {
            res.write("[Html.submit: insufficient arguments]");
            return;
        }
        var attr = Object.clone(param);
        attr.type = "submit";
        if (!attr.name)
            attr.name = attr.type;
        attr.value = (attr.value != null) ? encodeForm(attr.value) : attr.type;
        self.tag("input", attr);
        return;  
    };

    /**
     * return an x/html submit input element
     * @see this.submit
     * @return String the rendered element
     */
    this.submitAsString = function(attr) {
        res.push();
        self.submit(attr);
        return res.pop();
    };

    /**
     * render an x/html button input element
     * @param Object the element's attributes
     */
    this.button = function(param) {
        if (!param) {
            res.write("[Html.button: insufficient arguments]");
            return;
        }
        var attr = Object.clone(param);
        attr.type = "button";
        if (!attr.name)
            attr.name = attr.type;
        attr.value = (attr.value != null) ? encodeForm(attr.value) : attr.type;
        self.tag("input", attr);
        return;  
    };

    /**
     * return an x/html button input element
     * @see this.button
     * @return String the rendered element
     */
    this.buttonAsString = function(attr) {
        res.push();
        self.button(attr);
        return res.pop();
    };

    /**
     * render a x/html drop down box
     * @param Object the element's attributes
     * @param Array either an array of strings, an array  with 
     *     several {value: v, display: d} objects, or a collection 
     *     of ["value", "display"] arrays in an array
     * @param String the currently selected value
     * @param String the first option (without a value)
     */
    this.dropDown = function(param, options, selectedValue, firstOption) {
        if (!param) {
            res.write("[Html.dropDown: insufficient arguments]");
            return;
        }
        var attr = Object.clone(param);
        if (!attr.size)
            attr.size = 1;
        self.openTag("select", attr);
        res.write("\n ");
        if (firstOption) {
            self.openTag("option", {value: ""});
            res.write(firstOption);
            self.closeTag("option");
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
            if (isSelected(attr.value, selectedValue))
                attr.selected = "selected";
            self.openTag("option", attr);
            res.write(display);
            self.closeTag("option");
            res.write("\n ");
        }
        self.closeTag("select");
        res.write("\n ");
        return;
    };

    /**
     * return an x/html drop down box
     * @see this.dropDown
     * @return String the rendered drop down box
     */
    this.dropDownAsString = function(attr, options, selectedValue, firstOption) {
        res.push();
        self.dropDown(attr, options, selectedValue, firstOption);
        return res.pop();
    };

    /**
     *  Renders an image map from an array
     *  each array contains an object describing the parameters
     *  for the area of the image map
     *  @param String name of the image map
     *  @param String array of image map objects
     */
    this.map = function(name, param) {
        self.openTag("map", {name: name});
        var areas = Object.clone(param);
        for (var i in areas) {
            if (!areas[i].alt)
                areas[i].alt = "";
            if (!areas[i].shape)
                areas[i].shape = "rect";
            self.openTag("area", areas[i]);
        }
        self.closeTag("map");
        return;
    };

    /**
     * @see Html.map
     * @return the rendered element
     */
    this.mapAsString = function(name, areas) {
        res.push();
        self.map(name, areas);
        return res.pop();
    };

    /**
     * render a complete x/html table
     * @param Array an array containing table headers
     * @param Array a 2 dimensional array containing the table data
     * @param Object the table's, its rows' and cells' attributes
     */
    this.table = function(headers, data, param) {
        if (!param) {
            res.write("[Html.table: insufficient arguments]");
            return;
        }
        var attr = Object.clone(param);
        if (!attr.trHead) attr.trHead = attr.tr;
        if (!attr.trEven) attr.trEven = attr.tr;
        if (!attr.trOdd)  attr.trOdd = attr.tr;
        if (!attr.tdEven) attr.tdEven = attr.td;
        if (!attr.tdOdd)  attr.tdOdd = attr.td;
        if (!attr.thEven) attr.thEven = attr.th;
        if (!attr.thOdd)  attr.thOdd = attr.th;
        self.openTag("table", attr.table);
        if (headers) {
            self.openTag("tr", attr.trHead);
            for (var i in headers) {
                var evenOdd = i % 2 == 0 ? "Even" : "Odd";
                self.openTag("th", attr["th"+evenOdd]);
                res.write(headers[i]);
                self.closeTag("th");
            }
            self.closeTag("tr");
        }
        for (var i in data) {
            var evenOdd = i % 2 == 0 ? "Even" : "Odd";
            self.openTag("tr", attr["tr"+evenOdd]);
            for (var j in data[i]) {
                var evenOddCell = j % 2 == 0 ? "Even" : "Odd";
                self.openTag("td", attr["td"+evenOddCell]);
                res.write(data[i][j]);
                self.closeTag("td");
            }
            self.closeTag("tr");
        }
        self.closeTag("table");
    };

    /**
     * return a complete x/html table
     * @see this.table
     * @return String the rendered table
     */
    this.tableAsString = function(headers, data, attr) {
        res.push();
        self.table(headers, data, attr);
        return res.pop();
    };
    
    /**
     * utility object to provide an easy way
     * for programmatically creating an x/html table.
     * @param Number the number of columns in the table
     * @param Object the table's, its rows' and cells' attributes
     * @return Object an instance of TableWriter
     */
    this.TableWriter = function(numberOfColumns, attr) {
        this.ncols = numberOfColumns;
        if (isNaN(this.ncols))
            throw "Illegal argument in TableWriter(): first argument must be a number";
        if (this.ncols < 1)
            throw "Illegal argument in TableWriter(): first argument must be > 1";
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
        this.attr = attr;

        // write header row? set to true to use "th" tags for first row
        this.writeHeader = false;
        // write to string buffer rather than response?
        this.writeString = false;

        /**
         * Write a table cell.
         * @param String the table cell content as text
         * @param attr an optional attributes holder for the td tag
         */
        this.write = function(text, attr) {
            // set up some variables
            var isHeaderRow = (this.writeHeader && this.written < this.ncols);
            var isNewRow = (this.written % this.ncols == 0);
            var isEvenRow = ((this.written / this.ncols) % 2 == 0);
            var isEvenCol = ((this.written % this.ncols) % 2 == 0);
            // write out table and table row tags
            if (this.written == 0) {
                if (this.writeString)
                    res.push();
                self.openTag("table", this.attr.table);
                self.openTag("tr", this.attr.trHead);
            } else if (isNewRow) {
                self.closeTag("tr");
                if (isEvenRow)
                     self.openTag("tr", this.attr.trEven);
                else
                     self.openTag("tr", this.attr.trOdd);
            }
            // get the attribute object for the table cell
            if (!attr) {
                // no explicit attribute given
                if (isEvenCol)
                    attr = isHeaderRow ? this.attr.thEven : this.attr.tdEven;
                else
                    attr = isHeaderRow ? this.attr.thOdd : this.attr.tdOdd;
            }
            // write out table cell tag
            self.openTag(isHeaderRow ? "th" : "td", attr);
            // write out table cell contents
            if (text)
                res.write(text);
            // close table cell
            self.closeTag(isHeaderRow ? "th" : "td");
            if (attr && !isNaN(attr.colspan))
                this.written += attr.colspan;
            else
                this.written += 1;
            return;
        };

        /**
         * Close all open table tags.
         */
        this.close = function() {
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

        this.dontEnum("ncols", "written", "attr", "writeHeader");
        this.dontEnum("writeString", "write", "close");

        return this;
    };

/*********************************************************************/
/*                                                                                         */
/* the following functions should be deliberately altered or removed */
/* (most of these can easily be replaced by the methods they call)    */
/*                                                                                         */
/*********************************************************************/

    /**
     * render an x/html open link tag
     * @param Object the element's attributes 
     */
    this.openLink = function(attr) {
        self.openTag("a", attr);
        return;
    };

    /**
     * return an x/html open link tag
     * @see this.openTag
     * @return String the rendered element
     */
    this.openLinkAsString = function(attr) {
        return self.openTagAsString("a", attr);
    };

    /**
     * render an x/html close link tag
     */
    this.closeLink = function() {
        self.closeTag("a");
        return;
    };

    /**
     * return an x/html close link tag
     * @see this.closeLink
     * @return String the rendered element
     */
    this.closeLinkAsString = function() {
        return self.closeTagAsString("a");
    };

    /**
     * render a color string 
     * if it contains hex characters only prefix it with "#"
     * otherwise assume that it is a named color (eg. "yellow")
     * @param String the color string
     * @deprecated
     */
    this.color = function(c) {
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
     * return a color string
     * @see this.color
     * @return String the resulting color string
     * @deprecated
     */
    this.colorAsString = function(c) {
        res.push();
        return self.color(c);
        return res.pop();
    };

    /**
     * render an x/html open form tag
     * @param Object the element's attributes
     */
    this.form = function(attr) {
        self.openTag("form", attr);
        return;
    };

    /**
     * return an x/html open form tag
     * @see this.form
     * @return String the rendered element
     */
    this.formAsString = function(attr) {
        res.push();
        self.form(attr);
        return res.pop();
    };

    /**
     * render an x/html password input element
     * @param Object the element's attributes
     */
    this.password = function(attr) {
        if (!attr) {
            res.write("[Html.password: insufficient arguments]");
            return;
        }
        attr.type = "password";
        if (!attr.size)
            attr.size = 20;
        self.tag("input", attr);
        return;
    };

    /**
     * render an x/html password input element
     * @see this.password
     * @return String the rendered element
     */
    this.passwordAsString = function(attr) {
        res.push();
        self.password(attr);
        return res.pop();
    };

    /**
     * render an x/html file input element
     * @param Object the element's attributes
     */
    this.file = function(attr) {
        if (!attr) {
            res.write("[Html.file: insufficient arguments]");
            return;
        }
        attr.type = "file";
        self.tag("input", attr);
        return;
    };

    /**
     * return an x/html file input element
     * @see this.file
     * @return String the rendered element
     */
    this.fileAsString = function(attr) {
        res.push();
        self.file(attr);
        return res.pop();
    };

    for (var i in this)
        this.dontEnum(i);

    return this;
};


helma.Html.toString = function() {
    return "[helma.Html]";
};


/**
 * function parses a string and converts any
 * found URL into a HTML link tag
 * @return String resulting string with activated URLs
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


helma.lib = "Html";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
