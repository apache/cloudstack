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
package com.cloud.network.bigswitch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class BigSwitchVnsApi {
    private static final Logger s_logger = Logger.getLogger(BigSwitchVnsApi.class);
    private final static String _protocol = "http";
    private final static String _nsBaseUri = "/networkService/v1.1";
    private final static String CONTENT_TYPE = "Content-Type";
    private final static String ACCEPT = "Accept";
    private final static String CONTENT_JSON = "application/json";
    private final static String HTTP_HEADER_INSTANCE_ID = "INSTANCE_ID";
    private final static String CLOUDSTACK_INSTANCE_ID = "org.apache.cloudstack";
    private final static MultiThreadedHttpConnectionManager s_httpClientManager =
                         new MultiThreadedHttpConnectionManager();

    private String _host;

    private HttpClient _client;

    /* This factory method is protected so we can extend this
     * in the unittests.
     */
    protected HttpClient createHttpClient() {
        return new HttpClient(s_httpClientManager);
    }

    protected HttpMethod createMethod(String type, String uri, int port) throws BigSwitchVnsApiException {
        String url;
        try {
            url = new URL(_protocol, _host, port, uri).toString();
        } catch (MalformedURLException e) {
            s_logger.error("Unable to build BigSwitch API URL", e);
            throw new BigSwitchVnsApiException("Unable to build v API URL", e);
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
            throw new BigSwitchVnsApiException("Requesting unknown method type");
        }
    }

    public BigSwitchVnsApi() {
        _client = createHttpClient();
        _client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    }

    public void setControllerAddress(String address) {
        this._host = address;
    }


    /**
     * Logs into the BigSwitch API. The cookie is stored in the <code>_authcookie<code> variable.
     * <p>
     * The method returns false if the login failed or the connection could not be made.
     *
     */
    protected void login() throws BigSwitchVnsApiException {
        return;
    }

    public void createNetwork(NetworkData network)
                throws BigSwitchVnsApiException {
        String uri = _nsBaseUri + "/tenants/" + network.getNetwork().getTenant_id() + "/networks";
        executeCreateObject(network, new TypeToken<NetworkData>(){}.getType(),
                uri, Collections.<String,String>emptyMap());
    }

    public void deleteNetwork(String tenantId, String networkId) throws BigSwitchVnsApiException {
        String uri = _nsBaseUri + "/tenants/" + tenantId + "/networks/" + networkId;
        executeDeleteObject(uri);
    }

    public void createPort(String networkUuid, PortData port)
                throws BigSwitchVnsApiException {
	String uri = _nsBaseUri + "/tenants/" + port.getPort().getTenant_id() + "/networks/" + networkUuid + "/ports";
        executeCreateObject(port, new TypeToken<PortData>(){}.getType(),
                uri, Collections.<String,String>emptyMap());
    }

    public void modifyPort(String networkId, PortData port)
                throws BigSwitchVnsApiException {
        String uri = _nsBaseUri + "/tenants/" + port.getPort().getTenant_id() + "/networks/" + networkId + "/ports";
        executeUpdateObject(port, uri, Collections.<String,String>emptyMap());
    }

    public void deletePort(String tenantId, String networkId, String portId)
                throws BigSwitchVnsApiException {
        String uri = _nsBaseUri + "/tenants/" + tenantId + "/networks/" + networkId + "/ports/" + portId;
        executeDeleteObject(uri);
    }

    public void modifyPortAttachment(String tenantId,
            String networkId,
            String portId,
            AttachmentData attachment) throws BigSwitchVnsApiException {
        String uri = _nsBaseUri + "/tenants/" + tenantId + "/networks/" + networkId + "/ports/" + portId + "/attachment";
        executeUpdateObject(attachment, uri, Collections.<String,String>emptyMap());
    }

    public void deletePortAttachment(String tenantId, String networkId, String portId)
                throws BigSwitchVnsApiException {
        String uri = _nsBaseUri + "/tenants/" + tenantId + "/networks/" + networkId + "/ports/" + portId + "/attachment";
        executeDeleteObject(uri);
    }

    public ControlClusterStatus getControlClusterStatus() throws BigSwitchVnsApiException {
        String uri = _nsBaseUri + "/health";
        ControlClusterStatus ccs = executeRetrieveObject(new TypeToken<ControlClusterStatus>(){}.getType(),
                                                         uri, 80, null);
        ccs.setStatus(true);

        return ccs;
    }

    protected <T> void executeUpdateObject(T newObject, String uri, Map<String,String> parameters)
                       throws BigSwitchVnsApiException {
        if (_host == null || _host.isEmpty()) {
            throw new BigSwitchVnsApiException("Hostname is null or empty");
        }

        Gson gson = new Gson();

        PutMethod pm = (PutMethod) createMethod("put", uri, 80);
        pm.setRequestHeader(CONTENT_TYPE, CONTENT_JSON);
        pm.setRequestHeader(ACCEPT, CONTENT_JSON);
        pm.setRequestHeader(HTTP_HEADER_INSTANCE_ID, CLOUDSTACK_INSTANCE_ID);
        try {
            pm.setRequestEntity(new StringRequestEntity(
                    gson.toJson(newObject), CONTENT_JSON, null));
        } catch (UnsupportedEncodingException e) {
            throw new BigSwitchVnsApiException("Failed to encode json request body", e);
        }

        executeMethod(pm);

        if (pm.getStatusCode() != HttpStatus.SC_OK) {
            String errorMessage = responseToErrorMessage(pm);
            pm.releaseConnection();
            s_logger.error("Failed to update object : " + errorMessage);
            throw new BigSwitchVnsApiException("Failed to update object : " + errorMessage);
        }
        pm.releaseConnection();
    }

    protected <T> void executeCreateObject(T newObject, Type returnObjectType, String uri,
                                           Map<String,String> parameters)
                       throws BigSwitchVnsApiException {
        if (_host == null || _host.isEmpty()) {
            throw new BigSwitchVnsApiException("Hostname is null or empty");
        }

        Gson gson = new Gson();

        PostMethod pm = (PostMethod) createMethod("post", uri, 80);
        pm.setRequestHeader(CONTENT_TYPE, CONTENT_JSON);
        pm.setRequestHeader(ACCEPT, CONTENT_JSON);
        pm.setRequestHeader(HTTP_HEADER_INSTANCE_ID, CLOUDSTACK_INSTANCE_ID);
        try {
            pm.setRequestEntity(new StringRequestEntity(
                    gson.toJson(newObject), CONTENT_JSON, null));
        } catch (UnsupportedEncodingException e) {
            throw new BigSwitchVnsApiException("Failed to encode json request body", e);
        }

        executeMethod(pm);

        if (pm.getStatusCode() != HttpStatus.SC_OK) {
            String errorMessage = responseToErrorMessage(pm);
            pm.releaseConnection();
            s_logger.error("Failed to create object : " + errorMessage);
            throw new BigSwitchVnsApiException("Failed to create object : " + errorMessage);
        }
        pm.releaseConnection();

        return;
    }

    protected void executeDeleteObject(String uri) throws BigSwitchVnsApiException {
        if (_host == null || _host.isEmpty()) {
            throw new BigSwitchVnsApiException("Hostname is null or empty");
        }

        DeleteMethod dm = (DeleteMethod) createMethod("delete", uri, 80);
        dm.setRequestHeader(CONTENT_TYPE, CONTENT_JSON);
        dm.setRequestHeader(ACCEPT, CONTENT_JSON);
        dm.setRequestHeader(HTTP_HEADER_INSTANCE_ID, CLOUDSTACK_INSTANCE_ID);

        executeMethod(dm);

        if (dm.getStatusCode() != HttpStatus.SC_OK) {
            String errorMessage = responseToErrorMessage(dm);
            dm.releaseConnection();
            s_logger.error("Failed to delete object : " + errorMessage);
            throw new BigSwitchVnsApiException("Failed to delete object : " + errorMessage);
        }
        dm.releaseConnection();
    }

    @SuppressWarnings("unchecked")
    protected <T> T executeRetrieveObject(Type returnObjectType, String uri, int port, Map<String,String> parameters)
                    throws BigSwitchVnsApiException {
        if (_host == null || _host.isEmpty()) {
            throw new BigSwitchVnsApiException("Hostname is null or empty");
        }

        GetMethod gm = (GetMethod) createMethod("get", uri, port);
        gm.setRequestHeader(CONTENT_TYPE, CONTENT_JSON);
        gm.setRequestHeader(ACCEPT, CONTENT_JSON);
        gm.setRequestHeader(HTTP_HEADER_INSTANCE_ID, CLOUDSTACK_INSTANCE_ID);

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
            throw new BigSwitchVnsApiException("Failed to retrieve object : " + errorMessage);
        }

        Gson gson = new Gson();
        T returnValue;
        try {
            returnValue = (T)gson.fromJson(gm.getResponseBodyAsString(), returnObjectType);
        } catch (IOException e) {
            s_logger.error("IOException while retrieving response body",e);
            throw new BigSwitchVnsApiException(e);
        } finally {
            gm.releaseConnection();
        }
        return returnValue;
    }

    protected void executeMethod(HttpMethodBase method) throws BigSwitchVnsApiException {
        try {
            _client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                method.releaseConnection();
                // login and try again
                login();
                _client.executeMethod(method);
            }
        } catch (HttpException e) {
            s_logger.error("HttpException caught while trying to connect to the BigSwitch Controller", e);
            method.releaseConnection();
            throw new BigSwitchVnsApiException("API call to BigSwitch Controller Failed", e);
        } catch (IOException e) {
            s_logger.error("IOException caught while trying to connect to the BigSwitch Controller", e);
            method.releaseConnection();
            throw new BigSwitchVnsApiException("API call to BigSwitch Controller Failed", e);
        }
    }

    private String responseToErrorMessage(HttpMethodBase method) {
        assert method.isRequestSent() : "no use getting an error message unless the request is sent";

        if ("text/html".equals(method.getResponseHeader(CONTENT_TYPE).getValue())) {
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

}
