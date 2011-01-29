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

import junit.framework.TestCase;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.extras.DOMConfigurator;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.util.Compare;
import org.apache.log4j.xml.Log4jEntityResolver;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * Tests for TimeFilter.
 *
 */
public class TimeFilterTest extends TestCase {
    /**
     * Construct new instance.
     * @param testName test name.
     */
    public TimeFilterTest(final String testName) {
        super(testName);
    }

    /**
     * Configure log4j from resource.
     * @param resourceName resource name.
     * @throws Exception if IO or other error.
     */
    private final void configure(final String resourceName) throws Exception {
      Logger.getRootLogger().getLoggerRepository().resetConfiguration();
      InputStream is = getClass().getResourceAsStream(resourceName);
      if (is == null) {
          throw new FileNotFoundException(
                  "Could not find resource " + resourceName);
      }
      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setEntityResolver(new Log4jEntityResolver());
      Document doc = builder.parse(is);
      DOMConfigurator.configure(doc.getDocumentElement());
    }


    /**
     * Test 2 AM events against a 2 AM - 3 AM filter.
     * @param tz time zone, may be null.
     * @param dayIncrement days in advance of current date.
     */
    private void common2AM(String tz, int dayIncrement) {
        TimeFilter timeFilter = new TimeFilter();
        timeFilter.setStart("02:00:00");
        timeFilter.setEnd("03:00:00");
        if (tz != null) {
            timeFilter.setTimeZone(tz);
        }
        timeFilter.activateOptions();

        Calendar cal;
        if (tz == null) {
            cal = Calendar.getInstance();
        } else {
            cal = Calendar.getInstance(TimeZone.getTimeZone(tz));
        }
        cal.set(Calendar.HOUR_OF_DAY, 2);
        if (dayIncrement != 0) {
            cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + dayIncrement);
        }
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(TimeFilterTest.class),
                cal.getTimeInMillis(), Level.INFO, "Hello, world.", null);
        assertEquals(Filter.ACCEPT, timeFilter.decide(event));
        timeFilter.setAcceptOnMatch(false);
        assertEquals(Filter.DENY, timeFilter.decide(event));
    }

    /**
     * Test 3 AM events against a 2 AM - 3 AM filter.
     * @param tz time zone, may be null.
     * @param dayIncrement days in advance of current date.
     */
    private void common3AM(String tz, int dayIncrement) {
        TimeFilter timeFilter = new TimeFilter();
        timeFilter.setStart("02:00:00");
        timeFilter.setEnd("03:00:00");
        if (tz != null) {
            timeFilter.setTimeZone(tz);
        }
        timeFilter.activateOptions();

        Calendar cal;
        if (tz == null) {
            cal = Calendar.getInstance();
        } else {
            cal = Calendar.getInstance(TimeZone.getTimeZone(tz));
        }
        cal.set(Calendar.HOUR_OF_DAY, 3);
        if (dayIncrement != 0) {
            cal.set(Calendar.DAY_OF_YEAR, cal.get(Calendar.DAY_OF_YEAR) + dayIncrement);
        }
        LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                Logger.getLogger(TimeFilterTest.class),
                cal.getTimeInMillis(), Level.INFO, "Hello, world.", null);
        assertEquals(Filter.NEUTRAL, timeFilter.decide(event));
        timeFilter.setAcceptOnMatch(false);
        assertEquals(Filter.NEUTRAL, timeFilter.decide(event));
        timeFilter.setAcceptOnMatch(true);
    }

    /**
     * Test 2 AM local today event against 2 AM - 3 AM local time filter.
     */
    public void test2AMLocal() {
        common2AM(null, 0);
    }

    /**
     * Test 2 AM local yesterday event against 2 AM - 3 AM local time filter.
     */
    public void test2AMLocalYesterday() {
        common2AM(null, -1);
    }

    /**
     * Test 2 AM local tomorrow event against 2 AM - 3 AM local time filter.
     */
    public void test2AMLocalTomorrow() {
        common2AM(null, 1);
    }

    /**
     * Test 2 AM local last week event against 2 AM - 3 AM local time filter.
     */
    public void test2AMLocalLastWeek() {
        common2AM(null, -7);
    }

    /**
     * Test 2 AM local next week event against 2 AM - 3 AM local time filter.
     */
    public void test2AMLocalNextWeek() {
        common2AM(null, 7);
    }

    /**
     * Test 3 AM local today event against 2 AM - 3 AM local time filter.
     */
    public void test3AMLocal() {
        common3AM(null, 0);
    }

    /**
     * Test 3 AM local yesterday event against 2 AM - 3 AM local time filter.
     */
    public void test3AMLocalYesterday() {
        common3AM(null, -1);
    }

    /**
     * Test 3 AM local tomorrow event against 2 AM - 3 AM local time filter.
     */
    public void test3AMLocalTomorrow() {
        common3AM(null, 1);
    }

    /**
     * Test 3 AM local last week event against 2 AM - 3 AM local time filter.
     */
    public void test3AMLocalLastWeek() {
        common3AM(null, -7);
    }

    /**
     * Test 3 AM local next week event against 2 AM - 3 AM local time filter.
     */
    public void test3AMLocalNextWeek() {
        common3AM(null, 7);
    }

    /**
     * Test 2 AM UTC today event against 2 AM - 3 AM GMT filter.
     */
    public void test2AMGMT() {
        common2AM("GMT", 0);
    }

    /**
     * Test 3 AM UTC today event against 2 AM - 3 AM GMT filter.
     */
    public void test3AMGMT() {
        common3AM("GMT", 0);
    }

    /**
     * Log events every 15 minutes from midnight to midnight in
     * using specified calendar.
     * @param cal calendar.
     */
    private void common(Calendar cal) {
        Logger logger = Logger.getLogger(TimeFilterTest.class);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        for (int hour = 0; hour < 24; hour++) {
            for (int minute = 0; minute < 60; minute += 15) {
                cal.set(Calendar.HOUR_OF_DAY, hour);
                cal.set(Calendar.MINUTE, minute);
                LoggingEvent event = new LoggingEvent("org.apache.log4j.Logger",
                        logger,
                        cal.getTimeInMillis(), Level.INFO, "Hello, world.", null);
                logger.callAppenders(event);
            }
        }
    }


    /**
     * Test 2 AM-3AM local time accept on match filter.
     * @throws Exception if IO exception.
     */
    public void testConfig1() throws Exception {
      configure("timeFilter1.xml");
      common(Calendar.getInstance());

      assertTrue(Compare.compare(TimeFilterTest.class,
               "timeFilter.1", "timeFilter1.log"));
    }

    /**
     * Test 2 AM-3AM UTC reject on match filter.
     * @throws Exception if IO exception.
     */
    public void testConfig2() throws Exception {
      configure("timeFilter2.xml");
      common(Calendar.getInstance(TimeZone.getTimeZone("GMT")));

      assertTrue(Compare.compare(TimeFilterTest.class,
                 "timeFilter.2", "timeFilter2.log"));
    }

}
