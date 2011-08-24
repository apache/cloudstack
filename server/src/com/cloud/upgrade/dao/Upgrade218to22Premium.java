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

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade218to22Premium extends Upgrade218to22 {
    @Override
    public File[] getPrepareScripts() {
        File[] scripts = super.getPrepareScripts();
        File[] newScripts = new File[2]; 
        newScripts[0] = scripts[0];

        String file = Script.findScript("","db/schema-21to22-premium.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-21to22-premium.sql");
        }
        
        newScripts[1] = new File(file);

        return newScripts;
    }
    
    @Override
    public void performDataMigration(Connection conn) {
        super.performDataMigration(conn);
        updateUserStats(conn);
        updateUsageIpAddress(conn);
    }
    
    @Override
    public File[] getCleanupScripts() {
        File[] scripts = super.getCleanupScripts();
        File[] newScripts = new File[1]; 
        // Change the array to 2 when you add in the scripts for premium.
        newScripts[0] = scripts[0];
        return newScripts;
    }
    
    private void updateUserStats(Connection conn) {
        try {

            // update device_id information
            PreparedStatement pstmt = conn.prepareStatement("update cloud_usage.user_statistics uus set device_id = " +
            		"(select device_id from cloud.user_statistics us where uus.id = us.id)");
            pstmt.executeUpdate();
            pstmt.close();

            s_logger.debug("Upgraded cloud_usage user_statistics with deviceId");
            
            // update host_id information in usage_network
            PreparedStatement pstmt1 = conn.prepareStatement("update cloud_usage.usage_network un set host_id = " +
                    "(select device_id from cloud_usage.user_statistics us where us.account_id = un.account_id and us.data_center_id = un.zone_id)");
            pstmt1.executeUpdate();
            pstmt1.close();

            s_logger.debug("Upgraded cloud_usage usage_network with hostId");

           
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to upgrade user stats: ", e);
        }
    }
    
    private void updateUsageIpAddress(Connection conn) {
        try {

            // update id information
            PreparedStatement pstmt = conn.prepareStatement("update cloud_usage.usage_ip_address uip set id = " +
                    "(select id from cloud.user_ip_address ip where uip.public_ip_address = ip.public_ip_address and ip.data_center_id = uip.zone_id)");
            pstmt.executeUpdate();
            pstmt.close();

            s_logger.debug("Upgraded cloud_usage usage_ip_address with Id");
            
        } catch (Exception e) {
            throw new CloudRuntimeException("Failed to upgrade usage_ip_address: ", e);
        }
    }

}
