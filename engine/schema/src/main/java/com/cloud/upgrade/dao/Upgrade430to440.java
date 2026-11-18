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


import com.cloud.network.Network;
import com.cloud.network.Networks.BroadcastDomainType;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade430to440 extends DbUpgradeAbstractImpl {

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
    public InputStream[] getPrepareScripts() {
        final String scriptFile = "META-INF/db/schema-430to440.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }

    @Override
    public void performDataMigration(Connection conn) {
        secondaryIpsAccountAndDomainIdsUpdate(conn);
        moveCidrsToTheirOwnTable(conn);
        addExtractTemplateAndVolumeColumns(conn);
        updateVlanUris(conn);
    }

    private void addExtractTemplateAndVolumeColumns(Connection conn) {

        try (PreparedStatement selectTemplateInfostmt = conn.prepareStatement("SELECT *  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'cloud' AND TABLE_NAME = 'template_store_ref' AND COLUMN_NAME = 'download_url_created'");
             ResultSet templateInfoResults = selectTemplateInfostmt.executeQuery();
             PreparedStatement addDownloadUrlCreatedToTemplateStorerefstatement = conn.prepareStatement("ALTER TABLE `cloud`.`template_store_ref` ADD COLUMN `download_url_created` datetime");
             PreparedStatement addDownloadUrlToTemplateStorerefstatement = conn.prepareStatement("ALTER TABLE `cloud`.`template_store_ref` ADD COLUMN `download_url` varchar(255)");
             PreparedStatement selectVolumeInfostmt = conn.prepareStatement("SELECT *  FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = 'cloud' AND TABLE_NAME = 'volume_store_ref' AND COLUMN_NAME = 'download_url_created'");
             ResultSet volumeInfoResults = selectVolumeInfostmt.executeQuery();
             PreparedStatement addDownloadUrlCreatedToVolumeStorerefstatement = conn.prepareStatement("ALTER TABLE `cloud`.`volume_store_ref` ADD COLUMN `download_url_created` datetime");
            ) {

            // Add download_url_created, download_url to template_store_ref
            if (!templateInfoResults.next()) {
                addDownloadUrlCreatedToTemplateStorerefstatement.executeUpdate();
                addDownloadUrlToTemplateStorerefstatement.executeUpdate();
            }

            // Add download_url_created to volume_store_ref - note download_url already exists
            if (!volumeInfoResults.next()) {
                addDownloadUrlCreatedToVolumeStorerefstatement.executeUpdate();
            }

        } catch (SQLException e) {
            throw new CloudRuntimeException("Adding columns for Extract Template And Volume functionality failed");
        }
    }

    private void secondaryIpsAccountAndDomainIdsUpdate(Connection conn) {
        String secondIpsSql = "SELECT id, vmId, network_id, account_id, domain_id, ip4_address FROM `cloud`.`nic_secondary_ips`";

        try (PreparedStatement pstmt = conn.prepareStatement(secondIpsSql);
             ResultSet rs1 = pstmt.executeQuery();
            ) {
            while(rs1.next()) {
                long ipId = rs1.getLong(1);
                long vmId = rs1.getLong(2);
                long networkId = rs1.getLong(3);
                long accountId = rs1.getLong(4);
                long domainId = rs1.getLong(5);
                String ipAddr = rs1.getString(6);

                try(PreparedStatement pstmtVm = conn.prepareStatement("SELECT account_id, domain_id FROM `cloud`.`vm_instance` where id = ?");) {
                    pstmtVm.setLong(1,vmId);

                    try(ResultSet vmRs = pstmtVm.executeQuery();) {

                        if (vmRs.next()) {
                            long vmAccountId = vmRs.getLong(1);
                            long vmDomainId = vmRs.getLong(2);

                            if (vmAccountId != accountId && vmAccountId != domainId) {
                                // update the secondary ip accountid and domainid to vm accountid domainid
                                // check the network type. If network is shared accountid doaminid needs to be updated in
                                // in both nic_secondary_ips table and user_ip_address table

                                try(PreparedStatement pstmtUpdate = conn.prepareStatement("UPDATE `cloud`.`nic_secondary_ips` SET account_id = ?, domain_id= ? WHERE id = ?");) {
                                    pstmtUpdate.setLong(1, vmAccountId);
                                    pstmtUpdate.setLong(2,vmDomainId);
                                    pstmtUpdate.setLong(3,ipId);
                                    pstmtUpdate.executeUpdate();
                                } catch (SQLException e) {
                                    throw new CloudRuntimeException("Exception while updating secondary ip for nic " + ipId, e);
                                }

                                try(PreparedStatement pstmtNw = conn.prepareStatement("SELECT guest_type FROM `cloud`.`networks` where id = ?");) {
                                    pstmtNw.setLong(1,networkId);

                                    try(ResultSet networkRs = pstmtNw.executeQuery();) {
                                        if (networkRs.next()) {
                                            String guesttype = networkRs.getString(1);

                                            if (guesttype.equals(Network.GuestType.Shared.toString())) {
                                                try(PreparedStatement pstmtUpdate = conn.prepareStatement("UPDATE `cloud`.`user_ip_address` SET account_id = ?, domain_id= ? WHERE public_ip_address = ?");) {
                                                    pstmtUpdate.setLong(1,vmAccountId);
                                                    pstmtUpdate.setLong(2,vmDomainId);
                                                    pstmtUpdate.setString(3,ipAddr);
                                                    pstmtUpdate.executeUpdate();
                                                } catch (SQLException e) {
                                                    throw new CloudRuntimeException("Exception while updating public ip  " + ipAddr, e);
                                                }
                                            }
                                        }
                                    } catch (SQLException e) {
                                        throw new CloudRuntimeException("Exception while retrieving guest type for network " + networkId, e);
                                    }

                                } catch (SQLException e) {
                                    throw new CloudRuntimeException("Exception while retrieving guest type for network " + networkId, e);
                                }
                            } // if
                        } // if
                    }
                }
            } // while
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while Moving private zone information to dedicated resources", e);
        }
        logger.debug("Done updating vm nic secondary ip  account and domain ids");
    }


    private void moveCidrsToTheirOwnTable(Connection conn) {
        logger.debug("Moving network acl item cidrs to a row per cidr");

        String networkAclItemSql = "SELECT id, cidr FROM `cloud`.`network_acl_item`";
        String networkAclItemCidrSql = "INSERT INTO `cloud`.`network_acl_item_cidrs` (network_acl_item_id, cidr) VALUES (?,?)";

        try (PreparedStatement pstmtItem = conn.prepareStatement(networkAclItemSql);
             ResultSet rsItems = pstmtItem.executeQuery();
             PreparedStatement pstmtCidr = conn.prepareStatement(networkAclItemCidrSql);
            ) {


            // for each network acl item
            while(rsItems.next()) {
                long itemId = rsItems.getLong(1);
                // get the source cidr list
                String cidrList = rsItems.getString(2);
                logger.debug("Moving '" + cidrList +  "' to a row per cidr");
                // split it
                String[] cidrArray = cidrList.split(",");
                // insert a record per cidr
                pstmtCidr.setLong(1, itemId);
                for (String cidr : cidrArray) {
                    pstmtCidr.setString(2, cidr);
                    pstmtCidr.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Exception while Moving network acl item cidrs to a row per cidr", e);
        }
        logger.debug("Done moving network acl item cidrs to a row per cidr");
    }

    private void updateVlanUris(Connection conn) {
        logger.debug("updating vlan URIs");
        try(PreparedStatement selectstatement = conn.prepareStatement("SELECT id, vlan_id FROM `cloud`.`vlan` where vlan_id not like '%:%'");
            ResultSet results = selectstatement.executeQuery()) {

            while (results.next()) {
                long id = results.getLong(1);
                String vlan = results.getString(2);
                if (vlan == null || "".equals(vlan)) {
                    continue;
                }
                String vlanUri = BroadcastDomainType.Vlan.toUri(vlan).toString();
                try(PreparedStatement updatestatement = conn.prepareStatement("update `cloud`.`vlan` set vlan_id=? where id=?");)
                {
                    updatestatement.setString(1, vlanUri);
                    updatestatement.setLong(2, id);
                    updatestatement.executeUpdate();
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Unable to update vlan URI " + vlanUri + " for vlan record " + id, e);
                }
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update vlan URIs ", e);
        }
        logger.debug("Done updating vlan URIs");
    }

    @Override
    public InputStream[] getCleanupScripts() {
        final String scriptFile = "META-INF/db/schema-430to440-cleanup.sql";
        final InputStream script = Thread.currentThread().getContextClassLoader().getResourceAsStream(scriptFile);
        if (script == null) {
            throw new CloudRuntimeException("Unable to find " + scriptFile);
        }

        return new InputStream[] {script};
    }
}
