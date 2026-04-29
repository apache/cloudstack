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
 * @see http://msdn.microsoft.com/en-us/library/cc240489.aspx
 */
public class ClientSynchronizePDU extends OneTimeSwitch {

    public ClientSynchronizePDU(String id) {
        super(id);
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

        int length = 1024; // Large enough
        ByteBuffer buf = new ByteBuffer(length, true);

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
                // Share ID: 0x000103ea (LE)
                (byte)0xea, (byte)0x03, (byte)0x01,  (byte)0x00,
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

        // Trim buffer to actual length of data written
        buf.trimAtCursor();

        pushDataToOTOut(buf);

        switchOff();
    }

    /**
     * Example.
     *
     * @see http://msdn.microsoft.com/en-us/library/cc240841.aspx
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        /* @formatter:off */
        byte[] packet = new byte[] {
                // TPKT
                (byte)0x03, (byte)0x00,
                // TPKT length: 37 bytes
                (byte)0x00, (byte)0x25,
                // X224 Data PDU
                (byte)0x02, (byte)0xf0, (byte)0x80,

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
                // Share ID: 0x000103ea (LE)
                (byte)0xea, (byte)0x03, (byte)0x01,  (byte)0x00,
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

        };
        /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));
        Element todo = new ClientSynchronizePDU("TODO");
        Element x224 = new ClientX224DataPDU("x224");
        Element tpkt = new ClientTpkt("tpkt");
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, todo, x224, tpkt, sink, mainSink);
        pipeline.link("source", "TODO", "mainSink");
        pipeline.link("TODO >" + OTOUT, "x224", "tpkt", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}

/*
 * @formatting:off

 * 03 00 00 25 02 F0 80 64 00 03 03 EB 70 80 16 16 00 17 00 EC 03 EA 03 01 00 00 01 08 00 1F 00 00 00 01 00 EA 03

  Frame: Number = 40, Captured Frame Length = 94, MediaType = DecryptedPayloadHeader
+ DecryptedPayloadHeader: FrameCount = 1, ErrorStatus = SUCCESS
  TLSSSLData: Transport Layer Security (TLS) Payload Data
+ TLS: TLS Rec Layer-1 SSL Application Data
  ISOTS: TPKTCount = 1
- TPKT: version: 3, Length: 37
    version: 3 (0x3)
    Reserved: 0 (0x0)
    PacketLength: 37 (0x25)
- X224: Data
    Length: 2 (0x2)
    Type: Data
    EOT: 128 (0x80)
- T125: Data Packet
  - MCSHeader: Type=Send Data Request, UserID=1004, ChannelID=1003
   - Type: Send Data Request
    - RootIndex: 25
       Value: (011001..) 0x19
   - UserID: 0x3ec
    - UserID: 0x3ec
     - ChannelId: 1004
      - Align: No Padding
         Padding2: (00......) 0x0
        Value: 3 (0x3)
   - Channel: 0x3eb
    - ChannelId: 1003
       Align: No Padding
       Value: 1003 (0x3EB)
   - DataPriority: high
    - DataPriority: high
     - RootIndex: 1
        Value: (01......) 0x1
   - Segmentation: Begin End
      Begin: (1.......) Begin
      End:   (.1......) End
   - Length: 22
    - Align: No Padding
       Padding4: (0000....) 0x0
      Length: 22
    RDP: RDPBCGR
- RDPBCGR: SynchronizePDU
  - SlowPathPacket: SynchronizePDU
   - SlowPath: Type = TS_PDUTYPE_DATAPDU
    - TsShareControlHeader: Type = TS_PDUTYPE_DATAPDU
       TotalLength: 22 (0x16)
     - PDUType: 23 (0x17)
        Type:            (............0111) TS_PDUTYPE_DATAPDU
        ProtocolVersion: (000000000001....) 1
       PDUSource: 1004 (0x3EC)
    - SlowPathIoPacket: 0x0
     - ShareDataHeader: TS_PDUTYPE2_SYNCHRONIZE
        ShareID: 66538 (0x103EA)
        Pad1: 0 (0x0)
        StreamID: TS_STREAM_LOW
        UncompressedLength: 8 (0x8)
        PDUType2: TS_PDUTYPE2_SYNCHRONIZE
      - CompressedType: Not Compressed
         MPPC:       (....0000) MPPC 8K
         Reserved:   (...0....)
         Compressed: (..0.....) Not Compressed
         Front:      (.0......) Not At Front
         Flush:      (0.......) Not Flushed
        CompressedLength: 0 (0x0)
     - TsSynchronizePDU: 0x1
        MessageType: 0x1, MUST be set to SYNCMSGTYPE_SYNC (1)
        TargetUser: 1002 (0x3EA)
 */
