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

public class Upgrade227to228Premium extends Upgrade227to228 {

    @Override
    public InputStream[] getPrepareScripts() {
        InputStream[] newScripts = new InputStream[2];
        newScripts[0] = super.getPrepareScripts()[0];
        final String scriptFile = "META-INF/db/schema-227to228-premium.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }
        newScripts[1] = script;

        return newScripts;
    }

    @Override
    public void performDataMigration(Connection conn) {
        addSourceIdColumn(conn);
        addNetworkIdsToUserStats(conn);
        super.performDataMigration(conn);
    }

    private void addSourceIdColumn(Connection conn) {
        boolean insertField = false;
        try {
            PreparedStatement pstmt;
            try {
                pstmt = conn.prepareStatement("SELECT source_id FROM `cloud_usage`.`usage_storage`");
                ResultSet rs = pstmt.executeQuery();

                if (rs.next()) {
                    logger.info("The source id field already exist, not adding it");
                }

            } catch (Exception e) {
                // if there is an exception, it means that field doesn't exist, and we can create it
                insertField = true;
            }

            if (insertField) {
                logger.debug("Adding source_id to usage_storage...");
                pstmt = conn.prepareStatement("ALTER TABLE `cloud_usage`.`usage_storage` ADD COLUMN `source_id` bigint unsigned");
                pstmt.executeUpdate();
                logger.debug("Column source_id was added successfully to usage_storage table");
                pstmt.close();
            }

        } catch (SQLException e) {
            logger.error("Failed to add source_id to usage_storage due to ", e);
            throw new CloudRuntimeException("Failed to add source_id to usage_storage due to ", e);
        }
    }

    private void addNetworkIdsToUserStats(Connection conn) {
        logger.debug("Adding network IDs to user stats...");
        try {
            String stmt = "SELECT DISTINCT public_ip_address FROM `cloud`.`user_statistics` WHERE public_ip_address IS NOT null";
            PreparedStatement pstmt = conn.prepareStatement(stmt);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                String publicIpAddress = rs.getString(1);
                stmt = "SELECT network_id FROM `cloud`.`user_ip_address` WHERE public_ip_address = ?";
                pstmt = conn.prepareStatement(stmt);
                pstmt.setString(1, publicIpAddress);
                ResultSet rs2 = pstmt.executeQuery();

                if (rs2.next()) {
                    Long networkId = rs2.getLong(1);
                    String[] dbs = {"cloud", "cloud_usage"};
                    for (String db : dbs) {
                        stmt = "UPDATE `" + db + "`.`user_statistics` SET network_id = ? WHERE public_ip_address = ?";
                        pstmt = conn.prepareStatement(stmt);
                        pstmt.setLong(1, networkId);
                        pstmt.setString(2, publicIpAddress);
                        pstmt.executeUpdate();
                    }
                }

                rs2.close();
            }

            rs.close();
            pstmt.close();
            logger.debug("Successfully added network IDs to user stats.");
        } catch (SQLException e) {
            String errorMsg = "Failed to add network IDs to user stats.";
            logger.error(errorMsg, e);
            throw new CloudRuntimeException(errorMsg, e);
        }
    }

}
