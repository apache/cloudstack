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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.StandardMBean;

import com.cloud.utils.db.TransactionLegacy.StackElement;

public class TransactionMBeanImpl extends StandardMBean implements TransactionMBean {

    Map<Long, TransactionLegacy> _txns = new ConcurrentHashMap<Long, TransactionLegacy>();

    public TransactionMBeanImpl() {
        super(TransactionMBean.class, false);
    }

    public void addTransaction(TransactionLegacy txn) {
        _txns.put(txn.getId(), txn);
    }

    public void removeTransaction(TransactionLegacy txn) {
        _txns.remove(txn.getId());
    }

    @Override
    public int getTransactionCount() {
        return _txns.size();
    }

    @Override
    public int[] getActiveTransactionCount() {
        int[] count = new int[2];
        count[0] = 0;
        count[1] = 0;
        for (TransactionLegacy txn : _txns.values()) {
            if (txn.getStack().size() > 0) {
                count[0]++;
            }
            if (txn.getCurrentConnection() != null) {
                count[1]++;
            }
        }
        return count;
    }

    @Override
    public List<Map<String, String>> getTransactions() {
        ArrayList<Map<String, String>> txns = new ArrayList<Map<String, String>>();
        for (TransactionLegacy info : _txns.values()) {
            txns.add(toMap(info));
        }
        return txns;
    }

    @Override
    public List<Map<String, String>> getActiveTransactions() {
        ArrayList<Map<String, String>> txns = new ArrayList<Map<String, String>>();
        for (TransactionLegacy txn : _txns.values()) {
            if (txn.getStack().size() > 0 || txn.getCurrentConnection() != null) {
                txns.add(toMap(txn));
            }
        }
        return txns;
    }

    protected Map<String, String> toMap(TransactionLegacy txn) {
        Map<String, String> map = new HashMap<String, String>();
        map.put("name", txn.getName());
        map.put("id", Long.toString(txn.getId()));
        map.put("creator", txn.getCreator());
        Connection conn = txn.getCurrentConnection();
        map.put("db", conn != null ? Integer.toString(System.identityHashCode(conn)) : "none");
        StringBuilder buff = new StringBuilder();
        for (StackElement element : txn.getStack()) {
            buff.append(element.toString()).append(",");
        }
        map.put("stack", buff.toString());

        return map;
    }

    @Override
    public List<Map<String, String>> getTransactionsWithDatabaseConnection() {
        ArrayList<Map<String, String>> txns = new ArrayList<Map<String, String>>();
        for (TransactionLegacy txn : _txns.values()) {
            if (txn.getCurrentConnection() != null) {
                txns.add(toMap(txn));
            }
        }
        return txns;
    }
}
