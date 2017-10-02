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
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.log4j.Logger;

import javax.inject.Inject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@APICommand(name = "quotaTariffList", responseObject = QuotaTariffResponse.class, description = "Lists all quota tariff plans", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaTariffListCmd extends BaseListCmd {
    public static final Logger s_logger = Logger.getLogger(QuotaTariffListCmd.class);
    private static final String s_name = "quotatarifflistresponse";

    @Inject
    QuotaResponseBuilder _responseBuilder;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, required = false, description = "Usage type of the resource")
    private Integer usageType;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, required = false, description = "The effective start date on/after which the quota tariff is effective and older tariffs are no longer used for the usage type. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-03.")
    private Date effectiveDate;

    public QuotaTariffListCmd() {
        super();
    }

    @Override
    public void execute() {
        final List<QuotaTariffVO> result = _responseBuilder.listQuotaTariffPlans(this);

        final List<QuotaTariffResponse> responses = new ArrayList<QuotaTariffResponse>();
        for (final QuotaTariffVO resource : result) {
            if (s_logger.isDebugEnabled()) {
                s_logger.debug("Result desc=" + resource.getDescription() + " date=" + resource.getEffectiveOn() + " val=" + resource.getCurrencyValue());
            }
            responses.add(_responseBuilder.createQuotaTariffResponse(resource));
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

    public Date getEffectiveDate() {
        return effectiveDate ==null ? null : new Date(effectiveDate.getTime());
    }

    public Integer getUsageType() {
        return usageType;
    }

    public void setUsageType(Integer usageType) {
        this.usageType = usageType;
    }

}
