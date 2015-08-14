// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package com.cloud.utils.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.cloud.utils.component.ComponentContext;
import com.cloud.utils.exception.CloudRuntimeException;

/**
 * A test fixture to test APIs or bugs found for Transaction class. This test fixture will do one time setup before
 * all its testcases to set up a test db table, and then tear down these test db artifacts after all testcases are run.
 *
 */
public class TransactionTest {

    @BeforeClass
    public static void oneTimeSetup() {
        try (
                Connection conn = TransactionLegacy.getStandaloneConnection();
                PreparedStatement pstmt =
                    conn.prepareStatement("CREATE TABLE `cloud`.`test` (" + "`id` bigint unsigned NOT NULL UNIQUE AUTO_INCREMENT," + "`fld_int` int unsigned,"
                        + "`fld_long` bigint unsigned," + "`fld_string` varchar(255)," + "PRIMARY KEY (`id`)" + ") ENGINE=InnoDB DEFAULT CHARSET=utf8;");
            ) {

            pstmt.execute();

        } catch (SQLException e) {
            throw new CloudRuntimeException("Problem with sql", e);
        }
    }

    @Test
    /**
     * When a transaction is set to use user-managed db connection, for each following db statement, we should see
     * that the same db connection is reused rather than acquiring a new one each time in typical transaction model.
     */
    public void testUserManagedConnection() {
        DbTestDao testDao = ComponentContext.inject(DbTestDao.class);
        TransactionLegacy txn = TransactionLegacy.open("SingleConnectionThread");
        Connection conn = null;
        try {
            conn = TransactionLegacy.getStandaloneConnectionWithException();
            txn.transitToUserManagedConnection(conn);
            // try two SQLs to make sure that they are using the same connection
            // acquired above.
            testDao.create(1, 1, "Record 1");
            Connection checkConn = TransactionLegacy.currentTxn().getConnection();
            if (checkConn != conn) {
                Assert.fail("A new db connection is acquired instead of using old one after create sql");
            }
            testDao.update(2, 2, "Record 1");
            Connection checkConn2 = TransactionLegacy.currentTxn().getConnection();
            if (checkConn2 != conn) {
                Assert.fail("A new db connection is acquired instead of using old one after update sql");
            }
        } catch (SQLException e) {
            Assert.fail(e.getMessage());
        } finally {
            txn.transitToAutoManagedConnection(TransactionLegacy.CLOUD_DB);
            txn.close();

            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    throw new CloudRuntimeException("Problem with close db connection", e);
                }
            }
        }
    }

    @Test
    /**
     * This test is simulating ClusterHeartBeat process, where the same transaction and db connection is reused.
     */
    public void testTransactionReuse() {
        DbTestDao testDao = ComponentContext.inject(DbTestDao.class);
        // acquire a db connection and keep it
        Connection conn = null;
        try {
            conn = TransactionLegacy.getStandaloneConnectionWithException();
        } catch (SQLException ex) {
            throw new CloudRuntimeException("Problem with getting db connection", ex);
        }

        // start heartbeat loop, make sure that each loop still use the same
        // connection
        TransactionLegacy txn = null;
        for (int i = 0; i < 3; i++) {
            txn = TransactionLegacy.open("HeartbeatSimulator");
            try {

                txn.transitToUserManagedConnection(conn);
                testDao.create(i, i, "Record " + i);
                Connection checkConn = TransactionLegacy.currentTxn().getConnection();
                if (checkConn != conn) {
                    Assert.fail("A new db connection is acquired instead of using old one in loop " + i);
                }
            } catch (SQLException e) {
                Assert.fail(e.getMessage());
            } finally {
                txn.transitToAutoManagedConnection(TransactionLegacy.CLOUD_DB);
                txn.close();
            }
        }
        // close the connection once we are done since we are managing db
        // connection ourselves.
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                throw new CloudRuntimeException("Problem with close db connection", e);
            }
        }
    }

    @After
    /**
     * Delete all records after each test, but table is still kept
     */
    public void tearDown() {
        try (
                Connection conn = TransactionLegacy.getStandaloneConnection();
                PreparedStatement pstmt = conn.prepareStatement("truncate table `cloud`.`test`");
            ) {
            pstmt.execute();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Problem with sql", e);
        }
    }

    @AfterClass
    public static void oneTimeTearDown() {
        try (
                Connection conn = TransactionLegacy.getStandaloneConnection();
                PreparedStatement pstmt = conn.prepareStatement("DROP TABLE IF EXISTS `cloud`.`test`");
            ) {
            pstmt.execute();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Problem with sql", e);
        }
    }
}
