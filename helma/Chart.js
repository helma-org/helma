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
 * $RCSfile: Chart.js,v $
 * $Author$
 * $Revision$
 * $Date$
 */


/**
 * @fileoverview Fields and methods of the helma.Chart prototype
 */

// take care of any dependencies
app.addRepository('modules/helma/jxl.jar');

/**
 * Define the global namespace if not existing
 */
if (!global.helma) {
    global.helma = {};
}

/**
 * Creates a new instance of helma.Chart
 * @class Instances of this class are capable of reading
 * Excel spreadsheets and rendering them as XHTML table. Internally
 * helma.Chart uses the <a href="http://www.jexcelapi.org/ ">Java Excel API</a>
 * by <a href="http://www.andykhan.com/">Andy Khan</a>.
 * @param {String} fpath The path to the spreadsheet file
 * @param {String} prefix An optional prefix to use for all
 * stylesheet classes within the rendered table
 * @param {String} sheetName The name of the sheet within the
 * spreadsheet file to render. If this argument is omitted, the
 * first sheet is rendered.
 * @returns A newly created helma.Chart instance.
 * @constructor
 * @author Tobi Schaefer
 */
helma.Chart = function(fpath, prefix, sheetName) {
    var JXLPKG = Packages.jxl.Workbook;
    var JXLPKGNAME = "jxl.jar";
    var JXLPKGURL = "http://www.andykhan.com/jexcelapi/";

    var workbook, file;
    try {
        file = new java.io.File(fpath); 
        workbook = JXLPKG.getWorkbook(file);
    } catch (e) {
        if (e instanceof TypeError == false)
            throw(e);
        throw("helma.Chart needs " + JXLPKGNAME + 
              " in lib/ext or application directory " +
              "[" + JXLPKGURL + "]");
    }

    var getCellStyle = function(c) {
        if (!c)
            return;
        var result = new Object();
        var format = c.getCellFormat();
        var font = format.getFont();
        if (font.getBoldWeight() > 400)
            result.bold = true;
        result.italic = font.isItalic();
        result.wrap = format.getWrap();
        var type = c.getType();
        var align = format.getAlignment().getDescription();
        if (align == "right" || type == "Number" || type == "Date")
            result.align = "right";
        else if (align == "centre")
            result.align = "center";
        return result;
    }

    if (sheetName) {
        var sheet = workbook.getSheet(sheetName);
    } else {
        var sheet = workbook.getSheet(0);
    }
    if (!sheet)
        return;

    prefix = prefix ? prefix + "_" : "chart_";

    /**
     * Renders the Excel spreadsheet as XHTML table.
     */
    this.render = function() {
        res.write('<table border="0" cellspacing="1" class="' + 
                  prefix + 'table">\n');
    
        var rowBuf = [];
        var rows = sheet.getRows();
        var max = 0;
        for (var i=0; i<rows; i+=1) {
            var row = sheet.getRow(i);
            if (row.length > max)
                max = row.length;
            rowBuf.push(row);
        }
    
        for (var i in rowBuf) {
            res.write('<tr class="' + prefix + 'row">\n');
            for (var n=0; n<max; n+=1) {
                if (n < rowBuf[i].length) {
                    var c = rowBuf[i][n];
                    var str = c.getContents();
                    if (str)
                        var style = getCellStyle(c);
                }
                res.write('<td class="' + prefix + 'cell"');
                if (style) {
                    if (!style.wrap)
                        res.write(' nowrap="nowrap"');
                    if (style.align)
                        res.write(' align="' + style.align + '"');
                    res.write(">");
                    if (style.bold)
                        res.write("<b>");
                    if (style.italic)
                        res.write("<i>");
                }
                else
                    res.write(">");
                res.write(str);
                if (style) {
                    if (style.italic)
                        res.write("</i>");
                    if (style.bold)
                        res.write("</b>");
                }
                res.write('</td>\n');
            }
            res.write('</tr>\n');
        }
    
        res.write('</table>\n');
        workbook.close();
    };

    /**
     * Returns the spreadsheet as rendered XHTML table.
     * @returns The rendered spreadsheet table
     * @type String
     */
    this.renderAsString = function() {
        res.push();
        this.render();
        return res.pop();
    };

    /** @ignore */
    this.toString = function() {
        return "[helma.Chart " + file + "]";
    };

    for (var i in this)
        this.dontEnum(i);

    return this;
}


/** @ignore */
helma.Chart.toString = function() {
    return "[helma.Chart]";
};


/**
 * A simple example for using helma.Chart that renders
 * the passed file as XHTML table to response.
 * @param {String} file The path to the Excel spreadsheet file
 */
helma.Chart.example = function(file) {
    // var file = "/path/to/file.xls";
    var chart = new helma.Chart(file);
    chart.render();
    return;
};


helma.lib = "Chart";
helma.dontEnum(helma.lib);
for (var i in helma[helma.lib])
    helma[helma.lib].dontEnum(i);
for (var i in helma[helma.lib].prototype)
    helma[helma.lib].prototype.dontEnum(i);
delete helma.lib;
