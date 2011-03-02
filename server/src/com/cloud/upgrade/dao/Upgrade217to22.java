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

import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade217to22 implements DbUpgrade {
    boolean _basicZone;

    @Override
    public File[] getPrepareScripts() {
        File file = PropertiesUtil.findConfigFile("schema-21to22.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22.sql");
        }
        
        return new File[] {file};
    }
    
    protected void upgradeStoragePools(Connection conn) {
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement("UPDATE storage_pool SET status='Up'");
            pstmt.executeUpdate();
            pstmt.close();
        } catch(SQLException e) {
            throw new CloudRuntimeException("Can't upgrade storage pool ", e);
        }
        
    }
    
    protected void insertNetwork(Connection conn, String name, String displayText, String trafficType, String broadcastDomainType, String broadcastUri,
                                 String gateway, String cidr, String mode, long networkOfferingId, long dataCenterId, String guruName,
                                 String state, long domainId, long accountId, String dns1, String dns2, String guestType, boolean shared,
                                 String networkDomain, boolean isDefault) {
        String getNextNetworkSequenceSql = "SELECT value from sequence where name='networks_seq'";
        String advanceNetworkSequenceSql = "UPDATE sequence set value=value+1 where name='networks_seq'";
        String insertNetworkSql = "INSERT INTO NETWORKS(id, name, display_text, traffic_type, broadcast_domain_type, gateway, cidr, mode, network_offering_id, data_center_id, guru_name, state, domain_id, account_id, dns1, dns2, guest_type, shared, is_default, created, network_domain) " + 
                                                "VALUES(?,  ?,    ?,            ?,            ?,                     ?,       ?,    ?,    ?,                   ?,              ?,         ?,     ?,         ?,          ?,    ?,    ,          false,  true,       now(), ?)"; 
        try {
            PreparedStatement pstmt = conn.prepareStatement(getNextNetworkSequenceSql);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            long seq = rs.getLong(1);
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement(advanceNetworkSequenceSql);
            pstmt.executeUpdate();
            pstmt.close();
            
            pstmt = conn.prepareStatement(insertNetworkSql);
            int i = 1;
            pstmt.setLong(i++, seq);
            pstmt.setString(i++, name);
            pstmt.setString(i++, displayText);
            pstmt.setString(i++, trafficType);
            pstmt.setString(i++, broadcastDomainType);
            pstmt.setString(i++, gateway);
            pstmt.setString(i++, cidr);
            pstmt.setString(i++, mode);
            pstmt.setLong(i++, networkOfferingId);
            pstmt.setLong(i++, dataCenterId);
            pstmt.setString(i++, guruName);
            pstmt.setString(i++, state);
            pstmt.setLong(i++, domainId);
            pstmt.setLong(i++, accountId);
            pstmt.setString(i++, dns1);
            pstmt.setString(i++, dns2);
            pstmt.setString(i++, guestType);
            pstmt.setBoolean(i++, shared);
            pstmt.setBoolean(i++, isDefault);
            pstmt.setString(i++, networkDomain);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create network", e);
        }
    }
            
    protected void upgradeDataCenter(Connection conn) {
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement("SELECT value FROM configuration WHERE name='direct.attach.untagged.vlan.enabled'");
            ResultSet rs = pstmt.executeQuery();
            _basicZone = !(rs.next() && Boolean.parseBoolean(rs.getString(1)));
            rs.close();
            pstmt.close();
            pstmt = conn.prepareStatement("UPDATE data_center SET networktype=?, dns_provider=?, gateway_provider=?, firewall_provider=?, dhcp_provider=?, lb_provider=?, vpn_provider=?, userdata_provider=?");
            if (_basicZone) {
                pstmt.setString(1, "Basic");
                pstmt.setString(2, "DhcpServer");
                pstmt.setString(3, null);
                pstmt.setString(4, null);
                pstmt.setString(5, "DhcpServer");
                pstmt.setString(6, null);
                pstmt.setString(7, null);
                pstmt.setString(8, "DhcpServer");
            } else {
                pstmt.setString(1, "Advanced");
                pstmt.setString(2, "VirtualRouter");
                pstmt.setString(3, "VirtualRouter");
                pstmt.setString(4, "VirtualRouter");
                pstmt.setString(5, "VirtualRouter");
                pstmt.setString(6, "VirtualRouter");
                pstmt.setString(7, "VirtualRouter");
                pstmt.setString(8, "VirtualRouter");
            }
            pstmt.executeUpdate();
            pstmt.close();
            
            if (_basicZone) {
//              CREATE TABLE `networks` (
//              `id` bigint(20) unsigned NOT NULL AUTO_INCREMENT COMMENT 'id',
//              `name` varchar(255) DEFAULT NULL COMMENT 'name for this network',
//              `display_text` varchar(255) DEFAULT NULL COMMENT 'display text for this network',
//              `traffic_type` varchar(32) NOT NULL COMMENT 'type of traffic going through this network',
//              `broadcast_domain_type` varchar(32) NOT NULL COMMENT 'type of broadcast domain used',
//              `broadcast_uri` varchar(255) DEFAULT NULL COMMENT 'broadcast domain specifier',
//              `gateway` varchar(15) DEFAULT NULL COMMENT 'gateway for this network configuration',
//              `cidr` varchar(18) DEFAULT NULL COMMENT 'network cidr',
//              `mode` varchar(32) DEFAULT NULL COMMENT 'How to retrieve ip address in this network',
//              `network_offering_id` bigint(20) unsigned NOT NULL COMMENT 'network offering id that this configuration is created from',
//              `data_center_id` bigint(20) unsigned NOT NULL COMMENT 'data center id that this configuration is used in',
//              `guru_name` varchar(255) NOT NULL COMMENT 'who is responsible for this type of network configuration',
//              `state` varchar(32) NOT NULL COMMENT 'what state is this configuration in',
//              `related` bigint(20) unsigned NOT NULL COMMENT 'related to what other network configuration',
//              `domain_id` bigint(20) unsigned NOT NULL COMMENT 'foreign key to domain id',
//              `account_id` bigint(20) unsigned NOT NULL COMMENT 'owner of this network',
//              `dns1` varchar(255) DEFAULT NULL COMMENT 'comma separated DNS list',
//              `dns2` varchar(255) DEFAULT NULL COMMENT 'comma separated DNS list',
//              `guru_data` varchar(1024) DEFAULT NULL COMMENT 'data stored by the netwolrk guru that setup this network',
//              `set_fields` bigint(20) unsigned NOT NULL DEFAULT '0' COMMENT 'which fields are set already',
//              `guest_type` char(32) DEFAULT NULL COMMENT 'type of guest network',
//              `shared` int(1) unsigned NOT NULL DEFAULT '0' COMMENT '0 if network is shared, 1 if network dedicated',
//              `network_domain` varchar(255) DEFAULT NULL COMMENT 'domain',
//              `reservation_id` char(40) DEFAULT NULL COMMENT 'reservation id',
//              `is_default` int(1) unsigned NOT NULL DEFAULT '0' COMMENT '1 if network is default',
//              `created` datetime NOT NULL COMMENT 'date created',
//              `removed` datetime DEFAULT NULL COMMENT 'date removed if not null',
//              `is_security_group_enabled` tinyint(4) NOT NULL DEFAULT '0' COMMENT '1: enabled, 0: not',
//      (200,NULL,NULL,'Management','Native',NULL,NULL,NULL,'Static',2,1,'PodBasedNetworkGuru','Setup',200,1,1,NULL,NULL,NULL,0,NULL,1,NULL,NULL,0,now(),NULL,0)
                
//                insertNetwork(conn, "BasicZoneNetwork", "Network created for Basic Zone " + dataCenterId, "Management", "Native", null, null, null, "Static", 2, 1, )
            } else {
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't update data center ", e);
        }
    }
    
    protected void upgradeNetworks(Connection conn) {
        String getAccountsSql = "SELECT id, domain_id FROM accounts WHERE removed IS NULL AND id > 1";
        String getDataCenterSql = "SELECT id FROM data_center";
        String getNextNetworkSequenceSql = "SELECT value from sequence where name='networks_seq'";
        String advanceNetworkSequenceSql = "UPDATE sequence set value=value+1 where name='networks_seq'";
        String insertNetworkSql = "INSERT INTO NETWORKS(id, name, display_text, traffic_type, broadcast_domain_type, gateway, cidr, mode, network_offering_id, data_center_id, guru_name, state, domain_id, account_id, dns1, dns2, guest_type, shared, is_default, created) " + 
                                                "VALUES(?,  ?,    ?,            ?,            ?,                     ?,       ?,    ?,    ?,                   ?,              ?,         ?,     ?,         ?,          ?,    ?,    ,          false,  true,       now())"; 
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement(getAccountsSql);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<Pair<Long, Long>> accountIds = new ArrayList<Pair<Long, Long>>();
            while (rs.next()) {
                accountIds.add(new Pair<Long, Long>(rs.getLong(1), rs.getLong(2)));
            }
            rs.close();
            pstmt.close();
            pstmt = conn.prepareStatement(getDataCenterSql);
            rs = pstmt.executeQuery();
            ArrayList<Long> dataCenterIds = new ArrayList<Long>();
            while (rs.next()) {
                dataCenterIds.add(rs.getLong(1));
            }
            rs.close();
            pstmt.close();
            
            for (Pair<Long, Long> accountId : accountIds) {
                for (Long dataCenterId : dataCenterIds) {
                    pstmt = conn.prepareStatement(getNextNetworkSequenceSql);
                    rs = pstmt.executeQuery();
                    rs.next();
                    long seq = rs.getLong(1);
                    rs.close();
                    pstmt.close();
                    
                    pstmt = conn.prepareStatement(advanceNetworkSequenceSql);
                    pstmt.executeUpdate();
                    pstmt.close();
                    
                    pstmt = conn.prepareStatement(insertNetworkSql);
                }
            }
            
            
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to perform upgrade", e);
        }
    }
    
    @Override
    public void performDataMigration(Connection conn) {
        upgradeDataCenter(conn);
    //    upgradeNetworks(conn);
        upgradeStoragePools(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        File file = PropertiesUtil.findConfigFile("schema-21to22-cleanup.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22-cleanup.sql");
        }
        
        return new File[] { file };
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.1.7", "2.1.7" };
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.0";
    }
    
    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }
}
