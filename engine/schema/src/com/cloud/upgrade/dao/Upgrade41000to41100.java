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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41000to41100 implements DbUpgrade {

    final static Logger LOG = Logger.getLogger(Upgrade41000to41100.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.10.0.0", "4.11.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.11.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41000to41100.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        validateUserDataInBase64(conn);
        updateSystemVmTemplates(conn);
    }

    private void validateUserDataInBase64(Connection conn) {
        try (final PreparedStatement selectStatement = conn.prepareStatement("SELECT `id`, `user_data` FROM `cloud`.`user_vm` WHERE `user_data` IS NOT NULL;");
             final ResultSet selectResultSet = selectStatement.executeQuery()) {
            while (selectResultSet.next()) {
                final Long userVmId = selectResultSet.getLong(1);
                final String userData = selectResultSet.getString(2);
                if (Base64.isBase64(userData)) {
                    final String newUserData = Base64.encodeBase64String(Base64.decodeBase64(userData.getBytes()));
                    if (!userData.equals(newUserData)) {
                        try (final PreparedStatement updateStatement = conn.prepareStatement("UPDATE `cloud`.`user_vm` SET `user_data` = ? WHERE `id` = ? ;")) {
                            updateStatement.setString(1, newUserData);
                            updateStatement.setLong(2, userVmId);
                            updateStatement.executeUpdate();
                        } catch (SQLException e) {
                            LOG.error("Failed to update cloud.user_vm user_data for id:" + userVmId + " with exception: " + e.getMessage());
                            throw new CloudRuntimeException("Exception while updating cloud.user_vm for id " + userVmId, e);
                        }
                    }
                } else {
                    // Update to NULL since it's invalid
                    LOG.warn("Removing user_data for vm id " + userVmId + " because it's invalid");
                    LOG.warn("Removed data was: " + userData);
                    try (final PreparedStatement updateStatement = conn.prepareStatement("UPDATE `cloud`.`user_vm` SET `user_data` = NULL WHERE `id` = ? ;")) {
                        updateStatement.setLong(1, userVmId);
                        updateStatement.executeUpdate();
                    } catch (SQLException e) {
                        LOG.error("Failed to update cloud.user_vm user_data for id:" + userVmId + " to NULL with exception: " + e.getMessage());
                        throw new CloudRuntimeException("Exception while updating cloud.user_vm for id " + userVmId + " to NULL", e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while validating existing user_vm table's user_data column to be base64 valid with padding", e);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("Done validating base64 content of user data");
        }
    }

    @SuppressWarnings("serial")
    private void updateSystemVmTemplates(final Connection conn) {
        LOG.debug("Updating System Vm template IDs");
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
                put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.11");
                put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.11");
                put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.11");
                put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.11");
                put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.11");
                put(Hypervisor.HypervisorType.Ovm3, "systemvm-ovm3-4.11");
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
                // FIXME: upload templates
                put(Hypervisor.HypervisorType.KVM, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.0-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.VMware, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.0-vmware.ova");
                put(Hypervisor.HypervisorType.XenServer, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.0-xen.vhd.bz2");
                put(Hypervisor.HypervisorType.Hyperv, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.0-hyperv.vhd.zip");
                put(Hypervisor.HypervisorType.LXC, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.0-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.Ovm3, "https://download.cloudstack.org/systemvm/4.11/systemvmtemplate-4.11.0-ovm.raw.bz2");
            }
        };

        final Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                // FIXME: update checksums?
                put(Hypervisor.HypervisorType.KVM, "bc2eac46f16a2ece6c19d4b89db41de3");
                put(Hypervisor.HypervisorType.XenServer, "908c28a8d4c232f960e0f84af7f86c80");
                put(Hypervisor.HypervisorType.VMware, "970bfb070a80bd74820881d8149643c1");
                put(Hypervisor.HypervisorType.Hyperv, "0adb35bd9f92e80d3fc63fcdd9bb55e5");
                put(Hypervisor.HypervisorType.LXC, "bc2eac46f16a2ece6c19d4b89db41de3");
                put(Hypervisor.HypervisorType.Ovm3, "94a41f0a5361933813bb34a51df56f56");
            }
        };

        for (final Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()) {
            LOG.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
            try (PreparedStatement pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1")) {
                // Get 4.11.0 systemvm template id for corresponding hypervisor
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
                            .prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ?");) {
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
                        update_pstmt.setString(1, "4.11.0");
                        update_pstmt.setString(2, "minreq.sysvmtemplate.version");
                        update_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        LOG.error("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.11.0: " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.11.0", e);
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


    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41000to41100-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
