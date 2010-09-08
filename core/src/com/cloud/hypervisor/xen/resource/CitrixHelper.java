/**
 *  Copyright (C) 2010 Cloud.com.  All rights reserved.
 *
 * This software is licensed under the GNU General Public License v3 or later. 
 *
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later
version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package com.cloud.hypervisor.xen.resource;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Reduce bloat inside CitrixResourceBase
 *
 */
public class CitrixHelper {
	private static final HashMap<String, String> _guestOsMap = new HashMap<String, String>(70);
	private static final ArrayList<String> _guestOsList = new ArrayList<String>(70);


    static {
        _guestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5");
        _guestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6");
        _guestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7");
        _guestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8");
        _guestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5.0");
        _guestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5.0 x64");
        _guestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5.1");
        _guestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5.1 x64");
        _guestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5.2");
        _guestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5.2 x64");
        _guestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5.3");
        _guestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5.3 x64");
        _guestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5.4");
        _guestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5.4 x64");
        _guestOsMap.put("Debian Lenny 5.0 (32-bit)", "Debian Lenny 5.0");
        _guestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5.0");
        _guestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5.0 x64");
        _guestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5.1");
        _guestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5.1 x64");
        _guestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5.2");
        _guestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5.2 x64");
        _guestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5.3");
        _guestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5.3 x64");
        _guestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5.4");
        _guestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5.4 x64");
        _guestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5");
        _guestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6");
        _guestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7");
        _guestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8");
        _guestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5.0");
        _guestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5.0 x64");
        _guestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5.1");
        _guestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5.1 x64");
        _guestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5.2");
        _guestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5.2 x64");
        _guestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5.3");
        _guestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5.3 x64");
        _guestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5.4");
        _guestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5.4 x64");
        _guestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4");
        _guestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1");
        _guestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 x64");
        _guestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2");
        _guestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 x64");
        _guestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "Other install media");
        _guestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11");
        _guestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 x64");
        _guestOsMap.put("Windows 7 (32-bit)", "Windows 7");
        _guestOsMap.put("Windows 7 (64-bit)", "Windows 7 x64");
        _guestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003");
        _guestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 x64");
        _guestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008");
        _guestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 x64");
        _guestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 x64");
        _guestOsMap.put("Windows 2000 SP4 (32-bit)", "Windows 2000 SP4");
        _guestOsMap.put("Windows Vista (32-bit)", "Windows Vista");
        _guestOsMap.put("Windows XP SP2 (32-bit)", "Windows XP SP2");
        _guestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3");
        _guestOsMap.put("Other install media", "Other install media");

        //access by index
        _guestOsList.add("CentOS 4.5");
        _guestOsList.add("CentOS 4.6");
        _guestOsList.add("CentOS 4.7");
        _guestOsList.add("CentOS 4.8");
        _guestOsList.add("CentOS 5.0");
        _guestOsList.add("CentOS 5.0 x64");
        _guestOsList.add("CentOS 5.1");
        _guestOsList.add("CentOS 5.1 x64");
        _guestOsList.add("CentOS 5.2");
        _guestOsList.add("CentOS 5.2 x64");
        _guestOsList.add("CentOS 5.3");
        _guestOsList.add("CentOS 5.3 x64");
        _guestOsList.add("CentOS 5.4");
        _guestOsList.add("CentOS 5.4 x64");
        _guestOsList.add("Debian Lenny 5.0");
        _guestOsList.add("Oracle Enterprise Linux 5.0");
        _guestOsList.add("Oracle Enterprise Linux 5.0 x64");
        _guestOsList.add("Oracle Enterprise Linux 5.1");
        _guestOsList.add("Oracle Enterprise Linux 5.1 x64");
        _guestOsList.add("Oracle Enterprise Linux 5.2");
        _guestOsList.add("Oracle Enterprise Linux 5.2 x64");
        _guestOsList.add("Oracle Enterprise Linux 5.3");
        _guestOsList.add("Oracle Enterprise Linux 5.3 x64");
        _guestOsList.add("Oracle Enterprise Linux 5.4");
        _guestOsList.add("Oracle Enterprise Linux 5.4 x64");
        _guestOsList.add("Red Hat Enterprise Linux 4.5");
        _guestOsList.add("Red Hat Enterprise Linux 4.6");
        _guestOsList.add("Red Hat Enterprise Linux 4.7");
        _guestOsList.add("Red Hat Enterprise Linux 4.8");
        _guestOsList.add("Red Hat Enterprise Linux 5.0");
        _guestOsList.add("Red Hat Enterprise Linux 5.0 x64");
        _guestOsList.add("Red Hat Enterprise Linux 5.1");
        _guestOsList.add("Red Hat Enterprise Linux 5.1 x64");
        _guestOsList.add("Red Hat Enterprise Linux 5.2");
        _guestOsList.add("Red Hat Enterprise Linux 5.2 x64");
        _guestOsList.add("Red Hat Enterprise Linux 5.3");
        _guestOsList.add("Red Hat Enterprise Linux 5.3 x64");
        _guestOsList.add("Red Hat Enterprise Linux 5.4");
        _guestOsList.add("Red Hat Enterprise Linux 5.4 x64");
        _guestOsList.add("SUSE Linux Enterprise Server 9 SP4");
        _guestOsList.add("SUSE Linux Enterprise Server 10 SP1");
        _guestOsList.add("SUSE Linux Enterprise Server 10 SP1 x64");
        _guestOsList.add("SUSE Linux Enterprise Server 10 SP2");
        _guestOsList.add("SUSE Linux Enterprise Server 10 SP2 x64");
        _guestOsList.add("Other install media");
        _guestOsList.add("SUSE Linux Enterprise Server 11");
        _guestOsList.add("SUSE Linux Enterprise Server 11 x64");
        _guestOsList.add("Windows 7");
        _guestOsList.add("Windows 7 x64");
        _guestOsList.add("Windows Server 2003");
        _guestOsList.add("Windows Server 2003 x64");
        _guestOsList.add("Windows Server 2008");
        _guestOsList.add("Windows Server 2008 x64");
        _guestOsList.add("Windows Server 2008 R2 x64");
        _guestOsList.add("Windows 2000 SP4");
        _guestOsList.add("Windows Vista");
        _guestOsList.add("Windows XP SP2");
        _guestOsList.add("Windows XP SP3");
        _guestOsList.add("Other install media");
    }
    
    public static String getGuestOsType(String stdType) {
        return _guestOsMap.get(stdType);
    }

    public static String getGuestOsType(long guestOsId) {
        return _guestOsMap.get(guestOsId-1);
    }
}
