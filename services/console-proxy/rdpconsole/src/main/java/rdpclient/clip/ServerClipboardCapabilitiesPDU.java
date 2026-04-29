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
import streamer.Element;
import streamer.Link;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.MockSink;
import streamer.debug.MockSource;

public class ServerClipboardCapabilitiesPDU extends BaseElement {

    /**
     * General capability set.
     */
    public static final int CB_CAPSTYPE_GENERAL = 0x1;

    /**
     * The Long Format Name variant of the Format List PDU is supported for
     * exchanging updated format names. If this flag is not set, the Short Format
     * Name variant MUST be used. If this flag is set by both protocol endpoints,
     * then the Long Format Name variant MUST be used.
     */
    public static final int CB_USE_LONG_FORMAT_NAMES = 0x00000002;

    /**
     * File copy and paste using stream-based operations are supported using the
     * File Contents Request PDU and File Contents Response PDU.
     */
    public static final int CB_STREAM_FILECLIP_ENABLED = 0x00000004;

    /**
     * Indicates that any description of files to copy and paste MUST NOT include
     * the source path of the files.
     */
    public static final int CB_FILECLIP_NO_FILE_PATHS = 0x00000008;

    /**
     * Locking and unlocking of File Stream data on the clipboard is supported
     * using the Lock Clipboard Data PDU and Unlock Clipboard Data PDU.
     */
    public static final int CB_CAN_LOCK_CLIPDATA = 0x00000010;

    protected ClipboardState state;

    public ServerClipboardCapabilitiesPDU(String id, ClipboardState state) {
        super(id);
        this.state = state;
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // 0x01, 0x00, // CLIPRDR_CAPS::cCapabilitiesSets = 1
        int cCapabilitiesSets = buf.readUnsignedShortLE();

        // 0x00, 0x00, // CLIPRDR_CAPS::pad1
        buf.skipBytes(2);

        // Parse all capability sets
        for (int capabilitySet = 0; capabilitySet < cCapabilitiesSets; capabilitySet++) {
            // 0x01, 0x00, // CLIPRDR_CAPS_SET::capabilitySetType =
            // CB_CAPSTYPE_GENERAL (1)
            int capabilitySetType = buf.readUnsignedShortLE();

            // 0x0c, 0x00, // CLIPRDR_CAPS_SET::lengthCapability = 0x0c = 12 bytes
            int lengthCapability = buf.readUnsignedShortLE();

            // parse capability set
            switch (capabilitySetType) {
            case CB_CAPSTYPE_GENERAL:
                parseGeneralCapabilitySet(buf.readBytes(lengthCapability - 4));
                break;
            default:
                // Ignore
                // throw new RuntimeException("Unknown capability set type: " +
                // capabilitySetType + ". Expected value: CB_CAPSTYPE_GENERAL (1).");
            }
        }

        buf.unref();
    }

    protected void parseGeneralCapabilitySet(ByteBuffer buf) {
        // 0x02, 0x00, 0x00, 0x00, // CLIPRDR_GENERAL_CAPABILITY::version =
        // CB_CAPS_VERSION_2 (2)
        // long version = buf.readUnsignedIntLE();
        buf.skipBytes(4);

        // 0x0e, 0x00, 0x00, 0x00, // CLIPRDR_GENERAL_CAPABILITY::capabilityFlags
        // = 0x0000000e = 0x02 |0x04 |0x08 = CB_USE_LONG_FORMAT_NAMES |
        // CB_STREAM_FILECLIP_ENABLED | CB_FILECLIP_NO_FILE_PATHS
        int flags = buf.readSignedIntLE();

        if ((flags & CB_USE_LONG_FORMAT_NAMES) == CB_USE_LONG_FORMAT_NAMES) {
            state.serverUseLongFormatNames = true;
            if (verbose)
                System.out.println("[" + this + "] INFO: Server can use long format names for clipboard data.");
        }

        if ((flags & CB_STREAM_FILECLIP_ENABLED) == CB_STREAM_FILECLIP_ENABLED) {
            state.serverStreamFileClipEnabled = true;
            if (verbose)
                System.out.println("[" + this + "] INFO: Server supports stream based file clipboard operations.");
        }

        if ((flags & CB_FILECLIP_NO_FILE_PATHS) == CB_FILECLIP_NO_FILE_PATHS) {
            state.serverFileClipNoFilePaths = true;
            if (verbose)
                System.out.println("[" + this
                        + "] INFO: Server Indicates that any description of files to copy and paste MUST NOT include the source path of the files.");
        }

        if ((flags & CB_CAN_LOCK_CLIPDATA) == CB_CAN_LOCK_CLIPDATA) {
            state.serverCanLockClipdata = true;
            if (verbose)
                System.out.println("[" + this + "] INFO: Server can lock and unlock file streams on the clipboard.");
        }

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
                0x07, 0x00,  //  CLIPRDR_HEADER::msgType = CB_CLIP_CAPS (7)
                0x00, 0x00,  //  CLIPRDR_HEADER::msgFlags = 0
                0x10, 0x00, 0x00, 0x00,  //  CLIPRDR_HEADER::dataLen = 0x10 = 16 bytes
                0x01, 0x00,  //  CLIPRDR_CAPS::cCapabilitiesSets = 1
                0x00, 0x00,  //  CLIPRDR_CAPS::pad1
                0x01, 0x00,  //  CLIPRDR_CAPS_SET::capabilitySetType = CB_CAPSTYPE_GENERAL (1)
                0x0c, 0x00,  //  CLIPRDR_CAPS_SET::lengthCapability = 0x0c = 12 bytes
                0x02, 0x00, 0x00, 0x00,  //  CLIPRDR_GENERAL_CAPABILITY::version = CB_CAPS_VERSION_2 (2)
                0x0e, 0x00, 0x00, 0x00,  //  CLIPRDR_GENERAL_CAPABILITY::capabilityFlags = 0x0000000e = 0x02 |0x04 |0x08 = CB_USE_LONG_FORMAT_NAMES | CB_STREAM_FILECLIP_ENABLED | CB_FILECLIP_NO_FILE_PATHS
        };
        /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element router = new ServerClipRdrChannelRouter("router");
        ClipboardState state = new ClipboardState();
        Element clip_cap = new ServerClipboardCapabilitiesPDU("clip_cap", state);
        Element sink = new MockSink("sink", new ByteBuffer[] {});

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, router, clip_cap, sink);
        pipeline.link("source", "router >clipboard_capabilities", "clip_cap", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);

        // Check state
        if (!state.serverUseLongFormatNames || !state.serverStreamFileClipEnabled || !state.serverFileClipNoFilePaths || state.serverCanLockClipdata)
            throw new RuntimeException("Server clipboard capabilities packet parsed incorrectly.");

    }

}
