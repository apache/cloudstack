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

import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.crypt.EncryptionSecretKeyChecker;
import com.cloud.utils.db.DbProperties;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

import org.apache.log4j.Logger;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.properties.EncryptableProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;
import java.util.UUID;

public class Upgrade40to41 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade40to41.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "4.0.0", "4.1.0" };
    }

    @Override
    public String getUpgradedVersion() {
        return "4.1.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-40to410.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-40to410.sql");
        }

        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateRegionEntries(conn);
        upgradeEgressFirewallRules(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-40to410-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-40to410-cleanup.sql");
        }

        return new File[] { new File(script) };
    }

    private void updateRegionEntries(Connection conn) {
        final Properties dbProps = DbProperties.getDbProperties();
        int region_id = 1;
        String regionId = dbProps.getProperty("region.id");
        if(regionId != null){
            region_id = Integer.parseInt(regionId);
        }
        PreparedStatement pstmt = null;
        try {
            //Update regionId in region table
            s_logger.debug("Updating region table with Id: "+region_id);
            pstmt = conn.prepareStatement("update `cloud`.`region` set id = ?");
            pstmt.setInt(1, region_id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while updating region entries", e);
        } finally {
            try {
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }

    private void upgradeEgressFirewallRules(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rsId = null;
        ResultSet rsNw = null;
        try {
            // update the existing ingress rules traffic type
            pstmt = conn.prepareStatement("update `cloud`.`firewall_rules`  set traffic_type='Ingress' where purpose='Firewall' and ip_address_id is not null and traffic_type is null");
            s_logger.debug("Updating firewall Ingress rule traffic type: " + pstmt);
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("select network_id FROM `cloud`.`ntwk_service_map` where service='Firewall' and provider='VirtualRouter' ");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                    long netId = rs.getLong(1);
                    //When upgraded from 2.2.14 to 3.0.6 guest_type is updated to Isolated in the 2214to30 clean up sql. clean up executes
                    //after this. So checking for Isolated OR Virtual
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

}
