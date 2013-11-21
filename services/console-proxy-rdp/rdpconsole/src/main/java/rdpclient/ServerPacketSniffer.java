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

/**
 * Try to determine packet content by it header fingerprint.
 */
public class ServerPacketSniffer extends PacketSniffer {

    private static final Pair[] serverRegexps = new Pair[] {
// @formatter:off
  new Pair("Server FastPath update",             "04"),
  new Pair("Server X224ConnectionRequest",       "03 00 XX XX 0E D0"),
  new Pair("Server MCSConnectResponse",          "03 00 XX XX 02 F0 80 7F 66 5A"),
  new Pair("Server AttachUserConfirm",           "03 00 XX XX 02 F0 80 2E"),
  new Pair("Server ChannelJoinConfirm",          "03 00 XX XX 02 F0 80 3E"),
  new Pair("Server ErrorAlert",                  "03 00 XX XX 02 F0 80 68 00 01 03 EB 70 14 80 00"),
  new Pair("Server DemandActivePDU",             "03 00 XX XX 02 F0 80 68 00 01 03 EB 70 XX XX XX XX 11"),
  new Pair("Server ControlPDU",                  "03 00 XX XX 02 F0 80 68 00 01 03 EB 70 XX XX XX 17 00 EA 03 EA 03 XX 00 XX XX XX XX 14"),
  new Pair("Server SynchronizePDU",              "03 00 XX XX 02 F0 80 68 00 01 03 EB 70 XX XX XX 17 00 EA 03 EA 03 XX 00 XX XX XX XX 1F"),
  new Pair("Server FontMapPDU",                  "03 00 XX XX 02 F0 80 68 00 01 03 EB 70 XX XX XX 17 00 EA 03 EA 03 XX 00 XX XX XX XX 28"),
  new Pair("Server SET_ERROR_INFO_PDU",          "03 00 XX XX 02 F0 80 68 00 01 03 EB 30 XX XX XX 17 00 00 00 EA 03 XX 00 XX XX XX XX 2F"),
  new Pair("Server DeactivateAllPDU",            "03 00 XX XX 02 F0 80 68 00 01 03 EB 70 XX XX XX 16 00"),
  new Pair("Server CloseConnection",             "03 00 00 09 02 F0 80 21 80"),

//  new Pair("Server TPKT unknown packet",         "03"),
//  new Pair("Server FastPath update with flags or continuation",  ".*"),
  // @formatter:on

        };

    public ServerPacketSniffer(String id) {
        super(id, serverRegexps);
    }

}
