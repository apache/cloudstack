/**
 * Copyright (C) 2012 Citrix Systems, Inc.  All rights reserved
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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.cloud.configuration.Resource.ResourceType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade30to301 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade2214to30.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "3.0.0", "3.0.1" };
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

        return new File[] { new File(script) };
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
                pstmt = conn.prepareStatement("select count(*) from `cloud`.`networks` n, `cloud`.`account_network_ref` a, `cloud`.`network_offerings` no" +
                        " WHERE n.acl_type='Account' and n.id=a.network_id and a.account_id=? and a.is_owner=1 and no.specify_vlan=false and no.traffic_type='Guest'");
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
            try {
                if (rs != null) {
                    rs.close();
                }
                
                if (rs1 != null) {
                    rs1.close();
                }
                
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }
    
    protected void udpateDomainNetworkResourceCount(Connection conn) {
        Upgrade218to22.upgradeDomainResourceCounts(conn, ResourceType.network);
    }

}
