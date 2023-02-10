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
import com.cloud.exception.CloudTwoFactorAuthenticationException;
import com.cloud.user.AccountManager;
import com.cloud.user.UserAccount;
import com.cloud.user.UserAccountVO;
import com.cloud.utils.exception.CSExceptionErrorCode;
import org.apache.cloudstack.acl.RoleType;
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
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.resourcedetail.UserDetailVO;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

@APICommand(name = ValidateUserTwoFactorAuthenticationCodeCmd.APINAME, description = "Checks the 2FA code for the user.", requestHasSensitiveInfo = false,
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User},
        responseObject = SuccessResponse.class, entityType = {}, since = "4.18.0")
public class ValidateUserTwoFactorAuthenticationCodeCmd extends BaseCmd implements APIAuthenticator {

    public static final String APINAME = "validateUserTwoFactorAuthenticationCode";
    public static final Logger s_logger = Logger.getLogger(ValidateUserTwoFactorAuthenticationCodeCmd.class.getName());

    @Inject
    private AccountManager accountManager;

    @Inject
    ApiServerService _apiServer;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @Parameter(name = ApiConstants.CODE_FOR_2FA, type = CommandType.STRING, description = "two factor authentication code", required = true)
    private String codeFor2fa;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getCodeFor2fa() {
        return codeFor2fa;
    }

    @Override
    public void execute() throws ServerApiException {
        throw new ServerApiException(ApiErrorCode.METHOD_NOT_ALLOWED, "This is an authentication api, cannot be used directly");
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }

    @Override
    public String authenticate(String command, Map<String, Object[]> params, HttpSession session, InetAddress remoteAddress, String responseType, StringBuilder auditTrailSb, HttpServletRequest req, HttpServletResponse resp) throws ServerApiException {
        String codeFor2FA = null;
        if (params.containsKey(ApiConstants.CODE_FOR_2FA)) {
            codeFor2FA = ((String[])params.get(ApiConstants.CODE_FOR_2FA))[0];
        }
        if (StringUtils.isEmpty(codeFor2FA)) {
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, "Code for two factor authentication is required");
        }

        final long currentUserId = (Long) session.getAttribute("userid");
        final UserAccount currentUserAccount = _accountService.getUserAccountById(currentUserId);
        boolean setupPhase = false;
        Map<String, String> userDetails = currentUserAccount.getDetails();
        if (userDetails.containsKey(UserDetailVO.Setup2FADetail) && userDetails.get(UserDetailVO.Setup2FADetail).equals(UserAccountVO.Setup2FAstatus.ENABLED.name())) {
            setupPhase = true;
        }

        String serializedResponse = null;
        try {
            accountManager.verifyUsingTwoFactorAuthenticationCode(codeFor2FA, currentUserAccount.getDomainId(), currentUserId);
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
            return ApiResponseSerializer.toSerializedString(response, responseType);
        } catch (final CloudTwoFactorAuthenticationException ex) {
            if (!setupPhase) {
                ApiServlet.invalidateHttpSession(session, "fall through to API key,");
            }
            String msg = String.format("%s", ex.getMessage() != null ?
                    ex.getMessage() :
                    "failed to authenticate user, check if two factor authentication code is correct");
            auditTrailSb.append(" " + ApiErrorCode.UNAUTHORIZED2FA + " " + msg);
            serializedResponse = _apiServer.getSerializedApiError(ApiErrorCode.UNAUTHORIZED2FA.getHttpCode(), msg, params, responseType);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(msg);
            }
        }
        ServerApiException exception = new ServerApiException(ApiErrorCode.UNAUTHORIZED2FA, serializedResponse);
        exception.setCSErrorCode(CSExceptionErrorCode.getCSErrCode(CloudTwoFactorAuthenticationException.class.getName()));
        throw exception;
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.LOGIN_2FA_API;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
    }
}
