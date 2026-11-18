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
import java.util.Map;

import org.apache.commons.codec.binary.Base64;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41000to41100 extends DbUpgradeAbstractImpl {


    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.10.0.0", "4.11.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.11.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41000to41100.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        checkAndEnableDynamicRoles(conn);
        validateUserDataInBase64(conn);
    }

    private void checkAndEnableDynamicRoles(final Connection conn) {
        final Map<String, String> apiMap = PropertiesUtil.processConfigFile(new String[] { "commands.properties" });
        if (apiMap == null || apiMap.isEmpty()) {
            if (logger.isDebugEnabled()) {
                logger.debug("No commands.properties file was found, enabling dynamic roles by setting dynamic.apichecker.enabled to true if not already enabled.");
            }
            try (final PreparedStatement updateStatement = conn.prepareStatement("INSERT INTO cloud.configuration (category, instance, name, default_value, value) VALUES ('Advanced', 'DEFAULT', 'dynamic.apichecker.enabled', 'false', 'true') ON DUPLICATE KEY UPDATE value='true'")) {
                updateStatement.executeUpdate();
            } catch (SQLException e) {
                logger.error("Failed to set dynamic.apichecker.enabled to true, please run migrate-dynamicroles.py script to manually migrate to dynamic roles.", e);
            }
        } else {
            logger.warn("Old commands.properties static checker is deprecated, please use migrate-dynamicroles.py to migrate to dynamic roles. Refer http://docs.cloudstack.apache.org/projects/cloudstack-administration/en/latest/accounts.html#using-dynamic-roles");
        }
    }

    private void validateUserDataInBase64(Connection conn) {
        try (final PreparedStatement selectStatement = conn.prepareStatement("SELECT `id`, `user_data` FROM `cloud`.`user_vm` WHERE `user_data` IS NOT NULL;");
             final ResultSet selectResultSet = selectStatement.executeQuery()) {
            while (selectResultSet.next()) {
                final Long userVmId = selectResultSet.getLong(1);
                final String userData = selectResultSet.getString(2);
                if (Base64.isBase64(userData)) {
                    final String newUserData = Base64.encodeBase64String(Base64.decodeBase64(userData.getBytes()));
                    if (!userData.equals(newUserData)) {
                        try (final PreparedStatement updateStatement = conn.prepareStatement("UPDATE `cloud`.`user_vm` SET `user_data` = ? WHERE `id` = ? ;")) {
                            updateStatement.setString(1, newUserData);
                            updateStatement.setLong(2, userVmId);
                            updateStatement.executeUpdate();
                        } catch (SQLException e) {
                            logger.error("Failed to update cloud.user_vm user_data for id:" + userVmId + " with exception: " + e.getMessage());
                            throw new CloudRuntimeException("Exception while updating cloud.user_vm for id " + userVmId, e);
                        }
                    }
                } else {
                    // Update to NULL since it's invalid
                    logger.warn("Removing user_data for vm id " + userVmId + " because it's invalid");
                    logger.warn("Removed data was: " + userData);
                    try (final PreparedStatement updateStatement = conn.prepareStatement("UPDATE `cloud`.`user_vm` SET `user_data` = NULL WHERE `id` = ? ;")) {
                        updateStatement.setLong(1, userVmId);
                        updateStatement.executeUpdate();
                    } catch (SQLException e) {
                        logger.error("Failed to update cloud.user_vm user_data for id:" + userVmId + " to NULL with exception: " + e.getMessage());
                        throw new CloudRuntimeException("Exception while updating cloud.user_vm for id " + userVmId + " to NULL", e);
                    }
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while validating existing user_vm table's user_data column to be base64 valid with padding", e);
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Done validating base64 content of user data");
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41000to41100-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
