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
package rdpclient.ntlmssp.asn1;

import streamer.ByteBuffer;
import common.asn1.Asn1Integer;
import common.asn1.OctetString;
import common.asn1.Sequence;
import common.asn1.Tag;

/**
 * The TSRequest structure is the top-most structure used by the CredSSP client
 * and CredSSP server. It contains the SPNEGO messages between the client and
 * server, and either the public key authentication messages that are used to
 * bind to the TLS session or the client credentials that are delegated to the
 * server. The TSRequest message is always sent over the TLS-encrypted channel
 * between the client and server in a CredSSP Protocol exchange (see step 1 in
 * section 3.1.5).
 *
 * <pre>
 * TSRequest ::= SEQUENCE {
 *   version       [0] INTEGER,
 *   negoTokens    [1] NegoData OPTIONAL,
 *   authInfo      [2] OCTET STRING OPTIONAL,
 *   pubKeyAuth    [3] OCTET STRING OPTIONAL
 * }
 *
 * </pre>
 * <ul>
 *
 * <li>version: This field specifies the supported version of the CredSSP
 * Protocol. This field MUST be 2. If the version is greater than 2, a version 2
 * client or server treats its peer as one that is compatible with version 2 of
 * the CredSSP Protocol.
 *
 * <li>negoTokens: A NegoData structure, as defined in section 2.2.1.1, that
 * contains the SPNEGO messages that are passed between the client and server.
 *
 * <li>authInfo: A TSCredentials structure, as defined in section 2.2.1.2, that
 * contains the user's credentials that are delegated to the server. The
 * authinfo field <b>MUST be encrypted</b> under the encryption key that is
 * negotiated under the SPNEGO package.
 *
 * <li>pubKeyAuth: This field is used to assure that the public key that is used
 * by the server during the TLS handshake belongs to the target server and not
 * to a "man in the middle". The client encrypts the public key it received from
 * the server (contained in the X.509 certificate) in the TLS handshake from
 * step 1, by using the confidentiality support of SPNEGO. The public key that
 * is encrypted is the ASN.1-encoded SubjectPublicKey sub-field of
 * SubjectPublicKeyInfo from the X.509 certificate, as specified in [RFC3280]
 * section 4.1. The encrypted key is encapsulated in the pubKeyAuth field of the
 * TSRequest structure and is sent over the TLS channel to the server. After the
 * client completes the SPNEGO phase of the CredSSP Protocol, it uses
 * GSS_WrapEx() for the negotiated protocol to encrypt the server's public key.
 * The pubKeyAuth field carries the message signature and then the encrypted
 * public key to the server. In response, the server uses the pubKeyAuth field
 * to transmit to the client a modified version of the public key (as described
 * in section 3.1.5) that is encrypted under the encryption key that is
 * negotiated under SPNEGO.
 * </ul>
 *
 * @see http://msdn.microsoft.com/en-us/library/cc226780.aspx
 */
public class TSRequest extends Sequence {
    public Asn1Integer version = new Asn1Integer("version") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 0;
        }
    };
    public NegoData negoTokens = new NegoData("negoTokens") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 1;
            optional = true;
        }
    };
    public OctetString authInfo = new OctetString("authInfo") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 2;
            optional = true;
        }
    };
    public OctetString pubKeyAuth = new OctetString("pubKeyAuth") {
        {
            explicit = true;
            tagClass = CONTEXT_CLASS;
            tagNumber = 3;
            optional = true;
        }
    };

    public TSRequest(String name) {
        super(name);
        tags = new Tag[] {version, negoTokens, authInfo, pubKeyAuth};
    }

    @Override
    public Tag deepCopy(String suffix) {
        return new TSRequest(name + suffix).copyFrom(this);
    }

    /**
     * Example.
     */
    public static void main(String[] args) {

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
                0x35, (byte) 0x82, (byte) 0x8a, (byte) 0xe2, // NegotiateFlags TODO
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

        TSRequest request = new TSRequest("TSRequest");

        // Read request from buffer
        // System.out.println("Request BER tree before parsing: " + request);
        ByteBuffer toReadBuf = new ByteBuffer(packet);
        request.readTag(toReadBuf);
        // System.out.println("Request BER tree after parsing: " + request);

        // System.out.println("version value: " + request.version.value);
        // System.out.println("negoToken value: " + ((NegoItem)
        // request.negoTokens.tags[0]).negoToken.value);

        // Write request to buffer and compare with original
        ByteBuffer toWriteBuf = new ByteBuffer(packet.length + 100, true);
        request.writeTag(toWriteBuf);
        toWriteBuf.trimAtCursor();

        if (!toReadBuf.equals(toWriteBuf))
            throw new RuntimeException("Data written to buffer is not equal to data read from buffer. \nExpected: " + toReadBuf + "\nActual: " + toWriteBuf + ".");
    }

}
