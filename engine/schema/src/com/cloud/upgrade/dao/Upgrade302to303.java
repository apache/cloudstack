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

/**
 * @author Alena Prokharchyk
 */
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;

//
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade302to303 extends LegacyDbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade302to303.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"3.0.2", "3.0.3"};
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.3";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-302to303.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-302to303.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        setupExternalNetworkDevices(conn);
        encryptConfig(conn);
    }

    // upgrades deployment with F5 and SRX devices, to 3.0's Network offerings & service providers paradigm
    private void setupExternalNetworkDevices(Connection conn) {
        PreparedStatement zoneSearchStmt = null, pNetworkStmt = null, f5DevicesStmt = null, srxDevicesStmt = null;
        ResultSet zoneResults = null, pNetworksResults = null, f5DevicesResult = null, srxDevicesResult = null;

        try {
            zoneSearchStmt = conn.prepareStatement("SELECT id, networktype FROM `cloud`.`data_center`");
            zoneResults = zoneSearchStmt.executeQuery();
            while (zoneResults.next()) {
                long zoneId = zoneResults.getLong(1);
                String networkType = zoneResults.getString(2);

                if (!NetworkType.Advanced.toString().equalsIgnoreCase(networkType)) {
                    continue;
                }

                pNetworkStmt = conn.prepareStatement("SELECT id FROM `cloud`.`physical_network` where data_center_id=?");
                pNetworkStmt.setLong(1, zoneId);
                pNetworksResults = pNetworkStmt.executeQuery();
                while (pNetworksResults.next()) {
                    long physicalNetworkId = pNetworksResults.getLong(1);
                    PreparedStatement fetchF5NspStmt =
                        conn.prepareStatement("SELECT id from `cloud`.`physical_network_service_providers` where physical_network_id=" + physicalNetworkId +
                            " and provider_name = 'F5BigIp'");
                    ResultSet rsF5NSP = fetchF5NspStmt.executeQuery();
                    boolean hasF5Nsp = rsF5NSP.next();
                    fetchF5NspStmt.close();

                    if (!hasF5Nsp) {
                        f5DevicesStmt = conn.prepareStatement("SELECT id FROM host WHERE data_center_id=? AND type = 'ExternalLoadBalancer' AND removed IS NULL");
                        f5DevicesStmt.setLong(1, zoneId);
                        f5DevicesResult = f5DevicesStmt.executeQuery();

                        while (f5DevicesResult.next()) {
                            long f5HostId = f5DevicesResult.getLong(1);
                            ;
                            // add F5BigIP provider and provider instance to physical network
                            addF5ServiceProvider(conn, physicalNetworkId, zoneId);
                            addF5LoadBalancer(conn, f5HostId, physicalNetworkId);
                        }
                    }

                    PreparedStatement fetchSRXNspStmt =
                        conn.prepareStatement("SELECT id from `cloud`.`physical_network_service_providers` where physical_network_id=" + physicalNetworkId +
                            " and provider_name = 'JuniperSRX'");
                    ResultSet rsSRXNSP = fetchSRXNspStmt.executeQuery();
                    boolean hasSrxNsp = rsSRXNSP.next();
                    fetchSRXNspStmt.close();

                    if (!hasSrxNsp) {
                        srxDevicesStmt = conn.prepareStatement("SELECT id FROM host WHERE data_center_id=? AND type = 'ExternalFirewall' AND removed IS NULL");
                        srxDevicesStmt.setLong(1, zoneId);
                        srxDevicesResult = srxDevicesStmt.executeQuery();

                        while (srxDevicesResult.next()) {
                            long srxHostId = srxDevicesResult.getLong(1);
                            // add SRX provider and provider instance to physical network
                            addSrxServiceProvider(conn, physicalNetworkId, zoneId);
                            addSrxFirewall(conn, srxHostId, physicalNetworkId);
                        }
                    }
                }
            }
            closeAutoCloseable(zoneResults);
            closeAutoCloseable(zoneSearchStmt);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworks", e);
        }
    }

    private void addF5LoadBalancer(Connection conn, long hostId, long physicalNetworkId) {
        PreparedStatement pstmtUpdate = null;
        try {
            s_logger.debug("Adding F5 Big IP load balancer with host id " + hostId + " in to physical network" + physicalNetworkId);
            String insertF5 =
                "INSERT INTO `cloud`.`external_load_balancer_devices` (physical_network_id, host_id, provider_name, "
                    + "device_name, capacity, is_dedicated, device_state, allocation_state, is_inline, is_managed, uuid) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pstmtUpdate = conn.prepareStatement(insertF5);
            pstmtUpdate.setLong(1, physicalNetworkId);
            pstmtUpdate.setLong(2, hostId);
            pstmtUpdate.setString(3, "F5BigIp");
            pstmtUpdate.setString(4, "F5BigIpLoadBalancer");
            pstmtUpdate.setLong(5, 0);
            pstmtUpdate.setBoolean(6, false);
            pstmtUpdate.setString(7, "Enabled");
            pstmtUpdate.setString(8, "Shared");
            pstmtUpdate.setBoolean(9, false);
            pstmtUpdate.setBoolean(10, false);
            pstmtUpdate.setString(11, UUID.randomUUID().toString());
            pstmtUpdate.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding F5 load balancer device", e);
        } finally {
            closeAutoCloseable(pstmtUpdate);
        }
    }

    private void addSrxFirewall(Connection conn, long hostId, long physicalNetworkId) {
        PreparedStatement pstmtUpdate = null;
        try {
            s_logger.debug("Adding SRX firewall device with host id " + hostId + " in to physical network" + physicalNetworkId);
            String insertSrx =
                "INSERT INTO `cloud`.`external_firewall_devices` (physical_network_id, host_id, provider_name, "
                    + "device_name, capacity, is_dedicated, device_state, allocation_state, uuid) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pstmtUpdate = conn.prepareStatement(insertSrx);
            pstmtUpdate.setLong(1, physicalNetworkId);
            pstmtUpdate.setLong(2, hostId);
            pstmtUpdate.setString(3, "JuniperSRX");
            pstmtUpdate.setString(4, "JuniperSRXFirewall");
            pstmtUpdate.setLong(5, 0);
            pstmtUpdate.setBoolean(6, false);
            pstmtUpdate.setString(7, "Enabled");
            pstmtUpdate.setString(8, "Shared");
            pstmtUpdate.setString(9, UUID.randomUUID().toString());
            pstmtUpdate.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding SRX firewall device ", e);
        } finally {
            closeAutoCloseable(pstmtUpdate);
        }
    }

    private void addF5ServiceProvider(Connection conn, long physicalNetworkId, long zoneId) {
        PreparedStatement pstmtUpdate = null;
        try {
            // add physical network service provider - F5BigIp
            s_logger.debug("Adding PhysicalNetworkServiceProvider F5BigIp" + " in to physical network" + physicalNetworkId);
            String insertPNSP =
                "INSERT INTO `cloud`.`physical_network_service_providers` (`uuid`, `physical_network_id` , `provider_name`, `state` ,"
                    + "`destination_physical_network_id`, `vpn_service_provided`, `dhcp_service_provided`, `dns_service_provided`, `gateway_service_provided`,"
                    + "`firewall_service_provided`, `source_nat_service_provided`, `load_balance_service_provided`, `static_nat_service_provided`,"
                    + "`port_forwarding_service_provided`, `user_data_service_provided`, `security_group_service_provided`) VALUES (?,?,?,?,0,0,0,0,0,0,0,1,0,0,0,0)";

            pstmtUpdate = conn.prepareStatement(insertPNSP);
            pstmtUpdate.setString(1, UUID.randomUUID().toString());
            pstmtUpdate.setLong(2, physicalNetworkId);
            pstmtUpdate.setString(3, "F5BigIp");
            pstmtUpdate.setString(4, "Enabled");
            pstmtUpdate.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworkServiceProvider F5BigIp", e);
        } finally {
            closeAutoCloseable(pstmtUpdate);
        }
    }

    private void addSrxServiceProvider(Connection conn, long physicalNetworkId, long zoneId) {
        PreparedStatement pstmtUpdate = null;
        try {
            // add physical network service provider - JuniperSRX
            s_logger.debug("Adding PhysicalNetworkServiceProvider JuniperSRX");
            String insertPNSP =
                "INSERT INTO `cloud`.`physical_network_service_providers` (`uuid`, `physical_network_id` , `provider_name`, `state` ,"
                    + "`destination_physical_network_id`, `vpn_service_provided`, `dhcp_service_provided`, `dns_service_provided`, `gateway_service_provided`,"
                    + "`firewall_service_provided`, `source_nat_service_provided`, `load_balance_service_provided`, `static_nat_service_provided`,"
                    + "`port_forwarding_service_provided`, `user_data_service_provided`, `security_group_service_provided`) VALUES (?,?,?,?,0,0,0,0,1,1,1,0,1,1,0,0)";

            pstmtUpdate = conn.prepareStatement(insertPNSP);
            pstmtUpdate.setString(1, UUID.randomUUID().toString());
            pstmtUpdate.setLong(2, physicalNetworkId);
            pstmtUpdate.setString(3, "JuniperSRX");
            pstmtUpdate.setString(4, "Enabled");
            pstmtUpdate.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworkServiceProvider JuniperSRX", e);
        } finally {
            closeAutoCloseable(pstmtUpdate);
        }
    }

    private void encryptConfig(Connection conn) {
        //Encrypt config params and change category to Hidden
        s_logger.debug("Encrypting Config values");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt =
                conn.prepareStatement("select name, value from `cloud`.`configuration` where name in ('router.ram.size', 'secondary.storage.vm', 'security.hash.key') and category <> 'Hidden'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (value == null) {
                    continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement("update `cloud`.`configuration` set value=?, category = 'Hidden' where name=?");
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setString(2, name);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt configuration values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt configuration values ", e);
        } finally {
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
        }
        s_logger.debug("Done encrypting Config values");
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
}
