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
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.offering.NetworkOffering;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.crypt.EncryptionSecretKeyChecker;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade2214to30 extends Upgrade30xBase implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade2214to30.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.14", "3.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-2214to30.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-2214to30.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        // Fail upgrade if encryption is not enabled
        if (!EncryptionSecretKeyChecker.useEncryption()) {
            throw new CloudRuntimeException("Encryption is not enabled. Please Run cloud-setup-encryption to enable encryption");
        }

        // physical network setup
        setupPhysicalNetworks(conn);
        // encrypt data
        encryptData(conn);
        // drop keys
        dropKeysIfExist(conn);
        //update templete ID for system Vms
        //updateSystemVms(conn); This is not required as system template update is handled during 4.2 upgrade
        // update domain network ref
        updateDomainNetworkRef(conn);
        // update networks that use redundant routers to the new network offering
        updateReduntantRouters(conn);
        // update networks that have to switch from Shared to Isolated network offerings
        switchAccountSpecificNetworksToIsolated(conn);
        // update networks to external network offerings if needed
        String externalOfferingName = fixNetworksWithExternalDevices(conn);
        // create service/provider map for network offerings
        createNetworkOfferingServices(conn, externalOfferingName);
        // create service/provider map for networks
        createNetworkServices(conn);
        //migrate user concentrated deployment planner choice to new global setting
        migrateUserConcentratedPlannerChoice(conn);
        // update domain router table for element it;
        updateRouters(conn);
        //update host capacities
        updateHostCapacity(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-2214to30-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-2214to30-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

    private void setupPhysicalNetworks(Connection conn) {
        /**
         * for each zone:
         * add a p.network, use zone.vnet and zone.type
         * add default traffic types, pnsp and virtual router element in enabled state
         * set p.network.id in op_dc_vnet and vlan and user_ip_address
         * list guest networks for the zone, set p.network.id
         *
         * for cases where network_tags are used for multiple guest networks:
         * - figure out distinct tags
         * - create physical network per tag
         * - create traffic types and set the tag to xen_network_label
         * - add physical network id  to networks, vlan, user_ip_address for networks belonging to this tag
         */
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmtUpdate = null;
        try {
            // Load all DataCenters

            String xenPublicLabel = getNetworkLabelFromConfig(conn, "xen.public.network.device");
            String xenPrivateLabel = getNetworkLabelFromConfig(conn, "xen.private.network.device");
            String xenStorageLabel = getNetworkLabelFromConfig(conn, "xen.storage.network.device1");
            String xenGuestLabel = getNetworkLabelFromConfig(conn, "xen.guest.network.device");

            String kvmPublicLabel = getNetworkLabelFromConfig(conn, "kvm.public.network.device");
            String kvmPrivateLabel = getNetworkLabelFromConfig(conn, "kvm.private.network.device");
            String kvmGuestLabel = getNetworkLabelFromConfig(conn, "kvm.guest.network.device");

            String vmwarePublicLabel = getNetworkLabelFromConfig(conn, "vmware.public.vswitch");
            String vmwarePrivateLabel = getNetworkLabelFromConfig(conn, "vmware.private.vswitch");
            String vmwareGuestLabel = getNetworkLabelFromConfig(conn, "vmware.guest.vswitch");

            pstmt = conn.prepareStatement("SELECT id, domain_id, networktype, vnet, name, removed FROM `cloud`.`data_center`");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long zoneId = rs.getLong(1);
                Long domainId = rs.getLong(2);
                String networkType = rs.getString(3);
                String vnet = rs.getString(4);
                String zoneName = rs.getString(5);
                String removed = rs.getString(6);

                //set uuid for the zone
                String uuid = UUID.randomUUID().toString();
                String updateUuid = "UPDATE `cloud`.`data_center` SET uuid = ? WHERE id = ?";
                pstmtUpdate = conn.prepareStatement(updateUuid);
                pstmtUpdate.setString(1, uuid);
                pstmtUpdate.setLong(2, zoneId);
                pstmtUpdate.executeUpdate();
                pstmtUpdate.close();

                //check if public network needs to be created
                boolean crtPbNtwk = false;
                pstmt = conn.prepareStatement("SELECT * FROM `cloud`.`networks` where traffic_type=\"public\" and data_center_id=?");
                pstmt2Close.add(pstmt);
                pstmt.setLong(1, zoneId);
                ResultSet rs1 = pstmt.executeQuery();
                if (rs1.next()) {
                    crtPbNtwk = true;
                }

                //check if there are multiple guest networks configured using network_tags
                PreparedStatement pstmt2 =
                    conn.prepareStatement("SELECT distinct tag FROM `cloud`.`network_tags` t JOIN `cloud`.`networks` n ON t.network_id = n.id WHERE n.data_center_id = ? and n.removed IS NULL");
                pstmt2Close.add(pstmt2);
                pstmt2.setLong(1, zoneId);
                ResultSet rsTags = pstmt2.executeQuery();
                if (rsTags.next()) {
                    s_logger.debug("Network tags are not empty, might have to create more than one physical network...");
                    //make sure setup does not use guest vnets

                    if (vnet != null) {
                        //check if any vnet is allocated and guest networks are using vnets.
                        PreparedStatement pstmt4 =
                            conn.prepareStatement("SELECT v.* FROM `cloud`.`op_dc_vnet_alloc` v JOIN `cloud`.`networks` n ON CONCAT('vlan://' , v.vnet) = " +
                                "n.broadcast_uri WHERE v.taken IS NOT NULL AND v.data_center_id = ? AND n.removed IS NULL");
                        pstmt2Close.add(pstmt4);
                        pstmt4.setLong(1, zoneId);
                        ResultSet rsVNet = pstmt4.executeQuery();

                        if (rsVNet.next()) {
                            String message = "Cannot upgrade. Your setup has multiple Physical Networks and is using guest "
                                + "Vnet that is assigned wrongly. To upgrade, first correct the setup by doing the following: \n"
                                + "1. Please rollback to your 2.2.14 setup\n"
                                + "2. Please stop all VMs using isolated(virtual) networks through CloudStack\n"
                                + "3. Run following query to find if any networks still have nics allocated:\n\t"
                                + "a) check if any virtual guest networks still have allocated nics by running:\n\t"
                                + "SELECT DISTINCT op.id from `cloud`.`op_networks` op JOIN `cloud`.`networks` n on "
                                + "op.id=n.id WHERE nics_count != 0 AND guest_type = 'Virtual';\n\t"
                                + "b) If this returns any networkd ids, then ensure that all VMs are stopped, no new VM is being started, and then shutdown management server\n\t"
                                + "c) Clean up the nics count for the 'virtual' network id's returned in step (a) by running this:\n\t"
                                + "UPDATE `cloud`.`op_networks` SET nics_count = 0 WHERE  id = <enter id of virtual network>\n\t"
                                + "d) Restart management server and wait for all networks to shutdown. [Networks shutdown will be "
                                + "determined by network.gc.interval and network.gc.wait seconds] \n"
                                + "4. Please ensure all networks are shutdown and all guest Vnet's are free.\n"
                                + "5. Run upgrade. This will allocate all your guest vnet range to first physical network.  \n"
                                + "6. Reconfigure the vnet ranges for each physical network as desired by using updatePhysicalNetwork API \n"
                                + "7. Start all your VMs";

                            s_logger.error(message);

                            throw new CloudRuntimeException(
                                "Cannot upgrade this setup since it uses guest vnet and will have multiple physical networks. Please check the logs for details on how to proceed");
                        }
                        rsVNet.close();

                        //Clean up any vnets that have no live networks/nics
                        pstmt4 =
                            conn.prepareStatement("SELECT v.id, v.vnet, v.reservation_id FROM `cloud`.`op_dc_vnet_alloc` v LEFT JOIN networks n ON CONCAT('vlan://' , v.vnet) = n.broadcast_uri WHERE v.taken IS NOT NULL AND v.data_center_id = ? AND n.broadcast_uri IS NULL AND n.removed IS NULL");
                        pstmt2Close.add(pstmt4);
                        pstmt4.setLong(1, zoneId);
                        rsVNet = pstmt4.executeQuery();
                        while (rsVNet.next()) {
                            Long vnet_id = rsVNet.getLong(1);
                            String vnetValue = rsVNet.getString(2);
                            String reservationId = rsVNet.getString(3);
                            //does this vnet have any nic associated?
                            PreparedStatement pstmt5 = conn.prepareStatement("SELECT id, instance_id FROM `cloud`.`nics` where broadcast_uri = ? and removed IS NULL");
                            pstmt2Close.add(pstmt5);
                            String uri = "vlan://" + vnetValue;
                            pstmt5.setString(1, uri);
                            ResultSet rsNic = pstmt5.executeQuery();
                            Long nic_id = rsNic.getLong(1);
                            Long instance_id = rsNic.getLong(2);
                            if (rsNic.next()) {
                                throw new CloudRuntimeException("Cannot upgrade. Please cleanup the guest vnet: " + vnetValue + " , it is being used by nic_id: " +
                                    nic_id + " , instance_id: " + instance_id);
                            }

                            //free this vnet
                            String freeVnet = "UPDATE `cloud`.`op_dc_vnet_alloc` SET account_id = NULL, taken = NULL, reservation_id = NULL WHERE id = ?";
                            pstmtUpdate = conn.prepareStatement(freeVnet);
                            pstmtUpdate.setLong(1, vnet_id);
                            pstmtUpdate.executeUpdate();
                            pstmtUpdate.close();
                        }
                    }

                    boolean isFirstPhysicalNtwk = true;
                    do {
                        //create one physical network per tag
                        String guestNetworkTag = rsTags.getString(1);
                        long physicalNetworkId = addPhysicalNetworkToZone(conn, zoneId, zoneName, networkType, (isFirstPhysicalNtwk) ? vnet : null, domainId);
                        //add Traffic types
                        if (isFirstPhysicalNtwk) {
                            if (crtPbNtwk) {
                                addTrafficType(conn, physicalNetworkId, "Public", xenPublicLabel, kvmPublicLabel, vmwarePublicLabel);
                            } else {
                                s_logger.debug("Skip adding public traffic type to zone id=" + zoneId);
                            }
                            addTrafficType(conn, physicalNetworkId, "Management", xenPrivateLabel, kvmPrivateLabel, vmwarePrivateLabel);
                            addTrafficType(conn, physicalNetworkId, "Storage", xenStorageLabel, null, null);
                        }
                        addTrafficType(conn, physicalNetworkId, "Guest", guestNetworkTag, kvmGuestLabel, vmwareGuestLabel);
                        addDefaultVRProvider(conn, physicalNetworkId, zoneId);
                        addDefaultSGProvider(conn, physicalNetworkId, zoneId, networkType, false);
                        //for all networks with this tag, add physical_network_id

                        PreparedStatement pstmt3 = conn.prepareStatement("SELECT network_id FROM `cloud`.`network_tags` where tag= ?");
                        pstmt3.setString(1,guestNetworkTag);
                        ResultSet rsNet = pstmt3.executeQuery();
                        s_logger.debug("Adding PhysicalNetwork to VLAN");
                        s_logger.debug("Adding PhysicalNetwork to user_ip_address");
                        s_logger.debug("Adding PhysicalNetwork to networks");
                        while (rsNet.next()) {
                            Long networkId = rsNet.getLong(1);
                            addPhysicalNtwk_To_Ntwk_IP_Vlan(conn, physicalNetworkId, networkId);
                        }
                        pstmt3.close();

                        // add the reference to this physical network for the default public network entries in vlan / user_ip_address tables
                        // add first physicalNetworkId to op_dc_vnet_alloc for this zone - just a placeholder since direct networking dont need this
                        if (isFirstPhysicalNtwk) {
                            s_logger.debug("Adding PhysicalNetwork to default Public network entries in vlan and user_ip_address");
                            pstmt3 = conn.prepareStatement("SELECT id FROM `cloud`.`networks` where traffic_type = 'Public' and data_center_id = " + zoneId);
                            ResultSet rsPubNet = pstmt3.executeQuery();
                            if (rsPubNet.next()) {
                                Long publicNetworkId = rsPubNet.getLong(1);
                                addPhysicalNtwk_To_Ntwk_IP_Vlan(conn, physicalNetworkId, publicNetworkId);
                            }
                            pstmt3.close();

                            s_logger.debug("Adding PhysicalNetwork to op_dc_vnet_alloc");
                            String updateVnet = "UPDATE `cloud`.`op_dc_vnet_alloc` SET physical_network_id = " + physicalNetworkId + " WHERE data_center_id = " + zoneId;
                            pstmtUpdate = conn.prepareStatement(updateVnet);
                            pstmtUpdate.executeUpdate();
                            pstmtUpdate.close();
                        }

                        isFirstPhysicalNtwk = false;
                    } while (rsTags.next());
                    pstmt2.close();
                } else {
                    //default to one physical network
                    long physicalNetworkId = addPhysicalNetworkToZone(conn, zoneId, zoneName, networkType, vnet, domainId);
                    // add traffic types
                    if (crtPbNtwk) {
                        addTrafficType(conn, physicalNetworkId, "Public", xenPublicLabel, kvmPublicLabel, vmwarePublicLabel);
                    } else {
                        s_logger.debug("Skip adding public traffic type to zone id=" + zoneId);
                    }
                    addTrafficType(conn, physicalNetworkId, "Management", xenPrivateLabel, kvmPrivateLabel, vmwarePrivateLabel);
                    addTrafficType(conn, physicalNetworkId, "Storage", xenStorageLabel, null, null);
                    addTrafficType(conn, physicalNetworkId, "Guest", xenGuestLabel, kvmGuestLabel, vmwareGuestLabel);
                    addDefaultVRProvider(conn, physicalNetworkId, zoneId);
                    addDefaultSGProvider(conn, physicalNetworkId, zoneId, networkType, false);

                    // add physicalNetworkId to op_dc_vnet_alloc for this zone
                    s_logger.debug("Adding PhysicalNetwork to op_dc_vnet_alloc");
                    String updateVnet = "UPDATE `cloud`.`op_dc_vnet_alloc` SET physical_network_id = " + physicalNetworkId + " WHERE data_center_id = " + zoneId;
                    pstmtUpdate = conn.prepareStatement(updateVnet);
                    pstmtUpdate.executeUpdate();
                    pstmtUpdate.close();

                    // add physicalNetworkId to vlan for this zone
                    s_logger.debug("Adding PhysicalNetwork to VLAN");
                    String updateVLAN = "UPDATE `cloud`.`vlan` SET physical_network_id = " + physicalNetworkId + " WHERE data_center_id = " + zoneId;
                    pstmtUpdate = conn.prepareStatement(updateVLAN);
                    pstmtUpdate.executeUpdate();
                    pstmtUpdate.close();

                    // add physicalNetworkId to user_ip_address for this zone
                    s_logger.debug("Adding PhysicalNetwork to user_ip_address");
                    String updateUsrIp = "UPDATE `cloud`.`user_ip_address` SET physical_network_id = " + physicalNetworkId + " WHERE data_center_id = " + zoneId;
                    pstmtUpdate = conn.prepareStatement(updateUsrIp);
                    pstmtUpdate.executeUpdate();
                    pstmtUpdate.close();

                    // add physicalNetworkId to guest networks for this zone
                    s_logger.debug("Adding PhysicalNetwork to networks");
                    String updateNet =
                        "UPDATE `cloud`.`networks` SET physical_network_id = " + physicalNetworkId + " WHERE data_center_id = " + zoneId + " AND traffic_type = 'Guest'";
                    pstmtUpdate = conn.prepareStatement(updateNet);
                    pstmtUpdate.executeUpdate();
                    pstmtUpdate.close();

                    //mark this physical network as removed if the zone is removed.
                    if (removed != null) {
                        pstmtUpdate = conn.prepareStatement("UPDATE `cloud`.`physical_network` SET removed = now() WHERE id = ?");
                        pstmtUpdate.setLong(1, physicalNetworkId);
                        pstmtUpdate.executeUpdate();
                        pstmtUpdate.close();
                    }
                }

            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworks", e);
        } finally {
            TransactionLegacy.closePstmts(pstmt2Close);
        }

    }

    private void encryptData(Connection conn) {
        s_logger.debug("Encrypting the data...");
        encryptConfigValues(conn);
        encryptHostDetails(conn);
        encryptVNCPassword(conn);
        encryptUserCredentials(conn);
        encryptVPNPassword(conn);
        s_logger.debug("Done encrypting the data");
    }

    private void encryptConfigValues(Connection conn) {
        s_logger.debug("Encrypting Config values");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select name, value from `cloud`.`configuration` where category in ('Hidden', 'Secure')");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if (value == null) {
                    continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement("update `cloud`.`configuration` set value=? where name=?");
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setString(2, name);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt configuration values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt configuration values ", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
                s_logger.info("[ignored]",e);
            }
        }
        s_logger.debug("Done encrypting Config values");
    }

    private void encryptHostDetails(Connection conn) {
        s_logger.debug("Encrypting host details");
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select id, value from `cloud`.`host_details` where name = 'password'");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (value == null) {
                    continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement("update `cloud`.`host_details` set value=? where id=?");
                pstmt2Close.add(pstmt);
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt host_details values ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt host_details values ", e);
        } finally {
            TransactionLegacy.closePstmts(pstmt2Close);
        }
        s_logger.debug("Done encrypting host details");
    }

    private void encryptVNCPassword(Connection conn) {
        s_logger.debug("Encrypting vm_instance vnc_password");
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            int numRows = 0;
            pstmt = conn.prepareStatement("select count(id) from `cloud`.`vm_instance`");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                numRows = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
            int offset = 0;
            while (offset < numRows) {
                pstmt = conn.prepareStatement("select id, vnc_password from `cloud`.`vm_instance` limit " + offset + ", 500");
                pstmt2Close.add(pstmt);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    long id = rs.getLong(1);
                    String value = rs.getString(2);
                    if (value == null) {
                        continue;
                    }
                    String encryptedValue = DBEncryptionUtil.encrypt(value);
                    pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vnc_password=? where id=?");
                    pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                    pstmt.setLong(2, id);
                    pstmt.executeUpdate();
                    pstmt.close();
                }
                rs.close();
                offset += 500;
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt vm_instance vnc_password ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt vm_instance vnc_password ", e);
        } finally {
            TransactionLegacy.closePstmts(pstmt2Close);
        }
        s_logger.debug("Done encrypting vm_instance vnc_password");
    }

    private void encryptUserCredentials(Connection conn) {
        s_logger.debug("Encrypting user keys");
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select id, secret_key from `cloud`.`user`");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String secretKey = rs.getString(2);
                String encryptedSecretKey = DBEncryptionUtil.encrypt(secretKey);
                pstmt = conn.prepareStatement("update `cloud`.`user` set secret_key=? where id=?");
                pstmt2Close.add(pstmt);
                if (encryptedSecretKey == null) {
                    pstmt.setNull(1, Types.VARCHAR);
                } else {
                    pstmt.setBytes(1, encryptedSecretKey.getBytes("UTF-8"));
                }
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt user secret key ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt user secret key ", e);
        } finally {
            TransactionLegacy.closePstmts(pstmt2Close);
        }
        s_logger.debug("Done encrypting user keys");
    }

    private void encryptVPNPassword(Connection conn) {
        s_logger.debug("Encrypting vpn_users password");
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select id, password from `cloud`.`vpn_users`");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String password = rs.getString(2);
                String encryptedpassword = DBEncryptionUtil.encrypt(password);
                pstmt = conn.prepareStatement("update `cloud`.`vpn_users` set password=? where id=?");
                pstmt2Close.add(pstmt);
                if (encryptedpassword == null) {
                    pstmt.setNull(1, Types.VARCHAR);
                } else {
                    pstmt.setBytes(1, encryptedpassword.getBytes("UTF-8"));
                }
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt vpn_users password ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable encrypt vpn_users password ", e);
        } finally {
            TransactionLegacy.closePstmts(pstmt2Close);
        }
        s_logger.debug("Done encrypting vpn_users password");
    }

    private void dropKeysIfExist(Connection conn) {
        HashMap<String, List<String>> uniqueKeys = new HashMap<String, List<String>>();
        List<String> keys = new ArrayList<String>();
        keys.add("public_ip_address");
        uniqueKeys.put("console_proxy", keys);
        uniqueKeys.put("secondary_storage_vm", keys);

        // drop keys
        s_logger.debug("Dropping public_ip_address keys from `cloud`.`secondary_storage_vm` and console_proxy tables...");
        for (String tableName : uniqueKeys.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, uniqueKeys.get(tableName), false);
        }
    }

    private void createNetworkOfferingServices(Connection conn, String externalOfferingName) {
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt =
                conn.prepareStatement("select id, dns_service, gateway_service, firewall_service, lb_service, userdata_service,"
                    + " vpn_service, dhcp_service, unique_name from `cloud`.`network_offerings` where traffic_type='Guest'");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                boolean sharedSourceNat = false;
                boolean dedicatedLb = true;
                long id = rs.getLong(1);
                String uniqueName = rs.getString(9);

                Map<String, String> services = new HashMap<String, String>();
                if (rs.getLong(2) != 0) {
                    services.put("Dns", "VirtualRouter");
                }

                if (rs.getLong(3) != 0) {
                    if (externalOfferingName != null && uniqueName.equalsIgnoreCase(externalOfferingName)) {
                        services.put("Gateway", "JuniperSRX");
                    } else {
                        services.put("Gateway", "VirtualRouter");
                    }
                }

                if (rs.getLong(4) != 0) {
                    if (externalOfferingName != null && uniqueName.equalsIgnoreCase(externalOfferingName)) {
                        services.put("Firewall", "JuniperSRX");
                    } else {
                        services.put("Firewall", "VirtualRouter");
                    }
                }

                if (rs.getLong(5) != 0) {
                    if (externalOfferingName != null && uniqueName.equalsIgnoreCase(externalOfferingName)) {
                        services.put("Lb", "F5BigIp");
                        dedicatedLb = false;
                    } else {
                        services.put("Lb", "VirtualRouter");
                    }
                }

                if (rs.getLong(6) != 0) {
                    services.put("UserData", "VirtualRouter");
                }

                if (rs.getLong(7) != 0) {
                    if (externalOfferingName == null || !uniqueName.equalsIgnoreCase(externalOfferingName)) {
                        services.put("Vpn", "VirtualRouter");
                    }
                }

                if (rs.getLong(8) != 0) {
                    services.put("Dhcp", "VirtualRouter");
                }

                if (uniqueName.equalsIgnoreCase(NetworkOffering.DefaultSharedNetworkOfferingWithSGService.toString())) {
                    services.put("SecurityGroup", "SecurityGroupProvider");
                }

                if (uniqueName.equals(NetworkOffering.DefaultIsolatedNetworkOfferingWithSourceNatService.toString()) ||
                    uniqueName.equals(NetworkOffering.DefaultIsolatedNetworkOfferingWithSourceNatService.toString() + "-redundant") ||
                    uniqueName.equalsIgnoreCase(externalOfferingName)) {
                    if (externalOfferingName != null && uniqueName.equalsIgnoreCase(externalOfferingName)) {
                        services.put("SourceNat", "JuniperSRX");
                        services.put("PortForwarding", "JuniperSRX");
                        services.put("StaticNat", "JuniperSRX");
                        sharedSourceNat = true;
                    } else {
                        services.put("SourceNat", "VirtualRouter");
                        services.put("PortForwarding", "VirtualRouter");
                        services.put("StaticNat", "VirtualRouter");
                    }
                }

                for (String service : services.keySet()) {
                    pstmt =
                        conn.prepareStatement("INSERT INTO `cloud`.`ntwk_offering_service_map` (`network_offering_id`,"
                            + " `service`, `provider`, `created`) values (?,?,?, now())");
                    pstmt2Close.add(pstmt);
                    pstmt.setLong(1, id);
                    pstmt.setString(2, service);
                    pstmt.setString(3, services.get(service));
                    pstmt.executeUpdate();
                }

                //update shared source nat and dedicated lb
                pstmt = conn.prepareStatement("UPDATE `cloud`.`network_offerings` set shared_source_nat_service=?, dedicated_lb_service=? where id=?");
                pstmt2Close.add(pstmt);
                pstmt.setBoolean(1, sharedSourceNat);
                pstmt.setBoolean(2, dedicatedLb);
                pstmt.setLong(3, id);
                pstmt.executeUpdate();

            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create service/provider map for network offerings", e);
        } finally {
            TransactionLegacy.closePstmts(pstmt2Close);
        }
    }

    private void updateDomainNetworkRef(Connection conn) {
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            // update subdomain access field for existing domain specific networks
            pstmt = conn.prepareStatement("select value from `cloud`.`configuration` where name='allow.subdomain.network.access'");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                boolean subdomainAccess = Boolean.valueOf(rs.getString(1));
                pstmt = conn.prepareStatement("UPDATE `cloud`.`domain_network_ref` SET subdomain_access=?");
                pstmt2Close.add(pstmt);
                pstmt.setBoolean(1, subdomainAccess);
                pstmt.executeUpdate();
                s_logger.debug("Successfully updated subdomain_access field in network_domain table with value " + subdomainAccess);
            }

            // convert zone level 2.2.x networks to ROOT domain 3.0 access networks
            pstmt = conn.prepareStatement("select id from `cloud`.`networks` where shared=true and is_domain_specific=false and traffic_type='Guest'");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long networkId = rs.getLong(1);
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`domain_network_ref` (domain_id, network_id, subdomain_access) VALUES (1, ?, 1)");
                pstmt2Close.add(pstmt);
                pstmt.setLong(1, networkId);
                pstmt.executeUpdate();
                s_logger.debug("Successfully converted zone specific network id=" + networkId + " to the ROOT domain level network with subdomain access set to true");
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update domain network ref", e);
        } finally {
            TransactionLegacy.closePstmts(pstmt2Close);
        }
    }

    protected void createNetworkServices(Connection conn) {
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet networkRs = null;
        ResultSet offeringRs = null;
        try {
            pstmt = conn.prepareStatement("select id, network_offering_id from `cloud`.`networks` where traffic_type='Guest'");
            pstmt2Close.add(pstmt);
            networkRs = pstmt.executeQuery();
            while (networkRs.next()) {
                long networkId = networkRs.getLong(1);
                long networkOfferingId = networkRs.getLong(2);
                pstmt = conn.prepareStatement("select service, provider from `cloud`.`ntwk_offering_service_map` where network_offering_id=?");
                pstmt2Close.add(pstmt);
                pstmt.setLong(1, networkOfferingId);
                offeringRs = pstmt.executeQuery();
                while (offeringRs.next()) {
                    String service = offeringRs.getString(1);
                    String provider = offeringRs.getString(2);
                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`ntwk_service_map` (`network_id`, `service`, `provider`, `created`) values (?,?,?, now())");
                    pstmt.setLong(1, networkId);
                    pstmt.setString(2, service);
                    pstmt.setString(3, provider);
                    pstmt.executeUpdate();
                }
                s_logger.debug("Created service/provider map for network id=" + networkId);
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create service/provider map for networks", e);
        } finally {
            TransactionLegacy.closePstmts(pstmt2Close);
        }
    }

    protected void updateRouters(Connection conn) {
        PreparedStatement pstmt = null;
        try {
            s_logger.debug("Updating domain_router table");
            pstmt =
                conn.prepareStatement("UPDATE domain_router, virtual_router_providers vrp LEFT JOIN (physical_network_service_providers pnsp INNER JOIN physical_network pntwk INNER JOIN vm_instance vm INNER JOIN domain_router vr) ON (vrp.nsp_id = pnsp.id AND pnsp.physical_network_id = pntwk.id AND pntwk.data_center_id = vm.data_center_id AND vm.id=vr.id) SET vr.element_id=vrp.id;");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update router table. ", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Unable to close statement for router table. ", e);
            }
        }
    }

    protected void updateReduntantRouters(Connection conn) {
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        try {
            // get all networks that need to be updated to the redundant network offerings
            pstmt =
                conn.prepareStatement("select ni.network_id, n.network_offering_id from `cloud`.`nics` ni, `cloud`.`networks` n where ni.instance_id in (select id from `cloud`.`domain_router` where is_redundant_router=1) and n.id=ni.network_id and n.traffic_type='Guest'");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            pstmt = conn.prepareStatement("select count(*) from `cloud`.`network_offerings`");
            pstmt2Close.add(pstmt);
            rs1 = pstmt.executeQuery();
            long ntwkOffCount = 0;
            while (rs1.next()) {
                ntwkOffCount = rs1.getLong(1);
            }

            s_logger.debug("Have " + ntwkOffCount + " networkOfferings");
            pstmt = conn.prepareStatement("CREATE TEMPORARY TABLE `cloud`.`network_offerings2` ENGINE=MEMORY SELECT * FROM `cloud`.`network_offerings` WHERE id=1");
            pstmt2Close.add(pstmt);
            pstmt.executeUpdate();

            HashMap<Long, Long> newNetworkOfferingMap = new HashMap<Long, Long>();

            while (rs.next()) {
                long networkId = rs.getLong(1);
                long networkOfferingId = rs.getLong(2);
                s_logger.debug("Updating network offering for the network id=" + networkId + " as it has redundant routers");
                Long newNetworkOfferingId = null;

                if (!newNetworkOfferingMap.containsKey(networkOfferingId)) {
                    // clone the record to
                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_offerings2` SELECT * FROM `cloud`.`network_offerings` WHERE id=?");
                    pstmt2Close.add(pstmt);
                    pstmt.setLong(1, networkOfferingId);
                    pstmt.executeUpdate();

                    pstmt = conn.prepareStatement("SELECT unique_name FROM `cloud`.`network_offerings` WHERE id=?");
                    pstmt2Close.add(pstmt);
                    pstmt.setLong(1, networkOfferingId);
                    rs1 = pstmt.executeQuery();
                    String uniqueName = null;
                    while (rs1.next()) {
                        uniqueName = rs1.getString(1) + "-redundant";
                    }

                    pstmt = conn.prepareStatement("UPDATE `cloud`.`network_offerings2` SET id=?, redundant_router_service=1, unique_name=?, name=? WHERE id=?");
                    pstmt2Close.add(pstmt);
                    ntwkOffCount = ntwkOffCount + 1;
                    newNetworkOfferingId = ntwkOffCount;
                    pstmt.setLong(1, newNetworkOfferingId);
                    pstmt.setString(2, uniqueName);
                    pstmt.setString(3, uniqueName);
                    pstmt.setLong(4, networkOfferingId);
                    pstmt.executeUpdate();

                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_offerings` SELECT * from `cloud`.`network_offerings2` WHERE id=" + newNetworkOfferingId);
                    pstmt2Close.add(pstmt);
                    pstmt.executeUpdate();

                    pstmt = conn.prepareStatement("UPDATE `cloud`.`networks` SET network_offering_id=? where id=?");
                    pstmt2Close.add(pstmt);
                    pstmt.setLong(1, newNetworkOfferingId);
                    pstmt.setLong(2, networkId);
                    pstmt.executeUpdate();

                    newNetworkOfferingMap.put(networkOfferingId, ntwkOffCount);
                } else {
                    pstmt = conn.prepareStatement("UPDATE `cloud`.`networks` SET network_offering_id=? where id=?");
                    pstmt2Close.add(pstmt);
                    newNetworkOfferingId = newNetworkOfferingMap.get(networkOfferingId);
                    pstmt.setLong(1, newNetworkOfferingId);
                    pstmt.setLong(2, networkId);
                    pstmt.executeUpdate();
                }

                s_logger.debug("Successfully updated network offering id=" + networkId + " with new network offering id " + newNetworkOfferingId);
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update redundant router networks", e);
        } finally {
            try {
                pstmt = conn.prepareStatement("DROP TABLE `cloud`.`network_offerings2`");
                pstmt.executeUpdate();
                pstmt.close();
            } catch (SQLException e) {
                s_logger.info("[ignored]",e);
            }
            TransactionLegacy.closePstmts(pstmt2Close);
        }
    }

    protected void updateHostCapacity(Connection conn) {
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        try {
            s_logger.debug("Updating op_host_capacity table, column capacity_state");
            pstmt =
                conn.prepareStatement("UPDATE op_host_capacity, host SET op_host_capacity.capacity_state='Disabled' where host.id=op_host_capacity.host_id and op_host_capacity.capacity_type in (0,1) and host.resource_state='Disabled';");
            pstmt2Close.add(pstmt);
            pstmt.executeUpdate();

            pstmt =
                conn.prepareStatement("UPDATE op_host_capacity, cluster SET op_host_capacity.capacity_state='Disabled' where cluster.id=op_host_capacity.cluster_id and cluster.allocation_state='Disabled';");
            pstmt2Close.add(pstmt);
            pstmt.executeUpdate();

            pstmt =
                conn.prepareStatement("UPDATE op_host_capacity, host_pod_ref SET op_host_capacity.capacity_state='Disabled' where host_pod_ref.id=op_host_capacity.pod_id and host_pod_ref.allocation_state='Disabled';");
            pstmt2Close.add(pstmt);
            pstmt.executeUpdate();

            pstmt =
                conn.prepareStatement("UPDATE op_host_capacity, data_center SET op_host_capacity.capacity_state='Disabled' where data_center.id=op_host_capacity.data_center_id and data_center.allocation_state='Disabled';");
            pstmt2Close.add(pstmt);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update op_host_capacity table. ", e);
        } finally {
            TransactionLegacy.closePstmts(pstmt2Close);
        }
    }

    protected void switchAccountSpecificNetworksToIsolated(Connection conn) {
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        try {
            //check if switch_to_isolated is present; if not - skip this part of the code
            try {
                pstmt = conn.prepareStatement("select switch_to_isolated from `cloud`.`networks`");
                pstmt2Close.add(pstmt);
                rs = pstmt.executeQuery();
            } catch (Exception ex) {
                s_logger.debug("switch_to_isolated field is not present in networks table");
                if (pstmt != null) {
                    pstmt.close();
                }
                return;
            }

            // get all networks that need to be updated to the isolated network offering
            pstmt = conn.prepareStatement("select id, network_offering_id from `cloud`.`networks` where switch_to_isolated=1");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();

            pstmt = conn.prepareStatement("select count(*) from `cloud`.`network_offerings`");
            pstmt2Close.add(pstmt);
            rs1 = pstmt.executeQuery();
            long ntwkOffCount = 0;
            while (rs1.next()) {
                ntwkOffCount = rs1.getLong(1);
            }

            s_logger.debug("Have " + ntwkOffCount + " networkOfferings");
            pstmt = conn.prepareStatement("CREATE TEMPORARY TABLE `cloud`.`network_offerings2` ENGINE=MEMORY SELECT * FROM `cloud`.`network_offerings` WHERE id=1");
            pstmt2Close.add(pstmt);
            pstmt.executeUpdate();

            HashMap<Long, Long> newNetworkOfferingMap = new HashMap<Long, Long>();

            while (rs.next()) {
                long networkId = rs.getLong(1);
                long networkOfferingId = rs.getLong(2);
                s_logger.debug("Updating network offering for the network id=" + networkId + " as it has switch_to_isolated=1");
                Long newNetworkOfferingId = null;

                if (!newNetworkOfferingMap.containsKey(networkOfferingId)) {
                    // clone the record to
                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_offerings2` SELECT * FROM `cloud`.`network_offerings` WHERE id=?");
                    pstmt2Close.add(pstmt);
                    pstmt.setLong(1, networkOfferingId);
                    pstmt.executeUpdate();

                    pstmt = conn.prepareStatement("UPDATE `cloud`.`network_offerings2` SET id=?, guest_type='Isolated', unique_name=?, name=? WHERE id=?");
                    pstmt2Close.add(pstmt);
                    ntwkOffCount = ntwkOffCount + 1;
                    newNetworkOfferingId = ntwkOffCount;
                    String uniqueName = "Isolated w/o source nat";
                    pstmt.setLong(1, newNetworkOfferingId);
                    pstmt.setString(2, uniqueName);
                    pstmt.setString(3, uniqueName);
                    pstmt.setLong(4, networkOfferingId);
                    pstmt.executeUpdate();

                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_offerings` SELECT * from `cloud`.`network_offerings2` WHERE id=" + newNetworkOfferingId);
                    pstmt2Close.add(pstmt);
                    pstmt.executeUpdate();

                    pstmt = conn.prepareStatement("UPDATE `cloud`.`networks` SET network_offering_id=? where id=?");
                    pstmt2Close.add(pstmt);
                    pstmt.setLong(1, newNetworkOfferingId);
                    pstmt.setLong(2, networkId);
                    pstmt.executeUpdate();

                    newNetworkOfferingMap.put(networkOfferingId, ntwkOffCount);
                } else {
                    pstmt = conn.prepareStatement("UPDATE `cloud`.`networks` SET network_offering_id=? where id=?");
                    pstmt2Close.add(pstmt);
                    newNetworkOfferingId = newNetworkOfferingMap.get(networkOfferingId);
                    pstmt.setLong(1, newNetworkOfferingId);
                    pstmt.setLong(2, networkId);
                    pstmt.executeUpdate();
                }

                s_logger.debug("Successfully updated network offering id=" + networkId + " with new network offering id " + newNetworkOfferingId);
            }

            try {
                pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`networks` DROP COLUMN `switch_to_isolated`");
                pstmt2Close.add(pstmt);
                pstmt.executeUpdate();
            } catch (SQLException ex) {
                // do nothing here
                s_logger.debug("Caught SQLException when trying to drop switch_to_isolated column ", ex);
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to switch networks to isolated", e);
        } finally {
            try {
                pstmt = conn.prepareStatement("DROP TABLE `cloud`.`network_offerings2`");
                pstmt.executeUpdate();
                pstmt.close();
            } catch (SQLException e) {
                s_logger.info("[ignored]",e);
            }
            TransactionLegacy.closePstmts(pstmt2Close);
        }
    }

    private void migrateUserConcentratedPlannerChoice(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("SELECT value FROM `cloud`.`configuration` where name = 'use.user.concentrated.pod.allocation'");
            rs = pstmt.executeQuery();
            Boolean isuserconcentrated = false;
            if (rs.next()) {
                String value = rs.getString(1);
                isuserconcentrated = new Boolean(value);
            }
            rs.close();
            pstmt.close();

            if (isuserconcentrated) {
                String currentAllocationAlgo = "random";
                pstmt = conn.prepareStatement("SELECT value FROM `cloud`.`configuration` where name = 'vm.allocation.algorithm'");
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    currentAllocationAlgo = rs.getString(1);
                }
                rs.close();
                pstmt.close();

                String newAllocAlgo = "userconcentratedpod_random";
                if ("random".equalsIgnoreCase(currentAllocationAlgo)) {
                    newAllocAlgo = "userconcentratedpod_random";
                } else {
                    newAllocAlgo = "userconcentratedpod_firstfit";
                }

                pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = 'vm.allocation.algorithm'");
                pstmt.setString(1, newAllocAlgo);
                pstmt.executeUpdate();

            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to migrate the user_concentrated planner choice", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
                s_logger.info("[ignored]",e);
            }
        }
    }

    protected String fixNetworksWithExternalDevices(Connection conn) {
        List<PreparedStatement> pstmt2Close = new ArrayList<PreparedStatement>();
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;

        //Get zones to upgrade
        List<Long> zoneIds = new ArrayList<Long>();
        try {
            pstmt =
                conn.prepareStatement("select id from `cloud`.`data_center` where lb_provider='F5BigIp' or firewall_provider='JuniperSRX' or gateway_provider='JuniperSRX'");
            pstmt2Close.add(pstmt);
            rs = pstmt.executeQuery();
            while (rs.next()) {
                zoneIds.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            TransactionLegacy.closePstmts(pstmt2Close);
            throw new CloudRuntimeException("Unable to switch networks to the new network offering", e);
        }

        String uniqueName = null;
        HashMap<Long, Long> newNetworkOfferingMap = new HashMap<Long, Long>();

        for (Long zoneId : zoneIds) {
            try {
                // Find the correct network offering
                pstmt = conn.prepareStatement("select id, network_offering_id from `cloud`.`networks` where guest_type='Virtual' and data_center_id=?");
                pstmt2Close.add(pstmt);
                pstmt.setLong(1, zoneId);
                rs = pstmt.executeQuery();
                pstmt = conn.prepareStatement("select count(*) from `cloud`.`network_offerings`");
                rs1 = pstmt.executeQuery();
                long ntwkOffCount = 0;
                while (rs1.next()) {
                    ntwkOffCount = rs1.getLong(1);
                }

                pstmt = conn.prepareStatement("CREATE TEMPORARY TABLE `cloud`.`network_offerings2` ENGINE=MEMORY SELECT * FROM `cloud`.`network_offerings` WHERE id=1");
                pstmt2Close.add(pstmt);
                pstmt.executeUpdate();

                while (rs.next()) {
                    long networkId = rs.getLong(1);
                    long networkOfferingId = rs.getLong(2);
                    s_logger.debug("Updating network offering for the network id=" + networkId + " as it has switch_to_isolated=1");
                    Long newNetworkOfferingId = null;
                    if (!newNetworkOfferingMap.containsKey(networkOfferingId)) {
                        uniqueName = "Isolated with external providers";
                        // clone the record to
                        pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_offerings2` SELECT * FROM `cloud`.`network_offerings` WHERE id=?");
                        pstmt2Close.add(pstmt);
                        pstmt.setLong(1, networkOfferingId);
                        pstmt.executeUpdate();

                        //set the new unique name
                        pstmt = conn.prepareStatement("UPDATE `cloud`.`network_offerings2` SET id=?, unique_name=?, name=? WHERE id=?");
                        pstmt2Close.add(pstmt);
                        ntwkOffCount = ntwkOffCount + 1;
                        newNetworkOfferingId = ntwkOffCount;
                        pstmt.setLong(1, newNetworkOfferingId);
                        pstmt.setString(2, uniqueName);
                        pstmt.setString(3, uniqueName);
                        pstmt.setLong(4, networkOfferingId);
                        pstmt.executeUpdate();

                        pstmt =
                            conn.prepareStatement("INSERT INTO `cloud`.`network_offerings` SELECT * from " + "`cloud`.`network_offerings2` WHERE id=" +
                                newNetworkOfferingId);
                        pstmt2Close.add(pstmt);
                        pstmt.executeUpdate();

                        pstmt = conn.prepareStatement("UPDATE `cloud`.`networks` SET network_offering_id=? where id=?");
                        pstmt2Close.add(pstmt);
                        pstmt.setLong(1, newNetworkOfferingId);
                        pstmt.setLong(2, networkId);
                        pstmt.executeUpdate();

                        newNetworkOfferingMap.put(networkOfferingId, ntwkOffCount);
                    } else {
                        pstmt = conn.prepareStatement("UPDATE `cloud`.`networks` SET network_offering_id=? where id=?");
                        pstmt2Close.add(pstmt);
                        newNetworkOfferingId = newNetworkOfferingMap.get(networkOfferingId);
                        pstmt.setLong(1, newNetworkOfferingId);
                        pstmt.setLong(2, networkId);
                        pstmt.executeUpdate();
                    }

                    s_logger.debug("Successfully updated network id=" + networkId + " with new network offering id " + newNetworkOfferingId);
                }

            } catch (SQLException e) {
                throw new CloudRuntimeException("Unable to switch networks to the new network offering", e);
            } finally {
                try (PreparedStatement dropStatement = conn.prepareStatement("DROP TABLE `cloud`.`network_offerings2`");){
                    dropStatement.executeUpdate();
                } catch (SQLException e) {
                    s_logger.info("[ignored]",e);
                }
                TransactionLegacy.closePstmts(pstmt2Close);
            }
        }

        return uniqueName;
    }
}
