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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cloud.capacity.Capacity;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade222to224 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade222to224.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.2.2", "2.2.3" };
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.4";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-222to224.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-222to224.sql");
        }

        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateClusterIdInOpHostCapacity(conn);
        updateGuestOsType(conn);
        updateNicsWithMode(conn);
        updateUserStatsWithNetwork(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        String file = Script.findScript("", "db/schema-222to224-cleanup.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-222to224-cleanup.sql");
        }

        return new File[] { new File(file) };
    }

    private void updateGuestOsType(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`guest_os` WHERE `display_name`='CentOS 5.3 (64-bit)'");
            ResultSet rs = pstmt.executeQuery();
            Long osId = null;
            if (rs.next()) {
                osId = rs.getLong(1);
            }

            if (osId != null) {
                pstmt = conn.prepareStatement("UPDATE `cloud`.`vm_template` SET `guest_os_id`=? WHERE id=2");
                pstmt.setLong(1, osId);
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update the guest os type for default template as a part of 222 to 224 upgrade", e);
        }
    }

    private void updateClusterIdInOpHostCapacity(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        PreparedStatement pstmtUpdate = null;
        try {
            // Host and Primary storage capacity types
            pstmt = conn.prepareStatement("SELECT host_id, capacity_type FROM op_host_capacity WHERE capacity_type IN (0,1,2,3)");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long hostId = rs.getLong(1);
                short capacityType = rs.getShort(2);
                String updateSQLPrefix = "Update op_host_capacity set cluster_id = (select cluster_id from ";
                String updateSQLSuffix = " where id = ? ) where host_id = ?";
                String tableName = "host";
                switch (capacityType) {
                case Capacity.CAPACITY_TYPE_MEMORY:
                case Capacity.CAPACITY_TYPE_CPU:
                    tableName = "host";
                    break;
                case Capacity.CAPACITY_TYPE_STORAGE:
                case Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED:
                    tableName = "storage_pool";
                    break;
                }
                pstmtUpdate = conn.prepareStatement(updateSQLPrefix + tableName + updateSQLSuffix);
                pstmtUpdate.setLong(1, hostId);
                pstmtUpdate.setLong(2, hostId);
                pstmtUpdate.executeUpdate();
                pstmtUpdate.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update the cluster Ids in Op_Host_capacity table", e);
        } finally {
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }

        }
    }

    private void updateNicsWithMode(Connection conn) {
        try {
            HashMap<Long, Long> nicNetworkMaps = new HashMap<Long, Long>();
            PreparedStatement pstmt = conn.prepareStatement("SELECT id, network_id FROM nics WHERE mode IS NULL");
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                nicNetworkMaps.put(rs.getLong(1), rs.getLong(2));
            }

            for (Long nic : nicNetworkMaps.keySet()) {
                pstmt = conn.prepareStatement("SELECT mode FROM networks WHERE id=?");
                pstmt.setLong(1, nicNetworkMaps.get(nic));
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    String mode = rs.getString(1);
                    pstmt = conn.prepareStatement("UPDATE nics SET mode=? where id=?");
                    pstmt.setString(1, mode);
                    pstmt.setLong(2, nic);
                    pstmt.executeUpdate();
                }
            }
            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update the Mode field for nics as a part of 222 to 224 upgrade", e);
        }
    }

    private void updateUserStatsWithNetwork(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id, device_id FROM user_statistics WHERE network_id=0 or network_id is NULL");
            s_logger.debug("Query is " + pstmt);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(1);
                Long instanceId = rs.getLong(2);

                pstmt = conn.prepareStatement("SELECT network_id FROM nics WHERE instance_id=? AND mode='Dhcp'");
                pstmt.setLong(1, instanceId);
                s_logger.debug("Query is " + pstmt);
                ResultSet rs1 = pstmt.executeQuery();

                if (!rs1.next()) {
                    throw new CloudRuntimeException("Failed to update user_statistics table as a part of 222 to 224 upgrade: couldn't get network_id from nics table");
                }

                Long networkId = rs1.getLong(1);

                if (networkId != null) {
                    pstmt = conn.prepareStatement("UPDATE user_statistics SET network_id=? where id=?");
                    pstmt.setLong(1, networkId);
                    pstmt.setLong(2, id);
                    pstmt.executeUpdate();
                }

            }

            s_logger.debug("Successfully update user_statistics table with network_ids as a part of 222 to 224 upgrade");
            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update the Mode field for nics as a part of 222 to 224 upgrade", e);
        }
    }

}
