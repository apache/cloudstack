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
 * @see http://msdn.microsoft.com/en-us/library/cc240470.aspx
 * @see http://msdn.microsoft.com/en-us/library/cc240663.aspx
 */
public class ClientX224ConnectionRequestPDU extends OneTimeSwitch {

    public static final int X224_TPDU_CONNECTION_REQUEST = 0xe0;
    public static final int X224_TPDU_CONNECTION_CONFIRM = 0xd0;
    public static final int X224_TPDU_DISCONNECTION_REQUEST = 0x80;
    public static final int X224_TPDU_DISCONNECTION_CONFIRM = 0xc0;
    public static final int X224_TPDU_EXPEDITED_DATA = 0x10;
    public static final int X224_TPDU_DATA_ACKNOWLEDGE = 0x61;
    public static final int X224_TPDU_EXPEDITET_ACKNOWLEDGE = 0x40;
    public static final int X224_TPDU_REJECT = 0x51;
    public static final int X224_TPDU_ERROR = 0x70;
    public static final int X224_TPDU_PROTOCOL_IDENTIFIER = 0x01;

    /**
     * Reconnection cookie.
     */
    protected String userName;

    public ClientX224ConnectionRequestPDU(String id, String userName) {
        super(id);
        this.userName = userName;
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

        // Length of packet without length field
        int length = 33 + userName.length();
        ByteBuffer buf = new ByteBuffer(length, true);

        // Type (high nibble) = 0xe = CR TPDU; credit (low nibble) = 0
        buf.writeByte(X224_TPDU_CONNECTION_REQUEST);

        buf.writeShort(0); // Destination reference = 0
        buf.writeShort(0); // Source reference = 0
        buf.writeByte(0); // Class and options = 0
        buf.writeString("Cookie: mstshash=" + userName + "\r\n", RdpConstants.CHARSET_8); // Cookie

        // RDP_NEG_REQ::type
        buf.writeByte(RdpConstants.RDP_NEG_REQ_TYPE_NEG_REQ);
        // RDP_NEG_REQ::flags (0)
        buf.writeByte(RdpConstants.RDP_NEG_REQ_FLAGS);
        // RDP_NEG_REQ::length (constant: 8) short int in LE format
        buf.writeByte(0x08);
        buf.writeByte(0x00);

        // RDP_NEG_REQ: Requested protocols: PROTOCOL_SSL
        buf.writeIntLE(RdpConstants.RDP_NEG_REQ_PROTOCOL_SSL);

        // Calculate length of packet and prepend it to buffer
        ByteBuffer data = new ByteBuffer(5);

        // Write length
        data.writeVariableIntLE(buf.length);

        // Reset length of buffer to actual length of data written
        data.length = data.cursor;

        buf.prepend(data);
        data.unref();

        pushDataToOTOut(buf);

        switchOff();
    }

    /**
     * Example.
     *
     * @see http://msdn.microsoft.com/en-us/library/cc240842.aspx
     * @see http://msdn.microsoft.com/en-us/library/cc240500.aspx
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        String cookie = "eltons";

        byte[] packet = new byte[] {

        0x03, // TPKT Header: version = 3
            0x00, // TPKT Header: Reserved = 0
            0x00, // TPKT Header: Packet length - high part
            0x2c, // TPKT Header: Packet length - low part (total = 44 bytes)
            0x27, // X.224: Length indicator (39 bytes)
            (byte)0xe0, // X.224: Type (high nibble) = 0xe = CR TPDU;
                        // credit (low nibble) = 0
            0x00, 0x00, // X.224: Destination reference = 0
            0x00, 0x00, // X.224: Source reference = 0
            0x00, // X.224: Class and options = 0

            'C', 'o', 'o', 'k', 'i', 'e', ':', ' ', 'm', 's', 't', 's', 'h', 'a', 's', 'h', '=', 'e', 'l', 't', 'o', 'n', 's', // "Cookie: mstshash=eltons"
            '\r', '\n', // -Cookie terminator sequence

            0x01, // RDP_NEG_REQ::type (TYPE_RDP_NEG_REQ)
            0x00, // RDP_NEG_REQ::flags (0)
            0x08, 0x00, // RDP_NEG_REQ::length (8 bytes)
            0x01, 0x00, 0x00, 0x00 // RDP_NEG_REQ: Requested protocols
                                   // (PROTOCOL_SSL in little endian format)
            };

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));
        Element cr = new ClientX224ConnectionRequestPDU("cr", cookie);
        Element tpkt = new ClientTpkt("tpkt");
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, cr, tpkt, sink, mainSink);
        pipeline.link("source", "cr", "mainSink");
        pipeline.link("cr >" + OTOUT, "tpkt", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
