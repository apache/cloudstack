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

import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.OneTimeSwitch;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.MockSink;
import streamer.debug.MockSource;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240475.aspx
 */
public class ClientInfoPDU extends OneTimeSwitch {

    public static final int INFO_MOUSE = 0x1;
    public static final int INFO_DISABLECTRLALTDEL = 0x2;
    public static final int INFO_UNICODE = 0x10;

    public static final int INFO_MAXIMIZESHELL = 0x20;
    public static final int INFO_loggerONNOTIFY = 0x40;
    public static final int INFO_ENABLEWINDOWSKEY = 0x100;
    public static final int INFO_MOUSE_HAS_WHEEL = 0x00020000;
    public static final int INFO_NOAUDIOPLAYBACK = 0x00080000;

    public static final int PERF_DISABLE_WALLPAPER = 0x1;
    public static final int PERF_DISABLE_FULLWINDOWDRAG = 0x2;
    public static final int PERF_DISABLE_MENUANIMATIONS = 0x4;

    protected byte[] userName = "".getBytes(RdpConstants.CHARSET_16);
    protected byte[] password = "".getBytes(RdpConstants.CHARSET_16); // No effect
    protected byte[] alternateShell = "".getBytes(RdpConstants.CHARSET_16);
    protected byte[] domain = "".getBytes(RdpConstants.CHARSET_16);
    protected byte[] workingDir = "".getBytes(RdpConstants.CHARSET_16);
    protected byte[] clientAddress = "192.168.0.100".getBytes(RdpConstants.CHARSET_16);
    protected byte[] clientDir = "C:\\Windows\\System32\\mstscax.dll".getBytes(RdpConstants.CHARSET_16);

    protected String standardTimeZoneName = "EET, Standard Time";
    protected String daylightTimeZoneName = "EET, Summer Time";
    protected int standardTimeZoneBias = 0; /* in minutes */
    protected int daylightTimeZoneBias = 60; /* in minutes */

    public ClientInfoPDU(String id, String userName) {
        super(id);
        this.userName = userName.getBytes(RdpConstants.CHARSET_16);
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        throw new RuntimeException("Unexpected packet: " + buf + ".");
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Length of packet
        ByteBuffer buf = new ByteBuffer(1024, true);

        // MCS Send Data Request PDU
        buf.writeByte(0x64);

        // Initiator: 0x03 + 1001 = 1004
        buf.writeShort(3);

        // Channel ID: 1003
        buf.writeShort(1003);

        // Data priority: high, segmentation: begin | end (0x40 | 0x20 | 0x10 = 0x70)
        buf.writeByte(0x70);

        // User data length: (variable length field)
        int length = 224 + userName.length + password.length + alternateShell.length + domain.length + workingDir.length + clientAddress.length + clientDir.length;
        buf.writeShort(length | 0x8000);

        // Flags: SEC_INFO_PKT (0x4000)
        buf.writeShort(0x4000);

        // TS_SECURITY_HEADER::flagsHi - ignored
        buf.writeShort(0x0000);

        // Codepage: 0 (UNKNOWN, LE) (use  0x04090409  (1033,1033) for EN_US)
        buf.writeIntLE(0x0000);

        // Flags
        buf.writeIntLE(INFO_MOUSE | INFO_DISABLECTRLALTDEL | INFO_UNICODE |
                INFO_MAXIMIZESHELL | INFO_loggerONNOTIFY | INFO_ENABLEWINDOWSKEY |
                INFO_MOUSE_HAS_WHEEL | INFO_NOAUDIOPLAYBACK);

        //
        // Lengths
        //

        // cbDomain length: 0 bytes (LE) (NOT including size of mandatory NULL terminator)
        buf.writeShortLE(domain.length);

        // cbUserName length: 16 bytes (0x10, LE) (NOT including size of mandatory NULL terminator)
        buf.writeShortLE(userName.length);

        // cbPassword length: (LE) (NOT including size of mandatory NULL terminator)
        buf.writeShortLE(password.length);

        // cbAlternateShell:  (LE) (NOT including size of mandatory NULL terminator)
        buf.writeShortLE(alternateShell.length);

        // cbWorkingDir: (LE) (NOT including size of mandatory NULL terminator)
        buf.writeShortLE(workingDir.length);

        //
        // Values
        //

        // Domain: (UCS2), see cbDomain
        buf.writeBytes(domain);
        buf.writeShort(0);

        // User name: (UCS2), see cbUserName
        buf.writeBytes(userName);
        buf.writeShort(0);

        // Password: (UCS2), see cbPassword
        buf.writeBytes(password);
        buf.writeShort(0);

        // Alternate shell: (UCS2), see cbAlternateShell
        buf.writeBytes(alternateShell);
        buf.writeShort(0);

        // Working directory: (UCS2), see cbWorkingDir
        buf.writeBytes(workingDir);
        buf.writeShort(0);

        // Client address family: 2 (AF_INET, LE)
        buf.writeShortLE(2);

        // cbClientAddress: ( LE) (including the size of the mandatory NULL terminator)
        buf.writeShortLE(clientAddress.length + 2);

        // Client address: (UCS2)
        buf.writeBytes(clientAddress);
        buf.writeShort(0);

        // cbClientDir: 64 bytes (0x40, LE) (including the size of the mandatory NULL terminator)
        buf.writeShortLE(clientDir.length + 2);

        // Client directory: (UCS2)
        buf.writeBytes(clientDir);
        buf.writeShort(0);

        //
        // Client time zone:
        //

        // Bias: 0 minutes (LE)
        buf.writeIntLE(0);

        // Standard name: "EET, Standard Time" (fixed string: 64 bytes, UCS2)
        buf.writeFixedString(62, standardTimeZoneName, RdpConstants.CHARSET_16);
        buf.writeShort(0);

        // Standard date
        buf.writeBytes(new byte[] {
                // wYear: 0 (LE)
                (byte)0x00, (byte)0x00,
                // wMonth: unknown (LE)
                (byte)0x00, (byte)0x00,
                // wDayOfWeek: Sunday (LE)
                (byte)0x00, (byte)0x00,
                // wDay: unknown (LE)
                (byte)0x00, (byte)0x00,
                // wHour: 0 (LE)
                (byte)0x00, (byte)0x00,
                // wMinute: 0 (LE)
                (byte)0x00, (byte)0x00,
                // wSecond: 0 (LE)
                (byte)0x00, (byte)0x00,
                // wMilliseconds: 0
                (byte)0x00, (byte)0x00,

        });

        // StandardBias: 0 minutes (LE)
        buf.writeIntLE(standardTimeZoneBias);

        // Daylight name: "EET, Summer Time" (fixed string: 64 bytes, UCS2)
        buf.writeFixedString(62, daylightTimeZoneName, RdpConstants.CHARSET_16);
        buf.writeShort(0);

        // Daylight date
        buf.writeBytes(new byte[] {
                // wYear: 0 (LE)
                (byte)0x00, (byte)0x00,
                // wMonth: unknown (LE)
                (byte)0x00, (byte)0x00,
                // wDayOfWeek: Sunday (LE)
                (byte)0x00, (byte)0x00,
                // wDay: unknown (LE)
                (byte)0x00, (byte)0x00,
                // wHour: 0 (LE)
                (byte)0x00, (byte)0x00,
                // wMinute: 0 (LE)
                (byte)0x00, (byte)0x00,
                // wSecond: 0 (LE)
                (byte)0x00, (byte)0x00,
                // wMilliseconds: 0
                (byte)0x00, (byte)0x00,

        });

        // Daylight bias: 60 minutes (LE)
        buf.writeIntLE(daylightTimeZoneBias);

        // Client session ID: 0x00000000 (LE)
        buf.writeIntLE(0);

        // Performance flags: 0x7 (LE) = PERF_DISABLE_WALLPAPER (0x1), PERF_DISABLE_FULLWINDOWDRAG (0x2), PERF_DISABLE_MENUANIMATIONS (0x4)
        buf.writeIntLE(PERF_DISABLE_WALLPAPER | PERF_DISABLE_FULLWINDOWDRAG | PERF_DISABLE_MENUANIMATIONS);

        // cbAutoReconnectCookie: 0 bytes (LE)
        buf.writeShortLE(0);

        // Trim buffer to actual length of data written
        buf.trimAtCursor();

        pushDataToOTOut(buf);

        switchOff();
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

                // TPKT
                (byte) 0x03, (byte) 0x00,

                // TPKT length: 343 bytes
                (byte) 0x01, (byte) 0x57,

                // X224 Data PDU
                (byte) 0x02, (byte) 0xf0, (byte) 0x80,


                // MCS Send Data Request PDU
                (byte) 0x64,

                // Initiator: 0x03 + 1001 = 1004
                (byte) 0x00, (byte) 0x03,

                // Channel ID: 1003 (IO Channel)
                (byte) 0x03, (byte) 0xeb,

                // Data priority: high, segmentation: begin | end (0x40 | 0x20 | 0x10 = 0x70)
                (byte) 0x70,

                // User data length: 328  (0x148) bytes, variable length field
                (byte) 0x81, (byte) 0x48,

                // Flags: SEC_INFO_PKT (0x4000)
                (byte) 0x40, (byte) 0x00,

                // TS_SECURITY_HEADER::flagsHi - ignored
                (byte) 0x00, (byte) 0x00,

                // Codepage: 0 (UNKNOWN, LE) (use  0x04090409  (1033,1033) for EN_US)
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                // Flags: 0xa0173 (LE), INFO_MOUSE (0x1), INFO_DISABLECTRLALTDEL (0x2), INFO_UNICODE (0x10),
                // INFO_MAXIMIZESHELL (0x20), INFO_loggerONNOTIFY (0x40), INFO_ENABLEWINDOWSKEY (0x100),
                // INFO_MOUSE_HAS_WHEEL (0x00020000), INFO_NOAUDIOPLAYBACK (0x00080000),
                (byte) 0x73, (byte) 0x01, (byte) 0x0a, (byte) 0x00,

                // Lengths

                // cbDomain length: 0 bytes (LE) (NOT including size of mandatory NULL terminator)
                (byte) 0x00, (byte) 0x00,

                // cbUserName length: 16 bytes (0x10, LE) (NOT including size of mandatory NULL terminator)
                (byte) 0x10, (byte) 0x00,

                // cbPassword length: 0 bytes (LE) (NOT including size of mandatory NULL terminator)
                (byte) 0x00, (byte) 0x00,

                // cbAlternateShell:  0 bytes (LE) (NOT including size of mandatory NULL terminator)
                (byte) 0x00, (byte) 0x00,

                // cbWorkingDir: 0 bytes (LE) (NOT including size of mandatory NULL terminator)
                (byte) 0x00, (byte) 0x00,

                // Values

                // Domain: "" (UCS2), see cbDomain
                (byte) 0x00, (byte) 0x00,

                // User name: "vlisivka" (UCS2), see cbUserName
                (byte) 0x76, (byte) 0x00, (byte) 0x6c, (byte) 0x00, (byte) 0x69, (byte) 0x00, (byte) 0x73, (byte) 0x00,
                (byte) 0x69, (byte) 0x00, (byte) 0x76, (byte) 0x00, (byte) 0x6b, (byte) 0x00, (byte) 0x61, (byte) 0x00,
                (byte) 0x00, (byte) 0x00,

                // Password: "" (UCS2), see cbPassword
                (byte) 0x00, (byte) 0x00,

                // Alternate shell: "" (UCS2), see cbAlternateShell
                (byte) 0x00, (byte) 0x00,

                // Working directory: "" (UCS2), see cbWorkingDir
                (byte) 0x00, (byte) 0x00,

                // Client address family: 2 (AF_INET, LE)
                (byte) 0x02, (byte) 0x00,

                // cbClientAddress = 28 bytes (0x1c, LE) (including the size of the mandatory NULL terminator)
                (byte) 0x1c, (byte) 0x00,

                // Client address: "192.168.0.100" (UCS2)
                (byte) 0x31, (byte) 0x00, (byte) 0x39, (byte) 0x00, (byte) 0x32, (byte) 0x00, (byte) 0x2e, (byte) 0x00,
                (byte) 0x31, (byte) 0x00, (byte) 0x36, (byte) 0x00, (byte) 0x38, (byte) 0x00, (byte) 0x2e, (byte) 0x00,
                (byte) 0x30, (byte) 0x00, (byte) 0x2e, (byte) 0x00, (byte) 0x31, (byte) 0x00, (byte) 0x30, (byte) 0x00,
                (byte) 0x30, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                // cbClientDir: 64 bytes (0x40, LE) (including the size of the mandatory NULL terminator)
                (byte) 0x40, (byte) 0x00,

                // Client directory: "C:\Windows\System32\mstscax.dll" (UCS2)
                (byte) 0x43, (byte) 0x00, (byte) 0x3a, (byte) 0x00, (byte) 0x5c, (byte) 0x00, (byte) 0x57, (byte) 0x00,
                (byte) 0x69, (byte) 0x00, (byte) 0x6e, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0x6f, (byte) 0x00,
                (byte) 0x77, (byte) 0x00, (byte) 0x73, (byte) 0x00, (byte) 0x5c, (byte) 0x00, (byte) 0x53, (byte) 0x00,
                (byte) 0x79, (byte) 0x00, (byte) 0x73, (byte) 0x00, (byte) 0x74, (byte) 0x00, (byte) 0x65, (byte) 0x00,
                (byte) 0x6d, (byte) 0x00, (byte) 0x33, (byte) 0x00, (byte) 0x32, (byte) 0x00, (byte) 0x5c, (byte) 0x00,
                (byte) 0x6d, (byte) 0x00, (byte) 0x73, (byte) 0x00, (byte) 0x74, (byte) 0x00, (byte) 0x73, (byte) 0x00,
                (byte) 0x63, (byte) 0x00, (byte) 0x61, (byte) 0x00, (byte) 0x78, (byte) 0x00, (byte) 0x2e, (byte) 0x00,
                (byte) 0x64, (byte) 0x00, (byte) 0x6c, (byte) 0x00, (byte) 0x6c, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                //
                // Client time zone:

                // Bias: 0 minutes (LE)
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                // Standard name: "EET, Standard Time" (fixed string: 64 bytes, UCS2)
                (byte) 0x45, (byte) 0x00, (byte) 0x45, (byte) 0x00, (byte) 0x54, (byte) 0x00, (byte) 0x2c, (byte) 0x00,
                (byte) 0x20, (byte) 0x00, (byte) 0x53, (byte) 0x00, (byte) 0x74, (byte) 0x00, (byte) 0x61, (byte) 0x00,
                (byte) 0x6e, (byte) 0x00, (byte) 0x64, (byte) 0x00, (byte) 0x61, (byte) 0x00, (byte) 0x72, (byte) 0x00,
                (byte) 0x64, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x54, (byte) 0x00, (byte) 0x69, (byte) 0x00,
                (byte) 0x6d, (byte) 0x00, (byte) 0x65, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                //
                // Standard date
                // wYear: 0 (LE)
                (byte) 0x00, (byte) 0x00,
                // wMonth: unknown (LE)
                (byte) 0x00, (byte) 0x00,
                // wDayOfWeek: Sunday (LE)
                (byte) 0x00, (byte) 0x00,
                // wDay: unknown (LE)
                (byte) 0x00, (byte) 0x00,
                // wHour: 0 (LE)
                (byte) 0x00, (byte) 0x00,
                // wMinute: 0 (LE)
                (byte) 0x00, (byte) 0x00,
                // wSecond: 0 (LE)
                (byte) 0x00, (byte) 0x00,
                // wMilliseconds: 0
                (byte) 0x00, (byte) 0x00,

                // StandardBias: 0 minutes (LE)
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                // Daylight name: "EET, Summer Time" (fixed string: 64 bytes, UCS2)
                (byte) 0x45, (byte) 0x00, (byte) 0x45, (byte) 0x00, (byte) 0x54, (byte) 0x00, (byte) 0x2c, (byte) 0x00,
                (byte) 0x20, (byte) 0x00, (byte) 0x53, (byte) 0x00, (byte) 0x75, (byte) 0x00, (byte) 0x6d, (byte) 0x00,
                (byte) 0x6d, (byte) 0x00, (byte) 0x65, (byte) 0x00, (byte) 0x72, (byte) 0x00, (byte) 0x20, (byte) 0x00,
                (byte) 0x54, (byte) 0x00, (byte) 0x69, (byte) 0x00, (byte) 0x6d, (byte) 0x00, (byte) 0x65, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                // Daylight date
                // wYear: 0 (LE)
                (byte) 0x00, (byte) 0x00,
                // wMonth: unknown (LE)
                (byte) 0x00, (byte) 0x00,
                // wDayOfWeek: Sunday (LE)
                (byte) 0x00, (byte) 0x00,
                // wDay: unknown (LE)
                (byte) 0x00, (byte) 0x00,
                // wHour: 0 (LE)
                (byte) 0x00, (byte) 0x00,
                // wMinute: 0 (LE)
                (byte) 0x00, (byte) 0x00,
                // wSecond: 0 (LE)
                (byte) 0x00, (byte) 0x00,
                // wMilliseconds: 0
                (byte) 0x00, (byte) 0x00,

                // Daylight bias: 60 minutes (LE)
                (byte) 0x3c, (byte) 0x00, (byte) 0x00, (byte) 0x00,


                // Client session ID: 0x00000000 (LE)
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                // Performance flags: 0x7 (LE) = PERF_DISABLE_WALLPAPER (0x1), PERF_DISABLE_FULLWINDOWDRAG (0x2), PERF_DISABLE_MENUANIMATIONS (0x4)
                (byte) 0x07, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                // cbAutoReconnectCookie: 0 bytes (LE)
                (byte) 0x00, (byte) 0x00,
        };
        /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));
        Element client_info = new ClientInfoPDU("client_info", "vlisivka");
        Element x224 = new ClientX224DataPDU("x224");
        Element tpkt = new ClientTpkt("tpkt");
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, client_info, x224, tpkt, sink, mainSink);
        pipeline.link("source", "client_info", "mainSink");
        pipeline.link("client_info >" + OTOUT, "x224", "tpkt", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
