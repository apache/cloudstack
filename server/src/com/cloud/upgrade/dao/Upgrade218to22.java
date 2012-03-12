/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package com.cloud.upgrade.dao;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.consoleproxy.ConsoleProxyManager;
import com.cloud.event.EventTypes;
import com.cloud.event.EventVO;
import com.cloud.event.UsageEventVO;
import com.cloud.network.router.VirtualNetworkApplianceManager;
import com.cloud.storage.secondary.SecondaryStorageVmManager;
import com.cloud.utils.DateUtil;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.net.NetUtils;
import com.cloud.utils.script.Script;

public class Upgrade218to22 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade218to22.class);
    boolean _basicZone;

    @Override
    public File[] getPrepareScripts() {
        String file = Script.findScript("", "db/schema-21to22.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22.sql");
        }

        return new File[] { new File(file) };
    }

    protected void upgradeStoragePools(Connection conn) {
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement("UPDATE storage_pool SET status='Up'");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't upgrade storage pool ", e);
        }
    }

    protected long insertNetworkOffering(Connection conn, String name, String displayText, String trafficType, boolean systemOnly, boolean defaultNetworkOffering, String availability,
            boolean dns_service, boolean gateway_service, boolean firewall_service, boolean lb_service, boolean userdata_service, boolean vpn_service, boolean dhcp_service) {
        String insertSql = "INSERT INTO network_offerings (name, display_text, nw_rate, mc_rate, concurrent_connections, traffic_type, tags, system_only, specify_vlan, service_offering_id, created, removed, `default`, availability, dns_service, gateway_service, firewall_service, lb_service, userdata_service, vpn_service, dhcp_service) "
                + "VALUES (?,    ?,            NULL,    NULL,    NULL,                   ?,            NULL, ?,           0,            NULL,                now(),   NULL,    ?,       ?,            ?,           ?,               ?,                ?,          ?,                ?,           ?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            pstmt.setString(i++, name);
            pstmt.setString(i++, displayText);
            pstmt.setString(i++, trafficType);
            pstmt.setBoolean(i++, systemOnly);
            pstmt.setBoolean(i++, defaultNetworkOffering);
            pstmt.setString(i++, availability);
            pstmt.setBoolean(i++, dns_service);
            pstmt.setBoolean(i++, gateway_service);
            pstmt.setBoolean(i++, firewall_service);
            pstmt.setBoolean(i++, lb_service);
            pstmt.setBoolean(i++, userdata_service);
            pstmt.setBoolean(i++, vpn_service);
            pstmt.setBoolean(i++, dhcp_service);
            pstmt.executeUpdate();
            ResultSet rs = pstmt.getGeneratedKeys();
            rs.next();
            long id = rs.getLong(1);
            rs.close();
            pstmt.close();
            return id;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to insert network offering ", e);
        }
    }

    protected void upgradeInstanceGroups(Connection conn) {
        try {

            // Create instance groups - duplicated names are allowed across accounts
            PreparedStatement pstmt = conn.prepareStatement("SELECT DISTINCT v.group, v.account_id from vm_instance v where v.group is not null");
            ResultSet rs = pstmt.executeQuery();
            ArrayList<Object[]> groups = new ArrayList<Object[]>();
            while (rs.next()) {
                Object[] group = new Object[10];
                group[0] = rs.getString(1); // group name
                group[1] = rs.getLong(2); // accountId
                groups.add(group);
            }
            rs.close();
            pstmt.close();

            for (Object[] group : groups) {
                String groupName = (String) group[0];
                Long accountId = (Long) group[1];
                createInstanceGroups(conn, groupName, accountId);
            }

            // update instance_group_vm_map
            pstmt = conn.prepareStatement("SELECT g.id, v.id from vm_instance v, instance_group g where g.name=v.group and g.account_id=v.account_id and v.group is not null");
            rs = pstmt.executeQuery();
            ArrayList<Object[]> groupVmMaps = new ArrayList<Object[]>();
            while (rs.next()) {
                Object[] groupMaps = new Object[10];
                groupMaps[0] = rs.getLong(1); // vmId
                groupMaps[1] = rs.getLong(2); // groupId
                groupVmMaps.add(groupMaps);
            }
            rs.close();
            pstmt.close();

            for (Object[] groupMap : groupVmMaps) {
                Long groupId = (Long) groupMap[0];
                Long instanceId = (Long) groupMap[1];
                createInstanceGroupVmMaps(conn, groupId, instanceId);
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't update instance groups ", e);
        }

    }

    protected void createInstanceGroups(Connection conn, String groupName, long accountId) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO instance_group (account_id, name, created) values (?, ?, now()) ");
        pstmt.setLong(1, accountId);
        pstmt.setString(2, groupName);
        pstmt.executeUpdate();
        pstmt.close();
    }

    protected void createInstanceGroupVmMaps(Connection conn, long groupId, long instanceId) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("INSERT INTO instance_group_vm_map (group_id, instance_id) values (?, ?) ");
        pstmt.setLong(1, groupId);
        pstmt.setLong(2, instanceId);
        pstmt.executeUpdate();
        pstmt.close();
    }

    protected long insertNic(Connection conn, long networkId, long instanceId, boolean running, String macAddress, String ipAddress, String netmask, String strategy, String gateway, String vnet,
            String guru, boolean defNic, int deviceId, String mode, String reservationId) throws SQLException {
        PreparedStatement pstmt = conn
                .prepareStatement(
                        "INSERT INTO nics (instance_id, network_id, mac_address, ip4_address, netmask, strategy, ip_type, broadcast_uri, mode, reserver_name, reservation_id, device_id, update_time, isolation_uri, ip6_address, default_nic, created, removed, state, gateway) "
                                + "VALUES (?,           ?,          ?,           ?,           ?,       ?,        'Ip4',   ?,             ?,    ?,             ?,              ?,         now(),       ?,          NULL,         ?,          now(),   NULL,    ?,     ?)",
                        Statement.RETURN_GENERATED_KEYS);
        int i = 1;
        String isolationUri = null;

        String broadcast = null;
        if (vnet != null) {
            broadcast = "vlan://" + vnet;
            if (vnet.equalsIgnoreCase("untagged")) {
                isolationUri = "ec2://" + vnet;
            } else {
                isolationUri = broadcast;
            }
        }
        pstmt.setLong(i++, instanceId);
        pstmt.setLong(i++, networkId);
        pstmt.setString(i++, macAddress);
        pstmt.setString(i++, ipAddress);
        pstmt.setString(i++, netmask);
        pstmt.setString(i++, strategy);
        pstmt.setString(i++, broadcast);
        pstmt.setString(i++, mode);
        pstmt.setString(i++, guru);
        pstmt.setString(i++, reservationId);
        pstmt.setInt(i++, deviceId);
        pstmt.setString(i++, isolationUri);
        pstmt.setBoolean(i++, defNic);
        pstmt.setString(i++, running ? "Reserved" : "Allocated");
        pstmt.setString(i++, gateway);
        pstmt.executeUpdate();
        ResultSet rs = pstmt.getGeneratedKeys();
        long nicId = 0;
        if (!rs.next()) {
            throw new CloudRuntimeException("Unable to get id for nic");
        }
        nicId = rs.getLong(1);
        rs.close();
        pstmt.close();
        return nicId;
    }

    protected void upgradeDomR(Connection conn, long dcId, long domrId, Long publicNetworkId, long guestNetworkId, long controlNetworkId, String zoneType, String vnet) throws SQLException {
        s_logger.debug("Upgrading domR" + domrId);
        PreparedStatement pstmt = conn
                .prepareStatement("SELECT vm_instance.id, vm_instance.state, vm_instance.private_mac_address, vm_instance.private_ip_address, vm_instance.private_netmask, domain_router.public_mac_address, domain_router.public_ip_address, domain_router.public_netmask, domain_router.guest_mac_address, domain_router.guest_ip_address, domain_router.guest_netmask, domain_router.vnet, domain_router.gateway FROM vm_instance INNER JOIN domain_router ON vm_instance.id=domain_router.id WHERE vm_instance.removed is NULL AND vm_instance.id=?");
        pstmt.setLong(1, domrId);
        ResultSet rs = pstmt.executeQuery();

        if (!rs.next()) {
            throw new CloudRuntimeException("Unable to find router " + domrId);
        }

        // long id = rs.getLong(1);
        String state = rs.getString(2);
        boolean running = state.equals("Running") | state.equals("Starting") | state.equals("Stopping");
        String privateMac = rs.getString(3);
        String privateIp = rs.getString(4);
        String privateNetmask = rs.getString(5);
        String publicMac = rs.getString(6);
        String publicIp = rs.getString(7);
        String publicNetmask = rs.getString(8);
        String guestMac = rs.getString(9);
        String guestIp = rs.getString(10);
        String guestNetmask = rs.getString(11);
        String gateway = rs.getString(13);
        rs.close();
        pstmt.close();

        pstmt = conn.prepareStatement("SELECT v.vlan_id from vlan v, user_ip_address u where v.id=u.vlan_db_id and u.public_ip_address=?");
        pstmt.setString(1, publicIp);
        rs = pstmt.executeQuery();

        String publicVlan = null;
        while (rs.next()) {
            publicVlan = rs.getString(1);
        }

        // Control nic is the same for all types of networks
        long controlNicId = insertNic(conn, controlNetworkId, domrId, running, privateMac, privateIp, privateNetmask, "Start", "169.254.0.1", null, "ControlNetworkGuru", false, 1, "Static",
                privateIp != null ? (domrId + privateIp) : null);
        if (privateIp != null) {
            pstmt = conn.prepareStatement("UPDATE op_dc_link_local_ip_address_alloc SET instance_id=? WHERE ip_address=? AND data_center_id=?");
            pstmt.setLong(1, controlNicId);
            pstmt.setString(2, privateIp);
            pstmt.setLong(3, dcId);
            pstmt.executeUpdate();
            pstmt.close();
        }

        if (zoneType.equalsIgnoreCase("Basic")) {
            insertNic(conn, guestNetworkId, domrId, running, guestMac, guestIp, guestNetmask, "Create", gateway, vnet, "DirectPodBasedNetworkGuru", true, 0, "Dhcp", null);
        } else if (publicIp != null) {
            // update virtual domR
            insertNic(conn, publicNetworkId, domrId, running, publicMac, publicIp, publicNetmask, "Create", gateway, publicVlan, "PublicNetworkGuru", true, 2, "Static", null);
            insertNic(conn, guestNetworkId, domrId, running, guestMac, guestIp, guestNetmask, "Start", null, vnet, "ExternalGuestNetworkGuru", false, 0, "Dhcp", null);
        } else {
            // update direct domR - dhcp case
            insertNic(conn, guestNetworkId, domrId, running, guestMac, guestIp, guestNetmask, "Create", gateway, vnet, "DirectNetworkGuru", true, 0, "Dhcp", null);
        }

    }

    protected void upgradeSsvm(Connection conn, long dataCenterId, long publicNetworkId, long managementNetworkId, long controlNetworkId, String zoneType) throws SQLException {
        s_logger.debug("Upgrading ssvm in " + dataCenterId);
        PreparedStatement pstmt = conn
                .prepareStatement("SELECT vm_instance.id, vm_instance.state, vm_instance.private_mac_address, vm_instance.private_ip_address, vm_instance.private_netmask, secondary_storage_vm.public_mac_address, secondary_storage_vm.public_ip_address, secondary_storage_vm.public_netmask, secondary_storage_vm.guest_mac_address, secondary_storage_vm.guest_ip_address, secondary_storage_vm.guest_netmask, secondary_storage_vm.gateway, vm_instance.type FROM vm_instance INNER JOIN secondary_storage_vm ON vm_instance.id=secondary_storage_vm.id WHERE vm_instance.removed is NULL AND vm_instance.data_center_id=? AND vm_instance.type='SecondaryStorageVm'");
        pstmt.setLong(1, dataCenterId);
        ResultSet rs = pstmt.executeQuery();

        if (!rs.next()) {
            s_logger.debug("Unable to find ssvm in data center " + dataCenterId);
            return;
        }

        long ssvmId = rs.getLong(1);
        String state = rs.getString(2);
        boolean running = state.equals("Running") | state.equals("Starting") | state.equals("Stopping");
        String privateMac = rs.getString(3);
        String privateIp = rs.getString(4);
        String privateNetmask = rs.getString(5);
        String publicMac = rs.getString(6);
        String publicIp = rs.getString(7);
        String publicNetmask = rs.getString(8);
        String guestMac = rs.getString(9);
        String guestIp = rs.getString(10);
        String guestNetmask = rs.getString(11);
        String gateway = rs.getString(12);
        String type = rs.getString(13);
        rs.close();
        pstmt.close();

        pstmt = conn
                .prepareStatement("SELECT host_pod_ref.gateway from host_pod_ref INNER JOIN vm_instance ON vm_instance.pod_id=host_pod_ref.id WHERE vm_instance.removed is NULL AND vm_instance.data_center_id=? AND vm_instance.type='SecondaryStorageVm'");
        pstmt.setLong(1, dataCenterId);
        rs = pstmt.executeQuery();

        if (!rs.next()) {
            s_logger.debug("Unable to find ssvm in data center " + dataCenterId);
            return;
        }

        String podGateway = rs.getString(1);
        rs.close();
        pstmt.close();

        pstmt = conn.prepareStatement("SELECT v.vlan_id from vlan v, user_ip_address u where v.id=u.vlan_db_id and u.public_ip_address=?");
        pstmt.setString(1, publicIp);
        rs = pstmt.executeQuery();

        String publicVlan = null;
        while (rs.next()) {
            publicVlan = rs.getString(1);
        }

        rs.close();
        pstmt.close();

        if (zoneType.equalsIgnoreCase("Basic")) {
            insertNic(conn, publicNetworkId, ssvmId, running, publicMac, publicIp, publicNetmask, "Create", gateway, publicVlan, "DirectPodBasedNetworkGuru", true, 2, "Dhcp", null);

        } else {
            insertNic(conn, publicNetworkId, ssvmId, running, publicMac, publicIp, publicNetmask, "Create", gateway, publicVlan, "PublicNetworkGuru", true, 2, "Static", null);
        }

        long controlNicId = insertNic(conn, controlNetworkId, ssvmId, running, guestMac, guestIp, guestNetmask, "Start", "169.254.0.1", null, "ControlNetworkGuru", false, 0, "Static",
                guestIp != null ? (ssvmId + guestIp) : null);
        if (guestIp != null) {
            pstmt = conn.prepareStatement("UPDATE op_dc_link_local_ip_address_alloc SET instance_id=? WHERE ip_address=? AND data_center_id=?");
            pstmt.setLong(1, controlNicId);
            pstmt.setString(2, guestIp);
            pstmt.setLong(3, dataCenterId);
            pstmt.executeUpdate();
            pstmt.close();
        }

        long mgmtNicId = insertNic(conn, managementNetworkId, ssvmId, running, privateMac, privateIp, privateNetmask, "Start", podGateway, null, "PodBasedNetworkGuru", false, 1, "Static", null);
        if (privateIp != null) {
            pstmt = conn.prepareStatement("UPDATE op_dc_ip_address_alloc SET instance_id=? WHERE ip_address=? AND data_center_id=?");
            pstmt.setLong(1, mgmtNicId);
            pstmt.setString(2, privateIp);
            pstmt.setLong(3, dataCenterId);
            pstmt.executeUpdate();
            pstmt.close();
        }
    }

    protected void upgradeConsoleProxy(Connection conn, long dcId, long cpId, long publicNetworkId, long managementNetworkId, long controlNetworkId, String zoneType) throws SQLException {
        s_logger.debug("Upgrading cp" + cpId);
        PreparedStatement pstmt = conn
                .prepareStatement("SELECT vm_instance.id, vm_instance.state, vm_instance.private_mac_address, vm_instance.private_ip_address, vm_instance.private_netmask, console_proxy.public_mac_address, console_proxy.public_ip_address, console_proxy.public_netmask, console_proxy.guest_mac_address, console_proxy.guest_ip_address, console_proxy.guest_netmask, console_proxy.gateway, vm_instance.type FROM vm_instance INNER JOIN console_proxy ON vm_instance.id=console_proxy.id WHERE vm_instance.removed is NULL AND vm_instance.id=?");
        pstmt.setLong(1, cpId);
        ResultSet rs = pstmt.executeQuery();

        if (!rs.next()) {
            throw new CloudRuntimeException("Unable to find cp " + cpId);
        }

        long id = rs.getLong(1);
        String state = rs.getString(2);
        boolean running = state.equals("Running") | state.equals("Starting") | state.equals("Stopping");
        String privateMac = rs.getString(3);
        String privateIp = rs.getString(4);
        String privateNetmask = rs.getString(5);
        String publicMac = rs.getString(6);
        String publicIp = rs.getString(7);
        String publicNetmask = rs.getString(8);
        String guestMac = rs.getString(9);
        String guestIp = rs.getString(10);
        String guestNetmask = rs.getString(11);
        String gateway = rs.getString(12);
        String type = rs.getString(13);
        rs.close();
        pstmt.close();

        pstmt = conn.prepareStatement("SELECT host_pod_ref.gateway from host_pod_ref INNER JOIN vm_instance ON vm_instance.pod_id=host_pod_ref.id WHERE vm_instance.id=?");
        pstmt.setLong(1, cpId);
        rs = pstmt.executeQuery();

        if (!rs.next()) {
            throw new CloudRuntimeException("Unable to find cp " + cpId);
        }

        String podGateway = rs.getString(1);
        rs.close();
        pstmt.close();

        pstmt = conn.prepareStatement("SELECT v.vlan_id from vlan v, user_ip_address u where v.id=u.vlan_db_id and u.public_ip_address=?");
        pstmt.setString(1, publicIp);
        rs = pstmt.executeQuery();

        String publicVlan = null;
        while (rs.next()) {
            publicVlan = rs.getString(1);
        }

        rs.close();
        pstmt.close();

        if (zoneType.equalsIgnoreCase("Basic")) {
            insertNic(conn, publicNetworkId, cpId, running, publicMac, publicIp, publicNetmask, "Create", gateway, publicVlan, "DirectPodBasedNetworkGuru", true, 2, "Dhcp", null);
        } else {
            insertNic(conn, publicNetworkId, cpId, running, publicMac, publicIp, publicNetmask, "Create", gateway, publicVlan, "PublicNetworkGuru", true, 2, "Static", null);
        }

        long controlNicId = insertNic(conn, controlNetworkId, cpId, running, guestMac, guestIp, guestNetmask, "Start", "169.254.0.1", null, "ControlNetworkGuru", false, 0, "Static",
                guestIp != null ? (cpId + guestIp) : null);
        if (guestIp != null) {
            pstmt = conn.prepareStatement("UPDATE op_dc_link_local_ip_address_alloc SET instance_id=? WHERE ip_address=? AND data_center_id=?");
            pstmt.setLong(1, controlNicId);
            pstmt.setString(2, guestIp);
            pstmt.setLong(3, dcId);
            pstmt.executeUpdate();
            pstmt.close();
        }
        long mgmtNicId = insertNic(conn, managementNetworkId, cpId, running, privateMac, privateIp, privateNetmask, "Start", podGateway, null, "PodBasedNetworkGuru", false, 1, "Static",
                privateIp != null ? (cpId + privateIp) : null);
        if (privateIp != null) {
            pstmt = conn.prepareStatement("UPDATE op_dc_ip_address_alloc SET instance_id=? WHERE ip_address=? AND data_center_id=?");
            pstmt.setLong(1, mgmtNicId);
            pstmt.setString(2, privateIp);
            pstmt.setLong(3, dcId);
            pstmt.executeUpdate();
            pstmt.close();
        }
    }

    protected void upgradeUserVms(Connection conn, long domainRouterId, long networkId, String gateway, String vnet, String guruName, String strategy) throws SQLException {
        PreparedStatement pstmt = conn
                .prepareStatement("SELECT vm_instance.id, vm_instance.private_mac_address, vm_instance.private_ip_address, vm_instance.private_netmask, vm_instance.state, vm_instance.type FROM vm_instance INNER JOIN user_vm ON vm_instance.id=user_vm.id WHERE user_vm.domain_router_id=? and vm_instance.removed IS NULL");
        pstmt.setLong(1, domainRouterId);
        ResultSet rs = pstmt.executeQuery();
        List<Object[]> vms = new ArrayList<Object[]>();
        while (rs.next()) {
            Object[] vm = new Object[10];
            vm[0] = rs.getLong(1); // vm id
            vm[1] = rs.getString(2); // mac address
            vm[2] = rs.getString(3); // ip address
            vm[3] = rs.getString(4); // netmask
            vm[4] = rs.getString(5); // vm state
            vms.add(vm);
        }
        rs.close();
        pstmt.close();

        s_logger.debug("Upgrading " + vms.size() + " vms for router " + domainRouterId);

        int count = 0;
        for (Object[] vm : vms) {
            String state = (String) vm[4];

            boolean running = false;
            if (state.equals("Running") || state.equals("Starting") || state.equals("Stopping")) {
                running = true;
                count++;
            }

            insertNic(conn, networkId, (Long) vm[0], running, (String) vm[1], (String) vm[2], (String) vm[3], strategy, gateway, vnet, guruName, true, 0, "Dhcp", null);
        }

        pstmt = conn.prepareStatement("SELECT state FROM vm_instance WHERE id=?");
        pstmt.setLong(1, domainRouterId);
        rs = pstmt.executeQuery();
        rs.next();
        String state = rs.getString(1);
        if (state.equals("Running") || state.equals("Starting") || state.equals("Stopping")) {
            count++;
        }
        rs.close();
        pstmt.close();

        Long originalNicsCount = 0L;
        pstmt = conn.prepareStatement("SELECT nics_count from op_networks where id=?");
        pstmt.setLong(1, networkId);
        ResultSet originalCountRs = pstmt.executeQuery();

        if (originalCountRs.next()) {
            originalNicsCount = originalCountRs.getLong(1);
        }

        Long resultCount = originalNicsCount + count;
        originalCountRs.close();
        pstmt.close();

        pstmt = conn.prepareStatement("UPDATE op_networks SET nics_count=?, check_for_gc=? WHERE id=?");
        pstmt.setLong(1, resultCount);
        if (count == 0) {
            pstmt.setBoolean(2, false);
        } else {
            pstmt.setBoolean(2, true);
        }
        pstmt.setLong(3, networkId);
        pstmt.executeUpdate();
        pstmt.close();
    }

    protected long insertNetwork(Connection conn, String name, String displayText, String trafficType, String broadcastDomainType, String broadcastUri, String gateway, String cidr, String mode,
            long networkOfferingId, long dataCenterId, String guruName, String state, long domainId, long accountId, String dns1, String dns2, String guestType, boolean shared, String networkDomain,
            boolean isDefault, String reservationId) {
        String getNextNetworkSequenceSql = "SELECT value from sequence where name='networks_seq'";
        String advanceNetworkSequenceSql = "UPDATE sequence set value=value+1 where name='networks_seq'";
        String insertNetworkSql = "INSERT INTO networks(id, name, display_text, traffic_type, broadcast_domain_type, gateway, cidr, mode, network_offering_id, data_center_id, guru_name, state, domain_id, account_id, dns1, dns2, guest_type, shared, is_default, created, network_domain, related, reservation_id, broadcast_uri) "
                + "VALUES(?,  ?,    ?,            ?,            ?,                     ?,       ?,    ?,    ?,                   ?,              ?,         ?,     ?,         ?,          ?,    ?,    ?,          ?,      ?,          now(),   ?,              ?,       ?,              ?)";
        try {
            PreparedStatement pstmt = conn.prepareStatement(getNextNetworkSequenceSql);
            ResultSet rs = pstmt.executeQuery();
            rs.next();
            long seq = rs.getLong(1);
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement(advanceNetworkSequenceSql);
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = conn.prepareStatement(insertNetworkSql);
            int i = 1;
            pstmt.setLong(i++, seq);
            pstmt.setString(i++, name);
            pstmt.setString(i++, displayText);
            pstmt.setString(i++, trafficType);
            pstmt.setString(i++, broadcastDomainType);
            pstmt.setString(i++, gateway);
            pstmt.setString(i++, cidr);
            pstmt.setString(i++, mode);
            pstmt.setLong(i++, networkOfferingId);
            pstmt.setLong(i++, dataCenterId);
            pstmt.setString(i++, guruName);
            pstmt.setString(i++, state);
            pstmt.setLong(i++, domainId);
            pstmt.setLong(i++, accountId);
            pstmt.setString(i++, dns1);
            pstmt.setString(i++, dns2);
            pstmt.setString(i++, guestType);
            pstmt.setBoolean(i++, shared);
            pstmt.setBoolean(i++, isDefault);
            pstmt.setString(i++, networkDomain);
            pstmt.setLong(i++, seq);
            pstmt.setString(i++, reservationId);
            pstmt.setString(i++, broadcastUri);
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("INSERT INTO op_networks(id, mac_address_seq, nics_count, gc, check_for_gc) VALUES(?, ?, ?, ?, ?)");
            pstmt.setLong(1, seq);
            pstmt.setLong(2, 0);
            pstmt.setLong(3, 0);
            if (trafficType.equals("Guest")) {
                pstmt.setBoolean(4, true);
            } else {
                pstmt.setBoolean(4, false);
            }
            pstmt.setBoolean(5, false);
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("INSERT INTO account_network_ref (account_id, network_id, is_owner) VALUES (?,    ?,  1)");
            pstmt.setLong(1, accountId);
            pstmt.setLong(2, seq);
            pstmt.executeUpdate();

            return seq;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create network", e);
        }
    }

    protected void upgradeManagementIpAddress(Connection conn, long dcId) throws SQLException {
        PreparedStatement pstmt = conn.prepareStatement("SELECT op_dc_ip_address_alloc.id FROM op_dc_ip_address_alloc WHERE data_center_id=?");
        pstmt.setLong(1, dcId);
        ResultSet rs = pstmt.executeQuery();
        ArrayList<Object[]> allocatedIps = new ArrayList<Object[]>();
        while (rs.next()) {
            Object[] ip = new Object[10];
            ip[0] = rs.getLong(1); // id
            allocatedIps.add(ip);
        }
        rs.close();
        pstmt.close();

        for (Object[] allocatedIp : allocatedIps) {
            pstmt = conn.prepareStatement("SELECT mac_address FROM data_center WHERE id = ?");
            pstmt.setLong(1, dcId);
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new CloudRuntimeException("Unable to get mac address for data center " + dcId);
            }
            long mac = rs.getLong(1);
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("UPDATE data_center SET mac_address=mac_address+1 WHERE id = ?");
            pstmt.setLong(1, dcId);
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = conn.prepareStatement("UPDATE op_dc_ip_address_alloc SET mac_address=? WHERE id=?");
            pstmt.setLong(1, mac);
            pstmt.setLong(2, (Long) allocatedIp[0]);
            pstmt.executeUpdate();
            pstmt.close();
        }

    }

    protected void upgradeDirectUserIpAddress(Connection conn, long dcId, long networkId, String vlanType) throws SQLException {
        s_logger.debug("Upgrading user ip address for data center " + dcId + " network " + networkId + " vlan type " + vlanType);
        PreparedStatement pstmt = conn
                .prepareStatement("UPDATE user_ip_address INNER JOIN vlan ON user_ip_address.vlan_db_id=vlan.id SET user_ip_address.source_network_id=vlan.network_id WHERE user_ip_address.data_center_id=? AND vlan.vlan_type=?");
        pstmt.setLong(1, dcId);
        pstmt.setString(2, vlanType);
        pstmt.executeUpdate();
        pstmt.close();

        pstmt = conn
                .prepareStatement("SELECT user_ip_address.id, user_ip_address.public_ip_address, user_ip_address.account_id, user_ip_address.allocated FROM user_ip_address INNER JOIN vlan ON vlan.id=user_ip_address.vlan_db_id WHERE user_ip_address.data_center_id = ? AND vlan.vlan_type=?");
        pstmt.setLong(1, dcId);
        pstmt.setString(2, vlanType);
        ResultSet rs = pstmt.executeQuery();
        ArrayList<Object[]> allocatedIps = new ArrayList<Object[]>();
        while (rs.next()) {
            Object[] ip = new Object[10];
            ip[0] = rs.getLong(1); // id
            ip[1] = rs.getString(2); // ip address
            ip[2] = rs.getLong(3); // account id
            ip[3] = rs.getDate(4); // allocated
            allocatedIps.add(ip);
        }
        rs.close();
        pstmt.close();

        s_logger.debug("Marking " + allocatedIps.size() + " ip addresses to belong to network " + networkId);
        s_logger.debug("Updating mac addresses for data center id=" + dcId + ". Found " + allocatedIps.size() + " ip addresses to update");

        for (Object[] allocatedIp : allocatedIps) {
            pstmt = conn.prepareStatement("SELECT mac_address FROM data_center WHERE id = ?");
            pstmt.setLong(1, dcId);
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new CloudRuntimeException("Unable to get mac address for data center " + dcId);
            }
            long mac = rs.getLong(1);
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("UPDATE data_center SET mac_address=mac_address+1 WHERE id = ?");
            pstmt.setLong(1, dcId);
            pstmt.executeUpdate();
            pstmt.close();

            pstmt = conn.prepareStatement("UPDATE user_ip_address SET mac_address=? WHERE id=?");
            pstmt.setLong(1, mac);
            pstmt.setLong(2, (Long) allocatedIp[0]);
            pstmt.executeUpdate();
            pstmt.close();
        }
    }

    protected void upgradePublicUserIpAddress(Connection conn, long dcId, long networkId, String vlanType) throws SQLException {
        s_logger.debug("Upgrading user ip address for data center " + dcId + " network " + networkId + " vlan type " + vlanType);
        PreparedStatement pstmt = conn
                .prepareStatement("UPDATE user_ip_address INNER JOIN vlan ON user_ip_address.vlan_db_id=vlan.id SET source_network_id=? WHERE user_ip_address.data_center_id=? AND vlan.vlan_type=?");
        pstmt.setLong(1, networkId);
        pstmt.setLong(2, dcId);
        pstmt.setString(3, vlanType);
        pstmt.executeUpdate();
        pstmt.close();

        pstmt = conn.prepareStatement("UPDATE vlan SET network_id = ? WHERE data_center_id=? AND vlan_type=?");
        pstmt.setLong(1, networkId);
        pstmt.setLong(2, dcId);
        pstmt.setString(3, vlanType);
        pstmt.executeUpdate();
        pstmt.close();

        pstmt = conn
                .prepareStatement("SELECT user_ip_address.id, user_ip_address.public_ip_address, user_ip_address.account_id, user_ip_address.allocated FROM user_ip_address INNER JOIN vlan ON vlan.id=user_ip_address.vlan_db_id WHERE user_ip_address.data_center_id = ? AND vlan.vlan_type='VirtualNetwork'");
        pstmt.setLong(1, dcId);
        ResultSet rs = pstmt.executeQuery();
        ArrayList<Object[]> allocatedIps = new ArrayList<Object[]>();
        while (rs.next()) {
            Object[] ip = new Object[10];
            ip[0] = rs.getLong(1); // id
            ip[1] = rs.getString(2); // ip address
            ip[2] = rs.getLong(3); // account id
            ip[3] = rs.getDate(4); // allocated
            allocatedIps.add(ip);
        }
        rs.close();
        pstmt.close();

        for (Object[] allocatedIp : allocatedIps) {
            pstmt = conn.prepareStatement("SELECT mac_address FROM data_center WHERE id = ?");
            pstmt.setLong(1, dcId);
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                throw new CloudRuntimeException("Unable to get mac address for data center " + dcId);
            }
            long mac = rs.getLong(1);
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("UPDATE data_center SET mac_address=mac_address+1 WHERE id = ?");
            pstmt.setLong(1, dcId);
            pstmt.executeUpdate();
            pstmt.close();

            Long associatedNetworkId = null;
            if (allocatedIp[3] != null && allocatedIp[2] != null) {
                pstmt = conn.prepareStatement("SELECT id FROM networks WHERE data_center_id=? AND account_id=?");
                pstmt.setLong(1, dcId);
                pstmt.setLong(2, (Long) allocatedIp[2]);
                rs = pstmt.executeQuery();
                if (!rs.next()) {
                    throw new CloudRuntimeException("Unable to find a network for account " + allocatedIp[2] + " in dc " + dcId);
                }
                associatedNetworkId = rs.getLong(1);
                rs.close();
                pstmt.close();
            }
            pstmt = conn.prepareStatement("UPDATE user_ip_address SET mac_address=?, network_id=? WHERE id=?");
            pstmt.setLong(1, mac);
            if (associatedNetworkId != null) {
                pstmt.setLong(2, associatedNetworkId);
            } else {
                pstmt.setObject(2, null);
            }
            pstmt.setLong(3, (Long) allocatedIp[0]);
            pstmt.executeUpdate();
            pstmt.close();
        }

    }

    protected void upgradeDataCenter(Connection conn) {
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement("SELECT value FROM configuration WHERE name='direct.attach.untagged.vlan.enabled'");
            ResultSet rs = pstmt.executeQuery();
            _basicZone = !rs.next() || Boolean.parseBoolean(rs.getString(1));
            rs.close();
            pstmt.close();
            pstmt = conn
                    .prepareStatement("UPDATE data_center SET networktype=?, dns_provider=?, gateway_provider=?, firewall_provider=?, dhcp_provider=?, lb_provider=?, vpn_provider=?, userdata_provider=?");
            if (_basicZone) {
                pstmt.setString(1, "Basic");
                pstmt.setString(2, "DhcpServer");
                pstmt.setString(3, null);
                pstmt.setString(4, null);
                pstmt.setString(5, "DhcpServer");
                pstmt.setString(6, null);
                pstmt.setString(7, null);
                pstmt.setString(8, "DhcpServer");
            } else {
                pstmt.setString(1, "Advanced");
                pstmt.setString(2, "VirtualRouter");
                pstmt.setString(3, "VirtualRouter");
                pstmt.setString(4, "VirtualRouter");
                pstmt.setString(5, "VirtualRouter");
                pstmt.setString(6, "VirtualRouter");
                pstmt.setString(7, "VirtualRouter");
                pstmt.setString(8, "VirtualRouter");
            }
            pstmt.executeUpdate();
            pstmt.close();

            // For basic zone vnet field should be NULL

            if (_basicZone) {
                pstmt = conn.prepareStatement("UPDATE data_center SET vnet=?, guest_network_cidr=?");
                pstmt.setString(1, null);
                pstmt.setString(2, null);
                pstmt.executeUpdate();
                pstmt.close();
            }

            pstmt = conn.prepareStatement("SELECT id, guest_network_cidr, domain FROM data_center");
            rs = pstmt.executeQuery();
            ArrayList<Object[]> dcs = new ArrayList<Object[]>();
            while (rs.next()) {
                Object[] dc = new Object[10];
                dc[0] = rs.getLong(1); // data center id
                dc[1] = rs.getString(2); // guest network cidr
                dc[2] = rs.getString(3); // network domain
                dcs.add(dc);
            }
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("SELECT id FROM network_offerings WHERE name='System-Management-Network'");
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                s_logger.error("Unable to find the management network offering.");
                throw new CloudRuntimeException("Unable to find the management network offering.");
            }
            long managementNetworkOfferingId = rs.getLong(1);
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("SELECT id FROM network_offerings WHERE name='System-Public-Network'");
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                s_logger.error("Unable to find the public network offering.");
                throw new CloudRuntimeException("Unable to find the public network offering.");
            }
            long publicNetworkOfferingId = rs.getLong(1);
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("SELECT id FROM network_offerings WHERE name='System-Control-Network'");
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                s_logger.error("Unable to find the control network offering.");
                throw new CloudRuntimeException("Unable to find the control network offering.");
            }
            long controlNetworkOfferingId = rs.getLong(1);
            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("SELECT id FROM network_offerings WHERE name='System-Storage-Network'");
            rs = pstmt.executeQuery();
            if (!rs.next()) {
                s_logger.error("Unable to find the storage network offering.");
                throw new CloudRuntimeException("Unable to find the storage network offering.");
            }
            long storageNetworkOfferingId = rs.getLong(1);
            rs.close();
            pstmt.close();

            if (_basicZone) {
                for (Object[] dc : dcs) {
                    Long dcId = (Long) dc[0];
                    long mgmtNetworkId = insertNetwork(conn, "ManagementNetwork" + dcId, "Management Network created for Zone " + dcId, "Management", "Native", null, null, null, "Static",
                            managementNetworkOfferingId, dcId, "PodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
                    long storageNetworkId = insertNetwork(conn, "StorageNetwork" + dcId, "Storage Network created for Zone " + dcId, "Storage", "Native", null, null, null, "Static",
                            storageNetworkOfferingId, dcId, "PodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
                    long controlNetworkId = insertNetwork(conn, "ControlNetwork" + dcId, "Control Network created for Zone " + dcId, "Control", "LinkLocal", null, NetUtils.getLinkLocalGateway(),
                            NetUtils.getLinkLocalCIDR(), "Static", controlNetworkOfferingId, dcId, "ControlNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
                    upgradeManagementIpAddress(conn, dcId);
                    long basicDefaultDirectNetworkId = insertNetwork(conn, "BasicZoneDirectNetwork" + dcId, "Basic Zone Direct Network created for Zone " + dcId, "Guest", "Native", null, null, null,
                            "Dhcp", 5, dcId, "DirectPodBasedNetworkGuru", "Setup", 1, 1, null, null, "Direct", true, null, true, null);

                    pstmt = conn.prepareStatement("SELECT id FROM vlan WHERE vlan_type='DirectAttached' AND data_center_id=?");
                    pstmt.setLong(1, dcId);
                    rs = pstmt.executeQuery();
                    while (rs.next()) {
                        long vlanId = rs.getLong(1);

                        pstmt = conn.prepareStatement("UPDATE vlan SET network_id=? WHERE id=?");
                        pstmt.setLong(1, basicDefaultDirectNetworkId);
                        pstmt.setLong(2, vlanId);
                        pstmt.executeUpdate();
                        pstmt.close();
                    }

                    upgradeDirectUserIpAddress(conn, dcId, basicDefaultDirectNetworkId, "DirectAttached");

                    // update Dhcp servers information in domain_router and vm_instance tables; all domRs belong to the same
                    // network
                    pstmt = conn
                            .prepareStatement("SELECT vm_instance.id, vm_instance.domain_id, vm_instance.account_id, domain_router.gateway, domain_router.guest_ip_address, domain_router.domain, domain_router.dns1, domain_router.dns2, domain_router.vnet FROM vm_instance INNER JOIN domain_router ON vm_instance.id=domain_router.id WHERE vm_instance.removed IS NULL AND vm_instance.type='DomainRouter' AND vm_instance.data_center_id=?");
                    pstmt.setLong(1, dcId);
                    rs = pstmt.executeQuery();
                    ArrayList<Object[]> routers = new ArrayList<Object[]>();
                    while (rs.next()) {
                        Object[] router = new Object[40];
                        router[0] = rs.getLong(1); // router id
                        router[1] = rs.getString(4); // router gateway which is gonna be gateway for user vms
                        routers.add(router);
                    }
                    rs.close();
                    pstmt.close();

                    for (Object[] router : routers) {
                        s_logger.debug("Updating domR with network id in basic zone id=" + dcId);
                        pstmt = conn.prepareStatement("UPDATE domain_router SET network_id = ? wHERE id = ? ");
                        pstmt.setLong(1, basicDefaultDirectNetworkId);
                        pstmt.setLong(2, (Long) router[0]);
                        pstmt.executeUpdate();
                        pstmt.close();

                        upgradeUserVms(conn, (Long) router[0], basicDefaultDirectNetworkId, (String) router[1], "untagged", "DirectPodBasedNetworkGuru", "Create");
                        upgradeDomR(conn, dcId, (Long) router[0], null, basicDefaultDirectNetworkId, controlNetworkId, "Basic", "untagged");
                    }

                    upgradeSsvm(conn, dcId, basicDefaultDirectNetworkId, mgmtNetworkId, controlNetworkId, "Basic");

                    pstmt = conn.prepareStatement("SELECT vm_instance.id FROM vm_instance WHERE removed IS NULL AND type='ConsoleProxy' AND data_center_id=?");
                    pstmt.setLong(1, dcId);
                    rs = pstmt.executeQuery();
                    while (rs.next()) {
                        upgradeConsoleProxy(conn, dcId, rs.getLong(1), basicDefaultDirectNetworkId, mgmtNetworkId, controlNetworkId, "Basic");
                    }

                }
            } else {
                for (Object[] dc : dcs) {
                    Long dcId = (Long) dc[0];
                    long mgmtNetworkId = insertNetwork(conn, "ManagementNetwork" + dcId, "Management Network created for Zone " + dcId, "Management", "Native", null, null, null, "Static",
                            managementNetworkOfferingId, dcId, "PodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
                    insertNetwork(conn, "StorageNetwork" + dcId, "Storage Network created for Zone " + dcId, "Storage", "Native", null, null, null, "Static", storageNetworkOfferingId, dcId,
                            "PodBasedNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
                    long controlNetworkId = insertNetwork(conn, "ControlNetwork" + dcId, "Control Network created for Zone " + dcId, "Control", "Native", null, null, null, "Static",
                            controlNetworkOfferingId, dcId, "ControlNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);
                    upgradeManagementIpAddress(conn, dcId);
                    long publicNetworkId = insertNetwork(conn, "PublicNetwork" + dcId, "Public Network Created for Zone " + dcId, "Public", "Vlan", null, null, null, "Static",
                            publicNetworkOfferingId, dcId, "PublicNetworkGuru", "Setup", 1, 1, null, null, null, true, null, false, null);

                    pstmt = conn
                            .prepareStatement("SELECT vm_instance.id, vm_instance.domain_id, vm_instance.account_id, domain_router.guest_ip_address, domain_router.domain, domain_router.dns1, domain_router.dns2, domain_router.vnet FROM vm_instance INNER JOIN domain_router ON vm_instance.id=domain_router.id WHERE vm_instance.removed IS NULL AND vm_instance.type='DomainRouter' AND vm_instance.data_center_id=? and domain_router.role='DHCP_FIREWALL_LB_PASSWD_USERDATA'");
                    pstmt.setLong(1, dcId);
                    rs = pstmt.executeQuery();
                    ArrayList<Object[]> routers = new ArrayList<Object[]>();
                    while (rs.next()) {
                        Object[] router = new Object[40];
                        router[0] = rs.getLong(1); // router id
                        router[1] = rs.getLong(2); // domain id
                        router[2] = rs.getLong(3); // account id
                        router[3] = rs.getString(4); // guest ip which becomes the gateway in network
                        router[4] = rs.getString(5); // domain name
                        router[5] = rs.getString(6); // dns1
                        router[6] = rs.getString(7); // dns2
                        router[7] = rs.getString(8); // vnet
                        routers.add(router);
                    }
                    rs.close();
                    pstmt.close();

                    for (Object[] router : routers) {
                        String vnet = (String) router[7];
                        String reservationId = null;
                        String state = "Allocated";
                        if (vnet != null) {
                            reservationId = dcId + "-" + vnet;
                            state = "Implemented";
                        }

                        String vlan = null;
                        if (vnet != null) {
                            vlan = "vlan://" + vnet;
                        }

                        long virtualNetworkId = insertNetwork(conn, "VirtualNetwork" + router[0], "Virtual Network for " + router[0], "Guest", "Vlan", vlan, (String) router[3], (String) dc[1],
                                "Dhcp", 6, dcId, "ExternalGuestNetworkGuru", state, (Long) router[1], (Long) router[2], (String) router[5], (String) router[6], "Virtual", false, (String) router[4],
                                true, reservationId);
                        pstmt = conn.prepareStatement("UPDATE domain_router SET network_id = ? wHERE id = ? ");
                        pstmt.setLong(1, virtualNetworkId);
                        pstmt.setLong(2, (Long) router[0]);
                        pstmt.executeUpdate();
                        pstmt.close();
                        s_logger.debug("Network inserted for " + router[0] + " id = " + virtualNetworkId);

                        upgradeUserVms(conn, (Long) router[0], virtualNetworkId, (String) router[3], vnet, "ExternalGuestNetworkGuru", "Start");
                        upgradeDomR(conn, dcId, (Long) router[0], publicNetworkId, virtualNetworkId, controlNetworkId, "Advanced", vnet);
                    }

                    upgradePublicUserIpAddress(conn, dcId, publicNetworkId, "VirtualNetwork");

                    // Create direct networks
                    pstmt = conn.prepareStatement("SELECT id, vlan_id, vlan_gateway, vlan_netmask FROM vlan WHERE vlan_type='DirectAttached' AND data_center_id=?");
                    pstmt.setLong(1, dcId);
                    rs = pstmt.executeQuery();
                    HashMap<String, Long> vlanNetworkMap = new HashMap<String, Long>();
                    while (rs.next()) {
                        long vlanId = rs.getLong(1);
                        String tag = rs.getString(2);
                        String gateway = rs.getString(3);
                        String netmask = rs.getString(4);
                        String cidr = NetUtils.getCidrFromGatewayAndNetmask(gateway, netmask);

                        // Get the owner of the network
                        Long accountId = 1L;
                        Long domainId = 1L;
                        boolean isShared = true;
                        pstmt = conn.prepareStatement("SELECT account_id FROM account_vlan_map WHERE account_id IS NOT NULL AND vlan_db_id=?");
                        pstmt.setLong(1, vlanId);
                        ResultSet accountRs = pstmt.executeQuery();
                        while (accountRs.next()) {
                            isShared = false;
                            accountId = accountRs.getLong(1);
                            pstmt = conn.prepareStatement("SELECT domain_id FROM account WHERE id=?");
                            pstmt.setLong(1, accountId);
                            ResultSet domainRs = pstmt.executeQuery();
                            while (domainRs.next()) {
                                domainId = domainRs.getLong(1);
                            }
                        }

                        if (vlanNetworkMap.get(tag) == null) {
                            long directNetworkId = insertNetwork(conn, "DirectNetwork" + vlanId, "Direct network created for " + vlanId, "Guest", "Vlan", "vlan://" + tag, gateway, cidr, "Dhcp", 7,
                                    dcId, "DirectNetworkGuru", "Setup", domainId, accountId, null, null, "Direct", isShared, (String) dc[2], true, null);
                            vlanNetworkMap.put(tag, directNetworkId);
                        }

                        pstmt = conn.prepareStatement("UPDATE vlan SET network_id=? WHERE id=?");
                        pstmt.setLong(1, vlanNetworkMap.get(tag));
                        pstmt.setLong(2, vlanId);
                        pstmt.executeUpdate();

                        pstmt.close();

                        upgradeDirectUserIpAddress(conn, dcId, vlanNetworkMap.get(tag), "DirectAttached");
                        s_logger.debug("Created Direct networks and upgraded Direct ip addresses");
                    }

                    // Create DHCP domRs - Direct networks
                    pstmt = conn
                            .prepareStatement("SELECT vm_instance.id, domain_router.guest_ip_address FROM vm_instance INNER JOIN domain_router ON vm_instance.id=domain_router.id WHERE vm_instance.removed IS NULL AND vm_instance.type='DomainRouter' AND vm_instance.data_center_id=? and domain_router.role='DHCP_USERDATA'");
                    pstmt.setLong(1, dcId);
                    rs = pstmt.executeQuery();
                    ArrayList<Object[]> dhcpServers = new ArrayList<Object[]>();
                    while (rs.next()) {
                        Object[] dhcpServer = new Object[40];
                        dhcpServer[0] = rs.getLong(1); // router id
                        dhcpServer[1] = rs.getString(2); // guest IP address - direct ip address of the domR
                        dhcpServers.add(dhcpServer);
                    }
                    rs.close();
                    pstmt.close();

                    for (Object[] dhcpServer : dhcpServers) {
                        Long routerId = (Long) dhcpServer[0];
                        String directIp = (String) dhcpServer[1];

                        pstmt = conn.prepareStatement("SELECT u.source_network_id, v.vlan_id from user_ip_address u, vlan v where u.public_ip_address=? and v.id=u.vlan_db_id");
                        pstmt.setString(1, directIp);
                        rs = pstmt.executeQuery();
                        if (!rs.next()) {
                            throw new CloudRuntimeException("Unable to find Direct ip address " + directIp + " in user_ip_address table");
                        }

                        Long directNetworkId = rs.getLong(1);
                        String vnet = rs.getString(2);
                        rs.close();

                        pstmt = conn.prepareStatement("SELECT gateway from networks where id=?");
                        pstmt.setLong(1, directNetworkId);
                        rs = pstmt.executeQuery();
                        if (!rs.next()) {
                            throw new CloudRuntimeException("Unable to find gateway for network id=" + directNetworkId);
                        }

                        String gateway = rs.getString(1);
                        rs.close();

                        pstmt = conn.prepareStatement("UPDATE domain_router SET network_id = ? wHERE id = ? ");
                        pstmt.setLong(1, directNetworkId);
                        pstmt.setLong(2, routerId);
                        pstmt.executeUpdate();
                        pstmt.close();
                        s_logger.debug("NetworkId updated for router id=" + routerId + "with network id = " + directNetworkId);

                        upgradeUserVms(conn, routerId, directNetworkId, gateway, vnet, "DirectNetworkGuru", "Create");
                        s_logger.debug("Upgraded Direct vms in Advance zone id=" + dcId);
                        upgradeDomR(conn, dcId, routerId, null, directNetworkId, controlNetworkId, "Advanced", vnet);
                        s_logger.debug("Upgraded Direct domRs in Advance zone id=" + dcId);
                    }

                    // Upgrade SSVM
                    upgradeSsvm(conn, dcId, publicNetworkId, mgmtNetworkId, controlNetworkId, "Advanced");

                    // Upgrade ConsoleProxy
                    pstmt = conn.prepareStatement("SELECT vm_instance.id FROM vm_instance WHERE removed IS NULL AND type='ConsoleProxy' AND data_center_id=?");
                    pstmt.setLong(1, dcId);
                    rs = pstmt.executeQuery();
                    while (rs.next()) {
                        upgradeConsoleProxy(conn, dcId, rs.getLong(1), publicNetworkId, mgmtNetworkId, controlNetworkId, "Advanced");
                    }
                    pstmt.close();
                }
            }

        } catch (SQLException e) {
            s_logger.error("Can't update data center ", e);
            throw new CloudRuntimeException("Can't update data center ", e);
        }
    }

    private void updateUserStats(Connection conn) {
        try {

            // update device_type information
            PreparedStatement pstmt = conn.prepareStatement("UPDATE user_statistics SET device_type='DomainRouter'");
            pstmt.executeUpdate();
            pstmt.close();
            s_logger.debug("Upgraded userStatistcis with device_type=DomainRouter");

            // update device_id infrormation
            pstmt = conn.prepareStatement("SELECT id, account_id, data_center_id FROM user_statistics");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(1); // user stats id
                Long accountId = rs.getLong(2); // account id
                Long dataCenterId = rs.getLong(3); // zone id

                pstmt = conn.prepareStatement("SELECT networktype from data_center where id=?");
                pstmt.setLong(1, dataCenterId);

                ResultSet dcSet = pstmt.executeQuery();

                if (!dcSet.next()) {
                    s_logger.error("Unable to get data_center information as a part of user_statistics update");
                    throw new CloudRuntimeException("Unable to get data_center information as a part of user_statistics update");
                }

                String dataCenterType = dcSet.getString(1);

                if (dataCenterType.equalsIgnoreCase("basic")) {
                    accountId = 1L;
                }

                pstmt = conn.prepareStatement("SELECT id from vm_instance where account_id=? AND data_center_id=? AND type='DomainRouter'");
                pstmt.setLong(1, accountId);
                pstmt.setLong(2, dataCenterId);
                ResultSet rs1 = pstmt.executeQuery();

                Long deviceId = 0L;
                if (!rs1.next()) {
                    // check if there are any non-removed user vms existing for this account
                    // if all vms are expunged, and there is no domR, just skip this record
                    pstmt = conn.prepareStatement("SELECT * from vm_instance where account_id=? AND data_center_id=? AND removed IS NULL");
                    pstmt.setLong(1, accountId);
                    pstmt.setLong(2, dataCenterId);
                    ResultSet nonRemovedVms = pstmt.executeQuery();

                    if (nonRemovedVms.next()) {
                        s_logger.warn("Failed to find domR for for account id=" + accountId + " in zone id=" + dataCenterId + "; will try to locate domR based on user_vm info");
                        //try to get domR information from the user_vm belonging to the account
                        pstmt = conn.prepareStatement("SELECT u.domain_router_id from user_vm u, vm_instance v where u.account_id=? AND v.data_center_id=? AND v.removed IS NULL AND u.domain_router_id is NOT NULL");
                        pstmt.setLong(1, accountId);
                        pstmt.setLong(2, dataCenterId);
                        ResultSet userVmSet = pstmt.executeQuery();
                        if (!userVmSet.next()) {
                            s_logger.warn("Skipping user_statistics upgrade for account id=" + accountId + " in datacenter id=" + dataCenterId);
                            continue;
                        } 
                        deviceId = userVmSet.getLong(1);
                    } else {
                        s_logger.debug("Account id=" + accountId + " doesn't own any user vms and domRs, so skipping user_statistics update");
                        continue;
                    }
                } else {
                    deviceId = rs1.getLong(1);
                }

                pstmt = conn.prepareStatement("UPDATE user_statistics SET device_id=? where id=?");
                pstmt.setLong(1, deviceId);
                pstmt.setLong(2, id);
                pstmt.executeUpdate();

                pstmt = conn.prepareStatement("");

            }
            s_logger.debug("Upgraded userStatistcis with deviceId(s)");

        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to migrate usage events: ", e);
        }
    }

    public void upgradePortForwardingRules(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id, public_ip_address, public_port, private_ip_address, private_port, protocol FROM ip_forwarding WHERE forwarding=1");
            ResultSet rs = pstmt.executeQuery();
            ArrayList<Object[]> rules = new ArrayList<Object[]>();
            while (rs.next()) {
                Object[] rule = new Object[10];
                rule[0] = rs.getLong(1); // rule id
                rule[1] = rs.getString(2); // rule public IP
                rule[2] = rs.getString(3); // rule public port
                rule[3] = rs.getString(4); // rule private Ip
                rule[4] = rs.getString(5); // rule private port
                rule[5] = rs.getString(6); // rule protocol
                rules.add(rule);
            }
            rs.close();
            pstmt.close();

            if (!rules.isEmpty()) {
                s_logger.debug("Found " + rules.size() + " port forwarding rules to upgrade");
                for (Object[] rule : rules) {
                    long id = (Long) rule[0];
                    String sourcePort = (String) rule[2];
                    String protocol = (String) rule[5];
                    String publicIp = (String) rule[1];

                    pstmt = conn.prepareStatement("SELECT id, account_id, domain_id, network_id FROM user_ip_address WHERE public_ip_address=?");
                    pstmt.setString(1, publicIp);
                    rs = pstmt.executeQuery();

                    if (!rs.next()) {
                        s_logger.error("Unable to find public IP address " + publicIp);
                        throw new CloudRuntimeException("Unable to find public IP address " + publicIp);
                    }

                    int ipAddressId = rs.getInt(1);
                    long accountId = rs.getLong(2);
                    long domainId = rs.getLong(3);
                    long networkId = rs.getLong(4);
                    String privateIp = (String) rule[3];

                    rs.close();
                    pstmt.close();

                    // update port_forwarding_rules table
                    s_logger.trace("Updating port_forwarding_rules table...");
                    pstmt = conn.prepareStatement("SELECT instance_id FROM nics where network_id=? AND ip4_address=?");
                    pstmt.setLong(1, networkId);
                    pstmt.setString(2, privateIp);
                    rs = pstmt.executeQuery();

                    if (!rs.next()) {
                        // the vm might be expunged already...so just give the warning
                        s_logger.warn("Unable to find vmId for private ip address " + privateIp + " for account id=" + accountId + "; assume that the vm is expunged");
                        // throw new CloudRuntimeException("Unable to find vmId for private ip address " + privateIp +
                        // " for account id=" + accountId);
                    } else {
                        long instanceId = rs.getLong(1);
                        s_logger.debug("Instance id is " + instanceId);
                        // update firewall_rules table
                        s_logger.trace("Updating firewall_rules table as a part of PF rules upgrade...");
                        pstmt = conn
                                .prepareStatement("INSERT INTO firewall_rules (id, ip_address_id, start_port, end_port, state, protocol, purpose, account_id, domain_id, network_id, xid, is_static_nat, created) VALUES (?,    ?,      ?,      ?,      'Active',        ?,     'PortForwarding',       ?,      ?,      ?,      ?,       0,     now())");
                        pstmt.setLong(1, id);
                        pstmt.setInt(2, ipAddressId);
                        pstmt.setInt(3, Integer.valueOf(sourcePort.trim()));
                        pstmt.setInt(4, Integer.valueOf(sourcePort.trim()));
                        pstmt.setString(5, protocol);
                        pstmt.setLong(6, accountId);
                        pstmt.setLong(7, domainId);
                        pstmt.setLong(8, networkId);
                        pstmt.setString(9, UUID.randomUUID().toString());
                        pstmt.executeUpdate();
                        pstmt.close();
                        s_logger.trace("firewall_rules table is updated as a part of PF rules upgrade");

                        rs.close();
                        pstmt.close();

                        String privatePort = (String) rule[4];
                        pstmt = conn.prepareStatement("INSERT INTO port_forwarding_rules VALUES (?,    ?,      ?,      ?,       ?)");
                        pstmt.setLong(1, id);
                        pstmt.setLong(2, instanceId);
                        pstmt.setString(3, privateIp);
                        pstmt.setInt(4, Integer.valueOf(privatePort.trim()));
                        pstmt.setInt(5, Integer.valueOf(privatePort.trim()));
                        pstmt.executeUpdate();
                        pstmt.close();
                        s_logger.trace("port_forwarding_rules table is updated");

                    }

                }
            }
            s_logger.debug("Port forwarding rules are updated");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't update port forwarding rules ", e);
        }
    }

    public void upgradeLoadBalancingRules(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT name, ip_address, public_port, private_port, algorithm, id FROM load_balancer");
            ResultSet rs = pstmt.executeQuery();
            ArrayList<Object[]> lbs = new ArrayList<Object[]>();
            while (rs.next()) {
                Object[] lb = new Object[10];
                lb[0] = rs.getString(1); // lb name
                lb[1] = rs.getString(2); // lb public IP
                lb[2] = rs.getString(3); // lb public port
                lb[3] = rs.getString(4); // lb private port
                lb[4] = rs.getString(5); // lb algorithm
                lb[5] = rs.getLong(6); // lb Id
                lbs.add(lb);
            }
            rs.close();
            pstmt.close();

            if (!lbs.isEmpty()) {
                s_logger.debug("Found " + lbs.size() + " lb rules to upgrade");
                pstmt = conn.prepareStatement("SELECT id FROM firewall_rules order by id");
                rs = pstmt.executeQuery();
                long newLbId = 0;
                while (rs.next()) {
                    newLbId = rs.getLong(1);
                }
                rs.close();
                pstmt.close();

                for (Object[] lb : lbs) {
                    String name = (String) lb[0];
                    String publicIp = (String) lb[1];
                    String sourcePort = (String) lb[2];
                    String destPort = (String) lb[3];
                    String algorithm = (String) lb[4];
                    Long originalLbId = (Long) lb[5];
                    newLbId = newLbId + 1;

                    pstmt = conn.prepareStatement("SELECT id, account_id, domain_id, network_id FROM user_ip_address WHERE public_ip_address=?");
                    pstmt.setString(1, publicIp);
                    rs = pstmt.executeQuery();

                    if (!rs.next()) {
                        s_logger.warn("Unable to find public IP address " + publicIp + "; skipping lb rule id=" + originalLbId
                                + " from update. Cleaning it up from load_balancer_vm_map and load_balancer table");
                        pstmt = conn.prepareStatement("DELETE from load_balancer_vm_map where load_balancer_id=?");
                        pstmt.setLong(1, originalLbId);
                        pstmt.executeUpdate();

                        pstmt = conn.prepareStatement("DELETE from load_balancer where id=?");
                        pstmt.setLong(1, originalLbId);
                        pstmt.executeUpdate();

                        continue;
                    }

                    int ipAddressId = rs.getInt(1);
                    long accountId = rs.getLong(2);
                    long domainId = rs.getLong(3);
                    long networkId = rs.getLong(4);

                    rs.close();
                    pstmt.close();

                    // update firewall_rules table
                    s_logger.trace("Updating firewall_rules table as a part of LB rules upgrade...");
                    pstmt = conn
                            .prepareStatement("INSERT INTO firewall_rules (id, ip_address_id, start_port, end_port, state, protocol, purpose, account_id, domain_id, network_id, xid, is_static_nat, created) VALUES (?,    ?,      ?,      ?,      'Active',        ?,     'LoadBalancing',       ?,      ?,      ?,      ?,       0,       now())");
                    pstmt.setLong(1, newLbId);
                    pstmt.setInt(2, ipAddressId);
                    pstmt.setInt(3, Integer.valueOf(sourcePort));
                    pstmt.setInt(4, Integer.valueOf(sourcePort));
                    pstmt.setString(5, "tcp");
                    pstmt.setLong(6, accountId);
                    pstmt.setLong(7, domainId);
                    pstmt.setLong(8, networkId);
                    pstmt.setString(9, UUID.randomUUID().toString());
                    pstmt.executeUpdate();
                    pstmt.close();
                    s_logger.trace("firewall_rules table is updated as a part of LB rules upgrade");

                    // update load_balancing_rules
                    s_logger.trace("Updating load_balancing_rules table as a part of LB rules upgrade...");
                    pstmt = conn.prepareStatement("INSERT INTO load_balancing_rules VALUES (?,      ?,      NULL,      ?,       ?,      ?)");
                    pstmt.setLong(1, newLbId);
                    pstmt.setString(2, name);
                    pstmt.setInt(3, Integer.valueOf(destPort));
                    pstmt.setInt(4, Integer.valueOf(destPort));
                    pstmt.setString(5, algorithm);
                    pstmt.executeUpdate();
                    pstmt.close();
                    s_logger.trace("load_balancing_rules table is updated as a part of LB rules upgrade");

                    // update load_balancer_vm_map table
                    s_logger.trace("Updating load_balancer_vm_map table as a part of LB rules upgrade...");
                    pstmt = conn.prepareStatement("SELECT instance_id FROM load_balancer_vm_map WHERE load_balancer_id=?");
                    pstmt.setLong(1, originalLbId);
                    rs = pstmt.executeQuery();
                    ArrayList<Object[]> lbMaps = new ArrayList<Object[]>();
                    while (rs.next()) {
                        Object[] lbMap = new Object[10];
                        lbMap[0] = rs.getLong(1); // instanceId
                        lbMaps.add(lbMap);
                    }
                    rs.close();
                    pstmt.close();

                    pstmt = conn.prepareStatement("UPDATE load_balancer_vm_map SET load_balancer_id=? WHERE load_balancer_id=?");
                    pstmt.setLong(1, newLbId);
                    pstmt.setLong(2, originalLbId);
                    pstmt.executeUpdate();
                    pstmt.close();

                    s_logger.trace("load_balancer_vm_map table is updated as a part of LB rules upgrade");
                }
            }
            s_logger.debug("LB rules are upgraded");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't update LB rules ", e);
        }
    }

    private void upgradeHostMemoryCapacityInfo(Connection conn) {
        try {
            // count user_vm memory info (M Bytes)
            PreparedStatement pstmt = conn
                    .prepareStatement("select h.id, sum(s.ram_size) from host h, vm_instance v, service_offering s where h.type='Routing' and v.state='Running' and v.`type`='User' and v.host_id=h.id  and v.service_offering_id = s.id group by h.id");

            ResultSet rs = pstmt.executeQuery();
            Map<Long, Long> hostUsedMemoryInfo = new HashMap<Long, Long>();
            while (rs.next()) {
                hostUsedMemoryInfo.put(rs.getLong(1), rs.getLong(2));
            }
            rs.close();
            pstmt.close();

            int proxyRamSize = NumbersUtil.parseInt(getConfigValue(conn, "consoleproxy.ram.size"), ConsoleProxyManager.DEFAULT_PROXY_VM_RAMSIZE);
            int domrRamSize = NumbersUtil.parseInt(getConfigValue(conn, "router.ram.size"), VirtualNetworkApplianceManager.DEFAULT_ROUTER_VM_RAMSIZE);
            int ssvmRamSize = NumbersUtil.parseInt(getConfigValue(conn, "secstorage.vm.ram.size"), SecondaryStorageVmManager.DEFAULT_SS_VM_RAMSIZE);

            pstmt = conn
                    .prepareStatement("select h.id, count(v.id) from host h, vm_instance v where h.type='Routing' and v.state='Running' and v.`type`='ConsoleProxy' and v.host_id=h.id group by h.id");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                if (hostUsedMemoryInfo.get(rs.getLong(1)) != null) {
                    Long usedMem = hostUsedMemoryInfo.get(rs.getLong(1));
                    hostUsedMemoryInfo.put(rs.getLong(1), rs.getLong(2) * proxyRamSize + usedMem);
                } else {
                    hostUsedMemoryInfo.put(rs.getLong(1), rs.getLong(2) * proxyRamSize);
                }
            }
            rs.close();
            pstmt.close();

            pstmt = conn
                    .prepareStatement("select h.id, count(v.id) from host h, vm_instance v where h.type='Routing' and v.state='Running' and v.`type`='DomainRouter' and v.host_id=h.id group by h.id");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                if (hostUsedMemoryInfo.get(rs.getLong(1)) != null) {
                    Long usedMem = hostUsedMemoryInfo.get(rs.getLong(1));
                    hostUsedMemoryInfo.put(rs.getLong(1), rs.getLong(2) * domrRamSize + usedMem);
                } else {
                    hostUsedMemoryInfo.put(rs.getLong(1), rs.getLong(2) * domrRamSize);
                }
            }
            rs.close();
            pstmt.close();

            pstmt = conn
                    .prepareStatement("select h.id, count(v.id) from host h, vm_instance v where h.type='Routing' and v.state='Running' and v.`type`='SecondaryStorageVm' and v.host_id=h.id group by h.id");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                if (hostUsedMemoryInfo.get(rs.getLong(1)) != null) {
                    Long usedMem = hostUsedMemoryInfo.get(rs.getLong(1));
                    hostUsedMemoryInfo.put(rs.getLong(1), rs.getLong(2) * ssvmRamSize + usedMem);
                } else {
                    hostUsedMemoryInfo.put(rs.getLong(1), rs.getLong(2) * ssvmRamSize);
                }
            }
            rs.close();
            pstmt.close();

            for (Map.Entry<Long, Long> entry : hostUsedMemoryInfo.entrySet()) {
                pstmt = conn.prepareStatement("update op_host_capacity set used_capacity=? where host_id=? and capacity_type=0");
                pstmt.setLong(1, entry.getValue() * 1024 * 1024);
                pstmt.setLong(2, entry.getKey());

                pstmt.executeUpdate();
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't upgrade host capacity info ", e);
        }
    }

    // per domain resource counts is introduced from 2.2 so we need to evaluate the domain limits
    // from the resource counts of account, and account-domain relation for all resource types
    private void upgradeDomainResourceCounts(Connection conn) {
        upgradeDomainResourceCounts(conn, ResourceType.volume);
        upgradeDomainResourceCounts(conn, ResourceType.user_vm);
        upgradeDomainResourceCounts(conn, ResourceType.snapshot);
        upgradeDomainResourceCounts(conn, ResourceType.template);
        upgradeDomainResourceCounts(conn, ResourceType.public_ip);
    }

    public static void upgradeDomainResourceCounts(Connection conn, ResourceType resourceType) {
        try {

            PreparedStatement account_count_pstmt = conn.prepareStatement("SELECT account_id, count from resource_count where type='" + resourceType + "'");
            ResultSet rs_account_count = account_count_pstmt.executeQuery();

            while (rs_account_count.next()) {
                Long accountId = rs_account_count.getLong(1);
                Long accountCount = rs_account_count.getLong(2);

                PreparedStatement account_pstmt = conn.prepareStatement("SELECT domain_id from account where id=?");
                account_pstmt.setLong(1, accountId);
                ResultSet rs_domain = account_pstmt.executeQuery();

                if (!rs_domain.next()) {
                    throw new CloudRuntimeException("Unable to get the domain for the account Id: " + accountId);
                }
                Long domainId = rs_domain.getLong(1);

                rs_domain.close();
                account_pstmt.close();

                // resource count on a domain is aggregate of resource count of all the accounts that belong to the domain and
                // its sub-domains.
                // so propagate the count across the domain hierarchy all the way up to the root domain.
                while (domainId != 0) {

                    PreparedStatement domain_count_pstmt = conn.prepareStatement("SELECT count from resource_count where type='" + resourceType + "' and domain_id=?");
                    domain_count_pstmt.setLong(1, domainId);
                    ResultSet rs_domain_count = domain_count_pstmt.executeQuery();

                    // if a row has been created for the domain in the resource_count table, add the count to the existing
                    // domain count
                    if (rs_domain_count.next()) {
                        Long domainCount = rs_domain_count.getLong(1);
                        domainCount = domainCount + accountCount;
                        PreparedStatement update_domain_count_pstmt = conn.prepareStatement("UPDATE resource_count set count=? where domain_id=? and type ='" + resourceType + "'");
                        update_domain_count_pstmt.setLong(1, domainCount);
                        update_domain_count_pstmt.setLong(2, domainId);
                        update_domain_count_pstmt.executeUpdate();
                        update_domain_count_pstmt.close();
                    } else {
                        PreparedStatement update_domain_count_pstmt = conn.prepareStatement("INSERT INTO resource_count (type, count, domain_id) VALUES (?,?,?)");
                        update_domain_count_pstmt.setString(1, resourceType.getName());
                        update_domain_count_pstmt.setLong(2, accountCount);
                        update_domain_count_pstmt.setLong(3, domainId);
                        update_domain_count_pstmt.executeUpdate();
                        update_domain_count_pstmt.close();
                    }

                    rs_domain_count.close();
                    domain_count_pstmt.close();

                    PreparedStatement parentDomain_pstmt = conn.prepareStatement("SELECT parent from domain where id=?");
                    parentDomain_pstmt.setLong(1, domainId);
                    ResultSet rs_domain_parent = parentDomain_pstmt.executeQuery();

                    if (rs_domain_parent.next()) {
                        domainId = rs_domain_parent.getLong(1);
                    } else {
                        throw new CloudRuntimeException("Unable to get the parent domain for the domain Id: " + domainId);
                    }

                    rs_domain_parent.close();
                    parentDomain_pstmt.close();
                }
            }

            rs_account_count.close();
            account_count_pstmt.close();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't upgrade domain resource counts ", e);
        }
    }

    private void upgradeHostCpuCapacityInfo(Connection conn) {
        try {
            // count user_vm memory info (M Bytes)
            PreparedStatement pstmt = conn
                    .prepareStatement("select h.id, sum(s.speed*s.cpu) from host h, vm_instance v, service_offering s where h.type='Routing' and v.state='Running' and v.`type`='User' and v.host_id=h.id  and v.service_offering_id = s.id group by h.id");

            ResultSet rs = pstmt.executeQuery();
            Map<Long, Long> hostUsedCpuInfo = new HashMap<Long, Long>();
            while (rs.next()) {
                hostUsedCpuInfo.put(rs.getLong(1), rs.getLong(2));
            }
            rs.close();
            pstmt.close();

            int proxyCpuMhz = NumbersUtil.parseInt(getConfigValue(conn, "consoleproxy.cpu.mhz"), ConsoleProxyManager.DEFAULT_PROXY_VM_CPUMHZ);
            int domrCpuMhz = NumbersUtil.parseInt(getConfigValue(conn, "router.cpu.mhz"), VirtualNetworkApplianceManager.DEFAULT_ROUTER_CPU_MHZ);
            int ssvmCpuMhz = NumbersUtil.parseInt(getConfigValue(conn, "secstorage.vm.cpu.mhz"), SecondaryStorageVmManager.DEFAULT_SS_VM_CPUMHZ);

            pstmt = conn
                    .prepareStatement("select h.id, count(v.id) from host h, vm_instance v where h.type='Routing' and v.state='Running' and v.`type`='ConsoleProxy' and v.host_id=h.id group by h.id");

            rs = pstmt.executeQuery();
            while (rs.next()) {
                if (hostUsedCpuInfo.get(rs.getLong(1)) != null) {
                    Long usedCpuMhz = hostUsedCpuInfo.get(rs.getLong(1));
                    hostUsedCpuInfo.put(rs.getLong(1), rs.getLong(2) * proxyCpuMhz + usedCpuMhz);
                } else {
                    hostUsedCpuInfo.put(rs.getLong(1), rs.getLong(2) * proxyCpuMhz);
                }
            }
            rs.close();
            pstmt.close();

            pstmt = conn
                    .prepareStatement("select h.id, count(v.id) from host h, vm_instance v where h.type='Routing' and v.state='Running' and v.`type`='DomainRouter' and v.host_id=h.id group by h.id");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                if (hostUsedCpuInfo.get(rs.getLong(1)) != null) {
                    Long usedCpuMhz = hostUsedCpuInfo.get(rs.getLong(1));
                    hostUsedCpuInfo.put(rs.getLong(1), rs.getLong(2) * domrCpuMhz + usedCpuMhz);
                } else {
                    hostUsedCpuInfo.put(rs.getLong(1), rs.getLong(2) * domrCpuMhz);
                }
            }
            rs.close();
            pstmt.close();

            pstmt = conn
                    .prepareStatement("select h.id, count(v.id) from host h, vm_instance v where h.type='Routing' and v.state='Running' and v.`type`='SecondaryStorageVm' and v.host_id=h.id group by h.id");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                if (hostUsedCpuInfo.get(rs.getLong(1)) != null) {
                    Long usedCpuMhz = hostUsedCpuInfo.get(rs.getLong(1));
                    hostUsedCpuInfo.put(rs.getLong(1), rs.getLong(2) * ssvmCpuMhz + usedCpuMhz);
                } else {
                    hostUsedCpuInfo.put(rs.getLong(1), rs.getLong(2) * ssvmCpuMhz);
                }
            }
            rs.close();
            pstmt.close();

            for (Map.Entry<Long, Long> entry : hostUsedCpuInfo.entrySet()) {
                pstmt = conn.prepareStatement("update op_host_capacity set used_capacity=? where host_id=? and capacity_type=1");
                pstmt.setLong(1, entry.getValue());
                pstmt.setLong(2, entry.getKey());

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't upgrade host capacity info ", e);
        }
    }

    private String getConfigValue(Connection conn, String name) {
        try {
            // count user_vm memory info (M Bytes)
            PreparedStatement pstmt = conn.prepareStatement("select value from configuration where name=?");
            pstmt.setString(1, name);
            ResultSet rs = pstmt.executeQuery();

            String val = null;
            if (rs.next()) {
                val = rs.getString(1);
            }
            rs.close();
            pstmt.close();

            return val;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Can't upgrade host capacity info ", e);
        }
    }

    private void migrateEvents(Connection conn) {
        try {
            PreparedStatement pstmt1 = conn.prepareStatement("SHOW DATABASES LIKE 'cloud_usage'");
            ResultSet rs1 = pstmt1.executeQuery();
            if (!rs1.next()) {
                s_logger.debug("cloud_usage db doesn't exist. Skipping events migration");
                return;
            }

            // get last processed event Id
            Long lastProcessedEvent = getMostRecentEvent(conn);
            // Events not yet processed
            String sql = "SELECT type, description, user_id, account_id, created, level, parameters FROM cloud.event vmevt WHERE vmevt.id > ? and vmevt.state = 'Completed' ";
            if (lastProcessedEvent == null) {
                s_logger.trace("no events are processed earlier, copying all events");
                sql = "SELECT type, description, user_id, account_id, created, level, parameters FROM cloud.event vmevt WHERE vmevt.state = 'Completed' ";
            }

            PreparedStatement pstmt = null;

            pstmt = conn.prepareStatement(sql);
            int i = 1;
            if (lastProcessedEvent != null) {
                pstmt.setLong(i++, lastProcessedEvent);
            }
            ResultSet rs = pstmt.executeQuery();
            s_logger.debug("Begin Migrating events");
            while (rs.next()) {
                EventVO event = new EventVO();
                event.setType(rs.getString(1));
                event.setDescription(rs.getString(2));
                event.setUserId(rs.getLong(3));
                event.setAccountId(rs.getLong(4));
                event.setCreatedDate(DateUtil.parseDateString(TimeZone.getTimeZone("GMT"), rs.getString(5)));
                event.setLevel(rs.getString(6));
                event.setParameters(rs.getString(7));
                convertEvent(event, conn);
            }
            s_logger.debug("Migrating events completed");
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to migrate usage events: ", e);
        }
    }

    private Long getMostRecentEvent(Connection conn) {
        PreparedStatement pstmt = null;
        String sql = "SELECT id FROM cloud_usage.event ORDER BY created DESC LIMIT 1";
        try {
            pstmt = conn.prepareStatement(sql);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (Exception ex) {
            throw new CloudRuntimeException("error getting most recent event date: " + ex.getMessage());
        }
        return null;
    }

    private void convertEvent(EventVO event, Connection conn) throws IOException, SQLException {
        // we only create usage for success cases as error cases mean
        // the event didn't happen, so it couldn't result in usage
        if (!EventVO.LEVEL_INFO.equals(event.getLevel())) {
            return;
        }
        String eventType = event.getType();
        UsageEventVO usageEvent = null;
        if (isVMEvent(eventType)) {
            usageEvent = convertVMEvent(event);
        } else if (isIPEvent(eventType)) {
            usageEvent = convertIPEvent(event, conn);
        } else if (isVolumeEvent(eventType)) {
            usageEvent = convertVolumeEvent(event, conn);
        } else if (isTemplateEvent(eventType)) {
            usageEvent = convertTemplateEvent(event);
        } else if (isISOEvent(eventType)) {
            usageEvent = convertISOEvent(event);
        } else if (isSnapshotEvent(eventType)) {
            usageEvent = convertSnapshotEvent(event, conn);
        } /*
           * else if (isSecurityGrpEvent(eventType)) { usageEvent = convertSecurityGrpEvent(event); } else if
           * (isLoadBalancerEvent(eventType)) { usageEvent = convertLoadBalancerEvent(event); }
           */
        if (usageEvent != null) {
            usageEvent.setCreatedDate(event.getCreateDate());
            if (usageEvent.getZoneId() == -1) {
                usageEvent.setZoneId(0);
            }
            // update firewall_rules table
            PreparedStatement pstmt = null;
            pstmt = conn
                    .prepareStatement("INSERT INTO usage_event (usage_event.type, usage_event.created, usage_event.account_id, usage_event.zone_id, usage_event.resource_id, usage_event.resource_name,"
                            + " usage_event.offering_id, usage_event.template_id, usage_event.size) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
            pstmt.setString(1, usageEvent.getType());
            pstmt.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), usageEvent.getCreateDate()));
            pstmt.setLong(3, usageEvent.getAccountId());
            pstmt.setLong(4, usageEvent.getZoneId());
            pstmt.setLong(5, usageEvent.getResourceId());
            pstmt.setString(6, usageEvent.getResourceName());
            if (usageEvent.getOfferingId() != null) {
                pstmt.setLong(7, usageEvent.getOfferingId());
            } else {
                pstmt.setNull(7, Types.BIGINT);
            }
            if (usageEvent.getTemplateId() != null) {
                pstmt.setLong(8, usageEvent.getTemplateId());
            } else {
                pstmt.setNull(8, Types.BIGINT);
            }
            if (usageEvent.getSize() != null) {
                pstmt.setLong(9, usageEvent.getSize());
            } else {
                pstmt.setNull(9, Types.BIGINT);
            }
            // pstmt.setString(10, usageEvent.getResourceType());
            pstmt.executeUpdate();
            pstmt.close();
        }
    }

    private boolean isVMEvent(String eventType) {
        if (eventType == null) {
            return false;
        }
        return eventType.startsWith("VM.");
    }

    private boolean isIPEvent(String eventType) {
        if (eventType == null) {
            return false;
        }
        return eventType.startsWith("NET.IP");
    }

    private boolean isVolumeEvent(String eventType) {
        if (eventType == null) {
            return false;
        }
        return (eventType.equals(EventTypes.EVENT_VOLUME_CREATE) || eventType.equals(EventTypes.EVENT_VOLUME_DELETE));
    }

    private boolean isTemplateEvent(String eventType) {
        if (eventType == null) {
            return false;
        }
        return (eventType.equals(EventTypes.EVENT_TEMPLATE_CREATE) || eventType.equals(EventTypes.EVENT_TEMPLATE_COPY) || eventType.equals(EventTypes.EVENT_TEMPLATE_DELETE));
    }

    private boolean isISOEvent(String eventType) {
        if (eventType == null) {
            return false;
        }
        return (eventType.equals(EventTypes.EVENT_ISO_CREATE) || eventType.equals(EventTypes.EVENT_ISO_COPY) || eventType.equals(EventTypes.EVENT_ISO_DELETE));
    }

    private boolean isSnapshotEvent(String eventType) {
        if (eventType == null) {
            return false;
        }
        return (eventType.equals(EventTypes.EVENT_SNAPSHOT_CREATE) || eventType.equals(EventTypes.EVENT_SNAPSHOT_DELETE));
    }

    private boolean isLoadBalancerEvent(String eventType) {
        if (eventType == null) {
            return false;
        }
        return eventType.startsWith("LB.");
    }

    private UsageEventVO convertVMEvent(EventVO event) throws IOException {

        Properties vmEventParams = new Properties();
        UsageEventVO usageEvent = null;
        long vmId = -1L;
        long soId = -1L; // service offering id
        long zoneId = -1L;
        String eventParams = event.getParameters();
        if (eventParams != null) {
            vmEventParams.load(new StringReader(eventParams));
            vmId = Long.parseLong(vmEventParams.getProperty("id"));
            soId = Long.parseLong(vmEventParams.getProperty("soId"));
            zoneId = Long.parseLong(vmEventParams.getProperty("dcId"));
        }

        if (EventTypes.EVENT_VM_START.equals(event.getType())) {
            long templateId = 0;
            String tId = vmEventParams.getProperty("tId");
            if (tId != null) {
                templateId = Long.parseLong(tId);
            }

            usageEvent = new UsageEventVO(EventTypes.EVENT_VM_START, event.getAccountId(), zoneId, vmId, vmEventParams.getProperty("vmName"), soId, templateId, "");
        } else if (EventTypes.EVENT_VM_STOP.equals(event.getType())) {
            usageEvent = new UsageEventVO(EventTypes.EVENT_VM_STOP, event.getAccountId(), zoneId, vmId, vmEventParams.getProperty("vmName"));
        } else if (EventTypes.EVENT_VM_CREATE.equals(event.getType())) {
            Long templateId = null;
            String tId = vmEventParams.getProperty("tId");
            if (tId != null) {
                templateId = new Long(Long.parseLong(tId));
            }

            usageEvent = new UsageEventVO(EventTypes.EVENT_VM_CREATE, event.getAccountId(), zoneId, vmId, vmEventParams.getProperty("vmName"), soId, templateId, "");
        } else if (EventTypes.EVENT_VM_DESTROY.equals(event.getType())) {
            usageEvent = new UsageEventVO(EventTypes.EVENT_VM_DESTROY, event.getAccountId(), zoneId, vmId, vmEventParams.getProperty("vmName"));
        }
        return usageEvent;
    }

    private UsageEventVO convertIPEvent(EventVO event, Connection conn) throws IOException, SQLException {

        Properties ipEventParams = new Properties();
        UsageEventVO usageEvent = null;
        ipEventParams.load(new StringReader(event.getParameters()));
        String ipAddress = ipEventParams.getProperty("address");
        if (ipAddress == null) {
            ipAddress = ipEventParams.getProperty("guestIPaddress");
            if (ipAddress == null) {
                // can not find IP address, bail for this event
                return null;
            }
        }

        // Get ip address information
        Long ipId = 0L;
        Long zoneId = 0L;
        PreparedStatement pstmt = conn.prepareStatement("SELECT id, data_center_id from user_ip_address where public_ip_address=?");
        pstmt.setString(1, ipAddress);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            ipId = rs.getLong(1);
            zoneId = rs.getLong(2);
        }
        rs.close();
        pstmt.close();

        boolean isSourceNat = Boolean.parseBoolean(ipEventParams.getProperty("sourceNat"));

        if (EventTypes.EVENT_NET_IP_ASSIGN.equals(event.getType())) {
            zoneId = Long.parseLong(ipEventParams.getProperty("dcId"));
            usageEvent = new UsageEventVO(EventTypes.EVENT_NET_IP_ASSIGN, event.getAccountId(), zoneId, ipId, ipAddress, isSourceNat,"", false);
        } else if (EventTypes.EVENT_NET_IP_RELEASE.equals(event.getType())) {
            usageEvent = new UsageEventVO(EventTypes.EVENT_NET_IP_RELEASE, event.getAccountId(), zoneId, ipId, ipAddress, isSourceNat,"", false);
        }
        return usageEvent;
    }

    private UsageEventVO convertVolumeEvent(EventVO event, Connection conn) throws IOException, SQLException {

        Properties volEventParams = new Properties();
        long volId = -1L;
        Long doId = -1L;
        long zoneId = -1L;
        Long templateId = -1L;
        long size = -1L;
        UsageEventVO usageEvent = null;
        volEventParams.load(new StringReader(event.getParameters()));
        volId = Long.parseLong(volEventParams.getProperty("id"));
        if (EventTypes.EVENT_VOLUME_CREATE.equals(event.getType())) {
            doId = Long.parseLong(volEventParams.getProperty("doId"));
            zoneId = Long.parseLong(volEventParams.getProperty("dcId"));
            templateId = Long.parseLong(volEventParams.getProperty("tId"));
            size = Long.parseLong(volEventParams.getProperty("size"));
            size = (size * 1048576);
            if (doId == -1) {
                doId = null;
            }
            if (templateId == -1) {
                templateId = null;
            }
        }

        // Get volume name information
        String volumeName = "";
        PreparedStatement pstmt = conn.prepareStatement("SELECT name, data_center_id from volumes where id=?");
        pstmt.setLong(1, volId);
        ResultSet rs = pstmt.executeQuery();
        if (rs.next()) {
            volumeName = rs.getString(1);
            zoneId = rs.getLong(2);
        }
        rs.close();
        pstmt.close();

        if (EventTypes.EVENT_VOLUME_CREATE.equals(event.getType())) {
            usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_CREATE, event.getAccountId(), zoneId, volId, volumeName, doId, templateId, size);
        } else if (EventTypes.EVENT_VOLUME_DELETE.equals(event.getType())) {
            usageEvent = new UsageEventVO(EventTypes.EVENT_VOLUME_DELETE, event.getAccountId(), zoneId, volId, volumeName);

        }
        return usageEvent;
    }

    private UsageEventVO convertTemplateEvent(EventVO event) throws IOException {

        Properties templateEventParams = new Properties();
        long templateId = -1L;
        long zoneId = -1L;
        long templateSize = -1L;
        UsageEventVO usageEvent = null;

        templateEventParams.load(new StringReader(event.getParameters()));
        templateId = Long.parseLong(templateEventParams.getProperty("id"));
        if (templateEventParams.getProperty("dcId") != null) {
            zoneId = Long.parseLong(templateEventParams.getProperty("dcId"));
        }
        if (EventTypes.EVENT_TEMPLATE_CREATE.equals(event.getType()) || EventTypes.EVENT_TEMPLATE_COPY.equals(event.getType())) {
            templateSize = Long.parseLong(templateEventParams.getProperty("size"));
            if (templateSize < 1) {
                return null;
            }
            if (zoneId == -1L) {
                return null;
            }
            usageEvent = new UsageEventVO(event.getType(), event.getAccountId(), zoneId, templateId, "", null, null, templateSize);
        } else if (EventTypes.EVENT_TEMPLATE_DELETE.equals(event.getType())) {
            usageEvent = new UsageEventVO(event.getType(), event.getAccountId(), zoneId, templateId, null);
        }
        return usageEvent;
    }

    private UsageEventVO convertISOEvent(EventVO event) throws IOException {
        Properties isoEventParams = new Properties();
        long isoId = -1L;
        long isoSize = -1L;
        long zoneId = -1L;
        UsageEventVO usageEvent = null;

        isoEventParams.load(new StringReader(event.getParameters()));
        isoId = Long.parseLong(isoEventParams.getProperty("id"));
        if (isoEventParams.getProperty("dcId") != null) {
            zoneId = Long.parseLong(isoEventParams.getProperty("dcId"));
        }

        if (EventTypes.EVENT_ISO_CREATE.equals(event.getType()) || EventTypes.EVENT_ISO_COPY.equals(event.getType())) {
            isoSize = Long.parseLong(isoEventParams.getProperty("size"));
            usageEvent = new UsageEventVO(event.getType(), event.getAccountId(), zoneId, isoId, "", null, null, isoSize);
        } else if (EventTypes.EVENT_ISO_DELETE.equals(event.getType())) {
            usageEvent = new UsageEventVO(event.getType(), event.getAccountId(), zoneId, isoId, null);
        }
        return usageEvent;
    }

    private UsageEventVO convertSnapshotEvent(EventVO event, Connection conn) throws IOException, SQLException {
        Properties snapEventParams = new Properties();
        long snapId = -1L;
        long snapSize = -1L;
        Long zoneId = 0L;
        UsageEventVO usageEvent = null;

        snapEventParams.load(new StringReader(event.getParameters()));
        snapId = Long.parseLong(snapEventParams.getProperty("id"));
        String snapshotName = snapEventParams.getProperty("ssName");

        String size = snapEventParams.getProperty("size");
        if (size != null) {
            snapSize = Long.parseLong(size);
        }

        String zoneString = snapEventParams.getProperty("dcId");
        if (zoneString != null) {
            zoneId = Long.parseLong(zoneString);
        }

        Long accountId = event.getAccountId();

        // Get snapshot info (there was a bug in 2.1.x - accountId is 0, and data_center info is not present in events table
        if (accountId.longValue() == 0L || zoneId.longValue() == 0L) {
            PreparedStatement pstmt = conn.prepareStatement("SELECT zone_id, account_id from usage_event where resource_id=? and type like '%SNAPSHOT%'");
            pstmt.setLong(1, snapId);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                zoneId = rs.getLong(1);
                accountId = rs.getLong(2);
            }

            rs.close();
            pstmt.close();
        }

        if (EventTypes.EVENT_SNAPSHOT_CREATE.equals(event.getType())) {
            usageEvent = new UsageEventVO(EventTypes.EVENT_SNAPSHOT_CREATE, accountId, zoneId, snapId, snapshotName, null, null, snapSize);
        } else if (EventTypes.EVENT_SNAPSHOT_DELETE.equals(event.getType())) {
            usageEvent = new UsageEventVO(EventTypes.EVENT_SNAPSHOT_DELETE, accountId, zoneId, snapId, snapshotName, null, null, 0L);
        }
        return usageEvent;
    }

    @Override
    public void performDataMigration(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("USE cloud");
            pstmt.executeQuery();
            upgradeDataCenter(conn);
            upgradeStoragePools(conn);
            upgradeInstanceGroups(conn);
            upgradePortForwardingRules(conn);
            upgradeLoadBalancingRules(conn);
            upgradeHostMemoryCapacityInfo(conn);
            upgradeHostCpuCapacityInfo(conn);
            upgradeDomainResourceCounts(conn);

            migrateEvents(conn);
            createPortForwardingEvents(conn);
            createLoadBalancerEvents(conn);
            createNetworkOfferingEvents(conn);

            // Update hypervisor type for user vm to be consistent with original 2.2.4
            pstmt = conn.prepareStatement("UPDATE vm_instance SET hypervisor_type='XenServer' WHERE hypervisor_type='xenserver'");
            pstmt.executeUpdate();
            pstmt.close();

            // Set account=systemAccount and domain=ROOT for CPVM/SSVM
            pstmt = conn.prepareStatement("UPDATE vm_instance SET account_id=1, domain_id=1 WHERE type='ConsoleProxy' or type='SecondaryStorageVm'");
            pstmt.executeUpdate();
            pstmt.close();

            // Update user statistics
            updateUserStats(conn);

            // delete orphaned (storage pool no longer exists) template_spool_ref(s)
            deleteOrphanedTemplateRef(conn);

            // Upgrade volumes with incorrect Destroyed field
            cleanupVolumes(conn);

            // modify network_group indexes
            modifyIndexes(conn);

            // cleanup lb - vm maps for load balancers that are already removed (there was a bug in 2.1.x when the mappings were
            // left around)
            cleanupLbVmMaps(conn);

        } catch (SQLException e) {
            s_logger.error("Can't perform data migration ", e);
            throw new CloudRuntimeException("Can't perform data migration ", e);
        }

    }

    @Override
    public File[] getCleanupScripts() {
        String file = Script.findScript("", "db/schema-21to22-cleanup.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22-cleanup.sql");
        }

        return new File[] { new File(file) };
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.1.8", "2.1.8" };
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.1";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    private void deleteOrphanedTemplateRef(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id, pool_id from template_spool_ref");
            ResultSet rs = pstmt.executeQuery();
            if (!rs.next()) {
                s_logger.debug("No records in template_spool_ref, skipping this upgrade part");
                return;
            }

            while (rs.next()) {
                Long id = rs.getLong(1);
                Long poolId = rs.getLong(2);

                pstmt = conn.prepareStatement("SELECT * from storage_pool where id=?");
                pstmt.setLong(1, poolId);
                ResultSet rs1 = pstmt.executeQuery();

                if (!rs1.next()) {
                    s_logger.debug("Orphaned template_spool_ref record is found (storage pool doesn't exist any more0) id=" + id + "; so removing the record");
                    pstmt = conn.prepareStatement("DELETE FROM template_spool_ref where id=?");
                    pstmt.setLong(1, id);
                    pstmt.executeUpdate();
                }

            }
            rs.close();
            pstmt.close();

            s_logger.debug("Finished deleting orphaned template_spool_ref(s)");
        } catch (Exception e) {
            s_logger.error("Failed to delete orphaned template_spool_ref(s): ", e);
            throw new CloudRuntimeException("Failed to delete orphaned template_spool_ref(s): ", e);
        }
    }

    private void cleanupVolumes(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id, instance_id, account_id from volumes where destroyed=127");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Long id = rs.getLong(1);
                s_logger.debug("Volume id is " + id);
                Long instanceId = rs.getLong(2);
                Long accountId = rs.getLong(3);

                boolean removeVolume = false;

                pstmt = conn.prepareStatement("SELECT * from account where id=? and removed is not null");
                pstmt.setLong(1, accountId);
                ResultSet rs1 = pstmt.executeQuery();

                if (rs1.next()) {
                    removeVolume = true;
                }

                if (instanceId != null) {
                    pstmt = conn.prepareStatement("SELECT * from vm_instance where id=? and removed is not null");
                    pstmt.setLong(1, instanceId);
                    rs1 = pstmt.executeQuery();

                    if (rs1.next()) {
                        removeVolume = true;
                    }
                }

                if (removeVolume) {
                    pstmt = conn.prepareStatement("UPDATE volumes SET state='Destroy' WHERE id=?");
                    pstmt.setLong(1, id);
                    pstmt.executeUpdate();
                    s_logger.debug("Volume with id=" + id + " is marked with Destroy state as a part of volume cleanup (it's Destroyed had 127 value)");
                }
            }
            rs.close();
            pstmt.close();

            s_logger.debug("Finished cleaning up volumes with incorrect Destroyed field (127)");
        } catch (Exception e) {
            s_logger.error("Failed to cleanup volumes with incorrect Destroyed field (127):", e);
            throw new CloudRuntimeException("Failed to cleanup volumes with incorrect Destroyed field (127):", e);
        }
    }

    private void modifyIndexes(Connection conn) {
        try {

            // removed indexes
            PreparedStatement pstmt = conn.prepareStatement("SHOW INDEX FROM security_group WHERE KEY_NAME = 'fk_network_group__account_id'");
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`security_group` DROP INDEX `fk_network_group__account_id`");
                pstmt.executeUpdate();
                s_logger.debug("Unique key 'fk_network_group__account_id' is removed successfully");
            }

            rs.close();
            pstmt.close();

            pstmt = conn.prepareStatement("SHOW INDEX FROM security_group WHERE KEY_NAME = 'fk_network_group___account_id'");
            rs = pstmt.executeQuery();

            if (rs.next()) {
                pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`security_group` DROP INDEX `fk_network_group___account_id`");
                pstmt.executeUpdate();
                s_logger.debug("Unique key 'fk_network_group___account_id' is removed successfully");
            }

            rs.close();
            pstmt.close();

            // add indexes
            pstmt = conn
                    .prepareStatement("ALTER TABLE `cloud`.`security_group` ADD CONSTRAINT `fk_security_group___account_id` FOREIGN KEY `fk_security_group__account_id` (`account_id`) REFERENCES `account` (`id`) ON DELETE CASCADE");
            pstmt.executeUpdate();
            pstmt.close();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to drop indexes for 'security_group' table due to:", e);
        }
    }

    // There was a bug in 2.1.x when LB rule mapping wasn't removed along with lb rule removal
    // Do cleanup after making sure that the rule was removed
    private void cleanupLbVmMaps(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT DISTINCT load_balancer_id FROM load_balancer_vm_map");
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                long lbId = rs.getLong(1);
                PreparedStatement pstmt1 = conn.prepareStatement("SELECT * FROM load_balancer where id=?");
                pstmt1.setLong(1, lbId);
                ResultSet rs1 = pstmt1.executeQuery();

                PreparedStatement pstmt2 = conn.prepareStatement("SELECT * from event where type like '%lb.delete%' and parameters like '%id=" + lbId + "%'");
                ResultSet rs2 = pstmt2.executeQuery();

                if (!rs1.next() && rs2.next()) {
                    s_logger.debug("Removing load balancer vm mappings for lb id=" + lbId + " as a part of cleanup");
                    pstmt = conn.prepareStatement("DELETE FROM load_balancer_vm_map where load_balancer_id=?");
                    pstmt.setLong(1, lbId);
                    pstmt.executeUpdate();
                }
                rs1.close();
                rs2.close();
                pstmt1.close();
                pstmt2.close();
            }

            rs.close();
            pstmt.close();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to cleanup orpahned lb-vm mappings due to:", e);
        }
    }

    /*
     * Create usage events for existing port forwarding rules
     */
    private void createPortForwardingEvents(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT fw.account_id, ip.data_center_id, fw.id FROM firewall_rules fw, user_ip_address ip where purpose = 'PortForwarding' and "
                    + "fw.state = 'Active' and ip.id = fw.ip_address_id");
            s_logger.debug("Creating Port Forwarding usage events");
            ResultSet rs = pstmt.executeQuery();
            Date now = new Date();
            while (rs.next()) {
                long accountId = rs.getLong(1);
                long zoneId = rs.getLong(2);
                long ruleId = rs.getLong(3);
                PreparedStatement pstmt1 = null;
                pstmt1 = conn.prepareStatement("INSERT INTO usage_event (usage_event.type, usage_event.created, usage_event.account_id, usage_event.zone_id, usage_event.resource_id)"
                        + " VALUES (?, ?, ?, ?, ?)");
                pstmt1.setString(1, EventTypes.EVENT_NET_RULE_ADD);
                pstmt1.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), now));
                pstmt1.setLong(3, accountId);
                pstmt1.setLong(4, zoneId);
                pstmt1.setLong(5, ruleId);

                pstmt1.executeUpdate();
                pstmt1.close();
            }

            rs.close();
            pstmt.close();
            s_logger.debug("Completed creating Port Forwarding usage events");

        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to add port forwarding usage events due to:", e);
        }
    }

    /*
     * Create usage events for existing load balancer rules
     */
    private void createLoadBalancerEvents(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT fw.account_id, ip.data_center_id, fw.id FROM firewall_rules fw, user_ip_address ip where purpose = 'LoadBalancing' and "
                    + "fw.state = 'Active' and ip.id = fw.ip_address_id");
            s_logger.debug("Creating load balancer usage events");
            ResultSet rs = pstmt.executeQuery();
            Date now = new Date();
            while (rs.next()) {
                long accountId = rs.getLong(1);
                long zoneId = rs.getLong(2);
                long ruleId = rs.getLong(3);
                PreparedStatement pstmt1 = null;
                pstmt1 = conn.prepareStatement("INSERT INTO usage_event (usage_event.type, usage_event.created, usage_event.account_id, usage_event.zone_id, usage_event.resource_id)"
                        + " VALUES (?, ?, ?, ?, ?)");
                pstmt1.setString(1, EventTypes.EVENT_LOAD_BALANCER_CREATE);
                pstmt1.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), now));
                pstmt1.setLong(3, accountId);
                pstmt1.setLong(4, zoneId);
                pstmt1.setLong(5, ruleId);

                pstmt1.executeUpdate();
                pstmt1.close();
            }

            rs.close();
            pstmt.close();
            s_logger.debug("Completed creating load balancer usage events");

        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to add Load Balancer usage events due to:", e);
        }
    }

    /*
     * Create usage events for network offerings
     */
    private void createNetworkOfferingEvents(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT vm.account_id, vm.data_center_id, ni.instance_id, vm.name, nw.network_offering_id, nw.is_default FROM nics ni, "
                    + "networks nw, vm_instance vm where vm.type = 'User' and ni.removed is null and ni.instance_id = vm.id and ni.network_id = nw.id;");
            s_logger.debug("Creating network offering usage events");
            ResultSet rs = pstmt.executeQuery();
            Date now = new Date();
            while (rs.next()) {
                long accountId = rs.getLong(1);
                long zoneId = rs.getLong(2);
                long vmId = rs.getLong(3);
                String vmName = rs.getString(4);
                long nw_offering_id = rs.getLong(5);
                long isDefault = rs.getLong(6);
                PreparedStatement pstmt1 = null;
                pstmt1 = conn
                        .prepareStatement("INSERT INTO usage_event (usage_event.type, usage_event.created, usage_event.account_id, usage_event.zone_id, usage_event.resource_id, usage_event.resource_name, "
                                + "usage_event.offering_id, usage_event.size)" + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
                pstmt1.setString(1, EventTypes.EVENT_NETWORK_OFFERING_ASSIGN);
                pstmt1.setString(2, DateUtil.getDateDisplayString(TimeZone.getTimeZone("GMT"), now));
                pstmt1.setLong(3, accountId);
                pstmt1.setLong(4, zoneId);
                pstmt1.setLong(5, vmId);
                pstmt1.setString(6, vmName);
                pstmt1.setLong(7, nw_offering_id);
                pstmt1.setLong(8, isDefault);

                pstmt1.executeUpdate();
                pstmt1.close();
            }

            rs.close();
            pstmt.close();
            s_logger.debug("Completed creating network offering usage events");

        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to add network offering usage events due to:", e);
        }
    }

}
