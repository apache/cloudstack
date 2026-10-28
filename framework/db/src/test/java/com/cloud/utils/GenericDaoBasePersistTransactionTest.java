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
package com.cloud.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Statement;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityExistsException;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.TransactionLegacy;

// Kept outside com.cloud.utils.db because framework/db excludes that package's tests from the build.
@RunWith(MockitoJUnitRunner.class)
public class GenericDaoBasePersistTransactionTest {

    private static final String TXN_NAME = "GenericDaoBasePersistTransactionTest";

    @Mock
    private Connection conn;
    @Mock
    private PreparedStatement insert;
    @Mock
    private PreparedStatement select;
    @Mock
    private ResultSet noRows;
    @Mock
    private Savepoint savepoint;

    private final PersistTestDao dao = new PersistTestDao();
    private TransactionLegacy txn;

    @Before
    public void setUp() {
        txn = TransactionLegacy.open(TXN_NAME);
        txn.transitToUserManagedConnection(conn);
    }

    @After
    public void tearDown() {
        // A failing test can leave the thread's transaction open, which would leak into the next test
        while (txn.dbTxnStarted()) {
            txn.rollback();
        }
        txn.close(TXN_NAME);
        txn.transitToAutoManagedConnection(TransactionLegacy.CLOUD_DB);
    }

    private void insertSucceeds() throws SQLException {
        Mockito.when(conn.prepareStatement(ArgumentMatchers.anyString(), ArgumentMatchers.eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(insert);
        Mockito.when(conn.prepareStatement(ArgumentMatchers.anyString())).thenReturn(select);
        Mockito.when(select.executeQuery()).thenReturn(noRows);
    }

    private void insertFailsWithDuplicateEntry() throws SQLException {
        Mockito.when(conn.prepareStatement(ArgumentMatchers.anyString(), ArgumentMatchers.eq(Statement.RETURN_GENERATED_KEYS))).thenReturn(insert);
        Mockito.when(insert.executeUpdate()).thenThrow(new SQLException("Duplicate entry", "23000", 1062));
    }

    @Test
    public void failedPersistInsideCallerTransactionOnlyRollsBackToItsSavepoint() throws SQLException {
        Mockito.when(conn.setSavepoint()).thenReturn(savepoint);
        insertFailsWithDuplicateEntry();
        txn.start();

        Assert.assertThrows(EntityExistsException.class, () -> dao.persist(new PersistTestVO()));

        Mockito.verify(conn).rollback(savepoint);
        Mockito.verify(conn, Mockito.never()).rollback();
        Assert.assertTrue(txn.dbTxnStarted());
        Assert.assertTrue(txn.commit());
        Mockito.verify(conn).commit();
    }

    @Test
    public void failedPersistOutsideATransactionRollsBackItsOwnTransaction() throws SQLException {
        insertFailsWithDuplicateEntry();

        Assert.assertThrows(EntityExistsException.class, () -> dao.persist(new PersistTestVO()));

        Mockito.verify(conn).rollback();
        Mockito.verify(conn, Mockito.never()).setSavepoint();
        Assert.assertFalse(txn.dbTxnStarted());
    }

    @Test
    public void persistInsideCallerTransactionReleasesItsSavepointAndLeavesTheCommitToTheCaller() throws SQLException {
        Mockito.when(conn.setSavepoint()).thenReturn(savepoint);
        insertSucceeds();
        txn.start();

        dao.persist(new PersistTestVO());

        Mockito.verify(conn).releaseSavepoint(savepoint);
        Mockito.verify(conn, Mockito.never()).commit();
        Assert.assertTrue(txn.commit());
        Mockito.verify(conn).commit();
    }

    @Test
    public void persistOutsideATransactionCommitsWithoutASavepoint() throws SQLException {
        insertSucceeds();

        dao.persist(new PersistTestVO());

        Mockito.verify(conn).commit();
        Mockito.verify(conn, Mockito.never()).setSavepoint();
        Assert.assertFalse(txn.dbTxnStarted());
    }

    @Test
    public void failedSetSavepointLeavesTheCallerTransactionIntact() throws SQLException {
        Mockito.when(conn.setSavepoint()).thenThrow(new SQLException("Connection lost"));
        txn.start();

        Assert.assertThrows(SQLException.class, () -> txn.setSavepoint());

        Assert.assertTrue(txn.commit());
        Mockito.verify(conn).commit();
    }

    @Test
    public void failedReleaseSavepointLeavesItAvailableForRollback() throws SQLException {
        Mockito.when(conn.setSavepoint()).thenReturn(savepoint);
        Mockito.doThrow(new SQLException("Connection lost")).when(conn).releaseSavepoint(savepoint);
        txn.start();
        final Savepoint sp = txn.setSavepoint();

        Assert.assertThrows(SQLException.class, () -> txn.releaseSavepoint(sp));
        txn.rollback(sp);

        Mockito.verify(conn).rollback(savepoint);
        Assert.assertTrue(txn.commit());
        Mockito.verify(conn).commit();
    }

    @Entity
    @Table(name = "persist_test")
    public static class PersistTestVO {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Column(name = "id")
        private long id;

        @Column(name = "name")
        private String name;

        public PersistTestVO() {
        }
    }

    public static class PersistTestDao extends GenericDaoBase<PersistTestVO, Long> {
    }
}
