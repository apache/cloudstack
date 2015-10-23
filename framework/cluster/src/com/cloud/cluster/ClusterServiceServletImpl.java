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
import java.rmi.RemoteException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.log4j.Logger;

import com.cloud.utils.Profiler;

public class ClusterServiceServletImpl implements ClusterService {
    private static final long serialVersionUID = 4574025200012566153L;
    private static final Logger s_logger = Logger.getLogger(ClusterServiceServletImpl.class);

    private String _serviceUrl;

    protected static HttpClient s_client = null;

    public ClusterServiceServletImpl() {
    }

    public ClusterServiceServletImpl(final String serviceUrl) {
        s_logger.info("Setup cluster service servlet. service url: " + serviceUrl + ", request timeout: " + ClusterServiceAdapter.ClusterMessageTimeOut.value() +
                " seconds");

        _serviceUrl = serviceUrl;
    }

    @Override
    public String execute(final ClusterServicePdu pdu) throws RemoteException {

        final HttpClient client = getHttpClient();
        final PostMethod method = new PostMethod(_serviceUrl);

        method.addParameter("method", Integer.toString(RemoteMethodConstants.METHOD_DELIVER_PDU));
        method.addParameter("sourcePeer", pdu.getSourcePeer());
        method.addParameter("destPeer", pdu.getDestPeer());
        method.addParameter("pduSeq", Long.toString(pdu.getSequenceId()));
        method.addParameter("pduAckSeq", Long.toString(pdu.getAckSequenceId()));
        method.addParameter("agentId", Long.toString(pdu.getAgentId()));
        method.addParameter("gsonPackage", pdu.getJsonPackage());
        method.addParameter("stopOnError", pdu.isStopOnError() ? "1" : "0");
        method.addParameter("pduType", Integer.toString(pdu.getPduType()));

        return executePostMethod(client, method);
    }

    @Override
    public boolean ping(final String callingPeer) throws RemoteException {
        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Ping at " + _serviceUrl);
        }

        final HttpClient client = getHttpClient();
        final PostMethod method = new PostMethod(_serviceUrl);

        method.addParameter("method", Integer.toString(RemoteMethodConstants.METHOD_PING));
        method.addParameter("callingPeer", callingPeer);

        final String returnVal = executePostMethod(client, method);
        if ("true".equalsIgnoreCase(returnVal)) {
            return true;
        }
        return false;
    }

    private String executePostMethod(final HttpClient client, final PostMethod method) {
        int response = 0;
        String result = null;
        try {
            final Profiler profiler = new Profiler();
            profiler.start();
            response = client.executeMethod(method);
            if (response == HttpStatus.SC_OK) {
                result = method.getResponseBodyAsString();
                profiler.stop();
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("POST " + _serviceUrl + " response :" + result + ", responding time: " + profiler.getDurationInMillis() + " ms");
                }
            } else {
                profiler.stop();
                s_logger.error("Invalid response code : " + response + ", from : " + _serviceUrl + ", method : " + method.getParameter("method") + " responding time: " +
                        profiler.getDurationInMillis());
            }
        } catch (final HttpException e) {
            s_logger.error("HttpException from : " + _serviceUrl + ", method : " + method.getParameter("method"));
        } catch (final IOException e) {
            s_logger.error("IOException from : " + _serviceUrl + ", method : " + method.getParameter("method"));
        } catch (final Throwable e) {
            s_logger.error("Exception from : " + _serviceUrl + ", method : " + method.getParameter("method") + ", exception :", e);
        } finally {
            method.releaseConnection();
        }

        return result;
    }

    private HttpClient getHttpClient() {

        if (s_client == null) {
            final MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
            mgr.getParams().setDefaultMaxConnectionsPerHost(4);

            // TODO make it configurable
            mgr.getParams().setMaxTotalConnections(1000);

            s_client = new HttpClient(mgr);
            final HttpClientParams clientParams = new HttpClientParams();
            clientParams.setSoTimeout(ClusterServiceAdapter.ClusterMessageTimeOut.value() * 1000);

            s_client.setParams(clientParams);
        }
        return s_client;
    }

}
