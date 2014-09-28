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

public class Upgrade450to460 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade450to460.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.5.0", "4.6.0"};
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
        String script = Script.findScript("", "db/schema-450to460.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-450to460.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        addRedundancyForNwAndVpc(conn);
    }

    private void addRedundancyForNwAndVpc(Connection conn) {
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
                long nwOfferingId = rs.getLong("nw.network_offering_id");
                updateNwRedundancyPstmt.setLong(1, nwOfferingId);
                updateNwRedundancyPstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new CloudRuntimeException("Adding redundancy to vpc, networks and vpc_offerings failed", e);
        }
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-450to460-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-450to460-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

}