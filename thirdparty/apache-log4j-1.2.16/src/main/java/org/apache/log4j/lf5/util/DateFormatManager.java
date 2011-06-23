/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.log4j.lf5.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Date format manager.
 * Utility class to help manage consistent date formatting and parsing.
 * It may be advantageous to have multiple DateFormatManagers per
 * application.  For example, one for handling the output (formatting) of
 * dates, and another one for handling the input (parsing) of dates.
 *
 * @author Robert Shaw
 * @author Michael J. Sikorsky
 */

// Contributed by ThoughtWorks Inc.
public class DateFormatManager {
  //--------------------------------------------------------------------------
  //   Constants:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Protected Variables:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Variables:
  //--------------------------------------------------------------------------
  private TimeZone _timeZone = null;
  private Locale _locale = null;

  private String _pattern = null;
  private DateFormat _dateFormat = null;

  //--------------------------------------------------------------------------
  //   Constructors:
  //--------------------------------------------------------------------------
  public DateFormatManager() {
    super();
    configure();
  }

  public DateFormatManager(TimeZone timeZone) {
    super();

    _timeZone = timeZone;
    configure();
  }

  public DateFormatManager(Locale locale) {
    super();

    _locale = locale;
    configure();
  }

  public DateFormatManager(String pattern) {
    super();

    _pattern = pattern;
    configure();
  }

  public DateFormatManager(TimeZone timeZone, Locale locale) {
    super();

    _timeZone = timeZone;
    _locale = locale;
    configure();
  }

  public DateFormatManager(TimeZone timeZone, String pattern) {
    super();

    _timeZone = timeZone;
    _pattern = pattern;
    configure();
  }

  public DateFormatManager(Locale locale, String pattern) {
    super();

    _locale = locale;
    _pattern = pattern;
    configure();
  }

  public DateFormatManager(TimeZone timeZone, Locale locale, String pattern) {
    super();

    _timeZone = timeZone;
    _locale = locale;
    _pattern = pattern;
    configure();
  }

  //--------------------------------------------------------------------------
  //   Public Methods:
  //--------------------------------------------------------------------------

  public synchronized TimeZone getTimeZone() {
    if (_timeZone == null) {
      return TimeZone.getDefault();
    } else {
      return _timeZone;
    }
  }

  public synchronized void setTimeZone(TimeZone timeZone) {
    _timeZone = timeZone;
    configure();
  }

  public synchronized Locale getLocale() {
    if (_locale == null) {
      return Locale.getDefault();
    } else {
      return _locale;
    }
  }

  public synchronized void setLocale(Locale locale) {
    _locale = locale;
    configure();
  }

  public synchronized String getPattern() {
    return _pattern;
  }

  /**
   * Set the pattern. i.e. "EEEEE, MMMMM d, yyyy hh:mm aaa"
   */
  public synchronized void setPattern(String pattern) {
    _pattern = pattern;
    configure();
  }


  /**
   * This method has been deprecated in favour of getPattern().
   * @deprecated Use getPattern().
   */
  public synchronized String getOutputFormat() {
    return _pattern;
  }

  /**
   * This method has been deprecated in favour of setPattern().
   * @deprecated Use setPattern().
   */
  public synchronized void setOutputFormat(String pattern) {
    _pattern = pattern;
    configure();
  }

  public synchronized DateFormat getDateFormatInstance() {
    return _dateFormat;
  }

  public synchronized void setDateFormatInstance(DateFormat dateFormat) {
    _dateFormat = dateFormat;
    // No reconfiguration necessary!
  }

  public String format(Date date) {
    return getDateFormatInstance().format(date);
  }

  public String format(Date date, String pattern) {
    DateFormat formatter = null;
    formatter = getDateFormatInstance();
    if (formatter instanceof SimpleDateFormat) {
      formatter = (SimpleDateFormat) (formatter.clone());
      ((SimpleDateFormat) formatter).applyPattern(pattern);
    }
    return formatter.format(date);
  }

  /**
   * @throws java.text.ParseException
   */
  public Date parse(String date) throws ParseException {
    return getDateFormatInstance().parse(date);
  }

  /**
   * @throws java.text.ParseException
   */
  public Date parse(String date, String pattern) throws ParseException {
    DateFormat formatter = null;
    formatter = getDateFormatInstance();
    if (formatter instanceof SimpleDateFormat) {
      formatter = (SimpleDateFormat) (formatter.clone());
      ((SimpleDateFormat) formatter).applyPattern(pattern);
    }
    return formatter.parse(date);
  }

  //--------------------------------------------------------------------------
  //   Protected Methods:
  //--------------------------------------------------------------------------

  //--------------------------------------------------------------------------
  //   Private Methods:
  //--------------------------------------------------------------------------
  private synchronized void configure() {
    _dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.FULL,
        DateFormat.FULL,
        getLocale());
    _dateFormat.setTimeZone(getTimeZone());

    if (_pattern != null) {
      ((SimpleDateFormat) _dateFormat).applyPattern(_pattern);
    }
  }

  //--------------------------------------------------------------------------
  //   Nested Top-Level Classes or Interfaces:
  //--------------------------------------------------------------------------

}
