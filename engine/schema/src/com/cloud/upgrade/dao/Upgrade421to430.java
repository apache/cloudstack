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
import java.util.Map;

import org.apache.log4j.Logger;

import org.apache.cloudstack.acl.RoleType;

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade421to430 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade421to430.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.2.1", "4.3.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.3.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-421to430.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-421to430.sql");
        }

        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        populateACLGroupAccountMap(conn);
        populateACLGroupRoleMap(conn);
        populateACLRoleBasedAPIPermission(conn);
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

    // populate acl_group_role_map table for existing accounts
    private void populateACLGroupRoleMap(Connection conn) {
        PreparedStatement sqlInsert = null;
        ResultSet rs = null;

        s_logger.debug("Populating acl_group_role_map table for default groups and roles...");
        try {
            sqlInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`acl_group_role_map` (group_id, role_id, created) values(?, ?, Now())");
            for (int i = 1; i < 6; i++) {
                // insert entry in acl_group_role_map table, 1 to 1 mapping for default group and role
                sqlInsert.setLong(1, i);
                sqlInsert.setLong(2, i);
                sqlInsert.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Unable to populate acl_group_role_map for default groups and roles." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (sqlInsert != null) {
                    sqlInsert.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed populate acl_group_role_map for existing accounts.");
    }

    private void populateACLRoleBasedAPIPermission(Connection conn) {
        // read the commands.properties.in and populate the table
        PreparedStatement apiInsert = null;

        s_logger.debug("Populating acl_api_permission table for existing commands...");
        try {
            apiInsert = conn.prepareStatement("INSERT INTO `cloud`.`acl_api_permission` (role_id, api, created) values(?, ?, Now())");

            Map<String, String> commandMap = PropertiesUtil.processConfigFile(new String[] { "commands.properties" });
            for (Map.Entry<String, String> entry : commandMap.entrySet()) {
                String apiName = entry.getKey();
                String roleMask = entry.getValue();
                try {
                    short cmdPermissions = Short.parseShort(roleMask);
                    for (RoleType roleType : RoleType.values()) {
                        if ((cmdPermissions & roleType.getValue()) != 0) {
                            // insert entry into api_permission for this role
                            apiInsert.setLong(1, roleType.ordinal() + 1);
                            apiInsert.setString(2, apiName);
                            apiInsert.executeUpdate();
                        }
                    }
                } catch (NumberFormatException nfe) {
                    s_logger.info("Malformed key=value pair for entry: " + entry.toString());
                }
            }
        } catch (SQLException e) {
            String msg = "Unable to populate acl_api_permission for existing commands." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (apiInsert != null) {
                    apiInsert.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed populate acl_api_permission for existing commands.");
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-421to430-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-421to430-cleanup.sql");
        }

        return new File[] { new File(script) };
    }

}
