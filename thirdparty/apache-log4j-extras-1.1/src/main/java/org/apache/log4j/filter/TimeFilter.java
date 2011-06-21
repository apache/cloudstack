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

package org.apache.log4j.filter;

import org.apache.log4j.helpers.LogLog;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Filters events that fall within a specified time period
 * in each day.
 *
*/
public final class TimeFilter extends Filter {

  private boolean acceptOnMatch;
    /**
     * Starting offset from midnight in milliseconds.
     */
  private long start;
    /**
     * Ending offset from midnight in milliseconds.
     */
  private long end;
    /**
     * Timezone.
     */
  private Calendar calendar;


    /**
     * Length of hour in milliseconds.
     */
  private static final long HOUR_MS = 3600000;

    /**
     * Length of minute in milliseconds.
     */
  private static final long MINUTE_MS = 60000;

    /**
     * Length of second in milliseconds.
     */
  private static final long SECOND_MS = 1000;

    /**
     * Constructor.
     */
  public TimeFilter() {
      acceptOnMatch = true;
      start = 0;
      end = Long.MAX_VALUE;
      calendar = Calendar.getInstance();
  }

    /**
     * Set start (inclusive) of time span.
     * @param s string representation of start time as HH:mm:ss.
     */
  public void setStart(final String s) {
      SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss");
      stf.setTimeZone(TimeZone.getTimeZone("UTC"));
      try {
         start = stf.parse(s).getTime();
      } catch(ParseException ex) {
          LogLog.warn("Error parsing start value " + s, ex);
      }
  }

    /**
     * Set end (exclusive) of time span.
     * @param s string representation of end time as HH:mm:ss.
     */
  public void setEnd(final String s) {
        SimpleDateFormat stf = new SimpleDateFormat("HH:mm:ss");
        stf.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            end = stf.parse(s).getTime();
        } catch(ParseException ex) {
            LogLog.warn("Error parsing end value " + s, ex);
        }
    }

    /**
     * Set timezone.
     * @param s time zone.
     */
  public void setTimeZone(final String s) {
      if (s == null) {
          calendar = Calendar.getInstance();
      } else {
        calendar = Calendar.getInstance(TimeZone.getTimeZone(s));
      }
  }

    /**
     * Sets whether an event within the timespan should be accepted or denied.
     * @param acceptOnMatch true if matching event should be accepted.
     */
  public synchronized void setAcceptOnMatch(boolean acceptOnMatch) {
    this.acceptOnMatch = acceptOnMatch;
  }

    /**
     * Gets whether an event within the timespan should be accepted or denied.
     * @return true if matching event should be accepted.
     */
  public synchronized boolean getAcceptOnMatch() {
    return acceptOnMatch;
  }

  /** {@inheritDoc} */
  public int decide(final LoggingEvent event) {
    calendar.setTimeInMillis(event.timeStamp);
    //
    //   get apparent number of milliseconds since midnight
    //      (ignores extra or missing hour on daylight time changes).
    //
    long apparentOffset = calendar.get(Calendar.HOUR_OF_DAY) * HOUR_MS +
        calendar.get(Calendar.MINUTE) * MINUTE_MS +
        calendar.get(Calendar.SECOND) * SECOND_MS +
        calendar.get(Calendar.MILLISECOND);
    if (apparentOffset >= start && apparentOffset < end) {
        if (acceptOnMatch) {
            return Filter.ACCEPT;
        } else {
            return Filter.DENY;
        }
    }
    return Filter.NEUTRAL;
  }
}

