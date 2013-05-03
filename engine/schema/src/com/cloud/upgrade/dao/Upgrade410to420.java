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
import java.util.UUID;

import com.cloud.network.vpc.NetworkACL;
import org.apache.log4j.Logger;

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
        createPlaceHolderNics(conn);
        updateRemoteAccessVpn(conn);
        updateSystemVmTemplates(conn);
        updateCluster_details(conn);
        updatePrimaryStore(conn);
        addEgressFwRulesForSRXGuestNw(conn);
        upgradeEIPNetworkOfferings(conn);
        upgradeDefaultVpcOffering(conn);
        upgradePhysicalNtwksWithInternalLbProvider(conn);
        updateNetworkACLs(conn);
    }
	
	private void updateSystemVmTemplates(Connection conn) {
	    PreparedStatement sql = null;
        try {
            sql = conn.prepareStatement("update vm_template set image_data_store_id = 1 where type = 'SYSTEM' or type = 'BUILTIN'");
            sql.executeUpdate();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Failed to upgrade vm template data store uuid: " + e.toString());
        } finally {
            if (sql != null) {
                try {
                    sql.close();
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
        //1,2 are default acl Ids, start Ids from 3
        long nextAclId = 3;
        try {
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
                        //create ACL
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
                    //get cidr
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
}
