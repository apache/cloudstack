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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade225to226 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade225to226.class);

    @Override
    public File[] getPrepareScripts() {
        String file = Script.findScript("", "db/schema-225to226.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-225to226.sql");
        }

        return new File[] { new File(file) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        dropKeysIfExist(conn);
        dropTableColumnsIfExist(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.2.5", "2.2.5" };
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.6";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    private void dropTableColumnsIfExist(Connection conn) {
        HashMap<String, List<String>> tablesToModify = new HashMap<String, List<String>>();

        // domain router table
        List<String> columns = new ArrayList<String>();
        columns.add("account_id");
        columns.add("domain_id");
        tablesToModify.put("domain_router", columns);

        s_logger.debug("Dropping columns that don't exist in 2.2.6 version of the DB...");
        for (String tableName : tablesToModify.keySet()) {
            DbUpgradeUtils.dropTableColumnsIfExist(conn, tableName, tablesToModify.get(tableName));
        }
    }

    private void dropKeysIfExist(Connection conn) {
        HashMap<String, List<String>> foreignKeys = new HashMap<String, List<String>>();
        HashMap<String, List<String>> indexes = new HashMap<String, List<String>>();

        // domain router table
        List<String> keys = new ArrayList<String>();
        keys.add("fk_domain_router__account_id");
        foreignKeys.put("domain_router", keys);

        keys = new ArrayList<String>();
        keys.add("i_domain_router__account_id");
        indexes.put("domain_router", keys);

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
