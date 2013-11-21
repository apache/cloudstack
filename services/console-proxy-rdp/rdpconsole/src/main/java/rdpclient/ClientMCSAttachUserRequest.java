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
 * @see http://msdn.microsoft.com/en-us/library/cc240684.aspx
 */
public class ClientMCSAttachUserRequest extends OneTimeSwitch {

    public ClientMCSAttachUserRequest(String id) {
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

        int length = 1;
        ByteBuffer buf = new ByteBuffer(length, true);

        buf.writeByte(0x28); // AttachUserRequest

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

        0x03, 0x00, 0x00, 0x08,  //  TPKT Header (length = 8 bytes)
        0x02, (byte) 0xf0, (byte) 0x80,  //  X.224 Data TPDU

        // PER encoded (ALIGNED variant of BASIC-PER) PDU contents:
        0x28,

        // 0x28:
        // 0 - --\
        // 0 -   |
        // 1 -   | CHOICE: From DomainMCSPDU select attachUserRequest (10)
        // 0 -   | of type AttachUserRequest
        // 1 -   |
        // 0 - --/
        // 0 - padding
        // 0 - padding

    };
    /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));
        Element todo = new ClientMCSAttachUserRequest("TODO");
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
