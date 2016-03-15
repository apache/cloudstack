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

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.db.ScriptRunner;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.cloudstack.acl.RoleType;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

public class Upgrade452to453 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade452to453.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.5.2", "4.5.3"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.5.3";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-452to453.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-452to453.sql");
        }
        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        setupRolesAndPermissionsForDynamicRBAC(conn);
    }

    private void createDefaultRole(final Connection conn, final Long id, final String name, final RoleType roleType) {
        final String insertSql = String.format("INSERT INTO `cloud`.`roles` (`id`, `uuid`, `name`, `role_type`, `description`) values (%d, UUID(), '%s', '%s', 'Default %s role');",
                id, name, roleType.name(), roleType.name().toLowerCase());
        try ( PreparedStatement updatePstmt = conn.prepareStatement(insertSql) ) {
            updatePstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create default role with id: " + id + " name: " + name, e);
        }
    }

    private void createRoleMapping(final Connection conn, final Long roleId, final String apiName) {
        final String insertSql = String.format("INSERT INTO `cloud`.`role_permissions` (`uuid`, `role_id`, `rule`, `permission`) values (UUID(), %d, '%s', 'ALLOW') ON DUPLICATE KEY UPDATE rule=rule;",
                roleId, apiName);
        try ( PreparedStatement updatePstmt = conn.prepareStatement(insertSql)) {
            updatePstmt.executeUpdate();
        } catch (SQLException ignored) {
            s_logger.debug("Unable to insert mapping for role id:" + roleId + " apiName: " + apiName);
        }
    }

    private void addRoleColumnAndMigrateAccountTable(final Connection conn, final RoleType[] roleTypes) {
        // Add role_id column to account table
        final String alterTableSql = "ALTER TABLE `cloud`.`account` ADD COLUMN `role_id` bigint(20) unsigned COMMENT 'role id for this account' AFTER `type`, " +
                "ADD KEY `fk_account__role_id` (`role_id`), " +
                "ADD CONSTRAINT `fk_account__role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`);";
        try (PreparedStatement pstmt = conn.prepareStatement(alterTableSql)) {
            pstmt.executeUpdate();
            s_logger.info("Altered cloud.account table and added column role_id");
        } catch (SQLException e) {
            if (e.getMessage().contains("role_id")) {
                s_logger.warn("cloud.account table already has the role_id column, skipping altering table and migration of accounts");
                return;
            } else {
                throw new CloudRuntimeException("Unable to create column quota_calculated in table cloud_usage.cloud_usage", e);
            }
        }
        // Migrate existing account to one of default roles based on account type
        migrateAccountsToDefaultRoles(conn, roleTypes);
    }

    private void migrateAccountsToDefaultRoles(final Connection conn, final RoleType[] roleTypes) {
        // Migrate existing accounts to default roles based on account type
        try (PreparedStatement selectStatement = conn.prepareStatement("SELECT `id`, `type` FROM `cloud`.`account`;");
             ResultSet selectResultSet = selectStatement.executeQuery()) {
            while (selectResultSet.next()) {
                Long accountId = selectResultSet.getLong(1);
                Short accountType = selectResultSet.getShort(2);
                Long roleId = null;
                for (RoleType roleType : roleTypes) {
                    if (roleType.getAccountType() == accountType) {
                        roleId = roleType.getId();
                        break;
                    }
                }
                // Skip is account type does not match any of the default roles
                if (roleId == null) {
                    continue;
                }
                try (PreparedStatement updateStatement = conn.prepareStatement("UPDATE `cloud`.`account` SET role_id = ? WHERE id = ?;")) {
                    updateStatement.setLong(1, roleId);
                    updateStatement.setLong(2, accountId);
                    updateStatement.executeUpdate();
                } catch (SQLException e) {
                    s_logger.error("Failed to update cloud.account role_id for account id:" + accountId + " with exception: " + e.getMessage());
                    throw new CloudRuntimeException("Exception while updating cloud.account role_id", e);
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while migrating existing account table's role_id column to a role based on account type", e);
        }
        s_logger.debug("Done migrating existing accounts to use one of default roles based on account type");
    }

    private void setupRolesAndPermissionsForDynamicRBAC(final Connection conn) {
        // If there are existing roles, avoid resetting data
        try (PreparedStatement selectStatement = conn.prepareStatement("SELECT * FROM `cloud`.`roles`")) {
            ResultSet resultSet = selectStatement.executeQuery();
            if (resultSet != null && resultSet.next()) {
                resultSet.close();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Found existing roles. Skipping migration of commands.properties to dynamic roles table.");
                }
                return;
            }
        } catch (SQLException e) {
            s_logger.error("Unable to find existing roles, if you need to add default roles please add them manually. Giving up!");
            return;
        }

        // Add default roles
        RoleType[] roleTypes = new RoleType[] {RoleType.Admin, RoleType.ResourceAdmin, RoleType.DomainAdmin, RoleType.User};
        for (RoleType roleType: roleTypes) {
            createDefaultRole(conn, roleType.getId(), roleType.name(), roleType);
        }

        // Add role_id column to account and map existing accounts to default roles
        addRoleColumnAndMigrateAccountTable(conn, roleTypes);

        // Add default set of role-api mapping when commands.properties file is not found
        Map<String, String> apiMap = PropertiesUtil.processConfigFile(new String[] { PropertiesUtil.getDefaultApiCommandsFileName() });
        if (apiMap == null || apiMap.isEmpty()) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("The commands.properties file and default role permissions were not found. " +
                        "Assuming new installation, configuring default role-api mappings.");
            }
            String script = Script.findScript("", "db/create-default-role-api-mappings.sql");
            if (script == null) {
                s_logger.error("Unable to find default role-api mapping sql file, please configure api per role manually");
                return;
            }
            try(FileReader reader = new FileReader(new File(script));) {
                ScriptRunner runner = new ScriptRunner(conn, false, true);
                runner.runScript(reader);
            } catch (SQLException | IOException e) {
                s_logger.error("Unable to insert default api-role mappings from file: " + script + ". Please configure api per role manually, giving up!", e);
            }
        } else {
            // If commands.properties file exists, use it to create the mappings
            for (RoleType roleType : roleTypes) {
                // Allow all for root admin
                if (roleType == RoleType.Admin) {
                    createRoleMapping(conn, roleType.getId(), "*");
                    continue;
                }
                for (Map.Entry<String, String> entry : apiMap.entrySet()) {
                    String apiName = entry.getKey();
                    String roleMask = entry.getValue();
                    try {
                        short cmdPermissions = Short.parseShort(roleMask);
                        if ((cmdPermissions & roleType.getMask()) != 0) {
                            createRoleMapping(conn, roleType.getId(), apiName);
                        }
                    } catch (NumberFormatException nfe) {
                        s_logger.info("Malformed key=value pair for entry: " + entry.toString());
                    }
                }
            }
        }
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-452to453-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-452to453-cleanup.sql");
        }
        return new File[] {new File(script)};
    }
}
