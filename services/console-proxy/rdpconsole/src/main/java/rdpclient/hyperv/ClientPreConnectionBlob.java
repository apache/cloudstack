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
package rdpclient.hyperv;

import rdpclient.rdp.RdpConstants;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.OneTimeSwitch;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.MockSink;
import streamer.debug.MockSource;

/**
 * For HyperV server, client must send VM ID before any other packet.
 */
public class ClientPreConnectionBlob extends OneTimeSwitch {

    protected String data;

    public ClientPreConnectionBlob(String id, String data) {
        super(id);
        this.data = data;
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        throw new RuntimeException("This element only sends data. This method must not be called. Unexpected packet: " + buf + ".");
    }

    @Override
    protected void onStart() {
        super.onStart();
        sendPreConnectionBlobData(data);
    }

    protected void sendPreConnectionBlobData(String data) {
        // Length of packet
        ByteBuffer buf = new ByteBuffer(1024, true);

        // Header
        buf.writeBytes(new byte[] {(byte)0x5e, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x02,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,});

        // Length of string in wide characters + two wide \0 (LE)
        buf.writeShortLE(data.length() + 2);

        // Wide string + two wide '\0' characters
        buf.writeString(data + "\0\0", RdpConstants.CHARSET_16);

        // Trim buffer to actual length of data written
        buf.trimAtCursor();

        pushDataToOTOut(buf);

        switchOff();
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
                // Header
                (byte) 0x5e, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,

                // Length of string in wide characters + two wide \0 (LE)
                (byte) 0x26, (byte) 0x00,

                // Wide string
                (byte) 0x33, (byte) 0x00, (byte) 0x39, (byte) 0x00, (byte) 0x34, (byte) 0x00, (byte) 0x31, (byte) 0x00, (byte) 0x38, (byte) 0x00, (byte) 0x46,
                (byte) 0x00, (byte) 0x39, (byte) 0x00, (byte) 0x30, (byte) 0x00, (byte) 0x2d, (byte) 0x00, (byte) 0x36, (byte) 0x00, (byte) 0x44, (byte) 0x00,
                (byte) 0x30, (byte) 0x00, (byte) 0x33, (byte) 0x00, (byte) 0x2d, (byte) 0x00, (byte) 0x34, (byte) 0x00, (byte) 0x36, (byte) 0x00, (byte) 0x38,
                (byte) 0x00, (byte) 0x45, (byte) 0x00, (byte) 0x2d, (byte) 0x00, (byte) 0x42, (byte) 0x00, (byte) 0x37, (byte) 0x00, (byte) 0x39, (byte) 0x00,
                (byte) 0x36, (byte) 0x00, (byte) 0x2d, (byte) 0x00, (byte) 0x39, (byte) 0x00, (byte) 0x31, (byte) 0x00, (byte) 0x43, (byte) 0x00, (byte) 0x36,
                (byte) 0x00, (byte) 0x30, (byte) 0x00, (byte) 0x44, (byte) 0x00, (byte) 0x44, (byte) 0x00, (byte) 0x36, (byte) 0x00, (byte) 0x36, (byte) 0x00,
                (byte) 0x35, (byte) 0x00, (byte) 0x33, (byte) 0x00, (byte) 0x41, (byte) 0x00,

                // Two wide \0
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        };
        /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));
        Element pcb = new ClientPreConnectionBlob("pcb", "39418F90-6D03-468E-B796-91C60DD6653A");
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, pcb, sink, mainSink);
        pipeline.link("source", "pcb", "mainSink");
        pipeline.link("pcb >" + OTOUT, "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
