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

public class ServerMCSPDU extends BaseElement {

    public ServerMCSPDU(String id) {
        super(id);
    }

    @Override
    public void handleData(ByteBuffer buf, Link link) {
        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        byte headerByte = buf.readSignedByte();
        int type = headerByte >> 2;

        switch (type) {
        // Expected type: send data indication: 26 (0x1a, top 6 bits, or 0x68)
            case 0x1a: {
                // int userId = buf.readUnsignedShort() + 1001; // User ID: 1002 (1001+1)
                buf.skipBytes(2); // Ignore user ID

                int channelId = buf.readUnsignedShort(); // Channel ID: 1003

                int flags = buf.readSignedByte();
                if ((flags & 0x30) != 0x30)
                    throw new RuntimeException("Fragmented MCS packets are not supported.");

                int payloadLength = buf.readVariableUnsignedShort();

                ByteBuffer data = buf.readBytes(payloadLength);

                buf.unref();

                pushDataToPad("channel_" + channelId, data);
                break;
            }

            case 0x8: {
                // Disconnection sequence.
                buf.unref();
                break;
            }

            default:
                throw new RuntimeException("Unsupported MCS packet type: " + type + "(" + headerByte + "), data: " + buf + ".");
        }

    }

    /**
     * Example.
     *
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        // System.setProperty("streamer.Element.debug", "true");
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

            // - PDUType: 22 (0x16, LE)
            // Type: (............0110) TS_PDUTYPE_DEACTIVATEALLPDU
            // ProtocolVersion: (000000000001....) 1
            (byte)0x16, (byte)0x00,

            (byte)0xEA, (byte)0x03, // PDU source: 1002 (LE)
            (byte)0xEA, (byte)0x03, (byte)0x01, (byte)0x00, // ShareID = 66538

            (byte)0x01, (byte)0x00, // Length if source descriptor: 1 (LE)
            (byte)0x00, // Source descriptor (should be set to 0): 0
        };

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element mcs = new ServerMCSPDU("mcs") {
            {
                verbose = true;
            }
        };
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
        pipeline.add(source, tpkt, x224, mcs, sink);
        pipeline.link("source", "tpkt", "x224", "mcs >channel_1003", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
