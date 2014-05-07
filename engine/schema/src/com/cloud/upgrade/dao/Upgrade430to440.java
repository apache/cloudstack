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

import org.apache.log4j.Logger;

import com.cloud.network.Network;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade430to440 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade430to440.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"4.3.0", "4.4.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "4.4.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-430to440.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-4310to440.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        populateIAMGroupAccountMap(conn);
        secondaryIpsAccountAndDomainIdsUpdate(conn);
        moveCidrsToTheirOwnTable(conn);
    }

    // populate iam_group_account_map table for existing accounts
    private void populateIAMGroupAccountMap(Connection conn) {
        PreparedStatement acctInsert = null;
        PreparedStatement acctQuery = null;
        ResultSet rs = null;

        s_logger.debug("Populating iam_group_account_map table for existing accounts...");
        try {
            acctInsert = conn
                    .prepareStatement("INSERT INTO `cloud`.`iam_group_account_map` (group_id, account_id, created) values(?, ?, Now())");
            acctQuery = conn
                    .prepareStatement("select id, type from `cloud`.`account` where removed is null");
            rs = acctQuery.executeQuery();

            while (rs.next()) {
                Long acct_id = rs.getLong("id");
                short type = rs.getShort("type");

                // insert entry in iam_group_account_map table
                acctInsert.setLong(1, type + 1);
                acctInsert.setLong(2, acct_id);
                acctInsert.executeUpdate();
            }
        } catch (SQLException e) {
            String msg = "Unable to populate iam_group_account_map for existing accounts." + e.getMessage();
            s_logger.error(msg);
            throw new CloudRuntimeException(msg, e);
        } finally {
            try {
                if (rs != null) {
                    rs.close();
                }

                if (acctInsert != null) {
                    acctInsert.close();
                }
                if (acctQuery != null) {
                    acctQuery.close();
                }
            } catch (SQLException e) {
            }
        }
        s_logger.debug("Completed populate iam_group_account_map for existing accounts.");
    }



    private void secondaryIpsAccountAndDomainIdsUpdate(Connection conn) {
        PreparedStatement pstmt = null;
        PreparedStatement pstmtVm = null;
        PreparedStatement pstmtNw = null;
        PreparedStatement pstmtUpdate = null;

        ResultSet rs1 = null;
        ResultSet vmRs = null;
        ResultSet networkRs = null;

        String secondIpsSql = "SELECT id, vmId, network_id, account_id, domain_id, ip4_address FROM `cloud`.`nic_secondary_ips`";

        try {
            pstmt = conn.prepareStatement(secondIpsSql);
            rs1 = pstmt.executeQuery();

            while(rs1.next()) {
                long ipId = rs1.getLong(1);
                long vmId = rs1.getLong(2);
                long networkId = rs1.getLong(3);
                long accountId = rs1.getLong(4);
                long domainId = rs1.getLong(5);
                String ipAddr = rs1.getString(6);

                pstmtVm = conn.prepareStatement("SELECT account_id, domain_id FROM `cloud`.`vm_instance` where id = ?");
                pstmtVm.setLong(1,vmId);

                vmRs = pstmtVm.executeQuery();

                if (vmRs.next()) {
                    long vmAccountId = vmRs.getLong(1);
                    long vmDomainId = vmRs.getLong(2);

                    if (vmAccountId != accountId && vmAccountId != domainId) {
                        // update the secondary ip accountid and domainid to vm accountid domainid
                        // check the network type. If network is shared accountid doaminid needs to be updated in
                        // in both nic_secondary_ips table and user_ip_address table

                        pstmtUpdate = conn.prepareStatement("UPDATE `cloud`.`nic_secondary_ips` SET account_id = ?, domain_id= ? WHERE id = ?");
                        pstmtUpdate.setLong(1, vmAccountId);
                        pstmtUpdate.setLong(2,vmDomainId);
                        pstmtUpdate.setLong(3,ipId);
                        pstmtUpdate.executeUpdate();
                        pstmtUpdate.close();

                        pstmtNw = conn.prepareStatement("SELECT guest_type FROM `cloud`.`networks` where id = ?");
                        pstmtNw.setLong(1,networkId);

                        networkRs = pstmtNw.executeQuery();
                        if (networkRs.next()) {
                            String guesttype = networkRs.getString(1);

                            if (guesttype.equals(Network.GuestType.Shared.toString())) {
                                pstmtUpdate = conn.prepareStatement("UPDATE `cloud`.`user_ip_address` SET account_id = ?, domain_id= ? WHERE public_ip_address = ?");
                                pstmtUpdate.setLong(1,vmAccountId);
                                pstmtUpdate.setLong(2,vmDomainId);
                                pstmtUpdate.setString(3,ipAddr);
                                pstmtUpdate.executeUpdate();
                                pstmtUpdate.close();

                            }
                        }
                                networkRs.close();
                        networkRs = null;
                                pstmtNw.close();
                        pstmtNw = null;
                    }
                } //if

                pstmtVm.close();
                pstmtVm = null;
                        vmRs.close();
                vmRs = null;
            } // while


        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while Moving private zone information to dedicated resources", e);
        } finally {

            if (pstmt != null) {
                try {
                    pstmt.close();

                } catch (SQLException e) {
                }
            }


            if (rs1 != null) {
                try {
                    rs1.close();
                } catch (SQLException e) {
                }
            }



            if (pstmtVm != null) {
                try {
                    pstmtVm.close();
                } catch (SQLException e) {
                }
            }

            if (vmRs != null) {
                try {
                    vmRs.close();
                } catch (SQLException e) {
                }
            }



            if (pstmtNw != null) {
                try {
                    pstmtNw.close();

                } catch (SQLException e) {
                }
            }


            if (networkRs != null) {
                try {
                    networkRs.close();
                } catch (SQLException e) {
                }
            }
        }
        s_logger.debug("Done updating vm nic secondary ip  account and domain ids");
    }


    private void moveCidrsToTheirOwnTable(Connection conn) {
        PreparedStatement pstmtItem = null;
        PreparedStatement pstmtCidr = null;
        ResultSet rsItems = null;

        String networkAclItemSql = "SELECT id, cidr FROM `cloud`.`network_acl_item`";

        s_logger.debug("Moving network acl item cidrs to a row per cidr");
        try {
            pstmtItem = conn.prepareStatement(networkAclItemSql);
            rsItems = pstmtItem.executeQuery();

            // for each network acl item
            while(rsItems.next()) {
                long itemId = rsItems.getLong(1);
                // get the source cidr list
                String cidrList = rsItems.getString(2);
                s_logger.debug("Moving '" + cidrList +  "' to a row per cidr");
                // split it
                String[] cidrArray = cidrList.split(",");
                // insert a record per cidr
                String networkAclItemCidrSql = "INSERT INTO `cloud`.`network_acl_item_cidrs` (network_acl_item_id, cidr) VALUES (?,?)";
                for(String cidr: cidrArray)
                {
                    pstmtCidr = conn.prepareStatement(networkAclItemCidrSql);
                    pstmtCidr.setLong(1,itemId);
                    pstmtCidr.setString(2,cidr);
                    pstmtCidr.executeUpdate();
                }
                pstmtCidr.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while Moving network acl item cidrs to a row per cidr", e);
        } finally {

            if (pstmtItem != null) {
                try {
                    pstmtItem.close();

                } catch (SQLException e) {
                }
            }
            if (pstmtCidr != null) {
                try {
                    pstmtCidr.close();

                } catch (SQLException e) {
                }
            }
        }
        s_logger.debug("Done moving network acl item cidrs to a row per cidr");
    }


    @Override
    public File[] getCleanupScripts() {
        String script = Script.findScript("", "db/schema-430to440-cleanup.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-430to440-cleanup.sql");
        }

        return new File[] {new File(script)};
    }
}
