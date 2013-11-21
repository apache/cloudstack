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

public class ServerLicenseErrorPDUValidClient extends OneTimeSwitch {

    public ServerLicenseErrorPDUValidClient(String id) {
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

    /* @formatter:off */
//   * Server Error alert
//
//03 00 00 22 02 F0 80 68 00 01 03 EB 70 14 80 00 F1 BC FF 03 10 00 07 00 00 00 02 00 00 00 04 00 00 00
//
//
// Frame: Number = 30, Captured Frame Length = 91, MediaType = DecryptedPayloadHeader
//+ DecryptedPayloadHeader: FrameCount = 1, ErrorStatus = SUCCESS
// TLSSSLData: Transport Layer Security (TLS) Payload Data
//+ TLS: TLS Rec Layer-1 SSL Application Data
// ISOTS: TPKTCount = 1
//- TPKT: version: 3, Length: 34
//    version: 3 (0x3)
//    Reserved: 0 (0x0)
//    PacketLength: 34 (0x22)
//- X224: Data
//    Length: 2 (0x2)
//    Type: Data
//    EOT: 128 (0x80)
//- T125: Data Packet
// - MCSHeader: Type=Send Data Indication, UserID=1002, ChannelID=1003
//  - Type: Send Data Indication
//    - RootIndex: 26
//      Value: (011010..) 0x1a
//  - UserID: 0x3ea
//    - UserID: 0x3ea
//    - ChannelId: 1002
//     - Align: No Padding
//        Padding2: (00......) 0x0
//       Value: 1 (0x1)
//  - Channel: 0x3eb
//    - ChannelId: 1003
//      Align: No Padding
//      Value: 1003 (0x3EB)
//  - DataPriority: high
//    - DataPriority: high
//    - RootIndex: 1
//       Value: (01......) 0x1
//  - Segmentation: Begin End
//     Begin: (1.......) Begin
//     End:   (.1......) End
//  - Length: 20
//    - Align: No Padding
//      Padding4: (0000....) 0x0
//     Length: 20
//    RDP: RDPBCGR
//- RDPBCGR: RDPELE
// - SecurityHeader: License Packet
//  - Flags: 128 (0x80)
//     SecurityExchange:        (...............0) Not Security Exchange PDU
//     Reserved1:               (.............00.) Reserved
//     Encrypted:               (............0...) Not Encrypted packet
//     ResetSeqNumber:          (...........0....) MUST be ignored.
//     IgnoreSeqNumber:         (..........0.....) MUST be ignored.
//     InfoPacket:              (.........0......) Not Client Info PDU
//     LicensePacket:           (........1.......) License Packet
//     Reserved2:               (.......0........) Reserved
//     LicensePacketEncryption: (......0.........) Not License Packet Encryption
//     ServerRedirectionPacket: (.....0..........) Not Standard Security Server Redirection PDU
//     ImprovedChecksumForMACG: (....0...........) Not Improved Checksum for MAC Generation
//     Reserved3:               (.000............) Reserved
//     FlagsHiValid:            (0...............) FlagsHi should be ignored
//    FlagsHi: Should be ignored
//- RDPELE: GM_ERROR_ALERT
// - TsPreambleHeader: Type = GM_ERROR_ALERT
//    MsgType: GM_ERROR_ALERT
//  - Flags: 3 (0x3)
//     LicenseProtocolVersionMask: (....0011) RDP 5.0, 5.1, 5.2, 6.0, 6.1, and 7.0
//     Unused:                     (.000....)
//     ExtendedErrorMSGsupported:  (0.......) that extended error information using the License Error Message is NOT supported.
//    MsgSize: 16 (0x10)
// - TsLicenseErrorMessage: ErrorCode = STATUS_VALID_CLIENT
//    ErrorCode: STATUS_VALID_CLIENT
//    StateTransition: ST_NO_TRANSITION
//  - LiceseBinaryBlob: Type = Not Available
//     RandomData: This value should be ignored
//     BlobLen: 0 (0x0)
//

}
