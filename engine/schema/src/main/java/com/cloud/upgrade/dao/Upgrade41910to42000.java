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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;
import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Upgrade41910to42000 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {
    protected Logger logger = LogManager.getLogger(Upgrade41910to42000.class);

    private SystemVmTemplateRegistration systemVmTemplateRegistration;
    private static final int MAX_INDEXED_CHARS_IN_CHAR_SET_UTF8MB4 = 191;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.19.1.0", "4.20.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.20.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41910to42000.sql";
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
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`user` DROP COLUMN api_key, DROP COLUMN secret_key;");
            pstmt.executeUpdate();
            logger.info("Successfully performed keypair migration.");
        } catch (SQLException ex) {
            logger.info("Unexpected exception in user keypair migration", ex);
            throw ex;
        }
    }


    @Override
    public void performDataMigration(Connection conn) {
        try {
            performKeyPairMigration(conn);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        checkAndUpdateAffinityGroupNameCharSetToUtf8mb4(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41910to42000-cleanup.sql";
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

    private void checkAndUpdateAffinityGroupNameCharSetToUtf8mb4(Connection conn) {
        logger.debug("Check and update char set for affinity group name to utf8mb4");
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT MAX(LENGTH(name)) FROM `cloud`.`affinity_group`");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                long maxLengthOfName = rs.getLong(1);
                if (maxLengthOfName <= MAX_INDEXED_CHARS_IN_CHAR_SET_UTF8MB4) {
                    pstmt = conn.prepareStatement(String.format("ALTER TABLE `cloud`.`affinity_group` MODIFY `name` VARCHAR(%d) CHARACTER SET utf8mb4 NOT NULL", MAX_INDEXED_CHARS_IN_CHAR_SET_UTF8MB4));
                    pstmt.executeUpdate();
                    logger.debug("Successfully updated char set for affinity group name to utf8mb4");
                } else {
                    logger.warn("Unable to update char set for affinity group name, as there are some names with more than " + MAX_INDEXED_CHARS_IN_CHAR_SET_UTF8MB4 +
                            " chars (max supported chars for index)");
                }
            }

            if (rs != null && !rs.isClosed())  {
                rs.close();
            }
            if (pstmt != null && !pstmt.isClosed())  {
                pstmt.close();
            }
        } catch (final SQLException e) {
            logger.warn("Exception while updating char set for affinity group name to utf8mb4: " + e.getMessage());
        }
    }
}
