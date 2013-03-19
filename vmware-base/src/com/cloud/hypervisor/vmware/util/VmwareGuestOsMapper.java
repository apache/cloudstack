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
		s_mapper.put("DOS", VirtualMachineGuestOsIdentifier.DOS_GUEST);
		s_mapper.put("OS/2", VirtualMachineGuestOsIdentifier.OS_2_GUEST);

		s_mapper.put("Windows 3.1", VirtualMachineGuestOsIdentifier.WIN_31_GUEST);
		s_mapper.put("Windows 95", VirtualMachineGuestOsIdentifier.WIN_95_GUEST);
		s_mapper.put("Windows 98", VirtualMachineGuestOsIdentifier.WIN_98_GUEST);
		s_mapper.put("Windows NT 4", VirtualMachineGuestOsIdentifier.WIN_NT_GUEST);
		s_mapper.put("Windows XP (32-bit)", VirtualMachineGuestOsIdentifier.WIN_XP_PRO_GUEST);
		s_mapper.put("Windows XP (64-bit)", VirtualMachineGuestOsIdentifier.WIN_XP_PRO_64_GUEST);
		s_mapper.put("Windows XP SP2 (32-bit)", VirtualMachineGuestOsIdentifier.WIN_XP_PRO_GUEST);
		s_mapper.put("Windows XP SP3 (32-bit)", VirtualMachineGuestOsIdentifier.WIN_XP_PRO_GUEST);
		s_mapper.put("Windows Vista (32-bit)", VirtualMachineGuestOsIdentifier.WIN_VISTA_GUEST);
		s_mapper.put("Windows Vista (64-bit)", VirtualMachineGuestOsIdentifier.WIN_VISTA_64_GUEST);
		s_mapper.put("Windows 7 (32-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_7_GUEST);
		s_mapper.put("Windows 7 (64-bit)", VirtualMachineGuestOsIdentifier.WINDOWS_7_64_GUEST);

		s_mapper.put("Windows 2000 Professional", VirtualMachineGuestOsIdentifier.WIN_2000_PRO_GUEST);
		s_mapper.put("Windows 2000 Server", VirtualMachineGuestOsIdentifier.WIN_2000_SERV_GUEST);
		s_mapper.put("Windows 2000 Server SP4 (32-bit)", VirtualMachineGuestOsIdentifier.WIN_2000_SERV_GUEST);
		s_mapper.put("Windows 2000 Advanced Server", VirtualMachineGuestOsIdentifier.WIN_2000_ADV_SERV_GUEST);

		s_mapper.put("Windows Server 2003 Enterprise Edition(32-bit)", VirtualMachineGuestOsIdentifier.WIN_NET_ENTERPRISE_GUEST);
		s_mapper.put("Windows Server 2003 Enterprise Edition(64-bit)", VirtualMachineGuestOsIdentifier.WIN_NET_ENTERPRISE_64_GUEST);
		s_mapper.put("Windows Server 2008 R2 (64-bit)", VirtualMachineGuestOsIdentifier.WIN_LONGHORN_64_GUEST);
		s_mapper.put("Windows Server 2003 DataCenter Edition(32-bit)", VirtualMachineGuestOsIdentifier.WIN_NET_DATACENTER_GUEST);
		s_mapper.put("Windows Server 2003 DataCenter Edition(64-bit)", VirtualMachineGuestOsIdentifier.WIN_NET_DATACENTER_64_GUEST);
		s_mapper.put("Windows Server 2003 Standard Edition(32-bit)", VirtualMachineGuestOsIdentifier.WIN_NET_STANDARD_GUEST);
		s_mapper.put("Windows Server 2003 Standard Edition(64-bit)", VirtualMachineGuestOsIdentifier.WIN_NET_STANDARD_64_GUEST);
		s_mapper.put("Windows Server 2003 Web Edition", VirtualMachineGuestOsIdentifier.WIN_NET_WEB_GUEST);
		s_mapper.put("Microsoft Small Bussiness Server 2003", VirtualMachineGuestOsIdentifier.WIN_NET_BUSINESS_GUEST);

		s_mapper.put("Windows Server 2008 (32-bit)", VirtualMachineGuestOsIdentifier.WIN_LONGHORN_GUEST);
		s_mapper.put("Windows Server 2008 (64-bit)", VirtualMachineGuestOsIdentifier.WIN_LONGHORN_64_GUEST);

        s_mapper.put("Windows 8", VirtualMachineGuestOsIdentifier.WINDOWS_8_GUEST);
        s_mapper.put("Windows 8 (64 bit)", VirtualMachineGuestOsIdentifier.WINDOWS_8_64_GUEST);
        s_mapper.put("Windows 8 Server (64 bit)", VirtualMachineGuestOsIdentifier.WINDOWS_8_SERVER_64_GUEST);

		s_mapper.put("Open Enterprise Server", VirtualMachineGuestOsIdentifier.OES_GUEST);

		s_mapper.put("Asianux 3(32-bit)", VirtualMachineGuestOsIdentifier.ASIANUX_3_GUEST);
		s_mapper.put("Asianux 3(64-bit)", VirtualMachineGuestOsIdentifier.ASIANUX_3_64_GUEST);

		s_mapper.put("Debian GNU/Linux 5(64-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_5_64_GUEST);
        s_mapper.put("Debian GNU/Linux 5.0 (32-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_5_GUEST);
		s_mapper.put("Debian GNU/Linux 4(32-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_4_GUEST);
		s_mapper.put("Debian GNU/Linux 4(64-bit)", VirtualMachineGuestOsIdentifier.DEBIAN_4_64_GUEST);

		s_mapper.put("Novell Netware 6.x", VirtualMachineGuestOsIdentifier.NETWARE_6_GUEST);
		s_mapper.put("Novell Netware 5.1", VirtualMachineGuestOsIdentifier.NETWARE_5_GUEST);

		s_mapper.put("Sun Solaris 10(32-bit)", VirtualMachineGuestOsIdentifier.SOLARIS_10_GUEST);
		s_mapper.put("Sun Solaris 10(64-bit)", VirtualMachineGuestOsIdentifier.SOLARIS_10_64_GUEST);
		s_mapper.put("Sun Solaris 9(Experimental)", VirtualMachineGuestOsIdentifier.SOLARIS_9_GUEST);
		s_mapper.put("Sun Solaris 8(Experimental)", VirtualMachineGuestOsIdentifier.SOLARIS_8_GUEST);

		s_mapper.put("FreeBSD (32-bit)", VirtualMachineGuestOsIdentifier.FREEBSD_GUEST);
		s_mapper.put("FreeBSD (64-bit)", VirtualMachineGuestOsIdentifier.FREEBSD_64_GUEST);

		s_mapper.put("SCO OpenServer 5", VirtualMachineGuestOsIdentifier.OTHER_GUEST);
		s_mapper.put("SCO UnixWare 7", VirtualMachineGuestOsIdentifier.UNIX_WARE_7_GUEST);

		s_mapper.put("SUSE Linux Enterprise 8(32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
		s_mapper.put("SUSE Linux Enterprise 8(64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
		s_mapper.put("SUSE Linux Enterprise 9(32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
		s_mapper.put("SUSE Linux Enterprise 9(64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
		s_mapper.put("SUSE Linux Enterprise 10(32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
		s_mapper.put("SUSE Linux Enterprise 10(64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);
		s_mapper.put("SUSE Linux Enterprise 10(32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
		s_mapper.put("Other SUSE Linux(32-bit)", VirtualMachineGuestOsIdentifier.SUSE_GUEST);
		s_mapper.put("Other SUSE Linux(64-bit)", VirtualMachineGuestOsIdentifier.SUSE_64_GUEST);

		s_mapper.put("CentOS 4.5 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 4.6 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 4.7 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 4.8 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 5.0 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 5.0 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
		s_mapper.put("CentOS 5.1 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 5.1 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
		s_mapper.put("CentOS 5.2 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 5.2 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
		s_mapper.put("CentOS 5.3 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 5.3 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
		s_mapper.put("CentOS 5.4 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 5.4 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
		s_mapper.put("CentOS 5.5 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 5.5 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
		s_mapper.put("CentOS 5.6 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 5.6 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
		s_mapper.put("CentOS 6.0 (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("CentOS 6.0 (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);
		s_mapper.put("Other CentOS (32-bit)", VirtualMachineGuestOsIdentifier.CENTOS_GUEST);
		s_mapper.put("Other CentOS (64-bit)", VirtualMachineGuestOsIdentifier.CENTOS_64_GUEST);

		s_mapper.put("Red Hat Enterprise Linux 2", VirtualMachineGuestOsIdentifier.RHEL_2_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 3 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_3_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 3 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_3_64_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 4 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_4_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 4 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_4_64_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_64_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 6 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 6 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_6_64_GUEST);

		s_mapper.put("Red Hat Enterprise Linux 4.5 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_4_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 4.6 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_4_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 4.7 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_4_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 4.8 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_4_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.0 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.0 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_64_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.1 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.1 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_64_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.2 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.2 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_64_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.3 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.3 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_64_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.4 (32-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_GUEST);
		s_mapper.put("Red Hat Enterprise Linux 5.4 (64-bit)", VirtualMachineGuestOsIdentifier.RHEL_5_64_GUEST);

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
		s_mapper.put("Other Ubuntu (32-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_GUEST);
		s_mapper.put("Other Ubuntu (64-bit)", VirtualMachineGuestOsIdentifier.UBUNTU_64_GUEST);

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
