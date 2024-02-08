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
package org.apache.cloudstack.network.element;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Stratosphere sdn platform api client
 */
public class SspClient {
    protected Logger logger = LogManager.getLogger(getClass());
    private static final HttpClient s_client = new DefaultHttpClient(
            new PoolingClientConnectionManager());
    static {
        s_client.getParams()
                .setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY)
                .setParameter(CoreConnectionPNames.SO_TIMEOUT, 10000);
    }

    private final String apiUrl;
    private final String username;
    private final String password;

    public SspClient(String apiUrl, String username, String password) {
        super();
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
    }

    protected HttpClient getHttpClient() { // for mock test
        return s_client;
    }

    private String executeMethod(HttpRequestBase req, String path) {
        try {
            URI base = new URI(apiUrl);
            req.setURI(new URI(base.getScheme(), base.getUserInfo(), base.getHost(),
                    base.getPort(), path, null, null));
        } catch (URISyntaxException e) {
            logger.error("invalid API URL " + apiUrl + " path " + path, e);
            return null;
        }
        try {
            String content = null;
            try {
                content = getHttpClient().execute(req, new BasicResponseHandler());
                logger.info("ssp api call: " + req);
            } catch (HttpResponseException e) {
                logger.info("ssp api call failed: " + req, e);
                if (e.getStatusCode() == HttpStatus.SC_UNAUTHORIZED && login()) {
                    req.reset();
                    content = getHttpClient().execute(req, new BasicResponseHandler());
                    logger.info("ssp api retry call: " + req);
                }
            }
            return content;
        } catch (ClientProtocolException e) { // includes HttpResponseException
            logger.error("ssp api call failed: " + req, e);
        } catch (IOException e) {
            logger.error("ssp api call failed: " + req, e);
        }
        return null;
    }

    public boolean login() {
        HttpPost method = new HttpPost();
        try {
            method.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                    new BasicNameValuePair("username", username),
                    new BasicNameValuePair("password", password))));
        } catch (UnsupportedEncodingException e) {
            logger.error("invalid username or password", e);
            return false;
        }
        if (executeMethod(method, "/ws.v1/login") != null) {
            return true;
        }
        return false;
    }

    public class TenantNetwork {
        public String uuid;
        public String name;
        @SerializedName("tenant_uuid")
        public String tenantUuid;
    }

    public TenantNetwork createTenantNetwork(String tenantUuid, String networkName) {
        TenantNetwork req = new TenantNetwork();
        req.name = networkName;
        req.tenantUuid = tenantUuid;

        HttpPost method = new HttpPost();
        method.setEntity(new StringEntity(new Gson().toJson(req), ContentType.APPLICATION_JSON));
        return new Gson().fromJson(
                executeMethod(method, "/ssp.v1/tenant-networks"),
                TenantNetwork.class);
    }

    public boolean deleteTenantNetwork(String tenantNetworkUuid) {
        HttpDelete method = new HttpDelete();
        if (executeMethod(method, "/ssp.v1/tenant-networks/" + tenantNetworkUuid) != null) {
            return true;
        }
        return false;
    }

    public class TenantPort {
        public String uuid;
        public String name;
        @SerializedName("network_uuid")
        public String networkUuid;
        @SerializedName("attachment_type")
        public String attachmentType;
        @SerializedName("attachment_ip_address")
        public String hypervisorIpAddress;
        @SerializedName("vlan_id")
        public Integer vlanId;
    }

    public TenantPort createTenantPort(String tenantNetworkUuid) {
        TenantPort req = new TenantPort();
        req.networkUuid = tenantNetworkUuid;
        req.attachmentType = "NoAttachment";

        HttpPost method = new HttpPost();
        method.setEntity(new StringEntity(new Gson().toJson(req), ContentType.APPLICATION_JSON));
        return new Gson().fromJson(
                executeMethod(method, "/ssp.v1/tenant-ports"),
                TenantPort.class);
    }

    public boolean deleteTenantPort(String tenantPortUuid) {
        HttpDelete method = new HttpDelete();
        if (executeMethod(method, "/ssp.v1/tenant-ports/" + tenantPortUuid) != null) {
            return true;
        }
        return false;
    }

    public TenantPort updateTenantVifBinding(String portUuid, String hypervisorIpAddress) {
        TenantPort req = new TenantPort();
        if (hypervisorIpAddress != null) {
            req.attachmentType = "VifAttachment";
            req.hypervisorIpAddress = hypervisorIpAddress;
        } else {
            req.attachmentType = "NoAttachment";
        }

        HttpPut method = new HttpPut();
        method.setEntity(new StringEntity(new Gson().toJson(req), ContentType.APPLICATION_JSON));
        return new Gson().fromJson(
                executeMethod(method, "/ssp.v1/tenant-ports/" + portUuid),
                TenantPort.class);
    }
}
