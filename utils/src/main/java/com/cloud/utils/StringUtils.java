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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.owasp.esapi.StringUtilities;

public class StringUtils {
    private static final char[] hexChar = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

    private static Charset preferredACSCharset;
    private static final String UTF8 = "UTF-8";

    static {
        if (isUtf8Supported()) {
            preferredACSCharset = Charset.forName(UTF8);
        } else {
            preferredACSCharset = Charset.defaultCharset();
        }
    }

    public static Charset getPreferredCharset() {
        return preferredACSCharset;
    }

    public static boolean isUtf8Supported() {
        return Charset.isSupported(UTF8);
    }

    protected static Charset getDefaultCharset() {
        return Charset.defaultCharset();
    }

    public static String join(final Iterable<? extends Object> iterable, final String delim) {
        final StringBuilder sb = new StringBuilder();
        if (iterable != null) {
            final Iterator<? extends Object> iter = iterable.iterator();
            if (iter.hasNext()) {
                final Object next = iter.next();
                sb.append(next.toString());
            }
            while (iter.hasNext()) {
                final Object next = iter.next();
                sb.append(delim + next.toString());
            }
        }
        return sb.toString();
    }

    public static String join(final String delimiter, final Object... components) {
        return org.apache.commons.lang.StringUtils.join(components, delimiter);
    }

    public static boolean isNotBlank(final String str) {
        if (str != null && str.trim().length() > 0) {
            return true;
        }

        return false;
    }

    public static String cleanupTags(String tags) {
        if (tags != null) {
            final String[] tokens = tags.split(",");
            final StringBuilder t = new StringBuilder();
            for (int i = 0; i < tokens.length; i++) {
                t.append(tokens[i].trim()).append(",");
            }
            t.delete(t.length() - 1, t.length());
            tags = t.toString();
        }

        return tags;
    }

    /**
     * @param tags
     * @return List of tags
     */
    public static List<String> csvTagsToList(final String tags) {
        final List<String> tagsList = new ArrayList<String>();

        if (tags != null) {
            final String[] tokens = tags.split(",");
            for (int i = 0; i < tokens.length; i++) {
                tagsList.add(tokens[i].trim());
            }
        }

        return tagsList;
    }

    /**
     * Converts a List of tags to a comma separated list
     * @param tags
     * @return String containing a comma separated list of tags
     */

    public static String listToCsvTags(final List<String> tagsList) {
        final StringBuilder tags = new StringBuilder();
        if (tagsList.size() > 0) {
            for (int i = 0; i < tagsList.size(); i++) {
                tags.append(tagsList.get(i));
                if (i != tagsList.size() - 1) {
                    tags.append(',');
                }
            }
        }

        return tags.toString();
    }

    public static String getExceptionStackInfo(final Throwable e) {
        final StringBuffer sb = new StringBuffer();

        sb.append(e.toString()).append("\n");
        final StackTraceElement[] elemnents = e.getStackTrace();
        for (final StackTraceElement element : elemnents) {
            sb.append(element.getClassName()).append(".");
            sb.append(element.getMethodName()).append("(");
            sb.append(element.getFileName()).append(":");
            sb.append(element.getLineNumber()).append(")");
            sb.append("\n");
        }

        return sb.toString();
    }

    public static String unicodeEscape(final String s) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c >> 7 > 0) {
                sb.append("\\u");
                sb.append(hexChar[c >> 12 & 0xF]); // append the hex character for the left-most 4-bits
                sb.append(hexChar[c >> 8 & 0xF]);  // hex for the second group of 4-bits from the left
                sb.append(hexChar[c >> 4 & 0xF]);  // hex for the third group
                sb.append(hexChar[c & 0xF]);         // hex for the last group, e.g., the right most 4-bits
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static String getMaskedPasswordForDisplay(final String password) {
        if (password == null || password.isEmpty()) {
            return "*";
        }

        final StringBuffer sb = new StringBuffer();
        sb.append(password.charAt(0));
        for (int i = 1; i < password.length(); i++) {
            sb.append("*");
        }

        return sb.toString();
    }

    // removes a password request param and it's value, also considering password is in query parameter value which has been url encoded
    private static final Pattern REGEX_PASSWORD_QUERYSTRING = Pattern.compile("(&|%26)?[^(&|%26)]*((p|P)assword|accesskey|secretkey)(=|%3D).*?(?=(%26|[&'\"]|$))");

    // removes a password/accesskey/ property from a response json object
    private static final Pattern REGEX_PASSWORD_JSON = Pattern.compile("\"((p|P)assword|privatekey|accesskey|secretkey)\":\\s?\".*?\",?");

    private static final Pattern REGEX_PASSWORD_DETAILS = Pattern.compile("(&|%26)?details(\\[|%5B)\\d*(\\]|%5D)\\.key(=|%3D)((p|P)assword|accesskey|secretkey)(?=(%26|[&'\"]))");

    private static final Pattern REGEX_PASSWORD_DETAILS_INDEX = Pattern.compile("details(\\[|%5B)\\d*(\\]|%5D)");

    private static final Pattern REGEX_REDUNDANT_AND = Pattern.compile("(&|%26)(&|%26)+");

    // Responsible for stripping sensitive content from request and response strings
    public static String cleanString(final String stringToClean) {
        String cleanResult = "";
        if (stringToClean != null) {
            cleanResult = REGEX_PASSWORD_QUERYSTRING.matcher(stringToClean).replaceAll("");
            cleanResult = REGEX_PASSWORD_JSON.matcher(cleanResult).replaceAll("");
            final Matcher detailsMatcher = REGEX_PASSWORD_DETAILS.matcher(cleanResult);
            while (detailsMatcher.find()) {
                final Matcher detailsIndexMatcher = REGEX_PASSWORD_DETAILS_INDEX.matcher(detailsMatcher.group());
                if (detailsIndexMatcher.find()) {
                    cleanResult = cleanDetails(cleanResult, detailsIndexMatcher.group());
                }
            }
        }
        return cleanResult;
    }

    public static String cleanDetails(final String stringToClean, final String detailsIndexSting) {
        String cleanResult = stringToClean;
        for (final String log : stringToClean.split("&|%26")) {
            if (log.contains(detailsIndexSting)) {
                cleanResult = cleanResult.replace(log, "");
            }
        }
        cleanResult = REGEX_REDUNDANT_AND.matcher(cleanResult).replaceAll("&");
        return cleanResult;
    }

    public static boolean areTagsEqual(final String tags1, final String tags2) {
        if (tags1 == null && tags2 == null) {
            return true;
        }

        if (tags1 != null && tags2 == null) {
            return false;
        }

        if (tags1 == null && tags2 != null) {
            return false;
        }

        final String delimiter = ",";

        final List<String> lstTags1 = new ArrayList<String>();
        final String[] aTags1 = tags1.split(delimiter);

        for (final String tag1 : aTags1) {
            lstTags1.add(tag1.toLowerCase());
        }

        final List<String> lstTags2 = new ArrayList<String>();
        final String[] aTags2 = tags2.split(delimiter);

        for (final String tag2 : aTags2) {
            lstTags2.add(tag2.toLowerCase());
        }

        return lstTags1.containsAll(lstTags2) && lstTags2.containsAll(lstTags1);
    }

    public static String stripControlCharacters(final String s) {
        return StringUtilities.stripControls(s);
    }

    public static int formatForOutput(final String text, final int start, final int columns, final char separator) {
        if (start >= text.length()) {
            return -1;
        }

        int end = start + columns;
        if (end > text.length()) {
            end = text.length();
        }
        final String searchable = text.substring(start, end);
        final int found = searchable.lastIndexOf(separator);
        return found > 0 ? found : end - start;
    }

    public static Map<String, String> stringToMap(final String s) {
        final Map<String, String> map = new HashMap<String, String>();
        final String[] elements = s.split(";");
        for (final String parts : elements) {
            final String[] keyValue = parts.split(":");
            map.put(keyValue[0], keyValue[1]);
        }
        return map;
    }

    public static String mapToString(final Map<String, String> map) {
        String s = "";
        for (final Map.Entry<String, String> entry : map.entrySet()) {
            s += entry.getKey() + ":" + entry.getValue() + ";";
        }
        if (s.length() > 0) {
            s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    public static <T> List<T> applyPagination(final List<T> originalList, final Long startIndex, final Long pageSizeVal) {
        // Most likely pageSize will never exceed int value, and we need integer to partition the listToReturn
        final boolean applyPagination = startIndex != null && pageSizeVal != null
                && startIndex <= Integer.MAX_VALUE && startIndex >= Integer.MIN_VALUE && pageSizeVal <= Integer.MAX_VALUE
                && pageSizeVal >= Integer.MIN_VALUE;
                List<T> listWPagination = null;
                if (applyPagination) {
                    listWPagination = new ArrayList<>();
                    final int index = startIndex.intValue() == 0 ? 0 : startIndex.intValue() / pageSizeVal.intValue();
                    final List<List<T>> partitions = StringUtils.partitionList(originalList, pageSizeVal.intValue());
                    if (index < partitions.size()) {
                        listWPagination = partitions.get(index);
                    }
                }
                return listWPagination;
    }

    private static <T> List<List<T>> partitionList(final List<T> originalList, final int chunkSize) {
        final List<List<T>> listOfChunks = new ArrayList<List<T>>();
        for (int i = 0; i < originalList.size() / chunkSize; i++) {
            listOfChunks.add(originalList.subList(i * chunkSize, i * chunkSize + chunkSize));
        }
        if (originalList.size() % chunkSize != 0) {
            listOfChunks.add(originalList.subList(originalList.size() - originalList.size() % chunkSize, originalList.size()));
        }
        return listOfChunks;
    }
}
