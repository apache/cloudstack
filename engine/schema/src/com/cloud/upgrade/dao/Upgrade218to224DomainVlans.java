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
import java.util.HashMap;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade218to224DomainVlans implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade218to224DomainVlans.class);

    @Override
    public InputStream[] getPrepareScripts() {
        return null;
    }

    @Override
    public void performDataMigration(Connection conn) {
        HashMap<Long, Long> networkDomainMap = new HashMap<Long, Long>();
        // populate domain_network_ref table
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM networks WHERE shared=1 AND traffic_type='Guest' AND guest_type='Direct'");
            ResultSet rs = pstmt.executeQuery();
            s_logger.debug("query is " + pstmt);
            while (rs.next()) {
                Long networkId = rs.getLong(1);
                Long vlanId = null;
                Long domainId = null;

                pstmt = conn.prepareStatement("SELECT id FROM vlan WHERE network_id=? LIMIT 0,1");
                pstmt.setLong(1, networkId);
                s_logger.debug("query is " + pstmt);
                rs = pstmt.executeQuery();

                while (rs.next()) {
                    vlanId = rs.getLong(1);
                }

                if (vlanId != null) {
                    pstmt = conn.prepareStatement("SELECT domain_id FROM account_vlan_map WHERE domain_id IS NOT NULL AND vlan_db_id=? LIMIT 0,1");
                    pstmt.setLong(1, vlanId);
                    s_logger.debug("query is " + pstmt);
                    rs = pstmt.executeQuery();

                    while (rs.next()) {
                        domainId = rs.getLong(1);
                    }

                    if (domainId != null) {
                        if (!networkDomainMap.containsKey(networkId)) {
                            networkDomainMap.put(networkId, domainId);
                        }
                    }
                }
            }

            // populate domain level networks
            for (Long networkId : networkDomainMap.keySet()) {
                pstmt = conn.prepareStatement("INSERT INTO domain_network_ref (network_id, domain_id) VALUES (?,    ?)");
                pstmt.setLong(1, networkId);
                pstmt.setLong(2, networkDomainMap.get(networkId));
                pstmt.executeUpdate();
            }

            rs.close();
            pstmt.close();

            performDbCleanup(conn);
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to convert 2.1.x domain level vlans to 2.2.x domain level networks", e);
        }
    }

    @Override
    public InputStream[] getCleanupScripts() {
        return null;
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.1.8", "2.1.8"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.4";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }

    private void performDbCleanup(Connection conn) {
        try {
            PreparedStatement pstmt = conn.prepareStatement("SELECT domain_id FROM account_vlan_map");
            try {
                pstmt.executeQuery();
            } catch (SQLException e) {
                s_logger.debug("Assuming that domain_id field doesn't exist in account_vlan_map table, no need to upgrade");
                return;
            }

            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`account_vlan_map` DROP FOREIGN KEY `fk_account_vlan_map__domain_id`");
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`account_vlan_map` DROP COLUMN `domain_id`");
            pstmt.executeUpdate();

            pstmt = conn.prepareStatement("DELETE FROM `cloud`.`account_vlan_map` WHERE account_id IS NULL");
            pstmt.executeUpdate();

            pstmt.close();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to delete domain_id field from account_vlan_map table due to:", e);
        }
    }
}
