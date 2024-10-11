rem Licensed to the Apache Software Foundation (ASF) under one
rem or more contributor license agreements.  See the NOTICE file
rem distributed with this work for additional information
rem regarding copyright ownership.  The ASF licenses this file
rem to you under the Apache License, Version 2.0 (the
rem "License"); you may not use this file except in compliance
rem with the License.  You may obtain a copy of the License at
rem 
rem   http://www.apache.org/licenses/LICENSE-2.0
rem 
rem Unless required by applicable law or agreed to in writing,
rem software distributed under the License is distributed on an
rem "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
rem KIND, either express or implied.  See the License for the
rem specific language governing permissions and limitations
rem under the License.

rem 
rem Configure and start RDP service. Run this script on RDP server.
rem 
 
rem Turn off firewall

netsh advfirewall firewall set rule group="Remote Desktop" new enable=yes

rem Enable TS connections

reg add "HKLM\System\CurrentControlSet\Control\Terminal Server" /v "AllowTSConnections" /t REG_DWORD /d 1 /f
reg add "HKLM\System\CurrentControlSet\Control\Terminal Server" /v "fDenyTSConnections" /t REG_DWORD /d 0 /f

rem Disable RDP NLA

reg add "HKLM\System\CurrentControlSet\Control\Terminal Server\WinStations\RDP-Tcp" /v UserAuthentication /t REG_DWORD /d 0 /f

rem Enable TS service

sc config TermService start=auto

rem Start TS service

net start Termservice
