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
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.AccountResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaConfigureEmailResponse;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;

import javax.inject.Inject;

@APICommand(name = "quotaListEmailConfiguration", responseObject = QuotaConfigureEmailResponse.class, description = "List quota email template configurations", since = "4.20.0.0",
        requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaListEmailConfigurationCmd extends BaseCmd {

    @Parameter(name = ApiConstants.ACCOUNT_ID, type = BaseCmd.CommandType.UUID, entityType = AccountResponse.class, required = true,
            description = "Account ID for which to list quota template email configurations")
    private long accountId;

    @Inject
    private QuotaResponseBuilder responseBuilder;

    @Override
    public void execute() {
        ListResponse<QuotaConfigureEmailResponse> response = new ListResponse<>();
        response.setResponses(responseBuilder.listEmailConfiguration(accountId));
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }
}
