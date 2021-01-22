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
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;

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
    private String agentIpAddress;
    private int port;
    private int requestMaxRetries = 0; //TODO

    public KvmAgentHaClient(String agentIpAddress, int port) {
        this.agentIpAddress = agentIpAddress;
        this.port = port;
    }

    /**
     * TODO
     *  Returns the System ID. Used when sending Computer System requests (e.g. ComputerSystem.Reset request).
     */
    public String checkVmsRunningOnAgent() {
        String url = String.format("http://%s:%d", agentIpAddress, port);

        URIBuilder builder = null;
        HttpGet httpReq = null;
        try {
            builder = new URIBuilder(url);
            httpReq = new HttpGet(builder.build());
        } catch (URISyntaxException e) {
            throw new CloudRuntimeException(String.format("Failed to create URI for GET request [URL: %s] due to exception.", url), e);
        }

        HttpClient client = HttpClientBuilder.create().build();

        HttpResponse response = null;

        try {
            response = client.execute(httpReq);
        } catch (IOException e) {
            if (requestMaxRetries == 0) {
                throw new CloudRuntimeException(String.format("Failed to execute HTTP %s request [URL: %s] due to exception %s.", httpReq.getMethod(), url, e), e);
            }
            retryHttpRequest(url, httpReq, client);
        }

        return processHttpResponseIntoJson(response);
    }

    /**
     * TODO
     */
    protected HttpResponse retryHttpRequest(String url, HttpRequestBase httpReq, HttpClient client) {
        LOGGER.warn(String.format("Failed to execute HTTP %s request [URL: %s]. Executing the request again.", httpReq.getMethod(), url));
        HttpResponse response = null;
        for (int attempt = 1; attempt < requestMaxRetries + 1; attempt++) {
            try {
                TimeUnit.SECONDS.sleep(WAIT_FOR_REQUEST_RETRY);
                LOGGER.debug(String.format("Retry HTTP %s request [URL: %s], attempt %d/%d.", httpReq.getMethod(), url, attempt, requestMaxRetries));
                response = client.execute(httpReq);
            } catch (IOException | InterruptedException e) {
                if (attempt == requestMaxRetries) {
                    throw new CloudRuntimeException(
                            String.format("Failed to execute HTTP %s request retry attempt %d/%d [URL: %s] due to exception %s", httpReq.getMethod(), attempt, requestMaxRetries,
                                    url, e));
                } else {
                    LOGGER.warn(
                            String.format("Failed to execute HTTP %s request retry attempt %d/%d [URL: %s] due to exception %s", httpReq.getMethod(), attempt, requestMaxRetries,
                                    url, e));
                }
            }
        }

        if (response == null) {
            throw new CloudRuntimeException(String.format("Failed to execute HTTP %s request [URL: %s].", httpReq.getMethod(), url));
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
        try {
            in = response.getEntity().getContent();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            jsonString = streamReader.readLine();
        } catch (UnsupportedOperationException | IOException e) {
            throw new CloudRuntimeException("Failed to process system Response", e);
        }
        return jsonString;
    }

}
