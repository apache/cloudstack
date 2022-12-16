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
package rdpclient.rdp;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.MockSink;
import streamer.debug.MockSource;

public class ServerIOChannelRouter extends BaseElement {

    /**
     * Demand Active PDU.
     */
    public static final int PDUTYPE_DEMANDACTIVEPDU = 0x1;

    /**
     * Confirm Active PDU.
     */
    public static final int PDUTYPE_CONFIRMACTIVEPDU = 0x3;

    /**
     * Deactivate All PDU.
     */
    public static final int PDUTYPE_DEACTIVATEALLPDU = 0x6;

    /**
     * Data PDU (actual type is revealed by the pduType2 field in the Share Data
     * Header).
     */
    public static final int PDUTYPE_DATAPDU = 0x7;

    /**
     * Enhanced Security Server Redirection PDU.
     */
    public static final int PDUTYPE_SERVER_REDIR_PKT = 0xA;

    protected RdpState state;

    public ServerIOChannelRouter(String id, RdpState state) {
        super(id);
        this.state = state;
    }

    /**
     * @see http://msdn.microsoft.com/en-us/library/cc240576.aspx
     */
    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        int length = buf.readUnsignedShortLE();
        if (buf.length != length)
        {
            // It is ServerErrorAlert-ValidClient
            // Ignore it
            //throw new RuntimeException("[" + this + "] ERROR: Incorrect PDU length: " + length + ", data: " + buf + ".");
        }

        int type = buf.readUnsignedShortLE() & 0xf;

        // int sourceId = buf.readUnsignedShortLE();
        buf.skipBytes(2);

        switch (type) {
        case PDUTYPE_DEMANDACTIVEPDU:
            pushDataToPad("demand_active", buf);
            break;
        case PDUTYPE_CONFIRMACTIVEPDU:
            throw new RuntimeException("Unexpected client CONFIRM ACTIVE PDU. Data: " + buf + ".");
        case PDUTYPE_DEACTIVATEALLPDU:
            // pushDataToPad("deactivate_all", buf);
            /* ignore */buf.unref();
            break;
        case PDUTYPE_DATAPDU:
            handleDataPdu(buf);
            break;
        case PDUTYPE_SERVER_REDIR_PKT:
            // pushDataToPad("server_redir", buf);
            /* ignore */buf.unref();
            break;
        default:
            throw new RuntimeException("[" + this + "] ERROR: Unknown PDU type: " + type + ", data: " + buf + ".");
        }

    }

    /**
     * Graphics Update PDU.
     */
    public static final int PDUTYPE2_UPDATE = 0x02;

    /**
     * Control PDU.
     */
    public static final int PDUTYPE2_CONTROL = 0x14;

    /**
     * Pointer Update PDU.
     */
    public static final int PDUTYPE2_POINTER = 0x1B;

    /**
     * Input Event PDU.
     */
    public static final int PDUTYPE2_INPUT = 0x1C;

    /**
     * Synchronize PDU.
     */
    public static final int PDUTYPE2_SYNCHRONIZE = 0x1F;

    /**
     * Refresh Rect PDU.
     */
    public static final int PDUTYPE2_REFRESH_RECT = 0x21;

    /**
     * Play Sound PDU.
     */
    public static final int PDUTYPE2_PLAY_SOUND = 0x22;

    /**
     * Suppress Output PDU.
     */
    public static final int PDUTYPE2_SUPPRESS_OUTPUT = 0x23;

    /**
     * Shutdown Request PDU.
     */
    public static final int PDUTYPE2_SHUTDOWN_REQUEST = 0x24;

    /**
     * Shutdown Request Denied PDU.
     */
    public static final int PDUTYPE2_SHUTDOWN_DENIED = 0x25;

    /**
     * Save Session Info PDU.
     */
    public static final int PDUTYPE2_SAVE_SESSION_INFO = 0x26;

    /**
     * Font List PDU.
     */
    public static final int PDUTYPE2_FONTLIST = 0x27;

    /**
     * Font Map PDU.
     */
    public static final int PDUTYPE2_FONTMAP = 0x28;

    /**
     * Set Keyboard Indicators PDU.
     */
    public static final int PDUTYPE2_SET_KEYBOARD_INDICATORS = 0x29;

    /**
     * Persistent Key List PDU.
     */
    public static final int PDUTYPE2_BITMAPCACHE_PERSISTENT_LIST = 0x2B;

    /**
     * Bitmap Cache Error PDU.
     */
    public static final int PDUTYPE2_BITMAPCACHE_ERROR_PDU = 0x2C;

    /**
     * Set Keyboard IME Status PDU.
     */
    public static final int PDUTYPE2_SET_KEYBOARD_IME_STATUS = 0x2D;

    /**
     * Offscreen Bitmap Cache Error PDU.
     */
    public static final int PDUTYPE2_OFFSCRCACHE_ERROR_PDU = 0x2E;

    /**
     * Set Error Info PDU.
     */
    public static final int PDUTYPE2_SET_ERROR_INFO_PDU = 0x2F;

    /**
     * DrawNineGrid Cache Error PDU.
     */
    public static final int PDUTYPE2_DRAWNINEGRID_ERROR_PDU = 0x30;

    /**
     * GDI+ Error PDU.
     */
    public static final int PDUTYPE2_DRAWGDIPLUS_ERROR_PDU = 0x31;

    /**
     * Auto-Reconnect Status PDU.
     */
    public static final int PDUTYPE2_ARC_STATUS_PDU = 0x32;

    /**
     * Status Info PDU.
     */
    public static final int PDUTYPE2_STATUS_INFO_PDU = 0x36;

    /**
     * Monitor Layout PDU.
     */
    public static final int PDUTYPE2_MONITOR_LAYOUT_PDU = 0x37;

    /**
     * Indicates an Orders Update.
     */
    public static final int UPDATETYPE_ORDERS = 0x0000;

    /**
     * Indicates a Bitmap Graphics Update.
     */
    public static final int UPDATETYPE_BITMAP = 0x0001;

    /**
     * Indicates a Palette Update.
     */
    public static final int UPDATETYPE_PALETTE = 0x0002;

    /**
     * Indicates a Synchronize Update.
     */
    public static final int UPDATETYPE_SYNCHRONIZE = 0x0003;

    /**
     * @see http://msdn.microsoft.com/en-us/library/cc240577.aspx
     */
    protected void handleDataPdu(ByteBuffer buf) {

        // (4 bytes): A 32-bit, unsigned integer. Share identifier for the packet.
        long shareId = buf.readUnsignedIntLE();
        if (shareId != state.serverShareId)
            throw new RuntimeException("Unexpected share ID: " + shareId + ".");
//    buf.skipBytes(4);

        // Padding.
        buf.skipBytes(1);

        // (1 byte): An 8-bit, unsigned integer. The stream identifier for the
        // packet.
        // int streamId = buf.readUnsignedByte();
        buf.skipBytes(1);

        // (2 bytes): A 16-bit, unsigned integer. The uncompressed length of the
        // packet in bytes.
        int uncompressedLength = buf.readUnsignedShortLE();

        // (1 byte): An 8-bit, unsigned integer. The type of Data PDU.
        int type2 = buf.readUnsignedByte();

        // (1 byte): An 8-bit, unsigned integer. The compression type and flags
        // specifying the data following the Share Data Header
        int compressedType = buf.readUnsignedByte();
        if (compressedType != 0)
            throw new RuntimeException("Compression of protocol packets is not supported. Data: " + buf + ".");

        // (2 bytes): A 16-bit, unsigned integer. The compressed length of the
        // packet in bytes.
        int compressedLength = buf.readUnsignedShortLE();
        if (compressedLength != 0)
            throw new RuntimeException("Compression of protocol packets is not supported. Data: " + buf + ".");

        ByteBuffer data = buf.readBytes(uncompressedLength - 18);
        buf.unref();

        switch (type2) {

        case PDUTYPE2_UPDATE: {

            // (2 bytes): A 16-bit, unsigned integer. Type of the graphics update.
            int updateType = data.readUnsignedShortLE();
            ByteBuffer payload = data.readBytes(data.length - data.cursor);
            data.unref();

            switch (updateType) {
            case UPDATETYPE_ORDERS:
                pushDataToPad("orders", payload);
                break;
            case UPDATETYPE_BITMAP:
                pushDataToPad("bitmap", payload);
                break;
            case UPDATETYPE_PALETTE:
                pushDataToPad("palette", payload);
                break;
            case UPDATETYPE_SYNCHRONIZE:
                // Ignore
                payload.unref();
                break;
            }

            break;
        }
        case PDUTYPE2_CONTROL:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_CONTROL ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_POINTER:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_POINTER ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_INPUT:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_INPUT ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_SYNCHRONIZE:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_SYNCHRONIZE ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_REFRESH_RECT:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_REFRESH_RECT ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_PLAY_SOUND:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_PLAY_SOUND ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_SUPPRESS_OUTPUT:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_SUPPRESS_OUTPUT ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_SHUTDOWN_REQUEST:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_SHUTDOWN_REQUEST ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_SHUTDOWN_DENIED:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_SHUTDOWN_DENIED ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_SAVE_SESSION_INFO:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_SAVE_SESSION_INFO ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_FONTLIST:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_FONTLIST ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_FONTMAP:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_FONTMAP ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_SET_KEYBOARD_INDICATORS:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_SET_KEYBOARD_INDICATORS ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_BITMAPCACHE_PERSISTENT_LIST:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_BITMAPCACHE_PERSISTENT_LIST ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_BITMAPCACHE_ERROR_PDU:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_BITMAPCACHE_ERROR_PDU ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_SET_KEYBOARD_IME_STATUS:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_SET_KEYBOARD_IME_STATUS ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_OFFSCRCACHE_ERROR_PDU:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_OFFSCRCACHE_ERROR_PDU ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_SET_ERROR_INFO_PDU:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_SET_ERROR_INFO_PDU ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_DRAWNINEGRID_ERROR_PDU:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_DRAWNINEGRID_ERROR_PDU ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_DRAWGDIPLUS_ERROR_PDU:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_DRAWGDIPLUS_ERROR_PDU ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_ARC_STATUS_PDU:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_ARC_STATUS_PDU ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_STATUS_INFO_PDU:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_STATUS_INFO_PDU ignored.");
            // Ignore
            data.unref();
            break;
        case PDUTYPE2_MONITOR_LAYOUT_PDU:
            if (verbose)
                System.out.println("[" + this + "] INFO: Packet PDUTYPE2_MONITOR_LAYOUT_PDU ignored.");
            // Ignore
            data.unref();
            break;

        default:
            throw new RuntimeException("Unknown data PDU type: " + type2 + ", data: " + buf + ".");
        }
    }

    /**
     * Example.
     *
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        byte[] packet = new byte[] {
                // TPKT
                (byte)0x03, (byte)0x00, // TPKT Header: TPKT version = 3
                (byte)0x00, (byte)0x1B, // TPKT length: 27 bytes

                // X224
                (byte)0x02, // X224 Length: 2 bytes
                (byte)0xF0, // X224 Type: Data
                (byte)0x80, // X224 EOT

                // MCS
                // Type: send data indication: 26 (0x1a, top 6 bits)
                (byte)0x68, // ??

                (byte)0x00, (byte)0x01, // User ID: 1002 (1001+1)
                (byte)0x03, (byte)0xEB, // Channel ID: 1003
                (byte)0x70, // Data priority: high, segmentation: begin|end
                (byte)0x0D, // Payload length: 13 bytes

                // Deactivate all PDU
                (byte)0x0D, (byte)0x00, // Length: 13 bytes (LE)

                // - PDUType: (0x16, LE)
                // Type: (............0110) TS_PDUTYPE_DEACTIVATEALLPDU
                // ProtocolVersion: (000000000001....) 1
                (byte)0x16, (byte)0x00,

                (byte)0xEA, (byte)0x03, // PDU source: 1002 (LE)
                (byte)0xEA, (byte)0x03, (byte)0x01, (byte)0x00, // ShareID = 66538

                (byte)0x01, (byte)0x00, // Length if source descriptor: 1 (LE)
                (byte)0x00, // Source descriptor (should be set to 0): 0
        };

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet));
        RdpState rdpState = new RdpState() {
            {
                serverShareId = 66538;
            }
        };
        Element channel1003 = new ServerIOChannelRouter("channel_1003", rdpState);
        Element mcs = new ServerMCSPDU("mcs");
        Element tpkt = new ServerTpkt("tpkt");
        Element x224 = new ServerX224DataPdu("x224");
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {
                // Deactivate all PDU
                (byte)0x0D, (byte)0x00, // Length: 13 bytes (LE)

                // - PDUType: 22 (0x16, LE)
                // Type: (............0110) TS_PDUTYPE_DEACTIVATEALLPDU
                // ProtocolVersion: (000000000001....) 1
                (byte)0x16, (byte)0x00,

                (byte)0xEA, (byte)0x03, // PDU source: 1002 (LE)
                (byte)0xEA, (byte)0x03, (byte)0x01, (byte)0x00, // ShareID = 66538

                (byte)0x01, (byte)0x00, // Length if source descriptor: 1 (LE)
                (byte)0x00, // Source descriptor (should be set to 0): 0
        }));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, tpkt, x224, mcs, channel1003, sink);
        pipeline.link("source", "tpkt", "x224", "mcs >channel_1003", "channel_1003 >deactivate_all", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
