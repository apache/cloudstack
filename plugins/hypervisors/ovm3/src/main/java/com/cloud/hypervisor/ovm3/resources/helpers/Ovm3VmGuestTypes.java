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

package com.cloud.hypervisor.ovm3.resources.helpers;

import java.util.HashMap;
import java.util.Map;

public class Ovm3VmGuestTypes {
    /* /usr/lib64/python2.4/site-packages/agent/lib/assembly */
    private static final Map<String, String> OVMHELPERMAP = new HashMap<String, String>();
    private static final String HVM = "hvm";
    private static final String PV = "xen_pvm";
    private static final String DOMSOL = "ldoms_pvm";
    {
        OVMHELPERMAP.put("Oracle Enterprise Linux 6.0 (32-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 6.0 (64-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.0 (32-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.0 (64-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.1 (32-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.1 (64-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.2 (32-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.2 (64-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.3 (32-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.3 (64-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.4 (32-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.4 (64-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.5 (32-bit)", PV);
        OVMHELPERMAP.put("Oracle Enterprise Linux 5.5 (64-bit)", PV);
        OVMHELPERMAP.put("Other Linux (32-bit)", PV);
        OVMHELPERMAP.put("Other Linux (64-bit)", PV);
        OVMHELPERMAP.put("Other PV (32-bit)", PV);
        OVMHELPERMAP.put("Other PV (64-bit)", PV);
        OVMHELPERMAP.put("Debian GNU/Linux 7(32-bit)", PV);
        OVMHELPERMAP.put("Debian GNU/Linux 7(64-bit)", PV);
        OVMHELPERMAP.put("Linux HVM (32-bit)", HVM);
        OVMHELPERMAP.put("Linux HVM (64-bit)", HVM);
        OVMHELPERMAP.put("Dos", HVM);
        OVMHELPERMAP.put("Windows 7 (32-bit)", HVM);
        OVMHELPERMAP.put("Windows 7 (64-bit)", HVM);
        OVMHELPERMAP.put("Windows 8 (64-bit)", HVM);
        OVMHELPERMAP.put("Windows Server 2003 (32-bit)", HVM);
        OVMHELPERMAP.put("Windows Server 2003 (64-bit)", HVM);
        OVMHELPERMAP.put("Windows Server 2008 (32-bit)", HVM);
        OVMHELPERMAP.put("Windows Server 2008 (64-bit)", HVM);
        OVMHELPERMAP.put("Windows Server 2008 R2 (64-bit)", HVM);
        OVMHELPERMAP.put("Windows Server 2012 (64-bit)", HVM);
        OVMHELPERMAP.put("Windows 2000 SP4 (32-bit)", HVM);
        OVMHELPERMAP.put("Windows Vista (32-bit)", HVM);
        OVMHELPERMAP.put("Windows XP SP2 (32-bit)", HVM);
        OVMHELPERMAP.put("Windows XP SP3 (32-bit)", HVM);
        OVMHELPERMAP.put("Sun Solaris 10(32-bit)", HVM);
        OVMHELPERMAP.put("Sun Solaris 10(64-bit)", HVM);
        OVMHELPERMAP.put("Sun Solaris 9(Experimental)", HVM);
        OVMHELPERMAP.put("Sun Solaris 8(Experimental)", HVM);
        OVMHELPERMAP.put("Sun Solaris 11 (32-bit)", HVM);
        OVMHELPERMAP.put("Sun Solaris 11 (64-bit)", HVM);
        OVMHELPERMAP.put("Sun Solaris PV (32-bit)", PV);
        OVMHELPERMAP.put("Sun Solaris PV (64-bit)", PV);
        OVMHELPERMAP.put("Sun Solaris Sparc (32-bit)", DOMSOL);
        OVMHELPERMAP.put("Sun Solaris Sparc (64-bit)", DOMSOL);
    }

    public String getOvm3GuestType(String stdType) {
        return OVMHELPERMAP.get(stdType);
    }
}
