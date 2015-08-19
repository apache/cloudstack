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

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade30to301 extends LegacyDbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade30to301.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"3.0.0", "3.0.1"};
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.1";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-30to301.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-30to301.sql");
        }

        return new File[] {new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
        // update network account resource count
        udpateAccountNetworkResourceCount(conn);
        // update network domain resource count
        udpateDomainNetworkResourceCount(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    protected void udpateAccountNetworkResourceCount(Connection conn) {
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        ResultSet rs1 = null;
        long accountId = 0;
        try {
            pstmt = conn.prepareStatement("select id from `cloud`.`account` where removed is null");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                accountId = rs.getLong(1);

                //get networks count for the account
                pstmt =
                    conn.prepareStatement("select count(*) from `cloud`.`networks` n, `cloud`.`account_network_ref` a, `cloud`.`network_offerings` no"
                        + " WHERE n.acl_type='Account' and n.id=a.network_id and a.account_id=? and a.is_owner=1 and no.specify_vlan=false and no.traffic_type='Guest'");
                pstmt.setLong(1, accountId);
                rs1 = pstmt.executeQuery();
                long count = 0;
                while (rs1.next()) {
                    count = rs1.getLong(1);
                }

                pstmt = conn.prepareStatement("insert into `cloud`.`resource_count` (account_id, domain_id, type, count) VALUES (?, null, 'network', ?)");
                pstmt.setLong(1, accountId);
                pstmt.setLong(2, count);
                pstmt.executeUpdate();
                s_logger.debug("Updated network resource count for account id=" + accountId + " to be " + count);
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update network resource count for account id=" + accountId, e);
        } finally {
            closeAutoCloseable(rs);
            closeAutoCloseable(rs1);
            closeAutoCloseable(pstmt);
        }
    }

    protected void udpateDomainNetworkResourceCount(Connection conn) {
        Upgrade218to22.upgradeDomainResourceCounts(conn, ResourceType.network);
    }

}
