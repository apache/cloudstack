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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade42300to2400 extends DbUpgradeAbstractImpl implements DbUpgrade, DbUpgradeSystemVmTemplate {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[]{"4.23.0.0", "24.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "24.0.0";
    }

    @Override
    public void performDataMigration(Connection conn) {
        encryptUserData(conn, "user_vm",
                "SELECT `id`, `user_data` FROM `cloud`.`user_vm` WHERE `user_data` IS NOT NULL",
                "UPDATE `cloud`.`user_vm` SET `user_data` = ? WHERE `id` = ?");
        encryptUserData(conn, "user_data",
                "SELECT `id`, `user_data` FROM `cloud`.`user_data` WHERE `user_data` IS NOT NULL",
                "UPDATE `cloud`.`user_data` SET `user_data` = ? WHERE `id` = ?");
        encryptUserData(conn, "autoscale_vmprofiles",
                "SELECT `id`, `user_data` FROM `cloud`.`autoscale_vmprofiles` WHERE `user_data` IS NOT NULL",
                "UPDATE `cloud`.`autoscale_vmprofiles` SET `user_data` = ? WHERE `id` = ?");
    }

    private void encryptUserData(Connection conn, String table, String selectSql, String updateSql) {
        try (PreparedStatement selectStatement = conn.prepareStatement(selectSql);
             ResultSet resultSet = selectStatement.executeQuery();
             PreparedStatement updateStatement = conn.prepareStatement(updateSql)) {
            while (resultSet.next()) {
                updateStatement.setString(1, DBEncryptionUtil.encrypt(resultSet.getString(2)));
                updateStatement.setLong(2, resultSet.getLong(1));
                updateStatement.addBatch();
            }
            updateStatement.executeBatch();
        } catch (SQLException e) {
            throw new CloudRuntimeException(String.format("Failed to encrypt user_data in table %s", table), e);
        }
        logger.debug("Encrypted user_data in table {}", table);
    }
}
