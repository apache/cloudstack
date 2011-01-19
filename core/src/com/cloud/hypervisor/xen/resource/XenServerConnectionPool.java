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

import java.net.SocketException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.XenAPIException;
import com.xensource.xenapi.Types.UuidInvalid;

public class XenServerConnectionPool {
    private static final Logger s_logger = Logger
            .getLogger(XenServerConnectionPool.class);
    protected HashMap<String /* poolUuid */, XenServerConnection> _conns = new HashMap<String, XenServerConnection>();
    protected int _retries;
    protected int _interval;

    static {
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1]; 
            javax.net.ssl.TrustManager tm = new TrustAllManager(); 
            trustAllCerts[0] = tm; 
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS"); 
            sc.init(null, trustAllCerts, null); 
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier hv = new HostnameVerifier() {
                public boolean verify(String hostName, SSLSession session) {
                    return true;
                }
            };
            HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (Exception e) {
        }
    }
    

    protected XenServerConnectionPool() {
        _retries = 3;
        _interval = 3;
    }
   
    private void addConnect(String poolUuid, XenServerConnection conn){
        if( poolUuid == null ) return;
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Add master connection through " + conn.getIp() + " for pool(" + conn.getPoolUuid() + ")");                           
        }
        synchronized (_conns) {
            _conns.put(poolUuid, conn);
        }
    }
    
    private XenServerConnection getConnect(String poolUuid) {
        if( poolUuid == null ) return null;
        synchronized (_conns) {
            return _conns.get(poolUuid);
        }
    }
    
    private void removeConnect(String poolUuid) {
        if( poolUuid == null ) {
            return;
        }
        XenServerConnection conn = null;
        synchronized (_conns) {
            conn =  _conns.remove(poolUuid);
        }
        if ( conn != null ) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Remove master connection through " + conn.getIp() + " for pool(" + conn.getPoolUuid() + ")");                           
            }
            
        }
    }
    
    static void forceSleep(long sec) {
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

    public boolean joinPool(Connection conn, String hostIp, String masterIp, String username, String password) {      
        try {
            Pool.join(conn, masterIp, username, password);
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Host(" + hostIp + ") Join the pool at " + masterIp);
            }           
            try {
                // slave will restart xapi in 10 sec
                Thread.sleep(10000);
            } catch (InterruptedException e) {
            }        
            for (int i = 0 ; i < 15; i++) {
                Connection slaveConn = null;
                Session slaveSession = null;
                try {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Logging on as the slave to " + hostIp);
                    }
                    slaveConn = new Connection(getURL(hostIp));
                    slaveSession = Session.slaveLocalLoginWithPassword(slaveConn, username, password);
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("Slave logon successful. session= " + slaveSession);
                    }
                    Pool.Record pr = getPoolRecord(slaveConn);
                    Host master = pr.master;
                    String ma = master.getAddress(slaveConn);
                    if (ma.trim().equals(masterIp.trim())) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("Host(" + hostIp + ") Joined the pool at " + masterIp);
                        }
                        return true;
                    }
                } catch (Exception e) {
                } finally {
                    if (slaveSession != null) {
                        try {
                            Session.logout(slaveConn);
                        } catch (Exception e) {
                        }
                        slaveConn.dispose();
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                } 
            }

        } catch (Exception e) {
            String msg = "Catch " + e.getClass().getName() + " Unable to allow host " + hostIp + " to join pool " + masterIp + " due to " + e.toString();          
            s_logger.warn(msg, e);
        }
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Host(" + hostIp + ") unable to Join the pool at " + masterIp);
        }
        return false;
    }

    public void switchMaster(String slaveIp, String poolUuid,
            Connection conn, Host host, String username, String password,
            int wait) throws XmlRpcException, XenAPIException {
        synchronized (poolUuid.intern()) {
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
                    Session slaveSession = null;

                    slaveConn = new Connection(getURL(slaveIp));
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
                    masterConn = new Connection(getURL(masterIp));
                    Session.loginWithPassword(masterConn, username, password,
                            APIVersion.latest().toString());
                    removeConnect(poolUuid);
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
    }

    private void logout(Connection conn) {
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

    public Connection slaveConnect(String ip, String username, String password) {
        Connection conn = null;
        try{ 
            conn = new Connection(getURL(ip));
            Session.slaveLocalLoginWithPassword(conn, username, password);
            return conn;
        }catch ( Exception e){
            s_logger.debug("Failed to slave local login to " + ip);
        } 
        return null;
    }
    
    public Connection masterConnect(String ip, String username, String password) {
        Connection conn = null;
        try{ 
            conn = new Connection(getURL(ip));
            s_logger.debug("Logging on as the master to " + ip);
            Session.loginWithPassword(conn, username, password,
                    APIVersion.latest().toString());
            return conn;
        }catch ( Exception e){
            s_logger.debug("Failed to slave local login to " + ip);
        } 
        throw new RuntimeException("can not log in to master " + ip);
    }
    
  
    public String getMasterIp(String ip, String username, String password) {
        Connection slaveConn = null;
        try{ 
            slaveConn = new Connection(getURL(ip));
            Session.slaveLocalLoginWithPassword(slaveConn, username, password);

            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Slave logon to " + ip);
            }
            String masterIp = null;
            Pool.Record pr = getPoolRecord(slaveConn);
            Host master = pr.master;
            masterIp = master.getAddress(slaveConn);
            return masterIp;
        }catch ( Exception e){
            s_logger.debug("Failed to slave local login to " + ip + " due to " + e.toString());
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
        throw new RuntimeException("can not get master ip");
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
            slaveConn = new Connection(getURL(slaveIp));
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
            c = new Connection(getURL(slaveIp));
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

    private void PoolEmergencyResetMaster(String slaveIp, String masterIp,
            String username, String password) {
        Connection slaveConn = null;
        try {
            s_logger.debug("Trying to reset master of slave " + slaveIp
                    + " to " + masterIp);
            slaveConn = new Connection(getURL(slaveIp));
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
                        s_logger.debug("Succeeded to reset master of slave " + slaveIp
                                + " to " + masterIp);
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
        throw new CloudRuntimeException("Unable to reset master of slave " + slaveIp
                    + " to " + masterIp + "after 30 retry");
    }

    protected void ensurePoolIntegrity(Connection conn,
            String masterIp, String username, String password, int wait) {
        try {
            // try recoverSlave first
            Set<Host> rcSlaves = Pool.recoverSlaves(conn);
            // wait 10 second
            forceSleep(10);
            for(Host slave : rcSlaves ) {
                for (int i = 0; i < 30; i++) {
                    Connection slaveConn = null;
                    try {
                        
                        String slaveIp = slave.getAddress(conn);
                        s_logger.debug("Logging on as the slave to " + slaveIp);
                        slaveConn = new Connection(getURL(slaveIp));
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
            // then try emergency reset master
            Set<Host> slaves = Host.getAll(conn);
            for (Host slave : slaves) {
                String slaveIp = slave.getAddress(conn);
                Connection slaveConn = null;
                try {
                    s_logger.debug("Logging on as the slave to " + slaveIp);
    
                    slaveConn = new Connection(getURL(slaveIp));
                    Session.slaveLocalLoginWithPassword(slaveConn, username,
                            password);
                    Pool.Record slavePoolr = getPoolRecord(slaveConn);
                    String ip = slavePoolr.master.getAddress(slaveConn);
                    if (!masterIp.trim().equals(ip.trim())) {
                        PoolEmergencyResetMaster(slaveIp, masterIp, username,
                                password);
                    }
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
        } catch (Exception e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Catch " + e.getClass().getName() + " due to " + e.toString());
            }     
        }
    }
    
    private URL getURL(String ip){
        try {
            return new URL("https://" + ip);
        } catch (Exception e) { 
            String msg = "Unable to convert IP " + ip + " to URL due to " + e.toString();
            if (s_logger.isDebugEnabled()) {
                s_logger.debug(msg);
            }
            throw new CloudRuntimeException(msg, e);
        }
    }

    public Connection connect(String hostUuid, String poolUuid, String ipAddress,
            String username, String password, int wait) {
        XenServerConnection mConn = null;
        Connection sConn = null;
        String masterIp = null;
        if (hostUuid == null || poolUuid == null || ipAddress == null || username == null || password == null) {
            String msg = "Connect some parameter are null hostUuid:" + hostUuid + " ,poolUuid:" + poolUuid
                    + " ,ipAddress:" + ipAddress;
            s_logger.debug(msg);
            throw new CloudRuntimeException(msg);
        }
        synchronized (poolUuid.intern()) {
            // Let's see if it is an existing connection.
            mConn = getConnect(poolUuid);
            if (mConn != null){
                try{
                    Host.getByUuid(mConn, hostUuid);
                    return mConn;
                } catch (Types.SessionInvalid e) {
                    s_logger.debug("Session thgrough ip " + mConn.getIp() + " is invalid for pool(" + poolUuid + ") due to " + e.toString());
                    try {
                        Session.loginWithPassword(mConn, mConn.getUsername(),
                            mConn.getPassword(), APIVersion.latest().toString());
                    } catch (Exception e1) {
                        if (s_logger.isDebugEnabled()) {
                            s_logger.debug("connect through IP(" + mConn.getIp() + " for pool(" + poolUuid + ") is broken due to " + e.toString());
                        }
                        removeConnect(poolUuid);
                        mConn = null;
                    }
                } catch (UuidInvalid e) {
                    String msg = "Host(" + hostUuid + ") doesn't belong to pool(" + poolUuid + "), please execute 'xe pool-join master-address=" + mConn.getIp()
                        + " master-username=" + mConn.getUsername() + " master-password=" + mConn.getPassword();
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(msg);
                    }
                    throw new CloudRuntimeException(msg, e);
                } catch (Exception e) { 
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("connect through IP(" + mConn.getIp() + " for pool(" + poolUuid + ") is broken due to " + e.toString());
                    }
                    removeConnect(poolUuid);
                    mConn = null;
                }
            }   
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Logging on as the slave to " + ipAddress);
            }
            try {
                sConn = new Connection(getURL(ipAddress));
                Session.slaveLocalLoginWithPassword(sConn,
                        username, password);
            } catch (Exception e){
                String msg = "Unable to create slave connection to host(" + hostUuid +") due to " + e.toString();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);                           
                }
                throw new CloudRuntimeException(msg, e);
            }
            Pool.Record pr = null;
            try {
                pr = getPoolRecord(sConn);
            } catch (Exception e) {
                PoolEmergencyTransitionToMaster(ipAddress, username, password);
                mConn = new XenServerConnection(getURL(ipAddress), ipAddress, username,
                        password, _retries, _interval, wait);
                try {
                    Session.loginWithPassword(mConn, username, password,
                        APIVersion.latest().toString());
                    pr = getPoolRecord(mConn);
                }  catch (Exception e1) {
                    String msg = "Unable to create master connection to host(" + hostUuid +") after transition it to master, due to " + e1.toString();
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(msg);                           
                    }
                    throw new CloudRuntimeException(msg, e1);
                }
                if ( !pr.uuid.equals(poolUuid) ) {
                    String msg = "host(" + hostUuid +") should be in pool(" + poolUuid + "), but it is actually in pool(" + pr.uuid + ")";
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug(msg);                           
                    }
                    throw new CloudRuntimeException(msg);
                } else {
                    ensurePoolIntegrity(mConn, ipAddress, username, password,
                            wait);
                    addConnect(poolUuid, mConn);
                    return mConn;
                }
            }
            if ( !pr.uuid.equals(poolUuid) ) {
                String msg = "host(" + hostUuid +") should be in pool(" + poolUuid + "), but it is actually in pool(" + pr.uuid + ")";
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);                           
                }
                throw new CloudRuntimeException(msg);
            }
            try {
                masterIp = pr.master.getAddress(sConn);
                mConn = new XenServerConnection(getURL(masterIp), masterIp, username,
                                password, _retries, _interval, wait);
                Session.loginWithPassword(mConn, username, password,
                                APIVersion.latest().toString());
                addConnect(poolUuid, mConn);
                return mConn;               
            } catch (Exception e) {
                String msg = "Unable to logon in " + masterIp + " as master in pool(" + poolUuid + ")";
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug(msg);                           
                }
                throw new CloudRuntimeException(msg);
            }
        }
    }

    static public Pool.Record getPoolRecord(Connection conn)
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



    public class XenServerConnection extends Connection {
        long _interval;
        int _retries;
        int _wait;
        String _ip;
        String _username;
        String _password;
        String _poolUuid;

        public XenServerConnection(URL url, String ip, String username, String password,
                int retries, int interval, int wait) {
            super(url);
            _wait = wait;
            _ip = ip;
            _retries = retries;
            _username = username;
            _password = password;
            _interval = (long) interval * 1000;
        }

        public int getWaitTimeout() {
            return _wait;
        }
        
        @Override
        protected XmlRpcClient getClientFromURL(URL url)
        {
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(url);
            config.setReplyTimeout(_wait * 1000);
            config.setConnectionTimeout(10000);
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);
            return client;
        }

        
        public String getPoolUuid() {
            return _poolUuid;
        }
        
        public String getUsername() {
            return _username;
        }
        
        public String getPassword() {
            return _password;
        }
        
        public String getIp() {
            return _ip;
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
                        s_logger.debug("Session is invalid for method: " + method_call + " due to " + e.getMessage() + ".  Reconnecting...retry="
                                + retries);
                        if (retries >= _retries) {
                            removeConnect(_poolUuid);
                            throw e;
                        }
                        Session.loginWithPassword(this, _username,
                                _password, APIVersion.latest().toString());
                        method_params[0] = getSessionReference();
                    } catch (XmlRpcException e) {
                        s_logger.debug("XmlRpcException for method: " + method_call + " due to " + e.getMessage() + ".  Reconnecting...retry="
                                + retries);
                        if (retries >= _retries) {
                            removeConnect(_poolUuid);
                            throw e;
                        }
                        Throwable cause = e.getCause();
                        if (cause == null || !(cause instanceof SocketException)) {
                            removeConnect(_poolUuid);
                            throw e;
                        }
                    } catch (Types.HostIsSlave e) {
                        s_logger.debug("HostIsSlave Exception for method: " + method_call + " due to " + e.getMessage() + ".  Reconnecting...retry="
                                + retries);
                        removeConnect(_poolUuid);
                        throw e;
                    }
    
                    try {
                        Thread.sleep(_interval);
                    } catch (InterruptedException e) {
                        s_logger.info("Who woke me from my slumber?");
                    }
                }
                assert false : "We should never get here";
                removeConnect(_poolUuid);
            }
            throw new CloudRuntimeException("After " + _retries
                    + " retries, we cannot contact the host ");
        }


    }
    
    public static class TrustAllManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        
        public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }
        
        public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }
        
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
            throws java.security.cert.CertificateException {
            return;
        }
        
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
            throws java.security.cert.CertificateException {
            return;
        }
    }
    
}