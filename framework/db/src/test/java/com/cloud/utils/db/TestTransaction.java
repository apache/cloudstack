/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.cloud.utils.db;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.FileNotFoundException;
import java.sql.Connection;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class TestTransaction {

    TransactionLegacy txn;
    Connection conn;

    @Before
    public void setup() {
        setup(TransactionLegacy.CLOUD_DB);
    }

    public void setup(short db) {
        txn = TransactionLegacy.open(db);
        conn = Mockito.mock(Connection.class);
        txn.setConnection(conn);
    }

    @After
    public void after() {
        TransactionLegacy.currentTxn().close();
    }

    @Test
    public void testCommit() throws Exception {
        assertEquals(42L, Transaction.execute(new TransactionCallback<Object>() {
            @Override
            public Object doInTransaction(TransactionStatus status) {
                return 42L;
            }
        }));

        verify(conn).setAutoCommit(false);
        verify(conn, times(1)).commit();
        verify(conn, times(0)).rollback();
        verify(conn, times(1)).close();
    }

    @Test
    public void testRollback() throws Exception {
        try {
            Transaction.execute(new TransactionCallback<Object>() {
                @Override
                public Object doInTransaction(TransactionStatus status) {
                    throw new RuntimeException("Panic!");
                }
            });
            fail();
        } catch (RuntimeException e) {
            assertEquals("Panic!", e.getMessage());
        }

        verify(conn).setAutoCommit(false);
        verify(conn, times(0)).commit();
        verify(conn, times(1)).rollback();
        verify(conn, times(1)).close();
    }

    @Test
    public void testRollbackWithException() throws Exception {
        try {
            Transaction.execute(new TransactionCallbackWithException<Object, FileNotFoundException>() {
                @Override
                public Object doInTransaction(TransactionStatus status) throws FileNotFoundException {
                    assertEquals(TransactionLegacy.CLOUD_DB, TransactionLegacy.currentTxn().getDatabaseId().shortValue());

                    throw new FileNotFoundException("Panic!");
                }
            });
            fail();
        } catch (FileNotFoundException e) {
            assertEquals("Panic!", e.getMessage());
        }

        verify(conn).setAutoCommit(false);
        verify(conn, times(0)).commit();
        verify(conn, times(1)).rollback();
        verify(conn, times(1)).close();
    }

    @Test
    public void testWithExceptionNoReturn() throws Exception {
        final AtomicInteger i = new AtomicInteger(0);
        assertTrue(Transaction.execute(new TransactionCallbackWithExceptionNoReturn<FileNotFoundException>() {
            @Override
            public void doInTransactionWithoutResult(TransactionStatus status) throws FileNotFoundException {
                i.incrementAndGet();
            }
        }));

        assertEquals(1, i.get());
        verify(conn).setAutoCommit(false);
        verify(conn, times(1)).commit();
        verify(conn, times(0)).rollback();
        verify(conn, times(1)).close();
    }
}
