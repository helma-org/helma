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
 * $RCSfile: helma.Chart.js,v $
 * $Author: czv $
 * $Revision: 1.6 $
 * $Date: 2006/04/18 13:06:58 $
 */


// take care of any dependencies
app.addRepository('modules/helma/jxl.jar');


if (!global.helma) {
    global.helma = {};
}

//
// chart package by tobi schaefer
// needs andy khan's java excel api: download jxl.jar at
// http://www.andykhan.com/jexcelapi/
//


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

    function getCellStyle(c) {
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

    if (sheetName)
        var sheet = workbook.getSheet(sheetName);
    else
        var sheet = workbook.getSheet(0);
    if (!sheet)
        return;

    prefix = prefix ? prefix + "_" : "chart_";

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

    this.renderAsString = function() {
        res.push();
        this.render();
        return res.pop();
    };

    this.toString = function() {
        return "[helma.Chart " + file + "]";
    };

    for (var i in this)
        this.dontEnum(i);

    return this;
}


helma.Chart.toString = function() {
    return "[helma.Chart]";
};


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
