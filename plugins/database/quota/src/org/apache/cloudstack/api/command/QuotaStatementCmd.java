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

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.context.CallContext;
import org.apache.cloudstack.api.response.QuotaStatementResponse;

import com.cloud.user.Account;

@APICommand(name = "quotaStatement", responseObject = QuotaStatementResponse.class, description = "Create a quota statement", since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaStatementCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaStatementCmd.class.getName());

    private static final String s_name = "quotastatementresponse";

    @Parameter(name = ApiConstants.ACCOUNT, type = CommandType.STRING, description = "Optional, Account Id for which statement needs to be generated")
    private String accountName;

    @Parameter(name = ApiConstants.DOMAIN_ID, type = CommandType.UUID, entityType = DomainResponse.class, description = "Optional, If domain Id is given and the caller is domain admin then the statement is generated for domain.")
    private Long domainId;

    public QuotaStatementCmd() {
        super();
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        Long accountId = _accountService.finalyzeAccountId(accountName, domainId, null, true);
        if (accountId == null) {
            return CallContext.current().getCallingAccount().getId();
        }
        return Account.ACCOUNT_ID_SYSTEM;
    }

    @Override
    public void execute() {
        /**
         * final Pair<List<QuotaConfigurationVO>, Integer> result =
         * _quotaManager.listConfigurations(this);
         *
         * final List<QuotaStatementResponse> responses = new
         * ArrayList<QuotaStatementResponse>(); for (final QuotaConfigurationVO
         * resource : result.first()) { final QuotaStatementResponse
         * configurationResponse =
         * _quotaManager.createQuotaConfigurationResponse(resource);
         * configurationResponse.setObjectName("QuotaConfiguration");
         * responses.add(configurationResponse); }
         **/

        final ListResponse<QuotaStatementResponse> response = new ListResponse<QuotaStatementResponse>();
        // response.setResponses(responses, responses.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

}
