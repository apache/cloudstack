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

    @Override
    public void performDataMigration(Connection conn) {
        migrateConfigurationScopeToBitmask(conn);
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
