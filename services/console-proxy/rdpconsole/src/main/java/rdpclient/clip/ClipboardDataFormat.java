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
package rdpclient.clip;

import java.util.Map;

import rdpclient.rdp.RdpConstants;
import streamer.ByteBuffer;

public class ClipboardDataFormat {

    public static final String HTML_FORMAT = "HTML Format";
    public static final String RTF_AS_TEXT = "RTF As Text";
    public static final String RICH_TEXT_FORMAT_WITHOUT_OBJECTS = "Rich Text Format Without Objects";
    public static final String RICH_TEXT_FORMAT = "Rich Text Format";

    public static final int CB_FORMAT_TEXT = 0x0001;
    public static final int CB_FORMAT_UNICODETEXT = 0x000D;

    /**
     * Supported clipboard data formats in order of preference.
     */
    public static final Object[] supportedTextBasedFormats = new Object[] {
        // ID's
        CB_FORMAT_UNICODETEXT, CB_FORMAT_TEXT,

        // Names
        HTML_FORMAT,

        // RTF_AS_TEXT,
        // RICH_TEXT_FORMAT_WITHOUT_OBJECTS,
        // RICH_TEXT_FORMAT,

    };

    public final int id;
    public final String name;

    public ClipboardDataFormat(int id, String name) {
        super();
        this.id = id;
        this.name = name;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ClipboardDataFormat other = (ClipboardDataFormat)obj;
        if (id != other.id)
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ClipboardDataFormat [id=" + id + ", name=\"" + name + "\"" + ((id == CB_FORMAT_UNICODETEXT) ? " (Unicode text)" : "")
                + ((id == CB_FORMAT_TEXT) ? " (text)" : "") + "]";
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /**
     * Parse response of supported format and return it as string.
     */
    public String parseServerResponseAsString(ByteBuffer buf) {
        switch (id) {
        case CB_FORMAT_UNICODETEXT:
            return buf.readVariableWideString(RdpConstants.CHARSET_16);
        case CB_FORMAT_TEXT:
            return buf.readVariableString(RdpConstants.CHARSET_8);
        }

        if (name == null || name.length() == 0)
            return null;

        if (HTML_FORMAT.equals(name))
            return buf.readVariableString(RdpConstants.CHARSET_8); // TODO: verify

        // if (RTF_AS_TEXT.equals(name))
        // return buf.readVariableString(RdpConstants.CHARSET_8); // TODO: verify
        //
        // if (RICH_TEXT_FORMAT_WITHOUT_OBJECTS.equals(name))
        // return buf.readVariableString(RdpConstants.CHARSET_8); // TODO: verify
        //
        // if (RICH_TEXT_FORMAT.equals(name))
        // return buf.readVariableString(RdpConstants.CHARSET_8); // TODO: verify

        return null;
    }

    /**
     * Find first (richest) text-based data format.
     *
     * @return text-based data format or null, when not found
     */
    public static ClipboardDataFormat findBestTextFormat(Map<Object, ClipboardDataFormat> serverClipboardDataFormats) {
        for (Object formatKey : ClipboardDataFormat.supportedTextBasedFormats)
            if (serverClipboardDataFormats.containsKey(formatKey))
                return serverClipboardDataFormats.get(formatKey);

        return null;
    }

}
