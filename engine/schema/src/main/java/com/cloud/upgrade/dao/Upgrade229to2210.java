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

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade229to2210 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade229to2210.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.9", "2.2.9"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.10";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-229to2210.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateFirewallRules(conn);
        updateSnapshots(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return null;
    }

    private void updateSnapshots(Connection conn) {
        long currentSnapshotId = 0;
        try (
                PreparedStatement pstmt = conn.prepareStatement("select id, prev_snap_id from snapshots where sechost_id is NULL and prev_snap_id is not NULL and status=\"BackedUp\" and removed is NULL order by id");
                ResultSet rs = pstmt.executeQuery();
                PreparedStatement pstmt2 = conn.prepareStatement("select sechost_id from snapshots where id=? and sechost_id is not NULL");
                PreparedStatement updateSnapshotStatement = conn.prepareStatement("update snapshots set sechost_id=? where id=?");
            ){
            while (rs.next()) {
                long id = rs.getLong(1);
                long preSnapId = rs.getLong(2);
                currentSnapshotId = id;
                pstmt2.setLong(1, preSnapId);
                try (ResultSet sechost = pstmt2.executeQuery();) {
                    if (sechost.next()) {
                        long secHostId = sechost.getLong(1);
                        updateSnapshotStatement.setLong(1, secHostId);
                        updateSnapshotStatement.setLong(2, id);
                        updateSnapshotStatement.executeUpdate();
                    }
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update snapshots id=" + currentSnapshotId, e);
        }
    }

    private void updateFirewallRules(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        long currentRuleId = 0;
        try {
            // Host and Primary storage capacity types
            pstmt =
                conn.prepareStatement("select id, ip_address_id, start_port, end_port, protocol, account_id, domain_id, network_id from firewall_rules where state != 'Revoke'");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                long ipId = rs.getLong(2);
                int startPort = rs.getInt(3);
                int endPort = rs.getInt(4);
                String protocol = rs.getString(5);
                long accountId = rs.getLong(6);
                long domainId = rs.getLong(7);
                long networkId = rs.getLong(8);
                currentRuleId = id;
                Long firewallRuleId = null;

                pstmt =
                    conn.prepareStatement("INSERT INTO firewall_rules (ip_address_id, start_port, end_port, protocol, account_id, domain_id, network_id, purpose, state, xid, created, related) VALUES (?, ?, ?, ?, ?, ?, ?, 'Firewall', 'Active', ?, now(), ?)");

                pstmt.setLong(1, ipId);
                pstmt.setInt(2, startPort);
                pstmt.setInt(3, endPort);
                pstmt.setString(4, protocol);
                pstmt.setLong(5, accountId);
                pstmt.setLong(6, domainId);
                pstmt.setLong(7, networkId);
                pstmt.setString(8, UUID.randomUUID().toString());
                pstmt.setLong(9, id);

                s_logger.debug("Updating firewall rule with the statement " + pstmt);
                pstmt.executeUpdate();

                //get new FirewallRule update
                pstmt =
                    conn.prepareStatement("SELECT id from firewall_rules where purpose='Firewall' and start_port=? and end_port=? and protocol=? and ip_address_id=? and network_id=? and related=?");
                pstmt.setInt(1, startPort);
                pstmt.setInt(2, endPort);
                pstmt.setString(3, protocol);
                pstmt.setLong(4, ipId);
                pstmt.setLong(5, networkId);
                pstmt.setLong(6, id);

                ResultSet rs1 = pstmt.executeQuery();

                if (rs1.next()) {
                    firewallRuleId = rs1.getLong(1);
                } else {
                    throw new CloudRuntimeException("Unable to find just inserted firewall rule for ptocol " + protocol + ", start_port " + startPort + " and end_port " +
                        endPort + " and ip address id=" + ipId);
                }

                pstmt = conn.prepareStatement("select id from firewall_rules_cidrs where firewall_rule_id=?");
                pstmt.setLong(1, id);

                ResultSet rs2 = pstmt.executeQuery();

                if (rs2.next()) {
                    pstmt = conn.prepareStatement("update firewall_rules_cidrs set firewall_rule_id=? where firewall_rule_id=?");
                    pstmt.setLong(1, firewallRuleId);
                    pstmt.setLong(2, id);
                    s_logger.debug("Updating existing cidrs for the rule id=" + id + " with the new Firewall rule id=" + firewallRuleId + " with statement" + pstmt);
                    pstmt.executeUpdate();
                } else {
                    pstmt = conn.prepareStatement("insert into firewall_rules_cidrs (firewall_rule_id,source_cidr) values (?, '0.0.0.0/0')");
                    pstmt.setLong(1, firewallRuleId);
                    s_logger.debug("Inserting rule for cidr 0.0.0.0/0 for the new Firewall rule id=" + firewallRuleId + " with statement " + pstmt);
                    pstmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update firewall rule id=" + currentRuleId, e);
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

}
