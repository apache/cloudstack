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

import java.util.ArrayList;
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
    public int getActiveTransactionCount() {
        int count = 0;
        for (Transaction txn : _txns.values()) {
            if (txn.getStack().size() > 0) {
                count++;
            }
        }
        return count;
    }
    
    @Override
    public List<String> getTransactions() {
        ArrayList<String> txns = new ArrayList<String>();
        for (Transaction info : _txns.values()) {
            txns.add(toString(info));
        }
        return txns;
    }
    
    @Override
    public List<String> getActiveTransactions() {
        ArrayList<String> txns = new ArrayList<String>();
        for (Transaction txn : _txns.values()) {
            if (txn.getStack().size() > 0 || txn.getCurrentConnection() != null) {
                txns.add(toString(txn));
            }
        }
        return txns;
    }
    
    protected String toString(Transaction txn) {
        StringBuilder buff = new StringBuilder("[Name=");
        buff.append(txn.getName());
        buff.append("; Creator=");
        buff.append(txn.getCreator());
        buff.append("; DB=");
        buff.append(txn.getCurrentConnection());
        buff.append("; Stack=");
        for (StackElement element : txn.getStack()) {
            buff.append(",").append(element.toString());
        }
        buff.append("]");
        
        return buff.toString();
    }
}