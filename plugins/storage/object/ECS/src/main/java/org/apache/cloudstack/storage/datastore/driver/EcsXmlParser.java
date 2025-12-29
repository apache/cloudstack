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

public class EcsXmlParser {

    public Integer parseIntTag(final String xml, final String tag) {
        String v = extractTag(xml, tag);
        if (v == null) return null;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException ignore) { return null; }
    }

    public String extractTag(final String xml, final String tag) {
        if (xml == null) return null;
        final String open = "<" + tag + ">";
        final String close = "</" + tag + ">";
        int i = xml.indexOf(open);
        if (i < 0) return null;
        int j = xml.indexOf(close, i + open.length());
        if (j < 0) return null;
        return xml.substring(i + open.length(), j).trim();
    }

    public List<String> extractAllTags(final String xml, final String tag) {
        final List<String> out = new ArrayList<>();
        if (xml == null) return out;

        final String open = "<" + tag + ">";
        final String close = "</" + tag + ">";

        int from = 0;
        while (true) {
            int i = xml.indexOf(open, from);
            if (i < 0) break;
            int j = xml.indexOf(close, i + open.length());
            if (j < 0) break;
            out.add(xml.substring(i + open.length(), j).trim());
            from = j + close.length();
        }
        return out;
    }

    public void extractKeysFromListBucketXml(final String xml, final List<String> keys) {
        if (xml == null) return;
        final String contentsOpen = "<Contents>";
        final String contentsClose = "</Contents>";
        int from = 0;
        while (true) {
            int i = xml.indexOf(contentsOpen, from);
            if (i < 0) break;
            int j = xml.indexOf(contentsClose, i + contentsOpen.length());
            if (j < 0) break;
            String block = xml.substring(i, j + contentsClose.length());
            String key = extractTag(block, "Key");
            if (key != null && !key.isEmpty()) keys.add(key.trim());
            from = j + contentsClose.length();
        }
    }

    public boolean looksLikeBucketAlreadyExists400(final String respBody) {
        final String lb = respBody == null ? "" : respBody.toLowerCase(Locale.ROOT);
        return lb.contains("already exist")
                || lb.contains("already_exists")
                || lb.contains("already-exists")
                || lb.contains("name already in use")
                || lb.contains("bucket exists")
                || lb.contains("duplicate");
    }
}
