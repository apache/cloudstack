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

public class ServerMCSChannelJoinConfirmPDU extends OneTimeSwitch {

    protected int channel;

    public ServerMCSChannelJoinConfirmPDU(String id, int channel) {
        super(id);
        this.channel = channel;
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

/*
 * 03 00 00 0F 02 F0 80 3E 00 00 03 03 EC 03 EC

  Frame: Number = 22, Captured Frame Length = 72, MediaType = DecryptedPayloadHeader
+ DecryptedPayloadHeader: FrameCount = 1, ErrorStatus = SUCCESS
  TLSSSLData: Transport Layer Security (TLS) Payload Data
+ TLS: TLS Rec Layer-1 SSL Application Data
  ISOTS: TPKTCount = 1
- TPKT: version: 3, Length: 15
    version: 3 (0x3)
    Reserved: 0 (0x0)
    PacketLength: 15 (0xF)
- X224: Data
    Length: 2 (0x2)
    Type: Data
    EOT: 128 (0x80)
- T125: Channel Join Confirm, ChannelId = 1004, Result = rt-successful
  - MCSHeader: Type=Channel Join Confirm
   - Type: Channel Join Confirm
    - RootIndex: 15
       Value: (001111..) 0xf
  - MCSChannelJoinConfirm: ChannelId = 1004, Result = rt-successful
     ChannelIdPresent: 1 (0x1)
   - Result: rt-successful
    - Result: rt-successful
     - RootIndex: 0
        Value: (0000....) 0x0
   - Initiator: 0x3ec
    - UserID: 0x3ec
     - ChannelId: 1004
      - Align: No Padding
         Padding5: (00000...) 0x0
        Value: 3 (0x3)
   - Requested: 0x3ec
    - ChannelId: 1004
       Align: No Padding
       Value: 1004 (0x3EC)
   - ChannelId: 0x3ec
    - ChannelId: 1004
       Align: No Padding
       Value: 1004 (0x3EC)

 */
