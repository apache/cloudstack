/**
 * *  Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
 *
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
=======
 *  Copyright (C) 2011 Cloud.com, Inc.  All rights reserved.
 */
package com.cloud.upgrade.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade227to228Premium extends Upgrade227to228 {
    final static Logger s_logger = Logger.getLogger(Upgrade227to228Premium.class);
    
    @Override
    public File[] getPrepareScripts() {
        File[] scripts = super.getPrepareScripts();
        File[] newScripts = new File[2]; 
        newScripts[0] = scripts[0];
        
        String file = Script.findScript("","db/schema-227to228-premium.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-227to228-premium.sql");
        }
        
        newScripts[1] = new File(file);
        
        return newScripts;
    }
    
    @Override
    public void performDataMigration(Connection conn) {
        addSourceIdColumn(conn);
        addNetworkIdsToUserStats(conn);
        super.performDataMigration(conn);
    }
    
    @Override
    public File[] getCleanupScripts() {
        return null;
    }

    private void addSourceIdColumn(Connection conn) {
        boolean insertField = false;
        try {
            PreparedStatement pstmt;
            try {
                pstmt = conn.prepareStatement("SELECT source_id FROM `cloud_usage`.`usage_storage`");
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    s_logger.info("The source id field already exist, not adding it");
                }
                
            } catch (Exception e) {
                // if there is an exception, it means that field doesn't exist, and we can create it   
                insertField = true;
            }
            
            if (insertField) {
                s_logger.debug("Adding source_id to usage_storage...");
                pstmt = conn.prepareStatement("ALTER TABLE `cloud_usage`.`usage_storage` ADD COLUMN `source_id` bigint unsigned");
                pstmt.executeUpdate();
                s_logger.debug("Column source_id was added successfully to usage_storage table");
                pstmt.close();
            }
          
            
        } catch (SQLException e) {
            s_logger.error("Failed to add source_id to usage_storage due to ", e);
            throw new CloudRuntimeException("Failed to add source_id to usage_storage due to ", e);
        }
    }
    
    private void addNetworkIdsToUserStats(Connection conn) {
    	s_logger.debug("Adding network IDs to user stats...");
        try {
        	String stmt = "SELECT DISTINCT public_ip_address FROM `cloud`.`user_statistics` WHERE public_ip_address IS NOT null";
        	PreparedStatement pstmt = conn.prepareStatement(stmt);
        	ResultSet rs = pstmt.executeQuery();
        	
        	while (rs.next()) {
        		String publicIpAddress = rs.getString(1);
        		stmt = "SELECT network_id FROM `cloud`.`user_ip_address` WHERE public_ip_address = ?";
        		pstmt = conn.prepareStatement(stmt);
        		pstmt.setString(1, publicIpAddress);
        		ResultSet rs2 = pstmt.executeQuery();
        		
        		if (rs2.next()) {
        			Long networkId = rs2.getLong(1);	
        			String[] dbs = {"cloud", "cloud_usage"};
        			for (String db : dbs) {
        				stmt = "UPDATE `" + db + "`.`user_statistics` SET network_id = ? WHERE public_ip_address = ?";
        				pstmt = conn.prepareStatement(stmt);
            			pstmt.setLong(1, networkId);
            			pstmt.setString(2, publicIpAddress);
            			pstmt.executeUpdate();
        			}        	
        		}
        		
        		rs2.close();
        	}
            
            rs.close();
            pstmt.close();
            s_logger.debug("Successfully added network IDs to user stats.");
        } catch (SQLException e) {
        	String errorMsg = "Failed to add network IDs to user stats.";
            s_logger.error(errorMsg, e);
            throw new CloudRuntimeException(errorMsg, e);
        }
    }
    
}
