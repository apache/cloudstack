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

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Link;

public class ServerClipRdrChannelRouter extends BaseElement {

    /**
     * Key for ASCII names message flag in payload metadata.  Value is Boolean.
     */
    public static final String ASCII_NAMES = "ascii_names";

    /**
     * Key for success/fail message flag in payload metadata. Value is Boolean.
     */
    public static final String SUCCESS = "success";

    /**
     * Monitor Ready PDU
     */
    public static final int CB_MONITOR_READY = 0x0001;

    /**
     * Format List PDU
     */
    public static final int CB_FORMAT_LIST = 0x0002;

    /**
     * Format List Response PDU
     */
    public static final int CB_FORMAT_LIST_RESPONSE = 0x0003;

    /**
     * Format Data Request PDU
     */
    public static final int CB_FORMAT_DATA_REQUEST = 0x0004;

    /**
     * Format Data Response PDU
     */
    public static final int CB_FORMAT_DATA_RESPONSE = 0x0005;

    /**
     * Temporary Directory PDU
     */
    public static final int CB_TEMP_DIRECTORY = 0x0006;

    /**
     * Clipboard Capabilities PDU
     */
    public static final int CB_CLIP_CAPS = 0x0007;

    /**
     * File Contents Request PDU
     */
    public static final int CB_FILECONTENTS_REQUEST = 0x0008;

    /**
     * File Contents Response PDU
     */
    public static final int CB_FILECONTENTS_RESPONSE = 0x0009;

    /**
     * Lock Clipboard Data PDU
     */
    public static final int CB_LOCK_CLIPDATA = 0x000A;

    /**
     * Unlock Clipboard Data PDU
     */
    public static final int CB_UNLOCK_CLIPDATA = 0x000B;

    /**
     * Used by the Format List Response PDU, Format Data Response PDU, and File
     * Contents Response PDU to indicate that the associated request Format List
     * PDU, Format Data Request PDU, and File Contents Request PDU were processed
     * successfully.
     */
    public static final int CB_RESPONSE_OK = 0x0001;

    /**
     * Used by the Format List Response PDU, Format Data Response PDU, and File
     * Contents Response PDU to indicate that the associated Format List PDU,
     * Format Data Request PDU, and File Contents Request PDU were not processed
     * successfully.
     */
    public static final int CB_RESPONSE_FAIL = 0x0002;

    /**
     * Used by the Short Format Name variant of the Format List Response PDU to
     * indicate the format names are in ASCII 8.
     */
    public static final int CB_ASCII_NAMES = 0x0004;

    public ServerClipRdrChannelRouter(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Parse PDU header
        // Example: 07 00 -> CLIPRDR_HEADER::msgType = CB_CLIP_CAPS (7)
        int msgType = buf.readUnsignedShortLE();

        // Example: 00 00 -> CLIPRDR_HEADER::msgFlags = 0
        int msgFlags = buf.readUnsignedShortLE();

        // Example: 10 00 00 00 -> CLIPRDR_HEADER::dataLen = 0x10 = 16 bytes
        long dataLenLong = buf.readSignedIntLE();
        if (dataLenLong > 4 * 1024 * 1024)
            throw new RuntimeException("Clipboard packet is too long. Expected length: less than 4MiB. Actual length: " + dataLenLong + ".");
        int dataLen = (int)dataLenLong;

        ByteBuffer payload = buf.readBytes(dataLen);

        // Parse message flags and store them in the payload metadata
        if ((msgFlags & CB_RESPONSE_OK) == CB_RESPONSE_OK)
            payload.putMetadata("success", true);
        if ((msgFlags & CB_RESPONSE_FAIL) == CB_RESPONSE_FAIL)
            payload.putMetadata(SUCCESS, false);
        if ((msgFlags & CB_ASCII_NAMES) == CB_ASCII_NAMES)
            payload.putMetadata(ASCII_NAMES, true);

        // Push PDU to appropriate handler
        switch (msgType) {
        case CB_MONITOR_READY:
            pushDataToPad("monitor_ready", payload);
            break;
        case CB_FORMAT_LIST:
            pushDataToPad("format_list", payload);
            break;
        case CB_FORMAT_LIST_RESPONSE:
            pushDataToPad("format_list_response", payload);
            break;
        case CB_FORMAT_DATA_REQUEST:
            pushDataToPad("format_data_request", payload);
            break;
        case CB_FORMAT_DATA_RESPONSE:
            pushDataToPad("format_data_response", payload);
            break;
        case CB_TEMP_DIRECTORY:
            throw new RuntimeException("[" + this + "] ERROR: Unexpected clipboard temporary directory PDU received from server. Data: " + buf + ".");
        case CB_CLIP_CAPS:
            pushDataToPad("clipboard_capabilities", payload);
            break;
        case CB_FILECONTENTS_REQUEST:
            pushDataToPad("filecontent_request", payload);
            break;
        case CB_FILECONTENTS_RESPONSE:
            pushDataToPad("filecontent_response", payload);
            break;
        case CB_LOCK_CLIPDATA:
            pushDataToPad("lock_clipdata", payload);
            break;
        case CB_UNLOCK_CLIPDATA:
            pushDataToPad("unlock_clipdata", payload);
            break;
        default:
            throw new RuntimeException("[" + this + "] ERROR: Unknown clipboard PDU message type: " + msgType + ".");
        }

        buf.unref();

    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        // TODO
    }

}
