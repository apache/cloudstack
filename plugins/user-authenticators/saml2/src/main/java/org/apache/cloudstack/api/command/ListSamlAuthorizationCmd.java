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

import com.cloud.user.Account;
import com.cloud.user.User;
import com.cloud.user.UserVO;
import com.cloud.user.dao.UserDao;
import org.apache.cloudstack.acl.SecurityChecker;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.SamlAuthorizationResponse;
import org.apache.cloudstack.api.response.UserResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "listSamlAuthorization", description = "Lists authorized users who can used SAML SSO", responseObject = SamlAuthorizationResponse.class, requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListSamlAuthorizationCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(ListSamlAuthorizationCmd.class.getName());
    private static final String s_name = "listsamlauthorizationsresponse";

    @Inject
    private UserDao _userDao;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////
    @Parameter(name = ApiConstants.USER_ID, type = CommandType.UUID, entityType = UserResponse.class, required = false, description = "User uuid")
    private Long userId;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    public Long getUserId() {
        return userId;
    }

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
        List<UserVO> users = new ArrayList<UserVO>();
        if (getUserId() != null) {
            UserVO user = _userDao.getUser(getUserId());
            if (user != null) {
                Account account = _accountService.getAccount(user.getAccountId());
                _accountService.checkAccess(CallContext.current().getCallingAccount(), SecurityChecker.AccessType.ListEntry, true, account);
                users.add(user);
            }
        } else if (CallContext.current().getCallingAccount().getType() == Account.Type.ADMIN) {
            users = _userDao.listAll();
        }

        ListResponse<SamlAuthorizationResponse> response = new ListResponse<SamlAuthorizationResponse>();
        List<SamlAuthorizationResponse> authorizationResponses = new ArrayList<SamlAuthorizationResponse>();
        for (User user: users) {
            SamlAuthorizationResponse authorizationResponse = new SamlAuthorizationResponse(user.getUuid(), user.getSource().equals(User.Source.SAML2), user.getExternalEntity());
            authorizationResponse.setObjectName("samlauthorization");
            authorizationResponses.add(authorizationResponse);
        }
        response.setResponses(authorizationResponses);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }
}
