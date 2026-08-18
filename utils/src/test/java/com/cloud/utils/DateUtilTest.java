//
// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
//
package com.cloud.utils;

import com.cloud.utils.DateUtil.IntervalType;
import com.cloud.utils.exception.CloudRuntimeException;
import org.junit.Test;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DateUtilTest {
    private static final String TEST_DATE_GMT = "2023-06-15 10:30:00";
    private static final String TEST_DATE_EST = "2023-06-15 05:30:00";
    private static final String TEST_DATE_ISO = "2023-06-15T10:30:00Z";
    private static final String TEST_DATE_YYMMDD = "20230615103000";
    private static final TimeZone GMT = DateUtil.GMT_TIMEZONE;
    private static final TimeZone EST = TimeZone.getTimeZone("EST");

    // command line test tool
    public static void main(String[] args) {
        TimeZone localTimezone = Calendar.getInstance().getTimeZone();
        TimeZone gmtTimezone = GMT;
        TimeZone estTimezone = EST;

        Date time = new Date();
        System.out.println("local time :" + DateUtil.getDateDisplayString(localTimezone, time));
        System.out.println("GMT time   :" + DateUtil.getDateDisplayString(gmtTimezone, time));
        System.out.println("EST time   :" + DateUtil.getDateDisplayString(estTimezone, time));
        //Test next run time. Expects interval and schedule as arguments
        if (args.length == 2) {
            System.out.println("Next run time: " + DateUtil.getNextRunTime(IntervalType.getIntervalType(args[0]),
                            args[1], "GMT", time)
                    .toString());
        }
    }

    @Test
    public void zonedTimeFormatLegacy() throws ParseException {
        Date time = new Date();
        DateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'Z");
        String str = dfDate.format(time);
        Date dtParsed = DateUtil.parseTZDateString(str);

        assertEquals(str, time.toString(), dtParsed.toString());
    }

    @Test
    public void zonedTimeFormat() throws ParseException {
        Date time = new Date();
        DateFormat dfDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        String str = dfDate.format(time);
        Date dtParsed = DateUtil.parseTZDateString(str);

        assertEquals(str, time.toString(), dtParsed.toString());
    }

    @Test
    public void zonedTimeFormatIsoOffsetDateTime() throws ParseException {
        Instant moment = Instant.now();
        Date time = Date.from(moment);
        String str = OffsetDateTime.ofInstant(moment, ZoneId.systemDefault()).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        Date dtParsed = DateUtil.parseTZDateString(str);

        assertEquals(str, time.toString(), dtParsed.toString());
    }

    @Test
    public void zonedTimeFormatIsoInstant() throws ParseException {
        Instant moment = Instant.now();
        Date time = Date.from(moment);
        String str = OffsetDateTime.ofInstant(moment, ZoneId.systemDefault()).format(DateTimeFormatter.ISO_INSTANT);

        Date dtParsed = DateUtil.parseTZDateString(str);

        assertEquals(str, time.toString(), dtParsed.toString());
    }

    @Test
    public void zonedTimeFormatIsoOffsetDateTimeMs() throws ParseException {
        Instant moment = Instant.now();
        Date time = Date.from(moment);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX");
        String str = OffsetDateTime.ofInstant(moment, ZoneId.systemDefault()).format(formatter);

        Date dtParsed = DateUtil.parseTZDateString(str);

        assertEquals(str, time.toString(), dtParsed.toString());
    }

    @Test
    public void zonedTimeFormatIsoInstantMs() throws ParseException {
        Instant moment = Instant.now();
        Date time = Date.from(moment);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");
        String str = OffsetDateTime.ofInstant(moment, ZoneId.of("UTC")).format(formatter);

        Date dtParsed = DateUtil.parseTZDateString(str);

        assertEquals(str, time.toString(), dtParsed.toString());
    }

    @Test
    public void zonedTimeFormatIsoNoColonZMs() throws ParseException {
        Instant moment = Instant.now();
        Date time = Date.from(moment);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ");
        String str = OffsetDateTime.ofInstant(moment, ZoneId.systemDefault()).format(formatter);

        Date dtParsed = DateUtil.parseTZDateString(str);

        assertEquals(str, time.toString(), dtParsed.toString());
    }

    @Test
    public void parseDateStringDefaultFormat() {
        TimeZone gmt = GMT;
        Date date = DateUtil.parseDateString(gmt, TEST_DATE_GMT);
        assertEquals(TEST_DATE_GMT, DateUtil.getDateDisplayString(gmt, date));
    }

    @Test
    public void parseDateStringInterpretedInRequestedTimezone() {
        TimeZone est = EST;
        Date date = DateUtil.parseDateString(est, TEST_DATE_EST);
        assertEquals(TEST_DATE_GMT, DateUtil.getDateDisplayString(GMT, date));
    }

    @Test
    public void parseDateStringCustomFormat() {
        TimeZone gmt = GMT;
        Date date = DateUtil.parseDateString(gmt, TEST_DATE_YYMMDD, DateUtil.YYYYMMDD_FORMAT);
        assertEquals(TEST_DATE_GMT, DateUtil.getDateDisplayString(gmt, date));
    }

    @Test(expected = CloudRuntimeException.class)
    public void parseDateStringInvalidInputThrows() {
        DateUtil.parseDateString(GMT, "not-a-date");
    }

    @Test(expected = CloudRuntimeException.class)
    public void parseDateStringFormatMismatchThrows() {
        DateUtil.parseDateString(GMT, TEST_DATE_GMT, DateUtil.YYYYMMDD_FORMAT);
    }

    @Test
    public void displayDateInTimezoneGmt() {
        Date date = Date.from(Instant.parse(TEST_DATE_ISO));
        assertEquals("2023-06-15T10:30:00+0000", DateUtil.displayDateInTimezone(GMT, date));
    }

    @Test
    public void displayDateInTimezoneEst() {
        Date date = Date.from(Instant.parse(TEST_DATE_ISO));
        assertEquals("2023-06-15T05:30:00-0500", DateUtil.displayDateInTimezone(EST, date));
    }

    @Test
    public void getDateDisplayStringGmt() {
        Date date = Date.from(Instant.parse(TEST_DATE_ISO));
        assertEquals(TEST_DATE_GMT, DateUtil.getDateDisplayString(GMT, date));
    }

    @Test
    public void getDateDisplayStringTimezoneShift() {
        Date date = Date.from(Instant.parse(TEST_DATE_ISO));
        assertEquals(TEST_DATE_EST, DateUtil.getDateDisplayString(EST, date));
    }

    @Test
    public void getDateDisplayStringCustomFormat() {
        Date date = Date.from(Instant.parse(TEST_DATE_ISO));
        assertEquals(TEST_DATE_YYMMDD, DateUtil.getDateDisplayString(GMT, date, DateUtil.YYYYMMDD_FORMAT));
    }

    @Test
    public void getDateDisplayStringNullDate() {
        assertEquals(null, DateUtil.getDateDisplayString(GMT, null));
    }

    @Test
    public void displayDateInTimezoneNullDate() {
        assertEquals(null, DateUtil.displayDateInTimezone(GMT, null));
    }

    @Test
    public void getOutputStringNull() {
        assertEquals("", DateUtil.getOutputString(null));
    }

    @Test
    public void getOutputStringNonNull() {
        Date date = Date.from(Instant.parse(TEST_DATE_ISO));
        String result = DateUtil.getOutputString(date);
        assertTrue(result.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{4}"));
    }
}
