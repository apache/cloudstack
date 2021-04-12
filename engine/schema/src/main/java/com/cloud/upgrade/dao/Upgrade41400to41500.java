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
        addRolePermissionsForNewReadOnlyAndSupportRoles(conn);
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
