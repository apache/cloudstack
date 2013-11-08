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

To debug RDP sessions with Network Monitor or Wireshark, you need to
configure RDP server with custom private key. For Network Monitor
Decrypt Expert, you also will need to downgrade RDP server TLS protocol
to version 1.0.

File dev-rdp-config.bat contains instructions to configure RDP to use custom
key, open firewall, disable NLA, downgrade TLS, and start RDP service.

File rdp.pfx contains custom private key (password: test) for use with
rdp-config.bat and Network Monitor Decrypt Expert. If you will generate
your own key, you will need to alter rpd-file.bat to use it
fingerprints.

File rdp-key.pem contains private key in PEM format for use with
Wireshark.
