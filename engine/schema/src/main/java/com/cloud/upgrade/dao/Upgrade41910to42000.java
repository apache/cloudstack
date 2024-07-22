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

import com.cloud.upgrade.SystemVmTemplateRegistration;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41910to42000 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {
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

    @Override
    public void performDataMigration(Connection conn) {
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
