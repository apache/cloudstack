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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import java.time.format.DateTimeFormatter;
import java.time.OffsetDateTime;

import com.cloud.utils.DateUtil.IntervalType;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class DateUtilTest {
    // command line test tool
    public static void main(String[] args) {
        TimeZone localTimezone = Calendar.getInstance().getTimeZone();
        TimeZone gmtTimezone = TimeZone.getTimeZone("GMT");
        TimeZone estTimezone = TimeZone.getTimeZone("EST");

        Date time = new Date();
        System.out.println("local time :" + DateUtil.getDateDisplayString(localTimezone, time));
        System.out.println("GMT time   :" + DateUtil.getDateDisplayString(gmtTimezone, time));
        System.out.println("EST time   :" + DateUtil.getDateDisplayString(estTimezone, time));
        //Test next run time. Expects interval and schedule as arguments
        if (args.length == 2) {
            System.out.println("Next run time: " + DateUtil.getNextRunTime(IntervalType.getIntervalType(args[0]), args[1], "GMT", time).toString());
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
}
