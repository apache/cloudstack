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

import java.util.concurrent.atomic.AtomicLong;


public class Transaction {
    private final static AtomicLong counter = new AtomicLong(0);
    private final static TransactionStatus STATUS = new TransactionStatus() {
    };


    @SuppressWarnings("deprecation")
    public static <T, E extends Throwable> T execute(TransactionCallbackWithException<T, E> callback) throws E {
        String name = "tx-" + counter.incrementAndGet();
        short databaseId = TransactionLegacy.CLOUD_DB;
        TransactionLegacy currentTxn = TransactionLegacy.currentTxn(false);
        if (currentTxn != null) {
            databaseId = currentTxn.getDatabaseId();
        }
        try (final TransactionLegacy txn = TransactionLegacy.open(name, databaseId, false)) {
            txn.start();
            T result = callback.doInTransaction(STATUS);
            txn.commit();
            return result;
        }
    }

    public static <T> T execute(final TransactionCallback<T> callback) {
        return execute(new TransactionCallbackWithException<T, RuntimeException>() {
            @Override
            public T doInTransaction(TransactionStatus status) throws RuntimeException {
                return callback.doInTransaction(status);
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static <T, E extends Throwable> T execute(final short databaseId, TransactionCallbackWithException<T, E> callback) throws E {
        String name = "tx-" + counter.incrementAndGet();
        TransactionLegacy currentTxn = TransactionLegacy.currentTxn(false);
        short outer_txn_databaseId = (currentTxn != null ? currentTxn.getDatabaseId() : databaseId);
        try (final TransactionLegacy txn = TransactionLegacy.open(name, databaseId, true)) {
            txn.start();
            T result = callback.doInTransaction(STATUS);
            txn.commit();
            return result;
        } finally {
            TransactionLegacy.open(outer_txn_databaseId).close();
        }
    }

    public static <T> T execute(final short databaseId, final TransactionCallback<T> callback) {
        return execute(databaseId, new TransactionCallbackWithException<T, RuntimeException>() {
            @Override
            public T doInTransaction(TransactionStatus status) throws RuntimeException {
                return callback.doInTransaction(status);
            }
        });
    }

}
