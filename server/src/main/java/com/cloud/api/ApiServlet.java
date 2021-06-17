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

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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

import com.cloud.projects.Project;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;

import com.cloud.utils.HttpUtils;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.NetUtils;

@Component("apiServlet")
@SuppressWarnings("serial")
public class ApiServlet extends HttpServlet {
    public static final Logger s_logger = Logger.getLogger(ApiServlet.class.getName());
    private static final Logger s_accessLogger = Logger.getLogger("apiserver." + ApiServer.class.getName());
    private final static List<String> s_clientAddressHeaders = Collections
            .unmodifiableList(Arrays.asList("X-Forwarded-For",
                    "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "Remote_Addr"));

    @Inject
    ApiServerService apiServer;
    @Inject
    AccountService accountMgr;
    @Inject
    EntityManager entityMgr;
    @Inject
    ManagedContext managedContext;
    @Inject
    APIAuthenticationManager authManager;
    @Inject
    private ProjectDao projectDao;

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
        managedContext.runWithContext(new Runnable() {
            @Override
            public void run() {
                processRequestInContext(req, resp);
            }
        });
    }

    void processRequestInContext(final HttpServletRequest req, final HttpServletResponse resp) {
        InetAddress remoteAddress = null;
        try {
            remoteAddress = getClientAddress(req);
        } catch (UnknownHostException e) {
            s_logger.warn("UnknownHostException when trying to lookup remote IP-Address. This should never happen. Blocking request.", e);
            final String response = apiServer.getSerializedApiError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "UnknownHostException when trying to lookup remote IP-Address", null,
                    HttpUtils.RESPONSE_TYPE_XML);
            HttpUtils.writeHttpResponse(resp, response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    HttpUtils.RESPONSE_TYPE_XML, ApiServer.JSONcontentType.value());
            return;
        }

        final StringBuilder auditTrailSb = new StringBuilder(128);
        auditTrailSb.append(" ").append(remoteAddress.getHostAddress());
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
        String cleanQueryString = StringUtils.cleanString(req.getQueryString());
        if (s_logger.isDebugEnabled()) {
            reqStr = auditTrailSb.toString() + " " + cleanQueryString;
            s_logger.debug("===START=== " + reqStr);
        }

        try {

            if (HttpUtils.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                resp.setContentType(ApiServer.JSONcontentType.value());
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

                APIAuthenticator apiAuthenticator = authManager.getAPIAuthenticator(command);
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

                        if (ApiServer.EnableSecureSessionCookie.value()) {
                            resp.setHeader("SET-COOKIE", String.format("JSESSIONID=%s;Secure;HttpOnly;Path=/client", session.getId()));
                            if (s_logger.isDebugEnabled()) {
                                s_logger.debug("Session cookie is marked secure!");
                            }
                        }
                    }

                    try {
                        responseString = apiAuthenticator.authenticate(command, params, session, remoteAddress, responseType, auditTrailSb, req, resp);
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
                                apiServer.logoutUser(userId);
                            }
                            try {
                                session.invalidate();
                            } catch (final IllegalStateException ignored) {
                            }
                        }
                        final Cookie[] cookies = req.getCookies();
                        if (cookies != null) {
                            for (final Cookie cookie : cookies) {
                                cookie.setValue("");
                                cookie.setMaxAge(0);
                                resp.addCookie(cookie);
                            }
                        }
                    }
                    HttpUtils.writeHttpResponse(resp, responseString, httpResponseCode, responseType, ApiServer.JSONcontentType.value());
                    return;
                }
            }

            auditTrailSb.append(cleanQueryString);
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
                            apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials", params, responseType);
                    HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType, ApiServer.JSONcontentType.value());
                    return;
                }

                // Do a sanity check here to make sure the user hasn't already been deleted
                if ((userId != null) && (account != null) && (accountObj != null) && apiServer.verifyUser(userId)) {
                    final String[] command = (String[])params.get(ApiConstants.COMMAND);
                    if (command == null) {
                        s_logger.info("missing command, ignoring request...");
                        auditTrailSb.append(" " + HttpServletResponse.SC_BAD_REQUEST + " " + "no command specified");
                        final String serializedResponse = apiServer.getSerializedApiError(HttpServletResponse.SC_BAD_REQUEST, "no command specified", params, responseType);
                        HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_BAD_REQUEST, responseType, ApiServer.JSONcontentType.value());
                        return;
                    }
                    final User user = entityMgr.findById(User.class, userId);
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
                            apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials", params, responseType);
                    HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType, ApiServer.JSONcontentType.value());
                    return;
                }
            } else {
                CallContext.register(accountMgr.getSystemUser(), accountMgr.getSystemAccount());
            }
            setProjectContext(params);
            if (apiServer.verifyRequest(params, userId, remoteAddress)) {
                auditTrailSb.insert(0, "(userId=" + CallContext.current().getCallingUserId() + " accountId=" + CallContext.current().getCallingAccount().getId() +
                        " sessionId=" + (session != null ? session.getId() : null) + ")");

                // Add the HTTP method (GET/POST/PUT/DELETE) as well into the params map.
                params.put("httpmethod", new String[]{req.getMethod()});
                setProjectContext(params);
                final String response = apiServer.handleRequest(params, responseType, auditTrailSb);
                HttpUtils.writeHttpResponse(resp, response != null ? response : "", HttpServletResponse.SC_OK, responseType, ApiServer.JSONcontentType.value());
            } else {
                if (session != null) {
                    try {
                        session.invalidate();
                    } catch (final IllegalStateException ise) {
                    }
                }

                auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials and/or request signature");
                final String serializedResponse =
                        apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials and/or request signature", params,
                                responseType);
                HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType, ApiServer.JSONcontentType.value());

            }
        } catch (final ServerApiException se) {
            final String serializedResponseText = apiServer.getSerializedApiError(se, params, responseType);
            resp.setHeader("X-Description", se.getDescription());
            HttpUtils.writeHttpResponse(resp, serializedResponseText, se.getErrorCode().getHttpCode(), responseType, ApiServer.JSONcontentType.value());
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

    private void setProjectContext(Map<String, Object[]> requestParameters) {
        final String[] command = (String[])requestParameters.get(ApiConstants.COMMAND);
        if (command == null) {
            s_logger.info("missing command, ignoring request...");
            return;
        }

        final String commandName = command[0];
        CallContext.current().setApiName(commandName);
        for (Map.Entry<String, Object[]> entry: requestParameters.entrySet()) {
            if (entry.getKey().equals(ApiConstants.PROJECT_ID) || isSpecificAPI(commandName)) {
                String projectId = null;
                if (isSpecificAPI(commandName)) {
                    projectId = String.valueOf(requestParameters.entrySet().stream()
                            .filter(e -> e.getKey().equals(ApiConstants.ID))
                            .map(Map.Entry::getValue).findFirst().get()[0]);
                } else {
                    projectId = String.valueOf(entry.getValue()[0]);
                }
                Project project = projectDao.findByUuid(projectId);
                if (project != null) {
                    CallContext.current().setProject(project);
                }
            }
        }
    }

    private boolean isSpecificAPI(String commandName) {
        List<String> commands = Arrays.asList("suspendProject", "updateProject", "activateProject", "deleteProject");
        if (commands.contains(commandName)) {
            return true;
        }
        return false;
    }

    //This method will try to get login IP of user even if servlet is behind reverseProxy or loadBalancer
    public static InetAddress getClientAddress(final HttpServletRequest request) throws UnknownHostException {
        for(final String header : s_clientAddressHeaders) {
            final String ip = getCorrectIPAddress(request.getHeader(header));
            if (ip != null) {
                return InetAddress.getByName(ip);
            }
        }

        return InetAddress.getByName(request.getRemoteAddr());
    }

    private static String getCorrectIPAddress(String ip) {
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            return null;
        }
        if(NetUtils.isValidIp4(ip) || NetUtils.isValidIp6(ip)) {
            return ip;
        }
        //it could be possible to have multiple IPs in HTTP header, this happens if there are multiple proxy in between
        //the client and the servlet, so parse the client IP
        String[] ips = ip.split(",");
        for(String i : ips) {
            if(NetUtils.isValidIp4(i.trim()) || NetUtils.isValidIp6(i.trim())) {
                return i.trim();
            }
        }
        return null;
    }
}
