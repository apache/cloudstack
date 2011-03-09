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


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import javax.naming.ConfigurationException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.cloud.upgrade.dao.VersionVO.Step;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DbTestUtils;
import com.cloud.utils.db.Transaction;
import com.cloud.utils.exception.CloudRuntimeException;

public class AdvanceZone217To221UpgradeTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(AdvanceZone217To221UpgradeTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        VersionVO version = new VersionVO("2.1.7");
        version.setStep(Step.Cleanup);
        DbTestUtils.executeScript("VersionDaoImplTest/clean-db.sql", false, true);
    }
    
    @Override
    @After
    public void tearDown() throws Exception {
    }
    
    public void test217to22Upgrade() {
        s_logger.debug("Finding sample data from 2.1.7");
        DbTestUtils.executeScript("VersionDaoImplTest/2.1.7/2.1.7.sample.sql", false, true);
        
        Connection conn = Transaction.getStandaloneConnection();
        PreparedStatement pstmt;
//        try {
//            pstmt = conn.prepareStatement("UPDATE configuration set value='true' WHERE name = 'direct.attach.untagged.vlan.enabled'");
//            pstmt.executeUpdate();
//            pstmt.close();
//        } catch(SQLException e) {
//            
//        } finally {
//            try {
//                conn.close();
//            } catch(SQLException e) {
//                
//            }
//        }
        
        VersionDaoImpl dao = ComponentLocator.inject(VersionDaoImpl.class);
        
        String version = dao.getCurrentVersion();
        assert version.equals("2.1.7") : "Version returned is not 2.1.7 but " + version;
        
        try {
            dao.upgrade("2.1.7", "2.2.3");
        } catch (ConfigurationException e) {
            s_logger.warn("Exception: ", e);
            assert false : "The test failed.  Check logs"; 
        }
        
        conn = Transaction.getStandaloneConnection();
        try {
            pstmt = conn.prepareStatement("SELECT version FROm version");
            ResultSet rs = pstmt.executeQuery();
            assert rs.next() : "No version selected";
            assert rs.getString(1).equals("2.2.1") : "VERSION stored is not 2.2.1: " + rs.getString(1);
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement("SELECT COUNT(*) FROM network_offerings");
            rs = pstmt.executeQuery();
            assert rs.next() : "Unable to get the count of network offerings.";
            assert (rs.getInt(1) == 7) : "Didn't find 7 network offerings but found " + rs.getInt(1);
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement("SELECT DISTINCT networktype FROM data_center");
            rs = pstmt.executeQuery();
            assert rs.next()  && rs.getString(1).equals("Advanced") : "Network type is not advanced? " + rs.getString(1);
            assert !rs.next() : "Why do we have another one? " + rs.getString(1);
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement("SELECT COUNT(*) FROM disk_offering WHERE removed IS NULL AND system_used=1 AND type='Service' AND recreatable=1");
            rs = pstmt.executeQuery();
            assert (rs.next() && rs.getInt(1) == 3) : "DiskOffering for system VMs are incorrect.  Expecting 3 but got " + rs.getInt(1);
            rs.close();
            pstmt.close();
            
            
        } catch (SQLException e) {
            throw new CloudRuntimeException("Problem checking upgrade version", e);
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }
    }
    
}
