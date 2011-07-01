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
package com.cloud.utils.db;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.StandardMBean;

import com.cloud.utils.db.Transaction.StackElement;

public class TransactionMBeanImpl extends StandardMBean implements TransactionMBean {
    
    Map<Long, Transaction> _txns = new ConcurrentHashMap<Long, Transaction>();
    
    public TransactionMBeanImpl() {
        super(TransactionMBean.class, false);
    }
    
    public void addTransaction(Transaction txn) {
        _txns.put(txn.getId(), txn);
    }
    
    public void removeTransaction(Transaction txn) {
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
        for (Transaction txn : _txns.values()) {
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
        for (Transaction info : _txns.values()) {
            txns.add(toMap(info));
        }
        return txns;
    }
    
    @Override
    public List<Map<String, String>> getActiveTransactions() {
        ArrayList<Map<String, String>> txns = new ArrayList<Map<String, String>>();
        for (Transaction txn : _txns.values()) {
            if (txn.getStack().size() > 0 || txn.getCurrentConnection() != null) {
                txns.add(toMap(txn));
            }
        }
        return txns;
    }
    
    protected Map<String, String> toMap(Transaction txn) {
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
        for (Transaction txn : _txns.values()) {
            if (txn.getCurrentConnection() != null) {
                txns.add(toMap(txn));
            }
        }
        return txns;
    }
}