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
    private static final HashMap<String, String> _xcp100GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> _xenServerGuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> _xenServer56FP1GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> _xenServer56FP2GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> _xenServer600GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> _xenServer602GuestOsMap = new HashMap<String, String>(70);
    private static final HashMap<String, String> _xenServer610GuestOsMap = new HashMap<String, String>(70);
    private static final ArrayList<String> _guestOsList = new ArrayList<String>(70);


    static {
        _xcp100GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        _xcp100GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        _xcp100GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        _xcp100GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        _xcp100GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        _xcp100GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        _xcp100GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        _xcp100GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        _xcp100GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        _xcp100GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit) (experimental)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        _xcp100GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 x64");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 x64");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "Other install media");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 x64");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        _xcp100GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        _xcp100GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        _xcp100GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        _xcp100GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        _xcp100GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        _xcp100GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        _xcp100GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        _xcp100GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        _xcp100GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        _xcp100GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        _xcp100GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit) (experimental)");
        _xcp100GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit) (experimental)");
        _xcp100GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        _xcp100GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        _xcp100GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        _xcp100GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }
    

    static {
    	_xenServerGuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
    	_xenServerGuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
    	_xenServerGuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
    	_xenServerGuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5.0 (32-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5.0 (64-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5.1 (32-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5.1 (64-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5.2 (32-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5.2 (64-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5.3 (32-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5.3 (64-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5.4 (32-bit)");
    	_xenServerGuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5.4 (64-bit)");
    	_xenServerGuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5.0 (32-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5.0 (64-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5.1 (32-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5.1 (64-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5.2 (32-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5.2 (64-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5.3 (32-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5.3 (64-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5.4 (32-bit)");
    	_xenServerGuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5.4 (64-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5.0 (32-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5.0 (64-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5.1 (32-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5.1 (64-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5.2 (32-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5.2 (64-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5.3 (32-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5.3 (64-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5.4 (32-bit)");
    	_xenServerGuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5.4 (64-bit)");
    	_xenServerGuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4 (32-bit)");
    	_xenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
    	_xenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
    	_xenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
    	_xenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
    	_xenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
    	_xenServerGuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
    	_xenServerGuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
    	_xenServerGuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
    	_xenServerGuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
    	_xenServerGuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
    	_xenServerGuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
    	_xenServerGuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
    	_xenServerGuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
    	_xenServerGuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
    	_xenServerGuestOsMap.put("Windows 2000 SP4 (32-bit)", "Windows 2000 SP4 (32-bit)");
    	_xenServerGuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
    	_xenServerGuestOsMap.put("Windows XP SP2 (32-bit)", "Windows XP SP2 (32-bit)");
    	_xenServerGuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
    	_xenServerGuestOsMap.put("Other Linux (32-bit)", "Other install media");
    	_xenServerGuestOsMap.put("Other Linux (64-bit)", "Other install media");
    	_xenServerGuestOsMap.put("Other PV (32-bit)", "CentOS 5.4 (32-bit)");
    	_xenServerGuestOsMap.put("Other PV (64-bit)", "CentOS 5.4 (64-bit)");
    }
    
    static {
        _xenServer56FP1GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit) (experimental)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit) (experimental)");
        _xenServer56FP1GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit) (experimental)");
        _xenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4 (32-bit)");
        _xenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        _xenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        _xenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        _xenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        _xenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        _xenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        _xenServer56FP1GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        _xenServer56FP1GuestOsMap.put("Windows 2000 SP4 (32-bit)", "Windows 2000 SP4 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        _xenServer56FP1GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit) (experimental)");
        _xenServer56FP1GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit) (experimental)");
        _xenServer56FP1GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        _xenServer56FP1GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        _xenServer56FP1GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP1GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }
    
    static {
        _xenServer56FP2GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit) (experimental)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        _xenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4 (32-bit)");
        _xenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        _xenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        _xenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        _xenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        _xenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        _xenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        _xenServer56FP2GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        _xenServer56FP2GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        _xenServer56FP2GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit) (experimental)");
        _xenServer56FP2GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit) (experimental)");
        _xenServer56FP2GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        _xenServer56FP2GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        _xenServer56FP2GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        _xenServer56FP2GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    
    static {
        _xenServer600GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.6 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer600GuestOsMap.put("CentOS 5.6 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer600GuestOsMap.put("CentOS 6.0 (32-bit)", "CentOS 6.0 (32-bit) (experimental)");
        _xenServer600GuestOsMap.put("CentOS 6.0 (64-bit)", "CentOS 6.0 (64-bit) (experimental)");
        _xenServer600GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        _xenServer600GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        _xenServer600GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.6 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 5.6 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 6.0 (32-bit)", "Oracle Enterprise Linux 6.0 (32-bit)");
        _xenServer600GuestOsMap.put("Oracle Enterprise Linux 6.0 (64-bit)", "Oracle Enterprise Linux 6.0 (64-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6.0 (32-bit)");
        _xenServer600GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6.0 (64-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4 (32-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", "SUSE Linux Enterprise Server 10 SP3 (32-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP4 (32-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", "SUSE Linux Enterprise Server 10 SP4 (64-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        _xenServer600GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        _xenServer600GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        _xenServer600GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        _xenServer600GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        _xenServer600GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        _xenServer600GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        _xenServer600GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit)");
        _xenServer600GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit)");
        _xenServer600GuestOsMap.put("Ubuntu 10.10 (32-bit)", "Ubuntu Maverick Meerkat 10.10 (32-bit) (experimental)");
        _xenServer600GuestOsMap.put("Ubuntu 10.10 (64-bit)", "Ubuntu Maverick Meerkat 10.10 (64-bit) (experimental)");
        _xenServer600GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other (32-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other (64-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other CentOS (32-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other CentOS (64-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other Ubuntu (32-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other Ubuntu (64-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other SUSE Linux(32-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other SUSE Linux(64-bit)", "Other install media");
        _xenServer600GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        _xenServer600GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }
    
    
    static {
        _xenServer602GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.6 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.6 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.7 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 5.7 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer602GuestOsMap.put("CentOS 6.0 (32-bit)", "CentOS 6.0 (32-bit)");
        _xenServer602GuestOsMap.put("CentOS 6.0 (64-bit)", "CentOS 6.0 (64-bit)");
        _xenServer602GuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
        _xenServer602GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        _xenServer602GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.6 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.6 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.7 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 5.7 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 6.0 (32-bit)", "Oracle Enterprise Linux 6.0 (32-bit)");
        _xenServer602GuestOsMap.put("Oracle Enterprise Linux 6.0 (64-bit)", "Oracle Enterprise Linux 6.0 (64-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6.0 (32-bit)");
        _xenServer602GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6.0 (64-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", "SUSE Linux Enterprise Server 10 SP3 (32-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP4 (32-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", "SUSE Linux Enterprise Server 10 SP4 (64-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        _xenServer602GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        _xenServer602GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        _xenServer602GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        _xenServer602GuestOsMap.put("Windows 8 (32-bit)", "Windows 8 (32-bit) (experimental)");
        _xenServer602GuestOsMap.put("Windows 8 (64-bit)", "Windows 7 (64-bit) (experimental)");
        _xenServer602GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2003 PAE (32-bit)", "Windows Server 2003 PAE (32-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        _xenServer602GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        _xenServer602GuestOsMap.put("Windows Server 8 (64-bit)", "Windows Server 8 (64-bit) (experimental)");
        _xenServer602GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        _xenServer602GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        _xenServer602GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit)");
        _xenServer602GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit)");
        _xenServer602GuestOsMap.put("Ubuntu 10.10 (32-bit)", "Ubuntu Maverick Meerkat 10.10 (32-bit) (experimental)");
        _xenServer602GuestOsMap.put("Ubuntu 10.10 (64-bit)", "Ubuntu Maverick Meerkat 10.10 (64-bit) (experimental)");
        _xenServer602GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other (32-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other (64-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other CentOS (32-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other CentOS (64-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other Ubuntu (32-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other Ubuntu (64-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other SUSE Linux(32-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other SUSE Linux(64-bit)", "Other install media");
        _xenServer602GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        _xenServer602GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    static {
        _xenServer610GuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.5 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.5 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.6 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.6 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.7 (32-bit)", "CentOS 5 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 5.7 (64-bit)", "CentOS 5 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 6.0 (32-bit)", "CentOS 6 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 6.0 (64-bit)", "CentOS 6 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 6.1 (32-bit)", "CentOS 6 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 6.1 (64-bit)", "CentOS 6 (64-bit)");
        _xenServer610GuestOsMap.put("CentOS 6.2 (32-bit)", "CentOS 6 (32-bit)");
        _xenServer610GuestOsMap.put("CentOS 6.2 (64-bit)", "CentOS 6 (64-bit)");
        _xenServer610GuestOsMap.put("Debian GNU/Linux 6(32-bit)", "Debian Squeeze 6.0 (32-bit)");
        _xenServer610GuestOsMap.put("Debian GNU/Linux 6(64-bit)", "Debian Squeeze 6.0 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.5 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.5 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.6 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.6 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.7 (32-bit)", "Oracle Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 5.7 (64-bit)", "Oracle Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 6.0 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 6.0 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 6.1 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 6.1 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 6.2 (32-bit)", "Oracle Enterprise Linux 6 (32-bit)");
        _xenServer610GuestOsMap.put("Oracle Enterprise Linux 6.2 (64-bit)", "Oracle Enterprise Linux 6 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", "Red Hat Enterprise Linux 5 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", "Red Hat Enterprise Linux 5 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", "Red Hat Enterprise Linux 6 (32-bit)");
        _xenServer610GuestOsMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", "Red Hat Enterprise Linux 6 (64-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1 (32-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 (64-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2 (32-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 (64-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", "SUSE Linux Enterprise Server 10 SP3 (32-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "SUSE Linux Enterprise Server 10 SP3 (64-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", "SUSE Linux Enterprise Server 10 SP4 (32-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", "SUSE Linux Enterprise Server 10 SP4 (64-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11 (32-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 (64-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", "SUSE Linux Enterprise Server 11 SP1 (32-bit)");
        _xenServer610GuestOsMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", "SUSE Linux Enterprise Server 11 SP1 (64-bit)");
        _xenServer610GuestOsMap.put("Windows 7 (32-bit)", "Windows 7 (32-bit)");
        _xenServer610GuestOsMap.put("Windows 7 (64-bit)", "Windows 7 (64-bit)");
        _xenServer610GuestOsMap.put("Windows 8 (32-bit)", "Windows 8 (32-bit) (experimental)");
        _xenServer610GuestOsMap.put("Windows 8 (64-bit)", "Windows 7 (64-bit) (experimental)");
        _xenServer610GuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2003 PAE (32-bit)", "Windows Server 2003 PAE (32-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003 (32-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003 (64-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008 (32-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 (64-bit)");
        _xenServer610GuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 (64-bit)");
        _xenServer610GuestOsMap.put("Windows Server 8 (64-bit)", "Windows Server 2012 (64-bit) (experimental)");
        _xenServer610GuestOsMap.put("Windows Vista (32-bit)", "Windows Vista (32-bit)");
        _xenServer610GuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3 (32-bit)");
        _xenServer610GuestOsMap.put("Ubuntu 10.04 (32-bit)", "Ubuntu Lucid Lynx 10.04 (32-bit)");
        _xenServer610GuestOsMap.put("Ubuntu 10.04 (64-bit)", "Ubuntu Lucid Lynx 10.04 (64-bit)");
        _xenServer610GuestOsMap.put("Ubuntu 10.10 (32-bit)", "Ubuntu Maverick Meerkat 10.10 (32-bit) (experimental)");
        _xenServer610GuestOsMap.put("Ubuntu 10.10 (64-bit)", "Ubuntu Maverick Meerkat 10.10 (64-bit) (experimental)");
        _xenServer610GuestOsMap.put("Ubuntu 12.04 (32-bit)", "Ubuntu Precise Pangolin 12.04 (32-bit)");
        _xenServer610GuestOsMap.put("Ubuntu 12.04 (64-bit)", "Ubuntu Precise Pangolin 12.04 (64-bit)");
        _xenServer610GuestOsMap.put("Other Linux (32-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other Linux (64-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other (32-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other (64-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other CentOS (32-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other CentOS (64-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other Ubuntu (32-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other Ubuntu (64-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other SUSE Linux(32-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other SUSE Linux(64-bit)", "Other install media");
        _xenServer610GuestOsMap.put("Other PV (32-bit)", "CentOS 5 (32-bit)");
        _xenServer610GuestOsMap.put("Other PV (64-bit)", "CentOS 5 (64-bit)");
    }

    public static String getXcpGuestOsType(String stdType) {
        String guestOS =  _xcp100GuestOsMap.get(stdType);
        if (guestOS == null) {
        	s_logger.debug("Can't find the guest os: " + stdType + " mapping into XCP's guestOS type, start it as HVM guest");
        	guestOS = "Other install media";
        }
        return guestOS;
    }
    
    
    public static String getXenServerGuestOsType(String stdType, boolean bootFromCD) {
        String guestOS =  _xenServerGuestOsMap.get(stdType);
        if (guestOS == null) {
            if ( !bootFromCD ) {
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
        String guestOS =  _xenServer56FP1GuestOsMap.get(stdType);
        if (guestOS == null) {
            if ( !bootFromCD ) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 5.6 FP1 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 5.6 FP1 DOES NOT support Guest OS type " + stdType;
                s_logger.warn(msg);
            }
        }
        return guestOS;
    }
    
    public static String getXenServer56SP2GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS =  _xenServer56FP2GuestOsMap.get(stdType);
        if (guestOS == null) {
            if ( !bootFromCD ) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 5.6 SP2 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 5.6 SP2 DOES NOT support Guest OS type " + stdType;
                s_logger.warn(msg);
            }
        }
        return guestOS;
    }
    
    
    public static String getXenServer600GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS =  _xenServer600GuestOsMap.get(stdType);
        if (guestOS == null) {
            if ( !bootFromCD ) {
                s_logger.debug("Can't find the guest os: " + stdType + " mapping into XenServer 6.0 guestOS type, start it as HVM guest");
                guestOS = "Other install media";
            } else {
                String msg = "XenServer 6.0 DOES NOT support Guest OS type " + stdType;
                s_logger.warn(msg);
            }

        }
        return guestOS;
    }

    public static String getXenServer602GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS = _xenServer602GuestOsMap.get(stdType);
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

    public static String getXenServer610GuestOsType(String stdType, boolean bootFromCD) {
        String guestOS = _xenServer610GuestOsMap.get(stdType);
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
}
