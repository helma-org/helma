//
// Jala Project [http://opensvn.csie.org/traccgi/jala]
//
// Copyright 2004 ORF Online und Teletext GmbH
//
// Licensed under the Apache License, Version 2.0 (the ``License'');
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an ``AS IS'' BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
// $Revision$
// $LastChangedBy$
// $LastChangedDate$
// $HeadURL$
//

/**
 * @fileoverview Fields and methods of the jala.Date class.
 */

// Define the global namespace for Jala modules
if (!global.jala) {
   global.jala = {};
}


/**
 * HelmaLib dependencies
 */
app.addRepository("modules/core/Date.js");
app.addRepository("modules/helma/Html.js");

/**
 * Constructs a new Renderings object.
 * @class This class provides various convenience
 * methods for rendering purposes.
 * @constructor
 */
jala.Date = function() {
   return this;
};

/**
 * Renders a timestamp as set of DropDown boxes, following the
 * format passed as argument. Every &lt;select&gt;
 * item is prefixed with a string so that it can be retrieved
 * easily from the values of a submitted POST request.
 * @param {String} prefix The prefix to use for all dropdown boxes, eg. "postdate"
 * @param {Date} date A Date object to use as preselection (optional)
 * @param {Object} fmt Array containing one parameter object for every single
 * select box that should be rendered, with the following properties set:
 * <ul>
 * <li>pattern - The date format pattern that should be rendered. Valid
 * patterns are: "dd", "MM", "yyyy", "HH", "ss".</li>
 * <li>firstOption - The string to use as first option, eg.: "choose a day"</li>
 * </ul>
 */
jala.Date.prototype.renderEditor = function(prefix, date, fmt) {
   /**
    * rendering method
    * @private
    */
   var render = function(param, date) {
      switch (param.pattern) {
         case "dd":
         param.offset = 1;
         param.max = 31;
         param.selected = (date ? date.getDate() : null);
         break;

         case "MM":
         param.offset = 1;
         param.max = 12;
         param.selected = (date ? date.getMonth() +1 : null);
         break;

         case "yyyy":
         param.offset = 2002;
         param.max = 20;
         param.selected = (date ? date.getFullYear() : null);
         break;

         case "HH":
         param.offset = 0;
         param.max = 24;
         param.selected = (date ? date.getHours() : null);
         break;

         case "mm":
         param.offset = 0;
         param.max = 60;
         param.selected = (date ? date.getMinutes() : null);
         break;

         case "ss":
         param.offset = 0;
         param.max = 60;
         param.selected = (date ? date.getSeconds() : null);
         break;
      }

      var key = prefix + ":" + param.pattern;
      if (req.data[key])
         param.selected = req.data[key];
      var options = [];
      var opt;
      for (var i=0;i<param.max;i++) {
         opt = (param.offset + i).format("00");
         options[i] = [opt, opt];
      }
      var html = new helma.Html();
      html.dropDown({name: key}, options, param.selected, param.firstOption);
   }

   if (!fmt)
      var fmt = [{pattern: "dd", firstOption: "day"},
                 {pattern: "MM", firstOption: "month"},
                 {pattern: "yyyy", firstOption: "year"},
                 {pattern: "HH", firstOption: "hour"},
                 {pattern: "mm", firstOption: "minute"}];

   for (var i in fmt) {
      render(fmt[i], date);
   }
   return;
};

/**
 * Returns a timestamp as set of dropdown-boxes
 * @see #renderEditor
 * @type String
 */
jala.Date.prototype.renderEditorAsString = function(prefix, date, pattern) {
   res.push();
   this.renderEditor(prefix, date, pattern);
   return res.pop();
};

/**
 * Creates a new instance of jala.Data.Calendar
 * @class This class represents a calendar based based on a grouped
 * collection of HopObjects. It provides several methods for rendering
 * the calendar plus defining locale and timezone settings.
 * @param {HopObject} collection A grouped HopObject collection to work on
 * @returns A newly created jala.Date.Calendar instance
 * @constructor
 */
jala.Date.Calendar = function(collection) {
   var renderer = null;
   var locale = java.util.Locale.getDefault();
   var timezone = java.util.TimeZone.getDefault();
   var hrefFormat = "yyyyMMdd";
   var accessNameFormat = "yyyyMMdd";

   /**
    * Returns the collection this calendar object works on
    * @returns The HopObject collection of this calendar
    * @type HopObject
    */
   this.getCollection = function() {
      return collection;
   };

   /**
    * Sets the renderer to use.
    * @param {Object} r The renderer to use
    * @see #getRenderer
    */
   this.setRenderer = function(r) {
      renderer = r;
      return;
   };

   /**
    * Returns the renderer used by this calendar.
    * @returns The calendar renderer
    * @type Object
    * @see #setRenderer
    */
   this.getRenderer = function() {
      if (!renderer) {
         renderer = new jala.Date.Calendar.Renderer(this);
      }
      return renderer;
   };

   /**
    * Sets the locale to use within this calendar object
    * @param {java.util.Locale} loc The locale to use
    * @see #getLocale
    */
   this.setLocale = function(loc) {
      locale = loc;
      return;
   };

   /**
    * Returns the locale used within this calendar instance.  By default
    * the locale used by this calendar is the default locale of the
    * Java Virtual Machine running Helma.
    * @returns The locale of this calendar
    * @type java.util.Locale
    * @see #setLocale
    */
   this.getLocale = function() {
      return locale;
   };

   /**
    * Sets the locale to use within this calendar object
    * @param {java.util.Locale} loc The locale to use
    * @see #getTimeZone
    */
   this.setTimeZone = function(tz) {
      timezone = tz;
      return;
   };

   /**
    * Returns the locale used within this calendar instance. By default
    * the timezone used by this calendar is the default timezone
    * of the Java Virtual Machine running Helma.
    * @returns The locale of this calendar
    * @type java.util.Locale
    * @see #setTimeZone
    */
   this.getTimeZone = function() {
      return timezone;
   };

   /**
    * Sets the format of the hrefs to render by this calendar
    * to the format pattern passed as argument.
    * @param {String} fmt The date format pattern to use for
    * rendering the href
    * @see #getHrefFormat
    */
   this.setHrefFormat = function(fmt) {
      hrefFormat = fmt;
      return;
   };

   /**
    * Returns the date formatting pattern used to render hrefs. The default
    * format is "yyyyMMdd".
    * @returns The date formatting pattern
    * @type String
    * @see #setHrefFormat
    */
   this.getHrefFormat = function() {
      return hrefFormat;
   };

   /**
    * Sets the format of the group name to use when trying to access
    * child objects of the collection this calendar is operating on.
    * @param {String} fmt The date format pattern to use for
    * accessing child objects
    * @see #getAccessNameFormat
    */
   this.setAccessNameFormat = function(fmt) {
      accessNameFormat = fmt;
      return;
   };

   /**
    * Returns the format of the access name used by this calendar to access
    * child group objects of the collection this calendar is operating on.
    * The default format is "yyyyMMdd".
    * @returns The date formatting pattern used to access child objects
    * @type String
    * @see #setAccessNameFormat
    */
   this.getAccessNameFormat = function() {
      return accessNameFormat;
   };

   return this;
};

/** @ignore */
jala.Date.Calendar.prototype.toString = function() {
   return "[Jala Calendar]";
};

/**
 * Renders the calendar using either a custom renderer defined
 * using {@link #setRenderer} or the default one.
 * @see #setRenderer
 * @see jala.Date.Calendar.Renderer
 */
jala.Date.Calendar.prototype.render = function(today) {
   var renderer = this.getRenderer();
   var collection = this.getCollection();
   var hrefFormat = this.getHrefFormat();
   var accessNameFormat = this.getAccessNameFormat();
   var locale = this.getLocale();
   var timezone = this.getTimeZone();
   var size = collection.size();
   if (size == null)
      return;

   /**
    * private method that creates a date object set
    * to the last date of the previous month or the
    * first date of the next month (if available)
    */
   var prevNextMonth = function(which, dayIndex) {
      var obj;
      if (which == "prev") {
         if (size <= dayIndex || !(obj = collection.get(dayIndex +1)))
            return;
      } else if (which == "next") {
         if (dayIndex == 0 || !(obj = collection.get(dayIndex - 1)))
            return;
      } else {
         return;
      }
      return new Date(obj.groupname.substring(0, 4),
                      obj.groupname.substring(4, 6) -1,
                      obj.groupname.substring(6));
   };

   // create the calendar object used for date calculations
   var cal = java.util.Calendar.getInstance(timezone, locale);
   var firstDayOfWeek = cal.getFirstDayOfWeek();
   var symbols = new java.text.DateFormatSymbols(locale);

   res.push();
   // render the header-row
   res.push();
   var weekdays = symbols.getShortWeekdays();
   for (var i=0;i<7;i++) {
      renderer.renderDayHeader(weekdays[(i+firstDayOfWeek-1)%7+1]);
   }
   renderer.renderRow(res.pop());

   cal.set(java.util.Calendar.DATE, 1);

   // check whether there's a day in path
   // if so, use it to determine the month to render
   if (today) {
      cal.set(java.util.Calendar.YEAR, today.getFullYear());
      cal.set(java.util.Calendar.MONTH, today.getMonth());
   }
   // nr. of empty days in rendered calendar before the first day of month appears
   var pre = (7-firstDayOfWeek+cal.get(java.util.Calendar.DAY_OF_WEEK)) % 7;
   var days = cal.getActualMaximum(java.util.Calendar.DATE);
   var weeks = Math.ceil((pre + days) / 7);
   var daycnt = 1;

   var date = new Date(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), 1);
   // remember the index of the first and last days within this month.
   // this is needed to optimize previous and next month links.
   var lastDayIndex = Number.MAX_VALUE;
   var firstDayIndex = -1;

   var dayObj, idx, selected;
   for (var i=0;i<weeks;i++) {
      res.push();
      for (var j=0;j<7;j++) {
         if ((i == 0 && j < pre) || daycnt > days) {
            renderer.renderDay(null);
         } else {
            date.setDate(daycnt);
            if ((dayObj = collection.get(date.format(accessNameFormat))) != null) {
               idx = collection.contains(dayObj);
               if (idx > -1) {
                  if  (idx > firstDayIndex) {
                     firstDayIndex = idx;
                  }
                  if (idx < lastDayIndex) {
                     lastDayIndex = idx;
                  }
               }
            }
            selected = (today != null) ? date.equals(today) : false;
            renderer.renderDay(date, dayObj != null, selected);
            daycnt++;
         }
      }
      renderer.renderRow(res.pop());
   }
   var prevMonth = prevNextMonth("prev", firstDayIndex) || null;
   var nextMonth = prevNextMonth("next", lastDayIndex) || null;
   renderer.renderCalendar(date, res.pop(), prevMonth, nextMonth);
   return;
};

/**
 * Returns a rendered calendar
 * @see #renderCalendar
 * @type String
 */
jala.Date.Calendar.prototype.getCalendar = function(today) {
   res.push();
   this.render(today);
   return res.pop();
};

/**
 * Returns a new instance of the default calendar renderer.
 * @class A default renderer to use in conjunction with jala.Date.Calendar
 * @param {jala.Date.Calendar} calendar The calendar utilizing this renderer
 * @returns A newly created instance of jala.Date.Calendar.Renderer
 * @constructor
 */
jala.Date.Calendar.Renderer = function(calendar) {

   /**
    * An instance of helma.Html used for rendering the calendar
    * @type helma.Html
    */
   this.html = new helma.Html();

   /**
    * The calendar utilizing this renderer instance
    * @type jala.Date.Calendar
    */
   this.calendar = calendar;

   return this;
};

/** @ignore */
jala.Date.Calendar.Renderer.prototype.toString = function() {
   return "[Jala Calendar Default Renderer]";
};

/**
 * Renders a single cell in the calendar day header row directly to response.
 * @param {String} text The text to display in the header field.
 */
jala.Date.Calendar.Renderer.prototype.renderDayHeader = function(text) {
   this.html.element("th", text);
   return;
};

/**
 * Renders a single calendar row directly to response.
 * @param {String} row The body of the calendar row.
 */
jala.Date.Calendar.Renderer.prototype.renderRow = function(row) {
   this.html.element("tr", row);
   return;
};

/**
 * Renders a single day within the calendar directly to response.
 * @param {Date} date A date instance representing the day within the calendar.
 * @param {Boolean} isExisting True if there is a child object in the calendar's
 * collection to which the date cell should link to
 * @param {Boolean} isSelected True if this calendar day should be rendered
 * as selected day.
 */
jala.Date.Calendar.Renderer.prototype.renderDay = function(date, isExisting, isSelected) {
   var attr = {"class": "jala-calendar-day day"};
   if (isSelected === true) {
      attr["class"] += " jala-calendar-selected selected";
   }
   this.html.openTag("td", attr);
   if (date != null) {
      var text = date.getDate();
      if (isExisting === true) {
         attr = {"href": this.calendar.getCollection().href() +
                         date.format(this.calendar.getHrefFormat())};
         this.html.link(attr, text);
      } else {
         res.write(text);
      }
   }
   this.html.closeTag("td");
   return;
};

/**
 * Renders a link to the previous or next month's calendar directly to response.
 * @param {Date} date A date object set to the previous or next available
 * month. This can be null in case there is no previous or next month.
 */
jala.Date.Calendar.Renderer.prototype.renderPrevNextLink = function(date) {
   if (date != null) {
      var attr = {"href": this.calendar.getCollection().href() +
                          date.format(this.calendar.getHrefFormat())};
      this.html.link(attr, date.format("MMMM", this.calendar.getLocale()));
   }
   return;
};

/**
 * Renders the calendar directly to response.
 * @param {Date} date A date object representing this calendar's month and year.
 * Please mind that the day will be set to the <em>last</em> date in this
 * month.
 * @param {String} body The rendered calendar weeks including the day header
 * (basically the whole kernel of the table).
 * @param {Date} prevMonth A date object set to the last available date of
 * the previous month. This can be used to render a navigation link to
 * the previous month.
 * @param {Date} nextMonth A date object set to the first available date
 * of the next month. This can be used to render a navigation link to
 * the next month.
 */
jala.Date.Calendar.Renderer.prototype.renderCalendar = function(date, body, prevMonth, nextMonth) {
   var locale = this.calendar.getLocale();
   this.html.openTag("table", {"class": "jala-calendar calendar"});
   this.html.openTag("thead");
   this.html.openTag("tr");
   this.html.openTag("th", {"colspan": 7});
   res.write(date.format("MMMM", locale));
   res.write(' ');
   res.write(date.format("yyyy", locale));
   this.html.closeTag("th");
   this.html.closeTag("tr");
   this.html.closeTag("thead");
   this.html.element("tbody", body);
   this.html.openTag("tfoot");
   this.html.openTag("tr");
   this.html.openTag("td", {"class": "jala-calendar-left left", "colspan": 3});
   this.renderPrevNextLink(prevMonth);
   this.html.closeTag("td");
   this.html.openTag("td");
   this.html.closeTag("td");
   this.html.openTag("td", {"class": "jala-calendar-right right", "colspan": 3});
   this.renderPrevNextLink(nextMonth);
   this.html.closeTag("td");
   this.html.closeTag("tr");
   this.html.closeTag("tfoot");
   this.html.closeTag("table");
   return;
};

/**
 * Default date class instance.
 * @type jala.Date
 * @final
 */
jala.date = new jala.Date();
