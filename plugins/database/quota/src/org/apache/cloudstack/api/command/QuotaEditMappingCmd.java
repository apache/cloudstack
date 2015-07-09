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
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.DomainResponse;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaConfigurationResponse;
import org.apache.cloudstack.api.response.QuotaCreditsResponse;
import org.apache.cloudstack.quota.QuotaConfigurationVO;
import org.apache.cloudstack.quota.QuotaDBUtilsImpl;

import com.cloud.user.Account;
import com.cloud.utils.Pair;

@APICommand(name = "quotaEditMapping", responseObject = QuotaCreditsResponse.class, description = "Edit the mapping for a resource", since = "4.2.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaEditMappingCmd extends BaseCmd {

    public static final Logger s_logger = Logger.getLogger(QuotaStatementCmd.class.getName());

    private static final String s_name = "quotaconfigurationresponse";

    @Inject
    QuotaDBUtilsImpl _quotaDBUtils;

    @Parameter(name = "type", type = CommandType.STRING, required = false, description = "Usage type of the resource")
    private String usageType;

    @Parameter(name = "value", type = CommandType.INTEGER, entityType = DomainResponse.class, description = "The quota vale of the resource as per the default unit")
    private Integer value;

    public String getUsageType() {
        return usageType;
    }

    public void setUsageType(String usageType) {
        this.usageType = usageType;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public QuotaEditMappingCmd() {
        super();
    }

    public QuotaEditMappingCmd(final QuotaDBUtilsImpl quotaDBUtils) {
        super();
        _quotaDBUtils = quotaDBUtils;
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        final Pair<List<QuotaConfigurationVO>, Integer> result = _quotaDBUtils.editQuotaMapping(this);

        final List<QuotaConfigurationResponse> responses = new ArrayList<QuotaConfigurationResponse>();
        for (final QuotaConfigurationVO resource : result.first()) {
            final QuotaConfigurationResponse configurationResponse = _quotaDBUtils.createQuotaConfigurationResponse(resource);
            configurationResponse.setObjectName("QuotaConfiguration");
            responses.add(configurationResponse);
        }

        final ListResponse<QuotaConfigurationResponse> response = new ListResponse<QuotaConfigurationResponse>();
        response.setResponses(responses, responses.size());
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
