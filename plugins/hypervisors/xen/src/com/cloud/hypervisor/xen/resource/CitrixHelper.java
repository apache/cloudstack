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
package com.cloud.hypervisor.xen.resource;

import java.util.ArrayList;
import java.util.HashMap;

import org.apache.log4j.Logger;

/**
 * Reduce bloat inside CitrixResourceBase
 *
 */
public class CitrixHelper {
    private static final Logger s_logger = Logger.getLogger(CitrixHelper.class);
    private static final HashMap<String, String> Xcp100GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> Xcp160GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> XenServerGuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> XenServer56FP1GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> XenServer56FP2GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> XenServer600GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> XenServer602GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> XenServer610GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> XenServer620GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, MemoryValues> XenServer620GuestOsMemoryMap = new HashMap<String, MemoryValues>(70);
    private static final HashMap<String, MemoryValues> XenServer610GuestOsMemoryMap = new HashMap<String, MemoryValues>(70);
    private static final HashMap<String, MemoryValues> XenServer602GuestOsMemoryMap = new HashMap<String, MemoryValues>(70);
    private static final HashMap<String, MemoryValues> XenServer600GuestOsMemoryMap = new HashMap<String, MemoryValues>(70);
    private static final HashMap<String, MemoryValues> XenServer56SP2GuestOsMemoryMap = new HashMap<String, MemoryValues>(70);
    private static final HashMap<String, MemoryValues> XenServer56FP1GuestOsMemoryMap = new HashMap<String, MemoryValues>(70);
    private static final ArrayList<String> GuestOsList = new ArrayList<String>(70);

    static {
        Xcp100GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        Xcp100GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        Xcp100GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        Xcp100GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        Xcp100GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        Xcp100GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        Xcp100GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        Xcp100GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        Xcp100GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        Xcp100GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit) (experimental)");
        Xcp100GuestOsMap.put("Debian GNU/Linux 7(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        Xcp100GuestOsMap.put("Debian GNU/Linux 7(64-bit)", "Debian Squeeze 6.0 (64-bit) (experimental)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        Xcp100GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 x64");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 x64");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "Other install media");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 x64");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        Xcp100GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        Xcp100GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        Xcp100GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        Xcp100GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        Xcp100GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        Xcp100GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        Xcp100GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        Xcp100GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        Xcp100GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        Xcp100GuestOsMap.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003 (32-bit)");
        Xcp100GuestOsMap.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003 (64-bit)");
        Xcp100GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        Xcp100GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        Xcp100GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        Xcp100GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        Xcp100GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        Xcp100GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit) (experimental)");
        Xcp100GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit) (experimental)");
        Xcp100GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        Xcp100GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        Xcp100GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        Xcp100GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    static {
        Xcp160GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 5.6 (32-bit)", "CentOS 5 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 5.6 (64-bit)", "CentOS 5 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 5.7 (32-bit)", "CentOS 5 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 5.7 (64-bit)", "CentOS 5 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 6.0 (32-bit)", "CentOS 6 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 6.0 (64-bit)", "CentOS 6 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 6.1 (32-bit)", "CentOS 6 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 6.1 (64-bit)", "CentOS 6 (64-bit)");
        Xcp160GuestOsMap.put("CentOS 6.2 (32-bit)", "CentOS 6 (32-bit)");
        Xcp160GuestOsMap.put("CentOS 6.2 (64-bit)", "CentOS 6 (64-bit)");
        Xcp160GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        Xcp160GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        Xcp160GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        Xcp160GuestOsMap.put("Debian GNU/Linux 7(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        Xcp160GuestOsMap.put("Debian GNU/Linux 7(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.6 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.6 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.7 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 5.7 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 6.0 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 6.0 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 6.1 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 6.1 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 6.2 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        Xcp160GuestOsMap.put("Oracle Enterprise Linux 6.2 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        Xcp160GuestOsMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 x64");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 x64");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "Other install media");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 x64");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        Xcp160GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        Xcp160GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        Xcp160GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        Xcp160GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        Xcp160GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        Xcp160GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        Xcp160GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        Xcp160GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        Xcp160GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        Xcp160GuestOsMap.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003 (32-bit)");
        Xcp160GuestOsMap.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003 (64-bit)");
        Xcp160GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        Xcp160GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        Xcp160GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        Xcp160GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        Xcp160GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        Xcp160GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit)");
        Xcp160GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit)");
        Xcp160GuestOsMap.put("Ubuntu 12.04 (32-bit)", "Ubuntu Precise Pangolin 12.04 (32-bit)");
        Xcp160GuestOsMap.put("Ubuntu 12.04 (64-bit)", "Ubuntu Precise Pangolin 12.04 (64-bit)");
        Xcp160GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        Xcp160GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        Xcp160GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        Xcp160GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    static {
        XenServerGuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        XenServerGuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        XenServerGuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        XenServerGuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        XenServerGuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5.0 (32-bit)");
        XenServerGuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5.0 (64-bit)");
        XenServerGuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5.1 (32-bit)");
        XenServerGuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5.1 (64-bit)");
        XenServerGuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5.2 (32-bit)");
        XenServerGuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5.2 (64-bit)");
        XenServerGuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5.3 (32-bit)");
        XenServerGuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5.3 (64-bit)");
        XenServerGuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5.4 (32-bit)");
        XenServerGuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5.4 (64-bit)");
        XenServerGuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        XenServerGuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Lenny 5.0 (32-bit)"); // This is to support Debian 6.0 in XS 5.6
        XenServerGuestOsMap.put("Debian GNU/Linux 7(32-bit)", "Debian Lenny 5.0 (32-bit)"); // This is to support Debian 7.0 in XS 5.6
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5.0 (32-bit)");
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5.0 (64-bit)");
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5.1 (32-bit)");
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5.1 (64-bit)");
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5.2 (32-bit)");
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5.2 (64-bit)");
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5.3 (32-bit)");
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5.3 (64-bit)");
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5.4 (32-bit)");
        XenServerGuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5.4 (64-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5.0 (32-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5.0 (64-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5.1 (32-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5.1 (64-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5.2 (32-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5.2 (64-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5.3 (32-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5.3 (64-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5.4 (32-bit)");
        XenServerGuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5.4 (64-bit)");
        XenServerGuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4 (32-bit)");
        XenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        XenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        XenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        XenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        XenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        XenServerGuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        XenServerGuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        XenServerGuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        XenServerGuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        XenServerGuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        XenServerGuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        XenServerGuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServerGuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServerGuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServerGuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServerGuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        XenServerGuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        XenServerGuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        XenServerGuestOsMap.put("Windows 2000 SP4 (32-bit)", "Windows 2000 SP4 (32-bit)");
        XenServerGuestOsMap.put("Windows 2000 Server SP4 (32-bit)", "Windows 2000 SP4 (32-bit)");
        XenServerGuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        XenServerGuestOsMap.put("Windows XP SP2 (32-bit)", "Windows XP SP2 (32-bit)");
        XenServerGuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        XenServerGuestOsMap.put("Other Linux (32-bit)", "Other install media");
        XenServerGuestOsMap.put("Other Linux (64-bit)", "Other install media");
        XenServerGuestOsMap.put("Other PV (32-bit)", "CentOS 5.4 (32-bit)");
        XenServerGuestOsMap.put("Other PV (64-bit)", "CentOS 5.4 (64-bit)");
    }

    static {
        XenServer56FP1GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        XenServer56FP1GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        XenServer56FP1GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit) (experimental)");
        XenServer56FP1GuestOsMap.put("Debian GNU/Linux 7(32-bit)", "Debian Squeeze 6.0 (32-bit)"); // This is to support Debian 7.0 in XS 5.6FP1
        XenServer56FP1GuestOsMap.put("Debian GNU/Linux 7(64-bit)", "Debian Squeeze 6.0 (64-bit) (experimental)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit) (experimental)");
        XenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit) (experimental)");
        XenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4 (32-bit)");
        XenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        XenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        XenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        XenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        XenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        XenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        XenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        XenServer56FP1GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        XenServer56FP1GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        XenServer56FP1GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        XenServer56FP1GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        XenServer56FP1GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer56FP1GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer56FP1GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer56FP1GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer56FP1GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        XenServer56FP1GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        XenServer56FP1GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        XenServer56FP1GuestOsMap.put("Windows 2000 SP4 (32-bit)", "Windows 2000 SP4 (32-bit)");
        XenServer56FP1GuestOsMap.put("Windows 2000 Server SP4 (32-bit)", "Windows 2000 SP4 (32-bit)");
        XenServer56FP1GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        XenServer56FP1GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        XenServer56FP1GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit) (experimental)");
        XenServer56FP1GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit) (experimental)");
        XenServer56FP1GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        XenServer56FP1GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        XenServer56FP1GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP1GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    static {
        XenServer56FP1GuestOsMemoryMap.put("CentOS 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("CentOS 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Debian GNU/Linux 6(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Debian GNU/Linux 6(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Debian GNU/Linux 7(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Debian GNU/Linux 7(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        // XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", new MemoryValues(512l, 16*1024l));// ??
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", new MemoryValues(512l, 128*1024l));  //?
        //_xenServer56FP1GuestOsMemoryMap.put("Windows 7 (32-bit)", new MemoryValues(512l, 16*1024l));   //?
        //_xenServer56FP1GuestOsMemoryMap.put("Windows 7 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        //_xenServer56FP1GuestOsMemoryMap.put("Windows 8 (32-bit)", new MemoryValues(512l, 16*1024l));      //?
        //_xenServer56FP1GuestOsMemoryMap.put("Windows 8 (64-bit)", new MemoryValues(512l, 16*1024l));         //?
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2003 (32-bit)", new MemoryValues(256l, 64*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2003 (64-bit)", new MemoryValues(256l, 128*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2003 PAE (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2008 (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Windows Server 2008 (64-bit)", new MemoryValues(512l, 16*1024l));
        XenServer56FP1GuestOsMemoryMap.put("Windows Server 2008 R2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Windows Server 2012 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Windows Server 8 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Windows Vista (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Windows XP SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Ubuntu 10.04 (32-bit)", new MemoryValues(128l, 512l));
        XenServer56FP1GuestOsMemoryMap.put("Ubuntu 10.04 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        // XenServer56FP1GuestOsMemoryMap.put("Ubuntu 10.10 (32-bit)", new MemoryValues(512l, 16*1024l));//?
        //_xenServer56FP1GuestOsMemoryMap.put("Ubuntu 10.10 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        XenServer56FP1GuestOsMemoryMap.put("Ubuntu 12.04 (32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56FP1GuestOsMemoryMap.put("Ubuntu 12.04 (64-bit)", new MemoryValues(128l, 128 * 1024l));
        // XenServer56FP1GuestOsMemoryMap.put("Other Linux (32-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer56FP1GuestOsMemoryMap.put("Other Linux (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other CentOS (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other CentOS (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other Ubuntu (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other Ubuntu (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other SUSE Linux(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other SUSE Linux(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other PV (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56FP1GuestOsMemoryMap.put("Other PV (64-bit)", new MemoryValues(512l, 16*1024l));
    }

    static {
        XenServer56FP2GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        XenServer56FP2GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        XenServer56FP2GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit) (experimental)");
        XenServer56FP2GuestOsMap.put("Debian GNU/Linux 7(32-bit)", "Debian Squeeze 6.0 (32-bit)"); // This is to support Debian 7.0 in XS 5.6FP2
        XenServer56FP2GuestOsMap.put("Debian GNU/Linux 7(64-bit)", "Debian Squeeze 6.0 (64-bit) (experimental)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        XenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        XenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4 (32-bit)");
        XenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        XenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        XenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        XenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        XenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        XenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        XenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        XenServer56FP2GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        XenServer56FP2GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        XenServer56FP2GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        XenServer56FP2GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        XenServer56FP2GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        XenServer56FP2GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        XenServer56FP2GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        XenServer56FP2GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        XenServer56FP2GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        XenServer56FP2GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit) (experimental)");
        XenServer56FP2GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit) (experimental)");
        XenServer56FP2GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        XenServer56FP2GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        XenServer56FP2GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        XenServer56FP2GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    static {
        XenServer56SP2GuestOsMemoryMap.put("CentOS 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("CentOS 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Debian GNU/Linux 6(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Debian GNU/Linux 6(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Debian GNU/Linux 7(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Debian GNU/Linux 7(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", new MemoryValues(512l, 16*1024l));// ??
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", new MemoryValues(512l, 128*1024l));  //?
        //_xenServer56SP2GuestOsMemoryMap.put("Windows 7 (32-bit)", new MemoryValues(512l, 16*1024l));   //?
        //_xenServer56SP2GuestOsMemoryMap.put("Windows 7 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        //_xenServer56SP2GuestOsMemoryMap.put("Windows 8 (32-bit)", new MemoryValues(512l, 16*1024l));      //?
        //_xenServer56SP2GuestOsMemoryMap.put("Windows 8 (64-bit)", new MemoryValues(512l, 16*1024l));         //?
        // XenServer56SP2GuestOsMemoryMap.put("Windows Server 2003 (32-bit)", new MemoryValues(256l, 64*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2003 (64-bit)", new MemoryValues(256l, 128*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2003 PAE (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2008 (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Windows Server 2008 (64-bit)", new MemoryValues(512l, 16*1024l));
        XenServer56SP2GuestOsMemoryMap.put("Windows Server 2008 R2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Windows Server 2012 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Windows Server 8 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Windows Vista (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Windows XP SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Ubuntu 10.04 (32-bit)", new MemoryValues(128l, 512l));
        XenServer56SP2GuestOsMemoryMap.put("Ubuntu 10.04 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        //_xenServer56SP2GuestOsMemoryMap.put("Ubuntu 10.10 (32-bit)", new MemoryValues(512l, 16*1024l));//?
        //_xenServer56SP2GuestOsMemoryMap.put("Ubuntu 10.10 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        XenServer56SP2GuestOsMemoryMap.put("Ubuntu 12.04 (32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer56SP2GuestOsMemoryMap.put("Ubuntu 12.04 (64-bit)", new MemoryValues(128l, 128 * 1024l));
        // XenServer56SP2GuestOsMemoryMap.put("Other Linux (32-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer56SP2GuestOsMemoryMap.put("Other Linux (64-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer56SP2GuestOsMemoryMap.put("Other (32-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer56SP2GuestOsMemoryMap.put("Other (64-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer56SP2GuestOsMemoryMap.put("Other CentOS (32-bit)", new MemoryValues(512l, 16*1024l));
        //   XenServer56SP2GuestOsMemoryMap.put("Other CentOS (64-bit)", new MemoryValues(512l, 16*1024l));
        //   XenServer56SP2GuestOsMemoryMap.put("Other Ubuntu (32-bit)", new MemoryValues(512l, 16*1024l));
        //   XenServer56SP2GuestOsMemoryMap.put("Other Ubuntu (64-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer56SP2GuestOsMemoryMap.put("Other SUSE Linux(32-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer56SP2GuestOsMemoryMap.put("Other SUSE Linux(64-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer56SP2GuestOsMemoryMap.put("Other PV (32-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer56SP2GuestOsMemoryMap.put("Other PV (64-bit)", new MemoryValues(512l, 16*1024l));
    }

    static {
        XenServer600GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        XenServer600GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        XenServer600GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        XenServer600GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        XenServer600GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        XenServer600GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        XenServer600GuestOsMap.put("CentOS 5.6 (32-bit)", "CentOS 5 (32-bit)");
        XenServer600GuestOsMap.put("CentOS 5.6 (64-bit)", "CentOS 5 (64-bit)");
        XenServer600GuestOsMap.put("CentOS 6.0 (32-bit)", "CentOS 6.0 (32-bit) (experimental)");
        XenServer600GuestOsMap.put("CentOS 6.0 (64-bit)", "CentOS 6.0 (64-bit) (experimental)");
        XenServer600GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        XenServer600GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        XenServer600GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        XenServer600GuestOsMap.put("Debian GNU/Linux 7(32-bit)", "Debian Squeeze 6.0 (32-bit)"); // This is to support Debian 7.0 in XS 6.0
        XenServer600GuestOsMap.put("Debian GNU/Linux 7(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.6 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 5.6 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 6.0 (32-bit)", "Oracle Enterprise Linux 6.0 (32-bit)");
        XenServer600GuestOsMap.put("Oracle Enterprise Linux 6.0 (64-bit)", "Oracle Enterprise Linux 6.0 (64-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6.0 (32-bit)");
        XenServer600GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6.0 (64-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4 (32-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", "SUSE Linux Enterprise Server 10 SP3 (32-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP4 (32-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", "SUSE Linux Enterprise Server 10 SP4 (64-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        XenServer600GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        XenServer600GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        XenServer600GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        XenServer600GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        XenServer600GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        XenServer600GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer600GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer600GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer600GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer600GuestOsMap.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer600GuestOsMap.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer600GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        XenServer600GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        XenServer600GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        XenServer600GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        XenServer600GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        XenServer600GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit)");
        XenServer600GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit)");
        XenServer600GuestOsMap.put("Ubuntu 10.10 (32-bit)", "Ubuntu Maverick Meerkat 10.10 (32-bit) (experimental)");
        XenServer600GuestOsMap.put("Ubuntu 10.10 (64-bit)", "Ubuntu Maverick Meerkat 10.10 (64-bit) (experimental)");
        XenServer600GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other (32-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other (64-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other CentOS (32-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other CentOS (64-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other Ubuntu (32-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other Ubuntu (64-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other SUSE Linux(32-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other SUSE Linux(64-bit)", "Other install media");
        XenServer600GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        XenServer600GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    static {
        XenServer600GuestOsMemoryMap.put("CentOS 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("CentOS 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Debian GNU/Linux 6(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("Debian GNU/Linux 6(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("Debian GNU/Linux 7(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("Debian GNU/Linux 7(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        //_xenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", new MemoryValues(512l, 16*1024l));// ??
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        //_xenServer600GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", new MemoryValues(512l, 128*1024l));  //?
        //_xenServer600GuestOsMemoryMap.put("Windows 7 (32-bit)", new MemoryValues(512l, 16*1024l));   //?
        //_xenServer600GuestOsMemoryMap.put("Windows 7 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        //_xenServer600GuestOsMemoryMap.put("Windows 8 (32-bit)", new MemoryValues(512l, 16*1024l));      //?
        //_xenServer600GuestOsMemoryMap.put("Windows 8 (64-bit)", new MemoryValues(512l, 16*1024l));         //?
        // XenServer600GuestOsMemoryMap.put("Windows Server 2003 (32-bit)", new MemoryValues(256l, 64*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2003 (64-bit)", new MemoryValues(256l, 128*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2003 PAE (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2008 (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Windows Server 2008 (64-bit)", new MemoryValues(512l, 16*1024l));
        XenServer600GuestOsMemoryMap.put("Windows Server 2008 R2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Windows Server 2012 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Windows Server 8 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Windows Vista (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Windows XP SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer600GuestOsMemoryMap.put("Ubuntu 10.04 (32-bit)", new MemoryValues(128l, 512l));
        XenServer600GuestOsMemoryMap.put("Ubuntu 10.04 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        //_xenServer600GuestOsMemoryMap.put("Ubuntu 10.10 (32-bit)", new MemoryValues(512l, 16*1024l));//?
        //_xenServer600GuestOsMemoryMap.put("Ubuntu 10.10 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        XenServer600GuestOsMemoryMap.put("Ubuntu 12.04 (32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer600GuestOsMemoryMap.put("Ubuntu 12.04 (64-bit)", new MemoryValues(128l, 128 * 1024l));
        //_xenServer600GuestOsMemoryMap.put("Other Linux (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other Linux (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other CentOS (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other CentOS (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other Ubuntu (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other Ubuntu (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other SUSE Linux(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other SUSE Linux(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other PV (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer600GuestOsMemoryMap.put("Other PV (64-bit)", new MemoryValues(512l, 16*1024l));
    }

    static {
        XenServer602GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        XenServer602GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        XenServer602GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        XenServer602GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        XenServer602GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        XenServer602GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        XenServer602GuestOsMap.put("CentOS 5.6 (32-bit)", "CentOS 5 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 5.6 (64-bit)", "CentOS 5 (64-bit)");
        XenServer602GuestOsMap.put("CentOS 5.7 (32-bit)", "CentOS 5 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 5.7 (64-bit)", "CentOS 5 (64-bit)");
        XenServer602GuestOsMap.put("CentOS 6.0 (32-bit)", "CentOS 6.0 (32-bit)");
        XenServer602GuestOsMap.put("CentOS 6.0 (64-bit)", "CentOS 6.0 (64-bit)");
        XenServer602GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        XenServer602GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        XenServer602GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        XenServer602GuestOsMap.put("Debian GNU/Linux 7(32-bit)", "Debian Squeeze 6.0 (32-bit)"); // This is to support Debian 7.0 in XS 6.0.2
        XenServer602GuestOsMap.put("Debian GNU/Linux 7(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.6 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.6 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.7 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 5.7 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 6.0 (32-bit)", "Oracle Enterprise Linux 6.0 (32-bit)");
        XenServer602GuestOsMap.put("Oracle Enterprise Linux 6.0 (64-bit)", "Oracle Enterprise Linux 6.0 (64-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6.0 (32-bit)");
        XenServer602GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6.0 (64-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", "SUSE Linux Enterprise Server 10 SP3 (32-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP4 (32-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", "SUSE Linux Enterprise Server 10 SP4 (64-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        XenServer602GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        XenServer602GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        XenServer602GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        XenServer602GuestOsMap.put("Windows 8 (32-bit)", "Windows 8 (32-bit) (experimental)");
        XenServer602GuestOsMap.put("Windows 8 (64-bit)", "Windows 8 (64-bit) (experimental)");
        XenServer602GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        XenServer602GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        XenServer602GuestOsMap.put("Windows Server 2003 PAE (32-bit)", "Windows Server 2003 PAE (32-bit)");
        XenServer602GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer602GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer602GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer602GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer602GuestOsMap.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer602GuestOsMap.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer602GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        XenServer602GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        XenServer602GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        XenServer602GuestOsMap.put("Windows Server 8 (64-bit)", "Windows Server 8 (64-bit) (experimental)");
        XenServer602GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        XenServer602GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        XenServer602GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit)");
        XenServer602GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit)");
        XenServer602GuestOsMap.put("Ubuntu 10.10 (32-bit)", "Ubuntu Maverick Meerkat 10.10 (32-bit) (experimental)");
        XenServer602GuestOsMap.put("Ubuntu 10.10 (64-bit)", "Ubuntu Maverick Meerkat 10.10 (64-bit) (experimental)");
        XenServer602GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other (32-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other (64-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other CentOS (32-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other CentOS (64-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other Ubuntu (32-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other Ubuntu (64-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other SUSE Linux(32-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other SUSE Linux(64-bit)", "Other install media");
        XenServer602GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        XenServer602GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    static {
        XenServer602GuestOsMemoryMap.put("CentOS 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("CentOS 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Debian GNU/Linux 5.0 (32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Debian GNU/Linux 6(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Debian GNU/Linux 6(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Debian GNU/Linux 7(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Debian GNU/Linux 7(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        // XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", new MemoryValues(512l, 16*1024l));// ??
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        //_xenServer602GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", new MemoryValues(512l, 128*1024l));  //?
        //_xenServer602GuestOsMemoryMap.put("Windows 7 (32-bit)", new MemoryValues(512l, 16*1024l));   //?
        //_xenServer602GuestOsMemoryMap.put("Windows 7 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        //_xenServer602GuestOsMemoryMap.put("Windows 8 (32-bit)", new MemoryValues(512l, 16*1024l));      //?
        //_xenServer602GuestOsMemoryMap.put("Windows 8 (64-bit)", new MemoryValues(512l, 16*1024l));         //?
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2003 (32-bit)", new MemoryValues(256l, 64*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2003 (64-bit)", new MemoryValues(256l, 128*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2003 PAE (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2008 (32-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer602GuestOsMemoryMap.put("Windows Server 2008 (64-bit)", new MemoryValues(512l, 16*1024l));
        XenServer602GuestOsMemoryMap.put("Windows Server 2008 R2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Windows Server 2012 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Windows Server 8 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Windows Vista (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Windows XP SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer602GuestOsMemoryMap.put("Ubuntu 10.04 (32-bit)", new MemoryValues(128l, 512l));
        XenServer602GuestOsMemoryMap.put("Ubuntu 10.04 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        //_xenServer602GuestOsMemoryMap.put("Ubuntu 10.10 (32-bit)", new MemoryValues(512l, 16*1024l));//?
        //_xenServer602GuestOsMemoryMap.put("Ubuntu 10.10 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        XenServer602GuestOsMemoryMap.put("Ubuntu 12.04 (32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer602GuestOsMemoryMap.put("Ubuntu 12.04 (64-bit)", new MemoryValues(128l, 128 * 1024l));
        // XenServer602GuestOsMemoryMap.put("Other Linux (32-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer602GuestOsMemoryMap.put("Other Linux (64-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer602GuestOsMemoryMap.put("Other (32-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer602GuestOsMemoryMap.put("Other (64-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer602GuestOsMemoryMap.put("Other CentOS (32-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer602GuestOsMemoryMap.put("Other CentOS (64-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer602GuestOsMemoryMap.put("Other Ubuntu (32-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer602GuestOsMemoryMap.put("Other Ubuntu (64-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer602GuestOsMemoryMap.put("Other SUSE Linux(32-bit)", new MemoryValues(512l, 16*1024l));
        //   XenServer602GuestOsMemoryMap.put("Other SUSE Linux(64-bit)", new MemoryValues(512l, 16*1024l));
        //   XenServer602GuestOsMemoryMap.put("Other PV (32-bit)", new MemoryValues(512l, 16*1024l));
        //   XenServer602GuestOsMemoryMap.put("Other PV (64-bit)", new MemoryValues(512l, 16*1024l));
    }

    static {
        XenServer610GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 5.6 (32-bit)", "CentOS 5 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 5.6 (64-bit)", "CentOS 5 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 5.7 (32-bit)", "CentOS 5 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 5.7 (64-bit)", "CentOS 5 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 6.0 (32-bit)", "CentOS 6 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 6.0 (64-bit)", "CentOS 6 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 6.1 (32-bit)", "CentOS 6 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 6.1 (64-bit)", "CentOS 6 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 6.2 (32-bit)", "CentOS 6 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 6.2 (64-bit)", "CentOS 6 (64-bit)");
        XenServer610GuestOsMap.put("CentOS 6.3 (32-bit)", "CentOS 6 (32-bit)");
        XenServer610GuestOsMap.put("CentOS 6.3 (64-bit)", "CentOS 6 (64-bit)");
        XenServer610GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        XenServer610GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        XenServer610GuestOsMap.put("Debian GNU/Linux 7(32-bit)", "Debian Squeeze 6.0 (32-bit)"); // This is to support Debian 7.0 in XS 6.1
        XenServer610GuestOsMap.put("Debian GNU/Linux 7(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.6 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.6 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.7 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 5.7 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 6.0 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 6.0 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 6.1 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 6.1 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 6.2 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        XenServer610GuestOsMap.put("Oracle Enterprise Linux 6.2 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        XenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", "SUSE Linux Enterprise Server 10 SP3 (32-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP4 (32-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", "SUSE Linux Enterprise Server 10 SP4 (64-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        XenServer610GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        XenServer610GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        XenServer610GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        XenServer610GuestOsMap.put("Windows 8 (32-bit)", "Windows 8 (32-bit) (experimental)");
        XenServer610GuestOsMap.put("Windows 8 (64-bit)", "Windows 8 (64-bit) (experimental)");
        XenServer610GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        XenServer610GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        XenServer610GuestOsMap.put("Windows Server 2003 PAE (32-bit)", "Windows Server 2003 PAE (32-bit)");
        XenServer610GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer610GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer610GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer610GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer610GuestOsMap.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer610GuestOsMap.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer610GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        XenServer610GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        XenServer610GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        XenServer610GuestOsMap.put("Windows Server 2012 (64-bit)", "Windows Server 2012 (64-bit) (experimental)");
        XenServer610GuestOsMap.put("Windows Server 8 (64-bit)", "Windows Server 2012 (64-bit) (experimental)");
        XenServer610GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        XenServer610GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        XenServer610GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit)");
        XenServer610GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit)");
        XenServer610GuestOsMap.put("Ubuntu 10.10 (32-bit)", "Ubuntu Maverick Meerkat 10.10 (32-bit) (experimental)");
        XenServer610GuestOsMap.put("Ubuntu 10.10 (64-bit)", "Ubuntu Maverick Meerkat 10.10 (64-bit) (experimental)");
        XenServer610GuestOsMap.put("Ubuntu 12.04 (32-bit)", "Ubuntu Precise Pangolin 12.04 (32-bit)");
        XenServer610GuestOsMap.put("Ubuntu 12.04 (64-bit)", "Ubuntu Precise Pangolin 12.04 (64-bit)");
        XenServer610GuestOsMap.put("Ubuntu 11.04 (32-bit)", "Other install media");
        XenServer610GuestOsMap.put("Ubuntu 11.04 (64-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other (32-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other (64-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other CentOS (32-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other CentOS (64-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other Ubuntu (32-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other Ubuntu (64-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other SUSE Linux(32-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other SUSE Linux(64-bit)", "Other install media");
        XenServer610GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        XenServer610GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    static {
        XenServer620GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 5.6 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.6 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 5.7 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.7 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 5.8 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.8 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 5.9 (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 5.9 (64-bit)", "CentOS 5 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 6.0 (32-bit)", "CentOS 6 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 6.0 (64-bit)", "CentOS 6 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 6.1 (32-bit)", "CentOS 6 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 6.1 (64-bit)", "CentOS 6 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 6.2 (32-bit)", "CentOS 6 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 6.2 (64-bit)", "CentOS 6 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 6.3 (32-bit)", "CentOS 6 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 6.3 (64-bit)", "CentOS 6 (64-bit)");
        XenServer620GuestOsMap.put("CentOS 6.4 (32-bit)", "CentOS 6 (32-bit)");
        XenServer620GuestOsMap.put("CentOS 6.4 (64-bit)", "CentOS 6 (64-bit)");
        XenServer620GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        XenServer620GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        XenServer620GuestOsMap.put("Debian GNU/Linux 7(32-bit)", "Debian Wheezy 7.0 (32-bit)");
        XenServer620GuestOsMap.put("Debian GNU/Linux 7(64-bit)", "Debian Wheezy 7.0 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.6 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.6 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.7 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.7 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.8 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.8 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.9 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 5.9 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.0 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.0 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.1 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.1 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.2 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.2 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.3 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.3 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.4 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Oracle Enterprise Linux 6.4 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.8 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.8 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.9 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 5.9 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.3 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.3 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.4 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        XenServer620GuestOsMap.put("Red Hat Enterprise Linux 6.4 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", "SUSE Linux Enterprise Server 10 SP3 (32-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP4 (32-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", "SUSE Linux Enterprise Server 10 SP4 (64-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 11 SP2 (32-bit)", "SUSE Linux Enterprise Server 11 SP2 (32-bit)");
        XenServer620GuestOsMap.put("SUSE Linux Enterprise Server 11 SP2 (64-bit)", "SUSE Linux Enterprise Server 11 SP2 (64-bit)");
        XenServer620GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        XenServer620GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        XenServer620GuestOsMap.put("Windows 7 SP1 (32-bit)", "Windows 7 SP1 (32-bit)");
        XenServer620GuestOsMap.put("Windows 7 SP1 (64-bit)", "Windows 7 SP1 (64-bit)");
        XenServer620GuestOsMap.put("Windows 8 (32-bit)", "Windows 8 (32-bit)");
        XenServer620GuestOsMap.put("Windows 8 (64-bit)", "Windows 8 (64-bit)");
        XenServer620GuestOsMap.put("Windows Server 2003 SP2 (32-bit)", "Windows Server 2003 SP2 (32-bit)");
        XenServer620GuestOsMap.put("Windows Server 2003 SP2 (64-bit)", "Windows Server 2003 SP2 (64-bit)");
        XenServer620GuestOsMap.put("Windows Server 2003 PAE (32-bit)", "Windows Server 2003 PAE (32-bit)");
        XenServer620GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer620GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer620GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer620GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer620GuestOsMap.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003 (32-bit)");
        XenServer620GuestOsMap.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003 (64-bit)");
        XenServer620GuestOsMap.put("Windows Server 2008 SP2 (32-bit)", "Windows Server 2008 SP2 (32-bit)");
        XenServer620GuestOsMap.put("Windows Server 2008 SP2 (64-bit)", "Windows Server 2008 SP2 (64-bit)");
        XenServer620GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        XenServer620GuestOsMap.put("Windows Server 2008 R2 SP1 (64-bit)", "Windows Server 2008 R2 SP1 (64-bit)");
        XenServer620GuestOsMap.put("Windows Server 2012 (64-bit)", "Windows Server 2012 (64-bit)");
        XenServer620GuestOsMap.put("Windows Vista SP2 (32-bit)", "Windows Vista (32-bit)");
        XenServer620GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        XenServer620GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit)");
        XenServer620GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit)");
        XenServer620GuestOsMap.put("Ubuntu 10.10 (32-bit)", "Ubuntu Maverick Meerkat 10.10 (32-bit) (experimental)");
        XenServer620GuestOsMap.put("Ubuntu 10.10 (64-bit)", "Ubuntu Maverick Meerkat 10.10 (64-bit) (experimental)");
        XenServer620GuestOsMap.put("Ubuntu 12.04 (32-bit)", "Ubuntu Precise Pangolin 12.04 (32-bit)");
        XenServer620GuestOsMap.put("Ubuntu 12.04 (64-bit)", "Ubuntu Precise Pangolin 12.04 (64-bit)");
        XenServer620GuestOsMap.put("Ubuntu 11.04 (32-bit)", "Other install media");
        XenServer620GuestOsMap.put("Ubuntu 11.04 (64-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other (32-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other (64-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other CentOS (32-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other CentOS (64-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other Ubuntu (32-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other Ubuntu (64-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other SUSE Linux(32-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other SUSE Linux(64-bit)", "Other install media");
        XenServer620GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        XenServer620GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    public static class MemoryValues {
        long max;
        long min;

        public MemoryValues(long min, long max) {
            this.min = min * 1024 * 1024;
            this.max = max * 1024 * 1024;
        }

        public long getMax() {
            return max;
        }

        public long getMin() {
            return min;
        }
    }

    static {
        XenServer610GuestOsMemoryMap.put("CentOS 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("CentOS 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Debian GNU/Linux 6(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("Debian GNU/Linux 6(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("Debian GNU/Linux 7(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("Debian GNU/Linux 7(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer610GuestOsMemoryMap.put("Windows 7 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        XenServer610GuestOsMemoryMap.put("Windows 7 (64-bit)", new MemoryValues(2 * 1024l, 128 * 1024l));
        //_xenServer610GuestOsMemoryMap.put("Windows 8 (32-bit)", new MemoryValues(512l, 16*1024l));      //?
        //_xenServer610GuestOsMemoryMap.put("Windows 8 (64-bit)", new MemoryValues(512l, 16*1024l));         //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2003 (32-bit)", new MemoryValues(256l, 64*1024l));  //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2003 (64-bit)", new MemoryValues(256l, 128*1024l));  //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2003 PAE (32-bit)", new MemoryValues(512l, 16*1024l)); //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(32-bit)", new MemoryValues(512l, 16*1024l)); //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(64-bit)", new MemoryValues(512l, 16*1024l));    //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(32-bit)", new MemoryValues(512l, 16*1024l));       //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2003 DataCenter Edition(64-bit)", new MemoryValues(512l, 16*1024l));          //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(32-bit)", new MemoryValues(512l, 16*1024l));               //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2003 Standard Edition(64-bit)", new MemoryValues(512l, 16*1024l));                  //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2008 (32-bit)", new MemoryValues(512l, 16*1024l));                       //?
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2008 (64-bit)", new MemoryValues(512l, 16*1024l));                          //?
        XenServer610GuestOsMemoryMap.put("Windows Server 2008 R2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        //_xenServer610GuestOsMemoryMap.put("Windows Server 2012 (64-bit)", new MemoryValues(512l, 16*1024l)); //?
        XenServer610GuestOsMemoryMap.put("Windows Server 8 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer610GuestOsMemoryMap.put("Windows Vista (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        XenServer610GuestOsMemoryMap.put("Windows XP SP3 (32-bit)", new MemoryValues(256l, 4 * 1024l));
        XenServer610GuestOsMemoryMap.put("Ubuntu 10.04 (32-bit)", new MemoryValues(128l, 512l));
        XenServer610GuestOsMemoryMap.put("Ubuntu 10.04 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        //_xenServer610GuestOsMemoryMap.put("Ubuntu 10.10 (32-bit)", new MemoryValues(512l, 16*1024l));//?
        //_xenServer610GuestOsMemoryMap.put("Ubuntu 10.10 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        XenServer610GuestOsMemoryMap.put("Ubuntu 12.04 (32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer610GuestOsMemoryMap.put("Ubuntu 12.04 (64-bit)", new MemoryValues(128l, 128 * 1024l));
        // XenServer610GuestOsMemoryMap.put("Other Linux (32-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer610GuestOsMemoryMap.put("Other Linux (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer610GuestOsMemoryMap.put("Other (32-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer610GuestOsMemoryMap.put("Other (64-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer610GuestOsMemoryMap.put("Other CentOS (32-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer610GuestOsMemoryMap.put("Other CentOS (64-bit)", new MemoryValues(512l, 16*1024l));
        //_xenServer610GuestOsMemoryMap.put("Other Ubuntu (32-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer610GuestOsMemoryMap.put("Other Ubuntu (64-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer610GuestOsMemoryMap.put("Other SUSE Linux(32-bit)", new MemoryValues(512l, 16*1024l));
        //  XenServer610GuestOsMemoryMap.put("Other SUSE Linux(64-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer610GuestOsMemoryMap.put("Other PV (32-bit)", new MemoryValues(512l, 16*1024l));
        // XenServer610GuestOsMemoryMap.put("Other PV (64-bit)", new MemoryValues(512l, 16*1024l));
    }

    static {
        XenServer620GuestOsMemoryMap.put("CentOS 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.8 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.8 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.9 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 5.9 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("CentOS 6.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Debian GNU/Linux 6(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer620GuestOsMemoryMap.put("Debian GNU/Linux 6(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer620GuestOsMemoryMap.put("Debian GNU/Linux 7(32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Debian GNU/Linux 7(64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.8 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.8 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.9 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 5.9 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Oracle Enterprise Linux 6.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.8 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.8 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.9 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 5.9 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("Red Hat Enterprise Linux 6.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServer620GuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));

        XenServer620GuestOsMemoryMap.put("Windows 7 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows 7 (64-bit)", new MemoryValues(2 * 1024l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows 7 SP1 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows 7 SP1 (64-bit)", new MemoryValues(2 * 1024l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows 8 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows 8 (64-bit)", new MemoryValues(2 * 1024l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows Server 2003 SP2 (32-bit)", new MemoryValues(256l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows Server 2003 SP2 (64-bit)", new MemoryValues(256l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows Server 2008 SP2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows Server 2008 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows Server 2008 R2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows Server 2008 R2 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows Server 2012 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows Vista SP2 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        XenServer620GuestOsMemoryMap.put("Windows XP SP3 (32-bit)", new MemoryValues(256l, 4 * 1024l));
        XenServer620GuestOsMemoryMap.put("Ubuntu 10.04 (32-bit)", new MemoryValues(128l, 512l));
        XenServer620GuestOsMemoryMap.put("Ubuntu 10.04 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        //_xenServer620GuestOsMemoryMap.put("Ubuntu 10.10 (32-bit)", new MemoryValues(512l, 16*1024l));//?
        //_xenServer620GuestOsMemoryMap.put("Ubuntu 10.10 (64-bit)", new MemoryValues(512l, 16*1024l));   //?
        XenServer620GuestOsMemoryMap.put("Ubuntu 12.04 (32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServer620GuestOsMemoryMap.put("Ubuntu 12.04 (64-bit)", new MemoryValues(128l, 128 * 1024l));
    }

    public static String getXcpGuestOsType(String stdType) {
        String guestOS = Xcp100GuestOsMap.get(stdType);
        if (guestOS == null) {
            s_logger.debug("Can't find the guest os: " + stdType + " mapping into XCP's guestOS type, start it as HVM guest");
            guestOS = "Other install media";
        }
        return guestOS;
    }

    public static String getXcp160GuestOsType(String stdType) {
        String guestOS = Xcp160GuestOsMap.get(stdType);
        if (guestOS == null) {
            s_logger.debug("Can't find the guest os: " + stdType + " mapping into XCP's guestOS type, start it as HVM guest");
            guestOS = "Other install media";
        }
        return guestOS;
    }

    public static String getXenServerGuestOsType(String stdType, boolean bootFromCD) {
        String guestOS = XenServerGuestOsMap.get(stdType);
        if (guestOS == null) {
            if (!bootFromCD) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 5.6 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 5.6 doesn't support Guest OS type " + stdType;
                s_logger.warn(msg);
            }
        }
        return guestOS;
    }

    public static String getXenServer56FP1GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS = XenServer56FP1GuestOsMap.get(stdType);
        if (guestOS == null) {
            if (!bootFromCD) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 5.6 FP1 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 5.6 FP1 DOES NOT support Guest OS type " + stdType;
                s_logger.warn(msg);
            }
        }
        return guestOS;
    }

    public static long getXenServer56FP1StaticMax(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer56FP1GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMax();
    }

    public static long getXenServer56FP1StaticMin(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer56FP1GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMin();
    }

    public static String getXenServer56SP2GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS = XenServer56FP2GuestOsMap.get(stdType);
        if (guestOS == null) {
            if (!bootFromCD) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 5.6 SP2 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 5.6 SP2 DOES NOT support Guest OS type " + stdType;
                s_logger.warn(msg);
            }
        }
        return guestOS;
    }

    public static long getXenServer56SP2StaticMax(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer56SP2GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMax();
    }

    public static long getXenServer56SP2StaticMin(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer56SP2GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMin();
    }

    public static String getXenServer600GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS = XenServer600GuestOsMap.get(stdType);
        if (guestOS == null) {
            if (!bootFromCD) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 6.0.2 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 6.0.2 DOES NOT support Guest OS type " + stdType;
                s_logger.warn(msg);
            }

        }
        return guestOS;
    }

    public static long getXenServer600StaticMax(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer600GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMax();
    }

    public static long getXenServer600StaticMin(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer600GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMin();
    }

    public static String getXenServer602GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS = XenServer602GuestOsMap.get(stdType);
        if (guestOS == null) {
            if (!bootFromCD) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 6.0.2 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 6.0.2 DOES NOT support Guest OS type " + stdType;
                s_logger.warn(msg);
            }

        }
        return guestOS;
    }

    public static long getXenServer602StaticMax(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer602GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMax();
    }

    public static long getXenServer602StaticMin(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer602GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMin();
    }

    public static String getXenServer610GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS = XenServer610GuestOsMap.get(stdType);
        if (guestOS == null) {
            if (!bootFromCD) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 6.1.0 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 6.1.0 DOES NOT support Guest OS type " + stdType;
                s_logger.warn(msg);
            }
        }
        return guestOS;
    }

    public static long getXenServer610StaticMax(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer610GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMax();
    }

    public static long getXenServer610StaticMin(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer610GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMin();
    }

    public static String getXenServer620GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS = XenServer620GuestOsMap.get(stdType);
        if (guestOS == null) {
            if (!bootFromCD) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 6.2.0 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 6.2.0 DOES NOT support Guest OS type " + stdType;
                s_logger.warn(msg);
            }
        }
        return guestOS;
    }

    public static long getXenServer620StaticMax(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer620GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMax();
    }

    public static long getXenServer620StaticMin(String stdType, boolean bootFromCD) {
        MemoryValues recommendedMaxMinMemory = XenServer620GuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMin();
    }
}