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
import streamer.Order;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.FakeSink;
import streamer.debug.MockSource;
import common.ScreenDescription;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240669.aspx
 * @see http://msdn.microsoft.com/en-us/library/cc240484.aspx
 */
public class ServerDemandActivePDU extends BaseElement {

    /**
     * Demand Active PDU.
     */
    public static final int PDUTYPE_DEMANDACTIVEPDU = 0x1;

    protected RdpState state;
    protected ScreenDescription screen;

    public ServerDemandActivePDU(String id, ScreenDescription screen, RdpState state) {
        super(id);
        this.state = state;
        this.screen = screen;
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Total length of packet
        int length = buf.readSignedShortLE(); // Ignore
        if (buf.length != length)
            throw new RuntimeException("Incorrect length of packet. Length: " + length + ", data: " + buf + ".");

        int type = buf.readSignedShortLE() & 0xf;
        if (type != PDUTYPE_DEMANDACTIVEPDU)
            throw new RuntimeException("Unknown PDU type. Expected type: Demand Active PDU (0x1), actual tyoe: " + type + ", data: " + buf + ".");

        // TS_SHARECONTROLHEADER::pduSource = 0x03ea (1002)
        int pduSource = buf.readSignedShortLE();
        if (pduSource != 1002)
            throw new RuntimeException("Unexpected source of demand active PDU. Expected source: 1002, actual source: " + pduSource + ".");

        // (4 bytes): A 32-bit, unsigned integer. The share identifier for the
        // packet (see [T128] section 8.4.2 for more information regarding share
        // IDs).
        long shareId = buf.readUnsignedIntLE();
        state.serverShareId = shareId;

        // Ignore rest of server data because it is not used by this client.
        // (2 bytes): A 16-bit, unsigned integer. The size in bytes of the
        // sourceDescriptor field.
        int lengthSourceDescriptor = buf.readUnsignedShortLE();

        // (2 bytes): A 16-bit, unsigned integer. The combined size in bytes of the
        // numberCapabilities, pad2Octets, and capabilitySets fields.
        int lengthCombinedCapabilities = buf.readUnsignedShortLE();

        // (variable): A variable-length array of bytes containing a source
        // descriptor,
        // ByteBuffer sourceDescriptor = buf.readBytes(lengthSourceDescriptor);
        buf.skipBytes(lengthSourceDescriptor);

        // (variable): An array of Capability Set (section 2.2.1.13.1.1.1)
        // structures. The number of capability sets is specified by the
        // numberCapabilities field.
        handleCapabiltySets(buf.readBytes(lengthCombinedCapabilities));

        // (4 bytes): A 32-bit, unsigned integer. The session identifier. This field
        // is ignored by the client.
        buf.skipBytes(4);

        /* DEBUG */buf.assertThatBufferIsFullyRead();

        buf.unref();

        sendHandshakePackets();
    }

    /**
     * General Capability Set
     */
    public static final int CAPSTYPE_GENERAL = 0x0001;
    /**
     * Bitmap Capability Set
     */
    public static final int CAPSTYPE_BITMAP = 0x0002;
    /**
     * Order Capability Set
     */
    public static final int CAPSTYPE_ORDER = 0x0003;
    /**
     * Revision 1 Bitmap Cache Capability Set
     */
    public static final int CAPSTYPE_BITMAPCACHE = 0x0004;
    /**
     * Control Capability Set
     */
    public static final int CAPSTYPE_CONTROL = 0x0005;
    /**
     * Window Activation Capability Set
     */
    public static final int CAPSTYPE_ACTIVATION = 0x0007;
    /**
     * Pointer Capability Set
     */
    public static final int CAPSTYPE_POINTER = 0x0008;
    /**
     * Share Capability Set
     */
    public static final int CAPSTYPE_SHARE = 0x0009;
    /**
     * Color Table Cache Capability Set
     */
    public static final int CAPSTYPE_COLORCACHE = 0x000A;
    /**
     * Sound Capability Set
     */
    public static final int CAPSTYPE_SOUND = 0x000C;
    /**
     * Input Capability Set
     */
    public static final int CAPSTYPE_INPUT = 0x000D;
    /**
     * Font Capability Set
     */
    public static final int CAPSTYPE_FONT = 0x000E;
    /**
     * Brush Capability Set
     */
    public static final int CAPSTYPE_BRUSH = 0x000F;
    /**
     * Glyph Cache Capability Set
     */
    public static final int CAPSTYPE_GLYPHCACHE = 0x0010;
    /**
     * Offscreen Bitmap Cache Capability Set
     */
    public static final int CAPSTYPE_OFFSCREENCACHE = 0x0011;
    /**
     * Bitmap Cache Host Support Capability Set
     */
    public static final int CAPSTYPE_BITMAPCACHE_HOSTSUPPORT = 0x0012;
    /**
     * Revision 2 Bitmap Cache Capability Set
     */
    public static final int CAPSTYPE_BITMAPCACHE_REV2 = 0x0013;
    /**
     * Virtual Channel Capability Set
     */
    public static final int CAPSTYPE_VIRTUALCHANNEL = 0x0014;
    /**
     * DrawNineGrid Cache Capability Set
     */
    public static final int CAPSTYPE_DRAWNINEGRIDCACHE = 0x0015;
    /**
     * Draw GDI+ Cache Capability Set
     */
    public static final int CAPSTYPE_DRAWGDIPLUS = 0x0016;
    /**
     * Remote Programs Capability Set
     */
    public static final int CAPSTYPE_RAIL = 0x0017;
    /**
     * Window List Capability Set
     */
    public static final int CAPSTYPE_WINDOW = 0x0018;
    /**
     * Desktop Composition Extension Capability Set
     */
    public static final int CAPSETTYPE_COMPDESK = 0x0019;
    /**
     * Multifragment Update Capability Set
     */
    public static final int CAPSETTYPE_MULTIFRAGMENTUPDATE = 0x001A;
    /**
     * Large Pointer Capability Set
     */
    public static final int CAPSETTYPE_LARGE_POINTER = 0x001B;
    /**
     * Surface Commands Capability Set
     */
    public static final int CAPSETTYPE_SURFACE_COMMANDS = 0x001C;
    /**
     * Bitmap Codecs Capability Set
     */
    public static final int CAPSETTYPE_BITMAP_CODECS = 0x001D;
    /**
     * Frame Acknowledge Capability Set
     */
    public static final int CAPSSETTYPE_FRAME_ACKNOWLEDGE = 0x001E;

    /**
     * @see http://msdn.microsoft.com/en-us/library/cc240486.aspx
     */
    protected void handleCapabiltySets(ByteBuffer buf) {
        // (2 bytes): A 16-bit, unsigned integer. The number of capability sets
        // included in the Demand Active PDU.
        int numberCapabilities = buf.readSignedShortLE();

        // (2 bytes): Padding.
        buf.skipBytes(2);

        for (int i = 0; i < numberCapabilities; i++) {
            // (2 bytes): A 16-bit, unsigned integer. The type identifier of the
            // capability set.
            int capabilitySetType = buf.readUnsignedShortLE();

            // (2 bytes): A 16-bit, unsigned integer. The length in bytes of the
            // capability data, including the size of the capabilitySetType and
            // lengthCapability fields.
            int lengthCapability = buf.readUnsignedShortLE();

            // (variable): Capability set data which conforms to the structure of the
            // type given by the capabilitySetType field.
            ByteBuffer capabilityData = buf.readBytes(lengthCapability - 4);

            switch (capabilitySetType) {
            case CAPSTYPE_GENERAL:
                break;
            case CAPSTYPE_BITMAP:
                handleBitmapCapabilities(capabilityData);
                break;
            case CAPSTYPE_ORDER:
                break;
            case CAPSTYPE_BITMAPCACHE:
                break;
            case CAPSTYPE_CONTROL:
                break;
            case CAPSTYPE_ACTIVATION:
                break;
            case CAPSTYPE_POINTER:
                break;
            case CAPSTYPE_SHARE:
                break;
            case CAPSTYPE_COLORCACHE:
                break;
            case CAPSTYPE_SOUND:
                break;
            case CAPSTYPE_INPUT:
                break;
            case CAPSTYPE_FONT:
                break;
            case CAPSTYPE_BRUSH:
                break;
            case CAPSTYPE_GLYPHCACHE:
                break;
            case CAPSTYPE_OFFSCREENCACHE:
                break;
            case CAPSTYPE_BITMAPCACHE_HOSTSUPPORT:
                break;
            case CAPSTYPE_BITMAPCACHE_REV2:
                break;
            case CAPSTYPE_VIRTUALCHANNEL:
                break;
            case CAPSTYPE_DRAWNINEGRIDCACHE:
                break;
            case CAPSTYPE_DRAWGDIPLUS:
                break;
            case CAPSTYPE_RAIL:
                break;
            case CAPSTYPE_WINDOW:
                break;
            case CAPSETTYPE_COMPDESK:
                break;
            case CAPSETTYPE_MULTIFRAGMENTUPDATE:
                break;
            case CAPSETTYPE_LARGE_POINTER:
                break;
            case CAPSETTYPE_SURFACE_COMMANDS:
                break;
            case CAPSETTYPE_BITMAP_CODECS:
                break;
            case CAPSSETTYPE_FRAME_ACKNOWLEDGE:
                break;
            default:
                // Ignore
                break;
            }

            capabilityData.unref();
        }

        // TODO

        buf.unref();
    }

    /**
     * @see http://msdn.microsoft.com/en-us/library/cc240554.aspx
     */
    protected void handleBitmapCapabilities(ByteBuffer buf) {

        // (2 bytes): A 16-bit, unsigned integer. The server MUST set this field to
        // the color depth of the session, while the client SHOULD set this field to
        // the color depth requested in the Client Core Data (section 2.2.1.3.2).
        int preferredBitsPerPixel = buf.readUnsignedShortLE();
        screen.setPixelFormatRGBTrueColor(preferredBitsPerPixel);

        // receive1BitPerPixel (2 bytes): A 16-bit, unsigned integer. Indicates
        // whether the client can receive 1 bpp. This field is ignored and SHOULD be
        // set to TRUE (0x0001).
        buf.skipBytes(2);

        // receive4BitsPerPixel(2 bytes): A 16-bit, unsigned integer. Indicates
        // whether the client can receive 4 bpp. This field is ignored and SHOULD be
        // set to TRUE (0x0001).
        buf.skipBytes(2);

        // receive8BitsPerPixel (2 bytes): A 16-bit, unsigned integer. Indicates
        // whether the client can receive 8 bpp. This field is ignored and SHOULD be
        // set to TRUE (0x0001).
        buf.skipBytes(2);

        // (2 bytes): A 16-bit, unsigned integer. The width of the desktop in the
        // session.
        int desktopWidth = buf.readUnsignedShortLE();

        // (2 bytes): A 16-bit, unsigned integer. The height of the desktop in the
        // session.
        int desktopHeight = buf.readUnsignedShortLE();

        screen.setFramebufferSize(desktopWidth, desktopHeight);

        // pad2octets (2 bytes): A 16-bit, unsigned integer. Padding. Values in this
        // field MUST be ignored.

        // desktopResizeFlag (2 bytes): A 16-bit, unsigned integer. Indicates
        // whether resizing the desktop by using a Deactivation-Reactivation
        // Sequence is supported.

        // bitmapCompressionFlag (2 bytes): A 16-bit, unsigned integer. Indicates
        // whether bitmap compression is supported. This field MUST be set to TRUE
        // (0x0001) because support for compressed bitmaps is required for a
        // connection to proceed.

        // highColorFlags (1 byte): An 8-bit, unsigned integer. Client support for
        // 16 bpp color modes. This field is ignored and SHOULD be set to zero.

        // drawingFlags (1 byte): An 8-bit, unsigned integer. Flags describing
        // support for 32 bpp bitmaps.

        // multipleRectangleSupport (2 bytes): A 16-bit, unsigned integer. Indicates
        // whether the use of multiple bitmap rectangles is supported in the Bitmap
        // Update (section 2.2.9.1.1.3.1.2). This field MUST be set to TRUE (0x0001)
        // because multiple rectangle support is required for a connection to
        // proceed.

        // pad2octetsB (2 bytes): A 16-bit, unsigned integer. Padding. Values in
        // this field MUST be ignored.
    }

    /**
     * Send all client requests in one hop, to simplify logic.
     */
    protected void sendHandshakePackets() {
        // Send reactivation sequence in bulk
        pushDataToPad("confirm_active", new ByteBuffer((Order)null));
    }

    /**
     * Example.
     *
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        /* @formatter:off */
        byte[] packet = new byte[] {
                0x67, 0x01,  //  TS_SHARECONTROLHEADER::totalLength = 0x0167 = 359 bytes
                0x11, 0x00,  //  TS_SHARECONTROLHEADER::pduType = 0x0011 0x0011 = 0x0010 | 0x0001  = TS_PROTOCOL_VERSION | PDUTYPE_DEMANDACTIVEPDU

                (byte) 0xea, 0x03,  //  TS_SHARECONTROLHEADER::pduSource = 0x03ea (1002)

                (byte) 0xea, 0x03, 0x01, 0x00,  //  TS_DEMAND_ACTIVE_PDU::shareId
                0x04, 0x00,  //  TS_DEMAND_ACTIVE_PDU::lengthSourceDescriptor = 4 bytes
                0x51, 0x01,  //  TS_DEMAND_ACTIVE_PDU::lengthCombinedCapabilities = 0x151 = 337 bytes

                0x52, 0x44, 0x50, 0x00,  //  TS_DEMAND_ACTIVE_PDU::sourceDescriptor = "RDP"

                0x0d, 0x00,  //  TS_DEMAND_ACTIVE_PDU::numberCapabilities = 13
                0x00, 0x00,  //  TS_DEMAND_ACTIVE_PDU::pad2octets

                //  Share Capability Set (8 bytes)
                // 0x09, 0x00, 0x08, 0x00, (byte) 0xea, 0x03, (byte) 0xdc, (byte) 0xe2,
                //
                0x09, 0x00,  //  TS_SHARE_CAPABILITYSET::capabilitySetType = CAPSTYPE_SHARE (9)
                0x08, 0x00,  //  TS_SHARE_CAPABILITYSET::lengthCapability = 8 bytes
                (byte) 0xea, 0x03,  //  TS_SHARE_CAPABILITYSET::nodeID = 0x03ea (1002)
                (byte) 0xdc, (byte) 0xe2,  //  TS_SHARE_CAPABILITYSET::pad2octets

                //  General Capability Set (24 bytes)
                // 0x01, 0x00, 0x18, 0x00, 0x01, 0x00, 0x03, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x00, 0x1d, 0x04,
                // 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x01,
                //
                0x01, 0x00,  //  TS_GENERAL_CAPABILITYSET::capabilitySetType = CAPSTYPE_GENERAL (1)
                0x18, 0x00,  //  TS_GENERAL_CAPABILITYSET::lengthCapability = 24 bytes

                0x01, 0x00,  //  TS_GENERAL_CAPABILITYSET::osMajorType = TS_OSMAJORTYPE_WINDOWS (1)
                0x03, 0x00,  //  TS_GENERAL_CAPABILITYSET::osMinorType = TS_OSMINORTYPE_WINDOWS_NT (3)
                0x00, 0x02,  //  TS_GENERAL_CAPABILITYSET::protocolVersion = TS_CAPS_PROTOCOLVERSION (0x0200)
                0x00, 0x00,  //  TS_GENERAL_CAPABILITYSET::pad2octetsA
                0x00, 0x00,  //  TS_GENERAL_CAPABILITYSET::generalCompressionTypes = 0
                0x1d, 0x04,  //  TS_GENERAL_CAPABILITYSET::extraFlags = 0x041d = 0x0400 | 0x0010 | 0x0008 | 0x0004 | 0x0001 = NO_BITMAP_COMPRESSION_HDR | ENC_SALTED_CHECKSUM | AUTORECONNECT_SUPPORTED | LONG_CREDENTIALS_SUPPORTED | FASTPATH_OUTPUT_SUPPORTED

                0x00, 0x00,  //  TS_GENERAL_CAPABILITYSET::updateCapabilityFlag = 0
                0x00, 0x00,  //  TS_GENERAL_CAPABILITYSET::remoteUnshareFlag = 0
                0x00, 0x00,  //  TS_GENERAL_CAPABILITYSET::generalCompressionLevel = 0
                0x01,  //  TS_GENERAL_CAPABILITYSET::refreshRectSupport = TRUE
                0x01,  //  TS_GENERAL_CAPABILITYSET::suppressOutputSupport = TRUE

                // Virtual Channel Capability Set (8 bytes)
                // 0x14, 0x00, 0x08, 0x00, 0x02, 0x00, 0x00, 0x00,
                //
                0x14, 0x00,  //  TS_VIRTUALCHANNEL_CAPABILITYSET::capabilitySetType = CAPSTYPE_VIRTUALCHANNEL (20)
                0x08, 0x00,  //  TS_VIRTUALCHANNEL_CAPABILITYSET::lengthCapability = 8 bytes

                0x02, 0x00, 0x00, 0x00,  //  TS_VIRTUALCHANNEL_CAPABILITYSET::vccaps1 = 0x00000002 = VCCAPS_COMPR_CS_8K

                //  DrawGdiPlus Capability Set (40 bytes)
                // 0x16, 0x00, 0x28, 0x00, 0x00, 0x00, 0x00, 0x00, 0x70, (byte) 0xf6, 0x13, (byte) 0xf3, 0x01, 0x00, 0x00, 0x00,
                // 0x01, 0x00, 0x00, 0x00, 0x18, 0x00, 0x00, 0x00, (byte) 0x9c, (byte) 0xf6, 0x13, (byte) 0xf3, 0x61, (byte) 0xa6, (byte) 0x82, (byte) 0x80,
                // 0x00, 0x00, 0x00, 0x00, 0x00, 0x50, (byte) 0x91, (byte) 0xbf,
                //
                0x16, 0x00,  //  TS_DRAW_GDIPLUS_CAPABILITYSET::capabilitySetType = CAPSTYPE_DRAWGDIPLUS (22)
                0x28, 0x00,  //  TS_DRAW_GDIPLUS_CAPABILITYSET::lengthCapability = 40 bytes

                0x00, 0x00, 0x00, 0x00,  //  TS_DRAW_GDIPLUS_CAPABILITYSET::drawGdiplusSupportLevel = TS_DRAW_GDIPLUS_DEFAULT (0)
                0x70, (byte) 0xf6, 0x13, (byte) 0xf3,  //  TS_DRAW_GDIPLUS_CAPABILITYSET::GdipVersion (not initialized by server)
                0x01, 0x00, 0x00, 0x00,  //  TS_DRAW_GDIPLUS_CAPABILITYSET::drawGdiplusCacheLevel  = TS_DRAW_GDIPLUS_CACHE_LEVEL_ONE (1)

                0x01, 0x00,  //  TS_GDIPLUS_CACHE_ENTRIES::GdipGraphicsCacheEntries  (not initialized by server)
                0x00, 0x00,  //  TS_GDIPLUS_CACHE_ENTRIES::GdipObjectBrushCacheEntries (not initialized by server)
                0x18, 0x00,  //  TS_GDIPLUS_CACHE_ENTRIES::GdipObjectPenCacheEntries (not initialized by server)
                0x00, 0x00,  //  TS_GDIPLUS_CACHE_ENTRIES::GdipObjectImageCacheEntries (not initialized by server)
                (byte) 0x9c, (byte) 0xf6,  //  TS_GDIPLUS_CACHE_ENTRIES::GdipObjectImageAttributesCacheEntries (not initialized by server)

                0x13, (byte) 0xf3,  //  TS_GDIPLUS_CACHE_CHUNK_SIZE::GdipGraphicsCacheChunkSize  (not initialized by server)
                0x61, (byte) 0xa6,  //  TS_GDIPLUS_CACHE_CHUNK_SIZE::GdipObjectBrushCacheChunkSize (not initialized by server)
                (byte) 0x82, (byte) 0x80,  //  TS_GDIPLUS_CACHE_CHUNK_SIZE::GdipObjectPenCacheChunkSize (not initialized by server)
                0x00, 0x00,  //   TS_GDIPLUS_CACHE_CHUNK_SIZE::GdipObjectImageAttributesCacheChunkSize (not initialized by server)

                0x00, 0x00,  //  TS_GDIPLUS_IMAGE_CACHE_PROPERTIES::GdipObjectImageCacheChunkSize  (not initialized by server)
                0x00, 0x50,  //  TS_GDIPLUS_IMAGE_CACHE_PROPERTIES::GdipObjectImageCacheTotalSize  (not initialized by server)
                (byte) 0x91, (byte) 0xbf,  //  TS_GDIPLUS_IMAGE_CACHE_PROPERTIES::GdipObjectImageCacheMaxSize (not initialized by server)

                //  Font Capability Set (4 bytes)
                // 0x0e, 0x00, 0x04, 0x00,
                //
                // Due to a bug, the TS_FONT_CAPABILITYSET capability set size is incorrectly set to 4 bytes (it must be 8 bytes). As a result of this bug, the fontSupportFlags and pad2octets fields are missing.
                0x0e, 0x00,  //  TS_FONT_CAPABILITYSET::capabilitySetType = CAPSTYPE_FONT (14)
                0x04, 0x00,  //  TS_FONT_CAPABILITYSET::lengthCapability = 4 bytes


                //  Bitmap Capability Set (28 bytes)
                // 0x02, 0x00, 0x1c, 0x00, 0x18, 0x00, 0x01, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x04,
                // 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00,
                //
                0x02, 0x00,  //  TS_BITMAP_CAPABILITYSET::capabilitySetType = CAPSTYPE_BITMAP (2)
                0x1c, 0x00,  //  TS_BITMAP_CAPABILITYSET::lengthCapability = 28 bytes

                0x18, 0x00,  //  TS_BITMAP_CAPABILITYSET::preferredBitsPerPixel = 24 bpp
                0x01, 0x00,  //  TS_BITMAP_CAPABILITYSET::receive1BitPerPixel = TRUE
                0x01, 0x00,  //  TS_BITMAP_CAPABILITYSET::receive4BitsPerPixel = TRUE
                0x01, 0x00,  //  TS_BITMAP_CAPABILITYSET::receive8BitsPerPixel = TRUE
                0x00, 0x05,  //  TS_BITMAP_CAPABILITYSET::desktopWidth = 1280 pixels
                0x00, 0x04,  //  TS_BITMAP_CAPABILITYSET::desktopHeight = 1024 pixels
                0x00, 0x00,  //  TS_BITMAP_CAPABILITYSET::pad2octets
                0x01, 0x00,  //  TS_BITMAP_CAPABILITYSET::desktopResizeFlag = TRUE
                0x01, 0x00,  //  TS_BITMAP_CAPABILITYSET::bitmapCompressionFlag = TRUE
                0x00,  //  TS_BITMAP_CAPABILITYSET::highColorFlags = 0
                0x00,  //  TS_BITMAP_CAPABILITYSET::pad1octet
                0x01, 0x00,  //  TS_BITMAP_CAPABILITYSET::multipleRectangleSupport = TRUE
                0x00, 0x00,  //  TS_BITMAP_CAPABILITYSET::pad2octetsB

                //  Order Capability Set (88 bytes)
                // 0x03, 0x00, 0x58, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                // 0x00, 0x00, 0x00, 0x00, 0x40, 0x42, 0x0f, 0x00, 0x01, 0x00, 0x14, 0x00, 0x00, 0x00, 0x01, 0x00,
                // 0x00, 0x00, 0x22, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01,
                // 0x00, 0x00, 0x00, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x00, 0x01, 0x01, 0x01, 0x01,
                // 0x00, 0x00, 0x00, 0x00, (byte) 0xa1, 0x06, 0x00, 0x00, 0x40, 0x42, 0x0f, 0x00, 0x40, 0x42, 0x0f, 0x00,
                // 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                //
                0x03, 0x00,  //  TS_ORDER_CAPABILITYSET::capabilitySetType = CAPSTYPE_ORDER (3)
                0x58, 0x00,  //  TS_ORDER_CAPABILITYSET::lengthCapability = 88 bytes

                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,  // TS_ORDER_CAPABILITYSET::terminalDescriptor = ""
                0x40, 0x42, 0x0f, 0x00,  //  TS_ORDER_CAPABILITYSET::pad4octetsA

                0x01, 0x00,  //  TS_ORDER_CAPABILITYSET::desktopSaveXGranularity = 1
                0x14, 0x00,  //  TS_ORDER_CAPABILITYSET::desktopSaveYGranularity = 20
                0x00, 0x00,  //  TS_ORDER_CAPABILITYSET::pad2octetsA
                0x01, 0x00,  //  TS_ORDER_CAPABILITYSET::maximumOrderLevel = ORD_LEVEL_1_ORDERS (1)
                0x00, 0x00,  //  TS_ORDER_CAPABILITYSET::numberFonts = 0

                0x22, 0x00,  //  TS_ORDER_CAPABILITYSET::orderFlags = 0x0022 = 0x0020 | 0x0002 = COLORINDEXSUPPORT | NEGOTIATEORDERSUPPORT

                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_DSTBLT_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_PATBLT_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_SCRBLT_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_MEMBLT_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_MEM3BLT_INDEX] = TRUE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_ATEXTOUT_INDEX] = FALSE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_AEXTTEXTOUT_INDEX] = FALSE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_DRAWNINEGRID_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_LINETO_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_MULTI_DRAWNINEGRID_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_OPAQUERECT_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_SAVEBITMAP_INDEX] = TRUE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_WTEXTOUT_INDEX] = FALSE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_MEMBLT_R2_INDEX] = FALSE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_MEM3BLT_R2_INDEX] = FALSE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_MULTIDSTBLT_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_MULTIPATBLT_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_MULTISCRBLT_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_MULTIOPAQUERECT_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_FAST_INDEX_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_POLYGON_SC_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_POLYGON_CB_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_POLYLINE_INDEX] = TRUE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[23] = 0
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_FAST_GLYPH_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_ELLIPSE_SC_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_ELLIPSE_CB_INDEX] = TRUE
                0x01,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_INDEX_INDEX] = TRUE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_WEXTTEXTOUT_INDEX] = FALSE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_WLONGTEXTOUT_INDEX] = FALSE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[TS_NEG_WLONGEXTTEXTOUT_INDEX] = FALSE
                0x00,  //  TS_ORDER_CAPABILITYSET::orderSupport[24] = 0

                (byte) 0xa1, 0x06,  //  TS_ORDER_CAPABILITYSET::textFlags = 0x06a1

                0x00, 0x00,  //  TS_ORDER_CAPABILITYSET::pad2octetsB
                0x40, 0x42, 0x0f, 0x00,  //  TS_ORDER_CAPABILITYSET::pad4octetsB

                0x40, 0x42, 0x0f, 0x00,  //  TS_ORDER_CAPABILITYSET::desktopSaveSize = 0xf4240 = 1000000
                0x01, 0x00,  //  TS_ORDER_CAPABILITYSET::pad2octetsC
                0x00, 0x00,  //  TS_ORDER_CAPABILITYSET::pad2octetsD
                0x00, 0x00,  //  TS_ORDER_CAPABILITYSET::textANSICodePage
                0x00, 0x00,  //  TS_ORDER_CAPABILITYSET::pad2octetsE

                // Color Table Cache Capability Set (8 bytes)
                // 0x0a, 0x00, 0x08, 0x00, 0x06, 0x00, 0x00, 0x00,
                //
                0x0a, 0x00,  //  TS_COLORTABLECACHE_CAPABILITYSET::capabilitySetType = CAPSTYPE_COLORCACHE (10)
                0x08, 0x00,  //  TS_COLORTABLECACHE_CAPABILITYSET::lengthCapability = 8 bytes

                0x06, 0x00,  //  TS_COLORTABLECACHE_CAPABILITYSET::colorTableCacheSize = 6
                0x00, 0x00,  //  TS_COLORTABLECACHE_CAPABILITYSET::pad2octets

                // Bitmap Cache Host Support Capability Set (8 bytes)
                // 0x12, 0x00, 0x08, 0x00, 0x01, 0x00, 0x00, 0x00,
                //
                0x12, 0x00,  //  TS_BITMAPCACHE_CAPABILITYSET_HOSTSUPPORT::capabilitySetType  = CAPSTYPE_BITMAPCACHE_HOSTSUPPORT (18)
                0x08, 0x00,  //  TS_BITMAPCACHE_CAPABILITYSET_HOSTSUPPORT::lengthCapability  = 8 bytes

                0x01,  //  TS_BITMAPCACHE_CAPABILITYSET_HOSTSUPPORT::CacheVersion = 1  (corresponds to rev. 2 capabilities)
                0x00,  //  TS_BITMAPCACHE_CAPABILITYSET_HOSTSUPPORT::Pad1
                0x00, 0x00,  //  TS_BITMAPCACHE_CAPABILITYSET_HOSTSUPPORT::Pad2

                // Pointer Capability Set (10 bytes)
                // 0x08, 0x00, 0x0a, 0x00, 0x01, 0x00, 0x19, 0x00, 0x19, 0x00,
                //
                0x08, 0x00,  //  TS_POINTER_CAPABILITYSET::capabilitySetType = CAPSTYPE_POINTER (8)
                0x0a, 0x00,  //  TS_POINTER_CAPABILITYSET::lengthCapability = 10 bytes

                0x01, 0x00,  //  TS_POINTER_CAPABILITYSET::colorPointerFlag = TRUE
                0x19, 0x00,  //  TS_POINTER_CAPABILITYSET::colorPointerCacheSize = 25
                0x19, 0x00,  //  TS_POINTER_CAPABILITYSET::pointerCacheSize = 25

                //  Input Capability Set (88 bytes)
                // 0x0d, 0x00, 0x58, 0x00, 0x35, 0x00, 0x00, 0x00, (byte) 0xa1, 0x06, 0x00, 0x00, 0x40, 0x42, 0x0f, 0x00,
                // 0x0c, (byte) 0xf6, 0x13, (byte) 0xf3, (byte) 0x93, 0x5a, 0x37, (byte) 0xf3, 0x00, (byte) 0x90, 0x30, (byte) 0xe1, 0x34, 0x1c, 0x38, (byte) 0xf3,
                // 0x40, (byte) 0xf6, 0x13, (byte) 0xf3, 0x04, 0x00, 0x00, 0x00, 0x4c, 0x54, (byte) 0xdc, (byte) 0xe2, 0x08, 0x50, (byte) 0xdc, (byte) 0xe2,
                // 0x01, 0x00, 0x00, 0x00, 0x08, 0x50, (byte) 0xdc, (byte) 0xe2, 0x00, 0x00, 0x00, 0x00, 0x38, (byte) 0xf6, 0x13, (byte) 0xf3,
                // 0x2e, 0x05, 0x38, (byte) 0xf3, 0x08, 0x50, (byte) 0xdc, (byte) 0xe2, 0x2c, (byte) 0xf6, 0x13, (byte) 0xf3, 0x00, 0x00, 0x00, 0x00,
                // 0x08, 0x00, 0x0a, 0x00, 0x01, 0x00, 0x19, 0x00,
                //
                0x0d, 0x00,  //  TS_INPUT_CAPABILITYSET::capabilitySetType = CAPSTYPE_INPUT (13)
                0x58, 0x00,  //  TS_INPUT_CAPABILITYSET::lengthCapability = 88 bytes

                0x35, 0x00,  //  TS_INPUT_CAPABILITYSET::inputFlags = 0x0035 = 0x0020 | 0x0010 | 0x0004 | 0x0001 = INPUT_FLAG_FASTPATH_INPUT2 | INPUT_FLAG_VKPACKET | INPUT_FLAG_MOUSEX | INPUT_FLAG_SCANCODES

                0x00, 0x00,  //  TS_INPUT_CAPABILITYSET::pad2octetsA
                (byte) 0xa1, 0x06, 0x00, 0x00,  //  TS_INPUT_CAPABILITYSET::keyboardLayout (not initialized by server)
                0x40, 0x42, 0x0f, 0x00,  //  TS_INPUT_CAPABILITYSET::keyboardType (not initialized by server)
                0x0c, (byte) 0xf6, 0x13, (byte) 0xf3,  //  TS_INPUT_CAPABILITYSET::keyboardSubType  (not initialized by server)
                (byte) 0x93, 0x5a, 0x37, (byte) 0xf3,  //  TS_INPUT_CAPABILITYSET::keyboardFunctionKey (not initialized by server)

                // TS_INPUT_CAPABILITYSET::imeFileName (not initialized by server)
                0x00, (byte) 0x90, 0x30, (byte) 0xe1, 0x34, 0x1c, 0x38, (byte) 0xf3, 0x40, (byte) 0xf6, 0x13, (byte) 0xf3, 0x04, 0x00, 0x00, 0x00,
                0x4c, 0x54, (byte) 0xdc, (byte) 0xe2, 0x08, 0x50, (byte) 0xdc, (byte) 0xe2, 0x01, 0x00, 0x00, 0x00, 0x08, 0x50, (byte) 0xdc, (byte) 0xe2,
                0x00, 0x00, 0x00, 0x00, 0x38, (byte) 0xf6, 0x13, (byte) 0xf3, 0x2e, 0x05, 0x38, (byte) 0xf3, 0x08, 0x50, (byte) 0xdc, (byte) 0xe2,
                0x2c, (byte) 0xf6, 0x13, (byte) 0xf3, 0x00, 0x00, 0x00, 0x00, 0x08, 0x00, 0x0a, 0x00, 0x01, 0x00, 0x19, 0x00,

                //  RAIL Capability Set (8 bytes)
                // 0x17, 0x00, 0x08, 0x00, 0x00, 0x00, 0x00, 0x00,
                //
                0x17, 0x00,  //  TS_RAIL_CAPABILITYSET::capabilitySetType = CAPSTYPE_RAIL (23)
                0x08, 0x00,  //  TS_RAIL_CAPABILITYSET::lengthCapability = 8 bytes

                0x00, 0x00, 0x00, 0x00,  //  TS_RAIL_CAPABILITYSET::railSupportLevel = TS_RAIL_LEVEL_DEFAULT (0)

                //  Windowing Capability Set (11 bytes)
                // 0x18, 0x00, 0x0b, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                //
                0x18, 0x00,  //  TS_WINDOW_CAPABILITYSET::capabilitySetType =  CAPSTYPE_WINDOW (24)
                0x0b, 0x00,  //  TS_WINDOW_CAPABILITYSET::lengthCapability = 11 bytes

                0x00, 0x00, 0x00, 0x00,  //  TS_WINDOW_CAPABILITYSET::wndSupportLevel = TS_WINDOW_LEVEL_DEFAULT (0)
                0x00,  //  TS_WINDOW_CAPABILITYSET::nIconCaches = 0
                0x00, 0x00,  //  TS_WINDOW_CAPABILITYSET::nIconCacheEntries = 0

                // Remainder of Demand Active PDU:

                0x00, 0x00, 0x00, 0x00,  //  TS_DEMAND_ACTIVE_PDU::sessionId = 0
        };
        /* @formatter:on */

        RdpState rdpState = new RdpState();
        ScreenDescription screenDescription = new ScreenDescription();

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element demandActive = new ServerDemandActivePDU("demand_active", screenDescription, rdpState);
        Element sink = new FakeSink("sink");

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, demandActive, sink);
        pipeline.link("source", "demand_active", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
