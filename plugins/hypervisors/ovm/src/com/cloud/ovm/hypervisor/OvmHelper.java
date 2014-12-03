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
package com.cloud.ovm.hypervisor;

import java.util.HashMap;

public class OvmHelper {
    private static final HashMap<String, String> s_ovmMap = new HashMap<String, String>();

    public static final String ORACLE_LINUX = "Oracle Linux";
    public static final String ORACLE_SOLARIS = "Oracle Solaris";
    public static final String WINDOWS = "Windows";

    static {
        s_ovmMap.put("Oracle Enterprise Linux 6.0 (32-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 6.0 (64-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.0 (32-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.0 (64-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.1 (32-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.1 (64-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.2 (32-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.2 (64-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.3 (32-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.3 (64-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.4 (32-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.4 (64-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.5 (32-bit)", ORACLE_LINUX);
        s_ovmMap.put("Oracle Enterprise Linux 5.5 (64-bit)", ORACLE_LINUX);
        s_ovmMap.put("Windows 7 (32-bit)", WINDOWS);
        s_ovmMap.put("Windows 7 (64-bit)", WINDOWS);
        s_ovmMap.put("Windows Server 2003 (32-bit)", WINDOWS);
        s_ovmMap.put("Windows Server 2003 (64-bit)", WINDOWS);
        s_ovmMap.put("Windows Server 2008 (32-bit)", WINDOWS);
        s_ovmMap.put("Windows Server 2008 (64-bit)", WINDOWS);
        s_ovmMap.put("Windows Server 2008 R2 (64-bit)", WINDOWS);
        s_ovmMap.put("Windows 2000 SP4 (32-bit)", WINDOWS);
        s_ovmMap.put("Windows Vista (32-bit)", WINDOWS);
        s_ovmMap.put("Windows XP SP2 (32-bit)", WINDOWS);
        s_ovmMap.put("Windows XP SP3 (32-bit)", WINDOWS);
        s_ovmMap.put("Sun Solaris 10(32-bit)", ORACLE_SOLARIS);
        s_ovmMap.put("Sun Solaris 10(64-bit)", ORACLE_SOLARIS);
        s_ovmMap.put("Sun Solaris 9(Experimental)", ORACLE_SOLARIS);
        s_ovmMap.put("Sun Solaris 8(Experimental)", ORACLE_SOLARIS);
        s_ovmMap.put("Sun Solaris 11 (32-bit)", ORACLE_SOLARIS);
        s_ovmMap.put("Sun Solaris 11 (64-bit)", ORACLE_SOLARIS);
    }

    public static String getOvmGuestType(String stdType) {
        return s_ovmMap.get(stdType);
    }
}
