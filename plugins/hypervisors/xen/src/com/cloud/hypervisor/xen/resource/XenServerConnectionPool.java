// Licensed to the Apache Software Foundation (ASF) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The ASF licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the 
// specific language governing permissions and limitations
// under the License.
package com.cloud.hypervisor.xen.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.SocketException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClientException;

import com.cloud.utils.NumbersUtil;
import com.cloud.utils.PropertiesUtil;
import com.cloud.utils.exception.CloudRuntimeException;
import com.xensource.xenapi.APIVersion;
import com.xensource.xenapi.Connection;
import com.xensource.xenapi.Host;
import com.xensource.xenapi.Pool;
import com.xensource.xenapi.Session;
import com.xensource.xenapi.Types;
import com.xensource.xenapi.Types.BadServerResponse;
import com.xensource.xenapi.Types.UuidInvalid;
import com.xensource.xenapi.Types.XenAPIException;

public class XenServerConnectionPool {
    private static final Logger s_logger = Logger.getLogger(XenServerConnectionPool.class);
    protected HashMap<String /* poolUuid */, XenServerConnection> _conns = new HashMap<String, XenServerConnection>();
    protected int _retries;
    protected int _interval;
    protected static long s_sleepOnError = 10 * 1000; // in ms
    static {
        File file = PropertiesUtil.findConfigFile("environment.properties");
        if (file == null) {
            s_logger.debug("Unable to find environment.properties");
        } else {
            FileInputStream finputstream;
            try {
                finputstream = new FileInputStream(file);
                final Properties props = new Properties();
                props.load(finputstream);
                finputstream.close();
                String search = props.getProperty("sleep.interval.on.error");
                if (search != null) {
                    s_sleepOnError = NumbersUtil.parseInterval(search,  10) * 1000;
                }
                s_logger.info("XenServer Connection Pool Configs: sleep.interval.on.error=" + s_sleepOnError);
            } catch (FileNotFoundException e) {
                s_logger.debug("File is not found", e);
            } catch (IOException e) {
                s_logger.debug("IO Exception while reading file", e);
            }
        }
        try {
            javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1]; 
            javax.net.ssl.TrustManager tm = new TrustAllManager(); 
            trustAllCerts[0] = tm; 
            javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("TLS"); 
            sc.init(null, trustAllCerts, null); 
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier hv = new HostnameVerifier() {
                @Override
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

    public boolean joinPool(Connection conn, String hostIp, String masterIp, String username, Queue<String> password) {      
        try {
            join(conn, masterIp, username, password);
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
                    slaveConn = new Connection(getURL(hostIp), 10);
                    slaveSession = slaveLocalLoginWithPassword(slaveConn, username, password);
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
                    Thread.sleep(3000);
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

   
    public Connection getConnect(String ip, String username, Queue<String> password) {
        Connection conn = new Connection(getURL(ip), 10);
        try {
            loginWithPassword(conn, username, password, APIVersion.latest().toString());
        }  catch (Types.HostIsSlave e) {
            String maddress = e.masterIPAddress;
            conn = new Connection(getURL(maddress), 10);
            try {
                loginWithPassword(conn, username, password, APIVersion.latest().toString());
            }  catch (Exception e1) {
                String msg = "Unable to create master connection to host(" + maddress +") , due to " + e1.toString();
                s_logger.debug(msg);
                throw new CloudRuntimeException(msg, e1);     
            }
        } catch (Exception e) {
            String msg = "Unable to create master connection to host(" + ip +") , due to " + e.toString();
            s_logger.debug(msg);
           throw new CloudRuntimeException(msg, e);               	
        }
        return conn;
    }
  
    public URL getURL(String ip){
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
            String username, Queue<String> password, int wait) {
        XenServerConnection mConn = null;
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
                } catch (Exception e) { 
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("connect through IP(" + mConn.getIp() + " for pool(" + poolUuid + ") is broken due to " + e.toString());
                    }
                    removeConnect(poolUuid);
                    mConn = null;
                }
            }   
            
            if ( mConn == null ) {
                mConn = new XenServerConnection(getURL(ipAddress), ipAddress, username, password, _retries, _interval, wait);
                try {
                    loginWithPassword(mConn, username, password, APIVersion.latest().toString());
                }  catch (Types.HostIsSlave e) {
                	String maddress = e.masterIPAddress;
                    mConn = new XenServerConnection(getURL(maddress), maddress, username, password, _retries, _interval, wait);
                    try {
                        loginWithPassword(mConn, username, password, APIVersion.latest().toString());
                    }  catch (Exception e1) {
                        String msg = "Unable to create master connection to host(" + maddress +") , due to " + e1.toString();
                        s_logger.debug(msg);
                        throw new CloudRuntimeException(msg, e1);
                        
                    }                   
                } catch (Exception e) {
                    String msg = "Unable to create master connection to host(" + ipAddress +") , due to " + e.toString();
                    s_logger.debug(msg);
                    throw new CloudRuntimeException(msg, e);               	
                }
                addConnect(poolUuid, mConn);
            }
        }
    
        return mConn;
    }
    

    protected Session slaveLocalLoginWithPassword(Connection conn, String username, Queue<String> password) throws
            BadServerResponse,
            XenAPIException,
            XmlRpcException {
        Session s = null;
        boolean logged_in=false;
        Exception ex = null;
        while (!logged_in){
            try {
                s = Session.slaveLocalLoginWithPassword(conn, username, password.peek());
                logged_in=true;
            } catch (BadServerResponse e) {
                logged_in=false; ex = e;
            } catch (XenAPIException e) {
                logged_in=false; ex = e;
            } catch (XmlRpcException e) {
                logged_in=false; ex = e;
            }
            if (logged_in && conn != null){
                break;
            }
            else {
                if (password.size() > 1){
                    password.remove();
                    continue;
                }
                else {
                    // the last password did not work leave it and flag error
                    if (ex instanceof BadServerResponse){
                        throw (BadServerResponse)ex;
                    }
                    else if (ex instanceof XmlRpcException){
                        throw (XmlRpcException)ex;
                    }
                    else if (ex instanceof Types.SessionAuthenticationFailed){
                        throw (Types.SessionAuthenticationFailed)ex;
                    }
                    else if (ex instanceof XenAPIException){
                        throw (XenAPIException)ex;
                    }
                    break;
                }
            }
        }
        return s;
    }
    

    protected Session loginWithPassword(Connection conn, String username, Queue<String> password, String version)throws
            BadServerResponse,
            XenAPIException,
            XmlRpcException  {
        Session s = null;
        boolean logged_in=false;
        Exception ex = null;
        while (!logged_in){
            try {
                s = Session.loginWithPassword(conn, username, password.peek(), APIVersion.latest().toString());
                logged_in=true;
            } catch (BadServerResponse e) {
                logged_in=false; ex = e;
            } catch (XenAPIException e) {
                logged_in=false; ex = e;
            } catch (XmlRpcException e) {
                logged_in=false; ex = e;
            } 
            
            if (logged_in && conn != null){
                break;
            }
            else {
                if (password.size() > 1){
                    password.remove();
                    continue;
                }
                else {
                    // the last password did not work leave it and flag error
                    if (ex instanceof BadServerResponse){
                        throw (BadServerResponse)ex;
                    }
                    else if (ex instanceof XmlRpcException){
                        throw (XmlRpcException)ex;
                    }
                    else if (ex instanceof Types.SessionAuthenticationFailed){
                        throw (Types.SessionAuthenticationFailed)ex;
                    }
                    else if (ex instanceof XenAPIException){
                        throw (XenAPIException)ex;
                    }
                }
            }
        }
        return s;
    }
    
   
    protected void  join(Connection conn, String masterIp, String username, Queue<String> password) throws
            BadServerResponse,
            XenAPIException,
            XmlRpcException,
            Types.JoiningHostCannotContainSharedSrs {
     
        boolean logged_in=false;
        Exception ex = null;
        while (!logged_in){
            try {
                Pool.join(conn, masterIp, username, password.peek());
                logged_in=true;
            } catch (BadServerResponse e) {
                logged_in=false; ex = e;
            } catch (XenAPIException e) {
                logged_in=false; ex = e;
            } catch (XmlRpcException e) {
                logged_in=false; ex = e;
            }
            if (logged_in && conn != null){
                break;
            }
            else {
                if (password.size() > 1){
                    password.remove();
                    continue;
                }
                else {
                    // the last password did not work leave it and flag error
                    if (ex instanceof BadServerResponse){
                        throw (BadServerResponse)ex;
                    }
                    else if (ex instanceof XmlRpcException){
                        throw (XmlRpcException)ex;
                    }
                    else if (ex instanceof Types.SessionAuthenticationFailed){
                        throw (Types.SessionAuthenticationFailed)ex;
                    }
                    else if (ex instanceof XenAPIException){
                        throw (XenAPIException)ex;
                    }
                    break;
                }
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
        String _ip;
        String _username;
        Queue<String> _password;
        String _poolUuid;

        public XenServerConnection(URL url, String ip, String username, Queue<String> password,
                int retries, int interval, int wait) {
            super(url, wait);
            _ip = ip;
            _retries = retries;
            _username = username;
            _password = password;
            _interval = (long) interval * 1000;

        }
        
        public String getPoolUuid() {
            return _poolUuid;
        }
        
        public String getUsername() {
            return _username;
        }
        
        public Queue<String> getPassword() {
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
                        loginWithPassword(this, _username, _password, APIVersion.latest().toString());
                        method_params[0] = getSessionReference();
                    } catch (XmlRpcClientException e) {
                        s_logger.debug("XmlRpcClientException for method: " + method_call + " due to " + e.getMessage()); 
                        removeConnect(_poolUuid);
                        throw e;
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
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        
        public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }
        
        public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
            return true;
        }
        
        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
            throws java.security.cert.CertificateException {
            return;
        }
        
        @Override
        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
            throws java.security.cert.CertificateException {
            return;
        }
    }
    
}
