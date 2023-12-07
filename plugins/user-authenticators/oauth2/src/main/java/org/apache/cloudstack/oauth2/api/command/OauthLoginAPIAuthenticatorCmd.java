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
package org.apache.cloudstack.oauth2.api.command;

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
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;
import java.net.InetAddress;

import static org.apache.cloudstack.oauth2.OAuth2AuthManager.OAuth2IsPluginEnabled;

@APICommand(name = "oauthlogin", description = "Logs a user into the CloudStack after successful verification of OAuth secret code from the particular provider." +
        "A successful login attempt will generate a JSESSIONID cookie value that can be passed in subsequent Query command calls until the \"logout\" command has been issued or the session has expired.",
        requestHasSensitiveInfo = true, responseObject = LoginCmdResponse.class, entityType = {}, since = "4.19.0")
public class OauthLoginAPIAuthenticatorCmd extends BaseCmd implements APIAuthenticator {

    public static final Logger s_logger = Logger.getLogger(OauthLoginAPIAuthenticatorCmd.class.getName());

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.PROVIDER, type = CommandType.STRING, description = "Name of the provider", required = true)
    private String provider;

    @Parameter(name = ApiConstants.EMAIL, type = CommandType.STRING, description = "Email id with which user tried to login using OAuth provider", required = true)
    private String email;

    @Parameter(name = ApiConstants.DOMAIN, type = CommandType.STRING, description = "Path of the domain that the user belongs to. Example: domain=/com/cloud/internal. If no domain is passed in, the ROOT (/) domain is assumed.")
    private String domain;

    @Parameter(name = ApiConstants.DOMAIN__ID, type = CommandType.LONG, description = "The id of the domain that the user belongs to. If both domain and domainId are passed in, \"domainId\" parameter takes precedence.")
    private Long domainId;

    @Parameter(name = ApiConstants.SECRET_CODE, type = CommandType.STRING, description = "Code that is provided by OAuth provider (Eg. google, github) after successful login")
    private String secretCode;

    @Inject
    ApiServerService _apiServer;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getProvider() {
        return provider;
    }

    public String getEmail() {
        return email;
    }

    public String getDomainName() {
        return domain;
    }

    public Long getDomainId() {
        return domainId;
    }

    public String getSecretCode() {
        return secretCode;
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
        if (!OAuth2IsPluginEnabled.value()) {
            throw new CloudAuthenticationException("OAuth is not enabled in CloudStack, users cannot login using OAuth");
        }
        final String[] provider = (String[])params.get(ApiConstants.PROVIDER);
        final String[] emailArray = (String[])params.get(ApiConstants.EMAIL);
        final String[] secretCodeArray = (String[])params.get(ApiConstants.SECRET_CODE);

        String oauthProvider = ((provider == null) ? null : provider[0]);
        String email = ((emailArray == null) ? null : emailArray[0]);
        String secretCode = ((secretCodeArray == null) ? null : secretCodeArray[0]);
        if (StringUtils.isAnyEmpty(oauthProvider, email, secretCode)) {
            throw new CloudAuthenticationException("OAuth provider, email, secretCode any of these cannot be null");
        }

        Long domainId = getDomainIdFromParams(params, auditTrailSb, responseType);
        final String[] domainName = (String[])params.get(ApiConstants.DOMAIN);
        String domain = getDomainName(auditTrailSb, domainName);

        return doOauthAuthentication(session, domainId, domain, email, params, remoteAddress, responseType, auditTrailSb);
    }

    private String doOauthAuthentication(HttpSession session, Long domainId, String domain, String email, Map<String, Object[]> params, InetAddress remoteAddress, String responseType, StringBuilder auditTrailSb) {
        String serializedResponse = null;

        try {
            final Domain userDomain = _domainService.findDomainByIdOrPath(domainId, domain);
            if (userDomain != null) {
                domainId = userDomain.getId();
            } else {
                throw new CloudAuthenticationException("Unable to find the domain from the path " + domain);
            }
            final List<UserAccount> userAccounts = _accountService.getActiveUserAccountByEmail(email, domainId);
            if (CollectionUtils.isEmpty(userAccounts)) {
                throw new CloudAuthenticationException("User not found in CloudStack to login. If user belongs to any domain, please provide it.");
            }
            if (userAccounts.size() > 1) {
                throw new CloudAuthenticationException("Multiple Users found in CloudStack. If user belongs to any specific domain, please provide it.");
            }
            UserAccount userAccount = userAccounts.get(0);
            if (userAccount != null && User.Source.SAML2 == userAccount.getSource()) {
                throw new CloudAuthenticationException("User is not allowed CloudStack login");
            }
            return ApiResponseSerializer.toSerializedString(_apiServer.loginUser(session, userAccount.getUsername(), null, domainId, domain, remoteAddress, params),
                    responseType);
        } catch (final CloudAuthenticationException ex) {
            ApiServlet.invalidateHttpSession(session, "fall through to API key,");
            String msg = String.format("%s", ex.getMessage() != null ?
                    ex.getMessage() :
                    "failed to authenticate user, check if username/password are correct");
            auditTrailSb.append(" " + ApiErrorCode.ACCOUNT_ERROR + " " + msg);
            serializedResponse = _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(), msg, params, responseType);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(msg);
            }
        }

        // We should not reach here and if we do we throw an exception
        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, serializedResponse);
    }

    protected Long getDomainIdFromParams(Map<String, Object[]> params, StringBuilder auditTrailSb, String responseType) {
        String[] domainIdArr = (String[])params.get(ApiConstants.DOMAIN_ID);

        if (domainIdArr == null) {
            domainIdArr = (String[])params.get(ApiConstants.DOMAIN__ID);
        }
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
        return domainId;
    }

    @Nullable
    protected String getDomainName(StringBuilder auditTrailSb, String[] domainName) {
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
