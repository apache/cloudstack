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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.YearMonth;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.OffsetDateTime;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cronutils.descriptor.CronDescriptor;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import org.springframework.scheduling.support.CronExpression;


public class DateUtil {
    public static final int HOURS_IN_A_MONTH = 30 * 24;

    public static final TimeZone GMT_TIMEZONE = TimeZone.getTimeZone("GMT");
    public static final String YYYYMMDD_FORMAT = "yyyyMMddHHmmss";
    private static final String ZONED_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZ";
    private static final DateFormat ZONED_DATETIME_SIMPLE_FORMATTER = new SimpleDateFormat(ZONED_DATETIME_FORMAT);

    private static final DateTimeFormatter[] parseFormats = new DateTimeFormatter[]{
        DateTimeFormatter.ISO_OFFSET_DATE_TIME,
        DateTimeFormatter.ofPattern(ZONED_DATETIME_FORMAT),
        DateTimeFormatter.ISO_INSTANT,
        // with milliseconds
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSX"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSZ"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'"),
        // legacy and non-sensical format
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'Z")
    };

    public static Date currentGMTTime() {
        // Date object always stores milliseconds offset based on GMT internally
        return new Date();
    }

    public static Date parseTZDateString(String str) throws ParseException {
        for (DateTimeFormatter formatter : parseFormats) {
            try {
                OffsetDateTime dt = OffsetDateTime.parse(str, formatter);
                return Date.from(dt.toInstant());
            } catch (DateTimeParseException e) {
                // do nothing
            }
        }
        throw new ParseException("Unparseable date: \"" + str + "\"", 0);
    }

    public static Date parseDateString(TimeZone tz, String dateString) {
        return parseDateString(tz, dateString, "yyyy-MM-dd HH:mm:ss");
    }

    public static Date parseDateString(TimeZone tz, String dateString, String formatString) {
        DateFormat df = new SimpleDateFormat(formatString);
        df.setTimeZone(tz);

        try {
            return df.parse(dateString);
        } catch (ParseException e) {
            throw new CloudRuntimeException("why why ", e);
        }
    }

    public static String displayDateInTimezone(TimeZone tz, Date time) {
        return getDateDisplayString(tz, time, ZONED_DATETIME_FORMAT);
    }

    public static String getDateDisplayString(TimeZone tz, Date time) {
        return getDateDisplayString(tz, time, "yyyy-MM-dd HH:mm:ss");
    }

    public static String getDateDisplayString(TimeZone tz, Date time, String formatString) {
        if (time == null) {
            return null;
        }

        DateFormat df = new SimpleDateFormat(formatString);
        df.setTimeZone(tz);

        return df.format(time);
    }

    public static String getOutputString(Date date) {
        if (date == null) {
            return "";
        }
        String formattedString;
        synchronized (ZONED_DATETIME_SIMPLE_FORMATTER) {
            formattedString = ZONED_DATETIME_SIMPLE_FORMATTER.format(date);
        }
        return formattedString;
    }

    public static Date now() {
        return new Date(System.currentTimeMillis());
    }

    public enum IntervalType {
        HOURLY, DAILY, WEEKLY, MONTHLY;

        boolean equals(String intervalType) {
            return super.toString().equalsIgnoreCase(intervalType);
        }

        public static IntervalType getIntervalType(String intervalTypeStr) {
            for (IntervalType intervalType : IntervalType.values()) {
                if (intervalType.equals(intervalTypeStr)) {
                    return intervalType;
                }
            }
            return null;
        }
    }

    public static IntervalType getIntervalType(short type) {
        if (type < 0 || type >= IntervalType.values().length) {
            return null;
        }
        return IntervalType.values()[type];
    }

    /**
     * Return next run time
     * @param intervalType  hourly/daily/weekly/monthly
     * @param schedule MM[:HH][:DD] format. DD is day of week for weekly and day of month for monthly
     * @param timezone The timezone in which the schedule string is specified
     * @param startDate if specified, returns next run time after the specified startDate
     * @return
     */
    public static Date getNextRunTime(IntervalType type, String schedule, String timezone, Date startDate) {

        String[] scheduleParts = schedule.split(":"); //MM:HH:DAY

        final Calendar scheduleTime = Calendar.getInstance();
        scheduleTime.setTimeZone(TimeZone.getTimeZone(timezone));

        if (startDate == null) {
            startDate = new Date();
        }
        scheduleTime.setTime(startDate);
        // Throw an ArrayIndexOutOfBoundsException if schedule is badly formatted.
        scheduleTime.setLenient(false);
        int minutes = 0;
        int hour = 0;
        int day = 0;
        Date execDate = null;

        switch (type) {
            case HOURLY:
                if (scheduleParts.length < 1) {
                    throw new CloudRuntimeException("Incorrect schedule format: " + schedule + " for interval type:" + type.toString());
                }
                minutes = Integer.parseInt(scheduleParts[0]);
                scheduleTime.set(Calendar.MINUTE, minutes);
                scheduleTime.set(Calendar.SECOND, 0);
                scheduleTime.set(Calendar.MILLISECOND, 0);
                try {
                    execDate = scheduleTime.getTime();
                } catch (IllegalArgumentException ex) {
                    scheduleTime.setLenient(true);
                    execDate = scheduleTime.getTime();
                    scheduleTime.setLenient(false);
                }
                // XXX: !execDate.after(startDate) is strictly for testing.
                // During testing we use a test clock which runs much faster than the real clock
                // So startDate and execDate will always be ahead in the future
                // and we will never increase the time here
                if (execDate.before(new Date()) || !execDate.after(startDate)) {
                    scheduleTime.add(Calendar.HOUR_OF_DAY, 1);
                }
                break;
            case DAILY:
                if (scheduleParts.length < 2) {
                    throw new CloudRuntimeException("Incorrect schedule format: " + schedule + " for interval type:" + type.toString());
                }
                minutes = Integer.parseInt(scheduleParts[0]);
                hour = Integer.parseInt(scheduleParts[1]);

                scheduleTime.set(Calendar.HOUR_OF_DAY, hour);
                scheduleTime.set(Calendar.MINUTE, minutes);
                scheduleTime.set(Calendar.SECOND, 0);
                scheduleTime.set(Calendar.MILLISECOND, 0);
                try {
                    execDate = scheduleTime.getTime();
                } catch (IllegalArgumentException ex) {
                    scheduleTime.setLenient(true);
                    execDate = scheduleTime.getTime();
                    scheduleTime.setLenient(false);
                }
                // XXX: !execDate.after(startDate) is strictly for testing.
                // During testing we use a test clock which runs much faster than the real clock
                // So startDate and execDate will always be ahead in the future
                // and we will never increase the time here
                if (execDate.before(new Date()) || !execDate.after(startDate)) {
                    scheduleTime.add(Calendar.DAY_OF_YEAR, 1);
                }
                break;
            case WEEKLY:
                if (scheduleParts.length < 3) {
                    throw new CloudRuntimeException("Incorrect schedule format: " + schedule + " for interval type:" + type.toString());
                }
                minutes = Integer.parseInt(scheduleParts[0]);
                hour = Integer.parseInt(scheduleParts[1]);
                day = Integer.parseInt(scheduleParts[2]);
                scheduleTime.set(Calendar.DAY_OF_WEEK, day);
                scheduleTime.set(Calendar.HOUR_OF_DAY, hour);
                scheduleTime.set(Calendar.MINUTE, minutes);
                scheduleTime.set(Calendar.SECOND, 0);
                scheduleTime.set(Calendar.MILLISECOND, 0);
                try {
                    execDate = scheduleTime.getTime();
                } catch (IllegalArgumentException ex) {
                    scheduleTime.setLenient(true);
                    execDate = scheduleTime.getTime();
                    scheduleTime.setLenient(false);
                }
                // XXX: !execDate.after(startDate) is strictly for testing.
                // During testing we use a test clock which runs much faster than the real clock
                // So startDate and execDate will always be ahead in the future
                // and we will never increase the time here
                if (execDate.before(new Date()) || !execDate.after(startDate)) {
                    scheduleTime.add(Calendar.DAY_OF_WEEK, 7);
                }
                ;
                break;
            case MONTHLY:
                if (scheduleParts.length < 3) {
                    throw new CloudRuntimeException("Incorrect schedule format: " + schedule + " for interval type:" + type.toString());
                }
                minutes = Integer.parseInt(scheduleParts[0]);
                hour = Integer.parseInt(scheduleParts[1]);
                day = Integer.parseInt(scheduleParts[2]);
                if (day > 28) {
                    throw new CloudRuntimeException("Day cannot be greater than 28 for monthly schedule");
                }
                scheduleTime.set(Calendar.DAY_OF_MONTH, day);
                scheduleTime.set(Calendar.HOUR_OF_DAY, hour);
                scheduleTime.set(Calendar.MINUTE, minutes);
                scheduleTime.set(Calendar.SECOND, 0);
                scheduleTime.set(Calendar.MILLISECOND, 0);
                try {
                    execDate = scheduleTime.getTime();
                } catch (IllegalArgumentException ex) {
                    scheduleTime.setLenient(true);
                    execDate = scheduleTime.getTime();
                    scheduleTime.setLenient(false);
                }
                // XXX: !execDate.after(startDate) is strictly for testing.
                // During testing we use a test clock which runs much faster than the real clock
                // So startDate and execDate will always be ahead in the future
                // and we will never increase the time here
                if (execDate.before(new Date()) || !execDate.after(startDate)) {
                    scheduleTime.add(Calendar.MONTH, 1);
                }
                break;
            default:
                throw new CloudRuntimeException("Incorrect interval: " + type.toString());
        }

        try {
            return scheduleTime.getTime();
        } catch (IllegalArgumentException ex) {
            scheduleTime.setLenient(true);
            Date nextScheduledDate = scheduleTime.getTime();
            scheduleTime.setLenient(false);
            return nextScheduledDate;
        }
    }

    public static long getTimeDifference(Date date1, Date date2){

        Calendar dateCalendar1 = Calendar.getInstance();
        dateCalendar1.setTime(date1);
        Calendar dateCalendar2 = Calendar.getInstance();
        dateCalendar2.setTime(date2);

        return (dateCalendar1.getTimeInMillis() - dateCalendar2.getTimeInMillis() )/1000;

    }

    public static CronExpression parseSchedule(String schedule) {
        if (schedule != null) {
            // CronExpression's granularity is in seconds. Prepending "0 " to change the granularity to minutes.
            return CronExpression.parse(String.format("0 %s", schedule));
        } else {
            return null;
        }
    }

    public static String getHumanReadableSchedule(CronExpression schedule) {
        CronDefinition cronDefinition = CronDefinitionBuilder.instanceDefinitionFor(CronType.SPRING);
        CronParser parser = new CronParser(cronDefinition);
        CronDescriptor descriptor = CronDescriptor.instance();
        return descriptor.describe(parser.parse(schedule.toString()));
    }

    public static ZonedDateTime getZoneDateTime(Date date, ZoneId tzId) {
        if (date == null) {
            return null;
        }
        ZonedDateTime zonedDate = ZonedDateTime.ofInstant(date.toInstant(), tzId);
        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), TimeZone.getDefault().toZoneId());
        zonedDate = zonedDate.withYear(localDateTime.getYear())
                .withMonth(localDateTime.getMonthValue())
                .withDayOfMonth(localDateTime.getDayOfMonth())
                .withHour(localDateTime.getHour())
                .withMinute(localDateTime.getMinute())
                .withSecond(localDateTime.getSecond());
        return zonedDate;
    }

    public static int getHoursInCurrentMonth(Date date) {
        return YearMonth.of(date.getYear(), date.getMonth() + 1).lengthOfMonth() * 24;
    }
}
