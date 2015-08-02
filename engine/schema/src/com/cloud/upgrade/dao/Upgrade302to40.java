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

import com.cloud.dc.DataCenter.NetworkType;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade302to40 extends Upgrade30xBase {
    final static Logger s_logger = Logger.getLogger(Upgrade302to40.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"3.0.2", "4.0.0"};
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

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        //updateVmWareSystemVms(conn); This is not required as system template update is handled during 4.2 upgrade
        correctVRProviders(conn);
        correctMultiplePhysicaNetworkSetups(conn);
        addHostDetailsUniqueKey(conn);
        addVpcProvider(conn);
        updateRouterNetworkRef(conn);
        fixForeignKeys(conn);
        setupExternalNetworkDevices(conn);
        fixZoneUsingExternalDevices(conn);
        encryptConfig(conn);
        encryptClusterDetails(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-302to40-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-302to40-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

    private void correctVRProviders(Connection conn) {
        PreparedStatement pstmtVR = null;
        ResultSet rsVR = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmtVR = conn.prepareStatement("SELECT id, nsp_id FROM `cloud`.`virtual_router_providers` where type = 'VirtualRouter' AND removed IS NULL");
            rsVR = pstmtVR.executeQuery();
            while (rsVR.next()) {
                long vrId = rsVR.getLong(1);
                long nspId = rsVR.getLong(2);

                //check that this nspId points to a VR provider.
                pstmt = conn.prepareStatement("SELECT  physical_network_id, provider_name FROM `cloud`.`physical_network_service_providers` where id = ?");
                pstmt.setLong(1, nspId);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    long physicalNetworkId = rs.getLong(1);
                    String providerName = rs.getString(2);
                    if (!providerName.equalsIgnoreCase("VirtualRouter")) {
                        //mismatch, correct the nsp_id in VR
                        PreparedStatement pstmt1 = null;
                        ResultSet rs1 = null;
                        pstmt1 =
                            conn.prepareStatement("SELECT  id FROM `cloud`.`physical_network_service_providers` where physical_network_id = ? AND provider_name = ? AND removed IS NULL");
                        pstmt1.setLong(1, physicalNetworkId);
                        pstmt1.setString(2, "VirtualRouter");
                        rs1 = pstmt1.executeQuery();
                        if (rs1.next()) {
                            long correctNSPId = rs1.getLong(1);

                            //update VR entry
                            PreparedStatement pstmtUpdate = null;
                            String updateNSPId = "UPDATE `cloud`.`virtual_router_providers` SET nsp_id = ? WHERE id = ?";
                            pstmtUpdate = conn.prepareStatement(updateNSPId);
                            pstmtUpdate.setLong(1, correctNSPId);
                            pstmtUpdate.setLong(2, vrId);
                            pstmtUpdate.executeUpdate();
                            pstmtUpdate.close();
                        }
                        rs1.close();
                        pstmt1.close();
                    }
                }
                rs.close();
                pstmt.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while correcting Virtual Router Entries", e);
        } finally {
            closeAutoCloseable(rsVR);
            closeAutoCloseable(pstmtVR);
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
        }

    }

    private void correctMultiplePhysicaNetworkSetups(Connection conn) {
        PreparedStatement pstmtZone = null;
        ResultSet rsZone = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {

            //check if multiple physical networks with 'Guest' Traffic types are present
            //Yes:
            //1) check if there are guest networks without tags, if yes then add a new physical network with default tag for them
            //2) Check if there are physical network tags present
            //No: Add unique tag to each physical network
            //3) Get all guest networks unique network offering id's

            //Clone each for each physical network and add the tag.
            //add ntwk service map entries
            //update all guest networks of 1 physical network having this offering id to this new offering id

            pstmtZone = conn.prepareStatement("SELECT id, domain_id, networktype, name, uuid FROM `cloud`.`data_center`");
            rsZone = pstmtZone.executeQuery();
            while (rsZone.next()) {
                long zoneId = rsZone.getLong(1);
                Long domainId = rsZone.getLong(2);
                String networkType = rsZone.getString(3);
                String zoneName = rsZone.getString(4);
                String uuid = rsZone.getString(5);

                PreparedStatement pstmtUpdate = null;
                if (uuid == null) {
                    uuid = UUID.randomUUID().toString();
                    String updateUuid = "UPDATE `cloud`.`data_center` SET uuid = ? WHERE id = ?";
                    pstmtUpdate = conn.prepareStatement(updateUuid);
                    pstmtUpdate.setString(1, uuid);
                    pstmtUpdate.setLong(2, zoneId);
                    pstmtUpdate.executeUpdate();
                    pstmtUpdate.close();
                }

                //check if any networks were untagged and remaining to be mapped to a physical network

                pstmt =
                    conn.prepareStatement("SELECT count(n.id) FROM networks n WHERE n.physical_network_id IS NULL AND n.traffic_type = 'Guest' and n.data_center_id = ? and n.removed is null");
                pstmt.setLong(1, zoneId);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    Long count = rs.getLong(1);
                    if (count > 0) {
                        // find the default tag to use from global config or use 'cloud-private'
                        String xenGuestLabel = getNetworkLabelFromConfig(conn, "xen.guest.network.device");
                        //Decrypt this value.
                        xenGuestLabel = DBEncryptionUtil.decrypt(xenGuestLabel);

                        //make sure that no physical network with this traffic label already exists. if yes, error out.
                        if (xenGuestLabel != null) {
                            PreparedStatement pstmt5 =
                                conn.prepareStatement("SELECT count(*) FROM `cloud`.`physical_network_traffic_types` pntt JOIN `cloud`.`physical_network` pn ON pntt.physical_network_id = pn.id WHERE pntt.traffic_type ='Guest' AND pn.data_center_id = ? AND pntt.xen_network_label = ?");
                            pstmt5.setLong(1, zoneId);
                            pstmt5.setString(2, xenGuestLabel);
                            ResultSet rsSameLabel = pstmt5.executeQuery();

                            if (rsSameLabel.next()) {
                                Long sameLabelcount = rsSameLabel.getLong(1);
                                if (sameLabelcount > 0) {
                                    s_logger.error("There are untagged networks for which we need to add a physical network with Xen traffic label = 'xen.guest.network.device' config value, which is: " +
                                        xenGuestLabel);
                                    s_logger.error("However already there are " + sameLabelcount + " physical networks setup with same traffic label, cannot upgrade");
                                    throw new CloudRuntimeException("Cannot upgrade this setup since a physical network with same traffic label: " + xenGuestLabel +
                                        " already exists, Please check logs and contact Support.");
                                }
                            }
                        }

                        //Create a physical network with guest traffic type and this tag
                        long physicalNetworkId = addPhysicalNetworkToZone(conn, zoneId, zoneName, networkType, null, domainId);
                        addTrafficType(conn, physicalNetworkId, "Guest", xenGuestLabel, null, null);
                        addDefaultVRProvider(conn, physicalNetworkId, zoneId);
                        addDefaultSGProvider(conn, physicalNetworkId, zoneId, networkType, true);

                        PreparedStatement pstmt3 =
                            conn.prepareStatement("SELECT n.id FROM networks n WHERE n.physical_network_id IS NULL AND n.traffic_type = 'Guest' and n.data_center_id = ? and n.removed is null");
                        pstmt3.setLong(1, zoneId);
                        ResultSet rsNet = pstmt3.executeQuery();
                        s_logger.debug("Adding PhysicalNetwork to VLAN");
                        s_logger.debug("Adding PhysicalNetwork to user_ip_address");
                        s_logger.debug("Adding PhysicalNetwork to networks");
                        while (rsNet.next()) {
                            Long networkId = rsNet.getLong(1);
                            addPhysicalNtwk_To_Ntwk_IP_Vlan(conn, physicalNetworkId, networkId);
                        }
                        rsNet.close();
                        pstmt3.close();
                    }
                }
                rs.close();
                pstmt.close();

                boolean multiplePhysicalNetworks = false;

                pstmt =
                    conn.prepareStatement("SELECT count(*) FROM `cloud`.`physical_network_traffic_types` pntt JOIN `cloud`.`physical_network` pn ON pntt.physical_network_id = pn.id WHERE pntt.traffic_type ='Guest' and pn.data_center_id = ?");
                pstmt.setLong(1, zoneId);
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    Long count = rs.getLong(1);
                    if (count > 1) {
                        s_logger.debug("There are " + count + " physical networks setup");
                        multiplePhysicalNetworks = true;
                    }
                }
                rs.close();
                pstmt.close();

                if (multiplePhysicalNetworks) {
                    //check if guest vnet is wrongly configured by earlier upgrade. If yes error out
                    //check if any vnet is allocated and guest networks are using vnet But the physical network id does not match on the vnet and guest network.
                    PreparedStatement pstmt4 =
                        conn.prepareStatement("SELECT v.id, v.vnet, v.reservation_id, v.physical_network_id as vpid, n.id, n.physical_network_id as npid FROM `cloud`.`op_dc_vnet_alloc` v JOIN `cloud`.`networks` n ON CONCAT('vlan://' , v.vnet) = n.broadcast_uri WHERE v.taken IS NOT NULL AND v.data_center_id = ? AND n.removed IS NULL AND v.physical_network_id !=  n.physical_network_id");
                    pstmt4.setLong(1, zoneId);
                    ResultSet rsVNet = pstmt4.executeQuery();
                    if (rsVNet.next()) {
                        String vnet = rsVNet.getString(2);
                        String networkId = rsVNet.getString(5);
                        String vpid = rsVNet.getString(4);
                        String npid = rsVNet.getString(6);
                        s_logger.error("Guest Vnet assignment is set wrongly . Cannot upgrade until that is corrected. Example- Vnet: " + vnet +
                            " has physical network id: " + vpid + " ,but the guest network: " + networkId + " that uses it has physical network id: " + npid);

                        String message = "Cannot upgrade. Your setup has multiple Physical Networks and is using guest Vnet that is assigned wrongly. "
                            + "To upgrade, first correct the setup by doing the following: \n"
                            + "1. Please rollback to your 2.2.14 setup\n"
                            + "2. Please stop all VMs using isolated(virtual) networks through CloudStack\n"
                            + "3. Run following query to find if any networks still have nics allocated:\n\t"
                            + "a) check if any virtual guest networks still have allocated nics by running:\n\t"
                            + "SELECT DISTINCT op.id from `cloud`.`op_networks` op JOIN `cloud`.`networks` n on op.id=n.id WHERE nics_count != 0 AND guest_type = 'Virtual';\n\t"
                            + "b) If this returns any networkd ids, then ensure that all VMs are stopped, no new VM is being started, and then shutdown management server\n\t"
                            + "c) Clean up the nics count for the 'virtual' network id's returned in step (a) by running this:\n\t"
                            + "UPDATE `cloud`.`op_networks` SET nics_count = 0 WHERE  id = <enter id of virtual network>\n\t"
                            + "d) Restart management server and wait for all networks to shutdown. [Networks shutdown will be determined by "
                            + "network.gc.interval and network.gc.wait seconds] \n"
                            + "4. Please ensure all networks are shutdown and all guest Vnet's are free.\n"
                            + "5. Run upgrade. This will allocate all your guest vnet range to first physical network.  \n"
                            + "6. Reconfigure the vnet ranges for each physical network as desired by using updatePhysicalNetwork API \n" + "7. Start all your VMs";

                        s_logger.error(message);
                        throw new CloudRuntimeException("Cannot upgrade this setup since Guest Vnet assignment to the multiple physical " +
                            "networks is incorrect. Please check the logs for details on how to proceed");

                    }
                    rsVNet.close();
                    pstmt4.close();

                    //Clean up any vnets that have no live networks/nics
                    pstmt4 =
                        conn.prepareStatement("SELECT v.id, v.vnet, v.reservation_id FROM `cloud`.`op_dc_vnet_alloc` v LEFT JOIN networks n ON CONCAT('vlan://' , v.vnet) = n.broadcast_uri WHERE v.taken IS NOT NULL AND v.data_center_id = ? AND n.broadcast_uri IS NULL AND n.removed IS NULL");
                    pstmt4.setLong(1, zoneId);
                    rsVNet = pstmt4.executeQuery();
                    while (rsVNet.next()) {
                        Long vnet_id = rsVNet.getLong(1);
                        String vnetValue = rsVNet.getString(2);
                        String reservationId = rsVNet.getString(3);
                        //does this vnet have any nic associated?
                        PreparedStatement pstmt5 = conn.prepareStatement("SELECT id, instance_id FROM `cloud`.`nics` where broadcast_uri = ? and removed IS NULL");
                        String uri = "vlan://" + vnetValue;
                        pstmt5.setString(1, uri);
                        ResultSet rsNic = pstmt5.executeQuery();
                        Long nic_id = rsNic.getLong(1);
                        Long instance_id = rsNic.getLong(2);
                        if (rsNic.next()) {
                            throw new CloudRuntimeException("Cannot upgrade. Please cleanup the guest vnet: " + vnetValue + " , it is being used by nic_id: " + nic_id +
                                " , instance_id: " + instance_id);
                        }

                        //free this vnet
                        String freeVnet = "UPDATE `cloud`.`op_dc_vnet_alloc` SET account_id = NULL, taken = NULL, reservation_id = NULL WHERE id = ?";
                        pstmtUpdate = conn.prepareStatement(freeVnet);
                        pstmtUpdate.setLong(1, vnet_id);
                        pstmtUpdate.executeUpdate();
                        pstmtUpdate.close();
                    }
                    rsVNet.close();
                    pstmt4.close();

                    //add tags to the physical networks if not present and clone offerings

                    pstmt =
                        conn.prepareStatement("SELECT pn.id as pid , ptag.tag as tag FROM `cloud`.`physical_network` pn LEFT JOIN `cloud`.`physical_network_tags` ptag ON pn.id = ptag.physical_network_id where pn.data_center_id = ?");
                    pstmt.setLong(1, zoneId);
                    rs = pstmt.executeQuery();
                    while (rs.next()) {
                        long physicalNetworkId = rs.getLong("pid");
                        String tag = rs.getString("tag");
                        if (tag == null) {
                            //need to add unique tag
                            String newTag = "pNtwk-tag-" + physicalNetworkId;

                            String updateVnet = "INSERT INTO `cloud`.`physical_network_tags`(tag, physical_network_id) VALUES( ?, ? )";
                            pstmtUpdate = conn.prepareStatement(updateVnet);
                            pstmtUpdate.setString(1, newTag);
                            pstmtUpdate.setLong(2, physicalNetworkId);
                            pstmtUpdate.executeUpdate();
                            pstmtUpdate.close();

                            //clone offerings and tag them with this new tag, if there are any guest networks for this physical network

                            PreparedStatement pstmt2 = null;
                            ResultSet rs2 = null;

                            pstmt2 =
                                conn.prepareStatement("SELECT distinct network_offering_id FROM `cloud`.`networks` where traffic_type= 'Guest' and physical_network_id = ? and removed is null");
                            pstmt2.setLong(1, physicalNetworkId);
                            rs2 = pstmt2.executeQuery();

                            while (rs2.next()) {
                                //clone each offering, add new tag, clone offering-svc-map, update guest networks with new offering id
                                long networkOfferingId = rs2.getLong(1);
                                cloneOfferingAndAddTag(conn, networkOfferingId, physicalNetworkId, newTag);
                            }
                            rs2.close();
                            pstmt2.close();
                        }
                    }
                    rs.close();
                    pstmt.close();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while correcting PhysicalNetwork setup", e);
        } finally {
            closeAutoCloseable(rsZone);
            closeAutoCloseable(pstmtZone);
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
        }
    }

    private void cloneOfferingAndAddTag(Connection conn, long networkOfferingId, long physicalNetworkId, String newTag) {

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select count(*) from `cloud`.`network_offerings`");
            rs = pstmt.executeQuery();
            long ntwkOffCount = 0;
            while (rs.next()) {
                ntwkOffCount = rs.getLong(1);
            }
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("DROP TEMPORARY TABLE IF EXISTS `cloud`.`network_offerings2`");
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("CREATE TEMPORARY TABLE `cloud`.`network_offerings2` ENGINE=MEMORY SELECT * FROM `cloud`.`network_offerings` WHERE id=1");
            pstmt.executeUpdate();
            pstmt.close();

            // clone the record to
            pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_offerings2` SELECT * FROM `cloud`.`network_offerings` WHERE id=?");
            pstmt.setLong(1, networkOfferingId);
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = conn.prepareStatement("SELECT unique_name FROM `cloud`.`network_offerings` WHERE id=?");
            pstmt.setLong(1, networkOfferingId);
            rs = pstmt.executeQuery();
            String uniqueName = null;
            while (rs.next()) {
                uniqueName = rs.getString(1) + "-" + physicalNetworkId;
            }
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("UPDATE `cloud`.`network_offerings2` SET id=?, unique_name=?, name=?, tags=?, uuid=?  WHERE id=?");
            ntwkOffCount = ntwkOffCount + 1;
            long newNetworkOfferingId = ntwkOffCount;
            pstmt.setLong(1, newNetworkOfferingId);
            pstmt.setString(2, uniqueName);
            pstmt.setString(3, uniqueName);
            pstmt.setString(4, newTag);
            String uuid = UUID.randomUUID().toString();
            pstmt.setString(5, uuid);
            pstmt.setLong(6, networkOfferingId);
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_offerings` SELECT * from `cloud`.`network_offerings2` WHERE id=" + newNetworkOfferingId);
            pstmt.executeUpdate();
            pstmt.close();

            //clone service map
            pstmt = conn.prepareStatement("select service, provider from `cloud`.`ntwk_offering_service_map` where network_offering_id=?");
            pstmt.setLong(1, networkOfferingId);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String service = rs.getString(1);
                String provider = rs.getString(2);
                pstmt =
                    conn.prepareStatement("INSERT INTO `cloud`.`ntwk_offering_service_map` (`network_offering_id`, `service`, `provider`, `created`) values (?,?,?, now())");
                pstmt.setLong(1, newNetworkOfferingId);
                pstmt.setString(2, service);
                pstmt.setString(3, provider);
                pstmt.executeUpdate();
            }
            rs.close();
            pstmt.close();

            pstmt =
                conn.prepareStatement("UPDATE `cloud`.`networks` SET network_offering_id=? where physical_network_id=? and traffic_type ='Guest' and network_offering_id=" +
                    networkOfferingId);
            pstmt.setLong(1, newNetworkOfferingId);
            pstmt.setLong(2, physicalNetworkId);
            pstmt.executeUpdate();
            pstmt.close();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while cloning NetworkOffering", e);
        } finally {
            closeAutoCloseable(rs);
            try {
                pstmt = conn.prepareStatement("DROP TEMPORARY TABLE `cloud`.`network_offerings2`");
                pstmt.executeUpdate();
            } catch (SQLException e) {
                s_logger.info("[ignored] ",e);
            }
            closeAutoCloseable(pstmt);
        }
    }

    private void addHostDetailsUniqueKey(Connection conn) {
        s_logger.debug("Checking if host_details unique key exists, if not we will add it");
        try (
                PreparedStatement pstmt = conn.prepareStatement("SHOW INDEX FROM `cloud`.`host_details` WHERE KEY_NAME = 'uk_host_id_name'");
                ResultSet rs = pstmt.executeQuery();
            ) {
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
        }
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
        try (
                PreparedStatement pstmt = conn.prepareStatement("SELECT d.id, d.network_id FROM `cloud`.`domain_router` d, `cloud`.`vm_instance` v " + "WHERE d.id=v.id AND v.removed is NULL");
                PreparedStatement pstmt1 = conn.prepareStatement("SELECT guest_type from `cloud`.`networks` where id=?");
                PreparedStatement pstmt2 = conn.prepareStatement("INSERT INTO `cloud`.`router_network_ref` (router_id, network_id, guest_type) " + "VALUES (?, ?, ?)");
                ResultSet rs = pstmt.executeQuery();
            ){
            while (rs.next()) {
                Long routerId = rs.getLong(1);
                Long networkId = rs.getLong(2);

                //get the network type
                pstmt1.setLong(1, networkId);
                try (ResultSet rs1 = pstmt1.executeQuery();) {
                    rs1.next();
                    String networkType = rs1.getString(1);

                    //insert the reference
                    pstmt2.setLong(1, routerId);
                    pstmt2.setLong(2, networkId);
                    pstmt2.setString(3, networkType);
                    pstmt2.executeUpdate();
                }
                s_logger.debug("Added reference for router id=" + routerId + " and network id=" + networkId);

            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to update the router/network reference ", e);
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
        s_logger.debug("Adding F5 Big IP load balancer with host id " + hostId + " in to physical network" + physicalNetworkId);
        String insertF5 =
            "INSERT INTO `cloud`.`external_load_balancer_devices` (physical_network_id, host_id, provider_name, "
                + "device_name, capacity, is_dedicated, device_state, allocation_state, is_inline, is_managed, uuid) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(insertF5);) {
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
        }
    }

    private void addSrxFirewall(Connection conn, long hostId, long physicalNetworkId) {
        s_logger.debug("Adding SRX firewall device with host id " + hostId + " in to physical network" + physicalNetworkId);
        String insertSrx =
            "INSERT INTO `cloud`.`external_firewall_devices` (physical_network_id, host_id, provider_name, "
                + "device_name, capacity, is_dedicated, device_state, allocation_state, uuid) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(insertSrx);) {
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
        }
    }

    private void addF5ServiceProvider(Connection conn, long physicalNetworkId, long zoneId) {
        // add physical network service provider - F5BigIp
        s_logger.debug("Adding PhysicalNetworkServiceProvider F5BigIp" + " in to physical network" + physicalNetworkId);
        String insertPNSP =
            "INSERT INTO `cloud`.`physical_network_service_providers` (`uuid`, `physical_network_id` , `provider_name`, `state` ,"
                + "`destination_physical_network_id`, `vpn_service_provided`, `dhcp_service_provided`, `dns_service_provided`, `gateway_service_provided`,"
                + "`firewall_service_provided`, `source_nat_service_provided`, `load_balance_service_provided`, `static_nat_service_provided`,"
                + "`port_forwarding_service_provided`, `user_data_service_provided`, `security_group_service_provided`) VALUES (?,?,?,?,0,0,0,0,0,0,0,1,0,0,0,0)";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(insertPNSP);) {
            pstmtUpdate.setString(1, UUID.randomUUID().toString());
            pstmtUpdate.setLong(2, physicalNetworkId);
            pstmtUpdate.setString(3, "F5BigIp");
            pstmtUpdate.setString(4, "Enabled");
            pstmtUpdate.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworkServiceProvider F5BigIp", e);
        }
    }

    private void addSrxServiceProvider(Connection conn, long physicalNetworkId, long zoneId) {
        // add physical network service provider - JuniperSRX
        s_logger.debug("Adding PhysicalNetworkServiceProvider JuniperSRX");
        String insertPNSP =
            "INSERT INTO `cloud`.`physical_network_service_providers` (`uuid`, `physical_network_id` , `provider_name`, `state` ,"
                + "`destination_physical_network_id`, `vpn_service_provided`, `dhcp_service_provided`, `dns_service_provided`, `gateway_service_provided`,"
                + "`firewall_service_provided`, `source_nat_service_provided`, `load_balance_service_provided`, `static_nat_service_provided`,"
                + "`port_forwarding_service_provided`, `user_data_service_provided`, `security_group_service_provided`) VALUES (?,?,?,?,0,0,0,0,1,1,1,0,1,1,0,0)";
        try (PreparedStatement pstmtUpdate = conn.prepareStatement(insertPNSP);) {
            pstmtUpdate.setString(1, UUID.randomUUID().toString());
            pstmtUpdate.setLong(2, physicalNetworkId);
            pstmtUpdate.setString(3, "JuniperSRX");
            pstmtUpdate.setString(4, "Enabled");
            pstmtUpdate.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworkServiceProvider JuniperSRX", e);
        }
    }

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
            s_logger.info("Successfully upgraded networks using F5 and SRX devices to have a entry in the network_external_lb_device_map and network_external_firewall_device_map");
        }
    }

    private void encryptConfig(Connection conn) {
        //Encrypt config params and change category to Hidden
        s_logger.debug("Encrypting Config values");
        try (
                PreparedStatement pstmt = conn.prepareStatement("select name, value from `cloud`.`configuration` where name in ('router.ram.size', 'secondary.storage.vm', 'security.hash.key') and category <> 'Hidden'");
                PreparedStatement pstmt1 = conn.prepareStatement("update `cloud`.`configuration` set value=?, category = 'Hidden' where name=?");
                ResultSet rs = pstmt.executeQuery();
            ) {
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (value == null) {
                    continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt1.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt1.setString(2, name);
                pstmt1.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt configuration values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt configuration values ", e);
        }
        s_logger.debug("Done encrypting Config values");
    }

    private void encryptClusterDetails(Connection conn) {
        s_logger.debug("Encrypting cluster details");
        try (
                PreparedStatement pstmt = conn.prepareStatement("select id, value from `cloud`.`cluster_details` where name = 'password'");
                PreparedStatement pstmt1 = conn.prepareStatement("update `cloud`.`cluster_details` set value=? where id=?");
                ResultSet rs = pstmt.executeQuery();
            ) {
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (value == null) {
                    continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt1.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt1.setLong(2, id);
                pstmt1.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt cluster_details values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt cluster_details values ", e);
        }
        s_logger.debug("Done encrypting cluster_details");
    }
}
