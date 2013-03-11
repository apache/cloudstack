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

import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.SpringBeanAutowiringSupport;

import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.AccountService;
import com.cloud.user.UserContext;
import com.cloud.utils.StringUtils;

@Component("apiServlet")
@SuppressWarnings("serial")
public class ApiServlet extends HttpServlet {
    public static final Logger s_logger = Logger.getLogger(ApiServlet.class.getName());
    private static final Logger s_accessLogger = Logger.getLogger("apiserver." + ApiServer.class.getName());

    @Inject ApiServerService _apiServer;
    @Inject AccountService _accountMgr;

    public ApiServlet() {
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
    	SpringBeanAutowiringSupport.processInjectionBasedOnServletContext(this, config.getServletContext());       	
    }
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        processRequest(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        processRequest(req, resp);
    }

    private void utf8Fixup(HttpServletRequest req, Map<String, Object[]> params) {
        if (req.getQueryString() == null)
            return;

        String[] paramsInQueryString = req.getQueryString().split("&");
        if (paramsInQueryString != null) {
            for (String param : paramsInQueryString) {
                String[] paramTokens = param.split("=");
                if (paramTokens != null && paramTokens.length == 2) {
                    String name = paramTokens[0];
                    String value = paramTokens[1];

                    try {
                        name = URLDecoder.decode(name, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                    try {
                        value = URLDecoder.decode(value, "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                    }
                    params.put(name, new String[] { value });
                } else {
                    s_logger.debug("Invalid parameter in URL found. param: " + param);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void processRequest(HttpServletRequest req, HttpServletResponse resp) {
        StringBuffer auditTrailSb = new StringBuffer();
        auditTrailSb.append(" " + req.getRemoteAddr());
        auditTrailSb.append(" -- " + req.getMethod() + " ");
        // get the response format since we'll need it in a couple of places
        String responseType = BaseCmd.RESPONSE_TYPE_XML;
        Map<String, Object[]> params = new HashMap<String, Object[]>();
        params.putAll(req.getParameterMap());

        // For HTTP GET requests, it seems that HttpServletRequest.getParameterMap() actually tries
        // to unwrap URL encoded content from ISO-9959-1.
        // After failed in using setCharacterEncoding() to control it, end up with following hacking:
        // for all GET requests, we will override it with our-own way of UTF-8 based URL decoding.
        utf8Fixup(req, params);

        // logging the request start and end in management log for easy debugging
        String reqStr = "";
        if (s_logger.isDebugEnabled()) {
            reqStr = auditTrailSb.toString() + " " + req.getQueryString();
            s_logger.debug("===START=== " + StringUtils.cleanString(reqStr));
        }

        try {
            HttpSession session = req.getSession(false);
            Object[] responseTypeParam = params.get("response");
            if (responseTypeParam != null) {
                responseType = (String) responseTypeParam[0];
            }

            Object[] commandObj = params.get("command");
            if (commandObj != null) {
                String command = (String) commandObj[0];
                if ("logout".equalsIgnoreCase(command)) {
                    // if this is just a logout, invalidate the session and return
                    if (session != null) {
                        Long userId = (Long) session.getAttribute("userid");
                        Account account = (Account) session.getAttribute("accountobj");
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
                        } catch (IllegalStateException ise) {
                        }
                    }
                    auditTrailSb.append("command=logout");
                    auditTrailSb.append(" " + HttpServletResponse.SC_OK);
                    writeResponse(resp, getLogoutSuccessResponse(responseType), HttpServletResponse.SC_OK, responseType);
                    return;
                } else if ("login".equalsIgnoreCase(command)) {
                    auditTrailSb.append("command=login");
                    // if this is a login, authenticate the user and return
                    if (session != null) {
                        try {
                            session.invalidate();
                        } catch (IllegalStateException ise) {
                        }
                    }
                    session = req.getSession(true);
                    String[] username = (String[]) params.get("username");
                    String[] password = (String[]) params.get("password");
                    String[] domainIdArr = (String[]) params.get("domainid");

                    if (domainIdArr == null) {
                        domainIdArr = (String[]) params.get("domainId");
                    }
                    String[] domainName = (String[]) params.get("domain");
                    Long domainId = null;
                    if ((domainIdArr != null) && (domainIdArr.length > 0)) {
                        try {
                            //check if UUID is passed in for domain
                            domainId = _apiServer.fetchDomainId(domainIdArr[0]);
                            if(domainId == null){
                                domainId = new Long(Long.parseLong(domainIdArr[0]));
                            }
                            auditTrailSb.append(" domainid=" + domainId);// building the params for POST call
                        } catch (NumberFormatException e) {
                            s_logger.warn("Invalid domain id entered by user");
                            auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "Invalid domain id entered, please enter a valid one");
                            String serializedResponse = _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid domain id entered, please enter a valid one", params,
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
                        String pwd = ((password == null) ? null : password[0]);
                        try {
                            _apiServer.loginUser(session, username[0], pwd, domainId, domain, req.getRemoteAddr(), params);
                            auditTrailSb.insert(0,
                                    "(userId=" + session.getAttribute("userid") + " accountId=" + ((Account) session.getAttribute("accountobj")).getId() + " sessionId=" + session.getId() + ")");
                            String loginResponse = getLoginSuccessResponse(session, responseType);
                            writeResponse(resp, loginResponse, HttpServletResponse.SC_OK, responseType);
                            return;
                        } catch (CloudAuthenticationException ex) {
                            // TODO: fall through to API key, or just fail here w/ auth error? (HTTP 401)
                            try {
                                session.invalidate();
                            } catch (IllegalStateException ise) {
                            }

                            auditTrailSb.append(" " + ApiErrorCode.ACCOUNT_ERROR + " " + ex.getMessage() != null ? ex.getMessage() : "failed to authenticate user, check if username/password are correct");
                            String serializedResponse = _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(), ex.getMessage() != null ? ex.getMessage()
                                    : "failed to authenticate user, check if username/password are correct", params, responseType);
                            writeResponse(resp, serializedResponse, ApiErrorCode.ACCOUNT_ERROR.getHttpCode(), responseType);
                            return;
                        }
                    }
                }
            }
            auditTrailSb.append(req.getQueryString());
            boolean isNew = ((session == null) ? true : session.isNew());

            // Initialize an empty context and we will update it after we have verified the request below,
            // we no longer rely on web-session here, verifyRequest will populate user/account information
            // if a API key exists
            UserContext.registerContext(_accountMgr.getSystemUser().getId(), _accountMgr.getSystemAccount(), null, false);
            Long userId = null;

            if (!isNew) {
                userId = (Long) session.getAttribute("userid");
                String account = (String) session.getAttribute("account");
                Object accountObj = session.getAttribute("accountobj");
                String sessionKey = (String) session.getAttribute("sessionkey");
                String[] sessionKeyParam = (String[]) params.get("sessionkey");
                if ((sessionKeyParam == null) || (sessionKey == null) || !sessionKey.equals(sessionKeyParam[0])) {
                    try {
                        session.invalidate();
                    } catch (IllegalStateException ise) {
                    }
                    auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials");
                    String serializedResponse = _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials", params, responseType);
                    writeResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType);
                    return;
                }

                // Do a sanity check here to make sure the user hasn't already been deleted
                if ((userId != null) && (account != null)
                        && (accountObj != null) && _apiServer.verifyUser(userId)) {
                    String[] command = (String[]) params.get("command");
                    if (command == null) {
                        s_logger.info("missing command, ignoring request...");
                        auditTrailSb.append(" " + HttpServletResponse.SC_BAD_REQUEST + " " + "no command specified");
                        String serializedResponse = _apiServer.getSerializedApiError(HttpServletResponse.SC_BAD_REQUEST, "no command specified", params, responseType);
                        writeResponse(resp, serializedResponse, HttpServletResponse.SC_BAD_REQUEST, responseType);
                        return;
                    }
                    UserContext.updateContext(userId, (Account) accountObj, session.getId());
                } else {
                    // Invalidate the session to ensure we won't allow a request across management server
                    // restarts if the userId was serialized to the stored session
                    try {
                        session.invalidate();
                    } catch (IllegalStateException ise) {
                    }

                    auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials");
                    String serializedResponse = _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials", params, responseType);
                    writeResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType);
                    return;
                }
            }

            if (_apiServer.verifyRequest(params, userId)) {
                /*
                 * if (accountObj != null) { Account userAccount = (Account)accountObj; if (userAccount.getType() ==
                 * Account.ACCOUNT_TYPE_NORMAL) { params.put(BaseCmd.Properties.USER_ID.getName(), new String[] { userId });
                 * params.put(BaseCmd.Properties.ACCOUNT.getName(), new String[] { account });
                 * params.put(BaseCmd.Properties.DOMAIN_ID.getName(), new String[] { domainId });
                 * params.put(BaseCmd.Properties.ACCOUNT_OBJ.getName(), new Object[] { accountObj }); } else {
                 * params.put(BaseCmd.Properties.USER_ID.getName(), new String[] { userId });
                 * params.put(BaseCmd.Properties.ACCOUNT_OBJ.getName(), new Object[] { accountObj }); } }
                 * 
                 * // update user context info here so that we can take information if the request is authenticated // via api
                 * key mechanism updateUserContext(params, session != null ? session.getId() : null);
                 */

                auditTrailSb.insert(0, "(userId=" + UserContext.current().getCallerUserId() + " accountId="
                        + UserContext.current().getCaller().getId() + " sessionId=" + (session != null ? session.getId() : null) + ")");

                    String response = _apiServer.handleRequest(params, responseType, auditTrailSb);
                    writeResponse(resp, response != null ? response : "", HttpServletResponse.SC_OK, responseType);
            } else {
                if (session != null) {
                    try {
                        session.invalidate();
                    } catch (IllegalStateException ise) {
                    }
                }

                auditTrailSb.append(" " + HttpServletResponse.SC_UNAUTHORIZED + " " + "unable to verify user credentials and/or request signature");
                String serializedResponse = _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "unable to verify user credentials and/or request signature", params, responseType);
                writeResponse(resp, serializedResponse, HttpServletResponse.SC_UNAUTHORIZED, responseType);

            }
        } catch (ServerApiException se) {
            String serializedResponseText = _apiServer.getSerializedApiError(se, params, responseType);
                resp.setHeader("X-Description", se.getDescription());
            writeResponse(resp, serializedResponseText, se.getErrorCode().getHttpCode(), responseType);
                auditTrailSb.append(" " + se.getErrorCode() + " " + se.getDescription());
        } catch (Exception ex) {
                s_logger.error("unknown exception writing api response", ex);
                auditTrailSb.append(" unknown exception writing api response");
        } finally {
            s_accessLogger.info(auditTrailSb.toString());
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("===END=== " + StringUtils.cleanString(reqStr));
            }
            // cleanup user context to prevent from being peeked in other request context
            UserContext.unregisterContext();
        }
    }

    /*
     * private void updateUserContext(Map<String, Object[]> requestParameters, String sessionId) { String userIdStr =
     * (String)(requestParameters.get(BaseCmd.Properties.USER_ID.getName())[0]); Account accountObj =
     * (Account)(requestParameters.get(BaseCmd.Properties.ACCOUNT_OBJ.getName())[0]);
     * 
     * Long userId = null; Long accountId = null; if(userIdStr != null) userId = Long.parseLong(userIdStr);
     * 
     * if(accountObj != null) accountId = accountObj.getId(); UserContext.updateContext(userId, accountId, sessionId); }
     */

    // FIXME: rather than isError, we might was to pass in the status code to give more flexibility
    private void writeResponse(HttpServletResponse resp, String response, int responseCode, String responseType) {
        try {
            if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
                resp.setContentType(ApiServer.jsonContentType + "; charset=UTF-8");
            } else {
                resp.setContentType("text/xml; charset=UTF-8");
            }

            resp.setStatus(responseCode);
            resp.getWriter().print(response);
        } catch (IOException ioex) {
            if (s_logger.isTraceEnabled()) {
                s_logger.trace("exception writing response: " + ioex);
            }
        } catch (Exception ex) {
            if (!(ex instanceof IllegalStateException)) {
                s_logger.error("unknown exception writing api response", ex);
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private String getLoginSuccessResponse(HttpSession session, String responseType) {
        StringBuffer sb = new StringBuffer();
        int inactiveInterval = session.getMaxInactiveInterval();

        String user_UUID = (String)session.getAttribute("user_UUID");
        session.removeAttribute("user_UUID");

        String domain_UUID = (String)session.getAttribute("domain_UUID");
        session.removeAttribute("domain_UUID");

        if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            sb.append("{ \"loginresponse\" : { ");
            Enumeration attrNames = session.getAttributeNames();
            if (attrNames != null) {
                sb.append("\"timeout\" : \"" + inactiveInterval + "\"");
                while (attrNames.hasMoreElements()) {
                    String attrName = (String) attrNames.nextElement();
                    if("userid".equalsIgnoreCase(attrName)){
                        sb.append(", \"" + attrName + "\" : \"" + user_UUID + "\"");
                    }else if("domainid".equalsIgnoreCase(attrName)){
                        sb.append(", \"" + attrName + "\" : \"" + domain_UUID + "\"");
                    }else{
                        Object attrObj = session.getAttribute(attrName);
                        if ((attrObj instanceof String) || (attrObj instanceof Long)) {
                            sb.append(", \"" + attrName + "\" : \"" + attrObj.toString() + "\"");
                        }
                    }
                }
            }
            sb.append(" } }");
        } else {
            sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            sb.append("<loginresponse cloud-stack-version=\"" + ApiDBUtils.getVersion() + "\">");
            sb.append("<timeout>" + inactiveInterval + "</timeout>");
            Enumeration attrNames = session.getAttributeNames();
            if (attrNames != null) {
                while (attrNames.hasMoreElements()) {
                    String attrName = (String) attrNames.nextElement();
                    if("userid".equalsIgnoreCase(attrName)){
                        sb.append("<" + attrName + ">" + user_UUID + "</" + attrName + ">");
                    }else if("domainid".equalsIgnoreCase(attrName)){
                        sb.append("<" + attrName + ">" + domain_UUID + "</" + attrName + ">");
                    }else{
                        Object attrObj = session.getAttribute(attrName);
                        if (attrObj instanceof String || attrObj instanceof Long || attrObj instanceof Short) {
                            sb.append("<" + attrName + ">" + attrObj.toString() + "</" + attrName + ">");
                        }
                    }
                }
            }

            sb.append("</loginresponse>");
        }
        return sb.toString();
    }

    private String getLogoutSuccessResponse(String responseType) {
        StringBuffer sb = new StringBuffer();
        if (BaseCmd.RESPONSE_TYPE_JSON.equalsIgnoreCase(responseType)) {
            sb.append("{ \"logoutresponse\" : { \"description\" : \"success\" } }");
        } else {
            sb.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            sb.append("<logoutresponse cloud-stack-version=\"" + ApiDBUtils.getVersion() + "\">");
            sb.append("<description>success</description>");
            sb.append("</logoutresponse>");
        }
        return sb.toString();
    }

}
