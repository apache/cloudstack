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

import java.io.IOException;
import java.io.InputStream;

/**
 * Provide converters for regexp (case independent tokens)
 * Also provide upper case or lower case (default) converters for byte array b[] to hex String
 */
public class StringHelper {
    public static final String EMPTY_STRING = "";

    private static final char[] hexCharsUpperCase = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static final char[] hexCharsLowerCase = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    /* Convert byte array b[] into an uppercase hex string
    */
    public static String toHexStringUpperCase(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            sb.append(hexCharsUpperCase[(int)(((int)b[i] >> 4) & 0x0f)]);
            sb.append(hexCharsUpperCase[(int)(((int)b[i]) & 0x0f)]);
        }
        return sb.toString();
    }

    /* Convert byte array b[] into a lowercase (default) hex string
     */
    public static String toHexString(byte[] b) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            sb.append(hexCharsLowerCase[(int)(((int)b[i] >> 4) & 0x0f)]);
            sb.append(hexCharsLowerCase[(int)(((int)b[i]) & 0x0f)]);
        }
        return sb.toString();
    }

    public static String substringInBetween(String name, String prefix, String delimiter) {

        int startPos = 0;
        if (prefix != null)
            startPos = prefix.length() + 1;

        int endPos = name.indexOf(delimiter, startPos);
        if (endPos > 0)
            return name.substring(startPos, endPos);

        return null;
    }

    public static String stringFromStream(InputStream is) throws IOException {
        StringBuffer sb = new StringBuffer();
        byte[] b = new byte[4096];
        int n;
        while ((n = is.read(b)) != -1) {
            sb.append(new String(b, 0, n));
        }
        return sb.toString();
    }

    /**
     * Convert the string into a regex to allow easy matching.  In both S3 and EC2 regex strings
     * are used for matching.  We must remember to quote all special regex characters that appear
     * in the string.
     */
    public static String toRegex(String param) {
        StringBuffer regex = new StringBuffer();
        for (int i = 0; i < param.length(); i++) {
            char next = param.charAt(i);
            if ('*' == next)
                regex.append(".+");   // -> multi-character match wild card
            else if ('?' == next)
                regex.append(".");   // -> single-character match wild card
            else if ('.' == next)
                regex.append("\\.");   // all of these are special regex characters we are quoting
            else if ('+' == next)
                regex.append("\\+");
            else if ('$' == next)
                regex.append("\\$");
            else if ('\\' == next)
                regex.append("\\\\");
            else if ('[' == next)
                regex.append("\\[");
            else if (']' == next)
                regex.append("\\]");
            else if ('{' == next)
                regex.append("\\{");
            else if ('}' == next)
                regex.append("\\}");
            else if ('(' == next)
                regex.append("\\(");
            else if (')' == next)
                regex.append("\\)");
            else if ('&' == next)
                regex.append("\\&");
            else if ('^' == next)
                regex.append("\\^");
            else if ('-' == next)
                regex.append("\\-");
            else if ('|' == next)
                regex.append("\\|");
            else
                regex.append(next);
        }

        return regex.toString();
    }
}
