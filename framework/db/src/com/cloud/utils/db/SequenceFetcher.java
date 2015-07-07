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
 * table retrieval was inside a transaction, the value
 * gets locked until the transaction is over.
 *
 * allocation size.
 *
 */
public class SequenceFetcher {
    private final static Logger s_logger = Logger.getLogger(SequenceFetcher.class);
    ExecutorService _executors;
    private final static Random random = new Random();

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

        @Override
        @SuppressWarnings("unchecked")
        public T call() throws Exception {
            StringBuilder sql = new StringBuilder("SELECT ");
            sql.append(_tg.valueColumnName()).append(" FROM ").append(_tg.table());
            sql.append(" WHERE ").append(_tg.pkColumnName()).append(" = ? FOR UPDATE");


            try (TransactionLegacy txn = TransactionLegacy.open("Sequence");
                 PreparedStatement selectStmt = txn.prepareStatement(sql.toString());
                ) {
                if (_key == null) {
                    selectStmt.setString(1, _tg.pkColumnValue());
                } else {
                    selectStmt.setObject(1, _key);
                }

                sql = new StringBuilder("UPDATE ");
                sql.append(_tg.table()).append(" SET ").append(_tg.valueColumnName()).append("=").append("?+?");
                sql.append(" WHERE ").append(_tg.pkColumnName()).append("=?");

                try (PreparedStatement updateStmt = txn.prepareStatement(sql.toString());) {
                    if (isRandom) {
                        updateStmt.setInt(2, random.nextInt(10) + 1);
                    } else {
                        updateStmt.setInt(2, _tg.allocationSize());
                    }
                    if (_key == null) {
                        updateStmt.setString(3, _tg.pkColumnValue());
                    } else {
                        updateStmt.setObject(3, _key);
                    }

                    txn.start();
                    Object obj = null;
                    try (ResultSet rs = selectStmt.executeQuery();) {

                        while (rs.next()) {
                            if (_clazz.isAssignableFrom(Long.class)) {
                                obj = rs.getLong(1);
                            } else if (_clazz.isAssignableFrom(Integer.class)) {
                                obj = rs.getInt(1);
                            } else {
                                obj = rs.getObject(1);
                            }
                        }
                    } catch (SQLException e) {
                        s_logger.warn("Caught this exception when running: " + (selectStmt != null ? selectStmt.toString() : ""), e);
                    }

                    if (obj == null) {
                        s_logger.warn("Unable to get a sequence: " + updateStmt.toString());
                        return null;
                    }

                    updateStmt.setObject(1, obj);
                    try {
                        int rows = updateStmt.executeUpdate();
                        assert rows == 1 : "Come on....how exactly did we update this many rows " + rows + " for " + updateStmt.toString();
                        txn.commit();
                        return (T)obj;
                    } catch (SQLException e) {
                        s_logger.warn("Caught this exception when running: " + (updateStmt != null ? updateStmt.toString() : ""), e);
                    }
                }
            } catch (Exception e) {
                s_logger.warn("Caught this exception when running.", e);
            }
            return null;
        }
    }
}
