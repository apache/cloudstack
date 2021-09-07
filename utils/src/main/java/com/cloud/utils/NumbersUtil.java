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

import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Formatter;
import java.util.Locale;

import org.apache.commons.lang.math.NumberUtils;

import com.cloud.utils.exception.CloudRuntimeException;

public class NumbersUtil {
    public static long parseLong(String s, long defaultValue) {
        return NumberUtils.toLong(s, defaultValue);
    }

    public static int parseInt(String s, int defaultValue) {
        return NumberUtils.toInt(s, defaultValue);
    }

    public static float parseFloat(String s, float defaultValue) {
        return NumberUtils.toFloat(s, defaultValue);
    }

    public static Boolean enableHumanReadableSizes = true;

    /**
     * Converts bytes to long on input.
     */
    public static long bytesToLong(byte b[]) {
        return bytesToLong(b, 0);
    }

    public static long bytesToLong(byte b[], int pos) {
        return ByteBuffer.wrap(b, pos, 8).getLong();
    }

    /**
     * Converts a byte array to a hex readable string.
     **/
    public static String bytesToString(byte[] data, int start, int end) {
        StringBuilder buf = new StringBuilder();
        if (end > data.length) {
            end = data.length;
        }
        for (int i = start; i < end; i++) {
            buf.append(" ");
            buf.append(Integer.toHexString(data[i] & 0xff));
        }
        return buf.toString();
    }

    protected static final long KB = 1024;
    protected static final long MB = 1024 * KB;
    protected static final long GB = 1024 * MB;
    protected static final long TB = 1024 * GB;

    public static String toReadableSize(Long bytes) {

        if (bytes == null){
            return "null";
        }

        if (bytes < KB && bytes >= 0) {
            return Long.toString(bytes) + " bytes";
        }
        StringBuilder builder = new StringBuilder();
        Formatter format = new Formatter(builder, Locale.getDefault());
        if (bytes < MB) {
            format.format("%.2f KB", (float)bytes / (float)KB);
        } else if (bytes < GB) {
            format.format("%.2f MB", (float)bytes / (float)MB);
        } else if (bytes < TB) {
            format.format("%.2f GB", (float)bytes / (float)GB);
        } else {
            format.format("%.4f TB", (float)bytes / (float)TB);
        }
        format.close();
        return builder.toString();
    }

    public static String toHumanReadableSize(long size) {
        if (enableHumanReadableSizes){
            return "(" + toReadableSize(size) + ") " + ((Long)size).toString();
        }
        return ((Long)size).toString();
    }

    /**
     * Converts a string of the format 'yy-MM-dd'T'HH:mm:ss.SSS" into ms.
     *
     * @param str containing the interval.
     * @param defaultValue value to return if str doesn't parse.  If -1, throws VmopsRuntimeException
     * @return interval in ms
     */
    public static long parseInterval(String str, long defaultValue) {
        try {
            if (str == null) {
                throw new ParseException("String is wrong", 0);
            }

            SimpleDateFormat sdf = null;
            if (str.contains("D")) {
                sdf = new SimpleDateFormat("dd'D'HH'h'mm'M'ss'S'SSS'ms'");
            } else if (str.contains("h")) {
                sdf = new SimpleDateFormat("HH'h'mm'M'ss'S'SSS'ms'");
            } else if (str.contains("M")) {
                sdf = new SimpleDateFormat("mm'M'ss'S'SSS'ms'");
            } else if (str.contains("S")) {
                sdf = new SimpleDateFormat("ss'S'SSS'ms'");
            } else if (str.contains("ms")) {
                sdf = new SimpleDateFormat("SSS'ms'");
            }
            if (sdf == null) {
                throw new ParseException("String is wrong", 0);
            }

            Date date = sdf.parse(str);
            return date.getTime();
        } catch (ParseException e) {
            if (defaultValue != -1) {
                return defaultValue;
            } else {
                throw new CloudRuntimeException("Unable to parse: " + str, e);
            }
        }
    }

    public static int hash(long value) {
        return (int)(value ^ (value >>> 32));
    }
}
