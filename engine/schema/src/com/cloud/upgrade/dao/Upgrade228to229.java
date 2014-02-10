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

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade228to229 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade228to229.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.8", "2.2.8"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.9";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-228to229.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-228to229.sql");
        }
        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        dropKeysIfExist(conn);
        PreparedStatement pstmt;
        try {
            /*fk_cluster__data_center_id has been wrongly added in previous upgrade(not sure which one), 228to229 upgrade drops it and re-add again*/
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`cluster` ADD CONSTRAINT `fk_cluster__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `cloud`.`data_center`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`snapshots` ADD INDEX `i_snapshots__removed`(`removed`)");
            pstmt.executeUpdate();

            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`network_tags` ADD CONSTRAINT `fk_network_tags__network_id` FOREIGN KEY (`network_id`) REFERENCES `networks`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();

            pstmt.close();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute cluster update", e);
        }
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    private void dropKeysIfExist(Connection conn) {
        HashMap<String, List<String>> indexes = new HashMap<String, List<String>>();
        HashMap<String, List<String>> foreignKeys = new HashMap<String, List<String>>();

        //indexes to drop
        //for network_offering
        List<String> keys = new ArrayList<String>();
        keys.add("name");
        indexes.put("network_offerings", keys);

        //for snapshot
        keys = new ArrayList<String>();
        keys.add("i_snapshots__removed");
        indexes.put("snapshots", keys);

        //for domain router
        keys = new ArrayList<String>();
        keys.add("i_domain_router__public_ip_address");
        indexes.put("domain_router", keys);

        //for user_ip_address
        keys = new ArrayList<String>();
        keys.add("i_user_ip_address__public_ip_address");
        indexes.put("user_ip_address", keys);

        //foreign keys to drop - this key would be re-added later
        keys = new ArrayList<String>();
        keys.add("fk_cluster__data_center_id");
        foreignKeys.put("cluster", keys);

        keys = new ArrayList<String>();
        keys.add("fk_domain_router__public_ip_address");
        foreignKeys.put("domain_router", keys);

        //drop foreign key from network tags table - it would be re-added later
        keys = new ArrayList<String>();
        keys.add("fk_network_tags__network_id");
        foreignKeys.put("network_tags", keys);

        // drop all foreign keys first
        s_logger.debug("Dropping keys that don't exist in 2.2.6 version of the DB...");
        for (String tableName : foreignKeys.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, foreignKeys.get(tableName), true);
        }

        // drop indexes now
        for (String tableName : indexes.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, indexes.get(tableName), false);
        }
    }

}
