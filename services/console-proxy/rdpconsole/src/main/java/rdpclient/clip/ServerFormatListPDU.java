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

import java.util.HashMap;
import java.util.Map;

import rdpclient.rdp.RdpConstants;
import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.MockSink;
import streamer.debug.MockSource;

public class ServerFormatListPDU extends BaseElement {

    protected ClipboardState state;

    public ServerFormatListPDU(String id, ClipboardState state) {
        super(id);
        this.state = state;
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        parseFormatNames(buf);
        buf.unref();

        // Automatically send request for text-based data to insert it into local
        // clipboard
        ClipboardDataFormat textFormat = ClipboardDataFormat.findBestTextFormat(state.serverClipboardDataFormats);
        if (textFormat != null) {
            // Send response: OK
            sendFormatListParseResponse(true);
            // Request data
            sendFormatDataRequest(textFormat);
        } else {
            // Send response: FAIL, we are not interested in this data
            sendFormatListParseResponse(false);
        }
    }

    /**
     * The Format Data Request PDU is sent by the recipient of the Format List
     * PDU. It is used to request the data for one of the formats that was listed
     * in the Format List PDU.
     */
    protected void sendFormatDataRequest(ClipboardDataFormat textFormat) {

        if (verbose)
            System.out.println("[" + this + "] INFO: Sending request for data in following format: " + textFormat + ".");

        // Store data format to parse server response later
        state.serverRequestedFormat = textFormat;

        ByteBuffer buf = new ByteBuffer(12, true);

        // Type
        buf.writeShortLE(ServerClipRdrChannelRouter.CB_FORMAT_DATA_REQUEST);
        // Message flags
        buf.writeShortLE(0);
        // Length
        buf.writeIntLE(4);

        // ID of chosen format
        buf.writeIntLE(textFormat.id);

        buf.trimAtCursor();

        pushDataToPad(STDOUT, buf);
    }

    /**
     * The Format List Response PDU is sent as a reply to the Format List PDU. It
     * is used to indicate whether processing of the Format List PDU was
     * successful.
     *
     * @param b
     */
    protected void sendFormatListParseResponse(boolean ok) {
        ByteBuffer buf = new ByteBuffer(8, true);

        // Type
        buf.writeShortLE(ServerClipRdrChannelRouter.CB_FORMAT_LIST_RESPONSE);
        // Message flags
        buf.writeShortLE((ok) ? ServerClipRdrChannelRouter.CB_RESPONSE_OK : ServerClipRdrChannelRouter.CB_RESPONSE_FAIL);
        // Length
        buf.writeIntLE(0);

        buf.trimAtCursor();

        pushDataToPad(STDOUT, buf);
    }

    protected void parseFormatNames(ByteBuffer buf) {

        // Set will not be modified after creation, so there is no need to make it
        // synchronous.
        Map<Object, ClipboardDataFormat> formats = new HashMap<Object, ClipboardDataFormat>();

        while (buf.cursor < buf.length) {
            int id = buf.readSignedIntLE();

            String name;
            if (state.serverUseLongFormatNames) {
                // Long format names in Unicode
                name = buf.readVariableWideString(RdpConstants.CHARSET_16);
            } else {
                Boolean asciiNames = (Boolean)buf.getMetadata(ServerClipRdrChannelRouter.ASCII_NAMES);

                if (asciiNames != null && asciiNames) {
                    // Short format names in ASCII
                    name = buf.readString(32, RdpConstants.CHARSET_8);
                } else {
                    // Short format names in Unicode
                    name = buf.readString(32, RdpConstants.CHARSET_16);
                }

            }

            // Store format in map by both ID and name (if name is not empty)
            formats.put(id, new ClipboardDataFormat(id, name));
            if (name.length() > 0)
                formats.put(name, new ClipboardDataFormat(id, name));
        }

        if (verbose)
            System.out.println("Server supports following formats for clipboard data: " + formats.values().toString() + ".");

        state.serverClipboardDataFormats = formats;
    }

    /**
     * Example.
     */
    public static void main(String[] args) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        /* @formatter:off */
        byte[] packet = new byte[] {
                0x02, 0x00,  //  CLIPRDR_HEADER::msgType = CB_FORMAT_LIST (2)
                0x00, 0x00,  //  CLIPRDR_HEADER::msgFlags = 0
                (byte) 0xe0, 0x00, 0x00, 0x00,  //  CLIPRDR_HEADER::dataLen = 0xe0 = 224 bytes

                (byte) 0x8a, (byte) 0xc0, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 0xc08a = 49290
                0x52, 0x00, 0x69, 0x00, 0x63, 0x00, 0x68, 0x00, 0x20, 0x00, 0x54, 0x00, 0x65, 0x00, 0x78, 0x00, 0x74, 0x00, 0x20, 0x00, 0x46, 0x00, 0x6f, 0x00, 0x72, 0x00, 0x6d, 0x00, 0x61, 0x00, 0x74, 0x00, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatName = "Rich Text Format"

                0x45, (byte) 0xc1, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 0xc145 = 49477
                0x52, 0x00, 0x69, 0x00, 0x63, 0x00, 0x68, 0x00, 0x20, 0x00, 0x54, 0x00, 0x65, 0x00, 0x78, 0x00,
                0x74, 0x00, 0x20, 0x00, 0x46, 0x00, 0x6f, 0x00, 0x72, 0x00, 0x6d, 0x00, 0x61, 0x00, 0x74, 0x00,
                0x20, 0x00, 0x57, 0x00, 0x69, 0x00, 0x74, 0x00, 0x68, 0x00, 0x6f, 0x00, 0x75, 0x00, 0x74, 0x00,
                0x20, 0x00, 0x4f, 0x00, 0x62, 0x00, 0x6a, 0x00, 0x65, 0x00, 0x63, 0x00, 0x74, 0x00, 0x73, 0x00,
                0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatName = "Rich Text Format Without Objects"

                0x43, (byte) 0xc1, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 0xc143 = 49475
                0x52, 0x00, 0x54, 0x00, 0x46, 0x00, 0x20, 0x00, 0x41, 0x00, 0x73, 0x00, 0x20, 0x00, 0x54, 0x00,
                0x65, 0x00, 0x78, 0x00, 0x74, 0x00, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatName = "RTF As Text"

                0x01, 0x00, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 1
                0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatName = ""

                0x0d, 0x00, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 0x0d = 13
                0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatName = ""

                0x04, (byte) 0xc0, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 0xc004 = 49156
                0x4e, 0x00, 0x61, 0x00, 0x74, 0x00, 0x69, 0x00, 0x76, 0x00, 0x65, 0x00, 0x00, 0x00,  //  "Native"

                0x0e, (byte) 0xc0, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 0xc00e = 49166
                0x4f, 0x00, 0x62, 0x00, 0x6a, 0x00, 0x65, 0x00, 0x63, 0x00, 0x74, 0x00, 0x20, 0x00, 0x44, 0x00,
                0x65, 0x00, 0x73, 0x00, 0x63, 0x00, 0x72, 0x00, 0x69, 0x00, 0x70, 0x00, 0x74, 0x00, 0x6f, 0x00,
                0x72, 0x00, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatName = "Object Descriptor"

                0x03, 0x00, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 3
                0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatName = ""

                0x10, 0x00, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 16
                0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatName = ""

                0x07, 0x00, 0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatId = 7
                0x00, 0x00,  //  CLIPRDR_LONG_FORMAT_NAME::formatName = ""
        };
        /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element router = new ServerClipRdrChannelRouter("router");
        ClipboardState state = new ClipboardState();
        state.serverUseLongFormatNames = true;
        Element format_list = new ServerFormatListPDU("format_list", state);

        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {
                // Format List Response PDU
                0x03, 0x00, // CLIPRDR_HEADER::msgType = CB_FORMAT_LIST_RESPONSE (3)
                0x01, 0x00, // CLIPRDR_HEADER::msgFlags = 0x0001 = CB_RESPONSE_OK
                0x00, 0x00, 0x00, 0x00, // CLIPRDR_HEADER::dataLen = 0 bytes
        }, new byte[] {
                // Format Data Request PDU
                0x04, 0x00, // CLIPRDR_HEADER::msgType = CB_FORMAT_DATA_REQUEST (4)
                0x00, 0x00, // CLIPRDR_HEADER::msgFlags = 0
                0x04, 0x00, 0x00, 0x00, // CLIPRDR_HEADER::dataLen = 4 bytes
                0x0d, 0x00, 0x00, 0x00, // CLIPRDR_FORMAT_DATA_REQUEST::requestedFormatId
                // = 0x0d
        }));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, router, format_list, sink);
        pipeline.link("source", "router >format_list", "format_list", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);

        // Check state
        if (!(state.serverClipboardDataFormats.containsKey(49475) && state.serverClipboardDataFormats.containsKey("Rich Text Format")))
            throw new RuntimeException("Server format list packet parsed incorrectly.");

    }

}
