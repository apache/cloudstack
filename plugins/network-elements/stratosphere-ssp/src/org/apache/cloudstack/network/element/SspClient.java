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
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

/**
 * Stratosphere sdn platform api client
 */
public class SspClient {
    private static final Logger s_logger = Logger.getLogger(SspClient.class);
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

    private HttpResponse innerExecuteMethod(HttpRequestBase req, String path) {
        try {
            URI base = new URI(apiUrl);
            req.setURI(new URI(base.getScheme(), base.getUserInfo(), base.getHost(),
                    base.getPort(), path, null, null));
        } catch (URISyntaxException e) {
            s_logger.error("invalid API URL " + apiUrl + " path " + path, e);
            return null;
        }
        HttpResponse res = null;
        try {
            res = getHttpClient().execute(req);
            s_logger.info("ssp api call:" + req + " status=" + res.getStatusLine());
        } catch (IOException e) {
            s_logger.error("ssp api call failed: " + req, e);
        }
        return res;
    }

    private HttpResponse executeMethod(HttpRequestBase req, String path) {
        HttpResponse res = innerExecuteMethod(req, path);
        if (res.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED && login()) {
            req.reset();
            res = innerExecuteMethod(req, path);
        }
        return res;
    }

    public boolean login() {
        HttpPost method = new HttpPost();
        try {
            method.setEntity(new UrlEncodedFormEntity(Arrays.asList(
                    new BasicNameValuePair("username", username),
                    new BasicNameValuePair("password", password))));
        } catch (UnsupportedEncodingException e) {
            s_logger.error("invalid username or password", e);
            return false;
        }

        HttpResponse res = this.innerExecuteMethod(method, "/ws.v1/login");
        if (res != null && res.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
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
        HttpResponse res = executeMethod(method, "/ssp.v1/tenant-networks");
        if (res == null || res.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
            return null;
        }
        try {
            return new Gson().fromJson(new InputStreamReader(res.getEntity().getContent()),
                    TenantNetwork.class);
        } catch (JsonSyntaxException e) {
            s_logger.error("reading response body failed", e);
        } catch (JsonIOException e) {
            s_logger.error("reading response body failed", e);
        } catch (IllegalStateException e) {
            s_logger.error("reading response body failed", e);
        } catch (IOException e) {
            s_logger.error("reading response body failed", e);
        }
        return null;
    }

    public boolean deleteTenantNetwork(String tenantNetworkUuid) {
        HttpDelete method = new HttpDelete();
        HttpResponse res = executeMethod(method, "/ssp.v1/tenant-networks/" + tenantNetworkUuid);
        if (res != null && res.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
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
        HttpResponse res = executeMethod(method, "/ssp.v1/tenant-ports");

        if (res == null || res.getStatusLine().getStatusCode() != HttpStatus.SC_CREATED) {
            return null;
        }
        try {
            return new Gson().fromJson(new InputStreamReader(res.getEntity().getContent()),
                    TenantPort.class);
        } catch (JsonSyntaxException e) {
            s_logger.error("reading response body failed", e);
        } catch (JsonIOException e) {
            s_logger.error("reading response body failed", e);
        } catch (IllegalStateException e) {
            s_logger.error("reading response body failed", e);
        } catch (IOException e) {
            s_logger.error("reading response body failed", e);
        }
        return null;
    }

    public boolean deleteTenantPort(String tenantPortUuid) {
        HttpDelete method = new HttpDelete();
        HttpResponse res = executeMethod(method, "/ssp.v1/tenant-ports/" + tenantPortUuid);

        if (res != null && res.getStatusLine().getStatusCode() == HttpStatus.SC_NO_CONTENT) {
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
        HttpResponse res = executeMethod(method, "/ssp.v1/tenant-ports/" + portUuid);
        if (res == null || res.getStatusLine().getStatusCode() != HttpStatus.SC_OK) {
            return null;
        }
        try {
            return new Gson().fromJson(new InputStreamReader(res.getEntity().getContent()),
                    TenantPort.class);
        } catch (JsonSyntaxException e) {
            s_logger.error("reading response body failed", e);
        } catch (JsonIOException e) {
            s_logger.error("reading response body failed", e);
        } catch (IllegalStateException e) {
            s_logger.error("reading response body failed", e);
        } catch (IOException e) {
            s_logger.error("reading response body failed", e);
        }
        return null;
    }
}
