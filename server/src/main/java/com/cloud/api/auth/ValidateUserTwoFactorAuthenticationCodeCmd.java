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
import com.cloud.exception.CloudAuthenticationException;
import com.cloud.user.AccountManager;
import com.cloud.user.UserAccount;
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
import org.apache.log4j.Logger;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;

@APICommand(name = ValidateUserTwoFactorAuthenticationCodeCmd.APINAME, description = "Checks the 2fa code for the user.", requestHasSensitiveInfo = false,
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

    @Parameter(name = ApiConstants.TWOFACTORAUTHENTICATIONCODE, type = CommandType.STRING, description = "two factor authentication code", required = true)
    private String twoFactorAuthenticationCode;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////

    public String getTwoFactorAuthenticationCode() {
        return twoFactorAuthenticationCode;
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
        String twoFactorAuthenticationCode = null;
        if (params.containsKey(ApiConstants.TWOFACTORAUTHENTICATIONCODE)) {
            twoFactorAuthenticationCode = ((String[])params.get(ApiConstants.TWOFACTORAUTHENTICATIONCODE))[0];
        }
        if (twoFactorAuthenticationCode.isEmpty()) {
            throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, "Code for two factor authentication is required");
        }

        final long currentUserId = (Long) session.getAttribute("userid");
        final UserAccount currentUserAccount = _accountService.getUserAccountById(currentUserId);

        String serializedResponse = null;
        try {
            accountManager.verifyUsingTwoFactorAuthenticationCode(twoFactorAuthenticationCode, currentUserAccount.getDomainId(), currentUserId);
            SuccessResponse response = new SuccessResponse(getCommandName());
            setResponseObject(response);
            return ApiResponseSerializer.toSerializedString(response, responseType);
        } catch (final CloudAuthenticationException ex) {
            ApiServlet.invalidateHttpSession(session, "fall through to API key,");
            String msg = String.format("%s", ex.getMessage() != null ?
                    ex.getMessage() :
                    "failed to authenticate user, check if two factor authentication code is correct");
            auditTrailSb.append(" " + ApiErrorCode.ACCOUNT_ERROR + " " + msg);
            serializedResponse = _apiServer.getSerializedApiError(ApiErrorCode.ACCOUNT_ERROR.getHttpCode(), msg, params, responseType);
            if (s_logger.isTraceEnabled()) {
                s_logger.trace(msg);
            }
        }
        // We should not reach here and if we do we throw an exception
        throw new ServerApiException(ApiErrorCode.ACCOUNT_ERROR, serializedResponse);
    }

    @Override
    public APIAuthenticationType getAPIType() {
        return APIAuthenticationType.LOGIN_2FA_API;
    }

    @Override
    public void setAuthenticators(List<PluggableAPIAuthenticator> authenticators) {
    }
}
