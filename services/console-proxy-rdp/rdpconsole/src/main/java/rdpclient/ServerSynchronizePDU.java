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

import streamer.ByteBuffer;
import streamer.Link;
import streamer.OneTimeSwitch;

public class ServerSynchronizePDU extends OneTimeSwitch {

    public ServerSynchronizePDU(String id) {
        super(id);
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Ignore packet
        buf.unref();
        switchOff();
    }

}

/* @formatter:off */
/*

 * 03 00 00 24 02 F0 80 68 00 01 03 EB 70 16 16 00 17 00 EA 03 EA 03 01 00 08 00 16 00 1F 00 00 00 01 00 86 A4

  Frame: Number = 36, Captured Frame Length = 93, MediaType = DecryptedPayloadHeader
+ DecryptedPayloadHeader: FrameCount = 1, ErrorStatus = SUCCESS
  TLSSSLData: Transport Layer Security (TLS) Payload Data
+ TLS: TLS Rec Layer-1 SSL Application Data
  ISOTS: TPKTCount = 1
- TPKT: version: 3, Length: 36
    version: 3 (0x3)
    Reserved: 0 (0x0)
    PacketLength: 36 (0x24)
- X224: Data
    Length: 2 (0x2)
    Type: Data
    EOT: 128 (0x80)
- T125: Data Packet
  - MCSHeader: Type=Send Data Indication, UserID=1002, ChannelID=1003
   - Type: Send Data Indication
    - RootIndex: 26
       Value: (011010..) 0x1a
   - UserID: 0x3ea
    - UserID: 0x3ea
     - ChannelId: 1002
      - Align: No Padding
         Padding2: (00......) 0x0
        Value: 1 (0x1)
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
       PDUSource: 1002 (0x3EA)
    - SlowPathIoPacket: 0x0
     - ShareDataHeader: TS_PDUTYPE2_SYNCHRONIZE
        ShareID: 66538 (0x103EA)
        Pad1: 8 (0x8)
        StreamID: STREAM_UNDEFINED
        UncompressedLength: 22 (0x16)
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
        TargetUser: 42118 (0xA486)
 */
