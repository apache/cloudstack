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
import java.sql.Statement;
import java.util.ArrayList;

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
                                         boolean dhcp_service) {
        String insertSql = 
            "INSERT INTO network_offerings (name, display_text, nw_rate, mc_rate, concurrent_connections, traffic_type, tags, system_only, specify_vlan, service_offering_id, created, removed, `default`, availability, dns_service, gateway_service, firewall_service, lb_service, userdata_service, vpn_service, dhcp_service) " +
                                   "VALUES (?,    ?,            NULL,    NULL,    NULL,                   ?,            NULL, ?,           0,            NULL,                now(),   NULL,    ?,       ?,            ?,           ?,               ?,                ?,          ?,                ?,           ?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
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
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            rs.next();
            long id = rs.getLong(1);
            rs.close();
            pstmt.close();
            return id;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert network offering ", e);
        }        
    }
    
    protected long insertNetwork(Connection conn, String name, String displayText, String trafficType, String broadcastDomainType, String broadcastUri,
                                 String gateway, String cidr, String mode, long networkOfferingId, long dataCenterId, String guruName,
                                 String state, long domainId, long accountId, String dns1, String dns2, String guestType, boolean shared,
                                 String networkDomain, boolean isDefault, String reservationId) {
        String getNextNetworkSequenceSql = "SELECT value from sequence where name='networks_seq'";
        String advanceNetworkSequenceSql = "UPDATE sequence set value=value+1 where name='networks_seq'";
        String insertNetworkSql = "INSERT INTO networks(id, name, display_text, traffic_type, broadcast_domain_type, gateway, cidr, mode, network_offering_id, data_center_id, guru_name, state, domain_id, account_id, dns1, dns2, guest_type, shared, is_default, created, network_domain, related, reservation_id, broadcast_uri) " + 
                                                "VALUES(?,  ?,    ?,            ?,            ?,                     ?,       ?,    ?,    ?,                   ?,              ?,         ?,     ?,         ?,          ?,    ?,    ?,          ?,      ?,          now(),   ?,              ?,       ?,              ?)"; 
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
            pstmt.setString(i++, reservationId);
            pstmt.setString(i++, broadcastUri);
            pstmt.executeUpdate();
            return seq;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create network", e);
        }
    }
    
    protected void upgradeUserIpAddress(Connection conn, long dcId, long networkId) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("UPDATE user_ip_address INNER JOIN vlan ON user_ip_address.vlan_db_id=vlan.id SET source_network_id=? WHERE user_ip_address.data_center_id=? AND vlan.vlan_type='VirtualNetwork'");
        pstmt.setLong(1, networkId);
        pstmt.setLong(2, dcId);
        pstmt.executeUpdate();
        pstmt.close();
        
        pstmt = conn.prepareStatement("UPDATE vlan SET network_id = ? WHERE data_center_id=? AND vlan_type='VirtualNetwork'");
        pstmt.setLong(1, networkId);
        pstmt.setLong(2, dcId);
        pstmt.executeUpdate();
        pstmt.close();
    }
            
    protected void upgradeDataCenter(Connection conn) {
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement("SELECT value FROM configuration WHERE name='direct.attach.untagged.vlan.enabled'");
            ResultSet rs = pstmt.executeQuery();
            _basicZone = !rs.next() || Boolean.parseBoolean(rs.getString(1));
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
            
            pstmt = conn.prepareStatement("SELECT id, guest_network_cidr FROM data_center");
            rs = pstmt.executeQuery();
            ArrayList<Object[]> dcs = new ArrayList<Object[]>();
            while (rs.next()) {
                Object[] dc = new Object[10];
                dc[0] = rs.getLong(1);    // data center id
                dc[1] = rs.getString(2);  // guest network cidr
                dcs.add(dc);
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
            
            for (Object[] dc : dcs) {
                Long dcId = (Long)dc[0];
                insertNetwork(conn, "ManagementNetwork" + dcId, "Management Network created for Zone " + dcId, "Management", "Native", null, null, null, "Static", managementNetworkOfferingId, dcId, "PodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
                insertNetwork(conn, "StorageNetwork" + dcId, "Storage Network created for Zone " + dcId, "Storage", "Native", null, null, null, "Static", storageNetworkOfferingId, dcId, "PodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
                insertNetwork(conn, "ControlNetwork" + dcId, "Control Network created for Zone " + dcId, "Control", "Native", null, null, null, "Static", controlNetworkOfferingId, dcId, "ControlNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
            }
            
            
            if (_basicZone) {
                for (Object[] dc : dcs) {
                    Long dcId = (Long)dc[0];
                    insertNetwork(conn, "BasicZoneDirectNetwork" + dcId, "Basic Zone Direct Network created for Zone " + dcId, "Guest", "Native", null, null, null, "Dhcp", 5, dcId, "DirectPodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
                }
                
            } else {
                for (Object[] dc : dcs) {
                    Long dcId = (Long)dc[0];
                    long publicNetworkId = insertNetwork(conn, "PublicNetwork" + dcId, "Public Network Created for Zone " + dcId, "Public", "Native", null, null, null, "Static", publicNetworkOfferingId, dcId, "PublicNetworkGuru", "Setup", 1,1, null, null, null, true, null, false, null);
                    
                    pstmt = conn.prepareStatement("SELECT vm_instance.id, vm_instance.domain_id, vm_instance.account_id, domain_router.guest_ip_address, domain_router.domain, domain_router.dns1, domain_router.dns2, domain_router.vnet FROM vm_instance INNER JOIN domain_router ON vm_instance.id=domain_router.id WHERE vm_instance.removed IS NULL AND vm_instance.type='DomainRouter' AND vm_instance.data_center_id=?");
                    pstmt.setLong(1, dcId);
                    rs = pstmt.executeQuery();
                    ArrayList<Object[]> routers = new ArrayList<Object[]>();
                    while (rs.next()) {
                        Object[] router = new Object[40];
                        router[0] = rs.getLong(1); // router id
                        router[1] = rs.getLong(2); // domain id
                        router[2] = rs.getLong(3); // account id
                        router[3] = rs.getString(4); // guest ip which becomes the gateway in network
                        router[4] = rs.getString(5); // domain name
                        router[5] = rs.getString(6); // dns1
                        router[6] = rs.getString(7); // dns2
                        router[7] = rs.getString(8); // vnet
                        routers.add(router);
                    }
                    rs.close();
                    pstmt.close();
                    
                    for (Object[] router : routers) {
                        String vnet = (String)router[7];
                        String reservationId = null;
                        String state = "Allocated";
                        if (vnet != null) {
                            reservationId = dcId + "-" + vnet;
                            vnet = "vlan://" + vnet;
                            state = "Implemented";
                        }
                        long virtualNetworkId = insertNetwork(conn, "VirtualNetwork" + router[0], "Virtual Network for " + router[0], "Guest", "Vlan", vnet, (String)router[3], (String)dc[1], "Dhcp", 6, dcId, "GuestNetworkGuru", state, (Long)router[1], (Long)router[2], (String)router[5], (String)router[6], "Virtual", false, (String)router[4], true, reservationId);
                        pstmt = conn.prepareStatement("UPDATE domain_router SET network_id = ? wHERE id = ? ");
                        pstmt.setLong(1, virtualNetworkId);
                        pstmt.setLong(2, (Long)router[0]);
                        pstmt.executeUpdate();
                        pstmt.close();
                    }
                    
                    upgradeUserIpAddress(conn, dcId, publicNetworkId);
                }
                
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't update data center ", e);
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
        return null;
        
//        File file = PropertiesUtil.findConfigFile("schema-21to22-cleanup.sql");
//        if (file == null) {
//            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22-cleanup.sql");
//        }
//        
//        return new File[] { file };
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
