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

import com.cloud.utils.ConstantTimeComparator;

import java.util.Arrays;

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

/**
 * @see http://msdn.microsoft.com/en-us/library/cc236642.aspx
 */
public class ServerNtlmsspChallenge extends OneTimeSwitch implements NtlmConstants {

    protected NtlmState ntlmState;

    public ServerNtlmsspChallenge(String id, NtlmState state) {
        super(id);
        ntlmState = state;
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Extract server challenge, extract server flags.

        // Parse TSRequest in BER format
        TSRequest request = new TSRequest("TSRequest");
        request.readTag(buf);

        ByteBuffer negoToken = ((NegoItem)request.negoTokens.tags[0]).negoToken.value;
        ntlmState.challengeMessage = negoToken.toByteArray(); // Store message for MIC calculation in AUTH message

        parseNtlmChallenge(negoToken);

        negoToken.unref();
        buf.unref();
        switchOff();
    }

    public void parseNtlmChallenge(ByteBuffer buf) {

        // Signature: "NTLMSSP\0"
        String signature = buf.readVariableString(RdpConstants.CHARSET_8);
        if (!ConstantTimeComparator.compareStrings(signature, NTLMSSP))
            throw new RuntimeException("Unexpected NTLM message singature: \"" + signature + "\". Expected signature: \"" + NTLMSSP + "\". Data: " + buf + ".");

        // MessageType (CHALLENGE)
        int messageType = buf.readSignedIntLE();
        if (messageType != NtlmConstants.CHALLENGE)
            throw new RuntimeException("Unexpected NTLM message type: " + messageType + ". Expected type: CHALLENGE (" + NtlmConstants.CHALLENGE + "). Data: " + buf
                    + ".");

        // TargetName
        ntlmState.serverTargetName = readStringByDescription(buf);

        // NegotiateFlags
        ntlmState.negotiatedFlags = new NegoFlags(buf.readSignedIntLE());
        if (verbose)
            System.out.println("[" + this + "] INFO: Server negotiate flags: " + ntlmState.negotiatedFlags + ".");

        // ServerChallenge
        ByteBuffer challenge = buf.readBytes(8);
        ntlmState.serverChallenge = challenge.toByteArray();
        if (verbose)
            System.out.println("[" + this + "] INFO: Server challenge: " + challenge + ".");
        challenge.unref();

        // Reserved/context
        buf.skipBytes(8);

        // TargetInfo
        ByteBuffer targetInfo = readBlockByDescription(buf);

        // Store raw target info block for Type3 message
        ntlmState.serverTargetInfo = targetInfo.toByteArray();

        // Parse target info block
        parseTargetInfo(targetInfo);
        targetInfo.unref();

        // OS Version, NTLM revision, 8 bytes, Optional. Ignore it.

        // Ignore rest of buffer with allocated blocks

        buf.unref();
    }

    public void parseTargetInfo(ByteBuffer buf) {
        // Parse attribute list

        while (buf.remainderLength() > 0) {
            int type = buf.readUnsignedShortLE();
            int length = buf.readUnsignedShortLE();

            if (type == MSV_AV_EOL)
                // End of list
                break;

            ByteBuffer data = buf.readBytes(length);
            parseAttribute(data, type, length);
            data.unref();
        }
    }

    public void parseAttribute(ByteBuffer buf, int type, int length) {
        switch (type) {
        case MSV_AV_NETBIOS_DOMAIN_NAME:
            ntlmState.serverNetbiosDomainName = buf.readString(length, RdpConstants.CHARSET_16);
            break;
        case MSV_AV_NETBIOS_COMPUTER_NAME:
            ntlmState.serverNetbiosComputerName = buf.readString(length, RdpConstants.CHARSET_16);
            break;
        case MSV_AV_DNS_DOMAIN_NAME:
            ntlmState.serverDnsDomainName = buf.readString(length, RdpConstants.CHARSET_16);
            break;
        case MSV_AV_DNS_COMPUTER_NAME:
            ntlmState.serverDnsComputerName = buf.readString(length, RdpConstants.CHARSET_16);
            break;
        case MSV_AV_DNS_TREE_NAME:
            ntlmState.serverDnsTreeName = buf.readString(length, RdpConstants.CHARSET_16);
            break;

        case MSV_AV_TIMESTAMP:
            ByteBuffer tmp = buf.readBytes(length);
            ntlmState.serverTimestamp = tmp.toByteArray();
            //*DEBUG*/System.out.println("Server timestamp: "+tmp.toPlainHexString());
            tmp.unref();
            break;

        default:
            // Ignore
            //throw new RuntimeException("[" + this + "] ERROR: Unknown NTLM target info attribute: " + type + ". Data: " + buf + ".");

        }

    }

    /**
     * Read NTLM wide string, by it description. Buffer offset must point to
     * beginning of NTLM message signature.
     *
     * @param buf
     *          buffer with cursor pointing to description
     * @return
     */
    public static String readStringByDescription(ByteBuffer buf) {
        ByteBuffer block = readBlockByDescription(buf);
        String value = block.readString(block.length, RdpConstants.CHARSET_16);
        block.unref();

        return value;
    }

    public static ByteBuffer readBlockByDescription(ByteBuffer buf) {
        int blockLength = buf.readUnsignedShortLE(); // In bytes
        int allocatedSpace = buf.readUnsignedShortLE();
        int offset = buf.readSignedIntLE();

        if (allocatedSpace < blockLength)
            blockLength = allocatedSpace;

        if (offset > buf.length || offset < 0 || offset + allocatedSpace > buf.length)
            throw new RuntimeException("ERROR: NTLM block is too long. Allocated space: " + allocatedSpace + ", block offset: " + offset + ", data: "
                    + buf + ".");

        // Move cursor to position of allocated block, starting from beginning of
        // this buffer
        int storedCursor = buf.cursor;
        buf.cursor = offset;

        // Read string
        ByteBuffer value = buf.readBytes(blockLength);

        // Restore cursor
        buf.cursor = storedCursor;

        return value;
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
                0x30, (byte) 0x82, 0x01, 0x02, // TAG: [UNIVERSAL 16] (constructed) "SEQUENCE" LEN: 258 bytes
                (byte) 0xa0, 0x03, // TAG: [0] (constructed) LEN: 3 bytes
                0x02, 0x01, 0x03,  // TAG: [UNIVERSAL 2] (primitive) "INTEGER" LEN: 1 bytes, Version: 0x3
                (byte) 0xa1, (byte) 0x81, (byte) 0xfa, // TAG: [1] (constructed) LEN: 250 bytes
                0x30, (byte) 0x81, (byte) 0xf7, // TAG: [UNIVERSAL 16] (constructed) "SEQUENCE" LEN: 247 bytes
                0x30, (byte) 0x81, (byte) 0xf4, // TAG: [UNIVERSAL 16] (constructed) "SEQUENCE" LEN: 244 bytes
                (byte) 0xa0, (byte) 0x81, (byte) 0xf1, // TAG: [0] (constructed) LEN: 241 bytes
                0x04, (byte) 0x81, (byte) 0xee, // TAG: [UNIVERSAL 4] (primitive) "OCTET STRING" LEN: 238 bytes

                0x4e, 0x54, 0x4c, 0x4d, 0x53, 0x53, 0x50, 0x00, // "NTLMSSP\0"

                0x02, 0x00, 0x00, 0x00, // MessageType (CHALLENGE)
                0x1e, 0x00, 0x1e, 0x00, 0x38, 0x00, 0x00, 0x00, // TargetName (length: 30, allocated space: 30, offset: 56)
                0x35, (byte) 0x82, (byte) 0x8a, (byte) 0xe2, // NegotiateFlags
                0x52, (byte) 0xbe, (byte) 0x83, (byte) 0xd1, (byte) 0xf8, (byte) 0x80, 0x16, 0x6a,  //  ServerChallenge
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, //  Reserved
                (byte) 0x98, 0x00, (byte) 0x98, 0x00, 0x56, 0x00, 0x00, 0x00, // TargetInfo (length: 152, allocated space: 152, offset: 86)
                0x06, 0x03, (byte) 0xd7, 0x24, 0x00, 0x00, 0x00, 0x0f,  // Version (6.3, build 9431) , NTLM current revision: 15


                0x57, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x2d, 0x00, 0x4c, 0x00, 0x4f, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00, 0x42, 0x00, 0x32, 0x00, 0x4c, 0x00, 0x53, 0x00, 0x52, 0x00, 0x30, 0x00,  // Target name value: "WIN-LO419B2LSR0"

                // Target Info value:

                // Attribute list

                0x02, 0x00, // Item Type: NetBIOS domain name (0x0002, LE)
                0x1e, 0x00, //  Item Length: 30 (LE)
                0x57, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x2d, 0x00, 0x4c, 0x00, 0x4f, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00, 0x42, 0x00, 0x32, 0x00, 0x4c, 0x00, 0x53, 0x00, 0x52, 0x00, 0x30, 0x00, // "WIN-LO419B2LSR0"

                0x01, 0x00,  //  Item Type: NetBIOS computer name (0x0001, LE)
                0x1e, 0x00, //  Item Length: 30 (LE)
                0x57, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x2d, 0x00, 0x4c, 0x00, 0x4f, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00, 0x42, 0x00, 0x32, 0x00, 0x4c, 0x00, 0x53, 0x00, 0x52, 0x00, 0x30, 0x00, // "WIN-LO419B2LSR0"

                0x04, 0x00,  // Item Type: DNS domain name (0x0004, LE)
                0x1e, 0x00, //  Item Length: 30 (LE)
                0x57, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x2d, 0x00, 0x4c, 0x00, 0x4f, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00, 0x42, 0x00, 0x32, 0x00, 0x4c, 0x00, 0x53, 0x00, 0x52, 0x00, 0x30, 0x00, // "WIN-LO419B2LSR0"

                0x03, 0x00,  // Item Type: DNS computer name (0x0003, LE)
                0x1e, 0x00, //  Item Length: 30 (LE)
                0x57, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x2d, 0x00, 0x4c, 0x00, 0x4f, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00, 0x42, 0x00, 0x32, 0x00, 0x4c, 0x00, 0x53, 0x00, 0x52, 0x00, 0x30, 0x00, // "WIN-LO419B2LSR0"

                0x07, 0x00,  // Item Type: Timestamp (0x0007, LE)
                0x08, 0x00, //  Item Length: 8 (LE)
                (byte) 0x99, 0x4f, 0x02, (byte) 0xd8, (byte) 0xf4, (byte) 0xaf, (byte) 0xce, 0x01, // TODO

                // Attribute: End of list
                0x00, 0x00,
                0x00, 0x00,
        };
        /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(packet, new byte[] {1, 2, 3}));
        NtlmState state = new NtlmState();
        Element ntlmssp_challenge = new ServerNtlmsspChallenge("ntlmssp_challenge", state);
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers());
        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, ntlmssp_challenge, sink, mainSink);
        pipeline.link("source", "ntlmssp_challenge", "mainSink");
        pipeline.link("ntlmssp_challenge >" + OTOUT, "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);

        // Check state challenge
        byte[] challenge = new byte[] {0x52, (byte)0xbe, (byte)0x83, (byte)0xd1, (byte)0xf8, (byte)0x80, 0x16, 0x6a};
        if (state.serverChallenge == null)
            throw new RuntimeException("Challenge was not extracted from server NTLMSSP Challenge packet.");
        if (!Arrays.equals(challenge, state.serverChallenge))
            throw new RuntimeException("Challenge was extracted from server NTLMSSP Challenge packet is not equal to expected. Actual value: "
                    + state.serverChallenge + ", expected value: " + challenge + ".");

    }

}
