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
package com.cloud.hypervisor.xen.resource;

import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;

import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.XenAPIException;

public class XenServerConnectionPool {
    private static final Logger s_logger = Logger.getLogger(XenServerConnectionPool.class);
    
    protected HashMap<String /*hostUuid*/, XenServerConnection> _conns = new HashMap<String, XenServerConnection>();
    protected HashMap<String /*poolUuid*/, ConnectionInfo> _infos = new HashMap<String, ConnectionInfo>();
    
    protected int _retries;
    protected int _interval;
    
    protected XenServerConnectionPool() {
        _retries = 10;
        _interval = 3;
    }
    
    public synchronized void switchMaster(String lipaddr, String poolUuid, Connection conn, Host host, String username, String password, int wait) throws XmlRpcException, XenAPIException {
        String ipaddr = host.getAddress(conn);
        PoolSyncDB(conn);
        s_logger.debug("Designating the new master to " + ipaddr);
        Pool.designateNewMaster(conn, host);
        XenServerConnection slaveConn = null;
        XenServerConnection masterConn = null;
        int retry = 30;
        for (int i = 0; i < retry; i++) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
            }
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Logging on as the slave to " + lipaddr);
                }
                slaveConn = null;
                masterConn = null;
                URL slaveUrl = null;
                URL masterUrl = null;
                Session slaveSession = null;
                
                slaveUrl = new URL("http://" + lipaddr);
                slaveConn = new XenServerConnection(slaveUrl, username, password, _retries, _interval, 10);
                slaveSession = Session.slaveLocalLoginWithPassword(slaveConn, username, password);

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Slave logon successful. session= " + slaveSession);
                }
                
                Pool.Record pr = getPoolRecord(slaveConn);
                Host master = pr.master;
                String ma = master.getAddress(slaveConn);
                if( !ma.trim().equals(ipaddr.trim()) ) {
                    continue;
                }
                Session.localLogout(slaveConn);
                
                masterUrl = new URL("http://" + ipaddr);
                masterConn = new XenServerConnection(masterUrl, username, password, _retries, _interval, wait);
                Session.loginWithPassword(masterConn, username,
                                          password,
                                          APIVersion.latest().toString());
                cleanup(poolUuid);
                Pool.recoverSlaves(masterConn);
                PoolSyncDB(masterConn);
                return;
            } catch (Types.HostIsSlave e) {
                s_logger.debug("HostIsSlaveException: Still waiting for the conversion to the master");
            } catch (XmlRpcException e) {
                s_logger.debug("XmlRpcException: Still waiting for the conversion to the master " + e.getMessage());
            } catch (Exception e) {
                s_logger.debug("Exception: Still waiting for the conversion to the master" + e.getMessage());
            } finally {
            	if (masterConn != null) {
            		try {
            		  Session.logout(masterConn);
            		} catch (Exception e) {
            			s_logger.debug("Unable to log out of session: " + e.getMessage());
            		}
            		masterConn.dispose();
            		masterConn = null;
            	}
            }

        }
        
        throw new CloudRuntimeException("Unable to logon to the new master after " + retry + " retries");
    }
    
    protected synchronized void cleanup(String poolUuid) {
        ConnectionInfo info = _infos.remove(poolUuid);
        if (info == null) {
            s_logger.debug("Unable to find any information for pool " + poolUuid);
            return;
        }
        
        for (Member member : info.refs.values()) {
            _conns.remove(member.uuid);
        }
        
        if (info.conn != null) {
            try {
                s_logger.debug("Logging out of session " + info.conn.getSessionReference());
                
                Session.logout(info.conn);
            } catch (XenAPIException e) {
                s_logger.debug("Unable to logout of the session");
            } catch (XmlRpcException e) {
                s_logger.debug("Unable to logout of the session");
            }
            info.conn.dispose();
        }
        s_logger.debug("Session is cleaned up");
    }
    
    protected synchronized void cleanup(String poolUuid, ConnectionInfo info) {
        ConnectionInfo info2 = _infos.get(poolUuid);
        if (info != info2) {
            s_logger.debug("Session " + info.conn.getSessionReference() + " is already logged out.");
            return;
        }
        
        cleanup(poolUuid);
    }
    
    public synchronized void disconnect(String uuid, String poolUuid) {
        Connection conn = _conns.remove(uuid);
        if (conn == null) {
            return;
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Logging out of " + conn.getSessionReference() + " for host " + uuid);
        }
        
        conn.dispose();
        
        ConnectionInfo info = _infos.get(poolUuid);
        if (info == null) {
            return;
        }
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Connection for pool " + poolUuid + " found. session=" + info.conn.getSessionReference());
        }
        
        info.refs.remove(uuid);
        if (info.refs.size() == 0) {
         
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Logging out of the session " + info.conn.getSessionReference());
            }
            _infos.remove(poolUuid);
            try {
                Session.logout(info.conn);
                info.conn.dispose();
            } catch (Exception e) {
                s_logger.debug("Logout has a problem " + e.getMessage());
            }
            info.conn = null;
        }
    }
    
    public static void logout(Connection conn) {
        try {
            s_logger.debug("Logging out of the session " + conn.getSessionReference());
            Session.logout(conn);
        } catch (Exception e) {
            s_logger.debug("Logout has problem " + e.getMessage());
        } finally {
            conn.dispose();
        }
    }
    
    public Connection connect(String urlString, String username, String password, int wait) {
        return connect(null, urlString, username, password, wait);
    }
    
    protected ConnectionInfo getConnectionInfo(String poolUuid) {
        synchronized(_infos) {
            return _infos.get(poolUuid);
        }
    }
    
    protected XenServerConnection getConnection(String hostUuid) {
        synchronized(_conns) {
            return _conns.get(hostUuid);
        }
    }
    
    static void PoolSyncDB(Connection conn) {
        try{
            Set<Host> hosts = Host.getAll(conn);
            for(Host host : hosts) {
                try {
                    host.enable(conn);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            s_logger.debug("Enbale host failed due to " + e.getMessage() + e.toString());
        }
        try{      
            Pool.syncDatabase(conn);
        } catch (Exception e) {
            s_logger.debug("Sync Database failed due to " + e.getMessage() + e.toString());
        }


    }
    
    protected synchronized URL ensurePoolIntegrity(Connection conn, Host master, Pool.Record poolr, String ipAddress, String username, String password, int wait) throws XenAPIException, XmlRpcException {
        if (!ipAddress.equals(master.getAddress(conn))) {
            return null;  // Doesn't think it is the master anyways.
        }
        
        String poolUuid = poolr.uuid;
        ConnectionInfo info = _infos.get(poolUuid);
        if (info != null) {
            Connection poolConn = info.conn;
            if (poolConn != null) {
                Pool.recoverSlaves(poolConn);
                PoolSyncDB(poolConn);
                return info.masterUrl;
            }
        }

        Set<Host> slaves = Host.getAll(conn);
        HashMap<String, Integer> count = new HashMap<String, Integer>(slaves.size());
        for (Host slave : slaves) {
            String slaveAddress = slave.getAddress(conn);
            Connection slaveConn = null;
            try {
                slaveConn = new Connection(new URL("http://" + slaveAddress), wait);
                if (slaveConn != null) {
                    Session slaveSession = Session.slaveLocalLoginWithPassword(slaveConn, username, password);
                    Pool.Record slavePoolr = getPoolRecord(slaveConn);
                    String possibleMaster = slavePoolr.master.getAddress(slaveConn);
                    Integer c = count.get(possibleMaster);
                    if (c == null) {
                        c = 1;
                    } else {
                        c++;
                    }
                    count.put(possibleMaster, c);
                    try {
                        slaveSession.logout(slaveConn);
                    } catch (Exception e) {
                        s_logger.debug("client session logout: " + e.getMessage());
                    }
                    slaveConn.dispose();
                }
            } catch (MalformedURLException e) {
                throw new CloudRuntimeException("Bad URL" + slaveAddress, e);
            } catch (Exception e) {
                s_logger.debug("Unable to login to slave " + slaveAddress + " error " + e.getMessage());
            } finally {
                if (slaveConn != null) {
                    slaveConn.dispose();
                }
            }
        }
        
        Iterator<Map.Entry<String, Integer>> it = count.entrySet().iterator();
       
        Map.Entry<String, Integer> newMaster = it.next();
        while (it.hasNext()) {
            Map.Entry<String, Integer> entry = it.next();
            
            if (newMaster.getValue() < entry.getValue()) {
                newMaster = entry;
            }
        }
        
        String newMasterAddress = newMaster.getKey();
        if (count.size() > 1 && !ipAddress.equals(newMasterAddress)) {
            s_logger.debug("Asking the correct master to recover the slaves: " + newMasterAddress);
            
            URL newMasterUrl = null;
            try {
                newMasterUrl = new URL("http://" + newMasterAddress);
            } catch (MalformedURLException e) {
                throw new CloudRuntimeException("Unable to get url from " + newMasterAddress, e);
            }
            
            Connection newMasterConn = new Connection(newMasterUrl, wait);
            try {
                Session.loginWithPassword(newMasterConn, username, password, APIVersion.latest().toString());
                Pool.recoverSlaves(newMasterConn);
                PoolSyncDB(newMasterConn);
            } catch (Exception e) {
                throw new CloudRuntimeException("Unable to login to the real master at " + newMaster.getKey());
            } finally {
                try {
                    Session.logout(newMasterConn);
                } catch (Exception e) {
                    s_logger.debug("Unable to logout of the session: " + e.getMessage());
                }
                newMasterConn.dispose();
            }
            
            return newMasterUrl;
        }
        
        return null;
    }
    
    public synchronized Connection connect(String hostUuid, String ipAddress, String username, String password, int wait) {
        XenServerConnection masterConn = null;
        if (hostUuid != null) { // Let's see if it is an existing connection.
            masterConn = _conns.get(hostUuid);
            if (masterConn != null) {
                return masterConn;
            }
        }
        
        XenServerConnection slaveConn = null;
        URL slaveUrl = null;
        URL masterUrl = null;
        Session slaveSession = null;
        Session masterSession = null;
        
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating connection to " + ipAddress);
        }
        
        // Well, it's not already in the existing connection list.
        // Let's login and see what this connection should be.
        // You might think this is slow.  Why not cache the pool uuid
        // you say?  Well, this doesn't happen very often.
        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Logging on as the slave to " + ipAddress);
            }
            slaveUrl = new URL("http://" + ipAddress);
            slaveConn = new XenServerConnection(slaveUrl, username, password, _retries, _interval, 10);
            slaveSession = Session.slaveLocalLoginWithPassword(slaveConn, username, password);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Slave logon successful. session= " + slaveSession);
            }
            
            try {
                Pool.Record pr = getPoolRecord(slaveConn);
                Host master = pr.master;
                String ma = master.getAddress(slaveConn);
                masterUrl = new URL("http://" + ma);
                s_logger.debug("Logging on as the master to " + ma);
                masterConn = new XenServerConnection(masterUrl, username, password, _retries, _interval, wait);
                masterSession = Session.loginWithPassword(masterConn, username, password, APIVersion.latest().toString());
         
                URL realMasterUrl = ensurePoolIntegrity(masterConn, master, pr, ipAddress, username, password, wait);
                if (realMasterUrl != null) {
                    s_logger.debug("The real master url is at " + realMasterUrl);
                    masterUrl = realMasterUrl;
                    masterConn.dispose();
                    masterConn = new XenServerConnection(masterUrl, username, password, _retries, _interval, wait);
                    masterSession = Session.loginWithPassword(masterConn, username, password, APIVersion.latest().toString());
                    
                }
            } catch (XmlRpcException e) {
                Throwable c = e.getCause();
                if (c == null || (!(c instanceof SocketException) && !(c instanceof SocketTimeoutException))) {
                    s_logger.warn("Unable to connect to " + masterUrl, e);
                    throw e;
                }

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Unable to connect to the " + masterUrl + ". Attempting switch to master");
                }
                Pool.emergencyTransitionToMaster(slaveConn);
                Pool.recoverSlaves(slaveConn);
                
                s_logger.info("Successfully converted to the master: " + ipAddress);
                
                masterUrl = slaveUrl;
                masterConn = new XenServerConnection(masterUrl, username, password, _retries, _interval, wait);
                masterSession = Session.loginWithPassword(masterConn, username, password, APIVersion.latest().toString());
            }
                
            if (slaveSession != null) {
                s_logger.debug("Login to the master is successful.  Signing out of the slave connection: " + slaveSession);
                try {
                    Session.localLogout(slaveConn);
                } catch (Exception e) {
                    s_logger.debug("Unable to logout of slave session " + slaveSession);
                }
                slaveConn.dispose();
            }
            
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Successfully logged on to the master.  Session=" + masterConn.getSessionReference());
            }
            if (hostUuid == null) {
                s_logger.debug("Returning now.  Client is responsible for logging out of " + masterConn.getSessionReference());
                return masterConn;
            }
            
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Logon is successfully.  Let's see if we have other hosts logged onto the same master at " + masterUrl);
            }
            
            Pool.Record poolr = getPoolRecord(masterConn);
            String poolUuid = poolr.uuid;
            
            ConnectionInfo info = null;
            info = _infos.get(poolUuid);
            if (info != null && info.conn != null) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("The pool already has a master connection.  Session=" + info.conn.getSessionReference());
                }
                try {
                    s_logger.debug("Logging out of our own session: " + masterConn.getSessionReference());
                    Session.logout(masterConn);
                    masterConn.dispose();
                } catch (Exception e) {
                    s_logger.debug("Caught Exception while logging on but pushing on...." + e.getMessage());
                }
                masterConn = new XenServerConnection(info.conn);
            } else {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("This is the very first connection");
                }
                info = new ConnectionInfo();
                info.conn = masterConn;
                info.masterUrl = masterUrl;
                info.refs = new HashMap<String, Member>();
                _infos.put(poolUuid, info);
                masterConn.setInfo(poolUuid, info);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Pool " + poolUuid + " is matched with session " + info.conn.getSessionReference());
                }
            }
            
            s_logger.debug("Added a reference for host " + hostUuid + " to session " + masterConn.getSessionReference() + " in pool " +  poolUuid);
            info.refs.put(hostUuid, new Member(slaveUrl, hostUuid, username, password));
            _conns.put(hostUuid, masterConn);
            
            s_logger.info("Connection made to " + ipAddress + " for host " + hostUuid + ".  Pool Uuid is " + poolUuid);
            
            return masterConn;
        } catch (XenAPIException e) {
            s_logger.warn("Unable to make a connection to the server " + ipAddress, e);
            throw new CloudRuntimeException("Unable to make a connection to the server " + ipAddress, e);
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to make a connection to the server " + ipAddress, e);
            throw new CloudRuntimeException("Unable to make a connection to the server " + ipAddress, e);
        } catch (MalformedURLException e) {
            throw new CloudRuntimeException("How can we get a malformed exception for this " + ipAddress, e);
        }
    }
    
    
    
    protected Pool.Record getPoolRecord(Connection conn) throws XmlRpcException, XenAPIException {
        Map<Pool, Pool.Record> pools = Pool.getAllRecords(conn);
        assert pools.size() == 1 : "Pool size is not one....hmmm....wth? " + pools.size();
        
        return pools.values().iterator().next();
    }

    private static final XenServerConnectionPool s_instance = new XenServerConnectionPool();
    public static XenServerConnectionPool getInstance() {
        return s_instance;
    }
    
    protected class ConnectionInfo {
        public URL masterUrl;
        public XenServerConnection conn;
        public HashMap<String, Member> refs = new HashMap<String, Member>();
    }
    
    protected class Member {
        public URL url;
        public String uuid;
        public String username;
        public String password;
        
        public Member(URL url, String uuid, String username, String password) {
            this.url = url;
            this.uuid = uuid;
            this.username = username;
            this.password = password;
        }
    }
    
    public class XenServerConnection extends Connection {
        long _interval;
        int _retries;
        String _username;
        String _password;
        URL _url;
        ConnectionInfo _info;
        String _poolUuid;
        
        public XenServerConnection(URL url, String username, String password, int retries, int interval, int wait) {
            super(url, wait);
            _url = url;
            _retries = retries;
            _username = username;
            _password = password;
            _interval = (long)interval * 1000;
        }

        public XenServerConnection(XenServerConnection that) {
            super(that._url, that.getSessionReference(), that._wait);
            this._url = that._url;
            this._retries = that._retries;
            this._username = that._username;
            this._password = that._password;
            this._interval = that._interval;
            this._info = that._info;
            this._poolUuid = that._poolUuid;
            
        }
        
        public void setInfo(String poolUuid, ConnectionInfo info) {
            this._poolUuid = poolUuid;
            this._info = info;
        }
        
        public int getWaitTimeout() {
            return _wait;
        }
        
        @Override
        protected Map dispatch(String method_call, Object[] method_params) throws XmlRpcException, XenAPIException {
            if (method_call.equals("session.login_with_password") || method_call.equals("session.slave_local_login_with_password") || method_call.equals("session.logout")) {
                int retries = 0;
                while (retries++ < _retries) {
                    try {
                        return super.dispatch(method_call, method_params);
                    } catch (XmlRpcException e) {
                        Throwable cause = e.getCause();
                        if (cause == null || !(cause instanceof SocketException)) {
                            throw e;
                        }
                        if (retries >= _retries) {
                            throw e;
                        }
                        s_logger.debug("Unable to login...retrying " + retries);
                    }
                    try {
                        Thread.sleep(_interval);
                    } catch(InterruptedException e) {
                        s_logger.debug("Man....I was just getting comfortable there....who woke me up?");
                    }
                }
            }
            
            int retries = 0;
            while (retries++ < _retries) {
                try {
                    return super.dispatch(method_call, method_params);
                } catch (Types.SessionInvalid e) {
                    if (retries >= _retries) {
                        if (_poolUuid != null) {
                            cleanup(_poolUuid, _info);
                        }
                        throw e;
                    }
                    s_logger.debug("Session is invalid.  Reconnecting...retry=" + retries);
                } catch (XmlRpcException e) {
                    if (retries >= _retries) {
                        if (_poolUuid != null) {
                            cleanup(_poolUuid, _info);
                        }
                        throw e;
                    }
                    Throwable cause = e.getCause();
                    if (cause == null || !(cause instanceof SocketException)) {
                        if (_poolUuid != null) {
                            cleanup(_poolUuid, _info);
                        }
                        throw e;
                    }
                    s_logger.debug("Connection couldn't be made. Reconnecting....retry=" + retries);
                } catch (Types.HostIsSlave e) {
                    if (_poolUuid != null) {
                        cleanup(_poolUuid, _info);
                    }
                    throw e;
                }
                
                try {
                    Thread.sleep(_interval);
                } catch (InterruptedException e) {
                    s_logger.info("Who woke me from my slumber?");
                }

                Session session = Session.loginWithPassword(this, _username, _password, APIVersion.latest().toString());
                method_params[0] = getSessionReference();
            }
            assert false : "We should never get here";
            if (_poolUuid != null) {
                cleanup(_poolUuid, _info);
            }
            throw new CloudRuntimeException("After " + _retries + " retries, we cannot contact the host ");
        }
    }
}
