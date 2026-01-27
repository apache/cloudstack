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

package org.apache.cloudstack.veeam.api;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.RouteHandler;
import org.apache.cloudstack.veeam.VeeamControlServlet;
import org.apache.cloudstack.veeam.adapter.UserResourceAdapter;
import org.apache.cloudstack.veeam.api.dto.ImageTransfer;
import org.apache.cloudstack.veeam.api.dto.ImageTransfers;
import org.apache.cloudstack.veeam.utils.Negotiation;
import org.apache.cloudstack.veeam.utils.PathUtil;

import com.cloud.exception.InvalidParameterValueException;
import com.cloud.utils.Pair;
import com.cloud.utils.component.ManagerBase;
import com.cloud.utils.exception.CloudRuntimeException;
import com.fasterxml.jackson.core.JsonProcessingException;

public class ImageTransfersRouteHandler extends ManagerBase implements RouteHandler {
    public static final String BASE_ROUTE = "/api/imagetransfers";

    @Inject
    UserResourceAdapter userResourceAdapter;

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public int priority() {
        return 5;
    }

    @Override
    public boolean canHandle(String method, String path) {
        return getSanitizedPath(path).startsWith(BASE_ROUTE);
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final String method = req.getMethod();
        final String sanitizedPath = getSanitizedPath(path);
        if (sanitizedPath.equals(BASE_ROUTE)) {
            if ("GET".equalsIgnoreCase(method)) {
                handleGet(req, resp, outFormat, io);
                return;
            }
            if ("POST".equalsIgnoreCase(method)) {
                handlePost(req, resp, outFormat, io);
                return;
            }
        }

        if (!"GET".equalsIgnoreCase(method)) {
            io.methodNotAllowed(resp, "GET", outFormat);
            return;
        }
        Pair<String, String> idAndSubPath = PathUtil.extractIdAndSubPath(sanitizedPath, BASE_ROUTE);
        if (idAndSubPath != null) {
            // /api/imagetransfers/{id}
            if (idAndSubPath.first() != null) {
                if (idAndSubPath.second() == null) {
                    handleGetById(idAndSubPath.first(), resp, outFormat, io);
                    return;
                }
            }
        }

        resp.sendError(HttpServletResponse.SC_NOT_FOUND, "Not found");
    }

    public void handleGet(final HttpServletRequest req, final HttpServletResponse resp,
                          Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        final List<ImageTransfer> result = userResourceAdapter.listAllImageTransfers();
        final ImageTransfers response = new ImageTransfers();
        response.setImageTransfer(result);

        io.getWriter().write(resp, 400, response, outFormat);
    }

    public void handlePost(final HttpServletRequest req, final HttpServletResponse resp,
                           Negotiation.OutFormat outFormat, VeeamControlServlet io) throws IOException {
        String data = RouteHandler.getRequestData(req);
        logger.info("Received POST request on /api/imagetransfers endpoint, but method: POST is not supported atm. Request-data: {}", data);
        try {
            ImageTransfer request = io.getMapper().jsonMapper().readValue(data, ImageTransfer.class);
            ImageTransfer response = userResourceAdapter.handleCreateImageTransfer(request);
            io.getWriter().write(resp, 201, response, outFormat);
        } catch (JsonProcessingException | CloudRuntimeException e) {
            io.getWriter().write(resp, 400, e.getMessage(), outFormat);
        }
    }

    public void handleGetById(final String id, final HttpServletResponse resp, final Negotiation.OutFormat outFormat,
                              final VeeamControlServlet io) throws IOException {
        try {
            ImageTransfer response = userResourceAdapter.getImageTransfer(id);
            io.getWriter().write(resp, 200, response, outFormat);
        } catch (InvalidParameterValueException e) {
            io.getWriter().write(resp, 404, e.getMessage(), outFormat);
        }
    }
}
