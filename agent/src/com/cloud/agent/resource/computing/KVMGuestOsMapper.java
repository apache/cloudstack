/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.agent.resource.computing;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class KVMGuestOsMapper {
	 private static final Logger s_logger = Logger.getLogger(KVMGuestOsMapper.class);
	private static Map<String, String> s_mapper = new HashMap<String, String>();
	static {
		s_mapper.put("CentOS 4.5 (32-bit)", "CentOS 4.5");
		s_mapper.put("CentOS 4.6 (32-bit)", "CentOS 4.6");
		s_mapper.put("CentOS 4.7 (32-bit)", "CentOS 4.7");
		s_mapper.put("CentOS 4.8 (32-bit)", "CentOS 4.8");
		s_mapper.put("CentOS 5.0 (32-bit)", "CentOS 5.0");
		s_mapper.put("CentOS 5.0 (64-bit)", "CentOS 5.0");
		s_mapper.put("CentOS 5.1 (32-bit)", "CentOS 5.1");
		s_mapper.put("CentOS 5.1 (64-bit)", "CentOS 5.1");
		s_mapper.put("CentOS 5.2 (32-bit)", "CentOS 5.2");
		s_mapper.put("CentOS 5.2 (64-bit)", "CentOS 5.2");
		s_mapper.put("CentOS 5.3 (32-bit)", "CentOS 5.3");
		s_mapper.put("CentOS 5.3 (64-bit)", "CentOS 5.3");
		s_mapper.put("CentOS 5.4 (32-bit)", "CentOS 5.4");
		s_mapper.put("CentOS 5.4 (64-bit)", "CentOS 5.4");
		s_mapper.put("CentOS 5.5 (32-bit)", "CentOS 5.5");
		s_mapper.put("CentOS 5.5 (64-bit)", "CentOS 5.5");
		s_mapper.put("Red Hat Enterprise Linux 2", "Red Hat Enterprise Linux 2");
		s_mapper.put("Red Hat Enterprise Linux 3 (32-bit)", "Red Hat Enterprise Linux 3");
		s_mapper.put("Red Hat Enterprise Linux 3 (64-bit)", "Red Hat Enterprise Linux 3");	
		s_mapper.put("Red Hat Enterprise Linux 4(64-bit)", "Red Hat Enterprise Linux 4");
		s_mapper.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5");
		s_mapper.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6");
		s_mapper.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7");
		s_mapper.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8");
		s_mapper.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5.0");
		s_mapper.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5.0");
		s_mapper.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5.1");
		s_mapper.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5.1");
		s_mapper.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5.2");
		s_mapper.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5.2");
		s_mapper.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5.3");
		s_mapper.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5.3");
		s_mapper.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5.4");
		s_mapper.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5.4");
		s_mapper.put("Red Hat Enterprise Linux 5.5 (32-bit)", "Red Hat Enterprise Linux 5.5");
		s_mapper.put("Red Hat Enterprise Linux 5.5 (64-bit)", "Red Hat Enterprise Linux 5.5");
		s_mapper.put("Red Hat Enterprise Linux 6.0 (32-bit)", "Red Hat Enterprise Linux 6.0");
		s_mapper.put("Red Hat Enterprise Linux 6.0 (64-bit)", "Red Hat Enterprise Linux 6.0");
		s_mapper.put("Fedora 13", "Fedora 13");
		s_mapper.put("Fedora 12", "Fedora 12");
		s_mapper.put("Fedora 11", "Fedora 11");
		s_mapper.put("Fedora 10", "Fedora 10");
		s_mapper.put("Fedora 9", "Fedora 9");
		s_mapper.put("Fedora 8", "Fedora 8");
		s_mapper.put("Ubuntu 10.04 (32-bit)", "Ubuntu 10.04");
		s_mapper.put("Ubuntu 10.04 (64-bit)", "Ubuntu 10.04");
		s_mapper.put("Ubuntu 9.10 (32-bit)", "Ubuntu 9.10");
		s_mapper.put("Ubuntu 9.10 (64-bit)", "Ubuntu 9.10");
		s_mapper.put("Ubuntu 9.04 (32-bit)", "Ubuntu 9.04");
		s_mapper.put("Ubuntu 9.04 (64-bit)", "Ubuntu 9.04");
		s_mapper.put("Ubuntu 8.10 (32-bit)", "Ubuntu 8.10");
		s_mapper.put("Ubuntu 8.10 (64-bit)", "Ubuntu 8.10");
		s_mapper.put("Ubuntu 8.04 (32-bit)", "Ubuntu 8.04");
		s_mapper.put("Ubuntu 8.04 (64-bit)", "Ubuntu 8.04");
		s_mapper.put("Debian GNU/Linux 5.0 (32-bit)", "Debian GNU/Linux 5");
		s_mapper.put("Debian GNU/Linux 5.0 (64-bit)", "Debian GNU/Linux 5");
		s_mapper.put("Debian GNU/Linux 4.0 (32-bit)", "Debian GNU/Linux 4");
		s_mapper.put("Debian GNU/Linux 4.0 (64-bit)", "Debian GNU/Linux 4");
		s_mapper.put("Other 2.6x Linux (32-bit)", "Other 2.6x Linux");
		s_mapper.put("Other 2.6x Linux (64-bit)", "Other 2.6x Linux");
		s_mapper.put("Other Linux (32-bit)", "Other Linux");
		s_mapper.put("Other Linux (64-bit)", "Other Linux");
		s_mapper.put("Other Ubuntu (32-bit)", "Other Linux");
		s_mapper.put("Other Ubuntu (64-bit)", "Other Linux");
		s_mapper.put("Asianux 3(32-bit)", "Other Linux");
		s_mapper.put("Asianux 3(64-bit)", "Other Linux");
		s_mapper.put("Windows 7 (32-bit)", "Windows 7");
		s_mapper.put("Windows 7 (64-bit)", "Windows 7");
		s_mapper.put("Windows Server 2003 Enterprise Edition(32-bit)", "Windows Server 2003");
		s_mapper.put("Windows Server 2003 Enterprise Edition(64-bit)", "Windows Server 2003");
		s_mapper.put("Windows Server 2003 DataCenter Edition(32-bit)", "Windows Server 2003");
		s_mapper.put("Windows Server 2003 DataCenter Edition(64-bit)", "Windows Server 2003");
		s_mapper.put("Windows Server 2003 Standard Edition(32-bit)", "Windows Server 2003");
		s_mapper.put("Windows Server 2003 Standard Edition(64-bit)", "Windows Server 2003");
		s_mapper.put("Windows Server 2003 Web Edition", "Windows Server 2003");
		s_mapper.put("Microsoft Small Bussiness Server 2003", "Windows Server 2003");
		s_mapper.put("Windows Server 2008 (32-bit)", "Windows Server 2008");
		s_mapper.put("Windows Server 2008 (64-bit)", "Windows Server 2008");
		s_mapper.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008");
		s_mapper.put("Windows 2000 Server SP4 (32-bit)", "Windows 2000");
		s_mapper.put("Windows 2000 Server", "Windows 2000");
		s_mapper.put("Windows 2000 Advanced Server", "Windows 2000");
		s_mapper.put("Windows 2000 Professional", "Windows 2000");
		s_mapper.put("Windows Vista (32-bit)", "Windows Vista");
		s_mapper.put("Windows Vista (64-bit)", "Windows Vista");
		s_mapper.put("Windows XP SP2 (32-bit)", "Windows XP");
		s_mapper.put("Windows XP SP3 (32-bit)", "Windows XP");
		s_mapper.put("Windows XP (32-bit)", "Windows XP");
		s_mapper.put("Windows XP (64-bit)", "Windows XP");
		s_mapper.put("Windows 98", "Windows 98");
		s_mapper.put("Windows 95", "Windows 95");
		s_mapper.put("Windows NT 4", "Windows NT");
		s_mapper.put("Windows 3.1", "Windows 3.1");
		s_mapper.put("Windows PV", "Other PV");
		s_mapper.put("Other PV (32-bit)", "Other PV");
		s_mapper.put("Other PV (64-bit)", "Other PV");

	}
	
	public static String getGuestOsName(String guestOsName) {
		String guestOS = s_mapper.get(guestOsName);
		if (guestOS == null) {
			s_logger.debug("Can't find the mapping of guest os: " + guestOsName);
			return "Other";
		} else {
			return guestOS;
		}
	}
}

