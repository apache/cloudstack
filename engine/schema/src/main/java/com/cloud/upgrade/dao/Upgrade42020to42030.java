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

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42020to42030 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

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
        unhideJsInterpretationEnabled(conn);
    }

    protected void unhideJsInterpretationEnabled(Connection conn) {
        String value = getJsInterpretationEnabled(conn);
        if (value != null) {
            updateJsInterpretationEnabledFields(conn, value);
        }
    }

    protected String getJsInterpretationEnabled(Connection conn) {
        String query = "SELECT value FROM cloud.configuration WHERE name = 'js.interpretation.enabled' AND category = 'Hidden';";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("value");
            }
            logger.debug("Unable to retrieve value of hidden configuration 'js.interpretation.enabled'. The configuration may already be unhidden.");
            return null;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while retrieving value of hidden configuration 'js.interpretation.enabled'.", e);
        }
    }

    protected void updateJsInterpretationEnabledFields(Connection conn, String encryptedValue) {
        String query = "UPDATE cloud.configuration SET value = ?, category = 'System' WHERE name = 'js.interpretation.enabled';";

        try (PreparedStatement pstmt = conn.prepareStatement(query)) {
            String decryptedValue = DBEncryptionUtil.decrypt(encryptedValue);
            logger.info("Updating setting 'js.interpretation.enabled' to decrypted value [{}], and category 'System'.", decryptedValue);
            pstmt.setString(1, decryptedValue);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while unhiding configuration 'js.interpretation.enabled'.", e);
        } catch (CloudRuntimeException e) {
            logger.warn("Error while decrypting configuration 'js.interpretation.enabled'. The configuration may already be decrypted.");
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return null;
    }

    @Override
    public void updateSystemVmTemplates(Connection conn) {
    }
}
