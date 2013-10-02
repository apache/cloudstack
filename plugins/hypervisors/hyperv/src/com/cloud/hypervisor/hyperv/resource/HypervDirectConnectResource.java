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
package com.cloud.hypervisor.hyperv.resource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.PingRoutingCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupRoutingCommand;
import com.cloud.agent.api.StartupStorageCommand;
import com.cloud.agent.api.UnsupportedAnswer;
import com.cloud.agent.api.StartupRoutingCommand.VmState;
import com.cloud.host.Host.Type;
import com.cloud.hypervisor.Hypervisor;
import com.cloud.network.Networks.RouterPrivateIpStrategy;
import com.cloud.resource.ServerResource;
import com.cloud.resource.ServerResourceBase;
import com.cloud.serializer.GsonHelper;
import com.google.gson.Gson;

/**
 * Implementation of dummy resource to be returned from discoverer.
 **/

public class HypervDirectConnectResource extends ServerResourceBase implements
        ServerResource {
    public static final int DEFAULT_AGENT_PORT = 8250;
    private static final Logger s_logger = Logger
            .getLogger(HypervDirectConnectResource.class.getName());

    private static final Gson s_gson = GsonHelper.getGson();
    private String _zoneId;
    private String _podId;
    private String _clusterId;
    private String _guid;
    private String _agentIp;
    private int _port = DEFAULT_AGENT_PORT;

    private String _clusterGuid;

    // Used by initialize to assert object configured before
    // initialize called.
    private boolean _configureCalled = false;

    private String _username;
    private String _password;

    @Override
    public final Type getType() {
        return Type.Routing;
    }

    @Override
    public final StartupCommand[] initialize() {
        // assert
        if (!_configureCalled) {
            String errMsg =
                    this.getClass().getName()
                            + " requires configure() be called before"
                            + " initialize()";
            s_logger.error(errMsg);
        }

        // Create default StartupRoutingCommand, then customise
        StartupRoutingCommand defaultStartRoutCmd =
                new StartupRoutingCommand(0, 0, 0, 0, null,
                        Hypervisor.HypervisorType.Hyperv,
                        RouterPrivateIpStrategy.HostLocal,
                        new HashMap<String, VmState>());

        // Identity within the data centre is decided by CloudStack kernel,
        // and passed via ServerResource.configure()
        defaultStartRoutCmd.setDataCenter(_zoneId);
        defaultStartRoutCmd.setPod(_podId);
        defaultStartRoutCmd.setCluster(_clusterId);
        defaultStartRoutCmd.setGuid(_guid);
        defaultStartRoutCmd.setName(_name);
        defaultStartRoutCmd.setPrivateIpAddress(_agentIp);
        defaultStartRoutCmd.setStorageIpAddress(_agentIp);
        defaultStartRoutCmd.setPool(_clusterGuid);

        s_logger.debug("Generated StartupRoutingCommand for _agentIp \""
                + _agentIp + "\"");

        // TODO: does version need to be hard coded.
        defaultStartRoutCmd.setVersion("4.2.0");

        // Specifics of the host's resource capacity and network configuration
        // comes from the host itself. CloudStack sanity checks network
        // configuration
        // and uses capacity info for resource allocation.
        Command[] startCmds =
                requestStartupCommand(new Command[] {defaultStartRoutCmd});

        // TODO: may throw, is this okay?
        StartupRoutingCommand startCmd = (StartupRoutingCommand) startCmds[0];

        // Assert that host identity is consistent with existing values.
        if (startCmd == null) {
            String errMsg =
                    String.format("Host %s (IP %s)"
                            + "did not return a StartupRoutingCommand",
                            _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getDataCenter().equals(
                defaultStartRoutCmd.getDataCenter())) {
            String errMsg =
                    String.format(
                            "Host %s (IP %s) changed zone/data center.  Was "
                                    + defaultStartRoutCmd.getDataCenter()
                                    + " NOW its " + startCmd.getDataCenter(),
                            _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getPod().equals(defaultStartRoutCmd.getPod())) {
            String errMsg =
                    String.format("Host %s (IP %s) changed pod.  Was "
                            + defaultStartRoutCmd.getPod() + " NOW its "
                            + startCmd.getPod(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getCluster().equals(defaultStartRoutCmd.getCluster())) {
            String errMsg =
                    String.format("Host %s (IP %s) changed cluster.  Was "
                            + defaultStartRoutCmd.getCluster() + " NOW its "
                            + startCmd.getCluster(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getGuid().equals(defaultStartRoutCmd.getGuid())) {
            String errMsg =
                    String.format("Host %s (IP %s) changed guid.  Was "
                            + defaultStartRoutCmd.getGuid() + " NOW its "
                            + startCmd.getGuid(), _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getPrivateIpAddress().equals(
                defaultStartRoutCmd.getPrivateIpAddress())) {
            String errMsg =
                    String.format("Host %s (IP %s) IP address.  Was "
                            + defaultStartRoutCmd.getPrivateIpAddress()
                            + " NOW its " + startCmd.getPrivateIpAddress(),
                            _name, _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }
        if (!startCmd.getName().equals(defaultStartRoutCmd.getName())) {
            String errMsg =
                    String.format(
                            "Host %s (IP %s) name.  Was " + startCmd.getName()
                                    + " NOW its "
                                    + defaultStartRoutCmd.getName(), _name,
                            _agentIp);
            s_logger.error(errMsg);
            // TODO: valid to return null, or should we throw?
            return null;
        }

        // Host will also supply details of an existing StoragePool if it has
        // been configured with one.
        //
        // NB: if the host was configured
        // with a local storage pool, CloudStack may not be able to use it
        // unless
        // it is has service offerings configured to recognise this storage
        // type.
        StartupStorageCommand storePoolCmd = null;
        if (startCmds.length > 1) {
            storePoolCmd = (StartupStorageCommand) startCmds[1];
            // TODO: is this assertion required?
            if (storePoolCmd == null) {
                String frmtStr =
                        "Host %s (IP %s) sent incorrect Command, "
                                + "second parameter should be a "
                                + "StartupStorageCommand";
                String errMsg = String.format(frmtStr, _name, _agentIp);
                s_logger.error(errMsg);
                // TODO: valid to return null, or should we throw?
                return null;
            }
            s_logger.info("Host " + _name + " (IP " + _agentIp
                    + ") already configured with a storeage pool, details "
                    + s_gson.toJson(startCmds[1]));
        } else {
            s_logger.info("Host " + _name + " (IP " + _agentIp
                    + ") already configured with a storeage pool, details ");
        }
        return new StartupCommand[] {startCmd, storePoolCmd};
    }

    @Override
    public final PingCommand getCurrentStatus(final long id) {
        PingCommand pingCmd = new PingRoutingCommand(getType(), id, null);

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping host " + _name + " (IP " + _agentIp + ")");
        }

        Answer pingAns = this.executeRequest(pingCmd);

        if (pingAns == null || !pingAns.getResult()) {
            s_logger.info("Cannot ping host " + _name + " (IP " + _agentIp
                    + "), pingAns (blank means null) is:" + pingAns);
            return null;
        }
        return pingCmd;
    }

    // TODO: Is it valid to return NULL, or should we throw on error?
    // Returns StartupCommand with fields revised with values known only to the
    // host
    public final Command[] requestStartupCommand(final Command[] cmd) {
        // Set HTTP POST destination URI
        // Using java.net.URI, see
        // http://docs.oracle.com/javase/1.5.0/docs/api/java/net/URI.html
        URI agentUri = null;
        try {
            String cmdName = StartupCommand.class.getName();
            agentUri =
                    new URI("http", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            // TODO add proper logging
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
            return null;
        }
        String incomingCmd = postHttpRequest(s_gson.toJson(cmd), agentUri);

        if (incomingCmd == null) {
            return null;
        }
        Command[] result = null;
        try {
            result = s_gson.fromJson(incomingCmd, Command[].class);
        } catch (Exception ex) {
            String errMsg = "Failed to deserialize Command[] " + incomingCmd;
            s_logger.error(errMsg, ex);
        }
        s_logger.debug("requestStartupCommand received response "
                + s_gson.toJson(result));
        if (result.length > 0) {
            return result;
        }
        return null;
    }

    // TODO: Is it valid to return NULL, or should we throw on error?
    @Override
    public final Answer executeRequest(final Command cmd) {
        // Set HTTP POST destination URI
        // Using java.net.URI, see
        // http://docs.oracle.com/javase/1.5.0/docs/api/java/net/URI.html
        URI agentUri = null;
        try {
            String cmdName = cmd.getClass().getName();
            agentUri =
                    new URI("http", null, _agentIp, _port,
                            "/api/HypervResource/" + cmdName, null, null);
        } catch (URISyntaxException e) {
            // TODO add proper logging
            String errMsg = "Could not generate URI for Hyper-V agent";
            s_logger.error(errMsg, e);
            return null;
        }
        String ansStr = postHttpRequest(s_gson.toJson(cmd), agentUri);

        if (ansStr == null) {
            return null;
        }
        // Only Answer instances are returned by remote agents.
        // E.g. see Response.getAnswers()
        Answer[] result = s_gson.fromJson(ansStr, Answer[].class);
        s_logger.debug("executeRequest received response "
                + s_gson.toJson(result));
        if (result.length > 0) {
            return result[0];
        }
        return null;
    }

    public static String postHttpRequest(final String jsonCmd,
            final URI agentUri) {
        // Using Apache's HttpClient for HTTP POST
        // Java-only approach discussed at on StackOverflow concludes with
        // comment to use Apache HttpClient
        // http://stackoverflow.com/a/2793153/939250, but final comment is to
        // use Apache.
        s_logger.debug("POST request to" + agentUri.toString()
                + " with contents" + jsonCmd);

        // Create request
        HttpClient httpClient = new DefaultHttpClient();
        String result = null;

        // TODO: are there timeout settings and worker thread settings to tweak?
        try {
            HttpPost request = new HttpPost(agentUri);

            // JSON encode command
            // Assumes command sits comfortably in a string, i.e. not used for
            // large data transfers
            StringEntity cmdJson = new StringEntity(jsonCmd);
            request.addHeader("content-type", "application/json");
            request.setEntity(cmdJson);
            s_logger.debug("Sending cmd to " + agentUri.toString()
                    + " cmd data:" + jsonCmd);
            HttpResponse response = httpClient.execute(request);

            // Unsupported commands will not route.
            if (response.getStatusLine().getStatusCode()
                == HttpStatus.SC_NOT_FOUND) {
                String errMsg =
                        "Failed to send : HTTP error code : "
                                + response.getStatusLine().getStatusCode();
                s_logger.error(errMsg);
                String unsupportMsg =
                        "Unsupported command "
                                + agentUri.getPath()
                                + ".  Are you sure you got the right type of"
                                + " server?";
                Answer ans = new UnsupportedAnswer(null, unsupportMsg);
                s_logger.error(ans);
                result = s_gson.toJson(new Answer[] {ans});
            } else if (response.getStatusLine().getStatusCode()
                != HttpStatus.SC_OK) {
                String errMsg =
                        "Failed send to " + agentUri.toString()
                                + " : HTTP error code : "
                                + response.getStatusLine().getStatusCode();
                s_logger.error(errMsg);
                return null;
            } else {
                result = EntityUtils.toString(response.getEntity());
                s_logger.debug("POST response is" + result);
            }
        } catch (ClientProtocolException protocolEx) {
            // Problem with HTTP message exchange
            s_logger.error(protocolEx);
        } catch (IOException connEx) {
            // Problem with underlying communications
            s_logger.error(connEx);
        } finally {
            httpClient.getConnectionManager().shutdown();
        }
        return result;
    }

    @Override
    protected final String getDefaultScriptsDir() {
        // TODO Auto-generated method stub
        return null;
    }

    // NB: 'params' can come from one of two places.
    // For a new host, HypervServerDiscoverer.find().
    // For an existing host, DiscovererBase.reloadResource().
    // In the later case, the params Map is populated with predefined keys
    // and custom keys from the database that were passed out by the find()
    // call.
    // the custom keys go by the variable name 'details'.
    // Thus, in find(), you see that 'details' are added to the params Map.
    @Override
    public final boolean configure(final String name,
            final Map<String, Object> params) throws ConfigurationException {
        /* todo: update, make consistent with the xen server equivalent. */
        _guid = (String) params.get("guid");
        _zoneId = (String) params.get("zone");
        _podId = (String) params.get("pod");
        _clusterId = (String) params.get("cluster");
        _agentIp = (String) params.get("ipaddress"); // was agentIp
        _name = name;

        _clusterGuid = (String) params.get("cluster.guid");
        _username = (String) params.get("url");
        _password = (String) params.get("password");
        _username = (String) params.get("username");

        _configureCalled = true;
        return true;
    }

    @Override
    public void setName(final String name) {
        // TODO Auto-generated method stub
    }

    @Override
    public void setConfigParams(final Map<String, Object> params) {
        // TODO Auto-generated method stub
    }

    @Override
    public final Map<String, Object> getConfigParams() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public final int getRunLevel() {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public void setRunLevel(final int level) {
        // TODO Auto-generated method stub
    }

}
