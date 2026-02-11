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

package org.apache.cloudstack.veeam;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cloudstack.veeam.utils.Negotiation;

import com.cloud.utils.component.Adapter;

public interface RouteHandler extends Adapter {
    default int priority() { return 0; }
    boolean canHandle(String method, String path) throws IOException;
    void handle(HttpServletRequest req, HttpServletResponse resp, String path, Negotiation.OutFormat outFormat, VeeamControlServlet io)
            throws IOException;

    default String getSanitizedPath(String path) {
        // remove query params if exists
        int qIdx = path.indexOf('?');
        if (qIdx != -1) {
            return path.substring(0, qIdx);
        }
        return path;
    }

    static String getRequestData(HttpServletRequest req) {
        String contentType = req.getContentType();
        if (contentType == null) {
            return null;
        }
        String mime = contentType.split(";")[0].trim().toLowerCase();
        if (!"application/json".equals(mime) && !"application/x-www-form-urlencoded".equals(mime)) {
            return null;
        }
        try {
            StringBuilder data = new StringBuilder();
            String line;
            try (BufferedReader reader = req.getReader()) {
                while ((line = reader.readLine()) != null) {
                    data.append(line);
                }
            }
            return data.toString();
        } catch (IOException ignored) {
            return null;
        }
    }

    static Map<String, String> getRequestParams(HttpServletRequest req) {
        return req.getParameterMap().entrySet().stream()
                .filter(e -> e.getValue() != null && e.getValue().length > 0)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, e -> e.getValue()[0]));
    }
}
