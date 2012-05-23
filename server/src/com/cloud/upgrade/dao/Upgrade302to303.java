/*Copyright 2012 Citrix Systems, Inc. Licensed under the
Apache License, Version 2.0 (the "License"); you may not use this
file except in compliance with the License.  Citrix Systems, Inc.
reserves all rights not expressly granted by the License.
You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.*/


package com.cloud.upgrade.dao;

/**
 * @author Alena Prokharchyk
 */
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;
//
import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade302to303 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade302to303.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "3.0.2", "3.0.3" };
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

        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        setupExternalNetworkDevices(conn);
    }

    private void setupExternalNetworkDevices(Connection conn) {
        PreparedStatement dcSearchStmt, pNetworkStmt, devicesStmt = null;
        ResultSet dcResults, pNetworksResults, devicesResult = null;

        try {
        	dcSearchStmt = conn.prepareStatement("SELECT id, networktype FROM `cloud`.`data_center`");
        	dcResults = dcSearchStmt.executeQuery();
            while (dcResults.next()) {
                long zoneId = dcResults.getLong(1);
                long f5HostId = 0;
                long srxHostId = 0;

                String networkType = dcResults.getString(2);
                if (NetworkType.Advanced.toString().equalsIgnoreCase(networkType)) {

                    devicesStmt = conn.prepareStatement("SELECT id, type FROM host WHERE data_center_id=? AND type = 'ExternalLoadBalancer' OR type = 'ExternalFirewall'");
                    devicesStmt.setLong(1, zoneId);
                    devicesResult = devicesStmt.executeQuery();

                    while (devicesResult.next()) {
                        String device = devicesResult.getString(2);
                        if (device.equals("ExternalLoadBalancer")) {
                            f5HostId = devicesResult.getLong(1);
                        } else if (device.equals("ExternalFirewall")) {
                            srxHostId = devicesResult.getLong(1);
                        }
                    }

                    // check if the deployment had F5 and SRX devices
                    if (f5HostId != 0 && srxHostId != 0) {
                        pNetworkStmt = conn.prepareStatement("SELECT id FROM `cloud`.`physical_network` where data_center_id=?");
                        pNetworkStmt.setLong(1, zoneId);
                        pNetworksResults = pNetworkStmt.executeQuery();
                        if (pNetworksResults.first()) {
                            long physicalNetworkId = pNetworksResults.getLong(1);

                            // add F5BigIP provider and provider instance to physical network
                            addF5ServiceProvider(conn, physicalNetworkId, zoneId);
                            addF5LoadBalancer(conn, f5HostId, physicalNetworkId);

                            // add SRX provider and provider instance to physical network 
                            addSrxServiceProvider(conn, physicalNetworkId, zoneId);
                            addSrxFirewall(conn, srxHostId, physicalNetworkId);
                        }
                    }
                }
            }
            if (dcResults != null) {
                try {
                    dcResults.close();
                } catch (SQLException e) {
                }
            }
            if (dcSearchStmt != null) {
                try {
                    dcSearchStmt.close();
                } catch (SQLException e) {
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworks", e);
        } finally {

        }
    }

    private void addF5LoadBalancer(Connection conn, long hostId, long physicalNetworkId){
        // add traffic types
        PreparedStatement pstmtUpdate = null;
        try{
            s_logger.debug("Adding F5 Big IP load balancer with host id " + hostId);
            String insertF5 = "INSERT INTO `cloud`.`external_load_balancer_devices` (physical_network_id, host_id, provider_name, " +
                    "device_name, capacity, is_dedicated, device_state, allocation_state, is_inline, is_managed, uuid) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pstmtUpdate = conn.prepareStatement(insertF5);
            pstmtUpdate.setLong(1, physicalNetworkId);
            pstmtUpdate.setLong(2, hostId);
            pstmtUpdate.setString(3, "F5BigIp");
            pstmtUpdate.setString(4, "F5BigIp");
            pstmtUpdate.setLong(5, 0);
            pstmtUpdate.setBoolean(6, false);
            pstmtUpdate.setString(7, "Enabled");
            pstmtUpdate.setString(8, "Shared");
            pstmtUpdate.setBoolean(9, false);
            pstmtUpdate.setBoolean(10, false);
            pstmtUpdate.setString(11, UUID.randomUUID().toString());
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();
        }catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding F5 load balancer due to", e);
        } finally {
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private void addSrxFirewall(Connection conn, long hostId, long physicalNetworkId){
        // add traffic types
        PreparedStatement pstmtUpdate = null;
        try{
            s_logger.debug("Adding SRX firewall device with host id " + hostId);
            String insertSrx = "INSERT INTO `cloud`.`external_firewall_devices` (physical_network_id, host_id, provider_name, " +
                    "device_name, capacity, is_dedicated, device_state, allocation_state, uuid) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            pstmtUpdate = conn.prepareStatement(insertSrx);
            pstmtUpdate.setLong(1, physicalNetworkId);
            pstmtUpdate.setLong(2, hostId);
            pstmtUpdate.setString(3, "JuniperSRX");
            pstmtUpdate.setString(4, "JuniperSRX");
            pstmtUpdate.setLong(5, 0);
            pstmtUpdate.setBoolean(6, false);
            pstmtUpdate.setString(7, "Enabled");
            pstmtUpdate.setString(8, "Shared");
            pstmtUpdate.setString(9, UUID.randomUUID().toString());
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();
        }catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding F5 load balancer due to", e);
        } finally {
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private void addF5ServiceProvider(Connection conn, long physicalNetworkId, long zoneId){
        PreparedStatement pstmtUpdate = null;
        try{
            // add physical network service provider - F5BigIp
            s_logger.debug("Adding PhysicalNetworkServiceProvider F5BigIp");
            String insertPNSP = "INSERT INTO `cloud`.`physical_network_service_providers` (`uuid`, `physical_network_id` , `provider_name`, `state` ," +
                    "`destination_physical_network_id`, `vpn_service_provided`, `dhcp_service_provided`, `dns_service_provided`, `gateway_service_provided`," +
                    "`firewall_service_provided`, `source_nat_service_provided`, `load_balance_service_provided`, `static_nat_service_provided`," +
                    "`port_forwarding_service_provided`, `user_data_service_provided`, `security_group_service_provided`) VALUES (?,?,?,?,0,0,0,0,0,0,0,1,0,0,0,0)";

            pstmtUpdate = conn.prepareStatement(insertPNSP);
            pstmtUpdate.setString(1, UUID.randomUUID().toString());
            pstmtUpdate.setLong(2, physicalNetworkId);
            pstmtUpdate.setString(3, "F5BigIp");
            pstmtUpdate.setString(4, "Enabled");
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();
        }catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworkServiceProvider F5BigIp ", e);
        } finally {
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    private void addSrxServiceProvider(Connection conn, long physicalNetworkId, long zoneId){
        PreparedStatement pstmtUpdate = null;
        try{
            // add physical network service provider - JuniperSRX
            s_logger.debug("Adding PhysicalNetworkServiceProvider JuniperSRX");
            String insertPNSP = "INSERT INTO `cloud`.`physical_network_service_providers` (`uuid`, `physical_network_id` , `provider_name`, `state` ," +
                    "`destination_physical_network_id`, `vpn_service_provided`, `dhcp_service_provided`, `dns_service_provided`, `gateway_service_provided`," +
                    "`firewall_service_provided`, `source_nat_service_provided`, `load_balance_service_provided`, `static_nat_service_provided`," +
                    "`port_forwarding_service_provided`, `user_data_service_provided`, `security_group_service_provided`) VALUES (?,?,?,?,0,0,0,0,1,1,1,0,1,1,0,0)";

            pstmtUpdate = conn.prepareStatement(insertPNSP);
            pstmtUpdate.setString(1, UUID.randomUUID().toString());
            pstmtUpdate.setLong(2, physicalNetworkId);
            pstmtUpdate.setString(3, "JuniperSRX");
            pstmtUpdate.setString(4, "Enabled");
            pstmtUpdate.executeUpdate();
            pstmtUpdate.close();
        }catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworkServiceProvider JuniperSRX ", e);
        } finally {
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
}
