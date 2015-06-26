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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.QuotaConfigurationResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.quota.QuotaConfigurationVO;
import org.apache.cloudstack.quota.QuotaManager;

import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "listQuotaConfigurations", responseObject = QuotaConfigurationResponse.class, description = "Lists all Quota and Usage configurations", since = "4.2.0",
    requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class ListQuotaConfigurationsCmd extends BaseListCmd {

    public static final Logger s_logger = Logger
            .getLogger(ListQuotaConfigurationsCmd.class.getName());

    private static final String s_name = "quotaconfigurationresponse";

    @Inject
    private QuotaManager _quotaManager;

    @Parameter(name = "usageType", type = CommandType.STRING, required = false, description = "Usage type of the resource")
    private String _usageType;


    public ListQuotaConfigurationsCmd() {
        super();
    }


    public ListQuotaConfigurationsCmd(final QuotaManager quotaManager) {
        super();
        _quotaManager = quotaManager;
    }

    @Override
    public void execute() {
        final Pair<List<QuotaConfigurationVO>, Integer>  result = _quotaManager.listConfigurations(this);

        final List<QuotaConfigurationResponse> responses = new ArrayList<QuotaConfigurationResponse>();
        for (final QuotaConfigurationVO resource : result.first()) {
            final QuotaConfigurationResponse configurationResponse = _quotaManager.createQuotaConfigurationResponse(resource);
            configurationResponse.setObjectName("QuotaConfiguration");
            responses.add(configurationResponse);
        }

        final ListResponse<QuotaConfigurationResponse> response = new ListResponse<QuotaConfigurationResponse>();
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

    public String getUsageType() {
        return _usageType;
    }


    public void setUsageType(String usageType) {
        this._usageType = usageType;
    }


}
