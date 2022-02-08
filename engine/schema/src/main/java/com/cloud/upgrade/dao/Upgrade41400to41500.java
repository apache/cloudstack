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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.cloud.storage.GuestOSHypervisorMapping;
import com.cloud.upgrade.GuestOsMapper;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41400to41500 implements DbUpgrade {

    final static Logger LOG = Logger.getLogger(Upgrade41400to41500.class);
    private GuestOsMapper guestOsMapper = new GuestOsMapper();

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
        addRolePermissionsForNewReadOnlyAndSupportRoles(conn);
        updateGuestOsMappings(conn);
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

    private void updateGuestOsMappings(final Connection conn) {
        LOG.debug("Updating guest OS mappings");

        // The below existing Guest OS Ids must be used for updating the guest OS hypervisor mappings
        // CentOS - 1, Debian - 2, Oracle - 3, RedHat - 4, SUSE - 5, Windows - 6, Other - 7, Novel - 8, Unix - 9, Ubuntu - 10, None - 11

        // OVF configured OS while registering deploy-as-is templates Linux 3.x Kernel OS
        guestOsMapper.addGuestOsAndHypervisorMappings(11, "OVF Configured OS", null);

        List<GuestOSHypervisorMapping> mappings = new ArrayList<GuestOSHypervisorMapping>();
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "other3xLinux64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "other3xLinux64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Linux 3.x Kernel (64 bit)", mappings);
        mappings.clear();

        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "other3xLinuxGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "other3xLinuxGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(2, "Linux 3.x Kernel (32 bit)", mappings);
        mappings.clear();

        // Add Amazonlinux as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "amazonlinux2_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "amazonlinux2_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "amazonlinux2_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Amazon Linux 2 (64 bit)", mappings);
        mappings.clear();

        // Add asianux4 32 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux4Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux4Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 4 (32 bit)", mappings);
        mappings.clear();

        // Add asianux4 64 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux4_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux4_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 4 (64 bit)", mappings);
        mappings.clear();

        // Add asianux5 32 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux5Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 5 (32 bit)", mappings);
        mappings.clear();

        // Add asianux5 64 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux5_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux5_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 5 (64 bit)", mappings);
        mappings.clear();

        // Add asianux7 32 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux7Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux7Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 7 (32 bit)", mappings);
        mappings.clear();

        // Add asianux7 64 as support guest os, and  VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux7_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux7_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 7 (64 bit)", mappings);
        mappings.clear();

        // Add asianux8 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "asianux8_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "asianux8_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Asianux Server 8 (64 bit)", mappings);
        mappings.clear();

        // Add eComStation 2.0 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "eComStation2Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "eComStation2Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "eComStation 2.0", mappings);
        mappings.clear();

        // Add macOS 10.13 (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "darwin17_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "darwin17_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 10.13 (64 bit)", mappings);
        mappings.clear();

        // Add macOS 10.14 (64 bit) as support guest os, and VMWare guest os mapping
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "darwin18_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "darwin18_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "macOS 10.14 (64 bit)", mappings);
        mappings.clear();

        // Add Fedora Linux (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "fedora64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "fedora64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Fedora Linux (64 bit)", mappings);
        mappings.clear();

        // Add Fedora Linux as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "fedoraGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "fedoraGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Fedora Linux", mappings);
        mappings.clear();

        // Add Mandrake Linux as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "mandrakeGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "mandrakeGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Mandrake Linux", mappings);
        mappings.clear();

        // Add Mandriva Linux (64 bit)  as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "mandriva64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "mandriva64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Mandriva Linux (64 bit)", mappings);
        mappings.clear();

        // Add Mandriva Linux  as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "mandrivaGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "mandrivaGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Mandriva Linux", mappings);
        mappings.clear();

        // Add SCO OpenServer 5   as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "openServer5Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "openServer5Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "SCO OpenServer 5", mappings);
        mappings.clear();

        // Add SCO OpenServer 6 as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "openServer6Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "openServer6Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "SCO OpenServer 6", mappings);
        mappings.clear();

        // Add OpenSUSE Linux (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "opensuse64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "opensuse64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "OpenSUSE Linux (64 bit)", mappings);
        mappings.clear();

        // Add OpenSUSE Linux (32 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "opensuseGuest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "opensuseGuest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "OpenSUSE Linux (32 bit)", mappings);
        mappings.clear();

        // Add Solaris 11 (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.0", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "solaris11_64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "solaris11_64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "Solaris 11 (64 bit)", mappings);
        mappings.clear();

        // Add  VMware Photon (64 bit) as support guest os, and VMWare guest os mappings
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.5", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.1", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.2", "vmwarePhoton64Guest"));
        mappings.add(new GuestOSHypervisorMapping("VMware", "6.7.3", "vmwarePhoton64Guest"));
        guestOsMapper.addGuestOsAndHypervisorMappings(7, "VMware Photon (64 bit)", mappings);
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
