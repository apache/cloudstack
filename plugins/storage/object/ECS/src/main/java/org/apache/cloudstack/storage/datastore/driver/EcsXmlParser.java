/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cloudstack.storage.datastore.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Lightweight XML-extraction utilities for ECS API responses. */
public final class EcsXmlParser {

    private EcsXmlParser() { }

    /** Returns the text content of the first occurrence of &lt;tag&gt;…&lt;/tag&gt;, or {@code null}. */
    public static String extractTag(final String xml, final String tag) {
        if (xml == null) return null;
        final String open = "<" + tag + ">", close = "</" + tag + ">";
        final int i = xml.indexOf(open);
        if (i < 0) return null;
        final int j = xml.indexOf(close, i + open.length());
        if (j < 0) return null;
        return xml.substring(i + open.length(), j).trim();
    }

    /** Returns the text content of every occurrence of &lt;tag&gt;…&lt;/tag&gt;. */
    public static List<String> extractAllTags(final String xml, final String tag) {
        final List<String> out = new ArrayList<>();
        if (xml == null) return out;
        final String open = "<" + tag + ">", close = "</" + tag + ">";
        int from = 0;
        while (true) {
            final int i = xml.indexOf(open, from);
            if (i < 0) break;
            final int j = xml.indexOf(close, i + open.length());
            if (j < 0) break;
            out.add(xml.substring(i + open.length(), j).trim());
            from = j + close.length();
        }
        return out;
    }

    /** Parses the integer value of a tag, or returns {@code null} on missing/invalid content. */
    public static Integer parseIntTag(final String xml, final String tag) {
        final String val = extractTag(xml, tag);
        if (val == null) return null;
        try { return Integer.parseInt(val); } catch (NumberFormatException ignore) { return null; }
    }

    /** Collects every {@code <Key>} value inside {@code <Contents>} blocks (S3 list-objects response). */
    public static void extractKeysFromListBucketXml(final String xml, final List<String> keys) {
        if (xml == null) return;
        final String open = "<Contents>", close = "</Contents>";
        int from = 0;
        while (true) {
            final int i = xml.indexOf(open, from);
            if (i < 0) break;
            final int j = xml.indexOf(close, i + open.length());
            if (j < 0) break;
            final String key = extractTag(xml.substring(i, j + close.length()), "Key");
            if (key != null && !key.isEmpty()) keys.add(key.trim());
            from = j + close.length();
        }
    }

    /** Returns {@code true} if the HTTP 400 body looks like a "bucket already exists" error. */
    public static boolean looksLikeBucketAlreadyExists400(final String body) {
        if (body == null) return false;
        final String lb = body.toLowerCase(Locale.ROOT);
        return lb.contains("already exist") || lb.contains("already_exists") ||
               lb.contains("already-exists") || lb.contains("name already in use") ||
               lb.contains("bucket exists") || lb.contains("duplicate");
    }
}
