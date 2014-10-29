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

import java.util.Arrays;

import javax.crypto.Cipher;

import rdpclient.rdp.RdpConstants;
import streamer.ByteBuffer;

public class NtlmState implements NtlmConstants {

    /**
     * The set of configuration flags (section 2.2.2.5) that specifies the
     * negotiated capabilities of the client and server for the current NTLM
     * session.
     */
    public NegoFlags negotiatedFlags;

    /**
     * Target Information extracted from Type2 server response.
     */
    public byte[] serverTargetInfo;

    /**
     * Challenge extracted from Type2 server response.
     */
    public byte[] serverChallenge;

    public byte[] clientChallenge;

    public byte[] keyExchangeKey;

    /**
     * A 128-bit (16-byte) session key used to derive ClientSigningKey,
     * ClientSealingKey, ServerSealingKey, and ServerSigningKey.
     */
    public byte[] exportedSessionKey;

    /**
     * The signing key used by the client to sign messages and used by the server
     * to verify signed client messages. It is generated after the client is
     * authenticated by the server and is not passed over the wire.
     */
    public byte[] clientSigningKey;

    /**
     * The sealing key used by the client to seal messages and used by the server
     * to unseal client messages. It is generated after the client is
     * authenticated by the server and is not passed over the wire.
     */
    public byte[] clientSealingKey;

    public byte[] encryptedRandomSessionKey;

    public byte[] sessionBaseKey;

    public byte[] responseKeyNT;

    public byte[] ntProofStr1;

    public String domain;

    public String user;

    public String workstation;

    public String password;

    public String serverNetbiosDomainName;

    public String serverNetbiosComputerName;

    public String serverDnsDomainName;

    public String serverDnsComputerName;

    public String serverDnsTreeName;

    public String serverTargetName;

    public byte[] serverTimestamp;
    public byte[] clientChallengeTimestamp;

    public byte[] lmChallengeResponse;

    public byte[] ntChallengeResponse;

    public byte[] ntProofStr2;

    public byte[] randomSessionKey;

    public byte[] serverSigningKey;

    public byte[] serverSealingKey;

    public byte[] sendSigningKey;

    public byte[] recvSigningKey;

    public byte[] sendSealingKey;

    public byte[] recvSealingKey;

    public Cipher sendRc4Seal;

    public Cipher recvRc4Seal;

    public byte[] messageIntegrityCheck;

    public byte[] negotiateMessage;

    public byte[] challengeMessage;

    public byte[] authenticateMessage;

    /**
     * A 4-byte sequence number.
     *
     * In the case of connection-oriented authentication, the SeqNum parameter
     * MUST start at 0 and is incremented by one for each message sent. The
     * receiver expects the first received message to have SeqNum equal to 0, and
     * to be one greater for each subsequent message received. If a received
     * message does not contain the expected SeqNum, an error MUST be returned to
     * the receiving application, and SeqNum is not incremented.
     */
    public int sendSeqNum;
    public int recvSeqNum;

    public byte[] authenticateTargetInfo;

    public String servicePrincipalName;

    private byte[] channelBindingsHash = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public byte[] subjectPublicKey;

    public byte[] ntlm_generate_timestamp() {
        clientChallengeTimestamp = serverTimestamp;
        return clientChallengeTimestamp;
    }

    /**
     * MD4(UNICODE(Password))
     */
    public byte[] NTOWFv1W(String password) {
        return CryptoAlgos.MD4(password.getBytes(RdpConstants.CHARSET_16));
    }

    public void testNTOWFv1W() {
        byte[] expected = new byte[] {(byte)0x25, (byte)0xf3, (byte)0x39, (byte)0xc9, (byte)0x86, (byte)0xb5, (byte)0xc2, (byte)0x6f, (byte)0xdc,
                (byte)0xab, (byte)0x91, (byte)0x34, (byte)0x93, (byte)0xa2, (byte)0x18, (byte)0x2a};
        byte[] actual = NTOWFv1W("R2Preview!");
        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    /**
     * HMAC_MD5(NTOWFv1W(Password), UNICODE(ConcatenationOf(UpperCase(User),
     * Domain)))
     */
    public byte[] NTOWFv2W(String password, String user, String domain) {
        return CryptoAlgos.HMAC_MD5(NTOWFv1W(password), (user.toUpperCase() + domain).getBytes(RdpConstants.CHARSET_16));
    }

    public void testNTOWFv2W() {
        byte[] expected = new byte[] {(byte)0x5f, (byte)0xcc, (byte)0x4c, (byte)0x48, (byte)0x74, (byte)0x6b, (byte)0x94, (byte)0xce, (byte)0xb7,
                (byte)0xae, (byte)0xf1, (byte)0x0d, (byte)0xc9, (byte)0x11, (byte)0x22, (byte)0x8f,};
        byte[] actual = NTOWFv2W("R2Preview!", "Administrator", "workgroup");
        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_compute_ntlm_v2_hash() {
        return NTOWFv2W(password, user, domain);
    }

    public byte[] ntlm_generate_client_challenge() {
        if (clientChallenge == null) {
            clientChallenge = CryptoAlgos.NONCE(8);
        }
        return clientChallenge;
    }

    public byte[] ntlm_compute_lm_v2_response() {
        if (lmChallengeResponse == null) {

            byte[] ntlm_v2_hash = ntlm_compute_ntlm_v2_hash();

            ntlm_generate_client_challenge();

            byte[] challenges = CryptoAlgos.concatenationOf(serverChallenge, clientChallenge);

            lmChallengeResponse = CryptoAlgos.concatenationOf(CryptoAlgos.HMAC_MD5(ntlm_v2_hash, challenges), clientChallenge);
        }

        return lmChallengeResponse;
    }

    public void testComputeLmV2Response() {
        serverChallenge = new byte[] {(byte)0x34, (byte)0xe4, (byte)0x4c, (byte)0xd5, (byte)0x75, (byte)0xe3, (byte)0x43, (byte)0x0f};
        clientChallenge = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        password = "R2Preview!";
        user = "Administrator";
        domain = "workgroup";
        byte[] expected = new byte[] {(byte)0xa8, (byte)0xae, (byte)0xd7, (byte)0x46, (byte)0x06, (byte)0x32, (byte)0x02, (byte)0x35, (byte)0x1d,
                (byte)0x95, (byte)0x99, (byte)0x36, (byte)0x20, (byte)0x36, (byte)0xac, (byte)0xc3, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04,
                (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08,};
        byte[] actual = ntlm_compute_lm_v2_response();
        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] computeNtProofStr(byte[] ntlmV2Hash, byte[] data) {
        return CryptoAlgos.HMAC_MD5(ntlmV2Hash, data);
    }

    public void testComputeNtProofStr() {
        byte[] ntlm_v2_hash = new byte[] {(byte)0x5f, (byte)0xcc, (byte)0x4c, (byte)0x48, (byte)0x74, (byte)0x6b, (byte)0x94, (byte)0xce, (byte)0xb7,
                (byte)0xae, (byte)0xf1, (byte)0x0d, (byte)0xc9, (byte)0x11, (byte)0x22, (byte)0x8f,};
        byte[] data = new byte[] {(byte)0x4a, (byte)0x25, (byte)0x50, (byte)0xa5, (byte)0x11, (byte)0x9b, (byte)0xd6, (byte)0x16, (byte)0x01,
                (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xa0, (byte)0xe8, (byte)0x85, (byte)0x2c,
                (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07,
                (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00,
                (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34,
                (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00,
                (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57,
                (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00,
                (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c,
                (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x1e, (byte)0x00,
                (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f,
                (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00,
                (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x1e,
                (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00,
                (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32,
                (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x07, (byte)0x00,
                (byte)0x08, (byte)0x00, (byte)0xa0, (byte)0xe8, (byte)0x85, (byte)0x2c, (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01, (byte)0x06,
                (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0a, (byte)0x00, (byte)0x10, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x09, (byte)0x00, (byte)0x26, (byte)0x00, (byte)0x54, (byte)0x00,
                (byte)0x45, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x4d, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x56,
                (byte)0x00, (byte)0x2f, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x2e, (byte)0x00,
                (byte)0x31, (byte)0x00, (byte)0x36, (byte)0x00, (byte)0x38, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x2e,
                (byte)0x00, (byte)0x33, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,};

        byte[] expected = new byte[] {(byte)0x19, (byte)0x4b, (byte)0xeb, (byte)0xad, (byte)0xda, (byte)0x24, (byte)0xd5, (byte)0x96, (byte)0x85,
                (byte)0x2e, (byte)0x24, (byte)0x94, (byte)0xd6, (byte)0x4a, (byte)0xb8, (byte)0x5e,};
        byte[] actual = computeNtProofStr(ntlm_v2_hash, data);
        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] computeSessionBaseKey(byte[] ntlmV2Hash, byte[] ntProofStr) {
        return CryptoAlgos.HMAC_MD5(ntlmV2Hash, ntProofStr);
    }

    public void testComputeSessionBaseKey() {
        byte[] ntlm_v2_hash = new byte[] {(byte)0x5f, (byte)0xcc, (byte)0x4c, (byte)0x48, (byte)0x74, (byte)0x6b, (byte)0x94, (byte)0xce, (byte)0xb7,
                (byte)0xae, (byte)0xf1, (byte)0x0d, (byte)0xc9, (byte)0x11, (byte)0x22, (byte)0x8f,};
        byte[] nt_proof_str = new byte[] {(byte)0x19, (byte)0x4b, (byte)0xeb, (byte)0xad, (byte)0xda, (byte)0x24, (byte)0xd5, (byte)0x96, (byte)0x85,
                (byte)0x2e, (byte)0x24, (byte)0x94, (byte)0xd6, (byte)0x4a, (byte)0xb8, (byte)0x5e,};

        byte[] expected = new byte[] {(byte)0x8e, (byte)0x0f, (byte)0xdd, (byte)0x12, (byte)0x4c, (byte)0x3b, (byte)0x11, (byte)0x7f, (byte)0x22,
                (byte)0xb9, (byte)0x4b, (byte)0x59, (byte)0x52, (byte)0xbc, (byte)0xa7, (byte)0x18,};
        byte[] actual = computeSessionBaseKey(ntlm_v2_hash, nt_proof_str);
        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public String generateServicePrincipalName(String serverHostName) {
        servicePrincipalName = GSS_RDP_SERVICE_NAME + "/" + serverHostName;
        return servicePrincipalName;
    }

    public void writeAVPair(ByteBuffer buf, int avPairType, byte[] value) {
        if (value != null) {
            buf.writeShortLE(avPairType);
            buf.writeShortLE(value.length);
            buf.writeBytes(value);
        }
    }

    public void writeAVPair(ByteBuffer buf, int avPairType, String value) {
        if (value != null) {
            writeAVPair(buf, avPairType, value.getBytes(RdpConstants.CHARSET_16));
        }
    }

    public byte[] ntlm_construct_authenticate_target_info() {
        ByteBuffer buf = new ByteBuffer(4096);

        writeAVPair(buf, MSV_AV_NETBIOS_DOMAIN_NAME, serverNetbiosDomainName);

        writeAVPair(buf, MSV_AV_NETBIOS_COMPUTER_NAME, serverNetbiosComputerName);

        writeAVPair(buf, MSV_AV_DNS_DOMAIN_NAME, serverDnsDomainName);

        writeAVPair(buf, MSV_AV_DNS_COMPUTER_NAME, serverDnsComputerName);

        writeAVPair(buf, MSV_AV_DNS_TREE_NAME, serverDnsTreeName);

        writeAVPair(buf, MSV_AV_TIMESTAMP, serverTimestamp);

        byte[] flags = new byte[] {(byte)MSV_AV_FLAGS_MESSAGE_INTEGRITY_CHECK, 0, 0, 0};
        writeAVPair(buf, MSV_AV_FLAGS, flags);

        writeAVPair(buf, MSV_AV_CHANNEL_BINDINGS, channelBindingsHash);

        writeAVPair(buf, MSV_AV_TARGET_NAME, servicePrincipalName);

        writeAVPair(buf, MSV_AV_EOL, "");
        // DEBUG: put EOL 4 times, for compatibility with FreeRDP output
        //*DEBUG*/writeAVPair(buf, MSV_AV_EOL, "");
        //*DEBUG*/writeAVPair(buf, MSV_AV_EOL, "");
        //*DEBUG*/writeAVPair(buf, MSV_AV_EOL, "");
        buf.trimAtCursor();

        authenticateTargetInfo = buf.toByteArray();
        buf.unref();

        return authenticateTargetInfo;
    }

    public void testConstructAuthenticateTargetInfo() {
        serverNetbiosDomainName = "WIN-LO419B2LSR0";
        serverNetbiosComputerName = "WIN-LO419B2LSR0";
        serverDnsDomainName = "WIN-LO419B2LSR0";
        serverDnsComputerName = "WIN-LO419B2LSR0";
        serverTimestamp = new byte[] {(byte)0xa0, (byte)0xe8, (byte)0x85, (byte)0x2c, (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01,};
        servicePrincipalName = "TERMSRV/192.168.1.3";

        byte[] expected = new byte[] {(byte)0x02, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e,
                (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00,
                (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52,
                (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00,
                (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31,
                (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00,
                (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49,
                (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00,
                (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53,
                (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00,
                (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34,
                (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00,
                (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0xa0,
                (byte)0xe8, (byte)0x85, (byte)0x2c, (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01, (byte)0x06, (byte)0x00, (byte)0x04, (byte)0x00,
                (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0a, (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x09, (byte)0x00, (byte)0x26, (byte)0x00, (byte)0x54, (byte)0x00, (byte)0x45, (byte)0x00, (byte)0x52,
                (byte)0x00, (byte)0x4d, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x56, (byte)0x00, (byte)0x2f, (byte)0x00,
                (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x36,
                (byte)0x00, (byte)0x38, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x33, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,};
        byte[] actual = ntlm_construct_authenticate_target_info();
        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_compute_ntlm_v2_response() {
        ByteBuffer buf = new ByteBuffer(4096);

        byte[] ntlm_v2_hash = ntlm_compute_ntlm_v2_hash();

        buf.writeByte(0x1); // RespType
        buf.writeByte(0x1); // HighRespType
        buf.writeShort(0); // reserved
        buf.writeInt(0); // reserved
        buf.writeBytes(clientChallengeTimestamp); // Timestamp, 8 bytes
        buf.writeBytes(clientChallenge); // Client nonce, 8 bytes
        buf.writeInt(0); // reserved
        buf.writeBytes(authenticateTargetInfo); // Target Info block
        buf.trimAtCursor();
        byte[] bufBytes = buf.toByteArray();
        buf.unref();

        ntProofStr2 = computeNtProofStr(ntlm_v2_hash, CryptoAlgos.concatenationOf(serverChallenge, bufBytes));

        ntChallengeResponse = CryptoAlgos.concatenationOf(ntProofStr2, bufBytes);

        sessionBaseKey = computeSessionBaseKey(ntlm_v2_hash, ntProofStr2);

        return ntChallengeResponse;
    }

    public void testComputeNtlmV2Response() {
        serverChallenge = new byte[] {(byte)0x4a, (byte)0x25, (byte)0x50, (byte)0xa5, (byte)0x11, (byte)0x9b, (byte)0xd6, (byte)0x16,};
        clientChallenge = new byte[] {1, 2, 3, 4, 5, 6, 7, 8};
        password = "R2Preview!";
        user = "Administrator";
        domain = "workgroup";
        clientChallengeTimestamp = new byte[] {(byte)0xa0, (byte)0xe8, (byte)0x85, (byte)0x2c, (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01,};
        authenticateTargetInfo = new byte[] {(byte)0x02, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e,
                (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00,
                (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52,
                (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00,
                (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31,
                (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00,
                (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49,
                (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00,
                (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53,
                (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00,
                (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34,
                (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00,
                (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0xa0,
                (byte)0xe8, (byte)0x85, (byte)0x2c, (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01, (byte)0x06, (byte)0x00, (byte)0x04, (byte)0x00,
                (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0a, (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x09, (byte)0x00, (byte)0x26, (byte)0x00, (byte)0x54, (byte)0x00, (byte)0x45, (byte)0x00, (byte)0x52,
                (byte)0x00, (byte)0x4d, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x56, (byte)0x00, (byte)0x2f, (byte)0x00,
                (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x36,
                (byte)0x00, (byte)0x38, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x33, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,};

        byte[] expected = new byte[] {(byte)0x19, (byte)0x4b, (byte)0xeb, (byte)0xad, (byte)0xda, (byte)0x24, (byte)0xd5, (byte)0x96, (byte)0x85,
                (byte)0x2e, (byte)0x24, (byte)0x94, (byte)0xd6, (byte)0x4a, (byte)0xb8, (byte)0x5e, (byte)0x01, (byte)0x01, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xa0, (byte)0xe8, (byte)0x85, (byte)0x2c, (byte)0xe4, (byte)0xc9, (byte)0xce,
                (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e,
                (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00,
                (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52,
                (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00,
                (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31,
                (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00,
                (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49,
                (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00,
                (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53,
                (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00,
                (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34,
                (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00,
                (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0xa0,
                (byte)0xe8, (byte)0x85, (byte)0x2c, (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01, (byte)0x06, (byte)0x00, (byte)0x04, (byte)0x00,
                (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0a, (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x09, (byte)0x00, (byte)0x26, (byte)0x00, (byte)0x54, (byte)0x00, (byte)0x45, (byte)0x00, (byte)0x52,
                (byte)0x00, (byte)0x4d, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x56, (byte)0x00, (byte)0x2f, (byte)0x00,
                (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x36,
                (byte)0x00, (byte)0x38, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x33, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,};
        byte[] actual = ntlm_compute_ntlm_v2_response();
        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_generate_key_exchange_key() {
        keyExchangeKey = sessionBaseKey;
        return keyExchangeKey;
    }

    public byte[] ntlm_generate_random_session_key() {
        randomSessionKey = CryptoAlgos.NONCE(16);
        return randomSessionKey;
    }

    public byte[] ntlm_generate_exported_session_key() {
        exportedSessionKey = randomSessionKey;
        return exportedSessionKey;
    }

    public byte[] ntlm_encrypt_random_session_key() {
        encryptedRandomSessionKey = CryptoAlgos.RC4K(keyExchangeKey, randomSessionKey);
        return encryptedRandomSessionKey;
    }

    public void testComputeEncryptedRandomSessionKey() {
        keyExchangeKey = new byte[] {(byte)0x8e, (byte)0x0f, (byte)0xdd, (byte)0x12, (byte)0x4c, (byte)0x3b, (byte)0x11, (byte)0x7f, (byte)0x22,
                (byte)0xb9, (byte)0x4b, (byte)0x59, (byte)0x52, (byte)0xbc, (byte)0xa7, (byte)0x18,};
        randomSessionKey = new byte[] {(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09,
                (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d, (byte)0x0e, (byte)0x0f, (byte)0x10,};

        byte[] expected = new byte[] {(byte)0xe4, (byte)0xe9, (byte)0xc2, (byte)0xad, (byte)0x41, (byte)0x02, (byte)0x2f, (byte)0x3c, (byte)0xf9,
                (byte)0x4c, (byte)0x72, (byte)0x84, (byte)0xc5, (byte)0x2a, (byte)0x7c, (byte)0x6f,};
        byte[] actual = ntlm_encrypt_random_session_key();

        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_generate_signing_key(String signMagic) {
        return CryptoAlgos.MD5(CryptoAlgos.concatenationOf(exportedSessionKey, signMagic.getBytes(RdpConstants.CHARSET_8), new byte[] {0}));
    }

    public void testGenerateSigningKey() {
        exportedSessionKey = new byte[] {(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09,
                (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d, (byte)0x0e, (byte)0x0f, (byte)0x10,};
        byte[] expected = new byte[] {(byte)0xf6, (byte)0xae, (byte)0x96, (byte)0xcb, (byte)0x05, (byte)0xe2, (byte)0xab, (byte)0x54, (byte)0xf6,
                (byte)0xdd, (byte)0x59, (byte)0xf3, (byte)0xc9, (byte)0xd9, (byte)0xa0, (byte)0x43,};
        byte[] actual = ntlm_generate_signing_key(CLIENT_SIGN_MAGIC);

        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_generate_client_signing_key() {
        clientSigningKey = ntlm_generate_signing_key(CLIENT_SIGN_MAGIC);
        return clientSigningKey;
    }

    public void testGenerateClientSigningKey() {
        exportedSessionKey = new byte[] {(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09,
                (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d, (byte)0x0e, (byte)0x0f, (byte)0x10,};
        byte[] expected = new byte[] {(byte)0xf6, (byte)0xae, (byte)0x96, (byte)0xcb, (byte)0x05, (byte)0xe2, (byte)0xab, (byte)0x54, (byte)0xf6,
                (byte)0xdd, (byte)0x59, (byte)0xf3, (byte)0xc9, (byte)0xd9, (byte)0xa0, (byte)0x43,};
        byte[] actual = ntlm_generate_client_signing_key();

        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_generate_server_signing_key() {
        serverSigningKey = ntlm_generate_signing_key(SERVER_SIGN_MAGIC);
        return serverSigningKey;
    }

    public void testGenerateServerSigningKey() {
        exportedSessionKey = new byte[] {(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09,
                (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d, (byte)0x0e, (byte)0x0f, (byte)0x10,};
        byte[] expected = new byte[] {(byte)0xb6, (byte)0x58, (byte)0xc5, (byte)0x98, (byte)0x7a, (byte)0x25, (byte)0xf8, (byte)0x6e, (byte)0xd8,
                (byte)0xe5, (byte)0x6c, (byte)0xe9, (byte)0x3e, (byte)0x3c, (byte)0xc0, (byte)0x88,};
        byte[] actual = ntlm_generate_server_signing_key();

        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_generate_client_sealing_key() {
        clientSealingKey = ntlm_generate_signing_key(CLIENT_SEAL_MAGIC);
        return clientSealingKey;
    }

    public void testGenerateClientSealingKey() {
        exportedSessionKey = new byte[] {(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09,
                (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d, (byte)0x0e, (byte)0x0f, (byte)0x10,};
        byte[] expected = new byte[] {(byte)0x58, (byte)0x19, (byte)0x44, (byte)0xc2, (byte)0x7a, (byte)0xc6, (byte)0x34, (byte)0x45, (byte)0xe4,
                (byte)0xb8, (byte)0x2b, (byte)0x55, (byte)0xb9, (byte)0x0b, (byte)0x1f, (byte)0xb5,};
        byte[] actual = ntlm_generate_client_sealing_key();

        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_generate_server_sealing_key() {
        serverSealingKey = ntlm_generate_signing_key(SERVER_SEAL_MAGIC);
        return serverSealingKey;
    }

    public void testGenerateServerSealingKey() {
        exportedSessionKey = new byte[] {(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09,
                (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d, (byte)0x0e, (byte)0x0f, (byte)0x10,};
        byte[] expected = new byte[] {(byte)0x92, (byte)0x3a, (byte)0x73, (byte)0x5c, (byte)0x92, (byte)0xa7, (byte)0x04, (byte)0x34, (byte)0xbe,
                (byte)0x9a, (byte)0xa2, (byte)0x9f, (byte)0xed, (byte)0xc1, (byte)0xe6, (byte)0x13,};
        byte[] actual = ntlm_generate_server_sealing_key();

        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public void ntlm_init_rc4_seal_states() {
        ntlm_generate_client_signing_key();
        ntlm_generate_server_signing_key();
        ntlm_generate_client_sealing_key();
        ntlm_generate_server_sealing_key();

        sendSigningKey = clientSigningKey;
        recvSigningKey = serverSigningKey;
        sendSealingKey = clientSealingKey;
        recvSealingKey = serverSealingKey;

        sendRc4Seal = CryptoAlgos.initRC4(sendSealingKey);
        recvRc4Seal = CryptoAlgos.initRC4(recvSealingKey);
    }

    public byte[] ntlm_compute_message_integrity_check() {
        //* DEBUG */System.out.println("ntlm_compute_message_integrity_check: exportedSessionKey:\n" + new ByteBuffer(exportedSessionKey).dump() + "\n");
        //* DEBUG */System.out.println("ntlm_compute_message_integrity_check: negotiateMessage:\n" + new ByteBuffer(negotiateMessage).dump() + "\n");
        //* DEBUG */System.out.println("ntlm_compute_message_integrity_check: challengeMessage:\n" + new ByteBuffer(challengeMessage).dump() + "\n");
        //* DEBUG */System.out.println("ntlm_compute_message_integrity_check: authenticateMessage:\n" + new ByteBuffer(authenticateMessage).dump() + "\n");
        messageIntegrityCheck = CryptoAlgos.HMAC_MD5(exportedSessionKey, CryptoAlgos.concatenationOf(negotiateMessage, challengeMessage, authenticateMessage));
        //* DEBUG */System.out.println("ntlm_compute_message_integrity_check: messageIntegrityCheck:\n" + new ByteBuffer(messageIntegrityCheck).dump() + "\n");
        return messageIntegrityCheck;
    }

    public void testComputeMessageIntegrityCheck() {
        exportedSessionKey = new byte[] {(byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x09,
                (byte)0x0a, (byte)0x0b, (byte)0x0c, (byte)0x0d, (byte)0x0e, (byte)0x0f, (byte)0x10,};

        negotiateMessage = new byte[] {(byte)0x4e, (byte)0x54, (byte)0x4c, (byte)0x4d, (byte)0x53, (byte)0x53, (byte)0x50, (byte)0x00, (byte)0x01,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xb7, (byte)0x82, (byte)0x08, (byte)0xe2, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x06, (byte)0x01, (byte)0xb1, (byte)0x1d, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0f,};

        challengeMessage = new byte[] {(byte)0x4e, (byte)0x54, (byte)0x4c, (byte)0x4d, (byte)0x53, (byte)0x53, (byte)0x50, (byte)0x00, (byte)0x02,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x38, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x35, (byte)0x82, (byte)0x8a, (byte)0xe2, (byte)0x4a, (byte)0x25, (byte)0x50, (byte)0xa5, (byte)0x11, (byte)0x9b, (byte)0xd6,
                (byte)0x16, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x98, (byte)0x00,
                (byte)0x98, (byte)0x00, (byte)0x56, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x06, (byte)0x03, (byte)0xd7, (byte)0x24, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x0f, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00,
                (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42,
                (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00,
                (byte)0x02, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d,
                (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00,
                (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30,
                (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00,
                (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39,
                (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00,
                (byte)0x30, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e,
                (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00,
                (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52,
                (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00,
                (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31,
                (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00,
                (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0xa0, (byte)0xe8, (byte)0x85,
                (byte)0x2c, (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,};

        authenticateMessage = new byte[] {(byte)0x4e, (byte)0x54, (byte)0x4c, (byte)0x4d, (byte)0x53, (byte)0x53, (byte)0x50, (byte)0x00, (byte)0x03,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x18, (byte)0x00, (byte)0x18, (byte)0x00, (byte)0x90, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x16, (byte)0x01, (byte)0x16, (byte)0x01, (byte)0xa8, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x12, (byte)0x00, (byte)0x12,
                (byte)0x00, (byte)0x58, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x1a, (byte)0x00, (byte)0x1a, (byte)0x00, (byte)0x6a, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x0c, (byte)0x00, (byte)0x0c, (byte)0x00, (byte)0x84, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10,
                (byte)0x00, (byte)0x10, (byte)0x00, (byte)0xbe, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x35, (byte)0xb2, (byte)0x88, (byte)0xe2,
                (byte)0x06, (byte)0x01, (byte)0xb1, (byte)0x1d, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0f, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x77, (byte)0x00, (byte)0x6f, (byte)0x00, (byte)0x72, (byte)0x00, (byte)0x6b, (byte)0x00, (byte)0x67,
                (byte)0x00, (byte)0x72, (byte)0x00, (byte)0x6f, (byte)0x00, (byte)0x75, (byte)0x00, (byte)0x70, (byte)0x00, (byte)0x41, (byte)0x00,
                (byte)0x64, (byte)0x00, (byte)0x6d, (byte)0x00, (byte)0x69, (byte)0x00, (byte)0x6e, (byte)0x00, (byte)0x69, (byte)0x00, (byte)0x73,
                (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x72, (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x74, (byte)0x00, (byte)0x6f, (byte)0x00,
                (byte)0x72, (byte)0x00, (byte)0x61, (byte)0x00, (byte)0x70, (byte)0x00, (byte)0x6f, (byte)0x00, (byte)0x6c, (byte)0x00, (byte)0x6c,
                (byte)0x00, (byte)0x6f, (byte)0x00, (byte)0x7c, (byte)0xc0, (byte)0xfd, (byte)0x08, (byte)0xc5, (byte)0x14, (byte)0x05, (byte)0x34,
                (byte)0xf3, (byte)0x12, (byte)0x9e, (byte)0x3e, (byte)0xa3, (byte)0x09, (byte)0xbc, (byte)0xc6, (byte)0x01, (byte)0x02, (byte)0x03,
                (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07, (byte)0x08, (byte)0x19, (byte)0x4b, (byte)0xeb, (byte)0xad, (byte)0xda, (byte)0x24,
                (byte)0xd5, (byte)0x96, (byte)0x85, (byte)0x2e, (byte)0x24, (byte)0x94, (byte)0xd6, (byte)0x4a, (byte)0xb8, (byte)0x5e, (byte)0x01,
                (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xa0, (byte)0xe8, (byte)0x85, (byte)0x2c,
                (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x04, (byte)0x05, (byte)0x06, (byte)0x07,
                (byte)0x08, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57, (byte)0x00,
                (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00, (byte)0x34,
                (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c, (byte)0x00,
                (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x1e, (byte)0x00, (byte)0x57,
                (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f, (byte)0x00,
                (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x4c,
                (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x1e, (byte)0x00,
                (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x4f,
                (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32, (byte)0x00,
                (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x03, (byte)0x00, (byte)0x1e,
                (byte)0x00, (byte)0x57, (byte)0x00, (byte)0x49, (byte)0x00, (byte)0x4e, (byte)0x00, (byte)0x2d, (byte)0x00, (byte)0x4c, (byte)0x00,
                (byte)0x4f, (byte)0x00, (byte)0x34, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x42, (byte)0x00, (byte)0x32,
                (byte)0x00, (byte)0x4c, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x30, (byte)0x00, (byte)0x07, (byte)0x00,
                (byte)0x08, (byte)0x00, (byte)0xa0, (byte)0xe8, (byte)0x85, (byte)0x2c, (byte)0xe4, (byte)0xc9, (byte)0xce, (byte)0x01, (byte)0x06,
                (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x02, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x0a, (byte)0x00, (byte)0x10, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x09, (byte)0x00, (byte)0x26, (byte)0x00, (byte)0x54, (byte)0x00,
                (byte)0x45, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x4d, (byte)0x00, (byte)0x53, (byte)0x00, (byte)0x52, (byte)0x00, (byte)0x56,
                (byte)0x00, (byte)0x2f, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x39, (byte)0x00, (byte)0x32, (byte)0x00, (byte)0x2e, (byte)0x00,
                (byte)0x31, (byte)0x00, (byte)0x36, (byte)0x00, (byte)0x38, (byte)0x00, (byte)0x2e, (byte)0x00, (byte)0x31, (byte)0x00, (byte)0x2e,
                (byte)0x00, (byte)0x33, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
                (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0xe4, (byte)0xe9, (byte)0xc2,
                (byte)0xad, (byte)0x41, (byte)0x02, (byte)0x2f, (byte)0x3c, (byte)0xf9, (byte)0x4c, (byte)0x72, (byte)0x84, (byte)0xc5, (byte)0x2a,
                (byte)0x7c, (byte)0x6f,};

        byte[] expected = new byte[] {(byte)0xd9, (byte)0xe9, (byte)0xbc, (byte)0x9b, (byte)0x6f, (byte)0xa5, (byte)0xf9, (byte)0xc8, (byte)0x70,
                (byte)0x16, (byte)0x10, (byte)0x20, (byte)0xf8, (byte)0xf1, (byte)0x61, (byte)0x42,};
        byte[] actual = ntlm_compute_message_integrity_check();

        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_EncryptMessage(byte[] message) {
        byte[] versionBytes = new byte[] {0x01, 0x00, 0x00, 0x00}; // 0x00000001
        // LE
        byte[] seqNumBytes = new byte[] {(byte)(sendSeqNum & 0xff), (byte)((sendSeqNum >> 8) & 0xff), (byte)((sendSeqNum >> 16) & 0xff),
                (byte)((sendSeqNum >> 24) & 0xff)};

        byte[] digest = CryptoAlgos.HMAC_MD5(sendSigningKey, CryptoAlgos.concatenationOf(seqNumBytes, message));

        byte[] encrypted = CryptoAlgos.RC4(sendRc4Seal, message);

        // Encrypt first 8 bytes of digest only
        byte[] checksum = CryptoAlgos.RC4(sendRc4Seal, Arrays.copyOf(digest, 8));

        byte[] signature = CryptoAlgos.concatenationOf(versionBytes, checksum, seqNumBytes);

        sendSeqNum++;

        return CryptoAlgos.concatenationOf(signature, encrypted);
    }

    public void testNtlmEncryptMessage() {
        sendSigningKey = new byte[] {(byte)0xf6, (byte)0xae, (byte)0x96, (byte)0xcb, (byte)0x05, (byte)0xe2, (byte)0xab, (byte)0x54, (byte)0xf6,
                (byte)0xdd, (byte)0x59, (byte)0xf3, (byte)0xc9, (byte)0xd9, (byte)0xa0, (byte)0x43,};
        sendRc4Seal = CryptoAlgos.initRC4(new byte[] {(byte)0x58, (byte)0x19, (byte)0x44, (byte)0xc2, (byte)0x7a, (byte)0xc6, (byte)0x34, (byte)0x45,
                (byte)0xe4, (byte)0xb8, (byte)0x2b, (byte)0x55, (byte)0xb9, (byte)0x0b, (byte)0x1f, (byte)0xb5,});
        sendSeqNum = 0;
        byte[] serverPublicKey = new byte[] {(byte)0x30, (byte)0x82, (byte)0x01, (byte)0x0a, (byte)0x02, (byte)0x82, (byte)0x01, (byte)0x01, (byte)0x00,
                (byte)0xa8, (byte)0x56, (byte)0x65, (byte)0xd3, (byte)0xce, (byte)0x8a, (byte)0x54, (byte)0x4d, (byte)0x9d, (byte)0xb0, (byte)0x84,
                (byte)0x31, (byte)0x19, (byte)0x71, (byte)0x7f, (byte)0xdd, (byte)0x42, (byte)0xfb, (byte)0x2a, (byte)0x7a, (byte)0x72, (byte)0x13,
                (byte)0xa1, (byte)0xb9, (byte)0x72, (byte)0xbb, (byte)0xd3, (byte)0x08, (byte)0xad, (byte)0x7d, (byte)0x6c, (byte)0x15, (byte)0x65,
                (byte)0x03, (byte)0xd1, (byte)0xc4, (byte)0x54, (byte)0xc5, (byte)0x33, (byte)0x6b, (byte)0x7d, (byte)0x69, (byte)0x89, (byte)0x5e,
                (byte)0xfe, (byte)0xe0, (byte)0x01, (byte)0xc0, (byte)0x7e, (byte)0x9b, (byte)0xcb, (byte)0x5d, (byte)0x65, (byte)0x36, (byte)0xcd,
                (byte)0x77, (byte)0x5d, (byte)0xf3, (byte)0x7a, (byte)0x5b, (byte)0x29, (byte)0x44, (byte)0x72, (byte)0xd5, (byte)0x38, (byte)0xe2,
                (byte)0xcf, (byte)0xb1, (byte)0xc7, (byte)0x78, (byte)0x9b, (byte)0x58, (byte)0xb9, (byte)0x17, (byte)0x7c, (byte)0xb7, (byte)0xd6,
                (byte)0xc7, (byte)0xc7, (byte)0xbf, (byte)0x90, (byte)0x4e, (byte)0x7c, (byte)0x39, (byte)0x93, (byte)0xcb, (byte)0x2e, (byte)0xe0,
                (byte)0xc2, (byte)0x33, (byte)0x2d, (byte)0xa5, (byte)0x7e, (byte)0xe0, (byte)0x7b, (byte)0xb6, (byte)0xf9, (byte)0x91, (byte)0x32,
                (byte)0xb7, (byte)0xd4, (byte)0x85, (byte)0xb7, (byte)0x35, (byte)0x2d, (byte)0x2b, (byte)0x00, (byte)0x6d, (byte)0xf8, (byte)0xea,
                (byte)0x8c, (byte)0x97, (byte)0x5f, (byte)0x51, (byte)0x1d, (byte)0x68, (byte)0x04, (byte)0x3c, (byte)0x79, (byte)0x14, (byte)0x71,
                (byte)0xa7, (byte)0xc7, (byte)0xd7, (byte)0x70, (byte)0x7a, (byte)0xe0, (byte)0xba, (byte)0x12, (byte)0x69, (byte)0xc8, (byte)0xd3,
                (byte)0xd9, (byte)0x4e, (byte)0xab, (byte)0x51, (byte)0x47, (byte)0xa3, (byte)0xec, (byte)0x99, (byte)0xd4, (byte)0x88, (byte)0xca,
                (byte)0xda, (byte)0xc2, (byte)0x7f, (byte)0x79, (byte)0x4b, (byte)0x66, (byte)0xed, (byte)0x87, (byte)0xbe, (byte)0xc2, (byte)0x5f,
                (byte)0xea, (byte)0xcf, (byte)0xe1, (byte)0xb5, (byte)0xf0, (byte)0x3d, (byte)0x9b, (byte)0xf2, (byte)0x19, (byte)0xc3, (byte)0xe0,
                (byte)0xe1, (byte)0x7a, (byte)0x45, (byte)0x71, (byte)0x12, (byte)0x3d, (byte)0x72, (byte)0x1d, (byte)0x6f, (byte)0x2b, (byte)0x1c,
                (byte)0x46, (byte)0x68, (byte)0xc0, (byte)0x8f, (byte)0x4f, (byte)0xce, (byte)0x3a, (byte)0xc5, (byte)0xcd, (byte)0x22, (byte)0x65,
                (byte)0x2d, (byte)0x43, (byte)0xb0, (byte)0x5c, (byte)0xdd, (byte)0x89, (byte)0xae, (byte)0xbe, (byte)0x70, (byte)0x59, (byte)0x5e,
                (byte)0x0c, (byte)0xbd, (byte)0xf5, (byte)0x46, (byte)0x82, (byte)0x1e, (byte)0xe4, (byte)0x86, (byte)0x95, (byte)0x7b, (byte)0x60,
                (byte)0xae, (byte)0x45, (byte)0x50, (byte)0xc2, (byte)0x54, (byte)0x08, (byte)0x49, (byte)0x9a, (byte)0x9e, (byte)0xfb, (byte)0xb2,
                (byte)0xb6, (byte)0x78, (byte)0xe5, (byte)0x2f, (byte)0x9c, (byte)0x5a, (byte)0xd0, (byte)0x8a, (byte)0x03, (byte)0x77, (byte)0x68,
                (byte)0x30, (byte)0x93, (byte)0x78, (byte)0x6d, (byte)0x90, (byte)0x6d, (byte)0x50, (byte)0xfa, (byte)0xa7, (byte)0x65, (byte)0xfe,
                (byte)0x59, (byte)0x33, (byte)0x27, (byte)0x4e, (byte)0x4b, (byte)0xf8, (byte)0x38, (byte)0x44, (byte)0x3a, (byte)0x12, (byte)0xf4,
                (byte)0x07, (byte)0xa0, (byte)0x8d, (byte)0x02, (byte)0x03, (byte)0x01, (byte)0x00, (byte)0x01,};

        byte[] expected = new byte[] {(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x72, (byte)0x76, (byte)0x1e, (byte)0x57, (byte)0x49,
                (byte)0xb5, (byte)0x0f, (byte)0xad, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x15, (byte)0xf7, (byte)0xf2, (byte)0x54,
                (byte)0xda, (byte)0xa9, (byte)0xe5, (byte)0xad, (byte)0x85, (byte)0x04, (byte)0x67, (byte)0x4d, (byte)0x0b, (byte)0xcb, (byte)0xf9,
                (byte)0xb1, (byte)0xf8, (byte)0x02, (byte)0x8a, (byte)0x77, (byte)0xc2, (byte)0x63, (byte)0xab, (byte)0xd5, (byte)0x74, (byte)0x23,
                (byte)0x9f, (byte)0x9d, (byte)0x5d, (byte)0x1f, (byte)0xd3, (byte)0xb3, (byte)0xa0, (byte)0xac, (byte)0x16, (byte)0x8a, (byte)0x4b,
                (byte)0x08, (byte)0xf5, (byte)0x47, (byte)0x70, (byte)0x58, (byte)0x10, (byte)0xb4, (byte)0xe7, (byte)0x87, (byte)0xb3, (byte)0x4b,
                (byte)0xc9, (byte)0xa2, (byte)0xd5, (byte)0xd1, (byte)0xca, (byte)0x0f, (byte)0xd4, (byte)0xe3, (byte)0x8d, (byte)0x76, (byte)0x5a,
                (byte)0x60, (byte)0x28, (byte)0xf8, (byte)0x06, (byte)0x5d, (byte)0xe4, (byte)0x7e, (byte)0x21, (byte)0xc8, (byte)0xbb, (byte)0xac,
                (byte)0xe5, (byte)0x79, (byte)0x85, (byte)0x30, (byte)0x9b, (byte)0x88, (byte)0x13, (byte)0x2f, (byte)0x8f, (byte)0xfc, (byte)0x04,
                (byte)0x52, (byte)0xfe, (byte)0x87, (byte)0x94, (byte)0xcf, (byte)0xcb, (byte)0x49, (byte)0x4a, (byte)0xda, (byte)0x6f, (byte)0xdd,
                (byte)0xee, (byte)0x57, (byte)0xa5, (byte)0xe4, (byte)0x4d, (byte)0x0e, (byte)0x5c, (byte)0x3d, (byte)0x0b, (byte)0x63, (byte)0x1f,
                (byte)0xf6, (byte)0x3d, (byte)0x1b, (byte)0xae, (byte)0x5a, (byte)0xf6, (byte)0x42, (byte)0x2a, (byte)0x46, (byte)0xfa, (byte)0x42,
                (byte)0x71, (byte)0x67, (byte)0x46, (byte)0x02, (byte)0x71, (byte)0xea, (byte)0x51, (byte)0x98, (byte)0xf7, (byte)0xd4, (byte)0x43,
                (byte)0xbf, (byte)0x8e, (byte)0xe8, (byte)0x3c, (byte)0xc8, (byte)0xfa, (byte)0x79, (byte)0x9d, (byte)0x8c, (byte)0xfc, (byte)0xc2,
                (byte)0x42, (byte)0xc9, (byte)0xbb, (byte)0xd0, (byte)0xab, (byte)0x81, (byte)0xc4, (byte)0x53, (byte)0xfd, (byte)0x41, (byte)0xda,
                (byte)0xab, (byte)0x0f, (byte)0x25, (byte)0x79, (byte)0x5f, (byte)0xbd, (byte)0xa3, (byte)0x8c, (byte)0xd3, (byte)0xf5, (byte)0x1b,
                (byte)0xab, (byte)0x20, (byte)0xd1, (byte)0xf4, (byte)0xd8, (byte)0x81, (byte)0x9c, (byte)0x18, (byte)0x4a, (byte)0xa4, (byte)0x77,
                (byte)0xee, (byte)0xe1, (byte)0x51, (byte)0xee, (byte)0x2a, (byte)0xc1, (byte)0x94, (byte)0x37, (byte)0xc5, (byte)0x06, (byte)0x7a,
                (byte)0x3f, (byte)0x0f, (byte)0x25, (byte)0x5b, (byte)0x4e, (byte)0x6a, (byte)0xdc, (byte)0x0b, (byte)0x62, (byte)0x6f, (byte)0x12,
                (byte)0x83, (byte)0x03, (byte)0xae, (byte)0x4e, (byte)0xce, (byte)0x2b, (byte)0x6e, (byte)0xd4, (byte)0xd5, (byte)0x23, (byte)0x27,
                (byte)0xf6, (byte)0xa6, (byte)0x38, (byte)0x67, (byte)0xec, (byte)0x95, (byte)0x82, (byte)0xc6, (byte)0xba, (byte)0xd4, (byte)0xf6,
                (byte)0xe6, (byte)0x22, (byte)0x7d, (byte)0xb9, (byte)0xe4, (byte)0x81, (byte)0x97, (byte)0x24, (byte)0xff, (byte)0x40, (byte)0xb2,
                (byte)0x42, (byte)0x3c, (byte)0x11, (byte)0x24, (byte)0xd0, (byte)0x3a, (byte)0x96, (byte)0xd9, (byte)0xc1, (byte)0x13, (byte)0xd6,
                (byte)0x62, (byte)0x45, (byte)0x21, (byte)0x60, (byte)0x5b, (byte)0x7b, (byte)0x2b, (byte)0x62, (byte)0x44, (byte)0xf7, (byte)0x40,
                (byte)0x93, (byte)0x29, (byte)0x5b, (byte)0x44, (byte)0xb7, (byte)0xda, (byte)0x9c, (byte)0xa6, (byte)0xa9, (byte)0x3b, (byte)0xe1,
                (byte)0x3b, (byte)0x9d, (byte)0x31, (byte)0xf2, (byte)0x21, (byte)0x53, (byte)0x0f, (byte)0xb3, (byte)0x70, (byte)0x55, (byte)0x84,
                (byte)0x2c, (byte)0xb4,};
        byte[] actual = ntlm_EncryptMessage(serverPublicKey);

        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public byte[] ntlm_DecryptMessage(byte[] wrappedMssage) {

        byte[] versionBytes = new byte[] {0x01, 0x00, 0x00, 0x00}; // 0x00000001
        // LE
        byte[] seqNumBytes = new byte[] {(byte)(recvSeqNum & 0xff), (byte)((recvSeqNum >> 8) & 0xff), (byte)((recvSeqNum >> 16) & 0xff),
                (byte)((recvSeqNum >> 24) & 0xff)};

        // Unwrap message
        byte[] actualSignature = Arrays.copyOf(wrappedMssage, 16);
        byte[] encryptedMessage = Arrays.copyOfRange(wrappedMssage, 16, wrappedMssage.length);

        // Decrypt message
        byte[] decryptedMessage = CryptoAlgos.RC4(recvRc4Seal, encryptedMessage);

        // Compare actual signature with expected signature
        byte[] digest = CryptoAlgos.HMAC_MD5(recvSigningKey, CryptoAlgos.concatenationOf(seqNumBytes, decryptedMessage));

        // Encrypt first 8 bytes of digest only
        byte[] checksum = CryptoAlgos.RC4(recvRc4Seal, Arrays.copyOf(digest, 8));

        byte[] expectedSignature = CryptoAlgos.concatenationOf(versionBytes, checksum, seqNumBytes);

        if (!Arrays.equals(expectedSignature, actualSignature))
            throw new RuntimeException("Unexpected signature of message:\nExpected signature: " + new ByteBuffer(expectedSignature).toPlainHexString()
                    + "\n  Actual signature: " + new ByteBuffer(actualSignature).toPlainHexString());

        recvSeqNum++;

        return decryptedMessage;
    }

    public void testNtlmDecryptMessage() {
        recvSigningKey = new byte[] {(byte)0xb6, (byte)0x58, (byte)0xc5, (byte)0x98, (byte)0x7a, (byte)0x25, (byte)0xf8, (byte)0x6e, (byte)0xd8,
                (byte)0xe5, (byte)0x6c, (byte)0xe9, (byte)0x3e, (byte)0x3c, (byte)0xc0, (byte)0x88,};
        recvRc4Seal = CryptoAlgos.initRC4(new byte[] {(byte)0x92, (byte)0x3a, (byte)0x73, (byte)0x5c, (byte)0x92, (byte)0xa7, (byte)0x04, (byte)0x34,
                (byte)0xbe, (byte)0x9a, (byte)0xa2, (byte)0x9f, (byte)0xed, (byte)0xc1, (byte)0xe6, (byte)0x13,});
        recvSeqNum = 0;
        byte[] encryptedMessage = new byte[] {(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x25, (byte)0xf8, (byte)0x2d, (byte)0x1e, (byte)0x4e,
                (byte)0x6a, (byte)0xec, (byte)0x4f, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x03, (byte)0x12, (byte)0xdd, (byte)0xea,
                (byte)0x47, (byte)0xb3, (byte)0xff, (byte)0xe1, (byte)0x66, (byte)0x08, (byte)0xf6, (byte)0x6b, (byte)0xa0, (byte)0x62, (byte)0x42,
                (byte)0x67, (byte)0xbf, (byte)0x3d, (byte)0x59, (byte)0x60, (byte)0xef, (byte)0x52, (byte)0xb0, (byte)0x26, (byte)0x95, (byte)0xed,
                (byte)0x84, (byte)0x48, (byte)0x44, (byte)0xbb, (byte)0x8d, (byte)0x65, (byte)0xcf, (byte)0xe4, (byte)0x8e, (byte)0x6f, (byte)0x69,
                (byte)0xae, (byte)0xed, (byte)0x44, (byte)0xbb, (byte)0x49, (byte)0x1d, (byte)0x2a, (byte)0x40, (byte)0x29, (byte)0x2b, (byte)0x13,
                (byte)0x42, (byte)0x1c, (byte)0xeb, (byte)0xb1, (byte)0x6c, (byte)0x8a, (byte)0x3b, (byte)0x80, (byte)0xd1, (byte)0x70, (byte)0xfd,
                (byte)0xdd, (byte)0x79, (byte)0xe4, (byte)0x93, (byte)0x0b, (byte)0x47, (byte)0xbd, (byte)0x3a, (byte)0x7e, (byte)0x31, (byte)0x66,
                (byte)0x4b, (byte)0x65, (byte)0x8d, (byte)0x5c, (byte)0x2a, (byte)0xcd, (byte)0xc2, (byte)0x09, (byte)0x7a, (byte)0x3b, (byte)0xb2,
                (byte)0xfd, (byte)0x09, (byte)0x52, (byte)0x09, (byte)0x47, (byte)0x05, (byte)0xa4, (byte)0x6f, (byte)0x32, (byte)0xd1, (byte)0x76,
                (byte)0xb2, (byte)0xd4, (byte)0x59, (byte)0xe0, (byte)0x85, (byte)0xf1, (byte)0x36, (byte)0x7d, (byte)0x76, (byte)0x50, (byte)0x21,
                (byte)0x0e, (byte)0x20, (byte)0x22, (byte)0x83, (byte)0x1a, (byte)0x08, (byte)0xc0, (byte)0x85, (byte)0x5d, (byte)0x4f, (byte)0x5c,
                (byte)0x77, (byte)0x68, (byte)0x32, (byte)0x95, (byte)0xa9, (byte)0xa2, (byte)0x59, (byte)0x69, (byte)0xea, (byte)0x19, (byte)0x34,
                (byte)0x08, (byte)0xed, (byte)0x76, (byte)0xa3, (byte)0x58, (byte)0x37, (byte)0xf2, (byte)0x0a, (byte)0x0c, (byte)0xba, (byte)0x4d,
                (byte)0xbb, (byte)0x6f, (byte)0x82, (byte)0x94, (byte)0xd3, (byte)0x87, (byte)0xde, (byte)0xc9, (byte)0x8f, (byte)0xef, (byte)0x34,
                (byte)0x2d, (byte)0x8f, (byte)0xd0, (byte)0x0c, (byte)0x91, (byte)0x59, (byte)0xfd, (byte)0xea, (byte)0x6b, (byte)0xcb, (byte)0xbd,
                (byte)0xa2, (byte)0x20, (byte)0xed, (byte)0xb9, (byte)0x76, (byte)0xd3, (byte)0x64, (byte)0x1b, (byte)0xb3, (byte)0x3b, (byte)0xf5,
                (byte)0x9b, (byte)0x61, (byte)0xd7, (byte)0xab, (byte)0x26, (byte)0x9b, (byte)0x0d, (byte)0xa0, (byte)0xea, (byte)0xbf, (byte)0xad,
                (byte)0x2c, (byte)0xad, (byte)0x63, (byte)0x65, (byte)0xc6, (byte)0x70, (byte)0xc4, (byte)0xe5, (byte)0x8d, (byte)0x40, (byte)0xaa,
                (byte)0x08, (byte)0x45, (byte)0x66, (byte)0xe2, (byte)0x4d, (byte)0xc9, (byte)0x46, (byte)0x00, (byte)0x33, (byte)0x43, (byte)0xe0,
                (byte)0xba, (byte)0xd6, (byte)0x80, (byte)0x29, (byte)0x21, (byte)0x5e, (byte)0xd1, (byte)0x9a, (byte)0xbc, (byte)0x44, (byte)0xfa,
                (byte)0x4d, (byte)0x46, (byte)0xf9, (byte)0x25, (byte)0x80, (byte)0x40, (byte)0xb5, (byte)0x27, (byte)0xdd, (byte)0xc5, (byte)0x02,
                (byte)0xf8, (byte)0xa4, (byte)0x9a, (byte)0xcb, (byte)0xcf, (byte)0x3f, (byte)0xef, (byte)0xc7, (byte)0xcd, (byte)0x71, (byte)0x45,
                (byte)0xa5, (byte)0x35, (byte)0xb1, (byte)0x21, (byte)0x14, (byte)0x39, (byte)0x57, (byte)0xf8, (byte)0x0a, (byte)0x24, (byte)0x98,
                (byte)0xea, (byte)0x15, (byte)0xe1, (byte)0xe3, (byte)0xcb, (byte)0x9d, (byte)0xf2, (byte)0x4e, (byte)0xef, (byte)0x89, (byte)0x97,
                (byte)0xc0, (byte)0xb2, (byte)0x96, (byte)0x9a, (byte)0x1e, (byte)0xad, (byte)0xd0, (byte)0x9a, (byte)0x99, (byte)0x62, (byte)0x9f,
                (byte)0x13, (byte)0x2e,};

        byte[] expected = new byte[] {
                // First byte is increased by 1
                (byte)0x31, (byte)0x82, (byte)0x01, (byte)0x0a, (byte)0x02, (byte)0x82, (byte)0x01, (byte)0x01, (byte)0x00, (byte)0xa8, (byte)0x56,
                (byte)0x65, (byte)0xd3, (byte)0xce, (byte)0x8a, (byte)0x54, (byte)0x4d, (byte)0x9d, (byte)0xb0, (byte)0x84, (byte)0x31, (byte)0x19,
                (byte)0x71, (byte)0x7f, (byte)0xdd, (byte)0x42, (byte)0xfb, (byte)0x2a, (byte)0x7a, (byte)0x72, (byte)0x13, (byte)0xa1, (byte)0xb9,
                (byte)0x72, (byte)0xbb, (byte)0xd3, (byte)0x08, (byte)0xad, (byte)0x7d, (byte)0x6c, (byte)0x15, (byte)0x65, (byte)0x03, (byte)0xd1,
                (byte)0xc4, (byte)0x54, (byte)0xc5, (byte)0x33, (byte)0x6b, (byte)0x7d, (byte)0x69, (byte)0x89, (byte)0x5e, (byte)0xfe, (byte)0xe0,
                (byte)0x01, (byte)0xc0, (byte)0x7e, (byte)0x9b, (byte)0xcb, (byte)0x5d, (byte)0x65, (byte)0x36, (byte)0xcd, (byte)0x77, (byte)0x5d,
                (byte)0xf3, (byte)0x7a, (byte)0x5b, (byte)0x29, (byte)0x44, (byte)0x72, (byte)0xd5, (byte)0x38, (byte)0xe2, (byte)0xcf, (byte)0xb1,
                (byte)0xc7, (byte)0x78, (byte)0x9b, (byte)0x58, (byte)0xb9, (byte)0x17, (byte)0x7c, (byte)0xb7, (byte)0xd6, (byte)0xc7, (byte)0xc7,
                (byte)0xbf, (byte)0x90, (byte)0x4e, (byte)0x7c, (byte)0x39, (byte)0x93, (byte)0xcb, (byte)0x2e, (byte)0xe0, (byte)0xc2, (byte)0x33,
                (byte)0x2d, (byte)0xa5, (byte)0x7e, (byte)0xe0, (byte)0x7b, (byte)0xb6, (byte)0xf9, (byte)0x91, (byte)0x32, (byte)0xb7, (byte)0xd4,
                (byte)0x85, (byte)0xb7, (byte)0x35, (byte)0x2d, (byte)0x2b, (byte)0x00, (byte)0x6d, (byte)0xf8, (byte)0xea, (byte)0x8c, (byte)0x97,
                (byte)0x5f, (byte)0x51, (byte)0x1d, (byte)0x68, (byte)0x04, (byte)0x3c, (byte)0x79, (byte)0x14, (byte)0x71, (byte)0xa7, (byte)0xc7,
                (byte)0xd7, (byte)0x70, (byte)0x7a, (byte)0xe0, (byte)0xba, (byte)0x12, (byte)0x69, (byte)0xc8, (byte)0xd3, (byte)0xd9, (byte)0x4e,
                (byte)0xab, (byte)0x51, (byte)0x47, (byte)0xa3, (byte)0xec, (byte)0x99, (byte)0xd4, (byte)0x88, (byte)0xca, (byte)0xda, (byte)0xc2,
                (byte)0x7f, (byte)0x79, (byte)0x4b, (byte)0x66, (byte)0xed, (byte)0x87, (byte)0xbe, (byte)0xc2, (byte)0x5f, (byte)0xea, (byte)0xcf,
                (byte)0xe1, (byte)0xb5, (byte)0xf0, (byte)0x3d, (byte)0x9b, (byte)0xf2, (byte)0x19, (byte)0xc3, (byte)0xe0, (byte)0xe1, (byte)0x7a,
                (byte)0x45, (byte)0x71, (byte)0x12, (byte)0x3d, (byte)0x72, (byte)0x1d, (byte)0x6f, (byte)0x2b, (byte)0x1c, (byte)0x46, (byte)0x68,
                (byte)0xc0, (byte)0x8f, (byte)0x4f, (byte)0xce, (byte)0x3a, (byte)0xc5, (byte)0xcd, (byte)0x22, (byte)0x65, (byte)0x2d, (byte)0x43,
                (byte)0xb0, (byte)0x5c, (byte)0xdd, (byte)0x89, (byte)0xae, (byte)0xbe, (byte)0x70, (byte)0x59, (byte)0x5e, (byte)0x0c, (byte)0xbd,
                (byte)0xf5, (byte)0x46, (byte)0x82, (byte)0x1e, (byte)0xe4, (byte)0x86, (byte)0x95, (byte)0x7b, (byte)0x60, (byte)0xae, (byte)0x45,
                (byte)0x50, (byte)0xc2, (byte)0x54, (byte)0x08, (byte)0x49, (byte)0x9a, (byte)0x9e, (byte)0xfb, (byte)0xb2, (byte)0xb6, (byte)0x78,
                (byte)0xe5, (byte)0x2f, (byte)0x9c, (byte)0x5a, (byte)0xd0, (byte)0x8a, (byte)0x03, (byte)0x77, (byte)0x68, (byte)0x30, (byte)0x93,
                (byte)0x78, (byte)0x6d, (byte)0x90, (byte)0x6d, (byte)0x50, (byte)0xfa, (byte)0xa7, (byte)0x65, (byte)0xfe, (byte)0x59, (byte)0x33,
                (byte)0x27, (byte)0x4e, (byte)0x4b, (byte)0xf8, (byte)0x38, (byte)0x44, (byte)0x3a, (byte)0x12, (byte)0xf4, (byte)0x07, (byte)0xa0,
                (byte)0x8d, (byte)0x02, (byte)0x03, (byte)0x01, (byte)0x00, (byte)0x01,};
        byte[] actual = ntlm_DecryptMessage(encryptedMessage);

        if (!Arrays.equals(expected, actual))
            throw new RuntimeException("Incorrect result.\nExpected:\n" + new ByteBuffer(expected).toPlainHexString() + "\n  actual:\n"
                    + new ByteBuffer(actual).toPlainHexString() + ".");
    }

    public static void main(String args[]) {
        CryptoAlgos.callAll(new NtlmState());
    }

}
