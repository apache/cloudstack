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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cloud.user.Account;
import org.apache.cloudstack.acl.RoleType;

import com.cloud.utils.db.ScriptRunner;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade481to490 extends DbUpgradeAbstractImpl {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.8.1", "4.9.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.9.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-481to490.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        setupRolesAndPermissionsForDynamicChecker(conn);
    }

    private void migrateAccountsToDefaultRoles(final Connection conn) {
        try (final PreparedStatement selectStatement = conn.prepareStatement("SELECT `id`, `type` FROM `cloud`.`account`;");
             final ResultSet selectResultSet = selectStatement.executeQuery()) {
            while (selectResultSet.next()) {
                final Long accountId = selectResultSet.getLong(1);
                final Integer accountType = selectResultSet.getInt(2);
                final Long roleId = RoleType.getByAccountType(Account.Type.getFromValue(accountType)).getId();
                if (roleId < 1L || roleId > 4L) {
                    logger.warn("Skipping role ID migration due to invalid role_id resolved for account id=" + accountId);
                    continue;
                }
                try (final PreparedStatement updateStatement = conn.prepareStatement("UPDATE `cloud`.`account` SET account.role_id = ? WHERE account.id = ? ;")) {
                    updateStatement.setLong(1, roleId);
                    updateStatement.setLong(2, accountId);
                    updateStatement.executeUpdate();
                } catch (SQLException e) {
                    logger.error("Failed to update cloud.account role_id for account id:" + accountId + " with exception: " + e.getMessage());
                    throw new CloudRuntimeException("Exception while updating cloud.account role_id", e);
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while migrating existing account table's role_id column to a role based on account type", e);
        }
        logger.debug("Done migrating existing accounts to use one of default roles based on account type");
    }

    private void setupRolesAndPermissionsForDynamicChecker(final Connection conn) {
        final String alterTableSql = "ALTER TABLE `cloud`.`account` " +
                "ADD COLUMN `role_id` bigint(20) unsigned COMMENT 'role id for this account' AFTER `type`, " +
                "ADD KEY `fk_account__role_id` (`role_id`), " +
                "ADD CONSTRAINT `fk_account__role_id` FOREIGN KEY (`role_id`) REFERENCES `roles` (`id`);";
        try (final PreparedStatement pstmt = conn.prepareStatement(alterTableSql)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage().contains("role_id")) {
                logger.warn("cloud.account table already has the role_id column, skipping altering table and migration of accounts");
                return;
            } else {
                throw new CloudRuntimeException("Unable to create column role_id in table cloud.account", e);
            }
        }

        try (final PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud_usage`.`account` ADD COLUMN `role_id` bigint(20) unsigned AFTER `type`")) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create column role_id in table cloud_usage.account", e);
        }

        migrateAccountsToDefaultRoles(conn);

        if (logger.isDebugEnabled()) {
            logger.debug("Configuring default role-api mappings, use migrate-dynamicroles.py instead if you want to migrate rules from an existing commands.properties file");
        }
        final String scriptFile = "META-INF/db/create-default-role-api-mappings.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            logger.error("Unable to find default role-api mapping sql file, please configure api per role manually");
            return;
        }
        try(final InputStreamReader reader = new InputStreamReader(script)) {
            ScriptRunner runner = new ScriptRunner(conn, false, true);
            runner.runScript(reader);
        } catch (SQLException | IOException e) {
            logger.error("Unable to insert default api-role mappings from file: " + script + ". Please configure api per role manually, giving up!", e);
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-481to490-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
