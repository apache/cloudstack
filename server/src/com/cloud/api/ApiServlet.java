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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.managed.context.ManagedContext;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.EntityManager;

@Component("apiServlet")
@SuppressWarnings("serial")
public class ApiServlet extends HttpServlet {
    public static final Logger s_logger = Logger.getLogger(ApiServlet.class.getName());
    private static final Logger s_accessLogger = Logger.getLogger("apiserver." + ApiServer.class.getName());

    @Inject
    ApiServerService _apiServer;
    @Inject
    AccountService _accountMgr;
    @Inject
    EntityManager _entityMgr;
    @Inject
    ManagedContext _managedContext;

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
        final StringBuilder auditTrailSb = new StringBuilder(128);
        auditTrailSb.append(" ").append(req.getRemoteAddr());
        auditTrailSb.append(" -- ").append(req.getMethod()).append(' ');
        // get the response format since we'll need it in a couple of places
        String responseType = BaseCmd.RESPONSE_TYPE_XML;
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
            HttpSession session = req.getSession(false);
            final Object[] responseTypeParam = params.get(ApiConstants.RESPONSE);
            if (responseTypeParam != null) {
                responseType = (String)responseTypeParam[0];
            }

            final Object[] commandObj = params.get(ApiConstants.COMMAND);
            if (commandObj != null) {
                final String command = (String)commandObj[0];
                if ("logout".equalsIgnoreCase(command)) {
                    // if this is just a logout, invalidate the session and return
                    if (session != null) {
                        final Long userId = (Long)session.getAttribute("userid");
                        final Account account = (Account)session.getAttribute("accountobj");
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
                        } catch (final IllegalStateException ise) {
                        }
                    }
                    auditTrailSb.append("command=logout");
                    auditTrailSb.append(" ").append(HttpServletResponse.SC_OK);
                    writeResponse(resp, getLogoutSuccessResponse(responseType), HttpServletResponse.SC_OK, responseType);
                    return;
                } else if ("login".equalsIgnoreCase(command)) {
                    auditTrailSb.append("command=login");
                    // if this is a login, authenticate the user and return
                    if (session != null) {
                        try {
                            session.invalidate();
                        } catch (final IllegalStateException ise) {
                        }
                    }
                    session = req.getSession(true);
                    final String[] username = (String[])params.get(ApiConstants.USERNAME);
                    final String[] password = (String[])params.get(ApiConstants.PASSWORD);
                    String[] domainIdArr = (String[])params.get(ApiConstants.DOMAIN_ID);

                    if (domainIdArr == null) {
                        domainIdArr = (String[])params.get(ApiConstants.DOMAIN__ID);
                    }
                    final String[] domainName = (String[])params.get(ApiConstants.DOMAIN);
                    Long domainId = null;
                    if ((domainIdArr != null) && (domainIdArr.length > 0)) {
                        try {
                            //check if UUID is passed in for domain
                            domainId = _apiServer.fetchDomainId(domainIdArr[0]);
                            if (domainId == null) {
                                domainId = Long.parseLong(domainIdArr[0]);
                            }
                            auditTrailSb.append(" domainid=" + domainId);// building the params for POST call
                        } catch (final NumberFormatException e) {
                            s_logger.warn("Invalid domain id entered by user");
                            auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "Invalid domain id entered, please enter a valid one");
                            final String serializedResponse =
                                _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid domain id entered, please enter a valid one", params,
                                    responseType);
                            writeResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType);
                        }
                    }
                    String domain = null;
                    if (domainName != null) {
                        domain = domainName[0];
                        auditTrailSb.append(" domain=" + domain);
                        if (domain != null) {
                            // ensure domain starts with '/' and ends with '/'
                            if (!domain.endsWith("/")) {
                                domain += '/';
                            }
                            if (!domain.startsWith("/")) {
                                domain = "/" + domain;
                            }
                        }
                    }

                    if (username != null) {
                        final String pwd = ((password == null) ? null : password[0]);
                        try {
                            _apiServer.loginUser(session, username[0], pwd, domainId, domain, req.getRemoteAddr(), params);
                            auditTrailSb.insert(0, "(userId=" + session.getAttribute("userid") + " accountId=" + ((Account)session.getAttribute("accountobj")).getId() +
                                " sessionId=" + session.getId() + ")");
                            final String loginResponse = getLoginSuccessResponse(session, responseType);
                            writeResponse(resp, loginResponse, HttpServletResponse.SC_OK, responseType);
                            return;
                        } catch (final CloudAuthenticationException ex) {
                            // TODO: fall through to API key, or just fail here w/ auth error? (HTTP 401)
                            try {
                                session.invalidate();
                            } catch (final IllegalStateException ise) {
                            }

                            auditTrailSb.append(" " + ApiErrorCode.ACCOUNT_ERROR + " " + ex.getMessage() != null ? ex.getMessage()
                                : "failed to authenticate user, check if username/password are correct");
                            final String serializedResponse =
                                _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(), ex.getMessage() != null ? ex.getMessage()
                                    : "failed to authenticate user, check if username/password are correct", params, responseType);
                            writeResponse(resp, serializedResponse, ApiErrorCode.ACCOUNT_ERROR.getHttpCode(), responseType);
                            return;
                        }
                    }
                }
            }
            auditTrailSb.append(req.getQueryString());
            final boolean isNew = ((session == null) ? true : session.isNew());

            // Initialize an empty context and we will update it after we have verified the request below,
            // we no longer rely on web-session here, verifyRequest will populate user/account information
            // if a API key exists
            Long userId = null;

            if (!isNew) {
                userId = (Long)session.getAttribute("userid");
                final String account = (String)session.getAttribute("account");
                final Object accountObj = session.getAttribute("accountobj");
                final String sessionKey = (String)session.getAttribute("sessionkey");
                final String[] sessionKeyParam = (String[])params.get(ApiConstants.SESSIONKEY);
                if ((sessionKeyParam == null) || (sessionKey == null) || !sessionKey.equals(sessionKeyParam[0])) {
                    try {
                        session.invalidate();
                    } catch (final IllegalStateException ise) {
                    }
                    auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials");
                    final String serializedResponse =
                        _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials", params, responseType);
                    writeResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType);
                    return;
                }

                // Do a sanity check here to make sure the user hasn't already been deleted
                if ((userId != null) && (account != null) && (accountObj != null) && _apiServer.verifyUser(userId)) {
                    final String[] command = (String[])params.get(ApiConstants.COMMAND);
                    if (command == null) {
                        s_logger.info("missing command, ignoring request...");
                        auditTrailSb.append(" " + HttpServletResponse.SC_BAD_REQUEST + " " + "no command specified");
                        final String serializedResponse = _apiServer.getSerializedApiError(HttpServletResponse.SC_BAD_REQUEST, "no command specified", params, responseType);
                        writeResponse(resp, serializedResponse, HttpServletResponse.SC_BAD_REQUEST, responseType);
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
                    writeResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType);
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
                writeResponse(resp, response != null ? response : "", HttpServletResponse.SC_OK, responseType);
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
                writeResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType);

            }
        } catch (final ServerApiException se) {
            final String serializedResponseText = _apiServer.getSerializedApiError(se, params, responseType);
            resp.setHeader("X-Description", se.getDescription());
            writeResponse(resp, serializedResponseText, se.getErrorCode().getHttpCode(), responseType);
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

    // FIXME: rather than isError, we might was to pass in the status code to give more flexibility
    private void writeResponse(final HttpServletResponse resp, final String response, final int responseCode, final String responseType) {
        try {
            if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                resp.setContentType(ApiServer.getJsonContentType() + "; charset=UTF-8");
            } else {
                resp.setContentType("text/xml; charset=UTF-8");
            }

            resp.setStatus(responseCode);
            resp.getWriter().print(response);
        } catch (final IOException ioex) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("exception writing response: " + ioex);
            }
        } catch (final Exception ex) {
            if (!(ex instanceof IllegalStateException)) {
                s_logger.error("unknown exception writing api response", ex);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    String getLoginSuccessResponse(final HttpSession session, final String responseType) {
        final StringBuilder sb = new StringBuilder();
        final int inactiveInterval = session.getMaxInactiveInterval();

        final String user_UUID = (String)session.getAttribute("user_UUID");
        session.removeAttribute("user_UUID");

        final String domain_UUID = (String)session.getAttribute("domain_UUID");
        session.removeAttribute("domain_UUID");

        if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            sb.append("{ \"loginresponse\" : { ");
            final Enumeration attrNames = session.getAttributeNames();
            if (attrNames != null) {
                sb.append("\"timeout\" : \"").append(inactiveInterval).append("\"");
                while (attrNames.hasMoreElements()) {
                    final String attrName = (String)attrNames.nextElement();
                    if ("userid".equalsIgnoreCase(attrName)) {
                        sb.append(", \"" + attrName + "\" : \"" + user_UUID + "\"");
                    } else if ("domainid".equalsIgnoreCase(attrName)) {
                        sb.append(", \"" + attrName + "\" : \"" + domain_UUID + "\"");
                    } else {
                        final Object attrObj = session.getAttribute(attrName);
                        if ((attrObj instanceof String) || (attrObj instanceof Long)) {
                            sb.append(", \"" + attrName + "\" : \"" + attrObj.toString() + "\"");
                        }
                    }
                }
            }
            sb.append(" } }");
        } else {
            sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            sb.append("<loginresponse cloud-stack-version=\"").append(ApiDBUtils.getVersion()).append("\">");
            sb.append("<timeout>").append(inactiveInterval).append("</timeout>");
            final Enumeration attrNames = session.getAttributeNames();
            if (attrNames != null) {
                while (attrNames.hasMoreElements()) {
                    final String attrName = (String)attrNames.nextElement();
                    if (ApiConstants.USER_ID.equalsIgnoreCase(attrName)) {
                        sb.append("<").append(attrName).append(">")
                                .append(user_UUID).append("</")
                                .append(attrName).append(">");
                    } else if ("domainid".equalsIgnoreCase(attrName)) {
                        sb.append("<").append(attrName).append(">")
                                .append(domain_UUID).append("</")
                                .append(attrName).append(">");
                    } else {
                        final Object attrObj = session.getAttribute(attrName);
                        if (attrObj instanceof String || attrObj instanceof Long || attrObj instanceof Short) {
                            sb.append("<").append(attrName).append(">")
                                    .append(attrObj.toString()).append("</")
                                    .append(attrName).append(">");
                        }
                    }
                }
            }

            sb.append("</loginresponse>");
        }
        return sb.toString();
    }

    private String getLogoutSuccessResponse(final String responseType) {
        final StringBuilder sb = new StringBuilder();
        if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            sb.append("{ \"logoutresponse\" : { \"description\" : \"success\" } }");
        } else {
            sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            sb.append("<logoutresponse cloud-stack-version=\"").append(ApiDBUtils.getVersion()).append("\">");
            sb.append("<description>success</description>");
            sb.append("</logoutresponse>");
        }
        return sb.toString();
    }

}
