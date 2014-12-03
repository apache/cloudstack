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

import org.apache.log4j.Logger;

public class Transaction {
    private final static AtomicLong counter = new AtomicLong(0);
    private final static TransactionStatus STATUS = new TransactionStatus() {
    };

    private static final Logger s_logger = Logger.getLogger(Transaction.class);

    @SuppressWarnings("deprecation")
    public static <T, E extends Throwable> T execute(TransactionCallbackWithException<T, E> callback) throws E {
        String name = "tx-" + counter.incrementAndGet();
        short databaseId = TransactionLegacy.CLOUD_DB;
        TransactionLegacy currentTxn = TransactionLegacy.currentTxn(false);
        if (currentTxn != null) {
            databaseId = currentTxn.getDatabaseId();
        }
        TransactionLegacy txn = TransactionLegacy.open(name, databaseId, false);
        try {
//            if (txn.dbTxnStarted()){
//                String warnMsg = "Potential Wrong Usage: TRANSACTION.EXECUTE IS WRAPPED INSIDE ANOTHER DB TRANSACTION!";
//                s_logger.warn(warnMsg, new CloudRuntimeException(warnMsg));
//            }
            txn.start();
            T result = callback.doInTransaction(STATUS);
            txn.commit();
            return result;
        } finally {
            txn.close();
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

}
