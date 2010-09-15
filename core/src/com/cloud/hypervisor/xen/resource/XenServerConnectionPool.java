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
    private static final Logger s_logger = Logger
            .getLogger(XenServerConnectionPool.class);

    protected HashMap<String /* hostUuid */, XenServerConnection> _conns = new HashMap<String, XenServerConnection>();
    protected HashMap<String /* poolUuid */, ConnectionInfo> _infos = new HashMap<String, ConnectionInfo>();

    protected int _retries;
    protected int _interval;

    protected XenServerConnectionPool() {
        _retries = 3;
        _interval = 3;
    }

    void forceSleep(long sec) {
        long firetime = System.currentTimeMillis() + (sec * 1000);
        long msec = sec * 1000;
        while (true) {
            if (msec < 100)
                break;
            try {
                Thread.sleep(msec);
                return;
            } catch (InterruptedException e) {
                msec = firetime - System.currentTimeMillis();
            }
        }
    }

    public synchronized void switchMaster(String slaveIp, String poolUuid,
            Connection conn, Host host, String username, String password,
            int wait) throws XmlRpcException, XenAPIException {
        String masterIp = host.getAddress(conn);
        PoolSyncDB(conn);
        s_logger.debug("Designating the new master to " + masterIp);
        Pool.designateNewMaster(conn, host);
        Connection slaveConn = null;
        Connection masterConn = null;
        int retry = 30;
        for (int i = 0; i < retry; i++) {
            forceSleep(5);
            try {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Logging on as the slave to " + slaveIp);
                }
                slaveConn = null;
                masterConn = null;
                URL slaveUrl = null;
                URL masterUrl = null;
                Session slaveSession = null;

                slaveUrl = new URL("http://" + slaveIp);
                slaveConn = new Connection(slaveUrl, 100);
                slaveSession = Session.slaveLocalLoginWithPassword(slaveConn,
                        username, password);

                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Slave logon successful. session= "
                            + slaveSession);
                }

                Pool.Record pr = getPoolRecord(slaveConn);
                Host master = pr.master;
                String ma = master.getAddress(slaveConn);
                if (!ma.trim().equals(masterIp.trim())) {
                    continue;
                }
                Session.localLogout(slaveConn);
                slaveConn = null;
                s_logger.debug("Logging on as the master to " + masterIp);
                masterUrl = new URL("http://" + masterIp);
                masterConn = new Connection(masterUrl, 100);
                Session.loginWithPassword(masterConn, username, password,
                        APIVersion.latest().toString());
                cleanup(poolUuid);
                ensurePoolIntegrity(masterConn, masterIp, username, password,
                        wait);
                PoolSyncDB(masterConn);
                return;
            } catch (Types.HostIsSlave e) {
                s_logger
                        .debug("HostIsSlaveException: Still waiting for the conversion to the master");
            } catch (XmlRpcException e) {
                s_logger
                        .debug("XmlRpcException: Still waiting for the conversion to the master "
                                + e.getMessage());
            } catch (Exception e) {
                s_logger
                        .debug("Exception: Still waiting for the conversion to the master"
                                + e.getMessage());
            } finally {
                if (masterConn != null) {
                    try {
                        Session.logout(masterConn);
                    } catch (Exception e) {
                        s_logger.debug("Unable to log out of session: "
                                + e.getMessage());
                    }
                    masterConn.dispose();
                    masterConn = null;
                }
                if (slaveConn != null) {
                    try {
                        Session.localLogout(slaveConn);
                    } catch (Exception e) {
                        s_logger.debug("Unable to log out of session: "
                                + e.getMessage());
                    }
                    slaveConn.dispose();
                    slaveConn = null;
                }

            }

        }

        throw new CloudRuntimeException(
                "Unable to logon to the new master after " + retry + " retries");
    }

    protected synchronized void cleanup(String poolUuid) {
        ConnectionInfo info = _infos.remove(poolUuid);
        if (info == null) {
            s_logger.debug("Unable to find any information for pool "
                    + poolUuid);
            return;
        }

        s_logger.debug("Cleaning up session for pool " + poolUuid);
        for (Member member : info.refs.values()) {
            s_logger.debug("remove connection for host " + member.uuid);
            _conns.remove(member.uuid);
        }

        if (info.conn != null) {
            try {
                s_logger.debug("Logging out of session "
                        + info.conn.getSessionReference());
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
        s_logger
                .debug("Cleanup for Session " + info.conn.getSessionReference());
        if (info != info2) {
            s_logger.debug("Session " + info.conn.getSessionReference()
                    + " is already logged out.");
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
            s_logger.debug("Logging out of " + conn.getSessionReference()
                    + " for host " + uuid);
        }

        ConnectionInfo info = _infos.get(poolUuid);
        if (info == null) {
            return;
        }

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Connection for pool " + poolUuid
                    + " found. session=" + info.conn.getSessionReference());
        }

        Member member = info.refs.remove(uuid);
        
        if (info.refs.size() == 0 || ( member != null && member.ipAddr.equals(info.masterIp) )) {
            cleanup(poolUuid);
        }
    }

    public static void logout(Connection conn) {
        try {
            s_logger.debug("Logging out of the session "
                    + conn.getSessionReference());
            Session.logout(conn);
        } catch (Exception e) {
            s_logger.debug("Logout has problem " + e.getMessage());
        } finally {
            conn.dispose();
        }
    }

    public Connection masterConnect(String ip, String username, String password) {
        Connection slaveConn = null;
        Connection masterConn = null;
        try{ 
            URL slaveUrl = new URL("http://" + ip);
            slaveConn = new Connection(slaveUrl, 100);
            Session.slaveLocalLoginWithPassword(slaveConn, username, password);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Slave logon to " + ip);
            }
            String masterIp = null;
            try {
                Pool.Record pr = getPoolRecord(slaveConn);
                Host master = pr.master;
                masterIp = master.getAddress(slaveConn);

                s_logger.debug("Logging on as the master to " + masterIp);
                URL masterUrl = new URL("http://" + masterIp);
                masterConn = new Connection(masterUrl, 100);
                Session.loginWithPassword(masterConn, username, password,
                        APIVersion.latest().toString());
                return masterConn;
            } catch (Exception e) {
                s_logger.debug("Failed to log on as master to ");
                if( masterConn != null ) {
                    try {
                        Session.logout(masterConn);
                    } catch (Exception e1) {
                    }
                    masterConn.dispose();
                    masterConn = null;
                }
            }
        }catch ( Exception e){
            s_logger.debug("Failed to slave local login to " + ip);
        } finally {
            if( slaveConn != null ) {
                try {
                    Session.localLogout(slaveConn);
                } catch (Exception e1) {
                }
                slaveConn.dispose();
                slaveConn = null;
            }
            
        }
        return null;
    }
    
    public Connection slaveConnect(String ip, String username, String password) {
        Connection conn = null;
        try{ 
            URL url = new URL("http://" + ip);
            conn = new Connection(url, 100);
            Session.slaveLocalLoginWithPassword(conn, username, password);
            return conn;
        }catch ( Exception e){
            s_logger.debug("Failed to slave local login to " + ip);
        } 
        return null;
    }

    protected ConnectionInfo getConnectionInfo(String poolUuid) {
        synchronized (_infos) {
            return _infos.get(poolUuid);
        }
    }

    protected XenServerConnection getConnection(String hostUuid) {
        synchronized (_conns) {
            return _conns.get(hostUuid);
        }
    }

    static void PoolSyncDB(Connection conn) {
        try {
            Set<Host> hosts = Host.getAll(conn);
            for (Host host : hosts) {
                try {
                    host.enable(conn);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            s_logger.debug("Enbale host failed due to " + e.getMessage()
                    + e.toString());
        }
        try {
            Pool.syncDatabase(conn);
        } catch (Exception e) {
            s_logger.debug("Sync Database failed due to " + e.getMessage()
                    + e.toString());
        }
    }

    void PoolEmergencyTransitionToMaster(String slaveIp, String username, String password) {
        Connection slaveConn = null;
        Connection c = null;
        try{
            s_logger.debug("Trying to transition master to " + slaveIp);
            URL slaveUrl = new URL("http://" + slaveIp);
            slaveConn = new Connection(slaveUrl, 100);
            Session.slaveLocalLoginWithPassword(slaveConn, username, password);
            Pool.emergencyTransitionToMaster(slaveConn);
            try {
                Session.localLogout(slaveConn);
                slaveConn = null;
            } catch (Exception e) {
            }
            // restart xapi in 10 sec
            forceSleep(10);
            // check if the master of this host is set correctly.
            c = new Connection(slaveUrl, 100);
            for (int i = 0; i < 30; i++) {
                try {
                    Session.loginWithPassword(c, username, password, APIVersion.latest().toString());
                    s_logger.debug("Succeeded to transition master to " + slaveIp);
                    return;
                } catch (Types.HostIsSlave e) {
                    s_logger.debug("HostIsSlave: Still waiting for the conversion to the master " + slaveIp);
                } catch (Exception e) {
                    s_logger.debug("Exception: Still waiting for the conversion to the master");
                }
                forceSleep(2);
            }
            throw new RuntimeException("EmergencyTransitionToMaster failed after retry 30 times");
        } catch (Exception e) {
            throw new RuntimeException("EmergencyTransitionToMaster failed due to " + e.getMessage());
        } finally {
            if(slaveConn != null) {
                try {
                    Session.localLogout(slaveConn);
                } catch (Exception e) {
                }
            }
            if(c != null) {
                try {
                    Session.logout(c);
                    c.dispose();
                } catch (Exception e) {
                }
            }
        }
        
    }

    void PoolEmergencyResetMaster(String slaveIp, String masterIp,
            String username, String password) {
        Connection slaveConn = null;
        try {
            s_logger.debug("Trying to reset master of slave " + slaveIp
                    + " to " + masterIp);
            URL slaveUrl = new URL("http://" + slaveIp);
            slaveConn = new Connection(slaveUrl, 10);
            Session.slaveLocalLoginWithPassword(slaveConn, username, password);
            Pool.emergencyResetMaster(slaveConn, masterIp);
            if (slaveConn != null) {
                try {
                    Session.localLogout(slaveConn);
                } catch (Exception e) {
                }
            }
            forceSleep(10);
            for (int i = 0; i < 30; i++) {
                try {
                    Session.slaveLocalLoginWithPassword(slaveConn, username, password);
                    Pool.Record pr = getPoolRecord(slaveConn);
                    String mIp = pr.master.getAddress(slaveConn);
                    if (mIp.trim().equals(masterIp.trim())) {
                        return;
                    }
                } catch (Exception e) {
                }
                if (slaveConn != null) {
                    try {
                        Session.localLogout(slaveConn);
                    } catch (Exception e) {
                    }
                }
                // wait 2 second
                forceSleep(2);
            }
        } catch (Exception e) {

        } finally {
            if (slaveConn != null) {
                try {
                    Session.localLogout(slaveConn);
                    slaveConn = null;
                } catch (Exception e) {
                }
            }
        }
    }

    protected synchronized void ensurePoolIntegrity(Connection conn,
            String masterIp, String username, String password, int wait)
            throws XenAPIException, XmlRpcException {
        try {
            // try recoverSlave first
            Set<Host> slaves = Pool.recoverSlaves(conn);
            // wait 10 second
            forceSleep(10);
            for(Host slave : slaves ) {
                for (int i = 0; i < 30; i++) {
                    Connection slaveConn = null;
                    try {
                        
                        String slaveIp = slave.getAddress(conn);
                        s_logger.debug("Logging on as the slave to " + slaveIp);
                        URL slaveUrl = new URL("http://" + slaveIp);
                        slaveConn = new Connection(slaveUrl, 10);
                        Session.slaveLocalLoginWithPassword(slaveConn, username, password);
                        Pool.Record pr = getPoolRecord(slaveConn);
                        String mIp = pr.master.getAddress(slaveConn);
                        if (mIp.trim().equals(masterIp.trim())) {
                            break;
                        }
                    } catch (Exception e) {
                        try {
                            Session.localLogout(slaveConn);
                        } catch (Exception e1) {
                        }
                        slaveConn.dispose();
                    }
                    // wait 2 second
                    forceSleep(2);
                }
                
            }
        } catch (Exception e) {
        }

        // then try emergency reset master
        Set<Host> slaves = Host.getAll(conn);
        for (Host slave : slaves) {
            String slaveIp = slave.getAddress(conn);
            Connection slaveConn = null;
            try {
                s_logger.debug("Logging on as the slave to " + slaveIp);
                URL slaveUrl = new URL("http://" + slaveIp);
                slaveConn = new Connection(slaveUrl, 10);
                Session.slaveLocalLoginWithPassword(slaveConn, username,
                        password);
                Pool.Record slavePoolr = getPoolRecord(slaveConn);
                String ip = slavePoolr.master.getAddress(slaveConn);
                if (!masterIp.trim().equals(ip.trim())) {
                    PoolEmergencyResetMaster(slaveIp, masterIp, username,
                            password);
                }
            } catch (MalformedURLException e) {
                throw new CloudRuntimeException("Bad URL" + slaveIp, e);
            } catch (Exception e) {
                s_logger.debug("Unable to login to slave " + slaveIp
                        + " error " + e.getMessage());
            } finally {
                if (slaveConn != null) {
                    try {
                        Session.localLogout(slaveConn);
                    } catch (Exception e) {
                    }
                    slaveConn.dispose();
                }
            }
        }
    }

    public synchronized Connection connect(String hostUuid, String poolUuid, String ipAddress,
            String username, String password, int wait) {

        XenServerConnection masterConn = null;
        Connection slaveConn = null;
        URL slaveUrl = null;
        URL masterUrl = null;
        String masterIp = null;
        ConnectionInfo info = null;
        if(hostUuid == null || poolUuid == null || ipAddress == null || username == null || password == null ) {
            String msg = "Connect some parameter are null hostUuid:" + hostUuid + " ,poolUuid:"
                + poolUuid + " ,ipAddress:" + ipAddress;
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg);
        }
        // Let's see if it is an existing connection.
        masterConn = _conns.get(hostUuid);
        if (masterConn != null){
            try{
                Host.getByUuid(masterConn, hostUuid);
                return masterConn;
            } catch (Exception e) { 
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Master Session " + masterConn.getSessionReference() + " is broken due to " + e.getMessage());
                }
                cleanup(masterConn.get_poolUuid());
                masterConn = null;
            }
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Creating connection to " + ipAddress);
        }

        try {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Logging on as the slave to " + ipAddress);
            }
            slaveUrl = new URL("http://" + ipAddress);
            slaveConn = new Connection(slaveUrl, 100);
            Session.slaveLocalLoginWithPassword(slaveConn,
                    username, password);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Slave logon successful to " + ipAddress);
            }

            info = _infos.get(poolUuid);
            boolean create_new_session = true;
            if (info != null) {
                try {
                    masterConn = info.conn;
                    Host.getByUuid(masterConn, hostUuid);
                    ensurePoolIntegrity(masterConn, info.masterIp, username,
                            password, wait);
                    masterIp = info.masterIp;
                    create_new_session = false;
                } catch (Exception e) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Unable to connect to master " + info.masterIp);
                    }
                    
                    cleanup(poolUuid);
                    masterConn = null;
                    masterIp = null;
                }
            }
            if (create_new_session) {
                try{ 
                    cleanup(poolUuid);
                    s_logger.info("Attempting switch master to " + ipAddress);

                    PoolEmergencyTransitionToMaster(ipAddress, username, password);
    
                    s_logger.info("Successfully converted to master: " + ipAddress);
    
                    s_logger.info("Loginning on as master to " + ipAddress);
                    masterUrl = slaveUrl;
                    masterConn = new XenServerConnection(masterUrl, username,
                            password, _retries, _interval, wait);
                    Session.loginWithPassword(masterConn, username, password,
                            APIVersion.latest().toString());
                    s_logger.info("Logined on as master to " + ipAddress);
                    masterIp = ipAddress;
    
                    ensurePoolIntegrity(masterConn, ipAddress, username, password,
                            wait);
                } catch (Exception e) {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("failed to switch master to Unable to " + ipAddress + " due to " + e.getMessage());
                    }
                    cleanup(poolUuid);
                    masterConn = null;
                    masterIp = null;
                }
            }

            if( masterConn == null ) {
                throw new CloudRuntimeException(" Can not create connection to pool " + poolUuid);
            }
            info = _infos.get(poolUuid);

            if ( info == null ) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Create info on master :" + masterIp);
                }
                info = new ConnectionInfo();
                info.conn = masterConn;
                info.masterIp = masterIp;
                info.refs = new HashMap<String, Member>();
                masterConn.setInfo(poolUuid, info);
                _infos.put(poolUuid, info);
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Pool " + poolUuid
                            + " is matched with session "
                            + info.conn.getSessionReference());
                }
            }
            masterConn = new XenServerConnection(info.conn);

            s_logger.debug("Added a reference for host " + hostUuid
                    + " to session " + masterConn.getSessionReference()
                    + " in pool " + poolUuid);
            info.refs.put(hostUuid, new Member(ipAddress, hostUuid, username,
                    password));
            _conns.put(hostUuid, masterConn);

            s_logger.info("Connection made to " + ipAddress + " for host "
                    + hostUuid + ".  Pool Uuid is " + poolUuid);

            return masterConn;
        } catch (XenAPIException e) {
            s_logger.warn("Unable to make a connection to the server "
                    + ipAddress);
            throw new CloudRuntimeException(
                    "Unable to make a connection to the server " + ipAddress);
        } catch (XmlRpcException e) {
            s_logger.warn("Unable to make a connection to the server "
                    + ipAddress, e);
            throw new CloudRuntimeException(
                    "Unable to make a connection to the server " + ipAddress);
        } catch (MalformedURLException e) {
            throw new CloudRuntimeException(
                    "How can we get a malformed exception for this "
                            + ipAddress);
        } finally {
            if (slaveConn != null) {
                try {
                    Session.localLogout(slaveConn);
                } catch (Exception e) {
                }
                slaveConn.dispose();
                slaveConn = null;
            }
        }
    }

    protected Pool.Record getPoolRecord(Connection conn)
            throws XmlRpcException, XenAPIException {
        Map<Pool, Pool.Record> pools = Pool.getAllRecords(conn);
        assert pools.size() == 1 : "Pool size is not one....hmmm....wth? "
                + pools.size();

        return pools.values().iterator().next();
    }

    private static final XenServerConnectionPool s_instance = new XenServerConnectionPool();

    public static XenServerConnectionPool getInstance() {
        return s_instance;
    }

    protected class ConnectionInfo {
        public String masterIp;
        public XenServerConnection conn;
        public HashMap<String, Member> refs = new HashMap<String, Member>();
    }

    protected class Member {
        public String ipAddr;
        public String uuid;
        public String username;
        public String password;


        public Member(String ipAddr, String uuid, String username, String password) {
            this.ipAddr = ipAddr;
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

        public XenServerConnection(URL url, String username, String password,
                int retries, int interval, int wait) {
            super(url, wait);
            _url = url;
            _retries = retries;
            _username = username;
            _password = password;
            _interval = (long) interval * 1000;
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
        
        public String get_poolUuid() {
            return _poolUuid;
        }

        @Override
        protected Map dispatch(String method_call, Object[] method_params)  throws XmlRpcException, XenAPIException {
            if (method_call.equals("session.local_logout") 
                    || method_call.equals("session.slave_local_login_with_password") 
                    || method_call.equals("session.logout")) {
                return super.dispatch(method_call, method_params);
            }
            
            if (method_call.equals("session.login_with_password")) {
                int retries = 0;
                while (retries++ < _retries) {
                    try {
                        return super.dispatch(method_call, method_params);
                    } catch (XmlRpcException e) {
                        Throwable cause = e.getCause();
                        if (cause == null
                                || !(cause instanceof SocketException)) {
                            throw e;
                        }
                        if (retries >= _retries) {
                            throw e;
                        }
                        s_logger.debug("Unable to login...retrying " + retries);
                    }
                    try {
                        Thread.sleep(_interval);
                    } catch (InterruptedException e) {
                        s_logger
                                .debug("Man....I was just getting comfortable there....who woke me up?");
                    }
                }
            } else {
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
                        s_logger.debug("Session is invalid.  Reconnecting...retry="
                                + retries);
                        Session.loginWithPassword(this, _username,
                                _password, APIVersion.latest().toString());
                        method_params[0] = getSessionReference();
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
                        s_logger.debug("Connection couldn't be made for method " + method_call 
                                + " Reconnecting....retry=" + retries);
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
    

                }
                assert false : "We should never get here";
                if (_poolUuid != null) {
                    cleanup(_poolUuid, _info);
                }
            }
            throw new CloudRuntimeException("After " + _retries
                    + " retries, we cannot contact the host ");
        }


    }
}
