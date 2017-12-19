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
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Upgrade4930to41000 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade4930to41000.class);

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

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.9.3.0", "4.10.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.10.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-4930to41000.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateSystemVmTemplates(conn);
        populateGuestOsDetails(conn);
        updateSourceCidrs(conn);
    }

    @SuppressWarnings("serial")
    private void updateSystemVmTemplates(final Connection conn) {
        s_logger.debug("Updating System Vm template IDs");
        // Get all hypervisors in use
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
                    default: // no action on cases Any, BareMetal, None, Ovm,
                        // Parralels, Simulator and VirtualBox:
                        break;
                }
            }
        } catch (final SQLException e) {
            s_logger.error("updateSystemVmTemplates:Exception while getting hypervisor types from clusters: " + e.getMessage());
            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting hypervisor types from clusters", e);
        }

        final Map<Hypervisor.HypervisorType, String> NewTemplateNameList = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.10");
                put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.10");
                put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.10");
                put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.10");
                put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.10");
                put(Hypervisor.HypervisorType.Ovm3, "systemvm-ovm3-4.10");
            }
        };

        final Map<Hypervisor.HypervisorType, String> routerTemplateConfigurationNames = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.XenServer, "router.template.xenserver");
                put(Hypervisor.HypervisorType.VMware, "router.template.vmware");
                put(Hypervisor.HypervisorType.KVM, "router.template.kvm");
                put(Hypervisor.HypervisorType.Hyperv, "router.template.hyperv");
                put(Hypervisor.HypervisorType.LXC, "router.template.lxc");
                put(Hypervisor.HypervisorType.Ovm3, "router.template.ovm3");
            }
        };

        final Map<Hypervisor.HypervisorType, String> newTemplateUrl = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.XenServer, "https://download.cloudstack.org/systemvm/4.10/systemvm64template-master-4.10.0-xen.vhd.bz2");
                put(Hypervisor.HypervisorType.VMware, "https://download.cloudstack.org/systemvm/4.10/systemvm64template-master-4.10.0-vmware.ova");
                put(Hypervisor.HypervisorType.KVM, "https://download.cloudstack.org/systemvm/4.10/systemvm64template-master-4.10.0-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.Hyperv, "https://download.cloudstack.org/systemvm/4.10/systemvm64template-master-4.10.0-hyperv.vhd.zip");
                put(Hypervisor.HypervisorType.LXC, "https://download.cloudstack.org/systemvm/4.10/systemvm64template-master-4.10.0-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.Ovm3, "https://download.cloudstack.org/systemvm/4.10/systemvm64template-master-4.10.0-ovm.raw.bz2");
            }
        };

        final Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.XenServer, "908c28a8d4c232f960e0f84af7f86c80");
                put(Hypervisor.HypervisorType.VMware, "970bfb070a80bd74820881d8149643c1");
                put(Hypervisor.HypervisorType.KVM, "bc2eac46f16a2ece6c19d4b89db41de3");
                put(Hypervisor.HypervisorType.Hyperv, "0adb35bd9f92e80d3fc63fcdd9bb55e5");
                put(Hypervisor.HypervisorType.LXC, "bc2eac46f16a2ece6c19d4b89db41de3");
                put(Hypervisor.HypervisorType.Ovm3, "94a41f0a5361933813bb34a51df56f56");
            }
        };

        for (final Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()) {
            s_logger.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
            try (PreparedStatement pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1")) {
                // Get 4.10.0 system Vm template Id for corresponding hypervisor
                long templateId = -1;
                pstmt.setString(1, hypervisorAndTemplateName.getValue());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        templateId = rs.getLong(1);
                    }
                } catch (final SQLException e) {
                    s_logger.error("updateSystemVmTemplates:Exception while getting ids of templates: " + e.getMessage());
                    throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting ids of templates", e);
                }

                // change template type to SYSTEM
                if (templateId != -1) {
                    try (PreparedStatement templ_type_pstmt = conn.prepareStatement("update `cloud`.`vm_template` set type='SYSTEM' where id = ?");) {
                        templ_type_pstmt.setLong(1, templateId);
                        templ_type_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        s_logger.error("updateSystemVmTemplates:Exception while updating template with id " + templateId + " to be marked as 'system': " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while updating template with id " + templateId + " to be marked as 'system'", e);
                    }
                    // update template ID of system Vms
                    try (PreparedStatement update_templ_id_pstmt = conn
                            .prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ?");) {
                        update_templ_id_pstmt.setLong(1, templateId);
                        update_templ_id_pstmt.setString(2, hypervisorAndTemplateName.getKey().toString());
                        update_templ_id_pstmt.executeUpdate();
                    } catch (final Exception e) {
                        s_logger.error("updateSystemVmTemplates:Exception while setting template for " + hypervisorAndTemplateName.getKey().toString() + " to " + templateId
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
                        s_logger.error("updateSystemVmTemplates:Exception while setting " + routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()) + " to "
                                + hypervisorAndTemplateName.getValue() + ": " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting "
                                + routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()) + " to " + hypervisorAndTemplateName.getValue(), e);
                    }

                    // Change value of global configuration parameter
                    // minreq.sysvmtemplate.version for the ACS version
                    try (PreparedStatement update_pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?");) {
                        update_pstmt.setString(1, "4.10.0");
                        update_pstmt.setString(2, "minreq.sysvmtemplate.version");
                        update_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        s_logger.error("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.10.0: " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.10.0", e);
                    }
                } else {
                    if (hypervisorsListInUse.contains(hypervisorAndTemplateName.getKey())) {
                        throw new CloudRuntimeException(getUpgradedVersion() + hypervisorAndTemplateName.getKey() + " SystemVm template not found. Cannot upgrade system Vms");
                    } else {
                        s_logger.warn(getUpgradedVersion() + hypervisorAndTemplateName.getKey() + " SystemVm template not found. " + hypervisorAndTemplateName.getKey()
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
                            s_logger.error("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type "
                                    + hypervisorAndTemplateName.getKey().toString() + ": " + e.getMessage());
                            throw new CloudRuntimeException("updateSystemVmTemplates:Exception while updating 'url' and 'checksum' for hypervisor type "
                                    + hypervisorAndTemplateName.getKey().toString(), e);
                        }
                    }
                }
            } catch (final SQLException e) {
                s_logger.error("updateSystemVmTemplates:Exception while getting ids of templates: " + e.getMessage());
                throw new CloudRuntimeException("updateSystemVmTemplates:Exception while getting ids of templates", e);
            }
        }
        s_logger.debug("Updating System Vm Template IDs Complete");
    }


    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-4930to41000-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    private void populateGuestOsDetails(Connection conn){
        final HashMap<String, MemoryValues> xenServerGuestOsMemoryMap = new HashMap<String, MemoryValues>(70);

        xenServerGuestOsMemoryMap.put("CentOS 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.8 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.8 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.9 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.9 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.10 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 5.10 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 6.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 7", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 7.1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 7.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CentOS 7.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.8 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.8 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.9 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.9 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.10 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 5.10 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Oracle Enterprise Linux 6.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 4.5 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 4.6 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 4.7 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 4.8 (32-bit)", new MemoryValues(256l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.0 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.1 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.2 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.3 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.4 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.5 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.6 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.7 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.8 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.8 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.9 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.9 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.10 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 5.10 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.0 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (32-bit)", new MemoryValues(512l, 8 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.1 (64-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.5 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.5 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.6 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.6 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.7 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 6.7 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 7", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 7.1", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Red Hat Enterprise Linux 7.2", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Debian GNU/Linux 5.0 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Debian GNU/Linux 5(64-bit)", new MemoryValues(128l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Debian GNU/Linux 6(32-bit)", new MemoryValues(128l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Debian GNU/Linux 6(64-bit)", new MemoryValues(128l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Debian GNU/Linux 7(32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Debian GNU/Linux 7(64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Debian GNU/Linux 8(32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Debian GNU/Linux 8(64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10(32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10(64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 10 SP4 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP2 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP3 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 11 SP3 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 12 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("SUSE Linux Enterprise Server 12 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows 7 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows 7 (64-bit)", new MemoryValues(2 * 1024l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows 8 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows 8 (64-bit)", new MemoryValues(2 * 1024l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(32-bit)", new MemoryValues(256l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2003 Enterprise Edition(64-bit)", new MemoryValues(256l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2003 SP2 (32-bit)", new MemoryValues(256l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2003 SP2 (64-bit)", new MemoryValues(256l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2008 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2008 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2008 SP2 (32-bit)", new MemoryValues(512l, 64 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2008 SP2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2008 R2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2008 R2 SP1 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2012 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Server 2012 R2 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Vista (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Vista (64-bit)", new MemoryValues(1024l, 4 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows Vista SP2 (32-bit)", new MemoryValues(1024l, 4 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows XP (32-bit)", new MemoryValues(256l, 4 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows XP (64-bit)", new MemoryValues(256l, 4 * 1024l));
        xenServerGuestOsMemoryMap.put("Windows XP SP3 (32-bit)", new MemoryValues(256l, 4 * 1024l));
        xenServerGuestOsMemoryMap.put("Ubuntu 10.04 (32-bit)", new MemoryValues(128l, 512l));
        xenServerGuestOsMemoryMap.put("Ubuntu 10.04 (64-bit)", new MemoryValues(128l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Ubuntu 10.10 (32-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Ubuntu 10.10 (64-bit)", new MemoryValues(512l, 16 * 1024l));
        xenServerGuestOsMemoryMap.put("Ubuntu 12.04 (32-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Ubuntu 12.04 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Ubuntu 14.04 (32-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Ubuntu 14.04 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("Ubuntu 16.04 (32-bit)", new MemoryValues(512l, 32 * 1024l));
        xenServerGuestOsMemoryMap.put("Ubuntu 16.04 (64-bit)", new MemoryValues(512l, 128 * 1024l));
        xenServerGuestOsMemoryMap.put("CoreOS", new MemoryValues(512l, 128 * 1024l));

        final String insertDynamicMemoryVal = "insert into guest_os_details(guest_os_id, name, value, display) select id,?, ?, 0 from guest_os where display_name = ?";


        PreparedStatement ps = null;
        try{
            ps = conn.prepareStatement(insertDynamicMemoryVal);

            for (String key: xenServerGuestOsMemoryMap.keySet()){
                ps.setString(1,"xenserver.dynamicMin");
                ps.setString(2,String.valueOf(xenServerGuestOsMemoryMap.get(key).getMin()));
                ps.setString(3, key);
                ps.executeUpdate();

                ps.setString(1,"xenserver.dynamicMax");
                ps.setString(2,String.valueOf(xenServerGuestOsMemoryMap.get(key).getMax()));
                ps.setString(3, key);
                ps.executeUpdate();
            }


        }catch(SQLException e){
            throw new CloudRuntimeException("Unable to update guestOs details", e);
        }finally {
            try {
                if (ps != null && !ps.isClosed())  {
                    ps.close();
                }
            } catch (SQLException e) {
            }
        }

    }

    private void updateSourceCidrs(Connection conn){
        //with ipset the value for source cidr 0.0.0.0/0 can't be added in ipset. So changing it to network cidr.
        try(PreparedStatement pstmt = conn.prepareStatement("UPDATE `cloud`.`firewall_rules_cidrs` AS s, (SELECT IFNULL(networks.network_cidr,networks.cidr) cidr," +
                "`firewall_rules_cidrs`.`id`, `firewall_rules`.`traffic_type` "+
                "FROM `cloud`.`networks`, `cloud`.`firewall_rules`,`cloud`.`firewall_rules_cidrs` WHERE `cloud`.`networks`.`id`=`cloud`.`firewall_rules`.`network_id` " +
                "AND `cloud`.`firewall_rules`.`id` = `cloud`.`firewall_rules_cidrs`.`firewall_rule_id`) AS p " +
                "SET `s`.`source_cidr` = `p`.`cidr` WHERE `s`.`source_cidr`=\"0.0.0.0/0\" AND `s`.`id`=`p`.`id` AND `p`.`traffic_type`=\"Egress\" ;")){
            pstmt.execute();
        }catch (SQLException e) {
            throw new CloudRuntimeException("updateSourceCidrs:Exception:" + e.getMessage(), e);
        }
    }
}
