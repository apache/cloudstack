/*
 * // Licensed to the Apache Software Foundation (ASF) under one
 * // or more contributor license agreements.  See the NOTICE file
 * // distributed with this work for additional information
 * // regarding copyright ownership.  The ASF licenses this file
 * // to you under the Apache License, Version 2.0 (the
 * // "License"); you may not use this file except in compliance
 * // with the License.  You may obtain a copy of the License at
 * //
 * //   http://www.apache.org/licenses/LICENSE-2.0
 * //
 * // Unless required by applicable law or agreed to in writing,
 * // software distributed under the License is distributed on an
 * // "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * // KIND, either express or implied.  See the License for the
 * // specific language governing permissions and limitations
 * // under the License.
 */

package com.cloud.upgrade.dao;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade41100to41110 extends DbUpgradeAbstractImpl {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.11.0.0", "4.11.1.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.11.1.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-41100to41110.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        markUnnecessarySecureConfigsAsUnsecure(conn);
    }

    private void markUnnecessarySecureConfigsAsUnsecure(Connection conn) {
        /*
         * the following config items where added as 'Secure' in the past. For some this made sense but for the ones below,
         * this makes no sense and is a inconvenience at best. The below method will
         ** retrieve,
         ** unencrypt,
         ** mark as 'Advanced' and then
         ** store the item
         */
        String[] unsecureItems = new String[] {
                "ldap.basedn",
                "ldap.bind.principal",
                "ldap.email.attribute",
                "ldap.firstname.attribute",
                "ldap.group.object",
                "ldap.group.user.uniquemember",
                "ldap.lastname.attribute",
                "ldap.search.group.principle",
                "ldap.truststore",
                "ldap.user.object",
                "ldap.username.attribute"
        };

        for (String name : unsecureItems) {
            uncrypt(conn, name);
        }
    }

    /**
     * if encrypted, decrypt the ldap hostname and port and then update as they are not encrypted now.
     */
    private void uncrypt(Connection conn, String name)
    {
        String value = null;
        try (
                PreparedStatement prepSelStmt = conn.prepareStatement("SELECT conf.category,conf.value FROM `cloud`.`configuration` conf WHERE conf.name= ?");
        ) {
            prepSelStmt.setString(1,name);
            try (
                    ResultSet resultSet = prepSelStmt.executeQuery();
            ) {
                if (logger.isInfoEnabled()) {
                    logger.info("updating setting '" + name + "'");
                }
                if (resultSet.next()) {
                    if ("Secure".equals(resultSet.getString(1))) {
                        value = DBEncryptionUtil.decrypt(resultSet.getString(2));
                        try (
                                PreparedStatement prepUpdStmt= conn.prepareStatement("UPDATE `cloud`.`configuration` SET category = 'Advanced', value = ? WHERE name = ?" );
                        ) {
                            prepUpdStmt.setString(1, value);
                            prepUpdStmt.setString(2, name);
                            prepUpdStmt.execute();
                        } catch (SQLException e) {
                            if (logger.isInfoEnabled()) {
                                logger.info("failed to update configuration item '" + name + "' with value '" + value + "'");
                                if (logger.isDebugEnabled()) {
                                    logger.debug("no update because ", e);
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("failed to update configuration item '" + name + "' with value '" + value + "'", e);
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-41100to41110-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
