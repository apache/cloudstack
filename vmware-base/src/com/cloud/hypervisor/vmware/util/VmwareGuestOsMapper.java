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
package com.cloud.hypervisor.vmware.util;

import java.util.HashMap;
import java.util.Map;

import com.vmware.vim25.VirtualMachineGuestOsIdentifier;

public class VmwareGuestOsMapper {
    private static Map<String, VirtualMachineGuestOsIdentifier> s_mapper = new HashMap<String, VirtualMachineGuestOsIdentifier>();
    static {
        s_mapper.put("Windows Vista (32-bit)", VirtualMachineGuestOsIdentifier.WIN_VISTA_GUEST);
        s_mapper.put("Windows Vista (64-bit)", VirtualMachineGuestOsIdentifier.WIN_VISTA_64_GUEST);
        s_mapper.put("Windows Server 2008 (32-bit)", VirtualMachineGuestOsIdentifier.WIN_LONGHORN_GUEST);
        s_mapper.put("Windows Server 2008 (64-bit)", VirtualMachineGuestOsIdentifier.WIN_LONGHORN_64_GUEST);

        s_mapper.put("Windows 7 (32-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_7_GUEST);
        s_mapper.put("Windows 7 (64-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_7_64_GUEST);
        s_mapper.put("Windows Server 2008 R2 (64-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_7_SERVER_64_GUEST);

        s_mapper.put("Windows 8 (32-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_8_GUEST);
        s_mapper.put("Windows 8 (64-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_8_64_GUEST);
        s_mapper.put("Windows Server 2012 (64-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_8_SERVER_64_GUEST);

        s_mapper.put("Windows 8.1 (32-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_8_GUEST);
        s_mapper.put("Windows 8.1 (64-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_8_64_GUEST);
        s_mapper.put("Windows Server 2012 R2 (64-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_8_SERVER_64_GUEST);

        s_mapper.put("Windows 10 (32-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_9_GUEST);
        s_mapper.put("Windows 10 (64-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_9_64_GUEST);
        s_mapper.put("Windows Server 2016 (64-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_9_SERVER_64_GUEST);

        s_mapper.put("Debian GNU/Linux 6(64-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_6_64_GUEST);
        s_mapper.put("Debian GNU/Linux 6(32-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_6_GUEST);
        s_mapper.put("Debian GNU/Linux 7(32-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_7_GUEST);
        s_mapper.put("Debian GNU/Linux 7(64-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_7_64_GUEST);
        s_mapper.put("Debian GNU/Linux 8(32-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_8_GUEST);
        s_mapper.put("Debian GNU/Linux 8(64-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_8_64_GUEST);

        s_mapper.put("FreeBSD (32-bit)", VirtualMachineGuestOsIdentifier.FREEBSD_GUEST);
        s_mapper.put("FreeBSD (64-bit)", VirtualMachineGuestOsIdentifier.FREEBSD_64_GUEST);

        s_mapper.put("SUSE Linux Enterprise 8(32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise 8(64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise 9(32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise 9(64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise 10(32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise 10(64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 (32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 SP2 (32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 SP2 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 SP3 (32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 SP3 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 SP4 (32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 11 SP4 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 12 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("SUSE Linux Enterprise Server 12 SP1 (64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
        s_mapper.put("Other SUSE Linux(32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
        s_mapper.put("Other SUSE Linux(64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);

        s_mapper.put("CentOS 6.0 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
        s_mapper.put("CentOS 6.0 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 6.1 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
        s_mapper.put("CentOS 6.1 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 6.2 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
        s_mapper.put("CentOS 6.2 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 6.3 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
        s_mapper.put("CentOS 6.3 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 6.4 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
        s_mapper.put("CentOS 6.4 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 6.5 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
        s_mapper.put("CentOS 6.5 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 6.6 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
        s_mapper.put("CentOS 6.6 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 6.7 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
        s_mapper.put("CentOS 6.7 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 7.0-1406 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 7.1-1503 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("CentOS 7.2-1511 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
        s_mapper.put("Other CentOS (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
        s_mapper.put("Other CentOS (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);

        s_mapper.put("Red Hat Enterprise Linux 6.0 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.0 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.1 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.1 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.2 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.2 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.3 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.3 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.4 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.4 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.5 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.5 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.6 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.6 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.7 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 6.7 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 7.0 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_7_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 7.1 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_7_64_GUEST);
        s_mapper.put("Red Hat Enterprise Linux 7.2 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_7_64_GUEST);

        s_mapper.put("Fedora 14", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);
        s_mapper.put("Fedora 15", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);
        s_mapper.put("Fedora 16", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);
        s_mapper.put("Fedora 17", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);
        s_mapper.put("Fedora 18", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);
        s_mapper.put("Fedora 19", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);
        s_mapper.put("Fedora 20", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);
        s_mapper.put("Fedora 21", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);
        s_mapper.put("Fedora 22", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);
        s_mapper.put("Fedora 23", VirtualMachineGuestOsIdentifier.FEDORA_GUEST);

        s_mapper.put("Ubuntu 8.04 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 8.04 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 8.10 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 8.10 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 9.04 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 9.04 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 9.10 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 9.10 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 10.04 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 10.04 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 10.10 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 10.10 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 11.04 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 11.04 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 11.10 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 11.10 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 12.04 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 12.04 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 12.10 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 12.10 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 13.04 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 13.04 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 13.10 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 13.10 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 14.04 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 14.04 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 14.10 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 14.10 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 15.04 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 15.04 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Ubuntu 15.10 (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Ubuntu 15.10 (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);
        s_mapper.put("Other Ubuntu (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
        s_mapper.put("Other Ubuntu (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);

        s_mapper.put("Oracle Enterprise Linux 6.0 (32-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.0 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.1 (32-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.1 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.2 (32-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.2 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.3 (32-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.3 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.4 (32-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.4 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.5 (32-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.5 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.6 (32-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.6 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.7 (32-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_GUEST);
        s_mapper.put("Oracle Enterprise Linux 6.7 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 7.0 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 7.1 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);
        s_mapper.put("Oracle Enterprise Linux 7.2 (64-bit)", VirtualMachineGuestOsIdentifier.ORACLE_LINUX_64_GUEST);

        s_mapper.put("Other 2.6x Linux (32-bit)", VirtualMachineGuestOsIdentifier.OTHER_26_X_LINUX_GUEST);
        s_mapper.put("Other 2.6x Linux (64-bit)", VirtualMachineGuestOsIdentifier.OTHER_26_X_LINUX_64_GUEST);
        s_mapper.put("Other Linux (32-bit)", VirtualMachineGuestOsIdentifier.OTHER_LINUX_GUEST);
        s_mapper.put("Other Linux (64-bit)", VirtualMachineGuestOsIdentifier.OTHER_LINUX_64_GUEST);

        s_mapper.put("Other (32-bit)", VirtualMachineGuestOsIdentifier.OTHER_GUEST);
        s_mapper.put("Other (64-bit)", VirtualMachineGuestOsIdentifier.OTHER_GUEST_64);
    }

    public static VirtualMachineGuestOsIdentifier getGuestOsIdentifier(String guestOsName) {
        return s_mapper.get(guestOsName);
    }

}
