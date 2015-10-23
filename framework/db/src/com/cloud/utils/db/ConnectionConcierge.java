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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.StandardMBean;

import org.apache.log4j.Logger;

import org.apache.cloudstack.managed.context.ManagedContextRunnable;

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.mgmt.JmxUtil;

/**
 * ConnectionConcierge keeps stand alone database connections alive.  This is
 * needs someone to keep that database connection from being garbage collected
 *
 */
public class ConnectionConcierge {

    static final Logger s_logger = Logger.getLogger(ConnectionConcierge.class);

    static final ConnectionConciergeManager s_mgr = new ConnectionConciergeManager();

    Connection _conn;
    String _name;
    boolean _keepAlive;
    boolean _autoCommit;
    int _isolationLevel;
    int _holdability;

    public ConnectionConcierge(String name, Connection conn, boolean keepAlive) {
        _name = name + s_mgr.getNextId();
        _keepAlive = keepAlive;
        try {
            _autoCommit = conn.getAutoCommit();
            _isolationLevel = conn.getTransactionIsolation();
            _holdability = conn.getHoldability();
        } catch (SQLException e) {
            throw new CloudRuntimeException("Unable to get information from the connection object", e);
        }
        reset(conn);
    }

    public void reset(Connection conn) {
        try {
            release();
        } catch (Throwable th) {
            s_logger.error("Unable to release a connection", th);
        }
        _conn = conn;
        try {
            _conn.setAutoCommit(_autoCommit);
            _conn.setHoldability(_holdability);
            _conn.setTransactionIsolation(_isolationLevel);
        } catch (SQLException e) {
            s_logger.error("Unable to release a connection", e);
        }
        s_mgr.register(_name, this);
        s_logger.debug("Registering a database connection for " + _name);
    }

    public final Connection conn() {
        return _conn;
    }

    public void release() {
        s_mgr.unregister(_name);
        try {
            if (_conn != null) {
                _conn.close();
            }
            _conn = null;
        } catch (SQLException e) {
            throw new CloudRuntimeException("Problem in closing a connection", e);
        }
    }

    @Override
    protected void finalize() throws Exception {
        if (_conn != null) {
            release();
        }
    }

    public boolean keepAlive() {
        return _keepAlive;
    }

    protected static class ConnectionConciergeManager extends StandardMBean implements ConnectionConciergeMBean {
        ScheduledExecutorService _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ConnectionKeeper"));
        final ConcurrentHashMap<String, ConnectionConcierge> _conns = new ConcurrentHashMap<String, ConnectionConcierge>();
        final AtomicInteger _idGenerator = new AtomicInteger();

        ConnectionConciergeManager() {
            super(ConnectionConciergeMBean.class, false);
            resetKeepAliveTask(20);
            try {
                JmxUtil.registerMBean("DB Connections", "DB Connections", this);
            } catch (Exception e) {
                s_logger.error("Unable to register mbean", e);
            }
        }

        public Integer getNextId() {
            return _idGenerator.incrementAndGet();
        }

        public void register(String name, ConnectionConcierge concierge) {
            _conns.put(name, concierge);
        }

        public void unregister(String name) {
            _conns.remove(name);
        }

        protected String testValidity(String name, Connection conn) {
            if (conn != null) {
                synchronized (conn) {
                    try (PreparedStatement pstmt = conn.prepareStatement("SELECT 1");) {
                        pstmt.executeQuery();
                    } catch (Throwable th) {
                        s_logger.error("Unable to keep the db connection for " + name, th);
                        return th.toString();
                    }
                }
            }
            return null;
        }

        @Override
        public List<String> testValidityOfConnections() {
            ArrayList<String> results = new ArrayList<String>(_conns.size());
            for (Map.Entry<String, ConnectionConcierge> entry : _conns.entrySet()) {
                String result = testValidity(entry.getKey(), entry.getValue().conn());
                results.add(entry.getKey() + "=" + (result == null ? "OK" : result));
            }
            return results;
        }

        @Override
        public String resetConnection(String name) {
            ConnectionConcierge concierge = _conns.get(name);
            if (concierge == null) {
                return "Not Found";
            }

            Connection conn = TransactionLegacy.getStandaloneConnection();
            if (conn == null) {
                return "Unable to get anotehr db connection";
            }

            concierge.reset(conn);
            return "Done";
        }

        @Override
        public String resetKeepAliveTask(int seconds) {
            if (_executor != null) {
                try {
                    _executor.shutdown();
                } catch (Exception e) {
                    s_logger.error("Unable to shutdown executor", e);
                }
            }

            _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ConnectionConcierge"));

            _executor.scheduleAtFixedRate(new ManagedContextRunnable() {
                @Override
                protected void runInContext() {
                    s_logger.trace("connection concierge keep alive task");
                    for (Map.Entry<String, ConnectionConcierge> entry : _conns.entrySet()) {
                        ConnectionConcierge concierge = entry.getValue();
                        if (concierge.keepAlive()) {
                            testValidity(entry.getKey(), entry.getValue().conn());
                        }
                    }
                }
            }, 0, seconds, TimeUnit.SECONDS);

            return "As you wish.";
        }

        @Override
        public List<String> getConnectionsNotPooled() {
            return new ArrayList<String>(_conns.keySet());
        }
    }
}
