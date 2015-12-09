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
import org.apache.cloudstack.api.ApiErrorCode;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.QuotaResponseBuilder;
import org.apache.cloudstack.api.response.QuotaTariffResponse;
import org.apache.cloudstack.quota.vo.QuotaTariffVO;
import org.apache.log4j.Logger;

import javax.inject.Inject;

import java.util.Date;

@APICommand(name = "quotaTariffUpdate", responseObject = QuotaTariffResponse.class, description = "Update the tariff plan for a resource", since = "4.7.0", requestHasSensitiveInfo = false, responseHasSensitiveInfo = false)
public class QuotaTariffUpdateCmd extends BaseCmd {
    public static final Logger s_logger = Logger.getLogger(QuotaTariffUpdateCmd.class);
    private static final String s_name = "quotatariffupdateresponse";

    @Inject
    QuotaResponseBuilder _responseBuilder;

    @Parameter(name = ApiConstants.USAGE_TYPE, type = CommandType.INTEGER, required = true, description = "Integer value for the usage type of the resource")
    private Integer usageType;

    @Parameter(name = "value", type = CommandType.DOUBLE, required = true,  description = "The quota tariff value of the resource as per the default unit")
    private Double value;

    @Parameter(name = ApiConstants.START_DATE, type = CommandType.DATE, required = true, description = "The effective start date on/after which the quota tariff is effective and older tariffs are no longer used for the usage type. Use yyyy-MM-dd as the date format, e.g. startDate=2009-06-03.")
    private Date startDate;

    public int getUsageType() {
        return usageType;
    }

    public void setUsageType(int usageType) {
        this.usageType = usageType;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Date getStartDate() {
        return startDate == null ? null : new Date(startDate.getTime());
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate == null ? null : new Date(startDate.getTime());
    }

    public QuotaTariffUpdateCmd() {
        super();
    }

    @Override
    public String getCommandName() {
        return s_name;
    }

    @Override
    public void execute() {
        final QuotaTariffVO result = _responseBuilder.updateQuotaTariffPlan(this);
        if (result == null) {
            throw new ServerApiException(ApiErrorCode.INTERNAL_ERROR, "Failed to update quota tariff plan");
        }
        final QuotaTariffResponse response = _responseBuilder.createQuotaTariffResponse(result);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public long getEntityOwnerId() {
        return Account.ACCOUNT_ID_SYSTEM;
    }

}
