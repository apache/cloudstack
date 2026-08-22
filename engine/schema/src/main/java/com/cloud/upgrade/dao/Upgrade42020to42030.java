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

import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42020to42030 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

    private static String SELECT_TEMPLATE = "SELECT `name` FROM `cloud`.`email_template` WHERE `name`=\"backup_report_template\";";
    private static String INSERT_TEMPLATE = "INSERT INTO `cloud`.`email_template` (name, template) VALUES (\"backup_report_template\", \'<h1>Backup report</h1>\n" +
            "<p>Backup storage usage increase for the environment during the period is ${storageUsage} GiB.</p>\n" +
            "\n" +
            "<#list backupReportDomainResponseList as domain>\n" +
            "<hr>\n" +
            "<h2>Domain ${domain.domainName}</h2>\n" +
            "<p>Backup storage usage increase for the domain ${domain.domainName} is ${domain.storageUsage} GiB.</p>\n" +
            "\n" +
            "    <#list domain.backupReportAccountResponseList as account>\n" +
            "        <hr>\n" +
            "        <#if account.accountName??>\n" +
            "            <h3>Account ${account.accountName}</h3>\n" +
            "            <p>Backup storage usage increase for the account ${account.accountName} is ${account.storageUsage} GiB.</p>    \n" +
            "        <#else>\n" +
            "            <h3>Project ${account.projectName}</h3>\n" +
            "            <p>Backup storage usage increase for the project ${account.projectName} is ${account.storageUsage} GiB.</p>\n" +
            "        </#if>\n" +
            "        \n" +
            "\n" +
            "        <#list account.successfulBackups>\n" +
            "            <p>Successfully created backups for the ${(account.accountName??)?then(\"project\",\"account\")}:</p>\n" +
            "            <ul>\n" +
            "            <#items as backup>\n" +
            "                <li>Backup ${backup.name!\"without name\"} with ID ${backup.id} of VM ${backup.vmName} with ID ${backup.vmId} was created at ${backup.date?datetime};</li>\n" +
            "            </#items>\n" +
            "            </ul>\n" +
            "        </#list>\n" +
            "\n" +
            "        <#list account.failedBackups>\n" +
            "            <p>Failed backups for the ${(account.accountName??)?then(\"project\",\"account\")}:</p>\n" +
            "            <ul>\n" +
            "\n" +
            "            <#items as backup>\n" +
            "                <li>Backup ${backup.name!\"without name\"} with ID ${backup.id} of VM ${backup.vmName} with ID ${backup.vmId} failed at ${(backup.date?datetime)!\"unable to get date\"};\n" +
            "\n" +
            "                <#if backup.failureReason?? || backup.logid??>\n" +
            "                    <ul> \n" +
            "                    <#if backup.failureReason??>\n" +
            "                        <li>Due to ${backup.failureReason}</li>        \n" +
            "                    </#if>   \n" +
            "                    <#if backup.logid??>\n" +
            "                        <li>With logid:${backup.logid}</li>\n" +
            "                    </#if>\n" +
            "                    </ul>\n" +
            "                </#if>\n" +
            "            </#items>\n" +
            "\n" +
            "            </ul>\n" +
            "        </#list>\n" +
            "\n" +
            "        <#list account.deletedBackups>\n" +
            "            <p>Removed backups for the ${(account.accountName??)?then(\"project\",\"account\")}:</p>  \n" +
            "            <ul>\n" +
            "            <#items as backup>\n" +
            "                <li>Backup ${backup.name} with ID ${backup.id} of VM ${backup.vmName} with ID ${backup.vmId} was created at ${(backup.date?datetime)!\"unable to get date\"} and deleted at ${backup.removed?datetime};</li>\n" +
            "            </#items>\n" +
            "            </ul>  \n" +
            "        </#list>\n" +
            "    </#list>\n" +
            "</#list>\n" +
            "\n" +
            "<#list backupScheduleResponseList>\n" +
            "<hr>\n" +
            "<h1>Scheduled backups</h1>\n" +
            "\n" +
            "<table>\n" +
            "<thead>\n" +
            "<tr>\n" +
            "<th>VM</th>\n" +
            "<th>ID</th>\n" +
            "<th>Scheduled date</th>\n" +
            "<th>Schedule ID</th>\n" +
            "</tr>\n" +
            "</thead>\n" +
            "<tbody>   \n" +
            "    <#items as schedule>\n" +
            "        <tr>\n" +
            "        <td>${schedule.vmName}</td>\n" +
            "        <td>${schedule.vmId}</td>\n" +
            "        <td>${schedule.schedule}</td>\n" +
            "        <td>${schedule.id}</td>\n" +
            "        </tr>\n" +
            "    </#items>\n" +
            "</tbody>\n" +
            "</table>\n" +
            "</#list>\n\')";

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.20.2.0", "4.20.3.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.20.3.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-42020to42030.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        insertBackupReportEmailTemplate(conn);
    }

    private void insertBackupReportEmailTemplate(Connection conn) {
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_TEMPLATE)) {
            ResultSet result = pstmt.executeQuery();
            if (result.next()) {
                logger.debug("Email template for backup_report_template is already on the database.");
                return;
            }
        } catch (SQLException e) {
            String message = String.format("Unable to retrieve email templates due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }

        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_TEMPLATE)) {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            String message = String.format("Unable to insert email template for backup_report_template due to [%s].", e.getMessage());
            logger.error(message, e);
            throw new CloudRuntimeException(message, e);
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return null;
    }
}
