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
package com.cloud.upgrade.dao;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.GuestOSHypervisorMapping;
import com.cloud.upgrade.GuestOsMapper;
import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class Upgrade41800to41810 implements DbUpgrade, DbUpgradeSystemVmTemplate {
    final static Logger LOG = Logger.getLogger(Upgrade41800to41810.class);
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.18.0.0", "4.18.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.18.1.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41800to41810.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateGuestOsMappings();
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41800to41810-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    private void initSystemVmTemplateRegistration() {
        systemVmTemplateRegistration = new SystemVmTemplateRegistration("");
    }

    @Override
    public void updateSystemVmTemplates(Connection conn) {
        LOG.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    private void updateGuestOsMappings() {
        LOG.debug("Updating guest OS mappings");

        GuestOsMapper guestOsMapper = new GuestOsMapper();
        List<GuestOSHypervisorMapping> mappings = new ArrayList<>();

        final String hypervisorVMware = Hypervisor.HypervisorType.VMware.name();
        final String hypervisorVersionVmware8 = "8.0";

        // Add support for almalinux_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "almalinux_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(1, "AlmaLinux (64-bit)", mappings);
        mappings.clear();

        // Add support for darwin22_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "darwin22_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 13 (64-bit)", mappings);
        mappings.clear();

        // Add support for darwin23_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "darwin23_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 14 (64-bit)", mappings);
        mappings.clear();

        // Add support for debian12_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "debian12_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Debian GNU/Linux 12 (64-bit)", mappings);
        mappings.clear();

        // Add support for debian12Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "debian12Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Debian GNU/Linux 12 (32-bit)", mappings);
        mappings.clear();

        // Add support for freebsd14_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "freebsd14_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(9, "FreeBSD 14 (64-bit)", mappings);
        mappings.clear();

        // Add support for freebsd14Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "freebsd14Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(9, "FreeBSD 14 (32-bit)", mappings);
        mappings.clear();

        // Add support for other6xLinux64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "other6xLinux64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Linux 6.x Kernel (64-bit)", mappings);
        mappings.clear();

        // Add support for other6xLinuxGuest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "other6xLinuxGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Linux 6.x Kernel (32-bit)", mappings);
        mappings.clear();

        // Add support for rockylinux_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "rockylinux_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(1, "Rocky Linux (64-bit)", mappings);
        mappings.clear();

        // Add support for vmkernel8Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "vmkernel8Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "VMware ESXi 8.0", mappings);
        mappings.clear();

        // Add support for windows11_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "windows11_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(6, "Windows 11 (64-bit)", mappings);
        mappings.clear();

        // Add support for windows12_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "windows12_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(6, "Windows 12 (64-bit)", mappings);
        mappings.clear();

        // Add support for windows2022srvNext_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "windows2022srvNext_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(6, "Windows Server 2025 (64-bit)", mappings);
        mappings.clear();
    }
}
