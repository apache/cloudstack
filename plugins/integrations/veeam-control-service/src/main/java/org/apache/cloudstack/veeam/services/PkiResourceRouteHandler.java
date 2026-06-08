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

package org.apache.cloudstack.veeam.services;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.ca.CAManager;
import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.commons.lang3.StringUtils;

import com.cloud.utils.component.ManagerBase;

public class PkiResourceRouteHandler extends ManagerBase implements RouteHandler {
    private static final String BASE_ROUTE = "/services/pki-resource";
    private static final String RESOURCE_KEY = "resource";
    private static final String RESOURCE_VALUE = "ca-certificate";
    private static final String FORMAT_KEY = "format";
    private static final String FORMAT_VALUE = "X509-PEM-CA";
    private static final Charset OUTPUT_CHARSET = StandardCharsets.ISO_8859_1;

    @Inject
    CAManager caManager;

    @Override
    public boolean canHandle(String method, String path) {
        return getSanitizedPath(path).startsWith(BASE_ROUTE);
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE) && "GET".equalsIgnoreCase(req.getMethod())) {
            handleGet(req, resp, outFormat, io);
            return;
        }

        io.notFound(resp, null, outFormat);
    }

    protected void handleGet(HttpServletRequest req, HttpServletResponse resp,
                 Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        try {
            final String resource = req.getParameter(RESOURCE_KEY);
            final String format = req.getParameter(FORMAT_KEY);

            if (StringUtils.isNotBlank(resource) && !RESOURCE_VALUE.equals(resource)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported resource");
                return;
            }

            if (StringUtils.isNotBlank(format) && !FORMAT_VALUE.equals(format)) {
                resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported format");
                return;
            }

            byte[] pemBytes = returnCACertificate();
            if (pemBytes.length == 0) {
                resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "No certificate data available");
                return;
            }
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setHeader("Cache-Control", "no-store");
            resp.setContentType("application/x-x509-ca-cert; charset=" + OUTPUT_CHARSET.name());
            resp.setHeader("Content-Disposition",
                    "attachment; filename=\"pki-resource.cer\"");
            resp.setContentLength(pemBytes.length);

            try (OutputStream os = resp.getOutputStream()) {
                os.write(pemBytes);
            }
        } catch (IOException e) {
            String msg = "Failed to retrieve server CA certificate";
            logger.error(msg, e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, msg);
        }
    }

    private byte[] returnCACertificate() throws IOException {
        String tlsCaCert = caManager.getCaCertificate(null);
        return tlsCaCert.getBytes(OUTPUT_CHARSET);
    }
}
