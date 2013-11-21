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
public class ClientPacketSniffer extends PacketSniffer {

    private static final Pair[] clientRegexps = new Pair[] {
// @formatter:off
    new Pair("Client FastPath input",           "04"),
    new Pair("Client X224ConnectionRequest",    "03 00 XX XX 27 E0"),
    new Pair("Client ConnectionRequest",        "03 00 XX XX XX E0"),
    new Pair("Client MCConnectInitial",         "03 00 XX XX 02 F0 80 7F 65"),
    new Pair("Client ErectDomainRequest",       "03 00 XX XX 02 F0 80 04"),
    new Pair("Client AttachUserRequest",        "03 00 XX XX 02 F0 80 28"),
    new Pair("Client ChannelJoinRequest",       "03 00 XX XX 02 F0 80 38"),
    new Pair("Client Info",                     "03 00 XX XX 02 F0 80 64 00 03 03 EB 70 XX XX XX XX 00 00"),
    new Pair("Client ConfirmActivePDU",         "03 00 XX XX 02 F0 80 64 00 03 03 EB 70 XX XX XX XX 13 00"),
    new Pair("Client SynchronizePDU",           "03 00 XX XX 02 F0 80 64 00 03 03 EB 70 XX XX XX XX 17 00 EC 03 EA 03 XX 00 XX XX XX XX 1F"),
    new Pair("Client ControlPDU",               "03 00 XX XX 02 F0 80 64 00 03 03 EB 70 XX XX XX XX 17 00 EC 03 EA 03 XX 00 XX XX XX XX 14"),
    new Pair("Client FontListPDU",              "03 00 XX XX 02 F0 80 64 00 03 03 EB 70 XX XX XX XX 17 00 EC 03 EA 03 XX 00 XX XX XX XX 27"),
    new Pair("Client BitmapCachePersistentList","03 00 XX XX 02 F0 80 64 00 03 03 EB 70 XX XX XX XX 17 00 EC 03 EA 03 XX XX XX XX XX XX 2b"),
//    new Pair("Client TPKT Unknown packet",      "03"),
//    new Pair("Client UNKNOWN PACKET (ERROR)",   ".*"),
    // @formatter:on

        };

    public ClientPacketSniffer(String id) {
        super(id, clientRegexps);
    }

}
