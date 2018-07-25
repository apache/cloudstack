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
package rdpclient.ntlmssp;

import rdpclient.ntlmssp.asn1.NegoItem;
import rdpclient.ntlmssp.asn1.TSRequest;
import rdpclient.rdp.RdpConstants;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.OneTimeSwitch;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.MockSink;
import streamer.debug.MockSource;
import common.asn1.Tag;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc236641.aspx
 */
public class ClientNtlmsspNegotiate extends OneTimeSwitch {

    /**
     * The set of client configuration flags (section 2.2.2.5) that specify the
     * full set of capabilities of the client.
     */
    public NegoFlags clientConfigFlags = new NegoFlags().set_NEGOTIATE_56().set_NEGOTIATE_KEY_EXCH().set_NEGOTIATE_128().set_NEGOTIATE_VERSION()
            .set_NEGOTIATE_EXTENDED_SESSION_SECURITY().set_NEGOTIATE_ALWAYS_SIGN().set_NEGOTIATE_NTLM().set_NEGOTIATE_LM_KEY().set_NEGOTIATE_SEAL()
            .set_NEGOTIATE_SIGN().set_REQUEST_TARGET().set_NEGOTIATE_OEM().set_NEGOTIATE_UNICODE();

    protected NtlmState ntlmState;

    public ClientNtlmsspNegotiate(String id, NtlmState state) {
        super(id);
        ntlmState = state;
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

        ByteBuffer negoToken = generateNegotiateMessage();
        ntlmState.negotiateMessage = negoToken.toByteArray(); // Store message for MIC calculation in AUTH message

        // Length of packet
        ByteBuffer buf = new ByteBuffer(1024, true);

        TSRequest tsRequest = new TSRequest("TSRequest");
        tsRequest.version.value = 2L;
        NegoItem negoItem = new NegoItem("NegoItem");
        negoItem.negoToken.value = negoToken;
        tsRequest.negoTokens.tags = new Tag[] {negoItem};

        tsRequest.writeTag(buf);

        // Trim buffer to actual length of data written
        buf.trimAtCursor();

        pushDataToOTOut(buf);

        switchOff();
    }

    private ByteBuffer generateNegotiateMessage() {
        ByteBuffer buf = new ByteBuffer(1024);

        // Signature
        buf.writeString("NTLMSSP", RdpConstants.CHARSET_8);
        buf.writeByte(0);

        // Message type
        buf.writeIntLE(NtlmConstants.NEGOTIATE);

        buf.writeIntLE(clientConfigFlags.value); // Flags

        // If the NTLMSSP_NEGOTIATE_VERSION flag is set by the client application,
        // the Version field MUST be set to the current version (section 2.2.2.10),
        // the DomainName field MUST be set to a zero-length string, and the
        // Workstation field MUST be set to a zero-length string.

        // Domain: ""
        buf.writeShortLE(0); // Length
        buf.writeShortLE(0); // Allocated space
        buf.writeIntLE(0); // Offset

        // Workstation: ""
        buf.writeShortLE(0); // Length
        buf.writeShortLE(0); // Allocated space
        buf.writeIntLE(0); // Offset

        // OS Version: 6.1 (Build 7601); NTLM Current Revision 15
        buf.writeBytes(new byte[] {(byte)0x06, (byte)0x01, (byte)0xb1, (byte)0x1d, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0f});

        // Trim buffer to actual length of data written
        buf.trimAtCursor();

        return buf;
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
                // CredSSP BER header:

                (byte)0x30, // Sequence
                (byte)0x37, // Length, 55 bytes

                (byte)0xa0, (byte)0x03, // TAG: [0] (constructed) LEN: 3 byte
                (byte)0x02, (byte)0x01, (byte)0x02, // Version: (int, 1 byte, 0x02)

                // Sequence of sequence
                (byte)0xa1, (byte)0x30, // TAG: [1] (constructed) LEN: 48 bytes
                (byte)0x30, (byte)0x2e, // TAG: [UNIVERSAL 16] (constructed) "SEQUENCE" LEN: 46 bytes
                (byte)0x30, (byte)0x2c, // TAG: [UNIVERSAL 16] (constructed) "SEQUENCE" LEN: 44 bytes
                (byte)0xa0, (byte)0x2a, // TAG: [0] (constructed) LEN: 42 bytes


                (byte)0x04, (byte)0x28, // TAG: [UNIVERSAL 4] (primitive) "OCTET STRING" LEN: 40 bytes

                // NTLM negotiate request

                (byte)0x4e, (byte)0x54, (byte)0x4c, (byte)0x4d, (byte)0x53, (byte)0x53, (byte)0x50, (byte)0x00, // "NTLMSSP\0"

                (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, // Message type: NEGOTIATE (0x1, LE)

                (byte)0xb7, (byte)0x82, (byte)0x08, (byte)0xe2, // Flags: 0xe20882b7 (LE)

                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Domain (security buffer, 8bit, 8 bytes): length: 0x0000 (LE), allocated space: 0x0000 (LE), offset: 0x00000000 (LE)
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, // Workstation  (security buffer, 8bit, 8 bytes): length: 0x0000 (LE), allocated space: 0x0000 (LE), offset: 0x00000000 (LE)
                (byte)0x06, (byte)0x01, (byte)0xb1, (byte)0x1d, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0f, // OS Version: 6.1 (Build 7601); NTLM Current Revision 15, 8 bytes

        };
        /* @formatter:on */

        NtlmState state = new NtlmState();

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));
        Element ntlmssp_negotiate = new ClientNtlmsspNegotiate("ntlmssp_negotiate", state);
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(packet));
        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, ntlmssp_negotiate, sink, mainSink);
        pipeline.link("source", "ntlmssp_negotiate", "mainSink");
        pipeline.link("ntlmssp_negotiate >" + OTOUT, "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }
}
