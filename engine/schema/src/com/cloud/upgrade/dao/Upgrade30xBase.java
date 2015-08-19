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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public abstract class Upgrade30xBase extends LegacyDbUpgrade {

    final static Logger s_logger = Logger.getLogger(Upgrade30xBase.class);

    protected String getNetworkLabelFromConfig(Connection conn, String name) {
        String sql = "SELECT value FROM `cloud`.`configuration` where name = ?";
        String networkLabel = null;
        try (PreparedStatement pstmt = conn.prepareStatement(sql);) {
            pstmt.setString(1,name);
            try (ResultSet rs = pstmt.executeQuery();) {
                if (rs.next()) {
                    networkLabel = rs.getString(1);
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to fetch network label from configuration", e);
        }
        return networkLabel;
    }

    protected long addPhysicalNetworkToZone(Connection conn, long zoneId, String zoneName, String networkType, String vnet, Long domainId) {

        String getNextNetworkSequenceSql = "SELECT value from `cloud`.`sequence` where name='physical_networks_seq'";
        String advanceNetworkSequenceSql = "UPDATE `cloud`.`sequence` set value=value+1 where name='physical_networks_seq'";
        PreparedStatement pstmtUpdate = null, pstmt2 = null;
        // add p.network
        try {
            pstmt2 = conn.prepareStatement(getNextNetworkSequenceSql);

            ResultSet rsSeq = pstmt2.executeQuery();
            rsSeq.next();

            long physicalNetworkId = rsSeq.getLong(1);
            rsSeq.close();
            pstmt2.close();
            pstmt2 = conn.prepareStatement(advanceNetworkSequenceSql);
            pstmt2.executeUpdate();
            pstmt2.close();

            String uuid = UUID.randomUUID().toString();
            String broadcastDomainRange = "POD";
            if ("Advanced".equals(networkType)) {
                broadcastDomainRange = "ZONE";
            }

            s_logger.debug("Adding PhysicalNetwork " + physicalNetworkId + " for Zone id " + zoneId);
            String sql = "INSERT INTO `cloud`.`physical_network` (id, uuid, data_center_id, vnet, broadcast_domain_range, state, name) VALUES (?,?,?,?,?,?,?)";

            pstmtUpdate = conn.prepareStatement(sql);
            pstmtUpdate.setLong(1, physicalNetworkId);
            pstmtUpdate.setString(2, uuid);
            pstmtUpdate.setLong(3, zoneId);
            pstmtUpdate.setString(4, vnet);
            pstmtUpdate.setString(5, broadcastDomainRange);
            pstmtUpdate.setString(6, "Enabled");
            zoneName = zoneName + "-pNtwk" + physicalNetworkId;
            pstmtUpdate.setString(7, zoneName);
            s_logger.warn("Statement is " + pstmtUpdate.toString());
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();

            if (domainId != null && domainId.longValue() != 0) {
                s_logger.debug("Updating domain_id for physical network id=" + physicalNetworkId);
                sql = "UPDATE `cloud`.`physical_network` set domain_id=? where id=?";
                pstmtUpdate = conn.prepareStatement(sql);
                pstmtUpdate.setLong(1, domainId);
                pstmtUpdate.setLong(2, physicalNetworkId);
                pstmtUpdate.executeUpdate();
                pstmtUpdate.close();
            }

            return physicalNetworkId;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworks", e);
        } finally {
            closeAutoCloseable(pstmt2);
            closeAutoCloseable(pstmtUpdate);
        }
    }

    protected void addTrafficType(Connection conn, long physicalNetworkId, String trafficType, String xenPublicLabel, String kvmPublicLabel, String vmwarePublicLabel) {
        // add traffic types
        PreparedStatement pstmtUpdate = null;
        try {
            s_logger.debug("Adding PhysicalNetwork traffic types");
            String insertTraficType =
                "INSERT INTO `cloud`.`physical_network_traffic_types` (physical_network_id, traffic_type, xen_network_label, kvm_network_label, vmware_network_label, uuid) VALUES ( ?, ?, ?, ?, ?, ?)";
            pstmtUpdate = conn.prepareStatement(insertTraficType);
            pstmtUpdate.setLong(1, physicalNetworkId);
            pstmtUpdate.setString(2, trafficType);
            pstmtUpdate.setString(3, xenPublicLabel);
            pstmtUpdate.setString(4, kvmPublicLabel);
            pstmtUpdate.setString(5, vmwarePublicLabel);
            pstmtUpdate.setString(6, UUID.randomUUID().toString());
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworks", e);
        } finally {
            closeAutoCloseable(pstmtUpdate);
        }
    }

    protected void addDefaultSGProvider(Connection conn, long physicalNetworkId, long zoneId, String networkType, boolean is304) {
        PreparedStatement pstmtUpdate = null, pstmt2 = null;
        try {
            //add security group service provider (if security group service is enabled for at least one guest network)
            boolean isSGServiceEnabled = false;
            String selectSG = "";

            if (is304) {
                selectSG =
                    "SELECT nm.* FROM `cloud`.`ntwk_service_map` nm JOIN `cloud`.`networks` n ON nm.network_id = n.id where n.data_center_id = ? and nm.service='SecurityGroup'";
            } else {
                selectSG = "SELECT * from `cloud`.`networks` where is_security_group_enabled=1 and data_center_id=?";
            }

            pstmt2 = conn.prepareStatement(selectSG);
            pstmt2.setLong(1, zoneId);
            ResultSet sgDcSet = pstmt2.executeQuery();
            if (sgDcSet.next()) {
                isSGServiceEnabled = true;
            }
            sgDcSet.close();
            pstmt2.close();

            if (isSGServiceEnabled) {
                s_logger.debug("Adding PhysicalNetworkServiceProvider SecurityGroupProvider to the physical network id=" + physicalNetworkId);
                String insertPNSP =
                    "INSERT INTO `cloud`.`physical_network_service_providers` (`uuid`, `physical_network_id` , `provider_name`, `state` ,"
                        + "`destination_physical_network_id`, `vpn_service_provided`, `dhcp_service_provided`, `dns_service_provided`, `gateway_service_provided`,"
                        + "`firewall_service_provided`, `source_nat_service_provided`, `load_balance_service_provided`, `static_nat_service_provided`,"
                        + "`port_forwarding_service_provided`, `user_data_service_provided`, `security_group_service_provided`) VALUES (?,?,?,?,0,0,0,0,0,0,0,0,0,0,0,1)";
                pstmtUpdate = conn.prepareStatement(insertPNSP);
                pstmtUpdate.setString(1, UUID.randomUUID().toString());
                pstmtUpdate.setLong(2, physicalNetworkId);
                pstmtUpdate.setString(3, "SecurityGroupProvider");
                pstmtUpdate.setString(4, "Enabled");

                pstmtUpdate.executeUpdate();
                pstmtUpdate.close();
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding default Security Group Provider", e);
        } finally {
            closeAutoCloseable(pstmt2);
            closeAutoCloseable(pstmtUpdate);
        }
    }

    protected void addDefaultVRProvider(Connection conn, long physicalNetworkId, long zoneId) {
        PreparedStatement pstmtUpdate = null, pstmt2 = null;
        try {
            // add physical network service provider - VirtualRouter
            s_logger.debug("Adding PhysicalNetworkServiceProvider VirtualRouter");
            String insertPNSP =
                "INSERT INTO `cloud`.`physical_network_service_providers` (`uuid`, `physical_network_id` , `provider_name`, `state` ,"
                    + "`destination_physical_network_id`, `vpn_service_provided`, `dhcp_service_provided`, `dns_service_provided`, `gateway_service_provided`,"
                    + "`firewall_service_provided`, `source_nat_service_provided`, `load_balance_service_provided`, `static_nat_service_provided`,"
                    + "`port_forwarding_service_provided`, `user_data_service_provided`, `security_group_service_provided`) VALUES (?,?,?,?,0,1,1,1,1,1,1,1,1,1,1,0)";

            String routerUUID = UUID.randomUUID().toString();
            pstmtUpdate = conn.prepareStatement(insertPNSP);
            pstmtUpdate.setString(1, routerUUID);
            pstmtUpdate.setLong(2, physicalNetworkId);
            pstmtUpdate.setString(3, "VirtualRouter");
            pstmtUpdate.setString(4, "Enabled");
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();

            // add virtual_router_element
            String fetchNSPid =
                "SELECT id from `cloud`.`physical_network_service_providers` where physical_network_id=" + physicalNetworkId +
                    " AND provider_name = 'VirtualRouter' AND uuid = ?";
            pstmt2 = conn.prepareStatement(fetchNSPid);
            pstmt2.setString(1, routerUUID);
            ResultSet rsNSPid = pstmt2.executeQuery();
            rsNSPid.next();
            long nspId = rsNSPid.getLong(1);
            pstmt2.close();

            String insertRouter = "INSERT INTO `cloud`.`virtual_router_providers` (`nsp_id`, `uuid` , `type` , `enabled`) " + "VALUES (?,?,?,?)";
            pstmtUpdate = conn.prepareStatement(insertRouter);
            pstmtUpdate.setLong(1, nspId);
            pstmtUpdate.setString(2, UUID.randomUUID().toString());
            pstmtUpdate.setString(3, "VirtualRouter");
            pstmtUpdate.setInt(4, 1);
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworks", e);
        } finally {
            closeAutoCloseable(pstmt2);
            closeAutoCloseable(pstmtUpdate);
        }
    }

    protected void addPhysicalNtwk_To_Ntwk_IP_Vlan(Connection conn, long physicalNetworkId, long networkId) {
        PreparedStatement pstmtUpdate = null;
        try {
            // add physicalNetworkId to vlan for this zone
            String updateVLAN = "UPDATE `cloud`.`vlan` SET physical_network_id = " + physicalNetworkId + " WHERE network_id = " + networkId;
            pstmtUpdate = conn.prepareStatement(updateVLAN);
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();

            // add physicalNetworkId to user_ip_address for this zone
            String updateUsrIp = "UPDATE `cloud`.`user_ip_address` SET physical_network_id = " + physicalNetworkId + " WHERE source_network_id = " + networkId;
            pstmtUpdate = conn.prepareStatement(updateUsrIp);
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();

            // add physicalNetworkId to guest networks for this zone
            String updateNet = "UPDATE `cloud`.`networks` SET physical_network_id = " + physicalNetworkId + " WHERE id = " + networkId + " AND traffic_type = 'Guest'";
            pstmtUpdate = conn.prepareStatement(updateNet);
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworks", e);
        } finally {
            closeAutoCloseable(pstmtUpdate);
        }

    }

}
