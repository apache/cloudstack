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
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.engine.subsystem.api.storage.DataStoreProvider;

import com.cloud.deploy.DeploymentPlanner;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.network.vpc.NetworkACL;
import com.cloud.utils.Pair;
import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade410to420 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade410to420.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.1.0", "4.2.0"};
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

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        movePrivateZoneToDedicatedResource(conn);
        upgradeVmwareLabels(conn);
        persistLegacyZones(conn);
        persistVswitchConfiguration(conn);
        createPlaceHolderNics(conn);
        updateRemoteAccessVpn(conn);
        updateSystemVmTemplates(conn);
        updateOverCommitRatioClusterDetails(conn);
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
        setKVMSnapshotFlag(conn);
        addIndexForAlert(conn);
        fixBaremetalForeignKeys(conn);
        // storage refactor related migration
        // TODO: add clean-up scripts to delete the deprecated table.
        migrateSecondaryStorageToImageStore(conn);
        migrateVolumeHostRef(conn);
        migrateTemplateHostRef(conn);
        migrateSnapshotStoreRef(conn);
        migrateS3ToImageStore(conn);
        migrateSwiftToImageStore(conn);
        fixNiciraKeys(conn);
        fixRouterKeys(conn);
        encryptSite2SitePSK(conn);
        migrateDatafromIsoIdInVolumesTable(conn);
        setRAWformatForRBDVolumes(conn);
        migrateVolumeOnSecondaryStorage(conn);
        createFullCloneFlag(conn);
        upgradeVpcServiceMap(conn);
        upgradeResourceCount(conn);
    }

    private void createFullCloneFlag(Connection conn) {
        ResultSet rs = null;
        PreparedStatement delete = null;
        PreparedStatement query = null;
        PreparedStatement update = null;
        int numRows = 0;
        try {
            delete = conn.prepareStatement("delete from `cloud`.`configuration` where name='vmware.create.full.clone';");
            delete.executeUpdate();
            query = conn.prepareStatement("select count(*) from `cloud`.`data_center`");
            rs = query.executeQuery();
            if (rs.next()) {
                numRows = rs.getInt(1);
            }
            if (numRows > 0) {
                update =
                    conn.prepareStatement("insert into `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`) VALUES ('Advanced', 'DEFAULT', 'UserVmManager', 'vmware.create.full.clone' , 'false', 'If set to true, creates VMs as full clones on ESX hypervisor');");
            } else {
                update =
                    conn.prepareStatement("insert into `cloud`.`configuration` (`category`, `instance`, `component`, `name`, `value`, `description`) VALUES ('Advanced', 'DEFAULT', 'UserVmManager', 'vmware.create.full.clone' , 'true', 'If set to true, creates VMs as full clones on ESX hypervisor');");
            }
            update.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to set global flag vmware.create.full.clone: ", e);
        } finally {
            if (update != null) {
                try {
                    update.close();
                } catch (SQLException e) {

                }
            }
            if (query != null) {
                try {
                    query.close();
                } catch (SQLException e) {

                }
            }
            if (delete != null) {
                try {
                    delete.close();
                } catch (SQLException e) {

                }
            }
        }
    }

    private void migrateVolumeOnSecondaryStorage(Connection conn) {
        PreparedStatement sql = null;
        try {
            sql = conn.prepareStatement("update `cloud`.`volumes` set state='Uploaded' where state='UploadOp'");
            sql.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to upgrade volume state: ", e);
        } finally {
            if (sql != null) {
                try {
                    sql.close();
                } catch (SQLException e) {

                }
            }
        }
    }

    private void persistVswitchConfiguration(Connection conn) {
        PreparedStatement clustersQuery = null;
        ResultSet clusters = null;
        Long clusterId;
        String clusterHypervisorType;
        final String NEXUS_GLOBAL_CONFIG_PARAM_NAME = "vmware.use.nexus.vswitch";
        final String DVS_GLOBAL_CONFIG_PARAM_NAME = "vmware.use.dvswitch";
        final String VSWITCH_GLOBAL_CONFIG_PARAM_CATEGORY = "Network";
        final String VMWARE_STANDARD_VSWITCH = "vmwaresvs";
        final String NEXUS_1000V_DVSWITCH = "nexusdvs";
        String paramValStr;
        boolean readGlobalConfigParam = false;
        boolean nexusEnabled = false;
        String publicVswitchType = VMWARE_STANDARD_VSWITCH;
        String guestVswitchType = VMWARE_STANDARD_VSWITCH;
        Map<Long, List<Pair<String, String>>> detailsMap = new HashMap<Long, List<Pair<String, String>>>();
        List<Pair<String, String>> detailsList;

        try {
            clustersQuery = conn.prepareStatement("select id, hypervisor_type from `cloud`.`cluster` where removed is NULL");
            clusters = clustersQuery.executeQuery();
            while (clusters.next()) {
                clusterHypervisorType = clusters.getString("hypervisor_type");
                clusterId = clusters.getLong("id");
                if (clusterHypervisorType.equalsIgnoreCase("VMware")) {
                    if (!readGlobalConfigParam) {
                        paramValStr = getConfigurationParameter(conn, VSWITCH_GLOBAL_CONFIG_PARAM_CATEGORY, NEXUS_GLOBAL_CONFIG_PARAM_NAME);
                        if (paramValStr.equalsIgnoreCase("true")) {
                            nexusEnabled = true;
                        }
                    }
                    if (nexusEnabled) {
                        publicVswitchType = NEXUS_1000V_DVSWITCH;
                        guestVswitchType = NEXUS_1000V_DVSWITCH;
                    }
                    detailsList = new ArrayList<Pair<String, String>>();
                    detailsList.add(new Pair<String, String>(ApiConstants.VSWITCH_TYPE_GUEST_TRAFFIC, guestVswitchType));
                    detailsList.add(new Pair<String, String>(ApiConstants.VSWITCH_TYPE_PUBLIC_TRAFFIC, publicVswitchType));
                    detailsMap.put(clusterId, detailsList);

                    updateClusterDetails(conn, detailsMap);
                    s_logger.debug("Persist vSwitch Configuration: Successfully persisted vswitch configuration for cluster " + clusterId);
                } else {
                    s_logger.debug("Persist vSwitch Configuration: Ignoring cluster " + clusterId + " with hypervisor type " + clusterHypervisorType);
                    continue;
                }
            } // End cluster iteration

            if (nexusEnabled) {
                // If Nexus global parameter is true, then set DVS configuration parameter to true. TODOS: Document that this mandates that MS need to be restarted.
                setConfigurationParameter(conn, VSWITCH_GLOBAL_CONFIG_PARAM_CATEGORY, DVS_GLOBAL_CONFIG_PARAM_NAME, "true");
            }
        } catch (SQLException e) {
            String msg = "Unable to persist vswitch configuration of VMware clusters." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (clusters != null) {
                    clusters.close();
                }
                if (clustersQuery != null) {
                    clustersQuery.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void updateClusterDetails(Connection conn, Map<Long, List<Pair<String, String>>> detailsMap) {
        PreparedStatement clusterDetailsInsert = null;
        // Insert cluster details into cloud.cluster_details table for existing VMware clusters
        // Input parameter detailMap is a map of clusterId and list of key value pairs for that cluster
        Long clusterId;
        String key;
        String val;
        List<Pair<String, String>> keyValues;
        try {
            Iterator<Long> clusterIt = detailsMap.keySet().iterator();
            while (clusterIt.hasNext()) {
                clusterId = clusterIt.next();
                keyValues = detailsMap.get(clusterId);
                for (Pair<String, String> keyValuePair : keyValues) {
                    clusterDetailsInsert = conn.prepareStatement("INSERT INTO `cloud`.`cluster_details` (cluster_id, name, value) VALUES (?, ?, ?)");
                    key = keyValuePair.first();
                    val = keyValuePair.second();
                    clusterDetailsInsert.setLong(1, clusterId);
                    clusterDetailsInsert.setString(2, key);
                    clusterDetailsInsert.setString(3, val);
                    clusterDetailsInsert.executeUpdate();
                }
                s_logger.debug("Inserted vswitch configuration details into cloud.cluster_details for cluster with id " + clusterId + ".");
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable insert cluster details into cloud.cluster_details table.", e);
        } finally {
            try {
                if (clusterDetailsInsert != null) {
                    clusterDetailsInsert.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private String getConfigurationParameter(Connection conn, String category, String paramName) {
        ResultSet rs = null;
        PreparedStatement pstmt = null;
        try {
            pstmt =
                conn.prepareStatement("select value from `cloud`.`configuration` where category='" + category + "' and value is not NULL and name = '" + paramName + "';");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                return rs.getString("value");
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable read global configuration parameter " + paramName + ". ", e);
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
        return "false";
    }

    private void setConfigurationParameter(Connection conn, String category, String paramName, String paramVal) {
        PreparedStatement pstmt = null;
        try {
            pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = '" + paramVal + "' WHERE name = '" + paramName + "';");
            s_logger.debug("Updating global configuration parameter " + paramName + " with value " + paramVal + ". Update SQL statement is " + pstmt);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to set global configuration parameter " + paramName + " to " + paramVal + ". ", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void movePrivateZoneToDedicatedResource(Connection conn) {

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        PreparedStatement pstmtUpdate = null;
        PreparedStatement pstmt3 = null;
        ResultSet rs3 = null;

        try {

            pstmt3 = conn.prepareStatement("SELECT distinct(`domain_id`) FROM `cloud`.`data_center` WHERE `domain_id` IS NOT NULL AND removed IS NULL");
            rs3 = pstmt3.executeQuery();

            while (rs3.next()) {
                long domainId = rs3.getLong(1);
                long affinityGroupId = 0;

                // create or find an affinity group for this domain of type
                // 'ExplicitDedication'
                PreparedStatement pstmt2 = null;
                ResultSet rs2 = null;
                pstmt2 =
                    conn.prepareStatement("SELECT affinity_group.id FROM `cloud`.`affinity_group` INNER JOIN `cloud`.`affinity_group_domain_map` ON affinity_group.id=affinity_group_domain_map.affinity_group_id WHERE affinity_group.type = 'ExplicitDedication' AND affinity_group.acl_type = 'Domain'  AND  (affinity_group_domain_map.domain_id = ?)");
                pstmt2.setLong(1, domainId);
                rs2 = pstmt2.executeQuery();
                if (rs2.next()) {
                    // group exists, use it
                    affinityGroupId = rs2.getLong(1);
                } else {
                    // create new group
                    rs2.close();
                    pstmt2.close();

                    pstmt2 = conn.prepareStatement("SELECT name FROM `cloud`.`domain` where id = ?");
                    pstmt2.setLong(1, domainId);
                    rs2 = pstmt2.executeQuery();
                    String domainName = "";
                    if (rs2.next()) {
                        domainName = rs2.getString(1);
                    }
                    rs2.close();
                    pstmt2.close();
                    // create new domain level group for this domain
                    String type = "ExplicitDedication";
                    String uuid = UUID.randomUUID().toString();
                    String groupName = "DedicatedGrp-domain-" + domainName;
                    s_logger.debug("Adding AffinityGroup of type " + type + " for domain id " + domainId);

                    String sql =
                        "INSERT INTO `cloud`.`affinity_group` (`name`, `type`, `uuid`, `description`, `domain_id`, `account_id`, `acl_type`) VALUES (?, ?, ?, ?, 1, 1, 'Domain')";
                    pstmtUpdate = conn.prepareStatement(sql);
                    pstmtUpdate.setString(1, groupName);
                    pstmtUpdate.setString(2, type);
                    pstmtUpdate.setString(3, uuid);
                    pstmtUpdate.setString(4, "dedicated resources group");
                    pstmtUpdate.executeUpdate();
                    pstmtUpdate.close();

                    pstmt2 = conn.prepareStatement("SELECT affinity_group.id FROM `cloud`.`affinity_group` where uuid = ?");
                    pstmt2.setString(1, uuid);
                    rs2 = pstmt2.executeQuery();
                    if (rs2.next()) {
                        affinityGroupId = rs2.getLong(1);
                    }

                    // add the domain map
                    String sqlMap = "INSERT INTO `cloud`.`affinity_group_domain_map` (`domain_id`, `affinity_group_id`) VALUES (?, ?)";
                    pstmtUpdate = conn.prepareStatement(sqlMap);
                    pstmtUpdate.setLong(1, domainId);
                    pstmtUpdate.setLong(2, affinityGroupId);
                    pstmtUpdate.executeUpdate();
                    pstmtUpdate.close();

                }

                rs2.close();
                pstmt2.close();

                pstmt = conn.prepareStatement("SELECT `id` FROM `cloud`.`data_center` WHERE `domain_id` = ? AND removed IS NULL");
                pstmt.setLong(1, domainId);
                rs = pstmt.executeQuery();

                while (rs.next()) {
                    long zoneId = rs.getLong(1);
                    dedicateZone(conn, zoneId, domainId, affinityGroupId);
                }
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while Moving private zone information to dedicated resources", e);
        } finally {
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
            if (rs != null) {
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            }
            if (pstmt != null) {
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
            }
            if (rs3 != null) {
                try {
                    rs3.close();
                } catch (SQLException e) {
                }
            }
            if (pstmt3 != null) {
                try {
                    pstmt3.close();
                } catch (SQLException e) {
                }
            }

        }
    }

    private void dedicateZone(Connection conn, long zoneId, long domainId, long affinityGroupId) {
        PreparedStatement pstmtUpdate2 = null;
        try {
            // create the dedicated resources entry
            String sql = "INSERT INTO `cloud`.`dedicated_resources` (`uuid`,`data_center_id`, `domain_id`, `affinity_group_id`) VALUES (?, ?, ?, ?)";
            pstmtUpdate2 = conn.prepareStatement(sql);
            pstmtUpdate2.setString(1, UUID.randomUUID().toString());
            pstmtUpdate2.setLong(2, zoneId);
            pstmtUpdate2.setLong(3, domainId);
            pstmtUpdate2.setLong(4, affinityGroupId);
            pstmtUpdate2.executeUpdate();
            pstmtUpdate2.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while saving zone to dedicated resources", e);
        } finally {
            if (pstmtUpdate2 != null) {
                try {
                    pstmtUpdate2.close();
                } catch (SQLException e) {
                }
            }
        }
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
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_dhcp_devices` ADD CONSTRAINT `fk_external_dhcp_devices_nsp_id` FOREIGN KEY (`nsp_id`) REFERENCES `physical_network_service_providers` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_dhcp_devices` ADD CONSTRAINT `fk_external_dhcp_devices_host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            pstmt.close();
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_dhcp_devices` ADD CONSTRAINT `fk_external_dhcp_devices_physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            s_logger.debug("Added foreign keys for table baremetal_dhcp_devices");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to add foreign keys to baremetal_dhcp_devices table", e);
        }

        try {
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_pxe_devices` ADD CONSTRAINT `fk_external_pxe_devices_nsp_id` FOREIGN KEY (`nsp_id`) REFERENCES `physical_network_service_providers` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_pxe_devices` ADD CONSTRAINT `fk_external_pxe_devices_host_id` FOREIGN KEY (`host_id`) REFERENCES `host`(`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`baremetal_pxe_devices` ADD CONSTRAINT `fk_external_pxe_devices_physical_network_id` FOREIGN KEY (`physical_network_id`) REFERENCES `physical_network`(`id`) ON DELETE CASCADE");
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

    private void dropUploadTable(Connection conn) {

        PreparedStatement pstmt0 = null;
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        PreparedStatement pstmt3 = null;

        ResultSet rs0 = null;
        ResultSet rs2 = null;

        try {
            // Read upload table - Templates
            s_logger.debug("Populating template_store_ref table");
            pstmt0 = conn.prepareStatement("SELECT url, created, type_id, host_id from upload where type=?");
            pstmt0.setString(1, "TEMPLATE");
            rs0 = pstmt0.executeQuery();
            pstmt1 = conn.prepareStatement("UPDATE template_store_ref SET download_url=?, download_url_created=? where template_id=? and store_id=?");

            //Update template_store_ref
            while (rs0.next()) {
                pstmt1.setString(1, rs0.getString("url"));
                pstmt1.setDate(2, rs0.getDate("created"));
                pstmt1.setLong(3, rs0.getLong("type_id"));
                pstmt1.setLong(4, rs0.getLong("host_id"));
                pstmt1.executeUpdate();
            }

            // Read upload table - Volumes
            s_logger.debug("Populating volume store ref table");
            pstmt2 = conn.prepareStatement("SELECT url, created, type_id, host_id, install_path from upload where type=?");
            pstmt2.setString(1, "VOLUME");
            rs2 = pstmt2.executeQuery();

            pstmt3 =
                conn.prepareStatement("INSERT IGNORE INTO volume_store_ref (volume_id, store_id, zone_id, created, state, download_url, download_url_created, install_path) VALUES (?,?,?,?,?,?,?,?)");
            //insert into template_store_ref
            while (rs2.next()) {
                pstmt3.setLong(1, rs2.getLong("type_id"));
                pstmt3.setLong(2, rs2.getLong("host_id"));
                pstmt3.setLong(3, 1l);// ???
                pstmt3.setDate(4, rs2.getDate("created"));
                pstmt3.setString(5, "Ready");
                pstmt3.setString(6, rs2.getString("url"));
                pstmt3.setDate(7, rs2.getDate("created"));
                pstmt3.setString(8, rs2.getString("install_path"));
                pstmt3.executeUpdate();
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable add date into template/volume store ref from upload table.", e);
        } finally {
            try {
                if (pstmt0 != null) {
                    pstmt0.close();
                }
                if (pstmt1 != null) {
                    pstmt1.close();
                }
                if (pstmt2 != null) {
                    pstmt2.close();
                }
                if (pstmt3 != null) {
                    pstmt3.close();
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
        try {
            //Get all hypervisors in use
            Set<HypervisorType> hypervisorsListInUse = new HashSet<HypervisorType>();
            try {
                pstmt = conn.prepareStatement("select distinct(hypervisor_type) from `cloud`.`cluster` where removed is null");
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    switch (HypervisorType.getType(rs.getString(1))) {
                        case XenServer:
                            hypervisorsListInUse.add(HypervisorType.XenServer);
                            break;
                        case KVM:
                            hypervisorsListInUse.add(HypervisorType.KVM);
                            break;
                        case VMware:
                            hypervisorsListInUse.add(HypervisorType.VMware);
                            break;
                        case Hyperv:
                            hypervisorsListInUse.add(HypervisorType.Hyperv);
                            break;
                        case LXC:
                            hypervisorsListInUse.add(HypervisorType.LXC);
                            break;
                    }
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while listing hypervisors in use", e);
            }

            Map<HypervisorType, String> NewTemplateNameList = new HashMap<HypervisorType, String>() {
                {
                    put(HypervisorType.XenServer, "systemvm-xenserver-4.2");
                    put(HypervisorType.VMware, "systemvm-vmware-4.2");
                    put(HypervisorType.KVM, "systemvm-kvm-4.2");
                    put(HypervisorType.LXC, "systemvm-lxc-4.2");
                    put(HypervisorType.Hyperv, "systemvm-hyperv-4.2");
                }
            };

            Map<HypervisorType, String> routerTemplateConfigurationNames = new HashMap<HypervisorType, String>() {
                {
                    put(HypervisorType.XenServer, "router.template.xen");
                    put(HypervisorType.VMware, "router.template.vmware");
                    put(HypervisorType.KVM, "router.template.kvm");
                    put(HypervisorType.LXC, "router.template.lxc");
                    put(HypervisorType.Hyperv, "router.template.hyperv");
                }
            };

            Map<HypervisorType, String> newTemplateUrl = new HashMap<HypervisorType, String>() {
                {
                    put(HypervisorType.XenServer, "http://download.cloud.com/templates/4.2/systemvmtemplate-2013-07-12-master-xen.vhd.bz2");
                    put(HypervisorType.VMware, "http://download.cloud.com/templates/4.2/systemvmtemplate-4.2-vh7.ova");
                    put(HypervisorType.KVM, "http://download.cloud.com/templates/4.2/systemvmtemplate-2013-06-12-master-kvm.qcow2.bz2");
                    put(HypervisorType.LXC, "http://download.cloud.com/templates/acton/acton-systemvm-02062012.qcow2.bz2");
                    put(HypervisorType.Hyperv, "http://download.cloud.com/templates/4.2/systemvmtemplate-2013-06-12-master-xen.vhd.bz2");
                }
            };

            Map<HypervisorType, String> newTemplateChecksum = new HashMap<HypervisorType, String>() {
                {
                    put(HypervisorType.XenServer, "fb1b6e032a160d86f2c28feb5add6d83");
                    put(HypervisorType.VMware, "8fde62b1089e5844a9cd3b9b953f9596");
                    put(HypervisorType.KVM, "6cea42b2633841648040becb588bd8f0");
                    put(HypervisorType.LXC, "2755de1f9ef2ce4d6f2bee2efbb4da92");
                    put(HypervisorType.Hyperv, "fb1b6e032a160d86f2c28feb5add6d83");
                }
            };

            for (Map.Entry<HypervisorType, String> hypervisorAndTemplateName : NewTemplateNameList.entrySet()) {
                s_logger.debug("Updating " + hypervisorAndTemplateName.getKey() + " System Vms");
                try {
                    //Get 4.2.0 system Vm template Id for corresponding hypervisor
                    pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = ? and removed is null order by id desc limit 1");
                    pstmt.setString(1, hypervisorAndTemplateName.getValue());
                    rs = pstmt.executeQuery();
                    if (rs.next()) {
                        long templateId = rs.getLong(1);
                        rs.close();
                        pstmt.close();
                        // Mark the old system templates as removed
                        pstmt =
                            conn.prepareStatement("UPDATE `cloud`.`vm_template` SET removed = now() WHERE hypervisor_type = ? AND type = 'SYSTEM' AND removed is null");
                        pstmt.setString(1, hypervisorAndTemplateName.getKey().toString());
                        pstmt.executeUpdate();
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
                        pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = ?");
                        pstmt.setString(1, hypervisorAndTemplateName.getValue());
                        pstmt.setString(2, routerTemplateConfigurationNames.get(hypervisorAndTemplateName.getKey()));
                        pstmt.executeUpdate();
                        pstmt.close();
                    } else {
                        if (hypervisorsListInUse.contains(hypervisorAndTemplateName.getKey())) {
                            // throw new CloudRuntimeException("4.2.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. Cannot upgrade system Vms");
                        } else {
                            s_logger.warn("4.2.0 " + hypervisorAndTemplateName.getKey() + " SystemVm template not found. " + hypervisorAndTemplateName.getKey() +
                                " hypervisor is not used, so not failing upgrade");
                            // Update the latest template URLs for corresponding hypervisor
                            pstmt =
                                conn.prepareStatement("UPDATE `cloud`.`vm_template` SET url = ? , checksum = ? WHERE hypervisor_type = ? AND type = 'SYSTEM' AND removed is null order by id desc limit 1");
                            pstmt.setString(1, newTemplateUrl.get(hypervisorAndTemplateName.getKey()));
                            pstmt.setString(2, newTemplateChecksum.get(hypervisorAndTemplateName.getKey()));
                            pstmt.setString(3, hypervisorAndTemplateName.getKey().toString());
                            pstmt.executeUpdate();
                            pstmt.close();
                        }
                    }
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Error while updating " + hypervisorAndTemplateName.getKey() + " systemVm template", e);
                }
            }
            try {
                pstmt = conn.prepareStatement("UPDATE `cloud`.`vm_template` set dynamically_scalable = 1 where name = ? and type = 'SYSTEM'");
                pstmt.setString(1, NewTemplateNameList.get(HypervisorType.VMware));
                pstmt.executeUpdate();
                pstmt.close();
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while updating dynamically_scalable flag to 1 for SYSTEM template systemvm-vmware-4.2");
            }
            s_logger.debug("Updating System Vm Template IDs Complete");
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

    //KVM snapshot flag: only turn on if Customers is using snapshot;
    private void setKVMSnapshotFlag(Connection conn) {
        s_logger.debug("Verify and set the KVM snapshot flag if snapshot was used. ");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            int numRows = 0;
            pstmt = conn.prepareStatement("select count(*) from `cloud`.`snapshots` where hypervisor_type = 'KVM'");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                numRows = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
            if (numRows > 0) {
                //Add the configuration flag
                pstmt = conn.prepareStatement("UPDATE `cloud`.`configuration` SET value = ? WHERE name = 'kvm.snapshot.enabled'");
                pstmt.setString(1, "true");
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to read the snapshot table for KVM upgrade. ", e);
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
        s_logger.debug("Done set KVM snapshot flag. ");
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
    private void updateOverCommitRatioClusterDetails(Connection conn) {
        PreparedStatement pstmt = null;
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        PreparedStatement pstmt3 = null;
        ResultSet rs1 = null;
        ResultSet rscpu_global = null;
        ResultSet rsmem_global = null;
        try {
            pstmt = conn.prepareStatement("select id, hypervisor_type from `cloud`.`cluster` WHERE removed IS NULL");
            pstmt1 = conn.prepareStatement("INSERT INTO `cloud`.`cluster_details` (cluster_id, name, value)  VALUES(?, 'cpuOvercommitRatio', ?)");
            pstmt2 = conn.prepareStatement("INSERT INTO `cloud`.`cluster_details` (cluster_id, name, value)  VALUES(?, 'memoryOvercommitRatio', ?)");
            pstmt3 = conn.prepareStatement("select value from `cloud`.`configuration` where name=?");
            pstmt3.setString(1, "cpu.overprovisioning.factor");
            rscpu_global = pstmt3.executeQuery();
            String global_cpu_overprovisioning_factor = "1";
            if (rscpu_global.next())
                global_cpu_overprovisioning_factor = rscpu_global.getString(1);
            pstmt3.setString(1, "mem.overprovisioning.factor");
            rsmem_global = pstmt3.executeQuery();
            String global_mem_overprovisioning_factor = "1";
            if (rsmem_global.next())
                global_mem_overprovisioning_factor = rsmem_global.getString(1);
            rs1 = pstmt.executeQuery();

            while (rs1.next()) {
                long id = rs1.getLong(1);
                String hypervisor_type = rs1.getString(2);
                if (HypervisorType.VMware.toString().equalsIgnoreCase(hypervisor_type)) {
                    pstmt1.setLong(1, id);
                    pstmt1.setString(2, global_cpu_overprovisioning_factor);
                    pstmt1.execute();
                    pstmt2.setLong(1, id);
                    pstmt2.setString(2, global_mem_overprovisioning_factor);
                    pstmt2.execute();
                } else {
                    //update cluster_details table with the default overcommit ratios.
                    pstmt1.setLong(1, id);
                    pstmt1.setString(2, "1");
                    pstmt1.execute();
                    pstmt2.setLong(1, id);
                    pstmt2.setString(2, "1");
                    pstmt2.execute();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update cluster_details with default overcommit ratios.", e);
        } finally {
            try {
                if (rs1 != null) {
                    rs1.close();
                }
                if (rsmem_global != null) {
                    rsmem_global.close();
                }
                if (rscpu_global != null) {
                    rscpu_global.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
                if (pstmt2 != null) {
                    pstmt2.close();
                }
                if (pstmt3 != null) {
                    pstmt3.close();
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

        return new File[] {new File(script)};
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
                if (separatorIndex > -1) {
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
            pstmt =
                conn.prepareStatement("select name,value from `cloud`.`configuration` where category='Hidden' and value is not NULL and name REGEXP 'vmware*.vswitch';");
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
                pstmt =
                    conn.prepareStatement("select physical_network_id, traffic_type, vmware_network_label from physical_network_traffic_types where vmware_network_label is not NULL and traffic_type='" +
                        trafficType + "';");
                rsLabel = pstmt.executeQuery();
                newLabel = getNewLabel(rsLabel, trafficTypeVswitchParamValue);
                pstmt =
                    conn.prepareStatement("update physical_network_traffic_types set vmware_network_label = " + newLabel + " where traffic_type = '" + trafficType +
                        "' and vmware_network_label is not NULL;");
                s_logger.debug("Updating vmware label for " + trafficType + " traffic. Update SQL statement is " + pstmt);
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
            pstmt = conn.prepareStatement("select id from `cloud`.`data_center` where removed is NULL");
            rs = pstmt.executeQuery();

            while (rs.next()) {
                zoneId = rs.getLong("id");
                clustersQuery = conn.prepareStatement("select id, hypervisor_type from `cloud`.`cluster` where removed is NULL AND data_center_id=?");
                clustersQuery.setLong(1, zoneId);
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
                            dcOfPreviousCluster = dcOfCurrentCluster;
                            dcOfCurrentCluster = dcName + "@" + vc;
                            if (count > 0) {
                                if (!dcOfPreviousCluster.equalsIgnoreCase(dcOfCurrentCluster)) {
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
                    assert (clusterDetails != null) : "Couldn't retrieve details of cluster!";
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

                    pstmt =
                        conn.prepareStatement("INSERT INTO `cloud`.`vmware_data_center` (uuid, name, guid, vcenter_host, username, password) values(?, ?, ?, ?, ?, ?)");
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
                    if (dcInfo.next()) {
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
            for (Long zoneId : zones) {
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
            pstmt =
                conn.prepareStatement("SELECT network_id, gateway, ip4_address FROM `cloud`.`nics` WHERE reserver_name IN ('DirectNetworkGuru','DirectPodBasedNetworkGuru') and vm_type='DomainRouter' AND removed IS null");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                Long networkId = rs.getLong(1);
                String gateway = rs.getString(2);
                String ip = rs.getString(3);
                String uuid = UUID.randomUUID().toString();
                //Insert placeholder nic for each Domain router nic in Shared network
                pstmt =
                    conn.prepareStatement("INSERT INTO `cloud`.`nics` (uuid, ip4_address, gateway, network_id, state, strategy, vm_type, default_nic, created) VALUES (?, ?, ?, ?, 'Reserved', 'PlaceHolder', 'DomainRouter', 0, now())");
                pstmt.setString(1, uuid);
                pstmt.setString(2, ip);
                pstmt.setString(3, gateway);
                pstmt.setLong(4, networkId);
                pstmt.executeUpdate();
                s_logger.debug("Created placeholder nic for the ipAddress " + ip + " and network " + networkId);

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
            long id = 1;
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
                pstmt =
                    conn.prepareStatement("select account_id, domain_id FROM `cloud`.`networks` where (guest_type='Isolated' OR guest_type='Virtual') and traffic_type='Guest' and vpc_id is NULL and (state='implemented' OR state='Shutdown') and id=? ");
                pstmt.setLong(1, netId);
                s_logger.debug("Getting account_id, domain_id from networks table: " + pstmt);
                rsNw = pstmt.executeQuery();

                if (rsNw.next()) {
                    long accountId = rsNw.getLong(1);
                    long domainId = rsNw.getLong(2);

                    //Add new rule for the existing networks
                    s_logger.debug("Adding default egress firewall rule for network " + netId);
                    pstmt =
                        conn.prepareStatement("INSERT INTO firewall_rules (uuid, state, protocol, purpose, account_id, domain_id, network_id, xid, created,  traffic_type) VALUES (?, 'Active', 'all', 'Firewall', ?, ?, ?, ?, now(), 'Egress')");
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
                    if (rsId.next()) {
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
                s_logger.debug("Updating network ACLs for network: " + networkId);
                Long vpcId = rs.getLong(2);
                String tierUuid = rs.getString(3);
                pstmt =
                    conn.prepareStatement("SELECT id, uuid, start_port, end_port, state, protocol, icmp_code, icmp_type, created, traffic_type FROM `cloud`.`firewall_rules` where network_id = ? and purpose = 'NetworkACL'");
                pstmt.setLong(1, networkId);
                rsAcls = pstmt.executeQuery();
                boolean hasAcls = false;
                Long aclId = null;
                int number = 1;
                while (rsAcls.next()) {
                    if (!hasAcls) {
                        hasAcls = true;
                        aclId = nextAclId++;
                        //create ACL for the tier
                        s_logger.debug("Creating network ACL for tier: " + tierUuid);
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
                    while (rsCidr.next()) {
                        Long cidrId = rsCidr.getLong(1);
                        String sourceCidr = rsCidr.getString(2);
                        if (cidr == null) {
                            cidr = sourceCidr;
                        } else {
                            cidr += "," + sourceCidr;
                        }
                        //Delete cidr entry
                        pstmtDelete = conn.prepareStatement("DELETE FROM `cloud`.`firewall_rules_cidrs` where id = ?");
                        pstmtDelete.setLong(1, cidrId);
                        pstmtDelete.executeUpdate();
                    }

                    String aclItemUuid = rsAcls.getString(2);
                    //Move acl to network_acl_item table
                    s_logger.debug("Moving firewall rule: " + aclItemUuid);
                    pstmt =
                        conn.prepareStatement("INSERT INTO `cloud`.`network_acl_item` (uuid, acl_id, start_port, end_port, state, protocol, icmp_code, icmp_type, created, traffic_type, cidr, number, action) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ? )");
                    //uuid
                    pstmt.setString(1, aclItemUuid);
                    //aclId
                    pstmt.setLong(2, aclId);
                    //Start port
                    Integer startPort = rsAcls.getInt(3);
                    if (rsAcls.wasNull()) {
                        pstmt.setNull(3, Types.INTEGER);
                    } else {
                        pstmt.setLong(3, startPort);
                    }
                    //End port
                    Integer endPort = rsAcls.getInt(4);
                    if (rsAcls.wasNull()) {
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
                    if (rsAcls.wasNull()) {
                        pstmt.setNull(7, Types.INTEGER);
                    } else {
                        pstmt.setLong(7, icmpCode);
                    }

                    //icmp_type
                    Integer icmpType = rsAcls.getInt(8);
                    if (rsAcls.wasNull()) {
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
                if (!hasAcls) {
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
            pstmt = conn.prepareStatement("select value from `cloud`.`configuration` where name = 'vm.allocation.algorithm'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String globalValue = rs.getString(1);
                String plannerName = "FirstFitPlanner";

                if (globalValue != null) {
                    if (globalValue.equals(DeploymentPlanner.AllocationAlgorithm.random.toString())) {
                        plannerName = "FirstFitPlanner";
                    } else if (globalValue.equals(DeploymentPlanner.AllocationAlgorithm.firstfit.toString())) {
                        plannerName = "FirstFitPlanner";
                    } else if (globalValue.equals(DeploymentPlanner.AllocationAlgorithm.userconcentratedpod_firstfit.toString())) {
                        plannerName = "UserConcentratedPodPlanner";
                    } else if (globalValue.equals(DeploymentPlanner.AllocationAlgorithm.userconcentratedpod_random.toString())) {
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
            pstmt =
                conn.prepareStatement("select distinct map.vpc_offering_id from `cloud`.`vpc_offering_service_map` map, `cloud`.`vpc_offerings` off where off.id=map.vpc_offering_id AND service='Lb'");
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
                pstmt =
                    conn.prepareStatement("INSERT INTO `cloud`.`physical_network_service_providers` "
                        + "(uuid, physical_network_id, provider_name, state, load_balance_service_provided, destination_physical_network_id)"
                        + " VALUES (?, ?, 'InternalLbVm', 'Enabled', 1, 0)");
                pstmt.setString(1, uuid);
                pstmt.setLong(2, pNtwkId);
                pstmt.executeUpdate();

                //Add internal lb vm to the list of physical network elements
                PreparedStatement pstmt1 =
                    conn.prepareStatement("SELECT id FROM `cloud`.`physical_network_service_providers`" + " WHERE physical_network_id=? AND provider_name='InternalLbVm'");
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
            pstmt =
                conn.prepareStatement("select id, backup_snap_id from `cloud`.`snapshots` where hypervisor_type='KVM' and removed is null and backup_snap_id is not null");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String backUpPath = rs.getString(2);
                // Update Backup Path. Remove anything before /snapshots/
                // e.g 22x Path /mnt/0f14da63-7033-3ca5-bdbe-fa62f4e2f38a/snapshots/1/2/6/i-2-6-VM_ROOT-6_20121219072022
                // Above path should change to /snapshots/1/2/6/i-2-6-VM_ROOT-6_20121219072022
                int index = backUpPath.indexOf("snapshots" + File.separator);
                if (index > 1) {
                    String correctedPath = backUpPath.substring(index);
                    s_logger.debug("Updating Snapshot with id: " + id + " original backup path: " + backUpPath + " updated backup path: " + correctedPath);
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
                    PreparedStatement fetchF5NspStmt =
                        conn.prepareStatement("SELECT id from `cloud`.`physical_network_service_providers` where physical_network_id=" + physicalNetworkId +
                            " and provider_name = 'F5BigIp'");
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
                            long f5HostId = f5DevicesResult.getLong(1);
                            ;
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

    private void addF5LoadBalancer(Connection conn, long hostId, long physicalNetworkId) {
        PreparedStatement pstmtUpdate = null;
        try {
            s_logger.debug("Adding F5 Big IP load balancer with host id " + hostId + " in to physical network" + physicalNetworkId);
            String insertF5 =
                "INSERT INTO `cloud`.`external_load_balancer_devices` (physical_network_id, host_id, provider_name, "
                    + "device_name, capacity, is_dedicated, device_state, allocation_state, is_managed, uuid) VALUES ( ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
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
            pstmtUpdate.setString(10, UUID.randomUUID().toString());
            pstmtUpdate.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while adding F5 load balancer device", e);
        } finally {
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
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
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
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
            if (pstmtUpdate != null) {
                try {
                    pstmtUpdate.close();
                } catch (SQLException e) {
                }
            }
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
        long srxDevivceId, srxHostId;

        try {
            pstmt =
                conn.prepareStatement("select id from `cloud`.`data_center` where lb_provider='F5BigIp' or firewall_provider='JuniperSRX' or gateway_provider='JuniperSRX'");
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

    // migrate secondary storages NFS from host tables to image_store table
    private void migrateSecondaryStorageToImageStore(Connection conn) {
        PreparedStatement storeInsert = null;
        PreparedStatement storeDetailInsert = null;
        PreparedStatement nfsQuery = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet storeInfo = null;

        s_logger.debug("Migrating secondary storage to image store");
        boolean hasS3orSwift = false;
        try {
            s_logger.debug("Checking if we need to migrate NFS secondary storage to image store or staging store");
            int numRows = 0;
            pstmt = conn.prepareStatement("select count(*) from `cloud`.`s3`");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                numRows = rs.getInt(1);
            }
            rs.close();
            pstmt.close();
            if (numRows > 0) {
                hasS3orSwift = true;
            } else {
                // check if there is swift storage
                pstmt = conn.prepareStatement("select count(*) from `cloud`.`swift`");
                rs = pstmt.executeQuery();
                if (rs.next()) {
                    numRows = rs.getInt(1);
                }
                rs.close();
                pstmt.close();
                if (numRows > 0) {
                    hasS3orSwift = true;
                }
            }

            String store_role = "Image";
            if (hasS3orSwift) {
                store_role = "ImageCache";
            }

            s_logger.debug("Migrating NFS secondary storage to " + store_role + " store");

            storeDetailInsert = conn.prepareStatement("INSERT INTO `cloud`.`image_store_details` (store_id, name, value) values(?, ?, ?)");

            // migrate NFS secondary storage, for nfs, keep previous host_id as the store_id
            storeInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`image_store` (id, uuid, name, image_provider_name, protocol, url, data_center_id, scope, role, parent, total_size, created) values(?, ?, ?, 'NFS', 'nfs', ?, ?, 'ZONE', ?, ?, ?, ?)");
            nfsQuery =
                conn.prepareStatement("select id, uuid, url, data_center_id, parent, total_size, created from `cloud`.`host` where type = 'SecondaryStorage' and removed is null");
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
                storeInsert.setString(6, store_role);
                storeInsert.setString(7, nfs_parent);
                if (nfs_totalsize != null) {
                    storeInsert.setLong(8, nfs_totalsize);
                } else {
                    storeInsert.setNull(8, Types.BIGINT);
                }
                storeInsert.setDate(9, nfs_created);
                storeInsert.executeUpdate();
            }

            s_logger.debug("Marking NFS secondary storage in host table as removed");
            pstmt = conn.prepareStatement("UPDATE `cloud`.`host` SET removed = now() WHERE type = 'SecondaryStorage' and removed is null");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
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
                if (nfsQuery != null) {
                    nfsQuery.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed migrating secondary storage to image store");
    }

    // migrate volume_host_ref to volume_store_ref
    private void migrateVolumeHostRef(Connection conn) {
        PreparedStatement volStoreInsert = null;
        PreparedStatement volStoreUpdate = null;

        s_logger.debug("Updating volume_store_ref table from volume_host_ref table");
        try {
            volStoreInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`volume_store_ref` (store_id,  volume_id, zone_id, created, last_updated, job_id, download_pct, size, physical_size, download_state, checksum, error_str, local_path, install_path, url, destroyed, update_count, ref_cnt, state) select host_id, volume_id, zone_id, created, last_updated, job_id, download_pct, size, physical_size, download_state, checksum, error_str, local_path, install_path, url, destroyed, 0, 0, 'Allocated' from `cloud`.`volume_host_ref`");
            int rowCount = volStoreInsert.executeUpdate();
            s_logger.debug("Insert modified " + rowCount + " rows");

            volStoreUpdate = conn.prepareStatement("update `cloud`.`volume_store_ref` set state = 'Ready' where download_state = 'DOWNLOADED'");
            rowCount = volStoreUpdate.executeUpdate();
            s_logger.debug("Update modified " + rowCount + " rows");
        } catch (SQLException e) {
            String msg = "Unable to migrate volume_host_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (volStoreInsert != null) {
                    volStoreInsert.close();
                }
                if (volStoreUpdate != null) {
                    volStoreUpdate.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed updating volume_store_ref table from volume_host_ref table");
    }

    // migrate template_host_ref to template_store_ref
    private void migrateTemplateHostRef(Connection conn) {
        PreparedStatement tmplStoreInsert = null;
        PreparedStatement tmplStoreUpdate = null;

        s_logger.debug("Updating template_store_ref table from template_host_ref table");
        try {
            tmplStoreInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`template_store_ref` (store_id,  template_id, created, last_updated, job_id, download_pct, size, physical_size, download_state, error_str, local_path, install_path, url, destroyed, is_copy, update_count, ref_cnt, store_role, state) select host_id, template_id, created, last_updated, job_id, download_pct, size, physical_size, download_state, error_str, local_path, install_path, url, destroyed, is_copy, 0, 0, 'Image', 'Allocated' from `cloud`.`template_host_ref`");
            int rowCount = tmplStoreInsert.executeUpdate();
            s_logger.debug("Insert modified " + rowCount + " rows");

            tmplStoreUpdate = conn.prepareStatement("update `cloud`.`template_store_ref` set state = 'Ready' where download_state = 'DOWNLOADED'");
            rowCount = tmplStoreUpdate.executeUpdate();
            s_logger.debug("Update modified " + rowCount + " rows");
        } catch (SQLException e) {
            String msg = "Unable to migrate template_host_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (tmplStoreInsert != null) {
                    tmplStoreInsert.close();
                }
                if (tmplStoreUpdate != null) {
                    tmplStoreUpdate.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed updating template_store_ref table from template_host_ref table");
    }

    // migrate some entry contents of snapshots to snapshot_store_ref
    private void migrateSnapshotStoreRef(Connection conn) {
        PreparedStatement snapshotStoreInsert = null;

        s_logger.debug("Updating snapshot_store_ref table from snapshots table");
        try {
            //Update all snapshots except KVM snapshots
            snapshotStoreInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`snapshot_store_ref` (store_id,  snapshot_id, created, size, parent_snapshot_id, install_path, volume_id, update_count, ref_cnt, store_role, state) select sechost_id, id, created, size, prev_snap_id, CONCAT('snapshots', '/', account_id, '/', volume_id, '/', backup_snap_id), volume_id, 0, 0, 'Image', 'Ready' from `cloud`.`snapshots` where status = 'BackedUp' and hypervisor_type <> 'KVM' and sechost_id is not null and removed is null");
            int rowCount = snapshotStoreInsert.executeUpdate();
            s_logger.debug("Inserted " + rowCount + " snapshots into snapshot_store_ref");

            //backsnap_id for KVM snapshots is complate path. CONCAT is not required
            snapshotStoreInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`snapshot_store_ref` (store_id,  snapshot_id, created, size, parent_snapshot_id, install_path, volume_id, update_count, ref_cnt, store_role, state) select sechost_id, id, created, size, prev_snap_id, backup_snap_id, volume_id, 0, 0, 'Image', 'Ready' from `cloud`.`snapshots` where status = 'BackedUp' and hypervisor_type = 'KVM' and sechost_id is not null and removed is null");
            rowCount = snapshotStoreInsert.executeUpdate();
            s_logger.debug("Inserted " + rowCount + " KVM snapshots into snapshot_store_ref");
        } catch (SQLException e) {
            String msg = "Unable to migrate snapshot_store_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (snapshotStoreInsert != null) {
                    snapshotStoreInsert.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed updating snapshot_store_ref table from snapshots table");
    }

    // migrate secondary storages S3 from s3 tables to image_store table
    private void migrateS3ToImageStore(Connection conn) {
        PreparedStatement storeInsert = null;
        PreparedStatement storeDetailInsert = null;
        PreparedStatement storeQuery = null;
        PreparedStatement s3Query = null;
        ResultSet rs = null;
        ResultSet storeInfo = null;
        Long storeId = null;
        Map<Long, Long> s3_store_id_map = new HashMap<Long, Long>();

        s_logger.debug("Migrating S3 to image store");
        try {
            storeQuery = conn.prepareStatement("select id from `cloud`.`image_store` where uuid = ?");
            storeDetailInsert = conn.prepareStatement("INSERT INTO `cloud`.`image_store_details` (store_id, name, value) values(?, ?, ?)");

            // migrate S3 to image_store
            storeInsert = conn.prepareStatement("INSERT INTO `cloud`.`image_store` (uuid, name, image_provider_name, protocol, scope, role, created) " +
                "values(?, ?, 'S3', ?, 'REGION', 'Image', ?)");
            s3Query = conn.prepareStatement("select id, uuid, access_key, secret_key, end_point, bucket, https, connection_timeout, " +
                "max_error_retry, socket_timeout, created from `cloud`.`s3`");
            rs = s3Query.executeQuery();

            while (rs.next()) {
                Long s3_id = rs.getLong("id");
                String s3_uuid = rs.getString("uuid");
                String s3_accesskey = rs.getString("access_key");
                String s3_secretkey = rs.getString("secret_key");
                String s3_endpoint = rs.getString("end_point");
                String s3_bucket = rs.getString("bucket");
                boolean s3_https = rs.getObject("https") != null ? (rs.getInt("https") == 0 ? false : true) : false;
                Integer s3_connectiontimeout = rs.getObject("connection_timeout") != null ? rs.getInt("connection_timeout") : null;
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
        } catch (SQLException e) {
            String msg = "Unable to migrate S3 secondary storages." + e.getMessage();
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
                if (s3Query != null) {
                    s3Query.close();
                }
            } catch (SQLException e) {
            }
        }

        s_logger.debug("Migrating template_s3_ref to template_store_ref");
        migrateTemplateS3Ref(conn, s3_store_id_map);

        s_logger.debug("Migrating s3 backedup snapshots to snapshot_store_ref");
        migrateSnapshotS3Ref(conn, s3_store_id_map);

        s_logger.debug("Completed migrating S3 secondary storage to image store");
    }

    // migrate template_s3_ref to template_store_ref
    private void migrateTemplateS3Ref(Connection conn, Map<Long, Long> s3StoreMap) {
        PreparedStatement tmplStoreInsert = null;
        PreparedStatement s3Query = null;
        ResultSet rs = null;

        s_logger.debug("Updating template_store_ref table from template_s3_ref table");
        try {
            tmplStoreInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`template_store_ref` (store_id,  template_id, created, download_pct, size, physical_size, download_state, local_path, install_path, update_count, ref_cnt, store_role, state) values(?, ?, ?, 100, ?, ?, 'DOWNLOADED', '?', '?', 0, 0, 'Image', 'Ready')");
            s3Query =
                conn.prepareStatement("select template_s3_ref.s3_id, template_s3_ref.template_id, template_s3_ref.created, template_s3_ref.size, template_s3_ref.physical_size, vm_template.account_id from `cloud`.`template_s3_ref`, `cloud`.`vm_template` where vm_template.id = template_s3_ref.template_id");
            rs = s3Query.executeQuery();

            while (rs.next()) {
                Long s3_id = rs.getLong("s3_id");
                Long s3_tmpl_id = rs.getLong("template_id");
                Date s3_created = rs.getDate("created");
                Long s3_size = rs.getObject("size") != null ? rs.getLong("size") : null;
                Long s3_psize = rs.getObject("physical_size") != null ? rs.getLong("physical_size") : null;
                Long account_id = rs.getLong("account_id");

                tmplStoreInsert.setLong(1, s3StoreMap.get(s3_id));
                tmplStoreInsert.setLong(2, s3_tmpl_id);
                tmplStoreInsert.setDate(3, s3_created);
                if (s3_size != null) {
                    tmplStoreInsert.setLong(4, s3_size);
                } else {
                    tmplStoreInsert.setNull(4, Types.BIGINT);
                }
                if (s3_psize != null) {
                    tmplStoreInsert.setLong(5, s3_psize);
                } else {
                    tmplStoreInsert.setNull(5, Types.BIGINT);
                }
                String path = "template/tmpl/" + account_id + "/" + s3_tmpl_id;
                tmplStoreInsert.setString(6, path);
                tmplStoreInsert.setString(7, path);
                tmplStoreInsert.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Unable to migrate template_s3_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (tmplStoreInsert != null) {
                    tmplStoreInsert.close();
                }
                if (s3Query != null) {
                    s3Query.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed migrating template_s3_ref table.");
    }

    // migrate some entry contents of snapshots to snapshot_store_ref
    private void migrateSnapshotS3Ref(Connection conn, Map<Long, Long> s3StoreMap) {
        PreparedStatement snapshotStoreInsert = null;
        PreparedStatement s3Query = null;
        ResultSet rs = null;

        s_logger.debug("Updating snapshot_store_ref table from snapshots table for s3");
        try {
            snapshotStoreInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`snapshot_store_ref` (store_id,  snapshot_id, created, size, parent_snapshot_id, install_path, volume_id, update_count, ref_cnt, store_role, state) values(?, ?, ?, ?, ?, ?, ?, 0, 0, 'Image', 'Ready')");
            s3Query =
                conn.prepareStatement("select s3_id, id, created, size, prev_snap_id, CONCAT('snapshots', '/', account_id, '/', volume_id, '/', backup_snap_id), volume_id, 0, 0, 'Image', 'Ready' from `cloud`.`snapshots` where status = 'BackedUp' and hypervisor_type <> 'KVM' and s3_id is not null and removed is null");
            rs = s3Query.executeQuery();

            while (rs.next()) {
                Long s3_id = rs.getLong("s3_id");
                Long snapshot_id = rs.getLong("id");
                Date s3_created = rs.getDate("created");
                Long s3_size = rs.getObject("size") != null ? rs.getLong("size") : null;
                Long s3_prev_id = rs.getObject("prev_snap_id") != null ? rs.getLong("prev_snap_id") : null;
                String install_path = rs.getString(6);
                Long s3_vol_id = rs.getLong("volume_id");

                snapshotStoreInsert.setLong(1, s3StoreMap.get(s3_id));
                snapshotStoreInsert.setLong(2, snapshot_id);
                snapshotStoreInsert.setDate(3, s3_created);
                if (s3_size != null) {
                    snapshotStoreInsert.setLong(4, s3_size);
                } else {
                    snapshotStoreInsert.setNull(4, Types.BIGINT);
                }
                if (s3_prev_id != null) {
                    snapshotStoreInsert.setLong(5, s3_prev_id);
                } else {
                    snapshotStoreInsert.setNull(5, Types.BIGINT);
                }
                snapshotStoreInsert.setString(6, install_path);
                snapshotStoreInsert.setLong(7, s3_vol_id);
                snapshotStoreInsert.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Unable to migrate s3 backedup snapshots to snapshot_store_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (snapshotStoreInsert != null) {
                    snapshotStoreInsert.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed updating snapshot_store_ref table from s3 snapshots entries");
    }

    // migrate secondary storages Swift from swift tables to image_store table
    private void migrateSwiftToImageStore(Connection conn) {
        PreparedStatement storeInsert = null;
        PreparedStatement storeDetailInsert = null;
        PreparedStatement storeQuery = null;
        PreparedStatement swiftQuery = null;
        ResultSet rs = null;
        ResultSet storeInfo = null;
        Long storeId = null;
        Map<Long, Long> swift_store_id_map = new HashMap<Long, Long>();

        s_logger.debug("Migrating Swift to image store");
        try {
            storeQuery = conn.prepareStatement("select id from `cloud`.`image_store` where uuid = ?");
            storeDetailInsert = conn.prepareStatement("INSERT INTO `cloud`.`image_store_details` (store_id, name, value) values(?, ?, ?)");

            // migrate SWIFT secondary storage
            storeInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`image_store` (uuid, name, image_provider_name, protocol, url, scope, role, created) values(?, ?, 'Swift', 'http', ?, 'REGION', 'Image', ?)");
            swiftQuery = conn.prepareStatement("select id, uuid, url, account, username, swift.key, created from `cloud`.`swift`");
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
        } catch (SQLException e) {
            String msg = "Unable to migrate swift secondary storages." + e.getMessage();
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
            } catch (SQLException e) {
            }
        }

        s_logger.debug("Migrating template_swift_ref to template_store_ref");
        migrateTemplateSwiftRef(conn, swift_store_id_map);

        s_logger.debug("Migrating swift backedup snapshots to snapshot_store_ref");
        migrateSnapshotSwiftRef(conn, swift_store_id_map);

        s_logger.debug("Completed migrating Swift secondary storage to image store");
    }

    // migrate template_s3_ref to template_store_ref
    private void migrateTemplateSwiftRef(Connection conn, Map<Long, Long> swiftStoreMap) {
        PreparedStatement tmplStoreInsert = null;
        PreparedStatement s3Query = null;
        ResultSet rs = null;

        s_logger.debug("Updating template_store_ref table from template_swift_ref table");
        try {
            tmplStoreInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`template_store_ref` (store_id,  template_id, created, download_pct, size, physical_size, download_state, local_path, install_path, update_count, ref_cnt, store_role, state) values(?, ?, ?, 100, ?, ?, 'DOWNLOADED', '?', '?', 0, 0, 'Image', 'Ready')");
            s3Query = conn.prepareStatement("select swift_id, template_id, created, path, size, physical_size from `cloud`.`template_swift_ref`");
            rs = s3Query.executeQuery();

            while (rs.next()) {
                Long swift_id = rs.getLong("swift_id");
                Long tmpl_id = rs.getLong("template_id");
                Date created = rs.getDate("created");
                String path = rs.getString("path");
                Long size = rs.getObject("size") != null ? rs.getLong("size") : null;
                Long psize = rs.getObject("physical_size") != null ? rs.getLong("physical_size") : null;

                tmplStoreInsert.setLong(1, swiftStoreMap.get(swift_id));
                tmplStoreInsert.setLong(2, tmpl_id);
                tmplStoreInsert.setDate(3, created);
                if (size != null) {
                    tmplStoreInsert.setLong(4, size);
                } else {
                    tmplStoreInsert.setNull(4, Types.BIGINT);
                }
                if (psize != null) {
                    tmplStoreInsert.setLong(5, psize);
                } else {
                    tmplStoreInsert.setNull(5, Types.BIGINT);
                }
                tmplStoreInsert.setString(6, path);
                tmplStoreInsert.setString(7, path);
                tmplStoreInsert.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Unable to migrate template_swift_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (tmplStoreInsert != null) {
                    tmplStoreInsert.close();
                }
                if (s3Query != null) {
                    s3Query.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed migrating template_swift_ref table.");
    }

    // migrate some entry contents of snapshots to snapshot_store_ref
    private void migrateSnapshotSwiftRef(Connection conn, Map<Long, Long> swiftStoreMap) {
        PreparedStatement snapshotStoreInsert = null;
        PreparedStatement s3Query = null;
        ResultSet rs = null;

        s_logger.debug("Updating snapshot_store_ref table from snapshots table for swift");
        try {
            snapshotStoreInsert =
                conn.prepareStatement("INSERT INTO `cloud`.`snapshot_store_ref` (store_id,  snapshot_id, created, size, parent_snapshot_id, install_path, volume_id, update_count, ref_cnt, store_role, state) values(?, ?, ?, ?, ?, ?, ?, 0, 0, 'Image', 'Ready')");
            s3Query =
                conn.prepareStatement("select swift_id, id, created, size, prev_snap_id, CONCAT('snapshots', '/', account_id, '/', volume_id, '/', backup_snap_id), volume_id, 0, 0, 'Image', 'Ready' from `cloud`.`snapshots` where status = 'BackedUp' and hypervisor_type <> 'KVM' and swift_id is not null and removed is null");
            rs = s3Query.executeQuery();

            while (rs.next()) {
                Long swift_id = rs.getLong("swift_id");
                Long snapshot_id = rs.getLong("id");
                Date created = rs.getDate("created");
                Long size = rs.getLong("size");
                Long prev_id = rs.getLong("prev_snap_id");
                String install_path = rs.getString(6);
                Long vol_id = rs.getLong("volume_id");

                snapshotStoreInsert.setLong(1, swiftStoreMap.get(swift_id));
                snapshotStoreInsert.setLong(2, snapshot_id);
                snapshotStoreInsert.setDate(3, created);
                snapshotStoreInsert.setLong(4, size);
                snapshotStoreInsert.setLong(5, prev_id);
                snapshotStoreInsert.setString(6, install_path);
                snapshotStoreInsert.setLong(7, vol_id);
                snapshotStoreInsert.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Unable to migrate swift backedup snapshots to snapshot_store_ref." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (snapshotStoreInsert != null) {
                    snapshotStoreInsert.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed updating snapshot_store_ref table from swift snapshots entries");
    }

    private void fixNiciraKeys(Connection conn) {
        //First drop the key if it exists.
        List<String> keys = new ArrayList<String>();
        s_logger.debug("Dropping foreign key fk_nicira_nvp_nic_map__nic from the table nicira_nvp_nic_map if it exists");
        keys.add("fk_nicira_nvp_nic_map__nic");
        DbUpgradeUtils.dropKeysIfExist(conn, "nicira_nvp_nic_map", keys, true);

        //Now add foreign key.
        PreparedStatement pstmt = null;
        try {
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`nicira_nvp_nic_map` ADD CONSTRAINT `fk_nicira_nvp_nic_map__nic` FOREIGN KEY (`nic`) REFERENCES `nics` (`uuid`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            s_logger.debug("Added foreign key fk_nicira_nvp_nic_map__nic to the table nicira_nvp_nic_map");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to add foreign key fk_nicira_nvp_nic_map__nic to the table nicira_nvp_nic_map", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void fixRouterKeys(Connection conn) {
        //First drop the key if it exists.
        List<String> keys = new ArrayList<String>();
        s_logger.debug("Dropping foreign key fk_router_network_ref__router_id from the table router_network_ref if it exists");
        keys.add("fk_router_network_ref__router_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "router_network_ref", keys, true);

        //Now add foreign key.
        PreparedStatement pstmt = null;
        try {
            pstmt =
                conn.prepareStatement("ALTER TABLE `cloud`.`router_network_ref` ADD CONSTRAINT `fk_router_network_ref__router_id` FOREIGN KEY (`router_id`) REFERENCES `domain_router` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            s_logger.debug("Added foreign key fk_router_network_ref__router_id to the table router_network_ref");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to add foreign key fk_router_network_ref__router_id to the table router_network_ref", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void encryptSite2SitePSK(Connection conn) {
        s_logger.debug("Encrypting Site2Site Customer Gateway pre-shared key");
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select id, ipsec_psk from `cloud`.`s2s_customer_gateway`");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if (value == null) {
                    continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement("update `cloud`.`s2s_customer_gateway` set ipsec_psk=? where id=?");
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to encrypt Site2Site Customer Gateway pre-shared key ", e);
        } catch (UnsupportedEncodingException e) {
            throw new CloudRuntimeException("Unable to encrypt Site2Site Customer Gateway pre-shared key ", e);
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
        s_logger.debug("Done encrypting Site2Site Customer Gateway pre-shared key");
    }

    protected void updateConcurrentConnectionsInNetworkOfferings(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        ResultSet rs2 = null;
        try {
            try {
                pstmt =
                    conn.prepareStatement("SELECT *  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'cloud' AND TABLE_NAME = 'network_offerings' AND COLUMN_NAME = 'concurrent_connections'");
                rs = pstmt.executeQuery();
                if (!rs.next()) {
                    pstmt =
                        conn.prepareStatement("ALTER TABLE `cloud`.`network_offerings` ADD COLUMN `concurrent_connections` int(10) unsigned COMMENT 'Load Balancer(haproxy) maximum number of concurrent connections(global max)'");
                    pstmt.executeUpdate();
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("migration of concurrent connections from network_detais failed");
            }

            pstmt = conn.prepareStatement("select network_id, value from `cloud`.`network_details` where name='maxconnections'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long networkId = rs.getLong(1);
                int maxconnections = Integer.parseInt(rs.getString(2));
                pstmt = conn.prepareStatement("select network_offering_id from `cloud`.`networks` where id= ?");
                pstmt.setLong(1, networkId);
                rs1 = pstmt.executeQuery();
                if (rs1.next()) {
                    long network_offering_id = rs1.getLong(1);
                    pstmt = conn.prepareStatement("select concurrent_connections from `cloud`.`network_offerings` where id= ?");
                    pstmt.setLong(1, network_offering_id);
                    rs2 = pstmt.executeQuery();
                    if ((!rs2.next()) || (rs2.getInt(1) < maxconnections)) {
                        pstmt = conn.prepareStatement("update network_offerings set concurrent_connections=? where id=?");
                        pstmt.setInt(1, maxconnections);
                        pstmt.setLong(2, network_offering_id);
                        pstmt.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (rs1 != null) {
                    rs1.close();
                }
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void migrateDatafromIsoIdInVolumesTable(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            pstmt = conn.prepareStatement("SELECT iso_id1 From `cloud`.`volumes`");
            rs = pstmt.executeQuery();
            if (rs.next()) {
                pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`volumes` DROP COLUMN `iso_id`");
                pstmt.executeUpdate();
                pstmt =
                    conn.prepareStatement("ALTER TABLE `cloud`.`volumes` CHANGE COLUMN `iso_id1` `iso_id` bigint(20) unsigned COMMENT 'The id of the iso from which the volume was created'");
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            //implies iso_id1 is not present, so do nothing.
        }
    }

    protected void setRAWformatForRBDVolumes(Connection conn) {
        PreparedStatement pstmt = null;
        try {
            s_logger.debug("Setting format to RAW for all volumes on RBD primary storage pools");
            pstmt = conn.prepareStatement("UPDATE volumes SET format = 'RAW' WHERE pool_id IN(SELECT id FROM storage_pool WHERE pool_type = 'RBD')");
            pstmt.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to update volume format to RAW for volumes on RBD pools due to exception ", e);
        }
    }

    private void upgradeVpcServiceMap(Connection conn) {
        s_logger.debug("Upgrading VPC service Map");
        PreparedStatement listVpc = null;
        PreparedStatement listServiceProviders = null;
        PreparedStatement insertProviders = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        try {
            //Get all vpc Ids along with vpc offering Id
            listVpc = conn.prepareStatement("SELECT id, vpc_offering_id FROM `cloud`.`vpc` where removed is NULL");
            rs = listVpc.executeQuery();
            while (rs.next()) {
                long vpc_id = rs.getLong(1);
                long offering_id = rs.getLong(2);
                //list all services and providers in offering
                listServiceProviders = conn.prepareStatement("SELECT service, provider FROM `cloud`.`vpc_offering_service_map` where vpc_offering_id = ?");
                listServiceProviders.setLong(1, offering_id);
                rs1 = listServiceProviders.executeQuery();
                //Insert entries in vpc_service_map
                while (rs1.next()) {
                    String service = rs1.getString(1);
                    String provider = rs1.getString(2);
                    insertProviders =
                        conn.prepareStatement("INSERT INTO `cloud`.`vpc_service_map` (`vpc_id`, `service`, `provider`, `created`) VALUES (?, ?, ?, now());");
                    insertProviders.setLong(1, vpc_id);
                    insertProviders.setString(2, service);
                    insertProviders.setString(3, provider);
                    insertProviders.executeUpdate();
                }
                s_logger.debug("Upgraded service map for VPC: " + vpc_id);
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Error during VPC service map upgrade", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (rs1 != null) {
                    rs1.close();
                }
                if (listVpc != null) {
                    listVpc.close();
                }
                if (listServiceProviders != null) {
                    listServiceProviders.close();
                }
                if (insertProviders != null) {
                    insertProviders.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void upgradeResourceCount(Connection conn) {
        s_logger.debug("upgradeResourceCount start");
        PreparedStatement pstmt1 = null;
        PreparedStatement pstmt2 = null;
        PreparedStatement pstmt3 = null;
        PreparedStatement pstmt4 = null;
        PreparedStatement pstmt5 = null;
        ResultSet rs = null;
        ResultSet rsAccount = null;
        ResultSet rsCount = null;
        try {
            pstmt1 = conn.prepareStatement("select id, domain_id FROM `cloud`.`account` where removed is NULL ");
            rsAccount = pstmt1.executeQuery();
            while (rsAccount.next()) {
                long account_id = rsAccount.getLong(1);
                long domain_id = rsAccount.getLong(2);
                // 1. update cpu,memory for all accounts
                pstmt2 =
                    conn.prepareStatement("SELECT SUM(service_offering.cpu), SUM(service_offering.ram_size)" + " FROM `cloud`.`vm_instance`, `cloud`.`service_offering`"
                        + " WHERE vm_instance.service_offering_id = service_offering.id AND vm_instance.account_id = ?" + " AND vm_instance.removed is NULL"
                        + " AND vm_instance.vm_type='User' AND state not in ('Destroyed', 'Error', 'Expunging')");
                pstmt2.setLong(1, account_id);
                rsCount = pstmt2.executeQuery();
                if (rsCount.next()) {
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "cpu", rsCount.getLong(1));
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "memory", rsCount.getLong(2));
                } else {
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "cpu", 0L);
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "memory", 0L);
                }
                // 2. update primary_storage for all accounts
                pstmt3 =
                    conn.prepareStatement("SELECT sum(size) FROM `cloud`.`volumes` WHERE account_id= ?"
                        + " AND (path is not NULL OR state in ('Allocated')) AND removed is NULL"
                        + " AND instance_id IN (SELECT id FROM `cloud`.`vm_instance` WHERE vm_type='User')");
                pstmt3.setLong(1, account_id);
                rsCount = pstmt3.executeQuery();
                if (rsCount.next()) {
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "primary_storage", rsCount.getLong(1));
                } else {
                    upgradeResourceCountforAccount(conn, account_id, domain_id, "primary_storage", 0L);
                }
                // 3. update secondary_storage for all accounts
                long totalVolumesSize = 0;
                long totalSnapshotsSize = 0;
                long totalTemplatesSize = 0;
                pstmt4 =
                    conn.prepareStatement("SELECT sum(size) FROM `cloud`.`volumes` WHERE account_id= ?"
                        + " AND path is NULL AND state not in ('Allocated') AND removed is NULL");
                pstmt4.setLong(1, account_id);
                rsCount = pstmt4.executeQuery();
                if (rsCount.next()) {
                    totalVolumesSize = rsCount.getLong(1);
                }
                pstmt4 = conn.prepareStatement("SELECT sum(size) FROM `cloud`.`snapshots` WHERE account_id= ? AND removed is NULL");
                pstmt4.setLong(1, account_id);
                rsCount = pstmt4.executeQuery();
                if (rsCount.next()) {
                    totalSnapshotsSize = rsCount.getLong(1);
                }
                pstmt4 =
                    conn.prepareStatement("SELECT sum(template_store_ref.size) FROM `cloud`.`template_store_ref`,`cloud`.`vm_template` WHERE account_id = ?"
                        + " AND template_store_ref.template_id = vm_template.id AND download_state = 'DOWNLOADED' AND destroyed = false AND removed is NULL");
                pstmt4.setLong(1, account_id);
                rsCount = pstmt4.executeQuery();
                if (rsCount.next()) {
                    totalTemplatesSize = rsCount.getLong(1);
                }
                upgradeResourceCountforAccount(conn, account_id, domain_id, "secondary_storage", totalVolumesSize + totalSnapshotsSize + totalTemplatesSize);
            }
            // 4. upgrade cpu,memory,primary_storage,secondary_storage for domains
            String resource_types[] = {"cpu", "memory", "primary_storage", "secondary_storage"};
            pstmt5 = conn.prepareStatement("select id FROM `cloud`.`domain`");
            rsAccount = pstmt5.executeQuery();
            while (rsAccount.next()) {
                long domain_id = rsAccount.getLong(1);
                for (int count = 0; count < resource_types.length; count++) {
                    String resource_type = resource_types[count];
                    upgradeResourceCountforDomain(conn, domain_id, resource_type, 0L); // reset value to 0 before statistics
                }
            }
            for (int count = 0; count < resource_types.length; count++) {
                String resource_type = resource_types[count];
                pstmt5 =
                    conn.prepareStatement("select account.domain_id,sum(resource_count.count) from `cloud`.`account` left join `cloud`.`resource_count` on account.id=resource_count.account_id "
                        + "where resource_count.type=? group by account.domain_id;");
                pstmt5.setString(1, resource_type);
                rsCount = pstmt5.executeQuery();
                while (rsCount.next()) {
                    long domain_id = rsCount.getLong(1);
                    long resource_count = rsCount.getLong(2);
                    upgradeResourceCountforDomain(conn, domain_id, resource_type, resource_count);
                }
            }
            s_logger.debug("upgradeResourceCount finish");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to upgrade resource count (cpu,memory,primary_storage,secondary_storage) ", e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }
                if (rsAccount != null) {
                    rsAccount.close();
                }
                if (rsCount != null) {
                    rsCount.close();
                }
                if (pstmt1 != null) {
                    pstmt1.close();
                }
                if (pstmt2 != null) {
                    pstmt2.close();
                }
                if (pstmt3 != null) {
                    pstmt3.close();
                }
                if (pstmt4 != null) {
                    pstmt4.close();
                }
                if (pstmt5 != null) {
                    pstmt5.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private static void upgradeResourceCountforAccount(Connection conn, Long account_id, Long domain_id, String type, Long resource_count) throws SQLException {
        //update or insert into resource_count table.
        PreparedStatement pstmt = null;
        pstmt =
            conn.prepareStatement("INSERT INTO `cloud`.`resource_count` (account_id, type, count) VALUES (?,?,?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id), count=?");
        pstmt.setLong(1, account_id);
        pstmt.setString(2, type);
        pstmt.setLong(3, resource_count);
        pstmt.setLong(4, resource_count);
        pstmt.executeUpdate();
        pstmt.close();
    }

    private static void upgradeResourceCountforDomain(Connection conn, Long domain_id, String type, Long resource_count) throws SQLException {
        //update or insert into resource_count table.
        PreparedStatement pstmt = null;
        pstmt =
            conn.prepareStatement("INSERT INTO `cloud`.`resource_count` (domain_id, type, count) VALUES (?,?,?) ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id), count=?");
        pstmt.setLong(1, domain_id);
        pstmt.setString(2, type);
        pstmt.setLong(3, resource_count);
        pstmt.setLong(4, resource_count);
        pstmt.executeUpdate();
        pstmt.close();
    }
}
