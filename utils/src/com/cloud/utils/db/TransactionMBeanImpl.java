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
import java.util.List;

import javax.management.StandardMBean;

import com.cloud.utils.db.Transaction.StackElement;

public class TransactionMBeanImpl extends StandardMBean implements TransactionMBean {
    Transaction _txn = null;
    String _threadName = null;
    
    public TransactionMBeanImpl(Transaction txn) {
        super(TransactionMBean.class, false);
        _txn = txn;
        _threadName = Thread.currentThread().getName();
    }
    
    @Override
    public String getThreadName() {
        return _threadName;
    }
    
    @Override
    public List<String> getStack() {
        ArrayList<StackElement> elements = new ArrayList<StackElement>(_txn.getStack());
        ArrayList<String> stack = new ArrayList<String>(elements.size());
        
        for (StackElement element : elements) {
            stack.add(element.toString());
        }
        
        return stack;
    }
    
    @Override
    public String getDbConnection() {
        Connection conn = _txn.getCurrentConnection();
        
        return (conn != null) ? conn.toString() : "No DB connection";
    }
    

    @Override
    public String getName() {
        return _txn.getName();
    }
    
}