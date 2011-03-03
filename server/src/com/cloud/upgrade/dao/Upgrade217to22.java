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
    
    protected long insertNetworkOffering(Connection conn, String name, String displayText, 
                                         String trafficType, boolean systemOnly, boolean defaultNetworkOffering,
                                         String availability, boolean dns_service, boolean gateway_service, 
                                         boolean firewall_service, boolean lb_service, 
                                         boolean userdata_service, boolean vpn_service, 
                                         boolean dhcp_service, String guest_type) {
        String insertSql = 
            "INSERT INTO network_offerings (name, display_text, nw_rate, mc_rate, concurrent_connections, traffic_type, tags, system_only, specify_vlan, service_offering_id, created, removed, default, availability, dns_service, gateway_service, firewall_service, lb_service, userdata_service, vpn_service, dhcp_service, guest_type) " +
                                   "VALUES (?,    ?,            NULL,    NULL,    NULL,                   ?,            NULL, ?,           0,            NULL,                now(),   NULL,    ?,       ?,            ?,           ?,               ?,                ?,          ?,                ?,           ?,            ?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(insertSql);
            int i = 1;
            pstmt.setString(i++, name);
            pstmt.setString(i++, displayText);
            pstmt.setString(i++, trafficType);
            pstmt.setBoolean(i++, systemOnly);
            pstmt.setBoolean(i++, defaultNetworkOffering);
            pstmt.setString(i++, availability);
            pstmt.setBoolean(i++, dns_service);
            pstmt.setBoolean(i++, gateway_service);
            pstmt.setBoolean(i++, firewall_service);
            pstmt.setBoolean(i++, lb_service);
            pstmt.setBoolean(i++, userdata_service);
            pstmt.setBoolean(i++, vpn_service);
            pstmt.setBoolean(i++, dhcp_service);
            pstmt.setString(i++, guest_type);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            rs.close();
            pstmt.close();
            long id = rs.getLong(1);
            return id;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert network offering ", e);
        }        
    }
    
    protected void insertNetwork(Connection conn, String name, String displayText, String trafficType, String broadcastDomainType, String broadcastUri,
                                 String gateway, String cidr, String mode, long networkOfferingId, long dataCenterId, String guruName,
                                 String state, long domainId, long accountId, String dns1, String dns2, String guestType, boolean shared,
                                 String networkDomain, boolean isDefault) {
        String getNextNetworkSequenceSql = "SELECT value from sequence where name='networks_seq'";
        String advanceNetworkSequenceSql = "UPDATE sequence set value=value+1 where name='networks_seq'";
        String insertNetworkSql = "INSERT INTO networks(id, name, display_text, traffic_type, broadcast_domain_type, gateway, cidr, mode, network_offering_id, data_center_id, guru_name, state, domain_id, account_id, dns1, dns2, guest_type, shared, is_default, created, network_domain, related) " + 
                                                "VALUES(?,  ?,    ?,            ?,            ?,                     ?,       ?,    ?,    ?,                   ?,              ?,         ?,     ?,         ?,          ?,    ?,    ?,          ?,      ?,          now(),   ?,              ?)"; 
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
            pstmt.setLong(i++, seq);
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
            
            pstmt = conn.prepareStatement("SELECT id FROM data_center");
            rs = pstmt.executeQuery();
            ArrayList<Long> dcs = new ArrayList<Long>();
            while (rs.next()) {
                dcs.add(rs.getLong(1));
            }
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement("SELECT id FROM network_offerings WHERE name='System-Management-Network'");
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new CloudRuntimeException("Unable to find the management network offering.");
            }
            long managementNetworkOfferingId = rs.getLong(1);
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement("SELECT id FROM network_offerings WHERE name='System-Public-Network'");
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new CloudRuntimeException("Unable to find the public network offering.");
            }
            long publicNetworkOfferingId = rs.getLong(1);
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement("SELECT id FROM network_offerings WHERE name='System-Control-Network'");
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new CloudRuntimeException("Unable to find the control network offering.");
            }
            long controlNetworkOfferingId = rs.getLong(1);
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement("SELECT id FROM network_offerings WHERE name='System-Storage-Network'");
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new CloudRuntimeException("Unable to find the storage network offering.");
            }
            long storageNetworkOfferingId = rs.getLong(1);
            rs.close();
            pstmt.close();
            
            for (Long dcId : dcs) {
                insertNetwork(conn, "ManagementNetwork" + dcId, "Management Network created for Zone " + dcId, "Management", "Native", null, null, null, "Static", managementNetworkOfferingId, dcId, "PodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false);
                insertNetwork(conn, "StorageNetwork" + dcId, "Storage Network created for Zone " + dcId, "Storage", "Native", null, null, null, "Static", storageNetworkOfferingId, dcId, "PodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false);
                insertNetwork(conn, "ControlNetwork" + dcId, "Control Network created for Zone " + dcId, "Control", "Native", null, null, null, "Static", publicNetworkOfferingId, dcId, "PublicNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false);
            }
            
            
            if (_basicZone) {
                long networkOfferingId = insertNetworkOffering(conn, "System-Guest-Network", "System-Guest-Network", "Guest", false, true, "Required", true, false, false, false, true, false, true, "Direct");
                for (Long dcId : dcs) {
//                  (200,NULL,NULL,'Management','Native',NULL,NULL,NULL,'Static',2,1,'PodBasedNetworkGuru','Setup',200,1,1,NULL,NULL,NULL,0,NULL,1,NULL,NULL,0,'2011-03-01 03:18:53',NULL,0),
//                  (201,NULL,NULL,'Control','LinkLocal',NULL,'169.254.0.1','169.254.0.0/16','Static',3,1,'ControlNetworkGuru','Setup',201,1,1,NULL,NULL,NULL,0,NULL,1,NULL,NULL,0,'2011-03-01 03:18:53',NULL,0),
//                  (202,NULL,NULL,'Storage','Native',NULL,NULL,NULL,'Static',4,1,'PodBasedNetworkGuru','Setup',202,1,1,NULL,NULL,NULL,0,NULL,1,NULL,NULL,0,'2011-03-01 03:18:53',NULL,0),
//                  (203,NULL,NULL,'Guest','Native',NULL,NULL,NULL,'Dhcp',5,1,'DirectPodBasedNetworkGuru','Setup',203,1,1,NULL,NULL,NULL,0,'Direct',1,NULL,NULL,1,'2011-03-01 03:18:53',NULL,1);
//                    (1,'System-Public-Network','System Offering for System-Public-Network',NULL,NULL,NULL,'Public',NULL,1,0,NULL,'2011-03-01 03:18:01',NULL,0,'Required',0,0,0,0,0,0,0,NULL),
//                    (2,'System-Management-Network','System Offering for System-Management-Network',NULL,NULL,NULL,'Management',NULL,1,0,NULL,'2011-03-01 03:18:01',NULL,0,'Required',0,0,0,0,0,0,0,NULL),
//                    (3,'System-Control-Network','System Offering for System-Control-Network',NULL,NULL,NULL,'Control',NULL,1,0,NULL,'2011-03-01 03:18:01',NULL,0,'Required',0,0,0,0,0,0,0,NULL),
//                    (4,'System-Storage-Network','System Offering for System-Storage-Network',NULL,NULL,NULL,'Storage',NULL,1,0,NULL,'2011-03-01 03:18:02',NULL,0,'Required',0,0,0,0,0,0,0,NULL),
//                    (5,'System-Guest-Network','System-Guest-Network',NULL,NULL,NULL,'Guest',NULL,1,0,NULL,'2011-03-01 03:18:02',NULL,1,'Required',1,0,0,0,1,0,1,'Direct'),
//                    (6,'DefaultVirtualizedNetworkOffering','Virtual Vlan',NULL,NULL,NULL,'Guest',NULL,0,0,NULL,'2011-03-01 03:18:02',NULL,1,'Required',1,1,1,1,1,1,1,'Virtual'),
//                    (7,'DefaultDirectNetworkOffering','Direct',NULL,NULL,NULL,'Guest',NULL,0,0,NULL,'2011-03-01 03:18:02',NULL,1,'Required',1,0,0,0,1,0,1,'Direct');
                    pstmt = conn.prepareStatement("INSERT INTO network_offerings VALUES ('System-Guest-Network','System-Guest-Network',NULL,NULL,NULL,'Guest',NULL,1,0,NULL,now(),NULL,1,'Required',1,0,0,0,1,0,1,'Direct')");
                    
                    insertNetwork(conn, "BasicZoneDirectNetwork" + dcId, "Basic Zone Direct Network created for Zone " + dcId, "Guest", "Native", null, null, null, "Dhcp", networkOfferingId, dcId, "DirectPodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false);
                }
                
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
