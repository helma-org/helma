// DatePrototype.java
// FESI Copyright (c) Jean-Marc Lugrin, 1999
//
// This program is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2 of the License, or (at your option) any later version.

// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.

// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA

package FESI.Data;

import FESI.Exceptions.*;
import FESI.Interpreter.*;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Implements the prototype and is the class of all Date objects
 */
public class DatePrototype extends ESObject {

    // The value
    protected Date date = null;
        
    /**
     * Create a new Date object with a null date
     * @param prototype the Date prototype
     * @param evaluator the Evaluator
     */
    DatePrototype(ESObject prototype, Evaluator evaluator) {
        super(prototype, evaluator);
        date = new Date();
    }
    
    /**
     * Create a new Date object with a specified date
     * @param prototype the Date prototype
     * @param evaluator the Evaluator
     * @param date the Date
     */
    public DatePrototype (Evaluator evaluator, Date aDate) {
       super(evaluator.getDatePrototype(), evaluator);
       date = new Date(aDate.getTime());
   }
  
    /**
     * Create a new Date object with a specified date
     * @param prototype the Date prototype
     * @param evaluator the Evaluator
     * @param time the Date
     */
    public DatePrototype (Evaluator evaluator, long time) {
       super(evaluator.getDatePrototype(), evaluator);
       date = new Date(time);
    }
  
    // overrides
    public String getESClassName() {
        return "Date";
    }
    
    /**
     * Set the year value of the date. BEWARE: Fixed as base 1900 !
     * @param arguments The array of arguments, the first one being the year
     * @return the new date as a number
     */
    public ESValue setYear(ESValue[] arguments) throws EcmaScriptException {
        if (date == null) {
               return new ESNumber(Double.NaN);
        } else {
              if (arguments.length<=0) {
                  date = null;
                  return new ESNumber(Double.NaN);
              }
              GregorianCalendar cal = new GregorianCalendar(TimeZone.getDefault());
              cal.setTime(date);
              double d = arguments[0].doubleValue();
              if (Double.isNaN(d)) {
                      date = null;
                      return new ESNumber(Double.NaN);
              }
              if (d <100) d+= 1900;
              // System.out.println("SETYEAR to " + d);
              cal.set(Calendar.YEAR, (int) d);
              date = cal.getTime();
              long t = date.getTime();
              return new ESNumber((double) t);
        }
    }
    

    /**
     * Set the time value of the date based on the element type to change
     * Assume that the time elements are in the local time zone
     * @param arguments The array of arguments
     * @para, argTypes The array of element type
     * @return the new date as a number
     */
    public ESValue setTime(ESValue[] arguments, int [] argTypes) throws EcmaScriptException {
        if (date == null) {
                       return new ESNumber(Double.NaN);
        } else {
              if (arguments.length<=0) {
                  date = null;
                  return new ESNumber(Double.NaN);
              }
              GregorianCalendar cal = new GregorianCalendar(TimeZone.getDefault());
              cal.setTime(date);
              for (int iarg=0; (iarg<argTypes.length) && (iarg<arguments.length); iarg++) {
                 double d = arguments[iarg].doubleValue();
                 if (Double.isNaN(d)) {
                      date = null;
                      return new ESNumber(Double.NaN);
                 }
                 // System.out.println("SET " + argTypes[iarg] + " to " + d);
                 cal.set(argTypes[iarg], (int) d);
              }
              date = cal.getTime();
              long t = date.getTime();
              return new ESNumber((double) t);
        }
    }
    
    /**
     * Set the time value of the date based on the element type to change
     * Assume that the time elements are in the UTC time zone
     * @param arguments The array of arguments
     * @para, argTypes The array of element type
     * @return the new date as a number
     */
    public ESValue setUTCTime(ESValue[] arguments, int [] argTypes) throws EcmaScriptException {
        if (date == null) {
                       return new ESNumber(Double.NaN);
        } else {
              if (arguments.length<=0) {
                  date = null;
                  return new ESNumber(Double.NaN);
              }
              GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
              cal.setTime(date);
              for (int iarg=0; (iarg<argTypes.length) && (iarg<arguments.length); iarg++) {
                 double d = arguments[iarg].doubleValue();
                 if (Double.isNaN(d)) {
                      date = null;
                      return new ESNumber(Double.NaN);
                 }
                 // System.out.println("UTCSET " + argTypes[iarg] + " to " + d);
                 cal.set(argTypes[iarg], (int) d);
              }
              date = cal.getTime();
              long t = date.getTime();
              return new ESNumber((double) t);
        }
    }

    /**
     * Get an element of the date (in local time zone)
     * @param element The type of the element
     * @return the element as a value
     */
    public ESValue get(int element) {
        if (date == null) {
                       return new ESNumber(Double.NaN);
        } else {
              GregorianCalendar cal = new GregorianCalendar(TimeZone.getDefault());
              cal.setTime(date);
              long t = cal.get(element);
              // EcmaScript has SUNDAY=0, java SUNDAY=1 - converted in DatePrototype
              if (element == Calendar.DAY_OF_WEEK) t--;
              return new ESNumber((double) t);
        }
    }

    /**
     * Get an element of the date (in UTC time zone)
     * @param element The type of the element
     * @return the element as a value
     */
    public ESValue getUTC(int element) {
        if (date == null) {
                       return new ESNumber(Double.NaN);
        } else {
              GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
              cal.setTime(date);
              long t = cal.get(element);
              // EcmaScript has SUNDAY=0, java SUNDAY=1 - converted in DatePrototype
              if (element == Calendar.DAY_OF_WEEK) t--;
              return new ESNumber((double) t);
        }
    }

    // overrides
    public String toString() {
         return (date==null ? "null" : date.toString());
    }

    // overrides
    public String toDetailString() {
        return "ES:[Object: builtin " + this.getClass().getName() + ":" + 
            ((date == null) ? "null" : date.toString()) + "]";
    }
    
    // overrides
    public Object toJavaObject() {
        return date;
    }

    // overrides
    public ESValue getDefaultValue() 
                                throws EcmaScriptException {
        return this.getDefaultValue(EStypeString);
    }

}