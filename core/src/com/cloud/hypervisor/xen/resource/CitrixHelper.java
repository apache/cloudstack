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

import org.apache.log4j.Logger;

/**
 * Reduce bloat inside CitrixResourceBase
 *
 */
public class CitrixHelper {
	 private static final Logger s_logger = Logger.getLogger(CitrixHelper.class);
	 private static final HashMap<String, String> _xcpGuestOsMap = new HashMap<String, String>(70);
	 private static final HashMap<String, String> _xenServerGuestOsMap = new HashMap<String, String>(70);
	 private static final ArrayList<String> _guestOsList = new ArrayList<String>(70);


    static {
    	_xcpGuestOsMap.put("CentOS 4.5 (32-bit)", "CentOS 4.5");
    	_xcpGuestOsMap.put("CentOS 4.6 (32-bit)", "CentOS 4.6");
    	_xcpGuestOsMap.put("CentOS 4.7 (32-bit)", "CentOS 4.7");
    	_xcpGuestOsMap.put("CentOS 4.8 (32-bit)", "CentOS 4.8");
    	_xcpGuestOsMap.put("CentOS 5.0 (32-bit)", "CentOS 5.0");
    	_xcpGuestOsMap.put("CentOS 5.0 (64-bit)", "CentOS 5.0 x64");
    	_xcpGuestOsMap.put("CentOS 5.1 (32-bit)", "CentOS 5.1");
    	_xcpGuestOsMap.put("CentOS 5.1 (64-bit)", "CentOS 5.1 x64");
    	_xcpGuestOsMap.put("CentOS 5.2 (32-bit)", "CentOS 5.2");
    	_xcpGuestOsMap.put("CentOS 5.2 (64-bit)", "CentOS 5.2 x64");
    	_xcpGuestOsMap.put("CentOS 5.3 (32-bit)", "CentOS 5.3");
    	_xcpGuestOsMap.put("CentOS 5.3 (64-bit)", "CentOS 5.3 x64");
    	_xcpGuestOsMap.put("CentOS 5.4 (32-bit)", "CentOS 5.4");
    	_xcpGuestOsMap.put("CentOS 5.4 (64-bit)", "CentOS 5.4 x64");
    	_xcpGuestOsMap.put("Debian GNU/Linux 5.0 (32-bit)", "Debian Lenny 5.0 (32-bit)");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.0 (32-bit)", "Oracle Enterprise Linux 5.0");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.0 (64-bit)", "Oracle Enterprise Linux 5.0 x64");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.1 (32-bit)", "Oracle Enterprise Linux 5.1");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.1 (64-bit)", "Oracle Enterprise Linux 5.1 x64");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.2 (32-bit)", "Oracle Enterprise Linux 5.2");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.2 (64-bit)", "Oracle Enterprise Linux 5.2 x64");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.3 (32-bit)", "Oracle Enterprise Linux 5.3");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.3 (64-bit)", "Oracle Enterprise Linux 5.3 x64");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.4 (32-bit)", "Oracle Enterprise Linux 5.4");
    	_xcpGuestOsMap.put("Oracle Enterprise Linux 5.4 (64-bit)", "Oracle Enterprise Linux 5.4 x64");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", "Red Hat Enterprise Linux 4.5");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", "Red Hat Enterprise Linux 4.6");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", "Red Hat Enterprise Linux 4.7");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", "Red Hat Enterprise Linux 4.8");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", "Red Hat Enterprise Linux 5.0");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", "Red Hat Enterprise Linux 5.0 x64");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", "Red Hat Enterprise Linux 5.1");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", "Red Hat Enterprise Linux 5.1 x64");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", "Red Hat Enterprise Linux 5.2");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", "Red Hat Enterprise Linux 5.2 x64");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", "Red Hat Enterprise Linux 5.3");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", "Red Hat Enterprise Linux 5.3 x64");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", "Red Hat Enterprise Linux 5.4");
    	_xcpGuestOsMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", "Red Hat Enterprise Linux 5.4 x64");
    	_xcpGuestOsMap.put("SUSE Linux Enterprise Server 9 SP4 (32-bit)", "SUSE Linux Enterprise Server 9 SP4");
    	_xcpGuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", "SUSE Linux Enterprise Server 10 SP1");
    	_xcpGuestOsMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", "SUSE Linux Enterprise Server 10 SP1 x64");
    	_xcpGuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2");
    	_xcpGuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", "SUSE Linux Enterprise Server 10 SP2 x64");
    	_xcpGuestOsMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", "Other install media");
    	_xcpGuestOsMap.put("SUSE Linux Enterprise Server 11 (32-bit)", "SUSE Linux Enterprise Server 11");
    	_xcpGuestOsMap.put("SUSE Linux Enterprise Server 11 (64-bit)", "SUSE Linux Enterprise Server 11 x64");
    	_xcpGuestOsMap.put("Windows 7 (32-bit)", "Windows 7");
    	_xcpGuestOsMap.put("Windows 7 (64-bit)", "Windows 7 x64");
    	_xcpGuestOsMap.put("Windows Server 2003 (32-bit)", "Windows Server 2003");
    	_xcpGuestOsMap.put("Windows Server 2003 (64-bit)", "Windows Server 2003 x64");
    	_xcpGuestOsMap.put("Windows Server 2008 (32-bit)", "Windows Server 2008");
    	_xcpGuestOsMap.put("Windows Server 2008 (64-bit)", "Windows Server 2008 x64");
    	_xcpGuestOsMap.put("Windows Server 2008 R2 (64-bit)", "Windows Server 2008 R2 x64");
    	_xcpGuestOsMap.put("Windows 2000 SP4 (32-bit)", "Windows 2000 SP4");
    	_xcpGuestOsMap.put("Windows Vista (32-bit)", "Windows Vista");
    	_xcpGuestOsMap.put("Windows XP SP2 (32-bit)", "Windows XP SP2");
    	_xcpGuestOsMap.put("Windows XP SP3 (32-bit)", "Windows XP SP3");
    	_xcpGuestOsMap.put("Other install media", "Other install media");
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
    	_xenServerGuestOsMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", "SUSE Linux Enterprise Server 10 SP2");
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
    }
    
    public static String getXcpGuestOsType(String stdType) {
        String guestOS =  _xcpGuestOsMap.get(stdType);
        if (guestOS == null) {
        	s_logger.debug("Can't find the guest os: " + stdType + " mapping into xenserver's guestOS type, start it as HVM guest");
        	guestOS = "Other install media";
        }
        return guestOS;
    }
    
    public static String getXenServerGuestOsType(String stdType) {
        String guestOS =  _xenServerGuestOsMap.get(stdType);
        if (guestOS == null) {
        	s_logger.debug("Can't find the guest os: " + stdType + " mapping into xenserver's guestOS type, start it as HVM guest");
        	guestOS = "Other install media";
        }
        return guestOS;
    }
}
