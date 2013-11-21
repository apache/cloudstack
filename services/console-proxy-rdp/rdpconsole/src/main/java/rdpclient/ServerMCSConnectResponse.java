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
import streamer.Link;
import streamer.OneTimeSwitch;

/**
 * Once the basic server settings data blocks have been processed successfully, the client MUST send the MCS Attach User Request PDU to the server.
 *
 * @see http://msdn.microsoft.com/en-us/library/cc240682.aspx
 */
public class ServerMCSConnectResponse extends OneTimeSwitch {

    public ServerMCSConnectResponse(String id) {
        super(id);
    }

    @Override
    protected void handleOneTimeData(ByteBuffer buf, Link link) {
        if (buf == null)
            return;

        if (verbose)
            System.out.println("[" + this + "] INFO: Data received: " + buf + ".");

        // Ignore packet
        buf.unref();
        switchOff();
    }

}

/*
 * @formatter:off
 * 03 00 00 64 02 F0 80 7F 66 5A 0A 01 00 02 01 00 30 1A 02 01 22 02 01 03 02 01 00 02 01 01 02 01 00 02 01 01 02 03 00 FF F8 02 01 02 04 36 00 05 00 14 7C 00 01 2A 14 76 0A 01 01 00 01 C0 00 4D 63 44 6E 20 01 0C 0C 00 04 00 08 00 01 00 00 00 03 0C 08 00 EB 03 00 00 02 0C 0C 00 00 00 00 00 00 00 00 00

  Frame: Number = 12, Captured Frame Length = 157, MediaType = DecryptedPayloadHeader
+ DecryptedPayloadHeader: FrameCount = 1, ErrorStatus = SUCCESS
  TLSSSLData: Transport Layer Security (TLS) Payload Data
+ TLS: TLS Rec Layer-1 SSL Application Data
  ISOTS: TPKTCount = 1
- TPKT: version: 3, Length: 100
    version: 3 (0x3)
    Reserved: 0 (0x0)
    PacketLength: 100 (0x64)
- X224: Data
    Length: 2 (0x2)
    Type: Data
    EOT: 128 (0x80)
- T125: MCSConnect Response
  - MCSConnectResponse: Result = rt-successful
   - ConnectResponseHeader:
    - AsnId: Application Constructed Tag (102)
     - HighTag:
        Class:     (01......) Application (1)
        Type:      (..1.....) Constructed
        TagNumber: (...11111)
        TagValueEnd: 102 (0x66)
    - AsnLen: Length = 90, LengthOfLength = 0
       Length: 90 bytes, LengthOfLength = 0
   - Result: rt-successful
    - Value: 0
     - AsnIntegerHeader:
      - AsnId: Enumerated type (Universal 10)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...01010) 10
      - AsnLen: Length = 1, LengthOfLength = 0
         Length: 1 bytes, LengthOfLength = 0
       AsnInt: 0 (0x0)
   - CalledConnectId: 0
    - AsnIntegerHeader:
     - AsnId: Integer type (Universal 2)
      - LowTag:
         Class:    (00......) Universal (0)
         Type:     (..0.....) Primitive
         TagValue: (...00010) 2
     - AsnLen: Length = 1, LengthOfLength = 0
        Length: 1 bytes, LengthOfLength = 0
      AsnInt: 0 (0x0)
   - DomainParameters: Length = 26, LengthOfLength = 0
    - DomainParametersHeader: 0x1
     - AsnId: Sequence and SequenceOf types (Universal 16)
      - LowTag:
         Class:    (00......) Universal (0)
         Type:     (..1.....) Constructed
         TagValue: (...10000) 16
     - AsnLen: Length = 26, LengthOfLength = 0
        Length: 26 bytes, LengthOfLength = 0
    - ChannelIds: 34
     - AsnIntegerHeader:
      - AsnId: Integer type (Universal 2)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...00010) 2
      - AsnLen: Length = 1, LengthOfLength = 0
         Length: 1 bytes, LengthOfLength = 0
       AsnInt: 34 (0x22)
    - UserIDs: 3
     - AsnIntegerHeader:
      - AsnId: Integer type (Universal 2)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...00010) 2
      - AsnLen: Length = 1, LengthOfLength = 0
         Length: 1 bytes, LengthOfLength = 0
       AsnInt: 3 (0x3)
    - TokenIds: 0
     - AsnIntegerHeader:
      - AsnId: Integer type (Universal 2)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...00010) 2
      - AsnLen: Length = 1, LengthOfLength = 0
         Length: 1 bytes, LengthOfLength = 0
       AsnInt: 0 (0x0)
    - NumPriorities: 1
     - AsnIntegerHeader:
      - AsnId: Integer type (Universal 2)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...00010) 2
      - AsnLen: Length = 1, LengthOfLength = 0
         Length: 1 bytes, LengthOfLength = 0
       AsnInt: 1 (0x1)
    - MinThroughput: 0
     - AsnIntegerHeader:
      - AsnId: Integer type (Universal 2)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...00010) 2
      - AsnLen: Length = 1, LengthOfLength = 0
         Length: 1 bytes, LengthOfLength = 0
       AsnInt: 0 (0x0)
    - Height: 1
     - AsnIntegerHeader:
      - AsnId: Integer type (Universal 2)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...00010) 2
      - AsnLen: Length = 1, LengthOfLength = 0
         Length: 1 bytes, LengthOfLength = 0
       AsnInt: 1 (0x1)
    - MCSPDUsize: 65528
     - AsnIntegerHeader:
      - AsnId: Integer type (Universal 2)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...00010) 2
      - AsnLen: Length = 3, LengthOfLength = 0
         Length: 3 bytes, LengthOfLength = 0
       AsnInt: 65528 (0xFFF8)
    - protocolVersion: 2
     - AsnIntegerHeader:
      - AsnId: Integer type (Universal 2)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...00010) 2
      - AsnLen: Length = 1, LengthOfLength = 0
         Length: 1 bytes, LengthOfLength = 0
       AsnInt: 2 (0x2)
   - UserData: Identifier = Generic Conference Contro (0.0.20.124.0.1)
    - UserDataHeader:
     - AsnId: OctetString type (Universal 4)
      - LowTag:
         Class:    (00......) Universal (0)
         Type:     (..0.....) Primitive
         TagValue: (...00100) 4
     - AsnLen: Length = 54, LengthOfLength = 0
        Length: 54 bytes, LengthOfLength = 0
    - AsnBerObjectIdentifier: Generic Conference Contro (0.0.20.124.0.1)
     - AsnObjectIdentifierHeader:
      - AsnId: Reserved for use by the encoding rules (Universal 0)
       - LowTag:
          Class:    (00......) Universal (0)
          Type:     (..0.....) Primitive
          TagValue: (...00000) 0
      - AsnLen: Length = 5, LengthOfLength = 0
         Length: 5 bytes, LengthOfLength = 0
       First: 0 (0x0)
       Final: 20 (0x14)
       Final: 124 (0x7C)
       Final: 0 (0x0)
       Final: 1 (0x1)
    - ConnectPDULength: 42
       Align: No Padding
       Length: 42
    - ConnectGCCPDU: conferenceCreateResponse
       ExtensionBit: 0 (0x0)
     - ChoiceValue: conferenceCreateResponse
        Value: (001.....) 0x1
     - conferenceCreateResponse:
        ExtensionBit: 0 (0x0)
        userDataPresent: 1 (0x1)
      - nodeID: 0x79f3
       - UserID: 31219
        - Align: No Padding
           Padding2: (00......) 0x0
          Value: 30218 (0x760A)
      - tag: 1 (0x1)
       - Length: 1
          Align: No Padding
          Length: 1
         Value: 1 (0x1)
      - result: success
         ExtensionBit: 0 (0x0)
       - RootIndex: 0
          Value: (000.....) 0x0
      - userData:
       - Size: 1
        - Align: No Padding
           Padding4: (0000....) 0x0
          Length: 1
       - UserData: 0x4d63446e
          valuePresent: 1 (0x1)
        - key: h221NonStandard
         - ChoiceValue: h221NonStandard
            Value: (1.......) 0x1
         - h221NonStandard:
          - H221NonStandardIdentifier: length: 4
           - ConstrainedLength: 4
              Value: (00000000) 0x0
           - Align: No Padding
              Padding6: (000000..) 0x0
             Value: Binary Large Object (4 Bytes)
        - ServerMcsConnectResponsePdu:
         - RDPGCCUserDataResponseLength: 32
            Align: No Padding
            Length: 32
         - TsUd: SC_CORE
          - TsUdHeader: Type = SC_CORE, Length = 12
             Type: SC_CORE
             Length: 12 (0xC)
          - TsUdScCore:
             Version: RDP 5.0, 5.1, 5.2, 6.0, 6.1, and 7.0
             ClientRequestedProtocols: TLS 1.0
         - TsUd: SC_NET
          - TsUdHeader: Type = SC_NET, Length = 8
             Type: SC_NET
             Length: 8 (0x8)
          - TsUdScNet:
             MCSChannelID: 1003 (0x3EB)
             ChannelCount: 0 (0x0)
             Pad: 0 Bytes
         - TsUd: SC_SECURITY
          - TsUdHeader: Type = SC_SECURITY, Length = 12
             Type: SC_SECURITY
             Length: 12 (0xC)
          - TsUdSCSec1:
           - EncryptionMethod:
              Support40Bit:  (...............................0) Not Support
              Support128Bit: (..............................0.) Not Support 128-bit
              Reserved1:     (.............................0..)
              Support56Bit:  (............................0...) Not Support 56-bit
              SupportFIPS:   (...........................0....) Not Support FIPS Compliant
              Reserved2:     (000000000000000000000000000.....)
             EncryptionLevel: TS_ENCRYPTION_NONE
 */
