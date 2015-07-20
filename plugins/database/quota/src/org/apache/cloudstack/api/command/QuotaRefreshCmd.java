//Licensed to the Apache Software Foundation (ASF) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The ASF licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.
package org.apache.cloudstack.api.command;

import com.cloud.user.Account;

import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.SuccessResponse;
import org.apache.cloudstack.quota.QuotaManagerImpl;
import org.apache.log4j.Logger;

import javax.inject.Inject;

@APICommand(name = "quotaRefresh", responseObject = SuccessResponse.class, description = "Refresh the quota for all accounts if enabled", since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaRefreshCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaStatementCmd.class.getName());

    private static final String s_name = "quotarefreshresponse";

    @Inject
    QuotaManagerImpl _quotaManager;

    public QuotaRefreshCmd() {
        super();
    }

    public QuotaRefreshCmd(final QuotaManagerImpl quotaManager) {
        super();
        _quotaManager = quotaManager;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() throws ServerApiException {
        boolean result = _quotaManager.calculateQuotaUsage();
        if (result) {
            SuccessResponse response = new SuccessResponse(getCommandName());
            this.setResponseObject(response);
        } else {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to refresh quota records");
        }
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
