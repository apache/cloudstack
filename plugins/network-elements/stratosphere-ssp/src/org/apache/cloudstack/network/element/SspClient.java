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

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.EntityEnclosingMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/**
 * Stratosphere sdn platform api client
 */
public class SspClient {
    private static final Logger s_logger = Logger.getLogger(SspClient.class);
    private static final HttpConnectionManager s_httpclient_manager = new MultiThreadedHttpConnectionManager();
    private static final HttpClientParams s_httpclient_params = new HttpClientParams();
    static {
        s_httpclient_params.setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
    }

    private final String apiUrl;
    private final String username;
    private final String password;

    protected HttpClient client;
    protected PostMethod postMethod;
    protected DeleteMethod deleteMethod;
    protected PutMethod putMethod;

    public SspClient(String apiUrl, String username, String password) {
        super();
        this.apiUrl = apiUrl;
        this.username = username;
        this.password = password;
        client = new HttpClient(s_httpclient_params, s_httpclient_manager);
        postMethod = new PostMethod(apiUrl);
        deleteMethod = new DeleteMethod(apiUrl);
        putMethod = new PutMethod(apiUrl);
    }

    public boolean login(){
        PostMethod method = postMethod;
        method.setPath("/ws.v1/login"); // NOTE: /ws.v1/login is correct
        method.addParameter("username", username);
        method.addParameter("password", password);

        try {
            client.executeMethod(method);
        } catch (HttpException e) {
            s_logger.info("Login "+username+" to "+apiUrl+" failed", e);
            return false;
        } catch (IOException e) {
            s_logger.info("Login "+username+" to "+apiUrl+" failed", e);
            return false;
        } finally {
            method.releaseConnection();
        }
        String apiCallPath = null;
        try {
            apiCallPath = method.getName() + " " + method.getURI().toString();
        } catch (URIException e) {
            s_logger.error("method getURI failed", e);
        }
        s_logger.info("ssp api call:" + apiCallPath + " user="+username+" status="+method.getStatusLine());
        if(method.getStatusCode() == HttpStatus.SC_OK){
            return true;
        }
        return false;
    }

    private String executeMethod(HttpMethod method){
        String apiCallPath = null;
        try {
            apiCallPath = method.getName() + " " + method.getURI().toString();
        } catch (URIException e) {
            s_logger.error("method getURI failed", e);
        }

        String response = null;
        try {
            client.executeMethod(method);
            response = method.getResponseBodyAsString();
        } catch (HttpException e) {
            s_logger.error("ssp api call failed "+apiCallPath, e);
            return null;
        } catch (IOException e) {
            s_logger.error("ssp api call failed "+apiCallPath, e);
            return null;
        } finally {
            method.releaseConnection();
        }

        if(method.getStatusCode() == HttpStatus.SC_UNAUTHORIZED){
            if(!login()){
                return null;
            }

            try {
                client.executeMethod(method);
                response = method.getResponseBodyAsString();
            } catch (HttpException e) {
                s_logger.error("ssp api call failed "+apiCallPath, e);
                return null;
            } catch (IOException e) {
                s_logger.error("ssp api call failed "+apiCallPath, e);
                return null;
            } finally {
                method.releaseConnection();
            }
        }
        s_logger.info("ssp api call:" + apiCallPath + " user="+username+" status="+method.getStatusLine());
        if(method instanceof EntityEnclosingMethod){
            EntityEnclosingMethod emethod = (EntityEnclosingMethod)method;
            RequestEntity reqEntity = emethod.getRequestEntity();
            if(reqEntity instanceof StringRequestEntity){
                StringRequestEntity strReqEntity = (StringRequestEntity)reqEntity;
                s_logger.debug("ssp api request body:"+strReqEntity.getContent());
            }else{
                s_logger.debug("ssp api request body:"+emethod.getRequestEntity());
            }
        }
        s_logger.debug("ssp api response body:" + response);
        return response;
    }

    public class TenantNetwork {
        public String uuid;
        public String name;
        @SerializedName("tenant_uuid")
        public String tenantUuid;
    }

    public TenantNetwork createTenantNetwork(String tenantUuid, String networkName){
        TenantNetwork req = new TenantNetwork();
        req.name = networkName;
        req.tenantUuid = tenantUuid;

        PostMethod method = postMethod;
        method.setPath("/ssp.v1/tenant-networks");
        StringRequestEntity entity = null;
        try {
            entity = new StringRequestEntity(new Gson().toJson(req), "application/json", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            s_logger.error("failed creating http request body", e);
            return null;
        }
        method.setRequestEntity(entity);

        String response = executeMethod(method);
        if(response != null && method.getStatusCode() == HttpStatus.SC_CREATED){
            return new Gson().fromJson(response, TenantNetwork.class);
        }
        return null;
    }

    public boolean deleteTenantNetwork(String tenantNetworkUuid){
        DeleteMethod method = deleteMethod;
        method.setPath("/ssp.v1/tenant-networks/"+tenantNetworkUuid);

        executeMethod(method);
        if(method.getStatusCode() == HttpStatus.SC_NO_CONTENT){
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

    public TenantPort createTenantPort(String tenantNetworkUuid){
        TenantPort req = new TenantPort();
        req.networkUuid = tenantNetworkUuid;
        req.attachmentType = "NoAttachment";

        PostMethod method = postMethod;
        method.setPath("/ssp.v1/tenant-ports");
        StringRequestEntity entity = null;
        try {
            entity = new StringRequestEntity(new Gson().toJson(req), "application/json", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            s_logger.error("failed creating http request body", e);
            return null;
        }
        method.setRequestEntity(entity);

        String response = executeMethod(method);
        if(response != null && method.getStatusCode() == HttpStatus.SC_CREATED){
            return new Gson().fromJson(response, TenantPort.class);
        }
        return null;
    }

    public boolean deleteTenantPort(String tenantPortUuid){
        DeleteMethod method = deleteMethod;
        method.setPath("/ssp.v1/tenant-ports/"+tenantPortUuid);

        executeMethod(method);
        if(method.getStatusCode() == HttpStatus.SC_NO_CONTENT){
            return true;
        }
        return false;
    }

    public TenantPort updateTenantVifBinding(String portUuid, String hypervisorIpAddress){
        TenantPort req = new TenantPort();
        if(hypervisorIpAddress != null){
            req.attachmentType = "VifAttachment";
            req.hypervisorIpAddress = hypervisorIpAddress;
        }else{
            req.attachmentType = "NoAttachment";
        }

        PutMethod method = putMethod;
        method.setPath("/ssp.v1/tenant-ports/"+portUuid);
        StringRequestEntity entity = null;
        try {
            entity = new StringRequestEntity(new Gson().toJson(req), "application/json", "UTF-8");
        } catch (UnsupportedEncodingException e) {
            s_logger.error("failed creating http request body", e);
            return null;
        }
        method.setRequestEntity(entity);

        String response = executeMethod(method);
        if(response != null && method.getStatusCode() == HttpStatus.SC_OK){
            return new Gson().fromJson(response, TenantPort.class);
        }
        return null;
    }
}
