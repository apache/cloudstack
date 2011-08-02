/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
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
        return new String[] { "2.2.8", "2.2.8"};
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

        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        dropKeysIfExist(conn);
        PreparedStatement pstmt;
        try {
            /*fk_cluster__data_center_id has been wrongly added in previous upgrade(not sure which one), 228to229 upgrade drops it and re-add again*/
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`cluster` ADD CONSTRAINT `fk_cluster__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `cloud`.`data_center`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`snapshots` ADD INDEX `i_snapshots__removed`(`removed`)");
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

        //foreign keys to drop - this key would be re-added later
        keys = new ArrayList<String>();
        keys.add("fk_cluster__data_center_id");
        foreignKeys.put("cluster", keys);


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
