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

public class Usage217To224UpgradeTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(Usage217To224UpgradeTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        DbTestUtils.executeScript("cleanup.sql", false, true);
        DbTestUtils.executeUsageScript("cleanup.sql", false, true);
    }
    
    @Override
    @After
    public void tearDown() throws Exception {
    }
    
    public void test21to22Upgrade() throws SQLException {
        s_logger.debug("Finding sample data from 2.1.7");
        DbTestUtils.executeScript("fake.sql", false, true);
        DbTestUtils.executeUsageScript("fake.sql", false, true);
        
        Connection conn;
        PreparedStatement pstmt;
        
        VersionDaoImpl dao = ComponentLocator.inject(VersionDaoImpl.class);
        PremiumDatabaseUpgradeChecker checker = ComponentLocator.inject(PremiumDatabaseUpgradeChecker.class);
        
        String version = dao.getCurrentVersion();
        assert version.equals("2.1.7") : "Version returned is not 2.1.7 but " + version;
        
        checker.upgrade("2.1.7", "2.2.4");
        
        conn = Transaction.getStandaloneConnection();
        try {
            pstmt = conn.prepareStatement("SELECT version FROM version ORDER BY id DESC LIMIT 1");
            ResultSet rs = pstmt.executeQuery();
            assert rs.next() : "No version selected";
            assert rs.getString(1).equals("2.2.4") : "VERSION stored is not 2.2.4: " + rs.getString(1);
            rs.close();
            pstmt.close();
            
            pstmt = conn.prepareStatement("SELECT COUNT(*) FROM usage_event");
            rs = pstmt.executeQuery();
            assert rs.next() : "Unable to get the count of usage events";
            assert (rs.getInt(1) == 182) : "Didn't find 182 usage events but found " + rs.getInt(1);
            rs.close();
            pstmt.close();
            
        } finally {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }
    }
    
}
