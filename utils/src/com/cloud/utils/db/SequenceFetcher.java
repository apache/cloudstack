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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.persistence.TableGenerator;

import org.apache.log4j.Logger;

import com.cloud.utils.concurrency.NamedThreadFactory;

/**
 * Since Mysql does not have sequence support, we have
 * to use this to create that support because if the
 * table retrieval was inside a transaction, the value
 * gets locked until the transaction is over.
 * 
 * TODO: enhance this so that it actually follows the
 * allocation size.
 *
 */
public class SequenceFetcher {
    private final static Logger s_logger = Logger.getLogger(SequenceFetcher.class);
    ExecutorService _executors;
    
    public <T> T getNextSequence(Class<T> clazz, TableGenerator tg) {
        return getNextSequence(clazz, tg, null, false);
    }
    
    public <T> T getNextSequence(Class<T> clazz, TableGenerator tg, Object key) {
    	return getNextSequence(clazz, tg, key, false);
    }
    
    public <T> T getRandomNextSequence(Class<T> clazz, TableGenerator tg) {
        return getNextSequence(clazz, tg, null, true);
    }
    
    public <T> T getNextSequence(Class<T> clazz, TableGenerator tg, Object key, boolean isRandom) {
        Future<T> future = _executors.submit(new Fetcher<T>(clazz, tg, key, isRandom));
        try {
            return future.get();
        } catch (Exception e) {
            s_logger.warn("Unable to get sequeunce for " + tg.table() + ":" + tg.pkColumnValue(), e);
            return null;
        }
    }
    
    protected SequenceFetcher() {
        _executors = new ThreadPoolExecutor(100, 100, 120l, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(250), new NamedThreadFactory("SequenceFetcher"));
    }

    protected static final SequenceFetcher s_instance = new SequenceFetcher();
    public static SequenceFetcher getInstance() {
        return s_instance;
    }
    
    protected class Fetcher<T> implements Callable<T> {
        TableGenerator _tg;
        Class<T> _clazz;
        Object _key;
        boolean isRandom = false;
        
        protected Fetcher(Class<T> clazz, TableGenerator tg, Object key, boolean isRandom) {
            _tg = tg;
            _clazz = clazz;
            _key = key;
            this.isRandom = isRandom;
        }
        
        @Override @SuppressWarnings("unchecked")
        public T call() throws Exception {
            try {
                PreparedStatement stmt = null;
                StringBuilder sql = new StringBuilder("SELECT ");
                sql.append(_tg.valueColumnName()).append(" FROM ").append(_tg.table());
                sql.append(" WHERE ").append(_tg.pkColumnName()).append(" = ? FOR UPDATE");
                
                Transaction txn = Transaction.open("Sequence");
                
                PreparedStatement selectStmt = txn.prepareStatement(sql.toString());
                if (_key == null) {
                    selectStmt.setString(1, _tg.pkColumnValue());
                } else {
                    selectStmt.setObject(1, _key);
                }

                sql = new StringBuilder("UPDATE ");
                sql.append(_tg.table()).append(" SET ").append(_tg.valueColumnName()).append("=").append("?+?");
                sql.append(" WHERE ").append(_tg.pkColumnName()).append("=?");
                
                PreparedStatement updateStmt = txn.prepareStatement(sql.toString());
                if(isRandom){
                	Random random = new Random();
                	updateStmt.setInt(2, random.nextInt(10));
                } else {
                	updateStmt.setInt(2, _tg.allocationSize());
                }
                if (_key == null) {
                    updateStmt.setString(3, _tg.pkColumnValue());
                } else {
                    updateStmt.setObject(3, _key);
                }
                
                ResultSet rs = null;
                try {
                    txn.start();
                    
                    stmt = selectStmt;
                    rs = stmt.executeQuery();
                    Object obj = null;
                    while (rs.next()) {
                        if (_clazz.isAssignableFrom(Long.class)) {
                            obj = rs.getLong(1);
                        } else if (_clazz.isAssignableFrom(Integer.class)) {
                            obj = rs.getInt(1);
                        } else {
                            obj = rs.getObject(1);
                        }
                    }
                    
                    if (obj == null) {
                        s_logger.warn("Unable to get a sequence: " + updateStmt.toString());
                        return null;
                    }
                    
                    updateStmt.setObject(1, obj);
                    stmt = updateStmt;
                    int rows = stmt.executeUpdate();
                    assert rows == 1 : "Come on....how exactly did we update this many rows " + rows + " for " + updateStmt.toString();
                    txn.commit();
                    return (T)obj;
                } catch (SQLException e) {
                    s_logger.warn("Caught this exception when running: " + (stmt != null ? stmt.toString() : ""), e);
                } finally {
                    if (rs != null) {
                        rs.close();
                    }
                    selectStmt.close();
                    updateStmt.close();
                    txn.close();
                }
            } catch (Exception e) {
                s_logger.warn("Caught this exception when running.", e);
            }
            return null;
        }
    }
    
}
