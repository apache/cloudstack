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
import org.apache.cloudstack.quota.QuotaMappingVO;
import org.apache.cloudstack.quota.QuotaDBUtilsImpl;

import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "quotaMapping", responseObject = QuotaConfigurationResponse.class, description = "Lists all Quota and Usage configurations", since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaMappingCmd extends BaseListCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaMappingCmd.class.getName());

    private static final String s_name = "quotaconfigurationresponse";

    @Inject
    QuotaDBUtilsImpl _quotaDBUtils;

    @Parameter(name = "type", type = CommandType.STRING, required = false, description = "Usage type of the resource")
    private String usageType;

    public QuotaMappingCmd() {
        super();
    }

    public QuotaMappingCmd(final QuotaDBUtilsImpl quotaDBUtils) {
        super();
        _quotaDBUtils = quotaDBUtils;
    }

    @Override
    public void execute() {
        final Pair<List<QuotaMappingVO>, Integer> result = _quotaDBUtils.listConfigurations(this);

        final List<QuotaConfigurationResponse> responses = new ArrayList<QuotaConfigurationResponse>();
        for (final QuotaMappingVO resource : result.first()) {
            final QuotaConfigurationResponse configurationResponse = _quotaDBUtils.createQuotaConfigurationResponse(resource);
            configurationResponse.setObjectName("quotamapping");
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
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

}
