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
package com.cloud.cluster;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.apache.cloudstack.framework.ca.CAService;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.cloud.utils.HttpUtils;
import com.cloud.utils.Profiler;
import com.cloud.utils.nio.Link;
import com.google.gson.Gson;

public class ClusterServiceServletImpl implements ClusterService {
    private static final long serialVersionUID = 4574025200012566153L;
    protected Logger logger = LogManager.getLogger(getClass());

    private String serviceUrl;

    private CAService caService;

    private Gson gson = new Gson();

    protected static CloseableHttpClient s_client = null;

    private void logPostParametersForFailedEncoding(List<NameValuePair> parameters) {
        if (logger.isTraceEnabled()) {
            logger.trace(String.format("%s encoding failed for POST parameters: %s", HttpUtils.UTF_8,
                    gson.toJson(parameters)));
        }
    }

    public ClusterServiceServletImpl() {
    }

    public ClusterServiceServletImpl(final String serviceUrl, final CAService caService) {
            logger.info(String.format("Setup cluster service servlet. service url: %s, request timeout: %d seconds", serviceUrl,
                ClusterServiceAdapter.ClusterMessageTimeOut.value()));
        this.serviceUrl = serviceUrl;
        this.caService = caService;
    }

    protected List<NameValuePair> getClusterServicePduPostParameters(final ClusterServicePdu pdu) {
        List<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair("method", Integer.toString(RemoteMethodConstants.METHOD_DELIVER_PDU)));
        postParameters.add(new BasicNameValuePair("sourcePeer", pdu.getSourcePeer()));
        postParameters.add(new BasicNameValuePair("destPeer", pdu.getDestPeer()));
        postParameters.add(new BasicNameValuePair("pduSeq", Long.toString(pdu.getSequenceId())));
        postParameters.add(new BasicNameValuePair("pduAckSeq", Long.toString(pdu.getAckSequenceId())));
        postParameters.add(new BasicNameValuePair("agentId", Long.toString(pdu.getAgentId())));
        postParameters.add(new BasicNameValuePair("gsonPackage", pdu.getJsonPackage()));
        postParameters.add(new BasicNameValuePair("stopOnError", pdu.isStopOnError() ? "1" : "0"));
        postParameters.add(new BasicNameValuePair("pduType", Integer.toString(pdu.getPduType())));
        return postParameters;
    }

    @Override
    public String execute(final ClusterServicePdu pdu) throws RemoteException {
        if (logger.isDebugEnabled()) {
            logger.debug(String.format("Executing ClusterServicePdu with service URL: %s", serviceUrl));
        }
        final CloseableHttpClient client = getHttpClient();
        final HttpPost method = new HttpPost(serviceUrl);
        final List<NameValuePair> postParameters = getClusterServicePduPostParameters(pdu);
        try {
            method.setEntity(new UrlEncodedFormEntity(postParameters, HttpUtils.UTF_8));
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to encode request POST parameters", e);
            logPostParametersForFailedEncoding(postParameters);
            throw new RemoteException("Failed to encode request POST parameters", e);
        }

        return executePostMethod(client, method);
    }

    protected List<NameValuePair> getPingPostParameters(final String callingPeer) {
        List<NameValuePair> postParameters = new ArrayList<>();
        postParameters.add(new BasicNameValuePair("method", Integer.toString(RemoteMethodConstants.METHOD_PING)));
        postParameters.add(new BasicNameValuePair("callingPeer", callingPeer));
        return postParameters;
    }

    @Override
    public boolean ping(final String callingPeer) throws RemoteException {
        if (logger.isDebugEnabled()) {
            logger.debug("Ping at " + serviceUrl);
        }

        final CloseableHttpClient client = getHttpClient();
        final HttpPost method = new HttpPost(serviceUrl);

        List<NameValuePair> postParameters = getPingPostParameters(callingPeer);
        try {
            method.setEntity(new UrlEncodedFormEntity(postParameters, HttpUtils.UTF_8));
        } catch (UnsupportedEncodingException e) {
            logger.error("Failed to encode ping request POST parameters", e);
            logPostParametersForFailedEncoding(postParameters);
            throw new RemoteException("Failed to encode ping request POST parameters", e);
        }

        final String returnVal = executePostMethod(client, method);
        return Boolean.TRUE.toString().equalsIgnoreCase(returnVal);
    }

    private String executePostMethod(final CloseableHttpClient client, final HttpPost method) {
        String result = null;
        try {
            final Profiler profiler = new Profiler();
            profiler.start();
            CloseableHttpResponse httpResponse = client.execute(method);
            int response = httpResponse.getStatusLine().getStatusCode();
            if (response == HttpStatus.SC_OK) {
                result = EntityUtils.toString(httpResponse.getEntity());
                profiler.stop();
                if (logger.isDebugEnabled()) {
                    logger.debug("POST " + serviceUrl + " response :" + result + ", responding time: " + profiler.getDurationInMillis() + " ms");
                }
            } else {
                profiler.stop();
                logger.error("Invalid response code : " + response + ", from : " + serviceUrl + ", method : " + method.getParams().getParameter("method") + " responding time: " +
                        profiler.getDurationInMillis());
            }
        } catch (IOException e) {
            logger.error("Exception from : " + serviceUrl + ", method : " + method.getParams().getParameter("method") + ", exception :", e);
        } finally {
            method.releaseConnection();
        }

        return result;
    }

    private CloseableHttpClient getHttpClient() {
        if (s_client == null) {
            SSLContext sslContext = null;
            try {
                sslContext = Link.initManagementSSLContext(caService);
            } catch (GeneralSecurityException | IOException e) {
                throw new RuntimeException(e);
            }

            int timeout = ClusterServiceAdapter.ClusterMessageTimeOut.value() * 1000;
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeout)
                    .setConnectionRequestTimeout(timeout)
                    .setSocketTimeout(timeout).build();

            s_client = HttpClientBuilder.create()
                    .setDefaultRequestConfig(config)
                    .setSSLContext(sslContext)
                    .build();
        }
        return s_client;
    }

}
