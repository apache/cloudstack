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

package org.apache.log4j.pattern;

import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.LoggingEvent;

import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Date;
import java.util.TimeZone;


/**
 * Convert and format the event's date in a StringBuffer.
 *
 * @author Ceki G&uuml;lc&uuml;
 */
public final class DatePatternConverter extends LoggingEventPatternConverter {
    /**
     * ABSOLUTE string literal.
     */
  private static final String ABSOLUTE_FORMAT = "ABSOLUTE";
    /**
     * SimpleTimePattern for ABSOLUTE.
     */
  private static final String ABSOLUTE_TIME_PATTERN = "HH:mm:ss,SSS";


    /**
     * DATE string literal.
     */
  private static final String DATE_AND_TIME_FORMAT = "DATE";
    /**
     * SimpleTimePattern for DATE.
     */
  private static final String DATE_AND_TIME_PATTERN = "dd MMM yyyy HH:mm:ss,SSS";

    /**
     * ISO8601 string literal.
     */
  private static final String ISO8601_FORMAT = "ISO8601";
    /**
     * SimpleTimePattern for ISO8601.
     */
  private static final String ISO8601_PATTERN = "yyyy-MM-dd HH:mm:ss,SSS";
  /**
   * Date format.
   */
  private final CachedDateFormat df;

    /**
     * This class wraps a DateFormat and forces the time zone to the
     *   default time zone before each format and parse request.
     */
  private static class DefaultZoneDateFormat extends DateFormat {
     /**
      * Serialization version ID.
      */
     private static final long serialVersionUID = 1;
     /**
         * Wrapped instance of DateFormat.
         */
    private final DateFormat dateFormat;

        /**
         * Construct new instance.
         * @param format format, may not be null.
         */
    public DefaultZoneDateFormat(final DateFormat format) {
        dateFormat = format;
    }

        /**
         * @{inheritDoc}
         */
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        dateFormat.setTimeZone(TimeZone.getDefault());
        return dateFormat.format(date, toAppendTo, fieldPosition);
    }

        /**
         * @{inheritDoc}
         */
    public Date parse(String source, ParsePosition pos) {
        dateFormat.setTimeZone(TimeZone.getDefault());
        return dateFormat.parse(source, pos);
    }
  }
  
  /**
   * Private constructor.
   * @param options options, may be null.
   */
  private DatePatternConverter(final String[] options) {
    super("Date", "date");

    String patternOption;

    if ((options == null) || (options.length == 0)) {
      // the branch could be optimized, but here we are making explicit
      // that null values for patternOption are allowed.
      patternOption = null;
    } else {
      patternOption = options[0];
    }

    String pattern;

    if (
      (patternOption == null)
        || patternOption.equalsIgnoreCase(ISO8601_FORMAT)) {
      pattern = ISO8601_PATTERN;
    } else if (patternOption.equalsIgnoreCase(ABSOLUTE_FORMAT)) {
      pattern = ABSOLUTE_TIME_PATTERN;
    } else if (patternOption.equalsIgnoreCase(DATE_AND_TIME_FORMAT)) {
      pattern = DATE_AND_TIME_PATTERN;
    } else {
      pattern = patternOption;
    }

    int maximumCacheValidity = 1000;
    DateFormat simpleFormat = null;

    try {
      simpleFormat = new SimpleDateFormat(pattern);
      maximumCacheValidity = CachedDateFormat.getMaximumCacheValidity(pattern);
    } catch (IllegalArgumentException e) {
        LogLog.warn(
          "Could not instantiate SimpleDateFormat with pattern "
          + patternOption, e);

      // default to the ISO8601 format
      simpleFormat = new SimpleDateFormat(ISO8601_PATTERN);
    }

    // if the option list contains a TZ option, then set it.
    if ((options != null) && (options.length > 1)) {
      TimeZone tz = TimeZone.getTimeZone((String) options[1]);
      simpleFormat.setTimeZone(tz);
    } else {
      simpleFormat = new DefaultZoneDateFormat(simpleFormat);
    }

    df = new CachedDateFormat(simpleFormat, maximumCacheValidity);
  }

  /**
   * Obtains an instance of pattern converter.
   * @param options options, may be null.
   * @return instance of pattern converter.
   */
  public static DatePatternConverter newInstance(
    final String[] options) {
    return new DatePatternConverter(options);
  }

  /**
   * {@inheritDoc}
   */
  public void format(final LoggingEvent event, final StringBuffer output) {
    synchronized(this) {
    	df.format(event.timeStamp, output);
    }
  }

  /**
   * {@inheritDoc}
   */
  public void format(final Object obj, final StringBuffer output) {
    if (obj instanceof Date) {
      format((Date) obj, output);
    }

    super.format(obj, output);
  }

  /**
   * Append formatted date to string buffer.
   * @param date date
   * @param toAppendTo buffer to which formatted date is appended.
   */
  public void format(final Date date, final StringBuffer toAppendTo) {
    synchronized(this) {
    	df.format(date.getTime(), toAppendTo);
    }
  }
}
