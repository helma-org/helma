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
 * $RCSfile: helma.Inspector.js,v $
 * $Author: czv $
 * $Revision: 1.5 $
 * $Date: 2006/04/24 11:12:40 $
 */

// take care of any dependencies
app.addRepository('modules/core/String.js');
app.addRepository('modules/core/Number.js');
app.addRepository('modules/helma/Html.js');

if (!global.helma) {
    global.helma = {};
}

helma.Inspector = function(hopObj) {
    if (!hopObj)
        hopObj == root;

    var version = "4.0";
    var children = [];
    var properties = [];
    var collections = [];

    var html = new helma.Html();
    var keySorter = new String.Sorter("key");
    var idSorter = new Number.Sorter("id");

    var skins = {
        child: createSkin(helma.Inspector.child_skin),
        collection: createSkin(helma.Inspector.collection_skin),
        date: createSkin(helma.Inspector.date_skin),
        editor: createSkin(helma.Inspector.editor_skin),
        property: createSkin(helma.Inspector.property_skin)
    };

    var genOptions = function(start, end, selected) {
        res.push();
        for (var i=start; i<=end; i+=1) {
            res.write("<option");
            if (i == selected)
                res.write(' selected="selected"');
            res.write(">");
            if (i < 10)
                res.write("0");
            res.write(i);
            res.write("</option>\n");
        }
        return res.pop();
    };

    this.render = function() {
        for (var i in hopObj) {
            var obj = {
                key: i,
                value: hopObj[i]
            };
            if (hopObj[i]) {
                if (obj.value._prototype &&
                     obj.value._prototype == "HopObject")
                    collections.push(obj);
                else
                    properties.push(obj);
            }
        }
        properties.sort(keySorter);
        collections.sort(keySorter);

        var n = hopObj.size();
        for (var i=0; i<n; i++) {
            var h = hopObj.get(i);
            var obj = {
                key: i,
                value: h,
                id: h._id
            };
            children.push(obj);
        }
        children.sort(idSorter);

        res.handlers.inspector = this;
        renderSkin("helma.Inspector.main");
        return;
    };

    this.action = function() {
        if (req.data.done) {
            helma.invalidate('inspector');
            res.redirect(path.href());
        }
        if (!helma.auth('inspector'))
            res.abort();
        if (req.data.cancel)
            res.redirect(req.action);
        else if (req.data.save) {
            if (req.data.type == "date") {
                hopObj[req.data.key] = new Date(
                    req.data.y, req.data.M-1, req.data.d,
                    req.data.H, req.data.m, req.data.s
                );
            } else
                hopObj[req.data.key] = req.data[req.data.key];
            res.redirect(req.action);
        };
        this.render();
    };


    /*****    M A C R O S    *****/

    this.version_macro = function() {
        return version;
    };

    this.title_macro = function() {
        var str = String(hopObj);
        res.write(hopObj);
        return;
    };

    this.prototype_macro = function() {
        if (hopObj != root)
            res.write(hopObj._prototype);
        return;
    };

    this.this_macro = function() {
        res.write(hopObj);
        return;
    };

    this.parent_macro = function() {
        res.write(hopObj._parent);
        return;
    };

    this.href_macro = function() {
        res.write(hopObj.href(req.action));
        return;
    };

    this.path_macro = function() {
        for (var i=0; i<path.length-1; i++) {
            var obj = path[i];
            html.link({href: obj.href(req.action)},
                (obj == root ? obj._prototype : obj));
            res.write(" / ");
        }
        return;
    };

    this.collections_macro = function() {
        for (var i in collections) {
            var obj = collections[i];
            var param = {
                href: obj.value.href(req.action),
                key: obj.key,
                size: obj.value.size()
            };
            renderSkin(skins.collection, param);
        }
        return;
    };

    this.childProto_macro = function() {
        var first = hopObj.get(0);
        if (first)
            res.write(first._prototype);
        return;
    };

    this.properties_macro = function() {
        if (hopObj._id) {
            renderSkin(skins.property, {
                title: "_id", value: hopObj._id
            });
            renderSkin(skins.property, {
                title: "_prototype", value: hopObj._prototype
            });
        }

        if (hopObj._parent) {
            renderSkin(skins.property, {
                title: html.linkAsString({
                    href: hopObj._parent.href(req.action)
                }, "_parent"),
                value: hopObj._parent
            });
        }

        for (var i in properties) {
            var obj = properties[i];
            if (obj.value instanceof Date) {
                var param = {value: obj.value.toString()};
                param.title = html.linkAsString({
                    href: "javascript:void(null)",
                    onclick: "toggleEditor(this)"
                }, obj.key);
                param.key = obj.key;
                param.years = genOptions(1970, 2070, obj.value.getFullYear());
                param.months = genOptions(1, 12, obj.value.getMonth()+1);
                param.days = genOptions(1, 31, obj.value.getDate());
                param.hours = genOptions(0, 23, obj.value.getHours());
                param.minutes = genOptions(0, 59, obj.value.getMinutes());
                param.seconds = genOptions(0, 59, obj.value.getSeconds());
                renderSkin(skins.date, param);
            } else {
                var param = {value: obj.value};
                if (obj.value.href) {
                    param.title = html.linkAsString({href: obj.value.href(req.action)}, obj.key);
                    renderSkin(skins.property, param);
                } else {
                    param.title = html.linkAsString({
                        href: "javascript:void(null)",
                        onclick: "toggleEditor(this)"
                    }, obj.key);
                    param.key = obj.key;
                    renderSkin(skins.editor, param);
                }
            }
        }

        return;
    };

    this.children_macro = function() {
        for (var i in children) {
            var obj = children[i];
            renderSkin(skins.child, {
                href: obj.value.href(req.action),
                //prototype: obj.value._prototype,
                title: obj.value
            });
        }
        return;
    };

    for (var i in this)
        this.dontEnum(i);

    return this;
};


/*****    S K I N S    *****/

helma.Inspector.child_skin = '<tr>\
<td><a href="<% param.href %>"><% param.prototype suffix="&nbsp;" %><% param.title %></a></td>\
</tr>';

helma.Inspector.collection_skin = '<tr>\
<td><a href="<% param.href %>"><% param.key %></a></td>\
<td align="right"><% param.size %></td>\
</tr>';

helma.Inspector.property_skin = '<tr>\
<td><% param.title %></td>\
<td><% param.value encoding="all" %></td>\
</tr>';

helma.Inspector.date_skin = '<tr>\
<td><% param.title %></td>\
<td>\
<div class="display" id="<% param.key %>_display"><% param.value encoding="form" %></div>\
<div class="editor" id="<% param.key %>_editor">\
<form action="<% inspector.href %>" method="post">\
<input type="hidden" name="key" value="<% param.key %>" />\
<input type="hidden" name="type" value="date" />\
<select name="y"><% param.years %></select>.<select name="M"><% param.months %></select>.\
<select name="d"><% param.days %></select>, <select name="H"><% param.hours %></select>:<select name="m"><% param.minutes %></select>:<select name="s"><% param.seconds %></select><br />\
<input type="submit" name="save" value="Save" />\
<input type="reset" name="reset" value="Reset" />\
<input type="submit" name="cancel" value="Cancel" />\
</form></div>\
</td>\
</tr>';

helma.Inspector.editor_skin = '<tr>\
<td><% param.title %></td>\
<td>\
<div class="display" id="<% param.key %>_display"><% param.value encoding="form" %></div>\
<div class="editor" id="<% param.key %>_editor">\
<form action="<% inspector.href %>" method="post">\
<textarea name="<% param.key %>" <% param.key prefix=\'id="content_\' suffix=\'"\' %> cols="40" rows="1"><% param.value encoding="form" %></textarea><br />\
<input type="hidden" name="key" value="<% param.key %>" />\
<input type="submit" name="save" value="Save" />\
<input type="reset" name="reset" value="Reset" />\
<input type="submit" name="cancel" value="Cancel" />\
</form>\
</div>\
</td>\
</tr>';

HopObject.prototype[ (app.properties['inspectorAction'] || 'inspector') +'_action' ] = function() {
    if (!helma.auth('inspector')) 
        res.abort();
    if (typeof helma == "undefined" || !helma.Inspector) {
        res.write("Could not create instance of helma.Inspector ");
        res.write("(probably due to missing helmaLib v4.0+)");
        return;
    }
    var inspector = new helma.Inspector(this);
    inspector.action();
    return;
};

helma.lib = "Inspector";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
