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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.storage.GuestOSHypervisorMapping;
import com.cloud.upgrade.GuestOsMapper;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41500to41510 implements DbUpgrade, DbUpgradeSystemVmTemplate {

    final static Logger LOG = Logger.getLogger(Upgrade41500to41510.class);
    private GuestOsMapper guestOsMapper = new GuestOsMapper();

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.15.0.0", "4.15.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.15.1.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41500to41510.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        correctGuestOsNames(conn);
        updateGuestOsMappings(conn);
    }

    @Override
    @SuppressWarnings("serial")
    public void updateSystemVmTemplates(final Connection conn) {
        LOG.debug("Updating System Vm template IDs");
        final Set<Hypervisor.HypervisorType> hypervisorsListInUse = new HashSet<Hypervisor.HypervisorType>();
        try (PreparedStatement pstmt = conn.prepareStatement("select distinct(hypervisor_type) from `cloud`.`cluster` where removed is null"); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                switch (Hypervisor.HypervisorType.getType(rs.getString(1))) {
                    case XenServer:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.XenServer);
                        break;
                    case KVM:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.KVM);
                        break;
                    case VMware:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.VMware);
                        break;
                    case Hyperv:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.Hyperv);
                        break;
                    case LXC:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.LXC);
                        break;
                    case Ovm3:
                        hypervisorsListInUse.add(Hypervisor.HypervisorType.Ovm3);
                        break;
                    default:
                        break;
                }
            }
        } catch (final SQLException e) {
            LOG.error("updateSystemVmTemplates: Exception caught while getting hypervisor types from clusters: " + e.getMessage());
            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting hypervisor types from clusters", e);
        }

        final Map<Hypervisor.HypervisorType, String> NewTemplateNameList = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.15.1");
                put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.15.1");
                put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.15.1");
                put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.15.1");
                put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.15.1");
                put(Hypervisor.HypervisorType.Ovm3, "systemvm-ovm3-4.15.1");
            }
        };

        final Map<Hypervisor.HypervisorType, String> routerTemplateConfigurationNames = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
                put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
                put(Hypervisor.HypervisorType.XenServer, "router.template.xenserver");
                put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
                put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
                put(Hypervisor.HypervisorType.Ovm3, "router.template.ovm3");
            }
        };

        final Map<Hypervisor.HypervisorType, String> newTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.1-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.VMware, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.1-vmware.ova");
                put(Hypervisor.HypervisorType.XenServer, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.1-xen.vhd.bz2");
                put(Hypervisor.HypervisorType.Hyperv, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.1-hyperv.vhd.zip");
                put(Hypervisor.HypervisorType.LXC, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.1-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.Ovm3, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.1-ovm.raw.bz2");
            }
        };

        final Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "0e9f9a7d0957c3e0a2088e41b2da2cec");
                put(Hypervisor.HypervisorType.XenServer, "86373992740b1eca8aff8b08ebf3aea5");
                put(Hypervisor.HypervisorType.VMware, "4006982765846d373eb3719b2fe4d720");
                put(Hypervisor.HypervisorType.Hyperv, "0b9514e4b6cba1f636fea2125f0f7a5f");
                put(Hypervisor.HypervisorType.LXC, "0e9f9a7d0957c3e0a2088e41b2da2cec");
                put(Hypervisor.HypervisorType.Ovm3, "ae3977e696b3e6c81bdcbb792d514d29");
            }
        };

        for (final Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()) {
            LOG.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
            try (PreparedStatement pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null and account_id in (select id from account where type = 1 and removed is NULL) order by id desc limit 1")) {
                // Get systemvm template id for corresponding hypervisor
                long templateId = -1;
                pstmt.setString(1, hypervisorAndTemplateName.getValue());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        templateId = rs.getLong(1);
                    }
                } catch (final SQLException e) {
                    LOG.error("updateSystemVmTemplates: Exception caught while getting ids of templates: " + e.getMessage());
                    throw new CloudRuntimeException("updateSystemVmTemplates: Exception caught while getting ids of templates", e);
                }

                // change template type to SYSTEM
                if (templateId != -1) {
                    try (PreparedStatement templ_type_pstmt = conn.prepareStatement("update `cloud`.`vm_template` set type='SYSTEM' where id = ?");) {
                        templ_type_pstmt.setLong(1, templateId);
                        templ_type_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        LOG.error("updateSystemVmTemplates:Exception while updating template with id " + templateId + " to be marked as 'system': " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while updating template with id " + templateId + " to be marked as 'system'", e);
                    }
                    // update template ID of system Vms
                    try (PreparedStatement update_templ_id_pstmt = conn
                            .prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ? and removed is NULL");) {
                        update_templ_id_pstmt.setLong(1, templateId);
                        update_templ_id_pstmt.setString(2, hypervisorAndTemplateName.getKey().toString());
                        update_templ_id_pstmt.executeUpdate();
                    } catch (final Exception e) {
                        LOG.error("updateSystemVmTemplates:Exception while setting template for " + hypervisorAndTemplateName.getKey().toString() + " to " + templateId
                                + ": " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting template for " + hypervisorAndTemplateName.getKey().toString() + " to "
                                + templateId, e);
                    }

                    // Change value of global configuration parameter
                    // router.template.* for the corresponding hypervisor
                    try (PreparedStatement update_pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?");) {
                        update_pstmt.setString(1, hypervisorAndTemplateName.getValue());
                        update_pstmt.setString(2, routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()));
                        update_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        LOG.error("updateSystemVmTemplates:Exception while setting " + routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()) + " to "
                                + hypervisorAndTemplateName.getValue() + ": " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting "
                                + routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()) + " to " + hypervisorAndTemplateName.getValue(), e);
                    }

                    // Change value of global configuration parameter
                    // minreq.sysvmtemplate.version for the ACS version
                    try (PreparedStatement update_pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?");) {
                        update_pstmt.setString(1, "4.15.1");
                        update_pstmt.setString(2, "minreq.sysvmtemplate.version");
                        update_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        LOG.error("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.15.1: " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.15.1", e);
                    }
                } else {
                    if (hypervisorsListInUse.contains(hypervisorAndTemplateName.getKey())) {
                        throw new CloudRuntimeException(getUpgradedVersion() + hypervisorAndTemplateName.getKey() + " SystemVm template not found. Cannot upgrade system Vms");
                    } else {
                        LOG.warn(getUpgradedVersion() + hypervisorAndTemplateName.getKey() + " SystemVm template not found. " + hypervisorAndTemplateName.getKey()
                                + " hypervisor is not used, so not failing upgrade");
                        // Update the latest template URLs for corresponding
                        // hypervisor
                        try (PreparedStatement update_templ_url_pstmt = conn
                                .prepareStatement("UPDATE `cloud`.`vm_template` SET url = ? , checksum = ? WHERE hypervisor_type = ? AND type = 'SYSTEM' AND removed is null order by id desc limit 1");) {
                            update_templ_url_pstmt.setString(1, newTemplateUrl.get(hypervisorAndTemplateName.getKey()));
                            update_templ_url_pstmt.setString(2, newTemplateChecksum.get(hypervisorAndTemplateName.getKey()));
                            update_templ_url_pstmt.setString(3, hypervisorAndTemplateName.getKey().toString());
                            update_templ_url_pstmt.executeUpdate();
                        } catch (final SQLException e) {
                            LOG.error("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type "
                                    + hypervisorAndTemplateName.getKey().toString() + ": " + e.getMessage());
                            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type "
                                    + hypervisorAndTemplateName.getKey().toString(), e);
                        }
                    }
                }
            } catch (final SQLException e) {
                LOG.error("updateSystemVmTemplates:Exception while getting ids of templates: " + e.getMessage());
                throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting ids of templates", e);
            }
        }
        LOG.debug("Updating System Vm Template IDs Complete");
    }

    private void correctGuestOsNames(final Connection conn) {
        guestOsMapper.updateGuestOsName(7, "Fedora Linux", "Fedora Linux (32 bit)");
        guestOsMapper.updateGuestOsName(7, "Mandriva Linux", "Mandriva Linux (32 bit)");

        GuestOSHypervisorMapping mapping = new GuestOSHypervisorMapping("VMware", "6.7.3", "opensuseGuest");
        guestOsMapper.updateGuestOsNameFromMapping("OpenSUSE Linux (32 bit)", mapping);
    }

    private void updateGuestOsMappings(final Connection conn) {
        LOG.debug("Updating guest OS mappings");

        // Add support for SUSE Linux Enterprise Desktop 12 SP3 (64-bit) for Xenserver 8.1.0
        List<GuestOSHypervisorMapping> mappings = new ArrayList<GuestOSHypervisorMapping>();
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "SUSE Linux Enterprise Desktop 12 SP3 (64-bit)"));
        guestOsMapper.addGuestOsAndHypervisorMappings (5, "SUSE Linux Enterprise Desktop 12 SP3 (64-bit)", mappings);
        mappings.clear();

        // Add support for SUSE Linux Enterprise Desktop 12 SP4 (64-bit) for Xenserver 8.1.0
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "SUSE Linux Enterprise Desktop 12 SP4 (64-bit)"));
        guestOsMapper.addGuestOsAndHypervisorMappings (5, "SUSE Linux Enterprise Desktop 12 SP4 (64-bit)", mappings);
        mappings.clear();

        // Add support for SUSE Linux Enterprise Server 12 SP4 (64-bit) for Xenserver 8.1.0
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "SUSE Linux Enterprise Server 12 SP4 (64-bit)"));
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "NeoKylin Linux Server 7"));
        guestOsMapper.addGuestOsAndHypervisorMappings(5, "SUSE Linux Enterprise Server 12 SP4 (64-bit)", mappings);
        mappings.clear();

        // Add support for Scientific Linux 7 for Xenserver 8.1.0
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "Scientific Linux 7"));
        guestOsMapper.addGuestOsAndHypervisorMappings (9, "Scientific Linux 7", mappings);
        mappings.clear();

        // Add support for NeoKylin Linux Server 7 for Xenserver 8.1.0
        guestOsMapper.addGuestOsAndHypervisorMappings(9, "NeoKylin Linux Server 7", mappings); //334
        mappings.clear();

        // Pass Guest OS Ids to update pre-4.14 mappings
        // Add support CentOS 8 for Xenserver 8.1.0
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "CentOS 8"), 297);

        // Add support for Debian Buster 10 for Xenserver 8.1.0
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "Debian Buster 10"), 292);
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "Debian Buster 10"), 293);

        // Add support for SUSE Linux Enterprise 15 (64-bit) for Xenserver 8.1.0
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("Xenserver", "8.1.0", "SUSE Linux Enterprise 15 (64-bit)"), 291);

        // Add support for Ubuntu Focal Fossa 20.04 for Xenserver 8.2.0
        mappings.add(new GuestOSHypervisorMapping("Xenserver", "8.2.0", "Ubuntu Focal Fossa 20.04"));
        guestOsMapper.addGuestOsAndHypervisorMappings(10, "Ubuntu 20.04 LTS", mappings);
        mappings.clear();

        // Add support for darwin19_64Guest from VMware 7.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "darwin19_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 10.15 (64 bit)", mappings);
        mappings.clear();

        // Add support for debian11_64Guest from VMware 7.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "debian11_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Debian GNU/Linux 11 (64-bit)", mappings);
        mappings.clear();

        // Add support for debian11Guest from VMware 7.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0", "debian11Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Debian GNU/Linux 11 (32-bit)", mappings);
        mappings.clear();

        // Add support for windows2019srv_64Guest from VMware 7.0
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "7.0", "windows2019srv_64Guest"), 276);

        // Add support for amazonlinux3_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "amazonlinux3_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Amazon Linux 3 (64 bit)", mappings);
        mappings.clear();

        // Add support for asianux9_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "asianux9_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 9 (64 bit)", mappings);
        mappings.clear();

        // Add support for centos9_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "centos9_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(1, "CentOS 9", mappings);
        mappings.clear();

        // Add support for darwin20_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "darwin20_64Guest"));
        // Add support for darwin21_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "darwin21_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 11 (64 bit)", mappings);
        mappings.clear();

        // Add support for freebsd13_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "freebsd13_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(9, "FreeBSD 13 (64-bit)", mappings);
        mappings.clear();

        // Add support for freebsd13Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "freebsd13Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(9, "FreeBSD 13 (32-bit)", mappings);
        mappings.clear();

        // Add support for oracleLinux9_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "oracleLinux9_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(3, "Oracle Linux 9", mappings);
        mappings.clear();

        // Add support for other5xLinux64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "other5xLinux64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Linux 5.x Kernel (64-bit)", mappings);
        mappings.clear();

        // Add support for other5xLinuxGuest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "other5xLinuxGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Linux 5.x Kernel (32-bit)", mappings);
        mappings.clear();

        // Add support for rhel9_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "rhel9_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(4, "Red Hat Enterprise Linux 9.0", mappings);
        mappings.clear();

        // Add support for sles16_64Guest from VMware 7.0.1.0
        mappings.add(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "sles16_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(5, "SUSE Linux Enterprise Server 16 (64-bit)", mappings);
        mappings.clear();

        // Add support for windows2019srvNext_64Guest from VMware 7.0.1.0 - Pass Guest OS Ids to update pre-4.14 mappings
        guestOsMapper.addGuestOsHypervisorMapping(new GuestOSHypervisorMapping("VMware", "7.0.1.0", "windows2019srvNext_64Guest"), 276);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41500to41510-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
