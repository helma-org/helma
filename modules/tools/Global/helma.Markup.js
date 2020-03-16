/*
*   Copyright (C) 2004 Hannes Wallnoefer
*/

////////////////////////////////////////////////////////////////////////
// Html element functions
////////////////////////////////////////////////////////////////////////

if (!global.helma) {
    global.helma = {};
}

helma.Markup = {};

helma.Markup.element = function(name, attributes, content) {
    if (!name) {
        throw "helma.Markup.element called without element name";
    }
    // open tag
    res.write("<");
    res.write(name);
    if (attributes) {
        for (var i in attributes) {
            if (typeof(attributes[i]) == "undefined")
                continue;
            res.write(" ");
            res.write(i);
            res.write("=\"");
            res.write(encodeForm(attributes[i]));
            res.write("\"");
        }
    }
    // if no child objects create empty element and return
    if (typeof(content) == "undefined") {
        res.write(" />");
        return;
    }
    res.write(">");

    // write content
    res.write(content);

    // close tag
    res.write("</");
    res.write(name);
    res.write(">");
}

helma.Markup.Element = function(name, attributes, children) {
    return new MarkupElement(name, attributes, children);
}

helma.Markup.form = function(attributes, content) {
    this.element("form", attributes, content);
}

helma.Markup.Form = function(attributes, children) {
    return new MarkupElement("form", attributes, children);
}

helma.Markup.textarea = function(attributes, content) {
    this.element("textarea", attributes, encodeForm(content));
}

helma.Markup.Textarea = function(attributes, children) {
    return new HtmlTextarea(attributes, children);
}

helma.Markup.input = function(attributes, content) {
    this.element("input", attributes, content);
}

helma.Markup.Input = function(attributes, children) {
    return new MarkupElement("input", attributes, children);
}

helma.Markup.button = function(attributes, content) {
    if (!attributes)
        attributes = {};
    attributes.type = "button";
    this.element("input", attributes, content);
}

helma.Markup.Button = function(attributes, children) {
    if (!attributes)
        attributes = {};
    attributes.type = "button";
    return new MarkupElement("input", attributes, children);
}

helma.Markup.submit = function(attributes, content) {
    if (!attributes)
        attributes = {};
    attributes.type = "submit";
    this.element("input", attributes, content);
}

helma.Markup.Submit = function(attributes, children) {
    if (!attributes)
        attributes = {};
    attributes.type = "submit";
    return new MarkupElement("input", attributes, children);
}

helma.Markup.hidden = function(attributes, content) {
    if (!attributes)
        attributes = {};
    attributes.type = "hidden";
    this.element("input", attributes, content);
}

helma.Markup.Hidden = function(attributes, children) {
    if (!attributes)
        attributes = {};
    attributes.type = "hidden";
    return new MarkupElement("input", attributes, children);
}

helma.Markup.file = function(attributes, content) {
    if (!attributes)
        attributes = {};
    attributes.type = "file";
    this.element("input", attributes, content);
}

helma.Markup.File = function(attributes, children) {
    if (!attributes)
        attributes = {};
    attributes.type = "file";
    return new MarkupElement("input", attributes, children);
}

helma.Markup.password = function(attributes, content) {
    if (!attributes)
        attributes = {};
    attributes.type = "password";
    this.element("input", attributes, content);
}

helma.Markup.Password = function(attributes, children) {
    if (!attributes)
        attributes = {};
    attributes.type = "password";
    return new MarkupElement("input", attributes, children);
}

helma.Markup.checkbox = function(attributes, content) {
    if (!attributes)
        attributes = {};
    attributes.type = "checkbox";
    if (!attributes.checked)
        delete(attributes.checked);
    this.element("input", attributes, content);
}

helma.Markup.Checkbox = function(attributes, children) {
    if (!attributes)
        attributes = {};
    attributes.type = "checkbox";
    if (!attributes.checked)
        delete(attributes.checked);
    return new MarkupElement("input", attributes, children);
}

helma.Markup.select = function(attributes, children, selected, firstLine) {
    res.write(new HtmlSelect(attributes, children, selected, firstLine));
}

helma.Markup.Select = function(attributes, children, selected, firstLine) {
    return new HtmlSelect(attributes, children, selected, firstLine);
}

helma.Markup.head = function(attributes, content) {
    this.element("head", attributes, content);
}

helma.Markup.Head = function(attributes, children) {
    return new MarkupElement("head", attributes, children);
}

helma.Markup.title = function(attributes, content) {
    this.element("title", attributes, content);
}

helma.Markup.Title = function(attributes, children) {
    return new MarkupElement("title", attributes, children);
}

helma.Markup.body = function(attributes, content) {
    this.element("body", attributes, content);
}

helma.Markup.Body = function(attributes, children) {
    return new MarkupElement("body", attributes, children);
}

helma.Markup.div = function(attributes, content) {
    this.element("div", attributes, content);
}

helma.Markup.Div = function(attributes, children) {
    return new MarkupElement("div", attributes, children);
}

helma.Markup.p = function(attributes, content) {
    this.element("p", attributes, content);
}

helma.Markup.P = function(attributes, children) {
    return new MarkupElement("p", attributes, children);
}

helma.Markup.b = function(attributes, content) {
    this.element("b", attributes, content);
}

helma.Markup.B = function(attributes, children) {
    return new MarkupElement("b", attributes, children);
}

helma.Markup.span = function(attributes, content) {
    this.element("span", attributes, content);
}

helma.Markup.Span = function(attributes, children) {
    return new MarkupElement("span", attributes, children);
}

helma.Markup.img = function(attributes) {
    this.element("img", attributes);
}

helma.Markup.Img = function(attributes) {
    return new MarkupElement("img", attributes);
}

helma.Markup.script = function(attributes, content) {
    this.element("script", attributes, content);
}

helma.Markup.Script = function(attributes, children) {
    return new MarkupElement("script", attributes, children);
}

helma.Markup.ul = function(attributes, content) {
    this.element("ul", attributes, content);
}

helma.Markup.Ul = function(attributes, children) {
    return new MarkupElement("ul", attributes, children);
}

helma.Markup.ol = function(attributes, content) {
    this.element("ol", attributes, content);
}

helma.Markup.Ol = function(attributes, children) {
    return new MarkupElement("ol", attributes, children);
}

helma.Markup.li = function(attributes, content) {
    this.element("li", attributes, content);
}

helma.Markup.Li = function(attributes, children) {
    return new MarkupElement("li", attributes, children);
}


helma.Markup.a = function(attributes, content) {
    this.element("a", attributes, content);
}

helma.Markup.link = helma.Markup.a;

helma.Markup.A = function(attributes, children) {
    return new MarkupElement("a", attributes, children);
}

helma.Markup.table = function(attributes, content) {
    this.element("table", attributes, content);
}

helma.Markup.Table = function(attributes, children) {
    return new MarkupElement("table", attributes, children);
}

helma.Markup.Colgroup = function(attributes, children) {
    return new MarkupElement("colgroup", attributes, children);
}

helma.Markup.colgroup = function(attributes, content) {
    this.element("colgroup", attributes, content);
}

helma.Markup.Col = function(attributes, children) {
    return new MarkupElement("col", attributes, children);
}

helma.Markup.col = function(attributes, content) {
    this.element("col", attributes, content);
}

helma.Markup.tr = function(attributes, content) {
    this.element("tr", attributes, content);
}

helma.Markup.Tr = function(attributes, children) {
    return new MarkupElement("tr", attributes, children);
}

helma.Markup.th = function(attributes, content) {
    this.element("th", attributes, content);
}

helma.Markup.Th = function(attributes, children) {
    return new MarkupElement("th", attributes, children);
}

helma.Markup.td = function(attributes, content) {
    this.element("td", attributes, content);
}

helma.Markup.Td = function(attributes, children) {
    return new MarkupElement("td", attributes, children);
}

helma.Markup.h1 = function(attributes, content) {
    this.element("h1", attributes, content);
}

helma.Markup.H1 = function(attributes, children) {
    return new MarkupElement("h1", attributes, children);
}

helma.Markup.h2 = function(attributes, content) {
    this.element("h2", attributes, content);
}

helma.Markup.H2 = function(attributes, children) {
    return new MarkupElement("h2", attributes, children);
}

helma.Markup.h3 = function(attributes, content) {
    this.element("h3", attributes, content);
}

helma.Markup.H3 = function(attributes, children) {
    return new MarkupElement("h3", attributes, children);
}

helma.Markup.h4 = function(attributes, content) {
    this.element("h4", attributes, content);
}

helma.Markup.H4 = function(attributes, children) {
    return new MarkupElement("h4", attributes, children);
}

helma.Markup.h5 = function(attributes, content) {
    this.element("h5", attributes, content);
}

helma.Markup.H5 = function(attributes, children) {
    return new MarkupElement("h5", attributes, children);
}

helma.Markup.h6 = function(attributes, content) {
    this.element("h6", attributes, content);
}

helma.Markup.H6 = function(attributes, children) {
    return new MarkupElement("h6", attributes, children);
}

helma.Markup.pre = function(attributes, content) {
    this.element("pre", attributes, content);
}

helma.Markup.Pre = function(attributes, children) {
    return new MarkupElement("pre", attributes, children);
}

helma.Markup.br = function(attributes) {
    this.element("br", attributes);
}

helma.Markup.Br = function(attributes, children) {
    return new MarkupElement("br", attributes, children);
}

helma.Markup.openTag = function(name, attributes) {
    if (!name) {
        throw "helma.Markup.openTag called without element name";
    }
    res.write("<");
    res.write(name);
    if (attributes) {
        for (var i in attributes) {
            if (typeof(attributes[i]) == "undefined")
                continue;
            res.write(" ");
            res.write(i);
            res.write("=\"");
            res.write(encodeForm(attributes[i]));
            res.write("\"");
        }
    }
    res.write(">");
}

helma.Markup.closeTag = function(name) {
    res.write("</");
    res.write(name);
    res.write(">");
}


/**
 * utility object to provide an easy way
 * for programmatically creating an x/html table.
 * @param Number the number of columns in the table
 * @param Object the table's, its rows' and cells' attributes
 * @return Object an instance of TableWriter
 */
helma.Markup.TableWriter = function(numberOfColumns, attr) {
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
            helma.Markup.openTag("table", this.attr.table);
            helma.Markup.openTag("tr", this.attr.trHead);
        } else if (isNewRow) {
            helma.Markup.closeTag("tr");
            if (isEvenRow)
                 helma.Markup.openTag("tr", this.attr.trEven);
            else
                 helma.Markup.openTag("tr", this.attr.trOdd);
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
        helma.Markup.openTag(isHeaderRow ? "th" : "td", attr);
        // write out table cell contents
        if (text)
            res.write(text);
        // close table cell
        helma.Markup.closeTag(isHeaderRow ? "th" : "td");
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
    return this;
}


////////////////////////////////////////////////////////////////////////
// MarkupElement is the base class for all elements
////////////////////////////////////////////////////////////////////////

/**
 * Element constructor. Takes a name,
 * a map of attributes and an array of child
 * elements as arguments.
 */
function MarkupElement(name, attributes, children) {
    if (!attributes)
        attributes = {};
    this.attr = attributes;
    // if (name && !this._elementName) {
    this._elementName = name;
    // } else {
    if (attributes && attributes.name) {
        this.name = attributes.name;
        // this.attr.name = name;
    }

    this.map = {};

    this.add(children);

    this.initValueProperty();
}

/**
 * Add a new child element
 */
MarkupElement.prototype.add = function(child) {
    if (typeof(child) == "undefined") {
        return;
    }
    // initialize child array if necessary
    if (!this.children) {
        this.children = [];
    }
    if (child instanceof Array) {
        for (var i in child) {
            this.add(child[i]);
        }
        return;
    }
    // add new child
    this.children.push(child);
    // register child if it has a name property
    if (child) {
        if (child.name && !this.map[child.name]) {
            this.map[child.name] = child;
        }
        // register grandchilds unless the name slot is already taken
        for (var i in child.map) {
            var c = child.map[i];
            if (c && c.name && !this.map[c.name]) {
                this.map[c.name] = c;
            }
        }
        // set parent property in child
        child.parent = this;
    }
}

MarkupElement.prototype.firstChild = function() {
    return this.children ? this.children[0] : undefined;
}

MarkupElement.prototype.lastChild = function() {
    return this.children ? this.children[this.children.length - 1] : undefined;
}


/**
 * Render the element to the response buffer.
 */
MarkupElement.prototype.render = function(writer) {
    if (!writer)
        writer = res;

    this.processValueProperty();
    // open tag
    if (this._elementName) {
        writer.write("<");
        writer.write(this._elementName);
        for (var i in this.attr) {
            if (typeof(this.attr[i]) == "undefined")
                continue;
            writer.write(" ");
            writer.write(i);
            writer.write("=\"");
            writer.write(encodeForm(this.attr[i]));
            writer.write("\"");
        }
        // render type attribute if set
        if (this._type) {
            writer.write(" type=\"");
            writer.write(this._type);
            writer.write("\"");
        }
        // if no child objects create empty element and return
        if (typeof(this.children) == "undefined") {
            writer.write(" />");
            return;
        }
        writer.write(">");
    }

    // write child elements
    if (typeof(this.children) != "undefined") {
        if (this.children instanceof Array) {
            for (var i in this.children) {
                if (typeof(this.children[i]) instanceof MarkupElement) {
                    this.children[i].render();
                } else if (this.children[i]) {
                    writer.write(this.children[i]);
                }
            }
        } else {
            writer.write(this.children);
        }
    }

    // close tag
    if (this._elementName) {
        writer.write("</");
        writer.write(this._elementName);
        writer.write(">");
    }
}

/**
 * Return an object containing the rendered child elements
 * of this element keyed by element name. This is suitable
 * for rendering the elements of a markup object through
 * a skin, using the object returned by this function as macro
 * handler.
 */
MarkupElement.prototype.renderMap = function() {
    var map = {};
    if (this.children && typeof(this.children) == "object") {
        for (var i in this.children) {
            if (typeof(this.children[i]) == "object") {
                var comp = this.children[i];
                map[comp.name] = comp.toString();
            }
        }
    }
    return map;
}

/**
 * Return an array containing the rendered child elements
 * of this element keyed index position. This is suitable
 * for those cases where we want to print out a markup
 * object's elements programmatically.
 */
MarkupElement.prototype.renderArray = function() {
    var list = [];
    if (this.children && typeof(this.children) == "object") {
        for (var i in this.children) {
            if (typeof(this.children[i]) == "object") {
                var comp = this.children[i];
                list.push(comp.toString());
            }
        }
    }
    return list;
}

/**
 * Render the element to a string.
 */
MarkupElement.prototype.toString = function() {
    res.push();
    this.render(res);
    return res.pop();
}

/**
 * Recursively populate this object and its child objects,
 * reading values from the argument object.
 */
MarkupElement.prototype.populate = function(obj) {
    // if no object passed populate from req.data
    if (!obj)
        obj = req.data;

    // set value
    if (this.name && this._type != "submit")
        this.value = obj[this.name];

    // populate named child elements
    for (var i in this.map) {
        if (typeof(this.map[i]) == "object" && this.map[i].populate) {
            this.map[i].populate(obj);
        }
    }
}

/**
 * Recursively validate this element and its child elements.
 */
MarkupElement.prototype.validate = function() {
    // apply constraints
    if (this.constraints) {
        for (var i in this.constraints) {
            this.contstraints[i].apply(this);
        }
    }

    // validate child elements
    for (var i in this.map) {
        if (typeof(this.map[i]) == "object" && this.map[i].validate) {
            this.map[i].validate();
        }
    }
}

/**
 * Set up this Element's value property.
 */
MarkupElement.prototype.initValueProperty = function() {
    this.value = this.attr.value;
}

/**
 * Process this Element's value property.
 */
MarkupElement.prototype.processValueProperty = function() {
    this.attr.value = this.value;
}

////////////////////////////////////////////////////////////////////////
// MarkupElement subclasses for Html form elements.
////////////////////////////////////////////////////////////////////////


/**
 * Html textarea
 */
function HtmlTextarea(attributes, children) {
    this.constructor("textarea", attributes, children);
}
HtmlTextarea.prototype = new MarkupElement("textarea");

/**
 * Set up this Textarea's value property.
 */
HtmlTextarea.prototype.initValueProperty = function() {
    if (typeof(this.attr.value) != "undefined")
        this.value = this.attr.value;
    else if (this.children && this.children.length > 0)
        this.value = this.children[0];
}

/**
 * Process this Textarea's value property.
 */
HtmlTextarea.prototype.processValueProperty = function() {
    this.children = [encodeForm(this.value)];
}

/**
 * Select list
 */
function HtmlSelect(attributes, children, selectedValue, firstLine) {
    var options = [];
    if (firstLine)
        options.push(new MarkupElement("option", {value: ""}, ""));
    if (children instanceof Array) {
        for (var i in children) {
            var child = children[i];
            var value, display;
            if (child instanceof Array && child.length == 2) {
                value = child[0];
                display = child[1];
            } else if (child.value != null && child.display != null) {
                value = child.value;
                display = child.display;
            } else {
                display = child;
                value = i;
            }
            var attr = {value: value};
            if (value == selectedValue)
                attr.selected = "selected";
            options.push(new MarkupElement("option", attr, display));
        }
    }
    this.constructor("select", attributes, options);
}
HtmlSelect.prototype = new MarkupElement("select");

