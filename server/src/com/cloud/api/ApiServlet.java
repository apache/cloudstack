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
package com.cloud.api;

import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.HttpUtils;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.NetUtils;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationManager;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.managed.context.ManagedContext;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("apiServlet")
@SuppressWarnings("serial")
public class ApiServlet extends HttpServlet {
    public static final Logger s_logger = Logger.getLogger(ApiServlet.class.getName());
    private static final Logger s_accessLogger = Logger.getLogger("apiserver." + ApiServer.class.getName());
    private final static List<String> s_clientAddressHeaders = Collections
            .unmodifiableList(Arrays.asList("X-Forwarded-For",
                    "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "Remote_Addr"));

    @Inject
    ApiServerService _apiServer;
    @Inject
    AccountService _accountMgr;
    @Inject
    EntityManager _entityMgr;
    @Inject
    ManagedContext _managedContext;
    @Inject
    APIAuthenticationManager _authManager;

    public ApiServlet() {
    }

    @Override
    public void init(final ServletConfig config) throws ServletException {
        SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());
    }

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) {
        processRequest(req, resp);
    }

    void utf8Fixup(final HttpServletRequest req, final Map<String, Object[]> params) {
        if (req.getQueryString() == null) {
            return;
        }

        final String[] paramsInQueryString = req.getQueryString().split("&");
        if (paramsInQueryString != null) {
            for (final String param : paramsInQueryString) {
                final String[] paramTokens = param.split("=", 2);
                if (paramTokens.length == 2) {
                    String name = decodeUtf8(paramTokens[0]);
                    String value = decodeUtf8(paramTokens[1]);
                    params.put(name, new String[] {value});
                } else {
                    s_logger.debug("Invalid parameter in URL found. param: " + param);
                }
            }
        }
    }

    private String decodeUtf8(final String value) {
        try {
            return URLDecoder.decode(value, "UTF-8");
        } catch (final UnsupportedEncodingException e) {
            //should never happen
            return null;
        }
    }

    private void processRequest(final HttpServletRequest req, final HttpServletResponse resp) {
        _managedContext.runWithContext(new Runnable() {
            @Override
            public void run() {
                processRequestInContext(req, resp);
            }
        });
    }

    void processRequestInContext(final HttpServletRequest req, final HttpServletResponse resp) {
        final String remoteAddress = getClientAddress(req);
        final StringBuilder auditTrailSb = new StringBuilder(128);
        auditTrailSb.append(" ").append(remoteAddress);
        auditTrailSb.append(" -- ").append(req.getMethod()).append(' ');
        // get the response format since we'll need it in a couple of places
        String responseType = HttpUtils.RESPONSE_TYPE_XML;
        final Map<String, Object[]> params = new HashMap<String, Object[]>();
        params.putAll(req.getParameterMap());

        // For HTTP GET requests, it seems that HttpServletRequest.getParameterMap() actually tries
        // to unwrap URL encoded content from ISO-9959-1.
        // After failed in using setCharacterEncoding() to control it, end up with following hacking:
        // for all GET requests, we will override it with our-own way of UTF-8 based URL decoding.
        utf8Fixup(req, params);

        // logging the request start and end in management log for easy debugging
        String reqStr = "";
        if (s_logger.isDebugEnabled()) {
            reqStr = auditTrailSb.toString() + " " + StringUtils.cleanString(req.getQueryString());
            s_logger.debug("===START=== " + reqStr);
        }

        try {

            if (HttpUtils.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                resp.setContentType(ApiServer.getJSONContentType());
            } else if (HttpUtils.RESPONSE_TYPE_XML.equalsIgnoreCase(responseType)){
                resp.setContentType(HttpUtils.XML_CONTENT_TYPE);
            }

            HttpSession session = req.getSession(false);
            final Object[] responseTypeParam = params.get(ApiConstants.RESPONSE);
            if (responseTypeParam != null) {
                responseType = (String)responseTypeParam[0];
            }

            final Object[] commandObj = params.get(ApiConstants.COMMAND);
            if (commandObj != null) {
                final String command = (String) commandObj[0];

                APIAuthenticator apiAuthenticator = _authManager.getAPIAuthenticator(command);
                if (apiAuthenticator != null) {
                    auditTrailSb.append("command=");
                    auditTrailSb.append(command);

                    int httpResponseCode = HttpServletResponse.SC_OK;
                    String responseString = null;

                    if (apiAuthenticator.getAPIType() == APIAuthenticationType.LOGIN_API) {
                        if (session != null) {
                            try {
                                session.invalidate();
                            } catch (final IllegalStateException ise) {
                            }
                        }
                        session = req.getSession(true);
                        if (ApiServer.isSecureSessionCookieEnabled()) {
                            resp.setHeader("SET-COOKIE", String.format("JSESSIONID=%s;Secure;HttpOnly;Path=/client", session.getId()));
                            if (s_logger.isDebugEnabled()) {
                                if (s_logger.isDebugEnabled()) {
                                    s_logger.debug("Session cookie is marked secure!");
                                }
                            }
                        }
                    }

                    try {
                        responseString = apiAuthenticator.authenticate(command, params, session, InetAddress.getByName(remoteAddress), responseType, auditTrailSb, req, resp);
                        if (session != null && session.getAttribute(ApiConstants.SESSIONKEY) != null) {
                            resp.addHeader("SET-COOKIE", String.format("%s=%s;HttpOnly", ApiConstants.SESSIONKEY, session.getAttribute(ApiConstants.SESSIONKEY)));
                        }
                    } catch (ServerApiException e) {
                        httpResponseCode = e.getErrorCode().getHttpCode();
                        responseString = e.getMessage();
                        s_logger.debug("Authentication failure: " + e.getMessage());
                    }

                    if (apiAuthenticator.getAPIType() == APIAuthenticationType.LOGOUT_API) {
                        if (session != null) {
                            final Long userId = (Long) session.getAttribute("userid");
                            final Account account = (Account) session.getAttribute("accountobj");
                            Long accountId = null;
                            if (account != null) {
                                accountId = account.getId();
                            }
                            auditTrailSb.insert(0, "(userId=" + userId + " accountId=" + accountId + " sessionId=" + session.getId() + ")");
                            if (userId != null) {
                                _apiServer.logoutUser(userId);
                            }
                            try {
                                session.invalidate();
                            } catch (final IllegalStateException ignored) {
                            }
                        }
                        Cookie sessionKeyCookie = new Cookie(ApiConstants.SESSIONKEY, "");
                        sessionKeyCookie.setMaxAge(0);
                        resp.addCookie(sessionKeyCookie);
                    }
                    HttpUtils.writeHttpResponse(resp, responseString, httpResponseCode, responseType, ApiServer.getJSONContentType());
                    return;
                }
            }

            auditTrailSb.append(StringUtils.cleanString(req.getQueryString()));
            final boolean isNew = ((session == null) ? true : session.isNew());

            // Initialize an empty context and we will update it after we have verified the request below,
            // we no longer rely on web-session here, verifyRequest will populate user/account information
            // if a API key exists
            Long userId = null;

            if (!isNew) {
                userId = (Long)session.getAttribute("userid");
                final String account = (String) session.getAttribute("account");
                final Object accountObj = session.getAttribute("accountobj");
                if (!HttpUtils.validateSessionKey(session, params, req.getCookies(), ApiConstants.SESSIONKEY)) {
                    try {
                        session.invalidate();
                    } catch (final IllegalStateException ise) {
                    }
                    auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials");
                    final String serializedResponse =
                        _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials", params, responseType);
                    HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType, ApiServer.getJSONContentType());
                    return;
                }

                // Do a sanity check here to make sure the user hasn't already been deleted
                if ((userId != null) && (account != null) && (accountObj != null) && _apiServer.verifyUser(userId)) {
                    final String[] command = (String[])params.get(ApiConstants.COMMAND);
                    if (command == null) {
                        s_logger.info("missing command, ignoring request...");
                        auditTrailSb.append(" " + HttpServletResponse.SC_BAD_REQUEST + " " + "no command specified");
                        final String serializedResponse = _apiServer.getSerializedApiError(HttpServletResponse.SC_BAD_REQUEST, "no command specified", params, responseType);
                        HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_BAD_REQUEST, responseType, ApiServer.getJSONContentType());
                        return;
                    }
                    final User user = _entityMgr.findById(User.class, userId);
                    CallContext.register(user, (Account)accountObj);
                } else {
                    // Invalidate the session to ensure we won't allow a request across management server
                    // restarts if the userId was serialized to the stored session
                    try {
                        session.invalidate();
                    } catch (final IllegalStateException ise) {
                    }

                    auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials");
                    final String serializedResponse =
                        _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials", params, responseType);
                    HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType, ApiServer.getJSONContentType());
                    return;
                }
            } else {
                CallContext.register(_accountMgr.getSystemUser(), _accountMgr.getSystemAccount());
            }

            if (_apiServer.verifyRequest(params, userId)) {
                auditTrailSb.insert(0, "(userId=" + CallContext.current().getCallingUserId() + " accountId=" + CallContext.current().getCallingAccount().getId() +
                    " sessionId=" + (session != null ? session.getId() : null) + ")");

                // Add the HTTP method (GET/POST/PUT/DELETE) as well into the params map.
                params.put("httpmethod", new String[] {req.getMethod()});
                final String response = _apiServer.handleRequest(params, responseType, auditTrailSb);
                HttpUtils.writeHttpResponse(resp, response != null ? response : "", HttpServletResponse.SC_OK, responseType, ApiServer.getJSONContentType());
            } else {
                if (session != null) {
                    try {
                        session.invalidate();
                    } catch (final IllegalStateException ise) {
                    }
                }

                auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials and/or request signature");
                final String serializedResponse =
                    _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials and/or request signature", params,
                        responseType);
                HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType, ApiServer.getJSONContentType());

            }
        } catch (final ServerApiException se) {
            final String serializedResponseText = _apiServer.getSerializedApiError(se, params, responseType);
            resp.setHeader("X-Description", se.getDescription());
            HttpUtils.writeHttpResponse(resp, serializedResponseText, se.getErrorCode().getHttpCode(), responseType, ApiServer.getJSONContentType());
            auditTrailSb.append(" " + se.getErrorCode() + " " + se.getDescription());
        } catch (final Exception ex) {
            s_logger.error("unknown exception writing api response", ex);
            auditTrailSb.append(" unknown exception writing api response");
        } finally {
            s_accessLogger.info(auditTrailSb.toString());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("===END=== " + reqStr);
            }
            // cleanup user context to prevent from being peeked in other request context
            CallContext.unregister();
        }
    }

    //This method will try to get login IP of user even if servlet is behind reverseProxy or loadBalancer
    static String getClientAddress(final HttpServletRequest request) {
        for(final String header : s_clientAddressHeaders) {
            final String ip = getCorrectIPAddress(request.getHeader(header));
            if (ip != null) {
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    private static String getCorrectIPAddress(String ip) {
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        if(NetUtils.isValidIp(ip) || NetUtils.isValidIpv6(ip)) {
            return ip;
        }
        //it could be possible to have multiple IPs in HTTP header, this happens if there are multiple proxy in between
        //the client and the servlet, so parse the client IP
        String[] ips = ip.split(",");
        for(String i : ips) {
            if(NetUtils.isValidIp(i.trim()) || NetUtils.isValidIpv6(i.trim())) {
                return i.trim();
            }
        }
        return null;
    }
}
