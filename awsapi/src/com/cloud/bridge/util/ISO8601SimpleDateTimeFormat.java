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

package com.cloud.bridge.util;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Format and parse a date string which is expected to be in ISO 8601 DateTimeFormat especially for
 * use in XML documents.
 * An example is for use with GMTDateTimeUserType to provide parsing of DateTime format strings into
 * accurate Java Date representations based on UTC.
 * The purpose of this class is to allow the creation of accurate date time representations following
 * the ISO 8601 format YYYY-MM-DDThh:MM:ss
 * using the letter "T" as the date/time separator
 * This representation may be immediately followed by a "Z" (Zulu i.e. at zero offset from GMT) to indicate UTC
 * or, otherwise, to a specific time zone.  If a time zone (tz) is encoded then this is held as the difference
 * between the local time in the tz and UCT, expressed as a positive(+) or negative(-) offset (hhMM) appended
 * to the format.
 * The default case holds no tz information and assumes that a date time representation referenced to Zulu
 * (i.e. zero offset from GMT) is required.  When formatting an existing Date transform it into the Zulu timezone
 * so that it is explicitly at GMT with zero offset.  This provides the default representation for the encoding
 * of AWS datetime values.
 * For testing, it may be useful to note that, as at 2012, a city whose time is always in the Zulu timezone is
 * Reykjavik, Iceland.
 * The parsing and formatting methods provided by this class are GMT-referenced and locale insensitive.
 */

public class ISO8601SimpleDateTimeFormat extends SimpleDateFormat {

    private static final long serialVersionUID = 7388260211953189670L;

    protected static TimeZone defaultTimeZone = TimeZone.getTimeZone("Z");

    /**
     * Construct a new ISO8601DateTimeFormat using the default time zone.
     * Initializes calendar inherited from java.text.DateFormat.calendar
     *
     */
    public ISO8601SimpleDateTimeFormat() {
        setCalendar(Calendar.getInstance(defaultTimeZone));
    }

    /**
     * Construct a new ISO8601DateTimeFormat using a specific time zone.
     * Initializes calendar inherited from java.text.DateFormat.calendar
     * @param tz The time zone used to format and parse the date.
     */
    public ISO8601SimpleDateTimeFormat(TimeZone tz) {
        setCalendar(Calendar.getInstance(tz));
    }

    /**
     * The abstract superclass DateFormat has two business methods to override.  These are
     * public StringBuffer format(Date arg0, StringBuffer arg1, FieldPosition arg2)
     * public Date parse(String arg0, ParsePosition arg1)
     */

    /**
     * @see DateFormat#format(Date, StringBuffer, FieldPosition)
     */
    @Override
    public StringBuffer format(Date date, StringBuffer stringBuffer, FieldPosition fieldPosition) {
        calendar.setTime(date);
        calendar.setTimeZone(defaultTimeZone);
        writeYYYYMM(stringBuffer);
        stringBuffer.append('T');
        writehhMMss(stringBuffer);
        stringBuffer.append(".000Z");
        return stringBuffer;
    }

    /* @see DateFormat#parse(String, ParsePosition)
     * Assigns the values of YYYY-MM-DDThh:MM:ss fields between the delimiters of dateString
     * or a near approximation using the superclass SimpleDateFormat if not formatted exactly as ISO8601
     */
    @Override
    public Date parse(String dateString, ParsePosition pos) {
        ParsePosition startpos = pos;
        int p = pos.getIndex();
        // Assign value of YYYY
        try {
            int YYYY = Integer.valueOf(dateString.substring(p, p + 4)).intValue();
            p += 4;
            if (dateString.charAt(p) != '-') {
                throw new IllegalArgumentException();
            }
            p++;
            // Assign value of MM
            int MM = Integer.valueOf(dateString.substring(p, p + 2)).intValue() - 1;
            p += 2;
            if (dateString.charAt(p) != '-') {
                throw new IllegalArgumentException();
            }
            p++;
            // Asign value of dd
            int DD = Integer.valueOf(dateString.substring(p, p + 2)).intValue();
            p += 2;
            if (dateString.charAt(p) != 'T') {
                throw new IllegalArgumentException();
            }
            p++;
            // Assign value of hh
            int hh = Integer.valueOf(dateString.substring(p, p + 2)).intValue();
            p += 2;
            if (dateString.charAt(p) != ':') {
                throw new IllegalArgumentException();
            }
            p++;
            // Assign value of mm
            int mm = Integer.valueOf(dateString.substring(p, p + 2)).intValue();
            p += 2;
            if (dateString.charAt(p) != ':') {
                throw new IllegalArgumentException();
            }
            p++;
            // Assign value of ss
            int ss = 0;
            //   if (p < dateString.length() && dateString.charAt(p) == ':') {
            // Allow exactly two ss digits after final : delimiter
            ss = Integer.valueOf(dateString.substring(p, p + 2)).intValue();
            p += 2;
            // Set calendar inherited from java.text.DateFormat.calendar
            calendar.set(YYYY, MM, DD, hh, mm, ss);
            calendar.set(Calendar.MILLISECOND, 0); // Since java.util.Date holds none, zeroize milliseconds
            // process appended timezone if any or Z otherwise
            p = parseTZ(p, dateString);
        } catch (IllegalArgumentException ex) {
            super.setTimeZone(TimeZone.getTimeZone("GMT"));
            super.applyPattern("yyyy-MM-dd HH:mm:ss");
            return super.parse(dateString, startpos);
        } catch (Exception ex) {
            super.setTimeZone(TimeZone.getTimeZone("GMT"));
            return super.parse(dateString, startpos);    // default pattern
        } finally {
            pos.setIndex(p);
        }
        // Return the Calendar instance's Date representation of its value
        return calendar.getTime();
    }

    /**
     * Write the time zone string.  Remember that in the default TimeZone there is no offset and the
     * convention to supply the TimeZone string constant "Z" is applicable.  As an optimization there
     * is no need to call this method where the default TimeZone has been set.
     * @param stringBuffer The buffer to append the time zone.
     */
    protected final void writeTZ(StringBuffer stringBuffer) {
        int offset = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);
        if (offset == 0) {
            stringBuffer.append('Z');
        } else {
            int offsetHour = offset / 3600000;
            int offsetMin = (offset % 3600000) / 60000;
            if (offset >= 0) {
                stringBuffer.append('+');
            } else {
                stringBuffer.append('-');
                offsetHour = 0 - offsetHour;
                offsetMin = 0 - offsetMin;
            }
            appendInt(stringBuffer, offsetHour, 2);
            stringBuffer.append(':');
            appendInt(stringBuffer, offsetMin, 2);
        }
    }

    /**
     * Write hour, minutes, and seconds.
     * @param stringBuffer The buffer to append the string.
     */
    protected final void writehhMMss(StringBuffer stringBuffer) {
        int hh = calendar.get(Calendar.HOUR_OF_DAY);
        appendInt(stringBuffer, hh, 2);
        stringBuffer.append(':');

        int mm = calendar.get(Calendar.MINUTE);
        appendInt(stringBuffer, mm, 2);
        stringBuffer.append(':');

        int ss = calendar.get(Calendar.SECOND);
        appendInt(stringBuffer, ss, 2);
    }

    /**
     * Write YYYY, and MMs.
     * @param stringBuffer The buffer to append the string.
     */
    protected final void writeYYYYMM(StringBuffer stringBuffer) {
        int YYYY = calendar.get(Calendar.YEAR);
        appendInt(stringBuffer, YYYY, 4);

        String MM;
        switch (calendar.get(Calendar.MONTH)) {
            case Calendar.JANUARY:
                MM = "-01-";
                break;
            case Calendar.FEBRUARY:
                MM = "-02-";
                break;
            case Calendar.MARCH:
                MM = "-03-";
                break;
            case Calendar.APRIL:
                MM = "-04-";
                break;
            case Calendar.MAY:
                MM = "-05-";
                break;
            case Calendar.JUNE:
                MM = "-06-";
                break;
            case Calendar.JULY:
                MM = "-07-";
                break;
            case Calendar.AUGUST:
                MM = "-08-";
                break;
            case Calendar.SEPTEMBER:
                MM = "-09-";
                break;
            case Calendar.OCTOBER:
                MM = "-10-";
                break;
            case Calendar.NOVEMBER:
                MM = "-11-";
                break;
            case Calendar.DECEMBER:
                MM = "-12-";
                break;
            default:
                MM = "-NA-";
                break;
        }
        stringBuffer.append(MM);

        int DD = calendar.get(Calendar.DAY_OF_MONTH);
        appendInt(stringBuffer, DD, 2);
    }

    /**
     * Write an integer value with leading zeros.
     * @param stringBuffer The buffer to append the string.
     * @param value The value to write.
     * @param length The length of the string to write.
     */
    protected final void appendInt(StringBuffer stringBuffer, int value, int length) {
        int len1 = stringBuffer.length();
        stringBuffer.append(value);
        int len2 = stringBuffer.length();
        for (int i = len2; i < len1 + length; ++i) {
            stringBuffer.insert(len1, '0');
        }
    }

    /**
     * Parse the time zone.
     * @param i The position to start parsing.
     * @param dateString The dateString to parse.
     * @return The position after parsing has finished.
     */

    protected final int parseTZ(int i, String dateString) {
        if (i < dateString.length()) {
            // check and handle the zone/dst offset
            int offset = 0;
            if (dateString.charAt(i) == 'Z') {
                offset = 0;
                i++;
            } else {
                int sign = 1;
                if (dateString.charAt(i) == '-') {
                    sign = -1;
                } else if (dateString.charAt(i) != '+') {
                    throw new IllegalArgumentException();
                }
                i++;

                int offsetHour = Integer.valueOf(dateString.substring(i, i + 2)).intValue();
                i += 2;

                if (dateString.charAt(i) != ':') {
                    throw new IllegalArgumentException();
                }
                i++;

                int offsetMin = Integer.valueOf(dateString.substring(i, i + 2)).intValue();
                i += 2;
                offset = ((offsetHour * 60) + offsetMin) * 60000 * sign;
            }
            int offsetCal = calendar.get(Calendar.ZONE_OFFSET) + calendar.get(Calendar.DST_OFFSET);

            calendar.add(Calendar.MILLISECOND, offsetCal - offset);
        }
        return i;

    }

    @Override
    public int hashCode() {
        return (calendar.get(2) + calendar.get(16));
        // numberFormat (used by superclass) will not distribute, so use calendar third and penultimate fields
        // (i.e. dd and ss) instead
        // in Java 6 (Calendar.FIELD_COUNT-1) returns 16
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        DateFormat other = (DateFormat)obj;
        Calendar otherCalendar = other.getCalendar();
        for (int i = 0; i < Calendar.FIELD_COUNT; i++)
            if (calendar.get(i) != (otherCalendar.get(i)))
                return false;
        return true;
    }

}
