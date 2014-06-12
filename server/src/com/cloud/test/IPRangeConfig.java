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

import java.lang.reflect.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.UUID;
import java.util.Vector;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.db.DB;
import com.cloud.utils.db.TransactionLegacy;
import com.cloud.utils.net.NetUtils;

public class IPRangeConfig {

    public static void main(String[] args) {
        IPRangeConfig config = ComponentContext.inject(IPRangeConfig.class);
        config.run(args);
        System.exit(0);
    }

    private String usage() {
        return "Usage: ./change_ip_range.sh [add|delete] [public zone | private pod zone] startIP endIP";
    }

    public void run(String[] args) {
        if (args.length < 2) {
            printError(usage());
        }

        String op = args[0];
        String type = args[1];

        if (type.equals("public")) {
            if (args.length != 4 && args.length != 5) {
                printError(usage());
            }
            String zone = args[2];
            String startIP = args[3];
            String endIP = null;
            if (args.length == 5) {
                endIP = args[4];
            }

            String result = checkErrors(type, op, null, zone, startIP, endIP);
            if (!result.equals("success")) {
                printError(result);
            }

            long zoneId = PodZoneConfig.getZoneId(zone);
            result = changeRange(op, "public", -1, zoneId, startIP, endIP, null, -1);
            result = result.replaceAll("<br>", "/n");
            System.out.println(result);
        } else if (type.equals("private")) {
            if (args.length != 5 && args.length != 6) {
                printError(usage());
            }
            String pod = args[2];
            String zone = args[3];
            ;
            String startIP = args[4];
            String endIP = null;
            if (args.length == 6) {
                endIP = args[5];
            }

            String result = checkErrors(type, op, pod, zone, startIP, endIP);
            if (!result.equals("success")) {
                printError(result);
            }

            long podId = PodZoneConfig.getPodId(pod, zone);
            long zoneId = PodZoneConfig.getZoneId(zone);
            result = changeRange(op, "private", podId, zoneId, startIP, endIP, null, -1);
            result = result.replaceAll("<br>", "/n");
            System.out.println(result);
        } else {
            printError(usage());
        }
    }

    public List<String> changePublicIPRangeGUI(String op, String zone, String startIP, String endIP, long physicalNetworkId) {
        String result = checkErrors("public", op, null, zone, startIP, endIP);
        if (!result.equals("success")) {
            return DatabaseConfig.genReturnList("false", result);
        }

        long zoneId = PodZoneConfig.getZoneId(zone);
        result = changeRange(op, "public", -1, zoneId, startIP, endIP, null, physicalNetworkId);

        return DatabaseConfig.genReturnList("true", result);
    }

    public List<String> changePrivateIPRangeGUI(String op, String pod, String zone, String startIP, String endIP) {
        String result = checkErrors("private", op, pod, zone, startIP, endIP);
        if (!result.equals("success")) {
            return DatabaseConfig.genReturnList("false", result);
        }

        long podId = PodZoneConfig.getPodId(pod, zone);
        long zoneId = PodZoneConfig.getZoneId(zone);
        result = changeRange(op, "private", podId, zoneId, startIP, endIP, null, -1);

        return DatabaseConfig.genReturnList("true", result);
    }

    private String checkErrors(String type, String op, String pod, String zone, String startIP, String endIP) {
        if (!op.equals("add") && !op.equals("delete")) {
            return usage();
        }

        if (type.equals("public")) {
            // Check that the zone is valid
            if (!PodZoneConfig.validZone(zone)) {
                return "Please specify a valid zone.";
            }
        } else if (type.equals("private")) {
            // Check that the pod and zone are valid
            if (!PodZoneConfig.validZone(zone)) {
                return "Please specify a valid zone.";
            }
            if (!PodZoneConfig.validPod(pod, zone)) {
                return "Please specify a valid pod.";
            }
        }

        if (!validIP(startIP)) {
            return "Please specify a valid start IP";
        }

        if (!validOrBlankIP(endIP)) {
            return "Please specify a valid end IP";
        }

        // Check that the IPs that are being added are compatible with either the zone's public netmask, or the pod's CIDR
        if (type.equals("public")) {
            // String publicNetmask = getPublicNetmask(zone);
            // String publicGateway = getPublicGateway(zone);

            // if (publicNetmask == null) return "Please ensure that your zone's public net mask is specified";
            // if (!sameSubnet(startIP, endIP, publicNetmask)) return "Please ensure that your start IP and end IP are in the same subnet, as per the zone's netmask.";
            // if (!sameSubnet(startIP, publicGateway, publicNetmask)) return "Please ensure that your start IP is in the same subnet as your zone's gateway, as per the zone's netmask.";
            // if (!sameSubnet(endIP, publicGateway, publicNetmask)) return "Please ensure that your end IP is in the same subnet as your zone's gateway, as per the zone's netmask.";
        } else if (type.equals("private")) {
            String cidrAddress = getCidrAddress(pod, zone);
            long cidrSize = getCidrSize(pod, zone);

            if (!sameSubnetCIDR(startIP, endIP, cidrSize)) {
                return "Please ensure that your start IP and end IP are in the same subnet, as per the pod's CIDR size.";
            }
            if (!sameSubnetCIDR(startIP, cidrAddress, cidrSize)) {
                return "Please ensure that your start IP is in the same subnet as the pod's CIDR address.";
            }
            if (!sameSubnetCIDR(endIP, cidrAddress, cidrSize)) {
                return "Please ensure that your end IP is in the same subnet as the pod's CIDR address.";
            }
        }

        if (!validIPRange(startIP, endIP)) {
            return "Please specify a valid IP range.";
        }

        return "success";
    }

    private String genChangeRangeSuccessString(List<String> problemIPs, String op) {
        if (problemIPs == null) {
            return "";
        }

        if (problemIPs.size() == 0) {
            if (op.equals("add")) {
                return "Successfully added all IPs in the specified range.";
            } else if (op.equals("delete")) {
                return "Successfully deleted all IPs in the specified range.";
            } else {
                return "";
            }
        } else {
            String successString = "";
            if (op.equals("add")) {
                successString += "Failed to add the following IPs, because they are already in the database: <br><br>";
            } else if (op.equals("delete")) {
                successString += "Failed to delete the following IPs, because they are in use: <br><br>";
            }

            for (int i = 0; i < problemIPs.size(); i++) {
                successString += problemIPs.get(i);
                if (i != (problemIPs.size() - 1)) {
                    successString += ", ";
                }
            }

            successString += "<br><br>";

            if (op.equals("add")) {
                successString += "Successfully added all other IPs in the specified range.";
            } else if (op.equals("delete")) {
                successString += "Successfully deleted all other IPs in the specified range.";
            }

            return successString;
        }
    }

    private String changeRange(String op, String type, long podId, long zoneId, String startIP, String endIP, Long networkId, long physicalNetworkId) {

        // Go through all the IPs and add or delete them
        List<String> problemIPs = null;
        if (op.equals("add")) {
            problemIPs = saveIPRange(type, podId, zoneId, 1, startIP, endIP, networkId, physicalNetworkId);
        } else if (op.equals("delete")) {
            problemIPs = deleteIPRange(type, podId, zoneId, 1, startIP, endIP);
        }

        if (problemIPs == null) {
            return null;
        } else {
            return genChangeRangeSuccessString(problemIPs, op);
        }
    }

    private String genSuccessString(Vector<String> problemIPs, String op) {
        if (problemIPs == null) {
            return "";
        }

        if (problemIPs.size() == 0) {
            if (op.equals("add")) {
                return "Successfully added all IPs in the specified range.";
            } else if (op.equals("delete")) {
                return "Successfully deleted all IPs in the specified range.";
            } else {
                return "";
            }
        } else {
            String successString = "";
            if (op.equals("add")) {
                successString += "Failed to add the following IPs, because they are already in the database: <br><br>";
            } else if (op.equals("delete")) {
                successString += "Failed to delete the following IPs, because they are in use: <br><br>";
            }

            for (int i = 0; i < problemIPs.size(); i++) {
                successString += problemIPs.elementAt(i);
                if (i != (problemIPs.size() - 1)) {
                    successString += ", ";
                }
            }

            successString += "<br><br>";

            if (op.equals("add")) {
                successString += "Successfully added all other IPs in the specified range.";
            } else if (op.equals("delete")) {
                successString += "Successfully deleted all other IPs in the specified range.";
            }

            return successString;
        }
    }

    public static String getCidrAddress(String pod, String zone) {
        long dcId = PodZoneConfig.getZoneId(zone);
        String selectSql = "SELECT * FROM `cloud`.`host_pod_ref` WHERE name = \"" + pod + "\" AND data_center_id = \"" + dcId + "\"";
        String errorMsg = "Could not read CIDR address for pod/zone: " + pod + "/" + zone + " from database. Please contact Cloud Support.";
        return DatabaseConfig.getDatabaseValueString(selectSql, "cidr_address", errorMsg);
    }

    public static long getCidrSize(String pod, String zone) {
        long dcId = PodZoneConfig.getZoneId(zone);
        String selectSql = "SELECT * FROM `cloud`.`host_pod_ref` WHERE name = \"" + pod + "\" AND data_center_id = \"" + dcId + "\"";
        String errorMsg = "Could not read CIDR address for pod/zone: " + pod + "/" + zone + " from database. Please contact Cloud Support.";
        return DatabaseConfig.getDatabaseValueLong(selectSql, "cidr_size", errorMsg);
    }

    @DB
    protected Vector<String> deleteIPRange(String type, long podId, long zoneId, long vlanDbId, String startIP, String endIP) {
        long startIPLong = NetUtils.ip2Long(startIP);
        long endIPLong = startIPLong;
        if (endIP != null) {
            endIPLong = NetUtils.ip2Long(endIP);
        }

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        Vector<String> problemIPs = null;
        if (type.equals("public")) {
            problemIPs = deletePublicIPRange(txn, startIPLong, endIPLong, vlanDbId);
        } else if (type.equals("private")) {
            problemIPs = deletePrivateIPRange(txn, startIPLong, endIPLong, podId, zoneId);
        }

        return problemIPs;
    }

    private Vector<String> deletePublicIPRange(TransactionLegacy txn, long startIP, long endIP, long vlanDbId) {
        String deleteSql = "DELETE FROM `cloud`.`user_ip_address` WHERE public_ip_address = ? AND vlan_id = ?";
        String isPublicIPAllocatedSelectSql = "SELECT * FROM `cloud`.`user_ip_address` WHERE public_ip_address = ? AND vlan_id = ?";

        Vector<String> problemIPs = new Vector<String>();
        Connection conn = null;
        try {
            conn = txn.getConnection();
        }
        catch (SQLException e) {
            System.out.println("deletePublicIPRange. Exception: " +e.getMessage());
            return null;
        }
        try(PreparedStatement stmt = conn.prepareStatement(deleteSql);
            PreparedStatement isAllocatedStmt = conn.prepareStatement(isPublicIPAllocatedSelectSql);) {
            while (startIP <= endIP) {
                if (!isPublicIPAllocated(startIP, vlanDbId, isAllocatedStmt)) {
                        stmt.clearParameters();
                        stmt.setLong(1, startIP);
                        stmt.setLong(2, vlanDbId);
                        stmt.executeUpdate();
                    }
                else {
                    problemIPs.add(NetUtils.long2Ip(startIP));
                }
                startIP += 1;
            }
        }catch (Exception ex) {
           System.out.println("deletePublicIPRange. Exception: " +ex.getMessage());
           return null;
        }

        return problemIPs;
    }

    private Vector<String> deletePrivateIPRange(TransactionLegacy txn, long startIP, long endIP, long podId, long zoneId) {
        String deleteSql = "DELETE FROM `cloud`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND pod_id = ? AND data_center_id = ?";
        String isPrivateIPAllocatedSelectSql = "SELECT * FROM `cloud`.`op_dc_ip_address_alloc` WHERE ip_address = ? AND data_center_id = ? AND pod_id = ?";
        Vector<String> problemIPs = new Vector<String>();
        try {
            Connection conn = txn.getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(deleteSql);
                 PreparedStatement isAllocatedStmt = conn.prepareStatement(isPrivateIPAllocatedSelectSql);) {
                while (startIP <= endIP) {
                    if (!isPrivateIPAllocated(NetUtils.long2Ip(startIP), podId, zoneId, isAllocatedStmt)) {
                        stmt.clearParameters();
                        stmt.setString(1, NetUtils.long2Ip(startIP));
                        stmt.setLong(2, podId);
                        stmt.setLong(3, zoneId);
                        stmt.executeUpdate();
                    } else {
                        problemIPs.add(NetUtils.long2Ip(startIP));
                    }
                    startIP += 1;
                }
            } catch (SQLException e) {
                System.out.println("deletePrivateIPRange. Exception: " + e.getMessage());
                printError("deletePrivateIPRange. Exception: " + e.getMessage());
            }
        }catch (SQLException e) {
            System.out.println("deletePrivateIPRange. Exception: " + e.getMessage());
            printError("deletePrivateIPRange. Exception: " + e.getMessage());
        }
        return problemIPs;
    }

    private boolean isPublicIPAllocated(long ip, long vlanDbId, PreparedStatement stmt) {
        try(ResultSet rs = stmt.executeQuery();) {
            stmt.clearParameters();
            stmt.setLong(1, ip);
            stmt.setLong(2, vlanDbId);
            if (rs.next()) {
                return (rs.getString("allocated") != null);
            } else {
                return false;
            }
        }
        catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return true;
        }
    }

    private boolean isPrivateIPAllocated(String ip, long podId, long zoneId, PreparedStatement stmt) {
        try {
            stmt.clearParameters();
            stmt.setString(1, ip);
            stmt.setLong(2, zoneId);
            stmt.setLong(3, podId);
            try(ResultSet rs = stmt.executeQuery();) {
                if (rs.next()) {
                    return (rs.getString("taken") != null);
                } else {
                    return false;
                }
            }
            catch (Exception ex) {
                System.out.println(ex.getMessage());
                return true;
            }
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
            return true;
        }
    }

    @DB
    public List<String> saveIPRange(String type, long podId, long zoneId, long vlanDbId, String startIP, String endIP, Long sourceNetworkId, long physicalNetworkId) {
        long startIPLong = NetUtils.ip2Long(startIP);
        long endIPLong = startIPLong;
        if (endIP != null) {
            endIPLong = NetUtils.ip2Long(endIP);
        }

        TransactionLegacy txn = TransactionLegacy.currentTxn();
        List<String> problemIPs = null;

        if (type.equals("public")) {
            problemIPs = savePublicIPRange(txn, startIPLong, endIPLong, zoneId, vlanDbId, sourceNetworkId, physicalNetworkId);
        } else if (type.equals("private")) {
            problemIPs = savePrivateIPRange(txn, startIPLong, endIPLong, podId, zoneId);
        }

        String[] linkLocalIps = NetUtils.getLinkLocalIPRange(10);
        long startLinkLocalIp = NetUtils.ip2Long(linkLocalIps[0]);
        long endLinkLocalIp = NetUtils.ip2Long(linkLocalIps[1]);

        saveLinkLocalPrivateIPRange(txn, startLinkLocalIp, endLinkLocalIp, podId, zoneId);

        return problemIPs;
    }

    public Vector<String> savePublicIPRange(TransactionLegacy txn, long startIP, long endIP, long zoneId, long vlanDbId, Long sourceNetworkId, long physicalNetworkId) {
        String insertSql =
            "INSERT INTO `cloud`.`user_ip_address` (public_ip_address, data_center_id, vlan_db_id, mac_address, source_network_id, physical_network_id, uuid) VALUES (?, ?, ?, (select mac_address from `cloud`.`data_center` where id=?), ?, ?, ?)";
        String updateSql = "UPDATE `cloud`.`data_center` set mac_address = mac_address+1 where id=?";
        Vector<String> problemIPs = new Vector<String>();

        Connection conn = null;
        try {
            conn = txn.getConnection();
        } catch (SQLException e) {
            return null;
        }
        while (startIP <= endIP) {
            try (PreparedStatement insert_stmt = conn.prepareStatement(insertSql);
            PreparedStatement update_stmt = conn.prepareStatement(updateSql);
            ){
                insert_stmt.setString(1, NetUtils.long2Ip(startIP));
                insert_stmt.setLong(2, zoneId);
                insert_stmt.setLong(3, vlanDbId);
                insert_stmt.setLong(4, zoneId);
                insert_stmt.setLong(5, sourceNetworkId);
                insert_stmt.setLong(6, physicalNetworkId);
                insert_stmt.setString(7, UUID.randomUUID().toString());
                insert_stmt.executeUpdate();
                update_stmt.setLong(1, zoneId);
                update_stmt.executeUpdate();
            } catch (Exception ex) {
                problemIPs.add(NetUtils.long2Ip(startIP));
            }
            startIP++;
        }

        return problemIPs;
    }

    public List<String> savePrivateIPRange(TransactionLegacy txn, long startIP, long endIP, long podId, long zoneId) {
        String insertSql =
            "INSERT INTO `cloud`.`op_dc_ip_address_alloc` (ip_address, data_center_id, pod_id, mac_address) VALUES (?, ?, ?, (select mac_address from `cloud`.`data_center` where id=?))";
        String updateSql = "UPDATE `cloud`.`data_center` set mac_address = mac_address+1 where id=?";
        Vector<String> problemIPs = new Vector<String>();

        try {
            Connection conn = txn.getConnection();
            while (startIP <= endIP) {
                try (PreparedStatement insert_stmt = conn.prepareStatement(insertSql);
                     PreparedStatement update_stmt = conn.prepareStatement(updateSql);
                )
                {
                    insert_stmt.setString(1, NetUtils.long2Ip(startIP));
                    insert_stmt.setLong(2, zoneId);
                    insert_stmt.setLong(3, podId);
                    insert_stmt.setLong(4, zoneId);
                    insert_stmt.executeUpdate();
                    update_stmt.setLong(1, zoneId);
                    update_stmt.executeUpdate();
                } catch (Exception e) {
                    problemIPs.add(NetUtils.long2Ip(startIP));
                }
                startIP++;
            }
        } catch (Exception ex) {
            System.out.print(ex.getMessage());
            ex.printStackTrace();
        }

        return problemIPs;
    }

    private Vector<String> saveLinkLocalPrivateIPRange(TransactionLegacy txn, long startIP, long endIP, long podId, long zoneId) {
        String insertSql = "INSERT INTO `cloud`.`op_dc_link_local_ip_address_alloc` (ip_address, data_center_id, pod_id) VALUES (?, ?, ?)";
        Vector<String> problemIPs = new Vector<String>();

        Connection conn = null;
        try {
            conn = txn.getConnection();
        } catch (SQLException e) {
            System.out.println("Exception: " + e.getMessage());
            printError("Unable to start DB connection to save private IPs. Please contact Cloud Support.");
        }
        long start = startIP;

        try(PreparedStatement stmt = conn.prepareStatement(insertSql);)
        {
            while (startIP <= endIP) {
                stmt.setString(1, NetUtils.long2Ip(startIP++));
                stmt.setLong(2, zoneId);
                stmt.setLong(3, podId);
                stmt.addBatch();
            }
            int[] results = stmt.executeBatch();
            for (int i = 0; i < results.length; i += 2) {
                if (results[i] == Statement.EXECUTE_FAILED) {
                    problemIPs.add(NetUtils.long2Ip(start + (i / 2)));
                }
            }
        } catch (Exception ex) {
            System.out.println("saveLinkLocalPrivateIPRange. Exception: " + ex.getMessage());
        }
        return problemIPs;
    }

    public static String getPublicNetmask(String zone) {
        return DatabaseConfig.getDatabaseValueString("SELECT * FROM `cloud`.`data_center` WHERE name = \"" + zone + "\"", "netmask",
            "Unable to start DB connection to read public netmask. Please contact Cloud Support.");
    }

    public static String getPublicGateway(String zone) {
        return DatabaseConfig.getDatabaseValueString("SELECT * FROM `cloud`.`data_center` WHERE name = \"" + zone + "\"", "gateway",
            "Unable to start DB connection to read public gateway. Please contact Cloud Support.");
    }

    public static String getGuestNetworkCidr(Long zoneId) {
        return DatabaseConfig.getDatabaseValueString("SELECT * FROM `cloud`.`data_center` WHERE id = \"" + zoneId + "\"", "guest_network_cidr",
            "Unable to start DB connection to read guest cidr network. Please contact Cloud Support.");
    }

    public static boolean validCIDR(final String cidr) {
        if (cidr == null || cidr.isEmpty()) {
            return false;
        }
        String[] cidrPair = cidr.split("\\/");
        if (cidrPair.length != 2) {
            return false;
        }
        String cidrAddress = cidrPair[0];
        String cidrSize = cidrPair[1];
        if (!validIP(cidrAddress)) {
            return false;
        }
        int cidrSizeNum = -1;

        try {
            cidrSizeNum = Integer.parseInt(cidrSize);
        } catch (Exception e) {
            return false;
        }

        if (cidrSizeNum < 1 || cidrSizeNum > 32) {
            return false;
        }

        return true;
    }

    public static boolean validOrBlankIP(final String ip) {
        if (ip == null || ip.isEmpty()) {
            return true;
        }
        return validIP(ip);
    }

    public static boolean validIP(final String ip) {
        final String[] ipAsList = ip.split("\\.");

        // The IP address must have four octets
        if (Array.getLength(ipAsList) != 4) {
            return false;
        }

        for (int i = 0; i < 4; i++) {
            // Each octet must be an integer
            final String octetString = ipAsList[i];
            int octet;
            try {
                octet = Integer.parseInt(octetString);
            } catch (final Exception e) {
                return false;
            }
            // Each octet must be between 0 and 255, inclusive
            if (octet < 0 || octet > 255) {
                return false;
            }

            // Each octetString must have between 1 and 3 characters
            if (octetString.length() < 1 || octetString.length() > 3) {
                return false;
            }

        }

        // IP is good, return true
        return true;
    }

    public static boolean validIPRange(String startIP, String endIP) {
        if (endIP == null || endIP.isEmpty()) {
            return true;
        }

        long startIPLong = NetUtils.ip2Long(startIP);
        long endIPLong = NetUtils.ip2Long(endIP);
        return (startIPLong < endIPLong);
    }

    public static boolean sameSubnet(final String ip1, final String ip2, final String netmask) {
        if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) {
            return true;
        }
        String subnet1 = NetUtils.getSubNet(ip1, netmask);
        String subnet2 = NetUtils.getSubNet(ip2, netmask);

        return (subnet1.equals(subnet2));
    }

    public static boolean sameSubnetCIDR(final String ip1, final String ip2, final long cidrSize) {
        if (ip1 == null || ip1.isEmpty() || ip2 == null || ip2.isEmpty()) {
            return true;
        }
        String subnet1 = NetUtils.getCidrSubNet(ip1, cidrSize);
        String subnet2 = NetUtils.getCidrSubNet(ip2, cidrSize);

        return (subnet1.equals(subnet2));
    }

    private static void printError(String message) {
        DatabaseConfig.printError(message);
    }

}
