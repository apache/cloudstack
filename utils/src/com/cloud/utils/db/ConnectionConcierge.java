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

import com.cloud.utils.concurrency.NamedThreadFactory;
import com.cloud.utils.exception.CloudRuntimeException;
import com.cloud.utils.mgmt.JmxUtil;

/**
 * ConnectionConcierge keeps stand alone database connections alive.  This is
 * useful when the code needs to keep a database connection for itself and
 * needs someone to keep that database connection from being garbage collected
 * because the connection has been idled for a long time.
 * 
 * ConnectionConierge also has JMX hooks to allow for connections to be reset
 * and validated on a live system so using it means you don't need to implement
 * your own.
 */
public class ConnectionConcierge {
    
    static final Logger s_logger = Logger.getLogger(ConnectionConcierge.class);
    
    static final ConnectionConciergeManager s_mgr = new ConnectionConciergeManager();
    
    Connection _conn;
    String _name;
    boolean _keepAlive;
    boolean _autoCommit;
    
    public ConnectionConcierge(String name, Connection conn, boolean autoCommit, boolean keepAlive) {
        _name = name + s_mgr.getNextId();
        _keepAlive = keepAlive;
        _autoCommit = autoCommit;
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
                JmxUtil.registerMBean("ConnectionConciergeManager", "ConnectionConciergeManager", this);
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
            PreparedStatement pstmt = null;
            try {
                if (conn != null) {
                    pstmt = conn.prepareStatement("SELECT 1");
                    pstmt.executeQuery();
                }
                return null;
            } catch (Throwable th) {
                s_logger.error("Unable to keep the db connection for " + name, th);
                return th.toString();
            } finally {
                if (pstmt != null) {
                    try {
                        pstmt.close();
                    } catch (SQLException e) {
                    }
                }
            }
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
            
            Connection conn = Transaction.getStandaloneConnection();
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
                } catch(Exception e) {
                    s_logger.error("Unable to shutdown executor", e);
                }
            }
            
            _executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("ConnectionConcierge"));
            _executor.schedule(new Runnable() {
                @Override
                public void run() {
                    s_logger.trace("connection concierge keep alive task");
                    for (Map.Entry<String, ConnectionConcierge> entry : _conns.entrySet()) {
                        ConnectionConcierge concierge = entry.getValue();
                        if (concierge.keepAlive()) {
                            testValidity(entry.getKey(), entry.getValue().conn());
                        }
                    }
                }
            }, seconds, TimeUnit.SECONDS);
            
            return "As you wish.";
        }

        @Override
        public List<String> getConnectionsNotPooled() {
            return new ArrayList<String>(_conns.keySet());
        }
    }
}
