//
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
//

package com.cloud.network.bigswitch;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.binary.Base64;
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
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class BigSwitchBcfApi {
    private static final Logger S_LOGGER = Logger.getLogger(BigSwitchBcfApi.class);
    private final static String S_PROTOCOL = "https";
    private final static String S_NS_BASE_URL = "/networkService/v1.1";
    private final static String CONTENT_TYPE = "Content-type";
    private final static String ACCEPT = "Accept";
    private final static String CONTENT_JSON = "application/json";
    private final static String HTTP_HEADER_INSTANCE_ID = "Instance-ID";
    private final static String CLOUDSTACK_INSTANCE_ID = "cloudstack";
    private final static String HASH_MATCH = "X-BSN-BVS-HASH-MATCH";
    private final static MultiThreadedHttpConnectionManager S_HTTP_CLIENT_MANAGER = new MultiThreadedHttpConnectionManager();

    private String host;
    private String username;
    private String password;
    private String hash;
    private String zoneId;
    private Boolean nat;

    private boolean isPrimary;

    private int _port = 8000;

    private HttpClient _client;
    private Gson gson = new Gson();

    public final static String HASH_CONFLICT = "HASH_CONFLICT";
    public final static String HASH_IGNORE = "HASH_IGNORE";

    /* This factory method is protected so we can extend this
     * in the unittests.
     */
    protected HttpClient createHttpClient() {
        return new HttpClient(S_HTTP_CLIENT_MANAGER);
    }

    protected HttpMethod createMethod(final String type, final String uri, final int port) throws BigSwitchBcfApiException {
        String url;
        try {
            url = new URL(S_PROTOCOL, host, port, uri).toString();
        } catch (MalformedURLException e) {
            S_LOGGER.error("Unable to build Big Switch API URL", e);
            throw new BigSwitchBcfApiException("Unable to build Big Switch API URL", e);
        }

        if ("post".equalsIgnoreCase(type)) {
            return new PostMethod(url);
        } else if ("get".equalsIgnoreCase(type)) {
            return new GetMethod(url);
        } else if ("delete".equalsIgnoreCase(type)) {
            return new DeleteMethod(url);
        } else if ("put".equalsIgnoreCase(type)) {
            return new PutMethod(url);
        } else {
            throw new BigSwitchBcfApiException("Requesting unknown method type");
        }
    }

    public BigSwitchBcfApi() {
        _client = createHttpClient();
        _client.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

        try {
            // Cast to ProtocolSocketFactory to avoid the deprecated constructor with the SecureProtocolSocketFactory parameter
            Protocol.registerProtocol("https", new Protocol("https", (ProtocolSocketFactory) new TrustingProtocolSocketFactory(), _port));
        } catch (IOException e) {
            S_LOGGER.warn("Failed to register the TrustingProtocolSocketFactory, falling back to default SSLSocketFactory", e);
        }
    }

    /**
     * Setter used by UI to set BSN controller address
     * @param address
     */
    public void setControllerAddress(final String address) {
        this.host = address;
    }

    /**
     * Setter used by UI to set BSN controller user name
     * @param username
     */
    public void setControllerUsername(final String username) {
        this.username = username;
    }

    /**
     * Setter used by UI to set BSN controller password
     * @param password
     */
    public void setControllerPassword(final String password) {
        this.password = password;
    }

    /**
     * Setter used by UI to set BSN controller NAT mode
     * @param nat
     */
    public void setControllerNat(final Boolean nat) {
        this.nat = nat;
    }

    public boolean isNatEnabled() {
        return this.nat;
    }

    public void setZoneId(final String zoneId) {
        this.zoneId = zoneId;
    }

    public String createNetwork(final NetworkData network) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/tenants/" + network.getNetwork().getTenantId() + "/networks";
        return executeCreateObject(network, uri, Collections.<String, String> emptyMap());
    }

    public String deleteNetwork(final String tenantId, final String networkId) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/tenants/" + tenantId + "/networks/" + networkId;
        return executeDeleteObject(uri);
    }

    public String createAttachment(final String tenantId, final String networkId,
            final AttachmentData attachment) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/tenants/" + tenantId + "/networks/" + networkId + "/ports/" + attachment.getAttachment().getId() + "/attachment";
        return executeCreateObject(attachment, uri, Collections.<String, String> emptyMap());
    }

    public String modifyAttachment(final String tenantId, final String networkId,
            final AttachmentData attachment) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/tenants/" + tenantId + "/networks/" + networkId + "/ports/" + attachment.getAttachment().getId() + "/attachment";
        return executeUpdateObject(attachment, uri, Collections.<String, String> emptyMap());
    }

    public String deleteAttachment(final String tenantId, final String networkId,
            final String attachmentId) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/tenants/" + tenantId + "/networks/" + networkId + "/ports/" + attachmentId + "/attachment";
        return executeDeleteObject(uri);
    }

    public String createRouter(final String tenantId, final RouterData router) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/tenants/" + tenantId + "/routers";
        return executeCreateObject(router, uri, Collections.<String, String> emptyMap());
    }

    public String modifyRouter(final String tenantId, final RouterData router) throws BigSwitchBcfApiException,
    IllegalArgumentException{
        String uri = S_NS_BASE_URL + "/tenants/" + tenantId + "/routers";
        return executeCreateObject(router, uri, Collections.<String, String> emptyMap());
    }

    public String createRouterInterface(final String tenantId, final String routerId,
            final RouterInterfaceData routerInterface) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/tenants/" + tenantId + "/routers/" + routerId + "/interfaces";
        return executeCreateObject(routerInterface, uri, Collections.<String, String> emptyMap());
    }

    public String createFloatingIp(final String tenantId, final FloatingIpData fip) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/tenants/" + tenantId + "/floatingips";
        return executeCreateObject(fip, uri, Collections.<String, String> emptyMap());
    }

    public String deleteFloatingIp(final String tenantId, final String fipId) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/tenants/" + tenantId + "/floatingips/" + fipId;
        return executeDeleteObject(uri);
    }

    public ControlClusterStatus getControlClusterStatus() throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/health";
        ControlClusterStatus ccs = executeRetrieveObject(new TypeToken<ControlClusterStatus>() {
        }.getType(), uri, null);
        ccs.setStatus(true);
        return ccs;
    }

    public Capabilities getCapabilities() throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/capabilities";
        List<String> capslist = executeRetrieveObject(new TypeToken<List<String>>() {
        }.getType(), uri, null);
        Capabilities caps = new Capabilities();
        caps.setCapabilities(capslist);
        return caps;
    }

    public String syncTopology(final TopologyData topo) throws BigSwitchBcfApiException {
        String uri = S_NS_BASE_URL + "/topology";
        return executeCreateObject(topo, uri, Collections.<String, String> emptyMap());
    }

    public ControllerData getControllerData() {
        return new ControllerData(host, isPrimary);
    }

    private void checkInvariants() throws BigSwitchBcfApiException{
        if (host == null || host.isEmpty()) {
            throw new BigSwitchBcfApiException("Hostname is null or empty");
        }
        if (username == null || username.isEmpty()){
            throw new BigSwitchBcfApiException("Username is null or empty");
        }
        if (password == null || password.isEmpty()){
            throw new BigSwitchBcfApiException("Password is null or empty");
        }
    }

    private String checkResponse(final HttpMethodBase m, final String errorMessageBase) throws BigSwitchBcfApiException,
    IllegalArgumentException{
        String customErrorMsg = null;
        if (m.getStatusCode() == HttpStatus.SC_OK) {
            String hash = "";
            if (m.getResponseHeader(HASH_MATCH) != null) {
                hash = m.getResponseHeader(HASH_MATCH).getValue();
                set_hash(hash);
            }
            return hash;
        }
        if (m.getStatusCode() == HttpStatus.SC_CONFLICT) {
            if(m instanceof GetMethod) {
                return HASH_CONFLICT;
            }
            throw new BigSwitchBcfApiException("BCF topology sync required", true);
        }
        if (m.getStatusCode() == HttpStatus.SC_SEE_OTHER) {
            isPrimary = false;
            set_hash(HASH_IGNORE);
            return HASH_IGNORE;
        }
        if (m.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            if (m instanceof DeleteMethod){
                return "";
            }
        }
        if (m.getStatusCode() == HttpStatus.SC_BAD_REQUEST) {
            customErrorMsg = " Invalid data in BCF request";
            throw new IllegalArgumentException(customErrorMsg);
        }
        String errorMessage = responseToErrorMessage(m);
        m.releaseConnection();
        S_LOGGER.error(errorMessageBase + errorMessage);
        throw new BigSwitchBcfApiException(errorMessageBase + errorMessage + customErrorMsg);
    }

    private void setHttpHeader(final HttpMethodBase m) {
        m.setRequestHeader(CONTENT_TYPE, CONTENT_JSON);
        m.setRequestHeader(ACCEPT, CONTENT_JSON);
        m.setRequestHeader(HTTP_HEADER_INSTANCE_ID, CLOUDSTACK_INSTANCE_ID + "-" + zoneId);
        if (StringUtils.isNotEmpty(hash)) {
            m.setRequestHeader(HASH_MATCH, hash);
        }

        String authString = username + ":" + password;
        String encodedAuthString = "Basic " + Base64.encodeBase64String(authString.getBytes(Charset.forName("UTF-8")));
        m.setRequestHeader("Authorization", encodedAuthString);
    }

    protected <T> String executeUpdateObject(final T newObject, final String uri,
            final Map<String, String> parameters) throws BigSwitchBcfApiException,
    IllegalArgumentException{
        checkInvariants();

        PutMethod pm = (PutMethod)createMethod("put", uri, _port);

        setHttpHeader(pm);

        try {
            pm.setRequestEntity(new StringRequestEntity(gson.toJson(newObject), CONTENT_JSON, null));
        } catch (UnsupportedEncodingException e) {
            throw new BigSwitchBcfApiException("Failed to encode json request body", e);
        }

        executeMethod(pm);

        String hash = checkResponse(pm, "BigSwitch HTTP update failed: ");

        pm.releaseConnection();

        return hash;
    }

    protected <T> String executeCreateObject(final T newObject, final String uri,
            final Map<String, String> parameters) throws BigSwitchBcfApiException {
        checkInvariants();

        PostMethod pm = (PostMethod)createMethod("post", uri, _port);

        setHttpHeader(pm);

        try {
            pm.setRequestEntity(new StringRequestEntity(gson.toJson(newObject), CONTENT_JSON, null));
        } catch (UnsupportedEncodingException e) {
            throw new BigSwitchBcfApiException("Failed to encode json request body", e);
        }

        executeMethod(pm);

        String hash = checkResponse(pm, "BigSwitch HTTP create failed: ");

        pm.releaseConnection();

        return hash;
    }

    protected String executeDeleteObject(final String uri) throws BigSwitchBcfApiException {
        checkInvariants();

        DeleteMethod dm = (DeleteMethod)createMethod("delete", uri, _port);

        setHttpHeader(dm);

        executeMethod(dm);

        String hash = checkResponse(dm, "BigSwitch HTTP delete failed: ");

        dm.releaseConnection();

        return hash;
    }

    @SuppressWarnings("unchecked")
    protected <T> T executeRetrieveObject(final Type returnObjectType,
            final String uri, final Map<String, String> parameters) throws BigSwitchBcfApiException {
        checkInvariants();

        GetMethod gm = (GetMethod)createMethod("get", uri, _port);

        setHttpHeader(gm);

        if (parameters != null && !parameters.isEmpty()) {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(parameters.size());
            for (Entry<String, String> e : parameters.entrySet()) {
                nameValuePairs.add(new NameValuePair(e.getKey(), e.getValue()));
            }
            gm.setQueryString(nameValuePairs.toArray(new NameValuePair[0]));
        }

        executeMethod(gm);

        String hash = checkResponse(gm, "BigSwitch HTTP get failed: ");

        T returnValue;
        try {
            // CAUTIOUS: Safety margin of 2048 characters - extend if needed.
            returnValue = (T)gson.fromJson(gm.getResponseBodyAsString(2048), returnObjectType);
        } catch (IOException e) {
            S_LOGGER.error("IOException while retrieving response body", e);
            throw new BigSwitchBcfApiException(e);
        } finally {
            gm.releaseConnection();
        }
        if(returnValue instanceof ControlClusterStatus) {
            if(HASH_CONFLICT.equals(hash)) {
                isPrimary = true;
                ((ControlClusterStatus) returnValue).setTopologySyncRequested(true);
            } else if (!HASH_IGNORE.equals(hash) && !isPrimary) {
                isPrimary = true;
                ((ControlClusterStatus) returnValue).setTopologySyncRequested(true);
            }
        }
        return returnValue;
    }

    protected void executeMethod(final HttpMethodBase method) throws BigSwitchBcfApiException {
        try {
            _client.executeMethod(method);
            if (method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                method.releaseConnection();
            }
        } catch (HttpException e) {
            S_LOGGER.error("HttpException caught while trying to connect to the BigSwitch Controller", e);
            method.releaseConnection();
            throw new BigSwitchBcfApiException("API call to BigSwitch Controller Failed", e);
        } catch (IOException e) {
            S_LOGGER.error("IOException caught while trying to connect to the BigSwitch Controller", e);
            method.releaseConnection();
            throw new BigSwitchBcfApiException("API call to BigSwitch Controller Failed", e);
        }
    }

    private String responseToErrorMessage(final HttpMethodBase method) {
        assert method.isRequestSent() : "no use getting an error message unless the request is sent";

        if ("text/html".equals(method.getResponseHeader(CONTENT_TYPE).getValue())) {
            // The error message is the response content
            // Safety margin of 2048 characters, anything longer is probably useless
            // and will clutter the logs
            try {
                return method.getResponseBodyAsString(2048);
            } catch (IOException e) {
                S_LOGGER.debug("Error while loading response body", e);
            }
        }

        // The default
        return method.getStatusText();
    }

    public static String getCloudstackInstanceId() {
        return CLOUDSTACK_INSTANCE_ID;
    }

    public String get_hash() {
        return hash;
    }

    public void set_hash(final String hash) {
        this.hash = hash;
    }

}
