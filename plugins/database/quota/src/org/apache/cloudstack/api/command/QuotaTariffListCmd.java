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
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.quota.QuotaDBUtilsImpl;
import org.apache.cloudstack.quota.QuotaTariffVO;
import org.apache.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@APICommand(name = "quotaTariffList", responseObject = QuotaTariffResponse.class, description = "Lists all quota tariff plans", since = "4.6.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaTariffListCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(QuotaTariffListCmd.class.getName());
    private static final String s_name = "quotatarifflistresponse";

    @Inject
    QuotaDBUtilsImpl _quotaDBUtils;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, required = false, description = "Usage type of the resource")
    private Integer usageType;

    public QuotaTariffListCmd() {
        super();
    }

    public QuotaTariffListCmd(final QuotaDBUtilsImpl quotaDBUtils) {
        super();
        _quotaDBUtils = quotaDBUtils;
    }

    @Override
    public void execute() {
        final List<QuotaTariffVO> result = _quotaDBUtils.listQuotaTariffPlans(this);

        final List<QuotaTariffResponse> responses = new ArrayList<QuotaTariffResponse>();
        for (final QuotaTariffVO resource : result) {
            responses.add(_quotaDBUtils.createQuotaTariffResponse(resource));
        }

        final ListResponse<QuotaTariffResponse> response = new ListResponse<QuotaTariffResponse>();
        response.setResponses(responses);
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

    public Integer getUsageType() {
        return usageType;
    }

    public void setUsageType(Integer usageType) {
        this.usageType = usageType;
    }

}
