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
package com.cloud.network.resource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.BasicClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;

import com.cloud.agent.IAgentControl;
import com.cloud.agent.api.Answer;
import com.cloud.agent.api.Command;
import com.cloud.agent.api.ExternalNetworkResourceUsageAnswer;
import com.cloud.agent.api.ExternalNetworkResourceUsageCommand;
import com.cloud.agent.api.MaintainAnswer;
import com.cloud.agent.api.MaintainCommand;
import com.cloud.agent.api.NetScalerImplementNetworkCommand;
import com.cloud.agent.api.PingCommand;
import com.cloud.agent.api.ReadyAnswer;
import com.cloud.agent.api.ReadyCommand;
import com.cloud.agent.api.StartupCommand;
import com.cloud.agent.api.StartupExternalLoadBalancerCommand;
import com.cloud.agent.api.UnsupportedAnswer;
import com.cloud.agent.api.routing.DestroyLoadBalancerApplianceCommand;
import com.cloud.agent.api.routing.GlobalLoadBalancerConfigCommand;
import com.cloud.agent.api.routing.HealthCheckLBConfigAnswer;
import com.cloud.agent.api.routing.HealthCheckLBConfigCommand;
import com.cloud.agent.api.routing.IpAssocAnswer;
import com.cloud.agent.api.routing.IpAssocCommand;
import com.cloud.agent.api.routing.LoadBalancerConfigCommand;
import com.cloud.agent.api.routing.SetStaticNatRulesCommand;
import com.cloud.agent.api.to.IpAddressTO;
import com.cloud.agent.api.to.LoadBalancerTO;
import com.cloud.agent.api.to.LoadBalancerTO.DestinationTO;
import com.cloud.agent.api.to.LoadBalancerTO.StickinessPolicyTO;
import com.cloud.host.Host;
import com.cloud.host.Host.Type;
import com.cloud.network.rules.LbStickinessMethod.StickinessMethodType;
import com.cloud.resource.ServerResource;
import com.cloud.serializer.GsonHelper;
import com.cloud.utils.NumbersUtil;
import com.cloud.utils.exception.ExecutionException;
import com.cloud.utils.net.NetUtils;

class NccHttpCode {
    static final String INTERNAL_ERROR  = "INTERNAL ERROR";
    static final String NOT_FOUND = "NOT FOUND";
    static final String JOB_ID = "Job_id";
    static final String UNAUTHORIZED  = "UNAUTHORIZED";
}

public class NetScalerControlCenterResource implements ServerResource {

    // deployment configuration
    private String _name;
    private String _zoneId;
    private String _ip;
    private String _username;
    private String _password;
    private String _publicInterface;
    private String _privateInterface;
    private Integer _numRetries;
    private Long _nccCmdTimeout;
    private String _guid;
    private boolean _inline;
    private String _deviceName;
    private String _sessionid;
    public static final int DEFAULT_PORT = 443;
    private static final Gson s_gson = GsonHelper.getGson();
    private static final Logger s_logger = Logger.getLogger(NetScalerControlCenterResource.class);
    protected Gson _gson;
    private final String _objectNamePathSep = "-";
    final String protocol="https";
    private static String nccsession;
    private int pingCount = 0;

    public NetScalerControlCenterResource() {
        _gson = GsonHelper.getGsonLogger();
    }


    public static String get_nccsession() {
        return nccsession;
    }


    public static void set_nccsession(String nccsession) {
        NetScalerControlCenterResource.nccsession = nccsession;
    }


    @Override
    public boolean configure(String name, Map<String, Object> params) throws ConfigurationException {
        JSONObject jsonResponse = null;
        try {
            _name = (String)params.get("name");
            if (_name == null) {
                throw new ConfigurationException("Unable to find name in the configuration parameters");
            }

            _zoneId = (String)params.get("zoneId");
            if (_zoneId == null) {
                throw new ConfigurationException("Unable to find zone Id  in the configuration parameters");
            }

            _ip = (String)params.get("ip");
            if (_ip == null) {
                throw new ConfigurationException("Unable to find IP address in the configuration parameters");
            }

            _username = (String)params.get("username");
            if (_username == null) {
                throw new ConfigurationException("Unable to find username in the configuration parameters");
            }

            _password =  (String)params.get("password");
            if (_password == null) {
                throw new ConfigurationException("Unable to find password in the configuration parameters");
            }

            _numRetries = NumbersUtil.parseInt((String)params.get("numretries"), 2);

            _guid = (String)params.get("guid");
            if (_guid == null) {
                throw new ConfigurationException("Unable to find the guid in the configuration parameters");
            }

            _deviceName = (String)params.get("deviceName");
            if (_deviceName == null) {
                throw new ConfigurationException("Unable to find the device name in the configuration parameters");
            }
            _nccCmdTimeout = NumbersUtil.parseLong((String)params.get("numretries"), 600000);

            // validate device configuration parameters
            login();

            return true;
        } catch (ConfigurationException e) {
            throw new ConfigurationException(e.getMessage());
        } catch (ExecutionException e) {
            s_logger.debug("Execution Exception :" +  e.getMessage());
            throw new ConfigurationException("Failed to add the device. Please check the device is NCC and It is reachable from Management Server.");
        }
    }

    public void getServicePackages() throws ExecutionException {
            String result = null;
            try {
                URI agentUri = null;
                agentUri =
                        new URI("https", null, _ip, DEFAULT_PORT,
                                "/admin/v1/servicepackages", null, null);

                org.json.JSONObject jsonBody = new JSONObject();
                org.json.JSONObject jsonCredentials = new JSONObject();
                result = getHttpRequest(jsonBody.toString(), agentUri, _sessionid);
                s_logger.debug("List of Service Packages in NCC:: " + result);
                } catch (URISyntaxException e) {
                    String errMsg = "Could not generate URI for Hyper-V agent";
                    s_logger.error(errMsg, e);

                } catch (Exception e) {
                throw new ExecutionException("Failed to log in to NCC device at " + _ip + " due to " + e.getMessage());
            }
    }

    private synchronized String login() throws ExecutionException{
        String result = null;
        JSONObject jsonResponse = null;
        try {
            URI agentUri = null;
            agentUri =
                    new URI("https", null, _ip, DEFAULT_PORT,
                            "/nitro/v2/config/" + "login", null, null);
            org.json.JSONObject jsonBody = new JSONObject();
            org.json.JSONObject jsonCredentials = new JSONObject();
            jsonCredentials.put("username", _username);
            jsonCredentials.put("password", _password);
            jsonBody.put("login", jsonCredentials);

            result = postHttpRequest(jsonBody.toString(), agentUri, _sessionid);
            if (result == null) {
                throw new ConfigurationException("No Response Received from the NetScalerControlCenter Device");
            } else {
                jsonResponse = new JSONObject(result);
                org.json.JSONArray loginResponse = jsonResponse.getJSONArray("login");
                _sessionid = jsonResponse.getJSONArray("login").getJSONObject(0).getString("sessionid");
                s_logger.debug("New Session id from NCC :" + _sessionid);
                set_nccsession(_sessionid);
                s_logger.debug("session on Static Session variable" + get_nccsession());
            }
            s_logger.debug("Login to NCC Device response :: " + result);
            return result;
            } catch (URISyntaxException e) {
                String errMsg = "Could not generate URI for Hyper-V agent";
                s_logger.error(errMsg, e);

            } catch (JSONException e) {
                s_logger.debug("JSON Exception :" +  e.getMessage());
                throw new ExecutionException("Failed to log in to NCC device at " + _ip + " due to " + e.getMessage());
            } catch (Exception e) {
            throw new ExecutionException("Failed to log in to NCC device at " + _ip + " due to " + e.getMessage());
        }
        return result;
    }

    @Override
    public StartupCommand[] initialize() {
        StartupExternalLoadBalancerCommand cmd = new StartupExternalLoadBalancerCommand(Host.Type.NetScalerControlCenter);
        cmd.setName(_name);
        cmd.setDataCenter(_zoneId);
        cmd.setPod("");
        cmd.setPrivateIpAddress(_ip);
        cmd.setStorageIpAddress("");
        cmd.setVersion(NetScalerControlCenterResource.class.getPackage().getImplementationVersion());
        cmd.setGuid(_guid);
        return new StartupCommand[] {cmd};
    }

    @Override
    public Answer executeRequest(Command cmd) {
        return executeRequest(cmd, _numRetries);
    }

    private Answer executeRequest(Command cmd, int numRetries) {
        if (cmd instanceof ReadyCommand) {
            return execute((ReadyCommand)cmd);
        } else if (cmd instanceof MaintainCommand) {
            return execute((MaintainCommand)cmd);
        } else if (cmd instanceof IpAssocCommand) {
            return execute((IpAssocCommand)cmd, numRetries);
        } else if (cmd instanceof LoadBalancerConfigCommand) {
            return execute((LoadBalancerConfigCommand)cmd, numRetries);
        } else if (cmd instanceof ExternalNetworkResourceUsageCommand) {
            return execute((ExternalNetworkResourceUsageCommand)cmd, numRetries);
        } else if (cmd instanceof DestroyLoadBalancerApplianceCommand) {
            return Answer.createUnsupportedCommandAnswer(cmd);
        } else if (cmd instanceof SetStaticNatRulesCommand) {
            return execute((SetStaticNatRulesCommand)cmd, numRetries);
        } else if (cmd instanceof GlobalLoadBalancerConfigCommand) {
            return Answer.createUnsupportedCommandAnswer(cmd);
        } else if (cmd instanceof HealthCheckLBConfigCommand) {
            return execute((HealthCheckLBConfigCommand)cmd, numRetries);
        } else if (cmd instanceof NetScalerImplementNetworkCommand ) {
            return execute((NetScalerImplementNetworkCommand)cmd, numRetries);
        }
        else {
            return Answer.createUnsupportedCommandAnswer(cmd);
        }
    }

    private Answer execute(ReadyCommand cmd) {
        return new ReadyAnswer(cmd);
    }

    protected Answer execute(MaintainCommand cmd) {
        return new MaintainAnswer(cmd);
    }

    private void keepSessionAlive() throws ExecutionException {
        URI agentUri = null;
        try {
            agentUri =
                    new URI("https", null, _ip, DEFAULT_PORT,
                            "/cs/cca/v1/cloudstacks", null, null);
            org.json.JSONObject jsonBody = new JSONObject();
            getHttpRequest(jsonBody.toString(), agentUri, _sessionid);
            s_logger.debug("Keeping Session Alive");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private String queryAsyncJob(String jobId) throws ExecutionException {
        String result = null;
        try {
            URI agentUri = null;
            agentUri =
                    new URI("https", null, _ip, DEFAULT_PORT,
                            "/admin/v1/journalcontexts/" + jobId, null, null);

            org.json.JSONObject jsonBody = new JSONObject();

            long startTick = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTick <  _nccCmdTimeout) {
                result = getHttpRequest(jsonBody.toString(), agentUri, _sessionid);
                JSONObject response = new JSONObject(result);
                if(response != null ) {
                    s_logger.debug("Job Status result for ["+jobId + "]:: " + result + " Tick and currentTime :" +  System.currentTimeMillis() +" -" + startTick + "job cmd timeout :" +_nccCmdTimeout);
                    String status = response.getJSONObject("journalcontext").getString("status").toUpperCase();
                    String message = response.getJSONObject("journalcontext").getString("message");
                    s_logger.debug("Job Status Progress Status ["+ jobId + "]:: " + status);
                    switch(status) {
                    case "FINISHED":
                            return status;
                    case "IN PROGRESS":
                        break;
                    case "ERROR, ROLLBACK IN PROGRESS":
                        break;
                    case "ERROR, ROLLBACK COMPLETED":
                        throw new ExecutionException("ERROR, ROLLBACK COMPLETED " + message);
                    case "ERROR, ROLLBACK FAILED":
                        throw new ExecutionException("ERROR, ROLLBACK FAILED " + message);
                    }
                }
            }

        } catch (URISyntaxException e) {
            String errMsg = "Could not generate URI for NetScaler ControlCenter";
            s_logger.error(errMsg, e);
          } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
    private synchronized Answer execute(NetScalerImplementNetworkCommand cmd, int numRetries) {
        String result = null;
        try {
            URI agentUri = null;
            agentUri =
                    new URI("https", null, _ip, DEFAULT_PORT,
                            "/cs/adcaas/v1/networks", null, null);
            org.json.JSONObject jsonBody = new JSONObject(cmd.getDetails());
            s_logger.debug("Sending Network Implement to NCC:: " + jsonBody);
            result = postHttpRequest(jsonBody.toString(), agentUri, _sessionid);
            s_logger.debug("Result of Network Implement to NCC:: " + result);
            result = queryAsyncJob(result);
            s_logger.debug("Done query async of network implement request :: " + result);
            return new Answer(cmd, true, "Successfully allocated device");
            } catch (URISyntaxException e) {
                String errMsg = "Could not generate URI for NetScaler ControlCenter ";
                s_logger.error(errMsg, e);
            } catch (ExecutionException e) {
                if(e.getMessage().equalsIgnoreCase(NccHttpCode.NOT_FOUND)) {
                    return new Answer(cmd, true, "Successfully unallocated the device");
                }else if(e.getMessage().startsWith("ERROR, ROLLBACK") ) {
                    s_logger.error(e.getMessage());
                    return new Answer(cmd, false, e.getMessage());
                }
                else {
                    if (shouldRetry(numRetries)) {
                        s_logger.debug("Retrying the command NetScalerImplementNetworkCommand retry count: " + numRetries, e);
                        return retry(cmd, numRetries);
                    } else {
                        return new Answer(cmd, false, e.getMessage());
                    }
                }
            } catch (Exception e) {
                if (shouldRetry(numRetries)) {
                    s_logger.debug("Retrying the command NetScalerImplementNetworkCommand retry count: " + numRetries, e);
                    return retry(cmd, numRetries);
                } else {
                    return new Answer(cmd, false, e.getMessage());
                }
            }

        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    private synchronized Answer execute(IpAssocCommand cmd,  int numRetries) {

        String[] results = new String[cmd.getIpAddresses().length];
        int i = 0;
        IpAddressTO[] ips = cmd.getIpAddresses();
        for (IpAddressTO ip : ips) {
            results[i++] = ip.getPublicIp() + " - success";
        }

        return new IpAssocAnswer(cmd, results);
    }

    private Answer execute(HealthCheckLBConfigCommand cmd, int numRetries) {

        List<LoadBalancerTO> hcLB = new ArrayList<LoadBalancerTO>();
        try {

            LoadBalancerTO[] loadBalancers = cmd.getLoadBalancers();

            if (loadBalancers == null) {
                return new HealthCheckLBConfigAnswer(hcLB);
            }
            String result = getLBHealthChecks(cmd.getNetworkId());
            JSONObject res =  new JSONObject(result);
            JSONArray lbstatus = res.getJSONArray("lbhealthstatus");
            for(int i=0; i<lbstatus.length(); i++) {
                JSONObject lbstat = lbstatus.getJSONObject(i);
                LoadBalancerTO loadBalancer = null;
                JSONArray dest = lbstat.getJSONArray("destinations");
                List<DestinationTO> listDestTo = new ArrayList<DestinationTO>();
                for(int d=0; d<dest.length(); d++ ) {
                    JSONObject dt = dest.getJSONObject(d);
                    if( dt!=null ) {
                        DestinationTO destTO = new DestinationTO(dt.getString("destIp"), dt.getInt("destPort"), dt.getString("monitorState"));
                        listDestTo.add(destTO);
                    }
                }
                loadBalancer = new LoadBalancerTO(lbstat.getString("lb_uuid"),listDestTo);
                hcLB.add(loadBalancer);
            }
        } catch (ExecutionException e) {
            s_logger.error("Failed to execute HealthCheckLBConfigCommand due to ", e);
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new HealthCheckLBConfigAnswer(hcLB);
            }
        } catch (Exception e) {
            s_logger.error("Failed to execute HealthCheckLBConfigCommand due to ", e);
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new HealthCheckLBConfigAnswer(hcLB);
            }
        }
        return new HealthCheckLBConfigAnswer(hcLB);
    }

    private String getLBHealthChecks(long networkid) throws ExecutionException  {
        URI agentUri = null;
        String response = null;
        try {
            agentUri =
                    new URI("https", null, _ip, DEFAULT_PORT,
                            "/cs/adcaas/v1/networks/"+ networkid +"/lbhealthstatus", null, null);
            org.json.JSONObject jsonBody = new JSONObject();
            response = getHttpRequest(jsonBody.toString(), agentUri, _sessionid);
            s_logger.debug("LBHealthcheck Response :" + response);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return response;
    }
    private synchronized Answer execute(LoadBalancerConfigCommand cmd, int numRetries) {
        try {
            LoadBalancerTO[] loadBalancers = cmd.getLoadBalancers();
            if (loadBalancers == null) {
                return new Answer(cmd);
            }
            JSONObject lbConfigPaylod = new JSONObject(cmd);
            String gsonLBConfig =  _gson.toJson(cmd);
            URI agentUri = null;
            agentUri =
                    new URI("https", null, _ip, DEFAULT_PORT,
                            "/cs/adcaas/v1/loadbalancerCmds", null, null);
            JSONObject lbConfigCmd = new JSONObject();
            JSONObject lbcmd = new JSONObject(gsonLBConfig);
            s_logger.debug("LB config from gsonstring to JSONObject : " +  lbcmd.toString() + "\n" + "gson cmd is :: \t" + gsonLBConfig);
            lbConfigCmd.put("LoadBalancerConfigCommand",  lbcmd.getJSONArray("loadBalancers"));
            s_logger.debug("LB config paylod : " +  lbConfigCmd.toString());

            String result = postHttpRequest(lbConfigCmd.toString(), agentUri, _sessionid);
            s_logger.debug("Result of lbconfigcmg is "+ result);
            result = queryAsyncJob(result);
            s_logger.debug("Done query async of LB ConfigCmd implement request and result:: " + result);
            return new Answer(cmd);
        } catch (ExecutionException e) {
            s_logger.error("Failed to execute LoadBalancerConfigCommand due to ", e);
            if(e.getMessage().equalsIgnoreCase(NccHttpCode.NOT_FOUND)) {
                return new Answer(cmd, true, "LB Rule is not present in NS device. So returning as removed the LB Rule");
            } else  if(e.getMessage().startsWith("ERROR, ROLLBACK COMPLETED") || e.getMessage().startsWith("ERROR, ROLLBACK FAILED")) {
                s_logger.error("Failed to execute LoadBalancerConfigCommand due to : " + e.getMessage());
                return new Answer(cmd, false, e.getMessage());
            } else if (e.getMessage().startsWith(NccHttpCode.INTERNAL_ERROR)) {
                s_logger.error("Failed to execute LoadBalancerConfigCommand as Internal Error returning Internal error ::" + e.getMessage() );
                return new Answer(cmd, false, e.getMessage());
            }
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new Answer(cmd, false, e.getMessage());
            }
        } catch (Exception e) {
            s_logger.error("Failed to execute LoadBalancerConfigCommand due to ", e);
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new Answer(cmd, false, e.getMessage());
            }
        }
    }

    private synchronized Answer execute(SetStaticNatRulesCommand cmd, int numRetries) {
        return Answer.createUnsupportedCommandAnswer(cmd);
    }

    private synchronized Answer execute(ExternalNetworkResourceUsageCommand cmd, int numRetries) {
        try {

            return getPublicIpBytesSentAndReceived(cmd);

        } catch (ExecutionException e) {
            if (shouldRetry(numRetries)) {
                return retry(cmd, numRetries);
            } else {
                return new ExternalNetworkResourceUsageAnswer(cmd, e);
            }
        }
    }


    private String getNetScalerProtocol(LoadBalancerTO loadBalancer) throws ExecutionException {
        String port = Integer.toString(loadBalancer.getSrcPort());
        String lbProtocol = loadBalancer.getLbProtocol();
        StickinessPolicyTO[] stickyPolicies = loadBalancer.getStickinessPolicies();
        String nsProtocol = "TCP";

        if (lbProtocol == null)
            lbProtocol = loadBalancer.getProtocol();

        if ((stickyPolicies != null) && (stickyPolicies.length > 0) && (stickyPolicies[0] != null)) {
            StickinessPolicyTO stickinessPolicy = stickyPolicies[0];
            if (StickinessMethodType.LBCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName()) ||
                    (StickinessMethodType.AppCookieBased.getName().equalsIgnoreCase(stickinessPolicy.getMethodName()))) {
                nsProtocol = "HTTP";
                return nsProtocol;
            }
        }

        if (lbProtocol.equalsIgnoreCase(NetUtils.SSL_PROTO) || lbProtocol.equalsIgnoreCase(NetUtils.HTTP_PROTO))
            return lbProtocol.toUpperCase();

        if (port.equals(NetUtils.HTTP_PORT)) {
            nsProtocol = "HTTP";
        } else if (NetUtils.TCP_PROTO.equalsIgnoreCase(lbProtocol)) {
            nsProtocol = "TCP";
        } else if (NetUtils.UDP_PROTO.equalsIgnoreCase(lbProtocol)) {
            nsProtocol = "UDP";
        }

        return nsProtocol;
    }


    private ExternalNetworkResourceUsageAnswer getPublicIpBytesSentAndReceived(ExternalNetworkResourceUsageCommand cmd) throws ExecutionException {
        ExternalNetworkResourceUsageAnswer answer = new ExternalNetworkResourceUsageAnswer(cmd);
        long networkid = cmd.getNetworkid();
        try {
            //TODO send GET cmd to get the network stats

            URI agentUri = null;
            String response = null;
            try {
                agentUri =
                        new URI("https", null, _ip, DEFAULT_PORT,
                                "/cs/adcaas/v1/networks/"+ networkid +"/ipStats", null, null);
                org.json.JSONObject jsonBody = new JSONObject();
                response = getHttpRequest(jsonBody.toString(), agentUri, _sessionid);
                JSONArray statsIPList = null;
                if(response !=null ) {
                    statsIPList = new JSONObject(response).getJSONObject("stats") .getJSONArray("ipBytes");
                }
                if(statsIPList != null) {
                    for(int i=0; i<statsIPList.length(); i++) {
                        JSONObject ipstat = statsIPList.getJSONObject(i);
                        JSONObject ipvalues =  ipstat.getJSONObject("ipstats");
                        if(ipstat != null) {
                            long[] bytesSentAndReceived = new long[] {0, 0};
                            bytesSentAndReceived[0] = ipvalues.getLong("received");
                            bytesSentAndReceived[1] = ipvalues.getLong("sent");

                            if (bytesSentAndReceived[0] >= 0 && bytesSentAndReceived[1] >= 0) {
                                answer.ipBytes.put(ipstat.getString("ip"), bytesSentAndReceived);
                            }
                       }
                    }
                }
                s_logger.debug("IPStats Response :" + response);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                s_logger.debug("Seesion Alive" + e.getMessage());
                e.printStackTrace();
            }

        } catch (Exception e) {
            s_logger.error("Failed to get bytes sent and received statistics due to " + e);
            throw new ExecutionException(e.getMessage());
        }

        return answer;
    }

    private Answer retry(Command cmd, int numRetries) {
        int numRetriesRemaining = numRetries - 1;
        s_logger.warn("Retrying " + cmd.getClass().getSimpleName() + ". Number of retries remaining: " + numRetriesRemaining);
        return executeRequest(cmd, numRetriesRemaining);
    }

    private boolean shouldRetry(int numRetries) {
        try {
            if (numRetries > 0) {
                login();
                return true;
            }
        } catch (Exception e) {
            s_logger.error("Failed to log in to Netscaler ControlCenter device at " + _ip + " due to " + e.getMessage());
            return false;
        }
        return false;
    }


    @Override
    public IAgentControl getAgentControl() {
        return null;
    }

    private boolean refreshNCCConnection() {
        boolean ret = false;
        try {
            keepSessionAlive();
            return true;
        } catch (ExecutionException ex) {
            s_logger.debug("Failed to keep up the session alive ", ex);
        }
        return ret;
    }

    @Override
    public PingCommand getCurrentStatus(long id) {
        pingCount++;
        if (pingCount > 10 && refreshNCCConnection()) {
            pingCount = 0;
        }
        return new PingCommand(Host.Type.NetScalerControlCenter, id);
    }

    @Override
    public Type getType() {
        return Host.Type.NetScalerControlCenter ;
    }

    @Override
    public void setAgentControl(IAgentControl agentControl) {
        return;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public void setName(String name) {
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void disconnected() {
        return;
    }

    @Override
    public void setConfigParams(Map<String, Object> params) {
    }

    @Override
    public Map<String, Object> getConfigParams() {
        return null;
    }

    @Override
    public int getRunLevel() {
        return 0;
    }

    @Override
    public void setRunLevel(int level) {
    }

    public String getSessionID() {
        return _sessionid;
    }
    public static String cleanPassword(String logString) {
        String cleanLogString = null;
        if (logString != null) {
            cleanLogString = logString;
            String[] temp = logString.split(",");
            int i = 0;
            if (temp != null) {
                while (i < temp.length) {
                    temp[i] = com.cloud.utils.StringUtils.cleanString(temp[i]);
                    i++;
                }
                List<String> stringList = new ArrayList<String>();
                Collections.addAll(stringList, temp);
                cleanLogString = StringUtils.join(stringList, ",");
            }
        }
        return cleanLogString;
    }
    public static HttpClient getHttpClient() {

        HttpClient httpClient = null;
        TrustStrategy easyStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                return true;
            }
        };

        try {
            SSLSocketFactory sf = new SSLSocketFactory(easyStrategy, new AllowAllHostnameVerifier());
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("https", DEFAULT_PORT, sf));
            ClientConnectionManager ccm = new BasicClientConnectionManager(registry);
            httpClient = new DefaultHttpClient(ccm);
        } catch (KeyManagementException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        } catch (UnrecoverableKeyException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        } catch (KeyStoreException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        }
        return httpClient;
    }

    public static String getHttpRequest(final String jsonCmd, final URI agentUri, String sessionID) throws ExecutionException {
        // Using Apache's HttpClient for HTTP POST
        // Java-only approach discussed at on StackOverflow concludes with
        // comment to use Apache HttpClient
        // http://stackoverflow.com/a/2793153/939250, but final comment is to
        // use Apache.
        String logMessage = StringEscapeUtils.unescapeJava(jsonCmd);
        logMessage = cleanPassword(logMessage);
        s_logger.debug("GET request to " + agentUri.toString()
                + " with contents " + logMessage);

        // Create request
        HttpClient httpClient = getHttpClient();
        String result = null;

        // TODO: are there timeout settings and worker thread settings to tweak?
        try {
            HttpGet request = new HttpGet(agentUri);

            // JSON encode command
            // Assumes command sits comfortably in a string, i.e. not used for
            // large data transfers
            StringEntity cmdJson = new StringEntity(jsonCmd);
            request.addHeader("content-type", "application/json");
            request.addHeader("Cookie", "SessId=" + sessionID);
            s_logger.debug("Sending cmd to " + agentUri.toString()
                    + " cmd data:" + logMessage);
            HttpResponse response = httpClient.execute(request);

            // Unsupported commands will not route.
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                String errMsg = "Failed to send : HTTP error code : " + response.getStatusLine().getStatusCode();
                s_logger.error(errMsg);
                String unsupportMsg = "Unsupported command " + agentUri.getPath() + ".  Are you sure you got the right f of" + " server?";
                Answer ans = new UnsupportedAnswer(null, unsupportMsg);
                s_logger.error(ans);
                result = s_gson.toJson(new Answer[] {ans});
            } else if (response.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
                String errMsg = "Failed send to " + agentUri.toString() + " : HTTP error code : " + response.getStatusLine().getStatusCode();
                s_logger.error(errMsg);
                throw new ExecutionException("UNAUTHORIZED");
            } else {
                result = EntityUtils.toString(response.getEntity());
                String logResult = cleanPassword(StringEscapeUtils.unescapeJava(result));
                s_logger.debug("Get response is " + logResult);
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

    public static String postHttpRequest(final String jsonCmd, final URI agentUri, String sessionID) throws ExecutionException {
        // Using Apache's HttpClient for HTTP POST
        // Java-only approach discussed at on StackOverflow concludes with
        // comment to use Apache HttpClient
        // http://stackoverflow.com/a/2793153/939250, but final comment is to
        // use Apache.
        String logMessage = StringEscapeUtils.unescapeJava(jsonCmd);
        logMessage = cleanPassword(logMessage);
        s_logger.debug("POST request to " + agentUri.toString()
                + " with contents " + logMessage);

        // Create request
        HttpClient httpClient = getHttpClient();
        TrustStrategy easyStrategy = new TrustStrategy() {
            @Override
            public boolean isTrusted(X509Certificate[] chain, String authType)
                    throws CertificateException {
                return true;
            }
        };

        try {
            SSLSocketFactory sf = new SSLSocketFactory(easyStrategy, new AllowAllHostnameVerifier());
            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("https", DEFAULT_PORT, sf));
            ClientConnectionManager ccm = new BasicClientConnectionManager(registry);
            httpClient = new DefaultHttpClient(ccm);
        } catch (KeyManagementException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        } catch (UnrecoverableKeyException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        } catch (KeyStoreException e) {
            s_logger.error("failed to initialize http client " + e.getMessage());
        }

        String result = null;

        // TODO: are there timeout settings and worker thread settings to tweak?
        try {
            HttpPost request = new HttpPost(agentUri);

            // JSON encode command
            // Assumes command sits comfortably in a string, i.e. not used for
            // large data transfers
            StringEntity cmdJson = new StringEntity(jsonCmd);
            request.addHeader("content-type", "application/json");
            request.addHeader("Cookie", "SessId=" + sessionID);
            request.setEntity(cmdJson);
            s_logger.debug("Sending cmd to " + agentUri.toString()
                    + " cmd data:" + logMessage + "SEssion id: " + sessionID);
            HttpResponse response = httpClient.execute(request);

            // Unsupported commands will not route.
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                String errMsg = "Failed : HTTP error code : " + response.getStatusLine().getStatusCode();
                throw new ExecutionException(NccHttpCode.NOT_FOUND);
            } else if ((response.getStatusLine().getStatusCode() != HttpStatus.SC_OK ) && (response.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED )) {
                String errMsg = "Command Not Success " + agentUri.toString() + " : HTTP error code : " + response.getStatusLine().getStatusCode();
                s_logger.error(errMsg);
                throw new ExecutionException(NccHttpCode.INTERNAL_ERROR + " " + errMsg);
            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                //Successfully created the resource in the NCC, Now get the Job ID and send to the response
                // make login request and store new session id
                throw new ExecutionException(NccHttpCode.UNAUTHORIZED);
            } else if (response.getStatusLine().getStatusCode() == HttpStatus.SC_CREATED) {
                //Successfully created the resource in the NCC, Now get the Job ID and send to the response
                result = response.getFirstHeader(NccHttpCode.JOB_ID).getValue();
            } else {
                result = EntityUtils.toString(response.getEntity());
                String logResult = cleanPassword(StringEscapeUtils.unescapeJava(result));
                s_logger.debug("POST response is " + logResult);
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
}
