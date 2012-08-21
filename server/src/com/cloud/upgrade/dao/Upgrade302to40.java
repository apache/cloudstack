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
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade302to40 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade302to40.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "3.0.2", "4.0.0" };
    }

    @Override
    public String getUpgradedVersion() {
        return "4.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-302to40.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-302to40.sql");
        }

        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        addVpcProvider(conn);
        updateRouterNetworkRef(conn);
        fixForeignKeys(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-302to40-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-302to40-cleanup.sql");
        }

        return new File[] { new File(script) };
    }
    
    private void addVpcProvider(Connection conn){
        //Encrypt config params and change category to Hidden
        s_logger.debug("Adding vpc provider to all physical networks in the system");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`physical_network` WHERE removed is NULL");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Long pNtwkId = rs.getLong(1);
                
                //insert provider
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`physical_network_service_providers` " +
                        "(`physical_network_id`, `provider_name`, `state`, `vpn_service_provided`, `dhcp_service_provided`, " +
                        "`dns_service_provided`, `gateway_service_provided`, `firewall_service_provided`, `source_nat_service_provided`," +
                        " `load_balance_service_provided`, `static_nat_service_provided`, `port_forwarding_service_provided`," +
                        " `user_data_service_provided`, `security_group_service_provided`) " +
                        "VALUES (?, 'VpcVirtualRouter', 'Enabled', 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0)");
                
                pstmt.setLong(1, pNtwkId);
                pstmt.executeUpdate();
                
                //get provider id
                pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`physical_network_service_providers` " +
                        "WHERE physical_network_id=? and provider_name='VpcVirtualRouter'");
                pstmt.setLong(1, pNtwkId);
                ResultSet rs1 = pstmt.executeQuery();
                rs1.next();
                long providerId = rs1.getLong(1);
                
                //insert VR element
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`virtual_router_providers` (`nsp_id`, `type`, `enabled`) " +
                        "VALUES (?, 'VPCVirtualRouter', 1)");
                pstmt.setLong(1, providerId);
                pstmt.executeUpdate();
                
                s_logger.debug("Added VPC Virtual router provider for physical network id=" + pNtwkId);
                
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable add VPC physical network service provider ", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Done adding VPC physical network service providers to all physical networks");        
    }
    
    private void updateRouterNetworkRef(Connection conn){
        //Encrypt config params and change category to Hidden
        s_logger.debug("Updating router network ref");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("SELECT d.id, d.network_id FROM `cloud`.`domain_router` d, `cloud`.`vm_instance` v " +
                    "WHERE d.id=v.id AND v.removed is NULL");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Long routerId = rs.getLong(1);
                Long networkId = rs.getLong(2);
                
                //get the network type
                pstmt = conn.prepareStatement("SELECT guest_type from `cloud`.`networks` where id=?");
                pstmt.setLong(1, networkId);
                ResultSet rs1 = pstmt.executeQuery();
                rs1.next();
                String networkType = rs1.getString(1);
                
                //insert the reference
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`router_network_ref` (router_id, network_id, guest_type) " +
                        "VALUES (?, ?, ?)");
                
                pstmt.setLong(1, routerId);
                pstmt.setLong(2, networkId);
                pstmt.setString(3, networkType);
                pstmt.executeUpdate();

                s_logger.debug("Added reference for router id=" + routerId + " and network id=" + networkId);
                
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to update the router/network reference ", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Done updating router/network references");        
    }

    
    private void fixForeignKeys(Connection conn) {
        //Drop the keys (if exist)
        List<String> keys = new ArrayList<String>();
        keys.add("fk_ssh_keypair__account_id");
        keys.add("fk_ssh_keypair__domain_id");
        keys.add("fk_ssh_keypairs__account_id");
        keys.add("fk_ssh_keypairs__domain_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "ssh_keypairs", keys, true);
        
        keys = new ArrayList<String>();
        keys.add("fk_ssh_keypair__account_id");
        keys.add("fk_ssh_keypair__domain_id");
        keys.add("fk_ssh_keypairs__account_id");
        keys.add("fk_ssh_keypairs__domain_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "ssh_keypairs", keys, false);
        
        //insert the keys anew
        try {
            PreparedStatement pstmt; pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD " +
                    "CONSTRAINT `fk_ssh_keypair__account_id` FOREIGN KEY `fk_ssh_keypair__account_id` (`account_id`)" +
                    " REFERENCES `account` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute ssh_keypairs table update for adding account_id foreign key", e);
        }
            
        try {
            PreparedStatement pstmt; pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT" +
                    " `fk_ssh_keypair__domain_id` FOREIGN KEY `fk_ssh_keypair__domain_id` (`domain_id`) " +
                    "REFERENCES `domain` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute ssh_keypairs table update for adding domain_id foreign key", e);
        }
    }
}
