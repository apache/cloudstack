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

package com.cloud.hypervisor.ovm3.objects;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.TimeZone;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.TimingOutCallback;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.apache.xmlrpc.client.XmlRpcClientRequestImpl;

public class Connection extends XmlRpcClient {
    protected Logger logger = LogManager.getLogger(getClass());
    private final XmlRpcClientConfigImpl xmlClientConfig = new XmlRpcClientConfigImpl();
    private XmlRpcClient xmlClient;
    private String hostUser = null;
    private String hostPass = null;
    private String hostIp;
    private String hostName;
    private Integer hostPort = 8899;
    private Boolean hostUseSsl = false;
    private String cert = "";
    private String key = "";
    /* default to 20 mins ? */
    private Integer timeoutMs = 1200;
    private Integer timeoutS = timeoutMs * 1000;

    public Connection() {
    }

    public Connection(String ip, Integer port, String username, String password) {
        hostIp = ip;
        hostPort = port;
        hostUser = username;
        hostPass = password;
        xmlClient = setupXmlClient();
    }

    public Connection(String ip, String username, String password) {
        hostIp = ip;
        hostUser = username;
        hostPass = password;
        xmlClient = setupXmlClient();
    }

    private XmlRpcClient setupXmlClient() {
        final XmlRpcClient client = new XmlRpcClient();

        URL url;
        try {
            /* TODO: should add SSL checking here! */
            String prot = "http";
            if (hostUseSsl) {
                prot = "https";
            }
            url = new URL(prot + "://" + hostIp + ":" + hostPort.toString());
            xmlClientConfig.setTimeZone(TimeZone.getTimeZone("UTC"));
            xmlClientConfig.setServerURL(url);
            /* disable, we use asyncexecute to control timeout */
            xmlClientConfig.setReplyTimeout(0);
            /* default to 60 secs */
            xmlClientConfig.setConnectionTimeout(60000);
            /* reply time is 5 mins */
            xmlClientConfig.setReplyTimeout(60 * 15000);
            if (hostUser != null && hostPass != null) {
                logger.debug("Setting username " + hostUser);
                xmlClientConfig.setBasicUserName(hostUser);
                xmlClientConfig.setBasicPassword(hostPass);
            }
            xmlClientConfig.setXmlRpcServer(null);
            client.setConfig(xmlClientConfig);
            client.setTypeFactory(new RpcTypeFactory(client));
        } catch (MalformedURLException e) {
            logger.info("Incorrect URL: ", e);
        }
        return client;
    }

    public Object call(String method, List<?> params) throws XmlRpcException {
        return callTimeoutInSec(method, params, this.timeoutS);
    }

    public Object call(String method, List<?> params, boolean debug)
            throws XmlRpcException {
        return callTimeoutInSec(method, params, this.timeoutS, debug);
    }

    public Object callTimeoutInSec(String method, List<?> params, int timeout,
            boolean debug) throws XmlRpcException {
        TimingOutCallback callback = new TimingOutCallback(timeout * 1000);
        if (debug) {
            logger.debug("Call Ovm3 agent " + hostName + "(" + hostIp +"): " + method
                    + " with " + params);
        }
        long startTime = System.currentTimeMillis();
        try {
            /* returns actual xml */
            XmlRpcClientRequestImpl req = new XmlRpcClientRequestImpl(
                    xmlClient.getClientConfig(), method, params);
            xmlClient.executeAsync(req, callback);
            return callback.waitForResponse();
        } catch (TimingOutCallback.TimeoutException e) {
            logger.info("Timeout: ", e);
            throw new XmlRpcException(e.getMessage());
        } catch (XmlRpcException e) {
            logger.info("XML RPC Exception occurred: ", e);
            throw e;
        } catch (RuntimeException e) {
            logger.info("Runtime Exception: ", e);
            throw new XmlRpcException(e.getMessage());
        } catch (Throwable e) {
            logger.error("Holy crap batman!: ", e);
            throw new XmlRpcException(e.getMessage(), e);
        } finally {
            long endTime = System.currentTimeMillis();
            /* in seconds */
            float during = (endTime - startTime) / (float) 1000;
            logger.debug("Ovm3 call " + method + " finished in " + during
                    + " secs, on " + hostIp + ":" + hostPort);
        }
    }

    public Object callTimeoutInSec(String method, List<?> params, int timeout)
            throws XmlRpcException {
        return callTimeoutInSec(method, params, timeout, true);
    }

    public String getIp() {
        return hostIp;
    }

    public Integer getPort() {
        return hostPort;
    }

    public String getUserName() {
        return hostUser;
    }

    public void setUserName(String s) {
        hostUser = s;
    }

    public String getPassword() {
        return hostPass;
    }

    public Boolean getUseSsl() {
        return hostUseSsl;
    }

    public String getCert() {
        return cert;
    }

    public String getKey() {
        return key;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public void setIp(String agentIp) {
        hostIp = agentIp;
    }

    public void setPassword(String agentPassword) {
        hostPass = agentPassword;
    }
}
