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
package com.cloud.ovm.object;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.TimeZone;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.TimingOutCallback;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import com.cloud.utils.exception.CloudRuntimeException;

public class Connection {
    protected Logger logger = LogManager.getLogger(getClass());
    private XmlRpcClientConfigImpl _config = new XmlRpcClientConfigImpl();
    XmlRpcClient _client;
    String _username;
    String _password;
    String _ip;
    Integer _port = 8899;
    Boolean _isSsl = false;

    private XmlRpcClient getXmlClient() {
        XmlRpcClient client = new XmlRpcClient();

        URL url;
        try {
            url = new URL("http://" + _ip + ":" + _port.toString());
            _config.setTimeZone(TimeZone.getTimeZone("UTC"));
            _config.setServerURL(url);
            _config.setReplyTimeout(0); // disable, we use asyncexecute to control timeout
            _config.setConnectionTimeout(6000);
            _config.setBasicUserName(_username);
            _config.setBasicPassword(_password);
            client.setConfig(_config);
        } catch (MalformedURLException e) {
            throw new CloudRuntimeException(e.getMessage());
        }

        return client;
    }

    public Connection(String ip, Integer port, String username, String password) {
        _ip = ip;
        _port = port;
        _username = username;
        _password = password;
        _client = getXmlClient();
    }

    public Connection(String ip, String username, String password) {
        _ip = ip;
        _username = username;
        _password = password;
        _client = getXmlClient();
    }

    public Object call(String method, Object[] params) throws XmlRpcException {
        /* default timeout is 10 mins */
        return callTimeoutInSec(method, params, 600);
    }

    public Object call(String method, Object[] params, boolean debug) throws XmlRpcException {
        /* default timeout is 10 mins */
        return callTimeoutInSec(method, params, 600, debug);
    }

    public Object callTimeoutInSec(String method, Object[] params, int timeout, boolean debug) throws XmlRpcException {
        TimingOutCallback callback = new TimingOutCallback(timeout * 1000);
        Object[] mParams = new Object[params.length + 1];
        mParams[0] = method;
        for (int i = 0; i < params.length; i++) {
            mParams[i + 1] = params[i];
        }

        if (debug) {
            /*
             * some parameters including user password should not be printed in log
             */
            logger.debug("Call Ovm agent: " + Coder.toJson(mParams));
        }

        long startTime = System.currentTimeMillis();
        _client.executeAsync("OvmDispatch", mParams, callback);
        try {
            return callback.waitForResponse();
        } catch (TimingOutCallback.TimeoutException to) {
            throw to;
        } catch (Throwable e) {
            throw new XmlRpcException(-2, e.getMessage());
        } finally {
            long endTime = System.currentTimeMillis();
            long during = (endTime - startTime) / 1000; // in secs
            logger.debug("Ovm call " + method + " finished in " + String.valueOf(during) + " secs");
        }
    }

    public Object callTimeoutInSec(String method, Object[] params, int timeout) throws XmlRpcException {
        return callTimeoutInSec(method, params, timeout, true);
    }

    public String getIp() {
        return _ip;
    }

    public Integer getPort() {
        return _port;
    }

    public String getUserName() {
        return _username;
    }

    public String getPassword() {
        return _password;
    }

    public Boolean getIsSsl() {
        return _isSsl;
    }
}
