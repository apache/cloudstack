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
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade304to305 extends Upgrade30xBase {
    final static Logger s_logger = Logger.getLogger(Upgrade304to305.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"3.0.4", "3.0.5"};
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.5";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-304to305.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-304to305.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        addHostDetailsUniqueKey(conn);
        addVpcProvider(conn);
        updateRouterNetworkRef(conn);
        fixZoneUsingExternalDevices(conn);
//        updateSystemVms(conn);
        fixForeignKeys(conn);
        encryptClusterDetails(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-304to305-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-304to305-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

    private void updateSystemVms(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean VMware = false;
        try {
            pstmt = conn.prepareStatement("select distinct(hypervisor_type) from `cloud`.`cluster` where removed is null");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                if ("VMware".equals(rs.getString(1))) {
                    VMware = true;
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while iterating through list of hypervisors in use", e);
        }
        // Just update the VMware system template. Other hypervisor templates are unchanged from previous 3.0.x versions.
        s_logger.debug("Updating VMware System Vms");
        try {
            //Get 3.0.5 VMware system Vm template Id
            pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = 'systemvm-vmware-3.0.5' and removed is null");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                long templateId = rs.getLong(1);
                rs.close();
                pstmt.close();
                // change template type to SYSTEM
                pstmt = conn.prepareStatement("update `cloud`.`vm_template` set type='SYSTEM' where id = ?");
                pstmt.setLong(1, templateId);
                pstmt.executeUpdate();
                pstmt.close();
                // update templete ID of system Vms
                pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = 'VMware'");
                pstmt.setLong(1, templateId);
                pstmt.executeUpdate();
                pstmt.close();
            } else {
                if (VMware) {
                    throw new CloudRuntimeException("3.0.5 VMware SystemVm template not found. Cannot upgrade system Vms");
                } else {
                    s_logger.warn("3.0.5 VMware SystemVm template not found. VMware hypervisor is not used, so not failing upgrade");
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while updating VMware systemVm template", e);
        }
        s_logger.debug("Updating System Vm Template IDs Complete");
    }

    private void addVpcProvider(Connection conn) {
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
                pstmt =
                    conn.prepareStatement("INSERT INTO `cloud`.`physical_network_service_providers` "
                        + "(`physical_network_id`, `provider_name`, `state`, `vpn_service_provided`, `dhcp_service_provided`, "
                        + "`dns_service_provided`, `gateway_service_provided`, `firewall_service_provided`, `source_nat_service_provided`,"
                        + " `load_balance_service_provided`, `static_nat_service_provided`, `port_forwarding_service_provided`,"
                        + " `user_data_service_provided`, `security_group_service_provided`) "
                        + "VALUES (?, 'VpcVirtualRouter', 'Enabled', 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0)");

                pstmt.setLong(1, pNtwkId);
                pstmt.executeUpdate();

                //get provider id
                pstmt =
                    conn.prepareStatement("SELECT id FROM `cloud`.`physical_network_service_providers` "
                        + "WHERE physical_network_id=? and provider_name='VpcVirtualRouter'");
                pstmt.setLong(1, pNtwkId);
                ResultSet rs1 = pstmt.executeQuery();
                rs1.next();
                long providerId = rs1.getLong(1);

                //insert VR element
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`virtual_router_providers` (`nsp_id`, `type`, `enabled`) " + "VALUES (?, 'VPCVirtualRouter', 1)");
                pstmt.setLong(1, providerId);
                pstmt.executeUpdate();

                s_logger.debug("Added VPC Virtual router provider for physical network id=" + pNtwkId);

            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable add VPC physical network service provider ", e);
        } finally {
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
        }
        s_logger.debug("Done adding VPC physical network service providers to all physical networks");
    }

    private void updateRouterNetworkRef(Connection conn) {
        //Encrypt config params and change category to Hidden
        s_logger.debug("Updating router network ref");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("SELECT d.id, d.network_id FROM `cloud`.`domain_router` d, `cloud`.`vm_instance` v " + "WHERE d.id=v.id AND v.removed is NULL");
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
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`router_network_ref` (router_id, network_id, guest_type) " + "VALUES (?, ?, ?)");

                pstmt.setLong(1, routerId);
                pstmt.setLong(2, networkId);
                pstmt.setString(3, networkType);
                pstmt.executeUpdate();

                s_logger.debug("Added reference for router id=" + routerId + " and network id=" + networkId);

            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to update the router/network reference ", e);
        } finally {
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
        }
        s_logger.debug("Done updating router/network references");
    }

    private void addHostDetailsUniqueKey(Connection conn) {
        s_logger.debug("Checking if host_details unique key exists, if not we will add it");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("SHOW INDEX FROM `cloud`.`host_details` WHERE KEY_NAME = 'uk_host_id_name'");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                s_logger.debug("Unique key already exists on host_details - not adding new one");
            } else {
                //add the key
                PreparedStatement pstmtUpdate =
                    conn.prepareStatement("ALTER IGNORE TABLE `cloud`.`host_details` ADD CONSTRAINT UNIQUE KEY `uk_host_id_name` (`host_id`, `name`)");
                pstmtUpdate.executeUpdate();
                s_logger.debug("Unique key did not exist on host_details -  added new one");
                pstmtUpdate.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to check/update the host_details unique key ", e);
        } finally {
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
        }
    }

    // This fix does two things
    //
    // 1) ensure that networks using external load balancer/firewall in 2.2.14 or prior releases deployments
    //    has entry in network_external_lb_device_map and network_external_firewall_device_map
    //
    // 2) Some keys of host details for F5 and SRX devices were stored in Camel Case in 2.x releases. From 3.0
    //    they are made in lowercase. On upgrade change the host details name to lower case
    private void fixZoneUsingExternalDevices(Connection conn) {
        //Get zones to upgrade
        List<Long> zoneIds = new ArrayList<Long>();
        PreparedStatement pstmt = null;
        PreparedStatement pstmtUpdate = null;
        ResultSet rs = null;
        long networkOfferingId, networkId;
        long f5DeviceId, f5HostId;
        long srxDevivceId, srxHostId;

        try {
            pstmt =
                conn.prepareStatement("select id from `cloud`.`data_center` where lb_provider='F5BigIp' or firewall_provider='JuniperSRX' or gateway_provider='JuniperSRX'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                zoneIds.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create network to LB & firewalla device mapping for networks  that use them", e);
        }

        if (zoneIds.size() == 0) {
            return; // no zones using F5 and SRX devices so return
        }

        // find the default network offering created for external devices during upgrade from 2.2.14
        try {
            pstmt = conn.prepareStatement("select id from `cloud`.`network_offerings` where unique_name='Isolated with external providers' ");
            rs = pstmt.executeQuery();
            if (rs.first()) {
                networkOfferingId = rs.getLong(1);
            } else {
                throw new CloudRuntimeException("Cannot upgrade as there is no 'Isolated with external providers' network offering crearted .");
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create network to LB & firewalla device mapping for networks  that use them", e);
        }

        for (Long zoneId : zoneIds) {
            try {
                // find the F5 device id  in the zone
                pstmt = conn.prepareStatement("SELECT id FROM host WHERE data_center_id=? AND type = 'ExternalLoadBalancer' AND removed IS NULL");
                pstmt.setLong(1, zoneId);
                rs = pstmt.executeQuery();
                if (rs.first()) {
                    f5HostId = rs.getLong(1);
                } else {
                    throw new CloudRuntimeException("Cannot upgrade as there is no F5 load balancer device found in data center " + zoneId);
                }
                pstmt = conn.prepareStatement("SELECT id FROM external_load_balancer_devices WHERE  host_id=?");
                pstmt.setLong(1, f5HostId);
                rs = pstmt.executeQuery();
                if (rs.first()) {
                    f5DeviceId = rs.getLong(1);
                } else {
                    throw new CloudRuntimeException("Cannot upgrade as there is no F5 load balancer device with host ID " + f5HostId +
                        " found in external_load_balancer_device");
                }

                // find the SRX device id  in the zone
                pstmt = conn.prepareStatement("SELECT id FROM host WHERE data_center_id=? AND type = 'ExternalFirewall' AND removed IS NULL");
                pstmt.setLong(1, zoneId);
                rs = pstmt.executeQuery();
                if (rs.first()) {
                    srxHostId = rs.getLong(1);
                } else {
                    throw new CloudRuntimeException("Cannot upgrade as there is no SRX firewall device found in data center " + zoneId);
                }
                pstmt = conn.prepareStatement("SELECT id FROM external_firewall_devices WHERE  host_id=?");
                pstmt.setLong(1, srxHostId);
                rs = pstmt.executeQuery();
                if (rs.first()) {
                    srxDevivceId = rs.getLong(1);
                } else {
                    throw new CloudRuntimeException("Cannot upgrade as there is no SRX firewall device found with host ID " + srxHostId +
                        " found in external_firewall_devices");
                }

                // check if network any uses F5 or SRX devices  in the zone
                pstmt =
                    conn.prepareStatement("select id from `cloud`.`networks` where guest_type='Virtual' and data_center_id=? and network_offering_id=? and removed IS NULL");
                pstmt.setLong(1, zoneId);
                pstmt.setLong(2, networkOfferingId);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    // get the network Id
                    networkId = rs.getLong(1);

                    // add mapping for the network in network_external_lb_device_map
                    String insertLbMapping =
                        "INSERT INTO `cloud`.`network_external_lb_device_map` (uuid, network_id, external_load_balancer_device_id, created) VALUES ( ?, ?, ?, now())";
                    pstmtUpdate = conn.prepareStatement(insertLbMapping);
                    pstmtUpdate.setString(1, UUID.randomUUID().toString());
                    pstmtUpdate.setLong(2, networkId);
                    pstmtUpdate.setLong(3, f5DeviceId);
                    pstmtUpdate.executeUpdate();
                    s_logger.debug("Successfully added entry in network_external_lb_device_map for network " + networkId + " and F5 device ID " + f5DeviceId);

                    // add mapping for the network in network_external_firewall_device_map
                    String insertFwMapping =
                        "INSERT INTO `cloud`.`network_external_firewall_device_map` (uuid, network_id, external_firewall_device_id, created) VALUES ( ?, ?, ?, now())";
                    pstmtUpdate = conn.prepareStatement(insertFwMapping);
                    pstmtUpdate.setString(1, UUID.randomUUID().toString());
                    pstmtUpdate.setLong(2, networkId);
                    pstmtUpdate.setLong(3, srxDevivceId);
                    pstmtUpdate.executeUpdate();
                    s_logger.debug("Successfully added entry in network_external_firewall_device_map for network " + networkId + " and SRX device ID " + srxDevivceId);
                }

                // update host details for F5 and SRX devices
                s_logger.debug("Updating the host details for F5 and SRX devices");
                pstmt = conn.prepareStatement("SELECT host_id, name FROM `cloud`.`host_details` WHERE  host_id=? OR host_id=?");
                pstmt.setLong(1, f5HostId);
                pstmt.setLong(2, srxHostId);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    long hostId = rs.getLong(1);
                    String camlCaseName = rs.getString(2);
                    if (!(camlCaseName.equalsIgnoreCase("numRetries") || camlCaseName.equalsIgnoreCase("publicZone") || camlCaseName.equalsIgnoreCase("privateZone") ||
                        camlCaseName.equalsIgnoreCase("publicInterface") || camlCaseName.equalsIgnoreCase("privateInterface") || camlCaseName.equalsIgnoreCase("usageInterface"))) {
                        continue;
                    }
                    String lowerCaseName = camlCaseName.toLowerCase();
                    pstmt = conn.prepareStatement("update `cloud`.`host_details` set name=? where host_id=? AND name=?");
                    pstmt.setString(1, lowerCaseName);
                    pstmt.setLong(2, hostId);
                    pstmt.setString(3, camlCaseName);
                    pstmt.executeUpdate();
                }
                s_logger.debug("Successfully updated host details for F5 and SRX devices");
            } catch (SQLException e) {
                throw new CloudRuntimeException("Unable create a mapping for the networks in network_external_lb_device_map and network_external_firewall_device_map", e);
            } finally {
                closeAutoCloseable(rs);
                closeAutoCloseable(pstmt);
            }
            s_logger.info("Successfully upgraded network using F5 and SRX devices to have a entry in the network_external_lb_device_map and network_external_firewall_device_map");
        }
    }

    private void fixForeignKeys(Connection conn) {
        s_logger.debug("Fixing foreign keys' names in ssh_keypairs table");
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
            PreparedStatement pstmt;
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD "
                    + "CONSTRAINT `fk_ssh_keypairs__account_id` FOREIGN KEY `fk_ssh_keypairs__account_id` (`account_id`)"
                    + " REFERENCES `account` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute ssh_keypairs table update for adding account_id foreign key", e);
        }

        try {
            PreparedStatement pstmt;
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD CONSTRAINT"
                    + " `fk_ssh_keypairs__domain_id` FOREIGN KEY `fk_ssh_keypairs__domain_id` (`domain_id`) " + "REFERENCES `domain` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute ssh_keypairs table update for adding domain_id foreign key", e);
        }
    }

    private void encryptClusterDetails(Connection conn) {
        s_logger.debug("Encrypting cluster details");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select id, value from `cloud`.`cluster_details` where name = 'password'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (value == null) {
                    continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement("update `cloud`.`cluster_details` set value=? where id=?");
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt cluster_details values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt cluster_details values ", e);
        } finally {
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
        }
        s_logger.debug("Done encrypting cluster_details");
    }
}
