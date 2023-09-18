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
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class Upgrade41810to41900 implements DbUpgrade, DbUpgradeSystemVmTemplate {
    final static Logger LOG = Logger.getLogger(Upgrade41810to41900.class);
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    private static final String ACCOUNT_DETAILS = "account_details";

    private static final String DOMAIN_DETAILS = "domain_details";

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.18.1.0", "4.19.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.19.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41810to41900.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        decryptConfigurationValuesFromAccountAndDomainScopesNotInSecureHiddenCategories(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41810to41900-cleanup.sql";
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
        LOG.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    protected void decryptConfigurationValuesFromAccountAndDomainScopesNotInSecureHiddenCategories(Connection conn) {
        LOG.info("Decrypting global configuration values from the following tables: account_details and domain_details.");

        Map<Long, String> accountsMap = getConfigsWithScope(conn, ACCOUNT_DETAILS);
        updateConfigValuesWithScope(conn, accountsMap, ACCOUNT_DETAILS);
        LOG.info("Successfully decrypted configurations from account_details table.");

        Map<Long, String> domainsMap = getConfigsWithScope(conn, DOMAIN_DETAILS);
        updateConfigValuesWithScope(conn, domainsMap, DOMAIN_DETAILS);
        LOG.info("Successfully decrypted configurations from domain_details table.");
    }

    protected Map<Long, String> getConfigsWithScope(Connection conn, String table) {
        Map<Long, String> configsToBeUpdated = new HashMap<>();
        String selectDetails = String.format("SELECT details.id, details.value from cloud.%s details, cloud.configuration c " +
                "WHERE details.name = c.name AND c.category NOT IN ('Hidden', 'Secure') AND details.value <> \"\" ORDER BY details.id;", table);

        try (PreparedStatement pstmt = conn.prepareStatement(selectDetails)) {
            try (ResultSet result = pstmt.executeQuery()) {
                while (result.next()) {
                    configsToBeUpdated.put(result.getLong("id"), result.getString("value"));
                }
            }
            return configsToBeUpdated;
        } catch (SQLException e) {
            String message = String.format("Unable to retrieve data from table [%s] due to [%s].", table, e.getMessage());
            LOG.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    protected void updateConfigValuesWithScope(Connection conn, Map<Long, String> configsToBeUpdated, String table) {
        String updateConfigValues = String.format("UPDATE cloud.%s SET value = ? WHERE id = ?;", table);

        for (Map.Entry<Long, String> config : configsToBeUpdated.entrySet()) {
            try (PreparedStatement pstmt = conn.prepareStatement(updateConfigValues)) {
                String decryptedValue = DBEncryptionUtil.decrypt(config.getValue());

                pstmt.setString(1, decryptedValue);
                pstmt.setLong(2, config.getKey());

                LOG.info(String.format("Updating config with ID [%s] to value [%s].", config.getKey(), decryptedValue));
                pstmt.executeUpdate();
            } catch (SQLException | EncryptionOperationNotPossibleException e) {
                String message = String.format("Unable to update config value with ID [%s] on table [%s] due to [%s]. The config value may already be decrypted.",
                        config.getKey(), table, e);
                LOG.error(message);
                throw new CloudRuntimeException(message, e);
            }
        }
    }
}
