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
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade430to440 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade430to440.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.3.0", "4.4.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.4.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-430to440.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-4310to440.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        populateACLGroupAccountMap(conn);
    }

    // populate acl_group_account_map table for existing accounts
    private void populateACLGroupAccountMap(Connection conn) {
        PreparedStatement acctInsert = null;
        PreparedStatement acctQuery = null;
        ResultSet rs = null;

        s_logger.debug("Populating acl_group_account_map table for existing accounts...");
        try {
            acctInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`acl_group_account_map` (group_id, account_id, created) values(?, ?, Now())");
            acctQuery = conn
                    .prepareStatement("select id, type from `cloud`.`account` where removed is null");
            rs = acctQuery.executeQuery();

            while (rs.next()) {
                Long acct_id = rs.getLong("id");
                short type = rs.getShort("type");

                // insert entry in acl_group_account_map table
                acctInsert.setLong(1, type + 1);
                acctInsert.setLong(2, acct_id);
                acctInsert.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Unable to populate acl_group_account_map for existing accounts." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (acctInsert != null) {
                    acctInsert.close();
                }
                if (acctQuery != null) {
                    acctQuery.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed populate acl_group_account_map for existing accounts.");
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-430to440-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-430to440-cleanup.sql");
        }

        return new File[] {new File(script)};
    }
}
