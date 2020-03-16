if (!global.helma) {
    global.helma = {};
}

helma.sqlshell = {};

/**
* Get the helma datasource with the given name
*/
helma.sqlshell.getDatasource = function(name) {
    return app.getDbSource(name);
}

/**
 * Get an array of names of all defined data sources suitable for use
 * in html.select macro.
 */
helma.sqlshell.getDatasources = function() {
    var dbmap = app.getDbProperties();
    var sources = [];
    for (var i in dbmap) {
        var dot = i.indexOf(".");
        if (dot > -1 && i.lastIndexOf(".url") == i.length-4) {
            var source = i.substring(0, dot);
            sources.push([source, source]);
        }
    }
    return sources;
}

helma.sqlshell.getRepositories = function() {
    var rep = [];
    var repos = app.getRepositories();
    for (var i in repos) {
        if (repos[i].getClass() == Packages.helma.framework.repository.FileRepository)
            rep.push([i, repos[i].name]);
    }
    return rep;
}

helma.sqlshell.getProtoRepositories = function(protoName) {
    var rep = [];
    var proto = app.getPrototype(protoName);
    if (proto) {
        var repos = proto.getRepositories();
        for (var i in repos) {
            if (repos[i].getClass() == Packages.helma.framework.repository.FileRepository)
                rep.push([i, repos[i].name]);
        }
    }
    return rep;
}

/**
* Main action to set the Helma Dbsource, display forms, perform queries.
*/
helma.sqlshell.main = function() {
    // If done, end sqlshell session
    if (req.data.done) {
        helma.invalidate('sqlshell');
        if (session.data.sqlShellReturnUrl) {
            var targetUrl = session.data.sqlShellReturnUrl;
            delete session.data.sqlShellReturnUrl;
        } else {
            var targetUrl = path.href();
        }
        res.redirect(targetUrl);
    }
    
    // Check if sqlshell is called from the shell tool
    if (req.data.introFrom) {
        session.data.sqlShellReturnUrl = req.data.introFrom;
        if (session.data.shellAuthenticated) 
            session.data.sqlshellAuthenticated = true;
    }

    // Check authentication
    if (!helma.auth('sqlshell'))
        res.abort();

    // Handle authenticated requests
    res.handlers.html = helma.sqlshell.html;
    var param = {};
    param.datasource = req.data.datasource;
    res.data.fontface = 'Verdana, sans-serif';
    /* if (req.data.schema)
        session.data.sqlshellSchema = req.data.schema;
    if (req.data.datasource)
        session.data.sqlshellDatasource = req.data.datasource; */
    var dsource = req.data.datasource ?
        helma.sqlshell.getDatasource(req.data.datasource) : null;
    if (dsource) {
        (new helma.sqlshell.Datasource(dsource)).main();
    } else {
        if (req.data.datasource && req.isPost()) {
            param.message = "Sorry, data source " + req.data.datasource +
                            " is not defined for this application.";
        }
        res.data.header = "Choose data source";
        res.data.datasources = helma.sqlshell.getDatasources();
        res.data.body = renderSkinAsString("helma.sqlshell.selectdb", param);
        renderSkin("helma.sqlshell.page");
    }
}

helma.sqlshell.Datasource = function(datasource) {
    this.datasource = datasource;
    this.name = datasource.name;
    return this;
}

/**
* Get an array of schema names defined in the current database.
*/
helma.sqlshell.Datasource.prototype.getSchemas = function(meta) {
    // get available schemas and set up an array for the drop down box:
    var schemas = [];
    var t = meta.getSchemas();
    while (t.next()) {
        var s = t.getString(1);
        schemas.push([s, s]);
    }
    return schemas;
}

/**
 * Get table names and set up an array for the drop down box
 */
helma.sqlshell.Datasource.prototype.getTables = function(meta, schema) {
    var tables = [["", ""]];
    var t = meta.getTables (null, schema, "%", null);
    while (t.next()) {
        var table = t.getString (3);
        tables.push([table, table]);
    }
    return tables;
}

helma.sqlshell.Datasource.prototype.getColumns = function(meta, schema, table) {
    var columns = [];
    var t = meta.getColumns(null, schema, table, "%");
    while (t.next()) {
        columns.push(t.getString(4));
    }
    return columns;
}

helma.sqlshell.Datasource.prototype.getPrototypes = function() {
    var protos = [["", ""]];
    var protomap = app.getPrototypes();
    for (var i in protomap) {
        if (protomap[i].lowerCaseName != "global") {
            protos.push([protomap[i].name, protomap[i].name]);
        }
    }
    return protos.sort(function(a, b) {return a < b ? -1 : 1;});
}

helma.sqlshell.Datasource.prototype.href = function(name) {
    var href = path.href(req.action) + "?datasource=";
    href += encode(req.data.datasource);
    href += "&schema=";
    href += encode(req.data.schema);
    href += "&tab=";
    href += req.data.tab;
    return href;
}

helma.sqlshell.Datasource.prototype.href_macro = function(param) {
    return this.href(param.name);
}

helma.sqlshell.colors = {
    explore: "#bd9", query: "#db9", map: "#9bd"
}

helma.sqlshell.Datasource.prototype.main = function() {
    res.handlers.datasource = this;
    if (!req.data.tab) {
        req.data.tab = "explore";
    }
    res.data.tabcolor = helma.sqlshell.colors[req.data.tab];

    var param = new Object();
    param.action = this.href();

    // get connection
    var con = this.datasource.getConnection();

    // get database meta data
    var meta = con.getMetaData();

    res.data.datasources = helma.sqlshell.getDatasources();
    res.data.schemas = this.getSchemas(meta);
    var schema = req.data.schema;
    res.data.tables = this.getTables(meta, schema);

    if (req.data.action) {
        app.data.repositories = helma.sqlshell.getRepositories();
        if (req.data.action == "createproto" ) {
            var repos = app.repositories[req.data.repository];
            var file = new File(repos.directory.toString(), req.data.protoname);
            if (file.mkdir()) {

                renderSkin(this.getSkin("closePopup"), {
                    parentUrl: this.href() + "&prototype=" + req.data.protoname,
                    message: "<p>Created directory " + file + "</p>" +
                             "<p>Please wait for prototypes to be updated...</p>"
                } );
                return;
            } else {
                res.debug("Couldn't create directory: " + file);
                res.data.body = renderSkinAsString(this.getSkin("newproto"));
            }
        } else if (req.data.action == "extras") {
            var p = {};
            var t = app.getPrototype(req.data.target);
            var target = t && t.dbMapping ? t.dbMapping.tableName : null;
            p.targetColumns = this.getColumns(meta, schema, target).toSource();
            p.localColumns = this.getColumns(meta, schema, req.data.__sqlshell_table__).toSource();
            res.data.body = renderSkinAsString(this.getSkin(req.data.action), p);
        } else if (req.data.action == "generate") {
            if (req.data.create) {
                renderSkin(this.getSkin("closePopup"), {
                    parentUrl: this.href() + "&prototype=" + req.data.__sqlshell_prototype__,
                    message: "<p>Created type mapping " + file + "</p>" +
                             "<p>Please wait for prototypes to be updated...</p>"
                } );
            } else {
                var fields = {};
                var s = new java.lang.StringBuffer();
                for (var i in req.data) {
                    if (i.indexOf("maptype_") == 0) {
                        fields[i.substring(8)] = req.data[i];
                    }
                    s.append("<input type='hidden' name='").append(i)
                            .append("' value='").append("" + req.data[i]).append("'>");
                }
                if (req.data.__sqlshell_create__) {
                    // res.data.body = renderSkinAsString(this.getSkin("generate"), p);
                    var repos = app.getPrototype(req.data.__sqlshell_prototype__).repositories;
                    var resName = "type.properties";
                    for (var i in repos) {
                        var resource = repos[i].getResource(resName);
                        if (resource && resource.exists()) {
                            if (resource.getClass() == Packages.helma.framework.repository.FileResource) {
                                var file = new File(resource.getName());
                                var backup = new File(resource.getName() + ".bak");
                                if (backup.exists()) {
                                    var n = 1;
                                    do {
                                        backup = new File(resource.getName() + ".bak." + n++)
                                    } while (backup.exists());
                                }
                                if (!file.renameTo(backup)) {
                                    res.debug("ERROR: Couldn't create backup for " + resource);
                                }
                            } else {
                                res.debug("WARNING: Couldn't move " + resource);
                            }
                        }
                    }
                    var file = new File(repos[req.data.__sqlshell_repository__].getResource(resName).getName());
                    file.open();
                    file.writeln("# Created by Helma SqlShell at " + new Date());
                    if (req.data.__sqlshell_extends__)
                        file.writeln("_extends = " + req.data.__sqlshell_extends__);
                    if (req.data.__sqlshell_primaryKey__)
                        file.writeln("_id = " + req.data.__sqlshell_primaryKey__);
                    if (req.data.__sqlshell_protoColumn__)
                        file.writeln("_prototype = " + req.data.__sqlshell_protoColumn__);
                    if (req.data.__sqlshell_nameColumn__)
                        file.writeln("_name = " + req.data.__sqlshell_nameColumn__);
                    file.writeln("");
                    for (var i in fields) {
                        var propType = parseInt(fields[i]);
                        var propName = req.data[i];
                        if (!propName) continue;
                        file.write(propName);
                        file.write(" = ");
                        switch (propType) {
                        case 0:
                            file.writeln(req.data[i]);
                            break;
                        case 1:
                             file.writeln("object(" + req.data["target_" + i] + ")");
                             break;
                        case 2:
                             file.writeln("collection(" + req.data["target_" + i] + ")");
                             break;
                        case 3:
                             file.writeln("mountpoint(" + req.data["target_" + i] + ")");
                             break;
                        default:
                            res.debug(i + ": " + fields[i]);
                        }
                        for (var m in this.mappingOptions) {
                             if (this.mappingOptions[m] <= propType && req.data[i + "_" + m]) {
                                 file.write(propName);
                                 file.write(".");
                                 file.write(m.replace("_", "."));
                                 file.write(" = ");
                                 file.writeln(req.data[i + "_" + m]);
                             }
                        }
                        file.writeln("");
                    }
                    file.close();
                    res.data.body = "Successfully created mapping in " + file;
                } else {
                    var p = {};
                    p.data = s.toString();
                    res.data.repositories = helma.sqlshell.getProtoRepositories(req.data.__sqlshell_prototype__);
                    res.data.body = renderSkinAsString(this.getSkin("generate"), p);
                }
            }
        } else {
            res.data.body = renderSkinAsString(this.getSkin(req.data.action));
        }
    } else {
        // should we display type info on some table?
        if (req.data.tab == "explore") {
            param.body = this.explore(meta, schema, param);
        } else if (req.data.tab == "query") {
            param.body = this.query(con, param);
        } else if (req.data.tab == "map") {
            param.body = this.map(meta, schema, con, param);
        }
        // render the inner page skin and then the whole page
        res.data.body = renderSkinAsString("helma.sqlshell.main", param);
    }

    renderSkin("helma.sqlshell.page");
}

helma.sqlshell.Datasource.prototype.explore = function(meta, schema, param) {
    res.push();
    renderSkin(this.getSkin("explore"), param);
    if (req.data.__sqlshell_table__) {
        var tableStyle = { table: { "class": "explore" }, td: { "class": "explore" } };
        var t = meta.getColumns(null, schema, req.data.__sqlshell_table__, "%");
        var writer = new helma.Markup.TableWriter(6, tableStyle);
        writer.writeHeader = true;
        var columnNames = ["Column Name", "Column Type", "Column Size",
                "Nullable", "Default Value", "Extras"];
        for (var c in columnNames) {
            writer.write(columnNames[c]);
        }
        while (t.next()) {
            writer.write(t.getString(4));
            writer.write(t.getString(6));
            writer.write(t.getString(7));
            writer.write(t.getString(18));
            writer.write(t.getString(13));
            writer.write(t.getString(12));
        }
        writer.close();
    }
    return res.pop();
}

helma.sqlshell.Datasource.prototype.query = function(con, param) {
    // some SQL has been submitted - evaluate it
    if (req.data.sql) {
        var query = req.data.sql.trim();

        con.setReadOnly(false);
        var stmt = con.createStatement();
        var value;
        try {
            value = stmt.execute(query);
            if (!value) {
                param.updated = stmt.getUpdateCount();
            } else {
                var rs = stmt.getResultSet();
                var rsmeta = rs.getMetaData();
                var ncol = rsmeta.getColumnCount();

                res.push();
                var tableStyle = { table: { "class": "query" }, td: { "class": "query" } };
                var writer = new helma.Markup.TableWriter(ncol, tableStyle);
                writer.writeHeader = true;
                for (var i=1; i<=ncol; i++) {
                    writer.write(rsmeta.getColumnName(i));
                }

                while (rs.next()) {
                    for (var i=1; i<=ncol; i++) {
                        writer.write(encode(rs.getString(i)));
                    }
                }

                writer.close();
                param.resultset = res.pop();
            }
        } catch (error) {
            param.message = "Error: " + error;
        }
    }
    return renderSkinAsString(this.getSkin("query"), param);
}

helma.sqlshell.Datasource.prototype.map = function(meta, schema, con, param) {
    // for (var i in req.data) res.debug(i);
    res.push();
    res.data.prototypes = this.getPrototypes();
    var proto = app.getPrototype(req.data.__sqlshell_prototype__);
    if (proto) {
        var tableStyle = { table: { "class": "map" }, td: { "class": "map" } };
        var dbmap = proto.getDbMapping();
        if (!req.data.__sqlshell_table__ ||
                req.data.__sqlshell_prototype__ != req.data.previousProto) {
            req.data.__sqlshell_table__ = dbmap.tableName;
        }
        param.tableSelect = renderSkinAsString(createSkin('Map to table \
        <% html.select name="__sqlshell_table__" options="response.tables" \
         onchange="document.forms.tab.submit();"%>'));
    }
    renderSkin(this.getSkin("map"), param);
    if (proto) {
        var maptypes = ["Primitive", "Reference", "Collection", "Mountpoint"];
        var tableStyle = { table: { "class": "map" }, td: { "class": "map" } };
        if (req.data.__sqlshell_table__) {
            var primKey = "";
            try {
                var k = meta.getPrimaryKeys(null, schema, req.data.__sqlshell_table__);
                if (k.next()) {
                    primKey = k.getString(4);
                }
                if (k.next()) {
                    helma.Markup.p({"class": "error"}, "Table has composed primary key!");
                }
            } catch (error) {
                helma.Markup.p({"class": "error"}, "Error retrieving primary key: " + error);
            }
            var t = meta.getColumns(null, schema, req.data.__sqlshell_table__, "%");
            var columns = [];
            res.data.columns = [["", ""]];
            while (t.next()) {
                var colname = t.getString(4);
                columns.push(colname);
                res.data.columns.push([colname, colname]);
            }
            var writer = new helma.Markup.TableWriter(2, tableStyle);
            writer.write("Extends ");
            var ext = dbmap.getExtends() || app.getPrototype("hopobject").name;
            writer.write(helma.Markup.Select({name: "__sqlshell_extends__"}, res.data.prototypes, ext));
            writer.write("Primary key column ");
            writer.write(helma.Markup.Select({name: "__sqlshell_primaryKey__"}, res.data.columns, primKey));
            writer.write("Prototype column ");
            writer.write(helma.Markup.Select({name: "__sqlshell_protoColumn__"}, res.data.columns, dbmap.prototypeField));
            writer.write("Name column ");
            writer.write(helma.Markup.Select({name: "__sqlshell_nameColumn__"}, res.data.columns, dbmap.nameField));
            writer.close();
            tableStyle = { table: { "class": "map", id: "maptable" }, td: { "class": "map" } };
            writer = new helma.Markup.TableWriter(5, tableStyle);
            writer.writeHeader = true;
            var headers = ["Column Name", "Property Name", "Mapping",
                    "Target Prototype", "Extras"];
            for (var c in headers) {
                writer.write(headers[c]);
            }
            for (var col in columns) {
                var colname = columns[col];
                // if (colname == primKey) continue;
                var rel = dbmap.columnNameToRelation(colname);
                var value = rel && rel.propName ? rel.propName : this.toCamelCase(colname);
                var type = rel ? rel.refType : 0;
                var targetDisplay = type > 0 ? '': ' style="display: none;"';
                var target = rel && rel.targetType ? rel.targetType.typeName : "";
                writer.write(colname);
                writer.write('<input name="' + colname + '" value="' + value +'">');
                writer.write(helma.Markup.Select({name: "maptype_" + colname,
                    onchange: "toggleEditor(this)"}, maptypes, type));
                writer.write('<div id="refedit_' + colname + '"' + targetDisplay +'>' +
                             helma.Markup.Select({name: "target_" + colname}, res.data.prototypes, target) + '</div>');
                var buffer = new java.lang.StringBuffer();
                var config = rel ? wrapJavaMap(rel.config) : {};
                for (var i in this.mappingOptions) {
                    // var name = i.replace('_', '.');
                    var name = colname + "_" + i;
                    buffer.append(helma.Markup.Hidden({id: name, name: name, value: config[i] }).toString());
                }
                buffer.append(helma.Markup.A({href: this.href() + "&action=extras&col=" + colname,
                    id:"extralink_" + colname, style: type > 0 ? '': 'display: none;',
                    onclick: "openExtraEditor(this.href, '" + colname + "'); return false;"},
                        "edit").toString());
                writer.write(buffer);
                /* writer.write(helma.Markup.A({href: this.href() + "&action=extras&col=" + colname,
                    id:"extralink_" + colname, style: type > 0 ? '': 'display: none;',
                    onclick: "openPopup(this.href, 'extras'); return false;"},
                        "edit")); */
            }
            var props = dbmap.getPropertyNames();
            var collectionCount = 0;
            for (var p in props) {
                var rel = dbmap.propertyToRelation(props[p]);
                if (rel.refType < 1 || (rel.dbField && rel.dbField != primKey)) {
                    continue;
                }
                var propName = rel.propName;
                var target = rel.targetType ? rel.targetType.typeName : "";
                var type = rel.refType;
                if (type == 2 && !rel.dbField) {
                    // helma does not separate between collections and mountpoints internally
                    type = 3;
                }
                var colname = "collection_" + (collectionCount++);
                writer.write("");
                writer.write('<input name="' + colname + '" value="' + propName +'">');
                writer.write(helma.Markup.Select({name: "maptype_" + colname,
                    onchange: "toggleEditor(this)"}, maptypes, type));
                writer.write('<div id="refedit_' + colname + '">' +
                             helma.Markup.Select({name: "target_" + colname}, res.data.prototypes, target) + '</div>');
                var buffer = new java.lang.StringBuffer();
                var config = wrapJavaMap(rel.config);
                for (var i in this.mappingOptions) {
                    // var name = i.replace('_', '.');
                    var name = colname + "_" + i;
                    buffer.append(helma.Markup.Hidden({id: name, name: name, value: config[i] }).toString());
                }
                buffer.append(helma.Markup.A({href: this.href() + "&action=extras&col=" + colname,
                    id:"extralink_" + colname,
                    onclick: "openExtraEditor(this.href, '" + colname + "'); return false;"},
                        "edit").toString());
                writer.write(buffer);
            }
            writer.close();
            // FIXME: MAJOR HACK **********************************
            res.writeln('<script type="text/javascript">');
            res.writeln('var colcount = ' + collectionCount + ';');
            res.write('var rowtemplate = \'<td class="map"></td><td class="map"><input name="$$$" value=""></td>');
            res.write('<td class="map">')
            helma.Markup.select({name: "maptype_$$$",
                onchange: "toggleEditor(this)"}, maptypes, 2);
            res.write('</td><td class="map">')
            res.write('<div id="refedit_$$$">' +
                         helma.Markup.Select({name: "target_$$$"}, res.data.prototypes, target) + '</div>');
            res.write('</td><td class="map">')
            for (var i in this.mappingOptions) {
                // var name = i.replace('_', '.');
                var name = "$$$_" + i;
                helma.Markup.hidden({id: name, name: name, value: "" });
            }
            helma.Markup.a({href: this.href() + "&action=extras&col=$$$",
                id:"extralink_$$$",
                onclick: "openExtraEditor(this.href, \\'$$$\\'); return false;"},
                    "edit");
            res.write('</td>');
            res.writeln("';");
            res.writeln('</script>');
            // END OF MAJOR HACK **********************************
            helma.Markup.a({href: "#", onclick:'return appendTableRow("maptable");'}, "Add Collection");
            res.write(" ");
            helma.Markup.submit({name: "generateMapping",
                onclick:"submitFormToPopup(document.forms.tab, '" + this.href() + "&action=generate', 'generate',500,350); return false;",
                value: "Generate Mapping"});

        }
    }
    return res.pop();
}

helma.sqlshell.Datasource.prototype.mappingOptions = {
    local: 1,
    foreign: 1,
    order: 2,
    accessname: 2,
    group: 2,
    group_order: 2,
    group_prototype: 2,
    filter: 2,
    filter_additionalTables: 2,
    loadmode: 1,
    cachemode: 2,
    maxsize: 2,
    hints: 2,
    logicalOperator: 2,
    readonly: 2,
    "private": 2
}

helma.sqlshell.Datasource.prototype.toCamelCase = function(str) {
    var s = str.toLowerCase().split(/[-_]/);
    str = s[0];
    for (var i=1; i<s.length; i++) {
        str += s[i].charAt(0).toUpperCase() + s[i].substring(1);
    }
    return str;
}

helma.sqlshell.Datasource.prototype.getSkin = function(name) {
    switch (name) {
    case "query": return createSkin('<div class="activetab">SQL:<br>\
        <textarea cols="60" rows="6" wrap="virtual"\
        name="sql"><% request.sql encode="form" %></textarea><br>\
        <input type="submit" value="Submit"> \
        <input type="reset" value="Reset"></div>\
        <% param.message prefix="<p style=\'color: red\'>" suffix="</p>" %>\
        <% param.updated prefix="<p><span style=\'background-color: yellow\'>"\
        prefix="Statement executed, "\
        suffix=" row(s) affected</span></p>" %>\
        <% param.resultset %>');

    case "explore": return createSkin('<div class="activetab">Describe Table \
        <% html.select name="__sqlshell_table__" options="response.tables" \
        onchange="document.forms.tab.submit();" %>\
        <input type="submit" value="Submit"></div>\
        <% param.tableheader prefix="<h4>" suffix="</h4>" %>\
        <% param.tableinfo %>');

    case "map": return createSkin('<div class="activetab">Prototype \
        <% html.select name="__sqlshell_prototype__" options="response.prototypes" \
        onchange="document.forms.tab.submit();" %>\
        <a onclick="openPopup(\'' +this.href() + '&action=newproto\', \'newproto\',500,250); return false;" \
        href="?popup=newproto">[new]</a>\
        <% param.tableSelect %> \
        <input type="hidden" name="previousProto" value="<% request.__sqlshell_prototype__ %>">\
        <input type="submit" value="Set">\
        <input type="reset" value="Reset">\
        </div>');

    case "newproto": return createSkin('<form method="POST" action="' + this.href() + '&action=createproto">\
        Prototype Name: <br><input name="protoname"><br>\
        Create in Repository: <br><% html.select name="repository" options="app.repositories" %><br><br>\
        <input type="submit" name="create" value="Create Prototype">\
        </form>');

    case "extras":  return createSkin('<form name="extras">\
        <h4>Extra parameters for ' + req.data.col + '</h4>\
        <table>\
        <script type="text/javascript">\
            var localColumns = <% param.localColumns %>;\
            var targetColumns = <% param.targetColumns %>;\
            extraEditor("' + req.data.col + '", localColumns, targetColumns);\
        </script>\
        </table>\
        <input type="submit" value="Apply" \
        onclick="applyExtras(\'' + req.data.col + '\'); return false;">\
        </form>');

    case "generate": return createSkin('<form method="POST" action="' + this.href() + '&action=generate">\
        Create type.properties in Repository: <br><% html.select name="__sqlshell_repository__"\
        options="response.repositories" %><br><br>\
        <input type="submit" name="__sqlshell_create__" value="Create Type Mapping">\
        <% param.data %>\
        </form>');

    case "closePopup": return createSkin('<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">\
        <!--\
           This is an empty page that closes the current popup window and\
           optionally refreshes the parent page (passed as parentUrl).\
        -->\
        <html>\
        <head>\
        <title>closing window</title>\
        <script>\
            var parentUrl = "<% param.parentUrl %>";\
            function doClose() {\
               if (parentUrl)\
                   opener.location.href = parentUrl;\
               window.close();\
            }\
            setTimeout("doClose()", 150000);\
        </script>\
        </head>\
        <body>\
        <% param.message %>\
        </body>\
        </html>');

    default: return createSkin("No skin defined for " + name);
    }
}

helma.sqlshell.Datasource.href =

HopObject.prototype[ (app.properties['sqlshellAction'] || 'sqlshell') +'_action' ] = helma.sqlshell.main;


helma.dontEnum('sqlshell');




////////////////////////////////////////////////////////////////////////
//  Macro handler for Html tags
////////////////////////////////////////////////////////////////////////

helma.sqlshell.html = {

    tablink_macro: function(param) {
        var href = req.action + "?datasource=";
        href += encode(req.data.datasource);
        href += "&schema=";
        href += encode(req.data.schema);
        href += "&tab=";
        href += param.name;
        var attr = { href: href, "class": "tab" };
        if (req.data.tab == param.name) {
            attr["class"] += " activetab";
        } else {
            attr["class"] += " passivetab";
        }
        helma.Markup.element("a", attr, param.name);
    },

    select_macro: function(param) {
        if (!param.name) {
            throw "dropDown macro requires name attribute";
        }
        if (!param.options) {
            throw "dropDown macro requires options attribute";
        }
        var opts = param.options.split(".");
        if (opts.length != 2) {
            throw "options attribute must be of the form 'handler.options'";
        }
        var handler = this.getHandler(opts[0]);
        if (!handler) {
            throw "handler '"+opts[0]+" not found - " +
                  "valid options are (response|request|session|app)";
        }
        var options = handler[opts[1]];
        if (!options) {
            throw param.options+" is not defined ";
        }
        if (options.length == 0) {
            return;
        }
        var attr = {};
        for (var i in param) {
            if (i != "options" && i != "prefix" && i != "suffix") {
                attr[i] = param[i];
            }
        }
        helma.Markup.select(attr, options, req.data[param.name], param.firstoption);
    },

    getHandler: function (handlerName) {
        switch (handlerName) {
            case "response":
                return res.data;
            case "request":
                return req.data;
            case "session":
                return session.data;
            case "app":
                return app.data;
        }
        return null;
    }
}
