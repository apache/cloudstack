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
/** Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
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
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.httpclient.ConnectTimeoutException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class NiciraNvpApi {
    private static final Logger s_logger = Logger.getLogger(NiciraNvpApi.class);
    
    private String _name;
    private String _host;
    private String _adminuser;
    private String _adminpass;
    
    private HttpClient _client;

    public NiciraNvpApi(String host, String adminuser, String adminpass) throws NiciraNvpApiException {
        this._host = host;
        this._adminpass = adminpass;
        this._adminuser = adminuser;
        
        if (_host == null || _adminpass == null || _adminuser == null) {
            throw new NiciraNvpApiException("host, adminuser and adminpass may not be null");
        }

        _client = new HttpClient( );
        _client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
        
        try {             
            // Cast to ProtocolSocketFactory to avoid the deprecated constructor with the SecureProtocolSocketFactory parameter
            Protocol.registerProtocol("https", new Protocol("https", (ProtocolSocketFactory) new TrustingProtocolSocketFactory(), 443));
        } catch (IOException e) {
            s_logger.warn("Failed to register the TrustingProtocolSocketFactory, falling back to default SSLSocketFactory", e);
        }
        
    }
    
    /**
     * Logs into the Nicira API. The cookie is stored in the <code>_authcookie<code> variable.
     * <p>
     * The method returns false if the login failed or the connection could not be made.
     * 
     */
    private void login() throws NiciraNvpApiException {
        String url;
        
        try {
            url = new URL("https", _host, "/ws.v1/login").toString();
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
        }
        
        if (pm.getStatusCode() != HttpStatus.SC_OK) {
            s_logger.error("Nicira NVP API login failed : " + pm.getStatusText());
            throw new NiciraNvpApiException("Nicira NVP API login failed " + pm.getStatusText());
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
                
        if (lspl == null || lspl.getResult_count() != 1) {
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

    private <T> void executeUpdateObject(T newObject, String uri, Map<String,String> parameters) throws NiciraNvpApiException {
        String url;
        try {
            url = new URL("https", _host, uri).toString();
        } catch (MalformedURLException e) {
            s_logger.error("Unable to build Nicira API URL", e);
            throw new NiciraNvpApiException("Connection to NVP Failed");
        }
        
        Gson gson = new Gson();
        
        PutMethod pm = new PutMethod(url);
        pm.setRequestHeader("Content-Type", "application/json");
        try {
            pm.setRequestEntity(new StringRequestEntity(
                    gson.toJson(newObject),"application/json", null));
        } catch (UnsupportedEncodingException e) {
            throw new NiciraNvpApiException("Failed to encode json request body", e);
        }
                
        executeMethod(pm);
        
        if (pm.getStatusCode() != HttpStatus.SC_OK) {
            String errorMessage = responseToErrorMessage(pm);
            s_logger.error("Failed to update object : " + errorMessage);
            throw new NiciraNvpApiException("Failed to update object : " + errorMessage);
        }
        
    }
    
    private <T> T executeCreateObject(T newObject, Type returnObjectType, String uri, Map<String,String> parameters) throws NiciraNvpApiException {
        String url;
        try {
            url = new URL("https", _host, uri).toString();
        } catch (MalformedURLException e) {
            s_logger.error("Unable to build Nicira API URL", e);
            throw new NiciraNvpApiException("Unable to build Nicira API URL", e);
        }
        
        Gson gson = new Gson();
        
        PostMethod pm = new PostMethod(url);
        pm.setRequestHeader("Content-Type", "application/json");
        try {
            pm.setRequestEntity(new StringRequestEntity(
                    gson.toJson(newObject),"application/json", null));
        } catch (UnsupportedEncodingException e) {
            throw new NiciraNvpApiException("Failed to encode json request body", e);
        }
                
        executeMethod(pm);
        
        if (pm.getStatusCode() != HttpStatus.SC_CREATED) {
            String errorMessage = responseToErrorMessage(pm);
            s_logger.error("Failed to create object : " + errorMessage);
            throw new NiciraNvpApiException("Failed to create object : " + errorMessage);
        }
        
        T result;
        try {
            result = gson.fromJson(pm.getResponseBodyAsString(), TypeToken.get(newObject.getClass()).getType());
        } catch (IOException e) {
            throw new NiciraNvpApiException("Failed to decode json response body", e);
        }
        
        return result;        
    }
    
    private void executeDeleteObject(String uri) throws NiciraNvpApiException {
        String url;
        try {
            url = new URL("https", _host, uri).toString();
        } catch (MalformedURLException e) {
            s_logger.error("Unable to build Nicira API URL", e);
            throw new NiciraNvpApiException("Unable to build Nicira API URL", e);
        }
            
        DeleteMethod dm = new DeleteMethod(url);
        dm.setRequestHeader("Content-Type", "application/json");
                
        executeMethod(dm);
        
        if (dm.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
            String errorMessage = responseToErrorMessage(dm);
            s_logger.error("Failed to delete object : " + errorMessage);
            throw new NiciraNvpApiException("Failed to delete object : " + errorMessage);
        }
    }
    
    private <T> T executeRetrieveObject(Type returnObjectType, String uri, Map<String,String> parameters) throws NiciraNvpApiException {
        String url;
        try {
            url = new URL("https", _host, uri).toString();
        } catch (MalformedURLException e) {
            s_logger.error("Unable to build Nicira API URL", e);
            throw new NiciraNvpApiException("Unable to build Nicira API URL", e);
        }
            
        GetMethod gm = new GetMethod(url);
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
            s_logger.error("Failed to retrieve object : " + errorMessage);
            throw new NiciraNvpApiException("Failed to retrieve object : " + errorMessage);
        }
            
        Gson gson = new Gson();
        T returnValue;
        try {
            returnValue = gson.fromJson(gm.getResponseBodyAsString(), returnObjectType);
        } catch (IOException e) {
            s_logger.error("IOException while retrieving response body",e);
            throw new NiciraNvpApiException(e);
        }
        
        return returnValue;
    }
    
    private void executeMethod(HttpMethodBase method) throws NiciraNvpApiException {
        try {
            _client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                // login and try again
                login();
                _client.executeMethod(method);
            }
        } catch (HttpException e) {
            s_logger.error("HttpException caught while trying to connect to the Nicira NVP Controller", e);
            throw new NiciraNvpApiException("API call to Nicira NVP Controller Failed", e);
        } catch (IOException e) {
            s_logger.error("IOException caught while trying to connect to the Nicira NVP Controller", e);
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
    
    
}
