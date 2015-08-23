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

package com.cloud.utils;

import org.apache.log4j.Logger;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Map;

public class HttpUtils {

    public static final Logger s_logger = Logger.getLogger(HttpUtils.class);

    public static final String UTF_8 = "UTF-8";
    public static final String RESPONSE_TYPE_JSON = "json";
    public static final String RESPONSE_TYPE_XML = "xml";
    public static final String JSON_CONTENT_TYPE = "application/json; charset=UTF-8";
    public static final String XML_CONTENT_TYPE = "text/xml; charset=UTF-8";

    public static void addSecurityHeaders(final HttpServletResponse resp) {
        if (resp.containsHeader("X-Content-Type-Options")) {
            resp.setHeader("X-Content-Type-Options", "nosniff");
        }
        else {
            resp.addHeader("X-Content-Type-Options", "nosniff");
        }
        if (resp.containsHeader("X-XSS-Protection")) {
            resp.setHeader("X-XSS-Protection", "1;mode=block");
        }
        else {
            resp.addHeader("X-XSS-Protection", "1;mode=block");
        }
    }

    public static void writeHttpResponse(final HttpServletResponse resp, final String response,
                                         final Integer responseCode, final String responseType, final String jsonContentType) {
        try {
            if (RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                if (jsonContentType != null && !jsonContentType.isEmpty()) {
                    resp.setContentType(jsonContentType);
                } else {
                    resp.setContentType(JSON_CONTENT_TYPE);
                }
            } else if (RESPONSE_TYPE_XML.equalsIgnoreCase(responseType)){
                resp.setContentType(XML_CONTENT_TYPE);
            }
            if (responseCode != null) {
                resp.setStatus(responseCode);
            }
            addSecurityHeaders(resp);
            resp.getWriter().print(response);
        } catch (final IOException ioex) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("Exception writing http response: " + ioex);
            }
        } catch (final Exception ex) {
            if (!(ex instanceof IllegalStateException)) {
                s_logger.error("Unknown exception writing http response", ex);
            }
        }
    }

    public static String findCookie(final Cookie[] cookies, final String key) {
        if (cookies == null || key == null || key.isEmpty()) {
            return null;
        }
        for (Cookie cookie: cookies) {
            if (cookie != null && cookie.getName().equals(key)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public static boolean validateSessionKey(final HttpSession session, final Map<String, Object[]> params, final Cookie[] cookies, final String sessionKeyString) {
        if (session == null || sessionKeyString == null) {
            return false;
        }
        final String sessionKey = (String) session.getAttribute(sessionKeyString);
        final String sessionKeyFromCookie = HttpUtils.findCookie(cookies, sessionKeyString);
        String[] sessionKeyFromParams = null;
        if (params != null) {
            sessionKeyFromParams = (String[]) params.get(sessionKeyString);
        }
        if ((sessionKey == null)
                || (sessionKeyFromParams == null && sessionKeyFromCookie == null)
                || (sessionKeyFromParams != null && !sessionKey.equals(sessionKeyFromParams[0]))
                || (sessionKeyFromCookie != null && !sessionKey.equals(sessionKeyFromCookie))) {
            return false;
        }
        return true;
    }

}
