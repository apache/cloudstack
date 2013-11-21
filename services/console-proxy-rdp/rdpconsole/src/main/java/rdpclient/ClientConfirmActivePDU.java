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
package rdpclient;

import streamer.BaseElement;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.MockSink;
import streamer.MockSource;
import streamer.Pipeline;
import streamer.PipelineImpl;
import common.ScreenDescription;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240488.aspx
 */
public class ClientConfirmActivePDU extends BaseElement {

    public static final String SOURCE_DESC = "MSTSC";

    public static final int CAPSTYPE_BITMAP = 0x2;

    protected int numberCapabilities;

    protected RdpState state;
    protected ScreenDescription screen;

    protected boolean desktopResize = false;
    protected int prefferedBitsPerPixel = 16;

    public ClientConfirmActivePDU(String id, ScreenDescription screen, RdpState state) {
        super(id);
        this.state = state;
        this.screen = screen;
    }

    @Override
    public void handleData(ByteBuffer aBuf, Link link) {

        // Body
        ByteBuffer buf = new ByteBuffer(1024, true);
        numberCapabilities = 0;
        writeCapabilities(buf);
        buf.trimAtCursor();

        // Header
        ByteBuffer header = createMCSHeader(buf);

        // Length of source descriptor, including NULL character (LE)
        header.writeShortLE(SOURCE_DESC.length() + 1);

        // Length of combined capabilities + 4 bytes (number of capabilities and
        // padding) (LE)
        header.writeShortLE(buf.length + 4);

        header.writeString(SOURCE_DESC, RdpConstants.CHARSET_8);
        header.writeByte(0);

        // Number of capabilities
        header.writeShortLE(numberCapabilities);

        // Padding 2 bytes
        header.writeShortLE(0);

        header.trimAtCursor();

        // Prepend header to capabilities
        buf.prepend(header);

        // Trim buffer to actual length of data written
        buf.length = buf.cursor;

        pushDataToPad(STDOUT, buf);

        sendOtherRequredPackets();

    }

    private ByteBuffer createMCSHeader(ByteBuffer buf) {
        ByteBuffer header = new ByteBuffer(100);
        // MCS Send Data Request
        header.writeByte(0x64);

        // Initiator: 1004 (1001+3)
        header.writeShort(3);

        // Channel ID: 1003 (I/O channel)
        header.writeShort(1003);

        // Data priority: high (0x40), segmentation: begin (0x20) | end (0x10)
        header.writeByte(0x70);

        int length = buf.length + 26;

        // User data length: (variable length field, LE)
        header.writeVariableShort(length);

        // Total length: (LE)
        header.writeShortLE(length);

        // PDU type: Confirm Active PDU (0x3), TS_PROTOCOL_VERSION (0x10) (LE)
        header.writeShortLE(0x13);

        // PDU source: 1004 (LE)
        header.writeShortLE(1004);

        // Share ID, e.g. 0x000103ea (LE)
        header.writeIntLE((int)state.serverShareId);

        // Originator ID: 1002 (LE)
        header.writeShortLE(1002);
        return header;
    }

    private void sendOtherRequredPackets() {
        // Send sequence in bulk

        sendSynchronizePDU();
        sendControlPDUActionCooperate();
        sendControlPDUActionRequestControl();
        // sendBitmapCachePersistentListPDU();
        sendFontListPDU();
    }

    private void sendFontListPDU() {
        {
            int length = 1024; // Large enough
            ByteBuffer buf = new ByteBuffer(length, true);

            /* @formatter:off */
      buf.writeBytes(new byte[] {
          // MCS Send Data Request
          (byte)0x64,
          // Initiator: 1004 (1001+3)
          (byte)0x00, (byte)0x03,
          // Channel ID: 1003 (I/O channel)
          (byte)0x03, (byte)0xeb,
          // Data priority: high (0x40), segmentation: begin (0x20) | end (0x10)
          (byte)0x70,
          // User data length: 26 bytes (0x1a, variable length field)
          (byte)0x80, (byte)0x1a,

          // Total length: 26 bytes (0x1a, LE)
          (byte)0x1a, (byte)0x00,
          // PDU type: PDUTYPE_DATAPDU (0x7), PDU version: 1 (0x0010) (LE)
          (byte)0x17, (byte)0x00,
          // PDU source: 1004 (LE)
          (byte)0xec, (byte)0x03,
      });
      // Share ID, 4 bytes  (LE)
      buf.writeIntLE((int)state.serverShareId);

      buf.writeBytes(new byte[] {
          // Padding 1 byte
          (byte)0x00,
          // Stream ID: STREAM_LOW (1)
          (byte)0x01,
          // uncompressedLength : 12 bytes (LE)
          (byte)0x0c, (byte)0x00,

          // pduType2: PDUTYPE2_FONTLIST (39)
          (byte)0x27,
          // generalCompressedType: 0
          (byte)0x00,
          // generalCompressedLength: 0 (LE)
          (byte)0x00, (byte)0x00,

          // numberEntries (should be set to zero): 0 (LE)
          (byte)0x00, (byte)0x00,
          // totalNumEntries (should be set to zero): 0 (LE)
          (byte)0x00, (byte)0x00,
          // listFlags  (should be set to 0x3): 0x0003 (LE), FONTLIST_LAST(0x2) | FONTLIST_FIRST(0x1)
          (byte)0x03, (byte)0x00,
          // entrySize: 50 bytes (0x0032, LE)
          (byte)0x32, (byte)0x00,
      });
      /* @formatter:on */

            // Trim buffer to actual length of data written
            buf.trimAtCursor();

            pushDataToPad(STDOUT, buf);
        }
    }

    private void sendControlPDUActionRequestControl() {
        int length = 1024; // Large enough
        ByteBuffer buf = new ByteBuffer(length, true);

        /* @formatter:off */
    buf.writeBytes(new byte[] {
        // MCS Send Data Request
        (byte)0x64,
        // Initiator: 1004 (1001+3)
        (byte)0x00, (byte)0x03,
        // Channel ID: 1003 (I/O channel)
        (byte)0x03, (byte)0xeb,
        // Data priority: high (0x40), segmentation: begin (0x20) | end (0x10)
        (byte)0x70,
        // User data length: 26 bytes (0x1a, variable length field)
        (byte)0x80, (byte)0x1a,

        // Total length: 26 bytes (0x1a, LE)
        (byte)0x1a, (byte)0x00,
        // PDU type: PDUTYPE_DATAPDU (0x7), PDU version: 1 (0x0010) (LE)
        (byte)0x17, (byte)0x00,
        // PDU source: 1004 (LE)
        (byte)0xec, (byte)0x03,
    });
        // Share ID, 4 bytes  (LE)
    buf.writeIntLE((int)state.serverShareId);

    buf.writeBytes(new byte[] {
        // Padding 1 byte
        (byte)0x00,
        // Stream ID: STREAM_LOW (1)
        (byte)0x01,
        // uncompressedLength : 12 bytes (LE)
        (byte)0x0c, (byte)0x00,
        // pduType2: PDUTYPE2_CONTROL (20)
        (byte)0x14,
        // generalCompressedType: 0
        (byte)0x00,
        // generalCompressedLength: 0 (LE)
        (byte)0x00, (byte)0x00,

        // action: CTRLACTION_REQUEST_CONTROL (1) (LE)
        (byte)0x01, (byte)0x00,
        // grantId: 0 (LE)
        (byte)0x00, (byte)0x00,
        // controlId: 0 (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
    });
    /* @formatter:on */

        // Trim buffer to actual length of data written
        buf.trimAtCursor();

        pushDataToPad(STDOUT, buf);
    }

    private void sendControlPDUActionCooperate() {
        int length = 1024; // Large enough
        ByteBuffer buf = new ByteBuffer(length, true);

        /* @formatter:off */
    buf.writeBytes(new byte[] {
        // MCS Send Data Request
        (byte)0x64,
        // Initiator: 1004 (1001+3)
        (byte)0x00, (byte)0x03,
        // Channel ID: 1003 (I/O channel)
        (byte)0x03, (byte)0xeb,
        // Data priority: high (0x40), segmentation: begin (0x20) | end (0x10)
        (byte)0x70,
        // User data length: 26 bytes (0x1a, variable length field)
        (byte)0x80, (byte)0x1a,

        // Total length: 26 bytes (0x1a, LE)
        (byte)0x1a,(byte)0x00,
        // PDU type: PDUTYPE_DATAPDU (0x7), PDU version: 1 (0x0010) (LE)
        (byte)0x17, (byte)0x00,
        // PDU source: 1004 (LE)
        (byte)0xec, (byte)0x03,
    });
    // Share ID, 4 bytes  (LE)
    buf.writeIntLE((int)state.serverShareId);

    buf.writeBytes(new byte[] {
        // Padding 1 byte
        (byte)0x00,
        // Stream ID: STREAM_LOW (1)
        (byte)0x01,
        // uncompressedLength : 12 bytes (LE)
        (byte)0x0c, (byte)0x00,
        // pduType2: PDUTYPE2_CONTROL (20)
        (byte)0x14,
        // generalCompressedType: 0
        (byte)0x00,
        // generalCompressedLength: 0 (LE?)
        (byte)0x00, (byte)0x00,
        // action: CTRLACTION_COOPERATE (4) (LE)
        (byte)0x04, (byte)0x00,
        // grantId: 0 (LE)
        (byte)0x00, (byte)0x00,
        // controlId: 0
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
    });
    /* @formatter:on */

        buf.trimAtCursor();

        pushDataToPad(STDOUT, buf);
    }

    private void sendSynchronizePDU() {

        ByteBuffer buf = new ByteBuffer(1024, true);
        /* @formatter:off */
    buf.writeBytes(new byte[] {
        // MCS send data request
        (byte)0x64,
        // Initiator: 1004 (1001+3)
        (byte)0x00, (byte)0x03,
        // Channel ID: 1003 (I/O Channel)
        (byte)0x03, (byte)0xeb,
        // Data priority: high (0x40), segmentation: begin (0x20) | end (0x10)
        (byte)0x70,
        // Data length:  22 bytes (0x16, variable length field)
        (byte)0x80,  (byte)0x16,

        // RDP: total length: 22 bytes (LE)
        (byte)0x16, (byte)0x00,

        // PDU type: PDUTYPE_DATAPDU (0x7), TS_PROTOCOL_VERSION (0x10) (LE)
        (byte)0x17, (byte)0x00,

        // PDU source: 1007 (LE)
        (byte)0xec, (byte)0x03,
    });
    // Share ID, 4 bytes  (LE)
    buf.writeIntLE((int)state.serverShareId);

    buf.writeBytes(new byte[] {
        // Padding: 1 byte
        (byte)0x00,
        // Stream ID: STREAM_LOW (1)
        (byte)0x01,
        // uncompressedLength : 8 bytes (LE)
        (byte)0x08, (byte)0x00,
        // pduType2 = PDUTYPE2_SYNCHRONIZE (31)
        (byte)0x1f,
        // generalCompressedType: 0
        (byte)0x00,
        // generalCompressedLength: 0 (LE?)
        (byte)0x00, (byte)0x00,
        //  messageType: SYNCMSGTYPE_SYNC (1) (LE)
        (byte)0x01, (byte)0x00,
        // targetUser: 0x03ea
        (byte)0xea, (byte)0x03,
    });
    /* @formatter:on */
        buf.trimAtCursor();
        pushDataToPad(STDOUT, buf);
    }

    private void writeCapabilities(ByteBuffer buf) {
        writeGeneralCS(buf);

        writeBitmapCS(buf);

        writeOrderCS(buf);

        writeBitmapCache2CS(buf);

        writeColorTableCacheCS(buf);

        writeWindowActivationCS(buf);

        writeControlCS(buf);

        writePointerCS(buf);

        writeShareCS(buf);

        writeInputCS(buf);

        writeBrushCS(buf);

        writeSoundCS(buf);

        writeFontCS(buf);

        writeOffscreenBitmapCS(buf);

        writeGlyphCacheCS(buf);
    }

    private void writeBrushCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Brush Capability Set (8 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240564.aspx
            (byte)0x0f, (byte)0x00, // capability set type: CAPSTYPE_BRUSH (15,
                                    // LE)
            (byte)0x08, (byte)0x00, // length of capability set: 8 bytes (LE)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // brushSupportLevel:
                                                            // BRUSH_DEFAULT
                                                            // (0x0, LE)

        });
    }

    private void writeInputCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Input Capability Set (88 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240563.aspx
            (byte)0x0d,
            (byte)0x00, // capability set type: CAPSTYPE_INPUT (13, LE)
            (byte)0x58,
            (byte)0x00, // length of capability set: 88 bytes (LE)
            (byte)0x35,
            (byte)0x00, // inputFlags: 0x0035 (LE), INPUT_FLAG_FASTPATH_INPUT2
                        // (0x20), INPUT_FLAG_VKPACKET (0x10), INPUT_FLAG_MOUSEX
                        // (0x4), INPUT_FLAG_SCANCODES (0x1)
            (byte)0x00,
            (byte)0x00, // Padding 2 bytes
            (byte)0x09,
            (byte)0x04,
            (byte)0x00,
            (byte)0x00, // keyboardLayout: "US" keyboard layout (0x000409, LE)
            (byte)0x00,
            (byte)0x00,
            (byte)0x00,
            (byte)0x00, // keyboardType: unknown (LE)
            (byte)0x00,
            (byte)0x00,
            (byte)0x00,
            (byte)0x00, // keyboardSubType: unknown (LE)
            (byte)0x00,
            (byte)0x00,
            (byte)0x00,
            (byte)0x00, // keyboardFunctionKey: unknown (LE)
            // imeFileName: "", (64 bytes, including trailing NULL characters, UCS2)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,

        });
    }

    private void writeShareCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Share Capability Set (8 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240570.aspx
            (byte)0x09, (byte)0x00, // capability set type: CAPSTYPE_SHARE (9, LE)
            (byte)0x08, (byte)0x00, // length of capability set: 8 bytes (LE)
            (byte)0x00, (byte)0x00, // nodeID (must be set to 0 by client): 0 (LE)
            (byte)0x00, (byte)0x00, // Padding 2 bytes (LE)

        });
    }

    private void writePointerCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Pointer Capability Set (10 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240562.aspx
            (byte)0x08, (byte)0x00, // capability set type: CAPSTYPE_POINTER (8,
                                    // LE)
            (byte)0x0a, (byte)0x00, // length of capability set: 10 bytes (LE)
            (byte)0x00, (byte)0x00, // colorPointerFlag: FALSE (LE)
            (byte)0x00, (byte)0x00, // colorPointerCacheSize: 0 (LE)
            (byte)0x14, (byte)0x00, // pointerCacheSize: 20 (LE)

        });
    }

    private void writeControlCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Control Capability Set (12 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240568.aspx
            (byte)0x05, (byte)0x00, // capability set type: CAPSTYPE_ACTIVATION
                                    // (7)
            (byte)0x0c, (byte)0x00, // length of capability set: 12 bytes (LE)
            (byte)0x00, (byte)0x00, // controlFlags (should be set to 0): 0 (LE)
            (byte)0x00, (byte)0x00, // remoteDetachFlag (should be set to 0): 0
                                    // (LE)
            (byte)0x02, (byte)0x00, // controlInterest (should be set to
                                    // CONTROLPRIORITY_NEVER):
                                    // CONTROLPRIORITY_NEVER (2) (LE)
            (byte)0x02, (byte)0x00, // detachInterest (should be set to
                                    // CONTROLPRIORITY_NEVER):
                                    // CONTROLPRIORITY_NEVER (2) (LE)

        });
    }

    private void writeWindowActivationCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Window Activation Capability Set (12 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240569.aspx
            (byte)0x07, (byte)0x00, // capability set type: CAPSTYPE_ACTIVATION
                                    // (7) (LE)
            (byte)0x0c, (byte)0x00, // length of capability set: 12 bytes (LE)
            (byte)0x00, (byte)0x00, // helpKeyFlag (should be set to FALSE (0)):
                                    // FALSE (0, LE)
            (byte)0x00, (byte)0x00, // helpKeyIndexFlag (should be set to FALSE
                                    // (0)): FALSE (0, LE)
            (byte)0x00, (byte)0x00, // helpExtendedKeyFlag (should be set to FALSE
                                    // (0)): FALSE (0, LE)
            (byte)0x00, (byte)0x00, // windowManagerKeyFlag (should be set to
                                    // FALSE (0)): FALSE (0, LE)

        });
    }

    private void writeColorTableCacheCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {

            //
            // Color Table Cache Capability Set (8 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc241564.aspx
            (byte)0x0a, (byte)0x00, // capability set type: CAPSTYPE_COLORCACHE
                                    // (10) (LE)
            (byte)0x08, (byte)0x00, // length of capability set: 8 bytes (LE)
            (byte)0x06, (byte)0x00, // Color table cache size (must be ignored
                                    // during capability exchange and is assumed
                                    // to be 0x0006): 6 (LE)
            (byte)0x00, (byte)0x00, // Padding 2 bytes

        });
    }

    private void writeBitmapCache2CS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Bitmap Cache Rev. 2 Capability Set (40 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240560.aspx
            (byte)0x13, (byte)0x00, // capability set type:
                                    // CAPSTYPE_BITMAPCACHE_REV2 (19) (LE)
            (byte)0x28, (byte)0x00, // length of capability set: 40 bytes (LE)
            (byte)0x00, (byte)0x00, // Cache flags: 0 (LE)
            (byte)0x00, // Padding 1 byte
            (byte)0x00, // Number of cell caches: 0
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache0
                                                            // cell info: 0 (LE)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache1
                                                            // cell info: 0 (LE)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache2
                                                            // cell info: 0 (LE)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache3
                                                            // cell info: 0 (LE)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache4
                                                            // cell info: 0 (LE)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Padding 12 bytes
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Padding
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Padding
        });
    }

    private void writeGeneralCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            // Capabilities, see
            // http://msdn.microsoft.com/en-us/library/cc240486.aspx

            //
            // General capability set (24 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240549.aspx
            (byte)0x01, (byte)0x00, // capability set type: CAPSTYPE_GENERAL (1)
                                    // (LE)
            (byte)0x18, (byte)0x00, // length of capability set: 24 bytes (LE)
            (byte)0x01, (byte)0x00, // TS_OSMAJORTYPE_WINDOWS (1) (LE)
            (byte)0x03, (byte)0x00, // TS_OSMINORTYPE_WINDOWS_NT (3) (LE)
            (byte)0x00, (byte)0x02, // TS_CAPS_PROTOCOLVERSION (0x0200) (LE)
            (byte)0x00, (byte)0x00, // Padding 2 bytes
            (byte)0x00, (byte)0x00, // generalCompressionTypes: 0 (LE)

            // Extra flags: 0x040d (LE)
            // FastPathOutput: (...............1) Advertiser supports fast-path
            // output
            // ShadowCompression: (..............0.) Advertiser NOT supports shadow
            // compression
            // LongLengthCredentials: (.............1..) Advertiser supports
            // long-length credentials for the user name, password, or domain name
            // SessionAutoreconnection: (............1...) Advertiser supports
            // session auto-reconnection
            // ImprovedEncryptionChecksum: (...........0....) Client and server NOT
            // support improved encryption checksum
            // Reserved1: (......00000.....)
            // CompressedBitMapDataFlag: (.....1..........) No 8-UINT8 header is
            // present for compressed bitmap data
            // Reserved2: (00000...........)
            (byte)0x0d, (byte)0x04,

            (byte)0x00, (byte)0x00, // updateCapabilityFlag: 0 (LE)
            (byte)0x00, (byte)0x00, // remoteUnshareFlag: 0 (LE)
            (byte)0x00, (byte)0x00, // generalCompressionLevel: 0 (LE)
            (byte)0x00, // refreshRectSupport: FALSE (0)
            (byte)0x00, // suppressOutputSupport: FALSE (0)

        });
    }

    private void writeBitmapCS(ByteBuffer buf) {
        // Bitmap capability set (28 bytes), see
        // http://msdn.microsoft.com/en-us/library/cc240554.aspx

        numberCapabilities++;

        // Capability set type: CAPSTYPE_BITMAP (2) (LE)
        buf.writeShortLE(CAPSTYPE_BITMAP);

        // Length of capability set: 28 bytes (LE)
        buf.writeShortLE(28);

        // preferredBitsPerPixel: 16 bpp (LE)
        buf.writeShortLE(prefferedBitsPerPixel);

        // receive1BitPerPixel (ignored and SHOULD be set to TRUE (0x1)): TRUE (0x1) (LE)
        buf.writeShortLE(1);

        // receive4BitsPerPixel (ignored and SHOULD be set to TRUE (0x1)): TRUE (0x1) (LE)
        buf.writeShortLE(1);

        // receive8BitsPerPixel (ignored and SHOULD be set to TRUE (0x1)): TRUE (0x1) (LE)
        buf.writeShortLE(1);

        // Desktop width and height (LE)
        buf.writeShortLE(screen.getFramebufferWidth());
        buf.writeShortLE(screen.getFramebufferHeight());

        // Padding 2 bytes
        buf.writeShortLE(0);

        // desktopResizeFlag (LE)
        buf.writeShortLE((desktopResize) ? 1 : 0);

        buf.writeBytes(new byte[] {(byte)0x01, (byte)0x00, // bitmapCompressionFlag (must be set to TRUE
                                                           // (0x1)): TRUE (0x1) (LE)
            (byte)0x00, // highColorFlags (field is ignored and SHOULD be set to
                        // zero): 0
            (byte)0x01, // drawingFlags: 0x1 TODO: padding, why 0x1 ???
            (byte)0x01, (byte)0x00, // multipleRectangleSupport: TRUE (LE)
            (byte)0x00, (byte)0x00, // Padding 2 bytes

        });
    }

    private void writeOrderCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Order Capability Set (88 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240556.aspx
            (byte)0x03,
            (byte)0x00, // capability set type: CAPSTYPE_ORDER (3) (LE)
            (byte)0x58,
            (byte)0x00, // length of capability set: 88 bytes (LE)
            // terminalDescriptor = "" (16 bytes, UCS2)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // pad4octetsA
            (byte)0x01, (byte)0x00, // desktopSaveXGranularity (ignored): 1 (LE)
            (byte)0x14, (byte)0x00, // desktopSaveYGranularity (ignored): 20 (LE)
            (byte)0x00, (byte)0x00, // pad2octetsA (ignored)
            (byte)0x01, (byte)0x00, // maximumOrderLevel: ORD_LEVEL_1_ORDERS (1)
            (byte)0x00, (byte)0x00, // number of fonts (ignored): 0
            (byte)0x4a, (byte)0x00, // orderFlags = 0x004a (LE),
                                    // SOLIDPATTERNBRUSHONLY (0x40),
                                    // ZEROBOUNDSDELTASSUPPORT (0x8, MUST),
                                    // NEGOTIATEORDERSUPPORT (0x2, MUST)
            // Order support: 32 bytes (no primary drawing orders are supported, so
            // this array MUST be initialized to all zeros, use 0x01 for TRUE).
            (byte)0x00, // TS_NEG_DSTBLT_INDEX: FALSE
            (byte)0x00, // TS_NEG_PATBLT_INDEX: FALSE
            (byte)0x00, // TS_NEG_SCRBLT_INDEX: FALSE
            (byte)0x00, // TS_NEG_MEMBLT_INDEX: FALSE
            (byte)0x00, // TS_NEG_MEM3BLT_INDEX: FALSE
            (byte)0x00, // TS_NEG_ATEXTOUT_INDEX: FALSE
            (byte)0x00, // TS_NEG_AEXTTEXTOUT_INDEX: FALSE
            (byte)0x00, // TS_NEG_DRAWNINEGRID_INDEX: FALSE
            (byte)0x00, // TS_NEG_LINETO_INDEX: FALSE
            (byte)0x00, // TS_NEG_MULTI_DRAWNINEGRID_INDEX: FALSE
            (byte)0x00, // TS_NEG_OPAQUERECT_INDEX: FALSE
            (byte)0x00, // TS_NEG_SAVEBITMAP_INDEX: FALSE
            (byte)0x00, // TS_NEG_WTEXTOUT_INDEX: FALSE
            (byte)0x00, // TS_NEG_MEMBLT_R2_INDEX: FALSE
            (byte)0x00, // TS_NEG_MEM3BLT_R2_INDEX: FALSE
            (byte)0x00, // TS_NEG_MULTIDSTBLT_INDEX: FALSE
            (byte)0x00, // TS_NEG_MULTIPATBLT_INDEX: FALSE
            (byte)0x00, // TS_NEG_MULTISCRBLT_INDEX: FALSE
            (byte)0x00, // TS_NEG_MULTIOPAQUERECT_INDEX: FALSE
            (byte)0x00, // TS_NEG_FAST_INDEX_INDEX: FALSE
            (byte)0x00, // TS_NEG_POLYGON_SC_INDEX: FALSE
            (byte)0x00, // TS_NEG_POLYGON_CB_INDEX: FALSE
            (byte)0x00, // TS_NEG_POLYLINE_INDEX: TRUE
            (byte)0x00, // Unused: 0
            (byte)0x00, // TS_NEG_FAST_GLYPH_INDEX: FALSE
            (byte)0x00, // TS_NEG_ELLIPSE_SC_INDEX: FALSE
            (byte)0x00, // TS_NEG_ELLIPSE_CB_INDEX: FALSE
            (byte)0x00, // TS_NEG_INDEX_INDEX: FALSE
            (byte)0x00, // TS_NEG_WEXTTEXTOUT_INDEX: FALSE
            (byte)0x00, // TS_NEG_WLONGTEXTOUT_INDEX: FALSE
            (byte)0x00, // TS_NEG_WLONGEXTTEXTOUT_INDEX: FALSE
            (byte)0x00, // Unused: 0
            (byte)0x00, (byte)0x00, // Text flags (ignored): 0 (LE)
            (byte)0x00, (byte)0x00, // Order support extra flags: 0 (LE)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Padding 4 bytes
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Desktop save size
                                                            // (ignored): 0
                                                            // (assumed to be
                                                            // 230400 bytes
                                                            // (480*480,
                                                            // 0x38400, LE))
            (byte)0x00, (byte)0x00, // Padding 2 bytes
            (byte)0x00, (byte)0x00, // Padding 2 bytes
            (byte)0xe4, (byte)0x04, // Text ANSI Code Page: 1252, ANSI - Latin I
                                    // (0x04e4, LE)
            (byte)0x00, (byte)0x00, // Padding 2 bytes

        });
    }

    private void writeSoundCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Sound Capability Set (8 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240552.aspx
            (byte)0x0c, (byte)0x00, // capability set type: CAPSTYPE_SOUND (12,
                                    // LE)
            (byte)0x08, (byte)0x00, // length of capability set: 8 bytes (LE)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // soundFlags:
                                                            // 0x0000 (LE) //
                                                            // SOUND_FLAG_BEEPS
                                                            // (0x1)

        });
    }

    private void writeFontCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Font Capability Set (8 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240571.aspx
            (byte)0x0e, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,

        });
    }

    private void writeOffscreenBitmapCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Offscreen Bitmap Cache Capability Set (12 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240550.aspx
            (byte)0x11, (byte)0x00, // capability set type:
                                    // CAPSTYPE_OFFSCREENCACHE (17, LE)
            (byte)0x0c, (byte)0x00, // length of capability set: 12 bytes (LE)
            (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // offscreenSupportLevel:
                                                            // FALSE (LE)
            (byte)0x00, (byte)0x00, // offscreenCacheSize: 0 (LE)
            (byte)0x00, (byte)0x00, // offscreenCacheEntries: 0 (LE)

        });
    }

    private void writeGlyphCacheCS(ByteBuffer buf) {
        numberCapabilities++;
        buf.writeBytes(new byte[] {
            //
            // Glyph Cache Capability Set (52 bytes), see
            // http://msdn.microsoft.com/en-us/library/cc240565.aspx
            (byte)0x10, (byte)0x00, // capability set type:
                                    // CAPSTYPE_OFFSCREENCACHE (16, LE)
            (byte)0x34, (byte)0x00, // length of capability set: 52 bytes (LE)
            // Glyph Cache (40 bytes)
            (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
            (byte)0x04, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
            (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
            (byte)0x04, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
            (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
            (byte)0x08, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
            (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
            (byte)0x08, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
            (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
            (byte)0x10, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
            (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
            (byte)0x20, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
            (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
            (byte)0x40, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
            (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
            (byte)0x80, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
            (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
            (byte)0x00, (byte)0x01, // CacheMaximumCellSize: 4 (LE)
            (byte)0x40, (byte)0x00, // CacheEntries: 64 (LE)
            (byte)0x00, (byte)0x08, // CacheMaximumCellSize: 2048 (LE)
            // FragCache
            (byte)0x00, (byte)0x01, // CacheEntries: 256 (LE)
            (byte)0x00, (byte)0x01, // CacheMaximumCellSize: 256 (LE)
            //
            (byte)0x00, (byte)0x00, // GlyphSupportLevel: GLYPH_SUPPORT_NONE (0x0,
                                    // LE)
            (byte)0x00, (byte)0x00, // Padding 2 bytes
        });
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        /* @formatter:off */
    byte[] packet = new byte[] {
        // MCS Send Data Request
        (byte)0x64,

        // Initiator: 1004 (1001+3)
        (byte)0x00, (byte)0x03,

        // Channel ID: 1003 (I/O channel)
        (byte)0x03, (byte)0xeb,

        // Data priority: high (0x40), segmentation: begin (0x20) | end (0x10)
        (byte)0x70,

        // User data length: 432 bytes (0x1b0, variable length field)
        (byte)0x81, (byte)0xb0,

        // Total length: 432 bytes (0x1b0, LE)
        (byte)0xb0, (byte)0x01,

        // PDU type: Confirm Active PDU (0x3), TS_PROTOCOL_VERSION (0x10) (LE)
        (byte)0x13, (byte)0x00,

        // PDU source: 1004 (LE)
        (byte)0xec, (byte)0x03,

        // Share ID: 0x000103ea (LE)
        (byte)0xea, (byte)0x03, (byte)0x01, (byte)0x00,

        // Originator ID: 1002 (LE)
        (byte)0xea, (byte)0x03,

        // Length of source descriptor: 6 bytes (including NULL character) (LE)
        (byte)0x06, (byte)0x00,

        // Length of combined capabilities: 410 bytes (LE)
        (byte)0x9a, (byte)0x01,

        // Source descriptor: "MSTSC" ???
        (byte)0x4d, (byte)0x53, (byte)0x54, (byte)0x53, (byte)0x43, (byte)0x00,

        // Number of capabilities: 15 (LE)
        (byte)0x0f, (byte)0x00,

        // Padding 2 bytes
        (byte)0x00, (byte)0x00,

        // Capabilities, see http://msdn.microsoft.com/en-us/library/cc240486.aspx

        //
        // General capability set (24 bytes), see http://msdn.microsoft.com/en-us/library/cc240549.aspx
        (byte)0x01, (byte)0x00, // capability set type: CAPSTYPE_GENERAL (1) (LE)
        (byte)0x18, (byte)0x00, // length of capability set: 24 bytes (LE)
        (byte)0x01, (byte)0x00, // TS_OSMAJORTYPE_WINDOWS (1) (LE)
        (byte)0x03, (byte)0x00, // TS_OSMINORTYPE_WINDOWS_NT (3) (LE)
        (byte)0x00, (byte)0x02, // TS_CAPS_PROTOCOLVERSION (0x0200) (LE)
        (byte)0x00, (byte)0x00, // Padding 2 bytes
        (byte)0x00, (byte)0x00, // generalCompressionTypes: 0 (LE)

        // Extra flags: 0x040d (LE)
//        FastPathOutput:             (...............1) Advertiser supports fast-path output
//        ShadowCompression:          (..............0.) Advertiser NOT supports shadow compression
//        LongLengthCredentials:      (.............1..) Advertiser supports long-length credentials for the user name, password, or domain name
//        SessionAutoreconnection:    (............1...) Advertiser supports session auto-reconnection
//        ImprovedEncryptionChecksum: (...........0....) Client and server NOT support improved encryption checksum
//        Reserved1:                  (......00000.....)
//        CompressedBitMapDataFlag:   (.....1..........) No 8-UINT8 header is present for compressed bitmap data
//        Reserved2:                  (00000...........)
        (byte)0x0d, (byte)0x04,

        (byte)0x00, (byte)0x00, // updateCapabilityFlag: 0 (LE)
        (byte)0x00, (byte)0x00, // remoteUnshareFlag: 0 (LE)
        (byte)0x00, (byte)0x00, // generalCompressionLevel: 0 (LE)
        (byte)0x00, // refreshRectSupport: FALSE (0)
        (byte)0x00, // suppressOutputSupport: FALSE (0)

        //
        // Bitmap capability set (28 bytes), see http://msdn.microsoft.com/en-us/library/cc240554.aspx
        (byte)0x02, (byte)0x00, // capability set type: CAPSTYPE_BITMAP (2) (LE)
        (byte)0x1c, (byte)0x00, // length of capability set: 28 bytes (LE)
        (byte)0x10, (byte)0x00, // preferredBitsPerPixel: 16 bpp (LE)
        (byte)0x01, (byte)0x00, // receive1BitPerPixel (ignored and SHOULD be set to TRUE (0x1)): TRUE (0x1) (LE)
        (byte)0x01, (byte)0x00, // receive4BitsPerPixel (ignored and SHOULD be set to TRUE (0x1)): TRUE (0x1) (LE)
        (byte)0x01, (byte)0x00, // receive8BitsPerPixel (ignored and SHOULD be set to TRUE (0x1)): TRUE (0x1) (LE)
        (byte)0x00, (byte)0x04, // desktopWidth = 1024 pixels (LE)
        (byte)0x00, (byte)0x03, // desktopHeight = 768 pixels (LE)
        (byte)0x00, (byte)0x00, // Padding 2 bytes
        (byte)0x00, (byte)0x00, // desktopResizeFlag: FALSE (0x0) (LE)
        (byte)0x01, (byte)0x00, // bitmapCompressionFlag (must be set to TRUE (0x1)): TRUE (0x1) (LE)
        (byte)0x00, // highColorFlags (field is ignored and SHOULD be set to zero): 0
        (byte)0x01, // drawingFlags: 0x1 TODO: padding, why 0x1 ???
        (byte)0x01, (byte)0x00, // multipleRectangleSupport: TRUE (LE)
        (byte)0x00, (byte)0x00, // Padding 2 bytes

        //
        // Order Capability Set (88 bytes), see http://msdn.microsoft.com/en-us/library/cc240556.aspx
        (byte)0x03, (byte)0x00, // capability set type: CAPSTYPE_ORDER (3) (LE)
        (byte)0x58, (byte)0x00, // length of capability set: 88 bytes (LE)
        // terminalDescriptor = "" (16 bytes, UCS2)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // pad4octetsA
        (byte)0x01, (byte)0x00, // desktopSaveXGranularity (ignored): 1 (LE)
        (byte)0x14, (byte)0x00, // desktopSaveYGranularity (ignored): 20 (LE)
        (byte)0x00, (byte)0x00, // pad2octetsA (ignored)
        (byte)0x01, (byte)0x00, // maximumOrderLevel: ORD_LEVEL_1_ORDERS (1)
        (byte)0x00, (byte)0x00, // number of fonts (ignored): 0
        (byte)0x4a, (byte)0x00, // orderFlags = 0x004a (LE), SOLIDPATTERNBRUSHONLY (0x40), ZEROBOUNDSDELTASSUPPORT (0x8, MUST), NEGOTIATEORDERSUPPORT (0x2, MUST)
        // Order support: 32 bytes (no primary drawing orders are supported, so this array MUST be initialized to all zeros, use 0x01 for TRUE).
        (byte)0x00, // TS_NEG_DSTBLT_INDEX: FALSE
        (byte)0x00, // TS_NEG_PATBLT_INDEX: FALSE
        (byte)0x00, // TS_NEG_SCRBLT_INDEX: FALSE
        (byte)0x00, // TS_NEG_MEMBLT_INDEX: FALSE
        (byte)0x00, // TS_NEG_MEM3BLT_INDEX: FALSE
        (byte)0x00, // TS_NEG_ATEXTOUT_INDEX: FALSE
        (byte)0x00, // TS_NEG_AEXTTEXTOUT_INDEX: FALSE
        (byte)0x00, // TS_NEG_DRAWNINEGRID_INDEX: FALSE
        (byte)0x00, // TS_NEG_LINETO_INDEX: FALSE
        (byte)0x00, // TS_NEG_MULTI_DRAWNINEGRID_INDEX: FALSE
        (byte)0x00, // TS_NEG_OPAQUERECT_INDEX: FALSE
        (byte)0x00, // TS_NEG_SAVEBITMAP_INDEX: FALSE
        (byte)0x00, // TS_NEG_WTEXTOUT_INDEX: FALSE
        (byte)0x00, // TS_NEG_MEMBLT_R2_INDEX: FALSE
        (byte)0x00, // TS_NEG_MEM3BLT_R2_INDEX: FALSE
        (byte)0x00, // TS_NEG_MULTIDSTBLT_INDEX: FALSE
        (byte)0x00, // TS_NEG_MULTIPATBLT_INDEX: FALSE
        (byte)0x00, // TS_NEG_MULTISCRBLT_INDEX: FALSE
        (byte)0x00, // TS_NEG_MULTIOPAQUERECT_INDEX: FALSE
        (byte)0x00, // TS_NEG_FAST_INDEX_INDEX: FALSE
        (byte)0x00, // TS_NEG_POLYGON_SC_INDEX: FALSE
        (byte)0x00, // TS_NEG_POLYGON_CB_INDEX: FALSE
        (byte)0x00, // TS_NEG_POLYLINE_INDEX: TRUE
        (byte)0x00, // Unused: 0
        (byte)0x00, // TS_NEG_FAST_GLYPH_INDEX: FALSE
        (byte)0x00, // TS_NEG_ELLIPSE_SC_INDEX: FALSE
        (byte)0x00, // TS_NEG_ELLIPSE_CB_INDEX: FALSE
        (byte)0x00, // TS_NEG_INDEX_INDEX: FALSE
        (byte)0x00, // TS_NEG_WEXTTEXTOUT_INDEX: FALSE
        (byte)0x00, // TS_NEG_WLONGTEXTOUT_INDEX: FALSE
        (byte)0x00, // TS_NEG_WLONGEXTTEXTOUT_INDEX: FALSE
        (byte)0x00, // Unused: 0
        (byte)0x00, (byte)0x00, // Text flags (ignored): 0  (LE)
        (byte)0x00, (byte)0x00, // Order support extra flags: 0 (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Padding 4 bytes
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Desktop save size (ignored): 0 (assumed to be 230400 bytes (480*480, 0x38400, LE))
        (byte)0x00, (byte)0x00, // Padding 2 bytes
        (byte)0x00, (byte)0x00, // Padding 2 bytes
        (byte)0xe4, (byte)0x04, // Text ANSI Code Page: 1252,  ANSI - Latin I (0x04e4, LE)
        (byte)0x00, (byte)0x00, // Padding 2 bytes

        //
        // Bitmap Cache Rev. 2 Capability Set (40 bytes), see http://msdn.microsoft.com/en-us/library/cc240560.aspx
        (byte)0x13, (byte)0x00, // capability set type: CAPSTYPE_BITMAPCACHE_REV2 (19) (LE)
        (byte)0x28, (byte)0x00, // length of capability set: 40 bytes (LE)
        (byte)0x00, (byte)0x00, // Cache flags: 0 (LE)
        (byte)0x00, // Padding 1 byte
        (byte)0x00, // Number of cell caches: 0
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache0 cell info: 0 (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache1 cell info: 0 (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache2 cell info: 0 (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache3 cell info: 0 (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Bitmap cache4 cell info: 0 (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Padding 12 bytes
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Padding
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Padding

        //
        // Color Table Cache Capability Set (8 bytes), see http://msdn.microsoft.com/en-us/library/cc241564.aspx
        (byte)0x0a, (byte)0x00, // capability set type: CAPSTYPE_COLORCACHE (10) (LE)
        (byte)0x08, (byte)0x00, // length of capability set: 8 bytes (LE)
        (byte)0x06, (byte)0x00, // Color table cache size (must be ignored during capability exchange and is assumed to be 0x0006): 6 (LE)
        (byte)0x00, (byte)0x00, // Padding 2 bytes

        //
        // Window Activation Capability Set (12 bytes), see http://msdn.microsoft.com/en-us/library/cc240569.aspx
        (byte)0x07, (byte)0x00, // capability set type: CAPSTYPE_ACTIVATION (7) (LE)
        (byte)0x0c, (byte)0x00, // length of capability set: 12 bytes (LE)
        (byte)0x00, (byte)0x00, // helpKeyFlag (should be set to FALSE (0)): FALSE (0, LE)
        (byte)0x00, (byte)0x00, // helpKeyIndexFlag (should be set to FALSE (0)): FALSE (0, LE)
        (byte)0x00, (byte)0x00, // helpExtendedKeyFlag (should be set to FALSE (0)): FALSE (0, LE)
        (byte)0x00, (byte)0x00, // windowManagerKeyFlag (should be set to FALSE (0)): FALSE (0, LE)

        //
        // Control Capability Set (12 bytes), see http://msdn.microsoft.com/en-us/library/cc240568.aspx
        (byte)0x05, (byte)0x00, // capability set type: CAPSTYPE_ACTIVATION (7)
        (byte)0x0c, (byte)0x00, // length of capability set: 12 bytes (LE)
        (byte)0x00, (byte)0x00, // controlFlags (should be set to 0): 0 (LE)
        (byte)0x00, (byte)0x00, // remoteDetachFlag (should be set to 0): 0 (LE)
        (byte)0x02, (byte)0x00, // controlInterest (should be set to CONTROLPRIORITY_NEVER): CONTROLPRIORITY_NEVER (2) (LE)
        (byte)0x02, (byte)0x00, // detachInterest (should be set to CONTROLPRIORITY_NEVER): CONTROLPRIORITY_NEVER (2) (LE)

        //
        // Pointer Capability Set (10 bytes), see http://msdn.microsoft.com/en-us/library/cc240562.aspx
        (byte)0x08, (byte)0x00, // capability set type: CAPSTYPE_POINTER (8, LE)
        (byte)0x0a, (byte)0x00, // length of capability set: 10 bytes (LE)
        (byte)0x00, (byte)0x00, // colorPointerFlag: FALSE (LE)
        (byte)0x00, (byte)0x00, // colorPointerCacheSize: 0 (LE)
        (byte)0x14, (byte)0x00, // pointerCacheSize: 20 (LE)

        //
        // Share Capability Set (8 bytes), see http://msdn.microsoft.com/en-us/library/cc240570.aspx
        (byte)0x09, (byte)0x00, // capability set type: CAPSTYPE_SHARE (9, LE)
        (byte)0x08, (byte)0x00, // length of capability set: 8 bytes (LE)
        (byte)0x00, (byte)0x00, // nodeID (must be set to 0 by client): 0 (LE)
        (byte)0x00, (byte)0x00, // Padding 2 bytes (LE)

        //
        // Input Capability Set (88 bytes), see http://msdn.microsoft.com/en-us/library/cc240563.aspx
        (byte)0x0d, (byte)0x00, // capability set type:  CAPSTYPE_INPUT (13, LE)
        (byte)0x58, (byte)0x00, // length of capability set: 88 bytes (LE)
        (byte)0x35, (byte)0x00, // inputFlags: 0x0035  (LE),  INPUT_FLAG_FASTPATH_INPUT2 (0x20), INPUT_FLAG_VKPACKET (0x10), INPUT_FLAG_MOUSEX (0x4), INPUT_FLAG_SCANCODES (0x1)
        (byte)0x00, (byte)0x00, // Padding 2 bytes
        (byte)0x09, (byte)0x04, (byte)0x00, (byte)0x00, // keyboardLayout: "US" keyboard layout (0x000409, LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // keyboardType: unknown (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // keyboardSubType: unknown (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // keyboardFunctionKey: unknown (LE)
        // imeFileName: "", (64 bytes, including trailing NULL characters, UCS2)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,

        //
        // Brush Capability Set (8 bytes), see http://msdn.microsoft.com/en-us/library/cc240564.aspx
        (byte)0x0f, (byte)0x00, // capability set type: CAPSTYPE_BRUSH (15, LE)
        (byte)0x08, (byte)0x00, // length of capability set: 8 bytes (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // brushSupportLevel: BRUSH_DEFAULT (0x0, LE)

        //
        // Sound Capability Set (8 bytes), see http://msdn.microsoft.com/en-us/library/cc240552.aspx
        (byte)0x0c, (byte)0x00, // capability set type: CAPSTYPE_SOUND (12, LE)
        (byte)0x08, (byte)0x00, // length of capability set: 8 bytes (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // soundFlags: 0x0000 (LE) // SOUND_FLAG_BEEPS (0x1)

        //
        // Font Capability Set (8 bytes), see http://msdn.microsoft.com/en-us/library/cc240571.aspx
        (byte)0x0e, (byte)0x00,
        (byte)0x08, (byte)0x00,
        (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,

        //
        // Offscreen Bitmap Cache Capability Set (12 bytes), see http://msdn.microsoft.com/en-us/library/cc240550.aspx
        (byte)0x11, (byte)0x00, // capability set type: CAPSTYPE_OFFSCREENCACHE (17, LE)
        (byte)0x0c, (byte)0x00, // length of capability set: 12 bytes (LE)
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // offscreenSupportLevel: FALSE (LE)
        (byte)0x00, (byte)0x00, // offscreenCacheSize: 0 (LE)
        (byte)0x00, (byte)0x00, // offscreenCacheEntries: 0 (LE)

        //
        // Glyph Cache Capability Set (52 bytes), see http://msdn.microsoft.com/en-us/library/cc240565.aspx
        (byte)0x10, (byte)0x00, // capability set type: CAPSTYPE_OFFSCREENCACHE (16, LE)
        (byte)0x34, (byte)0x00, // length of capability set: 52 bytes (LE)
        // Glyph Cache (40 bytes)
        (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
        (byte)0x04, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
        (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
        (byte)0x04, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
        (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
        (byte)0x08, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
        (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
        (byte)0x08, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
        (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
        (byte)0x10, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
        (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
        (byte)0x20, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
        (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
        (byte)0x40, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
        (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
        (byte)0x80, (byte)0x00, // CacheMaximumCellSize: 4 (LE)
        (byte)0xfe, (byte)0x00, // CacheEntries: 254 (LE)
        (byte)0x00, (byte)0x01, // CacheMaximumCellSize: 4 (LE)
        (byte)0x40, (byte)0x00, // CacheEntries: 64 (LE)
        (byte)0x00, (byte)0x08, // CacheMaximumCellSize: 2048 (LE)
        // FragCache
        (byte)0x00, (byte)0x01, // CacheEntries: 256 (LE)
        (byte)0x00, (byte)0x01, // CacheMaximumCellSize: 256 (LE)
        //
        (byte)0x00, (byte)0x00, // GlyphSupportLevel: GLYPH_SUPPORT_NONE (0x0, LE)
        (byte)0x00, (byte)0x00, // Padding 2 bytes
    };
    /* @formatter:on */

        RdpState rdpState = new RdpState();
        ScreenDescription screenDescription = new ScreenDescription();
        screenDescription.setFramebufferSize(1024, 768);

        rdpState.serverShareId = 0x000103ea;

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {}));
        Element confirm_active = new ClientConfirmActivePDU("confirm_active", screenDescription, rdpState);
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(packet));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, confirm_active, sink);
        pipeline.link("source", "confirm_active", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
