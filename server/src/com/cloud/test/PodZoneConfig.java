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
package com.cloud.test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;

import com.cloud.network.Networks.TrafficType;
import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.net.NetUtils;

public class PodZoneConfig {

    public static void main(String[] args) {
        PodZoneConfig config = ComponentContext.inject(PodZoneConfig.class);
        //config.run(args);
        System.exit(0);
    }

    public void savePod(boolean printOutput, long id, String name, long dcId, String gateway, String cidr, int vlanStart, int vlanEnd) {
        // Check that the cidr was valid
        if (!IPRangeConfig.validCIDR(cidr))
            printError("Please enter a valid CIDR for pod: " + name);

        // Get the individual cidrAddress and cidrSize values
        String[] cidrPair = cidr.split("\\/");
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];

        String sql = null;
        if (id != -1)
            sql =
                "INSERT INTO `cloud`.`host_pod_ref` (id, name, data_center_id, gateway, cidr_address, cidr_size) " + "VALUES ('" + id + "','" + name + "','" + dcId +
                    "','" + gateway + "','" + cidrAddress + "','" + cidrSize + "')";
        else
            sql =
                "INSERT INTO `cloud`.`host_pod_ref` (name, data_center_id, gateway, cidr_address, cidr_size) " + "VALUES ('" + name + "','" + dcId + "','" + gateway +
                    "','" + cidrAddress + "','" + cidrSize + "')";

        DatabaseConfig.saveSQL(sql, "Failed to save pod due to exception. Please contact Cloud Support.");

        if (printOutput)
            System.out.println("Successfuly saved pod.");
    }

    public void checkAllPodCidrSubnets() {
        Vector<Long> allZoneIDs = getAllZoneIDs();
        for (Long dcId : allZoneIDs) {
            HashMap<Long, Vector<Object>> currentPodCidrSubnets = getCurrentPodCidrSubnets(dcId.longValue());
            String result = checkPodCidrSubnets(dcId.longValue(), currentPodCidrSubnets);
            if (!result.equals("success"))
                printError(result);
        }
    }

    private String checkPodCidrSubnets(long dcId, HashMap<Long, Vector<Object>> currentPodCidrSubnets) {

//        DataCenterDao _dcDao = null;
//        final ComponentLocator locator = ComponentLocator.getLocator("management-server");

//        _dcDao = locator.getDao(DataCenterDao.class);
        // For each pod, return an error if any of the following is true:
        // 1. The pod's CIDR subnet conflicts with the guest network subnet
        // 2. The pod's CIDR subnet conflicts with the CIDR subnet of any other pod

        String zoneName = PodZoneConfig.getZoneName(dcId);

        //get the guest network cidr and guest netmask from the zone
//        DataCenterVO dcVo = _dcDao.findById(dcId);

        String guestNetworkCidr = IPRangeConfig.getGuestNetworkCidr(dcId);

        if (guestNetworkCidr == null || guestNetworkCidr.isEmpty())
            return "Please specify a valid guest cidr";
        String[] cidrTuple = guestNetworkCidr.split("\\/");

        String guestIpNetwork = NetUtils.getIpRangeStartIpFromCidr(cidrTuple[0], Long.parseLong(cidrTuple[1]));
        long guestCidrSize = Long.parseLong(cidrTuple[1]);

        // Iterate through all pods in this zone
        for (Long podId : currentPodCidrSubnets.keySet()) {
            String podName;
            if (podId.longValue() == -1)
                podName = "newPod";
            else
                podName = PodZoneConfig.getPodName(podId.longValue(), dcId);

            Vector<Object> cidrPair = currentPodCidrSubnets.get(podId);
            String cidrAddress = (String)cidrPair.get(0);
            long cidrSize = ((Long)cidrPair.get(1)).longValue();

            long cidrSizeToUse = -1;
            if (cidrSize < guestCidrSize)
                cidrSizeToUse = cidrSize;
            else
                cidrSizeToUse = guestCidrSize;

            String cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
            String guestSubnet = NetUtils.getCidrSubNet(guestIpNetwork, cidrSizeToUse);

            // Check that cidrSubnet does not equal guestSubnet
            if (cidrSubnet.equals(guestSubnet)) {
                if (podName.equals("newPod")) {
                    return "The subnet of the pod you are adding conflicts with the subnet of the Guest IP Network. Please specify a different CIDR.";
                } else {
                    return "Warning: The subnet of pod " + podName + " in zone " + zoneName +
                        " conflicts with the subnet of the Guest IP Network. Please change either the pod's CIDR or the Guest IP Network's subnet, and re-run install-vmops-management.";
                }
            }

            // Iterate through the rest of the pods
            for (Long otherPodId : currentPodCidrSubnets.keySet()) {
                if (podId.equals(otherPodId))
                    continue;

                // Check that cidrSubnet does not equal otherCidrSubnet
                Vector<Object> otherCidrPair = currentPodCidrSubnets.get(otherPodId);
                String otherCidrAddress = (String)otherCidrPair.get(0);
                long otherCidrSize = ((Long)otherCidrPair.get(1)).longValue();

                if (cidrSize < otherCidrSize)
                    cidrSizeToUse = cidrSize;
                else
                    cidrSizeToUse = otherCidrSize;

                cidrSubnet = NetUtils.getCidrSubNet(cidrAddress, cidrSizeToUse);
                String otherCidrSubnet = NetUtils.getCidrSubNet(otherCidrAddress, cidrSizeToUse);

                if (cidrSubnet.equals(otherCidrSubnet)) {
                    String otherPodName = PodZoneConfig.getPodName(otherPodId.longValue(), dcId);
                    if (podName.equals("newPod")) {
                        return "The subnet of the pod you are adding conflicts with the subnet of pod " + otherPodName + " in zone " + zoneName +
                            ". Please specify a different CIDR.";
                    } else {
                        return "Warning: The pods " + podName + " and " + otherPodName + " in zone " + zoneName +
                            " have conflicting CIDR subnets. Please change the CIDR of one of these pods.";
                    }
                }
            }
        }

        return "success";
    }

    @DB
    protected HashMap<Long, Vector<Object>> getCurrentPodCidrSubnets(long dcId) {
        HashMap<Long, Vector<Object>> currentPodCidrSubnets = new HashMap<Long, Vector<Object>>();

        String selectSql = "SELECT id, cidr_address, cidr_size FROM host_pod_ref WHERE data_center_id=" + dcId;
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Long podId = rs.getLong("id");
                String cidrAddress = rs.getString("cidr_address");
                long cidrSize = rs.getLong("cidr_size");
                Vector<Object> cidrPair = new Vector<Object>();
                cidrPair.add(0, cidrAddress);
                cidrPair.add(1, new Long(cidrSize));
                currentPodCidrSubnets.put(podId, cidrPair);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            printError("There was an issue with reading currently saved pod CIDR subnets. Please contact Cloud Support.");
            return null;
        }

        return currentPodCidrSubnets;
    }

    public void deletePod(String name, long dcId) {
        String sql = "DELETE FROM `cloud`.`host_pod_ref` WHERE name=\"" + name + "\" AND data_center_id=\"" + dcId + "\"";
        DatabaseConfig.saveSQL(sql, "Failed to delete pod due to exception. Please contact Cloud Support.");
    }

    public long getVlanDbId(String zone, String vlanId) {
        long zoneId = getZoneId(zone);

        return DatabaseConfig.getDatabaseValueLong("SELECT * FROM `cloud`.`vlan` WHERE data_center_id=\"" + zoneId + "\" AND vlan_id =\"" + vlanId + "\"", "id",
            "Unable to start DB connection to read vlan DB id. Please contact Cloud Support.");
    }

    public List<String> modifyVlan(String zone, boolean add, String vlanId, String vlanGateway, String vlanNetmask, String pod, String vlanType, String ipRange,
        long networkId, long physicalNetworkId) {
        // Check if the zone is valid
        long zoneId = getZoneId(zone);
        if (zoneId == -1)
            return genReturnList("false", "Please specify a valid zone.");

        //check if physical network is valid
        long physicalNetworkDbId = checkPhysicalNetwork(physicalNetworkId);
        if (physicalNetworkId == -1)
            return genReturnList("false", "Please specify a valid physical network.");

        Long podId = pod != null ? getPodId(pod, zone) : null;
        if (podId != null && podId == -1)
            return genReturnList("false", "Please specify a valid pod.");

        if (add) {

            // Make sure the gateway is valid
            if (!NetUtils.isValidIp(vlanGateway))
                return genReturnList("false", "Please specify a valid gateway.");

            // Make sure the netmask is valid
            if (!NetUtils.isValidIp(vlanNetmask))
                return genReturnList("false", "Please specify a valid netmask.");

            // Check if a vlan with the same vlanId already exists in the specified zone
            if (getVlanDbId(zone, vlanId) != -1)
                return genReturnList("false", "A VLAN with the specified VLAN ID already exists in zone " + zone + ".");

            /*
            // Check if another vlan in the same zone has the same subnet
            String newVlanSubnet = NetUtils.getSubNet(vlanGateway, vlanNetmask);
            List<VlanVO> vlans = _vlanDao.findByZone(zoneId);
            for (VlanVO vlan : vlans) {
                String currentVlanSubnet = NetUtils.getSubNet(vlan.getVlanGateway(), vlan.getVlanNetmask());
                if (newVlanSubnet.equals(currentVlanSubnet))
                    return genReturnList("false", "The VLAN with ID " + vlan.getVlanId() + " in zone " + zone + " has the same subnet. Please specify a different gateway/netmask.");
            }
             */

            // Everything was fine, so persist the VLAN
            saveVlan(zoneId, podId, vlanId, vlanGateway, vlanNetmask, vlanType, ipRange, networkId, physicalNetworkDbId);
            if (podId != null) {
                long vlanDbId = getVlanDbId(zone, vlanId);
                String sql = "INSERT INTO `cloud`.`pod_vlan_map` (pod_id, vlan_db_id) " + "VALUES (?,?)";
                String errorMsg =  "Failed to save pod_vlan_map due to exception vlanDbId=" + vlanDbId + ", podId=" + podId + ". Please contact Cloud Support.";
                TransactionLegacy txn = TransactionLegacy.open("saveSQL");
                try ( PreparedStatement stmt = txn.prepareAutoCloseStatement(sql); ) {
                        stmt.setString(1, podId.toString());
                        stmt.setString(2, String.valueOf(vlanDbId));
                        stmt.executeUpdate();
                    } catch (SQLException ex) {
                        System.out.println("SQL Exception: " + ex.getMessage());
                        printError(errorMsg);
                    }
            }

            return genReturnList("true", "Successfully added VLAN.");

        } else {
            return genReturnList("false", "That operation is not suppored.");
        }

        /*
        else {

            // Check if a VLAN actually exists in the specified zone
            long vlanDbId = getVlanDbId(zone, vlanId);
            if (vlanDbId == -1)
                return genReturnList("false", "A VLAN with ID " + vlanId + " does not exist in zone " + zone);

            // Check if there are any public IPs that are in the specified vlan.
            List<IPAddressVO> ips = _publicIpAddressDao.listByVlanDbId(vlanDbId);
            if (ips.size() != 0)
                return genReturnList("false", "Please delete all IP addresses that are in VLAN " + vlanId + " before deleting the VLAN.");

            // Delete the vlan
            _vlanDao.delete(vlanDbId);

            return genReturnList("true", "Successfully deleted VLAN.");
        }
         */
    }

    @DB
    public void saveZone(boolean printOutput, long id, String name, String dns1, String dns2, String dns3, String dns4, String guestNetworkCidr, String networkType) {

        if (printOutput)
            System.out.println("Saving zone, please wait...");

        String columns = null;
        String values = null;

        if (id != -1) {
            columns = "(id, name";
            values = "('" + id + "','" + name + "'";
        } else {
            columns = "(name";
            values = "('" + name + "'";
        }

        if (dns1 != null) {
            columns += ", dns1";
            values += ",'" + dns1 + "'";
        }

        if (dns2 != null) {
            columns += ", dns2";
            values += ",'" + dns2 + "'";
        }

        if (dns3 != null) {
            columns += ", internal_dns1";
            values += ",'" + dns3 + "'";
        }

        if (dns4 != null) {
            columns += ", internal_dns2";
            values += ",'" + dns4 + "'";
        }

        if (guestNetworkCidr != null) {
            columns += ", guest_network_cidr";
            values += ",'" + guestNetworkCidr + "'";
        }

        if (networkType != null) {
            columns += ", networktype";
            values += ",'" + networkType + "'";
        }

        columns += ", uuid";
        values += ", UUID()";

        columns += ")";
        values += ")";

        String sql = "INSERT INTO `cloud`.`data_center` " + columns + " VALUES " + values;

        DatabaseConfig.saveSQL(sql, "Failed to save zone due to exception. Please contact Cloud Support.");

        if (printOutput)
            System.out.println("Successfully saved zone.");
    }

    @DB
    public void savePhysicalNetwork(boolean printOutput, long id, long dcId, int vnetStart, int vnetEnd) {

        if (printOutput)
            System.out.println("Saving physical network, please wait...");

        String columns = null;
        String values = null;

        columns = "(id ";
        values = "('" + id + "'";

        columns += ", name ";
        values += ",'physical network'";

        columns += ", data_center_id ";
        values += ",'" + dcId + "'";

        //save vnet information
        columns += ", vnet";
        values += ",'" + vnetStart + "-" + vnetEnd + "'";

        columns += ", state";
        values += ", 'Enabled'";

        columns += ", uuid";
        values += ", UUID()";

        columns += ")";
        values += ")";

        String sql = "INSERT INTO `cloud`.`physical_network` " + columns + " VALUES " + values;

        DatabaseConfig.saveSQL(sql, "Failed to save physical network due to exception. Please contact Cloud Support.");

        // Hardcode the vnet range to be the full range
        int begin = 0x64;
        int end = 64000;

        // If vnet arguments were passed in, use them
        if (vnetStart != -1 && vnetEnd != -1) {
            begin = vnetStart;
            end = vnetEnd;
        }

        String insertVnet = "INSERT INTO `cloud`.`op_dc_vnet_alloc` (vnet, data_center_id, physical_network_id) VALUES ( ?, ?, ?)";

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertVnet);
            for (int i = begin; i <= end; i++) {
                stmt.setString(1, Integer.toString(i));
                stmt.setLong(2, dcId);
                stmt.setLong(3, id);
                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException ex) {
            printError("Error creating vnet for the physical network. Please contact Cloud Support.");
        }

        //add default traffic types

        //get default Xen network labels
        String defaultXenPrivateNetworkLabel = getDefaultXenNetworkLabel(TrafficType.Management);
        String defaultXenPublicNetworkLabel = getDefaultXenNetworkLabel(TrafficType.Public);
        String defaultXenStorageNetworkLabel = getDefaultXenNetworkLabel(TrafficType.Storage);
        String defaultXenGuestNetworkLabel = getDefaultXenNetworkLabel(TrafficType.Guest);

        String insertTraficType = "INSERT INTO `cloud`.`physical_network_traffic_types` " + "(physical_network_id, traffic_type, xenserver_network_label) VALUES ( ?, ?, ?)";

        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(insertTraficType);
            for (TrafficType traffic : TrafficType.values()) {
                if (traffic.equals(TrafficType.Control) || traffic.equals(TrafficType.Vpn) || traffic.equals(TrafficType.None)) {
                    continue;
                }
                stmt.setLong(1, id);
                stmt.setString(2, traffic.toString());
                if (traffic.equals(TrafficType.Public)) {
                    stmt.setString(3, defaultXenPublicNetworkLabel);
                } else if (traffic.equals(TrafficType.Management)) {
                    stmt.setString(3, defaultXenPrivateNetworkLabel);
                } else if (traffic.equals(TrafficType.Storage)) {
                    stmt.setString(3, defaultXenStorageNetworkLabel);
                } else if (traffic.equals(TrafficType.Guest)) {
                    stmt.setString(3, defaultXenGuestNetworkLabel);
                }

                stmt.addBatch();
            }
            stmt.executeBatch();
        } catch (SQLException ex) {
            printError("Error adding default traffic types for the physical network. Please contact Cloud Support.");
        }

        if (printOutput)
            System.out.println("Successfully saved physical network.");
    }

    private String getDefaultXenNetworkLabel(TrafficType trafficType) {
        String xenLabel = null;
        String configName = null;
        switch (trafficType) {
            case Public:
                configName = "xenserver.public.network.device";
                break;
            case Guest:
                configName = "xenserver.guest.network.device";
                break;
            case Storage:
                configName = "xenserver.storage.network.device1";
                break;
            case Management:
                configName = "xenserver.private.network.device";
                break;
        }

        if (configName != null) {
            xenLabel = getConfiguredValue(configName);
        }
        return xenLabel;
    }

    public static String getConfiguredValue(String configName) {
        return DatabaseConfig.getDatabaseValueString("SELECT value FROM `cloud`.`configuration` where name = \"" + configName + "\"", "value",
            "Unable to start DB connection to read configuration. Please contact Cloud Support.");
    }

    public void deleteZone(String name) {
        String sql = "DELETE FROM `cloud`.`data_center` WHERE name=\"" + name + "\"";
        DatabaseConfig.saveSQL(sql, "Failed to delete zone due to exception. Please contact Cloud Support.");
    }

    public void saveVlan(long zoneId, Long podId, String vlanId, String vlanGateway, String vlanNetmask, String vlanType, String ipRange, long networkId,
        long physicalNetworkId) {
        String sql =
            "INSERT INTO `cloud`.`vlan` (vlan_id, vlan_gateway, vlan_netmask, data_center_id, vlan_type, description, network_id, physical_network_id) " + "VALUES ('" +
                vlanId + "','" + vlanGateway + "','" + vlanNetmask + "','" + zoneId + "','" + vlanType + "','" + ipRange + "','" + networkId + "','" + physicalNetworkId +
                "')";
        DatabaseConfig.saveSQL(sql, "Failed to save vlan due to exception. Please contact Cloud Support.");
    }

    public static long getPodId(String pod, String zone) {
        long dcId = getZoneId(zone);
        String selectSql = "SELECT * FROM `cloud`.`host_pod_ref` WHERE name = \"" + pod + "\" AND data_center_id = \"" + dcId + "\"";
        String errorMsg = "Could not read pod ID fro mdatabase. Please contact Cloud Support.";
        return DatabaseConfig.getDatabaseValueLong(selectSql, "id", errorMsg);
    }

    public static long getPodId(String pod, long dcId) {
        String selectSql = "SELECT * FROM `cloud`.`host_pod_ref` WHERE name = \"" + pod + "\" AND data_center_id = \"" + dcId + "\"";
        String errorMsg = "Could not read pod ID fro mdatabase. Please contact Cloud Support.";
        return DatabaseConfig.getDatabaseValueLong(selectSql, "id", errorMsg);
    }

    public static long getZoneId(String zone) {
        String selectSql = "SELECT * FROM `cloud`.`data_center` WHERE name = \"" + zone + "\"";
        String errorMsg = "Could not read zone ID from database. Please contact Cloud Support.";
        return DatabaseConfig.getDatabaseValueLong(selectSql, "id", errorMsg);
    }

    public static long checkPhysicalNetwork(long physicalNetworkId) {
        String selectSql = "SELECT * FROM `cloud`.`physical_network` WHERE id = \"" + physicalNetworkId + "\"";
        String errorMsg = "Could not read physicalNetwork ID from database. Please contact Cloud Support.";
        return DatabaseConfig.getDatabaseValueLong(selectSql, "id", errorMsg);
    }

    @DB
    public Vector<Long> getAllZoneIDs() {
        Vector<Long> allZoneIDs = new Vector<Long>();

        String selectSql = "SELECT id FROM data_center";
        TransactionLegacy txn = TransactionLegacy.currentTxn();
        try {
            PreparedStatement stmt = txn.prepareAutoCloseStatement(selectSql);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Long dcId = rs.getLong("id");
                allZoneIDs.add(dcId);
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            printError("There was an issue with reading zone IDs. Please contact Cloud Support.");
            return null;
        }

        return allZoneIDs;
    }

    public static boolean validPod(String pod, String zone) {
        return (getPodId(pod, zone) != -1);
    }

    public static boolean validZone(String zone) {
        return (getZoneId(zone) != -1);
    }

    public static String getPodName(long podId, long dcId) {
        return DatabaseConfig.getDatabaseValueString("SELECT * FROM `cloud`.`host_pod_ref` WHERE id=" + podId + " AND data_center_id=" + dcId, "name",
            "Unable to start DB connection to read pod name. Please contact Cloud Support.");
    }

    public static String getZoneName(long dcId) {
        return DatabaseConfig.getDatabaseValueString("SELECT * FROM `cloud`.`data_center` WHERE id=" + dcId, "name",
            "Unable to start DB connection to read zone name. Please contact Cloud Support.");
    }

    private static void printError(String message) {
        DatabaseConfig.printError(message);
    }

    private List<String> genReturnList(String success, String message) {
        List<String> returnList = new ArrayList<String>();
        returnList.add(0, success);
        returnList.add(1, message);
        return returnList;
    }

}
