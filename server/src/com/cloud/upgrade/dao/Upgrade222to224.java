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
import java.util.ArrayList;
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

    private void fixRelatedFkeyOnNetworksTable(Connection conn) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`networks` DROP FOREIGN KEY `fk_networks__related`");
        try {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            s_logger.debug("Ignore if the key is not there.");
        }
        pstmt.close();

        pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `fk_networks__related` FOREIGN KEY(`related`) REFERENCES `networks`(`id`) ON DELETE CASCADE");
        pstmt.executeUpdate();
        pstmt.close();
    }

    @Override
    public void performDataMigration(Connection conn) {
        try {
        	checkForDuplicatePublicNetworks(conn);
            fixRelatedFkeyOnNetworksTable(conn);
            updateClusterIdInOpHostCapacity(conn);
            updateGuestOsType(conn);
            updateNicsWithMode(conn);
            updateUserStatsWithNetwork(conn);
            dropIndexIfExists(conn);
            fixBasicZoneNicCount(conn);
            updateTotalCPUInOpHostCapacity(conn);
            upgradeGuestOs(conn);
            fixRecreatableVolumesProblem(conn);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to perform data migration", e);
        }
    }    

    @Override
    public File[] getCleanupScripts() {
        String file = Script.findScript("", "db/schema-222to224-cleanup.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-222to224-cleanup.sql");
        }

        return new File[] { new File(file) };
    }
    
    private void checkForDuplicatePublicNetworks(Connection conn) {
    	try {
    		// There should be one public network per zone    		
    		PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`data_center`");
    		ResultSet zones = pstmt.executeQuery();    		
    		ArrayList<Long> zonesWithDuplicateNetworks = new ArrayList<Long>();
    		String errorMsg = "Found zones with duplicate public networks during 222 to 224 upgrade. Zone IDs: ";
    		long zoneId;
    		
    		while (zones.next()) {    			
    			zoneId = zones.getLong(1);
    			pstmt = conn.prepareStatement("SELECT count(*) FROM `cloud`.`networks` WHERE `networks`.`traffic_type`='Public' AND `data_center_id`=?");
    			pstmt.setLong(1, zoneId);
    			ResultSet rs = pstmt.executeQuery();
    			
    			if (rs.next()) {
    				long numNetworks = rs.getLong(1);
    				if (numNetworks > 1) {
    					zonesWithDuplicateNetworks.add(zoneId);
    				}    		
    			}
    		}
    		
    		if (zonesWithDuplicateNetworks.size() > 0) {
    			s_logger.warn(errorMsg + zonesWithDuplicateNetworks);
    		}
    		
    	} catch (SQLException e) {
    		s_logger.warn(e);
    		throw new CloudRuntimeException("Unable to check for duplicate public networks as part of 222 to 224 upgrade.");
    	}
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

    // fixes bug 9597
    private void fixRecreatableVolumesProblem(Connection conn) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("UPDATE volumes as v SET recreatable=(SELECT recreatable FROM disk_offering d WHERE d.id = v.disk_offering_id)");
        pstmt.execute();
        pstmt.close();

        pstmt = conn.prepareStatement("UPDATE volumes SET recreatable=0 WHERE disk_offering_id is NULL");
        pstmt.execute();
        pstmt.close();
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
            while (rs.next()) {
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
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(1);
                Long instanceId = rs.getLong(2);

                if (instanceId != null && instanceId.longValue() != 0) {
                    // Check if domR is already expunged; we shouldn't update user stats in this case as nics are gone too
                    pstmt = conn.prepareStatement("SELECT * from vm_instance where id=? and removed is not null");
                    pstmt.setLong(1, instanceId);
                    ResultSet rs1 = pstmt.executeQuery();

                    if (rs1.next()) {
                        s_logger.debug("Not updating user_statistics table for domR id=" + instanceId + " as domR is already expunged");
                        continue;
                    }

                    pstmt = conn.prepareStatement("SELECT network_id FROM nics WHERE instance_id=? AND mode='Dhcp'");
                    pstmt.setLong(1, instanceId);
                    ResultSet rs2 = pstmt.executeQuery();

                    if (!rs2.next()) {
                        throw new CloudRuntimeException("Failed to update user_statistics table as a part of 222 to 224 upgrade: couldn't get network_id from nics table");
                    }

                    Long networkId = rs2.getLong(1);

                    if (networkId != null) {
                        pstmt = conn.prepareStatement("UPDATE user_statistics SET network_id=? where id=?");
                        pstmt.setLong(1, networkId);
                        pstmt.setLong(2, id);
                        pstmt.executeUpdate();
                    }
                }
            }

            s_logger.debug("Successfully update user_statistics table with network_ids as a part of 222 to 224 upgrade");
            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update the Mode field for nics as a part of 222 to 224 upgrade", e);
        }
    }

    private void dropIndexIfExists(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SHOW INDEX FROM domain WHERE KEY_NAME = 'path'");
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`domain` DROP INDEX `path`");
                pstmt.executeUpdate();
                s_logger.debug("Unique key 'path' is removed successfully");
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to drop 'path' index for 'domain' table due to:", e);
        }
    }

    private void fixBasicZoneNicCount(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id from data_center where networktype='Basic'");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Long zoneId = rs.getLong(1);
                Long networkId = null;
                Long vmCount = 0L;
                s_logger.debug("Updating basic zone id=" + zoneId + " with correct nic count");

                pstmt = conn.prepareStatement("SELECT id from networks where data_center_id=? AND guest_type='Direct'");
                pstmt.setLong(1, zoneId);
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    networkId = rs.getLong(1);
                } else {
                    continue;
                }

                pstmt = conn.prepareStatement("SELECT count(*) from vm_instance where name like 'i-%' and (state='Running' or state='Starting' or state='Stopping')");
                rs = pstmt.executeQuery();

                if (rs.next()) {
                    vmCount = rs.getLong(1);
                }

                pstmt = conn.prepareStatement("UPDATE op_networks set nics_count=? where id=?");
                pstmt.setLong(1, vmCount);
                pstmt.setLong(2, networkId);
                pstmt.executeUpdate();

            }

            s_logger.debug("Basic zones are updated with correct nic counts successfully");
            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to drop 'path' index for 'domain' table due to:", e);
        }
    }

    private void updateTotalCPUInOpHostCapacity(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        PreparedStatement pstmtUpdate = null;
        try {
            // Load all Routing hosts
            s_logger.debug("Updating total CPU capacity entries in op_host_capacity");
            pstmt = conn.prepareStatement("SELECT id, cpus, speed FROM host WHERE type = 'Routing'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long hostId = rs.getLong(1);
                int cpus = rs.getInt(2);
                long speed = rs.getLong(3);

                long totalCapacity = cpus * speed;

                String updateSQL = "UPDATE op_host_capacity SET total_capacity = ? WHERE host_id = ? AND capacity_type = 1";
                pstmtUpdate = conn.prepareStatement(updateSQL);
                pstmtUpdate.setLong(1, totalCapacity);
                pstmtUpdate.setLong(2, hostId);
                pstmtUpdate.executeUpdate();
                pstmtUpdate.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update the total host CPU capacity in Op_Host_capacity table", e);
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

    private void upgradeGuestOs(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT * from guest_os WHERE id=138");
            ResultSet rs = pstmt.executeQuery();

            if (!rs.next()) {
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`guest_os` (id, category_id, display_name) VALUES (138, 7, 'None')");
                pstmt.executeUpdate();
                s_logger.debug("Inserted NONE category to guest_os table");
            }

            rs.close();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unalbe to insert NONE guest category to guest_os table due to:", e);
        }
    }

}
