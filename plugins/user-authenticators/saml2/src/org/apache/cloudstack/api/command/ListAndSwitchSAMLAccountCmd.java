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
package org.apache.cloudstack.api.command;

import com.cloud.api.response.ApiResponseSerializer;
import com.cloud.domain.Domain;
import com.cloud.domain.dao.DomainDao;
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.user.dao.UserAccountDao;
import com.cloud.user.dao.UserDao;
import com.cloud.utils.HttpUtils;
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
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.LoginCmdResponse;
import org.apache.cloudstack.api.response.SamlUserAccountResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.saml.SAML2AuthManager;
import org.apache.cloudstack.saml.SAMLUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@APICommand(name = "listAndSwitchSamlAccount", description = "Lists and switches to other SAML accounts owned by the SAML user", responseObject = SuccessResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListAndSwitchSAMLAccountCmd extends BaseCmd implements APIAuthenticator {
    public static final Logger s_logger = Logger.getLogger(ListAndSwitchSAMLAccountCmd.class.getName());
    private static final String s_name = "listandswitchsamlaccountresponse";

    @Inject
    ApiServerService _apiServer;

    @Inject
    private UserAccountDao _userAccountDao;
    @Inject
    private UserDao _userDao;
    @Inject
    private DomainDao _domainDao;

    SAML2AuthManager _samlAuthManager;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, required = false, description = "User uuid")
    private Long userId;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, required = false, description = "Domain uuid")
    private Long domainId;

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        throw new ServerApiException(ApiErrorCode.METHOD_NOT_ALLOWED, "This is an authentication plugin api, cannot be used directly");
    }

    @Override
    public String authenticate(final String command, final Map<String, Object[]> params, final HttpSession session, InetAddress remoteAddress, final String responseType, final StringBuilder auditTrailSb, final HttpServletRequest req, final HttpServletResponse resp) throws ServerApiException {
        if (session == null || session.isNew()) {
            throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, _apiServer.getSerializedApiError(ApiErrorCode.UNAUTHORIZED.getHttpCode(),
                    "Only authenticated saml users can request this API",
                    params, responseType));
        }

        if (!HttpUtils.validateSessionKey(session, params, req.getCookies(), ApiConstants.SESSIONKEY)) {
            throw new ServerApiException(ApiErrorCode.UNAUTHORIZED, _apiServer.getSerializedApiError(ApiErrorCode.UNAUTHORIZED.getHttpCode(),
                    "Unauthorized session, please re-login",
                    params, responseType));
        }

        final long currentUserId = (Long) session.getAttribute("userid");
        final UserAccount currentUserAccount = _accountService.getUserAccountById(currentUserId);
        if (currentUserAccount == null || currentUserAccount.getSource() != User.Source.SAML2) {
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                    "Only authenticated saml users can request this API",
                    params, responseType));
        }

        String userUuid = null;
        String domainUuid = null;
        if (params.containsKey(ApiConstants.USER_ID)) {
            userUuid = ((String[])params.get(ApiConstants.USER_ID))[0];
        }
        if (params.containsKey(ApiConstants.DOMAIN_ID)) {
            domainUuid = ((String[])params.get(ApiConstants.DOMAIN_ID))[0];
        }

        if (userUuid != null && domainUuid != null) {
            final User user = _userDao.findByUuid(userUuid);
            final Domain domain = _domainDao.findByUuid(domainUuid);
            final UserAccount nextUserAccount = _accountService.getUserAccountById(user.getId());
            if (nextUserAccount != null && !nextUserAccount.getAccountState().equals(Account.State.enabled.toString())) {
                throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, _apiServer.getSerializedApiError(ApiErrorCode.PARAM_ERROR.getHttpCode(),
                        "The requested user account is locked and cannot be switched to, please contact your administrator.",
                        params, responseType));
            }
            if (nextUserAccount == null
                    || !nextUserAccount.getAccountState().equals(Account.State.enabled.toString())
                    || !nextUserAccount.getUsername().equals(currentUserAccount.getUsername())
                    || !nextUserAccount.getExternalEntity().equals(currentUserAccount.getExternalEntity())
                    || (nextUserAccount.getDomainId() != domain.getId())
                    || (nextUserAccount.getSource() != User.Source.SAML2)) {
                throw new ServerApiException(ApiErrorCode.PARAM_ERROR, _apiServer.getSerializedApiError(ApiErrorCode.PARAM_ERROR.getHttpCode(),
                        "User account is not allowed to switch to the requested account",
                        params, responseType));
            }
            try {
                if (_apiServer.verifyUser(nextUserAccount.getId())) {
                    final LoginCmdResponse loginResponse = (LoginCmdResponse) _apiServer.loginUser(session, nextUserAccount.getUsername(), nextUserAccount.getUsername() + nextUserAccount.getSource().toString(),
                            nextUserAccount.getDomainId(), null, remoteAddress, params);
                    SAMLUtils.setupSamlUserCookies(loginResponse, resp);
                    resp.sendRedirect(SAML2AuthManager.SAMLCloudStackRedirectionUrl.value());
                    return ApiResponseSerializer.toSerializedString(loginResponse, responseType);
                }
            } catch (CloudAuthenticationException | IOException exception) {
                s_logger.debug("Failed to switch to request SAML user account due to: " + exception.getMessage());
            }
        } else {
            List<UserAccountVO> switchableAccounts = _userAccountDao.getAllUsersByNameAndEntity(currentUserAccount.getUsername(), currentUserAccount.getExternalEntity());
            if (switchableAccounts != null && switchableAccounts.size() > 0 && currentUserId != User.UID_SYSTEM) {
                List<SamlUserAccountResponse> accountResponses = new ArrayList<SamlUserAccountResponse>();
                for (UserAccountVO userAccount: switchableAccounts) {
                    User user = _userDao.getUser(userAccount.getId());
                    Domain domain = _domainService.getDomain(userAccount.getDomainId());
                    SamlUserAccountResponse accountResponse = new SamlUserAccountResponse();
                    accountResponse.setUserId(user.getUuid());
                    accountResponse.setUserName(user.getUsername());
                    accountResponse.setDomainId(domain.getUuid());
                    accountResponse.setDomainName(domain.getName());
                    accountResponse.setAccountName(userAccount.getAccountName());
                    accountResponse.setIdpId(user.getExternalEntity());
                    accountResponses.add(accountResponse);
                }
                ListResponse<SamlUserAccountResponse> response = new ListResponse<SamlUserAccountResponse>();
                response.setResponses(accountResponses);
                response.setResponseName(getCommandName());
                return ApiResponseSerializer.toSerializedString(response, responseType);
            }
        }
        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(),
                "Unable to switch to requested SAML account. Please make sure your user/account is enabled. Please contact your administrator.",
                params, responseType));
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.READONLY_API;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
        for (PluggableAPIAuthenticator authManager: authenticators) {
            if (authManager != null && authManager instanceof SAML2AuthManager) {
                _samlAuthManager = (SAML2AuthManager) authManager;
            }
        }
        if (_samlAuthManager == null) {
            s_logger.error("No suitable Pluggable Authentication Manager found for SAML2 listAndSwitchSamlAccount Cmd");
        }
    }
}