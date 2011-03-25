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

import com.cloud.capacity.Capacity;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade222to224 implements DbUpgrade {

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] {"2.2.2", "2.2.3"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.4";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-222to224.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-222to224.sql");
        }
        
        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
    	updateClusterIdInOpHostCapacity(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        String file = Script.findScript("", "db/schema-222to224-cleanup.sql");
        if (file == null) {
            throw new CloudRuntimeException("Unable to find the upgrade script, schema-222to224-cleanup.sql");
        }
        
        return new File[] {new File(file)};
    }

    private void updateClusterIdInOpHostCapacity(Connection conn){
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        PreparedStatement pstmtUpdate = null;
        try {
            //Host and Primary storage capacity types
            pstmt = conn.prepareStatement("SELECT host_id, capacity_type FROM op_host_capacity WHERE capacity_type IN (0,1,2,3)");
            rs = pstmt.executeQuery();
            while (rs.next()) {
            	long hostId = rs.getLong(1);
            	short capacityType = rs.getShort(2);
            	String updateSQLPrefix = "Update op_host_capacity set cluster_id = (select cluster_id from ";
            	String updateSQLSuffix = " where id = ? ) where host_id = ?";
            	String tableName = "host";
            	switch(capacityType){
            	case Capacity.CAPACITY_TYPE_MEMORY:
            	case Capacity.CAPACITY_TYPE_CPU:
            		tableName = "host";
            		break;
            	case Capacity.CAPACITY_TYPE_STORAGE:
            	case Capacity.CAPACITY_TYPE_STORAGE_ALLOCATED:
            		tableName = "storage_pool";
            		break;
            	}
            	pstmtUpdate = conn.prepareStatement(updateSQLPrefix + tableName + updateSQLSuffix);
            	pstmtUpdate.setLong(1, hostId);
            	pstmtUpdate.setLong(2, hostId);
            	pstmtUpdate.executeUpdate();
            	pstmtUpdate.close();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to update the cluster Ids in Op_Host_capacity table", e);
        }finally{
        	if(pstmtUpdate != null){
            	try{
            		pstmtUpdate.close();
            	}catch (SQLException e) {
                }
            }
            if(rs != null){
            	try{
            		rs.close();
            	}catch (SQLException e) {
                }
            }        	
            if(pstmt != null){
            	try{
            		pstmt.close();
            	}catch (SQLException e) {
                }
            }

        }
        
    }
}
