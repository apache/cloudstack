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
import com.cloud.storage.GuestOSVO;
import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.exception.CloudRuntimeException;

import java.io.InputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Upgrade41800to41810 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {
    private GuestOsMapper guestOsMapper = new GuestOsMapper();

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
        fixForeignKeyNames(conn);
        updateGuestOsMappings(conn);
        copyGuestOsMappingsToVMware80u1();
        addForeignKeyToAutoscaleVmprofiles(conn);
        mergeDuplicateGuestOSes();
        addIndexes(conn);
    }

    private void mergeDuplicateGuestOSes() {
        guestOsMapper.mergeDuplicates();
        List<GuestOSVO> nines = guestOsMapper.listByDisplayName("Red Hat Enterprise Linux 9");
        GuestOSVO nineDotZero = guestOsMapper.listByDisplayName("Red Hat Enterprise Linux 9.0").get(0);
        guestOsMapper.makeNormative(nineDotZero, new HashSet<>(nines));
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
        logger.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    private void updateGuestOsMappings(Connection conn) {
        logger.debug("Updating guest OS mappings");

        GuestOsMapper guestOsMapper = new GuestOsMapper();
        List<GuestOSHypervisorMapping> mappings = new ArrayList<>();

        logger.debug("Adding Ubuntu 20.04 support for VMware 6.5+");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "6.5", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "6.7", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "6.7.1", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "6.7.2", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "6.7.3", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "7.0", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "8.0", "ubuntu64Guest"), 10, "Ubuntu 20.04 LTS");

        logger.debug("Adding Ubuntu 22.04 support for KVM and VMware 6.5+");
        mappings.add(new GuestOSHypervisorMapping("KVM", "default", "Ubuntu 22.04 LTS"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "ubuntu64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "ubuntu64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "ubuntu64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "ubuntu64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "ubuntu64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "ubuntu64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "ubuntu64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.2.0", "ubuntu64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.3.0", "ubuntu64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "8.0", "ubuntu64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(10, "Ubuntu 22.04 LTS", mappings);
        mappings.clear();

        logger.debug("Correcting guest OS names in hypervisor mappings for VMware 8.0 ad 8.0.0.1");
        final String hypervisorVMware = Hypervisor.HypervisorType.VMware.name();
        final String hypervisorVersionVmware8 = "8.0";
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "AlmaLinux 9", new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "almalinux_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Oracle Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "oracleLinux9_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Rocky Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "rockylinux_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "AlmaLinux 9", new GuestOSHypervisorMapping(hypervisorVMware, "8.0.0.1", "almalinux_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Oracle Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, "8.0.0.1", "oracleLinux9_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Rocky Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, "8.0.0.1", "rockylinux_64Guest"));

        logger.debug("Correcting guest OS names in hypervisor mappings for Red Hat Enterprise Linux 9");
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Red Hat Enterprise Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, "7.0", "rhel9_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Red Hat Enterprise Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, "7.0.1.0", "rhel9_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Red Hat Enterprise Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, "7.0.2.0", "rhel9_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Red Hat Enterprise Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, "7.0.3.0", "rhel9_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Red Hat Enterprise Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "rhel9_64Guest"));
        guestOsMapper.updateGuestOsNameInHypervisorMapping(1, "Red Hat Enterprise Linux 9", new GuestOSHypervisorMapping(hypervisorVMware, "8.0.0.1", "rhel9_64Guest"));

        logger.debug("Adding new guest OS ids in hypervisor mappings for VMware 8.0");
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

        // Add support for vmkernel8Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "vmkernel8Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "VMware ESXi 8.0", mappings);
        mappings.clear();

        // Add support for windows11_64Guest from VMware 8.0
        mappings.add(new GuestOSHypervisorMapping(hypervisorVMware, hypervisorVersionVmware8, "windows11_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(6, "Windows 11 (64-bit)", mappings);
        mappings.clear();
    }

    private void copyGuestOsMappingsToVMware80u1() {
        logger.debug("Copying guest OS mappings from VMware 8.0 to VMware 8.0.1");
        GuestOsMapper guestOsMapper = new GuestOsMapper();
        guestOsMapper.copyGuestOSHypervisorMappings(Hypervisor.HypervisorType.VMware, "8.0", "8.0.1");
    }

    private void fixForeignKeyNames(Connection conn) {
        //Alter foreign key name for user_vm table from fk_user_data_id to fk_user_vm__user_data_id (if exists)
        List<String> keys = new ArrayList<String>();
        keys.add("fk_user_data_id");
        keys.add("fk_user_vm__user_data_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.user_vm", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.user_vm", keys, false);
        DbUpgradeUtils.addForeignKey(conn, "user_vm", "user_data_id", "user_data", "id");

        //Alter foreign key name for vm_template table from fk_user_data_id to fk_vm_template__user_data_id (if exists)
        keys = new ArrayList<>();
        keys.add("fk_user_data_id");
        keys.add("fk_vm_template__user_data_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_template", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_template", keys, false);
        DbUpgradeUtils.addForeignKey(conn, "vm_template", "user_data_id", "user_data", "id");

        //Alter foreign key name for volumes table from fk_passphrase_id to fk_volumes__passphrase_id (if exists)
        keys = new ArrayList<>();
        keys.add("fk_passphrase_id");
        keys.add("fk_volumes__passphrase_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.volumes", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.volumes", keys, false);
        DbUpgradeUtils.addForeignKey(conn, "volumes", "passphrase_id","passphrase", "id");
    }

    private void addForeignKeyToAutoscaleVmprofiles(Connection conn) {
        DbUpgradeUtils.addForeignKey(conn, "autoscale_vmprofiles", "user_data_id", "user_data", "id");
    }

    private void addIndexes(Connection conn) {
        DbUpgradeUtils.addIndexIfNeeded(conn, "cluster_details", "name");
    }
}
