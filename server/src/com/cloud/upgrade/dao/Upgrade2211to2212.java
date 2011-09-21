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
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.configuration.Resource;
import com.cloud.configuration.Resource.ResourceType;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade2211to2212 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade2211to2212.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.2.11", "2.2.11"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.12";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-2211to2212.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-2211to2212.sql");
        }
        
        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        createResourceCount(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
    
    
    private void createResourceCount(Connection conn) {
        s_logger.debug("Creating missing resource_count records as a part of 2.2.11-2.2.12 upgrade");
        try {
            
            //Get all non removed accounts
            List<Long> accounts = new ArrayList<Long>();
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM account");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                accounts.add(rs.getLong(1));
            }
            rs.close();
            
            
            //get all non removed domains
            List<Long> domains = new ArrayList<Long>();
            pstmt = conn.prepareStatement("SELECT id FROM domain");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                domains.add(rs.getLong(1));
            }           
            rs.close();

            for (Long accountId : accounts) {  
                for (ResourceType resourceType : Resource.ResourceType.values()) {
                    pstmt = conn.prepareStatement("SELECT * FROM resource_count WHERE type=? and account_id=?");
                    pstmt.setString(1, resourceType.toString());
                    pstmt.setLong(2, accountId);
                    rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        s_logger.debug("Inserting resource_count record of type " + resourceType + " for account id=" + accountId);
                        pstmt = conn.prepareStatement("INSERT INTO resource_count (account_id, domain_id, type, count) VALUES (?, null, ?, 0)");
                        pstmt.setLong(1, accountId);
                        pstmt.setString(2, resourceType.toString());
                        pstmt.executeUpdate();
                    }
                    rs.close();
                }
                pstmt.close();
            }
            
            for (Long domainId : domains) {  
                for (ResourceType resourceType : Resource.ResourceType.values()) {
                    pstmt = conn.prepareStatement("SELECT * FROM resource_count WHERE type=? and domain_id=?");
                    pstmt.setString(1, resourceType.toString());
                    pstmt.setLong(2, domainId);
                    rs = pstmt.executeQuery();
                    if (!rs.next()) {
                        s_logger.debug("Inserting resource_count record of type " + resourceType + " for domain id=" + domainId);
                        pstmt = conn.prepareStatement("INSERT INTO resource_count (account_id, domain_id, type, count) VALUES (null, ?, ?, 0)");
                        pstmt.setLong(1, domainId);
                        pstmt.setString(2, resourceType.toString());
                        pstmt.executeUpdate();
                    }
                    rs.close();
                }
                pstmt.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create default security groups for existing accounts due to", e);
        }
    }
 
}
