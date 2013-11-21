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
 * Once the External Security Protocol handshake has run to completion, the
 * client MUST continue with the connection sequence by sending the MCS Connect
 * Initial PDU to the server over the newly established secure channel.
 *
 *
 * @see http://msdn.microsoft.com/en-us/library/cc240663.aspx
 */
public class ServerX224ConnectionConfirmPDU extends OneTimeSwitch {

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
     * The server requires that the client support Enhanced RDP Security with
     * either TLS 1.0, 1.1 or 1.2 or CredSSP. If only CredSSP was requested then
     * the server only supports TLS.
     */
    public static final int SSL_REQUIRED_BY_SERVER = 0x00000001;

    /**
     * The server is configured to only use Standard RDP Security mechanisms and
     * does not support any External Security Protocols.
     */
    public static final int SSL_NOT_ALLOWED_BY_SERVER = 0x00000002;

    /**
     * The server does not possess a valid authentication certificate and cannot
     * initialize the External Security Protocol Provider.
     */
    public static final int SSL_CERT_NOT_ON_SERVER = 0x00000003;

    /**
     * The list of requested security protocols is not consistent with the current
     * security protocol in effect. This error is only possible when the Direct
     * Approach is used and an External Security Protocolis already being used.
     */
    public static final int INCONSISTENT_FLAGS = 0x00000004;

    /**
     * The server requires that the client support Enhanced RDP Security with
     * CredSSP.
     */
    public static final int HYBRID_REQUIRED_BY_SERVER = 0x00000005;

    /**
     * The server requires that the client support Enhanced RDP Security with TLS
     * 1.0, 1.1 or 1.2 and certificate-based client authentication.
     */
    public static final int SSL_WITH_USER_AUTH_REQUIRED_BY_SERVER = 0x00000006;

    public ServerX224ConnectionConfirmPDU(String id) {
        super(id);
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        int x224Length = buf.readVariableSignedIntLE();

        int x224Type = buf.readUnsignedByte();
        if (x224Type != X224_TPDU_CONNECTION_CONFIRM)
            throw new RuntimeException("Unexpected type of packet. Expected type: " + X224_TPDU_CONNECTION_CONFIRM + " (CONNECTION CONFIRM), actual type: " + x224Type +
                ", length: " + x224Length + ", buf: " + buf + ".");

        // Ignore destination reference, because client side has only one node
        buf.skipBytes(2);

        // Source reference
        // int srcRef = buf.readUnsignedShort();
        buf.skipBytes(2);

        // Ignore class and options
        buf.skipBytes(1);

        // RDP_NEG_RSP::type (TYPE_RDP_NEG_RSP)
        int negType = buf.readUnsignedByte();

        // RDP_NEG_RSP::flags (0)
        buf.skipBytes(1); // Ignore: always 0

        // RDP_NEG_RSP::length (always 8 bytes)
        int length = buf.readUnsignedShortLE();

        if (length != 8)
            throw new RuntimeException("Unexpected length of buffer. Expected value: 8, actual value: " + length + ", RDP NEG buf: " + buf + ".");

        // RDP_NEG_RSP: Selected protocols (PROTOCOL_SSL)
        int protocol = buf.readSignedIntLE();

        if (negType != RdpConstants.RDP_NEG_REQ_TYPE_NEG_RSP) {
            // Parse error code, see
            // http://msdn.microsoft.com/en-us/library/cc240507.aspx
            int errorCode = protocol;
            String message = "Unknown error.";
            switch (errorCode) {
                case SSL_REQUIRED_BY_SERVER:
                    message =
                        "The server requires that the client support Enhanced RDP Security with either TLS 1.0, 1.1 or 1.2 or CredSSP. If only CredSSP was requested then the server only supports TLS.";
                    break;

                case SSL_NOT_ALLOWED_BY_SERVER:
                    message = "The server is configured to only use Standard RDP Security mechanisms and does not support any External Security Protocols.";
                    break;

                case SSL_CERT_NOT_ON_SERVER:
                    message = "The server does not possess a valid authentication certificate and cannot initialize the External Security Protocol Provider.";
                    break;

                case INCONSISTENT_FLAGS:
                    message =
                        "The list of requested security protocols is not consistent with the current security protocol in effect. This error is only possible when the Direct Approach is used and an External Security Protocolis already being used.";
                    break;

                case HYBRID_REQUIRED_BY_SERVER:
                    message = "The server requires that the client support Enhanced RDP Security with CredSSP.";
                    break;

                case SSL_WITH_USER_AUTH_REQUIRED_BY_SERVER:
                    message = "The server requires that the client support Enhanced RDP Security  with TLS 1.0, 1.1 or 1.2 and certificate-based client authentication.";
                    break;

            }
            throw new RuntimeException("Connection failure: " + message);
        }

        if (protocol != RdpConstants.RDP_NEG_REQ_PROTOCOL_SSL)
            throw new RuntimeException("Unexpected protocol type. Expected protocol type: " + RdpConstants.RDP_NEG_REQ_PROTOCOL_SSL + " (SSL), actual response type: " +
                protocol + ", RDP NEG buf: " + buf + ".");

        if (verbose)
            System.out.println("[" + this + "] INFO: RDP Negotiation response. Type: " + negType + ", protocol: " + protocol + ".");

        // Next: upgrade socket to SSL, send ConnectInitial packet
        switchOff();
    }

    /**
     * Example.
     *
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

//    byte[] packet = new byte[] {
//
//        0x03, // -> TPKT Header: TPKT version = 3
//        0x00, // TPKT Header: Reserved = 0
//        0x00, 0x13, // TPKT Header: Packet length - (total = 19 bytes)
//        0x0e, // X.224: Length indicator (14 bytes)
//        (byte) 0xd0, // X.224: Type (high nibble) = 0xd = CC TPDU; credit
//                     // (low nibble) = 0
//        0x00, 0x00, // X.224: Destination reference = 0
//        0x12, 0x34, // X.224: Source reference = 0x1234 (bogus value)
//        0x00, // X.224: Class and options = 0
//
//        0x02, // RDP_NEG_RSP::type (TYPE_RDP_NEG_RSP)
//        0x00, // RDP_NEG_RSP::flags (0)
//        0x08, 0x00, // RDP_NEG_RSP::length (8 bytes)
//        0x01, 0x00, 0x00, 0x00 // RDP_NEG_RSP: Selected protocols (PROTOCOL_SSL)
//    };

        // Connection failure
        // 03 00 00 13 0e d0 00 00 12 34 00 03 00 08 00 05 00 00 00
        byte[] packet = new byte[] {

        0x03, // -> TPKT Header: TPKT version = 3
            0x00, // TPKT Header: Reserved = 0
            0x00, 0x13, // TPKT Header: Packet length - (total = 19 bytes)
            0x0e, // X.224: Length indicator (14 bytes)
            (byte)0xd0, // X.224: Type (high nibble) = 0xd = CC TPDU; credit
                        // (low nibble) = 0
            0x00, 0x00, // X.224: Destination reference = 0
            0x12, 0x34, // X.224: Source reference = 0x1234 (bogus value)
            0x00, // X.224: Class and options = 0
            (byte)0x03, // Failure
            (byte)0x00, // RDP_NEG_RSP::flags (0)
            (byte)0x08, (byte)0x00, // RDP_NEG_RSP::length (8 bytes)
            (byte)0x05, (byte)0x00, (byte)0x00, (byte)0x00, // Code:  HYBRID_REQUIRED_BY_SERVER

        };

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element cc = new ServerX224ConnectionConfirmPDU("cc");
        Element tpkt = new ServerTpkt("tpkt");
        Element sink = new MockSink("sink", new ByteBuffer[] {});
        Element mainSink = new MockSink("mainSink", new ByteBuffer[] {});

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, tpkt, cc, sink, mainSink);
        pipeline.link("source", "tpkt", "cc", "mainSink");
        pipeline.link("cc >" + OTOUT, "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }
}
