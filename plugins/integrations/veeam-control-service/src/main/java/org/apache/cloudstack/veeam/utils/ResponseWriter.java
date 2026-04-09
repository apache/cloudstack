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

package org.apache.cloudstack.veeam.utils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.api.dto.Fault;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ResponseWriter {
    private static final Logger LOGGER = LogManager.getLogger(ResponseWriter.class);

    private final Mapper mapper;

    public ResponseWriter(final Mapper mapper) {
        this.mapper = mapper;
    }

    public void write(final HttpServletResponse resp, final int status, final Object body, final Negotiation.OutFormat fmt)
            throws IOException {

        resp.setStatus(status);

        if (body == null) {
            resp.setContentLength(0);
            return;
        }

        final String payload;
        final String contentType;

        try {
            if (fmt == Negotiation.OutFormat.XML) {
                contentType = "application/xml";
                payload = mapper.toXml(body);
            } else {
                contentType = "application/json";
                payload = mapper.toJson(body);
            }
        } catch (Exception e) {
            // Last-resort fallback
            resp.setStatus(500);
            resp.setHeader("Content-Type", "text/plain");
            resp.getWriter().write("Internal Server Error");
            return;
        }

        LOGGER.info("Writing response: {}\n{}", status, payload);

        resp.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resp.setHeader("Content-Type", contentType);
        resp.getWriter().write(payload);
    }

    public void writeFault(final HttpServletResponse resp, final int status, final String reason, final String detail, final Negotiation.OutFormat fmt)
            throws IOException {
        Fault fault = new Fault(reason, detail);
        if (fmt == Negotiation.OutFormat.XML) {
            write(resp, status, fault, fmt);
        } else {
            write(resp, status, fault, fmt);
        }
    }
}
