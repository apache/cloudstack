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
import com.cloud.utils.DateUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Upgrade41810to41900 implements DbUpgrade, DbUpgradeSystemVmTemplate {
    final static Logger LOG = Logger.getLogger(Upgrade41810to41900.class);
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

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
        LOG.debug("Updating System Vm template IDs");
        initSystemVmTemplateRegistration();
        try {
            systemVmTemplateRegistration.updateSystemVmTemplates(conn);
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to find / register SystemVM template(s)");
        }
    }

    public void migrateBackupDates(Connection conn) {
        LOG.info("Trying to convert backups' date column from varchar(255) to datetime type.");

        modifyDateColumnNameAndCreateNewOne(conn);
        fetchDatesAndMigrateToNewColumn(conn);
        dropOldColumn(conn);

        LOG.info("Finished converting backups' date column from varchar(255) to datetime.");
    }

    private void modifyDateColumnNameAndCreateNewOne(Connection conn) {
        String alterColumnName = "ALTER TABLE `cloud`.`backups` CHANGE COLUMN `date` `old_date` varchar(255);";
        try (PreparedStatement pstmt = conn.prepareStatement(alterColumnName)) {
            pstmt.execute();
        } catch (SQLException e) {
            String message = String.format("Unable to alter backups' date column name due to [%s].", e.getMessage());
            LOG.error(message, e);
            throw new CloudRuntimeException(message, e);
        }

        String createNewColumn = "ALTER TABLE `cloud`.`backups` ADD COLUMN `date` DATETIME;";
        try (PreparedStatement pstmt = conn.prepareStatement(createNewColumn)) {
            pstmt.execute();
        } catch (SQLException e) {
            String message = String.format("Unable to crate new backups' column date due to [%s].", e.getMessage());
            LOG.error(message, e);
            throw new CloudRuntimeException(message, e);
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
            LOG.error(message, e);
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
            LOG.error(msg);
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
            LOG.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    private void dropOldColumn(Connection conn) {
        String dropOldColumn = "ALTER TABLE `cloud`.`backups` DROP COLUMN `old_date`;";
        try (PreparedStatement pstmt = conn.prepareStatement(dropOldColumn)) {
            pstmt.execute();
        } catch (SQLException e) {
            String message = String.format("Unable to drop old_date column due to [%s].", e.getMessage());
            LOG.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

}
