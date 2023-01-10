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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade225to226 extends DbUpgradeAbstractImpl {

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-225to226.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        dropKeysIfExist(conn);
        dropTableColumnsIfExist(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return null;
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.5", "2.2.5"};
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

        logger.debug("Dropping columns that don't exist in 2.2.6 version of the DB...");
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
        logger.debug("Dropping keys that don't exist in 2.2.6 version of the DB...");
        for (String tableName : foreignKeys.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, foreignKeys.get(tableName), true);
        }

        // drop indexes now
        for (String tableName : indexes.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, indexes.get(tableName), false);
        }
    }
}
