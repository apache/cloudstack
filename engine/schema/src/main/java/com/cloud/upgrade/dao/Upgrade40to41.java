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
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.Logger;

import com.cloud.utils.db.DbProperties;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade40to41 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade40to41.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.0.0", "4.1.0"};
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
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-40to410.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        updateRegionEntries(conn);
        upgradeEgressFirewallRules(conn);
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-40to410-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    private void updateRegionEntries(Connection conn) {
        final Properties dbProps = DbProperties.getDbProperties();
        int region_id = 1;
        String regionId = dbProps.getProperty("region.id");
        if (regionId != null) {
            region_id = Integer.parseInt(regionId);
        }
        try (PreparedStatement pstmt = conn.prepareStatement("update `cloud`.`region` set id = ?");) {
            //Update regionId in region table
            s_logger.debug("Updating region table with Id: " + region_id);
            pstmt.setInt(1, region_id);
            pstmt.executeUpdate();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Error while updating region entries", e);
        }
    }

    private void upgradeEgressFirewallRules(Connection conn) {

        // update the existing ingress rules traffic type
        try (PreparedStatement updateNwpstmt = conn.prepareStatement("update `cloud`.`firewall_rules`  set traffic_type='Ingress' where purpose='Firewall' and ip_address_id is " +
                "not null and traffic_type is null");)
        {
            updateNwpstmt.executeUpdate();
            s_logger.debug("Updating firewall Ingress rule traffic type: " + updateNwpstmt);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update ingress firewall rules ", e);
        }


        try (PreparedStatement vrNwpstmt = conn.prepareStatement("select network_id FROM `cloud`.`ntwk_service_map` where service='Firewall' and provider='VirtualRouter' ");
             ResultSet vrNwsRs = vrNwpstmt.executeQuery();
        ) {
            while (vrNwsRs.next()) {
                long netId = vrNwsRs.getLong(1);
                //When upgraded from 2.2.14 to 3.0.6 guest_type is updated to Isolated in the 2214to30 clean up sql. clean up executes
                //after this. So checking for Isolated OR Virtual
                try (PreparedStatement NwAcctDomIdpstmt = conn.prepareStatement("select account_id, domain_id FROM `cloud`.`networks` where (guest_type='Isolated' OR " +
                        "guest_type='Virtual') and traffic_type='Guest' and vpc_id is NULL and " +
                        "(state='implemented' OR state='Shutdown') and id=? "); ) {
                    NwAcctDomIdpstmt.setLong(1, netId);

                    try (ResultSet NwAcctDomIdps = NwAcctDomIdpstmt.executeQuery();) {
                        s_logger.debug("Getting account_id, domain_id from networks table: " + NwAcctDomIdpstmt);

                        if (NwAcctDomIdps.next()) {
                            long accountId = NwAcctDomIdps.getLong(1);
                            long domainId = NwAcctDomIdps.getLong(2);
                            //Add new rule for the existing networks
                            s_logger.debug("Adding default egress firewall rule for network " + netId);
                            try (PreparedStatement fwRulespstmt = conn.prepareStatement("INSERT INTO firewall_rules "+
                                    " (uuid, state, protocol, purpose, account_id, domain_id, network_id, xid, created,"
                                    + " traffic_type) VALUES (?, 'Active', 'all', 'Firewall', ?, ?, ?, ?, now(), "
                                 +"'Egress')");
                            ) {
                            fwRulespstmt.setString(1, UUID.randomUUID().toString());
                            fwRulespstmt.setLong(2, accountId);
                            fwRulespstmt.setLong(3, domainId);
                            fwRulespstmt.setLong(4, netId);
                            fwRulespstmt.setString(5, UUID.randomUUID().toString());
                            s_logger.debug("Inserting default egress firewall rule " + fwRulespstmt);
                            fwRulespstmt.executeUpdate();
                            }  catch (SQLException e) {
                                throw new CloudRuntimeException("failed to insert default egress firewall rule ", e);
                            }

                            try (PreparedStatement protoAllpstmt = conn.prepareStatement("select id from firewall_rules where protocol='all' and network_id=?");)
                            {
                            protoAllpstmt.setLong(1, netId);

                                try (ResultSet protoAllRs = protoAllpstmt.executeQuery();) {
                                    long firewallRuleId;
                                    if (protoAllRs.next()) {
                                        firewallRuleId = protoAllRs.getLong(1);

                                        try (PreparedStatement fwCidrsPstmt = conn.prepareStatement("insert into firewall_rules_cidrs (firewall_rule_id,source_cidr) values (?, '0.0.0.0/0')");) {
                                            fwCidrsPstmt.setLong(1, firewallRuleId);
                                            s_logger.debug("Inserting rule for cidr 0.0.0.0/0 for the new Firewall rule id=" + firewallRuleId + " with statement " + fwCidrsPstmt);
                                            fwCidrsPstmt.executeUpdate();
                                        }  catch (SQLException e) {
                                            throw new CloudRuntimeException("Unable to set egress firewall rules ", e);
                                        }

                                    }
                                } catch (SQLException e) {
                                    throw new CloudRuntimeException("Unable to set egress firewall rules ", e);
                                }

                            } catch (SQLException e) {
                                throw new CloudRuntimeException("Unable to set egress firewall rules ", e);
                            }

                        } //if
                    } catch (SQLException e) {
                        throw new CloudRuntimeException("Unable execute update query ", e);
                    }

                } catch (SQLException e) {
                    throw new CloudRuntimeException("Unable to get account id domainid of networks ", e);
                }
            } //while
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to set egress firewall rules ", e);

        }
    }

}
