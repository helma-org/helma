package helma.util;

/**
  * Modifications by Stefan Pollach, 2002-08, 2003-03
  * First, to compile without protomatter-library (we're only interested in the
  * parsing functions for helma), second to encapsulate a function call and not
  * a PASEvent
  */

/**
 *  The Protomatter Software License, Version 1.0
 *  derived from The Apache Software License, Version 1.1
 *  
 *  Copyright (c) 1998-2002 Nate Sammons.  All rights reserved.
 *  
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions
 *  are met:
 *  
 *  1. Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *  
 *  2. Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in
 *     the documentation and/or other materials provided with the
 *     distribution.
 *  
 *  3. The end-user documentation included with the redistribution,
 *     if any, must include the following acknowledgment:
 *        "This product includes software developed for the
 *         Protomatter Software Project
 *         (http://protomatter.sourceforge.net/)."
 *     Alternately, this acknowledgment may appear in the software itself, 
 *     if and wherever such third-party acknowledgments normally appear.
 *  
 *  4. The names "Protomatter" and "Protomatter Software Project" must
 *     not be used to endorse or promote products derived from this
 *     software without prior written permission. For written
 *     permission, please contact support@protomatter.com.
 *  
 *  5. Products derived from this software may not be called "Protomatter", 
 *     nor may "Protomatter" appear in their name, without prior written
 *     permission of the Protomatter Software Project
 *     (support@protomatter.com).
 *  
 *  THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 *  WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 *  OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED.  IN NO EVENT SHALL THE PROTOMATTER SOFTWARE PROJECT OR
 *  ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 *  SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 *  LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 *  USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *  OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 *  OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 *  SUCH DAMAGE.
 */

import java.util.*;


/**
 *  A cron entry, derived from Protomatter's CronEntry class.
 *  This class encapsulates a function call, a timeout value 
 *  and a specification for when the given event should be
 *  delivered to the given topics.  The specification of when
 *  the event should be delivered is based on the UNIX cron
 *  facility.
 */


public class CronJob {

   // used as the value in hashtables
   private static Object value = new Object();
   private static Hashtable all = new Hashtable ();
   private static String ALL_VALUE = "*";

   private Hashtable year;
   private Hashtable month;
   private Hashtable day;
   private Hashtable weekday;
   private Hashtable hour;
   private Hashtable minute;

   private String name     = null;
   private String function = null;
   private long   timeout = 20000;

   /** A method for parsing properties. It looks through the properties
     * file for entries that look like this:
     *
     * <blockquote><pre>
     *  cron.name1.function = functionname
     *
     *  cron.name1.year     = year-list
     *  cron.name1.month    = month-list
     *  cron.name1.day      = day-list
     *  cron.name1.weekday  = weekday-list
     *  cron.name1.hour     = hour-list
     *  cron.name1.minute   = minute-list
     *
     *  cron.name1.timeout  = timeout-value
     *  
     *  </pre></blockquote><p>
     *
     *  And delivers corresponding <tt>CronJob</tt> objects in a collection.
     *  The specified lists from above are:<P>
     *
     *  <ul><dl>
     *  <dt><tt>year-list</tt></dt>
     **  <dd>
     **  This is a comma (<tt>,</tt>) separated list of individual
     *  years or of year ranges.  Examples: "<tt>1999,2000</tt>" or
     *  "<tt>1999-2004,2005-2143,2650</tt>"
     *  </dd><P>
     *
     *  <dt><tt>month-list</tt></dt>
     *  <dd>
     *  This is a comma (<tt>,</tt>) separated list of month
     *  names.  Example: "<tt>january,march,may</tt>"
     *  </dd><P>
     *
     *  <dt><tt>day-list</tt></dt>
     *  <dd>
     *  This is a comma (<tt>,</tt>) separated list of individual
     *  day-of-month numbers or of day-of-month ranges.
     *  Examples: "<tt>1,15</tt>" or "<tt>1-5,7,10-24</tt>"
     *  </dd><P>
     *
     *  <dt><tt>weekday-list</tt></dt>
     *  <dd>
     *  This is a comma (<tt>,</tt>) separated list of weekday names
     *  names.  Example: "<tt>monday,tuesday</tt>"
     *  </dd><P>
     *
     *  <dt><tt>hour-list</tt></dt>
     *  <dd>
     *  This is a comma (<tt>,</tt>) separated list of individual
     *  hours-of-day (24-hour time) or of hour-of-day ranges.
     *  Examples: "<tt>12,15</tt>" or "<tt>8-17,19,20-22</tt>"
     *  </dd><P>
     *
     *  <dt><tt>minute-list</tt></dt>
     *  <dd>
     *  This is a comma (<tt>,</tt>) separated list of individual
     *  minutes (during an hour) or of minute ranges.
     *  Examples: "<tt>0,15,30,45</tt>" or "<tt>0-5,8-14,23,28-32</tt>"
     *  </dd><P>
     *
     *  </dl></ul>
     *
     *  The value of each of those lists can also be an asterisk (<tt>*</tt>),
     *  which tells the cron system to disregard the given list when
     *  determining if a cron entry applies to a specific time -- for instance
     *  setting the <tt>year-list</tt> to <tt>*</tt> would cause the system to
     *  not take the current year into consideration.  If a given list is
     *  not specified at all, it's the same as specifying it and giving it
     *  a value of <tt>*</tt>.<P>
     */


  public static Vector parse(Properties props) {
      Hashtable jobs = new Hashtable ();
      Enumeration e = props.keys ();
      while (e.hasMoreElements ()) {
         String key = (String) e.nextElement ();
         if (!key.startsWith ("cron."))
            continue;
         try {
            StringTokenizer st = new StringTokenizer (key.trim(), ".");
            st.nextElement ();
            String jobName = st.nextToken ();
            String jobSpec = st.nextToken ();
            if (jobSpec==null || jobSpec.equals("")) // might happen with cron.testname. = XXX
               continue;
            CronJob job = (CronJob) jobs.get (jobName);
            if (job==null) {
               job = new CronJob (jobName);
               jobs.put (jobName, job);
            }
            String value = props.getProperty (key);
            if (jobSpec.equalsIgnoreCase("function")) {
               job.setFunction(value);
            } else if (jobSpec.equalsIgnoreCase("year")) {
               parseYear (job, value);
            } else if (jobSpec.equalsIgnoreCase("month")) {
               parseMonth (job, value);
            } else if (jobSpec.equalsIgnoreCase("day")) {
               parseDay (job, value);
            } else if (jobSpec.equalsIgnoreCase("weekday")) {
               parseWeekDay (job, value);
            } else if (jobSpec.equalsIgnoreCase("hour")) {
               parseHour (job, value);
            } else if (jobSpec.equalsIgnoreCase("minute")) {
               parseMinute (job, value);
            } else if (jobSpec.equalsIgnoreCase("timeout")) {
            	parseTimeout (job, value);
            }
         } catch (NoSuchElementException nsee) {
         }
      }
      Vector jobVec = new Vector (jobs.values ());
      Collections.sort (jobVec, new Comparator() {
        public int compare (Object o1, Object o2) {
            CronJob cron1 = (CronJob) o1;
            CronJob cron2 = (CronJob) o2;
            if (cron1.getTimeout () > cron2.getTimeout ())
                return 1;
            else if (cron1.getTimeout () < cron2.getTimeout ())
                return -1;
            else
                return 0;
        }
        public boolean equals (Object o1, Object o2) {
            if (o1!=null) {
                return o1.equals (o2);
            } else {
                return false;
            }
        }

        });
      return jobVec;
   }



   public static void parseYear (CronJob job, String value) {
      if (value.equals("*")) {
         job.setAllYears(true);
      } else {
         StringTokenizer st = new StringTokenizer(value.trim(), ",");
         while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s.indexOf("-") != -1) {
               int start = Integer.parseInt(s.substring(0, s.indexOf("-")));
               int finish = Integer.parseInt(s.substring(s.indexOf("-") +1));
               for (int i=start; i<=finish; i++) {
                  job.addYear(i);
               }
            } else {
               int y = Integer.parseInt(s);
               job.addYear(y);
            }
         }
      }
   }

   public static void parseMonth (CronJob job, String value) {
      if (value.equals("*")) {
         job.setAllMonths(true);
      } else {
         StringTokenizer st = new StringTokenizer(value.trim(), ",");
         while (st.hasMoreTokens()) {
            String m = st.nextToken();
            if (m.equalsIgnoreCase("january"))
               job.addMonth(Calendar.JANUARY);
            if (m.equalsIgnoreCase("february"))
               job.addMonth(Calendar.FEBRUARY);
            if (m.equalsIgnoreCase("march"))
               job.addMonth(Calendar.MARCH);
            if (m.equalsIgnoreCase("april"))
               job.addMonth(Calendar.APRIL);
            if (m.equalsIgnoreCase("may"))
               job.addMonth(Calendar.MAY);
            if (m.equalsIgnoreCase("june"))
               job.addMonth(Calendar.JUNE);
            if (m.equalsIgnoreCase("july"))
               job.addMonth(Calendar.JULY);
            if (m.equalsIgnoreCase("august"))
               job.addMonth(Calendar.AUGUST);
            if (m.equalsIgnoreCase("september"))
               job.addMonth(Calendar.SEPTEMBER);
            if (m.equalsIgnoreCase("october"))
               job.addMonth(Calendar.OCTOBER);
            if (m.equalsIgnoreCase("november"))
               job.addMonth(Calendar.NOVEMBER);
            if (m.equalsIgnoreCase("december"))
               job.addMonth(Calendar.DECEMBER);
         }
      }
   }

   public static void parseDay (CronJob job, String day) {
      if (day.equals("*")) {
         job.setAllDays(true);
      } else {
         StringTokenizer st = new StringTokenizer(day.trim(), ",");
         while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s.indexOf("-") != -1) {
               int start = Integer.parseInt(s.substring(0, s.indexOf("-")));
               int finish = Integer.parseInt(s.substring(s.indexOf("-") +1));
               for (int i=start; i<=finish; i++) {
                  job.addDay(i);
               }
            } else {
               int d = Integer.parseInt(s);
               job.addDay(d);
            }
         }
      }
   }


   public static void parseWeekDay (CronJob job, String weekday) {
      if (weekday.equals("*")) {
         job.setAllWeekdays(true);
      } else {
         StringTokenizer st = new StringTokenizer(weekday.trim(), ",");
         while (st.hasMoreTokens()) {
            String d = st.nextToken();
            if (d.equalsIgnoreCase("monday"))
               job.addWeekday(Calendar.MONDAY);
            if (d.equalsIgnoreCase("tuesday"))
               job.addWeekday(Calendar.TUESDAY);
            if (d.equalsIgnoreCase("wednesday"))
               job.addWeekday(Calendar.WEDNESDAY);
            if (d.equalsIgnoreCase("thursday"))
               job.addWeekday(Calendar.THURSDAY);
            if (d.equalsIgnoreCase("friday"))
               job.addWeekday(Calendar.FRIDAY);
            if (d.equalsIgnoreCase("saturday"))
               job.addWeekday(Calendar.SATURDAY);
            if (d.equalsIgnoreCase("sunday"))
               job.addWeekday(Calendar.SUNDAY);
         }
      }
   }


   public static void parseHour (CronJob job, String hour) {
      if (hour.equals("*")) {
         job.setAllHours(true);
      } else {
         StringTokenizer st = new StringTokenizer(hour.trim (), ",");
         while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s.indexOf("-") != -1) {
               int start = Integer.parseInt(s.substring(0, s.indexOf("-")));
               int finish = Integer.parseInt(s.substring(s.indexOf("-") +1));
               for (int i=start; i<=finish; i++) {
                  job.addHour(i);
               }
            } else {
               int h = Integer.parseInt(s);
               job.addHour(h);
            }
         }
      }
   }


   public static void parseMinute (CronJob job, String minute) {
      if (minute.equals("*")) {
         job.setAllMinutes(true);
      } else {
         StringTokenizer st = new StringTokenizer(minute.trim (), ",");
         while (st.hasMoreTokens()) {
            String s = st.nextToken();
            if (s.indexOf("-") != -1) {
               int start = Integer.parseInt(s.substring(0, s.indexOf("-")));
               int finish = Integer.parseInt(s.substring(s.indexOf("-") +1));
               for (int i=start; i<=finish; i++) {
                  job.addMinute(i);
               }
            } else {
               int m = Integer.parseInt(s);
               job.addMinute(m);
            }
         }
      }
   }


   public static void parseTimeout (CronJob job, String timeout) {
      long timeoutValue = 1000 * Long.valueOf(timeout).longValue ();
      job.setTimeout (timeoutValue);
   }

	public static long nextFullMinute () {
		long now = System.currentTimeMillis();
      long millisAfterMinute = (now % 60000);
		return (now + 60000 - millisAfterMinute);
	}

	public static long millisToNextFullMinute () {
		long now = System.currentTimeMillis();
      long millisAfterMinute = (now % 60000);
		return (60000 - millisAfterMinute);
	}

  /**
   *  Create an empty CronJob.
   */
   public CronJob (String name) {
      this.name = name;
      all.put (ALL_VALUE, value);
      year = new Hashtable (all);
      month = new Hashtable (all);
      day = new Hashtable (all);
      weekday = new Hashtable (all);
      hour = new Hashtable (all);
      minute = new Hashtable (all);
   }

  /**
   *  Determines if this CronJob applies to the given date.
   *  Seconds and milliseconds in the date are ignored.
   */
  public boolean appliesToDate(Date date)
  {
    GregorianCalendar cal = new GregorianCalendar();
    cal.setTime(date);

    // try and short-circuit as fast as possible.
    Integer theYear = new Integer(cal.get(Calendar.YEAR));
    if (!year.containsKey(ALL_VALUE) && !year.containsKey(theYear))
      return false;

    Integer theMonth = new Integer(cal.get(Calendar.MONTH));
    if (!month.containsKey(ALL_VALUE) && !month.containsKey(theMonth))
      return false;
    
    Integer theDay = new Integer(cal.get(Calendar.DAY_OF_MONTH));
    if (!day.containsKey(ALL_VALUE) && !day.containsKey(theDay))
      return false;
    
    Integer theWeekDay = new Integer(cal.get(Calendar.DAY_OF_WEEK));
    if (!weekday.containsKey(ALL_VALUE) && !weekday.containsKey(theWeekDay))
      return false;
    
    Integer theHour = new Integer(cal.get(Calendar.HOUR_OF_DAY));
    if (!hour.containsKey(ALL_VALUE) && !hour.containsKey(theHour))
      return false;
    
    Integer theMinute = new Integer(cal.get(Calendar.MINUTE));
    if (!minute.containsKey(ALL_VALUE) && !minute.containsKey(theMinute))
      return false;
    
    return true;
  }


  /**
   *  Add a year to the list of years this entry applies to.
   */
  public void addYear(int year)
  {
    this.year.remove(ALL_VALUE);
    this.year.put(new Integer(year), value);
  }

  /**
   *  Remove a year from the list of years this entry applies to.
   */
  public void removeYear(int year)
  {
    this.year.remove(new Integer(year));
  }

  /**
   *  Should the current year be taken into consideration when
   *  deciding if this entry is applicable?
   *  If this is set to false (the default) then the values set with
   *  the <tt>addYear()</tt> and <tt>removeYear()</tt> are taken
   *  into consideration.  If this is set to true then the current
   *  year is not taken into consideration.
   */
  public void setAllYears(boolean set)
  {
    if (set)
      this.year.put(ALL_VALUE, value);
    else
      this.year.remove(ALL_VALUE);
  }


  /**
   *  Add a month to the list of years this entry applies to.
   *  Month numbers are taken from the constants on the
   *  <tt>java.util.Calendar</tt> class.
   */
  public void addMonth(int month)
  {
    this.month.remove(ALL_VALUE);
    this.month.put(new Integer(month), value);
  }

  /**
   *  Remove a month from the list of years this entry applies to.
   *  Month numbers are taken from the constants on the
   *  <tt>java.util.Calendar</tt> class.
   */
  public void removeMonth(int month)
  {
    this.month.remove(new Integer(month));
  }

  /**
   *  Should the current month be taken into consideration when
   *  deciding if this entry is applicable?
   *  If this is set to false (the default) then the values set with
   *  the <tt>addMonth()</tt> and <tt>removeMonth()</tt> are taken
   *  into consideration.  If this is set to true then the current
   *  month is not taken into consideration.
   */
  public void setAllMonths(boolean set)
  {
    if (set)
      this.month.put(ALL_VALUE, value);
    else
      this.month.remove(ALL_VALUE);
  }


  /**
   *  Add a day of the month to the list of years this entry applies to.
   */
  public void addDay(int day)
  {
    this.day.remove(ALL_VALUE);
    this.day.put(new Integer(day), value);
  }

  /**
   *  Remove a day of the month from the list of years this entry applies to.
   */
  public void removeDay(int day)
  {
    this.day.remove(new Integer(day));
  }

  /**
   *  Should the current day of the month be taken into consideration when
   *  deciding if this entry is applicable?
   *  If this is set to false (the default) then the values set with
   *  the <tt>addDay()</tt> and <tt>removeDay()</tt> are taken
   *  into consideration.  If this is set to true then the current
   *  year is not taken into consideration.
   */
  public void setAllDays(boolean set)
  {
    if (set)
      this.day.put(ALL_VALUE, value);
    else
      this.day.remove(ALL_VALUE);
  }


  /**
   *  Add a weekday to the list of years this entry applies to.
   *  Weekday numbers are taken from the constants on the
   *  <tt>java.util.Calendar</tt> class.
   */
  public void addWeekday(int weekday)
  {
    this.weekday.remove(ALL_VALUE);
    this.weekday.put(new Integer(weekday), value);
  }

  /**
   *  Remove a weekday from the list of years this entry applies to.
   *  Weekday numbers are taken from the constants on the
   *  <tt>java.util.Calendar</tt> class.
   */
  public void removeWeekday(int weekday)
  {
    this.weekday.remove(new Integer(weekday));
  }

  /**
   *  Should the current weekday be taken into consideration when
   *  deciding if this entry is applicable?
   *  If this is set to false (the default) then the values set with
   *  the <tt>addWeekday()</tt> and <tt>removeWeekday()</tt> are taken
   *  into consideration.  If this is set to true then the current
   *  weekday is not taken into consideration.
   */
  public void setAllWeekdays(boolean set)
  {
    if (set)
      this.weekday.put(ALL_VALUE, value);
    else
      this.weekday.remove(ALL_VALUE);
  }


  /**
   *  Add an hour to the list of years this entry applies to.
   */
  public void addHour(int hour)
  {
    this.hour.remove(ALL_VALUE);
    this.hour.put(new Integer(hour), value);
  }

  /**
   *  Remove an hour from the list of years this entry applies to.
   */
  public void removeHour(int hour)
  {
    this.hour.remove(new Integer(hour));
  }

  /**
   *  Should the current hour be taken into consideration when
   *  deciding if this entry is applicable?
   *  If this is set to false (the default) then the values set with
   *  the <tt>addHour()</tt> and <tt>removeHour()</tt> are taken
   *  into consideration.  If this is set to true then the current
   *  hour is not taken into consideration.
   */
  public void setAllHours(boolean set)
  {
    if (set)
      this.hour.put(ALL_VALUE, value);
    else
      this.hour.remove(ALL_VALUE);
  }


  /**
   *  Add a minute to the list of years this entry applies to.
   */
  public void addMinute(int minute)
  {
    this.minute.remove(ALL_VALUE);
    this.minute.put(new Integer(minute), value);
  }

  /**
   *  Remove a minute from the list of years this entry applies to.
   */
  public void removeMinute(int minute)
  {
    this.minute.remove(new Integer(minute));
  }

  /**
   *  Should the current minute be taken into consideration when
   *  deciding if this entry is applicable?
   *  If this is set to false (the default) then the values set with
   *  the <tt>addMinute()</tt> and <tt>removeMinute()</tt> are taken
   *  into consideration.  If this is set to true then the current
   *  minute is not taken into consideration.
   */
  public void setAllMinutes(boolean set)
  {
    if (set)
      this.minute.put(ALL_VALUE, value);
    else
      this.minute.remove(ALL_VALUE);
  }


  /**
   *  Set this entry's name
   */
  public void setName(String name)
  {
    this.name = name;
  }

  /**
   *  Get this entry's name
   */
  public String getName()
  {
    return this.name;
  }

  /**
   *  Set this entry's function
   */
  public void setFunction(String function)
  {
    this.function = function;
  }

  /**
   *  Get this entry's function
   */
  public String getFunction()
  {
    return this.function;
  }


  /**
   *  Set this entry's timeout
   */
  public void setTimeout(long timeout)
  {
    this.timeout = timeout;
  }


  /**
   *  Get this entry's timeout
   */
  public long getTimeout()
  {
    return this.timeout;
  }
  
  public String toString () {
  		return "[CronJob " + name + "]";
	}
  
}
