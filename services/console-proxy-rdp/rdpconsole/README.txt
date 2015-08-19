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
This project contains code for basic VNC, RDP, and HyperV (RDP) clients.

Usage: 
  java common.Client vnc|rdp|hyperv OPTIONS

Common options:
  --help|-h	Show this help text.
  --debug-link|-DL	Print debugging messages when packets are trasnferred via links.
  --debug-element|-DE	Print debugging messages when packets are received or sent by elements.
  --debug-pipeline|-DP	Print debugging messages in pipelines.
  --host|-n|--host-name VALUE	Name or IP address of host to connect to. Required.
  --width|-W VALUE	Width of canvas. Default value is "1024".
  --height|-H VALUE	Height of canvas. Default value is "768".

VNC options:
  --port|-p VALUE	Port of VNC display server to connect to. Calculate as 5900 + display number, e.g. 5900 for display #0, 5901 for display #1, and so on. Default value is "5901".
  --password|-P VALUE	Password to use. Required.

RDP options:
  --ssl-implementation|-j jre|apr|bco	Select SSL engine to use: JRE standard implementation, Apache Portable Runtime native library, BonuncyCastle.org implementation. Default value is "apr".
  --port|-p VALUE	Port of RDP server to connect to. Default value is "3389".
  --domain|-D VALUE	NTLM domain to login into. Default value is "Workgroup".
  --user|-U VALUE	User name to use. Default value is "Administrator".
  --password|-P VALUE	Password to use. If omitted, then login screen will be shown.

HyperV options:
  --ssl-implementation|-j jre|apr|bco	Select SSL engine to use: JRE standard implementation, Apache Portable Runtime native library, BonuncyCastle.org implementation. Default value is "apr".
  --port|-p VALUE	Port of HyperV server to connect to. Default value is "2179".
  --instance|-i VALUE	HyperV instance ID to use. Required.
  --domain|-D VALUE	NTLM domain to login into. Default value is "Workgroup".
  --user|-U VALUE	User name to use. Default value is "Administrator".
  --password|-P VALUE	Password to use. Required.


Limitations of VNC client:
  * only basic functionality work.

Limitations of RDP client:
  * it uses SSL/TLS;
  * NLA is not supported;
  * only basic functionality work.


To configure and start RDP service properly, run rdp-config.bat on server.
