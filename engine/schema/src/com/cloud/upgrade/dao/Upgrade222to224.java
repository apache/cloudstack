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

import com.cloud.capacity.Capacity;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade222to224 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade222to224.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.2", "2.2.3"};
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

        return new File[] {new File(script)};
    }

    private void fixRelatedFkeyOnNetworksTable(Connection conn) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`networks` DROP FOREIGN KEY `fk_networks__related`");
        try {
            pstmt.executeUpdate();
        } catch (SQLException e) {
            s_logger.debug("Ignore if the key is not there.");
        }
        pstmt.close();

        pstmt =
            conn.prepareStatement("ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `fk_networks__related` FOREIGN KEY(`related`) REFERENCES `networks`(`id`) ON DELETE CASCADE");
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
            updateFkeysAndIndexes(conn);
            fixIPResouceCount(conn);
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

        return new File[] {new File(file)};
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
        PreparedStatement pstmt =
            conn.prepareStatement("UPDATE volumes as v SET recreatable=(SELECT recreatable FROM disk_offering d WHERE d.id = v.disk_offering_id) WHERE disk_offering_id != 0");
        pstmt.execute();
        pstmt.close();

        pstmt = conn.prepareStatement("UPDATE volumes SET recreatable=0 WHERE disk_offering_id is NULL or disk_offering_id=0");
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
                    s_logger.info("[ignored]",e);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    s_logger.info("[ignored]",e);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    s_logger.info("[ignored]",e);
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
            PreparedStatement pstmt =
                conn.prepareStatement("SELECT id, device_id FROM user_statistics WHERE network_id=0 or network_id is NULL and public_ip_address is NULL");
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
                        pstmt = conn.prepareStatement("UPDATE user_statistics SET network_id=?, device_type='DomainRouter' where id=?");
                        pstmt.setLong(1, networkId);
                        pstmt.setLong(2, id);
                        pstmt.executeUpdate();
                    }
                }
            }

            rs.close();
            pstmt.close();

            s_logger.debug("Upgraded user_statistics with networkId for DomainRouter device type");

            // update network_id information for ExternalFirewall and ExternalLoadBalancer device types
            PreparedStatement pstmt1 =
                conn.prepareStatement("update user_statistics us, user_ip_address uip set us.network_id = uip.network_id where us.public_ip_address = uip.public_ip_address "
                    + "and us.device_type in ('ExternalFirewall' , 'ExternalLoadBalancer')");
            pstmt1.executeUpdate();
            pstmt1.close();

            s_logger.debug("Upgraded user_statistics with networkId for ExternalFirewall and ExternalLoadBalancer device types");

            s_logger.debug("Successfully update user_statistics table with network_ids as a part of 222 to 224 upgrade");

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update user_statistics table with network_ids as a part of 222 to 224 upgrade", e);
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
                    s_logger.info("[ignored]",e);
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                    s_logger.info("[ignored]",e);
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                    s_logger.info("[ignored]",e);
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

    private void updateFkeysAndIndexes(Connection conn) throws SQLException {
        List<String> keysToAdd = new ArrayList<String>();
        List<String> indexesToAdd = new ArrayList<String>();
        List<String> keysToDrop = new ArrayList<String>();
        List<String> indexesToDrop = new ArrayList<String>();

        // populate indexes/keys to drop
        keysToDrop.add("ALTER TABLE `cloud`.`data_center` DROP FOREIGN KEY `fk_data_center__domain_id`");
        indexesToDrop.add("ALTER TABLE `cloud`.`data_center` DROP KEY `i_data_center__domain_id`");

        keysToDrop.add("ALTER TABLE `cloud`.`vlan` DROP FOREIGN KEY `fk_vlan__data_center_id`");
        keysToDrop.add("ALTER TABLE `cloud`.`op_dc_ip_address_alloc` DROP FOREIGN KEY `fk_op_dc_ip_address_alloc__data_center_id`");

        indexesToDrop.add("ALTER TABLE `cloud`.`networks` DROP FOREIGN KEY `fk_networks__network_offering_id`");
        indexesToDrop.add("ALTER TABLE `cloud`.`networks` DROP FOREIGN KEY `fk_networks__data_center_id`");
        indexesToDrop.add("ALTER TABLE `cloud`.`networks` DROP FOREIGN KEY `fk_networks__account_id`");
        indexesToDrop.add("ALTER TABLE `cloud`.`networks` DROP FOREIGN KEY `fk_networks__domain_id`");
        keysToDrop.add("ALTER TABLE `cloud`.`networks` DROP KEY `i_networks__removed`");

        // populate indexes/keys to add
        keysToAdd.add("ALTER TABLE `cloud`.`data_center` ADD CONSTRAINT `fk_data_center__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain`(`id`)");
        indexesToAdd.add("ALTER TABLE `cloud`.`data_center` ADD  INDEX `i_data_center__domain_id`(`domain_id`)");

        keysToAdd.add("ALTER TABLE `cloud`.`vlan` ADD CONSTRAINT `fk_vlan__data_center_id` FOREIGN KEY `fk_vlan__data_center_id`(`data_center_id`) REFERENCES `data_center`(`id`)");
        keysToAdd.add("ALTER TABLE `cloud`.`op_dc_ip_address_alloc` ADD CONSTRAINT `fk_op_dc_ip_address_alloc__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center`(`id`) ON DELETE CASCADE");

        keysToAdd.add("ALTER TABLE `cloud`.`networks` ADD INDEX `i_networks__removed` (`removed`)");

        indexesToAdd.add("ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `fk_networks__network_offering_id` FOREIGN KEY (`network_offering_id`) REFERENCES `network_offerings`(`id`)");
        indexesToAdd.add("ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `fk_networks__data_center_id` FOREIGN KEY (`data_center_id`) REFERENCES `data_center` (`id`)");
        indexesToAdd.add("ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `fk_networks__account_id` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)");
        indexesToAdd.add("ALTER TABLE `cloud`.`networks` ADD CONSTRAINT `fk_networks__domain_id` FOREIGN KEY (`domain_id`) REFERENCES `domain` (`id`)");

        // drop keys
        for (String key : keysToDrop) {
            PreparedStatement pstmt = conn.prepareStatement(key);
            try {
                pstmt.executeUpdate();
            } catch (SQLException e) {
                s_logger.debug("Ignore if the key is not there.");
            }
            pstmt.close();
        }

        // drop indexes
        for (String index : indexesToDrop) {
            PreparedStatement pstmt = conn.prepareStatement(index);
            try {
                pstmt.executeUpdate();
            } catch (SQLException e) {
                s_logger.debug("Ignore if the index is not there.");
            }
            pstmt.close();
        }

        // update indexes
        for (String index : indexesToAdd) {
            PreparedStatement pstmt = conn.prepareStatement(index);
            pstmt.executeUpdate();
            pstmt.close();
        }

        // update keys
        for (String key : keysToAdd) {
            PreparedStatement pstmt = conn.prepareStatement(key);
            pstmt.executeUpdate();
            pstmt.close();
        }
    }

    // In 2.2.x there was a bug when resource_count was incremented when Direct ip was allocated. Have to fix it during the
    // upgrade
    private void fixIPResouceCount(Connection conn) throws SQLException {
        // First set all public_ip fields to be 0
        PreparedStatement pstmt = conn.prepareStatement("UPDATE resource_count set count=0 where type='public_ip'");
        pstmt.executeUpdate();

        pstmt = conn.prepareStatement("SELECT id, account_id from resource_count where type='public_ip' and domain_id is NULL");
        ResultSet rs = pstmt.executeQuery();

        while (rs.next()) {
            // upgrade resource count for account
            Long countId = rs.getLong(1);
            Long accountId = rs.getLong(2);
            pstmt = conn.prepareStatement("SELECT count(*) from user_ip_address where network_id is not null and account_id=?");
            pstmt.setLong(1, accountId);
            ResultSet rs1 = pstmt.executeQuery();
            if (rs1.next()) {
                Long ipCount = rs1.getLong(1);
                if (ipCount.longValue() > 0) {
                    pstmt = conn.prepareStatement("UPDATE resource_count set count=? where id=?");
                    pstmt.setLong(1, ipCount);
                    pstmt.setLong(2, countId);
                    pstmt.executeUpdate();
                }
                rs1.close();
            }
        }
        rs.close();
        pstmt.close();

        // upgrade resource count for domain
        HashMap<Long, Long> domainIpsCount = new HashMap<Long, Long>();
        pstmt = conn.prepareStatement("SELECT account_id, count from resource_count where type='public_ip' and domain_id is NULL");
        rs = pstmt.executeQuery();
        while (rs.next()) {
            Long accountId = rs.getLong(1);
            Long count = rs.getLong(2);
            pstmt = conn.prepareStatement("SELECT domain_id from account where id=?");
            pstmt.setLong(1, accountId);
            ResultSet rs1 = pstmt.executeQuery();

            if (!rs1.next()) {
                throw new CloudRuntimeException("Unable to get domain information from account table as a part of resource_count table cleanup");
            }

            Long domainId = rs1.getLong(1);

            if (!domainIpsCount.containsKey(domainId)) {
                domainIpsCount.put(domainId, count);
            } else {
                long oldCount = domainIpsCount.get(domainId);
                long newCount = oldCount + count;
                domainIpsCount.put(domainId, newCount);
            }
            rs1.close();

            Long parentId = 0L;
            while (parentId != null) {
                pstmt = conn.prepareStatement("SELECT parent from domain where id=?");
                pstmt.setLong(1, domainId);
                ResultSet parentSet = pstmt.executeQuery();

                if (parentSet.next()) {
                    parentId = parentSet.getLong(1);
                    if (parentId == null || parentId.longValue() == 0) {
                        parentId = null;
                        continue;
                    }

                    if (!domainIpsCount.containsKey(parentId)) {
                        domainIpsCount.put(parentId, count);
                    } else {
                        long oldCount = domainIpsCount.get(parentId);
                        long newCount = oldCount + count;
                        domainIpsCount.put(parentId, newCount);
                    }
                    parentSet.close();
                    domainId = parentId;
                }
            }
        }

        rs.close();

        for (Long domainId : domainIpsCount.keySet()) {
            pstmt = conn.prepareStatement("UPDATE resource_count set count=? where domain_id=? and type='public_ip'");
            pstmt.setLong(1, domainIpsCount.get(domainId));
            pstmt.setLong(2, domainId);
            pstmt.executeUpdate();
        }

        pstmt.close();

        s_logger.debug("Resource limit is cleaned up successfully as a part of db upgrade");

    }
}
