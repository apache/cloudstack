
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
package com.cloud.upgrade;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Before;

import com.cloud.upgrade.dao.VersionDaoImpl;
import com.cloud.utils.component.ComponentLocator;
import com.cloud.utils.db.DbTestUtils;
import com.cloud.utils.db.Transaction;

public class PortForwarding218To224UpgradeTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(PortForwarding218To224UpgradeTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        DbTestUtils.executeScript("cleanup.sql", false, true);
    }
    
    @Override
    @After
    public void tearDown() throws Exception {
    }
    
    public void test217to22Upgrade() throws SQLException {
        s_logger.debug("Finding sample data from 2.1.8");
        DbTestUtils.executeScript("fake.sql", false, true);
        
        Connection conn;
        PreparedStatement pstmt;
        ResultSet rs;
        
        VersionDaoImpl dao = ComponentLocator.inject(VersionDaoImpl.class);
        DatabaseUpgradeChecker checker = ComponentLocator.inject(DatabaseUpgradeChecker.class);
        
        String version = dao.getCurrentVersion();
        
        if (!version.equals("2.1.8")) {
            s_logger.error("Version returned is not 2.1.8 but " + version);
        } else {
            s_logger.debug("Port forwarding test version is " + version);
        }
       
        
        Long count21 = 0L;
        conn = Transaction.getStandaloneConnection();
        try {
            //Check that correct number of port forwarding rules were created
            pstmt = conn.prepareStatement("SELECT COUNT(*) FROM ip_forwarding WHERE forwarding=1");
            rs = pstmt.executeQuery();
            
            while (rs.next()) {
                count21 = rs.getLong(1);
            }
    
            rs.close();
            pstmt.close();
        } finally {
            conn.close();
        }
        
        checker.upgrade("2.1.8", "2.2.4");
        
        conn = Transaction.getStandaloneConnection();
        try {
            s_logger.debug("Starting tesing upgrade from 2.1.8 to 2.2.4 for Port forwarding rules...");
            
            //Version check
            pstmt = conn.prepareStatement("SELECT version FROM version");
            rs = pstmt.executeQuery();
            
            if (!rs.next()) {
                s_logger.error("ERROR: No version selected");
            } else if (!rs.getString(1).equals("2.2.4")) {
                s_logger.error("ERROR: VERSION stored is not 2.2.4: " + rs.getString(1));
            }
            rs.close();
            pstmt.close();
            
            
            Long count22 = 0L;
            pstmt = conn.prepareStatement("SELECT COUNT(*) FROM port_forwarding_rules");
            rs = pstmt.executeQuery();
            
            if (rs.next()) {
                count22 = rs.getLong(1);
            }

            rs.close();
            pstmt.close();
            
            if (count21.longValue() != count22.longValue()) {
                s_logger.error("ERROR: port forwarding rules were updated incorrectly. Have " + count21 + " rulrs in ip_forwarding table branch 21, and " + count22 + " in port_forwarding_rules table branch 22. Stopping the test");
                System.exit(2);
            }
            
            s_logger.debug("Port forwarding rules test is passed");
            
        } finally {
            conn.close();
        }
    }
    
}

