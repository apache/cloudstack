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
package com.cloud.network.nicira;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.httpclient.protocol.SecureProtocolSocketFactory;
import org.apache.log4j.Logger;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;

public class NiciraNvpApi {
    private static final Logger s_logger = Logger.getLogger(NiciraNvpApi.class);
    private final static String _protocol = "https";
    private static final MultiThreadedHttpConnectionManager s_httpClientManager = new MultiThreadedHttpConnectionManager();
    
    private String _name;
    private String _host;
    private String _adminuser;
    private String _adminpass;
    
    private HttpClient _client;
    private String _nvpversion;
    
    private Gson _gson;
    
    /* This factory method is protected so we can extend this
     * in the unittests.
     */
    protected HttpClient createHttpClient() {
    	return new HttpClient(s_httpClientManager);
    }
    
    protected HttpMethod createMethod(String type, String uri) throws NiciraNvpApiException {
    	String url;
        try {
            url = new URL(_protocol, _host, uri).toString();
        } catch (MalformedURLException e) {
            s_logger.error("Unable to build Nicira API URL", e);
            throw new NiciraNvpApiException("Unable to build Nicira API URL", e);
        }
        
        if ("post".equalsIgnoreCase(type)) {
        	return new PostMethod(url);    	
        }
        else if ("get".equalsIgnoreCase(type)) {
        	return new GetMethod(url);
        }
        else if ("delete".equalsIgnoreCase(type)) {
        	return new DeleteMethod(url);
        }
        else if ("put".equalsIgnoreCase(type)) {
        	return new PutMethod(url);
        }
        else {
        	throw new NiciraNvpApiException("Requesting unknown method type");
        }
    }

    public NiciraNvpApi() {
        _client = createHttpClient();
        _client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        
        try {             
            // Cast to ProtocolSocketFactory to avoid the deprecated constructor with the SecureProtocolSocketFactory parameter
            Protocol.registerProtocol("https", new Protocol("https", (ProtocolSocketFactory) new TrustingProtocolSocketFactory(), 443));
        } catch (IOException e) {
            s_logger.warn("Failed to register the TrustingProtocolSocketFactory, falling back to default SSLSocketFactory", e);
        }
        
        _gson = new GsonBuilder()
                .registerTypeAdapter(NatRule.class, new NatRuleAdapter())
                .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
                .create();
        
    }
    
    public void setControllerAddress(String address) {
    	this._host = address;
    }
    
    public void setAdminCredentials(String username, String password) {
    	this._adminuser = username;
    	this._adminpass = password;
    }
    
    /**
     * Logs into the Nicira API. The cookie is stored in the <code>_authcookie<code> variable.
     * <p>
     * The method returns false if the login failed or the connection could not be made.
     * 
     */
    protected void login() throws NiciraNvpApiException {
        String url;
        
        if (_host == null || _host.isEmpty() ||
        		_adminuser == null || _adminuser.isEmpty() ||
        		_adminpass == null || _adminpass.isEmpty()) {
        	throw new NiciraNvpApiException("Hostname/credentials are null or empty");
        }
        
        try {
            url = new URL(_protocol, _host, "/ws.v1/login").toString();
        } catch (MalformedURLException e) {
            s_logger.error("Unable to build Nicira API URL", e);
            throw new NiciraNvpApiException("Unable to build Nicira API URL", e);
        }
        
        PostMethod pm = new PostMethod(url);
        pm.addParameter("username", _adminuser);
        pm.addParameter("password", _adminpass);
        
        try {
            _client.executeMethod(pm);
        } catch (HttpException e) {
            throw new NiciraNvpApiException("Nicira NVP API login failed ", e);
        } catch (IOException e) {
            throw new NiciraNvpApiException("Nicira NVP API login failed ", e);
        } finally {
            pm.releaseConnection();
        }
        
        if (pm.getStatusCode() != HttpStatus.SC_OK) {
            s_logger.error("Nicira NVP API login failed : " + pm.getStatusText());
            throw new NiciraNvpApiException("Nicira NVP API login failed " + pm.getStatusText());
        }
        
        // Extract the version for later use
        if (pm.getResponseHeader("Server") != null) {
            _nvpversion = pm.getResponseHeader("Server").getValue();
            s_logger.debug("NVP Controller reports version " + _nvpversion);
        }
        
        // Success; the cookie required for login is kept in _client
    }
    
    public LogicalSwitch createLogicalSwitch(LogicalSwitch logicalSwitch) throws NiciraNvpApiException {
        String uri = "/ws.v1/lswitch";
        LogicalSwitch createdLogicalSwitch = executeCreateObject(logicalSwitch, new TypeToken<LogicalSwitch>(){}.getType(), uri, Collections.<String,String>emptyMap());
        
        return createdLogicalSwitch;
    }

    public void deleteLogicalSwitch(String uuid) throws NiciraNvpApiException {
        String uri = "/ws.v1/lswitch/" + uuid;
        executeDeleteObject(uri);
    }
    
    public LogicalSwitchPort createLogicalSwitchPort(String logicalSwitchUuid, LogicalSwitchPort logicalSwitchPort) throws NiciraNvpApiException {
        String uri = "/ws.v1/lswitch/" + logicalSwitchUuid + "/lport";
        LogicalSwitchPort createdLogicalSwitchPort = executeCreateObject(logicalSwitchPort, new TypeToken<LogicalSwitchPort>(){}.getType(), uri, Collections.<String,String>emptyMap());;
        
        return createdLogicalSwitchPort;
    }

    public void modifyLogicalSwitchPortAttachment(String logicalSwitchUuid, String logicalSwitchPortUuid, Attachment attachment) throws NiciraNvpApiException {
        String uri = "/ws.v1/lswitch/" + logicalSwitchUuid + "/lport/" + logicalSwitchPortUuid + "/attachment";
        executeUpdateObject(attachment, uri, Collections.<String,String>emptyMap());
    }
    
    public void deleteLogicalSwitchPort(String logicalSwitchUuid, String logicalSwitchPortUuid) throws NiciraNvpApiException {
        String uri = "/ws.v1/lswitch/" + logicalSwitchUuid + "/lport/" + logicalSwitchPortUuid;
        executeDeleteObject(uri);
    }
    
    public String findLogicalSwitchPortUuidByVifAttachmentUuid(String logicalSwitchUuid, String vifAttachmentUuid) throws NiciraNvpApiException {
        String uri = "/ws.v1/lswitch/" + logicalSwitchUuid + "/lport";
        Map<String,String> params = new HashMap<String,String>();
        params.put("attachment_vif_uuid", vifAttachmentUuid);
        params.put("fields", "uuid");
            
        NiciraNvpList<LogicalSwitchPort> lspl = executeRetrieveObject(new TypeToken<NiciraNvpList<LogicalSwitchPort>>(){}.getType(), uri, params);
                
        if (lspl == null || lspl.getResultCount() != 1) {
            throw new NiciraNvpApiException("Unexpected response from API");
        }
        
        LogicalSwitchPort lsp = lspl.getResults().get(0);
        return lsp.getUuid();
    }
    
    public ControlClusterStatus getControlClusterStatus() throws NiciraNvpApiException {
        String uri = "/ws.v1/control-cluster/status";
        ControlClusterStatus ccs = executeRetrieveObject(new TypeToken<ControlClusterStatus>(){}.getType(), uri, null);

        return ccs;
    }

    public NiciraNvpList<LogicalSwitchPort> findLogicalSwitchPortsByUuid(String logicalSwitchUuid, String logicalSwitchPortUuid) throws NiciraNvpApiException {
        String uri = "/ws.v1/lswitch/" + logicalSwitchUuid + "/lport";
        Map<String,String> params = new HashMap<String,String>();
        params.put("uuid", logicalSwitchPortUuid);
        params.put("fields", "uuid");
            
        NiciraNvpList<LogicalSwitchPort> lspl = executeRetrieveObject(new TypeToken<NiciraNvpList<LogicalSwitchPort>>(){}.getType(), uri, params);
                
        if (lspl == null ) {
            throw new NiciraNvpApiException("Unexpected response from API");
        }
        
        return lspl;
    }
    
    public LogicalRouterConfig createLogicalRouter(LogicalRouterConfig logicalRouterConfig) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter";
    	
    	LogicalRouterConfig lrc = executeCreateObject(logicalRouterConfig, new TypeToken<LogicalRouterConfig>(){}.getType(), uri, Collections.<String,String>emptyMap());
    	
    	return lrc;
    }

    public void deleteLogicalRouter(String logicalRouterUuid) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid;
    	
    	executeDeleteObject(uri);
    }
    
    public LogicalRouterPort createLogicalRouterPort(String logicalRouterUuid, LogicalRouterPort logicalRouterPort) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/lport";
    	
    	LogicalRouterPort lrp = executeCreateObject(logicalRouterPort, new TypeToken<LogicalRouterPort>(){}.getType(), uri, Collections.<String,String>emptyMap());
    	return lrp;    	
    }
    
    public void deleteLogicalRouterPort(String logicalRouterUuid, String logicalRouterPortUuid) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/lport/" +  logicalRouterPortUuid;
    	
    	executeDeleteObject(uri);
    }

    public void modifyLogicalRouterPort(String logicalRouterUuid, LogicalRouterPort logicalRouterPort) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/lport/" +  logicalRouterPort.getUuid();
    	
    	executeUpdateObject(logicalRouterPort, uri, Collections.<String,String>emptyMap());
    }
    
    public void modifyLogicalRouterPortAttachment(String logicalRouterUuid, String logicalRouterPortUuid, Attachment attachment) throws NiciraNvpApiException {
        String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/lport/" + logicalRouterPortUuid + "/attachment";
        executeUpdateObject(attachment, uri, Collections.<String,String>emptyMap());
    }
    
    public NatRule createLogicalRouterNatRule(String logicalRouterUuid, NatRule natRule) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/nat";
    	
    	return executeCreateObject(natRule, new TypeToken<NatRule>(){}.getType(), uri, Collections.<String,String>emptyMap());
    }
    
    public void modifyLogicalRouterNatRule(String logicalRouterUuid, NatRule natRule) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/nat/" + natRule.getUuid();
    	
    	executeUpdateObject(natRule, uri, Collections.<String,String>emptyMap());
    }
    
    public void deleteLogicalRouterNatRule(String logicalRouterUuid, UUID natRuleUuid) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/nat/" + natRuleUuid.toString();
    	
    	executeDeleteObject(uri);
    }
    
    public NiciraNvpList<LogicalRouterPort> findLogicalRouterPortByGatewayServiceAndVlanId(String logicalRouterUuid, String gatewayServiceUuid, long vlanId) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/lport";
        Map<String,String> params = new HashMap<String,String>();
        params.put("attachment_gwsvc_uuid", gatewayServiceUuid);
        params.put("attachment_vlan", "0");
        params.put("fields","*");
        
        return executeRetrieveObject(new TypeToken<NiciraNvpList<LogicalRouterPort>>(){}.getType(), uri, params);
    }
    
    public LogicalRouterConfig findOneLogicalRouterByUuid(String logicalRouterUuid) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid;
    	
    	return executeRetrieveObject(new TypeToken<LogicalRouterConfig>(){}.getType(), uri, Collections.<String,String>emptyMap());
    }
    
    public void updateLogicalRouterPortConfig(String logicalRouterUuid, LogicalRouterPort logicalRouterPort) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/lport" + logicalRouterPort.getUuid();
    	
    	executeUpdateObject(logicalRouterPort, uri, Collections.<String,String>emptyMap());
    }
    
    public NiciraNvpList<NatRule> findNatRulesByLogicalRouterUuid(String logicalRouterUuid) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/nat";
        Map<String,String> params = new HashMap<String,String>();
        params.put("fields","*");
        
    	return executeRetrieveObject(new TypeToken<NiciraNvpList<NatRule>>(){}.getType(), uri, params);
    }
    
    public NiciraNvpList<LogicalRouterPort> findLogicalRouterPortByGatewayServiceUuid(String logicalRouterUuid, String l3GatewayServiceUuid) throws NiciraNvpApiException {
    	String uri = "/ws.v1/lrouter/" + logicalRouterUuid + "/lport";
    	Map<String,String> params = new HashMap<String,String>();
    	params.put("fields", "*");
    	params.put("attachment_gwsvc_uuid", l3GatewayServiceUuid);
    	
    	return executeRetrieveObject(new TypeToken<NiciraNvpList<LogicalRouterPort>>(){}.getType(), uri, params);
    }
    
    protected <T> void executeUpdateObject(T newObject, String uri, Map<String,String> parameters) throws NiciraNvpApiException {
        if (_host == null || _host.isEmpty() ||
        		_adminuser == null || _adminuser.isEmpty() ||
        		_adminpass == null || _adminpass.isEmpty()) {
        	throw new NiciraNvpApiException("Hostname/credentials are null or empty");
        }
        
        PutMethod pm = (PutMethod) createMethod("put", uri);
        pm.setRequestHeader("Content-Type", "application/json");
        try {
            pm.setRequestEntity(new StringRequestEntity(
                    _gson.toJson(newObject),"application/json", null));
        } catch (UnsupportedEncodingException e) {
            throw new NiciraNvpApiException("Failed to encode json request body", e);
        }
                
        executeMethod(pm);
        
        if (pm.getStatusCode() != HttpStatus.SC_OK) {
            String errorMessage = responseToErrorMessage(pm);
            pm.releaseConnection();
            s_logger.error("Failed to update object : " + errorMessage);
            throw new NiciraNvpApiException("Failed to update object : " + errorMessage);
        }
        pm.releaseConnection();
    }
    
    protected <T> T executeCreateObject(T newObject, Type returnObjectType, String uri, Map<String,String> parameters) throws NiciraNvpApiException {
        if (_host == null || _host.isEmpty() ||
        		_adminuser == null || _adminuser.isEmpty() ||
        		_adminpass == null || _adminpass.isEmpty()) {
        	throw new NiciraNvpApiException("Hostname/credentials are null or empty");
        }
        
        PostMethod pm = (PostMethod) createMethod("post", uri);
        pm.setRequestHeader("Content-Type", "application/json");
        try {
            pm.setRequestEntity(new StringRequestEntity(
                    _gson.toJson(newObject),"application/json", null));
        } catch (UnsupportedEncodingException e) {
            throw new NiciraNvpApiException("Failed to encode json request body", e);
        }
                
        executeMethod(pm);
        
        if (pm.getStatusCode() != HttpStatus.SC_CREATED) {
            String errorMessage = responseToErrorMessage(pm);
            pm.releaseConnection();
            s_logger.error("Failed to create object : " + errorMessage);
            throw new NiciraNvpApiException("Failed to create object : " + errorMessage);
        }
        
        T result;
        try {
            result = (T)_gson.fromJson(pm.getResponseBodyAsString(), TypeToken.get(newObject.getClass()).getType());
        } catch (IOException e) {
            throw new NiciraNvpApiException("Failed to decode json response body", e);
        } finally {
            pm.releaseConnection();
        }
        
        return result;        
    }
    
    protected void executeDeleteObject(String uri) throws NiciraNvpApiException {
        if (_host == null || _host.isEmpty() ||
        		_adminuser == null || _adminuser.isEmpty() ||
        		_adminpass == null || _adminpass.isEmpty()) {
        	throw new NiciraNvpApiException("Hostname/credentials are null or empty");
        }
           
        DeleteMethod dm = (DeleteMethod) createMethod("delete", uri);
        dm.setRequestHeader("Content-Type", "application/json");
                
        executeMethod(dm);
        
        if (dm.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            String errorMessage = responseToErrorMessage(dm);
            dm.releaseConnection();
            s_logger.error("Failed to delete object : " + errorMessage);
            throw new NiciraNvpApiException("Failed to delete object : " + errorMessage);
        }
        dm.releaseConnection();
    }
    
    protected <T> T executeRetrieveObject(Type returnObjectType, String uri, Map<String,String> parameters) throws NiciraNvpApiException {
        if (_host == null || _host.isEmpty() ||
        		_adminuser == null || _adminuser.isEmpty() ||
        		_adminpass == null || _adminpass.isEmpty()) {
        	throw new NiciraNvpApiException("Hostname/credentials are null or empty");
        }
            
        GetMethod gm = (GetMethod) createMethod("get", uri);
        gm.setRequestHeader("Content-Type", "application/json");
        if (parameters != null && !parameters.isEmpty()) {
	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(parameters.size());
	        for (Entry<String,String> e : parameters.entrySet()) {
	            nameValuePairs.add(new NameValuePair(e.getKey(), e.getValue()));
	        }
	        gm.setQueryString(nameValuePairs.toArray(new NameValuePair[0]));
        }
                
        executeMethod(gm);
        
        if (gm.getStatusCode() != HttpStatus.SC_OK) {
            String errorMessage = responseToErrorMessage(gm);
            gm.releaseConnection();
            s_logger.error("Failed to retrieve object : " + errorMessage);
            throw new NiciraNvpApiException("Failed to retrieve object : " + errorMessage);
        }
            
        T returnValue;
        try {
            returnValue = (T)_gson.fromJson(gm.getResponseBodyAsString(), returnObjectType);
        } catch (IOException e) {
            s_logger.error("IOException while retrieving response body",e);
            throw new NiciraNvpApiException(e);
        } finally {
            gm.releaseConnection();
        }
        return returnValue;
    }
    
    protected void executeMethod(HttpMethodBase method) throws NiciraNvpApiException {
        try {
            _client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                method.releaseConnection();
                // login and try again
                login();
                _client.executeMethod(method);
            }
        } catch (HttpException e) {
            s_logger.error("HttpException caught while trying to connect to the Nicira NVP Controller", e);
            method.releaseConnection();
            throw new NiciraNvpApiException("API call to Nicira NVP Controller Failed", e);
        } catch (IOException e) {
            s_logger.error("IOException caught while trying to connect to the Nicira NVP Controller", e);
            method.releaseConnection();
            throw new NiciraNvpApiException("API call to Nicira NVP Controller Failed", e);            
        }
    }
    
    private String responseToErrorMessage(HttpMethodBase method) {
        assert method.isRequestSent() : "no use getting an error message unless the request is sent";
        
        if ("text/html".equals(method.getResponseHeader("Content-Type").getValue())) {
            // The error message is the response content
            // Safety margin of 1024 characters, anything longer is probably useless
            // and will clutter the logs
            try {
                return  method.getResponseBodyAsString(1024);
            } catch (IOException e) {
                s_logger.debug("Error while loading response body", e);
            }
        }
        
        // The default
        return method.getStatusText();
    }
    
    /* The Nicira controller uses a self-signed certificate. The 
     * TrustingProtocolSocketFactory will accept any provided
     * certificate when making an SSL connection to the SDN 
     * Manager
     */
    private class TrustingProtocolSocketFactory implements SecureProtocolSocketFactory {

        private SSLSocketFactory ssf;
        
        public TrustingProtocolSocketFactory() throws IOException {
            // Create a trust manager that does not validate certificate chains
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
         
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // Trust always
                    }
         
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // Trust always
                    }
                }
            };
         
            try {
                // Install the all-trusting trust manager
                SSLContext sc = SSLContext.getInstance("SSL");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());
                ssf =  sc.getSocketFactory();
            } catch (KeyManagementException e) {
                throw new IOException(e);
            } catch (NoSuchAlgorithmException e) {
                throw new IOException(e);
            }
        }
        
        @Override
        public Socket createSocket(String host, int port) throws IOException,
                UnknownHostException {
            return ssf.createSocket(host, port);
        }

        @Override
        public Socket createSocket(String address, int port, InetAddress localAddress,
                int localPort) throws IOException, UnknownHostException {
            return ssf.createSocket(address, port, localAddress, localPort);
        }

        @Override
        public Socket createSocket(Socket socket, String host, int port,
                boolean autoClose) throws IOException, UnknownHostException {
            return ssf.createSocket(socket, host, port, autoClose);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localAddress,
                int localPort, HttpConnectionParams params) throws IOException,
                UnknownHostException, ConnectTimeoutException {
            int timeout = params.getConnectionTimeout();
            if (timeout == 0) {
                return createSocket(host, port, localAddress, localPort);
            }
            else {
                Socket s = ssf.createSocket();
                s.bind(new InetSocketAddress(localAddress, localPort));
                s.connect(new InetSocketAddress(host, port), timeout);
                return s;
            }
        }

        
    }
    
    public static class NatRuleAdapter implements JsonDeserializer<NatRule> {

        @Override
        public NatRule deserialize(JsonElement jsonElement, Type type,
                JsonDeserializationContext context) throws JsonParseException {
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            NatRule natRule = null;
            
            if (!jsonObject.has("type")) {
                throw new JsonParseException("Deserializing as a NatRule, but no type present in the json object");
            }
            
            String natRuleType = jsonObject.get("type").getAsString();
            if ("SourceNatRule".equals(natRuleType)) {
                return context.deserialize(jsonElement, SourceNatRule.class);
            }
            else if ("DestinationNatRule".equals(natRuleType)) {
                return context.deserialize(jsonElement, DestinationNatRule.class);
            }
            
            throw new JsonParseException("Failed to deserialize type \"" + natRuleType + "\"");
        }

    }
}
