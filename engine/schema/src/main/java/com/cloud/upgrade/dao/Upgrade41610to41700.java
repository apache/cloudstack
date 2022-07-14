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
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.log4j.Logger;

import java.io.InputStream;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public class Upgrade41610to41700 implements DbUpgrade, DbUpgradeSystemVmTemplate {

    final static Logger LOG = Logger.getLogger(Upgrade41700to41710.class);
    private SystemVmTemplateRegistration systemVmTemplateRegistration;

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.16.1.0", "4.17.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.17.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41610to41700.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        fixWrongPoolUuid(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41610to41700-cleanup.sql";
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

    public void fixWrongPoolUuid(Connection conn) {
        LOG.debug("Replacement of faulty pool uuids");
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT id,uuid FROM storage_pool "
                + "WHERE uuid NOT LIKE \"%-%-%-%\" AND removed IS NULL;"); ResultSet rs = pstmt.executeQuery()) {
            PreparedStatement updateStmt = conn.prepareStatement("update storage_pool set uuid = ? where id = ?");
            while (rs.next()) {
                    UUID poolUuid = new UUID(
                            new BigInteger(rs.getString(2).substring(0, 16), 16).longValue(),
                            new BigInteger(rs.getString(2).substring(16), 16).longValue()
                    );
                    updateStmt.setLong(2, rs.getLong(1));
                    updateStmt.setString(1, poolUuid.toString());
                    updateStmt.addBatch();
            }
            updateStmt.executeBatch();
        } catch (SQLException ex) {
            String errorMsg = "fixWrongPoolUuid:Exception while updating faulty pool uuids";
            LOG.error(errorMsg,ex);
            throw new CloudRuntimeException(errorMsg, ex);
        }
    }
}
