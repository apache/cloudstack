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
import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.domain.Domain;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.utils.UuidUtils;
import com.cloud.utils.exception.CloudRuntimeException;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.ApiServerService;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.auth.APIAuthenticationType;
import org.apache.cloudstack.api.auth.APIAuthenticator;
import org.apache.cloudstack.api.auth.PluggableAPIAuthenticator;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

@APICommand(name = "resetPassword",
        description = "Resets the password for the user using the token generated via forgotPassword command.",
        since = "4.20.0.0",
        requestHasSensitiveInfo = true,
        responseObject = SuccessResponse.class)
public class DefaultResetPasswordAPIAuthenticatorCmd extends BaseCmd implements APIAuthenticator {


    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.USERNAME,
            type = CommandType.STRING,
            description = "Username", required = true)
    private String username;

    @Parameter(name = ApiConstants.DOMAIN,
            type = CommandType.STRING,
            description = "Path of the domain that the user belongs to. Example: domain=/com/cloud/internal. If no domain is passed in, the ROOT (/) domain is assumed.")
    private String domain;

    @Parameter(name = ApiConstants.TOKEN,
            type = CommandType.STRING,
            required = true,
            description = "Token generated via forgotPassword command.")
    private String token;

    @Parameter(name = ApiConstants.PASSWORD,
            type = CommandType.STRING,
            required = true,
            description = "New password in clear text (Default hashed to SHA256SALT).")
    private String password;

    @Inject
    ApiServerService _apiServer;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getUsername() {
        return username;
    }

    public String getDomainName() {
        return domain;
    }

    public String getToken() {
        return token;
    }

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

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
        final String[] username = (String[])params.get(ApiConstants.USERNAME);
        final String[] password = (String[])params.get(ApiConstants.PASSWORD);
        final String[] domainName = (String[])params.get(ApiConstants.DOMAIN);
        final String[] token = (String[])params.get(ApiConstants.TOKEN);

        Long domainId = null;
        String domain = null;
        domain = getDomainName(auditTrailSb, domainName, domain);

        String serializedResponse = null;

        if (!UuidUtils.isUuid(token[0])) {
            throw new ServerApiException(ApiErrorCode.PARAM_ERROR, "Invalid token");
        }

        if (username != null) {
            final String pwd = ((password == null) ? null : password[0]);
            try {
                final Domain userDomain = _domainService.findDomainByPath(domain);
                if (userDomain != null) {
                    domainId = userDomain.getId();
                } else {
                    throw new ServerApiException(ApiErrorCode.PARAM_ERROR, String.format("Unable to find the domain from the path %s", domain));
                }
                final UserAccount userAccount = _accountService.getActiveUserAccount(username[0], domainId);
                if (userAccount != null && List.of(User.Source.SAML2, User.Source.OAUTH2, User.Source.LDAP).contains(userAccount.getSource())) {
                    throw new CloudAuthenticationException("Password reset is not allowed for CloudStack login");
                }
                boolean success = _apiServer.resetPassword(userAccount, token[0], pwd);
                SuccessResponse successResponse = new SuccessResponse();
                successResponse.setSuccess(success);
                successResponse.setResponseName(getCommandName());
                return ApiResponseSerializer.toSerializedString(successResponse, responseType);
            } catch (final CloudRuntimeException ex) {
                ApiServlet.invalidateHttpSession(session, "fall through to API key,");
                String msg = String.format("%s", ex.getMessage() != null ?
                        ex.getMessage() :
                        "failed to reset password for user, check your inputs");
                auditTrailSb.append(" " + ApiErrorCode.ACCOUNT_ERROR + " " + msg);
                serializedResponse = _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(), msg, params, responseType);
                if (logger.isTraceEnabled()) {
                    logger.trace(msg);
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
        return APIAuthenticationType.PASSWORD_RESET;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
    }
}
