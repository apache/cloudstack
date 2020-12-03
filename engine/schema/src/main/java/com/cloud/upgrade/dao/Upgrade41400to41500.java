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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import com.cloud.hypervisor.Hypervisor;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41400to41500 implements DbUpgrade {

    final static Logger LOG = Logger.getLogger(Upgrade41400to41500.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.14.0.0", "4.15.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.15.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41400to41500.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateSystemVmTemplates(conn);
        addRolePermissionsForNewReadOnlyAndSupportRoles(conn);
    }

    @SuppressWarnings("serial")
    private void updateSystemVmTemplates(final Connection conn) {
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
                put(Hypervisor.HypervisorType.KVM, "systemvm-kvm-4.15.0");
                put(Hypervisor.HypervisorType.VMware, "systemvm-vmware-4.15.0");
                put(Hypervisor.HypervisorType.XenServer, "systemvm-xenserver-4.15.0");
                put(Hypervisor.HypervisorType.Hyperv, "systemvm-hyperv-4.15.0");
                put(Hypervisor.HypervisorType.LXC, "systemvm-lxc-4.15.0");
                put(Hypervisor.HypervisorType.Ovm3, "systemvm-ovm3-4.15.0");
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
                put(Hypervisor.HypervisorType.KVM, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.0-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.VMware, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.0-vmware.ova");
                put(Hypervisor.HypervisorType.XenServer, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.0-xen.vhd.bz2");
                put(Hypervisor.HypervisorType.Hyperv, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.0-hyperv.vhd.zip");
                put(Hypervisor.HypervisorType.LXC, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.0-kvm.qcow2.bz2");
                put(Hypervisor.HypervisorType.Ovm3, "https://download.cloudstack.org/systemvm/4.15/systemvmtemplate-4.15.0-ovm.raw.bz2");
            }
        };

        final Map<Hypervisor.HypervisorType, String> newTemplateChecksum = new HashMap<Hypervisor.HypervisorType, String>() {
            {
                put(Hypervisor.HypervisorType.KVM, "81b3e48bb934784a13555a43c5ef5ffb");
                put(Hypervisor.HypervisorType.XenServer, "1b178a5dbdbe090555515340144c6017");
                put(Hypervisor.HypervisorType.VMware, "e6a88e518c57d6f36c096c4204c3417f");
                put(Hypervisor.HypervisorType.Hyperv, "5c94da45337cf3e1910dcbe084d4b9ad");
                put(Hypervisor.HypervisorType.LXC, "81b3e48bb934784a13555a43c5ef5ffb");
                put(Hypervisor.HypervisorType.Ovm3, "875c5c65455fc06c4a012394410db375");
            }
        };

        for (final Map.Entry<Hypervisor.HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()) {
            LOG.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
            try (PreparedStatement pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1")) {
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
                        update_pstmt.setString(1, "4.15.0");
                        update_pstmt.setString(2, "minreq.sysvmtemplate.version");
                        update_pstmt.executeUpdate();
                    } catch (final SQLException e) {
                        LOG.error("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.15.0: " + e.getMessage());
                        throw new CloudRuntimeException("updateSystemVmTemplates:Exception while setting 'minreq.sysvmtemplate.version' to 4.15.0", e);
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

    private void addRolePermissionsForNewReadOnlyAndSupportRoles(final Connection conn) {
        addRolePermissionsForReadOnlyAdmin(conn);
        addRolePermissionsForReadOnlyUser(conn);
        addRolePermissionsForSupportAdmin(conn);
        addRolePermissionsForSupportUser(conn);
    }

    private void addRolePermissionsForReadOnlyAdmin(final Connection conn) {
        LOG.debug("Adding role permissions for new read-only admin role");
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only Admin - Default' AND is_default = 1");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long readOnlyAdminRoleId = rs.getLong(1);
                int readOnlyAdminSortOrder = 0;
                Map<String, String> readOnlyAdminRules = new LinkedHashMap<>();
                readOnlyAdminRules.put("list*", "ALLOW");
                readOnlyAdminRules.put("getUploadParamsFor*", "DENY");
                readOnlyAdminRules.put("get*", "ALLOW");
                readOnlyAdminRules.put("cloudianIsEnabled", "ALLOW");
                readOnlyAdminRules.put("queryAsyncJobResult", "ALLOW");
                readOnlyAdminRules.put("quotaIsEnabled", "ALLOW");
                readOnlyAdminRules.put("quotaTariffList", "ALLOW");
                readOnlyAdminRules.put("quotaSummary", "ALLOW");
                readOnlyAdminRules.put("*", "DENY");

                for (Map.Entry<String, String> readOnlyAdminRule : readOnlyAdminRules.entrySet()) {
                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, ?, ?, ?) ON DUPLICATE KEY UPDATE rule=rule");
                    pstmt.setLong(1, readOnlyAdminRoleId);
                    pstmt.setString(2, readOnlyAdminRule.getKey());
                    pstmt.setString(3, readOnlyAdminRule.getValue());
                    pstmt.setLong(4, readOnlyAdminSortOrder++);
                    pstmt.executeUpdate();
                }
            }

            if (rs != null && !rs.isClosed())  {
                rs.close();
            }
            if (pstmt != null && !pstmt.isClosed())  {
                pstmt.close();
            }
            LOG.debug("Successfully added role permissions for new read-only admin role");
        } catch (final SQLException e) {
            LOG.error("Exception while adding role permissions for read-only admin role: " + e.getMessage());
            throw new CloudRuntimeException("Exception while adding role permissions for read-only admin role: " + e.getMessage(), e);
        }
    }

    private void addRolePermissionsForReadOnlyUser(final Connection conn) {
        LOG.debug("Adding role permissions for new read-only user role");
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only User - Default' AND is_default = 1");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long readOnlyUserRoleId = rs.getLong(1);
                int readOnlyUserSortOrder = 0;

                pstmt = conn.prepareStatement("SELECT rule FROM `cloud`.`role_permissions` WHERE role_id = 4 AND permission = 'ALLOW' AND rule LIKE 'list%' ORDER BY sort_order");
                ResultSet rsRolePermissions = pstmt.executeQuery();

                while (rsRolePermissions.next()) {
                    String rule = rsRolePermissions.getString(1);
                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, ?, 'ALLOW', ?) ON DUPLICATE KEY UPDATE rule=rule");
                    pstmt.setLong(1, readOnlyUserRoleId);
                    pstmt.setString(2, rule);
                    pstmt.setLong(3, readOnlyUserSortOrder++);
                    pstmt.executeUpdate();
                }

                pstmt = conn.prepareStatement("SELECT rule FROM `cloud`.`role_permissions` WHERE role_id = 4 AND permission = 'ALLOW' AND rule LIKE 'get%' AND rule NOT LIKE 'getUploadParamsFor%' ORDER BY sort_order");
                rsRolePermissions = pstmt.executeQuery();

                while (rsRolePermissions.next()) {
                    String rule = rsRolePermissions.getString(1);
                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, ?, 'ALLOW', ?) ON DUPLICATE KEY UPDATE rule=rule");
                    pstmt.setLong(1, readOnlyUserRoleId);
                    pstmt.setString(2, rule);
                    pstmt.setLong(3, readOnlyUserSortOrder++);
                    pstmt.executeUpdate();
                }

                List<String> readOnlyUserRulesAllowed = new ArrayList<>();
                readOnlyUserRulesAllowed.add("cloudianIsEnabled");
                readOnlyUserRulesAllowed.add("queryAsyncJobResult");
                readOnlyUserRulesAllowed.add("quotaIsEnabled");
                readOnlyUserRulesAllowed.add("quotaTariffList");
                readOnlyUserRulesAllowed.add("quotaSummary");

                for(String readOnlyUserRule : readOnlyUserRulesAllowed) {
                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, ?, 'ALLOW', ?) ON DUPLICATE KEY UPDATE rule=rule");
                    pstmt.setLong(1, readOnlyUserRoleId);
                    pstmt.setString(2, readOnlyUserRule);
                    pstmt.setLong(3, readOnlyUserSortOrder++);
                    pstmt.executeUpdate();
                }

                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, '*', 'DENY', ?) ON DUPLICATE KEY UPDATE rule=rule");
                pstmt.setLong(1, readOnlyUserRoleId);
                pstmt.setLong(2, readOnlyUserSortOrder);
                pstmt.executeUpdate();

                if (rsRolePermissions != null && !rsRolePermissions.isClosed())  {
                    rsRolePermissions.close();
                }
            }

            if (rs != null && !rs.isClosed())  {
                rs.close();
            }
            if (pstmt != null && !pstmt.isClosed())  {
                pstmt.close();
            }
            LOG.debug("Successfully added role permissions for new read-only user role");
        } catch (final SQLException e) {
            LOG.error("Exception while adding role permissions for read-only user role: " + e.getMessage());
            throw new CloudRuntimeException("Exception while adding role permissions for read-only user role: " + e.getMessage(), e);
        }
    }

    private void addRolePermissionsForSupportAdmin(final Connection conn) {
        LOG.debug("Adding role permissions for new support admin role");
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`roles` WHERE name = 'Support Admin - Default' AND is_default = 1");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long supportAdminRoleId = rs.getLong(1);
                int supportAdminSortOrder = 0;

                pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only Admin - Default' AND is_default = 1");
                ResultSet rsReadOnlyAdmin = pstmt.executeQuery();
                if (rsReadOnlyAdmin.next()) {
                    long readOnlyAdminRoleId = rsReadOnlyAdmin.getLong(1);
                    pstmt = conn.prepareStatement("SELECT rule FROM `cloud`.`role_permissions` WHERE role_id = ? AND permission = 'ALLOW' ORDER BY sort_order");
                    pstmt.setLong(1, readOnlyAdminRoleId);
                    ResultSet rsRolePermissions = pstmt.executeQuery();

                    while (rsRolePermissions.next()) {
                        String rule = rsRolePermissions.getString(1);
                        pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, ?, 'ALLOW', ?) ON DUPLICATE KEY UPDATE rule=rule");
                        pstmt.setLong(1, supportAdminRoleId);
                        pstmt.setString(2, rule);
                        pstmt.setLong(3, supportAdminSortOrder++);
                        pstmt.executeUpdate();
                    }

                    List<String> supportAdminRulesAllowed = new ArrayList<>();
                    supportAdminRulesAllowed.add("prepareHostForMaintenance");
                    supportAdminRulesAllowed.add("cancelHostMaintenance");
                    supportAdminRulesAllowed.add("enableStorageMaintenance");
                    supportAdminRulesAllowed.add("cancelStorageMaintenance");
                    supportAdminRulesAllowed.add("createServiceOffering");
                    supportAdminRulesAllowed.add("createDiskOffering");
                    supportAdminRulesAllowed.add("createNetworkOffering");
                    supportAdminRulesAllowed.add("createVPCOffering");
                    supportAdminRulesAllowed.add("startVirtualMachine");
                    supportAdminRulesAllowed.add("stopVirtualMachine");
                    supportAdminRulesAllowed.add("rebootVirtualMachine");
                    supportAdminRulesAllowed.add("startKubernetesCluster");
                    supportAdminRulesAllowed.add("stopKubernetesCluster");
                    supportAdminRulesAllowed.add("createVolume");
                    supportAdminRulesAllowed.add("attachVolume");
                    supportAdminRulesAllowed.add("detachVolume");
                    supportAdminRulesAllowed.add("uploadVolume");
                    supportAdminRulesAllowed.add("attachIso");
                    supportAdminRulesAllowed.add("detachIso");
                    supportAdminRulesAllowed.add("registerTemplate");
                    supportAdminRulesAllowed.add("registerIso");

                    for(String supportAdminRule : supportAdminRulesAllowed) {
                        pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, ?, 'ALLOW', ?) ON DUPLICATE KEY UPDATE rule=rule");
                        pstmt.setLong(1, supportAdminRoleId);
                        pstmt.setString(2, supportAdminRule);
                        pstmt.setLong(3, supportAdminSortOrder++);
                        pstmt.executeUpdate();
                    }

                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, '*', 'DENY', ?) ON DUPLICATE KEY UPDATE rule=rule");
                    pstmt.setLong(1, supportAdminRoleId);
                    pstmt.setLong(2, supportAdminSortOrder);
                    pstmt.executeUpdate();

                    if (rsRolePermissions != null && !rsRolePermissions.isClosed())  {
                        rsRolePermissions.close();
                    }
                }

                if (rsReadOnlyAdmin != null && !rsReadOnlyAdmin.isClosed())  {
                    rsReadOnlyAdmin.close();
                }
            }

            if (rs != null && !rs.isClosed())  {
                rs.close();
            }
            if (pstmt != null && !pstmt.isClosed())  {
                pstmt.close();
            }
            LOG.debug("Successfully added role permissions for new support admin role");
        } catch (final SQLException e) {
            LOG.error("Exception while adding role permissions for support admin role: " + e.getMessage());
            throw new CloudRuntimeException("Exception while adding role permissions for support admin role: " + e.getMessage(), e);
        }
    }

    private void addRolePermissionsForSupportUser(final Connection conn) {
        LOG.debug("Adding role permissions for new support user role");
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`roles` WHERE name = 'Support User - Default' AND is_default = 1");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long supportUserRoleId = rs.getLong(1);
                int supportUserSortOrder = 0;

                pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`roles` WHERE name = 'Read-Only User - Default' AND is_default = 1");
                ResultSet rsReadOnlyUser = pstmt.executeQuery();
                if (rsReadOnlyUser.next()) {
                    long readOnlyUserRoleId = rsReadOnlyUser.getLong(1);
                    pstmt = conn.prepareStatement("SELECT rule FROM `cloud`.`role_permissions` WHERE role_id = ? AND permission = 'ALLOW' ORDER BY sort_order");
                    pstmt.setLong(1, readOnlyUserRoleId);
                    ResultSet rsRolePermissions = pstmt.executeQuery();
                    while (rsRolePermissions.next()) {
                        String rule = rsRolePermissions.getString(1);
                        pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, ?, 'ALLOW', ?) ON DUPLICATE KEY UPDATE rule=rule");
                        pstmt.setLong(1, supportUserRoleId);
                        pstmt.setString(2, rule);
                        pstmt.setLong(3, supportUserSortOrder++);
                        pstmt.executeUpdate();
                    }

                    List<String> supportUserRulesAllowed = new ArrayList<>();
                    supportUserRulesAllowed.add("startVirtualMachine");
                    supportUserRulesAllowed.add("stopVirtualMachine");
                    supportUserRulesAllowed.add("rebootVirtualMachine");
                    supportUserRulesAllowed.add("startKubernetesCluster");
                    supportUserRulesAllowed.add("stopKubernetesCluster");
                    supportUserRulesAllowed.add("createVolume");
                    supportUserRulesAllowed.add("attachVolume");
                    supportUserRulesAllowed.add("detachVolume");
                    supportUserRulesAllowed.add("uploadVolume");
                    supportUserRulesAllowed.add("attachIso");
                    supportUserRulesAllowed.add("detachIso");
                    supportUserRulesAllowed.add("registerTemplate");
                    supportUserRulesAllowed.add("registerIso");
                    supportUserRulesAllowed.add("getUploadParamsFor*");

                    for(String supportUserRule : supportUserRulesAllowed) {
                        pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, ?, 'ALLOW', ?) ON DUPLICATE KEY UPDATE rule=rule");
                        pstmt.setLong(1, supportUserRoleId);
                        pstmt.setString(2, supportUserRule);
                        pstmt.setLong(3, supportUserSortOrder++);
                        pstmt.executeUpdate();
                    }

                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`, `sort_order`) VALUES (UUID(), ?, '*', 'DENY', ?) ON DUPLICATE KEY UPDATE rule=rule");
                    pstmt.setLong(1, supportUserRoleId);
                    pstmt.setLong(2, supportUserSortOrder);
                    pstmt.executeUpdate();

                    if (rsRolePermissions != null && !rsRolePermissions.isClosed())  {
                        rsRolePermissions.close();
                    }
                }

                if (rsReadOnlyUser != null && !rsReadOnlyUser.isClosed())  {
                    rsReadOnlyUser.close();
                }
            }

            if (rs != null && !rs.isClosed())  {
                rs.close();
            }
            if (pstmt != null && !pstmt.isClosed())  {
                pstmt.close();
            }
            LOG.debug("Successfully added role permissions for new support user role");
        } catch (final SQLException e) {
            LOG.error("Exception while adding role permissions for support user role: " + e.getMessage());
            throw new CloudRuntimeException("Exception while adding role permissions for support user role: " + e.getMessage(), e);
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41400to41500-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
