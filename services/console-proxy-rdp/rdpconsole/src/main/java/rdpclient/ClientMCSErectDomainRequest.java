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
import streamer.Element;
import streamer.Link;
import streamer.MockSink;
import streamer.MockSource;
import streamer.OneTimeSwitch;
import streamer.Pipeline;
import streamer.PipelineImpl;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc240683.aspx
 */
public class ClientMCSErectDomainRequest extends OneTimeSwitch {

    public ClientMCSErectDomainRequest(String id) {
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

        int length = 5;
        ByteBuffer buf = new ByteBuffer(length, true);

        buf.writeByte(0x04); // Erect Domain Request

        // Client SHOULD initialize both the subHeight and subinterval fields of the MCS Erect Domain Request PDU to zero.

        buf.writeByte(1); // ErectDomainRequest::subHeight length = 1 byte
        buf.writeByte(0); // ErectDomainRequest::subHeight

        buf.writeByte(1); // ErectDomainRequest::subInterval length = 1 byte
        buf.writeByte(0); // ErectDomainRequest::subInterval

        pushDataToOTOut(buf);

        switchOff();
    }

    /**
     * Example.
     * @see http://msdn.microsoft.com/en-us/library/cc240837.aspx
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        /* @formatter:off */
    byte[] packet = new byte[] {

        0x03, 0x00, 0x00, 0x0c,  //  TPKT Header (length = 12 bytes)
        0x02, (byte) 0xf0, (byte) 0x80,  //  X.224 Data TPDU

        // PER encoded (ALIGNED variant of BASIC-PER) PDU contents:
        0x04, 0x01, 0x00, 0x01, 0x00,

        // 0x04:
        // 0 - --\
        // 0 -   |
        // 0 -   | CHOICE: From DomainMCSPDU select erectDomainRequest (1)
        // 0 -   | of type ErectDomainRequest
        // 0 -   |
        // 1 - --/
        // 0 - padding
        // 0 - padding

        // 0x01:
        // 0 - --\
        // 0 -   |
        // 0 -   |
        // 0 -   | ErectDomainRequest::subHeight length = 1 byte
        // 0 -   |
        // 0 -   |
        // 0 -   |
        // 1 - --/

        // 0x00:
        // 0 - --\
        // 0 -   |
        // 0 -   |
        // 0 -   | ErectDomainRequest::subHeight = 0
        // 0 -   |
        // 0 -   |
        // 0 -   |
        // 0 - --/

        // 0x01:
        // 0 - --\
        // 0 -   |
        // 0 -   |
        // 0 -   | ErectDomainRequest::subInterval length = 1 byte
        // 0 -   |
        // 0 -   |
        // 0 -   |
        // 1 - --/

        // 0x00:
        // 0 - --\
        // 0 -   |
        // 0 -   |
        // 0 -   | ErectDomainRequest::subInterval = 0
        // 0 -   |
        // 0 -   |
        // 0 -   |
        // 0 - --/


    };
    /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));
        Element todo = new ClientMCSErectDomainRequest("TODO");
        Element x224 = new ClientX224DataPdu("x224");
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
 * 03 00 00 0C 02 F0 80 04 01 00 01 00

  Frame: Number = 14, Captured Frame Length = 69, MediaType = DecryptedPayloadHeader
+ DecryptedPayloadHeader: FrameCount = 1, ErrorStatus = SUCCESS
  TLSSSLData: Transport Layer Security (TLS) Payload Data
+ TLS: TLS Rec Layer-1 SSL Application Data
  ISOTS: TPKTCount = 1
- TPKT: version: 3, Length: 12
    version: 3 (0x3)
    Reserved: 0 (0x0)
    PacketLength: 12 (0xC)
- X224: Data
    Length: 2 (0x2)
    Type: Data
    EOT: 128 (0x80)
- T125: Erect Domain Request, SubHeight = 0, SubInterval = 0
  - MCSHeader: Type=Erect Domain Request
   - Type: Erect Domain Request
    - RootIndex: 1
       Value: (000001..) 0x1
  - MCSErectDomainRequest: SubHeight = 0, SubInterval = 0
   - SubHeight: 0x0
    - Length: 1
     - Align: No Padding
        Padding2: (00......) 0x0
       Length: 1
      Value: 0 (0x0)
   - SubInterval: 0x0
    - Length: 1
       Align: No Padding
       Length: 1
      Value: 0 (0x0)

 */
