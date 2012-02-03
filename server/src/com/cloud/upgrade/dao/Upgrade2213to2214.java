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
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade2213to2214 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade2213to2214.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.2.13", "2.2.14"};
    }

    @Override
    public String getUpgradedVersion() {
        return "2.2.14";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/schema-2213to2214.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-2213to2214.sql");
        }
        
        return new File[] { new File(script) };
    }

    @Override
    public void performDataMigration(Connection conn) {
        fixIndexes(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
 

    private void fixIndexes(Connection conn) {
    	 //Drop i_usage_event__created key (if exists) and re-add it again
    	List<String> keys = new ArrayList<String>();
    	keys.add("i_usage_event__created");
    	DbUpgradeUtils.dropKeysIfExist(conn, "usage_event", keys, false);
    	try {
    	    PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`usage_event` ADD INDEX `i_usage_event__created`(`created`)");
    	    pstmt.executeUpdate();
    	    pstmt.close();
    	} catch (SQLException e) {
    	    throw new CloudRuntimeException("Unable to execute usage_event table update", e);
    	}

		if (DbUpgradeUtils.dropKeysIfExistAndReturnValue(conn, "ssh_keypairs", "fk_ssh_keypair__account_id", false)) {
			PreparedStatement pstmt;
            try {
	            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD KEY `fk_ssh_keypairs__account_id`(`account_id`)");
	            pstmt.executeUpdate();
				pstmt.close();
            } catch (SQLException e) {
            	throw new CloudRuntimeException("Unable to execute ssh_keypairs table update", e);
            }
			
		}
		
		if (DbUpgradeUtils.dropKeysIfExistAndReturnValue(conn, "ssh_keypairs", "fk_ssh_keypair__domain_id", false)) {
			PreparedStatement pstmt;
            try {
	            pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`ssh_keypairs` ADD KEY `fk_ssh_keypairs__domain_id`(`domain_id`)");
	            pstmt.executeUpdate();
				pstmt.close();
            } catch (SQLException e) {
            	throw new CloudRuntimeException("Unable to execute ssh_keypairs table update", e);
            }
			
		}
		
    	//In cloud_usage DB, drop i_usage_event__created key (if exists) and re-add it again
    	keys = new ArrayList<String>();
    	keys.add("i_usage_event__created");
    	DbUpgradeUtils.dropKeysIfExist(conn, "cloud_usage.usage_event", keys, false);
    	try {
    	    PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud_usage`.`usage_event` ADD INDEX `i_usage_event__created`(`created`)");
    	    pstmt.executeUpdate();
    	    pstmt.close();
    	} catch (SQLException e) {
    	    throw new CloudRuntimeException("Unable to execute cloud_usage usage_event table update", e);
    	}
    	
    	//Drop i_snapshots__removed key (if exists) and re-add it again
    	keys = new ArrayList<String>();
    	keys.add("i_snapshots__removed");
    	DbUpgradeUtils.dropKeysIfExist(conn, "cloud.snapshots", keys, false);
    	try {
    	    PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`snapshots` ADD INDEX `i_snapshots__removed`(`removed`)");
    	    pstmt.executeUpdate();
    	    pstmt.close();
    	} catch (SQLException e) {
    	    throw new CloudRuntimeException("Unable to insert index for removed column in snapshots", e);
    	}
        //Drop i_op_vm_ruleset_log__instance_id, u_op_vm_ruleset_log__instance_id key (if exists) and re-add u_op_vm_ruleset_log__instance_id again
        keys = new ArrayList<String>();
        keys.add("i_op_vm_ruleset_log__instance_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.op_vm_ruleset_log", keys, false);
        
        keys = new ArrayList<String>();
        keys.add("u_op_vm_ruleset_log__instance_id");
        DbUpgradeUtils.dropKeysIfExist(conn, "cloud.op_vm_ruleset_log", keys, false);
        try {
            PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`op_vm_ruleset_log` ADD CONSTRAINT `u_op_vm_ruleset_log__instance_id` UNIQUE (`instance_id`)");
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to execute changes for op_vm_ruleset_log", e);
        }  
        
    	//Drop netapp_volume primary key and add it again
    	DbUpgradeUtils.dropPrimaryKeyIfExists(conn, "cloud.netapp_volume");
    	try {
    	    PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`netapp_volume` add PRIMARY KEY (`id`)");
    	    pstmt.executeUpdate();
    	    pstmt.close();
    	} catch (SQLException e) {
    	    throw new CloudRuntimeException("Unable to update primary key for netapp_volume", e);
    	}

    	//Drop i_async__removed, i_async_job__removed  (if exists) and add i_async_job__removed 
    	keys = new ArrayList<String>();
    	keys.add("i_async__removed");
    	keys.add("i_async_job__removed");
    	DbUpgradeUtils.dropKeysIfExist(conn, "cloud.async_job", keys, false);
    	try {
    	    PreparedStatement pstmt = conn.prepareStatement("ALTER TABLE `cloud`.`async_job` ADD INDEX `i_async_job__removed`(`removed`)");
    	    pstmt.executeUpdate();
    	    pstmt.close();
    	} catch (SQLException e) {
    	    throw new CloudRuntimeException("Unable to insert index for removed column in async_job", e);
    	}
    
    }
}
