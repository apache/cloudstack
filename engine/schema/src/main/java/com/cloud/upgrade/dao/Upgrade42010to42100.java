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

import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.apache.cloudstack.framework.config.ConfigKey;

public class Upgrade42010to42100 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.20.1.0", "4.21.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.21.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42010to42100.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    private void performKeyPairMigration(Connection conn) throws SQLException {
        try {
            logger.debug("Performing keypair migration from user table to api_keypair table.");
            PreparedStatement pstmt = conn.prepareStatement("SELECT u.id, u.api_key, u.secret_key, a.domain_id, u.id FROM `cloud`.`user` AS u JOIN `cloud`.`account` AS a " +
                    "ON u.account_id = a.id WHERE u.api_key IS NOT NULL AND u.secret_key IS NOT NULL");
            ResultSet resultSet = pstmt.executeQuery();

            while (resultSet.next()) {
                long id = resultSet.getLong(1);
                String apiKey = resultSet.getString(2);
                String secretKey = resultSet.getString(3);
                Long domainId = resultSet.getLong(4);
                Long accountId = resultSet.getLong(5);
                Date timestamp = Date.valueOf(LocalDate.now());

                PreparedStatement preparedStatement = conn.prepareStatement("INSERT IGNORE INTO `cloud`.`api_keypair` (uuid, user_id, domain_id, account_id, api_key, secret_key, created, name) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                String uuid = UUID.randomUUID().toString();
                preparedStatement.setString(1, uuid);
                preparedStatement.setLong(2, id);
                preparedStatement.setLong(3, domainId);
                preparedStatement.setLong(4, accountId);

                preparedStatement.setString(5, apiKey);
                preparedStatement.setString(6, secretKey);
                preparedStatement.setDate(7, timestamp);
                preparedStatement.setString(8, uuid);

                preparedStatement.executeUpdate();
            }
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`user` DROP COLUMN IF EXISTS api_key, DROP COLUMN IF EXISTS secret_key;");
            pstmt.executeUpdate();
            logger.info("Successfully performed keypair migration.");
        } catch (SQLException ex) {
            logger.info("Unexpected exception in user keypair migration", ex);
            throw ex;
        }
    }

    @Override
    public void performDataMigration(Connection conn) {
        migrateConfigurationScopeToBitmask(conn);
        try {
            performKeyPairMigration(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-42010to42100-cleanup.sql";
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

    protected void migrateConfigurationScopeToBitmask(Connection conn) {
        String scopeDataType = DbUpgradeUtils.getTableColumnType(conn, "configuration", "scope");
        logger.info("Data type of the column scope of table configuration is {}", scopeDataType);
        if (!"varchar(255)".equals(scopeDataType)) {
            return;
        }
        DbUpgradeUtils.addTableColumnIfNotExist(conn, "configuration", "new_scope", "BIGINT DEFAULT 0");
        migrateExistingConfigurationScopeValues(conn);
        DbUpgradeUtils.dropTableColumnsIfExist(conn, "configuration", List.of("scope"));
        DbUpgradeUtils.changeTableColumnIfNotExist(conn, "configuration", "new_scope", "scope", "BIGINT NOT NULL DEFAULT 0 COMMENT 'Bitmask for scope(s) of this parameter'");
    }

    protected void migrateExistingConfigurationScopeValues(Connection conn) {
        StringBuilder sql = new StringBuilder("UPDATE configuration\n" +
                "SET new_scope = " +
                "    CASE ");
        for (ConfigKey.Scope scope : ConfigKey.Scope.values()) {
            sql.append("        WHEN scope = '").append(scope.name()).append("' THEN ").append(scope.getBitValue()).append(" ");
        }
        sql.append("        ELSE 0 " +
                "    END " +
                "WHERE scope IS NOT NULL;");
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try (PreparedStatement pstmt = txn.prepareAutoCloseStatement(sql.toString())) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to migrate existing configuration scope values to bitmask", e);
            throw new CloudRuntimeException(String.format("Failed to migrate existing configuration scope values to bitmask due to: %s", e.getMessage()));
        }
    }
}
