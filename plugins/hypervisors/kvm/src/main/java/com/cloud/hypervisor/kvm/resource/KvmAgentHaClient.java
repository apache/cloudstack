/*
 * Copyright 2021 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.hypervisor.kvm.resource;

import com.cloud.utils.exception.CloudRuntimeException;
import com.google.gson.JsonParser;
import org.apache.cloudstack.utils.redfish.RedfishException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * This class provides a client that checks Agent status via a webserver.
 *
 * The additional webserver exposes a simple JSON API which returns a list
 * of Virtual Machines that are running on that host according to libvirt.
 *
 * This way, KVM HA can verify, via libvirt, VMs status with a HTTP-call
 * to this simple webserver and determine if the host is actually down
 * or if it is just the Java Agent which has crashed.
 */
public class KvmAgentHaClient {

    private static final Logger LOGGER = Logger.getLogger(KvmAgentHaClient.class);
    private final static int WAIT_FOR_REQUEST_RETRY = 2;
    private final static String VM_COUNT = "count";
    private final static int ERROR_CODE = -1;
    private final static String EXPECTED_HTTP_STATUS = "2XX";
    private static final int MAX_REQUEST_RETRIES = 2;
    private static final int DEFAULT_PORT = 8080;
    private String agentIpAddress;
    private int port;

    /**
     * Instantiates a webclient that checks, via a webserver running on the KVM host, the VMs running
     * @param agentIpAddress address of the KVM host running the webserver
     */
    public KvmAgentHaClient(String agentIpAddress) {
        this.agentIpAddress = agentIpAddress;
    }

    public boolean isKvmHaAgentRunning() {
        if (countRunningVmsOnAgent() < 0) {
            return false;
        }
        return true;
    }

    /**
     *  Returns the number of VMs running on the KVM host according to libvirt.
     */
    public int countRunningVmsOnAgent() {
        String url = String.format("http://%s:%d", agentIpAddress, DEFAULT_PORT);
        HttpResponse response = executeHttpRequest(url);

        if (response == null)
            return ERROR_CODE;

        return Integer.valueOf(processHttpResponseIntoJson(response));
    }

    /**
     * Executes a GET request for the given URL address.
     */
    @Nullable
    protected HttpResponse executeHttpRequest(String url) {
        HttpGet httpReq = null;
        try {
            URIBuilder builder = new URIBuilder(url);
            httpReq = new HttpGet(builder.build());
        } catch (URISyntaxException e) {
            LOGGER.error(String.format("Failed to create URI for GET request [URL: %s] due to exception.", url), e);
            return null;
        }

        HttpClient client = HttpClientBuilder.create().build();
        HttpResponse response = null;
        try {
            response = client.execute(httpReq);
        } catch (IOException e) {
            if (MAX_REQUEST_RETRIES == 0) {
                LOGGER.warn(String.format("Failed to execute HTTP %s request [URL: %s] due to exception %s.", httpReq.getMethod(), url, e), e);
                return null;
            }
            retryHttpRequest(url, httpReq, client);
        }
        return response;
    }

    /**
     * Re-executes the HTTP GET request until it gets a response or it reaches the maximum request retries (#MAX_REQUEST_RETRIES)
     */
    protected HttpResponse retryHttpRequest(String url, HttpRequestBase httpReq, HttpClient client) {
        LOGGER.warn(String.format("Failed to execute HTTP %s request [URL: %s]. Executing the request again.", httpReq.getMethod(), url));
        HttpResponse response = null;
        for (int attempt = 1; attempt < MAX_REQUEST_RETRIES + 1; attempt++) {
            try {
                TimeUnit.SECONDS.sleep(WAIT_FOR_REQUEST_RETRY);
                LOGGER.debug(String.format("Retry HTTP %s request [URL: %s], attempt %d/%d.", httpReq.getMethod(), url, attempt, MAX_REQUEST_RETRIES));
                response = client.execute(httpReq);
            } catch (IOException | InterruptedException e) {
                if (attempt == MAX_REQUEST_RETRIES) {
                    LOGGER.error(
                            String.format("Failed to execute HTTP %s request retry attempt %d/%d [URL: %s] due to exception %s", httpReq.getMethod(), attempt, MAX_REQUEST_RETRIES,
                                    url, e));
                } else {
                    LOGGER.error(
                            String.format("Failed to execute HTTP %s request retry attempt %d/%d [URL: %s] due to exception %s", httpReq.getMethod(), attempt, MAX_REQUEST_RETRIES,
                                    url, e));
                }
            }
        }

        if (response == null) {
            LOGGER.error(String.format("Failed to execute HTTP %s request [URL: %s].", httpReq.getMethod(), url));
        }

        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode < HttpStatus.SC_OK || statusCode >= HttpStatus.SC_MULTIPLE_CHOICES) {
            throw new RedfishException(String.format("Failed to get VMs information with a %s request to URL '%s'. The expected HTTP status code is '%s' but it got '%s'.",
                    HttpGet.METHOD_NAME, url, EXPECTED_HTTP_STATUS, statusCode));
        }

        LOGGER.debug(String.format("Successfully executed HTTP %s request [URL: %s].", httpReq.getMethod(), url));
        return response;
    }

    /**
     * TODO
     * Processes the response of request GET System ID as a JSON object.
     */
    protected String processHttpResponseIntoJson(HttpResponse response) {
        InputStream in;
        String jsonString;
        if (response == null) {
            return Integer.toString(ERROR_CODE);
        }
        try {
            in = response.getEntity().getContent();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            jsonString = streamReader.readLine();
        } catch (UnsupportedOperationException | IOException e) {
            throw new CloudRuntimeException("Failed to process response", e);
        }

        String vmsCount = new JsonParser().parse(jsonString).getAsJsonObject().get(VM_COUNT).getAsString();
        return vmsCount;
    }

}
