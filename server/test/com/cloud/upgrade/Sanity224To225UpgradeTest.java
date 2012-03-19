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

public class Sanity224To225UpgradeTest extends TestCase {
    private static final Logger s_logger = Logger.getLogger(Sanity224To225UpgradeTest.class);

    @Override
    @Before
    public void setUp() throws Exception {
        DbTestUtils.executeScript("cleanup.sql", false, true);
    }

    @Override
    @After
    public void tearDown() throws Exception {
    }

    public void test224to225Upgrade() throws SQLException {
        s_logger.debug("Finding sample data from 2.2.4");
        DbTestUtils.executeScript("fake.sql", false, true);

        Connection conn;
        PreparedStatement pstmt;
        ResultSet rs;

        VersionDaoImpl dao = ComponentLocator.inject(VersionDaoImpl.class);
        DatabaseUpgradeChecker checker = ComponentLocator.inject(DatabaseUpgradeChecker.class);

        String version = dao.getCurrentVersion();

        if (!version.equals("2.2.4")) {
            s_logger.error("Version returned is not 2.2.4 but " + version);
        } else {
            s_logger.debug("Sanity 2.2.4 to 2.2.5 test version is " + version);
        }

        checker.upgrade("2.2.4", "2.2.5");

        conn = Transaction.getStandaloneConnection();
        try {
            s_logger.debug("Starting tesing upgrade from 2.2.4 to 2.2.5...");

            // Version check
            pstmt = conn.prepareStatement("SELECT version FROM version");
            rs = pstmt.executeQuery();

            if (!rs.next()) {
                s_logger.error("ERROR: No version selected");
            } else if (!rs.getString(1).equals("2.2.5")) {
                s_logger.error("ERROR: VERSION stored is not 2.2.5: " + rs.getString(1));
            }
            rs.close();
            pstmt.close();

            s_logger.debug("Sanity 2.2.4 to 2.2.5 DB upgrade test passed");

        } finally {
            conn.close();
        }
    }

}
