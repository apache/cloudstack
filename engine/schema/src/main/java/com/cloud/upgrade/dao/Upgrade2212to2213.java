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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade2212to2213 extends DbUpgradeAbstractImpl {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.12", "2.2.13"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.13";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-2212to2213.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        fixForeignKeys(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return null;
    }

    private void fixForeignKeys(Connection conn) {
        HashMap<String, List<String>> foreignKeys = new HashMap<String, List<String>>();
        List<String> keys = new ArrayList<String>();
        keys.add("fk_networks__data_center_id");
        foreignKeys.put("networks", keys);

        // drop all foreign keys
        logger.debug("Dropping old key fk_networks__data_center_id...");
        for (String tableName : foreignKeys.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, foreignKeys.get(tableName), true);
        }

        try {
            PreparedStatement pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `fk_networks__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to reinsert data center key for the network", e);
        }

        // drop primary keys
        DbUpgradeUtils.dropPrimaryKeyIfExists(conn, "cloud_usage.usage_load_balancer_policy");
        DbUpgradeUtils.dropPrimaryKeyIfExists(conn, "cloud_usage.usage_port_forwarding");

        //Drop usage_network_offering unique key
        try {
            PreparedStatement pstmt = conn.prepareStatement("drop index network_offering_id on cloud_usage.usage_network_offering");
            pstmt.executeUpdate();
            logger.debug("Dropped usage_network_offering unique key");
        } catch (Exception e) {
            // Ignore error if the usage_network_offering table or the unique key doesn't exist
        }
    }
}
