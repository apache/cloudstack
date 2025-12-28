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
