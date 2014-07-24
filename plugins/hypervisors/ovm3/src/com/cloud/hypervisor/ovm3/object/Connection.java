/*******************************************************************************
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package com.cloud.hypervisor.ovm3.object;

import java.net.URL;
import java.util.TimeZone;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.TimingOutCallback;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

public class Connection extends XmlRpcClient {
    private static final Logger s_logger = Logger.getLogger(Connection.class);
    private final XmlRpcClientConfigImpl _config = new XmlRpcClientConfigImpl();
    private XmlRpcClient _client;
    private String _username;
    private String _password;
    private String _ip;
    private Integer _port = 8898;
    private Boolean _isSsl = false;
    private String cert = "";
    private String key = "";
    private Integer timeout = 1200; /* seconds */
    private Integer _timeout = timeout * 1000; /* native is ms */

    private XmlRpcClient getXmlClient() throws XmlRpcException {
        // check ssl boolean
        final XmlRpcClient client = new XmlRpcClient();

        URL url;
        try {
            // here too
            url = new URL("http://" + _ip + ":" + _port.toString());
            _config.setTimeZone(TimeZone.getTimeZone("UTC"));
            _config.setServerURL(url);
            _config.setReplyTimeout(0); // disable, we use asyncexecute to
                                        // control timeout
            _config.setConnectionTimeout(60000);
            _config.setReplyTimeout(60 * 15000);
            _config.setBasicUserName(_username);
            _config.setBasicPassword(_password);
            _config.setXmlRpcServer(null);
            // _config.setEnabledForExtensions(true);
            client.setConfig(_config);
            client.setTypeFactory(new RpcTypeFactory(client));
        } catch (Exception e) {
            throw new XmlRpcException(e.getMessage());
        }
        return client;
    }

    public Connection(String ip, Integer port, String username, String password)
            throws XmlRpcException {
        _ip = ip;
        _port = port;
        _username = username;
        _password = password;
        _client = getXmlClient();
    }

    public Connection(String ip, String username, String password)
            throws XmlRpcException {
        _ip = ip;
        _username = username;
        _password = password;
        _client = getXmlClient();
    }

    public Object call(String method, Vector<?> params) throws XmlRpcException {
        /* default timeout is 10 mins */
        return callTimeoutInSec(method, params, this._timeout);
    }

    public Object call(String method, Vector<?> params, boolean debug)
            throws XmlRpcException {
        /* default timeout is 10 mins */
        return callTimeoutInSec(method, params, this._timeout, debug);
    }

    public Object callTimeoutInSec(String method, Vector<?> params,
            int timeout, boolean debug) throws XmlRpcException {
        TimingOutCallback callback = new TimingOutCallback(timeout * 1000);
        if (debug) {
            /*
             * some parameters including user password should not be printed in
             * log
             */
            s_logger.debug("Call Ovm3 agent: " + method + " with " + params);
        }

        long startTime = System.currentTimeMillis();
        _client.executeAsync(method, params, callback);
        try {
            return callback.waitForResponse();
        } catch (TimingOutCallback.TimeoutException to) {
            throw to;
        } catch (Throwable e) {
            throw new XmlRpcException(-2, e.getMessage());
        } finally {
            long endTime = System.currentTimeMillis();
            float during = (endTime - startTime) / 1000; // in secs
            s_logger.debug("Ovm3 call " + method + " finished in " + during
                    + " secs, on " + _ip + ":" + _port);
        }
    }

    public Object callTimeoutInSec(String method, Vector<?> params, int timeout)
            throws XmlRpcException {
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
