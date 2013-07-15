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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;

import com.cloud.deploy.DeploymentPlanner;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade410to420 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade410to420.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "4.1.0", "4.2.0" };
    }

    @Override
    public String getUpgradedVersion() {
        return "4.2.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-410to420.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-410to420.sql");
        }

        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        upgradeVmwareLabels(conn);
        persistLegacyZones(conn);
        createPlaceHolderNics(conn);
        updateRemoteAccessVpn(conn);
        updateSystemVmTemplates(conn);
        updateCluster_details(conn);
        updatePrimaryStore(conn);
        addEgressFwRulesForSRXGuestNw(conn);
        upgradeEIPNetworkOfferings(conn);
        updateGlobalDeploymentPlanner(conn);
        upgradeDefaultVpcOffering(conn);
        upgradePhysicalNtwksWithInternalLbProvider(conn);
        updateNetworkACLs(conn);
        addHostDetailsIndex(conn);
        updateNetworksForPrivateGateways(conn);
        correctExternalNetworkDevicesSetup(conn);
        removeFirewallServiceFromSharedNetworkOfferingWithSGService(conn);
        fix22xKVMSnapshots(conn);
        addIndexForAlert(conn);
        fixBaremetalForeignKeys(conn);
        // storage refactor related migration
        // TODO: add clean-up scripts to delete the deprecated table.
        migrateSecondaryStorageToImageStore(conn);
        migrateVolumeHostRef(conn);
        migrateTemplateHostRef(conn);
        migrateSnapshotStoreRef(conn);
    }

    private void fixBaremetalForeignKeys(Connection conn) {
        List<String> keys = new ArrayList<String>();
        keys.add("fk_external_dhcp_devices_nsp_id");
        keys.add("fk_external_dhcp_devices_host_id");
        keys.add("fk_external_dhcp_devices_pod_id");
        keys.add("fk_external_dhcp_devices_physical_network_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "baremetal_dhcp_devices", keys, true);

        keys.add("fk_external_pxe_devices_nsp_id");
        keys.add("fk_external_pxe_devices_host_id");
        keys.add("fk_external_pxe_devices_physical_network_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "baremetal_pxe_devices", keys, true);

        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_dhcp_devices` ADD CONSTRAINT `fk_external_dhcp_devices_nsp_id` FOREIGN KEY (`nsp_id`) REFERENCES `physical_network_service_providers` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_dhcp_devices` ADD CONSTRAINT `fk_external_dhcp_devices_host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_dhcp_devices` ADD CONSTRAINT `fk_external_dhcp_devices_pod_id` FOREIGN KEY (`pod_id`) REFERENCES `host_pod_ref`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_dhcp_devices` ADD CONSTRAINT `fk_external_dhcp_devices_physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            s_logger.debug("Added foreign keys for table baremetal_dhcp_devices");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to add foreign keys to baremetal_dhcp_devices table", e);
        }

        try {
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_pxe_devices` ADD CONSTRAINT `fk_external_pxe_devices_nsp_id` FOREIGN KEY (`nsp_id`) REFERENCES `physical_network_service_providers` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_pxe_devices` ADD CONSTRAINT `fk_external_pxe_devices_host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_pxe_devices` ADD CONSTRAINT `fk_external_pxe_devices_physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            s_logger.debug("Added foreign keys for table baremetal_pxe_devices");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to add foreign keys to baremetal_pxe_devices table", e);
        }
    }

    private void addIndexForAlert(Connection conn) {

        //First drop if it exists. (Due to patches shipped to customers some will have the index and some wont.)
        List<String> indexList = new ArrayList<String>();
        s_logger.debug("Dropping index i_alert__last_sent if it exists");
        indexList.add("last_sent"); // in 4.1, we created this index that is not in convention.
        indexList.add("i_alert__last_sent");
        DbUpgradeUtils.dropKeysIfExist(conn, "alert", indexList, false);

        //Now add index.
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`alert` ADD INDEX `i_alert__last_sent`(`last_sent`)");
            pstmt.executeUpdate();
            s_logger.debug("Added index i_alert__last_sent for table alert");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to add index i_alert__last_sent to alert table for the column last_sent", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }

    }

    private void updateSystemVmTemplates(Connection conn) {
        // TODO: system vm template migration after storage refactoring
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        s_logger.debug("Updating System Vm template IDs");
        try{
            //Get all hypervisors in use
            Set<HypervisorType> hypervisorsListInUse = new HashSet<HypervisorType>();
            try {
                pstmt = conn.prepareStatement("select distinct(hypervisor_type) from `cloud`.`cluster` where removed is null");
                rs = pstmt.executeQuery();
                while(rs.next()){
                    switch (HypervisorType.getType(rs.getString(1))) {
                    case XenServer: hypervisorsListInUse.add(HypervisorType.XenServer);
                    break;
                    case KVM:       hypervisorsListInUse.add(HypervisorType.KVM);
                    break;
                    case VMware:    hypervisorsListInUse.add(HypervisorType.VMware);
                    break;
                    case Hyperv:    hypervisorsListInUse.add(HypervisorType.Hyperv);
                    break;
                    case LXC:       hypervisorsListInUse.add(HypervisorType.LXC);
                    break;
                    }
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while listing hypervisors in use", e);
            }

            Map<HypervisorType, String> NewTemplateNameList = new HashMap<HypervisorType, String>(){
                {   put(HypervisorType.XenServer, "systemvm-xenserver-4.2");
                put(HypervisorType.VMware, "systemvm-vmware-4.2");
                put(HypervisorType.KVM, "systemvm-kvm-4.2");
                put(HypervisorType.LXC, "systemvm-lxc-4.2");
                put(HypervisorType.Hyperv, "systemvm-hyperv-4.2");
                }
            };

            Map<HypervisorType, String> routerTemplateConfigurationNames = new HashMap<HypervisorType, String>(){
                {   put(HypervisorType.XenServer, "router.template.xen");
                put(HypervisorType.VMware, "router.template.vmware");
                put(HypervisorType.KVM, "router.template.kvm");
                put(HypervisorType.LXC, "router.template.lxc");
                put(HypervisorType.Hyperv, "router.template.hyperv");
                }
            };

            for (Map.Entry<HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()){
                s_logger.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
                try {
                    //Get 4.2.0 system Vm template Id for corresponding hypervisor
                    pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name like ? and removed is null order by id desc limit 1");
                    pstmt.setString(1, hypervisorAndTemplateName.getValue());
                    rs = pstmt.executeQuery();
                    if(rs.next()){
                        long templateId = rs.getLong(1);
                        rs.close();
                        pstmt.close();
                        // change template type to SYSTEM
                        pstmt = conn.prepareStatement("update `cloud`.`vm_template` set type='SYSTEM' where id = ?");
                        pstmt.setLong(1, templateId);
                        pstmt.executeUpdate();
                        pstmt.close();
                        // update templete ID of system Vms
                        pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = ?");
                        pstmt.setLong(1, templateId);
                        pstmt.setString(2, hypervisorAndTemplateName.getKey().toString());
                        pstmt.executeUpdate();
                        pstmt.close();
                        // Change value of global configuration parameter router.template.* for the corresponding hypervisor
                        pstmt = conn.prepareStatement("INSERT IGNORE INTO `cloud`.`configuration` VALUES ('Advanced', 'DEFAULT', 'NetworkManager', ?, ?, 'Name of the default router template on Xenserver')");
                        pstmt.setString(1, routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()));
                        pstmt.setString(2, hypervisorAndTemplateName.getValue());
                        pstmt.execute();
                        pstmt.close();
                    } else {
                        if (hypervisorsListInUse.contains(hypervisorAndTemplateName.getKey())){
                            throw new CloudRuntimeException("4.2.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. Cannot upgrade system Vms");
                        } else {
                            s_logger.warn("4.2.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. " + hypervisorAndTemplateName.getKey() + " hypervisor is not used, so not failing upgrade");
                        }
                    }
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Error while updating "+ hypervisorAndTemplateName.getKey() +" systemVm template", e);
                }
            }

            s_logger.debug("Updating System Vm Template IDs Complete");
        }
        finally {
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
        /*
        pstmt = null;
        try {
            pstmt = conn.prepareStatement("update vm_template set image_data_store_id = 1 where type = 'SYSTEM' or type = 'BUILTIN'");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to upgrade vm template data store uuid: " + e.toString());
        } finally {
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
        }
         */
    }

    private void updatePrimaryStore(Connection conn) {
        PreparedStatement sql = null;
        PreparedStatement sql2 = null;
        try {
            sql = conn.prepareStatement("update storage_pool set storage_provider_name = ? , scope = ? where pool_type = 'Filesystem' or pool_type = 'LVM'");
            sql.setString(1, DataStoreProvider.DEFAULT_PRIMARY);
            sql.setString(2, "HOST");
            sql.executeUpdate();

            sql2 = conn.prepareStatement("update storage_pool set storage_provider_name = ? , scope = ? where pool_type != 'Filesystem' and pool_type != 'LVM'");
            sql2.setString(1, DataStoreProvider.DEFAULT_PRIMARY);
            sql2.setString(2, "CLUSTER");
            sql2.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to upgrade vm template data store uuid: " + e.toString());
        } finally {
            if (sql != null) {
                try {
                    sql.close();
                } catch (SQLException e) {
                }
            }

            if (sql2 != null) {
                try {
                    sql2.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    //update the cluster_details table with default overcommit ratios.
    private void updateCluster_details(Connection conn) {
        PreparedStatement pstmt = null;
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 =null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement("select id from `cloud`.`cluster`");
            pstmt1=conn.prepareStatement("INSERT INTO `cloud`.`cluster_details` (cluster_id, name, value)  VALUES(?, 'cpuOvercommitRatio', '1')");
            pstmt2=conn.prepareStatement("INSERT INTO `cloud`.`cluster_details` (cluster_id, name, value)  VALUES(?, 'memoryOvercommitRatio', '1')");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                //update cluster_details table with the default overcommit ratios.
                pstmt1.setLong(1,id);
                pstmt1.execute();
                pstmt2.setLong(1,id);
                pstmt2.execute();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update cluster_details with default overcommit ratios.", e);
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
    }


    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-410to420-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-410to420-cleanup.sql");
        }

        return new File[] { new File(script) };
    }

    private String getNewLabel(ResultSet rs, String oldParamValue) {
        int separatorIndex;
        String oldGuestLabel;
        String newGuestLabel = oldParamValue;
        try {
            // No need to iterate because the global param setting applies to all physical networks irrespective of traffic type
            if (rs.next()) {
                oldGuestLabel = rs.getString("vmware_network_label");
                // guestLabel is in format [[<VSWITCHNAME>],VLANID]
                separatorIndex = oldGuestLabel.indexOf(",");
                if(separatorIndex > -1) {
                    newGuestLabel += oldGuestLabel.substring(separatorIndex);
                }
            }
        } catch (SQLException e) {
            s_logger.error(new CloudRuntimeException("Failed to read vmware_network_label : " + e));
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
            } catch (SQLException e) {
            }
        }
        return newGuestLabel;
    }

    private void upgradeVmwareLabels(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rsParams = null;
        ResultSet rsLabel = null;
        String newLabel;
        String trafficType = null;
        String trafficTypeVswitchParam;
        String trafficTypeVswitchParamValue;

        try {
            // update the existing vmware traffic labels
            pstmt = conn.prepareStatement("select name,value from `cloud`.`configuration` where category='Hidden' and value is not NULL and name REGEXP 'vmware\\.*\\.vswitch';");
            rsParams = pstmt.executeQuery();
            while (rsParams.next()) {
                trafficTypeVswitchParam = rsParams.getString("name");
                trafficTypeVswitchParamValue = rsParams.getString("value");
                // When upgraded from 4.0 to 4.1 update physical network traffic label with trafficTypeVswitchParam
                if (trafficTypeVswitchParam.equals("vmware.private.vswitch")) {
                    trafficType = "Management"; //TODO(sateesh): Ignore storage traffic, as required physical network already implemented, anything else tobe done?
                } else if (trafficTypeVswitchParam.equals("vmware.public.vswitch")) {
                    trafficType = "Public";
                } else if (trafficTypeVswitchParam.equals("vmware.guest.vswitch")) {
                    trafficType = "Guest";
                }
                s_logger.debug("Updating vmware label for " + trafficType + " traffic. Update SQL statement is " + pstmt);
                pstmt = conn.prepareStatement("select physical_network_id, traffic_type, vmware_network_label from physical_network_traffic_types where vmware_network_label is not NULL and traffic_type='" + trafficType + "';");
                rsLabel = pstmt.executeQuery();
                newLabel = getNewLabel(rsLabel, trafficTypeVswitchParamValue);
                pstmt = conn.prepareStatement("update physical_network_traffic_types set vmware_network_label = " + newLabel + " where traffic_type = '" + trafficType + "' and vmware_network_label is not NULL;");
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to set vmware traffic labels ", e);
        } finally {
            try {
                if (rsParams != null) {
                    rsParams.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void persistLegacyZones(Connection conn) {
        List<Long> listOfLegacyZones = new ArrayList<Long>();
        PreparedStatement pstmt = null;
        PreparedStatement clustersQuery = null;
        PreparedStatement clusterDetailsQuery = null;
        ResultSet rs = null;
        ResultSet clusters = null;
        ResultSet clusterDetails = null;
        ResultSet dcInfo = null;
        Long vmwareDcId = 1L;
        Long zoneId;
        Long clusterId;
        String clusterHypervisorType;
        boolean legacyZone;
        boolean ignoreZone;
        Long count;
        String dcOfPreviousCluster = null;
        String dcOfCurrentCluster = null;
        String[] tokens;
        String url;
        String user = "";
        String password = "";
        String vc = "";
        String dcName = "";
        String guid;
        String key;
        String value;

        try {
            clustersQuery = conn.prepareStatement("select id, hypervisor_type from `cloud`.`cluster` where removed is NULL");
            pstmt = conn.prepareStatement("select id from `cloud`.`data_center` where removed is NULL");
            rs = pstmt.executeQuery();

            while (rs.next()) {
                zoneId = rs.getLong("id");
                legacyZone = false;
                ignoreZone = true;
                count = 0L;
                // Legacy zone term is meant only for VMware
                // Legacy zone is a zone with atleast 2 clusters & with multiple DCs or VCs
                clusters = clustersQuery.executeQuery();
                if (!clusters.next()) {
                    continue; // Ignore the zone without any clusters
                } else {
                    dcOfPreviousCluster = null;
                    dcOfCurrentCluster = null;
                    do {
                        clusterHypervisorType = clusters.getString("hypervisor_type");
                        clusterId = clusters.getLong("id");
                        if (clusterHypervisorType.equalsIgnoreCase("VMware")) {
                            ignoreZone = false;
                            clusterDetailsQuery = conn.prepareStatement("select value from `cloud`.`cluster_details` where name='url' and cluster_id=?");
                            clusterDetailsQuery.setLong(1, clusterId);
                            clusterDetails = clusterDetailsQuery.executeQuery();
                            clusterDetails.next();
                            url = clusterDetails.getString("value");
                            tokens = url.split("/"); // url format - http://vcenter/dc/cluster
                            vc = tokens[2];
                            dcName = tokens[3];
                            if (count > 0) {
                                dcOfPreviousCluster = dcOfCurrentCluster;
                                dcOfCurrentCluster = dcName + "@" + vc;
                                if (!dcOfPreviousCluster.equals(dcOfCurrentCluster)) {
                                    legacyZone = true;
                                    s_logger.debug("Marking the zone " + zoneId + " as legacy zone.");
                                }
                            }
                        } else {
                            s_logger.debug("Ignoring zone " + zoneId + " with hypervisor type " + clusterHypervisorType);
                            break;
                        }
                        count++;
                    } while (clusters.next());
                    if (ignoreZone) {
                        continue; // Ignore the zone with hypervisors other than VMware
                    }
                }
                if (legacyZone) {
                    listOfLegacyZones.add(zoneId);
                } else {
                    assert(clusterDetails != null) : "Couldn't retrieve details of cluster!";
                    s_logger.debug("Discovered non-legacy zone " + zoneId + ". Processing the zone to associate with VMware datacenter.");

                    clusterDetailsQuery = conn.prepareStatement("select name, value from `cloud`.`cluster_details` where cluster_id=?");
                    clusterDetailsQuery.setLong(1, clusterId);
                    clusterDetails = clusterDetailsQuery.executeQuery();
                    while (clusterDetails.next()) {
                        key = clusterDetails.getString(1);
                        value = clusterDetails.getString(2);
                        if (key.equalsIgnoreCase("username")) {
                            user = value;
                        } else if (key.equalsIgnoreCase("password")) {
                            password = value;
                        }
                    }
                    guid = dcName + "@" + vc;

                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`vmware_data_center` (uuid, name, guid, vcenter_host, username, password) values(?, ?, ?, ?, ?, ?)");
                    pstmt.setString(1, UUID.randomUUID().toString());
                    pstmt.setString(2, dcName);
                    pstmt.setString(3, guid);
                    pstmt.setString(4, vc);
                    pstmt.setString(5, user);
                    pstmt.setString(6, password);
                    pstmt.executeUpdate();

                    pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`vmware_data_center` where guid=?");
                    pstmt.setString(1, guid);
                    dcInfo = pstmt.executeQuery();
                    if(dcInfo.next()) {
                        vmwareDcId = dcInfo.getLong("id");
                    }

                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`vmware_data_center_zone_map` (zone_id, vmware_data_center_id) values(?, ?)");
                    pstmt.setLong(1, zoneId);
                    pstmt.setLong(2, vmwareDcId);
                    pstmt.executeUpdate();
                }
            }
            updateLegacyZones(conn, listOfLegacyZones);
        } catch (SQLException e) {
            String msg = "Unable to discover legacy zones." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (dcInfo != null) {
                    dcInfo.close();
                }
                if (clusters != null) {
                    clusters.close();
                }
                if (clusterDetails != null) {
                    clusterDetails.close();
                }
                if (clustersQuery != null) {
                    clustersQuery.close();
                }
                if (clusterDetailsQuery != null) {
                    clusterDetailsQuery.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void updateLegacyZones(Connection conn, List<Long> zones) {
        PreparedStatement legacyZonesQuery = null;
        //Insert legacy zones into table for legacy zones.
        try {
            legacyZonesQuery = conn.prepareStatement("INSERT INTO `cloud`.`legacy_zones` (zone_id) VALUES (?)");
            for(Long zoneId : zones) {
                legacyZonesQuery.setLong(1, zoneId);
                legacyZonesQuery.executeUpdate();
                s_logger.debug("Inserted zone " + zoneId + " into cloud.legacyzones table");
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable add zones to cloud.legacyzones table.", e);
        } finally {
            try {
                if (legacyZonesQuery != null) {
                    legacyZonesQuery.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void createPlaceHolderNics(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement("SELECT network_id, gateway, ip4_address FROM `cloud`.`nics` WHERE reserver_name IN ('DirectNetworkGuru','DirectPodBasedNetworkGuru') and vm_type='DomainRouter' AND removed IS null");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Long networkId = rs.getLong(1);
                String gateway = rs.getString(2);
                String ip = rs.getString(3);
                String uuid = UUID.randomUUID().toString();
                //Insert placeholder nic for each Domain router nic in Shared network
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`nics` (uuid, ip4_address, gateway, network_id, state, strategy, vm_type) VALUES (?, ?, ?, ?, 'Reserved', 'PlaceHolder', 'DomainRouter')");
                pstmt.setString(1, uuid);
                pstmt.setString(2, ip);
                pstmt.setString(3, gateway);
                pstmt.setLong(4, networkId);
                pstmt.executeUpdate();
                s_logger.debug("Created placeholder nic for the ipAddress " + ip);

            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create placeholder nics", e);
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
    }


    private void updateRemoteAccessVpn(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement("SELECT vpn_server_addr_id FROM `cloud`.`remote_access_vpn`");
            rs = pstmt.executeQuery();
            long id=1;
            while (rs.next()) {
                String uuid = UUID.randomUUID().toString();
                Long ipId = rs.getLong(1);
                pstmt = conn.prepareStatement("UPDATE `cloud`.`remote_access_vpn` set uuid=?, id=? where vpn_server_addr_id=?");
                pstmt.setString(1, uuid);
                pstmt.setLong(2, id);
                pstmt.setLong(3, ipId);
                pstmt.executeUpdate();
                id++;
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update id/uuid of remote_access_vpn table", e);
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
    }

    private void addEgressFwRulesForSRXGuestNw(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rsId = null;
        ResultSet rsNw = null;
        try {
            pstmt = conn.prepareStatement("select network_id FROM `cloud`.`ntwk_service_map` where service='Firewall' and provider='JuniperSRX' ");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long netId = rs.getLong(1);
                //checking for Isolated OR Virtual
                pstmt = conn.prepareStatement("select account_id, domain_id FROM `cloud`.`networks` where (guest_type='Isolated' OR guest_type='Virtual') and traffic_type='Guest' and vpc_id is NULL and (state='implemented' OR state='Shutdown') and id=? ");
                pstmt.setLong(1, netId);
                s_logger.debug("Getting account_id, domain_id from networks table: " + pstmt);
                rsNw = pstmt.executeQuery();

                if(rsNw.next()) {
                    long accountId = rsNw.getLong(1);
                    long domainId = rsNw.getLong(2);

                    //Add new rule for the existing networks
                    s_logger.debug("Adding default egress firewall rule for network " + netId);
                    pstmt = conn.prepareStatement("INSERT INTO firewall_rules (uuid, state, protocol, purpose, account_id, domain_id, network_id, xid, created,  traffic_type) VALUES (?, 'Active', 'all', 'Firewall', ?, ?, ?, ?, now(), 'Egress')");
                    pstmt.setString(1, UUID.randomUUID().toString());
                    pstmt.setLong(2, accountId);
                    pstmt.setLong(3, domainId);
                    pstmt.setLong(4, netId);
                    pstmt.setString(5, UUID.randomUUID().toString());
                    s_logger.debug("Inserting default egress firewall rule " + pstmt);
                    pstmt.executeUpdate();

                    pstmt = conn.prepareStatement("select id from firewall_rules where protocol='all' and network_id=?");
                    pstmt.setLong(1, netId);
                    rsId = pstmt.executeQuery();

                    long firewallRuleId;
                    if(rsId.next()) {
                        firewallRuleId = rsId.getLong(1);
                        pstmt = conn.prepareStatement("insert into firewall_rules_cidrs (firewall_rule_id,source_cidr) values (?, '0.0.0.0/0')");
                        pstmt.setLong(1, firewallRuleId);
                        s_logger.debug("Inserting rule for cidr 0.0.0.0/0 for the new Firewall rule id=" + firewallRuleId + " with statement " + pstmt);
                        pstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to set egress firewall rules ", e);
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
    }

    private void upgradeEIPNetworkOfferings(Connection conn) {

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement("select id, elastic_ip_service from `cloud`.`network_offerings` where traffic_type='Guest'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                // check if elastic IP service is enabled for network offering
                if (rs.getLong(2) != 0) {
                    //update network offering with eip_associate_public_ip set to true
                    pstmt = conn.prepareStatement("UPDATE `cloud`.`network_offerings` set eip_associate_public_ip=? where id=?");
                    pstmt.setBoolean(1, true);
                    pstmt.setLong(2, id);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to set elastic_ip_service for network offerings with EIP service enabled.", e);
        }
    }

    private void updateNetworkACLs(Connection conn) {
        //Fetch all VPC Tiers
        //For each tier create a network ACL and move all the acl_items to network_acl_item table
        // If there are no acl_items for a tier, associate it with default ACL

        s_logger.debug("Updating network ACLs");

        PreparedStatement pstmt = null;
        PreparedStatement pstmtDelete = null;
        ResultSet rs = null;
        ResultSet rsAcls = null;
        ResultSet rsCidr = null;

        //1,2 are default acl Ids, start acl Ids from 3
        long nextAclId = 3;

        try {
            //Get all VPC tiers
            pstmt = conn.prepareStatement("SELECT id, vpc_id, uuid FROM `cloud`.`networks` where vpc_id is not null and removed is null");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Long networkId = rs.getLong(1);
                s_logger.debug("Updating network ACLs for network: "+networkId);
                Long vpcId = rs.getLong(2);
                String tierUuid = rs.getString(3);
                pstmt = conn.prepareStatement("SELECT id, uuid, start_port, end_port, state, protocol, icmp_code, icmp_type, created, traffic_type FROM `cloud`.`firewall_rules` where network_id = ? and purpose = 'NetworkACL'");
                pstmt.setLong(1, networkId);
                rsAcls = pstmt.executeQuery();
                boolean hasAcls = false;
                Long aclId = null;
                int number = 1;
                while(rsAcls.next()){
                    if(!hasAcls){
                        hasAcls = true;
                        aclId = nextAclId++;
                        //create ACL for the tier
                        s_logger.debug("Creating network ACL for tier: "+tierUuid);
                        pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_acl` (id, uuid, vpc_id, description, name) values (?, UUID(), ? , ?, ?)");
                        pstmt.setLong(1, aclId);
                        pstmt.setLong(2, vpcId);
                        pstmt.setString(3, "ACL for tier " + tierUuid);
                        pstmt.setString(4, "tier_" + tierUuid);
                        pstmt.executeUpdate();
                    }

                    Long fwRuleId = rsAcls.getLong(1);
                    String cidr = null;
                    //get cidr from firewall_rules_cidrs
                    pstmt = conn.prepareStatement("SELECT id, source_cidr FROM `cloud`.`firewall_rules_cidrs` where firewall_rule_id = ?");
                    pstmt.setLong(1, fwRuleId);
                    rsCidr = pstmt.executeQuery();
                    while(rsCidr.next()){
                        Long cidrId = rsCidr.getLong(1);
                        String sourceCidr = rsCidr.getString(2);
                        if(cidr == null){
                            cidr = sourceCidr;
                        } else {
                            cidr += ","+sourceCidr;
                        }
                        //Delete cidr entry
                        pstmtDelete = conn.prepareStatement("DELETE FROM `cloud`.`firewall_rules_cidrs` where id = ?");
                        pstmtDelete.setLong(1, cidrId);
                        pstmtDelete.executeUpdate();
                    }


                    String aclItemUuid = rsAcls.getString(2);
                    //Move acl to network_acl_item table
                    s_logger.debug("Moving firewall rule: "+aclItemUuid);
                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`network_acl_item` (uuid, acl_id, start_port, end_port, state, protocol, icmp_code, icmp_type, created, traffic_type, cidr, number, action) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    //uuid
                    pstmt.setString(1, aclItemUuid);
                    //aclId
                    pstmt.setLong(2, aclId);
                    //Start port
                    Integer startPort = rsAcls.getInt(3);
                    if(rsAcls.wasNull()){
                        pstmt.setNull(3, Types.INTEGER);
                    } else {
                        pstmt.setLong(3, startPort);
                    }
                    //End port
                    Integer endPort = rsAcls.getInt(4);
                    if(rsAcls.wasNull()){
                        pstmt.setNull(4, Types.INTEGER);
                    } else {
                        pstmt.setLong(4, endPort);
                    }
                    //State
                    String state = rsAcls.getString(5);
                    pstmt.setString(5, state);
                    //protocol
                    String protocol = rsAcls.getString(6);
                    pstmt.setString(6, protocol);
                    //icmp_code
                    Integer icmpCode = rsAcls.getInt(7);
                    if(rsAcls.wasNull()){
                        pstmt.setNull(7, Types.INTEGER);
                    } else {
                        pstmt.setLong(7, icmpCode);
                    }

                    //icmp_type
                    Integer icmpType = rsAcls.getInt(8);
                    if(rsAcls.wasNull()){
                        pstmt.setNull(8, Types.INTEGER);
                    } else {
                        pstmt.setLong(8, icmpType);
                    }

                    //created
                    Date created = rsAcls.getDate(9);
                    pstmt.setDate(9, created);
                    //traffic type
                    String trafficType = rsAcls.getString(10);
                    pstmt.setString(10, trafficType);

                    //cidr
                    pstmt.setString(11, cidr);
                    //number
                    pstmt.setInt(12, number++);
                    //action
                    pstmt.setString(13, "Allow");
                    pstmt.executeUpdate();

                    //Delete firewall rule
                    pstmtDelete = conn.prepareStatement("DELETE FROM `cloud`.`firewall_rules` where id = ?");
                    pstmtDelete.setLong(1, fwRuleId);
                    pstmtDelete.executeUpdate();
                }
                if(!hasAcls){
                    //no network ACls for this network.
                    // Assign default Deny ACL
                    aclId = NetworkACL.DEFAULT_DENY;
                }
                //Assign acl to network
                pstmt = conn.prepareStatement("UPDATE `cloud`.`networks` set network_acl_id=? where id=?");
                pstmt.setLong(1, aclId);
                pstmt.setLong(2, networkId);
                pstmt.executeUpdate();
            }
            s_logger.debug("Done updating network ACLs ");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to move network acls from firewall rules table to network_acl_item table", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (rsAcls != null) {
                    rsAcls.close();
                }
                if (rsCidr != null) {
                    rsCidr.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void updateGlobalDeploymentPlanner(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn
                    .prepareStatement("select value from `cloud`.`configuration` where name = 'vm.allocation.algorithm'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String globalValue = rs.getString(1);
                String plannerName = "FirstFitPlanner";

                if (globalValue != null) {
                    if (globalValue.equals(DeploymentPlanner.AllocationAlgorithm.random.toString())) {
                        plannerName = "FirstFitPlanner";
                    } else if (globalValue.equals(DeploymentPlanner.AllocationAlgorithm.firstfit.toString())) {
                        plannerName = "FirstFitPlanner";
                    } else if (globalValue.equals(DeploymentPlanner.AllocationAlgorithm.userconcentratedpod_firstfit
                            .toString())) {
                        plannerName = "UserConcentratedPodPlanner";
                    } else if (globalValue.equals(DeploymentPlanner.AllocationAlgorithm.userconcentratedpod_random
                            .toString())) {
                        plannerName = "UserConcentratedPodPlanner";
                    } else if (globalValue.equals(DeploymentPlanner.AllocationAlgorithm.userdispersing.toString())) {
                        plannerName = "UserDispersingPlanner";
                    }
                }
                // update vm.deployment.planner global config
                pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` set value=? where name = 'vm.deployment.planner'");
                pstmt.setString(1, plannerName);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to set vm.deployment.planner global config", e);
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
    }


    private void upgradeDefaultVpcOffering(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement("select distinct map.vpc_offering_id from `cloud`.`vpc_offering_service_map` map, `cloud`.`vpc_offerings` off where off.id=map.vpc_offering_id AND service='Lb'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                //Add internal LB vm as a supported provider for the load balancer service
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`vpc_offering_service_map` (vpc_offering_id, service, provider) VALUES (?,?,?)");
                pstmt.setLong(1, id);
                pstmt.setString(2, "Lb");
                pstmt.setString(3, "InternalLbVm");
                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable update the default VPC offering with the internal lb service", e);
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
    }

    private void upgradePhysicalNtwksWithInternalLbProvider(Connection conn) {

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement("SELECT id FROM `cloud`.`physical_network` where removed is null");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long pNtwkId = rs.getLong(1);
                String uuid = UUID.randomUUID().toString();
                //Add internal LB VM to the list of physical network service providers
                pstmt = conn.prepareStatement("INSERT INTO `cloud`.`physical_network_service_providers` " +
                        "(uuid, physical_network_id, provider_name, state, load_balance_service_provided, destination_physical_network_id)" +
                        " VALUES (?, ?, 'InternalLbVm', 'Enabled', 1, 0)");
                pstmt.setString(1, uuid);
                pstmt.setLong(2, pNtwkId);
                pstmt.executeUpdate();

                //Add internal lb vm to the list of physical network elements
                PreparedStatement pstmt1 = conn.prepareStatement("SELECT id FROM `cloud`.`physical_network_service_providers`" +
                        " WHERE physical_network_id=? AND provider_name='InternalLbVm'");
                pstmt1.setLong(1, pNtwkId);
                ResultSet rs1 = pstmt1.executeQuery();
                while (rs1.next()) {
                    long providerId = rs1.getLong(1);
                    uuid = UUID.randomUUID().toString();
                    pstmt1 = conn.prepareStatement("INSERT INTO `cloud`.`virtual_router_providers` (nsp_id, uuid, type, enabled) VALUES (?, ?, 'InternalLbVm', 1)");
                    pstmt1.setLong(1, providerId);
                    pstmt1.setString(2, uuid);
                    pstmt1.executeUpdate();
                }
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update existing physical networks with internal lb provider", e);
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
    }

    private void addHostDetailsIndex(Connection conn) {
        s_logger.debug("Checking if host_details index exists, if not we will add it");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("SHOW INDEX FROM `cloud`.`host_details` where KEY_NAME = 'fk_host_details__host_id'");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                s_logger.debug("Index already exists on host_details - not adding new one");
            } else {
                // add the index
                PreparedStatement pstmtUpdate = conn.prepareStatement("ALTER IGNORE TABLE `cloud`.`host_details` ADD INDEX `fk_host_details__host_id` (`host_id`)");
                pstmtUpdate.executeUpdate();
                s_logger.debug("Index did not exist on host_details -  added new one");
                pstmtUpdate.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to check/update the host_details index ", e);
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
    }


    private void updateNetworksForPrivateGateways(Connection conn) {

        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            //1) get all non removed gateways
            pstmt = conn.prepareStatement("SELECT network_id, vpc_id FROM `cloud`.`vpc_gateways` WHERE type='Private' AND removed IS null");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Long networkId = rs.getLong(1);
                Long vpcId = rs.getLong(2);
                //2) Update networks with vpc_id if its set to NULL
                pstmt = conn.prepareStatement("UPDATE `cloud`.`networks` set vpc_id=? where id=? and vpc_id is NULL and removed is NULL");
                pstmt.setLong(1, vpcId);
                pstmt.setLong(2, networkId);
                pstmt.executeUpdate();

            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to update private networks with VPC id.", e);
        }
    }

    private void removeFirewallServiceFromSharedNetworkOfferingWithSGService(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement("select id from `cloud`.`network_offerings` where unique_name='DefaultSharedNetworkOfferingWithSGService'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                // remove Firewall service for SG shared network offering
                pstmt = conn.prepareStatement("DELETE from `cloud`.`ntwk_offering_service_map` where network_offering_id=? and service='Firewall'");
                pstmt.setLong(1, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to remove Firewall service for SG shared network offering.", e);
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
    }

    private void fix22xKVMSnapshots(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        s_logger.debug("Updating KVM snapshots");
        try {
            pstmt = conn.prepareStatement("select id, backup_snap_id from `cloud`.`snapshots` where hypervisor_type='KVM' and removed is null and backup_snap_id is not null");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String backUpPath = rs.getString(2);
                // Update Backup Path. Remove anything before /snapshots/
                // e.g 22x Path /mnt/0f14da63-7033-3ca5-bdbe-fa62f4e2f38a/snapshots/1/2/6/i-2-6-VM_ROOT-6_20121219072022
                // Above path should change to /snapshots/1/2/6/i-2-6-VM_ROOT-6_20121219072022
                int index = backUpPath.indexOf("snapshots"+File.separator);
                if (index > 1){
                    String correctedPath = File.separator + backUpPath.substring(index);
                    s_logger.debug("Updating Snapshot with id: "+id+" original backup path: "+backUpPath+ " updated backup path: "+correctedPath);
                    pstmt = conn.prepareStatement("UPDATE `cloud`.`snapshots` set backup_snap_id=? where id = ?");
                    pstmt.setString(1, correctedPath);
                    pstmt.setLong(2, id);
                    pstmt.executeUpdate();
                }
            }
            s_logger.debug("Done updating KVM snapshots");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update backup id for KVM snapshots", e);
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
    }

    // Corrects upgrade for deployment with F5 and SRX devices (pre 3.0) to network offering &
    // network service provider paradigm
    private void correctExternalNetworkDevicesSetup(Connection conn) {
        PreparedStatement zoneSearchStmt = null, pNetworkStmt = null, f5DevicesStmt = null, srxDevicesStmt = null;
        ResultSet zoneResults = null, pNetworksResults = null, f5DevicesResult = null, srxDevicesResult = null;

        try {
            zoneSearchStmt = conn.prepareStatement("SELECT id, networktype FROM `cloud`.`data_center`");
            zoneResults = zoneSearchStmt.executeQuery();
            while (zoneResults.next()) {
                long zoneId = zoneResults.getLong(1);
                String networkType = zoneResults.getString(2);

                if (!com.cloud.dc.DataCenter.NetworkType.Advanced.toString().equalsIgnoreCase(networkType)) {
                    continue;
                }

                pNetworkStmt = conn.prepareStatement("SELECT id FROM `cloud`.`physical_network` where data_center_id=?");
                pNetworkStmt.setLong(1, zoneId);
                pNetworksResults = pNetworkStmt.executeQuery();
                while (pNetworksResults.next()) {
                    long physicalNetworkId = pNetworksResults.getLong(1);
                    PreparedStatement fetchF5NspStmt = conn.prepareStatement("SELECT id from `cloud`.`physical_network_service_providers` where physical_network_id=" + physicalNetworkId
                            + " and provider_name = 'F5BigIp'");
                    ResultSet rsF5NSP = fetchF5NspStmt.executeQuery();
                    boolean hasF5Nsp = rsF5NSP.next();
                    fetchF5NspStmt.close();

                    // if there is no 'F5BigIP' physical network service provider added into physical network then
                    // add 'F5BigIP' as network service provider and add the entry in 'external_load_balancer_devices'
                    if (!hasF5Nsp) {
                        f5DevicesStmt = conn.prepareStatement("SELECT id FROM host WHERE data_center_id=? AND type = 'ExternalLoadBalancer' AND removed IS NULL");
                        f5DevicesStmt.setLong(1, zoneId);
                        f5DevicesResult = f5DevicesStmt.executeQuery();
                        // add F5BigIP provider and provider instance to physical network if there are any external load
                        // balancers added in the zone
                        while (f5DevicesResult.next()) {
                            long f5HostId = f5DevicesResult.getLong(1);;
                            addF5ServiceProvider(conn, physicalNetworkId, zoneId);
                            addF5LoadBalancer(conn, f5HostId, physicalNetworkId);
                        }
                    }

                    PreparedStatement fetchSRXNspStmt = conn.prepareStatement("SELECT id from `cloud`.`physical_network_service_providers` where physical_network_id=" + physicalNetworkId
                            + " and provider_name = 'JuniperSRX'");
                    ResultSet rsSRXNSP = fetchSRXNspStmt.executeQuery();
                    boolean hasSrxNsp = rsSRXNSP.next();
                    fetchSRXNspStmt.close();

                    // if there is no 'JuniperSRX' physical network service provider added into physical network then
                    // add 'JuniperSRX' as network service provider and add the entry in 'external_firewall_devices'
                    if (!hasSrxNsp) {
                        srxDevicesStmt = conn.prepareStatement("SELECT id FROM host WHERE data_center_id=? AND type = 'ExternalFirewall' AND removed IS NULL");
                        srxDevicesStmt.setLong(1, zoneId);
                        srxDevicesResult = srxDevicesStmt.executeQuery();
                        // add JuniperSRX provider and provider instance to physical network if there are any external
                        // firewall instances added in to the zone
                        while (srxDevicesResult.next()) {
                            long srxHostId = srxDevicesResult.getLong(1);
                            // add SRX provider and provider instance to physical network
                            addSrxServiceProvider(conn, physicalNetworkId, zoneId);
                            addSrxFirewall(conn, srxHostId, physicalNetworkId);
                        }
                    }
                }
            }

            // not the network service provider has been provisioned in to physical network, mark all guest network
            // to be using network offering 'Isolated with external providers'
            fixZoneUsingExternalDevices(conn);

            if (zoneResults != null) {
                try {
                    zoneResults.close();
                } catch (SQLException e) {
                }
            }

            if (zoneSearchStmt != null) {
                try {
                    zoneSearchStmt.close();
                } catch (SQLException e) {
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworks", e);
        } finally {

        }
    }

    private void addF5LoadBalancer(Connection conn, long hostId, long physicalNetworkId){
        PreparedStatement pstmtUpdate = null;
        try{
            s_logger.debug("Adding F5 Big IP load balancer with host id " + hostId + " in to physical network" + physicalNetworkId);
            String insertF5 = "INSERT INTO `cloud`.`external_load_balancer_devices` (physical_network_id, host_id, provider_name, " +
                    "device_name, capacity, is_dedicated, device_state, allocation_state, is_inline, is_managed, uuid) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
        }catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding F5 load balancer device" ,  e);
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
        PreparedStatement pstmtUpdate = null;
        try{
            s_logger.debug("Adding SRX firewall device with host id " + hostId + " in to physical network" + physicalNetworkId);
            String insertSrx = "INSERT INTO `cloud`.`external_firewall_devices` (physical_network_id, host_id, provider_name, " +
                    "device_name, capacity, is_dedicated, device_state, allocation_state, uuid) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
        }catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding SRX firewall device ",  e);
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
            s_logger.debug("Adding PhysicalNetworkServiceProvider F5BigIp" + " in to physical network" + physicalNetworkId);
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
        }catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworkServiceProvider F5BigIp", e);
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
        }catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding PhysicalNetworkServiceProvider JuniperSRX" ,  e);
        } finally {
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
        }
    }

    // This method does two things
    //
    // 1) ensure that networks using external load balancer/firewall in deployments prior to release 3.0
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
        long srxDevivceId,  srxHostId;

        try {
            pstmt = conn.prepareStatement("select id from `cloud`.`data_center` where lb_provider='F5BigIp' or firewall_provider='JuniperSRX' or gateway_provider='JuniperSRX'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                zoneIds.add(rs.getLong(1));
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create network to LB & firewall device mapping for networks  that use them", e);
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
        } catch  (SQLException e) {
            throw new CloudRuntimeException("Unable to create network to LB & firewalla device mapping for networks  that use them", e);
        }

        for (Long zoneId : zoneIds) {
            try {
                // find the F5 device id  in the zone
                pstmt = conn.prepareStatement("SELECT id FROM host WHERE data_center_id=? AND type = 'ExternalLoadBalancer' AND removed IS NULL");
                pstmt.setLong(1, zoneId);
                rs = pstmt.executeQuery();
                if (rs.first()) {
                    f5HostId  = rs.getLong(1);
                } else {
                    throw new CloudRuntimeException("Cannot upgrade as there is no F5 load balancer device found in data center " + zoneId);
                }
                pstmt = conn.prepareStatement("SELECT id FROM external_load_balancer_devices WHERE  host_id=?");
                pstmt.setLong(1, f5HostId);
                rs = pstmt.executeQuery();
                if (rs.first()) {
                    f5DeviceId = rs.getLong(1);
                } else {
                    throw new CloudRuntimeException("Cannot upgrade as there is no F5 load balancer device with host ID " + f5HostId + " found in external_load_balancer_device");
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
                    throw new CloudRuntimeException("Cannot upgrade as there is no SRX firewall device found with host ID " + srxHostId + " found in external_firewall_devices");
                }

                // check if network any uses F5 or SRX devices  in the zone
                pstmt = conn.prepareStatement("select id from `cloud`.`networks` where guest_type='Virtual' and data_center_id=? and network_offering_id=? and removed IS NULL");
                pstmt.setLong(1, zoneId);
                pstmt.setLong(2, networkOfferingId);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    // get the network Id
                    networkId = rs.getLong(1);

                    // add mapping for the network in network_external_lb_device_map
                    String insertLbMapping = "INSERT INTO `cloud`.`network_external_lb_device_map` (uuid, network_id, external_load_balancer_device_id, created) VALUES ( ?, ?, ?, now())";
                    pstmtUpdate = conn.prepareStatement(insertLbMapping);
                    pstmtUpdate.setString(1, UUID.randomUUID().toString());
                    pstmtUpdate.setLong(2, networkId);
                    pstmtUpdate.setLong(3, f5DeviceId);
                    pstmtUpdate.executeUpdate();
                    s_logger.debug("Successfully added entry in network_external_lb_device_map for network " +  networkId + " and F5 device ID " +  f5DeviceId);

                    // add mapping for the network in network_external_firewall_device_map
                    String insertFwMapping = "INSERT INTO `cloud`.`network_external_firewall_device_map` (uuid, network_id, external_firewall_device_id, created) VALUES ( ?, ?, ?, now())";
                    pstmtUpdate = conn.prepareStatement(insertFwMapping);
                    pstmtUpdate.setString(1, UUID.randomUUID().toString());
                    pstmtUpdate.setLong(2, networkId);
                    pstmtUpdate.setLong(3, srxDevivceId);
                    pstmtUpdate.executeUpdate();
                    s_logger.debug("Successfully added entry in network_external_firewall_device_map for network " +  networkId + " and SRX device ID " +  srxDevivceId);
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
                    if (!(camlCaseName.equalsIgnoreCase("numRetries") ||
                            camlCaseName.equalsIgnoreCase("publicZone") ||
                            camlCaseName.equalsIgnoreCase("privateZone") ||
                            camlCaseName.equalsIgnoreCase("publicInterface") ||
                            camlCaseName.equalsIgnoreCase("privateInterface") ||
                            camlCaseName.equalsIgnoreCase("usageInterface") )) {
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
            }  finally {
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
            s_logger.info("Successfully upgraded network using F5 and SRX devices to have a entry in the network_external_lb_device_map and network_external_firewall_device_map");
        }
    }

    // migrate secondary storages (NFS, S3, Swift) from host, s3, swift tables to image_store table
    private void migrateSecondaryStorageToImageStore(Connection conn) {
        PreparedStatement storeInsert = null;
        PreparedStatement storeDetailInsert = null;
        PreparedStatement storeQuery = null;
        PreparedStatement s3Query = null;
        PreparedStatement swiftQuery = null;
        PreparedStatement nfsQuery = null;
        ResultSet rs = null;
        ResultSet storeInfo = null;
        Long storeId = null;


        try {
            storeQuery = conn.prepareStatement("select id from `cloud`.`image_store` where uuid = ?");
            storeDetailInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`image_store_details` (store_id, name, value) values(?, ?, ?)");

            /*
            // migrate S3 secondary storage
            storeInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`image_store` (uuid, name, image_provider_name, protocol, scope, role, created) values(?, ?, 'S3', ?, 'REGION', 'Image', ?)");
            s3Query = conn
                    .prepareStatement("select id, uuid, access_key, secret_key, end_point, bucket, https, connection_timeout, max_error_retry, socket_timeout, created from `cloud`.`s3`");
            rs = s3Query.executeQuery();

            while (rs.next()) {
                Long s3_id = rs.getLong("id");
                String s3_uuid = rs.getString("uuid");
                String s3_accesskey = rs.getString("access_key");
                String s3_secretkey = rs.getString("secret_key");
                String s3_endpoint = rs.getString("end_point");
                String s3_bucket = rs.getString("bucket");
                boolean s3_https = rs.getObject("https") != null ? (rs.getInt("https") == 0 ? false : true) : false;
                Integer s3_connectiontimeout = rs.getObject("connection_timeout") != null ? rs
                        .getInt("connection_timeout") : null;
                Integer s3_retry = rs.getObject("max_error_retry") != null ? rs.getInt("max_error_retry") : null;
                Integer s3_sockettimeout = rs.getObject("socket_timeout") != null ? rs.getInt("socket_timeout") : null;
                Date s3_created = rs.getDate("created");

                // insert entry in image_store table and image_store_details
                // table and store s3_id and store_id mapping

                storeInsert.setString(1, s3_uuid);
                storeInsert.setString(2, s3_uuid);
                storeInsert.setString(3, s3_https ? "https" : "http");
                storeInsert.setDate(4, s3_created);
                storeInsert.executeUpdate();

                storeQuery.setString(1, s3_uuid);
                storeInfo = storeQuery.executeQuery();
                if (storeInfo.next()) {
                    storeId = storeInfo.getLong("id");
                }

                Map<String, String> detailMap = new HashMap<String, String>();
                detailMap.put(ApiConstants.S3_ACCESS_KEY, s3_accesskey);
                detailMap.put(ApiConstants.S3_SECRET_KEY, s3_secretkey);
                detailMap.put(ApiConstants.S3_BUCKET_NAME, s3_bucket);
                detailMap.put(ApiConstants.S3_END_POINT, s3_endpoint);
                detailMap.put(ApiConstants.S3_HTTPS_FLAG, String.valueOf(s3_https));
                if (s3_connectiontimeout != null) {
                    detailMap.put(ApiConstants.S3_CONNECTION_TIMEOUT, String.valueOf(s3_connectiontimeout));
                }
                if (s3_retry != null) {
                    detailMap.put(ApiConstants.S3_MAX_ERROR_RETRY, String.valueOf(s3_retry));
                }
                if (s3_sockettimeout != null) {
                    detailMap.put(ApiConstants.S3_SOCKET_TIMEOUT, String.valueOf(s3_sockettimeout));
                }

                Iterator<String> keyIt = detailMap.keySet().iterator();
                while (keyIt.hasNext()) {
                    String key = keyIt.next();
                    String val = detailMap.get(key);
                    storeDetailInsert.setLong(1, storeId);
                    storeDetailInsert.setString(2, key);
                    storeDetailInsert.setString(3, val);
                    storeDetailInsert.executeUpdate();
                }
                s3_store_id_map.put(s3_id, storeId);
            }

            // migrate SWIFT secondary storage
            storeInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`image_store` (uuid, name, image_provider_name, protocol, url, scope, role, created) values(?, ?, 'Swift', 'http', ?, 'REGION', 'Image', ?)");
            swiftQuery = conn
                    .prepareStatement("select id, uuid, url, account, username, key, created from `cloud`.`swift`");
            rs = swiftQuery.executeQuery();

            while (rs.next()) {
                Long swift_id = rs.getLong("id");
                String swift_uuid = rs.getString("uuid");
                String swift_url = rs.getString("url");
                String swift_account = rs.getString("account");
                String swift_username = rs.getString("username");
                String swift_key = rs.getString("key");
                Date swift_created = rs.getDate("created");

                // insert entry in image_store table and image_store_details
                // table and store swift_id and store_id mapping
                storeInsert.setString(1, swift_uuid);
                storeInsert.setString(2, swift_uuid);
                storeInsert.setString(3, swift_url);
                storeInsert.setDate(4, swift_created);
                storeInsert.executeUpdate();

                storeQuery.setString(1, swift_uuid);
                storeInfo = storeQuery.executeQuery();
                if (storeInfo.next()) {
                    storeId = storeInfo.getLong("id");
                }

                Map<String, String> detailMap = new HashMap<String, String>();
                detailMap.put(ApiConstants.ACCOUNT, swift_account);
                detailMap.put(ApiConstants.USERNAME, swift_username);
                detailMap.put(ApiConstants.KEY, swift_key);

                Iterator<String> keyIt = detailMap.keySet().iterator();
                while (keyIt.hasNext()) {
                    String key = keyIt.next();
                    String val = detailMap.get(key);
                    storeDetailInsert.setLong(1, storeId);
                    storeDetailInsert.setString(2, key);
                    storeDetailInsert.setString(3, val);
                    storeDetailInsert.executeUpdate();
                }
                swift_store_id_map.put(swift_id, storeId);
            }
             */

            // migrate NFS secondary storage, for nfs, keep previous host_id as the store_id
            storeInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`image_store` (id, uuid, name, image_provider_name, protocol, url, data_center_id, scope, role, parent, total_size, created) values(?, ?, ?, 'NFS', 'nfs', ?, ?, 'ZONE', 'Image', ?, ?, ?)");
            nfsQuery = conn
                    .prepareStatement("select id, uuid, url, data_center_id, parent, total_size, created from `cloud`.`host` where type = 'SecondaryStorage' and removed is null");
            rs = nfsQuery.executeQuery();

            while (rs.next()) {
                Long nfs_id = rs.getLong("id");
                String nfs_uuid = rs.getString("uuid");
                String nfs_url = rs.getString("url");
                String nfs_parent = rs.getString("parent");
                int nfs_dcid = rs.getInt("data_center_id");
                Long nfs_totalsize = rs.getObject("total_size") != null ? rs.getLong("total_size") : null;
                Date nfs_created = rs.getDate("created");

                // insert entry in image_store table and image_store_details
                // table and store host_id and store_id mapping
                storeInsert.setLong(1, nfs_id);
                storeInsert.setString(2, nfs_uuid);
                storeInsert.setString(3, nfs_uuid);
                storeInsert.setString(4, nfs_url);
                storeInsert.setInt(5, nfs_dcid);
                storeInsert.setString(6, nfs_parent);
                if (nfs_totalsize != null){
                    storeInsert.setLong(7,  nfs_totalsize);
                }
                else{
                    storeInsert.setNull(7, Types.BIGINT);
                }
                storeInsert.setDate(8, nfs_created);
                storeInsert.executeUpdate();

                storeQuery.setString(1, nfs_uuid);
                storeInfo = storeQuery.executeQuery();
                if (storeInfo.next()) {
                    storeId = storeInfo.getLong("id");
                }

                //host_store_id_map.put(nfs_id, storeId);
            }
        }
        catch (SQLException e) {
            String msg = "Unable to migrate secondary storages." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (storeInfo != null) {
                    storeInfo.close();
                }

                if (storeInsert != null) {
                    storeInsert.close();
                }
                if (storeDetailInsert != null) {
                    storeDetailInsert.close();
                }
                if (storeQuery != null) {
                    storeQuery.close();
                }
                if (swiftQuery != null) {
                    swiftQuery.close();
                }
                if (s3Query != null) {
                    s3Query.close();
                }
                if (nfsQuery != null) {
                    nfsQuery.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    // migrate volume_host_ref to volume_store_ref
    private void migrateVolumeHostRef(Connection conn) {
        PreparedStatement volStoreInsert = null;
        PreparedStatement volStoreUpdate = null;

        try {

            volStoreInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`volume_store_ref` (store_id,  volume_id, zone_id, created, last_updated, job_id, download_pct, size, physical_size, download_state, checksum, error_str, local_path, install_path, url, destroyed, state) select host_id, volume_id, zone_id, created, last_updated, job_id, download_pct, size, physical_size, download_state, checksum, error_str, local_path, install_path, url, destroyed, 'Allocated' from `cloud`.`volume_host_ref`");
            volStoreInsert.executeUpdate();

            volStoreUpdate = conn.prepareStatement("update `cloud`.`volume_store_ref` set state = 'Ready' where download_state = 'DOWNLOADED'");
            volStoreUpdate.executeUpdate();
        }
        catch (SQLException e) {
            String msg = "Unable to migrate volume_host_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try{
                if (volStoreInsert != null) {
                    volStoreInsert.close();
                }
                if (volStoreUpdate != null) {
                    volStoreUpdate.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    // migrate template_host_ref to template_store_ref
    private void migrateTemplateHostRef(Connection conn) {
        PreparedStatement tmplStoreInsert = null;
        PreparedStatement tmplStoreUpdate = null;

        try {

            tmplStoreInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`template_store_ref` (store_id,  template_id, created, last_updated, job_id, download_pct, size, physical_size, download_state, error_str, local_path, install_path, url, destroyed, is_copy, store_role, state) select host_id, template_id, created, last_updated, job_id, download_pct, size, physical_size, download_state, error_str, local_path, install_path, url, destroyed, is_copy, 'Image', 'Allocated' from `cloud`.`template_host_ref`");
            tmplStoreInsert.executeUpdate();

            tmplStoreUpdate = conn.prepareStatement("update `cloud`.`template_store_ref` set state = 'Ready' where download_state = 'DOWNLOADED'");
            tmplStoreUpdate.executeUpdate();
        }
        catch (SQLException e) {
            String msg = "Unable to migrate template_host_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try{
                if (tmplStoreInsert != null) {
                    tmplStoreInsert.close();
                }
                if (tmplStoreUpdate != null) {
                    tmplStoreUpdate.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    // migrate some entry contents of snapshots to snapshot_store_ref
    private void migrateSnapshotStoreRef(Connection conn) {
        PreparedStatement snapshotStoreInsert = null;

        try {
            snapshotStoreInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`snapshot_store_ref` (store_id,  snapshot_id, created, size, parent_snapshot_id, install_path, state) select sechost_id, id, created, size, prev_snap_id, path, 'Ready' from `cloud`.`snapshots` where status = 'BackedUp' and sechost_id is not null and removed is null");
            snapshotStoreInsert.executeUpdate();
        }
        catch (SQLException e) {
            String msg = "Unable to migrate snapshot_store_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try{
                if (snapshotStoreInsert != null) {
                    snapshotStoreInsert.close();
                }
            } catch (SQLException e) {
            }
        }
    }
}
