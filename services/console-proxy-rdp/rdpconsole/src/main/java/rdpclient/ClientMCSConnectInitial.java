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

public class ClientMCSConnectInitial extends OneTimeSwitch {

    public ClientMCSConnectInitial(String id) {
        super(id);
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

        int length = 1024; // Large enough
        ByteBuffer buf = new ByteBuffer(length, true);

        /* @formatter:off */
    buf.writeBytes(new byte[] {
//        - T125: MCSConnect Initial
//        - MCSConnectInitial: Identifier=Generic Conference Control (0.0.20.124.0.1), ConnectPDULength=254
//         - ConnectInitialHeader:
      (byte)0x7F, (byte)0x65,
//          - AsnId: Application Constructed Tag (101)
//           - HighTag:
//              Class:     (01......) Application (1)
//              Type:      (..1.....) Constructed
//              TagNumber: (...11111)
//              TagValueEnd: 101 (0x65)
      (byte)0x82, (byte)0x01, (byte)0x6C,
//          - AsnLen: Length = 364, LengthOfLength = 2
//             LengthType: LengthOfLength = 2
//             Length: 364 bytes
      (byte)0x04, (byte)0x01, (byte)0x01,
//         - CallingDomainSelector: 0x1
//          - AsnOctetStringHeader:
//           - AsnId: OctetString type (Universal 4)
//            - LowTag:
//               Class:    (00......) Universal (0)
//               Type:     (..0.....) Primitive
//               TagValue: (...00100) 4
//           - AsnLen: Length = 1, LengthOfLength = 0
//              Length: 1 bytes, LengthOfLength = 0
//            OctetStream: 0x1
      (byte)0x04, (byte)0x01, (byte)0x01,
//         - CalledDomainSelector: 0x1
//          - AsnOctetStringHeader:
//           - AsnId: OctetString type (Universal 4)
//            - LowTag:
//               Class:    (00......) Universal (0)
//               Type:     (..0.....) Primitive
//               TagValue: (...00100) 4
//           - AsnLen: Length = 1, LengthOfLength = 0
//              Length: 1 bytes, LengthOfLength = 0
//            OctetStream: 0x1
      (byte)0x01, (byte)0x01, (byte)0xFF,
//         - UpwardFlag: True
//          - AsnBooleanHeader:
//           - AsnId: Boolean type (Universal 1)
//            - LowTag:
//               Class:    (00......) Universal (0)
//               Type:     (..0.....) Primitive
//               TagValue: (...00001) 1
//           - AsnLen: Length = 1, LengthOfLength = 0
//              Length: 1 bytes, LengthOfLength = 0
//            Tf: 255 (0xFF)

//
//         - TargetParameters: Length = 26, LengthOfLength = 0
      (byte)0x30, (byte)0x1A,
//          - DomainParametersHeader: 0x1
//           - AsnId: Sequence and SequenceOf types (Universal 16)
//            - LowTag:
//               Class:    (00......) Universal (0)
//               Type:     (..1.....) Constructed
//               TagValue: (...10000) 16
//           - AsnLen: Length = 26, LengthOfLength = 0
//              Length: 26 bytes, LengthOfLength = 0
      (byte)0x02, (byte)0x01, (byte)0x22,
//          - ChannelIds: 34
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 34 (0x22)
      (byte)0x02, (byte)0x01, (byte)0x02,
//          - UserIDs: 2
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 2 (0x2)
      (byte)0x02, (byte)0x01, (byte)0x00,
//          - TokenIds: 0
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 0 (0x0)
      (byte)0x02, (byte)0x01, (byte)0x01,
//          - NumPriorities: 1
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 1 (0x1)
      (byte)0x02, (byte)0x01, (byte)0x00,
//          - MinThroughput: 0
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 0 (0x0)
      (byte)0x02, (byte)0x01, (byte)0x01,
//          - Height: 1
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 1 (0x1)
      (byte)0x02, (byte)0x03, (byte)0x00, (byte)0xFF, (byte)0xFF,
//          - MCSPDUsize: 65535
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 3, LengthOfLength = 0
//               Length: 3 bytes, LengthOfLength = 0
//             AsnInt: 65535 (0xFFFF)
      (byte)0x02, (byte)0x01, (byte)0x02,
//          - protocolVersion: 2
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 2 (0x2)

//
//         - MinimumParameters: Length = 25, LengthOfLength = 0
      (byte)0x30, (byte)0x19,
//          - DomainParametersHeader: 0x1
//           - AsnId: Sequence and SequenceOf types (Universal 16)
//            - LowTag:
//               Class:    (00......) Universal (0)
//               Type:     (..1.....) Constructed
//               TagValue: (...10000) 16
//           - AsnLen: Length = 25, LengthOfLength = 0
//              Length: 25 bytes, LengthOfLength = 0
      (byte)0x02, (byte)0x01, (byte)0x01,
//          - ChannelIds: 1
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 1 (0x1)
      (byte)0x02, (byte)0x01, (byte)0x01,
//          - UserIDs: 1
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 1 (0x1)
      (byte)0x02, (byte)0x01, (byte)0x01,
//          - TokenIds: 1
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 1 (0x1)
      (byte)0x02, (byte)0x01, (byte)0x01,
//          - NumPriorities: 1
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 1 (0x1)
      (byte)0x02, (byte)0x01, (byte)0x00,
//          - MinThroughput: 0
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 0 (0x0)
      (byte)0x02, (byte)0x01, (byte)0x01,
//          - Height: 1
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 1 (0x1)
      (byte)0x02, (byte)0x02, (byte)0x04, (byte)0x20,
//          - MCSPDUsize: 1056
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 2, LengthOfLength = 0
//               Length: 2 bytes, LengthOfLength = 0
//             AsnInt: 1056 (0x420)
      (byte)0x02, (byte)0x01, (byte)0x02,
//          - protocolVersion: 2
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 2 (0x2)
//         - MaximumParameters: Length = 31, LengthOfLength = 0
//          - DomainParametersHeader: 0x1
      (byte)0x30, (byte)0x1F,
//           - AsnId: Sequence and SequenceOf types (Universal 16)
//            - LowTag:
//               Class:    (00......) Universal (0)
//               Type:     (..1.....) Constructed
//               TagValue: (...10000) 16
//           - AsnLen: Length = 31, LengthOfLength = 0
//              Length: 31 bytes, LengthOfLength = 0
      (byte)0x02, (byte)0x03, (byte)0x00, (byte)0xFF, (byte)0xFF,
//          - ChannelIds: 65535
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 3, LengthOfLength = 0
//               Length: 3 bytes, LengthOfLength = 0
//             AsnInt: 65535 (0xFFFF)
      (byte)0x02, (byte)0x02, (byte)0xFC, (byte)0x17,
//          - UserIDs: 64535
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 2, LengthOfLength = 0
//               Length: 2 bytes, LengthOfLength = 0
//             AsnInt: 64535 (0xFC17)
      (byte)0x02, (byte)0x03, (byte)0x00, (byte)0xFF, (byte)0xFF,
//          - TokenIds: 65535
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 3, LengthOfLength = 0
//               Length: 3 bytes, LengthOfLength = 0
//             AsnInt: 65535 (0xFFFF)
      (byte)0x02, (byte)0x01, (byte)0x01,
//          - NumPriorities: 1
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 1 (0x1)
      (byte)0x02, (byte)0x01, (byte)0x00,
//          - MinThroughput: 0
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 0 (0x0)
      (byte)0x02, (byte)0x01, (byte)0x01,
//          - Height: 1
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 1 (0x1)
      (byte)0x02, (byte)0x03, (byte)0x00, (byte)0xFF, (byte)0xFF,
//          - MCSPDUsize: 65535
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 3, LengthOfLength = 0
//               Length: 3 bytes, LengthOfLength = 0
//             AsnInt: 65535 (0xFFFF)
      (byte)0x02, (byte)0x01, (byte)0x02,
//          - protocolVersion: 2
//           - AsnIntegerHeader:
//            - AsnId: Integer type (Universal 2)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00010) 2
//            - AsnLen: Length = 1, LengthOfLength = 0
//               Length: 1 bytes, LengthOfLength = 0
//             AsnInt: 2 (0x2)
//         - UserData: Identifier=Generic Conference Contro (0.0.20.124.0.1), ConnectPDULength=254
//          - UserDataHeader:
      (byte)0x04, (byte)0x82, (byte)0x01, (byte)0x07,
//           - AsnId: OctetString type (Universal 4)
//            - LowTag:
//               Class:    (00......) Universal (0)
//               Type:     (..0.....) Primitive
//               TagValue: (...00100) 4
//           - AsnLen: Length = 263, LengthOfLength = 2
//              LengthType: LengthOfLength = 2
//              Length: 263 bytes
      (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x14, (byte)0x7C, (byte)0x00, (byte)0x01,
//          - AsnBerObjectIdentifier: Generic Conference Contro (0.0.20.124.0.1)
//           - AsnObjectIdentifierHeader:
//            - AsnId: Reserved for use by the encoding rules (Universal 0)
//             - LowTag:
//                Class:    (00......) Universal (0)
//                Type:     (..0.....) Primitive
//                TagValue: (...00000) 0
//            - AsnLen: Length = 5, LengthOfLength = 0
//               Length: 5 bytes, LengthOfLength = 0
//             First: 0 (0x0)
//             Final: 20 (0x14)
//             Final: 124 (0x7C)
//             Final: 0 (0x0)
//             Final: 1 (0x1)
      (byte)0x80, (byte)0xFE,
//          - ConnectPDULength: 254
//             Align: No Padding
//             Length: 254
      (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x10,
//          - ConnectGCCPDU: conferenceCreateRequest
//             ExtensionBit: 0 (0x0)
//           - ChoiceValue: conferenceCreateRequest
//              Value: (000.....) 0x0
//           - conferenceCreateRequest:
//              ExtensionBit: 0 (0x0)
//              convenerPasswordPresent: 0 (0x0)
//              passwordPresent: 0 (0x0)
//              conductorPrivilegesPresent: 0 (0x0)
//              conductedPrivilegesPresent: 0 (0x0)
//              nonConductedPrivilegesPresent: 0 (0x0)
//              conferenceDescriptionPresent: 0 (0x0)
//              callerIdentifierPresent: 0 (0x0)
//              userDataPresent: 1 (0x1)
//            - conferenceName:
//               ExtensionBit: 0 (0x0)
//               textPresent: 0 (0x0)
//             - numeric: 1
//              - SimpleNumericString: 1
//               - NumericString: 1
//                - Align: No Padding
//                   Padding1: (0.......) 0x0
//                - Length: 1
//                   Value: (00000000) 0x0
//                - Restrictedstr: 1
//                   FourBits: (0001....) 0x1
//            - lockedConference: False
//               Value: False 0.......
//            - listedConference: False
//               Value: False 0.......
//            - conductibleConference: False
//               Value: False 0.......
//            - TerminationMethod: automatic
//               ExtensionBit: 0 (0x0)
//             - RootIndex: 0
//                Value: (0.......) 0x0
//            - userData:
      (byte)0x00, (byte)0x01,
//             - Size: 1
//              - Align: No Padding
//                 Padding7: (0000000.) 0x0
//                Length: 1
//             - UserData: 0x44756361
      (byte)0xC0, (byte)0x00, (byte)0x44, (byte)0x75, (byte)0x63, (byte)0x61,
//                valuePresent: 1 (0x1)
//              - key: h221NonStandard "Duca"
//               - ChoiceValue: h221NonStandard
//                  Value: (1.......) 0x1
//               - h221NonStandard:
//                - H221NonStandardIdentifier: length: 4
//                 - ConstrainedLength: 4
//                    Value: (00000000) 0x0
//                 - Align: No Padding
//                    Padding6: (000000..) 0x0
//                   Value: Binary Large Object (4 Bytes) "Duca"
//              - ClientMcsConnectInitialPdu:
      (byte)0x80, (byte)0xF0,
//               - RDPGCCUserDataRequestLength: 240
//                  Align: No Padding
//                  Length: 240
//               - TsUd: CS_CORE
      (byte)0x01, (byte)0xC0, (byte)0xD8, (byte)0x00,
//                - TsUdHeader: Type = CS_CORE, Length = 216
//                   Type: CS_CORE
//                   Length: 216 (0xD8)
//                - TsUdCsCore:
      (byte)0x04, (byte)0x00, (byte)0x08, (byte)0x00,
//                   Version: RDP 5.0, 5.1, 5.2, 6.0, 6.1, and 7.0
      (byte)0x00, (byte)0x04,
//                   DesktopWidth: 1024 (0x400)
      (byte)0x00, (byte)0x03,
//                   DesktopHeight: 768 (0x300)
      (byte)0x01, (byte)0xCA,
//                   ColorDepth: 8 bpp
      (byte)0x03, (byte)0xAA,
//                   SASSequence: 0xaa03, SHOULD be set to RNS_UD_SAS_DEL(0xAA03)
      (byte)0x09, (byte)0x04, (byte)0x00, (byte)0x00,
//                   KeyboardLayout: Language: English, Location: United States
      (byte)0x28, (byte)0x0A, (byte)0x00, (byte)0x00,
//                   ClientBuild: 2600 (0xA28)
      (byte)0x61, (byte)0x00, (byte)0x70, (byte)0x00, (byte)0x6F, (byte)0x00, (byte)0x6C, (byte)0x00, (byte)0x6C, (byte)0x00, (byte)0x6F, (byte)0x00, (byte)0x33, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//                   ClientName: apollo3
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//                   KeyboardType: Undefined value: 0
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//                   KeyboardSubType: 0 (0x0)
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//                   KeyboardFunctionKey: 0 (0x0)
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//                   ImeFileName:
      (byte)0x01, (byte)0xCA,
//                   PostBeta2ColorDepth: 8 bpp
      (byte)0x01, (byte)0x00,
//                   ClientProductId: 0x1, SHOULD be set to initialized to 1
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//                   SerialNumber: 0x0, SHOULD be set to 0
      (byte)0x10, (byte)0x00,
//                   HighColorDepth: 16-bit 565 RGB
      (byte)0x07, (byte)0x00,
//                 - SupportedColorDepth: 7 (0x7)
//                    Support24BPP: (...............1) Support 24BPP
//                    Support16BPP: (..............1.) Support 16BPP
//                    Support15BPP: (.............1..) Support 15BPP
//                    Support32BPP: (............0...) Not Support 32BPP
//                    Reserved:     (000000000000....)
      (byte)0x01, (byte)0x00,
//                 - EarlyCapabilityFlags: 1 (0x1)
//                    SupportSetErrorPdu:      (...............1) Indicates that the client supports the Set Error Info PDU
//                    Want32BppSession:        (..............0.) Client is not requesting 32BPP session
//                    SupportStatusInfoPdu:    (.............0..) Client not supports the Server Status Info PDU
//                    StrongAsymmetricKeys:    (............0...) Not support asymmetric keys larger than 512-bits
//                    Unused:                  (...........0....)
//                    ValidConnection:         (..........0.....) Not Indicates ConnectionType field contains valid data
//                    SupportMonitorLayoutPdu: (.........0......) Not Indicates that the client supports the Monitor Layout PDU
//                    Unused2:                 (000000000.......)
      (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//      ClientDigProductId:
(byte)0x00,
//      connectionType: invalid connection type
(byte)0x00,
//      pad1octet: 0 (0x0)
(byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
//      ServerSelectedProtocols: TLS 1.0
//
//  - TsUd: CS_CLUSTER
//   - TsUdHeader: Type = CS_CLUSTER, Length = 12
(byte)0x04, (byte)0xC0,
//      Type: CS_CLUSTER
(byte)0x0C, (byte)0x00,
//      Length: 12 (0xC)
(byte)0x0D, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//   - TsUdCsCluster:
//    - Flags: 13 (0xD)
//       RedirectedSupported: (...............................1) Support Redirected
//       SessionIDFieldValid: (..............................0.) SessionID Field not Valid
//       SupportedVersion:    (..........................0011..) REDIRECTION_VERSION4
//       RedirectedSmartcard: (.........................0......) Not Logon with Smartcard
//       Unused:           (0000000000000000000000000.......)
//      RedirectedSessionID: 0 (0x0)
//
//  - TsUd: CS_SECURITY
//   - TsUdHeader: Type = CS_SECURITY, Length = 12
(byte)0x02, (byte)0xC0,
//      Type: CS_SECURITY
(byte)0x0C, (byte)0x00,
//      Length: 12 (0xC)
//
//   - TsUdCsSec:
(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//    - EncryptionMethod:
//       Support40Bit:  (...............................0) Not Support
//       Support128Bit: (..............................0.) Not Support 128-bit
//       Reserved1:     (.............................0..)
//       Support56Bit:  (............................0...) Not Support 56-bit
//       SupportFIPS:   (...........................0....) Not Support FIPS Compliant
//       Reserved2:     (000000000000000000000000000.....)
(byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
//    - ExtEncryptionMethod:
//       Support40Bit:  (...............................0) Not Support
//       Support128Bit: (..............................0.) Not Support 128-bit
//       Reserved1:     (.............................0..)
//       Support56Bit:  (............................0...) Not Support 56-bit
//       SupportFIPS:   (...........................0....) Not Support FIPS Compliant
//       Reserved2:     (000000000000000000000000000.....)
    });
    /* @formatter:on */

        buf.length = buf.cursor;

        pushDataToOTOut(buf);

        switchOff();
    }

    /**
     * Example.
     *
     * @see http://msdn.microsoft.com/en-us/library/cc240836.aspx
     */
    public static void main(String args[]) {
        // System.setProperty("streamer.Link.debug", "true");
        System.setProperty("streamer.Element.debug", "true");
        // System.setProperty("streamer.Pipeline.debug", "true");

        /* @formatter:off */
    byte[] packet = new byte[] {
        // TPKT: TPKT version = 3
        (byte) 0x03,  (byte) 0x00,
        // TPKT: Packet length: 378 bytes
        (byte) 0x01,  (byte) 0x78,

        // X.224: Length indicator = 2
        (byte) 0x02,
        // X.224: Type: Data TPDU
        (byte) 0xf0,
        // X.224: EOT
        (byte) 0x80,

        // Captured packet
        (byte)0x7f, (byte)0x65, (byte)0x82, (byte)0x01, (byte)0x6c, (byte)0x04, (byte)0x01, (byte)0x01, (byte)0x04,
        (byte)0x01, (byte)0x01, (byte)0x01, (byte)0x01, (byte)0xff, (byte)0x30, (byte)0x1a, (byte)0x02, (byte)0x01, (byte)0x22, (byte)0x02, (byte)0x01, (byte)0x02, (byte)0x02, (byte)0x01, (byte)0x00,
        (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0x02, (byte)0x01,
        (byte)0x02, (byte)0x30, (byte)0x19, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02,
        (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x02, (byte)0x04, (byte)0x20, (byte)0x02, (byte)0x01, (byte)0x02, (byte)0x30, (byte)0x1f, (byte)0x02, (byte)0x03,
        (byte)0x00, (byte)0xff, (byte)0xff, (byte)0x02, (byte)0x02, (byte)0xfc, (byte)0x17, (byte)0x02, (byte)0x03, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02,
        (byte)0x01, (byte)0x00, (byte)0x02, (byte)0x01, (byte)0x01, (byte)0x02, (byte)0x03, (byte)0x00, (byte)0xff, (byte)0xff, (byte)0x02, (byte)0x01, (byte)0x02, (byte)0x04, (byte)0x82, (byte)0x01,
        (byte)0x07, (byte)0x00, (byte)0x05, (byte)0x00, (byte)0x14, (byte)0x7c, (byte)0x00, (byte)0x01, (byte)0x80, (byte)0xfe, (byte)0x00, (byte)0x08, (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x01,
        (byte)0xc0, (byte)0x00, (byte)0x44, (byte)0x75, (byte)0x63, (byte)0x61, (byte)0x80, (byte)0xf0, (byte)0x01, (byte)0xc0, (byte)0xd8, (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x08, (byte)0x00,
        (byte)0x00, (byte)0x04, (byte)0x00, (byte)0x03, (byte)0x01, (byte)0xca, (byte)0x03, (byte)0xaa, (byte)0x09, (byte)0x04, (byte)0x00, (byte)0x00, (byte)0x28, (byte)0x0a, (byte)0x00, (byte)0x00,
        (byte)0x61, (byte)0x00, (byte)0x70, (byte)0x00, (byte)0x6f, (byte)0x00, (byte)0x6c, (byte)0x00, (byte)0x6c, (byte)0x00, (byte)0x6f, (byte)0x00, (byte)0x33, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0xca, (byte)0x01, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x10, (byte)0x00, (byte)0x07, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00,
        (byte)0x04, (byte)0xc0, (byte)0x0c, (byte)0x00, (byte)0x0d, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x02, (byte)0xc0, (byte)0x0c, (byte)0x00,
        (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00, (byte)0x00,
    };
    /* @formatter:on */

        MockSource source = new MockSource("source", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));
        Element todo = new ClientMCSConnectInitial("ClientMCSConnectInitial");
        Element x224 = new ClientX224DataPdu("x224");
        Element tpkt = new ClientTpkt("tpkt");

        Element sink = new MockSink("sink", ByteBuffer.convertByteArraysToByteBuffers(packet));

        Element mainSink = new MockSink("mainSink", ByteBuffer.convertByteArraysToByteBuffers(new byte[] {1, 2, 3}));

        Pipeline pipeline = new PipelineImpl("test");
        pipeline.add(source, todo, x224, tpkt, sink, mainSink);
        pipeline.link("source", "ClientMCSConnectInitial", "mainSink");
        pipeline.link("ClientMCSConnectInitial >" + OTOUT, "x224", "tpkt", "sink");
        pipeline.runMainLoop("source", STDOUT, false, false);
    }

}
