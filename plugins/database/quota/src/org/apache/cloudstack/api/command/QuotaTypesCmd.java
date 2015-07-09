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

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.response.QuotaConfigurationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaTypeResponse;
import org.apache.cloudstack.quota.QuotaUsageTypes;

import com.cloud.user.Account;

@APICommand(name = "quotaTypes", responseObject = QuotaConfigurationResponse.class, description = "Lists all Quota type resources", since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaTypesCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaTypesCmd.class.getName());

    private static final String s_name = "quotatyperesponse";

    public QuotaTypesCmd() {
        super();
    }

    @Override
    public void execute() {
        final List<QuotaTypeResponse> responses = QuotaUsageTypes.listQuotaUsageTypes();
        final ListResponse<QuotaTypeResponse> response = new ListResponse<QuotaTypeResponse>();
        response.setResponses(responses, responses.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
