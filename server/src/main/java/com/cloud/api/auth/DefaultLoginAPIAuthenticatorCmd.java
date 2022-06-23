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
package com.cloud.api.auth;

import com.cloud.api.ApiServlet;
import com.cloud.domain.Domain;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import org.apache.cloudstack.api.ApiServerService;
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

@APICommand(name = ApiConstants.LOGIN, description = "Logs a user into the CloudStack. A successful login attempt will generate a JSESSIONID cookie value that can be passed in subsequent Query command calls until the \"logout\" command has been issued or the session has expired.", requestHasSensitiveInfo = true, responseObject = LoginCmdResponse.class, entityType = {})
public class DefaultLoginAPIAuthenticatorCmd extends BaseCmd implements APIAuthenticator {

    public static final Logger s_logger = Logger.getLogger(DefaultLoginAPIAuthenticatorCmd.class.getName());
    private static final String s_name = "loginresponse";

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.USERNAME, type = CommandType.STRING, description = "Username", required = true)
    private String username;

    @Parameter(name = ApiConstants.PASSWORD, type = CommandType.STRING, description = "Hashed password (Default is MD5). If you wish to use any other hashing algorithm, you would need to write a custom authentication adapter See Docs section.", required = true)
    private String password;

    @Parameter(name = ApiConstants.DOMAIN, type = CommandType.STRING, description = "Path of the domain that the user belongs to. Example: domain=/com/cloud/internal. If no domain is passed in, the ROOT (/) domain is assumed.")
    private String domain;

    @Parameter(name = ApiConstants.DOMAIN__ID, type = CommandType.LONG, description = "The id of the domain that the user belongs to. If both domain and domainId are passed in, \"domainId\" parameter takes precendence")
    private Long domainId;

    @Inject
    ApiServerService _apiServer;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getDomainName() {
        return domain;
    }

    public Long getDomainId() {
        return domainId;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.Type.NORMAL.ordinal();
    }

    @Override
    public void execute() throws ServerApiException {
        // We should never reach here
        throw new ServerApiException(ApiErrorCode.METHOD_NOT_ALLOWED, "This is an authentication api, cannot be used directly");
    }

    @Override
    public String authenticate(String command, Map<String, Object[]> params, HttpSession session, InetAddress remoteAddress, String responseType, StringBuilder auditTrailSb, final HttpServletRequest req, final HttpServletResponse resp) throws ServerApiException {
        // Disallow non POST requests
        if (HTTPMethod.valueOf(req.getMethod()) != HTTPMethod.POST) {
            throw new ServerApiException(ApiErrorCode.METHOD_NOT_ALLOWED, "Please use HTTP POST to authenticate using this API");
        }
        // FIXME: ported from ApiServlet, refactor and cleanup
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
                throw new ServerApiException(ApiErrorCode.UNAUTHORIZED,
                        _apiServer.getSerializedApiError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid domain id entered, please enter a valid one", params,
                                responseType));
            }
        }

        String domain = null;
        domain = getDomainName(auditTrailSb, domainName, domain);

        String serializedResponse = null;
        if (username != null) {
            final String pwd = ((password == null) ? null : password[0]);
            try {
                final Domain userDomain = _domainService.findDomainByIdOrPath(domainId, domain);
                if (userDomain != null) {
                    domainId = userDomain.getId();
                } else {
                    throw new CloudAuthenticationException("Unable to find the domain from the path " + domain);
                }
                final UserAccount userAccount = _accountService.getActiveUserAccount(username[0], domainId);
                if (userAccount != null && User.Source.SAML2 == userAccount.getSource()) {
                    throw new CloudAuthenticationException("User is not allowed CloudStack login");
                }
                return ApiResponseSerializer.toSerializedString(_apiServer.loginUser(session, username[0], pwd, domainId, domain, remoteAddress, params),
                        responseType);
            } catch (final CloudAuthenticationException ex) {
                ApiServlet.invalidateHttpSession(session, "fall through to API key,");
                // TODO: fall through to API key, or just fail here w/ auth error? (HTTP 401)
                String msg = String.format("%s", ex.getMessage() != null ?
                        ex.getMessage() :
                        "failed to authenticate user, check if username/password are correct");
                auditTrailSb.append(" " + ApiErrorCode.ACCOUNT_ERROR + " " + msg);
                serializedResponse = _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(), msg, params, responseType);
                if (s_logger.isTraceEnabled()) {
                    s_logger.trace(msg);
                }
            }
        }
        // We should not reach here and if we do we throw an exception
        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, serializedResponse);
    }

    @Nullable
    private String getDomainName(StringBuilder auditTrailSb, String[] domainName, String domain) {
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
        return domain;
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.LOGIN_API;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
    }
}
