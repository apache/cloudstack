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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade305to306 extends Upgrade30xBase {
    final static Logger s_logger = Logger.getLogger(Upgrade305to306.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"3.0.5", "3.0.6"};
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.6";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-305to306.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-305to306.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {

        //Add index for alert table.
        addIndexForAlert(conn);

        upgradeEIPNetworkOfferings(conn);

        addIndexForHostDetails(conn);
        upgradeEgressFirewallRules(conn);
        removeFirewallServiceFromSharedNetworkOfferingWithSGService(conn);
        fix22xKVMSnapshots(conn);
    }

    private void addIndexForAlert(Connection conn) {

        //First drop if it exists. (Due to patches shipped to customers some will have the index and some wont.)
        List<String> indexList = new ArrayList<String>();
        s_logger.debug("Dropping index i_alert__last_sent if it exists");
        indexList.add("i_alert__last_sent");
        DbUpgradeUtils.dropKeysIfExist(conn, "alert", indexList, false);

        //Now add index.
        try (PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`alert` ADD INDEX `i_alert__last_sent`(`last_sent`)");) {
            pstmt.executeUpdate();
            s_logger.debug("Added index i_alert__last_sent for table alert");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to add index i_alert__last_sent to alert table for the column last_sent", e);
        }

    }

    private void upgradeEIPNetworkOfferings(Connection conn) {
        try (
                PreparedStatement pstmt = conn.prepareStatement("select id, elastic_ip_service from `cloud`.`network_offerings` where traffic_type='Guest'");
                PreparedStatement pstmt1 = conn.prepareStatement("UPDATE `cloud`.`network_offerings` set eip_associate_public_ip=? where id=?");
                ResultSet rs = pstmt.executeQuery();
            ){
            while (rs.next()) {
                long id = rs.getLong(1);
                // check if elastic IP service is enabled for network offering
                if (rs.getLong(2) != 0) {
                    //update network offering with eip_associate_public_ip set to true
                    pstmt1.setBoolean(1, true);
                    pstmt1.setLong(2, id);
                    pstmt1.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to set eip_associate_public_ip for network offerings with EIP service enabled.", e);
        }
    }

    private void addIndexForHostDetails(Connection conn) {

        //First drop if it exists. (Due to patches shipped to customers some will have the index and some wont.)
        List<String> indexList = new ArrayList<String>();
        s_logger.debug("Dropping index fk_host_details__host_id if it exists");
        indexList.add("fk_host_details__host_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "host_details", indexList, false);

        //Now add index.
        try (PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`host_details` ADD INDEX `fk_host_details__host_id`(`host_id`)");) {
            pstmt.executeUpdate();
            s_logger.debug("Added index fk_host_details__host_id for table host_details");
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to add index fk_host_details__host_id to host_details table for the column host_id", e);
        }

    }

    private void upgradeEgressFirewallRules(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rsId = null;
        ResultSet rsNw = null;
        try {
            // update the existing ingress rules traffic type
            pstmt = conn.prepareStatement("update `cloud`.`firewall_rules`" +
                "  set traffic_type='Ingress' where purpose='Firewall' and ip_address_id is not null and traffic_type is null");
            s_logger.debug("Updating firewall Ingress rule traffic type: " + pstmt);
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("select network_id FROM `cloud`.`ntwk_service_map` where service='Firewall' and provider='VirtualRouter' ");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long netId = rs.getLong(1);
                //When upgraded from 2.2.14 to 3.0.6 guest_type is updated to Isolated in the 2214to30 clean up sql. clean up executes
                //after this. So checking for Isolated OR Virtual
                pstmt = conn.prepareStatement("select account_id, domain_id FROM `cloud`.`networks` where (guest_type='Isolated' OR guest_type='" +
                    "Virtual') and traffic_type='Guest' and vpc_id is NULL and (state='implemented' OR state='Shutdown') and id=? ");
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
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
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
                pstmt = conn.prepareStatement("DELETE FROM `cloud`.`ntwk_offering_service_map` where network_offering_id=? and service='Firewall'");
                pstmt.setLong(1, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to remove Firewall service for SG shared network offering.", e);
        } finally {
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
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
                    String correctedPath = File.separator + backUpPath.substring(index);
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
            closeAutoCloseable(rs);
            closeAutoCloseable(pstmt);
        }
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-305to306-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-305to306-cleanup.sql");
        }

        return new File[] {new File(script)};
    }

}
