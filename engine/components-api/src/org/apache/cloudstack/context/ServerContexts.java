// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.
package org.apache.cloudstack.context;

import com.cloud.async.AsyncJob;
import com.cloud.utils.db.Transaction;

/**
 * ServerContextInitializer is responsible for properly setting up the
 * contexts that all of the CloudStack code expects.  This includes
 *   - CallContext
 *   - JobContext
 *   - TransactionContext
 */
public class ServerContexts {
    public static void registerUserContext(long userId, long accountId) {
        Transaction txn = Transaction.open(Thread.currentThread().getName());
        CallContext context = CallContext.register(userId, accountId);
        context.putContextParameter("Transaction", txn);
//        AsyncJobExecutionContext.registerPseudoExecutionContext(userId, accountId);
    }

    public static void unregisterUserContext() {
        CallContext context = CallContext.unregister();
        if (context != null) {
//            AsyncJobExecutionContext.unregister();
            Transaction txn = (Transaction)context.getContextParameter("Transaction");
            txn.close(Thread.currentThread().getName());
        }
    }

    /**
     * Use this method to initialize the internal background threads.
     */
    public static void registerSystemContext() {
        Transaction txn = Transaction.open(Thread.currentThread().getName());
        CallContext context = CallContext.registerSystemCallContextOnceOnly();
        context.putContextParameter("Transaction", txn);
//        AsyncJobExecutionContext.registerPseudoExecutionContext(Account.ACCOUNT_ID_SYSTEM, User.UID_SYSTEM);
    }
    
    public static void unregisterSystemContext() {
        CallContext context = CallContext.unregister();
//        AsyncJobExecutionContext.unregister();
        Transaction txn = (Transaction)context.getContextParameter("Transaction");
        txn.close(Thread.currentThread().getName());
    }

    public static void registerJobContext(long userId, long accountId, AsyncJob job) {
        CallContext.register(userId, accountId);
    }
}
