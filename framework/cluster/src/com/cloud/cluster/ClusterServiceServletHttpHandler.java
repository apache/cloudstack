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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLDecoder;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

public class ClusterServiceServletHttpHandler implements HttpRequestHandler {
    private static final Logger s_logger = Logger.getLogger(ClusterServiceServletHttpHandler.class);

    private final ClusterManager manager;

    public ClusterServiceServletHttpHandler(ClusterManager manager) {
        this.manager = manager;
    }

    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext context) throws HttpException, IOException {

        try {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Start Handling cluster HTTP request");
            }

            parseRequest(request);
            handleRequest(request, response);

            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Handle cluster HTTP request done");
            }

        } catch (Throwable e) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Exception " + e.toString());
            }

            try {
                writeResponse(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, null);
            } catch (Throwable e2) {
                if (s_logger.isDebugEnabled()) {
                    s_logger.debug("Exception " + e2.toString());
                }
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void parseRequest(HttpRequest request) throws IOException {
        if (request instanceof HttpEntityEnclosingRequest) {
            HttpEntityEnclosingRequest entityRequest = (HttpEntityEnclosingRequest)request;

            String body = EntityUtils.toString(entityRequest.getEntity());
            if (body != null) {
                String[] paramArray = body.split("&");
                if (paramArray != null) {
                    for (String paramEntry : paramArray) {
                        String[] paramValue = paramEntry.split("=");
                        if (paramValue.length != 2) {
                            continue;
                        }

                        String name = URLDecoder.decode(paramValue[0]);
                        String value = URLDecoder.decode(paramValue[1]);

                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace("Parsed request parameter " + name + "=" + value);
                        }
                        request.getParams().setParameter(name, value);
                    }
                }
            }
        }
    }

    private void writeResponse(HttpResponse response, int statusCode, String content) {
        if (content == null) {
            content = "";
        }
        response.setStatusCode(statusCode);
        BasicHttpEntity body = new BasicHttpEntity();
        body.setContentType("text/html; charset=UTF-8");

        byte[] bodyData = content.getBytes();
        body.setContent(new ByteArrayInputStream(bodyData));
        body.setContentLength(bodyData.length);
        response.setEntity(body);
    }

    protected void handleRequest(HttpRequest req, HttpResponse response) {
        String method = (String)req.getParams().getParameter("method");

        int nMethod = RemoteMethodConstants.METHOD_UNKNOWN;
        String responseContent = null;
        try {
            if (method != null) {
                nMethod = Integer.parseInt(method);
            }

            switch (nMethod) {
                case RemoteMethodConstants.METHOD_DELIVER_PDU:
                    responseContent = handleDeliverPduMethodCall(req);
                    break;

                case RemoteMethodConstants.METHOD_PING:
                    responseContent = handlePingMethodCall(req);
                    break;

                case RemoteMethodConstants.METHOD_UNKNOWN:
                default:
                    assert (false);
                    s_logger.error("unrecognized method " + nMethod);
                    break;
            }
        } catch (Throwable e) {
            s_logger.error("Unexpected exception when processing cluster service request : ", e);
        }

        if (responseContent != null) {
            if (s_logger.isTraceEnabled())
                s_logger.trace("Write reponse with HTTP OK " + responseContent);

            writeResponse(response, HttpStatus.SC_OK, responseContent);
        } else {
            if (s_logger.isTraceEnabled())
                s_logger.trace("Write reponse with HTTP Bad request");

            writeResponse(response, HttpStatus.SC_BAD_REQUEST, null);
        }
    }

    private String handleDeliverPduMethodCall(HttpRequest req) {

        String pduSeq = (String)req.getParams().getParameter("pduSeq");
        String pduAckSeq = (String)req.getParams().getParameter("pduAckSeq");
        String sourcePeer = (String)req.getParams().getParameter("sourcePeer");
        String destPeer = (String)req.getParams().getParameter("destPeer");
        String agentId = (String)req.getParams().getParameter("agentId");
        String gsonPackage = (String)req.getParams().getParameter("gsonPackage");
        String stopOnError = (String)req.getParams().getParameter("stopOnError");
        String pduType = (String)req.getParams().getParameter("pduType");

        ClusterServicePdu pdu = new ClusterServicePdu();
        pdu.setSourcePeer(sourcePeer);
        pdu.setDestPeer(destPeer);
        pdu.setAgentId(Long.parseLong(agentId));
        pdu.setSequenceId(Long.parseLong(pduSeq));
        pdu.setAckSequenceId(Long.parseLong(pduAckSeq));
        pdu.setJsonPackage(gsonPackage);
        pdu.setStopOnError("1".equals(stopOnError));
        pdu.setPduType(Integer.parseInt(pduType));

        manager.OnReceiveClusterServicePdu(pdu);
        return "true";
    }

    private String handlePingMethodCall(HttpRequest req) {
        String callingPeer = (String)req.getParams().getParameter("callingPeer");

        if (s_logger.isDebugEnabled()) {
            s_logger.debug("Handle ping request from " + callingPeer);
        }

        return "true";
    }
}
