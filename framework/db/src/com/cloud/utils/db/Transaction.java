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

import com.cloud.utils.exception.ExceptionUtil;

public class Transaction {
    private final static AtomicLong counter = new AtomicLong(0);
    private final static TransactionStatus STATUS = new TransactionStatus() {
    };

    public static <T> T execute(TransactionCallback<T> callback) {
        String name = "tx-" + counter.incrementAndGet();
        TransactionLegacy txn = TransactionLegacy.open(name);
        try {
            txn.start();
            T result = callback.doInTransaction(STATUS);
            txn.commit();
            return result;
        } finally {
            txn.close();
        }
    }

    public static <T,X extends Exception> T executeWithException(final TransactionCallbackWithException<T> callback, Class<X> exception) throws X {
        try {
            return execute(new TransactionCallback<T>() {
                @Override
                public T doInTransaction(TransactionStatus status) {
                    try {
                        return callback.doInTransaction(status);
                    } catch (Exception e) {
                        ExceptionUtil.rethrowRuntime(e);
                        throw new TransactionWrappedExeception(e);
                    }
                }
            });
        } catch (TransactionWrappedExeception e) {
            ExceptionUtil.rethrowRuntime(e.getWrapped());
            ExceptionUtil.rethrow(e.getWrapped(), exception);
            throw e;
        }
    }

}
