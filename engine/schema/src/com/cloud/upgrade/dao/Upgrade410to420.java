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

import com.cloud.deploy.DeploymentPlanner;
import com.cloud.hypervisor.Hypervisor.HypervisorType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;
import org.apache.log4j.Logger;
import java.io.File;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.cloud.network.vpc.NetworkACL;

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
    }

    private void addIndexForAlert(Connection conn) {

        //First drop if it exists. (Due to patches shipped to customers some will have the index and some wont.)
        List<String> indexList = new ArrayList<String>();
        s_logger.debug("Dropping index i_alert__last_sent if it exists");
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

        PreparedStatement pstmt = null;
        ResultSet rs = null;
        boolean xenserver = false;
        boolean kvm = false;
        boolean VMware = false;
        boolean Hyperv = false;
        boolean LXC = false;
        s_logger.debug("Updating System Vm template IDs");
        try{
            //Get all hypervisors in use
            try {
                pstmt = conn.prepareStatement("select distinct(hypervisor_type) from `cloud`.`cluster` where removed is null");
                rs = pstmt.executeQuery();
                while(rs.next()){
                    if("XenServer".equals(rs.getString(1))){
                        xenserver = true;
                    } else if("KVM".equals(rs.getString(1))){
                        kvm = true;
                    } else if("VMware".equals(rs.getString(1))){
                        VMware = true;
                    } else if("Hyperv".equals(rs.getString(1))) {
                        Hyperv = true;
                    } else if("LXC".equals(rs.getString(1))) {
                        LXC = true;
                    }
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while listing hypervisors in use", e);
            }

            s_logger.debug("Updating XenSever System Vms");
            //XenServer
            try {
                //Get 4.2.0 xenserer system Vm template Id
                pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name like 'systemvm-xenserver-4.2' and removed is null order by id desc limit 1");
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
                    pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = 'XenServer'");
                    pstmt.setLong(1, templateId);
                    pstmt.executeUpdate();
                    pstmt.close();
                } else {
                    if (xenserver){
                        throw new CloudRuntimeException("4.2.0 XenServer SystemVm template not found. Cannot upgrade system Vms");
                    } else {
                        s_logger.warn("4.2.0 XenServer SystemVm template not found. XenServer hypervisor is not used, so not failing upgrade");
                    }
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while updating XenServer systemVm template", e);
            }

            //KVM
            s_logger.debug("Updating KVM System Vms");
            try {
                //Get 4.2.0 KVM system Vm template Id
                pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = 'systemvm-kvm-4.2' and removed is null order by id desc limit 1");
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
                    pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = 'KVM'");
                    pstmt.setLong(1, templateId);
                    pstmt.executeUpdate();
                    pstmt.close();
                } else {
                    if (kvm){
                        throw new CloudRuntimeException("4.2.0 KVM SystemVm template not found. Cannot upgrade system Vms");
                    } else {
                        s_logger.warn("4.2.0 KVM SystemVm template not found. KVM hypervisor is not used, so not failing upgrade");
                    }
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while updating KVM systemVm template", e);
            }

            //VMware
            s_logger.debug("Updating VMware System Vms");
            try {
                //Get 4.2.0 VMware system Vm template Id
                pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = 'systemvm-vmware-4.2' and removed is null order by id desc limit 1");
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
                    pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = 'VMware'");
                    pstmt.setLong(1, templateId);
                    pstmt.executeUpdate();
                    pstmt.close();
                } else {
                    if (VMware){
                        throw new CloudRuntimeException("4.2.0 VMware SystemVm template not found. Cannot upgrade system Vms");
                    } else {
                        s_logger.warn("4.2.0 VMware SystemVm template not found. VMware hypervisor is not used, so not failing upgrade");
                    }
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while updating VMware systemVm template", e);
            }

            //Hyperv
            s_logger.debug("Updating Hyperv System Vms");
            try {
                //Get 4.2.0 Hyperv system Vm template Id
                pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = 'systemvm-hyperv-4.2' and removed is null order by id desc limit 1");
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
                    pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = 'Hyperv'");
                    pstmt.setLong(1, templateId);
                    pstmt.executeUpdate();
                    pstmt.close();
                } else {
                    if (Hyperv){
                        throw new CloudRuntimeException("4.2.0 HyperV SystemVm template not found. Cannot upgrade system Vms");
                    } else {
                        s_logger.warn("4.2.0 Hyperv SystemVm template not found. Hyperv hypervisor is not used, so not failing upgrade");
                    }
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while updating Hyperv systemVm template", e);
            }

            //LXC
            s_logger.debug("Updating LXC System Vms");
            try {
                //Get 4.2.0 LXC system Vm template Id
                pstmt = conn.prepareStatement("select id from `cloud`.`vm_template` where name = 'systemvm-lxc-4.2' and removed is null order by id desc limit 1");
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
                    pstmt = conn.prepareStatement("update `cloud`.`vm_instance` set vm_template_id = ? where type <> 'User' and hypervisor_type = 'LXC'");
                    pstmt.setLong(1, templateId);
                    pstmt.executeUpdate();
                    pstmt.close();
                } else {
                    if (LXC){
                        throw new CloudRuntimeException("4.2.0 LXC SystemVm template not found. Cannot upgrade system Vms");
                    } else {
                        s_logger.warn("4.2.0 LXC SystemVm template not found. LXC hypervisor is not used, so not failing upgrade");
                    }
                }
            } catch (SQLException e) {
                throw new CloudRuntimeException("Error while updating LXC systemVm template", e);
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
	}

	private void updatePrimaryStore(Connection conn) {
	    PreparedStatement sql = null;
	    PreparedStatement sql2 = null;
        try {
            sql = conn.prepareStatement("update storage_pool set storage_provider_name = ? , scope = ? where pool_type = 'Filesystem' or pool_type = 'LVM'");
            sql.setString(1, "ancient primary data store provider");
            sql.setString(2, "HOST");
            sql.executeUpdate();

            sql2 = conn.prepareStatement("update storage_pool set storage_provider_name = ? , scope = ? where pool_type != 'Filesystem' and pool_type != 'LVM'");
            sql2.setString(1, "ancient primary data store provider");
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
            throw new CloudRuntimeException("Unable existing physical networks with internal lb provider", e);
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
}
