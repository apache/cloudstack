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

package org.apache.cloudstack.network.opendaylight.api.resources;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.cloudstack.network.opendaylight.api.NeutronInvalidCredentialsException;
import org.apache.cloudstack.network.opendaylight.api.NeutronRestApi;
import org.apache.cloudstack.network.opendaylight.api.NeutronRestApiException;
import org.apache.cloudstack.network.opendaylight.api.NeutronRestFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.log4j.Logger;

public abstract class Action {

    private static final Logger s_logger = Logger.getLogger(Action.class);
    private static final int BODY_RESP_MAX_LEN = 1024;

    // private static final String DEFAULT

    protected static final String TEXT_HTML_CONTENT_TYPE = "text/html";
    protected static final String JSON_CONTENT_TYPE = "application/json";
    protected static final String CONTENT_TYPE = "content-type";

    private final URL url;
    private final String username;
    private final String password;

    public Action(final URL url, final String username, final String password) {

        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String executeGet(final String uri, final Map<String, String> parameters) throws NeutronRestApiException {
        try {
            validateCredentials();
        } catch (NeutronInvalidCredentialsException e) {
            throw new NeutronRestApiException("Invalid credentials!", e);
        }

        NeutronRestFactory factory = NeutronRestFactory.getInstance();

        NeutronRestApi neutronRestApi = factory.getNeutronApi(GetMethod.class);
        GetMethod getMethod = (GetMethod) neutronRestApi.createMethod(url, uri);

        try {
            getMethod.setRequestHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);

            String encodedCredentials = encodeCredentials();
            getMethod.setRequestHeader("Authorization", "Basic " + encodedCredentials);

            if (parameters != null && !parameters.isEmpty()) {
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(parameters.size());
                for (Entry<String, String> e : parameters.entrySet()) {
                    nameValuePairs.add(new NameValuePair(e.getKey(), e.getValue()));
                }
                getMethod.setQueryString(nameValuePairs.toArray(new NameValuePair[0]));
            }

            neutronRestApi.executeMethod(getMethod);

            if (getMethod.getStatusCode() != HttpStatus.SC_OK) {
                String errorMessage = responseToErrorMessage(getMethod);
                getMethod.releaseConnection();
                s_logger.error("Failed to retrieve object : " + errorMessage);
                throw new NeutronRestApiException("Failed to retrieve object : " + errorMessage);
            }

            return getMethod.getResponseBodyAsString();

        } catch (NeutronRestApiException e) {
            s_logger.error("NeutronRestApiException caught while trying to execute HTTP Method on the Neutron Controller", e);
            throw new NeutronRestApiException("API call to Neutron Controller Failed", e);
        } catch (IOException e) {
            throw new NeutronRestApiException(e);
        } finally {
            getMethod.releaseConnection();
        }
    }

    protected String executePost(final String uri, final StringRequestEntity entity) throws NeutronRestApiException {
        try {
            validateCredentials();
        } catch (NeutronInvalidCredentialsException e) {
            throw new NeutronRestApiException("Invalid credentials!", e);
        }

        NeutronRestFactory factory = NeutronRestFactory.getInstance();

        NeutronRestApi neutronRestApi = factory.getNeutronApi(PostMethod.class);
        PostMethod postMethod = (PostMethod) neutronRestApi.createMethod(url, uri);

        try {
            postMethod.setRequestHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);
            postMethod.setRequestEntity(entity);

            String encodedCredentials = encodeCredentials();
            postMethod.setRequestHeader("Authorization", "Basic " + encodedCredentials);

            neutronRestApi.executeMethod(postMethod);

            if (postMethod.getStatusCode() != HttpStatus.SC_CREATED) {
                String errorMessage = responseToErrorMessage(postMethod);
                postMethod.releaseConnection();
                s_logger.error("Failed to create object : " + errorMessage);
                throw new NeutronRestApiException("Failed to create object : " + errorMessage);
            }

            return postMethod.getResponseBodyAsString();
        } catch (NeutronRestApiException e) {
            s_logger.error("NeutronRestApiException caught while trying to execute HTTP Method on the Neutron Controller", e);
            throw new NeutronRestApiException("API call to Neutron Controller Failed", e);
        } catch (IOException e) {
            throw new NeutronRestApiException("Failed to load json response body", e);
        } finally {
            postMethod.releaseConnection();
        }
    }

    protected void executePut(final String uri, final StringRequestEntity entity) throws NeutronRestApiException {
        try {
            validateCredentials();
        } catch (NeutronInvalidCredentialsException e) {
            throw new NeutronRestApiException("Invalid credentials!", e);
        }

        NeutronRestFactory factory = NeutronRestFactory.getInstance();

        NeutronRestApi neutronRestApi = factory.getNeutronApi(PutMethod.class);
        PutMethod putMethod = (PutMethod) neutronRestApi.createMethod(url, uri);

        try {
            putMethod.setRequestHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);
            putMethod.setRequestEntity(entity);

            String encodedCredentials = encodeCredentials();
            putMethod.setRequestHeader("Authorization", "Basic " + encodedCredentials);

            neutronRestApi.executeMethod(putMethod);

            if (putMethod.getStatusCode() != HttpStatus.SC_OK) {
                String errorMessage = responseToErrorMessage(putMethod);
                putMethod.releaseConnection();
                s_logger.error("Failed to update object : " + errorMessage);
                throw new NeutronRestApiException("Failed to update object : " + errorMessage);
            }
        } catch (NeutronRestApiException e) {
            s_logger.error("NeutronRestApiException caught while trying to execute HTTP Method on the Neutron Controller", e);
            throw new NeutronRestApiException("API call to Neutron Controller Failed", e);
        } finally {
            putMethod.releaseConnection();
        }
    }

    protected String executePut(final String uri) throws NeutronRestApiException {
        try {
            validateCredentials();
        } catch (NeutronInvalidCredentialsException e) {
            throw new NeutronRestApiException("Invalid credentials!", e);
        }

        NeutronRestFactory factory = NeutronRestFactory.getInstance();

        NeutronRestApi neutronRestApi = factory.getNeutronApi(PutMethod.class);
        PutMethod putMethod = (PutMethod) neutronRestApi.createMethod(url, uri);

        try {
            String encodedCredentials = encodeCredentials();
            putMethod.setRequestHeader("Authorization", "Basic " + encodedCredentials);

            neutronRestApi.executeMethod(putMethod);

            if (putMethod.getStatusCode() != HttpStatus.SC_OK) {
                String errorMessage = responseToErrorMessage(putMethod);
                putMethod.releaseConnection();
                s_logger.error("Failed to update object : " + errorMessage);
                throw new NeutronRestApiException("Failed to update object : " + errorMessage);
            }

            return putMethod.getResponseBodyAsString();
        } catch (NeutronRestApiException e) {
            s_logger.error("NeutronRestApiException caught while trying to execute HTTP Method on the Neutron Controller", e);
            throw new NeutronRestApiException("API call to Neutron Controller Failed", e);
        } catch (IOException e) {
            throw new NeutronRestApiException("Failed to load json response body", e);
        } finally {
            putMethod.releaseConnection();
        }
    }

    protected void executeDelete(final String uri) throws NeutronRestApiException {
        try {
            validateCredentials();
        } catch (NeutronInvalidCredentialsException e) {
            throw new NeutronRestApiException("Invalid credentials!", e);
        }

        NeutronRestFactory factory = NeutronRestFactory.getInstance();

        NeutronRestApi neutronRestApi = factory.getNeutronApi(DeleteMethod.class);
        DeleteMethod deleteMethod = (DeleteMethod) neutronRestApi.createMethod(url, uri);

        try {
            deleteMethod.setRequestHeader(CONTENT_TYPE, JSON_CONTENT_TYPE);

            String encodedCredentials = encodeCredentials();
            deleteMethod.setRequestHeader("Authorization", "Basic " + encodedCredentials);

            neutronRestApi.executeMethod(deleteMethod);

            if (deleteMethod.getStatusCode() != HttpStatus.SC_NO_CONTENT) {
                String errorMessage = responseToErrorMessage(deleteMethod);
                deleteMethod.releaseConnection();
                s_logger.error("Failed to delete object : " + errorMessage);
                throw new NeutronRestApiException("Failed to delete object : " + errorMessage);
            }
        } catch (NeutronRestApiException e) {
            s_logger.error("NeutronRestApiException caught while trying to execute HTTP Method on the Neutron Controller", e);
            throw new NeutronRestApiException("API call to Neutron Controller Failed", e);
        } finally {
            deleteMethod.releaseConnection();
        }
    }

    private void validateCredentials() throws NeutronInvalidCredentialsException {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            throw new NeutronInvalidCredentialsException("Credentials are null or empty");
        }
    }

    private String encodeCredentials() {
        String authString = username + ":" + password;
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        String authStringEnc = new String(authEncBytes);
        return authStringEnc;
    }

    private String responseToErrorMessage(final HttpMethodBase method) {
        assert method.isRequestSent() : "no use getting an error message unless the request is sent";

        final Header contentTypeHeader = method.getResponseHeader(CONTENT_TYPE);
        if (contentTypeHeader != null && TEXT_HTML_CONTENT_TYPE.equals(contentTypeHeader.getValue())) {
            // The error message is the response content
            // Safety margin of 1024 characters, anything longer is probably
            // useless and will clutter the logs
            try {
                return method.getResponseBodyAsString(BODY_RESP_MAX_LEN);
            } catch (IOException e) {
                s_logger.debug("Error while loading response body", e);
            }
        }

        // The default
        return method.getStatusText();
    }
}
