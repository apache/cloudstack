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

public class Upgrade451to460 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade451to460.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.5.1", "4.6.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.6.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        final String script = Script.findScript("", "db/schema-451to460.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-451to460.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(final Connection conn) {
        updateVMInstanceUserId(conn);
    }

    public void updateVMInstanceUserId(final Connection conn) {
        // For schemas before this, copy first user from an account_id which deployed already running VMs
        s_logger.debug("Updating vm_instance column user_id using first user in vm_instance's account_id");
        final String vmInstanceSql = "SELECT id, account_id FROM `cloud`.`vm_instance`";
        final String userSql = "SELECT id FROM `cloud`.`user` where account_id=?";
        final String userIdUpdateSql = "update `cloud`.`vm_instance` set user_id=? where id=?";
        try(PreparedStatement selectStatement = conn.prepareStatement(vmInstanceSql)) {
            final ResultSet results = selectStatement.executeQuery();
            while (results.next()) {
                final long vmId = results.getLong(1);
                final long accountId = results.getLong(2);
                try (PreparedStatement selectUserStatement = conn.prepareStatement(userSql)) {
                    selectUserStatement.setLong(1, accountId);
                    final ResultSet userResults = selectUserStatement.executeQuery();
                    if (userResults.next()) {
                        final long userId = userResults.getLong(1);
                        try (PreparedStatement updateStatement = conn.prepareStatement(userIdUpdateSql)) {
                            updateStatement.setLong(1, userId);
                            updateStatement.setLong(2, vmId);
                            updateStatement.executeUpdate();
                        } catch (final SQLException e) {
                            throw new CloudRuntimeException("Unable to update user ID " + userId + " on vm_instance id=" + vmId, e);
                        }
                    }

                } catch (final SQLException e) {
                    throw new CloudRuntimeException("Unable to update user ID using accountId " + accountId + " on vm_instance id=" + vmId, e);
                }
            }
        } catch (final SQLException e) {
            throw new CloudRuntimeException("Unable to update user Ids for previously deployed VMs", e);
        }
        s_logger.debug("Done updating user Ids for previously deployed VMs");
        addRedundancyForNwAndVpc(conn);
    }

    private void addRedundancyForNwAndVpc(final Connection conn) {
        ResultSet rs = null;
        try (PreparedStatement addRedundantColToVpcOfferingPstmt = conn.prepareStatement(
                "ALTER TABLE `cloud`.`vpc_offerings` ADD COLUMN `redundant_router_service` tinyint(1) DEFAULT 0");
                PreparedStatement addRedundantColToVpcPstmt = conn.prepareStatement(
                        "ALTER TABLE `cloud`.`vpc` ADD COLUMN `redundant` tinyint(1) DEFAULT 0");
                PreparedStatement addRedundantColToNwPstmt = conn.prepareStatement(
                        "ALTER TABLE `cloud`.`networks` ADD COLUMN `redundant` tinyint(1) DEFAULT 0");

                // The redundancy of the networks must be based on the redundancy of their network offerings
                PreparedStatement redundancyPerNwPstmt = conn.prepareStatement(
                        "select distinct nw.network_offering_id from networks nw join network_offerings off " +
                        "on nw.network_offering_id = off.id where off.redundant_router_service = 1");
                PreparedStatement updateNwRedundancyPstmt = conn.prepareStatement(
                        "update networks set redundant = 1 where network_offering_id = ?");
                ) {
            addRedundantColToVpcPstmt.executeUpdate();
            addRedundantColToVpcOfferingPstmt.executeUpdate();
            addRedundantColToNwPstmt.executeUpdate();

            rs = redundancyPerNwPstmt.executeQuery();
            while(rs.next()){
                final long nwOfferingId = rs.getLong("nw.network_offering_id");
                updateNwRedundancyPstmt.setLong(1, nwOfferingId);
                updateNwRedundancyPstmt.executeUpdate();
            }
        } catch (final SQLException e) {
            e.printStackTrace();
            throw new CloudRuntimeException("Adding redundancy to vpc, networks and vpc_offerings failed", e);
        }
    }

    @Override
    public File[] getCleanupScripts() {
        final String script = Script.findScript("", "db/schema-451to460-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-451to460-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

}