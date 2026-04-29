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

import java.nio.charset.Charset;

import rdpclient.ntlmssp.asn1.NegoItem;
import rdpclient.ntlmssp.asn1.SubjectPublicKeyInfo;
import rdpclient.ntlmssp.asn1.TSRequest;
import rdpclient.rdp.RdpConstants;
import streamer.ByteBuffer;
import streamer.Element;
import streamer.Link;
import streamer.OneTimeSwitch;
import streamer.Pipeline;
import streamer.PipelineImpl;
import streamer.debug.Dumper;
import streamer.debug.MockSink;
import streamer.debug.MockSource;
import streamer.ssl.SSLState;
import common.asn1.Tag;

/**
 * @see http://msdn.microsoft.com/en-us/library/cc236643.aspx
 */
public class ClientNtlmsspPubKeyAuth extends OneTimeSwitch implements NtlmConstants, Dumper {

    /**
     * Offset of first byte of allocated block after NTLMSSP header and block
     * descriptors.
     */
    private static final int BLOCKS_OFFSET = 88;

    protected NtlmState ntlmState;
    protected SSLState sslState;

    protected String targetDomain;
    protected String user;
    protected String password;
    protected String workstation;
    protected String serverHostName;

    public ClientNtlmsspPubKeyAuth(String id, NtlmState ntlmState, SSLState sslState, String serverHostName, String targetDomain, String workstation,
            String user, String password) {
        super(id);
        this.ntlmState = ntlmState;
        this.sslState = sslState;
        this.serverHostName = serverHostName;
        this.targetDomain = targetDomain;
        this.workstation = workstation;
        this.user = user;
        this.password = password;
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

        /*
         * @see
         * http://blogs.msdn.com/b/openspecification/archive/2010/04/20/ntlm-keys
         * -and-sundry-stuff.aspx
         */
        ntlmState.domain = targetDomain;
        ntlmState.user = user;
        ntlmState.password = password;
        ntlmState.workstation = workstation;
        ntlmState.generateServicePrincipalName(serverHostName);
        ntlmState.ntlm_construct_authenticate_target_info();
        ntlmState.ntlm_generate_timestamp();
        ntlmState.ntlm_generate_client_challenge();
        ntlmState.ntlm_compute_lm_v2_response();
        ntlmState.ntlm_compute_ntlm_v2_response();
        ntlmState.ntlm_generate_key_exchange_key();
        ntlmState.ntlm_generate_random_session_key();
        ntlmState.ntlm_generate_exported_session_key();
        ntlmState.ntlm_encrypt_random_session_key();
        ntlmState.ntlm_init_rc4_seal_states();

        ByteBuffer authenticateMessage = generateAuthenticateMessage(ntlmState);
        ByteBuffer messageSignatureAndEncryptedServerPublicKey = generateMessageSignatureAndEncryptedServerPublicKey(ntlmState);

        // Length of packet
        ByteBuffer buf = new ByteBuffer(4096, true);

        TSRequest tsRequest = new TSRequest("TSRequest");
        tsRequest.version.value = 2L;
        NegoItem negoItem = new NegoItem("NegoItem");
        negoItem.negoToken.value = authenticateMessage;

        tsRequest.negoTokens.tags = new Tag[] {negoItem};

        tsRequest.pubKeyAuth.value = messageSignatureAndEncryptedServerPublicKey;

        tsRequest.writeTag(buf);

        // Trim buffer to actual length of data written
        buf.trimAtCursor();

        pushDataToOTOut(buf);

        switchOff();
    }

    private byte[] getServerPublicKey() {
        // SSL certificate public key with algorithm
        ByteBuffer subjectPublicKeyInfo = new ByteBuffer(sslState.serverCertificateSubjectPublicKeyInfo);

        // Parse subjectPublicKeyInfo
        SubjectPublicKeyInfo parser = new SubjectPublicKeyInfo("SubjectPublicKeyInfo");
        parser.readTag(subjectPublicKeyInfo);

        // Copy subjectPublicKey subfield to separate byte buffer
        ByteBuffer subjectPublicKey = new ByteBuffer(subjectPublicKeyInfo.length);
        parser.subjectPublicKey.writeTag(subjectPublicKey);

        subjectPublicKeyInfo.unref();
        subjectPublicKey.trimAtCursor();

        // Skip tag:
        // 03 82 01 0f (tag) 00 (padding byte)
        subjectPublicKey.trimHeader(5);// FIXME: parse it properly
        // * DEBUG */System.out.println("DEBUG: subjectPublicKey:\n" +
        // subjectPublicKey.dump());

        ntlmState.subjectPublicKey = subjectPublicKey.toByteArray();

        return ntlmState.subjectPublicKey;
    }

    /**
     * The client encrypts the public key it received from the server (contained
     * in the X.509 certificate) in the TLS handshake from step 1, by using the
     * confidentiality support of SPNEGO.
     *
     * The public key that is encrypted is the ASN.1-encoded SubjectPublicKey
     * sub-field of SubjectPublicKeyInfo from the X.509 certificate, as specified
     * in [RFC3280] section 4.1. The encrypted key is encapsulated in the
     * pubKeyAuth field of the TSRequest structure and is sent over the TLS
     * channel to the server.
     */
    private ByteBuffer generateMessageSignatureAndEncryptedServerPublicKey(NtlmState ntlmState) {
        return new ByteBuffer(ntlmState.ntlm_EncryptMessage(getServerPublicKey()));
    }

    public static ByteBuffer generateAuthenticateMessage(NtlmState ntlmState) {

        // Allocate memory for blocks from given fixed offset
        int blocksCursor = BLOCKS_OFFSET;

        ByteBuffer buf = new ByteBuffer(4096);

        // Signature: "NTLMSSP\0"
        buf.writeString(NTLMSSP, RdpConstants.CHARSET_8);
        buf.writeByte(0);

        // NTLM Message Type: NTLMSSP_AUTH (0x00000003)
        buf.writeIntLE(NtlmConstants.NTLMSSP_AUTH);

        // Although the protocol allows authentication to succeed if the client
        // provides either LmChallengeResponse or NtChallengeResponse, Windows
        // implementations provide both.

        // LM V2 response
        blocksCursor = writeBlock(buf, ntlmState.lmChallengeResponse, blocksCursor);

        // NT v2 response
        blocksCursor = writeBlock(buf, ntlmState.ntChallengeResponse, blocksCursor);

        // DomainName
        blocksCursor = writeStringBlock(buf, ntlmState.domain, blocksCursor, RdpConstants.CHARSET_16);

        // UserName
        blocksCursor = writeStringBlock(buf, ntlmState.user, blocksCursor, RdpConstants.CHARSET_16);

        // Workstation
        blocksCursor = writeStringBlock(buf, ntlmState.workstation, blocksCursor, RdpConstants.CHARSET_16);

        // EncryptedRandomSessionKey, 16 bytes
        blocksCursor = writeBlock(buf, ntlmState.encryptedRandomSessionKey, blocksCursor);

        // NegotiateFlags (4 bytes): In connection-oriented mode, a NEGOTIATE
        // structure that contains the set of bit flags (section 2.2.2.5) negotiated
        // in the previous messages.
        buf.writeIntLE(/*ntlmState.negotiatedFlags.value*/0xe288b235); // FIXME: remove hardcoded value

        buf.writeBytes(generateVersion());

        // If the CHALLENGE_MESSAGE TargetInfo field (section 2.2.1.2) has an
        // MsvAvTimestamp present, the client SHOULD provide a MIC(Message Integrity
        // Check)

        int savedCursorForMIC = buf.cursor; // Save cursor position to write MIC
        // later
        buf.writeBytes(new byte[16]); // Write 16 zeroes

        if (BLOCKS_OFFSET != buf.cursor)
            throw new RuntimeException("BUG: Actual offset of first byte of allocated blocks is not equal hardcoded offset. Hardcoded offset: " + BLOCKS_OFFSET
                    + ", actual offset: " + buf.cursor + ". Update hardcoded offset to match actual offset.");

        buf.cursor = blocksCursor;
        buf.trimAtCursor();

        ntlmState.authenticateMessage = buf.toByteArray();

        // Calculate and write MIC to reserved position
        ntlmState.ntlm_compute_message_integrity_check();
        buf.cursor = savedCursorForMIC;
        buf.writeBytes(ntlmState.messageIntegrityCheck);
        buf.rewindCursor();

        return buf;
    }

    /**
     * Write string as security buffer, using given charset, without trailing '\0'
     * character.
     */
    private static int writeStringBlock(ByteBuffer buf, String string, int blocksCursor, Charset charset) {
        return writeBlock(buf, string.getBytes(charset), blocksCursor);
    }

    /**
     * Write block to blocks buffer and block descriptor to main buffer.
     */
    private static int writeBlock(ByteBuffer buf, byte[] block, int blocksCursor) {

        // Write block descriptor

        // Length
        buf.writeShortLE(block.length);
        // Allocated
        buf.writeShortLE(block.length);
        // Offset
        buf.writeIntLE(blocksCursor);

        // Write block to position pointed by blocksCursor instead of buf.cursor
        int savedCursor = buf.cursor;
        buf.cursor = blocksCursor;
        buf.writeBytes(block);
        blocksCursor = buf.cursor;
        buf.cursor = savedCursor;

        return blocksCursor;
    }

    /**
     * Version (8 bytes): A VERSION structure (section 2.2.2.10) that is present
     * only when the NTLMSSP_NEGOTIATE_VERSION flag is set in the NegotiateFlags
     * field. This structure is used for debugging purposes only. In normal
     * protocol messages, it is ignored and does not affect the NTLM message
     * processing.
     */
    private static byte[] generateVersion() {
        // Version (6.1, Build 7601), NTLM current revision: 15
        return new byte[] {0x06, 0x01, (byte)0xb1, 0x1d, 0x00, 0x00, 0x00, 0x0f};
    }

    /**
     * Example.
     */
    public static void main(String args[]) {
        System.setProperty("streamer.Element.debug", "true");

        /* @formatter:off */
        //
        // Client NEGOTIATE
        //
        byte[] clientNegotiatePacket = new byte[] {
                (byte)0x30, (byte)0x37, (byte)0xa0, (byte)0x03, (byte)0x02, (byte)0x01, (byte)0x02, (byte)0xa1, (byte)0x30, (byte)0x30, (byte)0x2e, (byte)0x30, (byte)0x2c, (byte)0xa0, (byte)0x2a, (byte)0x04,
                (byte)0x28, (byte)0x4e, (byte)0x54, (byte)0x4c, (byte)0x4d, (byte)0x53, (byte)0x53, (byte)0x50, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xb7, (byte)0x82, (byte)0x08,
                (byte)0xe2, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x06, (byte)0x01, (byte)0xb1, (byte)0x1d, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0f,
        };

//
// Server CHALLENGE
//
        byte[] serverChallengePacket = new byte[] {
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
                0x35, (byte) 0x82, (byte) 0x8a, (byte) 0xe2, // NegotiateFlags: NEGOTIATE_56 NEGOTIATE_KEY_EXCH NEGOTIATE_128 NEGOTIATE_VERSION NEGOTIATE_TARGET_INFO NEGOTIATE_EXTENDED_SESSION_SECURITY TARGET_TYPE_SERVER NEGOTIATE_ALWAYS_SIGN NEGOTIATE_NTLM NEGOTIATE_SEAL NEGOTIATE_SIGN REQUEST_TARGET NEGOTIATE_UNICODE

                (byte)0xc1, (byte)0x4a, (byte)0xc8, (byte)0x98, (byte)0x2f, (byte)0xd1, (byte)0x93, (byte)0xd4, //  ServerChallenge
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
                (byte)0x1d, (byte)0xea, (byte)0x6b, (byte)0x60, (byte)0xf8, (byte)0xc5, (byte)0xce, (byte)0x01, // Time: Oct 10, 2013 23:36:20.056937300 EEST

                // Attribute: End of list
                0x00, 0x00,
                0x00, 0x00,

        };

//
// Client NTLMSSP_AUTH
//
        byte[] clientAuthPacket = new byte[] {
                0x30, (byte) 0x82, 0x03, 0x13, //  TAG: [UNIVERSAL 16] (constructed) "SEQUENCE" LEN: 787 bytes

                //
                // TSRequest.version
                //
                (byte) 0xa0, 0x03, //  TAG: [0] (constructed) LEN: 3 bytes
                0x02, 0x01, 0x02, //  TAG: [UNIVERSAL 2] (primitive) "INTEGER" LEN: 1 bytes

                //
                // TSRequest.negoData
                //
                (byte) 0xa1, (byte) 0x82, 0x01, (byte) 0xe4, //  TAG: [1] (constructed) LEN: 484 bytes
                0x30, (byte) 0x82, 0x01, (byte) 0xe0, //  TAG: [UNIVERSAL 16] (constructed) "SEQUENCE" LEN: 480 bytes
                0x30, (byte) 0x82, 0x01, (byte) 0xdc, //  TAG: [UNIVERSAL 16] (constructed) "SEQUENCE" LEN: 476 bytes

                //
                // NegoItem.negoToken
                //
                (byte) 0xa0, (byte) 0x82, 0x01, (byte) 0xd8, //  TAG: [0] (constructed) LEN: 472 bytes
                0x04, (byte) 0x82, 0x01, (byte) 0xd4,  // TAG: [UNIVERSAL 4] (primitive) "OCTET STRING" LEN: 468 bytes

                // NTLMSSP

                0x4e, 0x54, 0x4c, 0x4d, 0x53, 0x53, 0x50, 0x00, //  "NTLMSSP\0"

                0x03, 0x00, 0x00, 0x00, //  NTLM Message Type: NTLMSSP_AUTH (0x00000003)
                0x18, 0x00, 0x18, 0x00, (byte) 0x92, 0x00, 0x00, 0x00, //  LmChallengeResponse (length 24, allocated: 24, offset 146)
                0x1a, 0x01, 0x1a, 0x01, (byte) 0xaa, 0x00, 0x00, 0x00, //  NtChallengeResponse (length 282, allocated: 282, offset 170)
                0x12, 0x00, 0x12, 0x00, 0x58, 0x00, 0x00, 0x00,  // DomainName (length 18, allocated: 88, offset 88)
                0x1a, 0x00, 0x1a, 0x00, 0x6a, 0x00, 0x00, 0x00,  // UserName (length 26, allocated:26, offset 106)
                0x0e, 0x00, 0x0e, 0x00, (byte) 0x84, 0x00, 0x00, 0x00,  // Workstation (length 14, offset 132)
                0x10, 0x00, 0x10, 0x00, (byte) 0xc4, 0x01, 0x00, 0x00,  // EncryptedRandomSessionKey (length 16, offset 452)
                0x35, (byte) 0xb2, (byte) 0x88, (byte) 0xe2,   // NegotiateFlags
                0x06, 0x01, (byte) 0xb1, 0x1d, 0x00, 0x00, 0x00, 0x0f, //  Version (6.1, Build 7601), NTLM current revision: 15

                (byte)0x8c, (byte)0x69, (byte)0x53, (byte)0x1c, (byte)0xbb, (byte)0x6f, (byte)0xfb, (byte)0x9a, (byte)0x5d, (byte)0x2c, (byte)0x63, (byte)0xf2, (byte)0xc9, (byte)0x51, (byte)0xc5, (byte)0x11, // Message integrity check

                0x77, 0x00, 0x6f, 0x00, 0x72, 0x00, 0x6b, 0x00, 0x67, 0x00, 0x72, 0x00, 0x6f, 0x00, 0x75, 0x00, 0x70, 0x00, // Domain name value: "Workgroup"

                0x41, 0x00, 0x64, 0x00, 0x6d, 0x00, 0x69, 0x00, 0x6e, 0x00, 0x69, 0x00, 0x73, 0x00, 0x74, 0x00, 0x72, 0x00, 0x61, 0x00, 0x74, 0x00, 0x6f, 0x00, 0x72, 0x00,  // User name value: "Administrator"

                0x61, 0x00, 0x70, 0x00, 0x6f, 0x00, 0x6c, 0x00, 0x6c, 0x00, 0x6f, 0x00, 0x33, 0x00, //  Workstation host name value: "apollo3"

                // Lan manager challenge response value
                // Response: HMAC_MD(ResponseKeyLM, concatenate(ServerChallenge, ClientChallenge), where ResponseKeyLM=ntlmv2Hash(target, user, password)
                (byte)0x17, (byte)0x9b, (byte)0x7d, (byte)0x7b, (byte)0x2f, (byte)0x79, (byte)0x9f, (byte)0x19, (byte)0xa0, (byte)0x4b, (byte)0x00, (byte)0xed, (byte)0x2b, (byte)0x39, (byte)0xbb, (byte)0x23,
                // Client challenge (fixed for debugging)
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08,

                //
                // NTLM challenge response value:
                //

                (byte)0x49, (byte)0xea, (byte)0x27, (byte)0x4f, (byte)0xcc, (byte)0x05, (byte)0x8b, (byte)0x79, (byte)0x20, (byte)0x0b, (byte)0x08, (byte)0x42, (byte)0xa9, (byte)0xc8, (byte)0x0e, (byte)0xc7, //  HMAC

                0x01, 0x01, 0x00, 0x00, // Header: 0x00000101 (LE)
                0x00, 0x00, 0x00, 0x00, // Reserved: 0x00000000
                (byte)0x1d, (byte)0xea, (byte)0x6b, (byte)0x60, (byte)0xf8, (byte)0xc5, (byte)0xce, (byte)0x01, // Time: Oct 10, 2013 23:36:20.056937300 EEST
                (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, // Client challenge (fixed)
                0x00, 0x00, 0x00, 0x00, //  Reserved

                // Target Info value:

                // Attribute list

                0x02, 0x00, // Item Type: NetBIOS domain name (0x0002, LE)
                0x1e, 0x00, //  Item Length: 30 (LE)
                0x57, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x2d, 0x00, 0x4c, 0x00, 0x4f, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00, 0x42, 0x00, 0x32, 0x00, 0x4c, 0x00, 0x53, 0x00, 0x52, 0x00, 0x30, 0x00,  //  "WIN-LO419B2LSR0 "

                0x01, 0x00,  //  Item Type: NetBIOS computer name (0x0001, LE)
                0x1e, 0x00,  // Item Length: 30 (LE)
                0x57, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x2d, 0x00, 0x4c, 0x00, 0x4f, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00, 0x42, 0x00, 0x32, 0x00, 0x4c, 0x00, 0x53, 0x00, 0x52, 0x00, 0x30, 0x00, //  "WIN-LO419B2LSR0 "

                0x04, 0x00,  // Item Type: DNS domain name (0x0004, LE)
                0x1e, 0x00,  // Item Length: 30 (LE)
                0x57, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x2d, 0x00, 0x4c, 0x00, 0x4f, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00, 0x42, 0x00, 0x32, 0x00, 0x4c, 0x00, 0x53, 0x00, 0x52, 0x00, 0x30, 0x00, //  "WIN-LO419B2LSR0 "

                0x03, 0x00,  // Item Type: DNS computer name (0x0003, LE)
                0x1e, 0x00,  // Item Length: 30 (LE)
                0x57, 0x00, 0x49, 0x00, 0x4e, 0x00, 0x2d, 0x00, 0x4c, 0x00, 0x4f, 0x00, 0x34, 0x00, 0x31, 0x00, 0x39, 0x00, 0x42, 0x00, 0x32, 0x00, 0x4c, 0x00, 0x53, 0x00, 0x52, 0x00, 0x30, 0x00, //  "WIN-LO419B2LSR0 "

                0x07, 0x00,  // Item Type: Timestamp (0x0007, LE)
                0x08, 0x00,  // Item Length: 8 (LE)
                (byte)0x1d, (byte)0xea, (byte)0x6b, (byte)0x60, (byte)0xf8, (byte)0xc5, (byte)0xce, (byte)0x01, // Timestamp: Oct 10, 2013 23:36:20.056937300 EEST

                0x06, 0x00,  // Item Type: Flags (0x0006, LE)
                0x04, 0x00, // Item Length: 4 (LE)
                0x02, 0x00, 0x00, 0x00, // Flags: 0x00000002

                0x0a, 0x00, // Item Type: Channel Bindings (0x000a, LE)
                0x10, 0x00, // Item Length: 16 (LE)
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, // Channel Bindings: 00000000000000000000000000000000

                0x09, 0x00, // Item Type: Target Name (0x0009, LE)
                0x2a, 0x00, // Item Length: 42 (LE)
                0x54, 0x00, 0x45, 0x00, 0x52, 0x00, 0x4d, 0x00, 0x53, 0x00, 0x52, 0x00, 0x56, 0x00, 0x2f, 0x00, 0x31, 0x00, 0x39, 0x00, 0x32, 0x00, 0x2e, 0x00, 0x31, 0x00, 0x36, 0x00, 0x38, 0x00, 0x2e, 0x00, 0x30, 0x00, 0x2e, 0x00, 0x31, 0x00, 0x30, 0x00, 0x31, 0x00, // Target Name: "TERMSRV/192.168.0.101" (UTF-16)

                // Attribute: End of list
                0x00, 0x00,  //
                0x00, 0x00,  //
                // Attribute: End of list
                0x00, 0x00,  //
                0x00, 0x00,  //
                // Attribute: End of list
                0x00, 0x00,  //
                0x00, 0x00,  //
                // Attribute: End of list
                0x00, 0x00,  //
                0x00, 0x00,  //

                // Session Key
                // RC4 key (Server KeyExchangeKey or SessionBaseKey):
                // 6e bd e3 da 83 c2 fd f1 38 a2 78 be 8c e6 75 d6
                //
                // RC4 data (Client nonce):
                // 01 02 03 04 05 06 07 08 09 0a 0b 0c 0d 0e 0f 10
                //
                // RC4 encrypted:
                // 2c 24 da 10 17 cf 40 69 35 49 6f 58 e1 29 9e 79
                (byte)0x2c, (byte)0x24, (byte)0xda, (byte)0x10, (byte)0x17, (byte)0xcf, (byte)0x40, (byte)0x69, (byte)0x35, (byte)0x49, (byte)0x6f, (byte)0x58, (byte)0xe1, (byte)0x29, (byte)0x9e, (byte)0x79,

                //
                // TSRequest.publicKey
                //
                (byte) 0xa3, (byte) 0x82, 0x01, 0x22, // TAG: [3] (constructed) LEN: 290 bytes
                0x04, (byte) 0x82, 0x01, 0x1e,  // TAG: [UNIVERSAL 4] (primitive) "OCTET STRING" LEN: 286 bytes

                // NTLMSSP_MESSAGE_SIGNATURE, @see http://msdn.microsoft.com/en-us/library/cc422952.aspx

                // Version: 0x00000001
                (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,

                // Checksum (8 bytes): An 8-byte array that contains the checksum for the message.
                (byte)0x72, (byte)0x76, (byte)0x1e, (byte)0x57, (byte)0x49, (byte)0xb5, (byte)0x0f, (byte)0xad,

                // seqNum of the message: 0
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,

                // Encrypted public key
                (byte)0x15, (byte)0xf7, (byte)0xf2, (byte)0x54, (byte)0xda, (byte)0xa9, (byte)0xe5, (byte)0xad, (byte)0x85, (byte)0x04, (byte)0x67, (byte)0x4d, (byte)0x0b, (byte)0xcb, (byte)0xf9, (byte)0xb1,
                (byte)0xf8, (byte)0x02, (byte)0x8a, (byte)0x77, (byte)0xc2, (byte)0x63, (byte)0xab, (byte)0xd5, (byte)0x74, (byte)0x23, (byte)0x9f, (byte)0x9d, (byte)0x5d, (byte)0x1f, (byte)0xd3, (byte)0xb3,
                (byte)0xa0, (byte)0xac, (byte)0x16, (byte)0x8a, (byte)0x4b, (byte)0x08, (byte)0xf5, (byte)0x47, (byte)0x70, (byte)0x58, (byte)0x10, (byte)0xb4, (byte)0xe7, (byte)0x87, (byte)0xb3, (byte)0x4b,
                (byte)0xc9, (byte)0xa2, (byte)0xd5, (byte)0xd1, (byte)0xca, (byte)0x0f, (byte)0xd4, (byte)0xe3, (byte)0x8d, (byte)0x76, (byte)0x5a, (byte)0x60, (byte)0x28, (byte)0xf8, (byte)0x06, (byte)0x5d,
                (byte)0xe4, (byte)0x7e, (byte)0x21, (byte)0xc8, (byte)0xbb, (byte)0xac, (byte)0xe5, (byte)0x79, (byte)0x85, (byte)0x30, (byte)0x9b, (byte)0x88, (byte)0x13, (byte)0x2f, (byte)0x8f, (byte)0xfc,
                (byte)0x04, (byte)0x52, (byte)0xfe, (byte)0x87, (byte)0x94, (byte)0xcf, (byte)0xcb, (byte)0x49, (byte)0x4a, (byte)0xda, (byte)0x6f, (byte)0xdd, (byte)0xee, (byte)0x57, (byte)0xa5, (byte)0xe4,
                (byte)0x4d, (byte)0x0e, (byte)0x5c, (byte)0x3d, (byte)0x0b, (byte)0x63, (byte)0x1f, (byte)0xf6, (byte)0x3d, (byte)0x1b, (byte)0xae, (byte)0x5a, (byte)0xf6, (byte)0x42, (byte)0x2a, (byte)0x46,
                (byte)0xfa, (byte)0x42, (byte)0x71, (byte)0x67, (byte)0x46, (byte)0x02, (byte)0x71, (byte)0xea, (byte)0x51, (byte)0x98, (byte)0xf7, (byte)0xd4, (byte)0x43, (byte)0xbf, (byte)0x8e, (byte)0xe8,
                (byte)0x3c, (byte)0xc8, (byte)0xfa, (byte)0x79, (byte)0x9d, (byte)0x8c, (byte)0xfc, (byte)0xc2, (byte)0x42, (byte)0xc9, (byte)0xbb, (byte)0xd0, (byte)0xab, (byte)0x81, (byte)0xc4, (byte)0x53,
                (byte)0xfd, (byte)0x41, (byte)0xda, (byte)0xab, (byte)0x0f, (byte)0x25, (byte)0x79, (byte)0x5f, (byte)0xbd, (byte)0xa3, (byte)0x8c, (byte)0xd3, (byte)0xf5, (byte)0x1b, (byte)0xab, (byte)0x20,
                (byte)0xd1, (byte)0xf4, (byte)0xd8, (byte)0x81, (byte)0x9c, (byte)0x18, (byte)0x4a, (byte)0xa4, (byte)0x77, (byte)0xee, (byte)0xe1, (byte)0x51, (byte)0xee, (byte)0x2a, (byte)0xc1, (byte)0x94,
                (byte)0x37, (byte)0xc5, (byte)0x06, (byte)0x7a, (byte)0x3f, (byte)0x0f, (byte)0x25, (byte)0x5b, (byte)0x4e, (byte)0x6a, (byte)0xdc, (byte)0x0b, (byte)0x62, (byte)0x6f, (byte)0x12, (byte)0x83,
                (byte)0x03, (byte)0xae, (byte)0x4e, (byte)0xce, (byte)0x2b, (byte)0x6e, (byte)0xd4, (byte)0xd5, (byte)0x23, (byte)0x27, (byte)0xf6, (byte)0xa6, (byte)0x38, (byte)0x67, (byte)0xec, (byte)0x95,
                (byte)0x82, (byte)0xc6, (byte)0xba, (byte)0xd4, (byte)0xf6, (byte)0xe6, (byte)0x22, (byte)0x7d, (byte)0xb9, (byte)0xe4, (byte)0x81, (byte)0x97, (byte)0x24, (byte)0xff, (byte)0x40, (byte)0xb2,
                (byte)0x42, (byte)0x3c, (byte)0x11, (byte)0x24, (byte)0xd0, (byte)0x3a, (byte)0x96, (byte)0xd9, (byte)0xc1, (byte)0x13, (byte)0xd6, (byte)0x62, (byte)0x45, (byte)0x21, (byte)0x60, (byte)0x5b,
                (byte)0x7b, (byte)0x2b, (byte)0x62, (byte)0x44, (byte)0xf7, (byte)0x40, (byte)0x93, (byte)0x29, (byte)0x5b, (byte)0x44, (byte)0xb7, (byte)0xda, (byte)0x9c, (byte)0xa6, (byte)0xa9, (byte)0x3b,
                (byte)0xe1, (byte)0x3b, (byte)0x9d, (byte)0x31, (byte)0xf2, (byte)0x21, (byte)0x53, (byte)0x0f, (byte)0xb3, (byte)0x70, (byte)0x55, (byte)0x84, (byte)0x2c, (byte)0xb4,
        };

        SSLState sslState = new SSLState();

        sslState.serverCertificateSubjectPublicKeyInfo = new byte[] {
                0x30, (byte) 0x82, 0x01, 0x22, // Sequence, length: 290 bytes
                0x30, 0x0d, // Sequence, length: 13 bytes {
                0x06, 0x09, // Object ID, length: 9 bytes
                0x2a, (byte) 0x86, 0x48, (byte) 0x86, (byte) 0xf7, 0x0d, 0x01, 0x01, 0x01,
                0x05, 0x00, // NULL, length: 0 bytes

                (byte)0x03, (byte)0x82, (byte)0x01, (byte)0x0f, // Bit string, length: 271 bytes

                (byte)0x00, // Padding
                (byte)0x30, (byte)0x82, (byte)0x01, (byte)0x0a, // Sequence
                (byte)0x02, (byte)0x82, (byte)0x01, (byte)0x01, // Integer, length: 257 bytes

                (byte)0x00, (byte)0xa8, (byte)0x56,
                (byte)0x65, (byte)0xd3, (byte)0xce, (byte)0x8a, (byte)0x54, (byte)0x4d, (byte)0x9d, (byte)0xb0,
                (byte)0x84, (byte)0x31, (byte)0x19, (byte)0x71, (byte)0x7f, (byte)0xdd, (byte)0x42, (byte)0xfb,
                (byte)0x2a, (byte)0x7a, (byte)0x72, (byte)0x13, (byte)0xa1, (byte)0xb9, (byte)0x72, (byte)0xbb,
                (byte)0xd3, (byte)0x08, (byte)0xad, (byte)0x7d, (byte)0x6c, (byte)0x15, (byte)0x65, (byte)0x03,
                (byte)0xd1, (byte)0xc4, (byte)0x54, (byte)0xc5, (byte)0x33, (byte)0x6b, (byte)0x7d, (byte)0x69,
                (byte)0x89, (byte)0x5e, (byte)0xfe, (byte)0xe0, (byte)0x01, (byte)0xc0, (byte)0x7e, (byte)0x9b,
                (byte)0xcb, (byte)0x5d, (byte)0x65, (byte)0x36, (byte)0xcd, (byte)0x77, (byte)0x5d, (byte)0xf3,
                (byte)0x7a, (byte)0x5b, (byte)0x29, (byte)0x44, (byte)0x72, (byte)0xd5, (byte)0x38, (byte)0xe2,
                (byte)0xcf, (byte)0xb1, (byte)0xc7, (byte)0x78, (byte)0x9b, (byte)0x58, (byte)0xb9, (byte)0x17,
                (byte)0x7c, (byte)0xb7, (byte)0xd6, (byte)0xc7, (byte)0xc7, (byte)0xbf, (byte)0x90, (byte)0x4e,
                (byte)0x7c, (byte)0x39, (byte)0x93, (byte)0xcb, (byte)0x2e, (byte)0xe0, (byte)0xc2, (byte)0x33,
                (byte)0x2d, (byte)0xa5, (byte)0x7e, (byte)0xe0, (byte)0x7b, (byte)0xb6, (byte)0xf9, (byte)0x91,
                (byte)0x32, (byte)0xb7, (byte)0xd4, (byte)0x85, (byte)0xb7, (byte)0x35, (byte)0x2d, (byte)0x2b,
                (byte)0x00, (byte)0x6d, (byte)0xf8, (byte)0xea, (byte)0x8c, (byte)0x97, (byte)0x5f, (byte)0x51,
                (byte)0x1d, (byte)0x68, (byte)0x04, (byte)0x3c, (byte)0x79, (byte)0x14, (byte)0x71, (byte)0xa7,
                (byte)0xc7, (byte)0xd7, (byte)0x70, (byte)0x7a, (byte)0xe0, (byte)0xba, (byte)0x12, (byte)0x69,
                (byte)0xc8, (byte)0xd3, (byte)0xd9, (byte)0x4e, (byte)0xab, (byte)0x51, (byte)0x47, (byte)0xa3,
                (byte)0xec, (byte)0x99, (byte)0xd4, (byte)0x88, (byte)0xca, (byte)0xda, (byte)0xc2, (byte)0x7f,
                (byte)0x79, (byte)0x4b, (byte)0x66, (byte)0xed, (byte)0x87, (byte)0xbe, (byte)0xc2, (byte)0x5f,
                (byte)0xea, (byte)0xcf, (byte)0xe1, (byte)0xb5, (byte)0xf0, (byte)0x3d, (byte)0x9b, (byte)0xf2,
                (byte)0x19, (byte)0xc3, (byte)0xe0, (byte)0xe1, (byte)0x7a, (byte)0x45, (byte)0x71, (byte)0x12,
                (byte)0x3d, (byte)0x72, (byte)0x1d, (byte)0x6f, (byte)0x2b, (byte)0x1c, (byte)0x46, (byte)0x68,
                (byte)0xc0, (byte)0x8f, (byte)0x4f, (byte)0xce, (byte)0x3a, (byte)0xc5, (byte)0xcd, (byte)0x22,
                (byte)0x65, (byte)0x2d, (byte)0x43, (byte)0xb0, (byte)0x5c, (byte)0xdd, (byte)0x89, (byte)0xae,
                (byte)0xbe, (byte)0x70, (byte)0x59, (byte)0x5e, (byte)0x0c, (byte)0xbd, (byte)0xf5, (byte)0x46,
                (byte)0x82, (byte)0x1e, (byte)0xe4, (byte)0x86, (byte)0x95, (byte)0x7b, (byte)0x60, (byte)0xae,
                (byte)0x45, (byte)0x50, (byte)0xc2, (byte)0x54, (byte)0x08, (byte)0x49, (byte)0x9a, (byte)0x9e,
                (byte)0xfb, (byte)0xb2, (byte)0xb6, (byte)0x78, (byte)0xe5, (byte)0x2f, (byte)0x9c, (byte)0x5a,
                (byte)0xd0, (byte)0x8a, (byte)0x03, (byte)0x77, (byte)0x68, (byte)0x30, (byte)0x93, (byte)0x78,
                (byte)0x6d, (byte)0x90, (byte)0x6d, (byte)0x50, (byte)0xfa, (byte)0xa7, (byte)0x65, (byte)0xfe,
                (byte)0x59, (byte)0x33, (byte)0x27, (byte)0x4e, (byte)0x4b, (byte)0xf8, (byte)0x38, (byte)0x44,
                (byte)0x3a, (byte)0x12, (byte)0xf4, (byte)0x07, (byte)0xa0, (byte)0x8d, (byte)0x02, (byte)0x03,
                (byte)0x01, (byte)0x00, (byte)0x01,
        };
        /* @formatter:on */

        NtlmState ntlmState = new NtlmState();
        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(serverChallengePacket, new byte[] {1, 2, 3}));
        Element ntlmssp_negotiate = new ClientNtlmsspNegotiate("ntlmssp_negotiate", ntlmState);
        Element ntlmssp_challenge = new ServerNtlmsspChallenge("ntlmssp_challenge", ntlmState);
        Element ntlmssp_auth = new ClientNtlmsspPubKeyAuth("ntlmssp_auth", ntlmState, sslState, "192.168.0.101", "workgroup", "apollo3", "Administrator",
                "R2Preview!");
        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(clientNegotiatePacket, clientAuthPacket), (Dumper)ntlmssp_auth);
        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, ntlmssp_negotiate, ntlmssp_challenge, ntlmssp_auth, sink, mainSink);
        pipeline.link("source", "ntlmssp_negotiate", "ntlmssp_challenge", "ntlmssp_auth", "mainSink");
        pipeline.link("ntlmssp_negotiate >" + OTOUT, "ntlmssp_negotiate< sink");
        pipeline.link("ntlmssp_challenge >" + OTOUT, "ntlmssp_challenge< sink");
        pipeline.link("ntlmssp_auth >" + OTOUT, "ntlmssp_auth< sink");
        pipeline.runMainLoop("source", STDOUT, false, false);

    }

    @Override
    public void dump(ByteBuffer buf) {
        buf.rewindCursor();
        TSRequest request = new TSRequest("TSRequest");
        request.readTag(buf);
        System.out.println("TSRequest version: " + request.version.value);
        System.out.println("TSRequest pubKey: " + request.pubKeyAuth.value.toPlainHexString());

        ByteBuffer negoToken = ((NegoItem)request.negoTokens.tags[0]).negoToken.value;
        System.out.println("TSRequest negotoken: " + negoToken.toPlainHexString());
        dumpNegoToken(negoToken);

        negoToken.unref();
    }

    private void dumpNegoToken(ByteBuffer buf) {
        String signature = buf.readVariableString(RdpConstants.CHARSET_8);
        if (!ConstantTimeComparator.compareStrings(signature, NTLMSSP))
            throw new RuntimeException("Unexpected NTLM message singature: \"" + signature + "\". Expected signature: \"" + NTLMSSP + "\". Data: " + buf + ".");

        // MessageType (CHALLENGE)
        int messageType = buf.readSignedIntLE();
        if (messageType != NtlmConstants.NTLMSSP_AUTH)
            throw new RuntimeException("Unexpected NTLM message type: " + messageType + ". Expected type: CHALLENGE (" + NtlmConstants.CHALLENGE + "). Data: " + buf
                    + ".");

        System.out.println("lmChallengeResponseFields: " + ServerNtlmsspChallenge.readBlockByDescription(buf).toPlainHexString());
        ByteBuffer ntChallengeResponseBuf = ServerNtlmsspChallenge.readBlockByDescription(buf);
        System.out.println("NtChallengeResponse: " + ntChallengeResponseBuf.toPlainHexString());
        System.out.println("DomainName: " + ServerNtlmsspChallenge.readStringByDescription(buf));
        System.out.println("UserName: " + ServerNtlmsspChallenge.readStringByDescription(buf));
        System.out.println("Workstation: " + ServerNtlmsspChallenge.readStringByDescription(buf));
        System.out.println("EncryptedRandomSessionKey: " + ServerNtlmsspChallenge.readBlockByDescription(buf).toPlainHexString());
        System.out.println("NegotiateFlags: " + new NegoFlags(buf.readSignedIntLE()));
        System.out.println("Version: " + buf.readBytes(8).toPlainHexString());

        dumpNtChallengeResponse(ntChallengeResponseBuf);
    }

    private void dumpNtChallengeResponse(ByteBuffer buf) {
        System.out.println("HMAC: " + buf.readBytes(16).toPlainHexString());
        System.out.format("Header: 0x%08x\n", buf.readUnsignedIntLE());
        System.out.format("Reserved: 0x%08x\n", buf.readUnsignedIntLE());
        System.out.println("Time: " + buf.readBytes(8).toPlainHexString());
        System.out.println("Client challenge: " + buf.readBytes(8).toPlainHexString());
        System.out.format("Reserved: 0x%08x\n", buf.readUnsignedIntLE());

        // Parse attribute list

        while (buf.remainderLength() > 0) {
            int type = buf.readUnsignedShortLE();
            int length = buf.readUnsignedShortLE();

            if (type == MSV_AV_EOL)
                // End of list
                break;

            ByteBuffer data = buf.readBytes(length);
            switch (type) {
            case MSV_AV_NETBIOS_DOMAIN_NAME:
                System.out.println("AV Netbios Domain name: " + data.readString(length, RdpConstants.CHARSET_16));
                break;
            case MSV_AV_NETBIOS_COMPUTER_NAME:
                System.out.println("AV Netbios Computer name: " + data.readString(length, RdpConstants.CHARSET_16));
                break;
            case MSV_AV_DNS_DOMAIN_NAME:
                System.out.println("AV DNS Domain name: " + data.readString(length, RdpConstants.CHARSET_16));
                break;
            case MSV_AV_DNS_COMPUTER_NAME:
                System.out.println("AV DNS Computer name: " + data.readString(length, RdpConstants.CHARSET_16));
                break;
            case MSV_AV_CHANNEL_BINDINGS:
                System.out.println("AV Channel Bindings: " + data.readBytes(length).toPlainHexString());
                break;
            case MSV_AV_TIMESTAMP:
                System.out.println("AV Timestamp: " + data.readBytes(length).toPlainHexString());
                break;
            case MSV_AV_FLAGS:
                System.out.println("AV Flags: " + data.readBytes(length).toPlainHexString());
                break;
            case MSV_AV_TARGET_NAME:
                System.out.println("AV Target Name: " + data.readString(length, RdpConstants.CHARSET_16));
                break;
            default:
                System.out.println("Unknown NTLM target info attribute: " + type + ". Data: " + data + ".");
            }
            data.unref();
        }

    }
}
