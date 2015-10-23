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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade301to302 extends LegacyDbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade301to302.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"3.0.1", "3.0.2"};
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.2";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-301to302.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-301to302.sql");
        }

        return new File[] {new File(script)};
    }

    private void dropKeysIfExists(Connection conn) {
        HashMap<String, List<String>> uniqueKeys = new HashMap<String, List<String>>();
        List<String> keys = new ArrayList<String>();

        keys.add("i_host__allocation_state");
        uniqueKeys.put("host", keys);

        s_logger.debug("Droping i_host__allocation_state key in host table");
        for (String tableName : uniqueKeys.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, uniqueKeys.get(tableName), false);
        }
    }

    @Override
    public void performDataMigration(Connection conn) {
        dropKeysIfExists(conn);
        updateSharedNetworks(conn);
        fixLastHostIdKey(conn);
        changeEngine(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-301to302-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-301to302-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

    protected void updateSharedNetworks(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;

        try {
            pstmt =
                conn.prepareStatement("select n.id, map.id from `cloud`.`network_offerings` n, `cloud`.`ntwk_offering_service_map` map "
                    + "where n.id=map.network_offering_id and map.service='Lb' and map.provider='VirtualRouter';");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long ntwkOffId = rs.getLong(1);
                long mapId = rs.getLong(2);

                //check if the network offering has source nat service enabled
                pstmt =
                    conn.prepareStatement("select n.id from `cloud`.`network_offerings` n, `cloud`.`ntwk_offering_service_map`"
                        + " map where n.id=map.network_offering_id and map.service='SourceNat' AND n.id=?");
                pstmt.setLong(1, ntwkOffId);
                rs1 = pstmt.executeQuery();
                if (rs1.next()) {
                    continue;
                }

                //delete the service only when there are no lb rules for the network(s) using this network offering
                pstmt =
                    conn.prepareStatement("select * from  `cloud`.`firewall_rules` f, `cloud`.`networks` n, `cloud`.`network_offerings`"
                        + " off where f.purpose='LB' and f.network_id=n.id and n.network_offering_id=off.id and off.id=?");
                pstmt.setLong(1, ntwkOffId);
                rs1 = pstmt.executeQuery();
                if (rs1.next()) {
                    continue;
                }

                //delete lb service for the network offering
                pstmt = conn.prepareStatement("DELETE FROM `cloud`.`ntwk_offering_service_map` WHERE id=?");
                pstmt.setLong(1, mapId);
                pstmt.executeUpdate();
                s_logger.debug("Deleted lb service for network offering id=" + ntwkOffId + " as it doesn't have source nat service enabled");

                //delete lb service for the network
                pstmt =
                    conn.prepareStatement("SELECT map.id, n.id FROM `cloud`.`ntwk_service_map` map, networks n WHERE n.network_offering_id=? "
                        + "AND  map.network_id=n.id AND map.service='Lb'");
                pstmt.setLong(1, ntwkOffId);
                rs1 = pstmt.executeQuery();
                while (rs1.next()) {
                    mapId = rs1.getLong(1);
                    long ntwkId = rs1.getLong(2);

                    pstmt = conn.prepareStatement("DELETE FROM `cloud`.`ntwk_service_map` WHERE id=?");
                    pstmt.setLong(1, mapId);
                    pstmt.executeUpdate();
                    s_logger.debug("Deleted lb service for network id=" + ntwkId + " as it doesn't have source nat service enabled");
                }

            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update shared networks due to exception while executing query " + pstmt, e);
        } finally {
            closeAutoCloseable(rs);
            closeAutoCloseable(rs1);
            closeAutoCloseable(pstmt);
        }
    }

    private void fixLastHostIdKey(Connection conn) {
        //Drop i_usage_event__created key (if exists) and re-add it again
        List<String> keys = new ArrayList<String>();

        //Drop vmInstance keys (if exists) and insert one with correct name
        keys = new ArrayList<String>();

        keys.add("fk_vm_instance__last_host_id");
        keys.add("i_vm_instance__last_host_id");

        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_instance", keys, true);
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.vm_instance", keys, false);
        try (
                PreparedStatement pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`vm_instance` ADD CONSTRAINT `fk_vm_instance__last_host_id` FOREIGN KEY (`last_host_id`) REFERENCES `host` (`id`)");
            ){
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert foreign key in vm_instance table ", e);
        }
    }

    private void changeEngine(Connection conn) {
        s_logger.debug("Fixing engine and row_format for op_lock and op_nwgrp_work tables");
        String sqlOpLock = "ALTER TABLE `cloud`.`op_lock` ENGINE=MEMORY, ROW_FORMAT = FIXED";
        try (
                PreparedStatement pstmt = conn.prepareStatement(sqlOpLock);
            ) {
            pstmt.executeUpdate();
        } catch (Exception e) {
            s_logger.debug("Failed do execute the statement " + sqlOpLock + ", moving on as it's not critical fix");
        }

        String sqlOpNwgrpWork = "ALTER TABLE `cloud`.`op_nwgrp_work` ENGINE=MEMORY, ROW_FORMAT = FIXED";
        try  (
                PreparedStatement pstmt = conn.prepareStatement(sqlOpNwgrpWork);
             ) {
            pstmt.executeUpdate();
        } catch (Exception e) {
            s_logger.debug("Failed do execute the statement " + sqlOpNwgrpWork + ", moving on as it's not critical fix");
        }
    }

}
