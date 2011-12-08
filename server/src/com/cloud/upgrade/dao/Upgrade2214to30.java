/**
 * Copyright (C) 2011 Citrix Systems, Inc.  All rights reserved
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
import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.cloud.utils.crypt.DBEncryptionUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.script.Script;

public class Upgrade2214to30 implements DbUpgrade {
    final static Logger s_logger = Logger.getLogger(Upgrade2214to30.class);

    @Override
    public String[] getUpgradableVersionRange() {
        return new String[] { "2.2.14", "3.0.0"};
    }

    @Override
    public String getUpgradedVersion() {
        return "3.0.0";
    }

    @Override
    public boolean supportsRollingUpgrade() {
        return true;
    }

    @Override
    public File[] getPrepareScripts() {
        String script = Script.findScript("", "db/db/schema-2214to30.sql");
        if (script == null) {
            throw new CloudRuntimeException("Unable to find db/schema-2214to30.sql");
        }
        
        return new File[] { new File(script)};
    }

    @Override
    public void performDataMigration(Connection conn) {
    	//encrypt data
        encryptData(conn);
        //drop keys
        dropKeysIfExist(conn);
    }

    @Override
    public File[] getCleanupScripts() {
        return null;
    }
 

    private void encryptData(Connection conn) {
    	encryptConfigValues(conn);
    	encryptHostDetails(conn);
    	encryptVNCPassword(conn);
    	encryptUserCredentials(conn);
    }
    
    private void encryptConfigValues(Connection conn) {
    	PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select name, value from configuration");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                String name = rs.getString(1);
                String value = rs.getString(2);
                if(value == null){
                	continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement("update configuration set value=? where name=?");
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setString(2, name);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt configuration values");
        } catch (UnsupportedEncodingException e) {
        	throw new CloudRuntimeException("Unable encrypt configuration values");
		} finally {
            try {
                if (rs != null) {
                    rs.close(); 
                }
               
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }
    
    private void encryptHostDetails(Connection conn) {
    	PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select id, value from host_details");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if(value == null){
                	continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement("update host_details set value=? where id=?");
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt host_details values");
        } catch (UnsupportedEncodingException e) {
        	throw new CloudRuntimeException("Unable encrypt host_details values");
		} finally {
            try {
                if (rs != null) {
                    rs.close(); 
                }
               
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }
    
    private void encryptVNCPassword(Connection conn) {
    	PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select id, vnc_password from vm_instance");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String value = rs.getString(2);
                if(value == null){
                	continue;
                }
                String encryptedValue = DBEncryptionUtil.encrypt(value);
                pstmt = conn.prepareStatement("update vm_instance set vnc_password=? where id=?");
                pstmt.setBytes(1, encryptedValue.getBytes("UTF-8"));
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt vm_instance vnc_password");
        } catch (UnsupportedEncodingException e) {
        	throw new CloudRuntimeException("Unable encrypt vm_instance vnc_password");
		} finally {
            try {
                if (rs != null) {
                    rs.close(); 
                }
               
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }
    
    private void encryptUserCredentials(Connection conn) {
    	PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = conn.prepareStatement("select id, secret_key from user");
            rs = pstmt.executeQuery();
            while (rs.next()) {
                long id = rs.getLong(1);
                String secretKey = rs.getString(2);
                String encryptedSecretKey = DBEncryptionUtil.encrypt(secretKey);
                pstmt = conn.prepareStatement("update user set secret_key=? where id=?");
                if(encryptedSecretKey == null){
                	pstmt.setNull(1, Types.VARCHAR);
                } else {
                	pstmt.setBytes(1, encryptedSecretKey.getBytes("UTF-8"));
                }
                pstmt.setLong(2, id);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable encrypt user secret key");
        } catch (UnsupportedEncodingException e) {
        	throw new CloudRuntimeException("Unable encrypt user secret key");
		} finally {
            try {
                if (rs != null) {
                    rs.close(); 
                }
               
                if (pstmt != null) {
                    pstmt.close();
                }
            } catch (SQLException e) {
            }
        }
    }
    
    
    private void dropKeysIfExist(Connection conn) {
        HashMap<String, List<String>> uniqueKeys = new HashMap<String, List<String>>();
        List<String> keys = new ArrayList<String>();
        keys.add("public_ip_address");
        uniqueKeys.put("console_proxy", keys);
        uniqueKeys.put("secondary_storage_vm", keys);

        // drop keys
        s_logger.debug("Dropping public_ip_address keys from secondary_storage_vm and console_proxy tables...");
        for (String tableName : uniqueKeys.keySet()) {
            DbUpgradeUtils.dropKeysIfExist(conn, tableName, uniqueKeys.get(tableName), true);
        }
    }
}
