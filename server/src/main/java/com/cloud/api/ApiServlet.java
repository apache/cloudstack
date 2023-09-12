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
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationManager;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.command.user.consoleproxy.CreateConsoleEndpointCmd;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.managed.context.ManagedContext;
import org.apache.cloudstack.utils.consoleproxy.ConsoleAccessUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.cloud.api.auth.ListUserTwoFactorAuthenticatorProvidersCmd;
import com.cloud.api.auth.SetupUserTwoFactorAuthenticationCmd;
import com.cloud.api.auth.ValidateUserTwoFactorAuthenticationCodeCmd;
import com.cloud.projects.Project;
import com.cloud.projects.dao.ProjectDao;
import com.cloud.user.Account;
import com.cloud.user.AccountManagerImpl;
import com.cloud.user.AccountService;
import com.cloud.user.User;
import com.cloud.user.UserAccount;

import com.cloud.utils.HttpUtils;
import com.cloud.utils.StringUtils;
import com.cloud.utils.db.EntityManager;
import com.cloud.utils.net.NetUtils;

@Component("apiServlet")
@SuppressWarnings("serial")
public class ApiServlet extends HttpServlet {
    public static final Logger s_logger = Logger.getLogger(ApiServlet.class.getName());
    private static final Logger s_accessLogger = Logger.getLogger("apiserver." + ApiServlet.class.getName());
    private final static List<String> s_clientAddressHeaders = Collections
            .unmodifiableList(Arrays.asList("X-Forwarded-For",
                    "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "Remote_Addr"));
    private static final String REPLACEMENT = "_";
    private static final String LOG_REPLACEMENTS = "[\n\r\t]";

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

    /**
     * For HTTP GET requests, it seems that HttpServletRequest.getParameterMap() actually tries
     * to unwrap URL encoded content from ISO-9959-1.
     * After failed in using setCharacterEncoding() to control it, end up with following hacking:
     * for all GET requests, we will override it with our-own way of UTF-8 based URL decoding.
     * @param req request containing parameters
     * @param params output of "our" map of parameters/values
     */
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

    private void checkSingleQueryParameterValue(Map<String, String[]> params) {
        params.forEach((k, v) -> {
            if (v.length > 1) {
                String message = String.format("Query parameter '%s' has multiple values %s. Only the last value will be respected." +
                    "It is advised to pass only a single parameter", k, Arrays.toString(v));
                s_logger.warn(message);
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
        Map<String, String[]> reqParams = req.getParameterMap();
        checkSingleQueryParameterValue(reqParams);
        params.putAll(reqParams);

        utf8Fixup(req, params);

        // logging the request start and end in management log for easy debugging
        String reqStr = "";
        String cleanQueryString = StringUtils.cleanString(req.getQueryString());
        if (s_logger.isDebugEnabled()) {
            reqStr = auditTrailSb.toString() + " " + cleanQueryString;
            s_logger.debug("===START=== " + reqStr);
        }

        try {
            resp.setContentType(HttpUtils.XML_CONTENT_TYPE);

            HttpSession session = req.getSession(false);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("session found: %s", session));
            }
            final Object[] responseTypeParam = params.get(ApiConstants.RESPONSE);
            if (responseTypeParam != null) {
                responseType = (String)responseTypeParam[0];
            }

            final Object[] commandObj = params.get(ApiConstants.COMMAND);
            final String command = commandObj == null ? null : (String) commandObj[0];
            final Object[] userObj = params.get(ApiConstants.USERNAME);
            String username = userObj == null ? null : (String)userObj[0];
            if (s_logger.isTraceEnabled()) {
                String logCommand = saveLogString(command);
                String logName = saveLogString(username);
                s_logger.trace(String.format("command %s processing for user \"%s\"",
                        logCommand,
                        logName));
            }

            if (command != null && !command.equals(ValidateUserTwoFactorAuthenticationCodeCmd.APINAME)) {

                APIAuthenticator apiAuthenticator = authManager.getAPIAuthenticator(command);
                if (apiAuthenticator != null) {
                    auditTrailSb.append("command=");
                    auditTrailSb.append(command);

                    int httpResponseCode = HttpServletResponse.SC_OK;
                    String responseString = null;

                    if (apiAuthenticator.getAPIType() == APIAuthenticationType.LOGIN_API) {
                        if (session != null) {
                            invalidateHttpSession(session, "invalidating session for login call");
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
                        if (s_logger.isTraceEnabled()) {
                            s_logger.trace(String.format("apiAuthenticator.authenticate(%s, params[%d], %s, %s, %s, %s, %s,%s)",
                                    saveLogString(command), params.size(), session.getId(), remoteAddress.getHostAddress(), saveLogString(responseType), "auditTrailSb", "req", "resp"));
                        }
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
                            invalidateHttpSession(session, "invalidating session after logout call");
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
            } else {
                s_logger.trace("no command available");
            }
            auditTrailSb.append(cleanQueryString);
            final boolean isNew = ((session == null) ? true : session.isNew());

            // Initialize an empty context and we will update it after we have verified the request below,
            // we no longer rely on web-session here, verifyRequest will populate user/account information
            // if a API key exists

            if (isNew && s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("new session: %s", session));
            }

            if (!isNew && (command.equalsIgnoreCase(ValidateUserTwoFactorAuthenticationCodeCmd.APINAME) || (!skip2FAcheckForAPIs(command) && !skip2FAcheckForUser(session)))) {
                s_logger.debug("Verifying two factor authentication");
                boolean success = verify2FA(session, command, auditTrailSb, params, remoteAddress, responseType, req, resp);
                if (!success) {
                    s_logger.debug("Verification of two factor authentication failed");
                    return;
                }
            }

            Long userId = null;
            if (!isNew) {
                userId = (Long)session.getAttribute("userid");
                final String account = (String) session.getAttribute("account");
                final Object accountObj = session.getAttribute("accountobj");
                if (account != null) {
                    if (invalidateHttpSessionIfNeeded(req, resp, auditTrailSb, responseType, params, session, account)) return;
                } else {
                    if (s_logger.isDebugEnabled()) {
                        s_logger.debug("no account, this request will be validated through apikey(%s)/signature");
                    }
                }

                if (! requestChecksoutAsSane(resp, auditTrailSb, responseType, params, session, command, userId, account, accountObj))
                    return;
            } else {
                CallContext.register(accountMgr.getSystemUser(), accountMgr.getSystemAccount());
            }
            setProjectContext(params);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("verifying request for user %s from %s with %d parameters",
                        userId, remoteAddress.getHostAddress(), params.size()));
            }
            if (apiServer.verifyRequest(params, userId, remoteAddress)) {
                auditTrailSb.insert(0, "(userId=" + CallContext.current().getCallingUserId() + " accountId=" + CallContext.current().getCallingAccount().getId() +
                        " sessionId=" + (session != null ? session.getId() : null) + ")");

                // Add the HTTP method (GET/POST/PUT/DELETE) as well into the params map.
                params.put("httpmethod", new String[]{req.getMethod()});
                setProjectContext(params);
                setClientAddressForConsoleEndpointAccess(command, params, req);
                final String response = apiServer.handleRequest(params, responseType, auditTrailSb);
                HttpUtils.writeHttpResponse(resp, response != null ? response : "", HttpServletResponse.SC_OK, responseType, ApiServer.JSONcontentType.value());
            } else {
                if (session != null) {
                    invalidateHttpSession(session, String.format("request verification failed for %s from %s", userId, remoteAddress.getHostAddress()));
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

    private boolean checkIfAuthenticatorIsOf2FA(String command) {
        boolean verify2FA = false;
        APIAuthenticator apiAuthenticator = authManager.getAPIAuthenticator(command);
        if (apiAuthenticator != null && apiAuthenticator.getAPIType().equals(APIAuthenticationType.LOGIN_2FA_API)) {
            verify2FA = true;
        } else {
            verify2FA = false;
        }
        return verify2FA;
    }

    protected boolean skip2FAcheckForAPIs(String command) {
        boolean skip2FAcheck = false;

        if (command.equalsIgnoreCase(ApiConstants.LIST_IDPS)
                || command.equalsIgnoreCase(ApiConstants.LIST_APIS)
                || command.equalsIgnoreCase(ListUserTwoFactorAuthenticatorProvidersCmd.APINAME)
                || command.equalsIgnoreCase(SetupUserTwoFactorAuthenticationCmd.APINAME)) {
            skip2FAcheck = true;
        }
        return skip2FAcheck;
    }

    protected boolean skip2FAcheckForUser(HttpSession session) {
        boolean skip2FAcheck = false;
        Long userId = (Long) session.getAttribute("userid");
        boolean is2FAverified = (boolean) session.getAttribute(ApiConstants.IS_2FA_VERIFIED);
        if (is2FAverified) {
            s_logger.debug(String.format("Two factor authentication is already verified for the user %d, so skipping", userId));
            skip2FAcheck = true;
        } else {
            UserAccount userAccount = accountMgr.getUserAccountById(userId);
            boolean is2FAenabled = userAccount.isUser2faEnabled();
            if (is2FAenabled) {
                skip2FAcheck = false;
            } else {
                Long domainId = userAccount.getDomainId();
                boolean is2FAmandated = Boolean.TRUE.equals(AccountManagerImpl.enableUserTwoFactorAuthentication.valueIn(domainId)) && Boolean.TRUE.equals(AccountManagerImpl.mandateUserTwoFactorAuthentication.valueIn(domainId));
                if (is2FAmandated) {
                    skip2FAcheck = false;
                } else {
                    skip2FAcheck = true;
                }
            }
        }
        return skip2FAcheck;
    }

    protected boolean verify2FA(HttpSession session, String command, StringBuilder auditTrailSb, Map<String, Object[]> params,
                                InetAddress remoteAddress, String responseType, HttpServletRequest req, HttpServletResponse resp) {
        boolean verify2FA = false;
        if (command.equals(ValidateUserTwoFactorAuthenticationCodeCmd.APINAME)) {
            APIAuthenticator apiAuthenticator = authManager.getAPIAuthenticator(command);
            if (apiAuthenticator != null) {
                String responseString = apiAuthenticator.authenticate(command, params, session, remoteAddress, responseType, auditTrailSb, req, resp);
                session.setAttribute(ApiConstants.IS_2FA_VERIFIED, true);
                HttpUtils.writeHttpResponse(resp, responseString, HttpServletResponse.SC_OK, responseType, ApiServer.JSONcontentType.value());
                verify2FA = true;
            } else {
                s_logger.error("Cannot find API authenticator while verifying 2FA");
                auditTrailSb.append(" Cannot find API authenticator while verifying 2FA");
                verify2FA = false;
            }
        } else {
            // invalidate the session
            Long userId = (Long) session.getAttribute("userid");
            UserAccount userAccount = accountMgr.getUserAccountById(userId);
            boolean is2FAenabled = userAccount.isUser2faEnabled();
            String keyFor2fa = userAccount.getKeyFor2fa();
            String providerFor2fa = userAccount.getUser2faProvider();
            String errorMsg;
            if (is2FAenabled) {
                if (org.apache.commons.lang3.StringUtils.isEmpty(keyFor2fa) || org.apache.commons.lang3.StringUtils.isEmpty(providerFor2fa)) {
                    errorMsg = "Two factor authentication is mandated by admin, user needs to setup 2FA using setupUserTwoFactorAuthentication API and" +
                            " then verify 2FA using validateUserTwoFactorAuthenticationCode API before calling other APIs. Existing session is invalidated.";
                } else {
                    errorMsg = "Two factor authentication 2FA is enabled but not verified, please verify 2FA using validateUserTwoFactorAuthenticationCode API before calling other APIs. Existing session is invalidated.";
                }
            } else {
                // when (is2FAmandated) is true
                errorMsg = "Two factor authentication is mandated by admin, user needs to setup 2FA using setupUserTwoFactorAuthentication API and" +
                        " then verify 2FA using validateUserTwoFactorAuthenticationCode API before calling other APIs. Existing session is invalidated.";
            }
            s_logger.error(errorMsg);

            invalidateHttpSession(session, String.format("Unable to process the API request for %s from %s due to %s", userId, remoteAddress.getHostAddress(), errorMsg));
            auditTrailSb.append(" " + ApiErrorCode.UNAUTHORIZED2FA + " " + errorMsg);
            final String serializedResponse = apiServer.getSerializedApiError(ApiErrorCode.UNAUTHORIZED2FA.getHttpCode(), "Unable to process the API request due to :" + errorMsg, params, responseType);
            HttpUtils.writeHttpResponse(resp, serializedResponse, ApiErrorCode.UNAUTHORIZED2FA.getHttpCode(), responseType, ApiServer.JSONcontentType.value());
            verify2FA = false;
        }

        return verify2FA;
    }
    protected void setClientAddressForConsoleEndpointAccess(String command, Map<String, Object[]> params, HttpServletRequest req) throws UnknownHostException {
        if (org.apache.commons.lang3.StringUtils.isNotBlank(command) &&
                command.equalsIgnoreCase(BaseCmd.getCommandNameByClass(CreateConsoleEndpointCmd.class))) {
            InetAddress addr = getClientAddress(req);
            String clientAddress = addr != null ? addr.getHostAddress() : null;
            params.put(ConsoleAccessUtils.CLIENT_INET_ADDRESS_KEY, new String[] {clientAddress});
        }
    }

    @Nullable
    private String saveLogString(String stringToLog) {
        return stringToLog == null ? null : stringToLog.replace(LOG_REPLACEMENTS, REPLACEMENT);
    }

    /**
     * Do a sanity check here to make sure the user hasn't already been deleted
     */
    private boolean requestChecksoutAsSane(HttpServletResponse resp, StringBuilder auditTrailSb, String responseType, Map<String, Object[]> params, HttpSession session, String command, Long userId, String account, Object accountObj) {
        if ((userId != null) && (account != null) && (accountObj != null) && apiServer.verifyUser(userId)) {
            if (command == null) {
                s_logger.info("missing command, ignoring request...");
                auditTrailSb.append(" " + HttpServletResponse.SC_BAD_REQUEST + " " + "no command specified");
                final String serializedResponse = apiServer.getSerializedApiError(HttpServletResponse.SC_BAD_REQUEST, "no command specified", params, responseType);
                HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_BAD_REQUEST, responseType, ApiServer.JSONcontentType.value());
                return true;
            }
            final User user = entityMgr.findById(User.class, userId);
            CallContext.register(user, (Account) accountObj);
        } else {
            invalidateHttpSession(session, "Invalidate the session to ensure we won't allow a request across management server restarts if the userId was serialized to the stored session");

            auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials");
            final String serializedResponse =
                    apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials", params, responseType);
            HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType, ApiServer.JSONcontentType.value());
            return false;
        }
        return true;
    }

    private boolean invalidateHttpSessionIfNeeded(HttpServletRequest req, HttpServletResponse resp, StringBuilder auditTrailSb, String responseType, Map<String, Object[]> params, HttpSession session, String account) {
        if (!HttpUtils.validateSessionKey(session, params, req.getCookies(), ApiConstants.SESSIONKEY)) {
            String msg = String.format("invalidating session %s for account %s", session.getId(), account);
            invalidateHttpSession(session, msg);
            auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials");
            final String serializedResponse =
                    apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials", params, responseType);
            HttpUtils.writeHttpResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType, ApiServer.JSONcontentType.value());
            return true;
        }
        return false;
    }

    public static void invalidateHttpSession(HttpSession session, String msg) {
        try {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(msg);
            }
            session.invalidate();
        } catch (final IllegalStateException ise) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(String.format("failed to invalidate session %s", session.getId()));
            }
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
