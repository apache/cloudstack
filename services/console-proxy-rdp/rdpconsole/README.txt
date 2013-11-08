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
This project contains code for basic VNC and RDP clients.

VNC client can be invoked using following command:

  mvn exec:java -Dexec.mainClass="common.Client" -Dexec.args="vnc 192.168.0.101 5901 password"

where
  * vnc - name of protcol;
  * 192.168.0.101 - IP of VNC server;
  * 5901 - port of VNC server screen (5900+display number);
  * password - VNC server password.


RDP client can be invoked using following command:

  mvn exec:java -Dexec.mainClass="common.Client" -Dexec.args="rdp 192.168.0.101 3389 Administrator"

where
  * rdp - name of protcol;
  * 192.168.0.101 - IP of RDP server;
  * 3389 - port of RDP server;
  * Administrator - user name for loging dialog.


Limitations of VNC client:
  * only basic functionality work.

Limitations of RDP client:
  * it uses SSL/TLS;
  * NLA is not supported;
  * only basic functionality work.


To configure and start RDP service properly, run rdp-config.bat on server.
