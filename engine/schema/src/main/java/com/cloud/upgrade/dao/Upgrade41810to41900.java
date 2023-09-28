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
import com.cloud.utils.DateUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Upgrade41810to41900 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    private static final String ACCOUNT_DETAILS = "account_details";

    private static final String DOMAIN_DETAILS = "domain_details";

    private final SimpleDateFormat[] formats = {
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"), new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"), new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"),
            new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy")};

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
        migrateBackupDates(conn);
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
        logger.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    protected void decryptConfigurationValuesFromAccountAndDomainScopesNotInSecureHiddenCategories(Connection conn) {
        logger.info("Decrypting global configuration values from the following tables: account_details and domain_details.");

        Map<Long, String> accountsMap = getConfigsWithScope(conn, ACCOUNT_DETAILS);
        updateConfigValuesWithScope(conn, accountsMap, ACCOUNT_DETAILS);
        logger.info("Successfully decrypted configurations from account_details table.");

        Map<Long, String> domainsMap = getConfigsWithScope(conn, DOMAIN_DETAILS);
        updateConfigValuesWithScope(conn, domainsMap, DOMAIN_DETAILS);
        logger.info("Successfully decrypted configurations from domain_details table.");
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
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    public void migrateBackupDates(Connection conn) {
        logger.info("Trying to convert backups' date column from varchar(255) to datetime type.");

        modifyDateColumnNameAndCreateNewOne(conn);
        fetchDatesAndMigrateToNewColumn(conn);
        dropOldColumn(conn);

        logger.info("Finished converting backups' date column from varchar(255) to datetime.");
    }

    private void modifyDateColumnNameAndCreateNewOne(Connection conn) {
        String alterColumnName = "ALTER TABLE `cloud`.`backups` CHANGE COLUMN `date` `old_date` varchar(255);";
        try (PreparedStatement pstmt = conn.prepareStatement(alterColumnName)) {
            pstmt.execute();
        } catch (SQLException e) {
            String message = String.format("Unable to alter backups' date column name due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }

        String createNewColumn = "ALTER TABLE `cloud`.`backups` ADD COLUMN `date` DATETIME;";
        try (PreparedStatement pstmt = conn.prepareStatement(createNewColumn)) {
            pstmt.execute();
        } catch (SQLException e) {
            String message = String.format("Unable to crate new backups' column date due to [%s].", e.getMessage());
            logger.error(message, e);
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

                logger.info(String.format("Updating config with ID [%s] to value [%s].", config.getKey(), decryptedValue));
                pstmt.executeUpdate();
            } catch (SQLException | EncryptionOperationNotPossibleException e) {
                String message = String.format("Unable to update config value with ID [%s] on table [%s] due to [%s]. The config value may already be decrypted.",
                        config.getKey(), table, e);
                logger.error(message);
                throw new CloudRuntimeException(message, e);
            }
        }
    }

    private void fetchDatesAndMigrateToNewColumn(Connection conn) {
        String selectBackupDates = "SELECT `id`, `old_date` FROM `cloud`.`backups` WHERE 1;";
        String date;
        java.sql.Date reformatedDate;

        try (PreparedStatement pstmt = conn.prepareStatement(selectBackupDates)) {
            try (ResultSet result = pstmt.executeQuery()) {
                while (result.next()) {
                    date = result.getString("old_date");
                    reformatedDate = tryToTransformStringToDate(date);
                    updateBackupDate(conn, result.getLong("id"), reformatedDate);
                }
            }
        } catch (SQLException e) {
            String message = String.format("Unable to retrieve backup dates due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    private java.sql.Date tryToTransformStringToDate(String date) {
        Date parsedDate = null;
        try {
            parsedDate = DateUtil.parseTZDateString(date);
        } catch (ParseException e) {
            for (SimpleDateFormat sdf: formats) {
                try {
                    parsedDate = sdf.parse(date);
                } catch (ParseException ex) {
                    continue;
                }
                break;
            }
        }
        if (parsedDate == null) {
            String msg = String.format("Unable to parse date [%s]. Will change backup date to null.", date);
            logger.error(msg);
            return null;
        }

        return new java.sql.Date(parsedDate.getTime());
    }

    private void updateBackupDate(Connection conn, long id, java.sql.Date date) {
        String updateBackupDate = "UPDATE `cloud`.`backups` SET `date` = ? WHERE `id` = ?;";
        try (PreparedStatement pstmt = conn.prepareStatement(updateBackupDate)) {
            pstmt.setDate(1, date);
            pstmt.setLong(2, id);

            pstmt.executeUpdate();
        } catch (SQLException e) {
            String message = String.format("Unable to update backup date with id [%s] to date [%s] due to [%s].", id, date, e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    private void dropOldColumn(Connection conn) {
        String dropOldColumn = "ALTER TABLE `cloud`.`backups` DROP COLUMN `old_date`;";
        try (PreparedStatement pstmt = conn.prepareStatement(dropOldColumn)) {
            pstmt.execute();
        } catch (SQLException e) {
            String message = String.format("Unable to drop old_date column due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

}
