/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
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
import java.util.ArrayList;

import com.cloud.utils.Pair;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.db.DB;
import com.cloud.utils.exception.CloudRuntimeException;

public class Upgrade217to22 implements DbUpgrade {

    @Override
    public File getPrepareScript() {
        File file = PropertiesUtil.findConfigFile("schema-21to22.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22.sql");
        }
        
        return file;
    }
    
    @DB
    protected void upgradeDataCenter() {
        
    }
    
    protected void upgradeNetworks(Connection conn) {
        String getAccountsSql = "SELECT id, domain_id FROM accounts WHERE removed IS NULL AND id > 1";
        String getDataCenterSql = "SELECT id FROM data_center";
        String getNextNetworkSequenceSql = "SELECT value from sequence where name='networks_seq'";
        String advanceNetworkSequenceSql = "UPDATE sequence set value=value+1 where name='networks_seq'";
        String insertNetworkSql = "INSERT INTO NETWORKS(id, name, display_text, traffic_type, broadcast_domain_type, gateway, cidr, mode, network_offering_id, data_center_id, guru_name, state, domain_id, account_id, dns1, dns2, guest_type, shared, is_default, created) " + 
                                                "VALUES(?,  ?,    ?,            ?,            ?,                     ?,       ?,    ?,    ?,                   ?,              ?,         ?,     ?,         ?,          ?,    ?,    ,          false,  true,       now())"; 
        PreparedStatement pstmt;
        try {
            pstmt = conn.prepareStatement(getAccountsSql);
            ResultSet rs = pstmt.executeQuery();
            ArrayList<Pair<Long, Long>> accountIds = new ArrayList<Pair<Long, Long>>();
            while (rs.next()) {
                accountIds.add(new Pair<Long, Long>(rs.getLong(1), rs.getLong(2)));
            }
            rs.close();
            pstmt.close();
            pstmt = conn.prepareStatement(getDataCenterSql);
            rs = pstmt.executeQuery();
            ArrayList<Long> dataCenterIds = new ArrayList<Long>();
            while (rs.next()) {
                dataCenterIds.add(rs.getLong(1));
            }
            rs.close();
            pstmt.close();
            
            for (Pair<Long, Long> accountId : accountIds) {
                for (Long dataCenterId : dataCenterIds) {
                    pstmt = conn.prepareStatement(getNextNetworkSequenceSql);
                    rs = pstmt.executeQuery();
                    rs.next();
                    long seq = rs.getLong(1);
                    rs.close();
                    pstmt.close();
                    
                    pstmt = conn.prepareStatement(advanceNetworkSequenceSql);
                    pstmt.executeUpdate();
                    pstmt.close();
                    
                    pstmt = conn.prepareStatement(insertNetworkSql);
                }
            }
            
            
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to perform upgrade", e);
        }
    }
    
    @Override
    public void performDataMigration(Connection conn) {
    }

    @Override
    public File getCleanupScript() {
        File file = PropertiesUtil.findConfigFile("schema-21to22-cleanup.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22-cleanup.sql");
        }
        
        return file;
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.1.7", "2.1.7" };
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.0";
    }
    
    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }
}
