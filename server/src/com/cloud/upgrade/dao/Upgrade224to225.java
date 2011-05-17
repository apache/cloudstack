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

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade224to225 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade224to225.class);
    
    @Override
    public File[] getPrepareScripts() {
        String file = Script.findScript("", "db/schema-224to225.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-224to225.sql");
        }
        
        return new File[] {new File(file)};
    }
        
    @Override
    public void performDataMigration(Connection conn) {
        //create security groups for existing accounts if not present
        createSecurityGroups(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.2.4", "2.2.4"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.5";
    }
    
    @Override
    public boolean supportsRollingUpgrade() {
        return false;
    }
    
    private void createSecurityGroups(Connection conn) {
        s_logger.debug("Creating missing default security group as a part of 224-225 upgrade");
        try {
            List<Long> accounts = new ArrayList<Long>();
            PreparedStatement pstmt = conn.prepareStatement("SELECT id FROM account WHERE removed IS NULL and id != 1");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                accounts.add(rs.getLong(1));
            }
            
            for (Long accountId : accounts) {
                //get default security group
                pstmt = conn.prepareStatement("SELECT * FROM security_group WHERE name='default' and account_id=?");
                pstmt.setLong(1, accountId);
                rs = pstmt.executeQuery();
                if (!rs.next()) {
                    s_logger.debug("Default security group is missing for account id=" + accountId + " so adding it");
                    
                    //get accountName/domainId information
                    
                    pstmt = conn.prepareStatement("SELECT account_name, domain_id FROM account WHERE id=?");
                    pstmt.setLong(1, accountId);
                    ResultSet rs1 = pstmt.executeQuery();
                    if (!rs1.next()) {
                        throw new CloudRuntimeException("Unable to create default security group for account id=" + accountId + ": unable to get accountName/domainId info");
                    }
                    String accountName = rs1.getString(1);
                    Long domainId = rs1.getLong(2);
                    
                    pstmt = conn.prepareStatement("INSERT INTO `cloud`.`security_group` (name, description, account_name, account_id, domain_id) VALUES ('default', 'Default Security Group', ?, ?, ?)");
                    pstmt.setString(1, accountName);
                    pstmt.setLong(2, accountId);
                    pstmt.setLong(3, domainId);
                    pstmt.executeUpdate();
                }
            }
            
            
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to create default security groups for existing accounts due to", e);
        }
    }
}
