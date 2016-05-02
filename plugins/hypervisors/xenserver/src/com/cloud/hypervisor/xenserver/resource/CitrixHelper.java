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
package com.cloud.hypervisor.xenserver.resource;

import java.util.HashMap;

import com.xensource.xenapi.Host;

/**
 * Reduce bloat inside CitrixResourceBase
 *
 */
public class CitrixHelper {
    private static final HashMap<String, MemoryValues> XenServerGuestOsMemoryMap = new HashMap<String, MemoryValues>(70);

    public static class MemoryValues {
        long max;
        long min;

        public MemoryValues(final long min, final long max) {
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
        XenServerGuestOsMemoryMap.put("CentOS 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.8 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.8 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.9 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.9 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.10 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 5.10 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("CentOS 6.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.8 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.8 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.9 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.9 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.10 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.10 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.8 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.8 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.9 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.9 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.10 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.10 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 7.0", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Debian GNU/Linux 5.0 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Debian GNU/Linux 5(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Debian GNU/Linux 6(32-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Debian GNU/Linux 6(64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Debian GNU/Linux 7(32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Debian GNU/Linux 7(64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 12 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 12 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows 7 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows 7 (64-bit)", new MemoryValues(2 * 1024l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows 8 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows 8 (64-bit)", new MemoryValues(2 * 1024l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows Server 2003 SP2 (32-bit)", new MemoryValues(256l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows Server 2003 SP2 (64-bit)", new MemoryValues(256l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows Server 2008 SP2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows Server 2008 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows Server 2008 R2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows Server 2008 R2 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows Server 2012 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows Server 2012 R2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows Vista SP2 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        XenServerGuestOsMemoryMap.put("Windows XP SP3 (32-bit)", new MemoryValues(256l, 4 * 1024l));
        XenServerGuestOsMemoryMap.put("Ubuntu 10.04 (32-bit)", new MemoryValues(128l, 512l));
        XenServerGuestOsMemoryMap.put("Ubuntu 10.04 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Ubuntu 10.10 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Ubuntu 10.10 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        XenServerGuestOsMemoryMap.put("Ubuntu 12.04 (32-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Ubuntu 12.04 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        XenServerGuestOsMemoryMap.put("Ubuntu 14.04 (32-bit)", new MemoryValues(512l, 32 * 1024l));
        XenServerGuestOsMemoryMap.put("Ubuntu 14.04 (64-bit)", new MemoryValues(512l, 128 * 1024l));
    }

    public static long getXenServerStaticMax(final String stdType, final boolean bootFromCD) {
        final MemoryValues recommendedMaxMinMemory = XenServerGuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMax();
    }

    public static long getXenServerStaticMin(final String stdType, final boolean bootFromCD) {
        final MemoryValues recommendedMaxMinMemory = XenServerGuestOsMemoryMap.get(stdType);
        if (recommendedMaxMinMemory == null) {
            return 0l;
        }
        return recommendedMaxMinMemory.getMin();
    }

    public static String getProductVersion(final Host.Record record) {
        String prodVersion = record.softwareVersion.get("product_version");
        if (prodVersion == null) {
            prodVersion = record.softwareVersion.get("platform_version").trim();
        } else {
            prodVersion = prodVersion.trim();
            final String[] items = prodVersion.split("\\.");
            if (Integer.parseInt(items[0]) > 6) {
                prodVersion = "6.5.0";
            } else if (Integer.parseInt(items[0]) == 6 && Integer.parseInt(items[1]) >= 4) {
                prodVersion = "6.5.0";
            }
        }
        return prodVersion;
    }

    public static String getPVbootloaderArgs(String guestOS) {
        if (guestOS.startsWith("SUSE Linux Enterprise Server")) {
            if (guestOS.contains("64-bit")) {
                return "--kernel /boot/vmlinuz-xen --ramdisk /boot/initrd-xen";
            } else if (guestOS.contains("32-bit")) {
                return "--kernel /boot/vmlinuz-xenpae --ramdisk /boot/initrd-xenpae";
            }
        }
        return "";
    }
}
