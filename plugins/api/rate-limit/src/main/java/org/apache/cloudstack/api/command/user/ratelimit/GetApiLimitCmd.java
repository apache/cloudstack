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
package org.apache.cloudstack.api.command.user.ratelimit;

import javax.inject.Inject;

import org.apache.log4j.Logger;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ApiLimitResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.framework.config.dao.ConfigurationDao;
import org.apache.cloudstack.ratelimit.ApiRateLimitService;

import com.cloud.configuration.Config;
import com.cloud.user.Account;

@APICommand(name = "getApiLimit", responseObject = ApiLimitResponse.class, description = "Get API limit count for the caller",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class GetApiLimitCmd extends BaseCmd {
    private static final Logger s_logger = Logger.getLogger(GetApiLimitCmd.class.getName());

    private static final String s_name = "getapilimitresponse";

    @Inject
    ApiRateLimitService _apiLimitService;

    @Inject
    ConfigurationDao _configDao;

    /////////////////////////////////////////////////////
    /////////////// API Implementation///////////////////
    /////////////////////////////////////////////////////

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Account account = CallContext.current().getCallingAccount();
        if (account != null) {
            return account.getId();
        }

        return Account.ACCOUNT_ID_SYSTEM; // no account info given, parent this command to SYSTEM so ERROR events are tracked
    }

    @Override
    public void execute() {
        boolean apiLimitEnabled = Boolean.parseBoolean(_configDao.getValue(Config.ApiLimitEnabled.key()));
        if (!apiLimitEnabled) {
            throw new ServerApiException(ApiErrorCode.UNSUPPORTED_ACTION_ERROR, "This api is only available when api.throttling.enabled = true.");
        }
        Account caller = CallContext.current().getCallingAccount();
        ApiLimitResponse response = _apiLimitService.searchApiLimit(caller);
        response.setResponseName(getCommandName());
        response.setObjectName("apilimit");
        this.setResponseObject(response);
    }
}
