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
package org.apache.cloudstack.api.command.admin.ratelimit;

import org.apache.cloudstack.api.ACL;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ApiLimitResponse;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.ratelimit.ApiRateLimitService;
import org.apache.log4j.Logger;

import com.cloud.configuration.Config;
import com.cloud.configuration.dao.ConfigurationDao;
import com.cloud.user.Account;
import com.cloud.user.UserContext;

import javax.inject.Inject;

@APICommand(name = "resetApiLimit", responseObject=ApiLimitResponse.class, description="Reset api count")
public class ResetApiLimitCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(ResetApiLimitCmd.class.getName());

    private static final String s_name = "resetapilimitresponse";

    @Inject
    ApiRateLimitService _apiLimitService;

    @Inject
    ConfigurationDao _configDao;

    /////////////////////////////////////////////////////
    //////////////// API parameters /////////////////////
    /////////////////////////////////////////////////////

    @ACL
    @Parameter(name=ApiConstants.ACCOUNT, type=CommandType.UUID, entityType=AccountResponse.class,
            description="the ID of the acount whose limit to be reset")
    private Long accountId;

    /////////////////////////////////////////////////////
    /////////////////// Accessors ///////////////////////
    /////////////////////////////////////////////////////


    public Long getAccountId() {
        return accountId;
    }


    public void setAccountId(Long accountId) {
        this.accountId = accountId;
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
        Account account = UserContext.current().getCaller();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute(){
        boolean apiLimitEnabled = Boolean.parseBoolean(_configDao.getValue(Config.ApiLimitEnabled.key()));
        if ( !apiLimitEnabled ){
            throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR, "This api is only available when api.throttling.enabled = true.");
        }
        boolean result = _apiLimitService.resetApiLimit(this.accountId);
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to reset api limit counter");
        }
    }
}
